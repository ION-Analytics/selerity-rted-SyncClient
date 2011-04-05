package com.selerity.sync.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
 * The transport used by Selerity's 'Rhino' server implementation exposed via HTTP (running in a Glassfish container).  Rhino 
 * supports both URL encoding or the application/x-json content type.  It does not (currently) support compression.  
 * 
 * @author andrewbrook
 *
 */
public class RhinoHTTPTransport implements Transport{
	
	private static final Log log = LogFactory.getLog (RhinoHTTPTransport.class);
	
	protected final URL serviceURL;
	protected final boolean enableURLEncoding;
	
	protected final Gson gson;
	protected final JsonParser jsonParser = new JsonParser();
	
	/** Creates transport that will POST JSON-RPC requests to the given URL.  Defaults to enableURLEncoding = false;
	 * 
	 * @param serviceURLString
	 * @throws MalformedURLException
	 */
	public RhinoHTTPTransport(String serviceURLString) throws MalformedURLException{
		this(new URL(serviceURLString), false);
	}
	
	/** Creates transport that will POST JSON-RPC requests to the given URL.  If enableURLEncoding is true then 
	 *  the request will be URL encoded, otherwise it will be sent as plain text.
	 * 
	 * @param serviceURLString
	 * @param enableURLEncoding
	 * @throws MalformedURLException
	 */
	public RhinoHTTPTransport(String serviceURLString, boolean enableURLEncoding) throws MalformedURLException{
		this(new URL(serviceURLString), enableURLEncoding);
	}
	
	/** Creates transport that will POST JSON-RPC requests to the given URL. Defaults to enableURLEncoding = false;
	 * 
	 * @param serviceURLString
	 * @param enableURLEncoding
	 * @throws MalformedURLException
	 */
	public RhinoHTTPTransport(URL serviceURL){
		this(serviceURL, false);
	}
	
	public RhinoHTTPTransport(URL serviceURL, boolean enableURLEncoding){
		this.serviceURL = serviceURL;
		this.enableURLEncoding = enableURLEncoding;
		
		GsonBuilder builder = new GsonBuilder().serializeNulls();
		builder.registerTypeAdapter(FullRequest.class, new FullRequest.FullRequestDeserializer());
		builder.registerTypeAdapter(FullRequest.class, new FullRequest.FullRequestSerializer());
		builder.registerTypeAdapter(Response.class, new Response.ResponseDeserializer());
		builder.registerTypeAdapter(Response.class, new Response.ResponseSerializer());
		gson = builder.create();
		
		log.info("connecting to URL: " + serviceURL + "; enableURLEncoding = " + enableURLEncoding);
	}
	
	
	public Response syncDispatch(FullRequest request) throws DispatchException{
		try{
			String jsonRequest = gson.toJson(request, FullRequest.class);
			String responseString = dispatch(jsonRequest);
			Response response = gson.fromJson(responseString, Response.class);
			return response;
		}
		catch (DispatchException dx){
			throw dx;
		}
		catch (Exception ex){
			throw new DispatchException(DispatchException.INTERNAL_ERROR, "caught " + ex + " while dispatching", ex.toString());
		}
	}
	
	public Response[] boxcarDispatch(FullRequest[] requests) throws DispatchException{
		StringBuffer requestBuf = new StringBuffer();
		requestBuf.append('['); // start the array
		boolean first = true;
		for (int i = 0; i < requests.length; i++){
			if (first){
				first = false;
			}
			else{
				requestBuf.append(',');
			}
			String requestString = gson.toJson(requests[i], FullRequest.class);
			requestBuf.append(requestString);
		}
		requestBuf.append(']');
		
		try{
			log.debug("about to dispatch boxcar with " + requests.length + " requests");
			String responseString = dispatch(requestBuf.toString());
			log.debug("got response to boxcar request");
			JsonArray responseArray = jsonParser.parse(responseString).getAsJsonArray();
			if (responseArray.size() != requests.length){
				log.error("sent " + requests.length + " requests but got " + responseArray.size() + " responses");
			}
			Response[] responses = new Response[responseArray.size()];
			for (int j = 0; j < responses.length; j++){
				responses[j] = gson.fromJson(responseArray.get(j), Response.class);
				String requestID = requests[j].getId();
				String responseID = responses[j].getID();
				if (!requestID.equals(responseID)){
					log.error("got mismatched ID's: request " + j + " had ID " + requestID + " but got response with ID " + responseID);
				}
			}
			return responses;
		}
		catch (DispatchException dx){
			throw dx;
		}
		catch (Exception ex){
			throw new DispatchException(DispatchException.INTERNAL_ERROR, "caught " + ex + " while dispatching", ex.toString());
		}		
	}
	
	
	/**
     * Dispatches a request to JSON-RPC server using a URL-encoded JSON request string and returns the response.
     *
     * @param request request to dispatch
     * @param url URL of RPC server
     * @return response based on the request
     */
    public String dispatch(String jsonRequest) throws Exception {
		// record start time of dispatch
    	long startTime = System.currentTimeMillis();
    	
    	if (log.isDebugEnabled()){
        	log.debug("preparing to send " + jsonRequest + "... to " + serviceURL);
        }
    	
    	URLConnection con = serviceURL.openConnection();
    	con.addRequestProperty("Accept","text/plain");
    	
		// Url encode the string in case parameters have spaces/invalid characters
    	String encodedJSONRequest = null;
    	if (enableURLEncoding){
    		encodedJSONRequest = "json=" + URLEncoder.encode(jsonRequest, "utf-8");
    	}
    	else{
    		con.addRequestProperty("Content-type", "application/x-json");
    		encodedJSONRequest = jsonRequest;
    	}
    	
	    String result;
	    BufferedReader in = null;
	    
	    // try to send request to server
        try {
		    
			// POST the json request
			con.setDoOutput(true);
			OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
			wr.write(encodedJSONRequest);
			wr.flush();
			wr.close();
	    
	    	// now read the response
            in = new BufferedReader(
            		new InputStreamReader(con.getInputStream()));
            
            //TODO: would be good to have a timeout check here
            StringBuffer resultBuf = new StringBuffer();
            String line = null;
            boolean first = true;
            while ((line = in.readLine()) != null){
            	if (first){
            		first = false;
            	}
            	else{
            		resultBuf.append('\n');
            	}
            	resultBuf.append(line);
            }
            result = resultBuf.toString();
        } 
        finally {
        	if( in != null){
	        	in.close();
        	}
        }
        
        // record end time of dispatch
        long deltaMillis = System.currentTimeMillis() - startTime;
        
        if (log.isDebugEnabled()){
        	log.debug("took " + deltaMillis + " ms to dispatch " + summarize(jsonRequest, 50) + "... to " + serviceURL);
        }
        
        return result;
    }
    
    protected String summarize(String s, int maxLength){
    	if (s.length() > maxLength){
    		s = s.substring(0, maxLength);
    	}
    	s = s.replace('\n', ' ');
    	s = s.replace('\t', ' ');
    	return s;
    }
    
    
    
    

}
