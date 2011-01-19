package com.selerity.sync.client;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
 * Some misc utility methods that are used elsewhere, mainly as convenience functions to support debugging
 * 
 * @author andrewbrook
 *
 */
public class MiscUtils {
	
	// used for parsing/formatting in the Selerity Time Format.
	protected static DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	protected static final int NANOS_PER_SECOND_POWER = 9;
	
	// useful for converting between the time scale used in java.util.Date (and unix in general)
	// and the Selerity Time Format's numeric version.
	public static final long NANOS_PER_MILLISECOND = 1000000L;

	public static String getString(JsonObject obj, String key, String defaultValueStr){
		JsonElement value = obj.get(key);
		//log.debug("got value " + value + " for key " + key);
		if ((value == null) || (value.isJsonNull())){
			return defaultValueStr;
		}
		return value.getAsString();
	}
	
	public static int getInt(JsonObject obj, String key, int defaultValueStr){
		JsonElement value = obj.get(key);
		//log.debug("got value " + value + " for key " + key);
		if ((value == null) || (value.isJsonNull())){
			return defaultValueStr;
		}
		return value.getAsInt();
	}
	
	
	
	/** A convenience function for displaying some elements of a set but with an upper bound
	 * 
	 * @param elements
	 * @param maxCount
	 * @return
	 */
	public static String listSomeElements(Set<String> elements, int maxCount){
		boolean all = false;
		if (maxCount >= elements.size()){
			all = true;
			maxCount = elements.size();
		}
		int count = 0;
		StringBuffer buf = new StringBuffer();
		for (String s : elements){
			buf.append(s);
			count++;
			if (count >= maxCount){
				if (!all){
					buf.append(" ...");
				}
				return buf.toString();
			}
		}
		// we shouldn't actually be able to get here, but it's not a problem...
		return buf.toString();
	}
	
	/** Convenience method to encode the String as a JSON string (including support for nulls)
	 * @param s
	 * @return
	 */
	public static String stringEncode(String s){
		if (s == null){
			return "null";
		}
		else{
			return "\"" + s + "\"";
		}
	}
	
	/** Convert a string in the Selerity Time Format into a long which represents the number
	 *  of nanoseconds since midnight January 1, 1970, UTC.
	 * 
	 * @param nanoTimeStr
	 * @return
	 * @throws ParseException
	 */
	public static long parseNanoTime(String nanoTimeStr) throws ParseException{
		
		int decimalIndex = nanoTimeStr.indexOf('.');
		//System.out.println("decimalIndex = " + decimalIndex);
		if (decimalIndex < 0){
			// no fractions of a second
			synchronized(ISO8601_FORMAT){ // need to be synchronized since DateFormat is not threadsafe
				return ISO8601_FORMAT.parse(nanoTimeStr).getTime() * NANOS_PER_MILLISECOND;
			}
		}
		else{
			String seconds = nanoTimeStr.substring(0, decimalIndex);
			String subSeconds = nanoTimeStr.substring(decimalIndex + 1);
			long nanos;
			synchronized(ISO8601_FORMAT){ // need to be synchronized since DateFormat is not threadsafe
				nanos = ISO8601_FORMAT.parse(seconds).getTime() * NANOS_PER_MILLISECOND;
			}
			nanos += parseNumber(subSeconds, NANOS_PER_SECOND_POWER);
			return nanos;
		}
	}
	
	/** Parses a number which may have some number of omitted trailing zeroes.
	 * 
	 *  Example: 123 as power 5 should be 12300.
	 * 
	 * @param s
	 * @param power
	 * @return
	 */
	private static long parseNumber(String s, int power){
		Long l = Long.parseLong(s);
		int extraZeroes = power - s.length();
		for (int i = 0; i < extraZeroes; i++){
			l *= 10L;
		}
		return l;
	}
	
	
}
