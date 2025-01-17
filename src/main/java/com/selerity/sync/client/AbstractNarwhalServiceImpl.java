/*
 * (C) Copyright Selerity, Inc. 2009-2019. All rights reserved. This source code
 * is confidential and proprietary information of Selerity Inc. and may be used
 * only by a recipient designated by and for the purposes permitted by Selerity
 * Inc. in writing. Reproduction of, dissemination of, modifications to or
 * creation of derivative works from this source code, whether in source or
 * binary forms, by any means and in any form or manner, is expressly
 * prohibited, except with the prior written permission of Selerity Inc.. THIS
 * CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND,
 * EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE. This notice may
 * not be removed from the software by any user thereof.
 */
package com.selerity.sync.client;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.selerity.sync.client.util.StatsLogger;

/**
 * An abstract class for building clients of a Narwhal-based service.
 */
public abstract class AbstractNarwhalServiceImpl implements NarwhalService {

    private static final Log log = LogFactory.getLog(AbstractNarwhalServiceImpl.class);

    protected final Gson gson;

    protected final long WARN_DISPATCH_TIME_MILLIS = 30000;  // 30 seconds is too long for most dispatches to take

    protected String serviceName = null;

    protected final StatsLogger<String, Long> methodStatsLogger;

    /**
     * Creates an abstract instance of a NarwhalService endpoint with the given name.
     * Enables method statistics if the appropriate system property is set.
     *
     * @param serviceName
     */
    public AbstractNarwhalServiceImpl(String serviceName) {
        gson = initGson();
        this.serviceName = serviceName;

        boolean enableStats = false;
        try {
            String enableStatsProp = System.getProperty(ENABLE_METHOD_STATS_PROPERTY_NAME, "false");
            enableStats = Boolean.parseBoolean(enableStatsProp);
        } catch (Exception ex) {
            log.error("caught " + ex + " while attempting to set method statistics logging", ex);
            enableStats = false;
        }

        if (enableStats) {
            log.info("method statistics logging enabled");
            this.methodStatsLogger = new StatsLogger<>();
        } else {
            log.info("method statistics logging disabled");
            this.methodStatsLogger = null;
        }
    }

    /**
     * Creates an abstract instance of a NarwhalService endpoint with the given name
     * and method statistics logger.  If the method statistics logger is null then stats
     * logging is disabled.
     *
     * @param serviceName
     * @param methodStatsLogger
     */
    public AbstractNarwhalServiceImpl(String serviceName, StatsLogger<String, Long> methodStatsLogger) {
        gson = initGson();
        this.serviceName = serviceName;
        this.methodStatsLogger = methodStatsLogger;

        if (methodStatsLogger == null) {
            log.info("method statistics logging disabled");
        } else {
            log.info("method statistics logging enabled");
        }
    }

    private static final Gson initGson() {
        GsonBuilder builder = new GsonBuilder().serializeNulls();
        builder.registerTypeAdapter(FullRequest.class, new FullRequest.FullRequestDeserializer());
        builder.registerTypeAdapter(FullRequest.class, new FullRequest.FullRequestSerializer());
        builder.registerTypeAdapter(Response.class, new Response.ResponseDeserializer());
        builder.registerTypeAdapter(Response.class, new Response.ResponseSerializer());
        return builder.create();
    }

    public void setName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getName() {
        return this.serviceName;
    }


    public StatsLogger<String, Long> getMethodStatsLogger() {
        return methodStatsLogger;
    }


    /**
     * Dispatches a Narwhal request and gets back a JsonReader pointed at the result object.
     *
     * Note that the caller *must* close the JsonReader to enable the connection
     * to the server to be closed (otherwise we end up with sockets left open indefinitely).
     *
     * @param request
     * @param session
     * @return
     * @throws DispatchException
     * @throws IOException
     * @throws Exception
     */
    public JsonReader dispatchForResultStream(Request request, Session session)
            throws DispatchException, IOException, Exception {
        final FullRequest fullRequest = new FullRequest(request, session.getHeaderParameter("user"),
                session.getHeaderParameter("token"), session.getHeaderParameter("client"),
                session.getHeaderParameter("mode"), UUID.randomUUID().toString());
        final JsonReader responseStream = dispatch(fullRequest);
        return MiscUtils.extractResultStream(responseStream);
    }

}
