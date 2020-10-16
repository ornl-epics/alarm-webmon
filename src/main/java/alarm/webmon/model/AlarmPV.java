/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package alarm.webmon.model;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerator;

/** Information for PV node in the alarm tree
 *  @author Kay Kasemir
 */
public class AlarmPV
{
    final private static ZoneId zone = ZoneId.systemDefault();
    final private static String MILLI_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    final public static DateTimeFormatter MILLI_FORMAT= DateTimeFormatter.ofPattern(MILLI_PATTERN).withZone(zone);



    private final String path;
    private volatile String description;
    private volatile SeverityLevel severity, current_severity;
    private volatile String message, current_message;
    private volatile String value;
    private volatile Instant timestamp;

    public AlarmPV(final String path)
    {
        this.path = path;
        this.description = AlarmTreePath.getName(path);
        severity = current_severity = SeverityLevel.OK;
        message = current_message = "OK";
        value = "";
        timestamp = Instant.now();
    }

    public String getPath()
    {
        return path;
    }

    public void setDescription(final String description)
    {
        this.description = description;
    }

    public void setAlarm(final SeverityLevel severity, final String message,
                         final SeverityLevel current_severity, final String current_message,
                         final String value, final Instant timestamp)
    {
        this.severity = severity;
        this.message = message;
        this.current_severity = current_severity;
        this.current_message = current_message;
        this.value = value;
        this.timestamp = timestamp;
    }

    public void serialize(final JsonGenerator g) throws IOException
    {
        g.writeStartObject();
        g.writeStringField("path", path);
        g.writeStringField("name", AlarmTreePath.getName(path));
        g.writeStringField("description", description);
        g.writeStringField("severity", severity.name());
        g.writeStringField("message", message);
        g.writeStringField("current_severity", current_severity.name());
        g.writeStringField("current_message", current_message);
        g.writeStringField("value", value);
        g.writeStringField("time", MILLI_FORMAT.format(timestamp));
        g.writeEndObject();
    }

    @Override
    public String toString()
    {
        return AlarmTreePath.getName(path) + " (" + description + "): " + severity + "/" + message;
    }
}
