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
 * An abstract interface for accessing a Narwhal-based service.
 *
 */

public interface NarwhalService {

	public String getName();
	
	public JsonElement dispatch(Request request, Session session) throws DispatchException;
	
	public Response dispatchWithResponse(Request request, Session session) throws DispatchException;
	
	public JsonReader dispatch(FullRequest request) throws Exception;
	
}
