package com.selerity.sync.client.examples;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.selerity.sync.client.DispatchException;
import com.selerity.sync.client.RhinoHTTPTransportFactory;
import com.selerity.sync.client.TransportFactory;

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
 * An example application which looks up identifier information about entities 
 * in the SeleritySync system and writes out a simple CSV-style summary of the 
 * entities and their various identifiers.  The format of the output file
 * matches the identifiers file that was extracted from the old RDS system.
 * 
 */

public class IdentifiersDump{
	
	protected class Tag {
		protected String name;
		protected String value;
		
	}

	private static final Log log = LogFactory.getLog(IdentifiersDump.class);

	private static final int PAGINATION_LIMIT = 500;
	private static final String SELERITY_FAMILY = "Selerity";
	private static final String CANONICAL_FAMILY = "CANONICAL";
	private static final String GENERIC_SYNONYM_FAMILY = "NULL";
	
	private static final Set<String> TICKER_FAMILIES = new HashSet<String>();
	
	static {
		TICKER_FAMILIES.add("NYSE");
		TICKER_FAMILIES.add("NASDAQ");
		TICKER_FAMILIES.add("TSX");
	}
	
	protected final TagDump tagDump;
	
	public IdentifiersDump(TransportFactory transportFactory, String user, String password, String clientAppName) throws Exception{
		tagDump = new TagDump(transportFactory, user, password, clientAppName);
	}
	
	/** Given a map of values->families->synonyms, need to generate a map of selerity synonyms to other synonyms
	 * 
	 * @param out
	 * @throws IOException
	 * @throws DispatchException
	 */
	public void dumpIdentifiers(Writer out) throws IOException, DispatchException {
		long startTime = System.currentTimeMillis();
		int idCount = 0;
		int entityCount = 0;
		
		SortedMap<String,SortedMap<String, String>> tagSynonymMap = tagDump.getTagSynonymMap("entity", PAGINATION_LIMIT);
		SortedMap<String,SortedMap<String, String>> seleritySynonymsMap = buildSeleritySynonymMap(tagSynonymMap);
		
		// write the header
		out.write("\"type\",\"value\",\"exchange\",\"entity_id\"\n");
		
		for (String selerityID : seleritySynonymsMap.keySet()){
			entityCount++;
			SortedMap<String,String> synonymsMap = seleritySynonymsMap.get(selerityID);
			for (String familyName : synonymsMap.keySet()){
				String synonymString = synonymsMap.get(familyName);
				idCount++;
				if (TICKER_FAMILIES.contains(familyName)){
					// assumes this is a stock ticker
					out.write("\"TICKER\",\"" + synonymString + "\",\"" + familyName + "\",\"" + selerityID + "\"\n");
				}
				else{
					out.write("\"" + familyName + "\",\"" + synonymString + "\",\"NONE\",\"" + selerityID + "\"\n");
				}
			}
		}
		
		double elapsed = ((double)(System.currentTimeMillis() - startTime)) / 1000d;
		log.info("wrote " + idCount + " identifiers for " + entityCount + " entities in " + elapsed + " seconds");
		
	}
	
	/** Creates a map from selerityID -> familyName -> synonym
	 * 
	 * @param tagSynonymMap
	 * @return
	 */
	protected SortedMap<String,SortedMap<String, String>> buildSeleritySynonymMap(SortedMap<String,SortedMap<String, String>> tagSynonymMap){
		SortedMap<String,String> canonicalToSelerityMap = getCanonicalToSelerityMap(tagSynonymMap);
		
		SortedMap<String,SortedMap<String, String>> seleritySynonymsMap = new TreeMap<String,SortedMap<String,String>>();
		
		for (String canonicalValue : canonicalToSelerityMap.keySet()){
			String selerityID = canonicalToSelerityMap.get(canonicalValue);
			SortedMap<String,String> synonymsMap = tagSynonymMap.get(canonicalValue);
			SortedMap<String,String> newSynonymsMap = new TreeMap<String,String>();
			seleritySynonymsMap.put(selerityID, newSynonymsMap);  // add the empty map to the tree to be returned
			for (String familyName : synonymsMap.keySet()){  // loop through all the synonyms
				if (!familyName.equals(SELERITY_FAMILY) && !familyName.equals(CANONICAL_FAMILY) 
						&& !familyName.equals(GENERIC_SYNONYM_FAMILY)){  // if it's not a selerity synonym or a canonical value then add it to the map
					String synonymString = synonymsMap.get(familyName);
					newSynonymsMap.put(familyName, synonymString);
				}
			}
		}
		
		return seleritySynonymsMap;
	}
	
	/** Creates a map from canonical values to selerityID's
	 * 
	 * @param tagSynonymMap
	 * @return
	 */
	protected SortedMap<String,String> getCanonicalToSelerityMap(SortedMap<String,SortedMap<String, String>> tagSynonymMap){
		SortedMap<String,String> selerityMap = new TreeMap<String,String>(); // this is a map from canonical values to Selerity entity ID's
		
		for (String canonicalValue : tagSynonymMap.keySet()){
			SortedMap<String,String> synonymMap = tagSynonymMap.get(canonicalValue);
			String selerityID = synonymMap.get(SELERITY_FAMILY);
			if (selerityID != null){
				selerityMap.put(canonicalValue, selerityID);
			}
		}		
		
		return selerityMap;
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
			IdentifiersDump dumper = new IdentifiersDump(new RhinoHTTPTransportFactory(host, port), user, password, "IdentifiersDump");

			// open the file
			BufferedWriter out = new BufferedWriter(new FileWriter(outputFileName));
			
			// write out the identifiers
			dumper.dumpIdentifiers(out);
			
			// all done
			out.close();			
			log.debug("all done");

		}
		catch (Exception ex) {
			log.error("caught exception " + ex, ex);
		}
	}
	
	
	
}
