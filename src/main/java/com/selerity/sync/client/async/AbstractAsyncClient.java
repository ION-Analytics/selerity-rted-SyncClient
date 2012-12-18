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
package com.selerity.sync.client.async;

import com.google.gson.JsonElement;
import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.PaginatedResponseIterator;
import com.selerity.sync.client.Request;
import com.selerity.sync.client.Response;
import com.selerity.sync.client.RhinoSessionFactory;
import com.selerity.sync.client.Session;

/**
 * A base class for asynchronous clients.
 */
public abstract class AbstractAsyncClient {

    protected final String clientAppName;
    protected final AsyncDispatcher dispatcher;
    protected final String user;
    protected final String password;

    /**
     * Initialize the dispatcher.
     */
    public AbstractAsyncClient(AsyncTransportFactory transportFactory, String user, String password, String clientAppName)
            throws Exception {
        // initialized the transport and method dispatcher
        dispatcher = new AsyncDispatcherImpl(transportFactory.getInstance());

        this.clientAppName = clientAppName;
        this.user = user;
        this.password = password;
    }

    /**
     * Dispatch the request using the current session.
     * 
     * @throws DispatchException
     */
    public StreamedResponse asyncDispatch(Request request, Session session) throws DispatchException {
        return dispatcher.asyncDispatch(request, session);
    }

    /**
     * Returns a single result assuming the method call is synchronous.
     * 
     * @param request
     * @param session
     * @return
     * @throws DispatchException
     */
    public JsonElement syncDispatch(Request request, Session session) throws DispatchException {
        Response response = dispatcher.syncDispatch(request, session);
        if (response.isError()) {
            throw response.getError();
        }
        return response.getResult();
    }

    /**
     * Create a paginated response iterator using the current session. Requires the name of the parameter object that will
     * carry the limit and offset parameters.
     * 
     * @param request
     * @return
     * @throws DispatchException
     */
    public PaginatedResponseIterator paginatedDispatch(Request request, Session session, String optionObjectName, int limit)
            throws DispatchException {
        return new AsyncPaginatedResponseIteratorImpl(dispatcher, session, request, optionObjectName, limit);
    }

    /**
     * Starts a session in normal mode.
     * 
     * @param user
     * @param password
     * @throws DispatchException
     */
    public Session startSession() throws DispatchException {
        return new RhinoSessionFactory().getInstance(dispatcher, clientAppName, null, user, password);
    }

    /**
     * Starts a session in the given mode.
     * 
     * @param user
     * @param password
     * @throws DispatchException
     */
    public Session startSession(String mode) throws DispatchException {
        return new RhinoSessionFactory().getInstance(dispatcher, clientAppName, mode, user, password);
    }

    /**
     * Starts a session in "extension" mode.
     * 
     * @param user
     * @param password
     * @throws DispatchException
     */
    public Session startSessionExtensionMode() throws DispatchException {
        return new RhinoSessionFactory().getInstance(dispatcher, clientAppName, "extension", user, password);
    }

    /**
     * Extends the current session.
     * 
     * @throws DispatchException
     */
    public void extendSession(Session session) throws DispatchException {
        Request extendRequest = new Request("AuthenticationHandler.extend");
        syncDispatch(extendRequest, session);
    }

    /**
     * Closes the current session.
     * 
     * @throws DispatchException
     */
    public void closeSession(Session session) throws DispatchException {
        Request logoutRequest = new Request("AuthenticationHandler.invalidate");
        syncDispatch(logoutRequest, session);
    }

}
