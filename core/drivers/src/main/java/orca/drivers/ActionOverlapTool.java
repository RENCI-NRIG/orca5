/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.drivers;

import java.util.Hashtable;
import java.util.Properties;

import org.apache.log4j.Logger;


public abstract class ActionOverlapTool
{
    protected class ActionWrapper
    {
        public Action action;
    }

    protected class ThreadLocalAction extends ThreadLocal<ActionWrapper>
    {
        public ActionWrapper initialValue()
        {
            return new ActionWrapper();
        }
    }

    /**
     * Action the current thread is executing.
     */
    protected ThreadLocalAction myAction = new ThreadLocalAction();

    /**
     * Map of currently executing actions for a VM to the thread processing them
     */
    protected Hashtable<String, Action> pending = new Hashtable<String, Action>();
    protected Logger logger;

    /**
     * Creates a new instance of the tool.
     */
    public ActionOverlapTool()
    {
        pending = new Hashtable<String, Action>();
        logger = Logger.getLogger(this.getClass().getCanonicalName());
    }

    /**
     * Handles an incoming action.
     * @param actionKey key representing the action
     * @return 0, if the action should proceed, negative number if the action
     *         cannot proceed. If the result is negative, actionStop does not
     *         have to be called.
     */
    public int actionStart(String actionKey, Integer actionId)
    {
        int result = 0;

        Action previous = null;

        /* the current action record */
        Action current = new Action(actionKey, actionId, Thread.currentThread());
        /* attach the action record */
        myAction.get().action = current;

        logger.debug("starting an action: " + current.actionKey);

        boolean done = false;

        while (!done) {
            synchronized (this) {
                /* get the previous action record */
                previous = pending.get(actionKey);

                if (previous == null) {
                    /* put the current action record */
                    logger.debug("addind an action record");
                    pending.put(actionKey, current);
                    done = true;
                }
            }

            logger.debug("after checking pending list");

            if (previous != null) {
                logger.debug("previous is not null");

                /*
                 * If there is an action in progress we must decide what to do.
                 * There are several options here: (1) ignore the other action,
                 * (2) wait for the completion of the other action and then
                 * execute the current action, (3) cancel the action in
                 * progress, (4) report an error.
                 */
                int code = handleOverlapAction(actionKey, previous, current);
                logger.debug("overlap code: " + code);

                switch (code) {
                    case ActionOverlapCodes.OverlapIgnore:

                        // go ahead
                        return result;

                    case ActionOverlapCodes.OverlapWait:

                        try {
                            synchronized (previous) {
                                while (!previous.done) {
                                    previous.wait();
                                }
                            }
                        } catch (InterruptedException e) {
                            /*
                             * We were interrupted while waiting for completion.
                             * what should we do?
                             */
                            return DriverExitCodes.ErrorInterrupted;
                        }

                        return result;

                    case ActionOverlapCodes.OverlapCancel:

                        // throw new RuntimeException("not implemented");
                        return DriverExitCodes.ErrorNotImplemented;

                    case ActionOverlapCodes.OverlapError:
                        return DriverExitCodes.ErrorInvalidActionOverlap;
                }
            } else {
                logger.debug("previous is null: done is " + done);
            }
        }

        return result;
    }

    public void actionFinish(int code, Properties out)
    {
        Action action = myAction.get().action;
        myAction.get().action = null;

        logger.debug("action completing: " + action.actionKey);

        synchronized (this) {
            Action temp = pending.get(action.actionKey);

            if (temp == action) {
                logger.debug("removing action record");
                pending.remove(action.actionKey);
            } else {
                logger.debug("not removing action record!!!");
            }
        }

        synchronized (action) {
            action.exitCode = code;
            action.out = out;
            action.done = true;
            action.notify();
        }
    }

    public abstract int handleOverlapAction(String actionKey, Action previous, Action current);
}