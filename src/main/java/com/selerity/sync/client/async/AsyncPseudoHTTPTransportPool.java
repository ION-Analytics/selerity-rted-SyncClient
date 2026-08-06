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

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.FullRequest;
import com.selerity.sync.client.Response;

//2026 we still use it
@Deprecated
public class AsyncPseudoHTTPTransportPool implements AsyncTransport, AsyncTransportListener, Runnable {
    private static final Log log = LogFactory.getLog(AsyncPseudoHTTPTransportPool.class);

    protected final AgedPool<AsyncPseudoHTTPTransport> transports;

    protected final String host;
    protected final int port;
    protected final int minPoolSize;

    protected final String httpAction;
    protected final String httpResource;

    protected int nextTransportNumber = 0;

    protected AsyncTransportListener listener = null;

    protected long checkIntervalMillis;
    protected long startIntervalMillis;

    protected long lastStartTimeMillis = 0;

    AsyncPseudoHTTPTransportPool(String httpAction, String httpResource, String host, int port, int minPoolSize,
                                 long activeIntervalMillis, long retirementIntervalMillis) {
        this.httpAction = httpAction;
        this.httpResource = httpResource;
        this.host = host;
        this.port = port;
        this.minPoolSize = minPoolSize;
        this.transports = new AgedPool<AsyncPseudoHTTPTransport>(activeIntervalMillis, retirementIntervalMillis);
    }

    public void start(long checkIntervalMillis, long startIntervalMillis) {
        this.checkIntervalMillis = checkIntervalMillis;
        this.startIntervalMillis = startIntervalMillis;

        startNewTransport(); // make sure there's at least one!

        // start the thread to do regular checking
        Thread th = new Thread(this, "transportHealthChecker");
        th.setDaemon(true);
        th.start();
        if (log.isDebugEnabled())
            log.debug("started - " + th);
    }

    @Override
    public synchronized void addAsyncTransportListener(AsyncTransportListener listener) {
        if (this.listener != null) {
            throw new IllegalArgumentException("cannot add a second listener to this transport");
        }
        this.listener = listener;
    }

    @Override
    public void asyncDispatch(FullRequest request) throws DispatchException {
        log.debug("getting next available transport instance...");
        AsyncPseudoHTTPTransport transport = transports.waitForNextActive(100);
        while (transport != null && !transport.isConnected()) {
            log.warn("got a disconnected transport " + transport + ", will not put back in pool");
            transport = transports.waitForNextActive(100);
        }

        if (log.isDebugEnabled())
            log.debug("got transport " + transport + " from pool");

        if (transport == null) {
            throw new DispatchException(DispatchException.INTERNAL_ERROR, "no available transports");
        }

        try {
            transport.asyncDispatch(request);
        } catch (DispatchException dx) {
            // fail this dispatch and take the transport out of the pool
            log.error("failed to dispatch due to " + dx + ", taking transport " + transport + " out of the pool");
            throw dx;
        }

        synchronized (transports) {
            if (log.isDebugEnabled())
                log.debug("adding " + transport + " back into the pool");

            transports.returnToActive(transport);
        }
    }

    @Override
    public void onResponse(Response response) {
        if (listener != null) {
            listener.onResponse(response);
        } else {
            log.error("no listener defined, discarding response to id \"" + response.getID() + "\"");
        }
    }

    @Override
    public void run() {
        while (true) {
            // first, check retirement ages of existing transports. Yes, this stops the world
            log.debug("checking ages of transports");
            Set<AsyncPseudoHTTPTransport> retired = transports.getRetired();
            for (AsyncPseudoHTTPTransport transport : retired) {
                transport.close();
            }
            if (log.isDebugEnabled())
                log.debug("retired " + retired.size() + " old transports");

            // now see if we need to start any new transports
            if (transports.getActiveCount() < 1) {
                log.warn("no active transports, need to start one immediately!");
                startNewTransport();
            } else if (transports.getActiveCount() < minPoolSize) {

                long elapsedSinceLastStart = System.currentTimeMillis() - lastStartTimeMillis;
                if (log.isDebugEnabled())
                    log.debug("currently have only " + transports.getActiveCount() + " transports," //
                            + " pool should have " + minPoolSize //
                            + ", " + elapsedSinceLastStart + " ms have passed since last start");

                if (elapsedSinceLastStart >= startIntervalMillis) {
                    if (log.isDebugEnabled())
                        log.debug("enough time has passed, starting another transport");

                    startNewTransport();
                } else {
                    if (log.isTraceEnabled())
                        log.trace("only " + elapsedSinceLastStart + " ms have passed since last start, need to wait a while");
                }
            }

            try {
                Thread.sleep(checkIntervalMillis);
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    protected void startNewTransport() {
        int transportNumber = getNextTransportNumber();
        AsyncPseudoHTTPTransport transport = new AsyncPseudoHTTPTransport(httpAction, httpResource, host, port, //
                host + ":" + port + ":" + transportNumber);
        transport.addAsyncTransportListener(this);
        try {
            transport.start();
            transports.addNew(transport);
            lastStartTimeMillis = System.currentTimeMillis();

            if (log.isDebugEnabled())
                log.debug("transport " + transport + " started");
        } catch (Exception ex) {
            log.error("caught " + ex + " while trying to start transport " + transport + "; giving up");
            transport.close();
        }
    }

    protected synchronized int getNextTransportNumber() {
        return nextTransportNumber++;
    }

}
