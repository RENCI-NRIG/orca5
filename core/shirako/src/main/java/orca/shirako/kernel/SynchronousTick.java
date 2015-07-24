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
import orca.util.OrcaException;



/**
 * An implementation of a Shirako container clock. This implementation is
 * mostly suitable for running experiments. The class supports both manual and
 * automatic modes of execution.<p>The class uses a single thread to
 * deliver clock event notifications. In automated mode, this is a separate
 * thread, internal to the class. In manual mode, this is the thread making
 * the call to <code>tick</code>.</p>
 *  <p>The class does not use the computer clock to compute the current
 * cycle.</p>
 */
public class SynchronousTick extends Tick
{
    /**
     * Worker thread for automatic ticking.
     */
    protected class TickWorker extends Thread
    {
        /**
         * Threshold for sleeping: if the time remaining is >
         * threshold then we will not sleep.
         */
        public static final long Threshold = 50;

        public void run()
        {
            while (go) {
                try {
                    long start = System.currentTimeMillis();

                    nextTick();

                    long end = System.currentTimeMillis();
                    long diff = cycleMillis - (end - start);

                    if (diff > Threshold) {
                        Thread.sleep(diff);
                    }
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        }
    }

    /**
     * The worker thread.
     */
    protected TickWorker worker;

    /**
     * Internal flag that controls the worker thread.
     */
    protected boolean go = false;

    /**
     * Initialization flag.
     */
    private boolean initialized = false;

    /**
     * Set of new subscribers
     */
    protected HashSet<ITick> toAdd;

    /**
     * Set of subscribers to be removed
     */
    protected HashSet<ITick> toRemove;

    /**
         * Creates a new instance.
         */
    public SynchronousTick()
    {
        toAdd = new HashSet<ITick>();
        toRemove = new HashSet<ITick>();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void addTickable(final ITick tickable)
    {
        checkInitialized();

        /*
         * We are not going to add to subscribers set directly, since the set
         * may be in use in an iteration. We are going to add to the temp set
         * now
         */
        toAdd.add(tickable);
        toRemove.remove(tickable);
    }

    /**
     * Calculates the next tick cycle.
     */
    protected void calculateCycle()
    {
        /*
         * In this implementation, cycles increase monotonically, independently
         * of real time. This implementation results in clock drift and in
         * general should be used mostly for emulation purposes.
         */
        currentCycle++;
    }

    /**
     * Checks if the clock has been initialized. Throws an exception if
     * the clock has not been initialized.
     *
     * @throws RuntimeException if not initialized
     */
    private void checkInitialized()
    {
        if (!initialized) {
            throw new RuntimeException("The clock must be initialized first.");
        }
    }

    /**
     * Delivers notification to subscribers.
     */
    protected void deliverNotification()
    {
        long cycle;

        synchronized (this) {
            cycle = currentCycle;
        }

        // add/remove subscribers
        updateSets();

        /*
         * At this point subscribers contains the current subscriber set. We
         * will use the current thread to deliver notifications to all
         * subscribers.
         */
        for (ITick t : subscribers) {
            try {
                t.externalTick(cycle);
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void initialize() throws OrcaException
    {
        if (!initialized) {
            super.initialize();
            initialized = true;
        }
    }

    protected void nextTick()
    {
        calculateCycle();
        deliverNotification();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void removeTickable(final ITick tickable)
    {
        checkInitialized();

        /*
         * We are not going to remove from the subscribers set directly. See
         * comment in addTickable.
         */
        toRemove.add(tickable);
        toAdd.remove(tickable);
    }

    /**
     * {@inheritDoc}
     */
    protected void startWorker()
    {
        go = true;
        worker = new TickWorker();
        worker.start();
    }

    /**
     * Stops the worker thread(s).
     *
     * @throws Exception
     */
    protected void stopWorker() throws Exception
    {
        go = false;
        worker.join();
        worker = null;
    }

    /**
     * Updates the subscriber set. This method must be called before
     * the clock starts delivering events to subscribers and should not be
     * called while the clock is iterating over the subscribers set and
     * delivering events.
     */
    protected void updateSets()
    {
        synchronized (this) {
            if (toAdd.size() > 0) {
                for (ITick t : toAdd) {
                    subscribers.add(t);
                }

                toAdd.clear();
            }

            if (toRemove.size() > 0) {
                for (ITick t : toRemove) {
                    subscribers.remove(t);
                }

                toRemove.clear();
            }
        }
    }
}