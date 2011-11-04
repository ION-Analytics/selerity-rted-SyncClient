package com.selerity.sync.client;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

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
 *  Encapsulates a response to a Narwhal service call including both the result and the exception.  Note that in the case 
 *  of success the exception field will be null while in the case of an error the result may (or may not) be 
 *  null.
 * 
 *
 */
public class Response {
	
	public static class ResponseDeserializer implements JsonDeserializer<Response> {
		public Response deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			
			// assume the json string corresponds to an object of the form of a valid JSON-RPC response
			JsonObject obj = json.getAsJsonObject();
			
			// get the result (but don't parse it down further
			JsonElement result = obj.get("result");
			
			// get the error, parsed as a DispatchException
			JsonElement errorElem = obj.get("error");
			DispatchException error = null;
			if ((errorElem != null) && (!errorElem.isJsonNull())){
				JsonObject errorObj = errorElem.getAsJsonObject();
				int code = MiscUtils.getInt(errorObj, "code", 0);
				String message = MiscUtils.getString(errorObj, "message", null);
				JsonElement data = errorObj.get("data");
				error = new DispatchException(code, message, data);
			}
			
			// get the id
			String id = MiscUtils.getString(obj, "id", null);

			// get the header (but don't bother to parse it down further)
			JsonObject header = MiscUtils.getJsonObject(obj, "header", null);
			
			// return the response
			return new Response(result, error, id, header);
		}
	}
	
	public static class ResponseSerializer implements JsonSerializer<Response> {
		public JsonElement serialize(Response response, Type typeOfT, JsonSerializationContext context)  {
			JsonObject responseObj = new JsonObject();
			responseObj.add("result", response.result);
			if (response.isError()){
				DispatchException error = response.getError();
				JsonObject errorObj = new JsonObject();
				errorObj.addProperty("code", error.getCode());
				errorObj.addProperty("message", error.getMessage());
				errorObj.add("data", error.getData());
				responseObj.add("error", errorObj);
			}
			else{
				responseObj.add("error", null);
			}			
			responseObj.addProperty("id", response.id);
			responseObj.add("header", response.header);
			return responseObj;
		}
	}

	protected final JsonElement result;
	protected final DispatchException error;
	protected final String id;
	protected final JsonObject header;
	
	/** Creates a response with the given result, error, id and header.  Throws an illegal argument exception if the result and error are both non-null.
	 * 
	 * @param result
	 * @param error
	 * @param id
	 * @param header
	 */
	public Response(JsonElement result, DispatchException error, String id, JsonObject header) {
		
		if ((result != null) && (!result.isJsonNull()) && (error != null)){
			throw new IllegalArgumentException("result and error cannot both be set in a response");
		}
		
		this.result = result;
		this.error = error;
		this.id = id;
		this.header = header;
	}
	
	/** Creates a response with the given result, error, id and header fields.  Throws an illegal argument exception if the result and error are both non-null.
	 * 
	 * @param result
	 * @param error
	 * @param id
	 * @param header
	 */
	public Response(JsonElement result, DispatchException error, String id, String resultAPI, String resultType, boolean more) {
		
		if ((result != null) && (!result.isJsonNull()) && (error != null)){
			throw new IllegalArgumentException("result and error cannot both be set in a response");
		}
		
		this.result = result;
		this.error = error;
		this.id = id;
		this.header = new JsonObject();
		header.addProperty("result-api", resultAPI);
		header.addProperty("result-type", resultType);
		header.addProperty("more", more);
	}
	
	
	/** Creates a response with the given result, id and header fields.  Throws an illegal argument exception if the result and error are both non-null.
	 * 
	 * @param result
	 * @param error
	 * @param id
	 * @param header
	 */
	public Response(JsonElement result, String id, String resultAPI, String resultType, boolean more) {
		this(result, null, id, resultAPI, resultType, more);
	}
	
	
	/** Creates a response with the given error, id and header fields.  Throws an illegal argument exception if the result and error are both non-null.
	 * 
	 * @param result
	 * @param error
	 * @param id
	 * @param header
	 */
	public Response(DispatchException error, String id, String resultAPI, boolean more) {
		this(null, error, id, resultAPI, "void", more);
	}
	
	

	/** Returns the result as a raw JsonElement.
	 * 
	 * @return
	 */
	public JsonElement getResult() {
		return result;
	}

	/** Returns the error in the form of a DispatchException.
	 * 
	 * @return
	 */
	public DispatchException getError() {
		return error;
	}
	
	/** Returns the request ID to which this response is associated.
	 * 
	 * @return
	 */
	public String getID(){
		return id;
	}
	
	/** Returns the header as a raw JsonObject.
	 * 
	 * @return
	 */
	public JsonObject getHeader(){
		return header;
	}
	
	/** Returns true if this response contains an error.
	 * 
	 * @return
	 */
	public boolean isError(){
		return error != null;
	}
	
	
	/** Returns the result-api header field if it's set.  If not set then returns null.
	 * 
	 * @return
	 */
	public String getResultAPI(){
		if ((header == null) || (header.isJsonNull())){
			return null;
		}
		return MiscUtils.getString(header, "result-api", null);
	}
	
	/** Returns the result-type header field if it's set.  If not set then returns null.
	 * 
	 * @return
	 */
	public String getResultType(){
		if ((header == null) || (header.isJsonNull())){
			return null;
		}
		return MiscUtils.getString(header, "result-type", null);
	}
	
	/** Returns true if this response has set the "more" field of its header to true.  If
	 *  the field is false or missing or the header itself is missing then returns false;
	 * 
	 * @return
	 */
	public boolean hasMore(){
		if ((header == null) || (header.isJsonNull())){
			return false;
		}
		return MiscUtils.getBoolean(header, "more", false);
	}

}
