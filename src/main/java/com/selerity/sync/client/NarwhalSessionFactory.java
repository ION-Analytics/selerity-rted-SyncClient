/* 
 * (C) Copyright Selerity, Inc. 2009-2012. All rights reserved. This source code
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
package com.selerity.sync.client;

import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A utility function for constructing session objects for use with Narwhal services. The session is used to hold fields
 * across service calls which are passed in the request header. These may include a security token, an access mode, etc.
 * 
 * Note - the sessions may be cached - calls to getInstance may returned cached instance rather than start new ones and
 * close() does nothing.
 */
public class NarwhalSessionFactory {

    private static final Log log = LogFactory.getLog(NarwhalSessionFactory.class);

    public static final String SYNC_USER_PROPERTY_NAME = "com.selerity.sync.user";
    public static final String SYNC_PASSWORD_PROPERTY_NAME = "com.selerity.sync.password";

    public static final String EXTENSION_MODE_STRING = "extension";

    // this is the minimum amount of time remaining on a session before it will be extended
    public static final long SYNC_SESSION_MIN_VALID_TIME_MILLIS = 300000L; // assumes sessions need to be valid
                                                                                         // for at least another 5 minutes in
                                                                                         // order to use.

    public static final long SESSION_REFRESH_INTERVAL_MILLIS = 1200000L; // refresh at least once every 20 minutes.

    /**
     * Simple data structure to record information about a session.
     */
    protected static class SessionData {

        private static final String KEY_FIELD_SEPARATOR = "|||";

        public static String getKey(String user, String password, String client, String mode) {
            return user + KEY_FIELD_SEPARATOR + password + KEY_FIELD_SEPARATOR + client + KEY_FIELD_SEPARATOR + mode;
        }

        private final Session session;
        private Date expiration;

        public SessionData(Session session, Date expiration) {
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

    // this is a shared global cache of sessions used by all factories
    private static final Map<String, SessionData> sessionCache = new HashMap<String, SessionData>();
    
    /**
     * Clears all sessions from the cache used by all factory instances.  Should
     * rarely be needed except in testing.
     */
    public static void clearAllSessions(){
    	synchronized(sessionCache){
    		sessionCache.clear();
    		log.info("cleared all sessions from cache!");
    	}
    }
    

    // Instance variables

    public final NarwhalService service; // this is the service used to perform authentication
    public final String client;
    public final String user;
    public final String password;

    public NarwhalSessionFactory(final NarwhalService service, final String client) {
        this(service, client, null, null);
    }

    public NarwhalSessionFactory(final NarwhalService service, final String client, final String user, final String password) {
        this.service = service;
        this.client = client;

        // if the user is set to null, try to look it up via the system property
        if (user == null) {
            this.user = System.getProperty(SYNC_USER_PROPERTY_NAME);
            if (this.user == null) {
                throw new NullPointerException("user cannot be null, try setting property " + SYNC_USER_PROPERTY_NAME);
            }
        } else {
            this.user = user;
        }

        // if the password is set to null, try to look it up via the system property
        if (password == null) {
            this.password = System.getProperty(SYNC_PASSWORD_PROPERTY_NAME);
            if (this.password == null) {
                throw new NullPointerException("password cannot be null, try setting property "
                        + SYNC_PASSWORD_PROPERTY_NAME);
            }
        } else {
            this.password = password;
        }

    }

    /**
     * Starts a thread to periodically force the session to be refreshed using default timing parameter
     * 
     * @param refreshIntervalMillis
     */
    public void startPerpetualRefresh() {
        startPerpetualRefresh(SESSION_REFRESH_INTERVAL_MILLIS);
    }

    /**
     * Starts a thread to periodically force the session to be refreshed using the given timing parameter
     * 
     * @param refreshIntervalMillis
     */
    public void startPerpetualRefresh(final long refreshIntervalMillis) {
        final Thread refreshThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        // look up the set of keys
                        final Set<String> sessionCacheKeys = new HashSet<String>();
                        synchronized (sessionCache) {
                            sessionCacheKeys.addAll(sessionCache.keySet());
                        }

                        // now loop through and force a reload of each
                        for (String key : sessionCacheKeys) {
                            final SessionData sessionData;
                            synchronized (sessionCache) {
                                sessionData = sessionCache.get(key);
                            }
                            extendSessionData(service, sessionData); // forces a reload
                        }

                    } catch (Exception ex) {
                        log.error("caught " + ex + " while refreshing session, ignoring");
                    }

                    try {
                        Thread.sleep(refreshIntervalMillis);
                    } catch (Exception ex) {

                    }
                }
            }
        }, "perpetualSessionRefreshThread");
        refreshThread.setDaemon(true);
        refreshThread.start();
        log.info("started session refresh thread: " + refreshThread.getName() + " refreshing every " + refreshIntervalMillis
                + " ms.");
    }

    /**
     * Gets an instance with default mode.
     * 
     * @param mode
     * @return
     * @throws DispatchException
     */
    public Session getInstance() throws DispatchException {
        return getInstance(null);
    }

    /**
     * Gets an instance with extension mode.
     * 
     * @param mode
     * @return
     * @throws DispatchException
     */
    public Session getInstanceExtensionMode() throws DispatchException {
        return getInstance(EXTENSION_MODE_STRING);
    }

    /**
     * Gets an instance with the provided mode.
     * 
     * @param mode
     * @return
     * @throws DispatchException
     */
    public Session getInstance(final String mode) throws DispatchException {

        final String sessionCacheKey = SessionData.getKey(user, password, client, mode);

        // first, see if there's a session already available.
        synchronized (sessionCache) {
            SessionData cacheEntry = sessionCache.get(sessionCacheKey);

            if (cacheEntry == null) { // there's no entry now, need to try to create one
                log.debug("there is no cached session for user=" + user + "; client=" + client + "; mode=" + mode
                        + "; password= <not shown>.  Will need to create one.");
                cacheEntry = startNewSession(mode);
                sessionCache.put(sessionCacheKey, cacheEntry);
                // now return the newly created entry
                return cacheEntry.getSession();
            }

            else {
                // an entry exists, check its time to see if it has enough life left before expiry
                final long now = System.currentTimeMillis();
                final long remainingMillis = cacheEntry.getExpiration().getTime() - now;
                final String token = cacheEntry.getSession().getHeaderParameter("token");
                if (remainingMillis < SYNC_SESSION_MIN_VALID_TIME_MILLIS) {
                    // need to extend
                    log.debug("need to extend session for user " + user + " with token " + token + " because it expires in "
                            + remainingMillis + "ms");
                    try {
                        extendSessionData(service, cacheEntry); // this updates the expiration
                    } catch (DispatchException dx) {
                        if (dx.getCode() == -1000) {
                            log.info("need to start a new session for user " + user + " with old token " + token
                                    + " because it is no longer valid (got exception: " + dx + " )");
                        } else {
                            log.error("need to start a new session for user " + user + " with old token " + token
                                    + " because an error occured: " + dx + " )", dx);
                        }
                        // token wasn't valid - create a new token
                        cacheEntry = startNewSession(mode);
                        sessionCache.put(sessionCacheKey, cacheEntry);
                    }
                } else {
                    log.debug("don't need to extend session for user " + user + " with token " + token
                            + " because it still has " + remainingMillis + "ms left");
                }
                return cacheEntry.getSession();
            }
        }

    }

    protected SessionData startNewSession(final String mode) throws DispatchException {
        final Request authRequest = new Request("AuthenticationHandler.authenticate");
        authRequest.setMethodParameter("user", user);
        authRequest.setMethodParameter("password", password);
        final SessionImpl session = new SessionImpl();
        session.setHeaderParameter("client", client);
        session.setHeaderParameter("mode", mode);

        // now try calling
        final JsonElement returnElement = service.dispatch(authRequest, session);

        if (returnElement.isJsonObject()) {
            // this must have been a call to the new Entitlements Service
            final JsonObject returnObject = returnElement.getAsJsonObject();
            final String token = MiscUtils.getString(returnObject, "id", null);
            final long expirationMillis = MiscUtils.getLong(returnObject, "leaseExpiration", 0);
            final Date expiration = new Date(expirationMillis);
            log.debug("generated new session with token " + token + " for user " + user + " expiring on "
                    + MiscUtils.formatNanoTime(expirationMillis * MiscUtils.NANOS_PER_MILLISECOND)
                    + " via new entitlements service.");
            return new SessionData(new RhinoSession(user, client, token, mode), expiration);
        } else {
            // this was a call to the old auth function in Rhino so we only get the token back
            final String token = returnElement.getAsString();

            // now we need to extend so that we can see when it expires (HACK)
            final Request extendRequest = new Request("AuthenticationHandler.extend");
            session.setHeaderParameter("user", user);
            session.setHeaderParameter("token", token);
            final String expirationStr = service.dispatch(extendRequest, session).getAsString();
            long expirationNanos = 0;
            try {
                expirationNanos = MiscUtils.parseNanoTime(expirationStr);
            } catch (ParseException px) {
                log.error("caught " + px + " while parsing expiration time " + expirationStr, px);
                throw new DispatchException(DispatchException.PARSE_ERROR, "failed to parse " + expirationStr
                        + " as a timestamp", expirationStr, px);
            }
            final Date expiration = new Date(expirationNanos / MiscUtils.NANOS_PER_MILLISECOND);
            log.debug("generated new session with token " + token + " for user " + user + " expiring on "
                    + MiscUtils.formatNanoTime(expiration.getTime() * MiscUtils.NANOS_PER_MILLISECOND)
                    + " via old Rhino authentication service.");
            return new SessionData(new RhinoSession(user, client, token, mode), expiration);
        }
    }

    protected void extendSessionData(final NarwhalService service, final SessionData sessionData) throws DispatchException {
        final Request extendRequest = new Request("AuthenticationHandler.extend");
        extendRequest.getMethodParametersAsArray(); // HACK to force zero-argument call to use array syntax instead of object
                                                    // syntax
        log.debug("about to call extend with session: " + sessionData.getSession());
        final JsonElement returnElement = service.dispatch(extendRequest, sessionData.getSession());

        log.debug("got returnElement=" + returnElement);

        try {
            // first, try the response as a milliseconds long
            final long expirationMillis = returnElement.getAsLong();
            final Date expiration = new Date(expirationMillis);
            sessionData.setExpiration(expiration);
            log.debug("extended existing session with token " + sessionData.getSession().getHeaderParameter("token")
                    + " for user " + sessionData.getSession().getHeaderParameter("user") + " expiring on "
                    + MiscUtils.formatNanoTime(expirationMillis * MiscUtils.NANOS_PER_MILLISECOND)
                    + " via new entitlements service.");

            return;
        } catch (NumberFormatException nfx) {
            // ignore, this just means it's returned a timestamp string, not a number
        }

        // didn't work as a long, try as a timestamp string
        try {
            final String expirationStr = returnElement.getAsString();
            final long expirationNanos = MiscUtils.parseNanoTime(expirationStr);
            final Date expiration = new Date(expirationNanos / MiscUtils.NANOS_PER_MILLISECOND);
            sessionData.setExpiration(expiration);
            log.debug("extended existing session with token " + sessionData.getSession().getHeaderParameter("token")
                    + " for user " + sessionData.getSession().getHeaderParameter("user") + " expiring on "
                    + MiscUtils.formatNanoTime(expiration.getTime() * MiscUtils.NANOS_PER_MILLISECOND)
                    + " via old Rhino authentication service.");
            return;
        } catch (ParseException px) {
            log.error("caught " + px + " while parsing expiration time " + returnElement, px);
            throw new DispatchException(DispatchException.PARSE_ERROR, "failed to parse " + returnElement
                    + " as a timestamp", returnElement.toString(), px);
        }

    }

    /**
     * Force any sessions which use this token to be removed from the cache. Subsequent requests for a session with the given
     * user, password, etc. will thus force a new session to start. This is a useful way to work around certain failure modes
     * in the entitlements system when can get out of sync for long-duration sessions.
     * 
     * 
     * @param token
     */
    public void invalidateSession(final String tokenToInvalidate) {
        synchronized (sessionCache) {
            final Set<String> keys = sessionCache.keySet();
            for (final String key : keys) {
                final SessionData cacheEntry = sessionCache.get(key);
                final String token = cacheEntry.getSession().getHeaderParameter(Session.TOKEN);
                if (tokenToInvalidate.equalsIgnoreCase(token)) {
                    final String user = cacheEntry.getSession().getHeaderParameter(Session.USER);
                    log.info("invalidating session for user " + user + " with token " + token);
                    sessionCache.remove(key);
                }
            }
        }
    }

    /**
     * Force this session and any other sessions which use the same token to be removed from the cache. Subsequent requests
     * for a session with the given user, password, etc. will thus force a new session to start. This is a useful way to work
     * around certain failure modes in the entitlements system when can get out of sync for long-duration sessions.
     * 
     * @param sessionToInvalidate
     */
    public void invalidateSession(final Session sessionToInvalidate) {
        invalidateSession(sessionToInvalidate.getHeaderParameter(Session.TOKEN));
    }

    public void close(Session session) {
        // no need to close this session
    }

    public void extend(Session session) {
        // no need to do this explicitly, it will be done on demand when needed
    }

    // useful for long-duration or manual integration testing.
    public static void main(String[] args) {
        try {

            // String serviceURLs = "http://ny2aclsp01:8080/rhino-1.0-SNAPSHOT/rpc.do";
            final String serviceURLs = "http://ny2aclsp01:8083/rpc.do";
            final NarwhalSessionFactory factory = new NarwhalSessionFactory(
                    new NarwhalHTTPServiceImpl(new URL(serviceURLs)), "NarwhalSessionFactoryTest");
            factory.startPerpetualRefresh();

            log.info("will get the first token now...");
            Session session = factory.getInstance();
            log.info("token = " + session.getHeaderParameter(RhinoSession.TOKEN));

            log.info("now sleep for 10 seconds and try again - should just hit cache...");
            Thread.sleep(10000);
            session = factory.getInstance();
            log.info("token = " + session.getHeaderParameter(RhinoSession.TOKEN));

            log.info("now invalidate...");
            factory.invalidateSession(session);

            log.info("now sleep for 10 seconds and try again - should force a new session to start...");
            Thread.sleep(10000);
            session = factory.getInstance();
            log.info("token = " + session.getHeaderParameter(RhinoSession.TOKEN));

            log.info("now sleep for 20 minutes and try again - should force an extension...");
            Thread.sleep(1200000);
            session = factory.getInstance();
            log.info("token = " + session.getHeaderParameter(RhinoSession.TOKEN));

            log.info("now sleep for 30 minutes and try again - should force an new session to start...");
            Thread.sleep(1800000);
            session = factory.getInstance();
            log.info("token = " + session.getHeaderParameter(RhinoSession.TOKEN));

            while (true) {
                long mins = (long) (Math.random() * 60.0); // pick a random number of minutes between 0 and 1 hour.
                log.info("now sleep for " + mins + " minutes and try again...");
                Thread.sleep(mins * 60000L);
                session = factory.getInstance();
                log.info("token = " + session.getHeaderParameter(RhinoSession.TOKEN));
            }

        } catch (Exception ex) {
            log.error("caught " + ex + " in main, exiting", ex);
        }
    }

}
