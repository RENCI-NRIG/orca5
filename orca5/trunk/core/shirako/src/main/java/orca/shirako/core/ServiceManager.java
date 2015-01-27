/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.core;

import java.util.Iterator;

import orca.manage.OrcaConstants;
import orca.manage.internal.ServiceManagerManagementObject;
import orca.security.AuthToken;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IClientPolicy;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.IServiceManagerPolicy;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.ReservationID;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.registry.PeerRegistry;
import orca.shirako.time.ActorClock;
import orca.shirako.util.Bids;
import orca.shirako.util.ReservationSet;
import orca.shirako.util.TestException;
import orca.shirako.util.UpdateData;
import orca.util.ID;
import orca.util.OrcaException;
import orca.util.persistence.NotPersistent;

/**
 * <code>ServiceManager</code> is the base implementation for a service manager
 * actor.
 */
public class ServiceManager extends Actor implements IServiceManager {
    /**
     * Recovered reservations that need to obtain tickets.
     */
    @NotPersistent
    protected ReservationSet ticketing = new ReservationSet();

    /**
     * Recovered reservations that need to extend tickets.
     */
    @NotPersistent
    protected ReservationSet extendingTicket = new ReservationSet();

    /**
     * Recovered reservations that need to be redeemed.
     */
    @NotPersistent
    protected ReservationSet redeeming = new ReservationSet();
    
    /**
     * Recovered reservations that need to extend leases.
     */
    @NotPersistent
    protected ReservationSet extendingLease = new ReservationSet();

    /**
     * Peer registry.
     */
    @NotPersistent
    protected PeerRegistry registry;

    /**
     * initialization status
     */
    @NotPersistent
    protected boolean initialized;

    /**
     * Creates a new instance.
     */
    public ServiceManager() {
        registry = new PeerRegistry();
        type = OrcaConstants.ActorTypeServiceManager;
    }

    /**
     * Creates a new service manager with the given identity and clock factory.
     * 
     * @param identity
     *            actor identity
     * @param clock
     *            clock factory
     */
    public ServiceManager(final AuthToken identity, final ActorClock clock) {
        super(identity, clock);
        registry = new PeerRegistry();
        type = OrcaConstants.ActorTypeServiceManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actorAdded() throws Exception {
        super.actorAdded();
        registry.actorAdded();
    }

    /**
     * {@inheritDoc}
     */
    public void addBroker(final IBrokerProxy broker) {
        registry.addBroker(broker);
    }

    /**
     * Bids for resources as dictated by the plugin bidding policy for the
     * current cycle.
     * 
     * @throws Exception
     */
    protected void bid() throws Exception {
        /*
         * Invoke policy module to select candidates for ticket and extend. Note
         * that candidates structure is discarded when we're done.
         */
        Bids candidates = ((IClientPolicy) policy).formulateBids(currentCycle);

        if (candidates != null) {
            ReservationSet ticketing = candidates.getTicketing();

            if (ticketing != null) {
                /*
                 * Issue new ticket requests.
                 */
                Iterator<IReservation> iter = ticketing.iterator();

                while (iter.hasNext()) {
                    IClientReservation r = (IClientReservation) iter.next();

                    try {
                        wrapper.ticket(r, this);
                    } catch (TestException e) {
                        throw e;
                    } catch (Exception e) {
                        logger.error("unexpected ticket failure for #"
                                + r.getReservationID().toHashString(), e);
                        r.fail("unexpected ticket failure", e);
                    }
                }
            }

            ReservationSet extending = candidates.getExtending();

            if (extending != null) {
                /*
                 * Issue extends for the renewal candidates.
                 */
                Iterator<IReservation> iter = extending.iterator();

                while (iter.hasNext()) {
                    IClientReservation r = (IClientReservation) iter.next();

                    try {
                        wrapper.extendTicket(r);
                    } catch (Exception e) {
                        logger.error("unexpected extend failure for #"
                                + r.getReservationID().toHashString(), e);
                        r.fail("unexpected ticket failure", e);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public IClientReservation claim(final ReservationID reservationID, final ResourceSet resources)
            throws Exception {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public IClientReservation claim(final ReservationID reservationID, final ResourceSet resources,
            final IBrokerProxy broker) throws Exception {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public IClientReservation claim(final ReservationID reservationID, final ResourceSet resources,
            final ISlice slice, final IBrokerProxy broker) throws Exception {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Issues close requests on all reservations scheduled for closing on the
     * current cycle
     */
    protected void closeExpiring() {
        ReservationSet set = policy.getClosing(currentCycle);

        if ((set != null) && (set.size() > 0)) {
            if (logger.isInfoEnabled()) {
                logger.info("SlottedSM close expiring for cycle " + currentCycle + " expiring "
                        + set.toLogString());
            }

            close(set);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void demand(final ReservationID rid) throws Exception {
        if (rid == null) {
            throw new IllegalArgumentException();
        }

        IClientReservation rc = (IClientReservation) getReservation(rid);
        if (rc == null) {
            throw new Exception("Unknown reservation: " + rid);
        }

        ((IClientPolicy) policy).demand(rc);
        rc.setPolicy((IClientPolicy) policy);
    }

    /**
     * {@inheritDoc}
     */
    public void extendLease(final IServiceManagerReservation reservation) throws Exception {
        if (!recovered) {
            extendingLease.add(reservation);
        }else {
            wrapper.extendLease(reservation);
        }
    }

    /**
     * Redeem all reservations.
     * @param set
     */
    public void extendLease(final ReservationSet set){
        for (IReservation r: set) {
            try {
                if (r instanceof IServiceManagerReservation) {
                    extendLease((IServiceManagerReservation)r);
                }else {
                    logger.warn("Reservation #" + r.getReservationID().toHashString() + " cannot extendLease");
                }
            } catch (Exception e) {
                logger.error("Could not exntedLease for #" + r.getReservationID().toHashString(), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void extendTicket(final IClientReservation reservation) throws Exception {
        if (!recovered) {
            extendingTicket.add(reservation);
        }else {
            wrapper.extendTicket(reservation);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void extendTicket(final ReservationSet set){
        for (IReservation r: set) {
            try {
                if (r instanceof IClientReservation) {
                    extendTicket((IClientReservation)r);
                }else {
                    logger.warn("Reservation #" + r.getReservationID() + " cannot be ticketed");
                }
            } catch (Exception e) {
                logger.error("Could not ticket for #" + r.getReservationID().toHashString());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public IBrokerProxy getBroker(final ID guid) {
        return registry.getBroker(guid);
    }

    /**
     * Returns all brokers registered with the service manager.
     * 
     * @return an array of brokers
     */
    public IBrokerProxy[] getBrokers() {
        return registry.getBrokers();
    }

    /**
     * {@inheritDoc}
     */
    public IBrokerProxy getDefaultBroker() {
        return registry.getDefaultBroker();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() throws OrcaException {
        if (!initialized) {
            super.initialize();

            registry.setSlicesPlugin(spi);
            registry.initialize();

            // spi.reset();
            initialized = true;
        }
    }

    /**
     * Issue redeem requests on all reservations scheduled for redeeming on the
     * current cycle
     */
    protected void processRedeeming() {
        ReservationSet set = ((IServiceManagerPolicy) policy).getRedeeming(currentCycle);

        if ((set != null) && (set.size() > 0)) {
            if (logger.isInfoEnabled()) {
                logger.info("SlottedSM redeem for cycle " + currentCycle + " redeeming "
                        + set.toLogString());
            }

            redeem(set);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void redeem(final IServiceManagerReservation reservation) throws Exception {
        if (!recovered) {
            redeeming.add(reservation);
        }else {
            wrapper.redeem(reservation);
        }
    }

    /**
     * Redeem all reservations.
     * @param set
     */
    public void redeem(final ReservationSet set){
        for (IReservation r: set) {
            try {
                if (r instanceof IServiceManagerReservation) {
                    redeem((IServiceManagerReservation)r);
                }else {
                    logger.warn("Reservation #" + r.getReservationID().toHashString() + " cannot be redeemed");
                }
            } catch (TestException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Could not redeem for #" + r.getReservationID().toHashString(), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void ticket(final IClientReservation reservation) throws Exception {
        if (!recovered) {
            ticketing.add(reservation);
        }else {
            wrapper.ticket(reservation, this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void ticket(final ReservationSet set){
        for (IReservation r: set) {
            try {
                if (r instanceof IClientReservation) {
                    ticket((IClientReservation)r);
                }else {
                    logger.warn("Reservation #" + r.getReservationID() + " cannot be ticketed");
                }
            } catch (Exception e) {
                logger.error("Could not ticket for #" + r.getReservationID().toHashString());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void tickHandler() throws Exception {
        /* close expiring reservations */
        closeExpiring();
        /* issue redeem/extend lease requests */
        processRedeeming();
        /* issue ticket/extend ticket requests */
        bid();
    }

    /**
     * {@inheritDoc}
     */
    public void updateLease(final IReservation r, final UpdateData udd, final AuthToken caller)
            throws Exception {
        /*
         * drop any messages if the actor is still recovering or has been
         * stopped
         */
        if (!isRecovered() || isStopped()) {
            throw new Exception("This actor cannot receive calls");
        }

        /* handle the update */
        wrapper.updateLease(r, udd, caller);
    }

    /**
     * {@inheritDoc}
     */
    public void updateTicket(final IReservation reservation, final UpdateData udd,
            final AuthToken caller) throws Exception {
        /*
         * drop any messages if the actor is still recovering or has been
         * stopped
         */
        if (!isRecovered() || isStopped()) {
            throw new Exception("This actor cannot receive calls");
        }

        /* handle the update */
        wrapper.updateTicket(reservation, udd, caller);
    }

    public String getManagementObjectClass() {
        return ServiceManagerManagementObject.class.getName();
    }
    
    @Override
    protected void issueDelayed() {
        super.issueDelayed();
        
        ticket(ticketing);
        ticketing.clear();
        
        extendTicket(extendingTicket);
        extendingTicket.clear();
        
        redeem(redeeming);
        redeeming.clear();
        
        extendLease(extendingLease);
        extendingLease.clear();
    }
}
