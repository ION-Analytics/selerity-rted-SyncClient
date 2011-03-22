package com.selerity.sync.client;

import java.net.MalformedURLException;

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
 * A factory which creates transports suitable for querying Rhino over HTTP directly (not via a proxy).
 * 
 * @author andrewbrook
 *
 */
public class RhinoHTTPTransportFactory implements TransportFactory {

	public final static String RHINO_RPC_RESOURCE = "rhino-1.0-SNAPSHOT/rpc.do";
	
	protected final String serviceURL;
	protected final boolean urlEncode;

	public RhinoHTTPTransportFactory(String host, int port) {
		this(host, port, RHINO_RPC_RESOURCE, true);
	}
	
	public RhinoHTTPTransportFactory(String host, int port, String rhinoRPCResource) {
		this(host, port, rhinoRPCResource, true);
	}
	
	public RhinoHTTPTransportFactory(String host, int port, boolean urlEncode) {
		this(host, port, RHINO_RPC_RESOURCE, urlEncode);
	}
	
	public RhinoHTTPTransportFactory(String host, int port, String rhinoRPCResource, boolean urlEncode) {
		this.serviceURL = "http://" + host + ":" + port + "/" + rhinoRPCResource;
		this.urlEncode = urlEncode;
	}
	
	public Transport getInstance() throws MalformedURLException{
		return new RhinoHTTPTransport(serviceURL, urlEncode);
	}
	
}
