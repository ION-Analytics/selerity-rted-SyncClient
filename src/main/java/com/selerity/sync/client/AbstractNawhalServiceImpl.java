package com.selerity.sync.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class AbstractNawhalServiceImpl implements NarwhalService{
	
	protected final Gson gson;
	
	protected final long WARN_DISPATCH_TIME_MILLIS = 30000;  // 30 seconds is too long for most dispatches to take

	protected String serviceName = null;
	
	public AbstractNawhalServiceImpl(String serviceName){
		GsonBuilder builder = new GsonBuilder().serializeNulls();
		builder.registerTypeAdapter(FullRequest.class, new FullRequest.FullRequestDeserializer());
		builder.registerTypeAdapter(FullRequest.class, new FullRequest.FullRequestSerializer());
		builder.registerTypeAdapter(Response.class, new Response.ResponseDeserializer());
		builder.registerTypeAdapter(Response.class, new Response.ResponseSerializer());
		gson = builder.create();
		this.serviceName = serviceName;
	}
	
	public void setName(String serviceName){
		this.serviceName = serviceName;
	}
	
	public String getName(){
		return this.serviceName;
	}
	

	
}
