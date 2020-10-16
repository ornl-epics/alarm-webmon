/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package alarm.webmon;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import alarm.webmon.model.AlarmMonitor;

/** Tomcat context handler
 *
 *  <p>Maintains the singleton {@link AlarmMonitor}
 *
 *  @author Kay Kasemir
 */
@WebListener
public class ContextHandler implements ServletContextListener
{
    /** Alarm system logger */
    public static final Logger logger = Logger.getLogger(AlarmMonitor.class.getPackage().getName());

    public static final String ALARM_SERVER = "ALARM_SERVER";
    public static final String ALARM_CONFIG = "ALARM_CONFIG";

    private static AlarmMonitor monitor;

    @Override
    public void contextInitialized(final ServletContextEvent ev)
    {
        final ServletContext context = ev.getServletContext();

        String server = System.getenv(ALARM_SERVER);
        if (server == null)
            server = "localhost:9092";

        String config = System.getenv(ALARM_CONFIG);
        if (config == null)
            config = "Accelerator";

        logger.log(Level.INFO, "===========================================");
        logger.log(Level.INFO, "Alarm Webmon " + context.getContextPath() + " started");
        logger.log(Level.INFO, ALARM_SERVER + "=" + server);
        logger.log(Level.INFO, ALARM_CONFIG + "=" + config);
        logger.log(Level.INFO, "===========================================");

        monitor = new AlarmMonitor(server, Arrays.asList(config));
    }

    public static AlarmMonitor getAlarmMonitor()
    {
        return monitor;
    }

    @Override
    public void contextDestroyed(final ServletContextEvent ev)
    {
        final ServletContext context = ev.getServletContext();

        monitor.close();
        logger.log(Level.INFO, "===========================================");
        logger.log(Level.INFO, context.getContextPath() + " shut down");
        logger.log(Level.INFO, "===========================================");
    }
}
