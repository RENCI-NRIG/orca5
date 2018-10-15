/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.util;


/**
 * Represents available units for a given interval.
 */
public class AvailableUnits implements Comparable<AvailableUnits>
{
    /**
     * Start of the interval.
     */
    public long start;

    /**
     * End of the interval.
     */
    public long end;

    /**
     * Available units.
     */
    public long units;

    /**
         * Empty constructor.
         */
    public AvailableUnits()
    {
    }

    /**
         * Creates a new instance.
         * @param start start time
         * @param end end time
         * @param units number of units
         */
    public AvailableUnits(final long start, final long end, final long units)
    {
        this.start = start;
        this.end = end;
        this.units = units;
    }

    public int compareTo(final AvailableUnits other)
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
    public AvailableUnits copy()
    {
        return new AvailableUnits(start, end, units);
    }

    @Override
    public String toString()
    {
        return "[" + start + "," + end + " ] => " + units;
    }
}
