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

import orca.drivers.util.EventLogger;

import org.apache.log4j.Logger;


public abstract class Driver implements IDriver
{
    public class EventLoggerWrapper
    {
        public EventLogger el;
    }

    public class ThreadLocalEventLogger extends ThreadLocal<EventLoggerWrapper>
    {
        public EventLoggerWrapper initialValue()
        {
            return new EventLoggerWrapper();
        }
    }

    /**
     * Map of action name to action code.
     */
    protected Hashtable<String, Integer> map = new Hashtable<String, Integer>();

    /**
     * Per thread event logger.
     */
    protected ThreadLocalEventLogger eventLogger = new ThreadLocalEventLogger();

    /**
     * Driver identifier.
     */
    protected DriverId driverId;

    /**
     * Driver factory that created this driver.
     */
    protected DriverFactory factory;

    /**
     * Logger.
     */
    protected Logger logger;

    /**
     * Tool for dealing with overlapping actions.
     */
    protected ActionOverlapTool overlap;

    public Driver(DriverId driverId)
    {
        this.driverId = driverId;
        logger =  Logger.getLogger(this.getClass().getCanonicalName());
    }

    public int initialize() throws Exception
    {
        int code = 0;

        if (overlap == null) {
            throw new Exception("Missing overlap object");
        }

        eventLogger.get().el = new EventLogger("initialize", "");

        return code;
    }

    public int dispatch(String actionName, Properties in, Properties out) throws Exception
    {
        int code = 0;

        /* get the action code */
        Integer actionID = map.get(actionName);

        if (actionID == null) {
            code = DriverExitCodes.UnknownAction;

            return code;
        }

        /* perform all preparation steps */
        code = prepareDispatch(actionID, actionName, in, out);

        if (code != 0) {
            return code;
        }

        /* dispatch the call */
        code = doDispatch(actionName, actionID, in, out);

        /* complete the operation */
        finishDispatch(actionName, actionID, in, out, code);

        return code;
    }

    public int dispatch2(String objectId, String actionId, Properties in, Properties out)
                  throws Exception
    {
        return 0;
    }

    /**
     * Performs the action dispatch operation.
     * @param actionName action name
     * @param actionID action identifier
     * @param in incoming properties list
     * @param out outgoing properties list
     * @return 0 on success, negative number if an error occurs
     */
    protected abstract int doDispatch(String actionName, Integer actionID, Properties in,
                                      Properties out);

    /**
     * Prepares an event logger for the current operation.
     * @param actionName action name
     * @param actionID action identifier
     * @param in incoming properties list
     * @return 0 on success, negative number if an error occurs
     */
    protected abstract int prepareEventLogger(String actionName, Integer actionID, Properties in);

    /**
     * Returns the action key for the given operation.
     * @return action key
     * @param action action
     * @param in in
     */
    protected abstract String getActionKey(Integer action, Properties in);

    /**
     * Performs validation and bookkeeping actions before dispatching a call.
     * @param actionID action identifier
     * @param actionName action name
     * @param in incoming properties list
     * @param out outgoing properties list
     * @return 0 - success, negative number - error
     */
    protected int prepareDispatch(Integer actionID, String actionName, Properties in, Properties out)
    {
        int code = 0;

        /* get the action key */
        String actionKey = getActionKey(actionID, in);

        if (actionKey == null) {
            code = DriverExitCodes.InvalidArguments;

            return code;
        }

        /* handle overlapping actions */
        code = overlap.actionStart(actionKey, actionID);

        if (code != 0) {
            return code;
        }

        /* prepare an event logger */
        code = prepareEventLogger(actionName, actionID, in);

        if (code != 0) {
            overlap.actionFinish(code, out);
        }

        return code;
    }

    /**
     * Performs bookkeeping operations to complete a dispatch call.
     * @param actionName action name
     * @param actionID action identifier
     * @param in incoming properties list
     * @param out outgoing properties list
     * @param code result of the dispatch call.
     */
    protected void finishDispatch(String actionName, Integer actionID, Properties in,
                                  Properties out, int code)
    {
        /* indicate completion */
        overlap.actionFinish(code, out);
    }

    protected void setEventLogger(EventLogger el)
    {
        eventLogger.get().el = el;
    }

    public EventLogger getEventLogger()
    {
        return eventLogger.get().el;
    }

    public int cleanup() throws Exception
    {
        return 0;
    }

    public boolean isStateful()
    {
        return true;
    }

    public void setFactory(DriverFactory factory)
    {
        this.factory = factory;
    }

    public String getDriverRoot()
    {
        return factory.getDriverRoot(this);
    }

    public DriverId getId()
    {
        return driverId;
    }
}
