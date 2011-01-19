package com.selerity.sync.client.examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
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
import com.selerity.sync.client.Response;
import com.selerity.sync.client.RhinoDispatcher;
import com.selerity.sync.client.RhinoHTTPTransport;
import com.selerity.sync.client.RhinoSessionFactory;
import com.selerity.sync.client.Session;
import com.selerity.sync.client.Transport;


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
 * 
 * A simple demonstration of an application which uses the Selerity API to retrieve some 
 * event-related information and write it out to a CSV file for import into Excel.
 * 
 *
 */
public class ExampleClient {

	private static final Log log = LogFactory.getLog(ExampleClient.class);	
	
	
	/** Main method takes host, port, user and passwords as command-line arguments. 
	 *  Tests several steps -- logging in, lookup up content sets, looking up tags, 
	 *  searching for events, looking up observables and observation specs, etc.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {


		try {
			
			if (args.length < 5){
				System.err.println("arguments: host port user password eventDataFile");
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
			String eventDataFileName = args[4];
			
			// initialized the transport and method dispatcher
			String rhinoURL = "http://" + host + ":" + port + "/rhino-1.0-SNAPSHOT/rpc.do";
			Transport transport = new RhinoHTTPTransport(rhinoURL, false);
			Dispatcher dispatcher = new RhinoDispatcher(transport);

			// log in, setting the 'client identification' field and enabling the 'extensions mode'
			Session session = new RhinoSessionFactory().getInstance(dispatcher,
					"ExampleClient", "extension", user, password);
			
			
			//
			// look up the content sets to which I am entitled and in particular look up the Selerity Corporate Earnings content set...
			//
			
			// first, look up all of the content sets to which this user is entitled
			Request contentSetRequest = new Request("ContentSetHandler.getContentSets");
			JsonArray contentSetResponse = dispatcher.dispatch(contentSetRequest, session).getAsJsonArray(); 
			log.debug("contentSetResponse = " + contentSetResponse);
			
			// print out the content sets
			for (int i = 0; i < contentSetResponse.size(); i++){
				JsonObject contentSet = contentSetResponse.get(i).getAsJsonObject();
				String contentSetName = contentSet.get("name").getAsString();
				String contentSetUUID = contentSet.get("contentSetId").getAsString();
				log.debug("content set[" + i + "] = " + contentSetName + " (" + contentSetUUID + ")");
			}
			
			// now look up the 'Selerity Corporate Earnings' set:
			String earningsContentSetID = null;
			String earningsContentSetName = "Selerity Corporate";
			Request earningsContentSetRequest = new Request("ContentSetHandler.getContentSetByName");
			earningsContentSetRequest.setMethodParameter("name", earningsContentSetName);
			JsonElement contentSetElement = dispatcher.dispatch(earningsContentSetRequest, session);
			if ((contentSetElement == null) || (contentSetElement.isJsonNull())){
				log.warn("User " + user + " is not entitled to " + earningsContentSetName);
			}
			else{
				String contentSetUUID = contentSetElement.getAsJsonObject().get("contentSetId").getAsString();
				earningsContentSetID = contentSetUUID;
			}
			
			
			
			//
			// now look up the tag for Category=Earnings, getting the UUID's for the name and value
			//
			
			String tagName = "Category";
			String tagValue = "Earnings";

			Request tagRequest = new Request("TagHandler.getTagByNameValue");
			tagRequest.setMethodParameter("tagName", tagName);
			tagRequest.setMethodParameter("tagValue", tagValue);
			JsonObject tagResponse = dispatcher.dispatch(tagRequest, session).getAsJsonObject();			
			log.debug("tagResponse = " + tagResponse);
			String tagNameUUID = tagResponse.get("nameId").getAsString();
			log.debug("'Category' tag name has UUID = " + tagNameUUID);
			String tagValueUUID = tagResponse.get("valueId").getAsString();
			log.debug("'Earnings' tag value has UUID = " + tagValueUUID);
			
			
			
			//
			// now do a search for events using both a search expression and some search options
			//
			
			Request earningsEventsRequest = new Request("EventHandler.search");
			
			// the search string just says that it must have a tag of Category=Earnings
			String searchString = "T(" + tagNameUUID + ",EQ," + tagValueUUID + ")";
			earningsEventsRequest.setMethodParameter("searchString", searchString);
			
			// set some options for the search
			JsonObject searchOptions = new JsonObject();
			
			// filter just on this content set
			if (earningsContentSetID != null){
				JsonArray contentSetIds = new JsonArray();
				//List<JsonPrimitive> contentSetIds = new ArrayList<JsonPrimitive>();
				contentSetIds.add(new JsonPrimitive(earningsContentSetID));
				log.debug("contentSetIds = " + contentSetIds);
				searchOptions.add("contentSetIds", contentSetIds);
			}
			
			// offset and limit are used to page through large sets of results.
			searchOptions.addProperty("offset", 0); // only return a certain number of results at a time
			searchOptions.addProperty("limit", 50); // only return a certain number of results at a time
			
			// sort by expected start date/time
			searchOptions.addProperty("sortOrder", "BY_EXP_START_DATE");
			
			log.debug("searchOptions = " + searchOptions);

			// apply the search options
			earningsEventsRequest.setMethodParameter("searchOption", searchOptions);
			
			// dispatch the search
			JsonArray eventResponse = dispatcher.dispatch(earningsEventsRequest, session).getAsJsonArray(); 
			
			// print out the array of events (just their start/end time and name) and record them in an array for future use
			JsonObject[] events = new JsonObject[eventResponse.size()];
			for (int i = 0; i < eventResponse.size(); i++){
				events[i] = eventResponse.get(i).getAsJsonObject();
				log.debug("event[" + i + "] = " + events[i]);
			}
			
			
			//
			// Request definitions for the observables for each event, using the 'boxcar' dispatching pattern
			//

			// generate the requests from the array of events
			Request[] observableRequests = new Request[events.length];
			for (int i = 0; i < eventResponse.size(); i++){
				JsonObject event = events[i];
				String eventID = event.get("eventId").getAsString();
				observableRequests[i] = new Request("ObservableHandler.getObservablesForEvent");
				observableRequests[i] .setMethodParameter("eventId", eventID);
			}
			
			// dispatch the requests as a boxcar
			Response[] observableResponses = dispatcher.boxcarDispatch(observableRequests, session);
			
			//
			// print out the events plus their corresponding observations and write them to a file in CSV-like format
			//
			
			// open the file
			BufferedWriter csvFileWriter = new BufferedWriter(new FileWriter(eventDataFileName));
			
			// write the header row
			csvFileWriter.write("eventID,eventName,eventSeriesID,entities,tickers,legacyEntityIDs,expectedStart,expectedEnd,actualStart,actualEnd,observableID,timeseriesID,measure,period,unit,spec\n");
			
			// loop through the events
			for (int eventIndex = 0; eventIndex < eventResponse.size(); eventIndex++){
				JsonObject event = events[eventIndex];
				String eventID = event.get("eventId").getAsString();
				String eventName = MiscUtils.getString(event, "name", "");
				String eventSeriesID = MiscUtils.getString(event, "eventSeriesId", "");
				String expectedStart = MiscUtils.getString(event, "expectedStart", "");
				String expectedEnd = MiscUtils.getString(event, "expectedEnd", "");
				String actualStart = MiscUtils.getString(event, "actualStart", "");
				String actualEnd = MiscUtils.getString(event, "actualEnd", "");
				
				// look up the entity names
				Set<String> entityTags = getEntityTags(event);
				
				// look up the corresponding ticker synonyms
				Set<String> exchanges = new HashSet<String>();
				exchanges.add("NYSE");
				exchanges.add("NASDAQ");
				Set<String> entityTickers = getEntitySynonyms(dispatcher, session, event, exchanges);
				
				Set<String> entityLegacyIDs = getEntitySynonyms(dispatcher, session, event, "Selerity");
				
				
				JsonArray observables = observableResponses[eventIndex].getResult().getAsJsonArray();
				//log.info("observableResponses[" + i + "] = " + observables);
				if (observables.size() > 0){
					for (int observableIndex = 0; observableIndex < observables.size(); observableIndex++){
						JsonObject observable = observables.get(observableIndex).getAsJsonObject();
						String observableID = MiscUtils.getString(observable, "observableId", "");
						String timeseriesID = MiscUtils.getString(observable, "timeseriesId", "");
						String measure = MiscUtils.getString(observable, "measure", "");
						String period = MiscUtils.getString(observable, "period", "");
						String unit = MiscUtils.getString(observable, "unit", "");
						String spec = getObsSpecForObservable(dispatcher, session, observableID, earningsContentSetID);
						
						// write a row into the log file for this observable (with its associated event)
						writeLine(csvFileWriter, eventID, eventName, eventSeriesID, entityTags, entityTickers, entityLegacyIDs,
								expectedStart, expectedEnd, actualStart, actualEnd,
								observableID, timeseriesID, measure, period, unit, spec);
										
					}
				}
				else{
					// no observables, just log the event itself
					writeLine(csvFileWriter, eventID, eventName, eventSeriesID, entityTags, entityTickers, entityLegacyIDs,
							expectedStart, expectedEnd, actualStart, actualEnd);
				}
				
			}
			
			// close the CSV output file
			csvFileWriter.close();
			
			

		}
		catch (Exception ex) {
			log.error("caught exception " + ex, ex);
		}

	}
	
	/** Look up the tag summaries for this event and collect all the entity tags
	 * 
	 * @param event
	 * @return
	 */
	protected static Set<String> getEntityTags(JsonObject event){
		Set<String> entityTags = new HashSet<String>();
		JsonArray tagSummaries = event.get("tagSummaries").getAsJsonArray();
		for (int t = 0; t < tagSummaries.size(); t++){
			JsonObject tagSummary = tagSummaries.get(t).getAsJsonObject();
			String name = tagSummary.get("name").getAsString();
			String value = tagSummary.get("value").getAsString();
			log.debug("tag name = " + name + "; value = " + value);
			if (name.equalsIgnoreCase("entity")){
				entityTags.add(value);
			}
		}
		return entityTags;
	}
	
	/** Look up the synonyms for this event's entity tags for the given family and collect them
	 * 
	 * @param event
	 * @return
	 * @throws DispatchException 
	 */
	protected static Set<String> getEntitySynonyms(Dispatcher dispatcher, Session session, JsonObject event, String familyName) throws DispatchException{
		Set<String> synonyms = new HashSet<String>();
		JsonArray tagSummaries = event.get("tagSummaries").getAsJsonArray();
		for (int t = 0; t < tagSummaries.size(); t++){
			JsonObject tagSummary = tagSummaries.get(t).getAsJsonObject();
			String name = tagSummary.get("name").getAsString();
			String tagID = tagSummary.get("tagId").getAsString();
			log.debug("tag name = " + name + "; tagID = " + tagID);
			if (name.equalsIgnoreCase("entity")){
				String synonym = getSynonymForTag(dispatcher, session, tagID, familyName);
				synonyms.add(synonym);
			}
		}
		return synonyms;
	}
	
	/** Look up the synonyms for this event's entity tags for the given families and collect them
	 * 
	 * @param event
	 * @return
	 * @throws DispatchException 
	 */
	protected static Set<String> getEntitySynonyms(Dispatcher dispatcher, Session session, JsonObject event, Set<String> familyNames) throws DispatchException{
		Set<String> synonyms = new HashSet<String>();
		for (String familyName : familyNames){
			synonyms.addAll(getEntitySynonyms(dispatcher, session, event, familyName));
		}
		return synonyms;
	}

	
	/** Returns the observation specification for the given observable and content set
	 * 
	 * @param dispatcher
	 * @param session
	 * @param observableID
	 * @param contentSetID
	 * @return
	 * @throws DispatchException
	 */
	protected static String getObsSpecForObservable(Dispatcher dispatcher, Session session, String observableID, String contentSetID) throws DispatchException{
		// build the request
		Request specRequest = new Request("ObservationSpecHandler.getCurrentObservationSpecForObservable");
		specRequest.setMethodParameter("observableId", observableID);
		specRequest.setMethodParameter("contentSetId", contentSetID);
		
		// dispatch the request
		JsonElement specResponse = dispatcher.dispatch(specRequest, session);
		
		// check if there's a spec
		if ((specResponse == null) || (specResponse.isJsonNull())){
			return "";
		}
		
		// return the spec ID
		JsonObject spec = specResponse.getAsJsonObject();
		String newSpecID = MiscUtils.getString(spec, "observationSpecId", "");
		String oldSpecID = MiscUtils.getString(spec, "legacyId", "");
		return newSpecID + " (" + oldSpecID + ")";
	}
	
	protected static String getSynonymForTag(Dispatcher dispatcher, Session session, String tagID, String familyName) throws DispatchException{
		Request synRequest = new Request("SynonymHandler.findByTagIdAndFamily");
		synRequest.setMethodParameter("tagId", tagID);
		synRequest.setMethodParameter("familyName", familyName);
		
		try{
		
			// dispatch the request
			JsonElement synResponse = dispatcher.dispatch(synRequest, session);
			
			// check if there's a synonym
			if ((synResponse == null) || (synResponse.isJsonNull())){
				return "";
			}
			
			JsonObject spec = synResponse.getAsJsonObject();
			return MiscUtils.getString(spec, "synonym", "");
		}
		catch (DispatchException dx){
			log.error("caught " + dx + " while looking up sysnonyms, skipping");
		}
		return "";
	}

	
	/** Logs a row of just event data (no observable) to the output file
	 * 
	 * @param out
	 * @param eventID
	 * @param eventName
	 * @param eventSeriesID
	 * @param entities
	 * @param expectedStart
	 * @param expectedEnd
	 * @param actualStart
	 * @param actualEnd
	 * @throws IOException
	 */
	protected static void writeLine(Writer out, String eventID, String eventName, String eventSeriesID, Set<String> entities, Set<String> tickers, Set<String> legacyEntityIDs,
			String expectedStart, String expectedEnd, String actualStart, String actualEnd) throws IOException{
		writeLine(out, eventID, eventName, eventSeriesID, entities, tickers, legacyEntityIDs, expectedStart, expectedEnd, actualStart, actualEnd, "", "", "", "", "", "");
	}
	
	/** Logs a row for both the event and one of its observables to the output file
	 * 
	 * @param out
	 * @param eventID
	 * @param eventName
	 * @param eventSeriesID
	 * @param entities
	 * @param expectedStart
	 * @param expectedEnd
	 * @param actualStart
	 * @param actualEnd
	 * @param observableID
	 * @param timeseriesID
	 * @param measure
	 * @param period
	 * @param unit
	 * @param spec
	 * @throws IOException
	 */
	protected static void writeLine(Writer out, String eventID, String eventName, String eventSeriesID, Set<String> entities, Set<String> tickers, Set<String> legacyEntityIDs,
			String expectedStart, String expectedEnd, String actualStart, String actualEnd, 
			String observableID, String timeseriesID, String measure, String period, String unit, String spec) throws IOException{
		
		String entitiesStr = "";
		if (entities.size() > 0){
			entitiesStr = MiscUtils.listSomeElements(entities, 3);
		}
		
		String tickersStr = "";
		if (tickers.size() > 0){
			tickersStr = MiscUtils.listSomeElements(tickers, 3);
		}
		
		String legacyIDStr = "";
		if (legacyEntityIDs.size() > 0){
			legacyIDStr = MiscUtils.listSomeElements(legacyEntityIDs, 3);
		}
		
		String line = "\"" + eventID + "\"," 
			+ "\"" + eventName + "\","
			+ "\"" + eventSeriesID + "\","
			+ "\"" + entitiesStr + "\","
			+ "\"" + tickersStr + "\","
			+ "\"" + legacyIDStr + "\","
			+ "\"" + expectedStart + "\"," 
			+ "\"" + expectedEnd + "\"," 
			+ "\"" + actualStart + "\"," 
			+ "\"" + actualEnd + "\","
			+ "\"" + observableID + "\","
			+ "\"" + timeseriesID + "\","
			+ "\"" + measure + "\","
			+ "\"" + period + "\","
			+ "\"" + unit + "\","
			+ "\"" + spec + "\"";
		log.debug("writing: " + line);
		out.write(line + "\n");
	}
	
	

}
