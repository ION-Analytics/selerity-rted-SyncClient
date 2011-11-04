package com.selerity.sync.client.examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.selerity.sync.client.AbstractSyncClient;
import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.MiscUtils;
import com.selerity.sync.client.PaginatedResponseIterator;
import com.selerity.sync.client.Request;
import com.selerity.sync.client.RhinoHTTPTransportFactory;
import com.selerity.sync.client.Session;
import com.selerity.sync.client.TransportFactory;

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
 * An example application which looks up information about all observables scheduled 
 * to occur within the given time range (or all time if no range given) for all content 
 * sets to which the user is entitled.
 * 
 */

public class ObservableDump  extends AbstractSyncClient{

	private static final Log log = LogFactory.getLog(ObservableDump.class);

	private static final String UTC = "UTC";
	private static final int EVENT_LOOKUP_LIMIT = 50;
	
	private static final String PUBLIC_CONTENT_SET_UUID = "c1bf8518-f826-5d16-80ae-6ac3b705c314";

	
	protected EventDump eventDump;
	
	protected final Map<String,String> measureCodeCache = new HashMap<String,String>();
	protected final Map<String,JsonObject> tagCache = new HashMap<String,JsonObject>();
	protected final Map<String,Map<String,String>> synonymCache = new HashMap<String,Map<String,String>>();
	
	public ObservableDump(TransportFactory transportFactory, String user, String password, String clientAppName) throws Exception{
		super(transportFactory, user, password, clientAppName);
		eventDump = new EventDump(transportFactory, user, password, clientAppName);
	}

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {

			if (args.length < 5) {
				System.err.println("arguments: host port user password outputFileName {startTime} {endTime} {overlap}");
				System.exit(1);
			}

			//
			// do the initial setup
			//

			// read in the arguments
			final String host = args[0];
			final int port = Integer.parseInt(args[1]);
			final String user = args[2];
			final String password = args[3];
			final String outputFileName = args[4];
			
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
			
			// see if overlap has been set
			final boolean overlap;
			if (args.length > 7){
				overlap = Boolean.parseBoolean(args[7]);
				log.debug("overlap is set to: " + overlap);
			}
			else{
				overlap = false;
				log.debug("overlap defaults to false");
			}
			
			// initialize the dumper
			ObservableDump dumper = new ObservableDump(new RhinoHTTPTransportFactory(host, port), user, password, "ObservableDump");

			// dump out a bunch of events
			dumper.dumpEventsWithOffsets(outputFileName, startTime, endTime, overlap);
			
			log.debug("all done");
		}
		catch (Exception ex) {
			log.error("caught exception " + ex, ex);
		}
	}
	
	public void dumpEvents(String outputFileName, String startTime, String endTime, boolean overlap) throws DispatchException, IOException{
		long startDump = System.currentTimeMillis();
		
		// open the file
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));
		out.write("eventName,earliestStart,expectedStart,expectedEnd,latestEnd,primaryEntity,primaryCategory,"
				+ "measureName,MeasureCode,period,periodRelativity,dataType,swordfishObsSpecID,"
				+ "eventSeriesID,eventID,observableID,timeseriesID,obsSpecID\n");
		
		Session session = startSession();
		
		// get the content sets for this user
		List<String> contentSetUUIDs = eventDump.getEntitledEventContentSetUUIDs(session);

		int eventCount = 0;
		int observableCount = 0;
		
		// loop through the content sets
		for (String contentSetUUID : contentSetUUIDs){
			
			// now look up events for each content set
			List<JsonObject> events = eventDump.getEventsForContentSet(session, contentSetUUID, startTime, endTime, EVENT_LOOKUP_LIMIT, UTC, overlap);
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
				JsonArray observables = getObservablesForEvent(session, eventID);
				
				observableCount += observables.size();
				
				// loop through the observables
				for (int i = 0; i < observables.size(); i++){
					JsonObject observable = observables.get(i).getAsJsonObject();
					String observableID = observable.get("observableId").getAsString();
					String measureCode = observable.get("measure").getAsString();
					String measureTagID = getMeasureTagID(session, measureCode);
					String measureName = getSynonym(session, measureTagID, null);

					String period = observable.get("period").getAsString();
					String timeseriesID = MiscUtils.getString(observable, "timeSeriesId", null);
					String dataType = observable.get("observationFieldDatatype").getAsJsonObject().get("name").getAsString();
					
					// look up the spec to get the legacy ID
					JsonObject obsSpec = getObservationSpecForObservable(session, observableID, contentSetUUID);
					String obsSpecID = "";
					String legacyObsSpecID = "";
					if (obsSpec != null){
						obsSpecID = obsSpec.get("observationSpecId").getAsString();
						legacyObsSpecID = obsSpec.get("legacyId").getAsString();
					}
					
					
					// look up the period relativity for the timeseries
					String periodRelativity = "";
					if (timeseriesID != null){
						JsonObject timeseries = getTimeSeriesForObservable(session, timeseriesID);
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
		
		closeSession(session);
		
		// print out some simple stats
		long elapsedDump = System.currentTimeMillis() - startDump;
		double elapsedDumpSeconds = ((double)elapsedDump) / 1000.0;
		log.debug("dumped " + observableCount + " observables for " + eventCount + " events in " + elapsedDumpSeconds + " seconds");		
	}
	
	
	public void dumpEventsWithOffsets(String outputFileName, String startTime, String endTime, boolean overlap) throws DispatchException, IOException{
		long startDump = System.currentTimeMillis();
		
		// open the file
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));
		out.write("eventName,earliestStart,expectedStart,expectedEnd,latestEnd,primaryEntity,primaryCategory,"
				+ "measureName,MeasureCode,period,periodRelativity,dataType,swordfishObsSpecID,"
				+ "eventSeriesID,eventID,observableID,timeseriesID,obsSpecID,"
				+ "MeasurementOffset,ObservationStatusOffset,EnvironmentLevelOffset,AlgorithmIDOffset,CorrelationIDOffset\n");

		Session session = startSession();
		
		// get the content sets for this user
		List<String> contentSetUUIDs = eventDump.getEntitledEventContentSetUUIDs(session);

		int eventCount = 0;
		int specCount = 0;
		
		// 
		// First, look through the scheduled events that fall within the time window.
		//
			
		// now look up events for each content set
		List<JsonObject> events = eventDump.getEventsForContentSet(session, PUBLIC_CONTENT_SET_UUID, startTime, endTime, EVENT_LOOKUP_LIMIT, UTC, overlap);
		log.debug("found " + events.size() + " scheduled events between " + startTime + " and " + endTime);

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

			// get the observables and timeseries
			JsonArray observables = getObservablesForEvent(session, eventID);
			Map<String,JsonObject> timeSeriesMap = null;
			if ((eventSeriesID != null) && (eventSeriesID.length() > 0)){
				timeSeriesMap = getTimeSeriesForEventSeries(session, eventSeriesID);
			}


			// loop through the observables
			for (int i = 0; i < observables.size(); i++){
				JsonObject observable = observables.get(i).getAsJsonObject();
				String observableID = observable.get("observableId").getAsString();

				// look up the spec 
				Map<Long,JsonObject> obsSpecMap = getObsSpecsForObservable(session, observableID);
				if (obsSpecMap.size() > 0){

					String measureCode = observable.get("measure").getAsString();
					String measureTagID = getMeasureTagID(session, measureCode);
					String measureName = getSynonym(session, measureTagID, null);

					String period = observable.get("period").getAsString();
					String timeseriesID = MiscUtils.getString(observable, "timeSeriesId", null);
					String dataType = observable.get("observationFieldDatatype").getAsJsonObject().get("name").getAsString();


					for (JsonObject obsSpec : obsSpecMap.values()){

						// get the ID's
						String obsSpecID = obsSpec.get("observationSpecId").getAsString();
						String swordfishObsSpecID = obsSpec.get("legacyId").getAsString();

						// get some field offsets rom the spec
						String measurementOffset = getFieldOffset(obsSpec, "Measurement");
						String observationStatusOffset = getFieldOffset(obsSpec, "ObservationStatus");
						String environmentLevelOffset = getFieldOffset(obsSpec, "EnvironmentLevel");
						String algorithmIDOffset = getFieldOffset(obsSpec, "AlgorithmID");
						String correlationIDOffset = getFieldOffset(obsSpec, "CorrelationID");
						
						// look up the period relativity for the timeseries
						String periodRelativity = "";
						if ((timeseriesID != null) && (timeSeriesMap != null)){
							JsonObject timeseries = timeSeriesMap.get(timeseriesID);
							if (timeseries != null){
								periodRelativity = timeseries.get("periodRelativity").getAsString();
							}
							else{
								log.warn("couldn't find timeseries for UUID " + timeseriesID);
							}
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
								+ swordfishObsSpecID + ","
								+ eventSeriesID + ","
								+ eventID + ","
								+ observableID + ","
								+ (timeseriesID == null ? "" : timeseriesID) + ","
								+ obsSpecID + "," 
								+ measurementOffset + ","
								+ observationStatusOffset + ","  
								+ environmentLevelOffset + ","  
								+ algorithmIDOffset + "," 
								+ correlationIDOffset + "\n");
						
						specCount++;
					}
				}
			}				
		}
			
		//
		// Second, look through the unscheduled event series to see if any of them have specs to which this user is entitled
		//
		
		// Some values don't make sense for an unscheduled event series so blank them out here:
		String earliestStart = "";
		String expectedStart = "";
		String expectedEnd = "";
		String latestEnd = "";
		String period = "";
		String eventID = "";
		String observableID = "";
		
		
		
		List<JsonObject> unscheduledEventSeriesList = getAllUnscheduledEventSeries(session);
		for (JsonObject eventSeriesObj : unscheduledEventSeriesList){
			
			// get some information from the event series directly
			String eventSeriesUUID = MiscUtils.getString(eventSeriesObj, "eventSeriesId", "");
			String eventSeriesName = MiscUtils.getString(eventSeriesObj, "name", "");
			String primaryEntity = eventDump.getPrimaryEntityForEventSeries(eventSeriesObj);
			String primaryCategory = eventDump.getPrimaryCategoryForEventSeries(eventSeriesObj);
			Map<String,JsonObject> timeSeriesMap = getTimeSeriesForEventSeries(session, eventSeriesUUID);
			for (JsonObject timeSeriesObj : timeSeriesMap.values()){
				String timeSeriesUUID = MiscUtils.getString(timeSeriesObj, "timeSeriesId", "");
				
				Map<Long,JsonObject> obsSpecMap = getObsSpecsForTimeSeries(session, contentSetUUIDs, timeSeriesUUID);
				if (obsSpecMap.size() > 0){
					// we have at least 1 obs spec so process this time series
				
				
					// get some information from the timeseries directly
					String measureTagUUID = MiscUtils.getString(timeSeriesObj, "measureTagId", null);
					String measureName = "";
					String measureCode = "";
					if (measureTagUUID != null){
						JsonObject tag = getTag(session, measureTagUUID);
						if (tag != null){
							measureCode = MiscUtils.getString(tag, "value", "");
							measureName = getSynonym(session, measureTagUUID, null);
							if (measureName == null){
								measureName = "";
							}
						}
					}
					String periodRelativity = MiscUtils.getString(timeSeriesObj, "periodRelativity", "");
					String dataType = MiscUtils.getString(timeSeriesObj, "datatypeId", "");
				
					for (JsonObject obsSpec : obsSpecMap.values()){
						// get the ID's
						String obsSpecID = obsSpec.get("observationSpecId").getAsString();
						String swordfishObsSpecID = obsSpec.get("legacyId").getAsString();
						
						// get some field offsets rom the spec
						String measurementOffset = getFieldOffset(obsSpec, "Measurement");
						String observationStatusOffset = getFieldOffset(obsSpec, "ObservationStatus");
						String environmentLevelOffset = getFieldOffset(obsSpec, "EnvironmentLevel");
						String algorithmIDOffset = getFieldOffset(obsSpec, "AlgorithmID");
						String correlationIDOffset = getFieldOffset(obsSpec, "CorrelationID");
						
						// all the data's been collected, write out the row
						// now write the line
						out.write("\"" + eventSeriesName + "\","
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
								+ swordfishObsSpecID + ","
								+ eventSeriesUUID + ","
								+ eventID + ","
								+ observableID + ","
								+ timeSeriesUUID + ","
								+ obsSpecID + "," 
								+ measurementOffset + ","
								+ observationStatusOffset + ","  
								+ environmentLevelOffset + ","  
								+ algorithmIDOffset + "," 
								+ correlationIDOffset + "\n");
						
						specCount++;
					}
				}
				
			}
			
			
		}
		
		
		
		// all done
		out.close();
		
		closeSession(session);
		
		// print out some simple stats
		long elapsedDump = System.currentTimeMillis() - startDump;
		double elapsedDumpSeconds = ((double)elapsedDump) / 1000.0;
		log.debug("dumped " + specCount + " rows for " + eventCount + " events in " + elapsedDumpSeconds + " seconds");		
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

	protected JsonArray getObservablesForEvent(Session session, String eventID) throws DispatchException{
		Request observableRequest = new Request("ObservableHandler.getObservablesForEvent");
		observableRequest.setMethodParameter("eventId", eventID);
		
		return dispatch(observableRequest, session).getAsJsonArray();
	}
	
	protected JsonObject getObservationSpecForObservable(Session session, String observableUUID, String contentSetUUID) throws DispatchException{
		Request obsSpecRequest = new Request("ObservationSpecHandler.getCurrentObservationSpecForObservable");
		obsSpecRequest.setMethodParameter("observableId", observableUUID);
		obsSpecRequest.setMethodParameter("contentSetId", contentSetUUID);
		JsonElement spec = dispatch(obsSpecRequest, session);
		if ((spec == null) || (spec.isJsonNull())){
			return null;
		}
		return spec.getAsJsonObject();
	}
	
	protected JsonObject getTimeSeriesForObservable(Session session, String observableUUID) throws DispatchException{
		Request timeseriesRequest = new Request("TimeSeriesHandler.findById");
		timeseriesRequest.setMethodParameter("id", observableUUID);
		return dispatch(timeseriesRequest, session).getAsJsonObject();
	}
	
	protected JsonObject getDataTypeForObservable(Session session, String observableUUID) throws DispatchException {
		Request timeseriesRequest = new Request("TimeSeriesHandler.findById");
		timeseriesRequest.setMethodParameter("id", observableUUID);
		return dispatch(timeseriesRequest, session).getAsJsonObject();
	}
	
	
	protected String getMeasureTagID(Session session, String measureCode) throws DispatchException{
		synchronized (measureCodeCache){
			String measureTagUUID = measureCodeCache.get(measureCode);
			if (measureTagUUID != null){
				return measureTagUUID;
			}
		}
		Request tagLookupRequest = new Request("TagHandler.getTagByNameValue");
		tagLookupRequest.setMethodParameter("tagName", "Measure");
		tagLookupRequest.setMethodParameter("tagValue", measureCode);
		JsonObject tag = dispatch(tagLookupRequest, session).getAsJsonObject();
		log.debug("got tag for measureCode " + measureCode + " is: " + tag);
		if ((tag == null) || (tag.isJsonNull())){
			return null;
		}
		String measureTagUUID = tag.get("tagId").getAsString();
		synchronized (measureCodeCache){
			measureCodeCache.put(measureCode, measureTagUUID);
		}
		return measureTagUUID;
	}
	
	protected JsonObject getTag(Session session, String tagUUID) throws DispatchException {
		synchronized (tagCache){
			JsonObject cachedTag = tagCache.get(tagUUID);
			if (cachedTag != null){
				return cachedTag;
			}
		}
		Request tagRequest = new Request("TagHandler.findById");
		tagRequest.setMethodParameter("tagId", tagUUID);
		JsonElement tagElem = dispatch(tagRequest, session);
		if ((tagElem == null) || (tagElem.isJsonNull())){
			return null;
		}
		JsonObject tagObj = tagElem.getAsJsonObject();
		synchronized (tagCache){
			tagCache.put(tagUUID, tagObj);
		}
		return tagObj;
	}
	
	protected String getSynonym(Session session, String tagID, String familyName) throws DispatchException{
		synchronized (synonymCache){
			Map<String,String> familySynonymMap = synonymCache.get(tagID);
			if (familySynonymMap != null){
				String synonym = familySynonymMap.get(familyName);
				if (synonym != null){
					return synonym;
				}
			}
		}
		Request synonymRequest = new Request("SynonymHandler.findByTagIdAndFamily");
		synonymRequest.setMethodParameter("tagId", tagID);
		synonymRequest.setMethodParameter("familyName", familyName);
		JsonObject synonym = dispatch(synonymRequest, session).getAsJsonObject();
		log.debug("got synonym for " + tagID + " for family " + familyName + " is: " + synonym);
		if ((synonym == null) || (synonym.isJsonNull())){
			return null;
		}
		String synonymString = synonym.get("synonym").getAsString();
		synchronized (synonymCache){
			Map<String,String> familySynonymMap = synonymCache.get(tagID);
			if (familySynonymMap == null){
				familySynonymMap = new HashMap<String,String>(1);
				synonymCache.put(tagID, familySynonymMap);
			}
			familySynonymMap.put(familyName, synonymString);
		}
		return synonymString;
	}
	
	
	//protected JsonObject getEventSeriesByID(Session session, String eventSeriesUUID) throws DispatchException{
	//	Request eventSeriesRequest = new Request("EventSeriesHandler.findById");
	//	eventSeriesRequest.setMethodParameter("id", eventSeriesUUID);
	//	JsonElement eventSeriesElement = dispatch(eventSeriesRequest, session);
	//	if ((eventSeriesElement == null) || (eventSeriesElement.isJsonNull())){
	//		return null;
	//	}
	//	return eventSeriesElement.getAsJsonObject();
	//}
	
	protected List<JsonObject> getAllUnscheduledEventSeries(Session session) throws DispatchException{
		List<JsonObject> eventSeriesList = new ArrayList<JsonObject>();
		Request eventSeriesRequest = new Request("EventSeriesHandler.getAllEventSeries");
		PaginatedResponseIterator eventSeriesIt = this.paginatedDispatch(eventSeriesRequest, session, "paginationOption", 100);
		while (eventSeriesIt.hasNextResult()){
			JsonObject eventSeriesObj = eventSeriesIt.nextResult().getAsJsonObject();
			JsonObject schedule = MiscUtils.getJsonObject(eventSeriesObj, "schedule", null);
			if (schedule != null){
				String type = MiscUtils.getString(schedule, "type", null);
				if ("Unscheduled".equals(type)){
					eventSeriesList.add(eventSeriesObj);
				}
			}
		}
		return eventSeriesList;
	}
	
	/** Returns a map from TimeSeriesUUID to TimeSeries object of all the time series for this event series.
	 * 
	 * @param session
	 * @param eventSeriesUUID
	 * @return
	 * @throws DispatchException
	 */
	protected Map<String,JsonObject> getTimeSeriesForEventSeries(Session session, String eventSeriesUUID) throws DispatchException{
		Map<String,JsonObject> timeSeriesMap = new HashMap<String,JsonObject>();
		Request timeSeriesRequest = new Request("EventSeriesHandler.getTimeSeries");
		timeSeriesRequest.setMethodParameter("id", eventSeriesUUID);
		JsonElement timeSeriesElem = dispatch(timeSeriesRequest, session);
		if ((timeSeriesElem == null) || (timeSeriesElem.isJsonNull())){
			return timeSeriesMap;
		}
		JsonArray timeSeriesArray = timeSeriesElem.getAsJsonArray();
		for (int i = 0; i < timeSeriesArray.size(); i++){
			JsonObject timeSeriesObj = timeSeriesArray.get(i).getAsJsonObject();
			String timeSeriesUUID = MiscUtils.getString(timeSeriesObj, "timeSeriesId", null);
			if (timeSeriesUUID != null){
				timeSeriesMap.put(timeSeriesUUID, timeSeriesObj);
			}
		}
		return timeSeriesMap;
	}
	
	public Map<Long,JsonObject> getObsSpecsForTimeSeries(Session session, Collection<String> contentSetUUIDs, String timeSeriesUUID) throws DispatchException{
		Map<Long,JsonObject> specMap = new HashMap<Long,JsonObject>();
	
		for (String contentSetUUID : contentSetUUIDs){
			Request obsSpecTSRequest = new Request("ObservationSpecHandler.getObservationSpecsForTimeseries");
			obsSpecTSRequest.setMethodParameter("timeseriesId", timeSeriesUUID);
			obsSpecTSRequest.setMethodParameter("contentSetId", contentSetUUID);
			JsonElement obsSpecElem = dispatch(obsSpecTSRequest, session);
			if ((obsSpecElem != null) && (!obsSpecElem.isJsonNull())){
				JsonArray obsSpecArray = obsSpecElem.getAsJsonArray();
				for (int i = 0; i < obsSpecArray.size(); i++){
					JsonObject obsSpec = obsSpecArray.get(i).getAsJsonObject();
					long swordfishID = MiscUtils.getLong(obsSpec, "legacyId", 0);
					specMap.put(swordfishID, obsSpec);
				}
			}
		}
		
		return specMap;
	}
	

	public Map<Long,JsonObject> getObsSpecsForObservable(Session session, String observableUUID) throws DispatchException{
		Map<Long,JsonObject> specMap = new HashMap<Long,JsonObject>();
		
		Request obsSpecOBRequest = new Request("ObservationSpecHandler.getCurrentObservationSpecForObservable");
		// variant for object (map) style parames)
		obsSpecOBRequest.setMethodParameter("observableId", observableUUID);
		JsonElement obsSpecElem = dispatch(obsSpecOBRequest, session);
		if ((obsSpecElem != null) && (!obsSpecElem.isJsonNull())){
			JsonArray obsSpecArray = obsSpecElem.getAsJsonArray();
			for (int i = 0; i < obsSpecArray.size(); i++){
				JsonObject obsSpec = obsSpecArray.get(i).getAsJsonObject();
				long swordfishID = MiscUtils.getLong(obsSpec, "legacyId", 0);
				specMap.put(swordfishID, obsSpec);
			}
		}		
		
		return specMap;
	}
	
}







