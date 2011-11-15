package com.selerity.sync.client.examples;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.selerity.sync.client.AbstractNarwhalClient;
import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.FullRequest;
import com.selerity.sync.client.MiscUtils;
import com.selerity.sync.client.NarwhalMetaServiceImpl;
import com.selerity.sync.client.NarwhalService;
import com.selerity.sync.client.Request;
import com.selerity.sync.client.Response;
import com.selerity.sync.client.Session;
import com.selerity.sync.client.async.NarwhalResponseAdapter;
import com.selerity.sync.client.async.StreamedResponseListener;
import com.selerity.sync.client.examples.refdata.ObservationMetaData;
import com.selerity.sync.client.examples.refdata.ObservationMetaDataCacheFactory;
import com.selerity.sync.client.examples.refdata.Tag;
import com.selerity.sync.client.examples.refdata.TagCache;
import com.selerity.sync.client.examples.refdata.TagCacheFactory;

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
 * An example application which subscribes to streaming observations and logs
 * them, along with meta data, to a CSV file.
 * 
 * This application is primaily useful as a debugging example.  In the case
 * of application like trading where fast decisions needed to be made, the
 * design would need to change significantly - most importantly by pre-loading
 * the reference data for specific target observables and setting up the mappings
 * ahead of time.
 * 
 */

public class StreamSubscriber extends AbstractNarwhalClient implements StreamedResponseListener {
	
	private static final Log log = LogFactory.getLog(StreamSubscriber.class);

	public static final int RESPONSE_QUEUE_SIZE = 1000;
	
	private final Gson gson;
	
	protected final Writer outputWriter;
	
	protected final TagCacheFactory tagCacheFactory;
	protected final ObservationMetaDataCacheFactory metaDataFactory;
	
	// a simple record of a response and its arrival timestamp
	protected static final class ResponseQueueEntry {
		protected final Response response;
		protected final long localReceiveTimeNanos;
		public ResponseQueueEntry(Response response, long localReceiveTimeNanos) {
			this.response = response;
			this.localReceiveTimeNanos = localReceiveTimeNanos;
		}
		
		public Response getResponse(){
			return this.response;
		}
		
		public long getLocalReceiveTimeNanos(){
			return this.localReceiveTimeNanos;
		}
		
	}
	
	// use a queue to temporarily hold responses until we have time to process them
	protected final BlockingQueue<ResponseQueueEntry> responseQueue = new ArrayBlockingQueue<ResponseQueueEntry>(RESPONSE_QUEUE_SIZE);
	
	/**
	 * Initialize the subscriber based on the SeleritySync API endpoints wrapped in the
	 * NarwhalService instance.  Writes output to the outputWriter in addition to
	 * the log.
	 * 
	 * @param service
	 * @param user
	 * @param password
	 * @param clientAppName
	 * @param outputWriter
	 * @throws Exception
	 */
	public StreamSubscriber(NarwhalService service, String user,
			String password, String clientAppName, Writer outputWriter) throws Exception {
		super(service, user, password, clientAppName);
		GsonBuilder builder = new GsonBuilder().serializeNulls();
		builder.registerTypeAdapter(Response.class, new Response.ResponseSerializer());
		builder.registerTypeAdapter(Response.class, new Response.ResponseDeserializer());
		gson = builder.create();
		this.outputWriter = outputWriter;
		this.tagCacheFactory = new TagCacheFactory(service, user, password, clientAppName);
		this.metaDataFactory = new ObservationMetaDataCacheFactory(service, user, password, clientAppName);
	}

	/**
	 *  Called whenever a response comes in for the subscribe method.
	 *  Quickly timestamps and enqueues the response for later processing.
	 */
	public void onResponse(Response response){
		// approximate value for local arrival time (doesn't take into account the local network stack or deserialization)
		long localReceiveTimeNanos = MiscUtils.getNanoTime();
		if (log.isDebugEnabled()){
			log.debug("got response: " + gson.toJson(response));
		}
		
		// add to the queue for later processing -- note that this will block
		if (!responseQueue.offer(new ResponseQueueEntry(response, localReceiveTimeNanos))){
			log.error("processing queue is full, discarding response: " + gson.toJson(response));
		}
	}
	
	/**
	 * Called by the processing thread - looks up reference data associated with this
	 * observation and writes it, along with the real-time data included in the message,
	 * into the output file.
	 * 
	 * @param response
	 * @throws ParseException 
	 * @throws DispatchException 
	 * @throws IOException 
	 */
	protected void processResponse(ResponseQueueEntry responseQueueEntry) throws ParseException, DispatchException, IOException{
		long currentTimeNanos = MiscUtils.getNanoTime();
		Response response = responseQueueEntry.getResponse();
		
		// check local receive time		
		long localReceiveTimestampNanos = responseQueueEntry.getLocalReceiveTimeNanos();
		if (log.isDebugEnabled()){
			long elapsedNanosSinceReceive = currentTimeNanos - localReceiveTimestampNanos;
			log.debug("processing currently lagging " + (elapsedNanosSinceReceive / 1000000.0) + " ms behind local receive");
		}
		String localReceiveTimestampStr = MiscUtils.formatNanoTime(localReceiveTimestampNanos);
		
		// parse the observation message base
		JsonElement result = response.getResult();
		if ((result == null) || (result.isJsonNull())){
			log.warn("got response that had not result: " + gson.toJson(response));
			return;
		}
		JsonObject obsObj = result.getAsJsonObject();
		long swordfishObsSpecID = MiscUtils.getLong(obsObj, "swordfishObsSpecID", 0);
		String proxyReceiveTimeString = MiscUtils.getString(obsObj, "proxyReceiveTime", null);
		if (log.isDebugEnabled()){
			long proxyReceiveTimeNanos = MiscUtils.parseNanoTime(proxyReceiveTimeString);
			long elapsedNanosSinceProxy = localReceiveTimestampNanos - proxyReceiveTimeNanos;
			log.debug("local receive currently lagging " + (elapsedNanosSinceProxy / 1000000.0) + " ms behind internet publication");
		}
		
		// parse the observation message fields
		JsonObject fields = MiscUtils.getJsonObject(obsObj, "fields", null);
		if (fields == null){
			log.error("observation missing fields!");
			return;
		}
		String measurement = MiscUtils.getString(fields, "Measurement", null);
		String algorithmId = MiscUtils.getString(fields, "AlgorithmID", null);
		String environmentLevel = MiscUtils.getString(fields, "EnvironmentLevel", null);
		String observationStatus = MiscUtils.getString(fields, "ObservationStatus", null);
		
		// note, not every observations will have the following - they're only used (today) for unscheduled events.
		long observationID = MiscUtils.getLong(fields, "ObservationID", 0);
		long correlationID = MiscUtils.getLong(fields, "CorrelationID", 0);
		
		String observationTimestampStr = MiscUtils.getString(fields, "ObservationTimestamp", null);
		if (log.isDebugEnabled()){
			long observationTimestampNanos = MiscUtils.parseNanoTime(observationTimestampStr);
			long elapsedNanosSinceObservation = localReceiveTimestampNanos - observationTimestampNanos;
			log.debug("local receive currently lagging " + (elapsedNanosSinceObservation / 1000000.0) + " ms behind multicast publication");
		}
		
		// now look up referenced meta data
		ObservationMetaData metaData = metaDataFactory.getInstance().getObservationMetaDataForSwordfishID(swordfishObsSpecID);
		TagCache tagCache = tagCacheFactory.getInstance();
		
		String entityIDs = getEntityIDString(metaData.getTags(), tagCache);
		
		// now write out the record
		outputWriter.write("\"" + localReceiveTimestampStr + "\","
				+ "\"" + proxyReceiveTimeString + "\","
				+ "\"" + observationTimestampStr + "\","
				+ "\"" + swordfishObsSpecID + "\","
				+ "\"" + metaData.getEventseriesUUID() + "\","
				+ "\"" + metaData.getTimeseriesUUID() + "\","
				+ "\"" + metaData.getEventUUID() + "\","
				+ "\"" + metaData.getObservableUUID() + "\","
				+ "\"" + observationID + "\","
				+ "\"" + correlationID + "\","
				+ "\"" + metaData.getEventName() + "\","
				+ "\"" + entityIDs + "\","
				+ "\"" + metaData.getMeasure() + "\","
				+ "\"" + metaData.getPeriod() + "\","
				+ "\"" + metaData.getPeriodRelativity() + "\","
				+ "\"" + environmentLevel + "\","
				+ "\"" + observationStatus + "\","
				+ "\"" + algorithmId + "\","
				+ "\"" + measurement + "\"\n");
		
		outputWriter.flush();
		
		if (log.isDebugEnabled()){
			long elapsedProcessingNanos = MiscUtils.getNanoTime() - currentTimeNanos;
			log.debug("processed observation for swordfish obs spec ID " + swordfishObsSpecID + " in " + (elapsedProcessingNanos / 1000000) + " ms");
		}		
	}
	
	/**
	 * Converts a set of tags (which may or may not contain any entity tags) into
	 * a string of Selerity Entity ID's separated by semi colons.  If Selerity Entity ID's
	 * aren't available for a given entity then it's canonical name will be used.
	 * 
	 * @param tags
	 * @param tagCache
	 * @return
	 */
	protected String getEntityIDString(Set<Tag> tags, TagCache tagCache){
		// first, create a set of selerity entity ID's
		Set<String> selerityEntityIDs = new HashSet<String>();
		for (Tag tag : tags){
			if (tag.getName().equals("Entity")){
				Set<String> synonyms = tag.getSynonyms("Selerity");
				if (synonyms.size() > 0){
					selerityEntityIDs.addAll(synonyms);
				}
				else{
					selerityEntityIDs.add(tag.getValue());
				}
			}
		}
		
		// now, for each entity ID, add it to the string, separated by semicolons
		StringBuffer s = new StringBuffer();
		boolean first = true;
		for (String entityID : selerityEntityIDs){
			if (first){
				first = false;
			}
			else{
				s.append(';');
			}
			s.append(entityID);
		}
	
		return s.toString();
	}

	/**
	 * Initiates a subscription to the streaming observation service of the SeleritySync API.
	 * 
	 * Starts a thread to process responses asynchronously.
	 * 
	 * @throws Exception
	 */
	public void subscribe() throws Exception{

		Session session = startSession();
		
		
		Request partialRequest = new Request("ObservationHandler.subscribe");
		partialRequest.setMethodParameter("conflationMode", "SPEC_MEASUREMENT_STATUS_ALGORITHM");
		FullRequest fullRequest = new FullRequest(partialRequest, session, UUID.randomUUID().toString());
		log.info("dispatching subscription request");
		JsonReader reader = dispatch(fullRequest);
		reader.setLenient(true);
		NarwhalResponseAdapter adapter = new NarwhalResponseAdapter(reader, this);
		Thread th = new Thread(adapter, "sub_adapter");
		th.setDaemon(false);
		th.start();
		log.info("subscription thread started");
		
		outputWriter.write("\"localReceiveTimestamp\","
				+ "\"proxyReceiveTimestamp\","
				+ "\"observationTimestamp\","
				+ "\"SwordfishObsSpecID\","
				+ "\"EventSeriesUUID\","
				+ "\"TimeSeriesUUID\","
				+ "\"EventUUID\","
				+ "\"ObservableUUID\","
				+ "\"ObservationID\","
				+ "\"CorrelationID\","
				+ "\"EventName\","
				+ "\"Entity\","
				+ "\"MeasureCode\","
				+ "\"Period\","
				+ "\"PeriodRelativity\","
				+ "\"EnvironmentLevel\","
				+ "\"ObservationStatus\","
				+ "\"Algorithm\","
				+ "\"Measurement\"\n");
		
		outputWriter.flush();
		
		// now start a thread to process the response queue asynchronously
		final Thread responseProcessorThread = new Thread("response_processor"){
			public void run(){
				while (true){
					try{
						ResponseQueueEntry responseQueueEntry = responseQueue.take();
						processResponse(responseQueueEntry);
					}
					catch (Exception ex){
						log.error("caught " + ex + " in response processing loop, skipping", ex);
					}
				}
			}
		};
		
		responseProcessorThread.setDaemon(true);
		responseProcessorThread.start();
		log.info("response processing thread started");
		
	}

	public static void main(String[] args){
		try {

			if (args.length < 4) {
				System.err.println("arguments: urls user password outputFileName");
				System.exit(1);
			}

			// read in the arguments
			String urls = args[0];
			String user = args[1];
			String password = args[2];
			String outputFileName = args[3];
			
			NarwhalService service = new NarwhalMetaServiceImpl(urls);
			
			Writer outputWriter = new FileWriter(outputFileName);
			
			StreamSubscriber subscriber = new StreamSubscriber(service, user, password, "StreamSubscriber", outputWriter);
			
			subscriber.subscribe();
			log.info("subscribed to " + urls + " as user " + user);
			
		}
		catch (Exception ex){
			log.error("caught exception " + ex + " in main loop, exiting", ex);
		}
	}
	
}
