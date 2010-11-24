package com.selerity.sync.client.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LegacySpecLoader {
	

	private static final Log log = LogFactory.getLog (LegacySpecLoader.class);

	protected URL serviceURL;
	
	public LegacySpecLoader(URL serviceURL) {
		this.serviceURL = serviceURL;
	}
	
	public LegacySpecLoader(String serviceURLString) throws MalformedURLException{
		this.serviceURL = new URL(serviceURLString);
	}
	
	public String getAllObservationSpecifications(String user, String token) throws IOException{
    	return getSpecs("user=" + user + "&token=" + token);
	}
	
	public String getObservationSpecification(long legacySpecID, String user, String token) throws IOException{
    	return getSpecs("user=" + user + "&token=" + token + "&legacyObsSpecId=" + legacySpecID);
	}
	
	public String getObservationSpecification(String observableId, String user, String token) throws IOException{
    	return getSpecs("user=" + user + "&token=" + token + "&observableId=" + observableId);
	}
	
	
	
	
	protected String getSpecs(String requestString) throws IOException {
		// record start time of dispatch
    	long startTime = System.currentTimeMillis();
    	
    	//String encodedRequest = URLEncoder.encode(requestString, "utf-8");
    	String encodedRequest = requestString;
    	
    	// Create the connection
    	URLConnection con = serviceURL.openConnection();
    	con.addRequestProperty("Accept","text/plain");
    	
    	String result;
	    BufferedReader in = null;
	    
	    // try to send request to server
        try {
		    
			// POST the request
			con.setDoOutput(true);
			OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
			wr.write(encodedRequest);
			wr.flush();
			wr.close();
	    
	    	// now read the response
            in = new BufferedReader(
            		new InputStreamReader(con.getInputStream()));
            
            //TODO: would be good to have a timeout check here
            StringBuffer resultBuf = new StringBuffer();
            String line = null;
            boolean first = true;
            while ((line = in.readLine()) != null){
            	if (first){
            		first = false;
            	}
            	else{
            		resultBuf.append('\n');
            	}
            	resultBuf.append(line);
            }
            result = resultBuf.toString();
        } 
        finally {
        	if( in != null){
	        	in.close();
        	}
        }
        
        // record end time of dispatch
        long deltaMillis = System.currentTimeMillis() - startTime;
        
        log.debug("took " + deltaMillis + " ms to dispatch request for specs to " + serviceURL);
        
        return result;
	}
	

	
}
