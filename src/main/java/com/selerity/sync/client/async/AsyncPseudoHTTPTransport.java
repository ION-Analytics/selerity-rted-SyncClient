package com.selerity.sync.client.async;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
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
 * @author andrewbrook
 *
 */

public class AsyncPseudoHTTPTransport implements AsyncTransport, Runnable{

	private static final Log log = LogFactory.getLog (AsyncPseudoHTTPTransport.class);

	public static final String DEFAULT_HTTP_ACTION = "POST";
	public static final String DEFAULT_HTTP_RESOURCE = "pseudo.do";
	
	protected final String host;
	protected final int port;
	protected final String pseudoRequestHeader;

	protected final Gson gson;

	protected Socket socket = null;
	protected InputStream in = null;
	protected OutputStream out = null;
	protected boolean isFirstRequest = true;
	
	protected final String name;

	protected AsyncTransportListener listener = null;


	public AsyncPseudoHTTPTransport(String httpAction, String resource, String host, int port, String name) {
		this.host = host;
		this.port = port;
		this.pseudoRequestHeader = httpAction + " /" + resource + " HTTP/1.1\n"
						+ "Accept: text/plain\n"
						+ "Content-type: application/x-json\n"
						+ "User-Agent: Java/SyncClient\n"
						+ "Host: " + host + ":" + port + "\n"
						+ "Connection: keep-alive";
		this.name = name;

		GsonBuilder builder = new GsonBuilder().serializeNulls();
		builder.registerTypeAdapter(FullRequest.class, new FullRequest.FullRequestDeserializer());
		builder.registerTypeAdapter(FullRequest.class, new FullRequest.FullRequestSerializer());
		builder.registerTypeAdapter(Response.class, new Response.ResponseDeserializer());
		builder.registerTypeAdapter(Response.class, new Response.ResponseSerializer());
		gson = builder.create();
	}

	public synchronized void start() throws UnknownHostException, IOException{
		socket = new Socket(host, port);
		in = socket.getInputStream();
		out = socket.getOutputStream();
		log.debug("connected " + name);

		Thread th = new Thread(this, name + "_reader");
		th.setDaemon(true);
		th.start();
		log.debug("started reader thread " + th);
	}


	public synchronized void addAsyncTransportListener(AsyncTransportListener listener){
		if (this.listener != null){
			throw new IllegalArgumentException("cannot add a second listener to this transport");
		}
		this.listener = listener;
	}


	public String getHost(){
		return host;
	}

	public int getPort(){
		return port;
	}

	public synchronized void close(){
		log.debug("closing " + name);
		try{
			if (in != null){
				in.close();
			}
			if (out != null){
				out.close();
			}
			if (socket != null){
				socket.close();
			}
		}
		catch (Exception ex){
			// ignore
		}
		finally{
			socket = null;
			in = null;
			out = null;
			notifyAll();
		}
		log.debug("closed " + name);
	}

	public synchronized boolean isConnected(){
		if (socket == null){
			return false;
		}
		return socket.isConnected();
	}


	public void asyncDispatch(FullRequest request) throws DispatchException{

		String jsonRequestString = gson.toJson(request, FullRequest.class);

		if (log.isDebugEnabled()){
        	log.debug("preparing to send " + jsonRequestString + " to " + name);
        }
		
		// record start time of dispatch
		if (log.isDebugEnabled()){
			log.debug("starting dispatch via " + name + " of " + summarize(jsonRequestString, 50));
		}

		final boolean includeHeader;
		synchronized(this){
			if (isFirstRequest){
				includeHeader = true;
				isFirstRequest = false;
			}
			else{
				includeHeader = false;
			}
		}
		
		// combine the output pieces:
		try{
			final String httpRequestString;
			if (includeHeader){
				// this is the first request, need it to look like a valid HTTP request
				byte[] contentBytes = jsonRequestString.getBytes("UTF-8");
				int contentLength = contentBytes.length;  // yes, we need this for some servers...  ugh.
				httpRequestString = pseudoRequestHeader + "\nContent-length: " + contentLength + ((char)13) + ((char)10) + ((char)13) + ((char)10) + jsonRequestString;
			}
			else{
				// it's not the first request, no need for the header
				httpRequestString = "" + ((char)13) + ((char)10) + ((char)13) + ((char)10) + jsonRequestString;
			}
	
			// send the request
			byte[] bytesToWrite = httpRequestString.getBytes("UTF-8");
			synchronized (this){
				out.write(bytesToWrite);
				out.flush();
			}
		}
		catch (Exception ex){
			log.error("caught " + ex + " while dispatching " + summarize(jsonRequestString, 50), ex);
			throw new DispatchException(DispatchException.INTERNAL_ERROR, "failed to send due to " + ex, ex.toString());
		}
	}


	public void run() {

		// strip the HTTP response header and get a JsonReader
		JsonReader reader = null;
		try{
			if (!stripHeader()){
				if (isConnected()){
					log.debug("failed to strip header, giving up");
					close();
				}
				return;
			}
			reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
			reader.setLenient(true); // this allows reading multiple top-level values
		}
		catch (Exception ex){
			log.error("failed to parse response due to " + ex, ex);
			close();
			return;
		}

		try{
			while (true){
				log.debug("waiting for a response");
				// read a single response from the socket
				Response response = gson.fromJson(reader, Response.class);
				log.debug("got response to id " + response.getID());
				if (listener == null){
					log.error("no listener for " + name + ", discarding response");
				}
				else{
					listener.onResponse(response);
				}
				log.debug("notified listener to response to id " + response.getID());
			}
		}
		catch (Exception ex){
			if (isConnected()){
				log.error("caught " + ex + " while reading responses for " + name + "; closing", ex);
				close();
			}
		}
	}


	/** Consumes an input stream until the 4-byte string 0x0d0a0d0a is found.  Leaves the stream pointing at the next byte after the pattern. 
	 * 
	 * @return
	 * @throws IOException
	 */
	protected boolean stripHeader() throws IOException{
		log.debug("stripping header on " + name + "...");
		int mode = 0;
		while (true){
			int c = in.read();
			if (c < 0){
				log.debug("failed to strip header on " + name);
				return false;
			}
			switch (mode){
			case 0:
				if (c == 13){
					mode = 1;
				}
				else{
					mode = 0;
				}
				break;

			case 1:
				if (c == 10){
					mode = 2;
				}
				else if (c == 13){
					mode = 1;
				}
				else{
					mode = 0;
				}
				break;

			case 2:
				if (c == 13){
					mode = 3;
				}
				else{
					mode = 0;
				}
				break;

			case 3:
				if (c == 10){
					log.debug("done stripping header on " + name);
					return true;
				}
				else if (c == 13){
					mode = 1;
				}
				else{
					mode = 0;
				}
				break;
			}
		}
	}

	protected String summarize(String s, int maxLength){
		if (s.length() > maxLength){
			s = s.substring(0, maxLength);
		}
		s = s.replace('\n', ' ');
		s = s.replace('\t', ' ');
		return s;
	}
	
	public String toString(){
		return name;
	}
}
