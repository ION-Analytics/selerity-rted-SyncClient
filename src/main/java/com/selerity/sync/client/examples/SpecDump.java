package com.selerity.sync.client.examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.Dispatcher;
import com.selerity.sync.client.MiscUtils;
import com.selerity.sync.client.Request;
import com.selerity.sync.client.RhinoDispatcher;
import com.selerity.sync.client.RhinoHTTPTransport;
import com.selerity.sync.client.RhinoSessionFactory;
import com.selerity.sync.client.Session;
import com.selerity.sync.client.Transport;

/**
 * © Copyrights Selerity, Inc. 2009-2010. All rights reserved. This source code
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
 * An example application which looks up observation specifications and writes 
 * them out to a file in the same format as was used by the legacy RDS system.
 * 
 */

public class SpecDump {
	
	private static final Log log = LogFactory.getLog(SpecDump.class);

	protected final Session session;
	protected final Dispatcher dispatcher;
	
	private static final String SELERITY_CORPORATE_CONTENT_SET_UUID = "c2bf8528-9a84-1fa7-1d19-d298c5e02b06";
	private static final String SELERITY_ECONOMICS_CONTENT_SET_UUID = "c3bf8537-cb61-4eb0-f514-8eae8961d49a";
	private static final String SELERITY_ENERGY_CONTENT_SET_UUID = "c4bf8547-bb70-52dc-bf01-38662f3e19fd";
	
	public static Set<String> getSelerityContentSetUUIDs(){
		Set<String> s = new HashSet<String>();
		s.add(SELERITY_CORPORATE_CONTENT_SET_UUID);
		s.add(SELERITY_ECONOMICS_CONTENT_SET_UUID);
		s.add(SELERITY_ENERGY_CONTENT_SET_UUID);
		return Collections.unmodifiableSet(s);
	}
	
	private static final Set<String> SELERITY_CONTENT_SET_UUIDS = getSelerityContentSetUUIDs();
	
	private static final int SPEC_LOOKUP_BATCH_SIZE_LIMIT = 1000;
	
	protected final Map<String,Long> eventStartTimes = new HashMap<String,Long>();
	protected final Map<String,Long> eventEndTimes = new HashMap<String,Long>();
	protected final Map<String,String> eventEntities = new HashMap<String,String>();
	
	
	
	public static void main(String[] args){
		try {

			if (args.length < 5) {
				System.err.println("arguments: host port user password outputFileName {startTime} {endTime}");
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
			String outputFileName = args[4];
			
			// see if start time has been set
			String startTimeStr = "1970-01-01T00:00:00.000000000";  // rough approximation for "beginning of time"
			if (args.length > 5){
				startTimeStr = args[5];
				log.debug("set startTime to " + startTimeStr);
			}
			else{
				log.debug("no startTime set");
			}
			long startTime = MiscUtils.parseNanoTime(startTimeStr);
			
			// see if end time has been set
			String endTimeStr = "2200-01-01T00:00:00.000000000";  // rough approximation for "end of time"
			if (args.length > 6){
				endTimeStr = args[6];
				log.debug("set endTime to " + endTimeStr);
			}
			else{
				log.debug("no endTime set");
			}
			long endTime = MiscUtils.parseNanoTime(endTimeStr);
			
			// initialize the dumper
			SpecDump dumper = new SpecDump(host, port, user, password);
			
			// dump the specs
			dumper.dumpSpecs(outputFileName, startTime, endTime, SPEC_LOOKUP_BATCH_SIZE_LIMIT);
			
			log.debug("all done");

		}
		catch (Exception ex) {
			log.error("caught exception " + ex, ex);
		}
	}
	
	
	
	
	public SpecDump(String host, int port, String user, String password) throws MalformedURLException, DispatchException{
		// initialized the transport and method dispatcher
		String rhinoURL = "http://" + host + ":" + port + "/rhino-1.0-SNAPSHOT/rpc.do";
		Transport transport = new RhinoHTTPTransport(rhinoURL, false);
		dispatcher = new RhinoDispatcher(transport);

		// log in, setting the 'client identification' field and enabling
		// the 'extensions mode'
		session = new RhinoSessionFactory().getInstance(dispatcher,
				"SpecDump", "extension", user, password);
	}
	
	protected void dumpSpecs(String outputFileName, Long startWindow, Long endWindow, int limit) throws IOException, DispatchException, ParseException{
		// log the start time so that we can compute elapsed time later
		long startDump = System.currentTimeMillis();
		
		// open the file
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));	
		
		int obsSpecCount = 0;
		int dumpCount = 0;
		
		// get the content sets for this user
		JsonArray contentSets = getEntitledContentSetUUIDs();
		
		// dowbload all observables for these content sets, mapping each spec to its observable
		boolean finished = false;
		int offset = 0;
		while (!finished){
			// download a chunk
			Request obsSpecRequest = new Request("ObservationSpecHandler.getAllObsSpecs");
			obsSpecRequest.setMethodParameter("contentSetIds", contentSets);
			JsonObject paginationOption = new JsonObject();
			paginationOption.addProperty("offset", offset);
			paginationOption.addProperty("limit", limit);
			obsSpecRequest.setMethodParameter("paginationOption", paginationOption);
			
			JsonArray obsSpecs = dispatcher.dispatch(obsSpecRequest, session).getAsJsonArray();
			if ((obsSpecs == null) || (obsSpecs.isJsonNull()) || (obsSpecs.size() < 1)){
				finished = true;
			}
			else{
				int batchCount = obsSpecs.size();
				
				obsSpecCount += batchCount;
				offset += batchCount;
				
				// process all of the specs in this batch
				for (int i = 0; i < batchCount; i++){
					JsonObject obsSpec = obsSpecs.get(i).getAsJsonObject();
					String observableID = obsSpec.get("observableId").getAsString();
					
					// look up the observable that corresponds to this spec
					Request observableRequest = new Request("ObservableHandler.findById");
					observableRequest.setMethodParameter("observableId", observableID);
					JsonObject observable = dispatcher.dispatch(observableRequest, session).getAsJsonObject();
					String eventID = observable.get("eventId").getAsString();
					
					// check if this observable is in the time range given
					if (overlapsTimeRange(eventID, startWindow, endWindow)){
						
						// dump out the spec
						dumpCount++;
						
						// ID
						long legacySpecID = obsSpec.get("legacyId").getAsLong();
						out.write("ID," + legacySpecID + "\n");
						
						// Length
						long length = obsSpec.get("messageLength").getAsLong();
						out.write("Length," + length + "\n");
						
						// field count
						JsonArray fields = obsSpec.get("fields").getAsJsonArray();  
						int fieldCount = fields.size() + 3;                         // Note - the 3 static fields need to be included!
						out.write("FieldCount," + fieldCount + "\n");
						
						// Active
						boolean active = obsSpec.get("isCurrent").getAsBoolean();
						out.write("Active," + active + "\n");
						
						// LastUpdateTime
						String lastUpdateTime = obsSpec.get("lastUpdate").getAsString();
						out.write("LastUpdateTime," + lastUpdateTime + "\n");
						
						// Static fields
						
						// Measure
						String measureCode = observable.get("measure").getAsString();
						out.write("Static,Measure,STRING," + measureCode + "\n");
						
						// Entity
						String entityID = eventEntities.get(eventID); // #TODO - fix hack; depends on this being set during the time overlap call!
						out.write("Static,EntityID,STRING," + entityID + "\n");
						
						// Period
						String period = observable.get("period").getAsString();
						out.write("Static,Period,STRING," + period + "\n");
					
						
						// Active Fields
						for (int f = 0; f < fields.size(); f++){
							JsonObject field = fields.get(f).getAsJsonObject();
							String fieldName = field.get("name").getAsString();
							JsonObject dataType = field.get("datatype").getAsJsonObject();
							String dataTypeName = dataType.get("name").getAsString();
							int fieldOffset = field.get("offset").getAsInt();
							JsonElement fieldLengthElement = field.get("length");
							if ((fieldLengthElement == null) || (fieldLengthElement.isJsonNull())){
								// fixed length field
								out.write("Dynamic," + fieldName + "," + dataTypeName + "," + fieldOffset + "\n");
							}
							else{
								// dynamic length field
								int fieldLength = field.get("length").getAsInt();
								out.write("Dynamic," + fieldName + "," + dataTypeName + "," + fieldOffset + "," + fieldLength + "\n");
							}
									
						}
						
						// done with this spec, add a blank separator line
						out.write("\n");
						out.flush();
						
					}
				}
			}
		}
		
		// all done
		out.close();
		
		// print out some simple stats
		long elapsedDump = System.currentTimeMillis() - startDump;
		double elapsedDumpSeconds = ((double)elapsedDump) / 1000.0;
		log.debug("dumped " + dumpCount + " observation specs out of "+ obsSpecCount + " total observables to which I am entitled in " + elapsedDumpSeconds + " seconds");		
		
	}
	
	protected boolean overlapsTimeRange(String eventID, Long startWindow, Long endWindow) throws DispatchException, ParseException{
		Long eventStartTime = eventStartTimes.get(eventID);
		Long eventEndTime = eventEndTimes.get(eventID);
		String eventEntity = eventEntities.get(eventID);
		if ((eventStartTime == null) || (eventEndTime == null) || (eventEntity == null)){
			// look up event watch window
			Request eventRequest = new Request("EventHandler.findById");
			eventRequest.setMethodParameter("eventId", eventID);
			eventRequest.setMethodParameter("timeZoneId", "UTC");
					
			JsonObject event = dispatcher.dispatch(eventRequest, session).getAsJsonObject(); 
			String earliestStart = event.get("earliestExpectedStart").getAsString();
			String latestEnd = event.get("latestExpectedEnd").getAsString();
			eventStartTime = MiscUtils.parseNanoTime(earliestStart);
			eventEndTime = MiscUtils.parseNanoTime(latestEnd);
			eventStartTimes.put(eventID, eventStartTime);
			eventEndTimes.put(eventID, eventEndTime);
			
			// also set the event entity
			eventEntity = getPrimaryEntityForEvent(event);
			eventEntities.put(eventID, eventEntity);
		}
		return (startWindow < eventEndTime) && (endWindow > eventStartTime);
	}
	
	
	/** Returns a JsonArray of content set UUID's to which the user is entitled and which refer to
	 *  non-trivial Selerity event content sets.
	 * 
	 * @return
	 * @throws DispatchException
	 */
	protected JsonArray getEntitledContentSetUUIDs() throws DispatchException{
		// look up all of the content sets to which this user is entitled
		Request contentSetRequest = new Request("ContentSetHandler.getContentSets");
		JsonArray contentSetResponse = dispatcher.dispatch(contentSetRequest, session).getAsJsonArray(); 
		log.debug("contentSetResponse = " + contentSetResponse);
		
		JsonArray contentSetUUIDs = new JsonArray();
		
		// print out the content sets
		for (int i = 0; i < contentSetResponse.size(); i++){
			JsonObject contentSet = contentSetResponse.get(i).getAsJsonObject();
			String contentSetName = contentSet.get("name").getAsString();
			String contentSetUUID = contentSet.get("contentSetId").getAsString();
			log.debug("content set[" + i + "] = " + contentSetName + " (" + contentSetUUID + ")");
			if (SELERITY_CONTENT_SET_UUIDS.contains(contentSetUUID)){  // filter out the non-selerity content sets
				contentSetUUIDs.add(new JsonPrimitive(contentSetUUID));
			}
		}
		
		return contentSetUUIDs;
	}
	
	protected String getPrimaryEntityForEvent(JsonObject event){
		double maxScore = Double.MIN_VALUE;
		String maxValue = null;
		JsonArray tags = event.get("tagSummaries").getAsJsonArray();
		for (int i = 0; i < tags.size(); i++){
			JsonObject tag = tags.get(i).getAsJsonObject();
			String name = tag.get("name").getAsString();
			if (name.equalsIgnoreCase("Entity")){
				double score = tag.get("relevanceScore").getAsDouble();
				if (score > maxScore){
					maxScore = score;
					maxValue = tag.get("value").getAsString();
				}
			}
		}
		return maxValue;		
	}
	
	
}
