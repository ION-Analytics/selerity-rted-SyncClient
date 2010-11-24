package com.selerity.sync.client;


/** 
 * © Copyrights Selerity, Inc. 2009-2010. All rights reserved. This source code is confidential 
 * and proprietary information of Selerity Inc. and may be used only by a recipient designated by 
 * and for the purposes permitted by Selerity Inc. in writing.  Reproduction of, dissemination of, 
 * modifications to or creation of derivative works from this source code, whether in source or 
 * binary forms, by any means and in any form or manner, is expressly prohibited, except with the 
 * prior written permission of Selerity Inc..  THIS CODE AND INFORMATION ARE PROVIDED ÒAS ISÓ 
 * WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE. This notice may not be 
 * removed from the software by any user thereof. 
 * 
 * A DispatchException can be thrown when dispatching a JSON-RPC method call.  The exception can be
 *  caused by an error on the client side or the server.  A set of defined error codes is included as
 *  public constants.
 * 
 * @author andrewbrook
 *
 */
public class DispatchException extends Exception{

	/** A static serial version UID
	 */
	private static final long serialVersionUID = 129823652L;
	
	
	
	public static final int OTHER_ERROR = 0;
	public static final int AUTHENTICATION_ERROR = -1000;
	public static final int AUTHENTICATION_INVALID_TOKEN_ERROR = -1001;
	public static final int ENTITLEMENTS_ERROR = -2000;
	public static final int PARSE_ERROR = -32700;
	public static final int INVALID_REQUEST = -32600;
	public static final int METHOD_NOT_FOUND = -32601;
	public static final int INVALID_PARAMETERS_ERROR = -32602;
	public static final int INTERNAL_ERROR = -32603;
	
	
	
	private int code;
    private String message;
    private String data;
    
    
	public DispatchException(int code, String message, String data) {
		super(message);
		this.code = code;
		this.message = message;
		this.data = data;
	}
	
	public DispatchException(int code, String message, String data, Exception nestedException) {
		super(message, nestedException);
		this.code = code;
		this.message = message;
		this.data = data;
	}
	

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public String getData() {
		return data;
	}
	
	
    
}
