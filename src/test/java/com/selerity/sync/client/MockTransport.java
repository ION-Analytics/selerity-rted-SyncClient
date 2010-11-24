package com.selerity.sync.client;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.selerity.sync.client.Transport;

/** Simple mock implementation of the Transport interface.  It can be set up with a list of pre-generated responses.  The responses will be delivered 
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
	
	public MockTransport(){}
	
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

	/** Log the request and return the next response from the queue, wrapped with the header from the request
	 * 
	 */
	public synchronized String dispatch(String jsonRequest) throws Exception{
		requests.add(jsonRequest);
		
		JsonObject requestObj = parser.parse(jsonRequest).getAsJsonObject();
		String id = requestObj.get("id").getAsString();
		
		String result = results.remove(0);
		String error = errors.remove(0);
		
		return "{\"result\":" + result + ",\"error\":" + error + ",\"header\":{},\"id\":" + id + "}";
		
	}
	
}
