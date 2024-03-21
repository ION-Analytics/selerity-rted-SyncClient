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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

/**
 *  Tests some of the misc utilities used in this package.
 */
public class MiscUtilsTest {

    @Test
    public void testParseNanoTime() throws ParseException {
        assertEquals(0, MiscUtils.parseNanoTime("1970-01-01T00:00:00.000000000"));
        assertEquals(1, MiscUtils.parseNanoTime("1970-01-01T00:00:00.000000001"));
        assertEquals(123456789, MiscUtils.parseNanoTime("1970-01-01T00:00:00.123456789"));
        assertEquals(1000000000, MiscUtils.parseNanoTime("1970-01-01T00:00:01.000000000"));
        assertEquals(86400000000000L, MiscUtils.parseNanoTime("1970-01-02T00:00:00.000000000"));
    }

    @Test
    public void testFormatNanoTime() throws ParseException {
        assertEquals("1970-01-01T00:00:00.000000000", MiscUtils.formatNanoTime(0));
        assertEquals("1970-01-01T00:00:00.000000001", MiscUtils.formatNanoTime(1));
        assertEquals("1970-01-01T00:00:01.000000000", MiscUtils.formatNanoTime(1000000000));
        assertEquals("1970-01-01T00:00:01.123456789", MiscUtils.formatNanoTime(1123456789));
        assertEquals("1970-01-02T00:00:00.000000000", MiscUtils.formatNanoTime(86400000000000L));
    }

    @Test
    public void testLeftPadNumber() throws ParseException {
        assertEquals("00000", MiscUtils.leftPadNumber(0, 5));
        assertEquals("00123", MiscUtils.leftPadNumber(123, 5));
        assertEquals("12345", MiscUtils.leftPadNumber(12345, 5));
        assertEquals("1234567", MiscUtils.leftPadNumber(1234567, 5));
    }

    @Test
    public void testMakeStringStringMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("foo", "blah");
        assertEquals("{\"foo\":\"blah\"}", MiscUtils.makeStringStringMap(map).toString());
    }

    @Test
    public void testCollapseSet() {
        assertEquals("", MiscUtils.collapseSet(new HashSet<String>()));
        assertEquals("test", MiscUtils.collapseSet(new HashSet<String>(Collections.singleton("test"))));
        assertEquals("aa,bb,cc", MiscUtils.collapseSet(new HashSet<String>(Arrays.asList("bb", "cc", "aa"))));
    }

    @Test
    public void testExtractResultStream() throws DispatchException, IOException {
        assertResultEquals("{\"header\":{\"user\":\"jdoe\"},\"result\":[1,2,3],\"error\":null,\"id\":42}", "[1,2,3]");
        assertResultEquals("{\"header\":{\"user\":\"jdoe\"},\"result\":{\"blobID\":47,\"blobName\":\"blobby\"},\"error\":null,\"id\":42}", "{\"blobID\":47,\"blobName\":\"blobby\"}");
        assertResultEquals("{\"header\":{\"user\":\"jdoe\"},\"result\":[1,2,3],\"error\":{\"code\":101,\"message\":\"err!\"},\"id\":42}", "[1,2,3]");
        assertResultEquals("{\"result\":[1,2,3],\"header\":{\"user\":\"jdoe\"},\"error\":null,\"id\":42}", "[1,2,3]");
        assertResultEquals("{\"header\":{\"user\":\"jdoe\"},\"result\":null,\"error\":null,\"id\":42}", "null");
    }

    @Test
    public void testExtractResultStreamWithError() throws IOException {
        try {
            assertResultEquals("{\"error\":{\"code\":101,\"message\":\"err!\"},\"header\":{\"user\":\"jdoe\"},\"result\":[1,2,3],\"id\":42}", "[1,2,3]");
        } catch (DispatchException dx) {
            assertEquals(101, dx.getCode());
            assertEquals("err!", dx.getMessage());
        }
    }

    @Test
    public void testJsonReaderAssumptions() throws IOException {
        final JsonReader reader = new JsonReader(new StringReader("null"));
        reader.setLenient(true);
        assertEquals(true, reader.hasNext());
        reader.nextNull();
        reader.close();
    }

    private void assertResultEquals(final String responseString, final String resultString) throws DispatchException, IOException {
        final JsonReader responseStream = new JsonReader(new StringReader(responseString));
        final JsonReader resultStream = MiscUtils.extractResultStream(responseStream);
        assertNextElementEquals(resultStream, resultString);
        resultStream.close();
        responseStream.close();
    }

    private void assertNextElementEquals(final JsonReader reader, final String nextElementToString) {
        JsonParser parser = new JsonParser();
        JsonElement elem = parser.parse(reader);
        assertEquals(nextElementToString, elem.toString());
    }

}
