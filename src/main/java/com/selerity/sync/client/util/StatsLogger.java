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

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * A simple utility class to collect 1-dimensional numerical stats on specific keys.
 * For example, this class can be used
 * to capture the amount of time used when making various remote API calls, organized by the remote method name.
 *
 * Note that this class is thread-safe.
 *
 * @param <K>
 * @param <V>
 */
public class StatsLogger<K, V extends Number> {

    private final SortedMap<K, SortedMap<V, Integer>> stats = new TreeMap<>();

    public StatsLogger() {
    }

    /** Adds a value associated with this key.
     *
     * @param key
     * @param value
     */
    public synchronized void addResult(K key, V value) {
        SortedMap<V, Integer> valueMap = stats.get(key);
        if (valueMap == null) {
            valueMap = new TreeMap<V, Integer>();
            stats.put(key, valueMap);
        }
        Integer count = valueMap.get(value);
        if (count == null) {
            valueMap.put(value, 1);
        } else {
            valueMap.put(value, count + 1);
        }
    }

    /** Returns a copy of the keys logged by this instance.
     *
     * @return
     */
    public synchronized SortedSet<K> getKeys() {
        return new TreeSet<K>(stats.keySet());
    }

    /** Returns the numerically smallest value associated with the given key.
     *
     * @param key
     * @return
     */
    public synchronized V getMinValue(K key) {
        SortedMap<V, Integer> valueMap = stats.get(key);
        if (valueMap == null) {
            return (V) null;
        }
        return valueMap.firstKey();
    }

    /** Reuturns the numerically largest value associated with the given key.
     *
     * @param key
     * @return
     */
    public synchronized V getMaxValue(K key) {
        SortedMap<V, Integer> valueMap = stats.get(key);
        if (valueMap == null) {
            return (V) null;
        }
        return valueMap.lastKey();
    }

    /** Returns the arithmetic mean of the values associated with the given key.
     *
     * @param key
     * @return
     */
    public synchronized double getMeanValue(K key) {
        SortedMap<V, Integer> valueMap = stats.get(key);
        if (valueMap == null) {
            return Double.NaN;
        }
        double sum = 0;
        int totalCount = 0;
        for (V value : valueMap.keySet()) {
            int count = valueMap.get(value);
            totalCount += count;
            sum += value.doubleValue() * ((double) count);
        }
        return sum / totalCount;
    }

    /** Returns the number of values associated with this key.
     *  Note that this is *not* a count of the number of *distinct* values.
     *
     * @param key
     * @return
     */
    public synchronized int getCountValues(K key) {
        SortedMap<V, Integer> valueMap = stats.get(key);
        if (valueMap == null) {
            return 0;
        }
        int totalCount = 0;
        for (V value : valueMap.keySet()) {
            int count = valueMap.get(value);
            totalCount += count;
        }
        return totalCount;
    }

    /** Returns the sum of all values associated with this key.
     *
     * @param key
     * @return
     */
    public synchronized double getSumOfValues(K key) {
        SortedMap<V, Integer> valueMap = stats.get(key);
        if (valueMap == null) {
            return 0.0;
        }
        double sum = 0;
        for (V value : valueMap.keySet()) {
            int count = valueMap.get(value);
            sum += value.doubleValue() * ((double) count);
        }
        return sum;
    }

}
