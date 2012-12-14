package com.selerity.sync.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
	
	private static final Log log = LogFactory.getLog(AbstractNarwhalClient.class);
	
	
	protected final NarwhalSessionFactory narwhalSessionFactory;
	protected final NarwhalService narwhalService;

	
	/** Creates a new instance with the given service endpoint, username, password and client app name.
	 * 
	 *  Note that this assumes that the given endpoint can be used for authentication as well as other methods. 
	 * 
	 * @param service
	 * @param user
	 * @param password
	 * @param clientAppName
	 * @throws Exception 
	 */
	public AbstractNarwhalClient(NarwhalService narwhalService, String user, String password, String clientAppName) throws Exception{
		this.narwhalService = narwhalService;		
		narwhalSessionFactory = new NarwhalSessionFactory(narwhalService, clientAppName, user, password);
	}

	/** Creates a new instance with the given service endpoint for normal method dispatch and the given
	 *  session factory for performing authentication.
	 * 
	 * @param narwhalService
	 * @param narwhalSessionFactory
	 * @throws Exception 
	 */
	public AbstractNarwhalClient(NarwhalService narwhalService, NarwhalSessionFactory narwhalSessionFactory) throws Exception{
		this.narwhalService = narwhalService;
		this.narwhalSessionFactory = narwhalSessionFactory;
	}
	

	/** 
	 * Dispatch the request using the given session.
	 * 
	 * @param request
	 * @return
	 * @throws DispatchException
	 */
	public JsonElement dispatch(Request request, Session session) throws DispatchException{
		try{
			return narwhalService.dispatch(request, session);
		}
		catch (DispatchException dx){  // catch any session-related errors and invalidate the session
			if (((dx.getCode() == -1000) || (dx.getMessage().startsWith("Session not valid"))) && (session != null)){
				final String token = session.getHeaderParameter(Session.TOKEN);
				log.error("session " + token + " had error, will invalidate", dx);
				this.narwhalSessionFactory.invalidateSession(token);
			}
			throw dx; // rethrow the exception
		}
	}
	
	/** 
	 * Dispatch the request using a session created by the internal factory.  Note that this
	 * method will be dispatched with a default mode session.
	 * 
	 * @param request
	 * @return
	 * @throws DispatchException
	 */
	public JsonElement dispatch(Request request) throws DispatchException{
		final Session session = narwhalSessionFactory.getInstance();
		try{
			return narwhalService.dispatch(request, session);
		}
		catch (DispatchException dx){  // catch any session-related errors and invalidate the session
			if (((dx.getCode() == -1000) || (dx.getMessage().startsWith("Session not valid"))) && (session != null)){
				final String token = session.getHeaderParameter(Session.TOKEN);
				log.error("session " + token + " had error, will invalidate", dx);
				this.narwhalSessionFactory.invalidateSession(token);
			}
			throw dx; // rethrow the exception
		}
	}
	
	/** 
	 * Dispatch the request using a session created by the internal factory.
	 * 
	 * Note that this method will be dispatched with an "extension mode" session.
	 * 
	 * @param request
	 * @return
	 * @throws DispatchException
	 */
	public JsonElement dispatchExtensionMode(Request request) throws DispatchException{		
		final Session session = narwhalSessionFactory.getInstanceExtensionMode();
		try{
			return narwhalService.dispatch(request, session);
		}
		catch (DispatchException dx){  // catch any session-related errors and invalidate the session
			if (((dx.getCode() == -1000) || (dx.getMessage().startsWith("Session not valid"))) && (session != null)){
				final String token = session.getHeaderParameter(Session.TOKEN);
				log.error("session " + token + " had error, will invalidate", dx);
				this.narwhalSessionFactory.invalidateSession(token);
			}
			throw dx; // rethrow the exception
		}
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
		try{
			return narwhalService.dispatch(request);
		}
		catch (DispatchException dx){  // catch any session-related errors and invalidate the session
			if (((dx.getCode() == -1000) || (dx.getMessage().startsWith("Session not valid"))) && (request != null)){
				final JsonElement tokenElem = request.getHeader().get(Session.TOKEN);
				if (tokenElem != null){
					final String token = tokenElem.getAsString();
					log.error("session " + token + " had error, will invalidate", dx);
					this.narwhalSessionFactory.invalidateSession(token);
				}
			}
			throw dx; // rethrow the exception
		}
	}
	
	/** Create a paginated response iterator using the given session.  Requires the name of the parameter object that
	 *  will carry the limit and offset parameters.
	 * 
	 * @param request
	 * @param session
	 * @param optionObjectName the name of the object that contains the "pagination options" block of limit and offset
	 * @param limit the page size to load
	 * @return
	 * @throws DispatchException
	 */
	public PaginatedResponseIterator paginatedDispatch(Request request, Session session, String optionObjectName, int limit) throws DispatchException{
		return new NarwhalPaginatedResponseInteratorImpl(narwhalService, session, request, optionObjectName, limit);
	}
	
	/** Create a paginated response iterator using the current session from the internal factory.  Requires the name of the parameter object that
	 *  will carry the limit and offset parameters.
	 *  
	 *  This call will be executed with a default mode session.
	 * 
	 * @param request
	 * @param session
	 * @param optionObjectName the name of the object that contains the "pagination options" block of limit and offset
	 * @param limit the page size to load
	 * @return
	 * @throws DispatchException
	 */
	public PaginatedResponseIterator paginatedDispatch(Request request, String optionObjectName, int limit) throws DispatchException{
		return new NarwhalPaginatedResponseInteratorImpl(narwhalService, narwhalSessionFactory.getInstance(), request, optionObjectName, limit);
	}
	
	/** Create a paginated response iterator using the current session from the internal factory.  Requires the name of the parameter object that
	 *  will carry the limit and offset parameters.
	 *  
	 *  This call will be executed with a "extension mode" session.
	 * 
	 * @param request
	 * @param session
	 * @param optionObjectName the name of the object that contains the "pagination options" block of limit and offset
	 * @param limit the page size to load
	 * @return
	 * @throws DispatchException
	 */
	public PaginatedResponseIterator paginatedDispatchExtensionMode(Request request, String optionObjectName, int limit) throws DispatchException{
		return new NarwhalPaginatedResponseInteratorImpl(narwhalService, narwhalSessionFactory.getInstanceExtensionMode(), request, optionObjectName, limit);
	}

	
	/** Starts a session in normal mode.
	 * 
	 * @param user
	 * @param password
	 * @throws DispatchException
	 */
	public Session startSession() throws DispatchException{
		return narwhalSessionFactory.getInstance();
	}
	
	/** Starts a session in the given mode.
	 * 
	 * @param user
	 * @param password
	 * @throws DispatchException
	 */
	public Session startSession(String mode) throws DispatchException{
		return narwhalSessionFactory.getInstance(mode);
	}
	
	/** Starts a session in "extension" mode.
	 * 
	 * @param user
	 * @param password
	 * @throws DispatchException
	 */
	public Session startSessionExtensionMode() throws DispatchException{
		return narwhalSessionFactory.getInstanceExtensionMode();
	}
	
	/** Extends the current session.
	 * 
	 * @throws DispatchException
	 */
	public void extendSession(Session session) throws DispatchException{
		narwhalSessionFactory.extend(session);
	}
	
	
	/** Closes the current session.
	 * 
	 * @throws DispatchException
	 */
	public void closeSession(Session session) throws DispatchException{
		narwhalSessionFactory.close(session);
	}
}
