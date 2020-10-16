/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package alarm.webmon.servlets;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonGenerator;

import alarm.webmon.ContextHandler;
import alarm.webmon.model.AlarmMonitor;
import alarm.webmon.model.AlarmPV;


/** Servlet for polling current alarms
 *  @author Kay Kasemir
 */
@WebServlet("/alarms/*")
public class AlarmsServlet extends JSONServlet
{
    private static final long serialVersionUID = 1L;

    @Override
    protected void writeJson(final HttpServletRequest request, final JsonGenerator g) throws IOException
    {
        final AlarmMonitor monitor = ContextHandler.getAlarmMonitor();

        g.writeStartObject();
        {
            g.writeArrayFieldStart("active");
            for (AlarmPV pv :  monitor.getActiveAlarms())
                pv.serialize(g);
            g.writeEndArray();
        }
        {
            g.writeArrayFieldStart("acknowledged");
            for (AlarmPV pv :  monitor.getAchnowledgedAlarms())
                pv.serialize(g);
            g.writeEndArray();
        }
        g.writeEndObject();
    }
}
