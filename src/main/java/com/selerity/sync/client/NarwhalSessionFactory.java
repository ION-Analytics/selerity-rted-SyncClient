package com.selerity.sync.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
 * A utility function for constructing session objects for use with Narwhal services.  The session
 * is used to hold fields across service calls which are passed in the request header.  These may
 * include a security token, an access mode, etc.
 *
 */

public class NarwhalSessionFactory {

private static final Log log = LogFactory.getLog (NarwhalSessionFactory.class);
	
	private final Gson gson = new GsonBuilder().serializeNulls().create(); 
	
	public static final String SYNC_USER_PROPERTY_NAME = "com.selerity.sync.user"; 
	public static final String SYNC_PASSWORD_PROPERTY_NAME = "com.selerity.sync.password";
	
	/** Gets an instance with the provided user and password.
	 * 
	 * @param dispatcher
	 * @param client
	 * @param mode
	 * @param user
	 * @param password
	 * @return
	 * @throws DispatchException
	 */
	public Session getInstance(NarwhalService service, String client, String mode, String user, String password) throws DispatchException{
		Request authRequest = new Request("AuthenticationHandler.authenticate");
		authRequest.setMethodParameter("user", user);
		authRequest.setMethodParameter("password", password);
		SessionImpl session = new SessionImpl();
		session.setHeaderParameter("client", client);
		session.setHeaderParameter("mode", mode);
		String token = gson.fromJson(service.dispatch(authRequest, session), String.class);
		log.debug("got token " + token + " for user " + user);
		return new RhinoSession(user, client, token, mode);
	}
	
	/** Gets a session using the user and password set in the system properties.
	 * 
	 * @param dispatcher
	 * @param client
	 * @param mode
	 * @return
	 * @throws DispatchException
	 */
	public Session getInstance(NarwhalService service, String client, String mode) throws DispatchException{
		String user = System.getProperty(SYNC_USER_PROPERTY_NAME);
		String password = System.getProperty(SYNC_PASSWORD_PROPERTY_NAME);
		if (user == null){
			throw new NullPointerException("user cannot be null, try setting property " + SYNC_USER_PROPERTY_NAME);
		}
		if (password == null){
			throw new NullPointerException("password cannot be null, try setting property " + SYNC_PASSWORD_PROPERTY_NAME);
		}

		return getInstance(service, client, mode, user, password);
	}
	

}
