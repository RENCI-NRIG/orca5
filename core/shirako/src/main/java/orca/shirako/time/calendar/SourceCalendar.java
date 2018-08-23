/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.time.calendar;

import orca.shirako.api.IReservation;
import orca.shirako.time.ActorClock;
import orca.shirako.util.ReservationHoldings;
import orca.shirako.util.ReservationList;


/**
 * <code>SourceCalendar</code> organizes state for a reservation used as a
 * source for client reservations. A source calendar maintains a list of
 * "outlays", client reservations that have been allocated from the source
 * reservation. The outlay calendar is organized by real time.
 * <p>
 * The calendar also maintains a list of incoming extension requests for
 * reservations that have been satisfied from the underlying source
 * reservations.
 *
 */
public class SourceCalendar
{
    /**
     * Allocated reservations.
     */
    protected ReservationHoldings outlays;

    /**
     * Incoming extension requests.
     */
    protected ReservationList extending;

    /**
     * The source reservation.
     */
    protected IReservation source;

    /**
     * Clock.
     */
    protected ActorClock clock;

    /**
     * Creates a new instance.
     * @param clock clock
     * @param source source reservation
     */
    public SourceCalendar(ActorClock clock, IReservation source)
    {
        this.clock = clock;
        this.source = source;
        outlays = new ReservationHoldings();
        extending = new ReservationList();
    }

    /**
     * {@inheritDoc}
     */
    public void tick(long cycle)
    {
        // outlays are organized by real time
        long ms = clock.cycleEndInMillis(cycle);
        outlays.tick(ms);
        // organized by cycles
        extending.tick(cycle);
    }
}
