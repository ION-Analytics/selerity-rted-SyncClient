package com.selerity.sync.client;


import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;

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
 * 
 *  Tests some of the misc utilities used in this package.
 * 
 * @author andrewbrook
 *
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

}
