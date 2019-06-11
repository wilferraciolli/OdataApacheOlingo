/*
 * (c) Midland Software Limited 2019
 * Name     : DemoServlet.java
 * Author   : ferraciolliw
 * Date     : 10 Jun 2019
 */
package com.wiltech.odata.web;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wiltech.odata.data.Storage;
import com.wiltech.odata.service.DemoEdmProvider;
import com.wiltech.odata.service.DemoEntityCollectionProcessor;
import com.wiltech.odata.service.DemoEntityProcessor;
import com.wiltech.odata.service.DemoPrimitiveProcessor;

/**
 * The type Demo servlet.
 */
public class DemoServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(DemoServlet.class);

    /**
     * Service.
     * @param req the req
     * @param resp the resp
     * @throws ServletException the servlet exception
     * @throws IOException the io exception
     */
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            final HttpSession session = req.getSession(true);
            Storage storage = (Storage) session.getAttribute(Storage.class.getName());
            if (storage == null) {
                storage = new Storage();
                session.setAttribute(Storage.class.getName(), storage);
            }

            // create odata handler and configure it with EdmProvider and Processor
            final OData odata = OData.newInstance();
            final ServiceMetadata edm = odata.createServiceMetadata(new DemoEdmProvider(), new ArrayList<EdmxReference>());

            //register handlers
            final ODataHttpHandler handler = odata.createHandler(edm);
            handler.register(new DemoEntityCollectionProcessor(storage));
            handler.register(new DemoEntityProcessor(storage));
            handler.register(new DemoPrimitiveProcessor(storage));

            // let the handler do the work
            handler.process(req, resp);
        } catch (final RuntimeException e) {
            LOG.error("Server Error occurred in ExampleServlet", e);
            throw new ServletException(e);
        }
    }
}
