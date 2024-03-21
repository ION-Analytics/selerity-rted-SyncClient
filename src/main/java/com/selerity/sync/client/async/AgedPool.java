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
package com.selerity.sync.client.async;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * A simple class that manages elements which transition from Active to Retiring to Retired state based on
 * time intervals.
 *
 * This class is thread safe.
 *
 * @param <T>
 */
public class AgedPool<T> {

    protected final Queue<T> activeQueue = new LinkedList<>();
    protected final Queue<T> retiringQueue = new LinkedList<>();

    protected final Map<T, Long> startTimesMillis = new HashMap<>();
    protected final Map<T, Long> retireTimesMillis = new HashMap<>();

    protected final long activeTimeLimitMillis;
    protected final long retireTimeLimitMillis;

    public AgedPool(long activeTimeLimitMillis, long retireTimeLimitMillis) {
        this.activeTimeLimitMillis = activeTimeLimitMillis;
        this.retireTimeLimitMillis = retireTimeLimitMillis;
    }

    public synchronized int getActiveCount() {
        return activeQueue.size();
    }

    public synchronized int getRetiringCount() {
        return retiringQueue.size();
    }

    public synchronized void addNew(T t) {
        if (startTimesMillis.containsKey(t)) {
            throw new IllegalArgumentException("cannot add new since element has already been added");
        } else {
            activeQueue.add(t);
            startTimesMillis.put(t, System.currentTimeMillis());
        }
        notifyAll();
    }

    public synchronized void returnToActive(T t) {
        if (startTimesMillis.containsKey(t)) {
            activeQueue.add(t);
        } else {
            throw new IllegalArgumentException("cannot return since element hasn't been added");
        }
        notifyAll();
    }

    public synchronized T waitForNextActive() {
        while (activeQueue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException ix) {
                // ignore
            }
        }

        notifyAll();

        return activeQueue.remove();
    }

    public synchronized T waitForNextActive(long maxWaitTimeMillis) {
        long giveUpTime = System.currentTimeMillis() + maxWaitTimeMillis;
        while ((System.currentTimeMillis() < giveUpTime) && (activeQueue.isEmpty())) {
            try {
                wait(giveUpTime - System.currentTimeMillis());
            } catch (InterruptedException ix) {
                // ignore
            }
        }

        if (activeQueue.isEmpty()) {
            return null; // no change in state, no need to notify
        }

        notifyAll();

        return activeQueue.remove();
    }

    public synchronized T getNextActive() {
        if (activeQueue.isEmpty()) {
            return null;
        }

        notifyAll();

        return activeQueue.remove();
    }

    /**
     * Checks the status of the elements, moving them from
     * Active to Retiring to Retired as appropriate and returning the most recently retire set.
     */
    public synchronized Set<T> getRetired() {
        long currentTimeMillis = System.currentTimeMillis();

        // first, check to see which items should be moved from active to retiring
        Iterator<T> activeIt = activeQueue.iterator();
        while (activeIt.hasNext()) {
            T t = activeIt.next();
            long startTimeMillis = startTimesMillis.get(t);
            long activeTimeMillis = currentTimeMillis - startTimeMillis;
            if (activeTimeMillis > activeTimeLimitMillis) {
                // move this one into the retiring queue
                activeIt.remove();
                startTimesMillis.remove(t);
                retiringQueue.add(t);
                retireTimesMillis.put(t, currentTimeMillis);
            }
        }

        // now see which of the retiring ones are done
        Set<T> retired = new HashSet<>();
        Iterator<T> retiringIt = retiringQueue.iterator();
        while (retiringIt.hasNext()) {
            T t = retiringIt.next();
            long retiredStartTimeMillis = retireTimesMillis.get(t);
            long retiredTimeMillis = currentTimeMillis - retiredStartTimeMillis;
            if (retiredTimeMillis > retireTimeLimitMillis) {
                retiringIt.remove();
                retireTimesMillis.remove(t);
                retired.add(t);
            }
        }

        notifyAll();

        return retired;
    }

}
