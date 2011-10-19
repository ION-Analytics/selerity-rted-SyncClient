package com.selerity.sync.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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
 *  Represents a generic Narwhal request separate from other header and invocation details.  This 
 *  consists of the name of the method to call and a map of method parameters.  The method parameters
 *  can be any object type that GSON knows how to serialize.  
 *    
 *
 */

public class Request {

	protected final String method;
	protected JsonElement methodParameters;

	public Request(String method) {
		this.method = method;
		this.methodParameters = new JsonObject();
	}

	public Request(String method, JsonElement methodParameters) {
		if (methodParameters.isJsonArray() || methodParameters.isJsonObject()){
			this.method = method;
			this.methodParameters = methodParameters;
		}
		else{
			throw new IllegalArgumentException("method parameters must be either an array or an object");
		}
	}

	public Request(String method, JsonObject methodParameters) {
		this.method = method;
		this.methodParameters = methodParameters;
	}

	public Request(String method, JsonArray methodParameters) {
		this.method = method;
		this.methodParameters = methodParameters;
	}


	public String getMethod() {
		return method;
	}


	public JsonElement getMethodParameters() {
		return methodParameters;
	}

	public JsonObject getMethodParametersAsObject(){
		if (this.methodParameters.isJsonObject()){
			return this.methodParameters.getAsJsonObject();
		}
		if (this.methodParameters.isJsonArray()){
			if (this.methodParameters.getAsJsonArray().size() < 1){
				// convert to object format since the array hasn't been filled in yet
				this.methodParameters = new JsonObject();
				return this.methodParameters.getAsJsonObject(); 
			}
			throw new IllegalStateException("can't convert from array format params to object format params because array has non-zero length");
		}
		else{
			throw new IllegalStateException("unknown params format");
		}
	}

	public JsonArray getMethodParametersAsArray(){
		if (this.methodParameters.isJsonArray()){
			return this.methodParameters.getAsJsonArray();
		}
		if (this.methodParameters.isJsonObject()){
			if (this.methodParameters.getAsJsonObject().entrySet().size() < 1){
				// convert to object format since the array hasn't been filled in yet
				this.methodParameters = new JsonArray();
				return this.methodParameters.getAsJsonArray(); 
			}
			throw new IllegalStateException("can't convert from object format params to array format params because object has non-zero size");
		}
		else{
			throw new IllegalStateException("unknown params format");
		}
	}




	//
	// object-style getters and setters
	//


	public void setMethodParameters(JsonObject methodParameters) {
		this.methodParameters = methodParameters;
	}

	public JsonElement getMethodParameter(String parameterName){
		return getMethodParametersAsObject().get(parameterName);
	}


	public void setMethodParameter(String parameterName, JsonElement parameterValue){
		getMethodParametersAsObject().add(parameterName, parameterValue);
	}

	public void setMethodParameter(String parameterName, String parameterValue){
		getMethodParametersAsObject().addProperty(parameterName, parameterValue);
	}

	public void setMethodParameter(String parameterName, Number parameterValue){
		getMethodParametersAsObject().addProperty(parameterName, parameterValue);
	}

	public void setMethodParameter(String parameterName, Boolean parameterValue){
		getMethodParametersAsObject().addProperty(parameterName, parameterValue);
	}

	public void setMethodParameter(String parameterName, Character parameterValue){
		getMethodParametersAsObject().addProperty(parameterName, parameterValue);
	}




	//
	// array-style getters and setters
	//

	public void setMethodParameters(JsonArray methodParameters) {
		this.methodParameters = methodParameters;
	}

	public JsonElement getMethodParameter(int parameterIndex){
		return getMethodParametersAsArray().get(parameterIndex);
	}

	public void addMethodParameter(String parameterValue){
		getMethodParametersAsArray().add(new JsonPrimitive(parameterValue));
	}

	public void addMethodParameter(JsonElement parameterValue){
		getMethodParametersAsArray().add(parameterValue);
	}

	public void addMethodParameter(Number parameterValue){
		getMethodParametersAsArray().add(new JsonPrimitive(parameterValue));
	}

	public void addMethodParameter(Boolean parameterValue){
		getMethodParametersAsArray().add(new JsonPrimitive(parameterValue));
	}

	public void addMethodParameter(Character parameterValue){
		getMethodParametersAsArray().add(new JsonPrimitive(parameterValue));
	}

}
