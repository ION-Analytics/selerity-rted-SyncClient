package com.selerity.sync.client;

import com.google.gson.JsonElement;

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
 * An interface for utility class which make it easy to iterate through results from services which 
 * support pagination (using a limit, offset model).
 *
 */

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