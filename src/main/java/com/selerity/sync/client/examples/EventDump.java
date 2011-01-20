package com.selerity.sync.client.examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
 * An example application which prints out all events, organized by start time.
 * The format matches one of the old RDS extraction formats.
 * 
 */

public class EventDump  extends AbstractSyncClient{
	
	private static final Log log = LogFactory.getLog(EventDump.class);
	
	private static final int REQUEST_LIMIT = 100;
	
	private static final String SELERITY_CORPORATE_CONTENT_SET_UUID = "c2bf8528-9a84-1fa7-1d19-d298c5e02b06";
	private static final String UTC = "UTC";
	
	protected TagDump tagDump;

	public EventDump(String host, int port, String clientAppName) throws MalformedURLException, DispatchException{
		super(host, port, clientAppName);
		tagDump = new TagDump(host, port, clientAppName);
	}
	
	public void startSession(String user, String password) throws DispatchException{
		super.startSession(user, password);
		tagDump.startSession(user, password);
	}
	
	public void closeSession() throws DispatchException{
		super.closeSession();
		tagDump.closeSession();
	} 
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		long startTime = System.currentTimeMillis();
		int eventCount = 0;
		
		try {

			if (args.length < 5) {
				System.err.println("arguments: host port user password outputFileName {daysPast} {daysFuture}");
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
			
			// look up optional start time offset (in days)
			int daysPast = 10;  // defaults to 10
			if (args.length > 5){
				daysPast = Integer.parseInt(args[5]);
				log.debug("set daysPast to " + daysPast);
			}
			else{
				log.debug("defaulting daysPast to " + daysPast);
			}
			
			
			// look up optional end time offset (in days)
			int daysFuture = 30;
			if (args.length > 6){
				daysFuture = Integer.parseInt(args[6]);
				log.debug("set daysFuture to " + daysFuture);
			}
			else{
				log.debug("defaulted daysFuture to " + daysFuture);
			}
			
			
			
			// initialize the dumper
			EventDump dumper = new EventDump(host, port, "EventDump");
			dumper.startSession(user, password);

			// open file
			BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));

			// write out events
			eventCount = dumper.writeEarningsEventsToFile(out, daysPast, daysFuture);
			
			// close file and session
			out.close();
			dumper.closeSession();
			
			log.debug("all done");

		}
		catch (Exception ex) {
			log.error("caught exception " + ex, ex);
		}
		
		double elapsedSeconds = ((double)(System.currentTimeMillis() - startTime)) / 1000d;
		log.info("wrote out " + eventCount + " events in " + elapsedSeconds + " seconds");
	}
	
	/* Note: the following is an example of the legacy file format:
	 
"DateTime (EST)","Entity","Entity ID","Event Subject","Event Description","Event ID"
"Jan 11, 2011 6:00:00 AM","Lennar Corp ","C.NYSE.LEN.200901","Company Earnings Release","Lennar Corp Earnings Release FQ-2010-Q4","3581"
"Jan 11, 2011 8:00:00 AM","SUPERVALU Inc","C.NYSE.SVU.200901","Company Earnings Release","SUPERVALU Inc Earnings Release FQ-2011-Q3","3582"
"Jan 12, 2011 10:30:00 AM","US Department of Energy","G.US.FED.DOE","Economic Indicator Release","DOE Weekly Inventories - Petroleum WE-4W-2011-JAN-07","3570"
"Jan 12, 2011 4:00:00 PM","People's United Financial Inc","C.NASDAQ.PBCT.200901","Earnings Preannouncement","People's United Financial Inc Financial Outlook 3Q2010","2469"
"Jan 12, 2011 4:15:00 PM","Advanced Micro Devices Inc","C.NYSE.AMD.200901","Earnings Preannouncement","Advanced Micro Devices Inc Financial Outlook 3Q2010","2536"

	 
	 */
	
	/** Dumps all events in the Selerity corporate content set which are earnings releases out to an event file that matches the 
	 *  old RDS format.
	 * 
	 */
	public int writeEarningsEventsToFile(Writer out, int daysPast, int daysFuture) throws DispatchException, IOException, ParseException{
		
		// we only care about corporate earnings
		Map<String,String> categoryMap = new HashMap<String,String>();
		categoryMap.put("Earnings", "Company Earnings Release");
		
		// get a map of all tags to their synonyns (by family)
		SortedMap<String,SortedMap<String, String>> tagSynonymMap = tagDump.getTagSynonymMap("entity", REQUEST_LIMIT);
		
		// the format for the legacy date/time values
		DateFormat legacyDateFormat = new SimpleDateFormat("MMM d, yyyy h:mm:ss a");
		TimeZone timeZone = TimeZone.getTimeZone("America/New_York");
		legacyDateFormat.setTimeZone(timeZone);
		
		return writeEventsToFile(out, SELERITY_CORPORATE_CONTENT_SET_UUID, categoryMap, tagSynonymMap, legacyDateFormat, daysPast, daysFuture);
	}
	
	/** Dumps all events in the given content set out to a CSV formatted file using the given maps for transforming categories and entity tags.
	 * 
	 * @param out
	 * @param contentSetUUID
	 * @param categoryMap
	 * @param tagSynonymMap
	 * @param dateFormat
	 * @param startTime
	 * @param endTime
	 * @return
	 * @throws DispatchException
	 * @throws IOException
	 * @throws ParseException
	 */
	public int writeEventsToFile(Writer out, String contentSetUUID, Map<String,String> categoryMap, SortedMap<String,SortedMap<String, String>> tagSynonymMap,
			DateFormat dateFormat, int daysPast, int daysFuture) throws DispatchException, IOException, ParseException{
		
		int eventCount = 0;
		
		// write the header
		out.write("\"DateTime (EST)\",\"Entity\",\"Entity ID\",\"Event Subject\",\"Event Description\",\"Event ID\"\n");
		
		// compute the time range
		long now = MiscUtils.getNanoTime();
		long startTime = now - daysPast * MiscUtils.NANOS_PER_DAY;
		long endTime = now + daysFuture * MiscUtils.NANOS_PER_DAY;
		
		String startTimeStr = MiscUtils.formatNanoTime(startTime);
		String endTimeStr = MiscUtils.formatNanoTime(endTime);
		log.info("searching for events starting between " + startTimeStr + " and " + endTimeStr);
	
		// now look up events for each content set
		List<JsonObject> events = getEventsForContentSet(contentSetUUID, startTimeStr, endTimeStr, REQUEST_LIMIT, UTC);
		log.debug("found " + events.size() + " events");
		
		// loop through each event
		for (JsonObject event : events){
			
			// look up the selerity entity ID from the primary entity tag's value
			String primaryEntityName = getPrimaryEntityForEvent(event);		
			String selerityEntityID = getSynonym(primaryEntityName, tagSynonymMap, "Selerity");
			if (selerityEntityID != null){  // only continue if we found a selerity ID
				
				// map the new category name to the legacy category name
				String primaryCategory = getPrimaryCategoryForEvent(event); 
				String renamedCategory = categoryMap.get(primaryCategory);
				if (renamedCategory != null){ // only continue if we have a mapping for this category
					
					// look up start time and convert it to the new format
					String expectedStart = MiscUtils.getString(event,"expectedStart","");
					Date date = new Date(MiscUtils.parseNanoTime(expectedStart) / MiscUtils.NANOS_PER_MILLISECOND); 
					String startString = dateFormat.format(date);
					log.debug("reformatted start time: " + expectedStart + " to: " + startString);
				
					// get the event name
					String eventName = MiscUtils.getString(event,"name","");
					
					// write it out
					out.write("\"" 
							+ startString + "\",\"" 
							+ primaryEntityName + "\",\"" 
							+ selerityEntityID + "\",\"" 
							+ renamedCategory + "\",\"" 
							+ eventName + "\",\"" 
							+ eventCount + "\"\n");   // using eventCount as a placeholder for the (unused) legacy event ID
					
					// keep track of how many we actually wrote out
					eventCount++;
				}
				else{
					log.debug("skipped event for category \"" + primaryCategory + "\" because it isn't one of the categories we care about");
				}
			}
			else{
				log.debug("skipped event for entity \"" + primaryEntityName + "\" because it doesn't have a Selerity legacy ID");
			}
			
		}
		
		return eventCount;
	}
	
	/** Returns the synonym of the given family for this value if any exists, otherwise null.
	 * 
	 * @param value
	 * @param tagSynonymMap
	 * @param familyName
	 * @return
	 */
	protected String getSynonym(String value, SortedMap<String,SortedMap<String, String>> tagSynonymMap, String familyName){
		Map<String,String> synonymMap = tagSynonymMap.get(value);
		if (synonymMap == null){
			return null;
		}
		return synonymMap.get(familyName);
	}
	
	/** Returns a list of content set UUID's to which the user is entitled and which refer to
	 *  non-trivial Selerity event content sets.
	 * 
	 * @return
	 * @throws DispatchException
	 */
	protected List<String> getEntitledEventContentSetUUIDs() throws DispatchException{
		// look up all of the content sets to which this user is entitled
		Request contentSetRequest = new Request("ContentSetHandler.getContentSets");
		JsonArray contentSetResponse = dispatch(contentSetRequest).getAsJsonArray(); 
		log.debug("contentSetResponse = " + contentSetResponse);
		
		List<String> contentSetUUIDs = new ArrayList<String>();
		
		// print out the content sets
		for (int i = 0; i < contentSetResponse.size(); i++){
			JsonObject contentSet = contentSetResponse.get(i).getAsJsonObject();
			String contentSetName = contentSet.get("name").getAsString();
			String contentSetUUID = contentSet.get("contentSetId").getAsString();
			log.debug("content set[" + i + "] = " + contentSetName + " (" + contentSetUUID + ")");
			if (!contentSetName.equals("Public")){
				contentSetUUIDs.add(contentSetUUID);
			}
		}
		
		return contentSetUUIDs;
	}
	
	/** Returns all events for the given content set within the given start and end time.  The events will be queried in chunks set by the limit.
	 * 
	 * @param contentSetUUID
	 * @param startTime
	 * @param endTime
	 * @param limit
	 * @return
	 * @throws DispatchException
	 */
	protected List<JsonObject> getEventsForContentSet(String contentSetUUID, String startTime, String endTime, int limit, String timezone) throws DispatchException{
	
		List<JsonObject> events = new ArrayList<JsonObject>();
		int offset = 0;
		
		while (true){
			List<JsonObject> eventsChunk = getEventsForContentSet(contentSetUUID, startTime, endTime, offset, limit, timezone);
			events.addAll(eventsChunk);
			if (eventsChunk.size() < limit){
				// we must have reached the end of the set
				return events;
			}
			// otherwise, increment the offset and keep going
			offset += limit;
		}
		
	}
	
	/** Queries a chunk of events based on the given offset and limit.
	 * 
	 * @param contentSetUUID
	 * @param startTime
	 * @param endTime
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DispatchException
	 */
	protected List<JsonObject> getEventsForContentSet(String contentSetUUID, String startTime, String endTime, int offset, int limit, String timezone) throws DispatchException{
		
		List<JsonObject> events = new ArrayList<JsonObject>();
		
		Request earningsEventsRequest = new Request("EventHandler.search");
		
		// a search string that should match all events
		String searchString = "AND()";
		earningsEventsRequest.setMethodParameter("searchString", searchString);
		
		// set some options for the search
		JsonObject searchOptions = new JsonObject();
		
		// filter just on this content set
		JsonArray contentSetIds = new JsonArray();
		contentSetIds.add(new JsonPrimitive(contentSetUUID));
		searchOptions.add("contentSetIds", contentSetIds);
		
		// limit to events which fall into this time range (expressed in UTC)
		searchOptions.addProperty("timeIntervalStart", startTime); 
		searchOptions.addProperty("timeIntervalEnd", endTime); 
		searchOptions.addProperty("timeIntervalTimeZoneId", timezone); 
		
		// offset and limit are used to page through large sets of results.
		searchOptions.addProperty("offset", offset); 
		searchOptions.addProperty("limit", limit); 
		
		// sort by expected start date/time
		searchOptions.addProperty("sortOrder", "BY_EXP_START_DATE");

		// apply the search options
		earningsEventsRequest.setMethodParameter("searchOption", searchOptions);
		
		// dispatch the search
		JsonArray eventResponse = dispatch(earningsEventsRequest).getAsJsonArray(); 
		
		// print out the array of events and record them in an array for future use
		for (int i = 0; i < eventResponse.size(); i++){
			JsonObject event = eventResponse.get(i).getAsJsonObject();
			events.add(event);
			log.debug("event[" + i + "] = " + event);
		}
		
		return events;
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
	
	protected String getPrimaryCategoryForEvent(JsonObject event){
		double maxScore = Double.MIN_VALUE;
		String maxValue = null;
		JsonArray tags = event.get("tagSummaries").getAsJsonArray();
		for (int i = 0; i < tags.size(); i++){
			JsonObject tag = tags.get(i).getAsJsonObject();
			String name = tag.get("name").getAsString();
			if (name.equalsIgnoreCase("Category")){
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
