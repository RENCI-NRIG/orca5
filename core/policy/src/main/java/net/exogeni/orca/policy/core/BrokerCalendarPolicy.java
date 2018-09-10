/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.policy.core;

import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import net.exogeni.orca.shirako.api.IBrokerReservation;
import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.core.BrokerPolicy;
import net.exogeni.orca.shirako.kernel.ReservationStates;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.shirako.time.calendar.BrokerCalendar;
import net.exogeni.orca.shirako.util.ReservationSet;
import net.exogeni.orca.shirako.util.ResourceCount;
import net.exogeni.orca.shirako.util.ResourceData;
import net.exogeni.orca.util.OrcaException;
import net.exogeni.orca.util.PropList;
import net.exogeni.orca.util.persistence.NotPersistent;


/**
 * <code>BrokerBasePolicyPlugin</code> specifies and implements some of the
 * broker's base resource allocation and upstream bidding policy.
 */
public abstract class BrokerCalendarPolicy extends BrokerPolicy
{
    /*
     * XXX: those constants should probably be extracted into the properties manager file?
     */
    public static final String PropertyTypeNamePrefix = "type.name.";
    public static final String PropertyTypeDescriptionPrefix = "type.description.";
    public static final String PropertyTypeUnitsPrefix = "type.units.";
    public static final String PropertyTypeCount = "type.count";
    public static final String PropertyDiscoverTypes = "query.discovertypes";

    /**
     * The broker calendar: list of client requests, source
     * reservations, and allocated reservations.
     */
    @NotPersistent
    protected BrokerCalendar calendar;

    /**
     * Indicates if this actor is initialized
     */
    @NotPersistent
    private boolean initialized = false;

    /**
         * Creates a "blank" instance.
         */
    public BrokerCalendarPolicy()
    {
    }

    /**
     * Adds the reservation to the approval list and removes the
     * reservation from the closing calendar (if it belongs to it).
     *
     * @param reservation reservation to add
     * @throws Exception in case of error
     */
    protected void addForApprovalCalendar(final IBrokerReservation reservation)
                                   throws Exception
    {
        /*
         * First remove from the closing calendar
         */
        if (reservation.getTerm() != null) {
            calendar.removeClosing(reservation);
        }

        /*
         * Now add to the approval list
         */
        addForApproval(reservation);
    }

    /**
     * Records the reservation in the calendar.
     *
     * @param reservation reservation
     *
     */
    protected void addToCalendar(final IBrokerReservation reservation)
    {
        if ((reservation.getApprovedResources() != null) && !reservation.isFailed()) {
            // add to the outlays calendar
            calendar.addOutlay(reservation.getSource(),
                               reservation,
                               reservation.getApprovedTerm().getNewStartTime(),
                               reservation.getApprovedTerm().getEndTime());

            // remove from the closing calendar
            if (reservation.getTerm() != null) {
                calendar.removeClosing(reservation);
            }

            // add to the closing under the new term
            calendar.addClosing(reservation,
                                clock.cycle(reservation.getApprovedTerm().getEndTime()));

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "AgentAllocated: units=" + reservation.getApprovedResources().getUnits() +
                    " res:" + reservation.toString() + " term=" +
                    reservation.getApprovedTerm().toString());
            }
        } else {
            String error = "Either there are no resources on the source or the reservation failed";
            reservation.fail(error);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void approve(final IBrokerReservation reservation)
    {
        /*
         * Our current policy is to allow the allocation code to issue a ticket
         * but to prevent the ticket from being sent to the client until the
         * administrator approves the allocation.
         */

        // remove from the approval list
        removeForApproval(reservation);
        // add to the calendar
        addToCalendar(reservation);
        // unblock
        reservation.setBidPending(false);
    }

    /**
     * Checks pending bids, and installs successfully completed
     * requests in the holdings calendar. Note that the policy module must add
     * bids to the pending set, or they may not install in the calendar.
     *
     * @throws Exception in case of error
     */
    protected void checkPending() throws Exception
    {
        ReservationSet rvset = calendar.getPending();

        if (rvset == null) {
            return;
        }

        Iterator<IReservation> i = rvset.iterator();

        while (i.hasNext()) {
            IClientReservation r = (IClientReservation) i.next();

            if (!r.isNascent() && r.isNoPending()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Pending request completed " + r);
                }

                if (!r.isTerminal()) {
                    if (r.isRenewable()) {
                    	// figure out when to renew
                        long cycle = getRenew(r);
                        r.setRenewTime(cycle);
                        r.setDirty();
                    }                  
                    // NOTE: donates happen during updateTicket, nothing to do here
                    //donate(r);
                }
                calendar.removePending(r);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(final IReservation reservation)
    {
        if (reservation instanceof IClientReservation) {
            // we are closing a source reservation
            // close all reservations that are derived from it
            ReservationSet rset = calendar.getOutlays(reservation);
            actor.close(rset);
        } else {        
            /* remove future and in-progress operations */
            calendar.removeScheduledOrInProgress(reservation);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closed(final IReservation reservation)
    {
        if (reservation instanceof IClientReservation) {
            release((IClientReservation) reservation);
        } else {
            release((IBrokerReservation) reservation);
        }
    }

    /**
     * Returns a counter for the passed set and the specified data.
     *
     * @param set the set of reservations being counted
     * @param when the date when to count the resources
     *
     * @return counter
     */
    protected ResourceCount count(final ReservationSet set, final Date when)
    {
        ResourceCount rc = new ResourceCount();
        set.count(rc, when);

        return rc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void donate(final IClientReservation reservation) throws Exception
    {
        Term term = reservation.getTerm();
        term.validate();

        if (logger.isDebugEnabled()) {
            logger.debug("Donated ticket = " + reservation.toLogString() + " " + term.toString());
        }

        term = reservation.getPreviousTicketTerm();

        if (term != null) {
            calendar.removeClosing(reservation);
        }

        calendar.addSource(reservation);
        term = reservation.getTerm();
        calendar.addClosing(reservation, clock.cycle(term.getEndTime()));

        if (reservation.isRenewable()) {
            calendar.addRenewing(reservation, reservation.getRenewTime());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finish(final long cycle)
    {
        calendar.tick(cycle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReservationSet getClosing(final long cycle)
    {
        return calendar.getClosing(cycle);
    }

    /**
     * Returns the cycle when the reservation must be renewed.
     *
     * @param reservation reservation for which to calculate renew time
     *
     * @return renew cycle
     *
     * @throws Exception in case of error
     */
    protected abstract long getRenew(IClientReservation reservation) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() throws OrcaException
    {
        if (!initialized) {
            super.initialize();
            // create the calendar
            this.calendar = new BrokerCalendar(clock);

            // create the approval set if necessary
            if (requireApproval) {
                forApproval = new ReservationSet();
            }

            // done
            initialized = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Properties query(final Properties properties)
    {
        Properties p = new Properties();

        if ((properties != null) && properties.containsKey(PropertyDiscoverTypes)) {
            ReservationSet holdings = calendar.getHoldings();
            int count = 0;
            Iterator<IReservation> iter = holdings.iterator();

            while (iter.hasNext()) {
                try {
                    IClientReservation r = (IClientReservation) iter.next();
                    ResourceSet ticket = r.getResources();

                    if (ticket != null) {
                        ResourceData rdata = ticket.getResourceData();

                        if (rdata != null) {
                            Properties resourceProperties = rdata.getResourceProperties();

                            if (resourceProperties != null) {
                                p.setProperty(PropertyTypeNamePrefix + count,
                                              ticket.getType().getType());
                                PropList.setProperty(p,
                                                     PropertyTypeDescriptionPrefix + count,
                                                     properties);
                                count++;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("query", e);
                }
            }

            PropList.setProperty(p, PropertyTypeCount, count);
        }

        
        return p;
    }

    /**
     * Release resources associated with a client reservation.
     *
     * @param r client reservation
     */
    protected void release(final IBrokerReservation r)
    {
        /* remove from the outlays list */
        IClientReservation source = r.getSource();

        if (source != null) {
            calendar.removeOutlay(source, r);
        }
    }

    /**
     * Release resources associated with a source reservation.
     *
     * @param r source reservation
     */
    protected void release(final IClientReservation r)
    {
        calendar.removeSourceCalendar(r);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void releaseNotApproved(final IBrokerReservation reservation)
    {
        release(reservation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final IReservation reservation)
    {
        /* make sure we keep the calendar up-to-date */
        calendar.remove(reservation);

        /*
         * XXX: [aydan 05/28/07] should we call release? A reservation can be
         * removed only if it is in the closed/failed state. Removing a closed
         * reservation does not require any additional cleanup. What about
         * failed reservations? For reservations failed by the policy, e.g, lack
         * of resources, we do not need to do anything. But what if a
         * reservation fails because of some other error? Currently, we do not
         * fail a reservation if the callback to the service manger fails. But
         * what about internal errors/exceptions? Such errors may fail a
         * reservation with an already committed resources. Removing the
         * reservation may then result in a resource leak.
         */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revisit(final IReservation reservation) throws Exception
    {
        super.revisit(reservation);

        if (reservation instanceof IClientReservation) {
            revisitClient((IClientReservation) reservation);
        } else {
            revisitServer((IBrokerReservation) reservation);
        }
    }

    /**
     * Recovers a source reservation.
     *
     * @param r reservation to recover
     *
     * @throws Exception in case of error
     */
    protected void revisitClient(final IClientReservation r) throws Exception
    {
        switch (r.getState()) {
            case ReservationStates.Nascent:

                switch (r.getPendingState()) {
                    case ReservationStates.None:
                        calendar.addPending(r);

                        break;
                }

                break;

            case ReservationStates.Ticketed:

                switch (r.getPendingState()) {
                    case ReservationStates.None:

                        /*
                         * The parent class should have already issued the
                         * donate call. Do nothing here.
                         */
                        break;

                    case ReservationStates.ExtendingTicket:
                        calendar.addPending(r);

                        break;
                }

                break;
            
            // FIXME: Failed?
        }
    }

    /**
     * Recovers a client reservation.
     *
     * @param r reservation to recover
     *
     * @throws Exception in case of error
     */
    protected void revisitServer(final IBrokerReservation r) throws Exception
    {
        switch (r.getState()) {
            case ReservationStates.Ticketed:

                switch (r.getPendingState()) {
                    case ReservationStates.None:
                    case ReservationStates.Priming:

                        IClientReservation source = (IClientReservation) r.getSource();

                        if (source == null) {
                            throw new Exception("Missing source reservation");
                        }

                        /* add to the outlay calendar */
                        calendar.addOutlay(source,
                                           r,
                                           r.getTerm().getNewStartTime(),
                                           r.getTerm().getEndTime());
                        /* add to the closing calendar */
                        calendar.addClosing(r, clock.cycle(r.getTerm().getEndTime()));

                        break;
                }
        }
    }
}
