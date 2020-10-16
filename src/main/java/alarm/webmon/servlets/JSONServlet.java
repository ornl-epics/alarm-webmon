/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package alarm.webmon.servlets;

import static alarm.webmon.model.AlarmMonitor.json_factory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerator;

/** Servled that returns JSON
 *  @author Kay Kasemir
 */
public abstract class JSONServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    @Override
    final protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final JsonGenerator g = json_factory.createGenerator(buf);
        writeJson(request, g);
        g.flush();

        response.setContentType("application/json");
        final PrintWriter writer = response.getWriter();
        writer.append(buf.toString());
    }

    /** Derived class implements this to fill the JSON that's returned by servlet
     *
     *  @param request {@link HttpServletRequest}
     *  @param g {@link JsonGenerator}
     *  @throws IOException on error
     */
    protected abstract void writeJson(final HttpServletRequest request, final JsonGenerator g) throws IOException;
}
