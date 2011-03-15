package com.selerity.sync.client.examples;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.selerity.sync.client.AbstractSyncClient;
import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.Request;
import com.selerity.sync.client.RhinoHTTPTransportFactory;
import com.selerity.sync.client.Session;
import com.selerity.sync.client.TransportFactory;

/**
 * © Copyrights Selerity, Inc. 2009-2011. All rights reserved. This source code
 * is confidential and proprietary information of Selerity Inc. and may be used
 * only by a recipient designated by and for the purposes permitted by Selerity
 * Inc. in writing. Reproduction of, dissemination of, modifications to or
 * creation of derivative works from this source code, whether in source or
 * binary forms, by any means and in any form or manner, is expressly
 * prohibited, except with the prior written permission of Selerity Inc.. THIS
 * CODE AND INFORMATION ARE PROVIDED ÒAS ISÓ WITHOUT WARRANTY OF ANY KIND,
 * EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE. This notice may
 * not be removed from the software by any user thereof.
 * 
 * An example application which tests entitlements.
 * 
 */

public class EntitlementTest extends AbstractSyncClient{

	private static final Log log = LogFactory.getLog(EntitlementTest.class);

	private static final int SPEC_LOOKUP_LIMIT = 500;

	
	public EntitlementTest(TransportFactory transportFactory, String user, String password, String clientAppName) throws Exception{
		super(transportFactory, user, password, clientAppName);
	}
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {

			if (args.length < 4) {
				System.err.println("arguments: host port user password");
				System.exit(1);
			}

			//
			// do the initial setup
			//

			// read in the arguments
			String host = args[0];
			int port = Integer.parseInt(args[1]);
			String user = args[2];
			String password = args[3];
			
			// initialize the dumper
			EntitlementTest tester = new EntitlementTest(new RhinoHTTPTransportFactory(host, port), user, password, "EntitlementTest");
			
			// dump out a bunch of events
			Set<Long> specIDs = tester.getAllObsSpecIDs(SPEC_LOOKUP_LIMIT);
			
			log.info("spec ID's = " + specIDs);
			
			log.debug("all done");

		}
		catch (Exception ex) {
			log.error("caught exception " + ex, ex);
		}
	}
	
	/** Returns a set of legacy spec ID's that correspond to the content sets to which this user is entitled.
	 * 
	 * @param limit
	 * @return
	 * @throws DispatchException
	 * @throws IOException
	 */
	public Set<Long> getAllObsSpecIDs(int limit) throws DispatchException, IOException{
		long startDump = System.currentTimeMillis();
		Set<Long> specIDSet = new HashSet<Long>();
		
		Session session = startSession();
		
		// get the content sets for this user
		Request contentSetRequest = new Request("ContentSetHandler.getContentSets");
		JsonArray contentSetResponse = dispatch(contentSetRequest, session).getAsJsonArray(); 
		log.debug("contentSetResponse = " + contentSetResponse);
		
		// print out the content sets
		for (int i = 0; i < contentSetResponse.size(); i++){
			JsonObject contentSet = contentSetResponse.get(i).getAsJsonObject();
			String contentSetName = contentSet.get("name").getAsString();
			String contentSetUUID = contentSet.get("contentSetId").getAsString();
			log.debug("getting specs for content set[" + i + "] = " + contentSetName + " (" + contentSetUUID + ")");
			specIDSet.addAll(getAllObsSpecIDsForContentSet(contentSetUUID, limit));
		}
		
		// all done; print out some simple stats
		long elapsedDump = System.currentTimeMillis() - startDump;
		double elapsedDumpSeconds = ((double)elapsedDump) / 1000.0;
		log.debug("got " + specIDSet.size() + " specs for all content sets in " + elapsedDumpSeconds + " seconds");
		
		closeSession(session);
		
		return specIDSet;
	}
	
	/** Returns a set of legacy spec ID's that correspond to the given content set.
	 * 
	 * @param limit
	 * @return
	 * @throws DispatchException
	 * @throws IOException
	 */
	public Set<Long> getAllObsSpecIDsForContentSet(String contentSetUUID, int limit) throws DispatchException, IOException{
		long startDump = System.currentTimeMillis();
		Session session = startSession();
		
		Set<Long> specIDSet = new HashSet<Long>();
		
		int offset = 0;
		int numRetreived = limit;
		
		while (numRetreived > 0){
		
			Request specRequest = new Request("ObservationSpecHandler.getAllObsSpecs");
			specRequest.setMethodParameter("contentSetId", contentSetUUID);
			
			// set some options for the search
			JsonObject paginationOption = new JsonObject();
			paginationOption.addProperty("offset", offset);
			paginationOption.addProperty("limit", limit);
			
			specRequest.setMethodParameter("paginationOption", paginationOption);
			
			JsonArray specs = dispatch(specRequest, session).getAsJsonArray();
			numRetreived = specs.size();
			offset += limit;
			log.debug("got " + specs.size() + " specs");
			
			for (int i = 0; i < specs.size(); i++){
				JsonObject spec = specs.get(i).getAsJsonObject();
				long legacySpecID = spec.get("legacyId").getAsLong();
				specIDSet.add(legacySpecID);
			}

		}
		
		// all done; print out some simple stats
		long elapsedDump = System.currentTimeMillis() - startDump;
		double elapsedDumpSeconds = ((double)elapsedDump) / 1000.0;
		log.debug("got " + specIDSet.size() + " specs for this content set in " + elapsedDumpSeconds + " seconds");
		
		closeSession(session);
		
		return specIDSet;
	}
	

	
}







