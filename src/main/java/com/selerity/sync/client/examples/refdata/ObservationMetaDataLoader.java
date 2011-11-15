package com.selerity.sync.client.examples.refdata;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.selerity.sync.client.AbstractNarwhalClient;
import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.MiscUtils;
import com.selerity.sync.client.NarwhalService;
import com.selerity.sync.client.Request;
import com.selerity.sync.client.Session;

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
 * Implements functionality to load ObservationMetaData from the SeleritySync API.
 * 
 */

public class ObservationMetaDataLoader extends AbstractNarwhalClient{
	
	private static final Log log = LogFactory.getLog (ObservationMetaDataLoader.class);

	protected final TagCacheFactory tagCacheFactory;
	
	public ObservationMetaDataLoader(NarwhalService service, String user,
			String password, String clientAppName, TagCacheFactory tagCacheFactory) throws Exception {
		super(service, user, password, clientAppName);
		this.tagCacheFactory = tagCacheFactory;
	}
	
	
	protected ObservationMetaData load(long swordfishObsSpecID) throws DispatchException{
		Session session = startSession();
		
		long startTimeMillis = System.currentTimeMillis();
		log.debug("loading obs meta data for obs spec with swordfish ID " + swordfishObsSpecID);
		Request obsSpecRequest = new Request("ObservationSpecHandler.getObsSpecForLegacyId");
		obsSpecRequest.setMethodParameter("legacyId", swordfishObsSpecID);
		JsonElement obsSpecElem = dispatch(obsSpecRequest, session);
		if ((obsSpecElem == null) || (obsSpecElem.isJsonNull())){
			log.debug("got no result for obs spec with swordfish ID " + swordfishObsSpecID);
			return null;
		}
		ObservationMetaData metaData = getByObservationSpec(session, obsSpecElem.getAsJsonObject());

		long elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis;
		double elapsedTimeSeconds = (double) elapsedTimeMillis / 1000.0;
		log.debug("loaded obs meta data for obs spec with swordfish ID " + swordfishObsSpecID + " in " + elapsedTimeSeconds + " seconds");
	
		return metaData;
	}
	
	public ObservationMetaData getByObservationSpec(Session session, JsonObject obsSpec) throws DispatchException{
		String observableID = MiscUtils.getString(obsSpec, "observableId", null);
		String timeseriesID = MiscUtils.getString(obsSpec, "timeseriesId", null);
		Set<Tag> tags = new HashSet<Tag>();
		TagCache tagCache = tagCacheFactory.getInstance();
		
		// some variables that might get set later
		String eventID = null;
		String eventseriesID = null;
		String measure = null;
		String dataType = null;
		String period = null;
		String periodRelativity = null;
		long scale = -1;
		String unit = null;
		
		String eventName = null;
		String timeseriesName = null;
		String eventseriesName = null;
		
		// some variables directly from the obsSpec
		String observationSpecUUID = MiscUtils.getString(obsSpec, "observationSpecId", null);
		long swordfishObsSpecID = MiscUtils.getLong(obsSpec, "legacyId", -1);
		
		
		// if this spec has an associated observable then look up some info based on it
		if (observableID != null){
			JsonObject observable = getObservable(session, observableID);
			if (observable != null){				
				eventID = MiscUtils.getString(observable, "eventId", eventID);
				
				measure = MiscUtils.getString(observable, "measure", measure);
				period = MiscUtils.getString(observable, "period", period);				
				scale = MiscUtils.getLong(observable, "scale", scale);
				unit = MiscUtils.getString(observable, "unit", unit);
				JsonElement dataTypeElem = observable.get("observationFieldDatatype");
				if ((dataTypeElem != null) && (!dataTypeElem.isJsonNull())){
					JsonObject dataTypeObj = dataTypeElem.getAsJsonObject();
					dataType = MiscUtils.getString(dataTypeObj, "name", dataType);
				}
				
				// timeseries on the obs spec is higher precedence than the timeseries on the observable (but they ought to be the same!!)
				String observableTimeSeriesID = MiscUtils.getString(observable, "timeSeriesId", null);
				if ((observableTimeSeriesID != null) && (timeseriesID != null)){
					if (!observableTimeSeriesID.equals(timeseriesID)){
						log.error("inconsistent timeseries ID's: found " + timeseriesID + " on observation spec " + observationSpecUUID
								+ " but found " + observableTimeSeriesID + " on observable " + observableID);
					}
				}
				if (timeseriesID == null){
					timeseriesID = observableTimeSeriesID;  // the observable timeseries should only be used if the obs spec timeseries isn't present
				}
				
				// tags on the observable
				JsonElement tagsElem = observable.get("tags");
				if ((tagsElem != null) && (!tagsElem.isJsonNull())){
					JsonArray tagsArray = tagsElem.getAsJsonArray();
					for (int i = 0; i < tagsArray.size(); i++){
						JsonObject tagJson = tagsArray.get(i).getAsJsonObject();
						String tagId = MiscUtils.getString(tagJson, "tagId", null);
						Tag tag = tagCache.getTagByTagId(tagId);						
						log.debug("found tag for observable " + observableID + " of " + tag);
						tags.add(tag);
					}
				}
			}
		}
		
		// if this spec has an event then look up some info based on it
		if (eventID != null){
			JsonObject event = getEvent(session, eventID);
			if (event != null){
				eventName = MiscUtils.getString(event, "name", null);
				eventseriesID = MiscUtils.getString(event, "eventSeriesId", eventseriesID);
				// note - we don't get the event's period since event period and observable period can be different
				
				// tags on the event
				JsonElement tagsElem = event.get("tagSummaries");
				if ((tagsElem != null) && (!tagsElem.isJsonNull())){
					JsonArray tagsArray = tagsElem.getAsJsonArray();
					for (int i = 0; i < tagsArray.size(); i++){
						JsonObject tagJson = tagsArray.get(i).getAsJsonObject();
						String tagId = MiscUtils.getString(tagJson, "tagId", null);
						Tag tag = tagCache.getTagByTagId(tagId);	
						log.debug("found tag for event " + eventName + " (" + eventID + "): " + tag.getName() + " = " + tag);
						tags.add(tag);
					}
				}
			}
		}
		
		// if this spec has a time series then look up some info based on it
		if (timeseriesID != null){
			JsonObject timeseries = getTimeSeries(session, timeseriesID);
			if (timeseries != null){
				eventseriesID = MiscUtils.getString(timeseries, "eventSeriesId", eventseriesID);
				timeseriesName = MiscUtils.getString(timeseries, "name", null);
				String measureTagUUID = MiscUtils.getString(timeseries, "measureTagId", null);
				if (measureTagUUID != null){
					Tag measureTag = tagCache.getTagByTagId(measureTagUUID);
					if (measureTag != null){
						if (measure == null){
							measure = measureTag.getValue();
						}
						log.debug("found measure tag for timeseries " + timeseriesName + " (" + timeseriesID + "): " + measureTag);
						tags.add(measureTag);
					}
				}
				periodRelativity = MiscUtils.getString(timeseries, "periodRelativity", periodRelativity);
				if (scale < 0){
					scale = MiscUtils.getLong(timeseries, "scale", scale);
				}
				String unitTagUUID = MiscUtils.getString(timeseries, "unitTagId", null);
				if (unitTagUUID != null){
					Tag unitTag = tagCache.getTagByTagId(unitTagUUID);
					if (unitTag != null){
						if (unit == null){
							unit = unitTag.getValue();
						}
						log.debug("found unit tag for timeseries " + timeseriesName + " (" + timeseriesID + "): " + unitTag);
						tags.add(unitTag);
					}
				}
			}
		}
		
		// if this spec has an event series then look up some info based on it
		if (eventseriesID != null){
			JsonObject eventseries = getEventSeries(session, eventseriesID);
			if (eventseries != null){
				eventseriesName = MiscUtils.getString(eventseries, "name", null);
				
				// tags on the event series
				JsonElement tagsElem = eventseries.get("tags");
				if ((tagsElem != null) && (!tagsElem.isJsonNull())){
					JsonArray tagsArray = tagsElem.getAsJsonArray();
					for (int i = 0; i < tagsArray.size(); i++){
						JsonObject tagJson = tagsArray.get(i).getAsJsonObject();
						String tagId = MiscUtils.getString(tagJson, "tagId", null);
						Tag tag = tagCache.getTagByTagId(tagId);
						log.debug("found tag for event series " + eventseriesName + " (" + eventseriesID + "): " + tag);
						tags.add(tag);
					}
				}
			}
			
		}
		
		return new ObservationMetaData(observationSpecUUID, swordfishObsSpecID, observableID, timeseriesID, eventID, eventseriesID, eventName, timeseriesName,
				eventseriesName, scale, measure, unit, period, periodRelativity, dataType, tags);

	}
	
	public JsonObject getObservable(Session session, String observableUUID) throws DispatchException{
		return getObjectByUUID(session, "observable", "ObservableHandler.findById", "observableId", observableUUID);
	}
	
	public JsonObject getTimeSeries(Session session, String timeseriesUUID) throws DispatchException{
		return getObjectByUUID(session, "timeseries", "TimeSeriesHandler.findById", "id", timeseriesUUID);
	}
	
	public JsonObject getEvent(Session session, String eventUUID) throws DispatchException{
		log.debug("loading event for eventId=" + eventUUID);
		Request request = new Request("EventHandler.findById");
		request.setMethodParameter("eventId", eventUUID);
		request.setMethodParameter("timeZoneId", (String)null);
		JsonElement objElem = dispatch(request, session);
		if ((objElem == null) || (objElem.isJsonNull())){
			log.warn("got no result for event for eventId=" + eventUUID);
			return null;
		}
		return objElem.getAsJsonObject();
	}
	
	public JsonObject getEventSeries(Session session, String eventseriesUUID) throws DispatchException{
		return getObjectByUUID(session, "eventseries", "EventSeriesHandler.findById", "id", eventseriesUUID);
	}
	
	public JsonObject getObjectByUUID(Session session, String objectType, String requestMethod, String idFieldName, String idFieldValue) throws DispatchException{
		log.debug("loading " + objectType + " for " + idFieldName + "=" + idFieldValue);
		Request request = new Request(requestMethod);
		request.setMethodParameter(idFieldName, idFieldValue);
		JsonElement objElem = dispatch(request, session);
		if ((objElem == null) || (objElem.isJsonNull())){
			log.warn("got no result for " + objectType + " for " + idFieldName + "=" + idFieldValue);
			return null;
		}
		return objElem.getAsJsonObject();
	}

}
