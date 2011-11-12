package com.selerity.sync.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;


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
 * Dispatches requests to whichever service is advertising support from amongst those registered.
 *  
 * This depends on the services implementing the Narhwal introspection services.
 * 
 * If two or more services advertise support for the same method name (ignoring arguments) then
 * dispatch will be attempted in the order of service registration.  
 * 
 * In general, if dispatch fails on a given service endpoint then the next endpoint which
 * supports a method with that name will be selected and dispatched to.
 *
 */
public class NarwhalMetaServiceImpl extends AbstractNawhalServiceImpl {

	private static final Log log = LogFactory.getLog (NarwhalMetaServiceImpl.class);

	protected final List<NarwhalService> serviceEndpoints = new ArrayList<NarwhalService>();
	protected final Map<String,List<NarwhalService>> serviceMap = new HashMap<String,List<NarwhalService>>();
	
	
	public NarwhalMetaServiceImpl(){
		super(null);
	}
	
	/** 
	 * Convenience constructor to initialize and add services based on the given urls.
	 * Equivalient to calling zero-args constructor followed by <code>addServices(urls)</code>.
	 * 
	 * 
	 * @param urls one or more URL's separated by semi-colons (';'). 
	 * Example: <code>"http://server1.example.com:8080/rpc.do;http://server2.example.com:49123/something/query.do"</code>
	 * @throws MalformedURLException
	 * @throws DispatchException
	 */
	public NarwhalMetaServiceImpl(String urls) throws MalformedURLException, DispatchException{
		super(null);
		addServices(urls);
	}
	
	/**
	 * Looks up the services offered by this endpoint and registers it to handle future requests.
	 * 
	 * @param host
	 * @param port
	 * @param resource
	 * @param introspectionUser
	 * @param introspectionPassword
	 * @throws MalformedURLException
	 * @throws DispatchException
	 */
	public void addService(String host, int port, String resource) throws MalformedURLException, DispatchException{
		String urlString = "http://" + host + ":" + port + "/" + resource;
		NarwhalService service = new NarwhalHTTPServiceImpl(new URL(urlString));
		addService(service);
	}
	
	/**
	 * Looks up the services offered by the endpoints described in the string of URL's (separated by semi-colons).
	 * 
	 * For example, if the following string is passed in:
	 * <code>"http://server1.example.com:8080/rpc.do;http://server2.example.com:49123/something/query.do"</code>
	 * then the services exposed by server1 and server2 will be registered. 
	 * 
	 * @param urls
	 * @throws MalformedURLException
	 * @throws DispatchException
	 */
	public void addServices(String urls) throws MalformedURLException, DispatchException{
		String[] urlStringArray = urls.split(";");
		DispatchException lastDispatchException = null;
		boolean anySuccess = false;
		for (String urlString : urlStringArray){
			try{
				NarwhalService service = new NarwhalHTTPServiceImpl(new URL(urlString));
				if (addService(service) > 0){
					anySuccess = true;
				}
			}
			catch (DispatchException ex){
				lastDispatchException = ex;
				log.warn("caught " + ex + " while trying to register service for " + urlString, ex);
			}
		}
		if (!anySuccess){
			log.error("failed to successfully connect to any Narwhal services, throwing last exception: " + lastDispatchException, lastDispatchException);
			throw lastDispatchException;
		}
	}
	
	/**
	 * Adds a specific service endpoint to the list of service adapters.
	 * 
	 * Returns the number of services supported.
	 * 
	 * @param service
	 * @return
	 * @throws MalformedURLException
	 * @throws DispatchException
	 */
	public int addService(NarwhalService service) throws MalformedURLException, DispatchException{
		Request request = new Request("IntrospectionHandler.getAllServices");
		Session session = new SessionImpl();
		JsonElement serviceListElem = service.dispatch(request, session); // should be no session header info needed for this call
		if (serviceListElem == null || serviceListElem.isJsonNull()){
			log.warn("no services defined");
			return 0;
		}
		JsonArray serviceArray = serviceListElem.getAsJsonArray();
		int serviceCount = 0;
		for (JsonElement serviceEntryElem : serviceArray){
			JsonObject serviceEntry = serviceEntryElem.getAsJsonObject();
			String methodName = serviceEntry.get("name").getAsString();
			List<NarwhalService> serviceList = serviceMap.get(methodName);
			if (serviceList == null){
				serviceList = new ArrayList<NarwhalService>();
				serviceMap.put(methodName, serviceList);
			}
			serviceList.add(service);
			log.debug("registered service " + service.getName() + " for method " + methodName);
			serviceCount++;
		}
		if (serviceName == null){
			serviceName = service.getName();
		}
		else{
			serviceName = serviceName + ":" + service.getName();
		}
		serviceEndpoints.add(service);
		return serviceCount;
	}
	
	
	public List<NarwhalService> getServiceEndpoints(){
		return Collections.unmodifiableList(serviceEndpoints);
	}
	

	public JsonElement dispatch(final Request request, final Session session) throws DispatchException{
		List<NarwhalService> serviceList = serviceMap.get(request.getMethod());
		if (serviceList == null){
			throw new DispatchException(DispatchException.INVALID_REQUEST, "no dispatchers registered for method " + request.getMethod());
		}
		DispatchException lastException = null;
		for (NarwhalService service : serviceList){
			try{
				JsonElement result = service.dispatch(request, session);
				return result;
			}
			catch (DispatchException dx){
				log.debug("failed request " + request.getMethod() + " with service " + service.getName());
				lastException = dx;
			}
		}
		log.debug("failed request " + request.getMethod() + " with all registered services");
		throw lastException;
	}
	
	public Response dispatchWithResponse(final Request request, final Session session) throws DispatchException{
		List<NarwhalService> serviceList = serviceMap.get(request.getMethod());
		if (serviceList == null){
			throw new DispatchException(DispatchException.INVALID_REQUEST, "no dispatchers registered for method " + request.getMethod());
		}
		DispatchException lastException = null;
		for (NarwhalService service : serviceList){
			try{
				Response response = service.dispatchWithResponse(request, session);
				return response;
			}
			catch (DispatchException dx){
				log.debug("failed request " + request.getMethod() + " with service " + service.getName());
				lastException = dx;
			}
		}
		log.debug("failed request " + request.getMethod() + " with all registered services");
		throw lastException;
	}

	public JsonReader dispatch(FullRequest request) throws Exception{
		List<NarwhalService> serviceList = serviceMap.get(request.getMethod());
		if (serviceList == null){
			throw new DispatchException(DispatchException.INVALID_REQUEST, "no dispatchers registered for method " + request.getMethod());
		}
		DispatchException lastException = null;
		for (NarwhalService service : serviceList){
			try{
				JsonReader reader = service.dispatch(request);
				return reader;
			}
			catch (DispatchException dx){
				log.debug("failed request " + request.getMethod() + " with service " + service.getName());
				lastException = dx;
			}
		}
		log.debug("failed request " + request.getMethod() + " with all registered services");
		throw lastException;
	}
	
}
