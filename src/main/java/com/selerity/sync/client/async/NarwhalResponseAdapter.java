package com.selerity.sync.client.async;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.selerity.sync.client.Response;

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
 * An adapter which listens to a JsonReader that reads a stream of Responses 
 * and dispatches them to a StreamedResponseListener.
 * 
 */

public class NarwhalResponseAdapter implements Runnable{
	
	private static final Log log = LogFactory.getLog(NarwhalResponseAdapter.class);

	protected final Gson gson;
	protected final JsonReader reader;
	protected final StreamedResponseListener responseListener;
	
	public NarwhalResponseAdapter(JsonReader reader,
			StreamedResponseListener responseListener) {
		this.reader = reader;
		this.responseListener = responseListener;
		
		GsonBuilder builder = new GsonBuilder().serializeNulls();
		builder.registerTypeAdapter(Response.class, new Response.ResponseDeserializer());
		builder.registerTypeAdapter(Response.class, new Response.ResponseSerializer());
		gson = builder.create();
	}
	

	public void run(){
		try{
			while (reader.hasNext()){
				Response response = gson.fromJson(reader, Response.class);
				try{
					responseListener.onResponse(response);
				}
				catch (Exception ex){
					log.error("caught " + ex + " while dispatching response, ignoring", ex);
				}
			}
		}
		catch (Exception ex2){
			log.error("caught " + ex2 + " while reading responses.  Halting.", ex2);
		}
	}

}
