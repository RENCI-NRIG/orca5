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


public class AllotmentEntry
{
    protected ResourceEntry machine;

    /**
     * Number of units allocated on this machine
     */
    protected int units;

    /**
     * Create an entry for the allotment table. Need to determine free space at
     * the machine and the per unit space.
     * @param startTime
     * @param endTime
     * @param numElements
     * @param id
     * @param units
     * @param resource
     * @param machine
     */
    public AllotmentEntry(int units, ResourceEntry machine)
    {
        this.machine = machine;
        this.units = units;
    }

    public long[] remainingSpacePerUnit()
    {
        long[] space = machine.getResources();
        long[] result = new long[space.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = (space[i]) / units;
        }

        return result;
    }

    public long[] remainingSpacePerUnit(long[] demand)
    {
        long[] space = machine.getResources();
        long[] result = new long[space.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = (space[i] - (demand[i] * units)) / units;
        }

        return result;
    }

    /**
     * Return the resource's id
     * @return
     */
    public ID getId()
    {
        return machine.getId();
    }

    public int getUnits()
    {
        return units;
    }

    public void addUnits(int newUnits)
    {
        units += newUnits;
    }
}