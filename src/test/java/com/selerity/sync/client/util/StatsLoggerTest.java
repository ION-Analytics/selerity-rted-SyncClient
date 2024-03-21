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
package com.selerity.sync.client.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class StatsLoggerTest {

    @Test
    public void testEmptyStats() {
        StatsLogger<String, Double> statsLogger = new StatsLogger<>();
        assertNull(statsLogger.getMaxValue("Foo"));
    }

    @Test
    public void testSingleStats() {
        StatsLogger<String, Double> statsLogger = new StatsLogger<>();
        statsLogger.addResult("Foo", 3.14);
        assertEquals((Double) 3.14, statsLogger.getMinValue("Foo"));
        assertEquals((Double) 3.14, new Double(statsLogger.getMeanValue("Foo")));
        assertEquals((Double) 3.14, statsLogger.getMaxValue("Foo"));
        assertEquals(1, statsLogger.getCountValues("Foo"));
        assertEquals((Double) 3.14, new Double(statsLogger.getSumOfValues("Foo")));
    }

    @Test
    public void testTwoStats() {
        StatsLogger<String, Double> statsLogger = new StatsLogger<>();
        statsLogger.addResult("Foo", 3.00);
        statsLogger.addResult("Foo", 4.00);
        assertEquals(new Double(3.00), statsLogger.getMinValue("Foo"));
        assertEquals(new Double(3.5), new Double(statsLogger.getMeanValue("Foo")));
        assertEquals(new Double(4.00), statsLogger.getMaxValue("Foo"));
        assertEquals(2, statsLogger.getCountValues("Foo"));
        assertEquals((Double) 7.0, new Double(statsLogger.getSumOfValues("Foo")));
    }

    @Test
    public void testOverlappingStats() {
        StatsLogger<String, Double> statsLogger = new StatsLogger<>();
        statsLogger.addResult("Foo", 3.00);
        statsLogger.addResult("Foo", 6.00);
        statsLogger.addResult("Foo", 3.00);
        assertEquals(new Double(3.0), statsLogger.getMinValue("Foo"));
        assertEquals(new Double(4.0), new Double(statsLogger.getMeanValue("Foo")));
        assertEquals(new Double(6.0), statsLogger.getMaxValue("Foo"));
        assertEquals(3, statsLogger.getCountValues("Foo"));
        assertEquals((Double) 12.0, new Double(statsLogger.getSumOfValues("Foo")));
    }

    @Test
    public void testTwoSetsOfStats() {
        StatsLogger<String, Double> statsLogger = new StatsLogger<>();
        statsLogger.addResult("Foo", 3.0);
        statsLogger.addResult("Foo", 4.0);
        statsLogger.addResult("Blah", 6.0);
        statsLogger.addResult("Blah", 7.0);
        statsLogger.addResult("Blah", 8.0);

        assertEquals(new Double(3.0), statsLogger.getMinValue("Foo"));
        assertEquals(new Double(3.5), new Double(statsLogger.getMeanValue("Foo")));
        assertEquals(new Double(4.0), statsLogger.getMaxValue("Foo"));
        assertEquals(2, statsLogger.getCountValues("Foo"));
        assertEquals((Double) 7.0, new Double(statsLogger.getSumOfValues("Foo")));

        assertEquals(new Double(6.0), statsLogger.getMinValue("Blah"));
        assertEquals(new Double(7.0), new Double(statsLogger.getMeanValue("Blah")));
        assertEquals(new Double(8.0), statsLogger.getMaxValue("Blah"));
        assertEquals(3, statsLogger.getCountValues("Blah"));
        assertEquals((Double) 21.0, new Double(statsLogger.getSumOfValues("Blah")));
    }

    @Test
    public void testPerformance() {
        StatsLogger<String, Double> statsLogger = new StatsLogger<>();
        long startMillis = System.currentTimeMillis();
        int count = 1000000;
        for (int i = 0; i < count; i++) {
            String key = Integer.toString(i % 1000);
            statsLogger.addResult(key, new Double(i));
        }
        long elapsedMillis = System.currentTimeMillis() - startMillis;
        assertTrue(elapsedMillis < 10000); // shouldn't take more than 10 seconds (typically takes about 1 second on 2.4 GHz CPU)
        //System.out.println("added " + count + " results in " + elapsedMillis + " ms; " + (count * 1000 / elapsedMillis) + " results per second");
    }

}
