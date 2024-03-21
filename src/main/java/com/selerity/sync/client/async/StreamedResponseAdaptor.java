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

import com.selerity.sync.client.Response;

/**
 * An adapter which reads responses from a streamed response and calls the response listener.
 */
public class StreamedResponseAdaptor implements Runnable {

    protected final StreamedResponseListener responseListener;
    protected final StreamedResponse streamedResponse;
    protected final long responseTimeoutMillis;

    public StreamedResponseAdaptor(StreamedResponseListener responseListener,
                                   StreamedResponse streamedResponse,
                                   long responseTimeoutMillis) {
        this.responseListener = responseListener;
        this.streamedResponse = streamedResponse;
        this.responseTimeoutMillis = responseTimeoutMillis;
    }

    public StreamedResponseAdaptor(StreamedResponseListener responseListener,
                                   StreamedResponse streamedResponse) {
        this.responseListener = responseListener;
        this.streamedResponse = streamedResponse;
        this.responseTimeoutMillis = 0;
    }

    @Override
    public void run() {
        while (streamedResponse.hasMore()) {
            if (responseTimeoutMillis > 0) {
                Response response = streamedResponse.getNextResponse(responseTimeoutMillis);
                if (response == null) {
                    return;
                }
                responseListener.onResponse(response);
            } else {
                responseListener.onResponse(streamedResponse.getNextResponse());
            }
        }
    }

}
