/*
 *  (C) Copyright Selerity, Inc. 2009-2012. All rights reserved. This source code
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Deprecated
public class AsyncPseudoHTTPTransportPoolFactory implements AsyncTransportFactory {

    private static final Log log = LogFactory.getLog(AsyncPseudoHTTPTransportPoolFactory.class);

    public static final String CONNECTION_POOL_CHECK_INTERVAL_MILLIS = "com.selerity.sync.client.async.connectionPool.checkInterval";
    public static final String CONNECTION_POOL_START_INTERVAL_MILLIS = "com.selerity.sync.client.async.connectionPool.startInterval";
    public static final String CONNECTION_POOL_ACTIVE_INTERVAL_MILLIS = "com.selerity.sync.client.async.connectionPool.activeInterval";
    public static final String CONNECTION_POOL_RETIREMENT_INTERVAL_MILLIS = "com.selerity.sync.client.async.connectionPool.retirementInterval";
    public static final String CONNECTION_POOL_MIN_SIZE = "com.selerity.sync.client.async.connectionPool.poolSize";

    protected final String httpAction;
    protected final String httpResource;
    protected final String host;
    protected final int port;
    protected final long checkIntervalMillis;
    protected final long startIntervalMillis;
    protected final int minPoolSize;
    protected final long activeIntervalMillis;
    protected final long retirementIntervalMillis;

    protected static long getDefaultedProperty(String propertyName, long defaultValue) {
        String s = System.getProperty(propertyName);
        if (s == null) {
            log.info("Optional property not set: " + propertyName + "; defaulting to " + defaultValue);
            return defaultValue;
        }
        return Long.parseLong(s);
    }

    public AsyncPseudoHTTPTransportPoolFactory(String httpAction, String httpResource, String host, int port) {
        this.httpAction = httpAction;
        this.httpResource = httpResource;
        this.host = host;
        this.port = port;
        this.checkIntervalMillis = getDefaultedProperty(CONNECTION_POOL_CHECK_INTERVAL_MILLIS, 5000);
        this.startIntervalMillis = getDefaultedProperty(CONNECTION_POOL_START_INTERVAL_MILLIS, 10000);
        this.activeIntervalMillis = getDefaultedProperty(CONNECTION_POOL_ACTIVE_INTERVAL_MILLIS, 60000);
        this.retirementIntervalMillis = getDefaultedProperty(CONNECTION_POOL_RETIREMENT_INTERVAL_MILLIS, 30000);
        this.minPoolSize = (int) getDefaultedProperty(CONNECTION_POOL_MIN_SIZE, 7);
    }

    public AsyncPseudoHTTPTransportPoolFactory(String host, int port) {
        this(AsyncPseudoHTTPTransport.DEFAULT_HTTP_ACTION, AsyncPseudoHTTPTransport.DEFAULT_HTTP_RESOURCE, host, port);
    }

    public AsyncPseudoHTTPTransportPoolFactory(String httpAction, String httpResource, String host, int port,
            long checkIntervalMillis, long startIntervalMillis, int minPoolSize, long activeIntervalMillis,
            long retirementIntervalMillis) {
        this.httpAction = httpAction;
        this.httpResource = httpResource;
        this.host = host;
        this.port = port;
        this.checkIntervalMillis = checkIntervalMillis;
        this.startIntervalMillis = startIntervalMillis;
        this.minPoolSize = minPoolSize;
        this.activeIntervalMillis = activeIntervalMillis;
        this.retirementIntervalMillis = retirementIntervalMillis;
    }

    public AsyncPseudoHTTPTransportPoolFactory(String host, int port, long checkIntervalMillis, long startIntervalMillis,
            int minPoolSize, long activeIntervalMillis, long retirementIntervalMillis) {

        this(AsyncPseudoHTTPTransport.DEFAULT_HTTP_ACTION, AsyncPseudoHTTPTransport.DEFAULT_HTTP_RESOURCE, host, port,
                checkIntervalMillis, startIntervalMillis, minPoolSize, activeIntervalMillis, retirementIntervalMillis);
    }

    public AsyncTransport getInstance() throws Exception {
        AsyncPseudoHTTPTransportPool transportPool = new AsyncPseudoHTTPTransportPool(httpAction, httpResource, host, port,
                minPoolSize, activeIntervalMillis, retirementIntervalMillis);
        transportPool.start(checkIntervalMillis, startIntervalMillis);
        if (log.isDebugEnabled()) {
            log.debug("started pool to " + host + ":" + port + " with min size " + minPoolSize + ", checking every "
                    + checkIntervalMillis + "ms, retiring transports with active period " + activeIntervalMillis
                    + " ms and with gaps of " + startIntervalMillis + " ms between starts");
        }
        return transportPool;
    }

}
