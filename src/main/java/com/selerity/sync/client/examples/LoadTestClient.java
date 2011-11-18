package com.selerity.sync.client.examples;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.stream.JsonReader;
import com.selerity.sync.client.FullRequest;
import com.selerity.sync.client.NarwhalMetaServiceImpl;
import com.selerity.sync.client.NarwhalService;
import com.selerity.sync.client.Request;
import com.selerity.sync.client.SessionImpl;

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
 * A simple app that applies load to the IntrospectionHandler.getVersion method.
 * 
 */

public class LoadTestClient {
	
	private static final Log log = LogFactory.getLog(LoadTestClient.class);
	
	
	public static void main(String[] args){
		Set<JsonReader> readers = new HashSet<JsonReader>();
		try {
			
			if (args.length < 3) {
				System.err.println("arguments: urls user password");
				System.exit(1);
			}

			// read in the arguments
			String urls = args[0];
			//String user = args[1];
			//String password = args[2];
			
			
			
			log.debug("starting...");
			for (int i = 0; i < 5; i++){
				Request request = new Request("IntrospectionHandler.getVersion");
				SessionImpl session = new SessionImpl();
				NarwhalService service = new NarwhalMetaServiceImpl(urls);
				//log.debug("about to send request: " + request.getMethod());
				//JsonElement resultElem = service.dispatch(request, session);
				JsonReader reader = service.dispatch(new FullRequest(request, session, UUID.randomUUID().toString()));
				readers.add(reader);
				//String version = resultElem.getAsString();
				//log.debug("got version " + version);
				//Thread.sleep(1);
			}
			log.debug("done sending, sleeping");
			
			
			while (true){
				Thread.sleep(1000);
			}
			
			
		}
		catch (Exception ex){
			log.error("caught exception " + ex + " in main loop, exiting", ex);
		}
		
		// this allows GC
		readers.clear();
		
		try{
			while (true){
				Thread.sleep(1000);
			}
		}
		catch (Exception ex2){
			log.error("caught exception " + ex2 + " in main loop, exiting", ex2);
		}
	}
	
}
