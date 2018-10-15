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

import net.exogeni.orca.shirako.api.IBrokerReservation;
import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.time.ActorClock;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.shirako.util.ReservationList;
import net.exogeni.orca.shirako.util.ReservationSet;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * An <code>BrokerCalendar</code>is used to organize reservation
 * information for a broker. It builds on the functionality provided by
 * <code>ClientCalendar</code> and extends it with the following lists:
 *  <ul>
 *      <li>closing: list of reservations organized by closing time</li>
 *      <li>requests: list of incoming requests</li>
 *      <li>source calendars for each source reservation</li>
 *  </ul>
 */
public class BrokerCalendar extends ClientCalendar
{
    /**
     * List of reservations grouped by closing time.
     */
    protected ReservationList closing;

    /**
     * Reservation requests grouped by start cycle.
     */
    protected ReservationList requests;

    /**
     * Source reservation calendars indexed by the source reservation
     * identifier.
     */
    protected HashMap<ReservationID, SourceCalendar> sources;

    /**
         * Creates a new instance.
         * @param clock clock factory
         */
    public BrokerCalendar(final ActorClock clock)
    {
        super(clock);

        closing = new ReservationList();
        requests = new ReservationList();
        sources = new HashMap<ReservationID, SourceCalendar>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void remove(final IReservation reservation)
    {
        super.remove(reservation);
        removeClosing(reservation);

        if (reservation instanceof IBrokerReservation) {
            removeRequest(reservation);

            IReservation source = ((IBrokerReservation) reservation).getSource();

            if (source != null) {
                removeRequest(source, reservation);
                removeOutlay(source, reservation);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void removeScheduledOrInProgress(final IReservation reservation)
    {
        super.removeScheduledOrInProgress(reservation);
        removeClosing(reservation);

        if (reservation instanceof IBrokerReservation) {
            removeRequest((IBrokerReservation) reservation);

            IReservation source = ((IBrokerReservation) reservation).getSource();

            if (source != null) {
                removeRequest(source, reservation);
            }
        }
    }

    /**
     * Returns all client requests for the given cycle.
     *
     * @param cycle cycle
     *
     * @return set of reservations representing requests starting at the
     *         specified cycle
     */
    public synchronized ReservationSet getRequests(final long cycle)
    {
        return requests.getReservations(cycle);
    }

    /**
     * Returns all client requests up the the given cycle.
     * @param cycle cylce
     * @return set of reservations representing requests with start time no later than cycle
     */
    public synchronized ReservationSet getAllRequests(final long cycle) {
        return requests.getAllReservations(cycle);
    }
    /**
     * Adds a client request.
     *
     * @param reservation client request
     * @param cycle start cycle
     */
    public synchronized void addRequest(final IReservation reservation, final long cycle)
    {
        requests.addReservation(reservation, cycle);
    }

    /**
     * Adds an extending reservation.
     *
     * @param reservation reservation to add
     * @param cycle desired start cycle
     * @param source source reservation
     */
    public synchronized void addRequest(final IReservation reservation, final long cycle,
                                        final IReservation source)
    {
        SourceCalendar calendar = getSourceCalendar(source);
        calendar.extending.addReservation(reservation, cycle);
    }

    /**
     * Returns the extending requests for the given source reservation.
     *
     * @param source source reservation
     * @param cycle cycle number
     *
     * @return set of extending reservation requests for the given source at
     *         the specified cycle
     */
    public synchronized ReservationSet getRequests(final IReservation source, final long cycle)
    {
        SourceCalendar calendar = getSourceCalendar(source);

        return calendar.extending.getReservations(cycle);
    }

    /**
     * Removes the specified reservation from the requests list.
     *
     * @param reservation reservation to remove
     */
    public synchronized void removeRequest(final IReservation reservation)
    {
        requests.removeReservation(reservation);
    }

    /**
     * Removes a reservation request from the source calendar of the
     * specified source reservation.
     *
     * @param source source reservation
     * @param request reservation to remove
     */
    public synchronized void removeRequest(final IReservation source, final IReservation request)
    {
        SourceCalendar calendar = getSourceCalendar(source);
        calendar.extending.removeReservation(request);
    }

    /**
     * Adds an outlay reservation.
     *
     * @param source source reservation
     * @param client reservation to add
     * @param start start time
     * @param end start time
     */
    public synchronized void addOutlay(final IReservation source, final IReservation client,
                                       final Date start, final Date end)
    {
        SourceCalendar calendar = getSourceCalendar(source);
        calendar.outlays.addReservation(client, start.getTime(), end.getTime());
    }

    /**
     * Removes an outlay reservation.
     *
     * @param source source reservation
     * @param client client reservation to be removed
     */
    public synchronized void removeOutlay(final IReservation source, final IReservation client)
    {
        SourceCalendar calendar = getSourceCalendar(source);
        calendar.outlays.removeReservation(client);
    }

    /**
     * Adds a source reservation. Creates a placeholder if necessary
     * and adds the reservation to the holdings list.
     *
     * @param source source reservation
     */
    public synchronized void addSource(final IClientReservation source)
    {
        // create a source place holder
        getSourceCalendar(source);

        Term term = source.getTerm();
        addHolding(source, term.getNewStartTime(), term.getEndTime());
    }

    /**
     * Returns the outlay calendar for the given source reservation.
     *
     * @param source source reservation
     *
     * @return source calendar
     */
    private SourceCalendar getSourceCalendar(final IReservation source)
    {
        SourceCalendar calendar = (SourceCalendar) sources.get(source.getReservationID());

        if (calendar == null) {
            calendar = new SourceCalendar(clock, source);
            sources.put(source.getReservationID(), calendar);
        }

        return calendar;
    }

    /**
     * Removes any data structures associated with a source
     * reservation.
     *
     * @param source source reservation
     */
    public synchronized void removeSourceCalendar(final IReservation source)
    {
        sources.remove(source.getReservationID());
    }

    /**
     * Returns the client reservations satisfied from the given source
     * reservation
     *
     * @param source source reservation
     *
     * @return set of client reservations satisfied from the given source
     */
    public synchronized ReservationSet getOutlays(final IReservation source)
    {
        SourceCalendar calendar = getSourceCalendar(source);

        return calendar.outlays.getReservations();
    }

    /**
     * Returns the client reservations satisfied from the given source
     * that are active at the specified time.
     *
     * @param source source reservation
     * @param time time instance
     *
     * @return the outlays calendar for the specified source at the specified
     *         time
     */
    public synchronized ReservationSet getOutlays(final IReservation source, final Date time)
    {
        SourceCalendar calendar = getSourceCalendar(source);

        return calendar.outlays.getReservations(time.getTime());
    }

    /**
     * Returns all reservations that need to be closed on the specified
     * cycle.
     *
     * @param cycle cycle
     *
     * @return a set of reservations to be closed on the specified cycle
     */
    public synchronized ReservationSet getClosing(final long cycle)
    {
        return closing.getAllReservations(cycle);
    }

    /**
     * Adds a reservation to be closed on the specified cycle
     *
     * @param reservation reservation to close
     * @param cycle cycle
     */
    public synchronized void addClosing(final IReservation reservation, final long cycle)
    {
        closing.addReservation(reservation, cycle);
    }

    /**
     * Removes the specified reservation from the list of closing
     * reservations.
     *
     * @param reservation reservation to remove
     */
    public synchronized void removeClosing(final IReservation reservation)
    {
        closing.removeReservation(reservation);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void tick(final long cycle)
    {
        super.tick(cycle);

        requests.tick(cycle);
        closing.tick(cycle);

        // tick the source calendars
        Set<?> entries = sources.entrySet();
        Iterator<?> it = entries.iterator();

        while (it.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
            SourceCalendar calendar = (SourceCalendar) entry.getValue();
            calendar.tick(cycle);
        }
    }
}
