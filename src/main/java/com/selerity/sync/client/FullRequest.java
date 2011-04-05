package com.selerity.sync.client;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

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
 * @author andrewbrook
 *
 */

public class FullRequest {
	
	public static class FullRequestDeserializer implements JsonDeserializer<FullRequest> {
		public FullRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject obj = json.getAsJsonObject();
			String method = obj.get("method").getAsString();
			JsonElement params = obj.get("params");
			JsonObject header = obj.get("header").getAsJsonObject();
			String id = obj.get("id").getAsString();
			if (params.isJsonObject()){
				return new FullRequest(method, params.getAsJsonObject(), header, id);
			}
			else if (params.isJsonArray()){
				return new FullRequest(method, params.getAsJsonArray(), header, id);
			}
			else{
				throw new IllegalStateException("params must be either JsonObject or JsonArray");
			}
		}
	}
	
	public static class FullRequestSerializer implements JsonSerializer<FullRequest> {
		public JsonElement serialize(FullRequest proxyRequest, Type typeOfT, JsonSerializationContext context) throws JsonParseException {
			JsonObject proxyRequestObj = new JsonObject();
			proxyRequestObj.addProperty("method", proxyRequest.method);
			proxyRequestObj.add("params", proxyRequest.params);
			proxyRequestObj.add("header", proxyRequest.header);
			proxyRequestObj.addProperty("id", proxyRequest.id);

			return proxyRequestObj;
		}
	}

	protected String method;
	protected JsonElement params;
	protected JsonObject header;
	protected String id;
	
	public FullRequest(){
		
	}

	
	public FullRequest(String method, JsonObject params, JsonObject header, String id) {
		this.method = method;
		this.params = params;
		this.header = header;
		this.id = id;
	}
	
	public FullRequest(String method, JsonArray params, JsonObject header, String id) {
		this.method = method;
		this.params = params;
		this.header = header;
		this.id = id;
	}
	
	public FullRequest(Request partialRequest, String user, String token, String client, String mode, String id){
		this.method = partialRequest.getMethod();
		this.params = partialRequest.getMethodParameters();
		this.header = new JsonObject();
		header.addProperty(RhinoSession.USER, user);
		header.addProperty(RhinoSession.TOKEN, token);
		header.addProperty(RhinoSession.CLIENT, client);
		header.addProperty(RhinoSession.MODE, mode);
		this.id = id;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public JsonElement getParams() {
		return params;
	}

	public void setParams(JsonObject params) {
		this.params = params;
	}

	public JsonObject getHeader() {
		return header;
	}

	public void setHeader(JsonObject header) {
		this.header = header;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
}
