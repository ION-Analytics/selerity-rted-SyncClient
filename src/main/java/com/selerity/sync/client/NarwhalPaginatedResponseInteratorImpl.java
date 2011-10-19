package com.selerity.sync.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
 * A utility class that enables easy iteration over services which implement a pagination option (combination of limit and offset).
 *
 */

public class NarwhalPaginatedResponseInteratorImpl implements PaginatedResponseIterator{
	private static final Log log = LogFactory.getLog (NarwhalPaginatedResponseInteratorImpl.class);

	protected static final String DEFAULT_LIMIT_FIELD_NAME = "limit";
	protected static final String DEFAULT_OFFSET_FIELD_NAME = "offset";
	
	protected final NarwhalService service;
	protected final Session session;
	protected final Request request;
	protected final String optionObjectName;
	protected final String limitFieldName;
	protected final String offsetFieldName;
	protected final int limit;
	protected int nextOffset = 0;	// the offset for the next page
	protected int index = 0;        // the index of the next result within the current results array
	protected JsonArray results = null;  // the array of the current page
	protected boolean done = false;     // set to true when there is no point in asking for another page
	
	
	
	/** Create a new instance for the given request and pagination option object.
	 * 
	 * @param dispatcher
	 * @param session
	 * @param request
	 * @param optionObjectName
	 * @param limitFieldName
	 * @param offsetFieldName
	 * @param limit
	 */
	public NarwhalPaginatedResponseInteratorImpl(NarwhalService service, Session session,
			Request request, String optionObjectName, int limit) {
		this.service = service;
		this.session = session;
		this.request = request;
		this.optionObjectName = optionObjectName;
		this.limitFieldName = DEFAULT_LIMIT_FIELD_NAME;
		this.offsetFieldName = DEFAULT_OFFSET_FIELD_NAME;
		this.limit = limit;
	}
	
	/** Create a new instance for the given request, pagination option object name and field names.
	 * 
	 * @param dispatcher
	 * @param session
	 * @param request
	 * @param optionObjectName
	 * @param limitFieldName
	 * @param offsetFieldName
	 * @param limit
	 */
	public NarwhalPaginatedResponseInteratorImpl(NarwhalService service, Session session,
			Request request, String optionObjectName, String limitFieldName, String offsetFieldName, int limit) {
		this.service = service;
		this.session = session;
		this.request = request;
		this.optionObjectName = optionObjectName;
		this.limitFieldName = limitFieldName;
		this.offsetFieldName = offsetFieldName;
		this.limit = limit;
	}

	/* (non-Javadoc)
	 * @see com.selerity.sync.client.PaginatedResponseIterator#hasNextResult()
	 */
	public synchronized boolean hasNextResult() throws DispatchException{
		if ((results == null) || (results.isJsonNull()) || (index >= results.size())){
			if (done){
				return false;
			}
			loadNextPage();
			if ((results == null) || (results.isJsonNull()) || (index >= results.size())){
				return false;
			}
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.selerity.sync.client.PaginatedResponseIterator#nextResult()
	 */
	public synchronized JsonElement nextResult() throws DispatchException{
		if (hasNextResult()){
			JsonElement result = results.get(index);
			index++;
			return result;
		}
		return null;
	}
	
	/** Loads the next page of results, starting from the offset in nextOffset.  This drops the existing results
	 *  so don't call it until you're done iterating over the prior page.
	 * 
	 * @throws DispatchException
	 */
	protected synchronized void loadNextPage() throws DispatchException{
		log.debug("loading next page for " + request.getMethod() + ", from offset " + nextOffset + " with limit " + limit);
		results = null;
		index = 0;
		
		// find the object that has the limit and offset fields defined, creating it if necessary, and update the offset and limit fields
		JsonObject options = null;  // start at the parameters object
		if (optionObjectName != null){ // the parameters are stored in an object
			options = request.getMethodParameters().getAsJsonObject();
			if (options.has(optionObjectName)){
				// the object exists, just remove/replace the limit and offset
				options = options.get(optionObjectName).getAsJsonObject();
				if (options.has(limitFieldName)){
					options.remove(limitFieldName);
				}
				options.addProperty(limitFieldName, limit);
				log.debug("updated " + request.getMethod() + "." + optionObjectName + "." + limitFieldName + " to " + limit);
				if (options.has(offsetFieldName)){
					options.remove(offsetFieldName);
				}
				options.addProperty(offsetFieldName, nextOffset);
				log.debug("updated " + request.getMethod() + "." + optionObjectName + "." + offsetFieldName + " to " + nextOffset);
			}
			else{
				// the object needs to be created
				options = new JsonObject();
				options.addProperty(limitFieldName, limit);
				options.addProperty(offsetFieldName, nextOffset);
				// now add it to the parameters
				request.setMethodParameter(optionObjectName, options);
				log.debug("created " + request.getMethod() + "." + optionObjectName + "." + limitFieldName + ", set to " + limit);
				log.debug("created " + request.getMethod() + "." + optionObjectName + "." + offsetFieldName + ", set to " + nextOffset);
			}
		}
		
		// now dispatch as normal, expecting an array of results
		JsonElement resultArray = service.dispatch(request, session);
		if ((resultArray != null) && (!resultArray.isJsonNull())){
			if (resultArray.isJsonArray()){
				results = resultArray.getAsJsonArray();  // convert the result into an array
				log.debug("got " + results.size() + " results for " + request.getMethod());
				nextOffset += limit;  // compute the new offset for the next request
			}
			else{
				throw new DispatchException(DispatchException.PARSE_ERROR, "Response was not an array", "request = " + request.getMethod());
			}
		}
		else{
			log.debug("done with " + request.getMethod());
			done = true;
		}
		
	}
}
