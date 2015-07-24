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

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.Date;


/**
 * Unit tests for <code>ActorClock</code>.
 */
public class ActorClockTest extends TestCase
{
    protected ActorClock getClock(final long offset, final long length)
    {
        return new ActorClock(offset, length);
    }

    /**
     * Tests <code>ActorClock</code> creation.
     */
    public void testCreate()
    {
        ActorClock clock = getClock(0, 1);

        Assert.assertEquals(0, clock.beginningOfTime);
        Assert.assertEquals(1, clock.cycleMillis);

        Assert.assertEquals(0, clock.getBeginningOfTime());
        Assert.assertEquals(1, clock.getCycleMillis());

        clock = getClock(1000, 10);

        Assert.assertEquals(1000, clock.beginningOfTime);
        Assert.assertEquals(10, clock.cycleMillis);

        Assert.assertEquals(1000, clock.getBeginningOfTime());
        Assert.assertEquals(10, clock.getCycleMillis());
    }

    /**
     * Tests conversion to cycle.
     */
    public void testCycle()
    {
        long offset = 1000;
        long length = 10;

        ActorClock clock = getClock(offset, length);

        long ms = offset;

        for (int i = 0; i < 100; i++) {
            long exp = i;
            Assert.assertEquals(exp, clock.cycle(ms));
            Assert.assertEquals(exp, clock.cycle(new Date(ms)));
            ms += length;
        }

        ms = offset;

        for (int i = 0; i < 100; i++) {
            long exp = i;

            for (int j = 0; j < length; j++) {
                Assert.assertEquals(exp, clock.cycle(ms));
                Assert.assertEquals(exp, clock.cycle(new Date(ms)));
                ms++;
            }
        }
    }

    /**
     * Tests conversion to date.
     */
    public void testDate()
    {
        long offset = 1000;
        long length = 10;

        ActorClock clock = getClock(offset, length);

        long ms = offset;

        for (int i = 0; i < 100; i++) {
            Assert.assertEquals(new Date(ms), clock.date((long) i));
            ms += length;
        }
    }

    /**
     * Tests cycle start/end date.
     */
    public void testCycleStartEndDate()
    {
        long offset = 1000;
        long length = 10;

        ActorClock clock = getClock(offset, length);

        long start = offset;
        long end = (offset + length) - 1;

        for (int i = 0; i < 100; i++) {
            Assert.assertEquals(new Date(start), clock.cycleStartDate((long) i));
            Assert.assertEquals(new Date(end), clock.cycleEndDate((long) i));
            Assert.assertEquals(start, clock.cycleStartInMillis((long) i));
            Assert.assertEquals(end, clock.cycleEndInMillis((long) i));
            start += length;
            end += length;
        }
    }

    /**
     * Tests getMillis() and convertMillis().
     */
    public void testMisc()
    {
        long offset = 1000;
        long length = 10;

        ActorClock clock = getClock(offset, length);

        long ms = 0;

        for (int i = 0; i < 100; i++) {
            long temp = clock.getMillis(i);
            Assert.assertEquals(ms, temp);
            Assert.assertEquals(i, clock.convertMillis(temp));
            ms += length;
        }
    }
}