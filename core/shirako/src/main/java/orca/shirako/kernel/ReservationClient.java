/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.kernel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import orca.security.AuthToken;
import orca.security.Guard;
import orca.shirako.api.IActor;
import orca.shirako.api.IAuthorityPolicy;
import orca.shirako.api.IAuthorityProxy;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.ICallbackProxy;
import orca.shirako.api.IClientActor;
import orca.shirako.api.IClientCallbackProxy;
import orca.shirako.api.IClientPolicy;
import orca.shirako.api.IConcreteSet;
import orca.shirako.api.IPolicy;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.IServiceManagerPolicy;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.common.ReservationID;
import orca.shirako.core.ServiceManager;
import orca.shirako.time.Term;
import orca.shirako.util.RPCError;
import orca.shirako.util.ReservationState;
import orca.shirako.util.ResourceCount;
import orca.shirako.util.TestException;
import orca.shirako.util.UpdateData;
import orca.util.OrcaException;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistent;
import orca.util.persistence.RecoverParent;

import org.apache.log4j.Logger;

/**
 * Reservation state machine for a client-side reservation. Role: service
 * manager, or an agent requesting tickets from an upstream agent. This class
 * includes support for client-side handling of leases as well as tickets; lease
 * handling is relevant only to the service manager.
 */

/*
 * Implementation note on terms. One complication in ReservationClient is that
 * acquiring or renewing a lease is a two-step process (first the ticket, then
 * the lease), thus there are some intermediate states and corner cases, and
 * multiple terms to keep track of. Problem: we could get confused if we try to
 * extend a ticket before the redeem() or extendLease() for the previously
 * awarded ticket completes. So we do not allow it: if the ticket term is
 * shorter than the time to redeem it, the reservation is forced to expire.
 */

/*
 * Implementation note: When we receive a new lease, new resources may require
 * some join processing. The current approach is to enter an (Active, None)
 * state immediately. Resources are presumed to automatically enter service
 * (e.g., by joining a collective) as they join: if any subset of the resources
 * could be active, then the ReservationClient is considered active. The
 * joinstate tracks joining for the reservation's first lease only, just so that
 * we can sequence/time the join and/or fail if all resources fail to prime. The
 * primary purpose of joinstate is to implement reservation groups with
 * sequenced priming or joining.
 */

// NOTE: term shows the current term: it is updated when a ticket or a lease is
// received -> previousTerm
// ticketTerm shows the current ticket term: it is updated only when a ticket
// update is received -> previousTicketTerm , resources
// leaseTerm shows the current lease term: it is updated only when a lease
// update is received -> previousLeaseTerm, leased resources
// previousResources is not used for a reservationclient

class ReservationClient extends Reservation implements IKernelClientReservation,
        IKernelServiceManagerReservation {
    public static final String PropertySuggestedResources = "ReservationClientSuggestedResources";
    public static final String PropertySuggestedTerm = "ReservationClientTermSuggested";
    public static final String PropertyBroker = "ReservationClientBroker";
    public static final String PropertyAuthority = "ReservationClientAuthority";
    public static final String PropertyTicketUpdate = "ReservationClientLastTicketUpdate";
    public static final String PropertyLeaseUpdate = "ReservationClientLastLeaseUpdate";
    public static final String PropertyJoinPredecessor = "ReservationClientJoinPredecessor";
    public static final String PropertyClientCallback = "ReservationClientCallback";
    public static final String PropertySequenceNumberTicketIn = "ReservationClientSequenceTicketIn";
    public static final String PropertySequenceNumberTicketOut = "ReservationClientSequenceTicketOut";
    public static final String PropertySequenceNumberLeaseIn = "ReservationClientSequenceLeaseIn";
    public static final String PropertySequenceNumberLeaseOut = "ReservationClientSequenceLeaseOut";
    public static final String PropertyTicketTerm = "ReservationClientTicketTerm";
    public static final String PropertyPreviousTicketTerm = "ReservationClientPreviousTicketTerm";
    public static final String PropertyPreviousLeasedTerm = "ReservationClientPreviousLeasedTerm";
    public static final String PropertyRenewTime = "ReservationClientRenewTime";
    public static final String PropertyExported = "ReservationClientExported";
    public static final String PropertyJoinFilter = "CodPredecessorReservationJoinFilter";

    public static final String PropertyRedeemPredecessorsCount = "redeempred.count";
    public static final String PropertyRedeemPredecessorReservation = "redeempred.rid.";
    public static final String PropertyRedeemPredecessorFilter = "redeempred.filter.";

    public static final String PropertyJoinPredecessorsCount = "joinmpred.count";
    public static final String PropertyJoinPredecessorReservation = "joinpred.rid.";
    public static final String PropertyJoinPredecessorFilter = "joinpred.filter.";

    public static final String PropertyRelinquieshed = "ReservationClientRelinquished";
    public static final String PropertyClosedDuringRedeem = "ReservationClientClosedDuringRedeem";

    /*
     * Members requiring serialization.
     */

    /**
     * Proxy to the broker that serves tickets for this reservation.
     */
    @Persistent(key = PropertyBroker)
    protected IBrokerProxy broker;

    /**
     * Proxy to the site authority that serves leases for this reservation.
     */
    @Persistent(key = PropertyAuthority)
    protected IAuthorityProxy authority;

    /**
     * Sequence number for incoming updateTicket messages.
     */
    @Persistent(key = PropertySequenceNumberTicketIn)
    protected int sequenceTicketIn = 0;

    /**
     * Sequence number for outgoing ticket/extend ticket messages. Increases
     * with every new message.
     */
    @Persistent(key = PropertySequenceNumberTicketOut)
    protected int sequenceTicketOut = 0;

    /**
     * Sequence number for incoming updateLease messages.
     */
    @Persistent(key = PropertySequenceNumberLeaseIn)
    protected int sequenceLeaseIn = 0;

    /**
     * Sequence number for outgoing redeem/extend lease messages. Increases with
     * every new message.
     */
    @Persistent(key = PropertySequenceNumberLeaseOut)
    protected int sequenceLeaseOut = 0;

    /**
     * Does this reservation represent resources exported by a broker?
     */
    @Persistent(key = PropertyExported)
    protected boolean exported = false;

    /**
     * The most recent granted term for a ticket. If the reservation has
     * obtained/extended a ticket but has not yet redeemed or extended its
     * lease, this field will have the same value as term. However, once the
     * site sends the update lease message, term may change and may no longer
     * equal ticketTerm. Use ticketTerm if you want to make sure that you refer
     * to the term reflected in the latest ticket.
     */
    @Persistent(key = PropertyTicketTerm)
    protected Term ticketTerm;

    /**
     * The previous ticket term. Some policy decisions, e.g., updating internal
     * calendar structures, may require access to the previous ticket term.
     */
    @Persistent(key = PropertyPreviousTicketTerm)
    protected Term previousTicketTerm;

    /**
     * The most recent granted term for a lease. Similarly to ticketTerm, term
     * equals lease term after the reservation has completed a redeem or extend
     * lease operation, but before it has extended its ticket. If you require
     * access to the latest lease term use this field.
     */
    @Persistent(key = PropertyTermLeased)
    protected Term leaseTerm;

    /**
     * The previous lease term.
     */
    @Persistent(key = PropertyPreviousLeasedTerm)
    protected Term previousLeaseTerm;

    /**
     * The leased resources. Will be null if no resource have yet been leased.
     */
    @Persistent(key = PropertyLeasedResources)
    protected ResourceSet leasedResources;

    /**
     * The most recently recommended term for new requests/extensions for this
     * reservation. This field will be set by the programmer/controllers to pass
     * information to the resource policy. The policy must examine this field
     * and decide what to do. Once a decision is made, the term chosen by the
     * policy will be in approvedTerm.
     */
    @Persistent(key = PropertySuggestedTerm)
    protected Term suggestedTerm;

    /**
     * The most recently recommended resources for new requests/extensions for
     * this reservation. This field will be set by the programmer/controller to
     * pass information to the resource policy. The policy must examine this
     * field and decide what to do. Once a decision is made, the resources
     * chosen by the policy will be in approvedResources.
     */
    @Persistent(key = PropertySuggestedResources)
    protected ResourceSet suggestedResources;

    /**
     * On the service manager, ReservationClient has an additional joinstate
     * variable to track and sequence join/redeem operations. Reservations may
     * be "blocked" from redeeming or joining the guest (i.e.,
     * configuration/post-install) until their "predecessor" reservations have
     * completed. There is at most one predecessor for joining and another for
     * redeeming: these may be the same, or either may be specified without the
     * other.
     */
    @Persistent(key = IReservation.PropertyStateJoined)
    protected int joinstate;

    /**
     * Join predecessors for this reservation (service manager only).
     */
    @Persistent
    protected HashMap<ReservationID, PredecessorState> joinPredecessors = new HashMap<ReservationID, PredecessorState>();

    /**
     * Redeem predecessors for this reservation (service manager only)
     */
    @Persistent
    protected HashMap<ReservationID, PredecessorState> redeemPredecessors = new HashMap<ReservationID, PredecessorState>();

    /**
     * The status of the last ticket update.
     */
    @Persistent(key = PropertyTicketUpdate)
    protected UpdateData lastTicketUpdate;

    /**
     * The status of the last lease update.
     */
    @Persistent(key = PropertyLeaseUpdate)
    protected UpdateData lastLeaseUpdate;

    /**
     * The cycle in which we have to issue ticket update request for this
     * reservation. The value is a cache of the the response we received from
     * the broker. This field is needed primarily for recovery: especially in
     * the case when the broker had also failed.
     */
    @Persistent(key = PropertyRenewTime)
    protected long renewTime = 0;

    /**
     * Callback object for callbacks on operations issued from this class.
     */
    @Persistent(key = PropertyClientCallback)
    protected IClientCallbackProxy callback;

    /**
     * Relinquish status.
     */
    @Persistent(key = PropertyRelinquieshed)
    protected boolean relinquished = false;

    /**
     * Set to true if a close is received while redeem is in progress.
     */
    @Persistent(key = PropertyClosedDuringRedeem)
    private boolean closedDuringRedeem = false;

    /**
     * The policy in control of this reservation. Some reservation operations
     * require interacting with the policy.
     */
    @Persistent(reference = true)
    protected IPolicy policy;

    /*
     * Members that do not have to be serialized.
     */

    /*
     * releasedResources should not be serialized: when we rebuild a reservation
     * we rebuild its leasedResources set to include only active resources.
     * Since on recovery we assume all resources are free and then mark only the
     * used resources, releasedResources should not be serialized.
     */

    /**
     * Resources ejected from leasedResources (e.g., due to failure) are held
     * here pending retrieval and/or processing by a service (unlocked) method.
     */
    @NotPersistent
    protected ResourceSet releasedResources;

    /**
     * True if the programmer has set new suggestedTerm or suggestedResources
     * since the last policy decision. This field is cleared when we receive an
     * updateTicket.
     */
    @NotPersistent
    protected boolean suggested = true;

    /**
     * Creates a new empty reservation. Generates a new reservation identifier.
     */
    public ReservationClient() {
        this(new ReservationID(), null, null, null, null);
    }

    /**
     * Creates a new instance using the specified reservation identifier.
     * 
     * @param rid
     *            reservation identifier
     */
    public ReservationClient(final ReservationID rid) {
        this(rid, null, null, null, null);
    }

    /**
     * Creates a new reservation instance.
     * 
     * @param rid
     *            reservation identifier
     * @param resources
     *            resource set
     * @param term
     *            term
     * @param slice
     *            containing slice
     */
    public ReservationClient(final ReservationID rid, final ResourceSet resources, final Term term,
            final IKernelSlice slice) {
        this(rid, resources, term, slice, null);
    }

    /**
     * Creates a new reservation instance.
     * 
     * @param rid
     *            reservation identifier
     * @param resources
     *            resource set
     * @param term
     *            term
     * @param slice
     *            containing slice
     * @param broker
     *            broker
     */
    public ReservationClient(final ReservationID rid, final ResourceSet resources, final Term term,
            final IKernelSlice slice, IBrokerProxy broker) {
        super(rid, resources, term, slice);

        this.broker = broker;
        this.servicePending = ReservationStates.None;
        this.lastTicketUpdate = new UpdateData();
        this.lastLeaseUpdate = new UpdateData();
        this.renewable = false;
        this.joinstate = ReservationStates.NoJoin;
        this.renewable = false;
        this.suggestedResources = resources;
        this.suggestedTerm = term;
        this.suggested = true;
        this.approvedResources = resources;
        this.approvedTerm = term;
        this.approved = true;
        this.category = CategoryClient;
    }

    /**
     * Creates a new reservation instance. Generates a new reservation
     * identifier.
     * 
     * @param resources
     *            desired resources
     * @param term
     *            desired term
     * @param slice
     *            containing slice
     */
    public ReservationClient(final ResourceSet resources, final Term term, final IKernelSlice slice) {
        this(new ReservationID(), resources, term, slice, null);
    }

    /**
     * Creates a new reservation instance. Generates a new reservation
     * identifier.
     * 
     * @param resources
     *            desired resources
     * @param term
     *            desired term
     * @param slice
     *            containing slice
     * @param broker
     *            broker to use
     */
    public ReservationClient(final ResourceSet resources, final Term term,
            final IKernelSlice slice, final IBrokerProxy broker) {
        this(new ReservationID(), resources, term, slice, broker);
    }

    /**
     * Absorbs and incoming lease update.
     * 
     * @param incoming
     *            incoming update
     * @param udd
     *            update data
     * @throws Exception
     */
    protected void absorbLeaseUpdate(final IReservation incoming, final UpdateData udd)
            throws Exception {
        /*
         * If this is the first update create a new resource set.
         */
        if (leasedResources == null) {
            leasedResources = resources.abstractClone();
        }

        if ((state == ReservationStates.CloseWait)
                && (incoming.getResources().getConcreteUnits() != 0)) {
            /*
             * We are waiting for a FIN, and this is not it. Minor hack: do not
             * incorporate changes into the resource set, since there may be new
             * resources and we do not want to process the joins. Just keep
             * waiting for the FIN. Essentially rejects the update without
             * transition to Failed.
             */
            udd.postError("reservation is closing, rejected lease is non-empty");
            logWarning("non-empty lease update received in CloseWait: waiting for FIN");
        } else {
            // absorb the incoming resources
            leasedResources.update(this, incoming.getResources());
        }

        /*
         * Remember the current term and lease term and absorb the incoming
         * ones.
         */
        previousTerm = term;
        previousLeaseTerm = leaseTerm;

        term = incoming.getTerm();
        leaseTerm = term;
    }

    /**
     * Absorbs an incoming ticket update.
     * 
     * @param incoming
     *            incoming ticket update
     * @param udd
     *            update data
     * @throws Exception
     */
    protected void absorbTicketUpdate(final IReservation incoming, final UpdateData udd)
            throws Exception {
        /*
         * Record the site authority.
         */
        IAuthorityProxy siteAuthority = incoming.getResources().getSiteProxy();

        if (authority == null) {
            authority = siteAuthority;
        }
        assert authority.getName().equalsIgnoreCase(siteAuthority.getName());

        /*
         * Remember the current term and ticket term and absorb the incoming
         * term.
         */
        previousTerm = term;
        previousTicketTerm = ticketTerm;

        ticketTerm = incoming.getTerm();
        term = ticketTerm;

        // absorb the update
        resources.update(this, incoming.getResources());

        if (logger.isDebugEnabled()) {
            logger.debug("AbsorbUpdate: " + incoming.toLogString() + " " + toString());
        }

        // inform the policy that this reservation has a new/updated ticket
        ((IClientPolicy) policy).updateTicketComplete(this);
    }

    /**
     * Determines whether the incoming lease update is acceptable and if so
     * accepts it.
     * 
     * @param incoming
     *            incoming lease update
     * @param udd
     *            update data
     * @return true if the update was successful
     */
    protected boolean acceptLeaseUpdate(final IReservation incoming, final UpdateData udd) {
        // XXX: should we absorb this. What if decide not to accept the update?
        lastLeaseUpdate.absorb(udd);

        /*
         * XXX: Policy: if this lease update fails, then transition to Failed.
         * Alternative: could transition to (state, None) to allow retry of the
         * redeem/extend by a higher level.
         */
        if (udd.failed()) {
            transition("failed lease update", ReservationStates.Failed, ReservationStates.None);
        } else {
            try {
                leaseUpdateSatisfies(incoming, udd);
                absorbLeaseUpdate(incoming, udd);
            } catch (Exception e) {
                transition("rejected lease update",
                        ReservationStates.Failed,
                        ReservationStates.None);
                udd.postError(e.toString());
                logger.error("acceptLeaseUpdate", e);
            }
        }

        return (udd.successful());
    }

    /**
     * Determines whether the incoming ticket update is acceptable and if so
     * accepts it.
     * 
     * @param incoming
     *            incoming ticket update
     * @param udd
     *            update data
     * @return true if the update was successful
     */
    protected boolean acceptTicketUpdate(final IReservation incoming, final UpdateData udd) {
        // XXX: should we absorb this. What if decide not to accept the update?
        lastTicketUpdate.absorb(udd);

        boolean success = true;

        if (udd.failed()) {
            success = false;
        } else {
            try {
                ticketUpdateSatisfies(incoming, udd);
                absorbTicketUpdate(incoming, udd);
            } catch (Exception e) {
                success = false;
                udd.error(e.toString());
                logError(e.toString(), e);
            }
        }

        if (!success) {
            if (state == ReservationStates.Nascent) {
                transition("failed ticket reserve",
                        ReservationStates.Failed,
                        ReservationStates.None);
            } else {
                transition("failed ticket update", state, ReservationStates.None);
            }
        }

        return success;
    }

    /**
     * Join predicate: invoked internally to determine if reservation
     * post-install actions can take place. This gives subclasses an opportunity
     * sequence post-install configuration actions.
     * <p>
     * If false, the reservation enters a "BlockedJoin" sub-state until a
     * subsequent approveJoin returns true. When true, the reservation can
     * manipulate the current reservation's property lists and attributes to
     * facilitate configuration. Note that approveJoin may be invoked multiple
     * times, and should be idempotent.
     * </p>
     * 
     * @return DOCUMENT ME!
     */
    protected boolean approveJoin() throws Exception {
        boolean approved = true;

        for (PredecessorState p : joinPredecessors.values()) {
            if (p.getReservation().isFailed() || p.getReservation().isClosed()) {
                logger.error("join predecessor reservation is in a terminal state. ignoring it: "
                        + p.getReservation());
                continue;
            }

            if (!p.getReservation().isActiveJoined()) {
                approved = false;
                break;
            }
        }

        if (approved) {
            prepareJoin();
        }

        return approved;
    }

    /**
     * Redeem predicate: invoked internally to determine if the reservation
     * should be redeemed. This gives subclasses an opportunity sequence install
     * configuration actions at the authority side.
     * <p>
     * If false, the reservation enters a "BlockedRedeem" sub-state until a
     * subsequent approveRedeem returns true. When true, the reservation can
     * manipulate the current reservation's properly lists and attributes to
     * facilitate configuration. Note that approveRedeem may be polled multiple
     * times, and should be idempotent.
     * </p>
     * 
     * @return DOCUMENT ME!
     */
    protected boolean approveRedeem() throws Exception {
        boolean approved = true;

        for (PredecessorState p : redeemPredecessors.values()) {
            if (p.getReservation().isFailed() || p.getReservation().isClosed()) {
                logger.error("redeem predecessor reservation is in a terminal state. ignoring it: "
                        + p.getReservation());
                continue;
            }
            // FIXME: the incoming resources are not applied to the reservation
            // until the
            // reservation transitions into the Joining state. We must use
            // isActiveJoined to make sure
            // that prepareRedeem is going to see the units inside the
            // predecessor reservation.
            if (!p.getReservation().isActiveJoined()) {
                approved = false;
                break;
            }
        }

        if (approved) {
            prepareRedeem();
        }

        return approved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canRedeem() {
        if (((state == ReservationStates.ActiveTicketed) || (state == ReservationStates.Ticketed) || ((state == ReservationStates.Active) && pendingRecover))
                && (pending == ReservationStates.None)) {
            assert resources != null;

            IConcreteSet c = resources.getResources();
            assert (c != null) && (c.getUnits() > 0);

            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * The reservation cannot be renewed if a previous renew attempt failed, or
     * if the reservation is terminal (closed, failed, closing), or if a renew
     * is currently in progress, or if the reservation has not yet been
     * ticketed.
     * </p>
     */
    @Override
    public boolean canRenew() {
        if (!renewable) {
            return false;
        }

        if (lastTicketUpdate == null) {
            return false;
        }

        if (isTerminal()) {
            return false;
        }

        if (isExtendingTicket()) {
            return false;
        }

        /*
         * For now we cannot renew if a previous ticket is still redeeming.
         */
        if (isExtendingLease() || isRedeeming()) {
            return false;
        }

        return lastTicketUpdate.successful();
    }

    /**
     * {@inheritDoc}
     */
    protected void clearNotice() {
        lastTicketUpdate.clear();
        lastLeaseUpdate.clear();
    }

    protected void doRelinquish() {
        if (!relinquished) {
            relinquished = true;
            // tell the policy this reservation is now closed
            try {
            	if (policy != null)
            		policy.closed(this);
            	else
            		logger.warn("doRelinquish(): policy not set in reservation " + rid + ", unable to call policy.closed(), continuing");
            } catch (Exception e) {
                logger.error("close with policy", e);
            }
            if (getRequestedResources() != null) {
                try {
                    sequenceTicketOut++;
                    RPCManager.relinquish(this);
                } catch (Exception e) {
                    logRemoteError("broker reports relinquish error: " + e.toString(), e);
                }
            } else {
                logger.info("Reservation #" + rid.toHashString()
                        + "  has not requested any resource yet. Nothing to relinquish.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        switch (state) {
        case ReservationStates.Nascent:
        case ReservationStates.Failed:
            transition("close", ReservationStates.Closed, pending);
            if (broker != null) {
                // tell the broker we do not need the resources anymore
                // note: we do not have a confirmation from the site, so the
                // broker
                // cannot validate our relinquish operation (e.g., the SM may
                // try to cheat)
                doRelinquish();
            }
            break;
        case ReservationStates.Ticketed:
            if (pending != ReservationStates.Redeeming) {
                transition("close", ReservationStates.Closed, pending);
                // tell the broker we do not need the resources anymore
                doRelinquish();
            } else {
                // the redeem is in progress.
                // delay the close until the redeem comes back
                logger.info("Received close for a redeeming reservation. Deferring close until redeem completes.");
                closedDuringRedeem = true;
            }
            break;
        case ReservationStates.Active:
        case ReservationStates.ActiveTicketed:
            if (pending == ReservationStates.Redeeming) {
                logger.info("Received close for a redeeming reservation. Deferring close until redeem completes.");
                closedDuringRedeem = true;
            } else {
                switch (joinstate) {
                case ReservationStates.BlockedJoin:
                    // no join operations have taken place, so no need for local
                    // leave operations
                    transition("close", ReservationStates.CloseWait, ReservationStates.None);

                    try {
                        sequenceLeaseOut++;
                        RPCManager.close(this);
                    } catch (Exception e) {
                        logRemoteError("authority reports close error: " + e.toString(), e);
                        // do not keep this reservation closed forever
                        transition("close", ReservationStates.Closed, ReservationStates.None);
                        // Note: the broker does not have information to ensure
                        // we are not cheating
                        doRelinquish();
                    }

                    break;

                default:
                    transition("close",
                            ReservationStates.Active,
                            ReservationStates.Closing,
                            ReservationStates.NoJoin);

                    break;
                }
            }
            break;

        case ReservationStates.Closed:
        case ReservationStates.CloseWait:
            break;
        }
    }

    /**
     * Counts the number of active and pending units in the reservation at the
     * given time instance.
     * 
     * @param when
     *            time instance
     * @return counter of active and pending units
     */
    protected CountHelper count(Date when) {
        CountHelper result = new CountHelper();

        if (isTerminal()) {
            // no units to report
        } else {
            if ((term != null) && term.contains(when) && (resources != null)) {
                result.active = resources.getConcreteUnits(when); // ticketed
                result.type = resources.type;
            } else {
                /*
                 * If approved == true, we have an outstanding ticket request
                 * that we are either about the send or are waiting for the
                 * reply.
                 */
                if (approved && (approvedTerm != null) && approvedTerm.contains(when)
                        && (approvedResources != null)) {
                    result.pending = approvedResources.units;
                    result.type = approvedResources.type;
                }
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void count(final ResourceCount rc, final Date when) {
        switch (state) {
        case ReservationStates.Nascent:
        case ReservationStates.Active:
        case ReservationStates.ActiveTicketed:
        case ReservationStates.Ticketed:

            CountHelper c = count(when);

            if (c.type != null) {
                rc.tallyActive(c.type, c.active);
                rc.tallyPending(c.type, c.pending);
            }

            break;

        case ReservationStates.Closed:
        case ReservationStates.CloseWait:

            if (resources != null) {
                rc.tallyClose(resources.type, resources.units);
            }

            break;

        case ReservationStates.Failed:

            if (resources != null) {
                rc.tallyFailed(resources.type, resources.units);
            }

            break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void extendLease() throws Exception {
        /*
         * Not permitted if there is a pending operation.
         */
        nothingPending();
        // validateRedeem();
        requestedTerm.enforceExtendsTerm(leaseTerm);

        switch (state) {
        case ReservationStates.ActiveTicketed:
            transition("extend lease",
                    ReservationStates.ActiveTicketed,
                    ReservationStates.ExtendingLease);
            sequenceLeaseOut++;

            RPCManager.extendLease(authority, this, slice.getOwner());

            break;

        default:
            error("Wrong state to initiate extend lease: " + ReservationStates.states[state]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifyLease() throws Exception {
        
    	/*
         * Not permitted if there is a pending operation.
         */
        nothingPending();
        // validateRedeem();

        switch (state) {
        case ReservationStates.Active:
            
            transition("modify lease",
                    ReservationStates.Active,
                    ReservationStates.ModifyingLease);
            sequenceLeaseOut++;

            RPCManager.modifyLease(authority, this, slice.getOwner());

            break;

        default:
            error("Wrong state to initiate modify lease: " + ReservationStates.states[state]);
        }
        
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void extendTicket(final IActor actor) throws Exception {
        /*
         * Not permitted if there is a pending operation: cannot renew while a
         * previous renew or redeem is in progress (see note above).
         */
        nothingPending();
        assert broker != null;

        approvedTerm.enforceExtendsTerm(term);
        requestedTerm = approvedTerm;
        requestedResources = approvedResources;

        // if (!renewable) {
        // error("reservation cannot be extended");
        // }
        switch (state) {
        case ReservationStates.Ticketed:

            if (!isServiceManager(actor)) {
                transition("extend ticket",
                        ReservationStates.Ticketed,
                        ReservationStates.ExtendingTicket);
            } else {
                throw new Exception("Cannot extend ticket while in Ticketed");
            }

            break;

        case ReservationStates.Active:
            transition("extend ticket", ReservationStates.Active, ReservationStates.ExtendingTicket);

            break;

        default:
            error("Wrong state to initiate extend ticket: " + ReservationStates.states[state]);
        }

        // mapper.uncommit(this);
        sequenceTicketOut++;
        
        RPCManager.extendTicket(this);
    }

    /**
     * {@inheritDoc}
     */
    public IAuthorityProxy getAuthority() {
        return authority;
    }

    /**
     * {@inheritDoc}
     */
    public IBrokerProxy getBroker() {
        return broker;
    }

    /**
     * {@inheritDoc}
     */
    public int getJoinState() {
        return joinstate;
    }

    /**
     * {@inheritDoc}
     */
    public String getJoinStateName() {
        return ReservationStates.getJoiningName(joinstate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLeasedAbstractUnits() {
        if (leasedResources != null) {
            return leasedResources.getUnits();
        } else {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public ResourceSet getLeasedResources() {
        return leasedResources;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLeasedUnits() {
        if (leasedResources != null) {
            IConcreteSet cs = leasedResources.getResources();

            if (cs != null) {
                return cs.getUnits();
            }
        }

        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getLeaseSequenceIn() {
        return sequenceLeaseIn;
    }

    /**
     * {@inheritDoc}
     */
    public int getLeaseSequenceOut() {
        return sequenceLeaseOut;
    }

    /**
     * {@inheritDoc}
     */
    public Term getLeaseTerm() {
        return leaseTerm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNotices() {
        String s = super.getNotices();
        String notices = getUpdateNotices();

        if (notices != null) {
            s = s + "\n" + notices;
        }

        return s;
    }

    /**
     * {@inheritDoc}
     */
    public Term getPreviousLeaseTerm() {
        return previousLeaseTerm;
    }

    /**
     * {@inheritDoc}
     */
    public Term getPreviousTicketTerm() {
        return previousTicketTerm;
    }

    /**
     * {@inheritDoc}
     */
    public long getRenewTime() {
        return renewTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReservationState getReservationState() {
        return new ReservationState(this.state, this.pending, this.joinstate);
    }

    /**
     * {@inheritDoc}
     */
    public ResourceSet getSuggestedResources() {
        return suggestedResources;
    }

    /**
     * {@inheritDoc}
     */
    public Term getSuggestedTerm() {
        return suggestedTerm;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceType getSuggestedType() {
        if (suggestedResources != null) {
            return suggestedResources.type;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public int getTicketSequenceIn() {
        return sequenceTicketIn;
    }

    /**
     * {@inheritDoc}
     */
    public int getTicketSequenceOut() {
        return sequenceTicketOut;
    }

    /**
     * {@inheritDoc}
     */
    public Term getTicketTerm() {
        return ticketTerm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceType getType() {
        // XXX: i think we should stick to the base method here
        if (resources != null) {
            return resources.type;
        } else if (requestedResources != null) {
            return requestedResources.type;
        } else if (approvedResources != null) {
            return approvedResources.getType();
        } else if (suggestedResources != null) {
            return suggestedResources.type;
        } else {
            return null;
        }
    }

    /**
     * Returns a string describing the reservation status.
     * 
     * @return status string
     */
    public String getUpdateNotices() {
        StringBuffer sb = new StringBuffer();

        if (lastTicketUpdate != null) {
            if (lastTicketUpdate.getMessage() != null) {
                sb.append("\nLast ticket update: " + lastTicketUpdate.getMessage());
            }
            String ev = lastTicketUpdate.getEvents();

            if (ev != null) {
                sb.append("\n\nTicket events\n" + ev);
            }
        }

        if (lastLeaseUpdate != null) {
            if (lastLeaseUpdate.getMessage() != null) {
                sb.append("\nLast lease update: " + lastLeaseUpdate.getMessage());
            }
            String ev = lastLeaseUpdate.getEvents();

            if (ev != null) {
                sb.append("\n\nLease events:\n" + ev);
            }
        }

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        return (((state == ReservationStates.Active) || (state == ReservationStates.ActiveTicketed)) && (joinstate == ReservationStates.NoJoin));
    }

    /**
     * {@inheritDoc}
     */
    public boolean isActiveJoined() {
        return isActive() && (joinstate == ReservationStates.NoJoin);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExported() {
        return this.exported;
    }

    /**
     * Checks if the passed actor is a service manager.
     * 
     * @param actor
     *            actor to check
     * @return true if the actor is a service manager
     */
    protected boolean isServiceManager(final IActor actor) {
        return (actor instanceof ServiceManager);
    }

    /**
     * Enforces minimum standards for lease updates. We aren't picky because it
     * could be a close confirmation, or an unsolicited update could reduce our
     * allocation, e.g., due to failures.
     * 
     * @param incoming
     *            incoming lease update
     * @param udd
     *            update data
     * @throws Exception
     */
    protected void leaseUpdateSatisfies(final IReservation incoming, final UpdateData udd)
            throws Exception {
        try {
            /*
             * Call the policy to determine if it is willing to accept the lease
             * update.
             */
            ((IServiceManagerPolicy) policy).leaseSatisfies(resources,
                    incoming.getResources(),
                    term,
                    incoming.getTerm());

            if (isActiveTicketed()) {
                /*
                 * If we are extending, the new start time should not be shifted
                 */
                assert incoming.getTerm().getNewStartTime().getTime() == term.getNewStartTime()
                        .getTime();
            }
        } catch (Exception e) {
            logWarning("lease update does not satisfy ticket term (ignored)");
            udd.post("lease update does not satisfy ticket term (ignored)");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void prepare(final ICallbackProxy callback, final Logger logger) {
        setLogger(logger);
        this.callback = (IClientCallbackProxy) callback;

        if (guard == null) {
            guard = new Guard();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void prepareJoin() throws Exception {
        Properties config = resources.getLocalProperties();
        for (PredecessorState p : joinPredecessors.values()) {
            p.setProperties(config);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareProbe() throws Exception {
        if (leasedResources != null) {
            if (joinstate != ReservationStates.BlockedJoin) {
                leasedResources.prepareProbe();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void prepareRedeem() throws Exception {
        Properties config = resources.getConfigurationProperties();
        for (PredecessorState p : redeemPredecessors.values()) {
            p.setProperties(config);
        }
    }

    /**
     * Called from a probe to monitor asynchronous processing related to the
     * joinstate for service manager.
     * 
     * @throws Exception
     *             passed through from prepareJoin or prepareRedeem
     */
    protected void probeJoinState() throws Exception {
        // validate the main reservation state
        switch (state) {
        case ReservationStates.Nascent:
        case ReservationStates.Closed:
        case ReservationStates.Failed:
            transition("clearing join state for terminal reservation",
                    state,
                    pending,
                    ReservationStates.NoJoin);
            return;
        }

        switch (joinstate) {
        case ReservationStates.BlockedRedeem:

            /*
             * this reservation has a ticket to redeem, and the redeem is
             * blocked for a predecessor: see if we can get it going now.
             */
            assert state == ReservationStates.Ticketed;

            if (approveRedeem()) {
                transition("unblocked redeem",
                        ReservationStates.Ticketed,
                        ReservationStates.Redeeming,
                        ReservationStates.NoJoin);
                // try {
                sequenceLeaseOut++;
                /*
                 * If redeem fails we should not fail the reservation!!! The
                 * failure may be due to the authority being unavailable
                 */
                RPCManager.redeem(this);

                // } catch (Exception e) {
                // remoteError("unblocked redeem");
                // transition("unblocked redeem failed", Failed, None);
                // fail("unblocked redeem failed");
                // }
            }

            break;

        case ReservationStates.BlockedJoin:

            /*
             * This reservation has a lease whose join processing was blocked
             * for a predecessor: see if we can get it going now. Note: if
             * pendingRecover is true the reservation cannot be unblocked, since
             * it may not actually have its leased resources. If state is
             * ActiveTicketed we will also not unblock, because we may be
             * recovering a reservation in Active, ExtendingTicket, BlockedJoin.
             * For reservations in this state the pendingRecover flag will be
             * cleared by the updateTicket message. Since the reservation does
             * not actually complete recovery until the lease comes back from
             * the site, unblocking the reservation will result in an error.
             */
            if (!pendingRecover && (state != ReservationStates.ActiveTicketed)) {
                if (approveJoin()) {
                    transition("unblocked join", state, pending, ReservationStates.Joining);
                    servicePending = ReservationStates.Joining;
                }
            }

            break;

        case ReservationStates.Joining:

            /*
             * Tracking initial join processing for first lease on a service
             * manager. The reservation is already "active", but we log
             * completion of the join here and Fail if it failed completely.
             */

            /*
             * Recovery note: if pendingRecover is true it is dangerous to allow
             * a transition to NoJoin, since the local lease may be different
             * from the lease at the site. If we fail during recovery, it is
             * possible to end up in Active, None, NoJoin and we will not be
             * able to tell if the lease is good or not. For the same reason,
             * when we are in ActiveTicketed we should not allow a transition to
             * NoJoin: pendingRecover may have already been cleared by an
             * updateTicket message but we still must get the lease from the
             * site.
             */
            if (servicePending == ReservationStates.None && leasedResources.isActive()
                    && !pendingRecover && (state != ReservationStates.ActiveTicketed)) {
                /* join completed: go ahead and transition to NoJoin */
                transition("join complete", state, pending, ReservationStates.NoJoin);

                if (leasedResources.getConcreteUnits() == 0) {
                    // logWarning("resources failed to join");
                    // transition("resources failed to join", Failed,
                    // pending);
                    if (leasedResources.getNotices().getNotice() != null) {
                        fail("resources failed to join: "
                                + leasedResources.getNotices().getNotice());
                    } else {
                        fail("resources failed to join: (no details)");
                    }
                }

                // if (LoggingTool.logTime()) {
                // logger.time("SMTransferInNodesEnd" +
                // System.currentTimeMillis() + " " + slice.getName() +
                // ":" + this.getRID() + " " + this.getState() + ":" +
                // this.getPending());
                // }
            }

            break;

        case ReservationStates.NoJoin:
            break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void probePending() throws Exception {
        /*
         * Process join state to complete or restart join-related operations for
         * service manager.
         */
        if (joinstate != ReservationStates.NoJoin) {
            probeJoinState();
        }

        if (leasedResources == null) {
            return;
        }

        // if (servicePending != None) {
        // logError("service overrun in probePending (skipped tick)");
        // return;
        // }

        /*
         * Handling for close completion. Note that this reservation could
         * "stick" once we enter the CloseWait state, if we never hear back from
         * the authority. There is no harm to purging a CloseWait reservation,
         * but we just leave them for now.
         */
        if ((pending == ReservationStates.Closing)) {
            logger.debug("RESERVATION IS CLOSING");

            if ((leasedResources.isClosed())) {
                logger.debug("LEASED RESOURCES are closed");

                transition("local close complete",
                        ReservationStates.CloseWait,
                        ReservationStates.None);

                try {
                    sequenceLeaseOut++;
                    RPCManager.close(this);
                } catch (Exception e) {
                    logRemoteError("authority reports close error: " + e.toString(), e);
                    /*
                     * If the authority is unreachable or rejects the request,
                     * then purge it. This is useful because the authority may
                     * close first and reject this request, which could lead to
                     * large numbers of stuck CloseWaits hanging around if we
                     * don't complete close here. But if the authority is merely
                     * unreachable, it might be better to retry.
                     */
                    transition("close complete", ReservationStates.Closed, ReservationStates.None);
                    // Note: the broker does not have information to ensure we
                    // are not cheating
                    doRelinquish();
                }
            }
        }
    }

    public void setPolicy(IClientPolicy policy) {
        this.policy = policy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reserve(final IPolicy policy) throws Exception {
        assert slice != null;

        nothingPending();
        this.policy = policy;

        switch (state) {
        case ReservationStates.Nascent:

            /*
             * We are a broker or service manager initiating a new ticket
             * request to an upstream agent.
             */
            assert broker != null;

            /* remember what we are going to request */
            requestedTerm = approvedTerm;
            requestedResources = approvedResources;

            /* make the transition */
            transition("ticket", ReservationStates.Nascent, ReservationStates.Ticketing);

            if (exported) {
                /*
                 * We are a broker or service manager initiating a ticket claim
                 * to an upstream broker for a pre-reserved ticket
                 * ("will call"). Note: no policy prepareReserve, because the
                 * ticket has already been exported (e.g., any payment has
                 * already been tendered).
                 */
                sequenceTicketOut++;
                RPCManager.claim(this);
            } else {
                /*
                 * This is a regular request for new resources to an upstream
                 * broker.
                 */
                sequenceTicketOut++;
                RPCManager.ticket(this);
            }

            break;

        case ReservationStates.Ticketed:

            if (exported) {
                /*
                 * For now we do not support exports of leases from an
                 * authority. So ending here, means that we are trying to
                 * re-claim an already claimed reservation. Throw an error for
                 * now.
                 */
                throw new Exception("Invalid state for claim. Did you already claim this reservation?");
            }

            /*
             * We are a service manager redeeming a ticket for a lease from the
             * site authority.
             */

            // validateRedeem();

            transition("redeem blocked",
                    ReservationStates.Ticketed,
                    pending,
                    ReservationStates.BlockedRedeem);

            break;

        case ReservationStates.Active:
            // error("initiating reserve on active reservation");
            // transition("redeem", Ticketed, Redeeming);
            sequenceLeaseOut++;
            RPCManager.redeem(this);

            break;

        case ReservationStates.ActiveTicketed:
            /*
             * If the service manager requests to redeem an extended ticket,
             * then present it to the authority as an extendLease rather than a
             * redeem. ExtendLease is equivalent to redeem on client side, and
             * it may be inconvenient for the service manager to distinguish
             * between them. In this case the requested term was left in
             * this.term by the extendTicket (absorbTicketUpdate).
             */
            extendLease();

            break;

        case ReservationStates.Closed:
        case ReservationStates.CloseWait:
        case ReservationStates.Failed:
            error("initiating reserve on defunct reservation");
        }
    }

    @Override
    public void setup() {
        super.setup();
        if (leasedResources != null) {
            leasedResources.setup(this);
        }
        if (suggestedResources != null) {
            suggestedResources.setup(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serviceClose() {
        // note: no close for the ticket, only close for the leased resources
        if (leasedResources != null) {
            leasedResources.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serviceProbe() throws Exception {
        /*
         * An exception in one of these service routines should mean some
         * unrecoverable, reservation-wide failure. It should not occur, e.g.,
         * if some subset of the resources fail.
         */
        try {
            switch (servicePending) {
            case ReservationStates.Joining:

                /*
                 * The reservation state may have changed by the time we reach
                 * here (e.g., Closing/Closed). However, even if we check here,
                 * there is no guarantee that the update initiated by
                 * leasedResources.serviceUpdate will be applied before a
                 * potential close request (We are not holding locks here, and
                 * we should not). So the concrete resource implementation must
                 * ensure that it will not honor the update if the reservation
                 * state has changed.
                 */

                assert leasedResources != null;
                leasedResources.serviceUpdate(this);
                break;
            }
        } catch (Exception e) {
            logException("SM failed servicing probe", e);
        }

        servicePending = ReservationStates.None;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serviceUpdateLease() throws Exception {
        if (leasedResources != null) {
            if (lastLeaseUpdate.successful()) {
                /*
                 * An update() was called above, so we must clear it. Update()
                 * must be called in every success case, and must not be called
                 * in any failure case. But: if the reservation is in
                 * BlockedJoin, then leave the update unserviced until a future
                 * probePending.
                 */
                if (joinstate != ReservationStates.BlockedJoin) {
                    leasedResources.serviceUpdate(this);
                    setDirty();
                }

                /*
                 * If subsequent lease updates come in (e.g., for an extend)
                 * before we have cleared the initial one, then
                 * rset.serviceUpdate should now do the right thing.
                 */
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serviceUpdateTicket() throws Exception {
        if (lastTicketUpdate.successful()) {
            resources.serviceUpdate(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBroker(final IBrokerProxy broker) throws Exception {
        if (state != ReservationStates.Nascent) {
            error("setBroker on reservation while in use");
        }

        this.broker = broker;
    }

    /**
     * {@inheritDoc}
     */
    public void setExported(final boolean exported) {
        this.exported = exported;
    }

    /**
     * {@inheritDoc}
     */
    public void setLeaseSequenceIn(final int sequence) {
        this.sequenceLeaseIn = sequence;
    }

    /**
     * {@inheritDoc}
     */
    public void setLeaseSequenceOut(final int sequence) {
        this.sequenceLeaseOut = sequence;
    }

    /**
     * Sets the policy in control of the reservation.
     * 
     * @param policy
     *            policy
     */
    protected void setPolicy(final IPolicy policy) {
        this.policy = policy;
    }

    /**
     * {@inheritDoc}
     */
    public void setRedeemPredecessor(final IServiceManagerReservation predecessor) {
        addRedeemPredecessor(predecessor);
    }

    /**
     * {@inheritDoc}
     */
    public void setRenewable(final boolean renewable) {
        this.renewable = renewable;
    }

    /**
     * {@inheritDoc}
     */
    public boolean getRenewable() {
    	return this.renewable;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setRenewTime(final long time) {
        this.renewTime = time;
    }

    /**
     * {@inheritDoc}
     */
    public void setSuggested() {
        this.suggested = true;
    }

    /**
     * {@inheritDoc}
     */
    public void setSuggested(final Term term, final ResourceSet rset) {
        this.suggestedResources = rset;
        this.suggestedTerm = term;
        this.suggested = true;
    }

    /**
     * {@inheritDoc}
     */
    public void setSuggestedResources(final ResourceSet rset) {
        this.suggestedResources = rset;
    }

    /**
     * {@inheritDoc}
     */
    public void setSuggestedTerm(final Term term) {
        this.suggestedTerm = term;
    }

    /**
     * {@inheritDoc}
     */
    public void setTicketSequenceIn(final int sequence) {
        this.sequenceTicketIn = sequence;
    }

    /**
     * {@inheritDoc}
     */
    public void setTicketSequenceOut(final int sequence) {
        this.sequenceTicketOut = sequence;
    }

    /**
     * Enforce minimum standards for an arriving ticket update.
     * 
     * @param incoming
     *            incoming ticket update
     * @param udd
     *            update data
     * @throws Exception
     *             thrown if resources do not satisfy request
     * @throws Exception
     *             thrown if term does not satisfy request
     */
    protected void ticketUpdateSatisfies(final IReservation incoming, final UpdateData udd)
            throws Exception {
        try {
            /*
             * Call the policy to determine if we can apply the incoming update.
             */
            ((IClientPolicy) policy).ticketSatisfies(requestedResources,
                    incoming.getResources(),
                    requestedTerm,
                    incoming.getTerm());

            /*
             * If the policy was careless about the term, make sure that the
             * incoming term extends the current one.
             */
            if (pending == ReservationStates.ExtendingTicket) {
                incoming.getTerm().enforceExtendsTerm(term);
            }
        } catch (Exception e) {
            error("incoming ticket does not satisfy our request: " + e);
        }
    }

    /**
     * Transitions this reservation into a new state. Uses
     * Reservation.transition, but also attends to joinstate.
     * 
     * @param prefix
     *            string to log for this transition
     * @param state
     *            the new state
     * @param pending
     *            the new pending
     * @param joinstate
     *            the new joinstate
     */
    protected void transition(final String prefix, final int state, final int pending,
            final int joinstate) {
        if (logger.isDebugEnabled()) {
            logger.debug("Reservation #" + rid.toHashString() + " " + prefix
                    + " transition for joinstate: " + ReservationStates.joinstates[this.joinstate]
                    + "->" + ReservationStates.joinstates[joinstate]);
        }

        this.joinstate = joinstate;
        transition(prefix, state, pending);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateLease(final IReservation incoming, final UpdateData udd) throws Exception {
        switch (state) {
        case ReservationStates.Nascent:
            error("Lease update for a reservation without a ticket");

            break;

        case ReservationStates.Ticketed:

            if (pending != ReservationStates.Redeeming) {
                // error("Unsolicited lease update");
                logger.warn("unsolicited lease update: Ticketed/None. Details: " + incoming);

                return;
            }

            if (acceptLeaseUpdate(incoming, udd)) {
                pendingRecover = false;
                // note that r3193 added this check but it does not work as
                // reservations always go into Fail
                // if (leasedResources.getConcreteUnits() == 0) {
                // fail("Resources failed to redeem: lease came back with 0 resources");
                // }else{
                // transition("lease arrival blocked join",
                // ReservationStates.Active, ReservationStates.None,
                // ReservationStates.BlockedJoin);
                // }
                transition("lease arrival blocked join",
                        ReservationStates.Active,
                        ReservationStates.None,
                        ReservationStates.BlockedJoin);
            }

            if (closedDuringRedeem) {
                logger.info("Received updateLease for a reservation closed in the Redeeming state. Issuing close.");
                close();
            }
            break;

        case ReservationStates.Active:
            // XXX; check for closing

            //acceptLeaseUpdate(incoming, udd);
            
            if (acceptLeaseUpdate(incoming, udd)) {
            	if (pending == ReservationStates.ModifyingLease) {
            		if (joinstate == ReservationStates.Joining) {
                        logger.warn("Received LeaseUpdate while in Joining");
                    }

                    transition("modified lease",
                            ReservationStates.Active,
                            ReservationStates.None);
            	}
            	
            }
            
            break;

        case ReservationStates.ActiveTicketed:

            // XXX; check for closing
            if (acceptLeaseUpdate(incoming, udd)) {
                /*
                 * Tricky transition: take this lease as an extension if we
                 * already issued the lease extend request, else accept it as an
                 * unsolicited and stay in ActiveTicketed.
                 */
                if (pending == ReservationStates.ExtendingLease) {
                    if (joinstate == ReservationStates.Joining) {
                        logger.warn("Received LeaseUpdate while in Joining");
                    }

                    transition("extended lease",
                            ReservationStates.Active,
                            ReservationStates.None,
                            ReservationStates.Joining);
                }

                pendingRecover = false;
            }

            if (closedDuringRedeem) {
                logger.info("Received updateLease for a reservation closed in the Redeeming state. Issuing close.");
                close();
            }

            // XXX: what if the update lease indicates that the request has
            // failed on the remote side?
            break;

        case ReservationStates.CloseWait:

            /*
             * This lease update should be a FIN (empty). However, the update
             * may not be empty or may indicate that some error has occurred
             * remotely. We have to be careful not to leave the reservation
             * forever in the CloseWait state, therefore we will ignore the
             * outcome of acceptLeaseUpdate and will transition to closed: we
             * have done everything we could do for this reservation on our
             * side.
             */
            boolean closeWaitTemp = acceptLeaseUpdate(incoming, udd);

            if (!closeWaitTemp) {
                logWarning("incoming lease update is not FIN or indicates a remote error. Transitioning to close nevertheless.");
            }

            pendingRecover = false;

            transition("close complete", ReservationStates.Closed, ReservationStates.None);
            doRelinquish();

            // if (LoggingTool.logTime()) {
            // logger.time("SMTransferOutNodesEnd" +
            // System.currentTimeMillis() + " " + slice.getName() +
            // ":" + this.getRID() + " " + this.getState() + ":" +
            // this.getPending());
            // }
            break;

        case ReservationStates.Closed:
            logError("Lease update on closed reservation");

            break;

        case ReservationStates.Failed:
            logError("Lease update on failed reservation");

            break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateTicket(final IReservation incoming, final UpdateData udd) throws Exception {
        switch (state) {
        case ReservationStates.Nascent:
        case ReservationStates.Ticketed:

            if ((pending != ReservationStates.Ticketing)
                    && (pending != ReservationStates.ExtendingTicket)) {
                // error("Unsolicited ticket update");
                logger.warn("unsolicited ticket update. Ignoring it. Details: " + incoming);

                return;
            }

            if (acceptTicketUpdate(incoming, udd)) {
                transition("ticket update", ReservationStates.Ticketed, ReservationStates.None);
                suggested = false;
                approved = false;
                pendingRecover = false;
            }

            break;

        case ReservationStates.Active:
        case ReservationStates.ActiveTicketed:

            if (pending != ReservationStates.ExtendingTicket) {
                // error("Unsolicited ticket update (extend)");
                logger.warn("unsolicited ticket update. Ignoring it. Details: " + incoming);

                return;
            }

            if (acceptTicketUpdate(incoming, udd)) {
                extended = true;
                transition("ticket update",
                        ReservationStates.ActiveTicketed,
                        ReservationStates.None);
                suggested = false;
                approved = false;
                pendingRecover = false;
            }

            break;

        case ReservationStates.Closed:
        case ReservationStates.CloseWait:
            logWarning("Ticket update after close");
            break;

        case ReservationStates.Failed:
            logError("Ticket update on failed reservation");

            break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateIncoming() throws Exception {
        if (slice == null) {
            error("no slice specified");
        }

        if (resources == null) {
            error("no resource set specified");
        }

        if (term == null) {
            error("no term specified");
        }

        term.validate();
        resources.validateIncoming();
    }

    /**
     * {@inheritDoc}
     */
    public void validateIncomingLease() throws Exception {
        validateIncoming();
    }

    /**
     * {@inheritDoc}
     */
    protected void validateIncomingTicket() throws Exception {
        validateIncoming();
        resources.validateIncomingTicket(term);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: Should be called only when talking to a broker. Use
     * {@link #validateRedeem()} for interactions with a site authority.
     * </p>
     */
    @Override
    public void validateOutgoing() throws Exception {
        if (slice == null) {
            error("no slice specified");
        }

        if (approvedTerm == null) {
            error("approved term is null");
        }

        if (approvedResources == null) {
            error("approved resources is null");
        }

        approvedTerm.validate();
        approvedResources.validateOutgoing();
    }

    /**
     * {@inheritDoc}
     */
    public void validateRedeem() throws Exception {
        if (authority == null) {
            internalError("no authority proxy for redeem");
        }

        if (resources.units == 0) {
            internalError("redeeming a reservation for 0 resources!");
        }

        if (slice == null) {
            error("no slice specified");
        }

        if (term == null) {
            error("term is null");
        }

        if (resources == null) {
            error("requested resources is null");
        }

        term.validate();
        resources.validateOutgoing();
    }

    public void setConfigurationProperty(String key, String value) {
        resources.getConfigurationProperties().setProperty(key, value);
    }

    public void setRequestProperty(String key, String value) {
        resources.getRequestProperties().setProperty(key, value);
    }

    public void setJoinPredecessor(final IServiceManagerReservation predecessor) {
        addJoinPredecessor(predecessor);
    }

    public void setJoinPredecessor(IServiceManagerReservation predecessor, Properties filter) {
        addJoinPredecessor(predecessor, filter);
    }

    public void setRedeemPredecessor(IServiceManagerReservation predecessor, Properties filter) {
        addRedeemPredecessor(predecessor, filter);
    }

    public void addRedeemPredecessor(IServiceManagerReservation r) {
        addRedeemPredecessor(r, null);
    }

    public void addRedeemPredecessor(IServiceManagerReservation r, Properties filter) {
        PredecessorState state = redeemPredecessors.get(r.getReservationID());
        if (state == null) {
            state = new PredecessorState((IKernelServiceManagerReservation) r, filter);
            redeemPredecessors.put(r.getReservationID(), state);
        }
    }

    public void addJoinPredecessor(IServiceManagerReservation r) {
        addJoinPredecessor(r, null);
    }

    public void addJoinPredecessor(IServiceManagerReservation r, Properties filter) {
        PredecessorState state = joinPredecessors.get(r.getReservationID());
        if (state == null) {
            state = new PredecessorState((IKernelServiceManagerReservation) r, filter);
            joinPredecessors.put(r.getReservationID(), state);
        }
    }

    public List<IServiceManagerReservation> getRedeemPredecessors() {
        ArrayList<IServiceManagerReservation> list = new ArrayList<IServiceManagerReservation>(redeemPredecessors.size());
        for (PredecessorState st : redeemPredecessors.values()) {
            list.add(st.getReservation());
        }
        return list;
    }

    public List<IServiceManagerReservation> getJoinPredecessors() {
        ArrayList<IServiceManagerReservation> list = new ArrayList<IServiceManagerReservation>(joinPredecessors.size());
        for (PredecessorState st : joinPredecessors.values()) {
            list.add(st.getReservation());
        }
        return list;
    }

    public IClientCallbackProxy getClientCallbackProxy() {
        return callback;
    }

    public void handleFailedRPC(FailedRPC rpc) {
        // make sure that the failed RPC came from either
        // the broker or the site.
        AuthToken remoteAuth = rpc.getRemoteAuth();
        switch (rpc.getRequestType()) {
        case Claim:
        case Ticket:
        case ExtendTicket:
        case Relinquish:
            if (broker == null || !broker.getIdentity().equals(remoteAuth)) {
                throw new RuntimeException("Unauthorized Failed reservation RPC: expected="
                        + broker.getIdentity() + ", but was: " + remoteAuth);
            }
            break;
        case Redeem:
        case ExtendLease:
        case Close:
            if (authority == null || !authority.getIdentity().equals(remoteAuth)) {
                throw new RuntimeException("Unauthorized Failed reservation RPC: expected="
                        + authority.getIdentity() + ", but was: " + remoteAuth);
            }
            break;
        default:
            throw new RuntimeException("Unexpected FailedRPC for ReservationClient. RequestType="
                    + rpc.getRequestType());
        }

        // If the error is in the network: keep retrying if possible
        if (rpc.getErrorType() == RPCError.NetworkError) {
            // do not retry if failed|closed
            if (isFailed() || isClosed()) {
                return;
            }
            // if the reservation is closing, we want to take care to make sure
            // it is closed eventually
            // if we have no more resources, then transition to closed and
            // relinquish the ticket.
            if (isClosing()) {
                if ((leasedResources == null) || leasedResources.isClosed()) {
                    transition("close complete", ReservationStates.Closed, ReservationStates.None);
                    doRelinquish();
                }
                return;
            }
            // the reservation is still viable. Retry the RPC.
            assert rpc.hasRequest();
            RPCManager.retry(rpc.getRequest());
            return;
        }

        if (rpc.getError().getCause() instanceof TestException) {
            // if this was a forced error do not fail the reservation
            logger.debug("Ignoring RPC failure due to TestException", rpc.getError());
        }else {
            // non-recoverable failure: fail the reservation
            fail("Failing reservation due to non-recoverable RPC error (" + rpc.getErrorType() + ")",
                    rpc.getError());
        }
    }

    private String printState() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(getStateName());
        sb.append(",");
        sb.append(getPendingStateName());
        sb.append(",");
        sb.append(getJoinStateName());
        sb.append("](");
        sb.append(getTicketSequenceOut());
        sb.append("/");
        sb.append(getTicketSequenceIn());
        sb.append(")(");
        sb.append(getLeaseSequenceOut());
        sb.append("/");
        sb.append(getLeaseSequenceIn());
        sb.append(")");
        return sb.toString();
    }

    private void recoverNascent() throws Exception {
        switch (pending) {
        case ReservationStates.None:
            ((IClientActor) actor).ticket(this);
            logger.debug("Issued ticket request for reservation #"
                    + getReservationID().toHashString() + " State=" + printState());
            break;

        case ReservationStates.Ticketing:
            setPendingRecover(true);
            transition("[recovery]", state, ReservationStates.None);
            setTicketSequenceOut(getTicketSequenceOut() - 1);
            ((IClientActor) actor).ticket(this);
            logger.debug("Issued ticket request for reservation #"
                    + getReservationID().toHashString() + " State=" + printState());
            break;
        default:
            throw new OrcaException("Invalid pending state");
        }
    }

    private void recoverTicketed() throws Exception {
        switch (pending) {
        case ReservationStates.None:
            logger.debug("No recovery necessary for reservation #"
                    + getReservationID().toHashString() + " State=" + printState());
            break;

        case ReservationStates.Redeeming:
            setPendingRecover(true);
            transition("[recovery]", state, ReservationStates.None);
            setLeaseSequenceOut(getLeaseSequenceOut() - 1);
            ((IServiceManager) actor).redeem(this);
            logger.debug("Issued redeem request for reservation #"
                    + getReservationID().toHashString() + " State=" + printState());
            break;

        case ReservationStates.ExtendingTicket:
            setPendingRecover(true);
            transition("[recovery]", state, ReservationStates.None);
            setTicketSequenceOut(getTicketSequenceIn() - 1);
            ((IClientActor) actor).extendTicket(this);
            logger.debug("Issued extendTicket request for reservation #"
                    + getReservationID().toHashString() + " State=" + printState());
            break;
        default:
            throw new OrcaException("Invalid pending state");
        }
    }

    private void recoverActiveNone() throws Exception {
        /*
         * If we were in Joining, restart the config actions and re-request the
         * lease. If we are in BlockedJoin, set joinstate to NoJoin and
         * re-request the lease. FIXME: why re-request the lease?
         */
        switch (joinstate) {
        case ReservationStates.NoJoin:
            logger.debug("No recovery necessary for reservation #"
                    + getReservationID().toHashString());
            break;

        case ReservationStates.Joining:
            logger.debug("Restarting configuration actions for reservation #"
                    + getReservationID().toHashString());
            // FIXME: this is a noop now. Should we just close?
            actor.getShirakoPlugin().restartConfigurationActions(this);
            logger.debug("Restarting configuration actions for reservation #"
                    + getReservationID().toHashString() + " complete");
            setPendingRecover(true);
            setLeaseSequenceOut(getLeaseSequenceOut() - 1);
            ((IServiceManager) actor).redeem(this);
            logger.debug("Issued redeem request for reservation #"
                    + getReservationID().toHashString() + " State=" + printState());
            // [We get to active/none after an updateTicket]
            break;

        case ReservationStates.BlockedJoin:
            /*
             * Do not clear the join state. If we fail here before issuing the
             * redeem request and the reservation gets committed to the
             * database, we will end in [Active, None, NoJoin], and then when we
             * try to recover we will assume (incorrectly) that the reservation
             * requires no recovery operations.
             */

            // r.transition("[recovery]", r.state, r.pending,
            // ReservationStates.NoJoin);
            setPendingRecover(true);
            setLeaseSequenceOut(getLeaseSequenceOut() - 1);
            ((IServiceManager) actor).redeem(this); // it could also
                                                    // be an
                                                    // extendLease
            logger.debug("Issued redeem request for reservation #"
                    + getReservationID().toHashString() + " State=" + printState());
            // [We get to active/none after an updateTicket]
            break;
        default:
            throw new OrcaException("Invalid join state");
        }

    }

    private void recoverActiveRedeeming() throws Exception {
        // the only way we can get here is if we failed during recovery
        logger.debug("It seems that we have failed earlier while recovering reservation #"
                + getReservationID().toHashString());

        // check if we need to restart configuration actions
        if (getPendingState() == ReservationStates.Joining) {
            logger.debug("Restarting configuration actions for reservation #"
                    + getReservationID().toHashString());
            actor.getShirakoPlugin().restartConfigurationActions(this);
        }

        setPendingRecover(true);
        // subtract one from the sequence counter and reissue the redeem
        transition("[recovery]", state, ReservationStates.None);
        setLeaseSequenceOut(getLeaseSequenceOut() - 1);
        ((IServiceManager) actor).redeem(this); // it could also
        logger.debug("Issued redeem request for reservation #" + getReservationID().toHashString()
                + " State=" + printState());
    }

    private void recoverActiveExtendingTicket() throws Exception {
        /* restart configuration actions if we are in Joining */
        if (joinstate == ReservationStates.Joining) {
            logger.debug("Restarting configuration actions for reservation #"
                    + getReservationID().toHashString());
            actor.getShirakoPlugin().restartConfigurationActions(this);
            logger.debug("Restarting configuration actions for reservation #"
                    + getReservationID().toHashString() + " complete");
        }

        // we need to get the ticket from the broker
        setPendingRecover(true);
        transition("[recovery]", state, ReservationStates.None);
        setTicketSequenceOut(getTicketSequenceOut() - 1);
        ((IClientActor) actor).extendTicket(this);
        logger.debug("Issued extendTicket request for reservation #"
                + getReservationID().toHashString() + " State=" + printState());
    }

    private void recoverActiveTicketedExtendingLease() throws Exception {
        // we need to get the lease from the site
        setPendingRecover(true);
        transition("[recovery]", state, ReservationStates.None);
        setLeaseSequenceOut(getLeaseSequenceOut() - 1);
        ((IServiceManager) actor).extendLease(this);
        logger.debug("Issued extend lease request for reservation #"
                + getReservationID().toHashString() + " State=" + printState());
    }

    private void recoverClosing() throws Exception {
        transition("[recovery]", state, ReservationStates.None);
        actor.close(this);
        logger.debug("Issued close request for reservation #" + getReservationID().toHashString()
                + " State=" + printState());
    }

    private void recoverActive() throws Exception {
        switch (pending) {
        case ReservationStates.None:
            recoverActiveNone();
            break;
        case ReservationStates.Redeeming:
            recoverActiveRedeeming();
            break;
        case ReservationStates.ExtendingTicket:
            recoverActiveExtendingTicket();
            break;
        case ReservationStates.Closing:
            recoverClosing();
            break;
        default:
            throw new OrcaException("Invalid pending state");
        }
    }

    private void recoverActiveTicketed() throws Exception {
        switch (pending) {
        case ReservationStates.None:
            recoverActiveNone();
            break;
        case ReservationStates.Redeeming:
            recoverActiveRedeeming();
            break;
        case ReservationStates.ExtendingLease:
            recoverActiveTicketedExtendingLease();
            break;
        case ReservationStates.Closing:
            recoverClosing();
            break;
        default:
            throw new OrcaException("Invalid pending state");
        }
    }

    public void recover(RecoverParent parent, Properties savedState) throws OrcaException {
        // ReservationClient is also used by sites to represent inventory. Make
        // sure
        // that recovery does not do anything fancy for these reservations.

        // WRITEME: make sure that ReservationClient when used by authority is
        // in ticketed,none so there is
        // nothing to do during recovery

        if (policy instanceof IAuthorityPolicy) {
            logger.debug("No recovery necessary for reservation #"
                    + getReservationID().toHashString());
            return;
        }

        try {
            switch (state) {
            case ReservationStates.Nascent:
                recoverNascent();
                break;
            case ReservationStates.Ticketed:
                recoverTicketed();
                break;
            case ReservationStates.Active:
                recoverActive();
                break;
            case ReservationStates.ActiveTicketed:
                recoverActiveTicketed();
                break;
            case ReservationStates.CloseWait:
                recoverClosing();
                break;
            case ReservationStates.Failed:
                // FIXME: what do we do here?
                logger.warn("Reservation #" + getReservationID().toHashString() + " has failed");
                break;
            }
        } catch (OrcaException e) {
            throw e;
        } catch (Exception e) {
            throw new OrcaException(e);
        }
    }
}
