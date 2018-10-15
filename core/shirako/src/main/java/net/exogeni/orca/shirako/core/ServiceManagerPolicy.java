/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.core;

import java.util.HashSet;
import java.util.Iterator;

import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.api.IReservationEventHandler;
import net.exogeni.orca.shirako.api.IServiceManager;
import net.exogeni.orca.shirako.api.IServiceManagerPolicy;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.shirako.util.Bids;
import net.exogeni.orca.shirako.util.ReservationSet;
import net.exogeni.orca.util.OrcaException;
import net.exogeni.orca.util.persistence.NotPersistent;
import net.exogeni.orca.util.persistence.Persistent;

/**
 * Base implementation for all service manager policies. This class contains no
 * actual policy and is intended to serve as the skeleton for service manager
 * policy implementations. The class provides the minimal hooks for recovery.
 */
public class ServiceManagerPolicy extends Policy implements IServiceManagerPolicy {
    /**
     * Initialization status.
     */
    @NotPersistent
    private boolean initialized;

    /**
     * An event handler for upcalls
     */
    @NotPersistent
    protected HashSet<IReservationEventHandler> events;

    /**
     * If true, the service manager will close reservations lazily: it will not
     * issue a close and will wait until the site terminates the lease. The
     * major drawback is that leave actions will not be able to connect to the
     * resources, since the resources will not exist at this time.
     */
    @Persistent(key = "LazyClose")
    protected boolean lazyClose;

    /**
     * Default constructor.
     */
    public ServiceManagerPolicy() {
        this.events = new HashSet<IReservationEventHandler>();
    }

    /**
     * Creates a new instance.
     * 
     * @param actor
     *            actor the mapper belongs to
     */
    public ServiceManagerPolicy(final IServiceManager actor) {
        super(actor);
    }

    /**
     * {@inheritDoc}
     */
    public void demand(final IClientReservation reservation) {
    }

    /**
     * {@inheritDoc}
     */
    public Bids formulateBids(final long cycle) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ReservationSet getHoldings(final long cycle) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ReservationSet getRedeeming(final long cycle) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() throws OrcaException {
        if (!initialized) {
            super.initialize();
            initialized = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void leaseSatisfies(final ResourceSet requestedResources,
            final ResourceSet actualResources, final Term requestedTerm, final Term actualTerm)
            throws Exception {
    }

    /**
     * Raises the "onBeforeExtendTicket" event for the specified reservation.
     * 
     * @param r
     *            reservation
     */
    protected void onBeforeExtendTicket(final IReservation r) {
        Iterator<IReservationEventHandler> i = events.iterator();

        while (i.hasNext()) {
            i.next().onBeforeExtendTicket(r);
        }
    }

    /**
     * Raises the "onClose" event for the specified reservation.
     * 
     * @param r
     *            reservation
     */
    protected void onClose(final IReservation r) {
        Iterator<IReservationEventHandler> i = events.iterator();

        while (i.hasNext()) {
            i.next().onClose(r);
        }
    }

    /**
     * Raises the "onCloseComplete" event for the specified reservation.
     * 
     * @param r
     *            reservation
     */
    protected void onCloseComplete(final IReservation r) {
        Iterator<IReservationEventHandler> i = events.iterator();

        while (i.hasNext()) {
            i.next().onCloseComplete(r);
        }
    }

    /**
     * Raises the "onExtendLease" event for the specified reservation.
     * 
     * @param r
     *            reservation
     */
    protected void onExtendLease(final IReservation r) {
        Iterator<IReservationEventHandler> i = events.iterator();

        while (i.hasNext()) {
            i.next().onExtendLease(r);
        }
    }

    /**
     * Raises the "onExtendLeaseComplete" event for the specified reservation.
     * 
     * @param r
     *            reservation
     */
    protected void onExtendLeaseComplete(final IReservation r) {
        Iterator<IReservationEventHandler> i = events.iterator();

        while (i.hasNext()) {
            i.next().onExtendLeaseComplete(r);
        }
    }

    /**
     * Raises the "onExtendTicket" event for the specified reservation.
     * 
     * @param r
     *            reservation
     */
    protected void onExtendTicket(final IReservation r) {
        Iterator<IReservationEventHandler> i = events.iterator();

        while (i.hasNext()) {
            i.next().onExtendTicket(r);
        }
    }

    /**
     * Raises the "onExtendTicketComplete" event for the specified reservation.
     * 
     * @param r
     *            reservation
     */
    protected void onExtendTicketComplete(final IReservation r) {
        Iterator<IReservationEventHandler> i = events.iterator();

        while (i.hasNext()) {
            i.next().onExtendTicketComplete(r);
        }
    }

    /**
     * Raises the "onLease" event for the specified reservation.
     * 
     * @param r
     *            reservation
     */
    protected void onLease(final IReservation r) {
        Iterator<IReservationEventHandler> i = events.iterator();

        while (i.hasNext()) {
            i.next().onLease(r);
        }
    }

    /**
     * Raises the "onLeaseComplete" event for the specified reservation.
     * 
     * @param r
     *            reservation
     */
    protected void onLeaseComplete(final IReservation r) {
        Iterator<IReservationEventHandler> i = events.iterator();

        while (i.hasNext()) {
            i.next().onLeaseComplete(r);
        }
    }

    /**
     * Raises the "onTicket" event for the specified reservation.
     * 
     * @param r
     *            reservation
     */
    protected void onTicket(final IReservation r) {
        Iterator<IReservationEventHandler> i = events.iterator();

        while (i.hasNext()) {
            i.next().onTicket(r);
        }
    }

    /**
     * Raises the "onTicketComplete" event for the specified reservation.
     * 
     * @param r
     *            reservation
     */
    protected void onTicketComplete(final IReservation r) {
        Iterator<IReservationEventHandler> i = events.iterator();

        while (i.hasNext()) {
            i.next().onTicketComplete(r);
        }
    }

    /**
     * Registers an event handler.
     * 
     * @param e
     *            event handler
     */
    public void register(final IReservationEventHandler e) {
        events.add(e);
    }

    /**
     * {@inheritDoc}
     */
    public void ticketSatisfies(final ResourceSet requestedResources,
            final ResourceSet actualResources, final Term requestedTerm, final Term actualTerm)
            throws Exception {
    }

    /**
     * Unregisters a previously registered event handler.
     * 
     * @param e
     *            event handler
     */
    public void unregister(final IReservationEventHandler e) {
        events.remove(e);
    }

    /**
     * {@inheritDoc}
     */
    public void updateTicketComplete(final IClientReservation reservation) throws Exception {
    }
}
