package com.selerity.sync.client.examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.MalformedURLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonObject;
import com.selerity.sync.client.AbstractSyncClient;
import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.Request;
import com.selerity.sync.client.Session;

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
	
	public TimeSeriesDump(String host, int port, String user, String password, String clientAppName) throws MalformedURLException, DispatchException{
		super(host, port, user, password, clientAppName);
	}
	
	
	/** 
	 * @param args
	 */
	public static void main(String[] args) {


		try {
			
			if (args.length < 7){
				System.err.println("arguments: host port user password contentSetUUID timeSeriesUUID outputFileName");
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
			String contentSetUUID = args[4];
			String timeSeriesUUID = args[5];
			String outputFileName = args[6];
			
			// timezone, hardcoded for now
			String timeZoneID = "UTC";
			
			// initialized the transport and method dispatcher
			TimeSeriesDump dumper = new TimeSeriesDump(host, port, user, password, "TimeSeriesDump");
			
			Session session = dumper.startSession();
			
			// look up the time series
			Request timeseriesRequest = new Request("TimeSeriesHandler.findById");
			timeseriesRequest.setMethodParameter("id", timeSeriesUUID);
			JsonObject timeseries = dumper.dispatch(timeseriesRequest, session).getAsJsonObject();		
			log.debug("got timeseries: " + timeseries);
			
			String timeseriesName = timeseries.get("name").getAsString();
			log.debug("timeseries name = " + timeseriesName);
			
			// look up next observable in the series
			Request nextObservableRequest = new Request("ObservableHandler.getNextObservableInTimeSeries");
			nextObservableRequest.setMethodParameter("timeSeriesId", timeSeriesUUID);
			JsonObject nextObservable = dumper.dispatch(nextObservableRequest, session).getAsJsonObject();		
			log.debug("got next observable: " + nextObservable);
			
			// if there is no next observable then give up
			if ((nextObservable == null) || (nextObservable.isJsonNull())){
				log.warn("found no subsequent observable in the series, quitting");
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
			
			// look up the spec for that observable
			Request obsSpecRequest = new Request("ObservationSpecHandler.getCurrentObservationSpecForObservable");
			obsSpecRequest.setMethodParameter("observableId", observableID);
			obsSpecRequest.setMethodParameter("contentSetId", contentSetUUID);
			JsonObject obsSpec = dumper.dispatch(obsSpecRequest, session).getAsJsonObject();		
			log.debug("obsSpec = " + obsSpec);
			
			// if there is no obs spec then quit
			if ((obsSpec == null) || (obsSpec.isJsonNull())){
				log.warn("no observation specification available, quitting");
				dumper.closeSession(session);
				return;
			}
			
			// get the legacy obs spec id
			long legacyObsSpecID = obsSpec.get("legacyId").getAsLong(); 
			log.debug("legacyObsSpecID = " + legacyObsSpecID);
			
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
			BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));
			out.write("expectedStartTime (" + timeZoneID + "),measure,period,legacyObsSpecID\n");
			out.write(expectedStart + "," + measure + "," + period + "," + legacyObsSpecID + "\n");
			out.close();
			
			// and close the session
			dumper.closeSession(session);
			
		}
		catch (Exception ex) {
			log.error("caught exception " + ex, ex);
		}

	}
	
	
}
