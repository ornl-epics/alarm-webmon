/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package alarm.webmon;

import static alarm.webmon.ContextHandler.logger;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import alarm.webmon.model.AlarmMonitor;

/** Standalone demo of the alarm client
 *  @author Kay Kasemir
 */
public class Demo
{
    public static void main(String[] args) throws Exception
    {
        Logger root = Logger.getLogger("");
        root.setLevel(Level.FINE);
        for (Handler handler : root.getHandlers())
            handler.setLevel(root.getLevel());

        final AlarmMonitor monitor = new AlarmMonitor("localhost:9092", Arrays.asList("Accelerator"));

        logger.info("Waiting...");

        TimeUnit.SECONDS.sleep(5);
        monitor.dump();
        monitor.close();

        logger.info("Done.");
    }
}
