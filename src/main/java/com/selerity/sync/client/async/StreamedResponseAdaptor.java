package com.selerity.sync.client.async;

import com.selerity.sync.client.Response;

public class StreamedResponseAdaptor implements Runnable{

	protected final StreamedResponseListener responseListener;
	protected final StreamedResponse streamedResponse;
	protected final long responseTimeoutMillis;
	
	public StreamedResponseAdaptor(StreamedResponseListener responseListener,
			StreamedResponse streamedResponse, long responseTimeoutMillis) {
		super();
		this.responseListener = responseListener;
		this.streamedResponse = streamedResponse;
		this.responseTimeoutMillis = responseTimeoutMillis;
	}
	
	public StreamedResponseAdaptor(StreamedResponseListener responseListener,
			StreamedResponse streamedResponse) {
		super();
		this.responseListener = responseListener;
		this.streamedResponse = streamedResponse;
		this.responseTimeoutMillis = 0;
	}
	
	public void run(){
		while (streamedResponse.hasMore()){
			if (responseTimeoutMillis > 0){
				Response response = streamedResponse.getNextResponse(responseTimeoutMillis);
				if (response == null){
					return;
				}
				responseListener.onResponse(response);
			}
			else{
				responseListener.onResponse(streamedResponse.getNextResponse());
			}
		}
	}
	
}
