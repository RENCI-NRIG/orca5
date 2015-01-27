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

import java.util.Properties;

import orca.shirako.api.IBroker;
import orca.shirako.api.IBrokerPolicy;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServerReservation;
import orca.shirako.common.ConfigurationException;
import orca.shirako.common.delegation.ResourceDelegation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.common.delegation.TicketException;
import orca.shirako.common.meta.QueryProperties;
import orca.shirako.common.meta.ResourcePoolDescriptor;
import orca.shirako.common.meta.ResourcePoolsDescriptor;
import orca.shirako.kernel.ReservationStates;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;
import orca.shirako.util.Bids;
import orca.shirako.util.ReservationSet;
import orca.shirako.util.ResourceData;
import orca.util.ID;
import orca.util.OrcaException;
import orca.util.PropList;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistent;

/**
 * Base class for all broker policy implementations.
 */
public class BrokerPolicy extends Policy implements IBrokerPolicy, QueryProperties {
    /**
     * If true, every allocated ticket will require administrative approval
     * before being sent back.
     */
    @Persistent(key = "PolicyRequireApproval")
    protected boolean requireApproval = false;
    /**
     * A list of reservations that require administrative approval. Note: this
     * field is private so that derived classes are forced to use the locked
     * methods to operate on it.
     */
    @NotPersistent
    protected ReservationSet forApproval;
    /**
     * Initialization status.
     */
    @NotPersistent
    private boolean initialized = false;

    /**
     * Creates a new instance.
     */
    public BrokerPolicy() {
    }

    /**
     * Creates a new instance.
     * 
     * @param actor
     *            actor the policy belongs to
     */
    public BrokerPolicy(final IBroker actor) {
        super(actor);
    }

    /**
     * Adds the reservation to the approval list.
     * 
     * @param reservation
     *            reservation to add
     */
    protected void addForApproval(final IBrokerReservation reservation) {
        synchronized (forApproval) {
            forApproval.add(reservation);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void allocate(final long cycle) throws Exception {
    }

    /**
     * Approve a reservation. To be used by policies that require administrative
     * intervention. Override to provide your own approval policy.
     * 
     * @param reservation
     *            reservation to approve
     */
    public void approve(final IBrokerReservation reservation) {
    }

    /**
     * {@inheritDoc}
     */
    public boolean bind(final IBrokerReservation reservation) throws Exception {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void demand(final IClientReservation reservation) {
    }

    /**
     * {@inheritDoc}
     */
    public void donate(final IClientReservation reservation) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public boolean extend(final IBrokerReservation reservation) throws Exception {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Bids formulateBids(final long cycle) {
        return null;
    }

    /**
     * Returns a list of all reservations that require administrative approval.
     * 
     * @return set of reservations that need approval
     */
    public ReservationSet getForApproval() {
        ReservationSet result = null;

        synchronized (forApproval) {
            result = (ReservationSet) forApproval.clone();
        }

        return result;
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
     * Indicates that the passed reservation should not be approved. To be used
     * by policies that require administrative approval. Override to provide
     * your own policy for dealing with reservations that are not approved.
     * 
     * @param reservation
     *            reservation that the administrator rejected
     */
    public void notApprove(final IBrokerReservation reservation) {
        /*
         * For now the policy is to fail this reservation. Clients can retry
         * their request.
         */

        /*
         * Remove from the approval list. Release resources bound to this
         * reservation. Fail the reservation (to trigger a message to the user).
         * There may be another way do send the update (e.g., generateUpdate())
         */
        removeForApproval(reservation);
        releaseNotApproved(reservation);
        reservation.failWarn("Rejected by administrator");
        reservation.setBidPending(false);
    }

    /**
     * Releases the resources for a reservation that was rejected by the
     * administrator.
     * 
     * @param reservation
     *            reservation
     */
    protected void releaseNotApproved(final IBrokerReservation reservation) {
    }

    /**
     * Removes the specified reservation from the approval list.
     * 
     * @param reservation
     *            reservation to remove
     */
    protected void removeForApproval(final IBrokerReservation reservation) {
        synchronized (forApproval) {
            forApproval.remove(reservation);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revisit(final IReservation reservation) throws Exception {
        if (reservation instanceof IClientReservation) {
            if (reservation.getState() == ReservationStates.Ticketed) {
                donate((IClientReservation) reservation);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void ticketSatisfies(final ResourceSet requestedResources,
            final ResourceSet actualResources, final Term requestedTerm, final Term actualTerm)
            throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void updateTicketComplete(final IClientReservation reservation) throws Exception {
        donate(reservation);
    }

    /**
     * Creates a new resource set using the source and the specified delegation.
     * 
     * @param source
     * @param delegation
     * @return
     * @throws TicketException
     */
    public ResourceSet extract(ResourceSet source, ResourceDelegation delegation)
            throws TicketException {
        // make a resource set to wrap around
        ResourceData rd = new ResourceData();
        PropList.mergeProperties(source.getResourceProperties(), rd.getResourceProperties());
        ResourceSet extracted = new ResourceSet(delegation.getUnits(),
                delegation.getResourceType(),
                rd);

        // obtain the source resource ticket
        ResourceTicket sourceTicket = ((Ticket) source.getResources()).getTicket();

        // make a new ResourceTicket using the delegation and the source ticket
        ResourceTicket newTicket = actor.getShirakoPlugin()
                .getTicketFactory()
                .makeTicket(sourceTicket, delegation);

        // make a concrete set to wrap the resource ticket
        Ticket cset = new Ticket((Ticket) source.getResources(), newTicket);
        // store the resource ticket
        extracted.setResources(cset);

        return extracted;
    }

    /**
     * Returns the client id.
     * 
     * @param reservation
     * @return
     */
    public ID getClientID(IServerReservation reservation) {
        return reservation.getClientAuthToken().getGuid();
    }

    public static ResourcePoolsDescriptor getResourcePools(Properties response)
            throws ConfigurationException {
        ResourcePoolsDescriptor result = new ResourcePoolsDescriptor();
        try {
            int count = PropList.getIntegerProperty(response, PoolsCount);
            for (int i = 0; i < count; i++) {
                ResourcePoolDescriptor rd = new ResourcePoolDescriptor();
                rd.reset(response, PoolPrefix + i + ".");
                result.add(rd);
            }
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Could not decode resource pools", e);
        }
        return result;
    }

    public static String getQueryAction(final Properties properties) {
        if (properties == null) {
            return null;
        }
        return properties.getProperty(QueryAction);
    }

    public static Properties getResourcePoolsQuery() {
        Properties p = new Properties();
        p.setProperty(QueryAction, QueryActionDisctoverPools);
        return p;
    }
}