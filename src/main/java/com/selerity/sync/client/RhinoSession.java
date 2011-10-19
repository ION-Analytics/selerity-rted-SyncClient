package com.selerity.sync.client;


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
 *  An implementation of Session which defines the specific header parameters that Selerity's 'Rhino' server implementation expects.
 * 
 *  Specifically, it supports the following header fields:
 *  
 *  user - the ID of the user who is invoking the method
 *  client - an identifier for the client application (mostly for debugging purposes now, may be enforced later)
 *  token - a security token generated during the authentication process (optional for some methods)
 *  mode - optional, can be used to indicate usage of certain non-standard packages (e.g. "extension")
 * 
 *
 */
public class RhinoSession extends SessionImpl {

	public static final String USER = "user";
	public static final String CLIENT = "client";
	public static final String TOKEN = "token";
	public static final String MODE = "mode";
	
	
	public RhinoSession(String user, String client, String token, String mode){
		setHeaderParameter(USER, user);
		setHeaderParameter(CLIENT, client);
		setHeaderParameter(TOKEN, token);
		setHeaderParameter(MODE, mode);
	}
	
	public String getUser(){
		return parameters.get(USER);
	}
	
	public String getClient(){
		return parameters.get(CLIENT);
	}
	
	public String getToken(){
		return parameters.get(TOKEN);
	}
	
	public String getMode(){
		return parameters.get(MODE);
	}
	
	
	
	

	
}
