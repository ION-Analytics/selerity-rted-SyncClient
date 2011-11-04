package com.selerity.sync.client;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
 * 
 *  Simple mock implementation of the Transport interface.  It can be set up with a list of pre-generated responses.  The responses will be delivered 
 *  when dispatch() is called.  The requests can also be queried.
 * 
 * @author andrewbrook
 *
 */
public class MockTransport implements Transport{
	
	protected List<String> requests = new ArrayList<String>();
	protected List<String> results = new LinkedList<String>();
	protected List<String> errors = new LinkedList<String>();
	
	protected JsonParser parser = new JsonParser();
	
	protected final Gson gson;

	protected final JsonParser jsonParser = new JsonParser();
	
	public MockTransport(){
		GsonBuilder builder = new GsonBuilder().serializeNulls();
		builder.registerTypeAdapter(FullRequest.class, new FullRequest.FullRequestDeserializer());
		builder.registerTypeAdapter(FullRequest.class, new FullRequest.FullRequestSerializer());
		builder.registerTypeAdapter(Response.class, new Response.ResponseDeserializer());
		builder.registerTypeAdapter(Response.class, new Response.ResponseSerializer());
		gson = builder.create();
	}
	
	/** Adds a response to the response queue.
	 * 
	 * @param response
	 */
	public synchronized void addResponse(String result, String error){
		results.add(result);
		errors.add(error);
	}
	
	/** How many requests has this Transport received?
	 * 
	 * @return
	 */
	public synchronized int getRequestCount(){
		return requests.size();
	}
	
	/** Tells how many responses are waiting to be returned
	 * 
	 * @return
	 */
	public synchronized int getResponsesRemaining(){
		return results.size();
	}
	
	/** Returns the request at the given index.
	 * 
	 * @param index
	 * @return
	 */
	public synchronized String getRequest(int index){
		return requests.get(index);
	}
	
	public Response syncDispatch(FullRequest request) throws DispatchException {
		String requestStr = gson.toJson(request, FullRequest.class);
		String responseString = dispatch(requestStr);
		return gson.fromJson(responseString, Response.class);
	}

	/** Log the request and return the next response from the queue, wrapped with the header from the request
	 * 
	 */
	public synchronized String dispatch(String jsonRequest) throws DispatchException{
		requests.add(jsonRequest);
		
		JsonObject requestObj = parser.parse(jsonRequest).getAsJsonObject();
		String id = requestObj.get("id").getAsString();
		
		String result = results.remove(0);
		String error = errors.remove(0);
		
		return "{\"result\":" + result + ",\"error\":" + error + ",\"header\":{},\"id\":" + id + "}";
		
	}
	
	public Response[] boxcarDispatch(FullRequest[] requests) throws DispatchException{
		throw new DispatchException(DispatchException.INTERNAL_ERROR, "not implemented");
	}
	
}
