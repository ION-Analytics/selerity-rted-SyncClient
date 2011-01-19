package com.selerity.sync.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
 * A dispatcher which knows how to format JSON-RPC requests for Selerity's 'Rhino' server implementation.
 * 
 * @author andrewbrook
 *
 */
public class RhinoDispatcher implements Dispatcher{
	
	private static final Log log = LogFactory.getLog (RhinoDispatcher.class);
	
	private static final long DISPATCH_WARN_THRESHOLD = 2000;
	
	private final Gson gson = new GsonBuilder().serializeNulls().create();
	private final JsonParser parser = new JsonParser();
	
	protected final Transport transport;
	
	protected int nextID = 0;
	
	public RhinoDispatcher(Transport transport){
		this.transport = transport;
	}

	public synchronized JsonElement dispatch(Request request, String user, String token, String client, String mode) throws DispatchException{
		
		// construct the request string
		String id = getNextID(user, client);
		StringBuffer reqBuf = new StringBuffer();
		appendRequestString(reqBuf, request, id, user, token, client, mode);
		String requestString = reqBuf.toString();
		
		// now actually do the dispatching
		try{
			log.debug("about to dispatch: " + requestString);
			long startTime = System.currentTimeMillis();
			String responseString = transport.dispatch(requestString);
			long elapsedTime = System.currentTimeMillis() - startTime;
			if (elapsedTime < DISPATCH_WARN_THRESHOLD){
				log.debug("DISPATCHTIME: " + elapsedTime + " ms after " + request.getMethod() + "; got response: " + responseString);
			}
			else{
				log.warn("DISPATCHTIME: " + elapsedTime + " ms after " + request.getMethod() + "; got response: " + responseString);
			}
			
			// parse the response into the result, error and correlation ID
			JsonObject responseMap = parser.parse(responseString).getAsJsonObject();
			JsonElement result = responseMap.get("result");
			DispatchException dx = getException(responseMap);
			String returnedID = gson.fromJson(responseMap.get("id"), String.class);
			
			// if there's no error, this will be null.  if not null then there was an error
			if (dx != null){
				log.warn("got error from server, throwing exception " + dx, dx);
				throw dx;
			}
			
			// check that the correlation ID's match.  This shouldn't ever fail.
			if ((id != null) && (!id.equals(returnedID))){
				String errorMsg = "correlation ID's didn't match, sent \"" + id + "\", received \"" + returnedID + "\"";
				log.error(errorMsg);
				throw new DispatchException(DispatchException.INTERNAL_ERROR, errorMsg, returnedID);
			}
			
			// if we got here then we have a valid response
			return result;
			
		}
		catch (Exception ex){
			// wrap and rethrow
			DispatchException localEx = new DispatchException(DispatchException.INTERNAL_ERROR, "local exception", ex.getMessage(), ex);
			throw localEx;
		}
	}
	
	public JsonElement dispatch(Request request, Session session) throws DispatchException{
		return dispatch(request, session.getHeaderParameter(RhinoSession.USER), 
				session.getHeaderParameter(RhinoSession.TOKEN), 
				session.getHeaderParameter(RhinoSession.CLIENT), 
				session.getHeaderParameter(RhinoSession.MODE));
	}
	
	public Response[] boxcarDispatch(Request[] requests, String user, String token, String client, String mode) throws DispatchException {
		// keep track of which request is which
		Map<String,Integer> requestIndex = new HashMap<String,Integer>();
		
		// keep track of any requests which don't get a corresponding response
		Set<String> missing = new HashSet<String>();
		
		// grab the next request ID, we'll append suffixes to it for each request in the train
		String baseID = getNextID(user, client);
		
		// generate the request string (and array of requests)
		StringBuffer reqBuf = new StringBuffer();
		reqBuf.append('[');
		boolean first = true;
		int index = 0;
		for (Request request : requests){
			if (first){
				first = false;
			}
			else{
				reqBuf.append(',');
			}
			String id = baseID + "_" + index;  // generate a new request ID which is easy to debug 
			requestIndex.put(id, index);  // record this request's correlation ID
			missing.add(id);  // put it into the missing list - we'll remove it later when the response comes back
			appendRequestString(reqBuf, request, id, user, token, client, mode);
			index++;
		}
		reqBuf.append(']');
		String requestString = reqBuf.toString();
		
		// now actually do the dispatching and correlate the results

		log.debug("about to dispatch: " + requestString);
		String responseString = null;
		try{
			responseString = transport.dispatch(requestString);
		}
		catch (Exception ex){
			// wrap and rethrow
			DispatchException localEx = new DispatchException(DispatchException.OTHER_ERROR, "dispatch exception", ex.getMessage(), ex);
			throw localEx;
		}
		log.debug("got response: " + responseString);
		
		// the response should be an array
		JsonArray responseArray = parser.parse(responseString).getAsJsonArray();
		
		// be sure we got back the same number as we sent!
		if (responseArray.size() != requests.length){
			String errorMsg = "sent " + requests.length + " boxcarred requests but got " + responseArray.size() + " responses";
			throw new DispatchException(DispatchException.INTERNAL_ERROR, errorMsg, "got " + responseArray.size() + " responses");
		}
		
		Response[] responses = new Response[responseArray.size()];
		for (JsonElement responseElement : responseArray){
			
			// parse out the result, error and correlation ID
			JsonObject responseMap = responseElement.getAsJsonObject();
			JsonElement result = responseMap.get("result");
			DispatchException dx = getException(responseMap);
			String returnedID = gson.fromJson(responseMap.get("id"), String.class);
			
			// check the ID and get the index for it
			index = requestIndex.get(returnedID);
			log.debug("mapped id: " + returnedID + " to index: " + index);
			if (responses[index] != null){
				String errorMsg = "got duplicate response for request " + returnedID + " at index " + index;
				throw new DispatchException(DispatchException.INTERNAL_ERROR, errorMsg, "returned ID = " + returnedID);
			}
			else{
				// valid response, set the value in the array and mark it off the missing list
				responses[index] = new Response(result, dx);
				missing.remove(returnedID);
			}
			
		}
			
		// now check to see if anything's still missing
		if (missing.size() > 0){
			String errorMsg = "no responses for " + missing.size() + " out of " 
							+ requests.length + " requests including: " + MiscUtils.listSomeElements(missing, 5);
			throw new DispatchException(DispatchException.INTERNAL_ERROR, errorMsg, "missing count = " + missing.size());
		}
			
		// if not then everything is done, return the response array
		return responses;
	}
	
	public Response[] boxcarDispatch(Request[] requests, Session session) throws DispatchException {
		return boxcarDispatch(requests, session.getHeaderParameter(RhinoSession.USER), 
				session.getHeaderParameter(RhinoSession.TOKEN), 
				session.getHeaderParameter(RhinoSession.CLIENT), 
				session.getHeaderParameter(RhinoSession.MODE));
	}
	
	protected DispatchException getException(JsonObject responseMap){
		JsonElement errorElement = responseMap.get("error");
		if ((errorElement == null) || (errorElement.isJsonNull())){
			return null;
		}
		JsonObject errorObj = errorElement.getAsJsonObject();
		int code = MiscUtils.getInt(errorObj, "code", DispatchException.OTHER_ERROR);
		String message = MiscUtils.getString(errorObj, "message", null);
		String data = MiscUtils.getString(errorObj, "data", null);
		return new DispatchException(code, message, data);
	}
	
	
	protected void appendRequestString(StringBuffer buf, Request request, String id, String user, String token, String client, String mode){
		buf.append("{"
	    	+"\"method\":" + MiscUtils.stringEncode(request.getMethod()) + ","
	    	+"\"params\":" 
	    	+ gson.toJson(request.getMethodParameters())
	    	+ ","
	    	+"\"header\":{"
	    	+"\"user\":" + MiscUtils.stringEncode(user) + ","
	    	+"\"token\":" + MiscUtils.stringEncode(token) + ","
	    	+"\"client\":" + MiscUtils.stringEncode(client) + ","
	    	+"\"mode\":" + MiscUtils.stringEncode(mode) + ""
	    	+"},"
	    	+"\"id\":\"" + id + "\""
    		+"}");
	}
	
	/** Returns a (nearly) unique correlation ID based on the user, client, dispatcher creation time and a sequence number.  Useful
	 *  for debugging.
	 * 
	 * @param user
	 * @param client
	 * @return
	 */
	protected synchronized String getNextID(String user, String client){
		int id = nextID++;
		return Integer.toString(id);
	}

	
}
