/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.policy.core.util;

import orca.util.ID;

import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;


public class AllotmentTable
{
    /**
     * The set of machines that this request has units alloted from
     */
    protected Hashtable<ID, AllotmentEntry> allotment;

    /**
     * Constructor.
     * @param startTime
     * @param endTime
     */
    public AllotmentTable(Date startTime, Date endTime)
    {
        allotment = new Hashtable<ID, AllotmentEntry>();
    }

    /**
     * Add an entry to the allotment
     * @param entry
     */
    public void addEntry(AllotmentEntry entry)
    {
        if (allotment.containsKey(entry.getId())) {
            AllotmentEntry a = allotment.get(entry.getId());
            a.addUnits(entry.getUnits());
        } else {
            allotment.put(entry.getId(), entry);
        }
    }

    /**
     * Determine the max amount of resources that can be allocated to a request.
     * First determine the most amount of growth per unit the machines can
     * handle, and then decide on the max resources to be granted.
     * @return maxResources
     */
    public long[] findMaxResources(long[] minResources, long[] resourceLimit)
    {
        long[] maxResources = new long[minResources.length];
        long[] minSpace = new long[minResources.length];

        for (int i = 0; i < minResources.length; i++) {
            minSpace[i] = Long.MAX_VALUE;
        }

        Iterator i = allotment.values().iterator();

        while (i.hasNext()) {
            AllotmentEntry entry = (AllotmentEntry) i.next();
            long[] temp = entry.remainingSpacePerUnit(minResources);

            for (int j = 0; j < minSpace.length; j++) {
                if (minSpace[j] > temp[j]) {
                    minSpace[j] = temp[j];
                }
            }
        }

        for (int j = 0; j < minSpace.length; j++) {
            maxResources[j] = minResources[j] + minSpace[j];

            if (maxResources[j] > resourceLimit[j]) {
                maxResources[j] = resourceLimit[j];
            }
        }

        return maxResources;
    }

    public long[] findMinAvailable(int dimensions)
    {
        long[] result = new long[dimensions];

        for (int i = 0; i < result.length; i++) {
            result[i] = Long.MAX_VALUE;
        }

        Iterator i = allotment.values().iterator();

        while (i.hasNext()) {
            AllotmentEntry entry = (AllotmentEntry) i.next();
            long[] temp = entry.remainingSpacePerUnit();

            for (int j = 0; j < result.length; j++) {
                if (result[j] > temp[j]) {
                    result[j] = temp[j];
                }
            }
        }

        return result;
    }

    public void mergeAllotments(AllotmentTable merge)
    {
        Iterator i = merge.iterator();

        while (i.hasNext()) {
            AllotmentEntry entry = (AllotmentEntry) i.next();

            if (allotment.containsKey(entry.getId())) {
                allotment.get(entry.getId()).addUnits(entry.getUnits());
            } else {
                allotment.put(entry.getId(), entry);
            }
        }
    }

    /**
     * Determine the total number of units that have been allocated across the
     * table
     * @return
     */
    public int totalUnits()
    {
        int totalUnits = 0;
        Iterator i = allotment.values().iterator();

        while (i.hasNext()) {
            AllotmentEntry entry = (AllotmentEntry) i.next();
            totalUnits = totalUnits + entry.getUnits();
        }

        return totalUnits;
    }

    public String getIdentifiers()
    {
        StringBuffer sb = new StringBuffer();

        Iterator i = this.iterator();
        int count = 0;

        while (i.hasNext()) {
            AllotmentEntry entry = (AllotmentEntry) i.next();
            ID id = entry.getId();

            for (int j = 0; j < entry.getUnits(); j++) {
                if (count > 0) {
                    sb.append(",");
                }

                sb.append(id.toString());
                count++;
            }
        }

        return sb.toString();
    }

    public void preCommit(long[] shares)
    {
        Iterator i = allotment.values().iterator();

        while (i.hasNext()) {
            AllotmentEntry entry = (AllotmentEntry) i.next();

            for (int j = 0; j < shares.length; j++) {
                entry.machine.resources[j] -= shares[j];
            }
        }
    }

    /**
     * Retun an iterator
     * @return
     */
    public Iterator iterator()
    {
        return allotment.values().iterator();
    }

    /**
     * Return the hashtable of the allotments
     * @return
     */
    public Hashtable<ID, AllotmentEntry> getAllotment()
    {
        return allotment;
    }
}