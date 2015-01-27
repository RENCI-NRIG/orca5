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
import orca.shirako.util.ReservationSet;
import orca.util.ResourceType;

import java.util.Date;


/**
 * This a client-side calendar to be used by brokers or service managers. A client
 * calendar maintains the following lists:
 * <ul>
 * <li>demand: a list of reservations representing demand for resources</li>
 * <li>pending: a list of reservations with pending operations</li>
 * <li>renewing: a list of reservations organized by the time they must be
 * renewed (cycle)</li>
 * <li>holdings: a list of active/granted reservations associated with their
 * lease term. This list should be maintained using <bold>real</bold> time, not
 * cycles.</li>
 * </ul>
 * <p>
 * The renewing and holding lists are automatically purged by the implementation as
 * time advances. The demand and pending list, however, must be purged manually by the
 * user of this class.
 */
public class ClientCalendar extends BaseCalendar
{
    /**
     * Set of reservations representing the current demand. Callers are
     * responsible for removing serviced reservations
     */
    protected ReservationSet demand;

    /**
     * Set of reservations for which a request has been issued but no
     * confirmation has been received. Callers are responsible for removing
     * acknowledged reservations.
     */
    protected ReservationSet pending;

    /**
     * Set of reservations grouped by renewing time.
     */
    protected ReservationList renewing;

    /**
     * Set of active reservations.
     */
    protected ReservationHoldings holdings;

    /**
     * Creates a new instance.
     * @param clock clock factory
     */
    public ClientCalendar(final ActorClock clock)
    {
        super(clock);

        demand = new ReservationSet();
        pending = new ReservationSet();
        renewing = new ReservationList();
        holdings = new ReservationHoldings();
    }

    /**
     * Removes the specified reservation from all internal calendar data
     * structures.
     * @param reservation reservation to remove
     */
    public synchronized void remove(final IReservation reservation)
    {
        removeDemand(reservation);
        removePending(reservation);
        removeRenewing(reservation);
        removeHolding(reservation);
    }

    /**
     * Removes the specified reservations from all internal calendar data
     * structures that represent operations to be scheduled in the future or
     * operations that are currently in progress. Does not remove the
     * reservation from the holdings list.
     * @param reservation reservation to remove
     */
    public synchronized void removeScheduledOrInProgress(final IReservation reservation)
    {
        removeDemand(reservation);
        removePending(reservation);
        removeRenewing(reservation);
    }

    /**
     * Returns the known demand. Can be for resources starting at different
     * times.
     * @return the set of demanded reservations. The returned set is a clone of
     *         the internal set. To remove reservations from the demand list use
     *         {@link #removeDemand(IReservation)}.
     */
    public synchronized ReservationSet getDemand()
    {
        return (ReservationSet) demand.clone();
    }

    /**
     * Adds a reservation to the demand list.
     * @param reservation reservation to add
     */
    public synchronized void addDemand(final IReservation reservation)
    {
        demand.add(reservation);
    }

    /**
     * Removes the specified reservation from the demand list.
     * @param reservation reservation to remove
     */
    public synchronized void removeDemand(final IReservation reservation)
    {
        demand.remove(reservation);
    }

    /**
     * Returns all pending reservations.
     * @return the set of pending reservations. The returned set is a clone of
     *         the internal set. To remove reservations from the pending list
     *         use {@link #removePending(IReservation)}.
     */
    public synchronized ReservationSet getPending()
    {
        return (ReservationSet) pending.clone();
    }

    /**
     * Adds the reservation to the pending list.
     * @param reservation to add
     */
    public synchronized void addPending(IReservation reservation)
    {
        pending.add(reservation);
    }

    /**
     * Removes the reservation from the pending list.
     * @param reservation reservation to remove
     */
    public synchronized void removePending(IReservation reservation)
    {
        pending.remove(reservation);
    }

    /**
     * Returns the reservations that need to be renewed on the specified cycle.
     * @param cycle cycle number
     * @return reservation set with reservations to be renewed on the specified
     *         cycle
     */
    public synchronized ReservationSet getRenewing(long cycle)
    {
        return renewing.getAllReservations(cycle);
    }

    /**
     * Adds a reservation to the renewing list at the given cycle.
     * @param reservation reservation to add
     * @param cycle cycle number
     */
    public synchronized void addRenewing(IReservation reservation, long cycle)
    {
        renewing.addReservation(reservation, cycle);
    }

    /**
     * Removes the reservation from the renewing list.
     * @param reservation reservation to remove
     */
    public synchronized void removeRenewing(IReservation reservation)
    {
        renewing.removeReservation(reservation);
    }

    /**
     * Returns the resources held by the client that are active at the specified
     * time instance.
     * @param time time instance.
     * @return st of reservations that are active at the specified time
     */
    public synchronized ReservationSet getHoldings(Date time)
    {
        return holdings.getReservations(time.getTime());
    }

    /**
     * Returns the resources held by the client.
     * @return set of all active reservations
     */
    public synchronized ReservationSet getHoldings()
    {
        return holdings.getReservations();
    }

    /**
     * Returns the resources of the specified type held by the client that are
     * active at the specified time instance.
     * @param time time instance
     * @param type resource type
     * @return set of reservations from the specified type active at the
     *         specified time instance
     */
    public synchronized ReservationSet getHoldings(Date time, ResourceType type)
    {
        return holdings.getReservations(time.getTime(), type);
    }

    /**
     * Adds a reservation to the holdings list.
     * @param reservation reservation to add
     * @param start start time
     * @param end end time
     */
    public synchronized void addHolding(IReservation reservation, Date start, Date end)
    {
        holdings.addReservation(reservation, start.getTime(), end.getTime());
    }

    /**
     * Removes the given reservation from the holdings list.
     * @param reservation reservation to remove
     */
    public synchronized void removeHolding(IReservation reservation)
    {
        holdings.removeReservation(reservation);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void tick(long cycle)
    {
        super.tick(cycle);

        // Note: not cleaning demand and pending
        // clients are responsible for keeping demand and pending clean.

        // organized by cycles
        renewing.tick(cycle);

        // holdings is organized by real time
        long ms = clock.cycleEndInMillis(cycle);
        holdings.tick(ms);
    }
}