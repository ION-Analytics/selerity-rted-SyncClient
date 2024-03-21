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

import com.google.gson.JsonElement;
import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedList;
import java.util.Queue;

public class SimpleStreamedResponse implements StreamedResponse {
    private static final Log LOG = LogFactory.getLog(SimpleStreamedResponse.class);

    public static final int DEFAULT_MAX_SIZE = 100;

    protected final Queue<Response> responseQueue;
    protected boolean hasMore = true;
    protected boolean wantsMore = true;
    protected final int maxSize;
    protected final String id;

    /**
     * Creates a new StreamedResponse instance with the given single response.
     *
     * @param response
     */
    public static StreamedResponse getSingleResponseInstance(Response response) {
        return getSingleResponseInstance(response, null);
    }

    /**
     * Creates a new StreamedResponse instance with the given single response and a given id.
     *
     * @param response
     */
    public static StreamedResponse getSingleResponseInstance(Response response, String id) {
        SimpleStreamedResponse sr;
        if (id == null || id.isEmpty()) {
            sr = new SimpleStreamedResponse(response.getID());
        } else {
            sr = new SimpleStreamedResponse(id);
        }
        if (response.hasMore()) {
            throw new IllegalArgumentException("cannot create a single response instance for a response which sets 'more' to true");
        }
        sr.add(response);
        return sr;
    }

    /**
     * Creates a new StreamedResponse instance with a single successful response using the given result.
     *
     * @param result
     * @param id
     * @param resultType
     */
    public final static StreamedResponse getSingleResponseInstance(JsonElement result, String id, String resultType) {
        return getSingleResponseInstance(new Response(result, id, "Selerity Streaming API", resultType, false));
    }

    /**
     * Creates a new StreamedResponse instance with a single error response using the given exception.
     *
     * @param id
     */
    public final static StreamedResponse getSingleResponseInstance(DispatchException error, String id) {
        return getSingleResponseInstance(new Response(error, id, "Selerity Streaming API", false));
    }

    /**
     * Creates a new StreamedResponse instance with a single error response constructed from the code, messagage and data fields.
     *
     * @param id
     */
    public final static StreamedResponse getSingleResponseInstance(int errorCode, String errorMessage, JsonElement errorData, String id) {
        return getSingleResponseInstance(new DispatchException(errorCode, errorMessage, errorData), id);
    }


    public SimpleStreamedResponse(String id) {
        this(id, DEFAULT_MAX_SIZE);
    }

    public SimpleStreamedResponse(String id, int maxSize) {
        this.id = id;
        this.maxSize = maxSize;
        this.responseQueue = new LinkedList<>();
    }

    /**
     * Gets the ID of the request to which this reponse corresponds.
     *
     * @return the ID of the request to which this reponse corresponds.
     */
    public String getID() {
        return id;
    }

    /**
     * Adds another response to this Streamed Response.
     * Sets the more flag to false if  this response's hasMore() method returns false.
     *
     * Note, this method may block if the response queue is full.
     *
     * @throws IllegalArgumentException if the 'more' flag is already set to false.
     * @param response
     */
    public synchronized void add(Response response) {
        add(response, true);  // note, discards the return value
    }

    /**
     * Adds another response to this Streamed Response.
     * Sets the more flag to false if this response's hasMore() method returns false.
     *
     * Note, this method may block if the response queue is full and block is set to true.
     * If the queue is full and block is false then it will discard the response.
     *
     * @throws IllegalArgumentException if the 'more' flag is already set to false.
     * @param response
     */
    public synchronized boolean add(Response response, boolean block) {
        //LOG.debug("adding to " + response.getID());
        while (block && wantsMore && hasMore && (responseQueue.size() >= maxSize)) {
            // the queue is full
            try {
                //LOG.debug("waiting to add to " + response.getID());
                wait();
            } catch (InterruptedException ix) {
                // ignore
            }
            //LOG.debug("done waiting to add to " + response.getID());
        }
        if (responseQueue.size() >= maxSize) {
            LOG.warn("discarding response because queue is full");
            return false;
        }
        if (!wantsMore) {
            LOG.info("discarding response because consumer doesn't want any more");
            return false;
        }
        if (!hasMore) {
            throw new IllegalArgumentException("cannot add a response after more has been set to false");
        }
        if (!response.hasMore()) {
            //LOG.debug("setting hasMore = false for " + response.getID());
            hasMore = false;
        }
        responseQueue.add(response);
        //LOG.debug("added to " + response.getID());
        notifyAll();
        //LOG.debug("notifying after adding to " + response.getID());
        return true;
    }


    /**
     * Returns true if there are responses pending (either already added or anticipated).
     *
     * Note that setting the 'more' flag to false does not make this method return false until the queue has been drained.
     */
    public synchronized boolean hasMore() {
        return hasMore || (!responseQueue.isEmpty());
    }

    /**
     * Returns true if this streamed response may be having additional responses added to it.
     * If false then no additional responses will be added to the queue but the queue may or may not be empty.
     */
    public boolean gettingMore() {
        return hasMore;
    }

    /**
     * Allows the producer to indicate that they intend to produce no more responses.
     * This does not immediately cause hasMore() to return false since there may still be some responses enqueued.
     */
    public synchronized void setHasNoMore() {
        //LOG.debug("setting has no more");
        hasMore = false;
        notifyAll();
    }

    /**
     * Returns true if the consumer wants to receive more responses.
     * If false then  the producer should not send any further responses (and can discard the object).
     */
    public synchronized boolean wantsMore() {
        return wantsMore;
    }

    /**
     * Allows the consumer to indicate that they don't want any more responses.
     * This immediately causes wantsMore() to return false.
     */
    public synchronized void setWantsNoMore() {
        //LOG.debug("setting wants no more");
        wantsMore = false;
        notifyAll();
    }


    /**
     * Blocks until the next response is available.  Returns null if no more responses are coming.
     * May block indefinitely if the writer doesn't set the 'more' flag to false.
     */
    public synchronized Response getNextResponse() {
        while (true) {
            //LOG.debug("in next response loop");
            if (!wantsMore) {
                LOG.error("wantsMore already set to false, cannot get next response (" + responseQueue.size() + " waiting in queue)");
                notifyAll();
                return null;
            }
            //LOG.debug("polling queue");
            Response response = responseQueue.poll();
            if (response != null) {
                //LOG.debug("returning response");
                notifyAll();
                return response;
            }
            //LOG.debug("polled a null");
            if (hasMore) {
                try {
                    //LOG.debug("waiting while we have more");
                    wait();
                } catch (InterruptedException ix) {
                    // ignore
                }
                //LOG.debug("done waiting after has more");
            } else {
                //LOG.debug("has no more and polled null so returning null");
                notifyAll();
                return null; // queue is no more available
            }
        }
    }

    /**
     * Blocks until the next response is available.  Returns null if no more responses are coming.
     * May time out and return null if the wait time is exceeeded.
     */
    public synchronized Response getNextResponse(long timeoutMillis) {
        long timeoutTime = System.currentTimeMillis() + timeoutMillis;
        while (timeoutTime > (System.currentTimeMillis())) {
            //LOG.debug("in next response loop (timed)");
            if (!wantsMore) {
                LOG.info("wantsMore already set to false, cannot get next response (" + responseQueue.size() + " waiting in queue)");
                notifyAll();
                return null;
            }
            //LOG.debug("polling queue (timed)");
            Response response = responseQueue.poll();
            if (response != null) {
                //LOG.debug("polling a value, returning (timed)");
                notifyAll();
                return response;
            }
            //LOG.debug("polling a null (timed)");
            if (hasMore) {
                try {
                    long waitTimeMillis = timeoutTime - System.currentTimeMillis();
                    //LOG.debug("waiting up to " + waitTimeMillis + " while we have more (timed)");
                    wait(waitTimeMillis);
                } catch (InterruptedException ix) {
                    // ignore
                }
                //LOG.debug("done waiting after has more (timed)");
            } else {
                //LOG.debug("has no more and polled null so returning null (timed)");
                notifyAll();
                return null; // queue is no more available
            }
        }
        //LOG.debug("time ran out so returning null (timed)");
        notifyAll();
        return null;
    }

}
