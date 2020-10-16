/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package alarm.webmon.model;

import static alarm.webmon.ContextHandler.logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Alarm Monitor
 *
 *  <p>Connects to Kafka and tracks current alarms,
 *  active and acknowledged.
 *
 *  @author Kay Kasemir
 */
public class AlarmMonitor
{
    /** For JSON parser */
    public static final ObjectMapper mapper = new ObjectMapper();

    public static final JsonFactory json_factory = new JsonFactory();

    /** Path prefix for config updates */
    public static final String CONFIG_PREFIX = "config:";

    /** Path prefix for state updates */
    public static final String STATE_PREFIX = "state:";

    private final Consumer<String, String> consumer;
    private volatile boolean running = true;
    private final AtomicLong message_count = new AtomicLong();

    /** Marker for a PV that has been deleted */
    private static final AlarmPV DELETED = new AlarmPV("null");

    private final ConcurrentHashMap<String, AlarmPV> config = new ConcurrentHashMap<>();
    private final KeySetView<AlarmPV,Boolean> active = ConcurrentHashMap.newKeySet();
    private final KeySetView<AlarmPV,Boolean> acknowledged = ConcurrentHashMap.newKeySet();

    private final Thread thread;

    public AlarmMonitor(final String kafka_servers, final List<String> topics)
    {
        consumer = createConsumer(kafka_servers, topics);
        thread = new Thread(this::handleMessages, "Message Handler");
        thread.setDaemon(true);
        thread.start();
    }

    private Consumer<String, String> createConsumer(final String kafka_servers, final List<String> topics)
    {
        final Properties props = new Properties();
        props.put("bootstrap.servers", kafka_servers);
        // API requires for Consumer to be in a group.
        // Each alarm client must receive all updates,
        // cannot balance updates across a group
        // --> Use unique group for each client
        final String group_id = "Alarm-" + UUID.randomUUID();
        props.put("group.id", group_id);

        logger.log(Level.FINE, () -> group_id + " subscribes to " + kafka_servers + " for " + topics);

        // Read key, value as string
        final Deserializer<String> deserializer = new StringDeserializer();
        final Consumer<String, String> consumer = new KafkaConsumer<>(props, deserializer, deserializer);

        // Rewind whenever assigned to partition
        final ConsumerRebalanceListener crl = new ConsumerRebalanceListener()
        {
            @Override
            public void onPartitionsAssigned(final Collection<TopicPartition> parts)
            {
                // For 'configuration', start reading all messages.
                // For 'commands', OK to just read commands from now on.
                for (TopicPartition part : parts)
                {
                    consumer.seekToBeginning(Arrays.asList(part));
                    logger.info("Reading from start of '" + part.topic() + "'");
                }
            }

            @Override
            public void onPartitionsRevoked(final Collection<TopicPartition> parts)
            {
                // Ignore
            }
        };
        consumer.subscribe(topics, crl);

        return consumer;
    }

    private void handleMessages()
    {
        try
        {
            while (running)
            {
                logger.finer("checking for messages...");
                final ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records)
                    handleUpdate(record);
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Message handling error", ex);
        }
        logger.fine("Message handler done.");
    }

    /** Handle one state: or config: update
     *  @param record Received record
     *  @throws Exception on error
     */
    private void handleUpdate(final ConsumerRecord<String, String> record) throws Exception
    {
        message_count.incrementAndGet();

        final String path;
        final JsonNode json = record.value() == null
                            ? null
                            : parseJsonText(record.value());
        // Ideally, we first get a 'config:' to define the item,
        // followed by 'state:' updates.
        // But the configuration can be modified (new guidance, ...) without a state change,
        // so we might get 'state' followed by 'config', meaning a 'state' for an at the time
        // unknown entry must be memorized.
        // Finally, there is a small chance that a 'config: .. null' entry to delete an item
        // is followed by just one more 'state' update, so remember deletions.
        if (record.key().startsWith(CONFIG_PREFIX))
        {
            path = record.key().substring(CONFIG_PREFIX.length());
            if (json == null)
            {
                config.put(path, DELETED);
                active.removeIf(pv -> pv.getPath().equals(path));
                acknowledged.removeIf(pv -> pv.getPath().equals(path));
            }
            else
            {
                // PV Configuration entries have a "description"
                JsonNode jn = json.get("description");
                if (jn == null)
                    return;

                final String desc = jn.asText();

                // "enabled":false
                jn = json.get("enabled");
                final boolean enabled = jn == null
                                      ? true
                                      : jn.asBoolean();

                config.compute(path, (p, v) ->
                {
                    if (! enabled)
                    {   // Tread disabled similar to deleted
                        if (v != null  &&  v != DELETED)
                        {
                            active.remove(v);
                            acknowledged.remove(v);
                        }
                        return DELETED;
                    }
                    // Restore explicitly deleted entry, create new entry
                    if (v == DELETED  ||  v == null)
                        v = new AlarmPV(p);
                    v.setDescription(desc);
                    return v;
                });
            }
        }
        else if (record.key().startsWith(STATE_PREFIX))
        {
            path = record.key().substring(STATE_PREFIX.length());
            JsonNode jn = json.get("severity");
            if (jn != null)
            {
                final SeverityLevel severity = SeverityLevel.valueOf(jn.asText());

                // Ignore updates that are not about PVs
                jn = json.get("current_severity");
                if (jn == null)
                    return;

                final SeverityLevel current_severity = SeverityLevel.valueOf(jn.asText());

                jn = json.get("message");
                final String message = jn == null
                                     ? "OK"
                                     : jn.asText();

                jn = json.get("current_message");
                final String current_message = jn == null
                                             ? "OK"
                                             : jn.asText();

                jn = json.get("value");
                final String value = jn == null
                                   ? ""
                                   : jn.asText();

                final Instant timestamp;
                jn = json.get("time");
                if (jn == null)
                    timestamp = Instant.now();
                else
                {
                    JsonNode sub = jn.get("seconds");
                    final long sec = sub == null ? 0 : sub.asLong();

                    sub = jn.get("nano");
                    final int nano = sub == null ? 0 : sub.asInt();

                    timestamp = Instant.ofEpochSecond(sec, nano);
                }

                config.compute(path,  (p, v) ->
                {
                    // Ignore state update of explicitly deleted entry
                    if (v == DELETED)
                        return DELETED;
                    if (v == null)
                        v = new AlarmPV(p);
                    v.setAlarm(severity, message, current_severity, current_message, value, timestamp);
                    if (severity.isActive())
                    {
                        active.add(v);
                        acknowledged.remove(v);
                    }
                    else if (severity != SeverityLevel.OK)
                    {
                        active.remove(v);
                        acknowledged.add(v);
                    }
                    else
                    {
                        active.remove(v);
                        acknowledged.remove(v);
                    }
                    return v;
                });
            }
        }
        else
            return;
    }

    public JsonNode parseJsonText(final String json_text) throws Exception
    {
        try
        (
            final JsonParser jp = mapper.getFactory().createParser(json_text);
        )
        {
            return mapper.readTree(jp);
        }
    }

    public Set<AlarmPV> getActiveAlarms()
    {
        return active;
    }

    public Set<AlarmPV> getAchnowledgedAlarms()
    {
        return acknowledged;
    }

    public void dump()
    {
        System.out.println("\nMessage Count: " + message_count.get());
        System.out.println("\nCONFIG:");
        final List<String> paths = new ArrayList<>();
        Enumeration<String> keys = config.keys();
        while (keys.hasMoreElements())
            paths.add(keys.nextElement());
        Collections.sort(paths);
        int i = 0;
        for (String path : paths)
            System.out.format("%4d %-120s %s\n", ++i, path, config.get(path));

        System.out.println("\nACTIVE:");
        for (AlarmPV pv : active)
            System.out.println(pv);

        System.out.println("\nACKNOWLEDGED:");
        for (AlarmPV pv : acknowledged)
            System.out.println(pv);
    }

    public void close()
    {
        running = false;
        try
        {
            thread.join(2000);
        }
        catch (InterruptedException ex)
        {
            logger.log(Level.WARNING, "Cannot join message handling thread", ex);
        }
        consumer.close();
    }
}
