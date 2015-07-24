/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.time;

import java.util.Date;


/**
 * ActorClock contains the conversions between different notions of time
 * (millis/dates/cycles). Each container can have its own notion how long a
 * cycle is as well as when time starts and this should not affect the
 * correctness of the program. <br>
 * Leases start on the first millisecond of the start time of the term, and
 * continue until the last millisecond of the end time of the term. The lease
 * interval of a term is closed on both sides.
 */
public class ActorClock
{
    /**
     * The "beginning of time" in milliseconds.
     */
    protected long beginningOfTime;

    /**
     * Number of milliseconds in a cycle.
     */
    protected long cycleMillis;

    /**
         * Creates a new ActorClock.
         * @param beginningOfTime time offset (milliseconds)
         * @param cycleMillis cycle length (milliseconds)
         */
    public ActorClock(final long beginningOfTime, final long cycleMillis)
    {
        if ((beginningOfTime < 0) || (cycleMillis < 1)) {
            throw new IllegalArgumentException();
        }

        this.beginningOfTime = beginningOfTime;
        this.cycleMillis = cycleMillis;
    }

    /**
     * Returns the number of cycles those milliseconds represent.
     *
     * @param millis milliseconds
     *
     * @return cycles
     */
    public long convertMillis(final long millis)
    {
        if (millis < 0) {
            throw new IllegalArgumentException();
        }

        return millis / cycleMillis;
    }

    /**
     * Converts a date to a cycle.
     *
     * @param date date to convert
     *
     * @return cycle number
     */
    public long cycle(final Date date)
    {
        if (date == null) {
            throw new IllegalArgumentException();
        }

        return cycle(date.getTime());
    }

    /**
     * Converts milliseconds to cycles.
     *
     * @param millis milliseconds
     *
     * @return cycles
     */
    public long cycle(final long millis)
    {
        if (millis < beginningOfTime) {
            return 0;
        }

        long difference = millis - beginningOfTime;
        long result = (long) ((double) difference / (double) cycleMillis);

        return result;
    }

    /**
     * Calculates the last millisecond of the given cycle.
     *
     * @param cycle cycle
     *
     * @return last millisecond of the cycle
     */
    public Date cycleEndDate(final long cycle)
    {
        if (cycle < 0) {
            throw new IllegalArgumentException();
        }

        return new Date((beginningOfTime + ((cycle + 1) * cycleMillis)) - 1);
    }

    /**
     * Calculates the last millisecond of the given cycle.
     *
     * @param cycle cycle
     *
     * @return the last millisecond of the cycle
     */
    public long cycleEndInMillis(final long cycle)
    {
        return (cycleStartInMillis(cycle) + cycleMillis) - 1;
    }

    /**
     * Calculates the first millisecond of the given cycle.
     *
     * @param cycle cycle
     *
     * @return first millisecond of the cycle
     */
    public Date cycleStartDate(final long cycle)
    {
        if (cycle < 0) {
            throw new IllegalArgumentException();
        }

        return date(cycle);
    }

    /**
     * Calculates the first millisecond of the given cycle.
     *
     * @param cycle cycle
     *
     * @return the first millisecond of the cycle
     */
    public long cycleStartInMillis(final long cycle)
    {
        return beginningOfTime + (cycle * cycleMillis);
    }

    /**
     * Converts a cycle to a date.
     *
     * @param cycle cycles to convert
     *
     * @return date
     */
    public Date date(final long cycle)
    {
        if (cycle < 0) {
            throw new IllegalArgumentException();
        }

        return new Date(beginningOfTime + (cycle * cycleMillis));
    }

    /**
     * Returns the initial time offset.
     *
     * @return time offset
     */
    public long getBeginningOfTime()
    {
        return beginningOfTime;
    }

    /**
     * Returns the number of milliseconds in a cycle.
     *
     * @return milliseconds in a cycle
     */
    public long getCycleMillis()
    {
        return cycleMillis;
    }

    /*
     * =======================================================================
     * GETTERS AND SETTERS
     * =======================================================================
     */

    /**
     * Returns the number of milliseconds for a specified number of
     * cycles. Does not look at beginning of time. This is useful for knowing
     * the length of a cycle span in milliseconds.
     *
     * @param cycle number of cycles
     *
     * @return millis for <code>cycle</code> cycles
     */
    public long getMillis(final long cycle)
    {
        if (cycle < 0) {
            throw new IllegalArgumentException();
        }

        return cycle * cycleMillis;
    }
}