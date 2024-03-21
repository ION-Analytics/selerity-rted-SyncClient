/*
 * (C) Copyright Selerity, Inc. 2009-2019. All rights reserved. This source code
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
 */
package com.selerity.sync.client.examples;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.selerity.sync.client.AbstractSyncClient;
import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.MiscUtils;
import com.selerity.sync.client.PaginatedResponseIterator;
import com.selerity.sync.client.Request;
import com.selerity.sync.client.RhinoHTTPTransportFactory;
import com.selerity.sync.client.Session;
import com.selerity.sync.client.TransportFactory;

/**
 * Creates specs for events which don't have one yet
 */
public class ObsSpecChecker extends AbstractSyncClient {
    private static final Log log = LogFactory.getLog(ObsSpecChecker.class);

    public static final String PUBLIC_CONTENT_SET_UUID = "c1bf8518-f826-5d16-80ae-6ac3b705c314";

    public ObsSpecChecker(TransportFactory transportFactory, String user, String password, String clientAppName) throws Exception {
        super(transportFactory, user, password, clientAppName);
    }


    /**
     * @param args
     */
    public static void main(String[] args) {
        long startMain = System.currentTimeMillis();
        int eventCount = 0;
        int specCount = 0;
        try {
            if (args.length < 4) {
                System.err.println("arguments: host port user password {daysPast} {daysFuture}");
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


            // look up optional start time offset (in days)
            int daysPast = 10;  // defaults to 10
            if (args.length > 4) {
                daysPast = Integer.parseInt(args[4]);
                log.debug("set daysPast to " + daysPast);
            } else {
                log.debug("defaulting daysPast to " + daysPast);
            }


            // look up optional end time offset (in days)
            int daysFuture = 30;
            if (args.length > 5) {
                daysFuture = Integer.parseInt(args[5]);
                log.debug("set daysFuture to " + daysFuture);
            } else {
                log.debug("defaulted daysFuture to " + daysFuture);
            }

            // compute the time range
            long now = MiscUtils.getNanoTime();
            long startTime = now - daysPast * MiscUtils.NANOS_PER_DAY;
            long endTime = now + daysFuture * MiscUtils.NANOS_PER_DAY;
            log.debug("time range = " + MiscUtils.formatNanoTime(startTime) + " to " + MiscUtils.formatNanoTime(endTime));

            // initialize the dumper
            TransportFactory transportFactory = new RhinoHTTPTransportFactory(host, port, false);
            ObsSpecChecker specCreator = new ObsSpecChecker(transportFactory, user, password, "ObsSpecCreator");

            Session session = specCreator.startSessionExtensionMode();

            try {
                List<JsonObject> events = specCreator.getEventsForContentSet(session, PUBLIC_CONTENT_SET_UUID, startTime, endTime, 100, "UTC");

                for (JsonObject event : events) {
                    eventCount++;
                    try {
                        specCount += specCreator.checkEvent(session, event);
                    } catch (Exception ex2) {
                        log.error("caught " + ex2 + " while search for event " + MiscUtils.getString(event, "name", null) + " (" + MiscUtils.getString(event, "eventId", null) + ")");
                    }
                }
            } catch (Exception ex) {
                log.error("caught " + ex + " while searching for content set " + PUBLIC_CONTENT_SET_UUID, ex);
            }

            specCreator.closeSession(session);

            log.debug("all done");

        } catch (Exception ex) {
            log.error("caught exception " + ex, ex);
        }

        double elapsedSeconds = ((double) (System.currentTimeMillis() - startMain)) / 1000d;
        log.info("checked " + eventCount + " events and fixed " + specCount + " specs in " + elapsedSeconds + " seconds");
    }


    protected int checkObservable(Session session, String eventID, String eventName, JsonObject observable, String contentSetName, String contentSetID) throws DispatchException {

        String observableID = MiscUtils.getString(observable, "observableId", null);
        String measure = MiscUtils.getString(observable, "measure", null);
        String period = MiscUtils.getString(observable, "period", null);

        Request specRequest = new Request("ObservationSpecHandler.getCurrentObservationSpecForObservable");
        specRequest.setMethodParameter("observableId", observableID);
        specRequest.setMethodParameter("contentSetId", contentSetID);


        JsonElement specElem = dispatch(specRequest, session);
        if ((specElem != null) && (!specElem.isJsonNull())) {
            JsonObject specObject = specElem.getAsJsonObject();
            boolean isCurrent = MiscUtils.getBoolean(specObject, "isCurrent", false);
            if (isCurrent) {
                return 0; // no need to fix, the spec is just fine
            }
        }

        // something went wrong - we need to create a spec now!
        log.info("creating a spec for observable for event " + eventName + " and observable " + observable
                + " for measure " + measure + ", period " + period + " and content set " + contentSetName);

        Request newSpecRequest = new Request("ObservableHandler.generateObservationSpec");
        newSpecRequest.setMethodParameter("observable", observable);
        JsonArray contentSetIDArray = new JsonArray();
        contentSetIDArray.add(new JsonPrimitive(contentSetID));
        newSpecRequest.setMethodParameter("contentSetIds", contentSetIDArray);

        JsonElement newSpecElem = dispatch(newSpecRequest, session);
        if ((newSpecElem == null) || (newSpecElem.isJsonNull())) {
            log.error("failed to create spec for observable for event " + eventName + " and observable " + observable
                    + " for measure " + measure + ", period " + period + " and content set " + contentSetName);
            return 0;
        }

        JsonObject newSpec = newSpecElem.getAsJsonObject();
        String specID = MiscUtils.getString(newSpec, "observationSpecId", null);
        long swordfishID = MiscUtils.getLong(newSpec, "legacyId", -1);
        log.info("created new spec " + swordfishID + " (" + specID + ") for observable " + observable + " for event " + eventName
                + " for measure " + measure + ", period " + period + " and content set " + contentSetName);
        return 1;
    }


    protected int checkEvent(Session session, JsonObject event) throws DispatchException {

        int specCount = 0;

        String eventID = MiscUtils.getString(event, "eventId", null);
        String eventName = MiscUtils.getString(event, "name", null);


        JsonObject contentSetObj = MiscUtils.getJsonObject(event, "contentSet", null);
        String contentSetID = null;
        String contentSetName = null;
        if (contentSetObj != null) {
            contentSetID = MiscUtils.getString(contentSetObj, "contentSetId", null);
            contentSetName = MiscUtils.getString(contentSetObj, "name", null);
        }

        log.debug("checking event " + eventName + " (" + eventID + ") with content set " + contentSetName + " (" + contentSetID + ")");

        Request obsRequest = new Request("ObservableHandler.getObservablesForEvent");
        obsRequest.setMethodParameter("eventId", eventID);

        JsonArray obsArray = dispatch(obsRequest, session).getAsJsonArray();

        log.debug("checking " + obsArray.size() + " observables...");

        for (int i = 0; i < obsArray.size(); i++) {
            JsonObject obs = obsArray.get(i).getAsJsonObject();
            specCount += checkObservable(session, eventID, eventName, obs, contentSetName, contentSetID);
        }

        log.debug("fixed " + specCount + " specs for event " + eventName + " (" + eventID + ") with content set " + contentSetName + " (" + contentSetID + ")");

        return specCount;
    }

    protected List<JsonObject> getEventsForContentSet(Session session, String contentSetUUID, long startTime, long endTime, int limit, String timezone) throws DispatchException {

        List<JsonObject> events = new ArrayList<>();

        Request eventsRequest = new Request("EventHandler.search");

        // a search string that should match all events
        String searchString = "AND()";
        eventsRequest.setMethodParameter("searchString", searchString);

        // set some options for the search
        JsonObject searchOptions = new JsonObject();

        // filter just on this content set
        JsonArray contentSetIdArray = new JsonArray();
        contentSetIdArray.add(new JsonPrimitive(contentSetUUID));
        searchOptions.add("contentSetIds", contentSetIdArray);


        // limit to events which fall into this time range (expressed in UTC)
        searchOptions.addProperty("timeIntervalStart", MiscUtils.formatNanoTime(startTime));
        searchOptions.addProperty("timeIntervalEnd", MiscUtils.formatNanoTime(endTime));
        searchOptions.addProperty("timeIntervalTimeZoneId", timezone);

        // offset and limit are used to page through large sets of results but they're handled bu the paginated result iterator

        // sort by expected start date/time
        searchOptions.addProperty("sortOrder", "BY_EXP_START_DATE");

        // apply the search options
        eventsRequest.setMethodParameter("searchOption", searchOptions);

        PaginatedResponseIterator responseIt = paginatedDispatch(eventsRequest, session, "searchOption", limit);

        while (responseIt.hasNextResult()) {
            JsonObject event = responseIt.nextResult().getAsJsonObject();
            events.add(event);
        }

        return events;
    }

    /** Returns a list of content set UUID's to which the user is entitled and which refer to
     *  non-trivial Selerity event content sets.
     *
     * @return
     * @throws DispatchException
     */
    protected Set<String> getEntitledEventContentSetUUIDs(Session session) throws DispatchException {
        // look up all of the content sets to which this user is entitled
        Request contentSetRequest = new Request("ContentSetHandler.getContentSets");
        JsonArray contentSetResponse = dispatch(contentSetRequest, session).getAsJsonArray();
        log.debug("contentSetResponse = " + contentSetResponse);

        Set<String> contentSetUUIDs = new HashSet<>();

        // print out the content sets
        for (int i = 0; i < contentSetResponse.size(); i++) {
            JsonObject contentSet = contentSetResponse.get(i).getAsJsonObject();
            String contentSetName = contentSet.get("name").getAsString();
            String contentSetUUID = contentSet.get("contentSetId").getAsString();
            log.debug("content set[" + i + "] = " + contentSetName + " (" + contentSetUUID + ")");
            if (!contentSetName.equals("Public")) {
                contentSetUUIDs.add(contentSetUUID);
            }
        }

        return contentSetUUIDs;
    }

}
