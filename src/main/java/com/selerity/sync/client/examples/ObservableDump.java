package com.selerity.sync.client.examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.selerity.sync.client.AbstractSyncClient;
import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.MiscUtils;
import com.selerity.sync.client.Request;

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
 * An example application which looks up information about all observables scheduled 
 * to occur within the given time range (or all time if no range given) for all content 
 * sets to which the user is entitled.
 * 
 */

public class ObservableDump  extends AbstractSyncClient{

	private static final Log log = LogFactory.getLog(ObservableDump.class);

	private static final String UTC = "UTC";
	private static final int EVENT_LOOKUP_LIMIT = 50;

	
	protected EventDump eventDump;
	
	
	public ObservableDump(String host, int port, String clientAppName) throws MalformedURLException, DispatchException{
		super(host, port, clientAppName);
		eventDump = new EventDump(host, port, clientAppName);
	}
	
	public void startSession(String user, String password) throws DispatchException{
		super.startSession(user, password);
		eventDump.startSession(user, password);
	}
	
	public void closeSession() throws DispatchException{
		super.closeSession();
		eventDump.closeSession();
	} 
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

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
			String startTime = "1970-01-01T00:00:00.000000000";  // rough approximation for "beginning of time"
			if (args.length > 5){
				startTime = args[5];
				log.debug("set startTime to " + startTime);
			}
			else{
				log.debug("no startTime set");
			}
			
			// see if end time has been set
			String endTime = "2200-01-01T00:00:00.000000000";  // rough approximation for "end of time"
			if (args.length > 6){
				endTime = args[6];
				log.debug("set endTime to " + endTime);
			}
			else{
				log.debug("no endTime set");
			}
			
			// initialize the dumper
			ObservableDump dumper = new ObservableDump(host, port, "ObservableDump");
			dumper.startSession(user, password);

			// dump out a bunch of events
			dumper.dumpEventsWithOffsets(outputFileName, startTime, endTime);
			
			// close session
			dumper.closeSession();
			
			log.debug("all done");
		}
		catch (Exception ex) {
			log.error("caught exception " + ex, ex);
		}
	}
	
	public void dumpEvents(String outputFileName, String startTime, String endTime) throws DispatchException, IOException{
		long startDump = System.currentTimeMillis();
		
		// open the file
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));
		out.write("eventName,earliestStart,expectedStart,expectedEnd,latestEnd,primaryEntity,primaryCategory,"
				+ "measureName,MeasureCode,period,periodRelativity,dataType,legacyObsSpecID,"
				+ "eventSeriesID,eventID,observableID,timeseriesID,obsSpecID\n");
		
		// get the content sets for this user
		List<String> contentSetUUIDs = eventDump.getEntitledEventContentSetUUIDs();

		int eventCount = 0;
		int observableCount = 0;
		
		// loop through the content sets
		for (String contentSetUUID : contentSetUUIDs){
			
			// now look up events for each content set
			List<JsonObject> events = eventDump.getEventsForContentSet(contentSetUUID, startTime, endTime, EVENT_LOOKUP_LIMIT, UTC);
			log.debug("found " + events.size() + " events");
			
			eventCount += events.size();
			
			// loop through each event
			for (JsonObject event : events){
				// some variables for the event
				String eventID = event.get("eventId").getAsString();
				String eventSeriesID = MiscUtils.getString(event, "eventSeriesId", "");
				String eventName = MiscUtils.getString(event,"name","");
				String earliestStart = MiscUtils.getString(event,"earliestExpectedStart","");
				String expectedStart = MiscUtils.getString(event,"expectedStart","");
				String expectedEnd = MiscUtils.getString(event,"expectedEnd","");
				String latestEnd = MiscUtils.getString(event,"latestExpectedEnd","");
				String primaryEntity = eventDump.getPrimaryEntityForEvent(event);
				String primaryCategory = eventDump.getPrimaryCategoryForEvent(event);
				
				// get the observables 
				JsonArray observables = getObservablesForEvent(eventID);
				
				observableCount += observables.size();
				
				// loop through the observables
				for (int i = 0; i < observables.size(); i++){
					JsonObject observable = observables.get(i).getAsJsonObject();
					String observableID = observable.get("observableId").getAsString();
					String measureCode = observable.get("measure").getAsString();
					String measureTagID = getMeasureTagID(measureCode);
					String measureName = getSynonym(measureTagID, null);

					String period = observable.get("period").getAsString();
					String timeseriesID = MiscUtils.getString(observable, "timeSeriesId", null);
					String dataType = observable.get("observationFieldDatatype").getAsJsonObject().get("name").getAsString();
					
					// look up the spec to get the legacy ID
					JsonObject obsSpec = getObservationSpecForObservable(observableID, contentSetUUID);
					String obsSpecID = "";
					String legacyObsSpecID = "";
					if (obsSpec != null){
						obsSpecID = obsSpec.get("observationSpecId").getAsString();
						legacyObsSpecID = obsSpec.get("legacyId").getAsString();
					}
					
					
					// look up the period relativity for the timeseries
					String periodRelativity = "";
					if (timeseriesID != null){
						JsonObject timeseries = getTimeSeriesForObservable(timeseriesID);
						periodRelativity = timeseries.get("periodRelativity").getAsString();
					}
					
					// now write the line
					out.write("\"" + eventName + "\","
							+ earliestStart + ","
							+ expectedStart + ","
							+ expectedEnd + ","
							+ latestEnd + ","
							+ "\"" + primaryEntity + "\","
							+ "\"" + primaryCategory + "\","
							+ "\"" + measureName + "\","
							+ measureCode + ","
							+ period + ","
							+ periodRelativity + ","
							+ dataType + ","
							+ legacyObsSpecID + ","
							+ eventSeriesID + ","
							+ eventID + ","
							+ observableID + ","
							+ (timeseriesID == null ? "" : timeseriesID) + ","
							+ obsSpecID + "\n");
				}
				
			}
		}
		
		// all done
		out.close();
		
		// print out some simple stats
		long elapsedDump = System.currentTimeMillis() - startDump;
		double elapsedDumpSeconds = ((double)elapsedDump) / 1000.0;
		log.debug("dumped " + observableCount + " observables for " + eventCount + " events in " + elapsedDumpSeconds + " seconds");		
	}
	
	
	public void dumpEventsWithOffsets(String outputFileName, String startTime, String endTime) throws DispatchException, IOException{
		long startDump = System.currentTimeMillis();
		
		// open the file
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));
		out.write("eventName,earliestStart,expectedStart,expectedEnd,latestEnd,primaryEntity,primaryCategory,"
				+ "measureName,MeasureCode,period,periodRelativity,dataType,legacyObsSpecID,"
				+ "eventSeriesID,eventID,observableID,timeseriesID,obsSpecID,"
				+ "MeasurementOffset,ObservationStatusOffset,EnvironmentLevelOffset,AlgorithmIDOffset\n");

		// get the content sets for this user
		List<String> contentSetUUIDs = eventDump.getEntitledEventContentSetUUIDs();

		int eventCount = 0;
		int observableCount = 0;
		
		// loop through the content sets
		for (String contentSetUUID : contentSetUUIDs){
			
			// now look up events for each content set
			List<JsonObject> events = eventDump.getEventsForContentSet(contentSetUUID, startTime, endTime, EVENT_LOOKUP_LIMIT, UTC);
			log.debug("found " + events.size() + " events");
			
			eventCount += events.size();
			
			// loop through each event
			for (JsonObject event : events){
				// some variables for the event
				String eventID = event.get("eventId").getAsString();
				String eventSeriesID = MiscUtils.getString(event, "eventSeriesId", "");
				String eventName = MiscUtils.getString(event,"name","");
				String earliestStart = MiscUtils.getString(event,"earliestExpectedStart","");
				String expectedStart = MiscUtils.getString(event,"expectedStart","");
				String expectedEnd = MiscUtils.getString(event,"expectedEnd","");
				String latestEnd = MiscUtils.getString(event,"latestExpectedEnd","");
				String primaryEntity = eventDump.getPrimaryEntityForEvent(event);
				String primaryCategory = eventDump.getPrimaryCategoryForEvent(event);
				
				// get the observables 
				JsonArray observables = getObservablesForEvent(eventID);
				
				observableCount += observables.size();
				
				// loop through the observables
				for (int i = 0; i < observables.size(); i++){
					JsonObject observable = observables.get(i).getAsJsonObject();
					String observableID = observable.get("observableId").getAsString();
					String measureCode = observable.get("measure").getAsString();
					String measureTagID = getMeasureTagID(measureCode);
					String measureName = getSynonym(measureTagID, null);

					String period = observable.get("period").getAsString();
					String timeseriesID = MiscUtils.getString(observable, "timeSeriesId", null);
					String dataType = observable.get("observationFieldDatatype").getAsJsonObject().get("name").getAsString();
					
					// look up the spec 
					JsonObject obsSpec = getObservationSpecForObservable(observableID, contentSetUUID);
					String obsSpecID = "";
					String legacyObsSpecID = "";
					
					String measurementOffset = "";
					String observationStatusOffset = "";
					String environmentLevelOffset = "";
					String algorithmIDOffset = "";
					
					if (obsSpec != null){
						// get the ID's
						obsSpecID = obsSpec.get("observationSpecId").getAsString();
						legacyObsSpecID = obsSpec.get("legacyId").getAsString();
						
						// get some field offsets rom the spec
						measurementOffset = getFieldOffset(obsSpec, "Measurement");
						observationStatusOffset = getFieldOffset(obsSpec, "ObservationStatus");
						environmentLevelOffset = getFieldOffset(obsSpec, "EnvironmentLevel");
						algorithmIDOffset = getFieldOffset(obsSpec, "AlgorithmID");
					}
					
					
					
					// look up the period relativity for the timeseries
					String periodRelativity = "";
					if (timeseriesID != null){
						JsonObject timeseries = getTimeSeriesForObservable(timeseriesID);
						periodRelativity = timeseries.get("periodRelativity").getAsString();
					}
					
					// now write the line
					out.write("\"" + eventName + "\","
							+ earliestStart + ","
							+ expectedStart + ","
							+ expectedEnd + ","
							+ latestEnd + ","
							+ "\"" + primaryEntity + "\","
							+ "\"" + primaryCategory + "\","
							+ "\"" + measureName + "\","
							+ measureCode + ","
							+ period + ","
							+ periodRelativity + ","
							+ dataType + ","
							+ legacyObsSpecID + ","
							+ eventSeriesID + ","
							+ eventID + ","
							+ observableID + ","
							+ (timeseriesID == null ? "" : timeseriesID) + ","
							+ obsSpecID + "," 
							+ measurementOffset + ","
							+ observationStatusOffset + ","  
							+ environmentLevelOffset + ","  
							+ algorithmIDOffset + "\n");
				}
				
			}
		}
		
		// all done
		out.close();
		
		// print out some simple stats
		long elapsedDump = System.currentTimeMillis() - startDump;
		double elapsedDumpSeconds = ((double)elapsedDump) / 1000.0;
		log.debug("dumped " + observableCount + " observables for " + eventCount + " events in " + elapsedDumpSeconds + " seconds");		
	}
	
	/** Returns the offset of the first field with the given name.  Returns a zero-length string if not present.
	 * 
	 * @param obsSpec
	 * @param targetFieldName
	 * @return
	 */
	protected String getFieldOffset(JsonObject obsSpec, String targetFieldName){
		JsonArray fields = obsSpec.get("fields").getAsJsonArray();
		for (int f = 0; f < fields.size(); f++){
			JsonObject field = fields.get(f).getAsJsonObject();
			String fieldName = field.get("name").getAsString();
			if (targetFieldName.equals(fieldName)){
				return MiscUtils.getString(field, "offset", "");
			}	
		}
		return "";
	}

	protected JsonArray getObservablesForEvent(String eventID) throws DispatchException{
		Request observableRequest = new Request("ObservableHandler.getObservablesForEvent");
		observableRequest.setMethodParameter("eventId", eventID);
		
		return dispatch(observableRequest).getAsJsonArray();
	}
	
	protected JsonObject getObservationSpecForObservable(String observableUUID, String contentSetUUID) throws DispatchException{
		Request obsSpecRequest = new Request("ObservationSpecHandler.getCurrentObservationSpecForObservable");
		obsSpecRequest.setMethodParameter("observableId", observableUUID);
		obsSpecRequest.setMethodParameter("contentSetId", contentSetUUID);
		JsonElement spec = dispatch(obsSpecRequest);
		if ((spec == null) || (spec.isJsonNull())){
			return null;
		}
		return spec.getAsJsonObject();
	}
	
	protected JsonObject getTimeSeriesForObservable(String observableUUID) throws DispatchException{
		Request timeseriesRequest = new Request("TimeSeriesHandler.findById");
		timeseriesRequest.setMethodParameter("id", observableUUID);
		return dispatch(timeseriesRequest).getAsJsonObject();
	}
	
	protected JsonObject getDataTypeForObservable(String observableUUID) throws DispatchException {
		Request timeseriesRequest = new Request("TimeSeriesHandler.findById");
		timeseriesRequest.setMethodParameter("id", observableUUID);
		return dispatch(timeseriesRequest).getAsJsonObject();
	}
	
	
	
	
	protected String getMeasureTagID(String measureCode) throws DispatchException{
		Request tagLookupRequest = new Request("TagHandler.getTagByNameValue");
		tagLookupRequest.setMethodParameter("tagName", "Measure");
		tagLookupRequest.setMethodParameter("tagValue", measureCode);
		JsonObject tag = dispatch(tagLookupRequest).getAsJsonObject();
		log.debug("got tag for measureCode " + measureCode + " is: " + tag);
		if ((tag == null) || (tag.isJsonNull())){
			return null;
		}
		return tag.get("tagId").getAsString();
	}
	
	protected String getSynonym(String tagID, String familyName) throws DispatchException{
		Request synonymRequest = new Request("SynonymHandler.findByTagIdAndFamily");
		synonymRequest.setMethodParameter("tagId", tagID);
		synonymRequest.setMethodParameter("familyName", familyName);
		JsonObject synonym = dispatch(synonymRequest).getAsJsonObject();
		log.debug("got synonym for " + tagID + " for family " + familyName + " is: " + synonym);
		if ((synonym == null) || (synonym.isJsonNull())){
			return null;
		}
		return synonym.get("synonym").getAsString();
	}
	
}







