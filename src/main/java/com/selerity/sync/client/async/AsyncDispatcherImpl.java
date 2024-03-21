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
package com.selerity.sync.client.async;

import com.google.gson.JsonPrimitive;
import com.selerity.sync.client.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

public class AsyncDispatcherImpl implements AsyncDispatcher, AsyncTransportListener {

    private static final Log log = LogFactory.getLog(AsyncDispatcherImpl.class);

    protected final AsyncTransport transport;
    protected final Map<String, SimpleStreamedResponse> responses = new HashMap<String, SimpleStreamedResponse>();
    protected int nextID = 0;
    protected final long DEFAULT_SYNCHRONOUS_TIMEOUT_MILLIS = 60000;

    public AsyncDispatcherImpl(AsyncTransport transport) {
        this.transport = transport;
        transport.addAsyncTransportListener(this);
    }

    public synchronized StreamedResponse asyncDispatch(Request request, String user, String token, String client, String mode) {
        String id = getNextID(user, client);
        if (responses.containsKey(id)) {
            return SimpleStreamedResponse.getSingleResponseInstance(DispatchException.INTERNAL_ERROR,
                    "got duplicated id: \"" + id + "\"", null, id);
        }
        SimpleStreamedResponse response = new SimpleStreamedResponse(id);
        responses.put(id, response);

        FullRequest fullRequest = new FullRequest(request, user, token, client, mode, id);

        try {
            transport.asyncDispatch(fullRequest);
        } catch (DispatchException dx) {
            return SimpleStreamedResponse.getSingleResponseInstance(dx, id);
        } catch (Exception ex) {
            return SimpleStreamedResponse.getSingleResponseInstance(DispatchException.INTERNAL_ERROR, "caught " + ex
                    + " while sending request ", new JsonPrimitive(ex.toString()), id);
        }
        return response;
    }

    public StreamedResponse asyncDispatch(Request request, Session session) {
        return asyncDispatch(request, session.getHeaderParameter(RhinoSession.USER),
                session.getHeaderParameter(RhinoSession.TOKEN),
                session.getHeaderParameter(RhinoSession.CLIENT),
                session.getHeaderParameter(RhinoSession.MODE));
    }

    public Response syncDispatch(Request request, String user, String token, String client, String mode) {
        long startTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("starting synchronous dispatch for request to " + request.getMethod());
        }

        StreamedResponse sr = asyncDispatch(request, user, token, client, mode);
        Response response = sr.getNextResponse(DEFAULT_SYNCHRONOUS_TIMEOUT_MILLIS);
        if (response == null) {
            log.error("timed out on response to request for " + request.getMethod());
            return new Response(new DispatchException(DispatchException.TIMEOUT_ERROR, "exceeded response timeout"),
                    sr.getID(), "Selerity Streaming API", false);
        }

        if (log.isDebugEnabled()) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.debug("got synchronous response in " + elapsedTime + " ms for request to " + request.getMethod());
        }

        if (response.hasMore()) {
            log.error("response may not have been single - remaining responses will be discarded");
            sr.setWantsNoMore(); // discard subsequent
        }
        return response;
    }

    public Response syncDispatch(Request request, Session session) {
        return syncDispatch(request, session.getHeaderParameter(RhinoSession.USER),
                session.getHeaderParameter(RhinoSession.TOKEN),
                session.getHeaderParameter(RhinoSession.CLIENT),
                session.getHeaderParameter(RhinoSession.MODE));
    }

    public synchronized void onResponse(Response response) {
        String id = response.getID();
        if (id == null) {
            log.error("got response with null id, discarding!");
        } else {
            if (log.isDebugEnabled()) {
                log.debug("got response for id \"" + id + "\"");
            }
            SimpleStreamedResponse sr = responses.get(id);
            if (sr != null) {
                sr.add(response);
            } else {
                log.error("got no streamed response object for id \"" + id + "\"");
            }

            if (!response.hasMore()) {
                if (log.isDebugEnabled()) {
                    log.debug("got last response for id \"" + id + "\"; removing streamed response from map");
                }
                responses.remove(id);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("stream for id \"" + id + "\" still has more");
                }
            }
        }
    }

    /**
     * Returns an ID which is unique for this dispatcher.
     *
     * @param user
     * @param client
     */
    protected synchronized String getNextID(String user, String client) {
        int id = nextID++;
        return Integer.toString(id);
    }

}
