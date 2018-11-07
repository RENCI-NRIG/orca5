/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.util;


import java.util.Properties;

import orca.util.PropList;
import orca.util.persistence.Persistable;
import orca.util.persistence.Persistent;


/**
 * <code>UpdateData</code> wraps state passed with a ticket or lease
 * update. Server-side code registers status of operations as they execute;
 * client-side code queries for status, and may also post status.
 */
public class UpdateData implements Persistable
{
    public static final String PropertyMessage = "UpdateDataMessage";
    public static final String PropertyEvents = "UpdateDataEvents";
    public static final String PropertyFailed = "UpdateDataFailed";

    /**
     * Status message reflecting the result of this update.
     */
    @Persistent(key = PropertyMessage)
    protected String message;

    /**
     * Printable list of events leading up to the last update.
     */
    @Persistent(key = PropertyEvents)
    protected String events;

    /**
     * Did the last operation fail?
     */
    @Persistent(key = PropertyFailed)
    protected boolean failed;

    /**
     * Creates a new instance.
     */
    public UpdateData()
    {
        message = null;
        failed = false;
    }

	/**
     * Creates a new instance using the specified message.
     * @param message message to use
     */
    public UpdateData(final String message)
    {
        error(message);
    }

    /**
     * Merges passed UpdateData into this. Posted events are extracted
     * and merged into this. The status message from this is overwritten with
     * the status message from the absorbed UpdateData.
     *
     * @param update absorbed update data
     */
    public void absorb(final UpdateData update)
    {
        post(update.events);
        if (message != null) {
            post(message);
        }
        message = update.message;
        failed = update.failed;
    }

    /**
     * Clears all events.
     */
    public void clear()
    {
        events = null;
    }
    
    /**
     * Clear the message
     */
    public void clearMessage() {
    	message = null;
    }

    /**
     * Indicates that an error has occurred.
     *
     * @param message error message
     */
    public void error(final String message)
    {
        this.message = message;
        failed = true;
    }

    /**
     * Checks if the operation represented by the object has failed.
     *
     * @return true if the operation has failed
     */
    public boolean failed()
    {
        return failed;
    }

    /**
     * Returns all events stored in the object.
     *
     * @return list of events. Event items are separated by "\n".
     */
    public String getEvents()
    {
        return events;
    }

    /**
     * Returns the message attached to the object.
     *
     * @return message
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Posts a human-readable string describing an event that the user
     * may wish to know about. If the object already contains messages, the
     * new message is prepended to the existing messages. Messages are
     * separated using "\n".
     *
     * @param event message describing event
     */
    public void post(final String event)
    {
        if (this.events == null) {
            this.events = event;
        } else {
            this.events = event + "\n" + this.events;
        }
    }

    /**
     * Posts a human-readable string describing an event that the user
     * may wish to know about, and also marks the UpdateData in a failed
     * state.
     *
     * @param event message describing event
     */
    public void postError(final String event)
    {
        post(event);
        error(event);
    }

    /**
     * Checks if the operation represented by the object has succeeded.
     *
     * @return true if the operation has succeeded
     */
    public boolean successful()
    {
        return !failed;
    }
}