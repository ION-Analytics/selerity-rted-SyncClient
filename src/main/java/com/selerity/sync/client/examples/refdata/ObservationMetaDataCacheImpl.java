package com.selerity.sync.client.examples.refdata;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.selerity.sync.client.DispatchException;

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
 * Implements an ObservationMetaDataCache with lazy loading and timeouts.
 * 
 * This instance is threadafe but not very efficient (all requests block while a value is being looked up - which may be a slow process).
 * 
 */

public class ObservationMetaDataCacheImpl implements ObservationMetaDataCache{
	
	private static final Log log = LogFactory.getLog (ObservationMetaDataCacheImpl.class);

	
	protected final Map<Long,ObservationMetaData> metaDataCache = new HashMap<Long,ObservationMetaData>();
	protected final Map<Long,Long> metaDataCacheTimeout = new HashMap<Long,Long>();
	
	protected final ObservationMetaDataLoader metaDataLoader;
	
	protected final long cacheTimeoutMilliseconds;
	
	public ObservationMetaDataCacheImpl(ObservationMetaDataLoader metaDataLoader,
			long cacheTimeout, TimeUnit cacheTimeoutTimeUnit) throws Exception {
		this.metaDataLoader = metaDataLoader;
		this.cacheTimeoutMilliseconds = TimeUnit.MILLISECONDS.convert(cacheTimeout, cacheTimeoutTimeUnit);
	}

	public synchronized ObservationMetaData getObservationMetaDataForSwordfishID(long swordfishObsSpecID) throws DispatchException{
		ObservationMetaData metaData = metaDataCache.get(swordfishObsSpecID);
		if (metaData == null){
			// cache miss - load and return (we know it's valid since we just loaded it).
			log.debug("cache miss on swordfish ID " + swordfishObsSpecID);
			return loadAndUpdateCache(swordfishObsSpecID);
		}
		else{
			// cache hit, now check time.
			long timeout = metaDataCacheTimeout.get(swordfishObsSpecID);
			long currentTime = System.currentTimeMillis();
			if (currentTime > timeout){
				// cache is timed out, need to reload
				log.debug("cache timeout on swordfish ID " + swordfishObsSpecID + ", expired " + (currentTime - timeout) + " ms ago");
				return loadAndUpdateCache(swordfishObsSpecID);
			}
			else{
				// cache is still valid - return
				log.debug("cache hit on swordfish ID " + swordfishObsSpecID + ", returning...");
				return metaData;
			}
		}
		
	}
	
	protected synchronized ObservationMetaData loadAndUpdateCache(long swordfishObsSpecID) throws DispatchException{
		log.debug("loading swordfish ID " + swordfishObsSpecID);
		ObservationMetaData metaData = metaDataLoader.load(swordfishObsSpecID);
		if (metaData == null){
			// we can't find data for this ID
			log.debug("failed to load for swordfish ID " + swordfishObsSpecID);
			return null;
		}
		// now update the cache
		metaDataCache.put(swordfishObsSpecID, metaData);
		metaDataCacheTimeout.put(swordfishObsSpecID, System.currentTimeMillis() + cacheTimeoutMilliseconds);
		log.debug("updated cache for swordfish ID " + swordfishObsSpecID);
		return metaData;
	}
	
	
	
}
