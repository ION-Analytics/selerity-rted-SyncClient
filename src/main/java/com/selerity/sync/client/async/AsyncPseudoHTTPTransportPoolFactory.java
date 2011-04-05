package com.selerity.sync.client.async;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

public class AsyncPseudoHTTPTransportPoolFactory implements AsyncTransportFactory{

	private static final Log log = LogFactory.getLog (AsyncPseudoHTTPTransportPoolFactory.class);

	protected final String httpAction;
	protected final String httpResource;
	protected final String host;
	protected final int port;
	protected final long checkIntervalMillis;
	protected final long startIntervalMillis;
	protected final long maxTransportAgeMillis;
	protected final int minPoolSize;


	public AsyncPseudoHTTPTransportPoolFactory(String httpAction, String httpResource,
			String host, int port,
			long checkIntervalMillis, long startIntervalMillis,
			long maxTransportAgeMillis, int minPoolSize) {
		this.httpAction = httpAction;
		this.httpResource = httpResource;
		this.host = host;
		this.port = port;
		this.checkIntervalMillis = checkIntervalMillis;
		this.startIntervalMillis = startIntervalMillis;
		this.maxTransportAgeMillis = maxTransportAgeMillis;
		this.minPoolSize = minPoolSize;
	}
	
	public AsyncPseudoHTTPTransportPoolFactory(String host, int port,
			long checkIntervalMillis, long startIntervalMillis,
			long maxTransportAgeMillis, int minPoolSize) {
		this(AsyncPseudoHTTPTransport.DEFAULT_HTTP_ACTION, AsyncPseudoHTTPTransport.DEFAULT_HTTP_RESOURCE,
				host, port, 
				checkIntervalMillis, startIntervalMillis, maxTransportAgeMillis, 
				minPoolSize);
	}

	public AsyncTransport getInstance() throws Exception{
		AsyncPseudoHTTPTransportPool transportPool = new AsyncPseudoHTTPTransportPool(httpAction, httpResource, host, port, minPoolSize);
		transportPool.start(checkIntervalMillis, startIntervalMillis, maxTransportAgeMillis);
		log.debug("started pool to " + host + ":" + port + " with min size " + minPoolSize 
				+ ", checking every " + checkIntervalMillis + "ms, retiring transports with max age " + maxTransportAgeMillis
				+ " ms and with gaps of " + startIntervalMillis + " ms between starts");
		return transportPool;
	}

}
