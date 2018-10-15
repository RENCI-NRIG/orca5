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

import net.exogeni.orca.shirako.api.IAuthorityReservation;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.api.IServerReservation;
import net.exogeni.orca.shirako.time.ActorClock;
import net.exogeni.orca.shirako.util.ReservationHoldings;
import net.exogeni.orca.shirako.util.ReservationList;
import net.exogeni.orca.shirako.util.ReservationSet;

import java.util.Date;


/**
 * An <code>AuthorityCalendar</code> is used to organized reservation
 * information for an authority. It extends the functionality of
 * <code>BaseCalendar</code> with a number of collections:
 * <ul>
 * <li>requests: a collection of client requests organized by the time to be
 * serviced
 * <li>closing: a collection of client reservations organized by closing time
 * <li>outlays: a collection of active reservations (outlays)
 * </ul>
 */
public class AuthorityCalendar extends BaseCalendar
{
    /**
     * List of incoming requests grouped by start cycle.
     */
    protected ReservationList requests;

    /**
     * List of reservations to be closed grouped by cycle.
     */
    protected ReservationList closing;

    /**
     * All currently active reservations.
     */
    protected ReservationHoldings outlays;

    /**
     * Creates a new instance.
     * @param clock clock factory
     */
    public AuthorityCalendar(final ActorClock clock)
    {
        super(clock);
        requests = new ReservationList();
        closing = new ReservationList();
        outlays = new ReservationHoldings();
    }

    /**
     * Removes the specified reservation from the calendar.
     * @param reservation reservation to remove
     */
    public synchronized void remove(final IReservation reservation)
    {
        if (reservation instanceof IServerReservation) {
            removeRequest(reservation);
            removeClosing(reservation);
        }

        if (reservation instanceof IAuthorityReservation) {
            removeOutlay(reservation);
        }
    }

    /**
     * Removes the specified reservations from all internal calendar data
     * structures that represent operations to be scheduled in the future or
     * operations that are currently in progress. Does not remove the
     * reservation from the outlays list
     * @param reservation reservation to remove
     */
    public synchronized void removeScheduledOrInProgress(final IReservation reservation)
    {
        if (reservation instanceof IServerReservation) {
            removeRequest(reservation);
            removeClosing(reservation);
        }
    }

    /**
     * Returns all client requests for the specified cycle.
     *
     * @param cycle cycle
     *
     * @return set of requests for the cycle
     */
    public synchronized ReservationSet getRequests(final long cycle)
    {
        return requests.getReservations(cycle);
    }

    /**
     * Adds a new client request.
     *
     * @param reservation reservation to add
     * @param cycle cycle
     */
    public synchronized void addRequest(final IReservation reservation, final long cycle)
    {
        requests.addReservation(reservation, cycle);
    }

    /**
     * Removes the specified reservation from the request list.
     * @param reservation reservation
     */
    public synchronized void removeRequest(final IReservation reservation)
    {
        requests.removeReservation(reservation);
    }

    /**
     * Returns all reservations scheduled for closing at the specified cycle.
     *
     * @param cycle cycle
     *
     * @return set of reservations scheduled for closing at the cycle
     *
     */
    public synchronized ReservationSet getClosing(final long cycle)
    {
        return closing.getAllReservations(cycle);
    }

    /**
     * Adds a reservation to the closing list.
     *
     * @param reservation reservation to add
     * @param cycle cycle
     */
    public synchronized void addClosing(final IReservation reservation, final long cycle)
    {
        closing.addReservation(reservation, cycle);
    }

    /**
     * Removes the specified reservation from the closing list.
     *
     * @param reservation reservation to remove
     */
    public synchronized void removeClosing(final IReservation reservation)
    {
        closing.removeReservation(reservation);
    }

    /**
     * Adds an allocated client reservation.
     *
     * @param reservation reservation to add
     * @param start start time
     * @param end end time
     */
    public synchronized void addOutlay(final IReservation reservation, final Date start,
                                       final Date end)
    {
        outlays.addReservation(reservation, start.getTime(), end.getTime());
    }

    /**
     * Removes a reservation from the outlays list.
     * @param reservation reservation to remove
     */
    public synchronized void removeOutlay(final IReservation reservation)
    {
        outlays.removeReservation(reservation);
    }

    /**
     * Returns the active client reservations.
     *
     * @return set of all active client reservations
     */
    public synchronized ReservationSet getOutlays()
    {
        return outlays.getReservations();
    }

    /**
     * Returns the active client reservations at the given time instance.
     *
     * @param date date 
     *
     * @return set of client reservations active at the specified time instance
     */
    public synchronized ReservationSet getOutlays(final Date date)
    {
        return outlays.getReservations(date.getTime());
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void tick(final long cycle)
    {
        super.tick(cycle);
        // organized by cycle
        requests.tick(cycle);
        // organized by cycle
        closing.tick(cycle);

        // organized by real time
        long ms = clock.cycleEndInMillis(cycle);
        outlays.tick(ms);
    }
}
