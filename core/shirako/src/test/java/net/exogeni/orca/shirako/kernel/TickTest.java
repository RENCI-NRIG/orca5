/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.kernel;

import junit.framework.Assert;
import junit.framework.TestCase;


/**
 * Unit tests for <code>Tick</code>
 */
public abstract class TickTest extends TestCase
{
    /**
     * Returns an instance of <code>ITick</code>
     * @return instance of <code>ITick</code>
     */
    protected abstract Tick getTick();

    /**
     * Tests object creation and default values.
     * @throws Exception
     */
    public void testCreate() throws Exception
    {
        Tick tick = getTick();
        tick.initialize();

        Assert.assertEquals(0, tick.getBeginningOfTime());
        Assert.assertEquals(1, tick.getCycleMillis());
        Assert.assertEquals(1, tick.getCycleMillis());
        Assert.assertEquals(false, tick.isManual());
        Assert.assertEquals(true, tick.stopped);
        Assert.assertNotNull(tick.clock);
        Assert.assertNotNull(tick.logger);
        Assert.assertNotNull(tick.subscribers);
    }

    /**
     * Tests setting properties.
     * @throws Exception
     */
    public void testProperties() throws Exception
    {
        long beginning = 1000;
        long cycleLength = 234;

        Tick tick = getTick();

        tick.setBeginningOfTime(beginning);
        tick.setCycleMillis(cycleLength);
        tick.initialize();

        Assert.assertEquals(beginning, tick.getBeginningOfTime());
        Assert.assertEquals(cycleLength, tick.getCycleMillis());

        boolean failed = false;

        try {
            tick.setCycleMillis(cycleLength + 10);
        } catch (RuntimeException e) {
            failed = true;
        }

        if (!failed) {
            Assert.fail();
        }

        Assert.assertEquals(cycleLength, tick.getCycleMillis());

        failed = false;

        try {
            tick.setBeginningOfTime(cycleLength + 10);
        } catch (RuntimeException e) {
            failed = true;
        }

        if (!failed) {
            Assert.fail();
        }

        Assert.assertEquals(beginning, tick.getBeginningOfTime());
    }

    /**
     * Tests starting/stopping the clock.
     * @throws Exception
     */
    public void testStartStop() throws Exception
    {
        Tick tick = getTick();
        tick.initialize();

        boolean failed = false;

        try {
            tick.tick();
        } catch (Exception e) {
            failed = true;
        }

        Assert.assertTrue(failed);
        tick.start();

        failed = false;

        try {
            tick.start();
        } catch (Exception e) {
            failed = true;
        }

        Assert.assertTrue(failed);

        tick.stop();

        failed = false;

        try {
            tick.stop();
        } catch (Exception e) {
            failed = true;
        }

        Assert.assertTrue(failed);

        tick.start();
    }

}
