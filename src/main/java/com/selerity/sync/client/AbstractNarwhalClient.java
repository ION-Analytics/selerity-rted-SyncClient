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
 * An abstract class for building clients of a Narwhal-based service.
 *
 */

public class AbstractNarwhalClient {
	
	
	protected final NarwhalService service;
	protected final String user;
	protected final String password;
	protected final String clientAppName;
	
	/** 
	 * 
	 * @param host
	 * @param port
	 * @param user
	 * @param password
	 * @throws Exception 
	 */
	public AbstractNarwhalClient(NarwhalService service, String user, String password, String clientAppName) throws Exception{
		this.service = service;
		
		this.user = user;
		this.password = password;
		
		this.clientAppName = clientAppName;
	}


	/** 
	 * Dispatch the request using the current session.
	 * 
	 * @param request
	 * @return
	 * @throws DispatchException
	 */
	public JsonElement dispatch(Request request, Session session) throws DispatchException{
		return service.dispatch(request, session); 	
	}
	
	/**
	 * Dispatch the request (assumes all header fields are filled in) and returns a reader.
	 * The reader may be used to read in one or more responses asynchronously.
	 * 
	 * @param request
	 * @return
	 * @throws Exception 
	 */
	public JsonReader dispatch(FullRequest request) throws Exception{
		return service.dispatch(request);
	}
	
	/** Create a paginated response iterator using the current session.  Requires the name of the parameter object that
	 *  will carry the limit and offset parameters.
	 * 
	 * @param request
	 * @return
	 * @throws DispatchException
	 */
	public PaginatedResponseIterator paginatedDispatch(Request request, Session session, String optionObjectName, int limit) throws DispatchException{
		return new NarwhalPaginatedResponseInteratorImpl(service, session, request, optionObjectName, limit);
	}

	
	/** Starts a session in normal mode.
	 * 
	 * @param user
	 * @param password
	 * @throws DispatchException
	 */
	public Session startSession() throws DispatchException{
		return new NarwhalSessionFactory().getInstance(service,
				clientAppName, null, user, password);
	}
	
	/** Starts a session in the given mode.
	 * 
	 * @param user
	 * @param password
	 * @throws DispatchException
	 */
	public Session startSession(String mode) throws DispatchException{
		return new NarwhalSessionFactory().getInstance(service,
				clientAppName, mode, user, password);
	}
	
	/** Starts a session in "extension" mode.
	 * 
	 * @param user
	 * @param password
	 * @throws DispatchException
	 */
	public Session startSessionExtensionMode() throws DispatchException{
		return new NarwhalSessionFactory().getInstance(service,
				clientAppName, "extension", user, password);
	}
	
	/** Extends the current session.
	 * 
	 * @throws DispatchException
	 */
	public void extendSession(Session session) throws DispatchException{
		Request extendRequest = new Request("AuthenticationHandler.extend");
		dispatch(extendRequest, session);
	}
	
	
	/** Closes the current session.
	 * 
	 * @throws DispatchException
	 */
	public void closeSession(Session session) throws DispatchException{
		Request logoutRequest = new Request("AuthenticationHandler.invalidate");
		dispatch(logoutRequest, session);
	}
}
