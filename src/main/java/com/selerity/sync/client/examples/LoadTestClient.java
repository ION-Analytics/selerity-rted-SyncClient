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
package com.selerity.sync.client.examples;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.stream.JsonReader;
import com.selerity.sync.client.NarwhalMetaServiceImpl;
import com.selerity.sync.client.NarwhalService;
import com.selerity.sync.client.Request;
import com.selerity.sync.client.SessionImpl;

/**
 * A simple app that applies load to the IntrospectionHandler.getVersion method.
 */
public class LoadTestClient {
    private static final Log log = LogFactory.getLog(LoadTestClient.class);

    protected static void startTestingThread(final NarwhalService service, final int requestCount) {
        Thread th = new Thread(() -> {
            log.debug("starting...");

            for (int i = 0; i < requestCount; i++) {
                try {
                    Request request = new Request("IntrospectionHandler.getVersion");
                    SessionImpl session = new SessionImpl();
                    // log.debug("about to send request: " + request.getMethod());
                    service.dispatch(request, session);
                    // String version = resultElem.getAsString();
                    // log.debug("got version " + version);
                    // Thread.sleep(1);
                } catch (Exception ex) {
                    log.error("caught " + ex + " in testing thread, skipping", ex);
                }
            }

            log.debug("done sending, sleeping");
        });
        th.start();
    }


    public static void main(String[] args) {
        Set<JsonReader> readers = new HashSet<>();
        try {
            if (args.length < 3) {
                System.err.println("arguments: urls threadCount requestCount");
                System.exit(1);
            }

            // read in the arguments
            String urls = args[0];
            int threadCount = Integer.parseInt(args[1]);
            int requestCount = Integer.parseInt(args[2]);

            NarwhalService service = new NarwhalMetaServiceImpl(urls);

            for (int i = 1; i <= threadCount; i++) {
                log.debug("starting thread " + i + " of  " + threadCount);
                startTestingThread(service, requestCount);
            }
            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception ex) {
            log.error("caught exception " + ex + " in main loop, exiting", ex);
        }

        // this allows GC
        readers.clear();

        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception ex2) {
            log.error("caught exception " + ex2 + " in main loop, exiting", ex2);
        }
    }

}
