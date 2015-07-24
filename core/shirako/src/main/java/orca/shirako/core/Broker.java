/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.core;

import java.security.cert.Certificate;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import orca.manage.OrcaConstants;
import orca.manage.internal.BrokerManagementObject;
import orca.security.AuthToken;
import orca.shirako.api.IBroker;
import orca.shirako.api.IBrokerPolicy;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientCallbackProxy;
import orca.shirako.api.IClientPolicy;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServerPolicy;
import orca.shirako.api.ISlice;
import orca.shirako.common.ReservationID;
import orca.shirako.kernel.BrokerReservationFactory;
import orca.shirako.kernel.ClientReservationFactory;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.plugins.db.ClientDatabase;
import orca.shirako.registry.PeerRegistry;
import orca.shirako.time.ActorClock;
import orca.shirako.time.Term;
import orca.shirako.util.Bids;
import orca.shirako.util.Client;
import orca.shirako.util.ReservationSet;
import orca.shirako.util.ResourceData;
import orca.shirako.util.UpdateData;
import orca.util.ID;
import orca.util.OrcaException;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.PersistenceUtils;

/**
 * <code>Broker</code> offers the base for all broker actors.
 */
public class Broker extends Actor implements IBroker {

    /**
     * Recovered reservations that need to obtain tickets (both server and client roles).
     */
    @NotPersistent
    protected ReservationSet ticketing = new ReservationSet();

    /**
     * Recovered reservations that need to extend tickets (both server and client roles).
     */
    @NotPersistent
    protected ReservationSet extending = new ReservationSet();

    /**
     * The peer registry.
     */
    @NotPersistent
    protected PeerRegistry registry;

    /**
     * Initialization status.
     */
    @NotPersistent
    private boolean initialized = false;

    /**
     * Creates a new instance.
     */
    public Broker() {
        registry = new PeerRegistry();
        type = OrcaConstants.ActorTypeBroker;
    }

    /**
     * Creates a new broker with the given identity and term factory.
     * 
     * @param identity
     *            broker identity
     * @param clock
     *            term factory
     */
    public Broker(final AuthToken identity, final ActorClock clock) {
        super(identity, clock);
        registry = new PeerRegistry();
        type = OrcaConstants.ActorTypeBroker;
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
     * {@inheritDoc}
     */
    public void registerClientSlice(ISlice slice) throws Exception {
        wrapper.registerSlice(slice);
    }

    /**
     * Bids for resources as dictated by the bidding policy for the current
     * cycle.
     * 
     * @param cycle
     *            cycle
     * 
     * @throws Exception
     */
    protected void bid(final long cycle) throws Exception {
        /*
         * NOTE: bid is identical to ServiceManager.bid, as it should be. All
         * differences should be in the formulateBids plugin policy. If you find
         * yourself modifying this routine in ways that diverge from
         * SlottedSM.bid, then check again to make sure you know what you are
         * doing.
         */

        // Invoke policy module to select candidates for ticket and extend.
        // Note that candidates structure is discarded when we're done.
        Bids candidates = null;

        candidates = ((IClientPolicy) policy).formulateBids(cycle);

        if (candidates != null) {
            /*
             * Send new ticket requests. Do not add them to the holdings list as
             * the upstream agent may modify the start and end time we
             * requested.
             */
            Iterator<IReservation> iter = candidates.getTicketing().iterator();

            while (iter.hasNext()) {
                IClientReservation r = (IClientReservation) iter.next();

                try {
                    wrapper.ticket(r, this);
                } catch (Exception e) {
                    logger.error("unexpected ticket failure for #"
                            + r.getReservationID().toHashString(), e);
                }
            }

            /*
             * Issue extends for the renewal candidates. Remove from expiring.
             * Do not add to holdings.
             */
            iter = candidates.getExtending().iterator();

            while (iter.hasNext()) {
                IClientReservation r = (IClientReservation) iter.next();

                try {
                    wrapper.extendTicket(r);
                } catch (Exception e) {
                    logger.error("unexpected extend failure for #"
                            + r.getReservationID().toHashString(), e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void claim(final IReservation reservation, final IClientCallbackProxy callback,
            final AuthToken caller) throws Exception {
        if (!isRecovered() || isStopped()) {
            throw new Exception("This actor cannot receive calls");
        }

        wrapper.claimRequest((IBrokerReservation) reservation, caller, callback);
    }

    /**
     * {@inheritDoc}
     */
    public IClientReservation claim(final ReservationID reservationID, final ResourceSet resources)
            throws Exception {
        return claim(reservationID, resources, getDefaultBroker());
    }

    /**
     * {@inheritDoc}
     */
    public IClientReservation claim(final ReservationID reservationID, final ResourceSet resources,
            final IBrokerProxy broker) throws Exception {
        ISlice slice = getDefaultSlice();

        if (slice == null) {
            slice = SliceFactory.getInstance().create(identity.getName());
            slice.setOwner(identity);
            slice.setInventory(true);
        }

        return claim(reservationID, resources, slice, broker);
    }

    /**
     * {@inheritDoc}
     */
    public IClientReservation claim(final ReservationID reservationID, final ResourceSet resources,
            final ISlice slice, final IBrokerProxy broker) throws Exception {
        Term term = new Term(clock.cycleStartDate(0));

        IClientReservation r = ClientReservationFactory.getInstance().create(reservationID,
                resources,
                term,
                slice,
                broker);
        r.setExported(true);

        wrapper.ticket(r, this);
        // NOTE: claim is an asynchronous operation
        return r;
    }

    /**
     * Closes all expiring reservations.
     * 
     * @param cycle
     *            cycle
     */
    protected void closeExpiring(final long cycle) {
        /*
         * Close down expired reservations. Call the policy plugin to give us
         * the list of reservations to close.
         */
        ReservationSet expired = policy.getClosing(cycle);
        if (expired != null) {
            if (logger.isInfoEnabled() && (expired.size() > 0)) {
                logger.info("Broker expiring for cycle " + cycle + " = " + expired);
            }

            close(expired);
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
    public void donate(final IClientReservation reservation) throws Exception {
        ((IServerPolicy) policy).donate(reservation);
    }

    /**
     * {@inheritDoc}
     */
    public void export(final IBrokerReservation reservation, final AuthToken client)
            throws Exception {
        wrapper.export(reservation, client);
    }

    /**
     * {@inheritDoc}
     */
    public ReservationID export(final ResourceSet resources, final Term term, final AuthToken client)
            throws Exception {
        ISlice s = SliceFactory.getInstance().create(client.getName(), new ResourceData());
        s.setClient();

        IBrokerReservation r = BrokerReservationFactory.getInstance().create(resources, term, s);
        r.setOwner(identity);

        wrapper.export(r, client);

        return r.getReservationID();
    }

    public void extendTicket(final ReservationSet set){
        for (IReservation r: set) {
            try {
                if (r instanceof IBrokerReservation) {
                    extendTicket((IBrokerReservation)r);
                }else if (r instanceof IClientReservation) {
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
    public void extendTicket(final IBrokerReservation reservation) throws Exception {
        if (!recovered) {
            extending.add(reservation);
        } else {
            wrapper.extendTicketRequest(reservation, reservation.getClientAuthToken(), false);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void extendTicket(final IClientReservation reservation) throws Exception {
        if (!recovered) {
            extending.add(reservation);
        } else {
            wrapper.extendTicket(reservation);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void extendTicket(final IReservation reservation, final AuthToken caller)
            throws Exception {
        if (!isRecovered() || isStopped()) {
            throw new Exception("This actor cannot receive calls");
        }

        wrapper.extendTicketRequest((IBrokerReservation) reservation, caller, true);
    }

    /**
     * {@inheritDoc}
     */
    public IBrokerProxy getBroker(final ID guid) {
        return registry.getBroker(guid);
    }

    /**
     * {@inheritDoc}
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
    public ISlice getDefaultSlice() throws Exception {
        ISlice[] slc = getInventorySlices();

        if ((slc != null) && (slc.length > 0)) {
            return slc[0];
        }

        return null;
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

            initialized = true;
        }
    }
    
    @Override
    protected void issueDelayed() {
        super.issueDelayed();
        
        extendTicket(extending);
        extending.clear();
        
        ticket(ticketing);
        ticketing.clear();
    }


    public void ticket(final ReservationSet set){
        for (IReservation r: set) {
            try {
                if (r instanceof IBrokerReservation) {
                    ticket((IBrokerReservation)r);
                }else if (r instanceof IClientReservation) {
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
    public void ticket(final IBrokerReservation reservation) throws Exception {
        if (!recovered) {
            ticketing.add(reservation);
        } else {
            /* process as usual but disable sequence number validation */
            wrapper.ticketRequest(reservation,
                    reservation.getClientAuthToken(),
                    (IClientCallbackProxy) reservation.getCallback(),
                    false);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void ticket(final IClientReservation reservation) throws Exception {
        if (!recovered) {
            ticketing.add(reservation);
        } else {
            wrapper.ticket(reservation, this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void ticket(final IReservation reservation, final IClientCallbackProxy callback,
            final AuthToken caller) throws Exception {
        if (!isRecovered() || isStopped()) {
            throw new Exception("This actor cannot receive calls");
        }

        /* process as usual: must validate sequence numbers */
        wrapper.ticketRequest((IBrokerReservation) reservation, caller, callback, true);
    }

    public void relinquish(final IReservation reservation, final AuthToken caller) throws Exception {
        if (!isRecovered() || isStopped()) {
            throw new Exception("This actor cannot receive calls");
        }
        wrapper.relinquishRequest((IBrokerReservation) reservation, caller);
    }

    /**
     * {@inheritDoc}
     */
    protected void tickHandler() throws Exception {
        /* allocate all requests for this cycle */
        ((IBrokerPolicy) policy).allocate(currentCycle);

        /* request new resources if necessary */
        bid(currentCycle);
        // close all expired reservations
        closeExpiring(currentCycle);
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

    public void registerClient(final Client client, Certificate certificate) throws Exception {
        assert client != null;
        assert certificate != null;

        ClientDatabase db = (ClientDatabase) spi.getDatabase();

        Vector<Properties> temp = null;
        try {
            db.getClient(client.getGuid());
        } catch (Exception e) {
            throw new Exception("Failed to check if client is present in the database", e);
        }

        if (temp != null && temp.size() > 0) {
            throw new Exception("Client with guid: " + client.getGuid() + " is already registered");
        }

        try {
            db.addClient(client);
        } catch (Exception e) {
            throw new Exception("Failed to add client to the database", e);
        }

        try {
            spi.getKeyStore().addTrustedCertificate(client.getGuid().toString(), certificate);
        } catch (Exception e) {
            logger.error("failed to register client certificate", e);
            try {
                db.removeClient(client.getGuid());
            } catch (Exception ee) {
                logger.error("Failed to undo client addition", ee);
            }
        }
    }

    public void unregisterClient(final ID guid) throws Exception {
        assert guid != null;

        ClientDatabase db = (ClientDatabase) spi.getDatabase();
        Vector<Properties> v = null;
        try {
            v = db.getClient(guid);
        } catch (Exception e) {
            throw new Exception("Failed to obtain client record from the database", e);
        }

        if (v == null || v.size() == 0) {
            throw new Exception("Client " + guid.toString() + " is not registerd");
        }

        Client c = new Client();
        try {
            PersistenceUtils.restore(c, v.get(0));
        } catch (Exception e) {
            throw new Exception("Failed to expand client record", e);
        }

        try {
            db.removeClient(guid);
        } catch (Exception e) {
            throw new Exception("Failed to remove client record from the database", e);
        }

        try {
            // FIXME: use getGuid() instead of getName()
            spi.getKeyStore().removeTrustedCertificate(c.getName());
        } catch (Exception e) {
            throw new Exception("Failed to unregister client certificate", e);
        }
    }

    public Client getClient(final ID guid) throws Exception {
        assert guid != null;

        ClientDatabase db = (ClientDatabase) spi.getDatabase();
        Vector<Properties> v = null;
        try {
            v = db.getClient(guid);
        } catch (Exception e) {
            throw new Exception("Failed to obtain client record from the database", e);
        }

        if (v == null || v.size() == 0) {
            return null;
        }

        Client c = new Client();
        try {
            PersistenceUtils.restore(c, v.get(0));
        } catch (Exception e) {
            throw new Exception("Failed to expand client record", e);
        }

        return c;
    }

    public String getManagementObjectClass() {
        return BrokerManagementObject.class.getName();
    }
    
    public void modify(final ReservationID reservationID, final Properties modifyProps) throws Exception{
    	// no-op
    }
    
}
