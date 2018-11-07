/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.kernel;

import java.util.HashSet;

import orca.shirako.api.ITick;
import orca.shirako.container.Globals;
import orca.shirako.kernel.interfaces.ITicker;
import orca.shirako.time.ActorClock;
import orca.util.Initializable;
import orca.util.OrcaException;

import org.apache.log4j.Logger;


/**
 * Abstract class for all container clock implementations.
 */
public abstract class Tick implements Initializable, ITicker
{
    /**
     * The current cycle.
     */
    protected long currentCycle = -1;

    /**
     * The logger.
     */
    protected Logger logger;

    /**
     * Are we active?
     */
    protected boolean stopped = true;

    /**
     * Cycle length in milliseconds. Default 1 cycle = 1millisecond.
     */
    protected long cycleMillis = 1;

    /**
     * Cycle offset in milliseconds. Default 0 milliseconds.
     */
    protected long beginningOfTime = 0;

    /**
     * The clock factory.
     */
    protected ActorClock clock;

    /**
     * Manual flag. Default value: false.
     */
    protected boolean manual = false;

    /**
     * Subscribers.
     */
    protected HashSet<ITick> subscribers;

    /**
     * Initialization status.
     */
    private boolean initialized = false;

    /**
         * Creates a new instance.
         */
    public Tick()
    {
        this.logger = Globals.getLogger("orca.tick");
        subscribers = new HashSet<ITick>();
    }

    /**
     * Calculates the cycle before the first cycle. The cycle is
     * derived from <code>System.currentTimeMillis()</code>.
     */
    protected void calculateCycle()
    {
        currentCycle = new Long(clock.cycle(System.currentTimeMillis()));
    }

    /**
     * Checks if the clock has been initialized. Throws an exception if
     * the clock has not been initialized.
     */
    private void checkInitialized()
    {
        if (!initialized) {
            throw new RuntimeException("The clock must be initialized first.");
        }
    }

    /**
     * Checks if the clock has not been initialized. Throws an
     * exception if the clock has been initialized.
     */
    private void checkNotInitialized()
    {
        if (initialized) {
            throw new RuntimeException("The clock is already initialized");
        }
    }

    /**
     * Checks if the clock has been stopped. Throws an exception if the
     * clock is not running.
     */
    protected void checkRunning()
    {
        if (stopped) {
            throw new RuntimeException("The clock is stopped.");
        }
    }

    /**
     * Checks if the clock has been stopped. Throws an exception if the
     * clock is running.
     */
    protected void checkStopped()
    {
        if (!stopped) {
            throw new RuntimeException("The clock is already running.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void clear() throws Exception
    {
        stop();
        subscribers.clear();
    }

    /**
     * {@inheritDoc}
     */
    public long getBeginningOfTime()
    {
        checkInitialized();

        return beginningOfTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getCurrentCycle()
    {
        checkInitialized();

        return currentCycle;
    }

    /**
     * {@inheritDoc}
     */
    public long getCycleMillis()
    {
        checkInitialized();

        return cycleMillis;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void initialize() throws OrcaException
    {
        if (!initialized) {
            clock = new ActorClock(beginningOfTime, cycleMillis);

            if (currentCycle == -1) {
                calculateCycle();
            }

            initialized = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isManual()
    {
        checkInitialized();

        return manual;
    }

    /**
     * Calculates and delivers the next tick.
     */
    protected abstract void nextTick();

    /**
     * {@inheritDoc}
     */
    public void setBeginningOfTime(long value)
    {
        checkNotInitialized();
        this.beginningOfTime = value;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setCurrentCycle(long cycle)
    {
        checkNotInitialized();
        currentCycle = new Long(cycle);
    }

    /**
     * {@inheritDoc}
     */
    public void setCycleMillis(long cycleMillis)
    {
        checkNotInitialized();
        this.cycleMillis = cycleMillis;
    }

    /**
     * {@inheritDoc}
     */
    public void setManual(boolean manual)
    {
        checkNotInitialized();
        this.manual = manual;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void start()
    {
       
    	checkInitialized();
        checkStopped();
        stopped = false;

    	logger.info("Internal clock starting. Tick length " + cycleMillis + "ms");
    	if (!manual) {
            startWorker();
        }
    }

    /**
     * Starts the worker thread(s).
     */
    protected abstract void startWorker();

    /**
     * {@inheritDoc}
     */
    public synchronized void stop() throws Exception
    {
        checkInitialized();
        checkRunning();

        logger.info("Internal clock stopping");        
        if (!manual) {
            stopWorker();
        }
        stopped = true;
    }

    /**
     * Stops the worker thread(s).
     * @throws Exception in case of error
     */
    protected abstract void stopWorker() throws Exception;

    /**
     * {@inheritDoc}
     */
    public void tick()
    {
        checkInitialized();
        checkRunning();

        if (!manual) {
            throw new IllegalStateException("The clock is automatic. No manual calls are allowed.");
        }
        
        nextTick();
    }
}
