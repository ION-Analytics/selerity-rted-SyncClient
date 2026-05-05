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
import java.io.StringReader;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.selerity.sync.client.util.StatsLogger;

/**
 * A mock implementation of the NarwhalService interface.
 * Allows pre-scripted responses to be queued and used as results for subsequent queries.
 */
public class MockNarwhalServiceImpl implements NarwhalService {

    private static final Log log = LogFactory.getLog(MockNarwhalServiceImpl.class);

    private final Queue<String> responses = new LinkedList<>();
    private final String name;
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    public MockNarwhalServiceImpl(final String name) {
        this.name = name;
    }

    /**
     * Adds a response to the queue.  Note that response must be a String in JSON format
     * which complies with Narwhal protocol requirements for format.
     */
    public void addResponse(final String response) {
        responses.add(response);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public StatsLogger<String, Long> getMethodStatsLogger() {
        // Auto-generated method stub
        return null;
    }

    @Override
    public JsonElement dispatch(final Request request, final Session session) throws DispatchException {
        final Response response = dispatchWithResponse(request, session);
        if (response.getError() != null) {
            throw response.getError();
        }
        return response.getResult();
    }

    @Override
    public Response dispatchWithResponse(final Request request, final Session session) throws DispatchException {
        return dispatchWithResponse(request, session, null);
    }

    @Override
    public Response dispatchWithResponse(final Request request, final Session session, final String id) throws DispatchException {
        JsonReader reader = null;
        try {
            @SuppressWarnings("Duplicates")
            FullRequest fullRequest = new FullRequest(request,
                    session.getHeaderParameter("user"),
                    session.getHeaderParameter("token"),
                    session.getHeaderParameter("client"),
                    session.getHeaderParameter("mode"),
                    (id != null && !id.isEmpty()) ? id : UUID.randomUUID().toString());

            reader = dispatch(fullRequest);

            return gson.fromJson(reader, Response.class);
        } catch (DispatchException dx) {
            throw dx;
        } catch (Exception ex) {
            throw new DispatchException(DispatchException.INTERNAL_ERROR,
                    "caught " + ex + " while dispatching to service " + name, ex.toString());
        } finally {
            if (reader != null) {
                try {
                    // this is important - without it, the socket sometimes gets left open indefinitely.
                    reader.close();
                } catch (Exception ex) {
                    // ignore the exception
                }
            }
        }
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public JsonReader dispatch(final FullRequest request) throws Exception {
        final String mockResponse = responses.poll();
        if (mockResponse == null) {
            throw new NullPointerException("no mock responses left in queue");
        }
        if (log.isDebugEnabled())
            log.debug("returning mock response: " + mockResponse);
        return new JsonReader(new StringReader(mockResponse));
    }

    @SuppressWarnings({"DuplicateThrows", "RedundantThrows"})
    @Override
    public JsonReader dispatchForResultStream(final Request request, final Session session) throws DispatchException, IOException,
            Exception {
        final String mockResponse = responses.remove();
        if (mockResponse == null) {
            throw new NullPointerException("no mock responses left in queue");
        }
        if (log.isDebugEnabled())
            log.debug("returning mock response: " + mockResponse);
        final JsonReader reader = new JsonReader(new StringReader(mockResponse));
        return MiscUtils.extractResultStream(reader);
    }

}
