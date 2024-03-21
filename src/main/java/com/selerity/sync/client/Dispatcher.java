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
 * A Dispatcher is responsible for converting the logical representation of a Request and some header parameters into the JSON-RPC format used by Selerity.
 *  Dispatching can be done using explicit header parameters (some of which can optionally be null) or using a Session.
 *
 *  Dispatching returns the results of the method call (as a JSON element, the type of which depends on the method called) or throws an exception.  If the exception
 *  is generated on the server side then it will contain the appropriate error code and message.
 *
 *  "Boxcar" dispatching is the technique of batching together multiple requests and sending them all as a single operation with common header parameters.  The sender should *not*
 *  depend on the requests actually being serviced in any particular order but can assume that responses will be held on the server side until all requests are completed.
 *
 *  Note that for single request dispatching, the result is parsed out of the response whereas for the boxcar dispatching the response is returned as an object (containing the 
 *  result and exception) since it's possible for some requests to succeed and some to fail.
 */
public interface Dispatcher {

    /** Dispatches a request using the explicit session information, returning the result or throwing an exception.
     *
     * @param request
     * @param user
     * @param token
     * @param client
     * @param mode
     * @return
     * @throws DispatchException
     */
    JsonElement dispatch(Request request, String user, String token, String client, String mode) throws DispatchException;

    /** Dispatches a request using the explicit session information, returning the full response object.
     *
     * @param request
     * @param user
     * @param token
     * @param client
     * @param mode
     * @return
     * @throws DispatchException
     */
    Response dispatchWithResponse(Request request, String user, String token, String client, String mode) throws DispatchException;

    /** Dispatches a request using the given session, returning the result or throwing an exception.
     *
     * @param request
     * @param session
     * @return
     * @throws DispatchException
     */
    JsonElement dispatch(Request request, Session session) throws DispatchException;

    /** Dispatches an array of requests using the explicit session information.  Returns the matching array of responses.
     *
     * @param requests
     * @param user
     * @param token
     * @param client
     * @param mode
     * @return
     * @throws DispatchException
     */
    Response[] boxcarDispatch(Request[] requests, String user, String token, String client, String mode) throws DispatchException;

    /** Dispatches an array of requests using the given session.  Returns the matching array of responses.
     *
     * @param requests
     * @param session
     * @return
     * @throws DispatchException
     */
    Response[] boxcarDispatch(Request[] requests, Session session) throws DispatchException;

}
