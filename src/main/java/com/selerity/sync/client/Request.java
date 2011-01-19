package com.selerity.sync.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
 *  Represents a generic JSON-RPC request separate from other header and invocation details.  This 
 *  consists of the name of the method to call and a map of method parameters.  The method parameters
 *  can be any object type that GSON knows how to serialize.  
 *    
 * 
 * @author andrewbrook
 *
 */

public class Request {

	protected String method;
	protected JsonObject methodParameters;
	
	public Request(String method) {
		super();
		this.method = method;
		this.methodParameters = new JsonObject();
	}
	
	public Request(String method, JsonObject methodParameters) {
		super();
		this.method = method;
		this.methodParameters = methodParameters;
	}
	

	
	

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	


	public JsonObject getMethodParameters() {
		return methodParameters;
	}

	public void setMethodParameters(JsonObject methodParameters) {
		this.methodParameters = methodParameters;
	}
	
	public JsonElement getMethodParameter(String parameterName){
		return this.methodParameters.get(parameterName);
	}
	
	public void setMethodParameter(String parameterName, String parameterValue){
		this.methodParameters.addProperty(parameterName, parameterValue);
	}
	
	public void setMethodParameter(String parameterName, JsonElement parameterValue){
		this.methodParameters.add(parameterName, parameterValue);
	}
	
	public void setMethodParameter(String parameterName, Number parameterValue){
		this.methodParameters.addProperty(parameterName, parameterValue);
	}

	public void setMethodParameter(String parameterName, Boolean parameterValue){
		this.methodParameters.addProperty(parameterName, parameterValue);
	}
	
	public void setMethodParameter(String parameterName, Character parameterValue){
		this.methodParameters.addProperty(parameterName, parameterValue);
	}
	
	
	
	
	
}
