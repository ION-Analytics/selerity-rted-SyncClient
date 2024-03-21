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
package com.selerity.sync.client.examples.refdata;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.selerity.sync.client.NarwhalMetaServiceImpl;
import com.selerity.sync.client.NarwhalService;

/**
 * Implements a factory for producting ObservationMetaDataCache instances.
 *
 * Note that the instances returned are threadsafe but not especially high performance.
 */
public class ObservationMetaDataCacheFactory {
    private static final Log log = LogFactory.getLog(ObservationMetaDataCacheFactory.class);

    protected final ObservationMetaDataCacheImpl instance;

    protected final static long DEFAULT_CACHE_TIMEOUT = 30;
    protected final static TimeUnit DEFAULT_CACHE_TIMEOUT_TIME_UNIT = TimeUnit.MINUTES;

    /**
     * Constructs a factory that uses the default timeout.
     *
     * @param service
     * @param user
     * @param password
     * @param clientAppName
     * @throws Exception
     */
    public ObservationMetaDataCacheFactory(NarwhalService service, String user,
                                           String password, String clientAppName) throws Exception {
        this(service, user, password, clientAppName, DEFAULT_CACHE_TIMEOUT, DEFAULT_CACHE_TIMEOUT_TIME_UNIT);
    }

    /**
     * Constructs a factory backed by the given SeleritySync API service and using the given timeout.
     *
     * @param service
     * @param user
     * @param password
     * @param clientAppName
     * @param cacheTimeout
     * @param cacheTimeoutTimeUnit
     * @throws Exception
     */
    public ObservationMetaDataCacheFactory(NarwhalService service, String user,
                                           String password, String clientAppName, long cacheTimeout, TimeUnit cacheTimeoutTimeUnit) throws Exception {
        TagCacheFactory tagCacheFactory = new TagCacheFactory(service, user, password, clientAppName);
        tagCacheFactory.startRefresh(cacheTimeout, cacheTimeoutTimeUnit);
        ObservationMetaDataLoader metaDataLoader = new ObservationMetaDataLoader(service, user, password, clientAppName, tagCacheFactory);
        instance = new ObservationMetaDataCacheImpl(metaDataLoader, cacheTimeout, cacheTimeoutTimeUnit);
    }

    /**
     * Returns an ObservationMetaDataCache instance.
     */
    public ObservationMetaDataCache getInstance() {
        return instance;
    }


    /**
     * Purely for integration testing purposes.
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                System.err.println("arguments: urls user password");
                System.exit(1);
            }

            // read in the arguments
            String urls = args[0];
            String user = args[1];
            String password = args[2];

            NarwhalService service = new NarwhalMetaServiceImpl(urls);
            ObservationMetaDataCacheFactory factory =
                    new ObservationMetaDataCacheFactory(service, user, password, "ObservationMetaDataCacheFactoryTest", 20, TimeUnit.SECONDS);

            while (true) {
                long startNanos = System.nanoTime();
                ObservationMetaDataCache metaCache = factory.getInstance();
                ObservationMetaData obs1 = metaCache.getObservationMetaDataForSwordfishID(238290);
                long elapsedNanos = System.nanoTime() - startNanos;
                log.debug("elapsed lookup time is: " + (elapsedNanos / 1000) + " microseconds");
                if (obs1 != null) {
                    log.debug("got obs " + obs1.toString());
                }

                Thread.sleep(15000);
            }

        } catch (Exception ex) {
            log.error("caught exception " + ex + " in main loop, exiting", ex);
        }
    }

}
