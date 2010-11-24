package com.selerity.sync.client;

import java.util.HashMap;
import java.util.Map;

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
 *  An implementation of Session which defines the specific header parameters that Selerity's 'Rhino' server implementation expects.
 * 
 *  Specifically, it supports the following header fields:
 *  
 *  user - the ID of the user who is invoking the method
 *  client - an identifier for the client application (mostly for debugging purposes now, may be enforced later)
 *  token - a security token generated during the authentication process (optional for some methods)
 *  mode - optional, can be used to indicate usage of certain non-standard packages (e.g. "extension")
 * 
 * @author andrewbrook
 *
 */
public class RhinoSession implements Session {

	public static final String USER = "user";
	public static final String CLIENT = "client";
	public static final String TOKEN = "token";
	public static final String MODE = "mode";
	
	protected Map<String,String> parameters = new HashMap<String,String>();
	
	public RhinoSession(String user, String client, String token, String mode){
		parameters.put(USER, user);
		parameters.put(CLIENT, client);
		parameters.put(TOKEN, token);
		parameters.put(MODE, mode);
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
	
	public String getHeaderParameter(String parameterName){
		return parameters.get(parameterName);
	}
	
	

	
}
