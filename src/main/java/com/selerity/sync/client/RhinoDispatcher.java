package com.selerity.sync.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

/** 
 * © Copyrights Selerity, Inc. 2009-2011. All rights reserved. This source code is confidential 
 * and proprietary information of Selerity Inc. and may be used only by a recipient designated by 
 * and for the purposes permitted by Selerity Inc. in writing.  Reproduction of, dissemination of, 
 * modifications to or creation of derivative works from this source code, whether in source or 
 * binary forms, by any means and in any form or manner, is expressly prohibited, except with the 
 * prior written permission of Selerity Inc..  THIS CODE AND INFORMATION ARE PROVIDED ÒAS ISÓ 
 * WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE. This notice may not be 
 * removed from the software by any user thereof. 
 * 
 * A dispatcher which knows how to format Narwhal requests for Selerity's 'Rhino' server implementation and runs on
 * a synchronous transport.
 * 
 *
 */
public class RhinoDispatcher implements Dispatcher{
	
	private static final Log log = LogFactory.getLog (RhinoDispatcher.class);
	
	private static final long DISPATCH_WARN_THRESHOLD_MILLIS = 10000;
	
	private final Gson gson;
	
	protected final Transport transport;  // yes, this is just a wrapper of an asynchronous dispatcher.
	
	protected int nextID = 0;
	
	public RhinoDispatcher(Transport transport){
		this.transport = transport;
		GsonBuilder builder = new GsonBuilder().serializeNulls();
		builder.registerTypeAdapter(FullRequest.class, new FullRequest.FullRequestDeserializer());
		builder.registerTypeAdapter(FullRequest.class, new FullRequest.FullRequestSerializer());
		builder.registerTypeAdapter(Response.class, new Response.ResponseDeserializer());
		builder.registerTypeAdapter(Response.class, new Response.ResponseSerializer());
		gson = builder.create();
	}

	/** Dispatch the request, returning the result (and throwing an error if one occurs).  Uses the explicit session
	 *  parameters given.
	 *  
	 *  Assumes a single response.
	 * 
	 */
	public JsonElement dispatch(Request request, String user, String token, String client, String mode) throws DispatchException{
		long startTime = System.currentTimeMillis();
		String id = getNextID();
		FullRequest fullRequest = new FullRequest(request, user, token, client, mode, id);
		Response response = transport.syncDispatch(fullRequest);
		long elapsedTime = System.currentTimeMillis() - startTime;
		if (elapsedTime < DISPATCH_WARN_THRESHOLD_MILLIS){
			if (log.isDebugEnabled()){
				log.debug("DISPATCHTIME: " + elapsedTime + " ms after " + request.getMethod() + "; got response: " + gson.toJson(response, Response.class));
			}
		}
		else{
			if (log.isWarnEnabled()){
				log.warn("DISPATCHTIME: " + elapsedTime + " ms after " + request.getMethod() + "; got response: " + gson.toJson(response, Response.class));
			}
		}
		
		if (response.isError()){
			throw response.getError();
		}
		return response.getResult();
		
	}
	
	
	/** Dispatches a request using the explicit session information, returning the full response object.
	 * 
	 */
	public synchronized Response dispatchWithResponse(Request request, String user, String token, String client, String mode) throws DispatchException {
		String id = getNextID();
		FullRequest fullRequest = new FullRequest(request, user, token, client, mode, id);
		Response response = transport.syncDispatch(fullRequest);
		return response;
	}
	
	
	
	/** Dispatch the request, returning the result (and throwing an error if one occurs).  Uses the cached session parameters.
	 * 
	 */
	public JsonElement dispatch(Request request, Session session) throws DispatchException{
		return dispatch(request, session.getHeaderParameter(RhinoSession.USER), 
				session.getHeaderParameter(RhinoSession.TOKEN), 
				session.getHeaderParameter(RhinoSession.CLIENT), 
				session.getHeaderParameter(RhinoSession.MODE));
	}
	
	
	
	/** Dispatch the array of requests as a boxcar, returning the corresponding array of responses.  Uses the explicit session
	 *  parameters given.
	 * 
	 */
	public Response[] boxcarDispatch(Request[] requests, String user, String token, String client, String mode) throws DispatchException {
		int requestCount = requests.length;
		
		// temporary objects to hold the full requests (the partials plus their ID's)
		FullRequest[] fullRequests = new FullRequest[requestCount];
		
		String baseID = getNextID();
		for (int i = 0; i < requestCount; i++){
			String id = baseID + "_" + i;
			log.debug("setting ID for sub-request " + i + " of " + requestCount + " as " + id);
			fullRequests[i] = new FullRequest(requests[i], user, token, client, mode, id);
		}
		
		// hand off to the transport layer
		return transport.boxcarDispatch(fullRequests);
	}
	
	/** Dispatch the array of requests as a boxcar.  Uses the cached session parameters.
	 * 
	 */
	public Response[] boxcarDispatch(Request[] requests, Session session) throws DispatchException {
		return boxcarDispatch(requests, session.getHeaderParameter(RhinoSession.USER), 
				session.getHeaderParameter(RhinoSession.TOKEN), 
				session.getHeaderParameter(RhinoSession.CLIENT), 
				session.getHeaderParameter(RhinoSession.MODE));
	}
	
	
	protected synchronized String getNextID(){
		int id = nextID++;
		return Integer.toString(id);
	}
	
}
