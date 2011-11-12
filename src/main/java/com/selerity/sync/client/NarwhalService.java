package com.selerity.sync.client;

import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;

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
 * An abstract interface for accessing a Narwhal-based service.  Narwhal is Selerity's
 * protocol for client-server communications and is based on the emerging JSON-RPC 2.0
 * standard.  It supports both request/response and streaming responses.
 *
 */

public interface NarwhalService {

	/** 
	 * Returns a name for the service instance, useful mainly for debugging purposes when there are multiple service
	 * instances.
	 * 
	 * @return
	 */
	public String getName();
	
	/**
	 * Dispatches the request, filling in the header from the session information as appropriate.
	 * 
	 * If the response is not an error then the result will be returned.
	 * 
	 * If the response is an error, the corresponding exception will be thrown.
	 * 
	 * Note that this method will *not* work correctly for streaming responses since it assumes a single response to each request.
	 * 
	 * @param request
	 * @param session
	 * @return the result 
	 * @throws DispatchException wraps the error returned by the server or any local exeption (e.g. IO exception) that may occur.
	 */
	public JsonElement dispatch(Request request, Session session) throws DispatchException;
	
	/**
	 * Dispatches the request, filling in the header from the session information as appropriate.
	 * 
	 * Returns the response object regardless of whether it's an error or not.
	 * 
	 * Will only throw an exception if a local exception occurs.  Server-side exceptions will be returned as the error field
	 * of the response object.
	 * 
	 * Note that this method will *not* work correctly for streaming responses since it assumes a single response to each request.
	 * 
	 * @param request
	 * @param session
	 * @return the response
	 * @throws DispatchException
	 */
	public Response dispatchWithResponse(Request request, Session session) throws DispatchException;
	
	/** 
	 * Dispatches the request (the header must already be filled in with session information if needed) and returns
	 * a JsonReader from which the response(s) can be read.
	 * 
	 * Will only throw exceptions due to local failures; server-side exceptions will be returned in the error field of
	 * the response(s) that can be read from the stream.
	 * 
	 * This method is particularly useful in situations where the service is capable of sending multiple responses for a
	 * single request, i.e. a method that supports streaming responses.
	 * 
	 * @param request
	 * @return a reader for the stream of responses.
	 * @throws Exception
	 */
	public JsonReader dispatch(FullRequest request) throws Exception;
	
}
