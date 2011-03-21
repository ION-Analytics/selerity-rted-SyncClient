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
 *  Represents a generic JSON-RPC request separate from other header and invocation details.  This 
 *  consists of the name of the method to call and a map of method parameters.  The method parameters
 *  can be any object type that GSON knows how to serialize.  
 *    
 * 
 * @author andrewbrook
 *
 */

public class Request {

	protected final String method;
	protected JsonElement methodParameters;

	public Request(String method) {
		this.method = method;
		this.methodParameters = new JsonObject();
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

	public void setMethodParameters(JsonObject methodParameters) {
		this.methodParameters = methodParameters;
	}
	
	

	public JsonElement getMethodParameter(String parameterName){
		if (this.methodParameters.isJsonObject()){
			return this.methodParameters.getAsJsonObject().get(parameterName);
		}
		else{
			throw new IllegalStateException("cannot use object syntax with array parameters, already initialized with object parameters");
		}
	}
	
	

	public void setMethodParameter(String parameterName, String parameterValue){
		if (this.methodParameters.isJsonObject()){
			this.methodParameters.getAsJsonObject().addProperty(parameterName, parameterValue);
		}
		else{
			throw new IllegalStateException("cannot use object syntax with array parameters, already initialized with object parameters");
		}
	}

	public void setMethodParameter(String parameterName, JsonElement parameterValue){
		if (this.methodParameters.isJsonObject()){
			this.methodParameters.getAsJsonObject().add(parameterName, parameterValue);
		}
		else{
			throw new IllegalStateException("cannot use object syntax with array parameters, already initialized with object parameters");
		}
	}

	public void setMethodParameter(String parameterName, Number parameterValue){
		if (this.methodParameters.isJsonObject()){
			this.methodParameters.getAsJsonObject().addProperty(parameterName, parameterValue);
		}
		else{
			throw new IllegalStateException("cannot use object syntax with array parameters, already initialized with object parameters");
		}
	}

	public void setMethodParameter(String parameterName, Boolean parameterValue){
		if (this.methodParameters.isJsonObject()){
			this.methodParameters.getAsJsonObject().addProperty(parameterName, parameterValue);
		}
		else{
			throw new IllegalStateException("cannot use object syntax with array parameters, already initialized with object parameters");
		}
	}

	public void setMethodParameter(String parameterName, Character parameterValue){
		if (this.methodParameters.isJsonObject()){
			this.methodParameters.getAsJsonObject().addProperty(parameterName, parameterValue);
		}
		else{
			throw new IllegalStateException("cannot use object syntax with array parameters, already initialized with object parameters");
		}
	}
	
	
	
	
	
	// and the array equivalents
	public void setMethodParameters(JsonArray methodParameters) {
		this.methodParameters = methodParameters;
	}
	
	public JsonElement getMethodParameter(int parameterIndex){
		if (this.methodParameters.isJsonArray()){
			return this.methodParameters.getAsJsonArray().get(parameterIndex);
		}
		throw new IllegalStateException("cannot use object syntax with object parameters, already initialized with array parameters");
	}
	
	
	
	public void addMethodParameter(String parameterValue){
		if (this.methodParameters.isJsonArray()){
			this.methodParameters.getAsJsonArray().add(new JsonPrimitive(parameterValue));
		}
		else{
			throw new IllegalStateException("cannot use object syntax with object parameters, already initialized with array parameters");
		}
	}

	public void addMethodParameter(JsonElement parameterValue){
		if (this.methodParameters.isJsonArray()){
			this.methodParameters.getAsJsonArray().add(parameterValue);
		}
		else{
			throw new IllegalStateException("cannot use object syntax with object parameters, already initialized with array parameters");
		}
	}

	public void addMethodParameter(Number parameterValue){
		if (this.methodParameters.isJsonArray()){
			this.methodParameters.getAsJsonArray().add(new JsonPrimitive(parameterValue));
		}
		else{
			throw new IllegalStateException("cannot use object syntax with object parameters, already initialized with array parameters");
		}
	}

	public void addMethodParameter(Boolean parameterValue){
		if (this.methodParameters.isJsonArray()){
			this.methodParameters.getAsJsonArray().add(new JsonPrimitive(parameterValue));
		}
		else{
			throw new IllegalStateException("cannot use object syntax with object parameters, already initialized with array parameters");
		}
	}

	public void addMethodParameter(Character parameterValue){
		if (this.methodParameters.isJsonArray()){
			this.methodParameters.getAsJsonArray().add(new JsonPrimitive(parameterValue));
		}
		else{
			throw new IllegalStateException("cannot use object syntax with object parameters, already initialized with array parameters");
		}
	}

}
