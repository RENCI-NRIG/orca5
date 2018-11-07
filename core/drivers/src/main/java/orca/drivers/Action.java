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

import java.util.Properties;


/**
 * Holds an executing thread and an action id
 */
public class Action
{
    /**
     * Thread executing the action.
     */
    public Thread thread;

    /**
     * Action identifier.
     */
    public Integer actionId;

    /**
     * Action key
     */
    public String actionKey;

    /**
     * Completion flag.
     */
    public boolean done = false;

    /**
     * Exit code.
     */
    public int exitCode = -1;

    /**
     * Output properties.
     */
    public Properties out;

    /**
     * Creates a new action record.
     * @param actionKey actionKey
     * @param actionId action identifier
     * @param thread thread executing the action
     */
    public Action(String actionKey, Integer actionId, Thread thread)
    {
        this.actionKey = actionKey;
        this.actionId = actionId;
        this.thread = thread;
    }
}
