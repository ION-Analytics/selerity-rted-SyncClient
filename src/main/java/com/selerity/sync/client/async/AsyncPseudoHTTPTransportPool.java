package com.selerity.sync.client.async;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.FullRequest;
import com.selerity.sync.client.Response;

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
 *
 */

public class AsyncPseudoHTTPTransportPool implements AsyncTransport, Runnable, AsyncTransportListener{
	
	private static final Log log = LogFactory.getLog (AsyncPseudoHTTPTransportPool.class);

	protected final AgedPool<AsyncPseudoHTTPTransport> transports;
	
	protected final String host;
	protected final int port;
	protected final int minPoolSize;
	
	protected final String httpAction;
	protected final String httpResource;
	
	protected int nextTransportNumber = 0;
	
	protected AsyncTransportListener listener = null;
	
	protected long checkIntervalMillis;
	protected long startIntervalMillis;
	
	
	AsyncPseudoHTTPTransportPool(String httpAction, String httpResource, String host, int port, int minPoolSize, long activeIntervalMillis, long retirementIntervalMillis){
		this.httpAction = httpAction;
		this.httpResource = httpResource;
		this.host = host;
		this.port = port;
		this.minPoolSize = minPoolSize;
		transports = new AgedPool<AsyncPseudoHTTPTransport>(activeIntervalMillis, retirementIntervalMillis);
	}
	
	public void start(long checkIntervalMillis, long startIntervalMillis){
		this.checkIntervalMillis = checkIntervalMillis;
		this.startIntervalMillis = startIntervalMillis;
		
		startNewTransport(); // make sure there's at least one!
		
		// start the thread to do regular checking
		Thread th = new Thread(this, "transportHealthChecker");
		th.setDaemon(true);
		th.start();
		log.debug("started " + th);
	}
	
	public synchronized void addAsyncTransportListener(AsyncTransportListener listener){
		if (this.listener != null){
			throw new IllegalArgumentException("cannot add a second listener to this transport");
		}
		this.listener = listener;
	}

	public void asyncDispatch(FullRequest request) throws DispatchException{
		AsyncPseudoHTTPTransport transport = null;
		synchronized (transports){
			transport = transports.getNextActive();
		}
			
		log.debug("got transport " + transport + " from pool");
		
		if (transport == null){
			throw new DispatchException(DispatchException.INTERNAL_ERROR, "no available transports");
		}
			
		try{
			transport.asyncDispatch(request);
		}
		catch (DispatchException dx){
			// fail this dispatch and take the transport out of the pool
			log.error("failed to dispatch due to " + dx + ", taking transport " + transport + " out of the pool");
			throw dx;
		}
		
		synchronized(transports){
			log.debug("adding " + transport + " back into the pool");
			transports.returnToActive(transport);
		}
	}
	
	public void onResponse(Response response){
		if (listener != null){
			listener.onResponse(response);
		}
		else{
			log.error("no listener defined, discarding response to id \"" + response.getID() + "\"");
		}
	}
	
	
	public void run(){
		while (true){
			// first, check retirement ages of exiting transports.  Yes, this stops the world
			log.debug("checking ages of transports");
			Set<AsyncPseudoHTTPTransport> retired;
			synchronized(transports){
				retired = transports.getRetired();
			}
			for (AsyncPseudoHTTPTransport transport : retired){
				transport.close();
			}
			log.debug("retired " + retired.size() + " old transports");
			
			// now see how many we need to start
			int startCount = 0;
			synchronized(transports){
				startCount = (minPoolSize - transports.getActiveCount());
			}
			log.debug("need to start " + startCount + " new transports");
			for (int i = 0; i < startCount; i++){
				startNewTransport();
				if (i < startCount){
					try{
						log.debug("sleeping a while before starting another transport");
						Thread.sleep(startIntervalMillis);
					}
					catch (Exception ex){
						// ignore
					}
				}
			}
			log.debug("finished starting new transports, sleeping a while until next check");
			try{
				Thread.sleep(checkIntervalMillis);
			}
			catch (Exception ex){
				// ignore
			}
		}
	}
	

	protected synchronized void startNewTransport(){
		int transportNumber = nextTransportNumber++;
		AsyncPseudoHTTPTransport transport = new AsyncPseudoHTTPTransport(httpAction, httpResource,
				host, port, host + ":" + port + ":" + transportNumber);
		try{
			log.debug("starting transport " + transport + "...");
			synchronized(transports){
				transports.addNew(transport);
			}
			log.debug("transport " + transport + " started");
		}
		catch (Exception ex){
			log.error("caught " + ex + " while trying to start transport " + transport + "; giving up");
			transport.close();
		}
		
	}
	
	
}


