/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */
package orca.shirako.time.calendar;

import orca.shirako.common.ResourceVector;

/**
 * Represents available resources for a given interval.
 */
public class AvailableResources implements Comparable<AvailableResources>
{
    /**
     * Start of the interval.
     */
    protected long start;

    /**
     * End of the interval.
     */
    protected long end;

    /**
     * Available units.
     */
    protected int units;

    protected ResourceVector vector;
    
    /**
         * Empty constructor.
         */
    public AvailableResources()
    {
    }

    /**
         * Creates a new instance.
         * @param start start time
         * @param end end time
         * @param units number of units
         * @param vector resource vector 
         */
    public AvailableResources(final long start, final long end, final int units, final ResourceVector vector)
    {
        this.start = start;
        this.end = end;
        this.units = units;
        this.vector = vector;
    }

    public int compareTo(final AvailableResources other)
    {
        if (this.end < other.end) {
            return -1;
        } else {
            if (this.end > other.end) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Creates a copy of the object.
     *
     * @return copy of the object
     */
    public AvailableResources copy()
    {
        return new AvailableResources(start, end, units, vector);
    }

    @Override
    public String toString()
    {
        return "[" + start + "," + end + " ] => " + units + " " + vector;
    }
}
