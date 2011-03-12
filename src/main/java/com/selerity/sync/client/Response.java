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
 *  Encapsulates a response to a mathod call including both the result and the exception.  Note that in the case 
 *  of success the exception field will be null while in the case of an error the result may (or may not) be 
 *  null.
 * 
 * @author andrewbrook
 *
 */
public class Response {

	protected final JsonElement result;
	protected final DispatchException exception;
	protected final String id;
	protected final JsonObject header;
	
	public Response(JsonElement result, DispatchException exception, String id, JsonObject header) {
		this.result = result;
		this.exception = exception;
		this.id = id;
		this.header = header;
	}
	
	public Response(DispatchException exception, String id) {
		this.result = null;
		this.exception = exception;
		this.id = id;
		this.header = new JsonObject();
		header.addProperty("result-type", "void");
	}

	public JsonElement getResult() {
		return result;
	}

	public DispatchException getException() {
		return exception;
	}
	
	public String getID(){
		return id;
	}
	
	public JsonObject getHeader(){
		return header;
	}
	
	public boolean isError(){
		return exception != null;
	}

}
