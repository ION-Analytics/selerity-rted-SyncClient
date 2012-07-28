package com.selerity.sync.client;

import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
 * A utility function for constructing session objects for use with Narwhal services.  The session
 * is used to hold fields across service calls which are passed in the request header.  These may
 * include a security token, an access mode, etc.
 * 
 * Note - the sessions may be cached - calls to getInstance may returned cached instance rather than start new ones
 * and close() does nothing.
 *
 */

public class NarwhalSessionFactory {

	private static final Log log = LogFactory.getLog (NarwhalSessionFactory.class);
	
	//private final Gson gson = new GsonBuilder().serializeNulls().create(); 
	
	public static final String SYNC_USER_PROPERTY_NAME = "com.selerity.sync.user"; 
	public static final String SYNC_PASSWORD_PROPERTY_NAME = "com.selerity.sync.password";

	// this is the minimum amount of time remaining on a session before it will be extended
	public static final String SYNC_SESSION_MIN_VALID_TIME_MILLIS_PROPERTY_NAME = "com.selerity.sync.min_valid_time_millis"; 
	public static final long   SYNC_SESSION_MIN_VALID_TIME_MILLIS_DEFAULT_VALUE = 300000L;  // assumes sessions need to be valid for at least another 5 minutes in order to use.
	
	protected static class SessionData {
		
		private static final String KEY_FIELD_SEPARATOR = "|||";
		
		public static String getKey(String user, String password, String client, String mode){
			return user + KEY_FIELD_SEPARATOR + password + KEY_FIELD_SEPARATOR + client + KEY_FIELD_SEPARATOR + mode;
		}
		
		private final Session session;
		private Date expiration;
		
		public SessionData(Session session, Date expiration) {
			super();
			this.session = session;
			this.expiration = expiration;
		}

		public Session getSession() {
			return session;
		}
		
		public synchronized Date getExpiration() {
			return expiration;
		}
		
		public synchronized void setExpiration(Date expiration) {
			this.expiration = expiration;
		}
	}
	
	public static final Map<String,SessionData> sessionCache = new HashMap<String,SessionData>();
	
	
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
	public Session getInstance(final NarwhalService service, final String client, final String mode, final String user, final String password) throws DispatchException{
		
		String sessionCacheKey = SessionData.getKey(user, password, client, mode);
		
		// first, see if there's a session already available.
		synchronized(sessionCache){
			SessionData cacheEntry = sessionCache.get(sessionCacheKey);
			
			if (cacheEntry == null){ // there's no entry now, need to try to create one
				log.debug("there is no cached session for user=" + user + "; client=" + client + "; mode=" + mode + "; password= <not shown>.  Will need to create one.");
				cacheEntry = startNewSession(service, client, mode, user, password);
				sessionCache.put(sessionCacheKey, cacheEntry);
				// now return the newly created entry
				return cacheEntry.getSession();
			}
			
			else{
				// an entry exists, check its time to see if it has enough life left before expiry
				long now = System.currentTimeMillis();
				long remainingMillis = cacheEntry.getExpiration().getTime() - now;
				String token = cacheEntry.getSession().getHeaderParameter("token");
				if (remainingMillis < SYNC_SESSION_MIN_VALID_TIME_MILLIS_DEFAULT_VALUE){
					// need to extend
					log.debug("need to extend session for user " + user + " with token " + token + " because it expires in " + remainingMillis + "ms");
					try{
						extendSessionData(service, cacheEntry);  // this updates the expiration
					}
					catch (DispatchException dx){
						if (dx.getCode() == -1000){
							log.info("need to start a new session for user " + user + " with old token " + token + " because it is no longer valid (got exception: " + dx + " )");
						}
						else{
							log.error("need to start a new session for user " + user + " with old token " + token + " because an error occured: " + dx + " )", dx);
						}
						// token wasn't valid - create a new token
						cacheEntry = startNewSession(service, client, mode, user, password);
						sessionCache.put(sessionCacheKey, cacheEntry);
					}
				}
				else{
					log.debug("don't need to extend session for user " + user + " with token " + token + " because it still has " + remainingMillis + "ms left");	
				}
				return cacheEntry.getSession();
			}
		}
		
		
		
		
		
		
	}

	
	
	protected SessionData startNewSession(final NarwhalService service, final String client, final String mode, final String user, final String password) throws DispatchException{
		Request authRequest = new Request("AuthenticationHandler.authenticate");
		authRequest.setMethodParameter("user", user);
		authRequest.setMethodParameter("password", password);
		SessionImpl session = new SessionImpl();
		session.setHeaderParameter("client", client);
		session.setHeaderParameter("mode", mode);
		
		// now try calling
		JsonElement returnElement = service.dispatch(authRequest, session);
		
		if (returnElement.isJsonObject()){
			// this must have been a call to the new Entitlements Service
			JsonObject returnObject = returnElement.getAsJsonObject();
			String token = MiscUtils.getString(returnObject, "id", null);
			long expirationMillis = MiscUtils.getLong(returnObject, "leaseExpiration", 0);
			Date expiration = new Date(expirationMillis);
			log.debug("generated new session with token " + token + " for user " + user + " expiring on " 
					+ MiscUtils.formatNanoTime(expirationMillis * MiscUtils.NANOS_PER_MILLISECOND) + " via new entitlements service.");
			return new SessionData(new RhinoSession(user, client, token, mode), expiration);
		}
		else{
			// this was a call to the old auth function in Rhino so we only get the token back
			String token = returnElement.getAsString();
			
			// now we need to extend so that we can see when it expires (HACK)
			Request extendRequest = new Request("AuthenticationHandler.extend");
			session.setHeaderParameter("user", user);
			session.setHeaderParameter("token", token);
			String expirationStr = service.dispatch(extendRequest, session).getAsString();
			long expirationNanos = 0;
			try{
				expirationNanos = MiscUtils.parseNanoTime(expirationStr);
			}
			catch (ParseException px){
				log.error("caught " + px + " while parsing expiration time " + expirationStr, px);
				throw new DispatchException(DispatchException.PARSE_ERROR, "failed to parse " + expirationStr + " as a timestamp", expirationStr, px);
			}
			Date expiration = new Date(expirationNanos / MiscUtils.NANOS_PER_MILLISECOND);
			log.debug("generated new session with token " + token + " for user " + user + " expiring on " 
					+ MiscUtils.formatNanoTime(expiration.getTime() * MiscUtils.NANOS_PER_MILLISECOND) + " via old Rhino authentication service.");
			return new SessionData(new RhinoSession(user, client, token, mode), expiration);
		}
	}
	
	protected void extendSessionData(final NarwhalService service, final SessionData sessionData) throws DispatchException{
		Request extendRequest = new Request("AuthenticationHandler.extend");
		extendRequest.getMethodParametersAsArray();  // HACK to force zero-argument call to use array syntax instead of object syntax
		log.debug("about to call extend with session: " + sessionData.getSession());
		JsonElement returnElement = service.dispatch(extendRequest, sessionData.getSession());
		
		log.debug("got returnElement=" + returnElement);
		
		try{
			// first, try the response as a milliseconds long
			long expirationMillis = returnElement.getAsLong();
			Date expiration = new Date(expirationMillis);
			sessionData.setExpiration(expiration);
			log.debug("extended existing session with token " + sessionData.getSession().getHeaderParameter("token") 
					+ " for user " + sessionData.getSession().getHeaderParameter("user") + " expiring on " 
					+ MiscUtils.formatNanoTime(expirationMillis * MiscUtils.NANOS_PER_MILLISECOND) + " via new entitlements service.");
			
			return;
		}
		catch (NumberFormatException nfx){
			// ignore, this just means it's returned a timestamp string, not a number
		}
		
		// didn't work as a long, try as a timestamp string
		try{
			String expirationStr = returnElement.getAsString();
			long expirationNanos = MiscUtils.parseNanoTime(expirationStr);
			Date expiration = new Date(expirationNanos / MiscUtils.NANOS_PER_MILLISECOND);
			sessionData.setExpiration(expiration);
			log.debug("extended existing session with token " + sessionData.getSession().getHeaderParameter("token") 
					+ " for user " + sessionData.getSession().getHeaderParameter("user") + " expiring on " 
					+ MiscUtils.formatNanoTime(expiration.getTime() * MiscUtils.NANOS_PER_MILLISECOND) + " via old Rhino authentication service.");
			return;
		}
		catch (ParseException px){
			log.error("caught " + px + " while parsing expiration time " + returnElement, px);
			throw new DispatchException(DispatchException.PARSE_ERROR, "failed to parse " + returnElement + " as a timestamp", returnElement.toString(), px);
		}
		
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
	
	public void close(Session session){
		// no need to close this session
	}
	
	
	// useful for long-duration for manual integration testing.
	public static void main(String[] args){
		try{
			NarwhalSessionFactory factory = new NarwhalSessionFactory();
			String serviceURLs = "http://ny2aclsp01:8080/rhino-1.0-SNAPSHOT/rpc.do";
			//String serviceURL = "http://ny2aclsp01:8083/rpc.do";
			NarwhalService narwhalService = new NarwhalHTTPServiceImpl(new URL(serviceURLs));

			log.info("will get the first token now...");
			Session session = factory.getInstance(narwhalService, "NarwhalSessionFactoryTest", null);
			log.info("token = " + session.getHeaderParameter(RhinoSession.TOKEN));
			
			log.info("now sleep for a minute and try again - should just hit cache...");
			Thread.sleep(60000);
			session = factory.getInstance(narwhalService, "NarwhalSessionFactoryTest", null);
			log.info("token = " + session.getHeaderParameter(RhinoSession.TOKEN));
			
			log.info("now sleep for 20 minutes and try again - should force an extension...");
			Thread.sleep(1200000);
			session = factory.getInstance(narwhalService, "NarwhalSessionFactoryTest", null);
			log.info("token = " + session.getHeaderParameter(RhinoSession.TOKEN));
			
			log.info("now sleep for 30 minutes and try again - should force an new session to start...");
			Thread.sleep(1800000);
			session = factory.getInstance(narwhalService, "NarwhalSessionFactoryTest", null);
			log.info("token = " + session.getHeaderParameter(RhinoSession.TOKEN));
			
			
			while (true){
				long mins = (long)(Math.random() * 60.0);  // pick a random number of minutes between 0 and 1 hour.
				log.info("now sleep for " + mins + " minutes and try again...");
				Thread.sleep(mins * 60000L);  
				session = factory.getInstance(narwhalService, "NarwhalSessionFactoryTest", null);
				log.info("token = " + session.getHeaderParameter(RhinoSession.TOKEN));
			}
			
		}
		catch (Exception ex){
			log.error("caught " + ex + " in main, exiting", ex);
		}
	}
	
	

}
