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
package com.selerity.sync.client.examples.refdata;

import java.util.Collections;
import java.util.Set;

/**
 * A simplified, flattened representation of the meta data that is related,
 * directly or indirectly, to an observation.
 */
public class ObservationMetaData {

    protected final String observationSpecUUID;
    protected final long swordfishObsSpecID;

    protected final String observableUUID;
    protected final String timeseriesUUID;
    protected final String eventUUID;
    protected final String eventseriesUUID;

    protected final String eventName;
    protected final String timeseriesName;
    protected final String eventseriesName;

    protected final long scale;
    protected final String measure;
    protected final String unit;
    protected final String period;
    protected final String periodRelativity;
    protected final String dataType;

    protected final Set<Tag> tags;

    public ObservationMetaData(String observationSpecUUID,
                               long swordfishObsSpecID, String observableUUID,
                               String timeseriesUUID, String eventUUID, String eventseriesUUID,
                               String eventName, String timeseriesName, String eventseriesName,
                               long scale, String measure, String unit, String period,
                               String periodRelativity, String dataType, Set<Tag> tags) {
        this.observationSpecUUID = observationSpecUUID;
        this.swordfishObsSpecID = swordfishObsSpecID;
        this.observableUUID = observableUUID;
        this.timeseriesUUID = timeseriesUUID;
        this.eventUUID = eventUUID;
        this.eventseriesUUID = eventseriesUUID;
        this.eventName = eventName;
        this.timeseriesName = timeseriesName;
        this.eventseriesName = eventseriesName;
        this.scale = scale;
        this.measure = measure;
        this.unit = unit;
        this.period = period;
        this.periodRelativity = periodRelativity;
        this.dataType = dataType;
        this.tags = Collections.unmodifiableSet(tags);
    }

    public String getObservationSpecUUID() {
        return observationSpecUUID;
    }

    public long getSwordfishObsSpecID() {
        return swordfishObsSpecID;
    }

    public String getObservableUUID() {
        return observableUUID;
    }

    public String getTimeseriesUUID() {
        return timeseriesUUID;
    }

    public String getEventUUID() {
        return eventUUID;
    }

    public String getEventseriesUUID() {
        return eventseriesUUID;
    }

    public String getEventName() {
        return eventName;
    }

    public String getTimeseriesName() {
        return timeseriesName;
    }

    public String getEventseriesName() {
        return eventseriesName;
    }

    public long getScale() {
        return scale;
    }

    public String getMeasure() {
        return measure;
    }

    public String getUnit() {
        return unit;
    }

    public String getPeriod() {
        return period;
    }

    public String getPeriodRelativity() {
        return periodRelativity;
    }

    public String getDataType() {
        return dataType;
    }

    public Set<Tag> getTags() {
        return tags;
    }

	@Override
    public String toString() {
        StringBuilder s = new StringBuilder("observationSpecUUID = " + observationSpecUUID + "; "
                + "swordfishObsSpecID = " + swordfishObsSpecID + "; "
                + "observableUUID = " + observableUUID + "; "
                + "eventUUID = " + eventUUID + "; "
                + "eventName = " + eventName + "; "
                + "timeseriesUUID = " + timeseriesUUID + "; "
                + "timeseriesName = " + timeseriesName + "; "
                + "eventseriesUUID = " + eventseriesUUID + "; "
                + "eventseriesName = " + eventseriesName + "; "
                + "scale = " + scale + "; "
                + "measure = " + measure + "; "
                + "unit = " + unit + "; "
                + "period = " + period + "; "
                + "periodRelativity = " + periodRelativity + "; "
                + "dataType = " + dataType + "; "
                + "tags = [");
        for (Tag tag : tags) {
            s.append(tag.getName() + "=" + tag.getValue() + "; ");
        }
        s.append("]");
        return s.toString();
    }


}
