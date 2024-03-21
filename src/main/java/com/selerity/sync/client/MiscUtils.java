/*
 * (C) Copyright Selerity, Inc. 2009-2019. All rights reserved. This source code
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
 */
package com.selerity.sync.client;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * Some misc utility methods that are used elsewhere, mainly as convenience functions to support debugging
 */
public class MiscUtils {

    private static final Log log = LogFactory.getLog(MiscUtils.class);

    // used for parsing/formatting in the Selerity Time Format.
    protected static DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    protected static DateFormat ISO8601_FORMAT_WITH_MILLIS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    protected static final int NANOS_PER_SECOND_POWER = 9;

    protected static final TimeZone UTC = TimeZone.getTimeZone("GMT");

    static {
        ISO8601_FORMAT.setTimeZone(UTC);
        ISO8601_FORMAT_WITH_MILLIS.setTimeZone(UTC);
    }

    // useful for converting between the time scale used in java.util.Date (and unix in general)
    // and the Selerity Time Format's numeric version.
    public static final long NANOS_PER_MILLISECOND = 1000000L;

    public static final long NANOS_PER_SECOND = 1000000000L;

    public static final long NANOS_PER_DAY = 86400000000000L;

    public static String getString(JsonObject obj, String key, String defaultValueStr) {
        JsonElement value = obj.get(key);
        //log.debug("got value " + value + " for key " + key);
        if ((value == null) || (value.isJsonNull())) {
            return defaultValueStr;
        }
        return value.getAsString();
    }

    public static int getInt(JsonObject obj, String key, int defaultValue) {
        JsonElement value = obj.get(key);
        //log.debug("got value " + value + " for key " + key);
        if ((value == null) || (value.isJsonNull())) {
            return defaultValue;
        }
        return value.getAsInt();
    }

    public static long getLong(JsonObject obj, String key, long defaultValue) {
        JsonElement value = obj.get(key);
        //log.debug("got value " + value + " for key " + key);
        if ((value == null) || (value.isJsonNull())) {
            return defaultValue;
        }
        return value.getAsLong();
    }

    public static boolean getBoolean(JsonObject obj, String key, boolean defaultValue) {
        JsonElement value = obj.get(key);
        //log.debug("got value " + value + " for key " + key);
        if ((value == null) || (value.isJsonNull())) {
            return defaultValue;
        }
        return value.getAsBoolean();
    }

    public static long getNanoTime(JsonObject obj, String key, long defaultValue) throws ParseException {
        JsonElement value = obj.get(key);
        if ((value == null) || (value.isJsonNull())) {
            return defaultValue;
        }
        return parseNanoTime(value.getAsString());
    }

    public static long getNanoTime(JsonObject obj, String key, String defaultValueStr) throws ParseException {
        JsonElement value = obj.get(key);
        if ((value == null) || (value.isJsonNull())) {
            return parseNanoTime(defaultValueStr);
        }
        return parseNanoTime(value.getAsString());
    }

    public static JsonObject getJsonObject(JsonObject obj, String key, JsonObject defaultValueObj) {
        JsonElement value = obj.get(key);
        if ((value == null) || (value.isJsonNull())) {
            return defaultValueObj;
        }
        return value.getAsJsonObject();
    }

    public static JsonArray getJsonArray(JsonObject obj, String key, JsonArray defaultValueArray) {
        JsonElement value = obj.get(key);
        if ((value == null) || (value.isJsonNull())) {
            return defaultValueArray;
        }
        return value.getAsJsonArray();
    }

    public static List<String> parseAsStringList(JsonArray array) {
        List<String> list = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            list.add(array.get(i).getAsString());
        }
        return list;
    }

    public static List<Long> parseAsLongList(JsonArray array) {
        List<Long> list = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            list.add(array.get(i).getAsLong());
        }
        return list;
    }

    public static Set<String> parseAsStringSet(JsonArray array) {
        Set<String> set = new HashSet<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            set.add(array.get(i).getAsString());
        }
        return set;
    }

    public static Set<Long> parseAsLongSet(JsonArray array) {
        Set<Long> set = new HashSet<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            set.add(array.get(i).getAsLong());
        }
        return set;
    }

    public static Map<String, String> parseAsStringStringMap(JsonObject obj) {
        Set<Entry<String, JsonElement>> entrySet = obj.entrySet();
        Map<String, String> map = new HashMap<>(entrySet.size());
        for (Entry<String, JsonElement> entry : entrySet) {
            map.put(entry.getKey(), entry.getValue().getAsString());
        }
        return map;
    }


    /** A convenience function for displaying some elements of a set but with an upper bound
     *
     * @param elements
     * @param maxCount
     * @return
     */
    public static String listSomeElements(Set<String> elements, int maxCount) {
        boolean all = false;
        if (maxCount >= elements.size()) {
            all = true;
            maxCount = elements.size();
        }
        int count = 0;
        StringBuffer buf = new StringBuffer();
        for (String s : elements) {
            buf.append(s);
            count++;
            if (count >= maxCount) {
                if (!all) {
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
    public static String stringEncode(String s) {
        if (s == null) {
            return "null";
        } else {
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
    public static long parseNanoTime(String nanoTimeStr) throws ParseException {

        int decimalIndex = nanoTimeStr.indexOf('.');
        //System.out.println("decimalIndex = " + decimalIndex);
        if (decimalIndex < 0) {
            // no fractions of a second
            synchronized (ISO8601_FORMAT) { // need to be synchronized since DateFormat is not threadsafe
                return ISO8601_FORMAT.parse(nanoTimeStr).getTime() * NANOS_PER_MILLISECOND;
            }
        } else {
            String seconds = nanoTimeStr.substring(0, decimalIndex);
            String subSeconds = nanoTimeStr.substring(decimalIndex + 1);
            long nanos;
            synchronized (ISO8601_FORMAT) { // need to be synchronized since DateFormat is not threadsafe
                nanos = ISO8601_FORMAT.parse(seconds).getTime() * NANOS_PER_MILLISECOND;
            }
            nanos += parseNumber(subSeconds, NANOS_PER_SECOND_POWER);
            return nanos;
        }
    }

    /** Returns the current time in nanoseconds since January 1, 1970, UTC.  Precision is
     *  limited to the nearest millisecond.
     *
     * @return
     */
    public static long getNanoTime() {
        return System.currentTimeMillis() * NANOS_PER_MILLISECOND;
    }

    /** Converts a timestamp in nanoseconds since midnight January 1, 1970 UTC)
     *  into the Selerity Time Format.  Truncates any values more precise than milliseconds.
     *
     * @param nanos
     * @return
     */
    public static String formatNanoTime(long nanos) {
        long millis = nanos / NANOS_PER_MILLISECOND;
        long nanoRemainder = (nanos - (millis * NANOS_PER_MILLISECOND));
        Date date = new Date(millis);
        String s = null;
        synchronized (ISO8601_FORMAT_WITH_MILLIS) {
            s = ISO8601_FORMAT_WITH_MILLIS.format(date);
        }
        return s + leftPadNumber(nanoRemainder, 6);
    }

    /** Prepends some zeroes to the string parsing of a long.
     *
     * @param number
     * @param length
     * @return
     */
    public static String leftPadNumber(long number, int length) {
        String s = Long.toString(number);
        StringBuffer buf = new StringBuffer();
        int padLen = length - s.length();
        for (int i = 0; i < padLen; i++) {
            buf.append('0');
        }
        buf.append(s);
        return buf.toString();
    }

    /** Parses a number which may have some number of omitted trailing zeroes.
     *
     *  Example: 123 as power 5 should be 12300.
     *
     * @param s
     * @param power
     * @return
     */
    public static long parseNumber(String s, int power) {
        Long l = Long.parseLong(s);
        int extraZeroes = power - s.length();
        for (int i = 0; i < extraZeroes; i++) {
            l *= 10L;
        }
        return l;
    }

    /** Converts a set of strings into a sorted list of comma-separated strings
     *
     * @param strings
     * @return
     */
    public static String collapseSet(Set<String> strings) {
        if ((strings == null) || (strings.size() < 1)) {
            return "";
        } else if (strings.size() == 1) {
            return strings.iterator().next();
        }

        String[] sortedStrings = strings.toArray(new String[strings.size()]);
        Arrays.sort(sortedStrings);

        StringBuffer buf = new StringBuffer();
        for (String string : sortedStrings) {
            buf.append(',').append(string);
        }

        return buf.substring(1);  // trims off the first comma
    }

    /** Merges the two objects into a new object.  The entries of the first object
     *  are added to the merged object, followed by the entries of the second object.
     *
     * @param obj1
     * @param obj2
     * @return
     */
    public static JsonObject merge(JsonObject obj1, JsonObject obj2) {
        JsonObject merged = new JsonObject();
        for (Entry<String, JsonElement> entry1 : obj1.entrySet()) {
            merged.add(entry1.getKey(), entry1.getValue());
        }
        for (Entry<String, JsonElement> entry2 : obj2.entrySet()) {
            merged.add(entry2.getKey(), entry2.getValue());
        }
        return merged;
    }


    public static JsonArray makeStringArray(Collection<String> strings) {
        JsonArray array = new JsonArray();
        for (String s : strings) {
            array.add(new JsonPrimitive(s));
        }
        return array;
    }

    public static JsonArray makeNumberArray(Collection<Number> numbers) {
        JsonArray array = new JsonArray();
        for (Number n : numbers) {
            array.add(new JsonPrimitive(n));
        }
        return array;
    }

    public static JsonArray makeBooleanArray(Collection<Boolean> booleans) {
        JsonArray array = new JsonArray();
        for (Boolean b : booleans) {
            array.add(new JsonPrimitive(b));
        }
        return array;
    }


    public static JsonObject makeStringStringMap(Map<String, String> strings) {
        JsonObject obj = new JsonObject();
        for (Entry<String, String> entry : strings.entrySet()) {
            obj.addProperty(entry.getKey(), entry.getValue());
        }
        return obj;
    }


    /**
     * Partially consumes a response stream up to the start of the result and
     * then returns the stream. This can be useful for callers who want to
     * consume a response
     *
     * @param responseStream
     * @return
     * @throws DispatchException
     */
    protected static JsonReader extractResultStream(final JsonReader responseStream) throws DispatchException {
        boolean gotNullResult = false;
        try {
            responseStream.beginObject(); // start response
            while (responseStream.hasNext()) {
                String responseEntry = responseStream.nextName();
                if (responseEntry.equalsIgnoreCase("result")) {
                    if (responseStream.peek().equals(JsonToken.NULL)) {
                        log.info("got a null result, make note and keep parsing for the error");
                        gotNullResult = true;
                        responseStream.skipValue();
                    } else {
                        log.debug("got a result, returning it");
                        return responseStream;
                    }
                } else if (responseEntry.equalsIgnoreCase("error")) {
                    if (responseStream.peek().equals(JsonToken.NULL)) {
                        log.info("got a null error, ignore and keep parsing for the error");
                        responseStream.skipValue();
                    } else {
                        DispatchException error = parseErrorFromResponseStream(responseStream);
                        responseStream.close();
                        throw error;
                    }
                } else if (responseEntry.equalsIgnoreCase("header")) {
                    responseStream.skipValue(); // ignore the header;
                } else if (responseEntry.equalsIgnoreCase("id")) {
                    responseStream.skipValue(); // ignore the id;
                } else {
                    log.error("got unexpected response element name: " + responseEntry);
                    responseStream.close();
                    throw new DispatchException(DispatchException.PARSE_ERROR, "got unexpected response element name: "
                            + responseEntry);
                }
            }
            responseStream.endObject();
            responseStream.close();
            if (gotNullResult) {
                log.debug("result was null so return a stream with null");
                final JsonReader nullResult = new JsonReader(new StringReader("null"));
                nullResult.setLenient(true); // necessary since we don't have a fully formed document
                return nullResult;
            }
            // otherwise, throw an exception since we should have gotten a result in the response
            throw new DispatchException(DispatchException.PARSE_ERROR, "no result in response");
        } catch (IOException iox) {
            log.error("caught " + iox + " while extracting result from JSON response stream, will wrap and rethrow", iox);
            try {
                responseStream.close(); // note - only want to close in case of error, *don't* want to close if it's being returned!
            } catch (IOException e) {
                //ignore
            }
            throw new DispatchException(DispatchException.INTERNAL_ERROR, "caught " + iox + " while parsing response");
        }
    }

    /**
     * Extracts a DispatchException from the error field of a Narwhal response
     *
     * @param reader
     * @throws IOException
     */
    private static DispatchException parseErrorFromResponseStream(JsonReader reader) throws IOException {
        JsonParser parser = new JsonParser();

        reader.beginObject();
        int code = 0;
        String message = null;
        JsonElement data = null;
        String error_id = null;
        while (reader.hasNext()) {
            final String fieldName = reader.nextName();
            if (fieldName.equalsIgnoreCase("code")) {
                code = reader.nextInt();
                log.debug("code = " + code);
            } else if (fieldName.equalsIgnoreCase("message")) {
                message = reader.nextString();
                log.debug("message = " + message);
            } else if (fieldName.equalsIgnoreCase("data")) {
                data = parser.parse(reader);
                log.debug("data = " + data);
            } else if (fieldName.equalsIgnoreCase("error-id")) {
                error_id = reader.nextString();
                log.debug("error-id = " + error_id);
            } else {
                // warn but continue to parse, no need to throw exception
                log.warn("unexpected error field: " + fieldName);
            }
        }
        reader.endObject();
        return new DispatchException(code, message, data);
    }

}




