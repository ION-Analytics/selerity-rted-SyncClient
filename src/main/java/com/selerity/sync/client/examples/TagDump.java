package com.selerity.sync.client.examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.Dispatcher;
import com.selerity.sync.client.MiscUtils;
import com.selerity.sync.client.Request;
import com.selerity.sync.client.RhinoDispatcher;
import com.selerity.sync.client.RhinoHTTPTransport;
import com.selerity.sync.client.RhinoSessionFactory;
import com.selerity.sync.client.Session;
import com.selerity.sync.client.Transport;

/**
 * © Copyrights Selerity, Inc. 2009-2011. All rights reserved. This source code
 * is confidential and proprietary information of Selerity Inc. and may be used
 * only by a recipient designated by and for the purposes permitted by Selerity
 * Inc. in writing. Reproduction of, dissemination of, modifications to or
 * creation of derivative works from this source code, whether in source or
 * binary forms, by any means and in any form or manner, is expressly
 * prohibited, except with the prior written permission of Selerity Inc.. THIS
 * CODE AND INFORMATION ARE PROVIDED ÒAS ISÓ WITHOUT WARRANTY OF ANY KIND,
 * EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND/OR FITNESS FOR A PARTICULAR PURPOSE. This notice may
 * not be removed from the software by any user thereof.
 * 
 * An example application which prints out all tags, organized by name, value 
 * and synonyms by family.
 * 
 */


public class TagDump {
	
	private static final Log log = LogFactory.getLog(TagDump.class);
	
	protected final Session session;
	protected final Dispatcher dispatcher;
	
	
	public TagDump(String host, int port, String user, String password) throws MalformedURLException, DispatchException{
		// initialized the transport and method dispatcher
		String rhinoURL = "http://" + host + ":" + port + "/rhino-1.0-SNAPSHOT/rpc.do";
		Transport transport = new RhinoHTTPTransport(rhinoURL, false);
		dispatcher = new RhinoDispatcher(transport);

		// log in, setting the 'client identification' field and enabling
		// the 'extensions mode'
		session = new RhinoSessionFactory().getInstance(dispatcher,
				"ObservableDump", "extension", user, password);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {

			if (args.length < 5) {
				System.err.println("arguments: host port user password outputFileName");
				System.exit(1);
			}

			//
			// do the initial setup
			//

			// read in the arguments
			String host = args[0];
			int port = Integer.parseInt(args[1]);
			String user = args[2];
			String password = args[3];
			String outputFileName = args[4];

			
			// initialize the dumper
			TagDump dumper = new TagDump(host, port, user, password);

			dumper.dumpAllTags(outputFileName, 500);
			
			log.debug("all done");

		}
		catch (Exception ex) {
			log.error("caught exception " + ex, ex);
		}
	}
	
	
	public void dumpAllTags(String outputFileName, int requestLimit) throws IOException, DispatchException{
		long startDump = System.currentTimeMillis();
		
		// open the file
		BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));
		out.write("\"name\",\"value\",\"family\",\"synonym\"\n");
		
		// look up the tag name
		Request tagNameRequest = new Request("TagHandler.getTagNames");
		JsonArray tagNames = dispatcher.dispatch(tagNameRequest, session).getAsJsonArray();
		
		int tagCount = 0;
		
		// for each tag name
		for (int n = 0; n < tagNames.size(); n++){
			String tagName = tagNames.get(n).getAsString();
			log.debug("looking up tags for name " + tagName);
			
			SortedMap<String,SortedMap<String, String>> tagSynonymMap = getTagSynonymMap(tagName, requestLimit);
			for (String valueString : tagSynonymMap.keySet()){
				
				SortedMap<String, String> synonymMap = tagSynonymMap.get(valueString);
				for (String familyString : synonymMap.keySet()){
					String synonymString = synonymMap.get(familyString);
					String row = "\"" + tagName + "\",\"" + valueString + "\",\"" + familyString + "\",\"" + synonymString + "\"";
					log.debug(row);
					out.write(row);
					out.write('\n');
					tagCount++;
				}
			}
		}
		out.close();
		
		// all done; print out some simple stats
		long elapsedDump = System.currentTimeMillis() - startDump;
		double elapsedDumpSeconds = ((double)elapsedDump) / 1000.0;
		log.debug("got " + tagCount + " tags (values and synonyms) in " + elapsedDumpSeconds + " seconds");
	}
		
		
	public SortedMap<String,SortedMap<String, String>> getTagSynonymMap(String tagName, int requestLimit) throws IOException, DispatchException{

		SortedMap<String,SortedMap<String, String>> tagSynonymMap = new TreeMap<String,SortedMap<String, String>>();
		
		// cycle through pages of tags
		int offset = 0;
		int numFound = requestLimit;
		
		while (numFound > 0){
			// build the request
			Request synonymsRequest = new Request("SynonymHandler.getTagsByName");
			synonymsRequest.setMethodParameter("name", tagName);
			JsonObject paginationOption = new JsonObject();
			paginationOption.addProperty("limit", requestLimit);
			paginationOption.addProperty("offset", offset);
			synonymsRequest.setMethodParameter("paginationOption", paginationOption);
			
			// dispatch the request
			JsonArray synonyms = dispatcher.dispatch(synonymsRequest, session).getAsJsonArray();
			
			// update the offset and the number found
			numFound = synonyms.size();
			log.debug("returned " + numFound + " synonym records");
			//offset += numFound;
			offset += requestLimit;  // this is not what I expected... but maybe it's correct?
			
			// for each synonym
			for (int s = 0; s < synonyms.size(); s++){
				JsonObject synonym = synonyms.get(s).getAsJsonObject();				
				String synonymString = MiscUtils.getString(synonym, "synonym", null);
				String valueString = MiscUtils.getString(synonym, "value", null);
				String familyString = MiscUtils.getString(synonym, "family", null);
				log.info("found " + tagName + " = " + valueString + " -> " + synonymString + " (" + familyString + ")");
				SortedMap<String, String> synonymMap = tagSynonymMap.get(valueString);
				if (synonymMap == null){
					synonymMap = new TreeMap<String, String>();
					synonymMap.put("CANONICAL", valueString);// a tag is its own synonym
					tagSynonymMap.put(valueString, synonymMap);
				}
				if (synonymString != null){
					if (familyString == null){
						synonymMap.put("NULL", synonymString);
					}
					else{
						synonymMap.put(familyString, synonymString);
					}
				}
			}
		}
			
		return tagSynonymMap;

	}
	

	
	
	
}
