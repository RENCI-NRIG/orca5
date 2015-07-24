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
import java.util.Properties;
import java.util.Vector;

import orca.manage.OrcaConstants;
import orca.manage.internal.AuthorityManagementObject;
import orca.security.AuthToken;
import orca.shirako.api.IAuthority;
import orca.shirako.api.IAuthorityPolicy;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientCallbackProxy;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServerPolicy;
import orca.shirako.api.IServiceManagerCallbackProxy;
import orca.shirako.api.ISlice;
import orca.shirako.common.ReservationID;
import orca.shirako.kernel.BrokerReservationFactory;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.plugins.db.ClientDatabase;
import orca.shirako.time.ActorClock;
import orca.shirako.time.Term;
import orca.shirako.util.Client;
import orca.shirako.util.ReservationSet;
import orca.shirako.util.ResourceData;
import orca.shirako.util.UpdateData;
import orca.util.ID;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.PersistenceUtils;

/**
 * <code>Authority</code> is the base implementation for a site authority actor.
 */
public class Authority extends Actor implements IAuthority {
    /**
     * Initialization status.
     */
    @NotPersistent
    private boolean initialized = false;
    /**
     * Reservations to redeem once the actor recovers.
     */
    @NotPersistent
    protected ReservationSet redeeming = new ReservationSet();
    /**
     * Reservations to extendLease for once the actor recovers.
     */
    @NotPersistent
    protected ReservationSet extendingLease = new ReservationSet();
    /**
     * Reservations to modifyLease for once the actor recovers.
     */
    @NotPersistent
    protected ReservationSet modifyingLease = new ReservationSet();
    
    /**
     * Creates a new instance.
     */
    public Authority() {
        type = OrcaConstants.ActorTypeSiteAuthority;
    }

    /**
     * Creates a new authority with the given identity and clock factory.
     * 
     * @param identity
     *            actor identity
     * @param clock
     *            clock factory
     */
    public Authority(final AuthToken identity, final ActorClock clock) {
        super(identity, clock);
        type = OrcaConstants.ActorTypeSiteAuthority;
    }

    /**
     * {@inheritDoc}
     */
    public void registerClientSlice(ISlice slice) throws Exception {
        wrapper.registerSlice(slice);
    }

    /**
     * {@inheritDoc}
     */
    public void available(final ResourceSet resources) throws Exception {
        ((IAuthorityPolicy) policy).available(resources);
    }

    /**
     * {@inheritDoc}
     */
    public void claim(final IReservation reservation, final IClientCallbackProxy callback,
            final AuthToken caller) throws Exception {
        ISlice s = reservation.getSlice();

        if (s != null) {
            s.setBrokerClient();
        }

        wrapper.claimRequest((IBrokerReservation) reservation, caller, callback);
    }

    /**
     * {@inheritDoc}
     */
    public void close(final IReservation reservation, final AuthToken caller) throws Exception {
        if (!isRecovered() || isStopped()) {
            throw new Exception("This actor cannot receive calls");
        }

        wrapper.closeRequest(reservation, caller, true);
    }

    /**
     * Closes the expiring reservations for the specified cycle.
     * 
     * @param cycle
     *            cycle number
     */
    protected void closeExpiring(final long cycle) {
        /*
         * Close down expired reservations. Call the policy plugin to give us
         * the list of reservations to close.
         */
        ReservationSet expired = policy.getClosing(cycle);

        if (expired != null) {
            if (logger.isInfoEnabled() && (expired.size() > 0)) {
                logger.info("Authority expiring for cycle " + cycle + " = " + expired);
            }

            close(expired);
        }
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
    public void donate(final ResourceSet resources) throws Exception {
        ((IAuthorityPolicy) policy).donate(resources);
    }

    /**
     * {@inheritDoc}
     */
    public void eject(final ResourceSet resources) throws Exception {
        ((IAuthorityPolicy) policy).eject(resources);
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
    public ReservationID export(final ResourceSet resources, final Term term,
            final AuthToken authToken) throws Exception {
        // FIXME: what if this slice already exists?

        /*
         * Note: the exported resources will be placed in a slice with name
         * equal to authToke.getName(). Assumption: if this slice already exists
         * it is marked as AgentClient.
         */
        ISlice s = SliceFactory.getInstance().create(authToken.getName(), new ResourceData());
        s.setOwner(authToken);
        s.setBrokerClient();

        IBrokerReservation r = BrokerReservationFactory.getInstance().create(resources, term, s);
        r.setOwner(identity);
        wrapper.export(r, authToken);

        ReservationID exported = r.getReservationID();

        return exported;
    }

    public void extendLease(final ReservationSet set){
        for (IReservation r: set) {
            try {
                extendLease((IAuthorityReservation)r);
            } catch (Exception e) {
                logger.error("Could not redeem for #" + r.getReservationID().toHashString(), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void extendLease(final IAuthorityReservation reservation) throws Exception {
        if (!recovered) {
            extendingLease.add(reservation);
        } else {
            wrapper.extendLeaseRequest(reservation, reservation.getClientAuthToken(), false);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void extendLease(final IReservation reservation, final AuthToken caller)
            throws Exception {
        if (!isRecovered() || isStopped()) {
            throw new Exception("This actor cannot receive calls");
        }

        wrapper.extendLeaseRequest((IAuthorityReservation) reservation, caller, true);
    }

    /**
     * {@inheritDoc}
     */
    public void modifyLease(final IAuthorityReservation reservation) throws Exception {
        if (!recovered) {
            modifyingLease.add(reservation);
        } else {
            wrapper.modifyLeaseRequest(reservation, reservation.getClientAuthToken(), false);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void modifyLease(final IReservation reservation, final AuthToken caller)
            throws Exception {
    	
        if (!isRecovered() || isStopped()) {
            throw new Exception("This actor cannot receive calls");
        }

        wrapper.modifyLeaseRequest((IAuthorityReservation) reservation, caller, true);
    }

    
    /**
     * {@inheritDoc}
     */
    public void extendTicket(final IReservation reservation, final AuthToken caller)
            throws Exception {
        ISlice s = reservation.getSlice();

        if (s != null) {
            s.setBrokerClient();
        }

        wrapper.extendTicketRequest((IBrokerReservation) reservation, caller, true);
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
    public void freed(final ResourceSet resources) throws Exception {
        ((IAuthorityPolicy) policy).freed(resources);
    }

    /**
     * {@inheritDoc}
     */
    public void redeem(final IAuthorityReservation reservation) throws Exception {
        if (!recovered) {
            redeeming.add(reservation);
        } else {
            wrapper.redeemRequest(reservation,
                    reservation.getClientAuthToken(),
                    (IServiceManagerCallbackProxy) reservation.getCallback(),
                    false);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void redeem(final IReservation reservation, final IServiceManagerCallbackProxy callback,
            final AuthToken caller) throws Exception {
        if (!isRecovered() || isStopped()) {
            throw new Exception("This actor cannot receive calls");
        }

        // we no longer need this hook. move the validation to
        // validateIncomingTicket
        if (spi.validateIncoming(reservation, caller)) {
            wrapper.redeemRequest((IAuthorityReservation) reservation, caller, callback, true);
        } else {
            /*
             * aydan, 11/03/05 We need to add some code to handle invalid redeem
             * requests (e.g. oversubscribed tickets). Seems that the decision
             * how to handle invalid redeems may be a policy plugin. For now we
             * simply log the error. We do not notify the caller.
             */
            logger.error("the redeem request is invalid");
        }
    }
    
    /**
     * Redeem all reservations.
     * @param set
     */
    public void redeem(final ReservationSet set){
        for (IReservation r: set) {
            try {
                if (r instanceof IAuthorityReservation) {
                    redeem((IAuthorityReservation)r);
                }else {
                    logger.warn("Reservation #" + r.getReservationID().toHashString() + " cannot be redeemed");
                }
            } catch (Exception e) {
                logger.error("Could not redeem for #" + r.getReservationID().toHashString(), e);
            }
        }
    }
    

    /**
     * {@inheritDoc}
     */
    public void ticket(final IReservation reservation, final IClientCallbackProxy callback,
            final AuthToken caller) throws Exception {
        ISlice s = reservation.getSlice();

        if (s != null) {
            s.setBrokerClient();
        }

        wrapper.ticketRequest((IBrokerReservation) reservation, caller, callback, true);
    }

    protected void tickHandler() throws Exception {
        /* close expired reservations */
        closeExpiring(currentCycle);
        /* process all requests for the current cycle */
        ((IAuthorityPolicy) policy).assign(currentCycle);
    }

    /**
     * {@inheritDoc}
     */
    public int unavailable(final ResourceSet resources) throws Exception {
        return ((IAuthorityPolicy) policy).unavailable(resources);
    }

    /**
     * {@inheritDoc}
     */
    public void updateTicket(final IReservation reservation, final UpdateData udd,
            final AuthToken caller) throws Exception {
        throw new RuntimeException("not implemented");
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
            // FIXME: use getGuid() instead of getName()
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

    @Override
    protected void issueDelayed() {
        super.issueDelayed();
        redeem(redeeming);
        redeeming.clear();
        extendLease(extendingLease);
        extendingLease.clear();
    }

    public String getManagementObjectClass() {
        return AuthorityManagementObject.class.getName();
    }
}