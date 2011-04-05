package com.selerity.sync.client;

import com.google.gson.JsonElement;

public interface PaginatedResponseIterator {

	/** Returns true if further results are available, false otherwise.
	 *  May block to request the next page of results.
	 * 
	 * @return
	 * @throws DispatchException
	 */
	public boolean hasNextResult() throws DispatchException;

	/** Returns the next result or null if no further results are available.
	 *  May block to request the next page of results.
	 * 
	 * @return
	 * @throws DispatchException
	 */
	public JsonElement nextResult() throws DispatchException;

}