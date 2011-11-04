package com.selerity.sync.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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


public abstract class AbstractNawhalServiceImpl implements NarwhalService{
	
	protected final Gson gson;
	
	protected final long WARN_DISPATCH_TIME_MILLIS = 30000;  // 30 seconds is too long for most dispatches to take

	protected String serviceName = null;
	
	public AbstractNawhalServiceImpl(String serviceName){
		GsonBuilder builder = new GsonBuilder().serializeNulls();
		builder.registerTypeAdapter(FullRequest.class, new FullRequest.FullRequestDeserializer());
		builder.registerTypeAdapter(FullRequest.class, new FullRequest.FullRequestSerializer());
		builder.registerTypeAdapter(Response.class, new Response.ResponseDeserializer());
		builder.registerTypeAdapter(Response.class, new Response.ResponseSerializer());
		gson = builder.create();
		this.serviceName = serviceName;
	}
	
	public void setName(String serviceName){
		this.serviceName = serviceName;
	}
	
	public String getName(){
		return this.serviceName;
	}
	

	
}
