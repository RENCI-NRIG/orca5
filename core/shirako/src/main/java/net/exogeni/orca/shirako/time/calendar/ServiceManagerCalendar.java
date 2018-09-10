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

import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.time.ActorClock;
import net.exogeni.orca.shirako.util.ReservationList;
import net.exogeni.orca.shirako.util.ReservationSet;


/**
 * Service manager calendar. In addition to the lists maintained by {@link
 * ClientCalendar}, this class maintains the following lists:
 *  <ul>
 *      <li>closing: a list of reservations organized by the cycle they
 *      must be closed</li>
 *      <li>redeeming: a list of reservations organized by the cycle
 *      they must be redeemed</li>
 *  </ul>
 */
public class ServiceManagerCalendar extends ClientCalendar
{
    /**
     * List of reservations to be closed grouped by closing time.
     */
    protected ReservationList closing;

    /**
     * List of reservations to be redeemed grouped by redeeming time.
     */
    protected ReservationList redeeming;

    /**
     * Creates a new instance.
     * @param clock clock factory
     */
    public ServiceManagerCalendar(final ActorClock clock)
    {
        super(clock);

        closing = new ReservationList();
        redeeming = new ReservationList();
    }

    /**
     * Removes the reservation from the calendar.
     *
     * @param reservation reservation to remove
     */
    public synchronized void remove(final IReservation reservation)
    {
        super.remove(reservation);
        removeClosing(reservation);
        removeRedeeming(reservation);
    }

    /**
     * Returns all reservations that need to be closed up to and
     * including the specified cycle.
     *
     * @param cycle cycle
     *
     * @return a set of reservations that must be closed on the specified cycle
     */
    public synchronized ReservationSet getClosing(final long cycle)
    {
        return closing.getAllReservations(cycle);
    }

    /**
     * Adds a reservation to be closed on the specified cycle.
     *
     * @param reservation reservation to add
     * @param cycle cycle
     */
    public synchronized void addClosing(final IReservation reservation, final long cycle)
    {
        closing.addReservation(reservation, cycle);
    }

    /**
     * Removes the given reservation from the closing list.
     *
     * @param reservation reservation to remove
     */
    public synchronized void removeClosing(final IReservation reservation)
    {
        closing.removeReservation(reservation);
    }

    /**
     * Returns all reservations that need to be redeemed on the given
     * cycle.
     *
     * @param cycle cycle
     *
     * @return a set of reservations to be redeemed on the given cycle
     */
    public synchronized ReservationSet getRedeeming(final long cycle)
    {
        return redeeming.getAllReservations(cycle);
    }

    /**
     * Adds a reservation to be redeemed on the given cycle.
     *
     * @param reservation reservation to add
     * @param cycle redeeming cycle
     */
    public synchronized void addRedeeming(final IReservation reservation, final long cycle)
    {
        redeeming.addReservation(reservation, cycle);
    }

    /**
     * Removes the given reservation from the list of redeeming
     * reservations.
     *
     * @param reservation reservation to remove
     */
    public synchronized void removeRedeeming(final IReservation reservation)
    {
        redeeming.removeReservation(reservation);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void tick(final long cycle)
    {
        super.tick(cycle);
        // uses cycles
        closing.tick(cycle);
        // uses cycles
        redeeming.tick(cycle);
    }
}
