/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.time.calendar;

import net.exogeni.orca.shirako.time.ActorClock;


/**
 * Base class of all actor calendars.
 */
public class BaseCalendar
{
    /**
     * Converter from real time to cycles.
     */
    protected ActorClock clock;

    /**
     * Creates a new instance.
     * @param clock clock factory
     */
    public BaseCalendar(final ActorClock clock)
    {
        this.clock = clock;
    }

    /**
     * Removes all reservations associated with time not after the specified
     * cycle.
     * @param cycle cycle
     */
    public void tick(final long cycle)
    {
    }
}
