package com.selerity.sync.client;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/** 
 * (C) Copyright Selerity, Inc. 2009-2011. All rights reserved. This source code
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
 * 
 * Implements an interface to a Narwhal-based service over HTTP.
 *
 */

public class NarwhalHTTPServiceImpl extends AbstractNawhalServiceImpl{

	private static final Log log = LogFactory.getLog (NarwhalHTTPServiceImpl.class);
	
	private static final int CONNECTION_TIMEOUT_MILLIS = 30000; 
	private static final int READ_TIMEOUT_MILLIS = 30000; 
	
	protected final URL serviceURL;
	
	
	/** Creates transport that will POST JSON-RPC requests to the given URL. 
	 * 
	 * @param serviceURLString
	 * @throws MalformedURLException
	 */
	public NarwhalHTTPServiceImpl(URL serviceURL){
		super(serviceURL.toString());
		this.serviceURL = serviceURL;
		log.info("connecting to URL: " + serviceURL);
	}
	
	public NarwhalHTTPServiceImpl(String host, int port, String resource) throws MalformedURLException{
		this(new URL("http://" + host + ":" + port +"/" + resource));
	}
	
	public JsonElement dispatch(final Request request, final Session session) throws DispatchException{
		final Response response = dispatchWithResponse(request, session);
		if (response.getError() != null){
			throw response.getError();
		}
		return response.getResult();
	}
	
	public Response dispatchWithResponse(final Request request, final Session session) throws DispatchException{
				
		final long startTimeMillis = System.currentTimeMillis();
		JsonReader reader = null;
		try{
			FullRequest fullRequest = new FullRequest(request, 
					session.getHeaderParameter("user"), session.getHeaderParameter("token"), 
					session.getHeaderParameter("client"), session.getHeaderParameter("mode"),
					UUID.randomUUID().toString());
			
			reader = dispatch(fullRequest);
			Response response = gson.fromJson(reader, Response.class);
			final long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
			if (elapsedMillis > WARN_DISPATCH_TIME_MILLIS){
				log.warn("got delayed response to " + request.getMethod() + " in " + elapsedMillis + " ms from service " + serviceName);
			}
			else{
				if (log.isDebugEnabled()){
					log.debug("got response to " + request.getMethod() + " in " + elapsedMillis + " ms from service " + serviceName);
				}
			}
			return response;
		}
		catch (DispatchException dx){
			throw dx;
		}
		catch (Exception ex){
			log.error("caught " + ex + " while dispatching to service " + serviceName, ex);
			throw new DispatchException(DispatchException.INTERNAL_ERROR, "caught " + ex + " while dispatching to service " + serviceName, ex.toString());
		}
		finally{
			if (reader != null){
				try{
					// this is important - without it, the socket sometimes gets left open indefinitely.
					reader.close();
				}
				catch (Exception ex){
					// ignore the exception
				}
			}
		}
	}
	
	/**
     * Dispatches a request to Narwhal server using a JSON request string and returns the response.
     * 
     * Note that it is the responsibility of the caller to close the reader when finished reading - otherwise 
     * a connection to the server may remain open indefinitely.
     *
     * @param request request to dispatch
     * @param url URL of RPC server
     * @return response based on the request
     */
    public JsonReader dispatch(FullRequest request) throws Exception {
		// record start time of dispatch
    	if (log.isDebugEnabled()){
        	log.debug("preparing to send " + gson.toJson(request, FullRequest.class) + " to " + serviceURL);
        }
    	
    	URLConnection con = null;
    	synchronized(serviceURL){  // it's not clear if URL.openConnection or URLConnection.connect are threadsafe so just to be sure...
    		con = serviceURL.openConnection();
    		con.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
            con.setReadTimeout(READ_TIMEOUT_MILLIS);
        	con.addRequestProperty("Accept","text/plain");
        	con.addRequestProperty("Content-type", "application/x-json");
        	con.addRequestProperty("User-Agent","Java/NarwhalClient");
        	con.setDoOutput(true);
    		con.connect();
    	}
    	
    	
		// write the JSON request out
		
		JsonWriter writer = new JsonWriter(new OutputStreamWriter(con.getOutputStream()));
		gson.toJson(request, FullRequest.class, writer);
		writer.flush();
		writer.close();
		
		// read in a JSON reader
		return new JsonReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
    }

	
	
	
}
