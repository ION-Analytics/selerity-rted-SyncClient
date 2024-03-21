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

import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.selerity.sync.client.util.StatsLogger;

public interface NarwhalService {

    public static final String ENABLE_METHOD_STATS_PROPERTY_NAME = "com.selerity.sync.client.EnableMethodStats";

    /**
     * Returns a name for the service instance, useful mainly for debugging purposes when there are multiple service
     * instances.
     */
    String getName();

    /**
     * Returns the statistics logger for method latency stats if stats logging has been enabled.
     * Otherwise returns null.
     *
     * Note that method statistics logging can be enabled by setting the system property com.selerity.sync.client.EnableMethodStats = true
     */
    StatsLogger<String, Long> getMethodStatsLogger();

    /**
     * Dispatches the request, filling in the header from the session information as appropriate.
     *
     * If the response is not an error then the result will be returned.
     *
     * If the response is an error, the corresponding exception will be thrown.
     *
     * Note that this method will *not* work correctly for streaming responses since it assumes a single response to each request.
     *
     * @param request
     * @param session
     * @return the result
     * @throws DispatchException wraps the error returned by the server or any local exeption (e.g. IO exception) that may occur.
     */
    JsonElement dispatch(Request request, Session session) throws DispatchException;

    /**
     * Dispatches the request with a random id, filling in the header from the session information as appropriate.
     *
     * Returns the response object regardless of whether it's an error or not.
     *
     * Will only throw an exception if a local exception occurs.
     * Server-side exceptions will be returned as the error field
     * of the response object.
     *
     * Note that this method will *not* work correctly for streaming responses since it assumes a single response to each request.
     *
     * @param request
     * @param session
     * @return the response
     * @throws DispatchException
     */
    Response dispatchWithResponse(Request request, Session session) throws DispatchException;

    /**
     * Dispatches the request with a given id, filling in the header from the session information as appropriate.
     *
     * Returns the response object regardless of whether it's an error or not.
     *
     * Will only throw an exception if a local exception occurs.
     * Server-side exceptions will be returned as the error field of the response object.
     *
     * Note that this method will *not* work correctly for streaming responses since it assumes a single response to each request.
     *
     * @param request
     * @param session
     * @param id
     * @return the response
     * @throws DispatchException
     */
    Response dispatchWithResponse(Request request, Session session, String id) throws DispatchException;

    /**
     * Dispatches the request (the header must already be filled in with session information if needed) and returns
     * a JsonReader from which the response(s) can be read.
     *
     * Will only throw exceptions due to local failures; server-side exceptions will be returned in the error field of
     * the response(s) that can be read from the stream.
     *
     * This method is particularly useful in situations where the service is capable of sending multiple responses for a
     * single request, i.e. a method that supports streaming responses.
     *
     * Note that the caller is responsible for closing the reader.
     * If it's not closed then the underlying socket may be left open.
     *
     * @param request
     * @return a reader for the stream of responses.
     * @throws Exception
     */
    JsonReader dispatch(FullRequest request) throws Exception;

    /**
     * Dispatches a Narwhal request and gets back a JsonReader pointed at the result object.
     *
     * Note that the caller *must* close the JsonReader to enable the connection to the server to be closed (otherwise
     * we end up with sockets left open indefinitely).
     *
     * @param request
     * @param session
     * @return
     * @throws DispatchException
     * @throws IOException
     * @throws Exception
     */
    JsonReader dispatchForResultStream(Request request, Session session) throws DispatchException, IOException, Exception;

}
