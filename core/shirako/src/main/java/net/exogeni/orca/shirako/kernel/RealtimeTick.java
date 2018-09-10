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

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import net.exogeni.orca.shirako.api.ITick;
import net.exogeni.orca.shirako.container.Globals;


/**
 * An implementation of a Shirako container clock. This implementation is
 * driven by the computer's internal clock. Each subscriber is represented by
 * a separate thread, which is used to deliver the clock events. The class
 * supports both manual and automated execution.<p>In automated mode the
 * class derives cycle information using the computer's internal clock. As a
 * result the drift between the logical time (cycles) and the physical time
 * should be limited.</p>
 *  <p>Since each subscriber is bound to a unique thread, this
 * implementation is suitable for scenarios with limited number of
 * subscribers, e.g., tens, rather than hundreds.</p>
 */
public class RealtimeTick extends Tick
{
    /**
     * A thread to use to measure the passage of time.
     */
    protected class TickNotifier extends TimerTask
    {
        public void run()
        {
            if (stopped) {
                // note: this renders the timer object unusable
                timer.cancel();
            } else {
                /*
                 * Calculate the current cycle. Uses the current time.
                 */
                synchronized (clockLock) {
                    long now = System.currentTimeMillis();
                    currentCycle = new Long(clock.cycle(now));
                    // send the notification
                    clockLock.notifyAll();
                }
            }
        }
    }

    /**
     * Thread responsible for delivering timer events to a single
     * subscriber.
     */
    protected class TickWrapper extends Thread
    {
        /**
         * The subscriber.
         */
        protected ITick tick;

        /**
         * Flag that controls execution.
         */
        protected boolean go = true;

        /**
         * Last cycle the subscriber was notified.
         */
        protected long lastCycle;

        /**
                         * Creates a new instance.
                         * @param tick subscriber
                         * @param lastCycle last cycle
                         */
        public TickWrapper(final ITick tick, final long lastCycle)
        {
            this.tick = tick;
            this.lastCycle = lastCycle;
            this.setDaemon(true);
        }

        /**
         * Computes the next cycle to be processed. This is a
         * blocking function: it will block until cycle is at least one more
         * than lastCycle or the subscriber un-subscribes.
         *
         * @return the next cycle
         *
         * @throws Exception if an error occurs while waiting for the next
         *         cycle
         */
        protected long nextCycle() throws Exception
        {
            synchronized (clockLock) {
                while ((lastCycle == currentCycle) && go) {
                    clockLock.wait();
                }

                return currentCycle;
            }
        }

        @Override
        public void run()
        {
            try {
                while (go) {
                    /* wait for the next cycle */
                    long cycle = nextCycle();

                    /* if the subscriber is no longer subscribed we need to exit */
                    if (!go) {
                        break;
                    }

                    /*
                     * More than one cycle may have passed since the last time
                     * we notified the subscriber. We can either rely on the
                     * subscriber being able to handle correctly skipping
                     * cycles, or we can make sure we send the missed
                     * notifications. As long as cycles are chosen of a proper
                     * length, and the subscriber is not overloaded, this
                     * approach should maintain a small drift from the real
                     * clock.
                     */
                    for (long temp = lastCycle + 1; temp <= cycle; temp++) {
                        /* process the new cycle */
                        try {
                            tick.externalTick(temp);
                        } catch (Exception e) {
                            Globals.Log.error("An error in tick", e);
                        }

                        lastCycle = temp;
                    }
                }
            } catch (Exception e) {
                logger.error(e);
                // no need to re-throw the exception: this is the bottom of the stack!
            }
        }
    }

    /**
     * Timer object.
     */
    protected Timer timer;

    /**
     * Table of wrappers, one per subscriber.
     */
    protected Hashtable<ITick, TickWrapper> wrappers;

    /**
     * Lock used to notify about the passage of time. All subscriber
     * threads do a wait on this lock. For each new cycle, the notifier thread
     * will issue a signal on the lock to all waiters.
     */
    protected Object clockLock;

    /**
             * Creates a new instance.
             */
    public RealtimeTick()
    {
        clockLock = new Object();
        wrappers = new Hashtable<ITick, TickWrapper>();
    }

    /**
     * {@inheritDoc}
     */
    public void addTickable(final ITick tickable)
    {
        synchronized (this) {
            // add only if not present already
            if (!wrappers.containsKey(tickable)) {
                TickWrapper w = new TickWrapper(tickable, currentCycle);
                if (tickable.getName() != null) {
                    w.setName(tickable.getName());
                }
                w.start();
                wrappers.put(tickable, w);
            }
        }
    }

    @Override
    protected synchronized void nextTick()
    {
        // advance the clock and notify subscribers
        synchronized (clockLock) {
            currentCycle++;
            clockLock.notifyAll();
        }

        /*
         * We must block until all subscribers process the event.
         */
        boolean done = false;

        while (!done) {
            done = true;

            // check completion
            for (TickWrapper w : wrappers.values()) {
                if (w.lastCycle < currentCycle) {
                    done = false;
                }
            }

            // take a nap if not done
            try {
                if (!done) {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeTickable(final ITick tickable)
    {
        TickWrapper w = null;

        synchronized (this) {
            w = wrappers.get(tickable);
            wrappers.remove(tickable);
        }

        if (w != null) {
            w.go = false;

            // XXX: should we do a join? we may block forever?
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void startWorker()
    {
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TickNotifier(), 0, cycleMillis);
    }

    /**
     * {@inheritDoc}
     */
    protected void stopWorker()
    {
        // no-op
    }
}
