/*
 * (C) Copyright Selerity, Inc. 2009-2019. All rights reserved. This source code
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

import com.google.gson.JsonElement;

/**
 * An abstract class for building clients of the Selerity Sync API.
 */
public abstract class AbstractSyncClient {

    protected final String clientAppName;
    protected final Dispatcher dispatcher;
    protected final String user;
    protected final String password;

    /** Initialize the dispatcher and start a session.
     *
     * @param transportFactory
     * @param user
     * @param password
     * @param clientAppName
     * @throws Exception
     */
    public AbstractSyncClient(TransportFactory transportFactory, String user, String password, String clientAppName) throws Exception {
        // initialized the transport and method dispatcher
        Transport transport = transportFactory.getInstance();
        dispatcher = new RhinoDispatcher(transport);

        this.clientAppName = clientAppName;
        this.user = user;
        this.password = password;
    }


    /** Dispatch the request using the current session.
     *
     * @param request
     * @param session
     * @throws DispatchException
     */
    public JsonElement dispatch(Request request, Session session) throws DispatchException {
        return dispatcher.dispatch(request, session);
    }

    /** Create a paginated response iterator using the current session.  Requires the name of the parameter object that
     *  will carry the limit and offset parameters.
     *
     * @param request
     * @return
     * @throws DispatchException
     */
    public PaginatedResponseIterator paginatedDispatch(Request request, Session session, String optionObjectName, int limit) throws DispatchException {
        return new PaginatedResponseIteratorImpl(dispatcher, session, request, optionObjectName, limit);
    }

    /** Dispatch the array of requests as a single boxcar using the current session.
     *
     * @param requests
     * @return
     * @throws DispatchException
     */
    public Response[] boxcarDispatch(Request[] requests, Session session) throws DispatchException {
        return dispatcher.boxcarDispatch(requests, session);
    }


    /** Starts a session in normal mode.
     *
     * @throws DispatchException
     */
    public Session startSession() throws DispatchException {
        return new RhinoSessionFactory().getInstance(dispatcher,
                clientAppName, null, user, password);
    }

    /** Starts a session in the given mode.
     *
     * @throws DispatchException
     */
    public Session startSession(String mode) throws DispatchException {
        return new RhinoSessionFactory().getInstance(dispatcher,
                clientAppName, mode, user, password);
    }

    /** Starts a session in "extension" mode.
     *
     * @throws DispatchException
     */
    public Session startSessionExtensionMode() throws DispatchException {
        return new RhinoSessionFactory().getInstance(dispatcher,
                clientAppName, "extension", user, password);
    }

    /** Extends the current session.
     *
     * @throws DispatchException
     */
    public void extendSession(Session session) throws DispatchException {
        Request extendRequest = new Request("AuthenticationHandler.extend");
        dispatch(extendRequest, session);
    }

    /** Closes the current session.
     *
     * @throws DispatchException
     */
    public void closeSession(Session session) throws DispatchException {
        Request logoutRequest = new Request("AuthenticationHandler.invalidate");
        dispatch(logoutRequest, session);
    }

}
