/** 
 * (C) Copyright Selerity, Inc. 2009-2012. All rights reserved. This source code
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
 * An interface to an HTTP-based Narwhal service endpoint which can maintain a persistently active connection and open session.
 * 
 * Note that after the service is instantiated, one or more calls to startPinging() are needed to initiate the persistent session
 * maintenance mechanism.
 * 
 * Note also that each call to startPinging starts a new thread.
 * 
 */

package com.selerity.sync.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.selerity.sync.client.util.StatsLogger;

public class PersistentNarwhalHTTPService implements NarwhalService{

	private static final Log log = LogFactory.getLog(PersistentNarwhalHTTPService.class);
	
	private static final String DEFAULT_PING_METHOD_NAME = "PingHandler.ping";
	private static final long DEFAULT_PING_INTERVAL_MILLIS = 15000; // ping every 15 seconds
	
	
	private final NarwhalSessionFactory sessionFactory;
	private final NarwhalHTTPServiceImpl service;
	
	public PersistentNarwhalHTTPService(URL serviceURL, NarwhalSessionFactory sessionFactory) {
		this.service = new NarwhalHTTPServiceImpl(serviceURL);
		this.sessionFactory = sessionFactory;
    }

    public PersistentNarwhalHTTPService(String host, int port, String resource, NarwhalSessionFactory sessionFactory)
            throws MalformedURLException {
    	this.service = new NarwhalHTTPServiceImpl(new URL("http://" + host + ":" + port + "/" + resource));
    	this.sessionFactory = sessionFactory;
    }

    /**
     * Starts an infinite loop on a separate thread to ping the service using the default ping method (which takes no arguments) and interval.
     * 
     */
    protected void startPinging(){
    	startPinging(DEFAULT_PING_METHOD_NAME, null, DEFAULT_PING_INTERVAL_MILLIS);
    }
    
    /**
     * Starts an infinite loop on a separate thread to ping the service using the given ping method and interval.
     * 
     * Arguments can be provided to the ping method or the arguments can be left null to indicate no arguments.
     * 
     * @param pingMethodName
     * @param pingIntervalMillis
     */
    protected void startPinging(final String pingMethodName, final Map<String,Object> pingMethodArguments, final long pingIntervalMillis){
    	log.info("starting to ping " + service.getName() + " on method " + pingMethodName + " every " + pingIntervalMillis + " ms");
    	while(true){
		
    		// construct the request, setting arguments as needed
			Request request = new Request(pingMethodName);
			if (pingMethodArguments != null){
				// add the arguments
				for (String name : pingMethodArguments.keySet()){
					Object value = pingMethodArguments.get(name);
					if (value instanceof String){
						request.setMethodParameter(name, (String)value);
					}
					else if (value instanceof Number){
						request.setMethodParameter(name, (Number)value);
					}
					else if (value instanceof Boolean){
						request.setMethodParameter(name, (Boolean)value);
					}
					else if (value instanceof Character){
						request.setMethodParameter(name, (Character)value);
					}
					else if (value instanceof JsonElement){
						request.setMethodParameter(name, (JsonElement)value);
					}
					else{
						throw new IllegalArgumentException("cannot set parameter with name = " + name + " and value = " + value 
								+ " for method " + pingMethodName + " due to incompatibility with JSON types");
					}
				}
			}
    			
    		try{
    			log.debug("about to call " + pingMethodName + " on service " + service.getName());
    			dispatch(request, sessionFactory.getInstance());
    			log.debug("calling " + pingMethodName + " on service " + service.getName() + " was successful, session and connection are ok");
    		}
    		catch (Exception ex){
    			log.error("caught " + ex + " while pinging " + pingMethodName + " on service " + service.getName() + "; will log and continue...", ex);
    		}
    		
    		// now wait a while before trying again
    		try{
    			Thread.sleep(pingIntervalMillis);    			
    		}
    		catch (Exception ex2){
    			// ignore errors while sleeping
    		}
    	}
    }
	
    @Override
	public String getName(){
		return this.service.getName();
	}
	
	@Override
	public StatsLogger<String,Long> getMethodStatsLogger(){
		return this.service.getMethodStatsLogger();
	}
	
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
	public JsonElement dispatch(Request request, Session session) throws DispatchException{
		return this.service.dispatch(request, session);
	}
	
	/**
	 * Dispatches the request with a random id, filling in the header from the session information as appropriate.
	 * 
	 * Returns the response object regardless of whether it's an error or not.
	 * 
	 * Will only throw an exception if a local exception occurs.  Server-side exceptions will be returned as the error field
	 * of the response object.
	 * 
	 * Note that this method will *not* work correctly for streaming responses since it assumes a single response to each request.
	 * 
	 * @param request
	 * @param session
	 * @return the response
	 * @throws DispatchException
	 */
	public Response dispatchWithResponse(Request request, Session session) throws DispatchException{
		return this.service.dispatchWithResponse(request, session);
	}

    /**
	 * Dispatches the request with a given id, filling in the header from the session information as appropriate.
	 *
	 * Returns the response object regardless of whether it's an error or not.
	 *
	 * Will only throw an exception if a local exception occurs.  Server-side exceptions will be returned as the error field
	 * of the response object.
	 *
	 * Note that this method will *not* work correctly for streaming responses since it assumes a single response to each request.
	 *
	 * @param request
	 * @param session
     * @param id
	 * @return the response
	 * @throws DispatchException
	 */
    public Response dispatchWithResponse(Request request, Session session, String id) throws  DispatchException{
    	return this.service.dispatchWithResponse(request, session, id);
    }

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
     * Note that the caller is responsible for closing the reader.  If it's not closed then the underlying socket may
     * be left open.
     *
     * @param request
     * @return a reader for the stream of responses.
     * @throws Exception
     */
    public JsonReader dispatch(FullRequest request) throws Exception{
    	return this.service.dispatch(request);
    }
    
    
    /**
	 * Dispatches a Narwhal request and gets back a JsonReader pointed at the
	 * result object.
	 * 
	 * Note that the caller *must* close the JsonReader to enable the connection
	 * to the server to be closed (otherwise we end up with sockets left open
	 * indefinitely).
	 * 
	 * @param request
	 * @param session
	 * @return
	 * @throws DispatchException
	 * @throws IOException
	 * @throws Exception
	 */
	public JsonReader dispatchForResultStream(Request request, Session session) throws DispatchException, IOException,
			Exception {
		final FullRequest fullRequest = new FullRequest(request, session.getHeaderParameter("user"),
				session.getHeaderParameter("token"), session.getHeaderParameter("client"),
				session.getHeaderParameter("mode"), UUID.randomUUID().toString());
		final JsonReader responseStream = dispatch(fullRequest);
		return MiscUtils.extractResultStream(responseStream);
	}
    
    
    
    
    // useful for long-duration or manual integration testing.
	public static void main(String[] args){
		try{
			
			//String serviceURLs = "http://ny2aclsp01:8080/rhino-1.0-SNAPSHOT/rpc.do";
			String serviceURLs = "http://ny2aclsp01:8083/rpc.do";
			NarwhalSessionFactory factory = new NarwhalSessionFactory(new NarwhalHTTPServiceImpl(new URL(serviceURLs)), "NarwhalSessionFactoryTest");

			PersistentNarwhalHTTPService service = new PersistentNarwhalHTTPService(new URL("http://ny2aclsp01:48640/"), factory);
			
			service.startPinging();
		
			
			while (true){
				long mins = (long)(Math.random() * 60.0);  // pick a random number of minutes between 0 and 1 hour.
				log.info("now sleep for " + mins + " minutes and try again...");
				Thread.sleep(mins * 60000L);  
				log.info("woke up, will now try a ping test");
				Request request = new Request("PingHandler.ping");
				// no arguments
				
				log.info("pinging...");
				long start = System.nanoTime();
				JsonElement response = service.dispatch(request, factory.getInstance());
				double elapsedNanos = System.nanoTime() - start;
				double elapsedMillis = elapsedNanos / 1000000.0;
				log.info("got response in " + elapsedMillis + " ms");
				
				log.info("server response was: " + response.getAsString());
			}
			
		}
		catch (Exception ex){
			log.error("caught " + ex + " in main, exiting", ex);
		}
	}
    
    
}
