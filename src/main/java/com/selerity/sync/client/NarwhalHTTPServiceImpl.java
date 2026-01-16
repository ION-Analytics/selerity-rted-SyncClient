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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.selerity.sync.client.util.StatsLogger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

public class NarwhalHTTPServiceImpl extends AbstractNarwhalServiceImpl {
    private static final Log log = LogFactory.getLog(NarwhalHTTPServiceImpl.class);

    private static final int CONNECTION_TIMEOUT_MILLIS;
    private static final int READ_TIMEOUT_MILLIS;

    static {
        CONNECTION_TIMEOUT_MILLIS = getProperty("com.selerity.sync.connection_timeout", (int) TimeUnit.SECONDS.toMillis(90));
        READ_TIMEOUT_MILLIS = getProperty("com.selerity.sync.read_timeout", (int) TimeUnit.HOURS.toMillis(3));
    }

    private static int getProperty(String propertyName, int default0) {
        try {
            String s = System.getProperty(propertyName);
            if (s != null && s.length() > 0) {
                return Integer.parseInt(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return default0;
    }

    private static URL URL_PREV;

    // //

    protected final URL serviceURL;

    /**
     * Creates transport that will POST JSON-RPC requests to the given URL.
     */
    public NarwhalHTTPServiceImpl(URL serviceURL) {
        super(serviceURL.toString());
        this.serviceURL = serviceURL;

        if (serviceURL != null && !serviceURL.equals(URL_PREV)) {
            URL_PREV = serviceURL;
            log.info("connecting to URL: " + serviceURL);
        } else {
            log.debug("connecting to URL: " + serviceURL);
        }
    }

    /**
     * Creates transport that will POST JSON-RPC requests to the given URL.
     * <p>
     * Uses the given method statistics logger.  If methodStatsLogger is null then
     * statistics logging is disabled.
     */
    public NarwhalHTTPServiceImpl(URL serviceURL, StatsLogger<String, Long> methodStatsLogger) {
        super(serviceURL.toString(), methodStatsLogger);
        this.serviceURL = serviceURL;

        if (serviceURL != null && !serviceURL.equals(URL_PREV)) {
            URL_PREV = serviceURL;
            log.info("connecting to URL: " + serviceURL);
        } else {
            log.debug("connecting to URL: " + serviceURL);
        }
    }

    /**
     * Creates transport that will POST JSON-RPC requests to the given resource on the given host and port.
     */
    public NarwhalHTTPServiceImpl(String host, int port, String resource) throws MalformedURLException {
        this(new URL("http://" + host + ":" + port + "/" + resource));
    }

    /**
     * Creates transport that will POST JSON-RPC requests to the given resource on the given host and port.
     * <p>
     * Uses the given method statistics logger.  If methodStatsLogger is null then
     * statistics logging is disabled.
     */
    public NarwhalHTTPServiceImpl(String host, int port, String resource, StatsLogger<String, Long> methodStatsLogger)
            throws MalformedURLException {
        this(new URL("http://" + host + ":" + port + "/" + resource), methodStatsLogger);
    }

    public JsonElement dispatch(final Request request, final Session session) throws DispatchException {
        final Response response = dispatchWithResponse(request, session);
        if (response.getError() != null) {
            throw response.getError();
        }
        return response.getResult();
    }

    public Response dispatchWithResponse(final Request request, final Session session) throws DispatchException {
        return dispatchWithResponse(request, session, null);
    }

    public synchronized Response dispatchWithResponse(final Request request,
                                                      final Session session, final String id) throws DispatchException {

        JsonReader reader = null;
        try {
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
            log.error("caught " + ex + " while dispatching to service " + serviceName, ex);
            throw new DispatchException(DispatchException.INTERNAL_ERROR,
                    "caught " + ex + " while dispatching to service " + serviceName, ex.toString());
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

    /**
     * Dispatches a request to Narwhal server using a JSON request string and returns the response.
     * <p/>
     * Note that it is the responsibility of the caller to close the reader when
     * finished reading - otherwise a connection to the server may remain open indefinitely.
     *
     * @param request request to dispatch
     * @return response based on the request
     */
    public synchronized JsonReader dispatch(FullRequest request) throws Exception {

        final long startNanos = MiscUtils.getNanoTime();

        // record start time of dispatch
        if (log.isDebugEnabled()) {
            JsonElement password = null;
            JsonObject params = null;

            if (request.params.isJsonObject()) {
                params = request.params.getAsJsonObject();

                if (params.has("password")) {
                    password = params.get("password");
                    params.addProperty("password", "XXX");
                }
            }

            log.debug("preparing to send " + gson.toJson(request, FullRequest.class) + " to " + serviceURL);

            if (request.params.isJsonObject()) {
                if (params.has("password")) {
                    params.add("password", password);
                }
            }
        }

        URLConnection con;

        con = serviceURL.openConnection();
        con.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
        con.setReadTimeout(READ_TIMEOUT_MILLIS);
        con.addRequestProperty("Accept", "text/plain");
        con.addRequestProperty("Content-type", "application/x-json");
        con.addRequestProperty("User-Agent", "Java/NarwhalClient");
        con.addRequestProperty("Connection", "Keep-Alive");
        con.addRequestProperty("Accept-Encoding", "gzip");
        con.addRequestProperty("X-Enable-Gzip", "gzip");
        con.setDoOutput(true);
        con.connect();

        // write the JSON request out

        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(con.getOutputStream()))) {
            gson.toJson(request, FullRequest.class, writer);
        }

        InputStream is = con.getInputStream();
        is = new BufferedInputStream(is, 32 * 1024);

        String respEncoding = con.getHeaderField("Content-Encoding");
        if (respEncoding != null && respEncoding.toLowerCase().contains("gzip")) {
            is = new GZIPInputStream(is, 32 * 1024);
        }
        JsonReader responseReader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        boolean logResponseHeaders = false;
        if (logResponseHeaders) {
            Map<String, List<String>> headers = con.getHeaderFields();

            System.out.println("Response Headers:");
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                List<String> headerValues = entry.getValue();

                if (headerName == null) {
                    System.out.println("Status-Line: " + headerValues.get(0));
                } else {
                    System.out.println(headerName + ": " + String.join(", ", headerValues));
                }
            }
        }

        final long elapsedNanos = MiscUtils.getNanoTime() - startNanos;
        final long elapsedMillis = (elapsedNanos / MiscUtils.NANOS_PER_MILLISECOND);

        if (elapsedNanos > WARN_DISPATCH_TIME_MILLIS * 1000000) {
            log.warn("got delayed response to " + request.getMethod() + " in " + elapsedMillis + " ms from service "
                    + serviceName);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("got response to " + request.getMethod() + " in " + elapsedMillis + " ms from service "
                        + serviceName);
            }
        }

        if (methodStatsLogger != null) {
            methodStatsLogger.addResult(request.getMethod(), elapsedNanos);
        }

        // read in a JSON reader
        return responseReader;
    }

}
