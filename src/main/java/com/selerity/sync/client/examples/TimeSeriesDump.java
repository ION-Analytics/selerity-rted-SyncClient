package com.selerity.sync.client.examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
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
import com.selerity.sync.client.RhinoHTTPTransportFactory;
import com.selerity.sync.client.Session;
import com.selerity.sync.client.TransportFactory;

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
 * An example application which looks up information about the next observable in a timeseries
 * (and some related info from the event and the observation specification) and writes it to
 * a file.
 *
 */

public class TimeSeriesDump extends AbstractSyncClient{

	private static final Log log = LogFactory.getLog(TimeSeriesDump.class);	
	
	public static final String DEFAULT_FIELD_SEPARATOR = "^";
	
	private static final List<String> EMPTY_LIST = Collections.unmodifiableList(new ArrayList<String>(0));
	
	public TimeSeriesDump(TransportFactory transportFactory, String user, String password, String clientAppName) throws Exception{
		super(transportFactory, user, password, clientAppName);
	}
	
	/** Looks up a timeseries with the given UUID. 
	 * 
	 * @param session
	 * @param timeSeriesUUID
	 * @return
	 * @throws DispatchException
	 */
	public JsonObject getTimeSeriesByID(Session session, String timeSeriesUUID) throws DispatchException{
		// look up the time series
		Request timeseriesRequest = new Request("TimeSeriesHandler.findById");
		timeseriesRequest.setMethodParameter("id", timeSeriesUUID);
		JsonElement timeSeriesElem = dispatch(timeseriesRequest, session).getAsJsonObject();		
		if ((timeSeriesElem == null) || (timeSeriesElem.isJsonNull())){
			return null;
		}
		return timeSeriesElem.getAsJsonObject();
	}
	
	/** Returns the next observable for a given time series
	 * 
	 * @param session
	 * @param timeSeriesUUID
	 * @return
	 * @throws DispatchException
	 */
	public JsonObject getNextObservableInTimeSeries(Session session, String timeSeriesUUID) throws DispatchException{
		Request nextObservableRequest = new Request("ObservableHandler.getNextObservableInTimeSeries");
		nextObservableRequest.setMethodParameter("timeSeriesId", timeSeriesUUID);
		JsonElement nextObservableElem = dispatch(nextObservableRequest, session);
		if ((nextObservableElem == null) || (nextObservableElem.isJsonNull())){
			return null;
		}
		return nextObservableElem.getAsJsonObject();		
	}
	
	/** Returns an unmodifiable list of UUID's for the content set to which the current
	 *  user is entitled.
	 * 
	 * @param session
	 * @return
	 * @throws DispatchException
	 */
	public List<String> getEntitledContentSetUUIDs(Session session) throws DispatchException{
		Request contentSetRequest = new Request("ContentSetHandler.getContentSets");
		JsonElement contentSetListElem = dispatch(contentSetRequest, session);
		if ((contentSetListElem == null) || (contentSetListElem.isJsonNull())){
			return EMPTY_LIST;
		}
		List<String> contentSetUUIList = new ArrayList<String>();
		JsonArray contentSetArray = contentSetListElem.getAsJsonArray();
		for (int i = 0; i < contentSetArray.size(); i++){
			JsonObject contentSet = contentSetArray.get(i).getAsJsonObject();
			String name = contentSet.get("name").getAsString();
			String uuid = contentSet.get("contentSetId").getAsString();
			log.debug("found content set " + name + " with UUID: " + uuid);
			contentSetUUIList.add(uuid);
		}
		return Collections.unmodifiableList(contentSetUUIList); // make it unmodifiable so that it doesn't accidentally get changed later
	}
	
	/** Extracts a field object from an observation spec object based on the field name.
	 *  Returns null if no matching field name is found.  Match is case sensitive.
	 * 
	 * @param obsSpec
	 * @param desiredFieldName
	 * @return
	 */
	public JsonObject getFieldByName(JsonObject obsSpec, String desiredFieldName){
		JsonElement fieldsElem = obsSpec.get("fields");
		if ((fieldsElem == null) || (fieldsElem.isJsonNull())){
			return null;
		}
		JsonArray fields = fieldsElem.getAsJsonArray();
		for (int i = 0; i < fields.size(); i++){
			JsonObject field = fields.get(i).getAsJsonObject();
			String fieldName = MiscUtils.getString(field, "name", null);
			if (desiredFieldName.equals(fieldName)){
				return field;
			}
		}
		return null;
	}
	
	/** Returns the data type (as a string) for a given field object.
	 *  Throws NullPointerException if the field has no data type. 
	 * 
	 * @param field
	 * @return
	 */
	public String getDataTypeForField(JsonObject field){
		JsonElement datatypeElem = field.get("datatype");
		if ((datatypeElem == null) || (datatypeElem.isJsonNull())){
			throw new NullPointerException("field " + field + " had no datatype");
		}
		JsonObject datatype = datatypeElem.getAsJsonObject();
		return MiscUtils.getString(datatype, "name", null);
	}
	
	/** Returns the offset of the given field object.  Throws
	 *  NullPointerException if no offset is defined.
	 * 
	 * @param field
	 * @return
	 */
	public Integer getOffsetForField(JsonObject field){
		JsonElement offsetElem = field.get("offset");
		if ((offsetElem == null) || (offsetElem.isJsonNull())){
			throw new NullPointerException("field " + field + " had no offset");
		}
		return offsetElem.getAsInt();
	}
	
	/** 
	 * @param args
	 */
	public static void main(String[] args) {

		long startTime = System.currentTimeMillis();
		
		try {
			
			if (args.length < 6){
				System.err.println("arguments: host port user password timeSeriesUUID outputFileName {fieldSeparator}");
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
			String timeSeriesUUID = args[4];
			String outputFileName = args[5];
			
			String fieldSeparator = DEFAULT_FIELD_SEPARATOR;
			if (args.length > 6){
				fieldSeparator = args[6];
			}
			
			// timezone, hardcoded for now
			String timeZoneID = "UTC";
			
			// initialized the transport and method dispatcher
			TimeSeriesDump dumper = new TimeSeriesDump(new RhinoHTTPTransportFactory(host, port), user, password, "TimeSeriesDump");
			
			// start a session, use the same session for all requests
			Session session = dumper.startSession();
			
			
			// look up the time series
			JsonObject timeseries = dumper.getTimeSeriesByID(session, timeSeriesUUID);	
			if (timeseries == null){
				log.warn("could not find a time series with this UUID: " + timeSeriesUUID + ", quitting");
				dumper.closeSession(session);
				return;
			}
			String timeseriesName = timeseries.get("name").getAsString();
			log.debug("timeseries name = " + timeseriesName);
			
			
			// look up next observable in the series
			JsonObject nextObservable = dumper.getNextObservableInTimeSeries(session, timeSeriesUUID);
			log.debug("got next observable: " + nextObservable);
			
			// if there is no next observable then give up
			if (nextObservable == null){
				log.warn("found no subsequent observable in the time series with this UUID: " + timeSeriesUUID + ", quitting");
				dumper.closeSession(session);
				return;
			}
			
			// pull a bunch of data out of the observable
			String eventID = nextObservable.get("eventId").getAsString();
			String observableID = nextObservable.get("observableId").getAsString();
			String measure = nextObservable.get("measure").getAsString();
			String period = nextObservable.get("period").getAsString();
			log.debug("measure = " + measure);
			log.debug("period = " + period);
			
			
			// look up the content sets to which this user is entitled
			List<String> contentSetUUIDList = dumper.getEntitledContentSetUUIDs(session);
			if (contentSetUUIDList.size() < 1){
				log.warn("user " + user + " is not entitled to any content sets, quitting");
				dumper.closeSession(session);
				return;
			}
			
			BufferedWriter out = null; // only open the output file if there's something to write
			int specCount = 0;
			
			// Loop through the remaining sections by content set
			for (String contentSetUUID : contentSetUUIDList){
				
				// look up the spec for that observable
				Request obsSpecRequest = new Request("ObservationSpecHandler.getCurrentObservationSpecForObservable");
				obsSpecRequest.setMethodParameter("observableId", observableID);
				obsSpecRequest.setMethodParameter("contentSetId", contentSetUUID);
				JsonElement obsSpecElem = dumper.dispatch(obsSpecRequest, session);
				if ((obsSpecElem == null) || (obsSpecElem.isJsonNull())){
					log.warn("no observation specification available for content set UUID " + contentSetUUID + ", trying next one");
				}
				else{					
					JsonObject obsSpec = obsSpecElem.getAsJsonObject();		
					log.debug("obsSpec = " + obsSpec);
					// get the legacy obs spec id
					long legacyObsSpecID = obsSpec.get("legacyId").getAsLong(); 
					log.debug("legacyObsSpecID = " + legacyObsSpecID);
					
					// look up some offset and data type info for the measurement field
					JsonObject measurementField = dumper.getFieldByName(obsSpec, "Measurement");
					if (measurementField == null){
						log.error("couldn't find measurement field for spec " + legacyObsSpecID);
					}
					int measurementOffset = dumper.getOffsetForField(measurementField);
					String measurementDataType = dumper.getDataTypeForField(measurementField);
					
					// and get the offset for the observation status field
					JsonObject obsStatusField = dumper.getFieldByName(obsSpec, "ObservationStatus");
					if (obsStatusField == null){
						log.error("couldn't find observation status field for spec " + legacyObsSpecID);
					}
					int obsStatusOffset = dumper.getOffsetForField(obsStatusField);
					
					// look up the event for that observable
					Request eventRequest = new Request("EventHandler.findById");
					eventRequest.setMethodParameter("eventId", eventID);
					eventRequest.setMethodParameter("timeZoneId", timeZoneID);
					eventRequest.setMethodParameter("contentSetId", contentSetUUID);
					JsonObject event = dumper.dispatch(eventRequest, session).getAsJsonObject();		
					log.debug("event = " + event);
					
					// get the expected start time of the event
					String expectedStart = event.get("expectedStart").getAsString();
					log.debug("expectedStart = " + expectedStart);
					
					// now write out the results to a file
					if (out == null){
						// if it wasn't opened already then open it and write the header first
						out = new BufferedWriter(new FileWriter(outputFileName));
						out.write("expectedStartTime (" + timeZoneID + ")" 
								+ fieldSeparator + "measure" 
								+ fieldSeparator + "period" 
								+ fieldSeparator + "legacyObsSpecID" 
								 + fieldSeparator + "measurement offset" 
								 + fieldSeparator + "observation status offset" 
								 + fieldSeparator + "measurement data type\n");
					}
					out.write(expectedStart 
							+ fieldSeparator + measure 
							+ fieldSeparator + period 
							+ fieldSeparator + legacyObsSpecID
							+ fieldSeparator + measurementOffset
							+ fieldSeparator + obsStatusOffset
							+ fieldSeparator + measurementDataType
							+ "\n");
					specCount++;
				}
			
			}
			
			// close the output file if it was opened
			if (out != null){
				out.close();
			}
				
			// and close the session
			dumper.closeSession(session);
			
			long elapsedTime = System.currentTimeMillis() - startTime;
			log.info("found " + specCount + " specs for timeSeriesUUID " + timeSeriesUUID + " in " + ((double)elapsedTime / 1000.0) + " seconds");
			
		}
		catch (Exception ex) {
			log.error("caught exception " + ex, ex);
		}

	}
	
	
}
