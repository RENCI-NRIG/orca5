/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.kernel;

import java.util.Date;
import java.util.Properties;

import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.api.IAuthorityPolicy;
import orca.shirako.api.IAuthorityProxy;
import orca.shirako.api.IBroker;
import orca.shirako.api.IBrokerPolicy;
import orca.shirako.api.ICallbackProxy;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IPolicy;
import orca.shirako.api.IServerPolicy;
import orca.shirako.common.ReservationID;
import orca.shirako.time.Term;
import orca.util.OrcaException;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistent;
import orca.util.persistence.RecoverParent;

import org.apache.log4j.Logger;

/*
 * A note on exported "will call" reservations. An export() operation may be
 * locally initiated on an agent. It binds and forms a ticket in the same way as
 * if the request came from a client, but there is no client rid (remoteRid) and
 * no callback object. The prepare method in AgentReservation and
 * register/unregister in ReservationServer handle these cases: the export
 * proceeds as a normal reserve request, but it leaves the callback and
 * remoteRid null, does not register the reservation with its slice (since there
 * is no remoteRid), and does not issue an updateTicket (since there is no
 * callback). The client claims the ticket with a claim request, passing the
 * exportedRid, and a remoteRid and callback in the usual fashion. At this time,
 * prepareClaim() below sets the callback and remoteRid, then claim() registers
 * the reservation with its slice and issues the ticket. It would be irregular
 * for an export request to not be satisfied immediately, or for an extend
 * request to arrive on an exported ticket that has not yet been claimed. Even
 * so, all code in AgentReservation checks against a null callback before
 * attempting to issue an updateTicket. Implementation note: once any request
 * fails, this version marks the reservation as Failed and disallows any
 * subsequent operations.
 */
class BrokerReservation extends ReservationServer implements IKernelBrokerReservation {
    public static final String PropertySource = "AgentReservationSource";
    public static final String PropertyExporting = "AgentReservationExporting";
    public static final String PropertyAuthority = "AgentReservationAuthority";
    public static final String PropertyMustSendUpdate = "AgentReservationMustSendUpdate";

    /*
     * Members requiring serialization
     */

    /**
     * Reservation backing the ticket granted to this reservation. For now only
     * one source reservation can be used to issue a ticket to satisfy a client
     * request.
     */
    @Persistent(key = PropertySource, reference = true)
    protected IClientReservation source;

    /**
     * If this flag is true, then the reservation represents a request to export
     * resources to a client.
     */
    @Persistent(key = PropertyExporting)
    protected boolean exporting;

    /**
     * The authority in control of the resources.
     */
    @Persistent(key = PropertyAuthority)
    protected IAuthorityProxy authority;

    /**
     * True if an updateTicket() must be sent on the next service probe.
     */
    @Persistent(key = PropertyMustSendUpdate)
    protected boolean mustSendUpdate;

    /*
     * Members that do not require serialization.
     */

    /**
     * True if we notified the client about the fact that the reservation had
     * failed.
     */
    @NotPersistent
    protected boolean notifiedFailed = false;

    /**
     * True if the reservation was closed in the priming state.
     */
    @NotPersistent
    protected boolean closedInPriming = false;

    /**
     * Creates a new "blank" reservation. Generates a new identifier.
     */
    public BrokerReservation() {
        this(new ReservationID(), null, null, null);
    }

    /**
     * Creates a new instance.
     * 
     * @param rid
     *            reservation identifier
     * @param resources
     *            requested resources
     * @param term
     *            requested term
     * @param slice
     *            containing slice
     */
    public BrokerReservation(final ReservationID rid, final ResourceSet resources, final Term term,
            final IKernelSlice slice) {
        super(rid, resources, term, slice);
        exporting = false;
        this.category = CategoryBroker;
    }

    /**
     * Creates a new instance. Generates a new reservation identifier.
     * 
     * @param resources
     *            requested resources
     * @param term
     *            requested term
     * @param slice
     *            containing slice
     */
    public BrokerReservation(final ResourceSet resources, final Term term, final IKernelSlice slice) {
        this(new ReservationID(), resources, term, slice);
    }

    /**
     * Converts the reservation to a state string.
     * 
     * @param res
     *            reservation
     * @return state string representing the reservation
     */
    private String printState() {
        return "[" + getStateName() + "," + getPendingStateName() + "] (" + getSequenceIn() + ")("
                + getSequenceOut() + ")";
    }

    public void recover(RecoverParent parent, Properties savedState) throws OrcaException {
        if (policy instanceof IAuthorityPolicy) {
            // FIXME: We do not do much with these, but this seems iffy.
            logger.debug("No recovery necessary for reservation #"
                    + getReservationID().toHashString());
            return;
        }

        if (!(policy instanceof IBrokerPolicy)) {
            throw new OrcaException("Do not know how to recover: policy="
                    + policy.getClass().getName());
        }

        try {
            switch (state) {
            case ReservationStates.Nascent:
                switch (pending) {
                case ReservationStates.None:
                    ((IBroker) actor).ticket(this);
                    logger.info("Added reservation #" + getReservationID().toHashString()
                            + " to the ticketing list. State=" + printState());
                    break;

                case ReservationStates.Ticketing:
                    setPendingRecover(true);
                    transition("[recovery]", state, ReservationStates.None);
                    ((IBroker) actor).ticket(this);
                    logger.info("Added reservation #" + getReservationID().toHashString()
                            + " to the ticketing list. State=" + printState());
                    break;

                default:
                    throw new OrcaException("Unexpected pending state");
                }
                break;

            case ReservationStates.Ticketed:
                switch (pending) {
                case ReservationStates.None:
                case ReservationStates.Priming:
                    setServicePending(ReservationStates.None);
                    logger.debug("No recovery necessary for reservation #"
                            + getReservationID().toHashString());
                    break;

                case ReservationStates.ExtendingTicket:
                    setPendingRecover(true);
                    transition("[recovery]", state, ReservationStates.None);
                    ((IBroker) actor).extendTicket(this);
                    logger.debug("Added reservation #" + getReservationID().toHashString()
                            + " to the extending list. State=" + printState());
                    break;

                default:
                    throw new OrcaException("Unexpected pending state");
                }

                break;
            case ReservationStates.Failed:
                // FIXME: what do we do here?
                logger.warn("Reservation #" + getReservationID().toHashString() + " has failed");
                break;

            default:
                throw new OrcaException("Unexpected reservation state");
            }
        } catch (OrcaException e) {
            throw e;
        } catch (Exception e) {
            throw new OrcaException(e);
        }
    }

    public void handleFailedRPC(FailedRPC rpc) {
        // make sure that the failed RPC came from the callback identity
        AuthToken remoteAuth = rpc.getRemoteAuth();
        switch (rpc.getRequestType()) {
        case UpdateTicket:
            if (callback == null || !callback.getIdentity().equals(remoteAuth)) {
                throw new RuntimeException("Unauthorized Failed reservation RPC: expected="
                        + callback.getIdentity() + ", but was: " + remoteAuth);
            }
            break;
        default:
            throw new RuntimeException("Unexpected FailedRPC for BrokerReservation. RequestType="
                    + rpc.getRequestType());
        }
        super.handleFailedRPC(rpc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepare(final ICallbackProxy srt, final Logger logger) throws Exception {
        setLogger(logger);
        callback = srt;

        /*
         * Null callback indicates a locally initiated request to create an
         * exported reservation. Else the request is from a client and must have
         * a client-specified RID.
         */
        if (callback != null) {
            if (rid == null) {
                error("no reservation ID specified for request");
            }
        }

        setDirty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reserve(final IPolicy policy) throws Exception {
        /*
         * These handlers may need to be slightly more sophisticated, since a
         * client may bid multiple times on a ticket as part of an auction
         * protocol: so we may receive a reserve or extend when there is already
         * a request pending.
         */
        incomingRequest();

        if ((pending != ReservationStates.None) && (pending != ReservationStates.Ticketing)) {
            // We do not want to fail the reservation
            // simply log a warning and exit from reserve
            logger.warn("Duplicate ticket request");
            return;
        }

        this.policy = policy;
        approved = false;
        bidPending = true;
        mapAndUpdate(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serviceReserve() throws Exception {
        // resources is null initially. It becomes non-null once the
        // policy completes its allocation.
        if (resources != null) {
            resources.serviceUpdate(this);
            if (!isFailed()) {
                transition("update absorbed", ReservationStates.Ticketed, ReservationStates.None);
                generateUpdate();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void claim() throws Exception {
        approved = false;
        switch (state) {
        case ReservationStates.Ticketed:
            /*
             * We are an agent asked to return a pre-reserved "will call" ticket
             * to a client. Set mustSendUpdate so that the update will be sent
             * on the next probe.
             */
            mustSendUpdate = true;
            break;
        default:
            error("Wrong reservation state for ticket claim");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void extendTicket(final IActor actor) throws Exception {
        incomingRequest();

        /*
         * State must be ticketed. The reservation may be active, but the agent
         * wouldn't know that.
         */
        if (state != ReservationStates.Ticketed) {
            error("extending unticketed reservation");
        }

        if ((pending != ReservationStates.None) && (pending != ReservationStates.ExtendingTicket)) {
            error("extending reservation with another pending request");
        }

        if (!requestedTerm.extendsTerm(term)) {
            error("new term does not extend current term");
        }

        approved = false;
        bidPending = true;
        pendingRecover = false;
        mapAndUpdate(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serviceExtendTicket() throws Exception {
        if (pending == ReservationStates.None) {
            resources.serviceUpdate(this);
            if (!isFailed()) {
                transition("update absorbed", ReservationStates.Ticketed, ReservationStates.None);
                generateUpdate();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        boolean sendNotification = false;

        if ((state == ReservationStates.Nascent) || (pending != ReservationStates.None)) {
            logger.warn("Closing a reservation in progress");
            sendNotification = true;
        }

        if (state != ReservationStates.Closed) {
            if (pending == ReservationStates.Priming
                    || (pending == ReservationStates.Ticketing && !bidPending)) {
                /*
                 * Close in Priming is a special case: when processing the close
                 * event inside the policy we cannot rely on resources to
                 * represent the resources allocated to the reservation. They
                 * may either represent the previous resources or a mixture of
                 * both. So here we will mark the reservation that it was closed
                 * while it was in the Priming state. When processing the close
                 * event the policy must free previousResources (if any) and
                 * approvedResources. The policy should not free resources.
                 */
                if (logger.isDebugEnabled()) {
                    logger.debug("closing reservation #" + rid.toHashString() + " while in Priming");
                }
                closedInPriming = true;
            }

            // transition to closed
            transition("closed", ReservationStates.Closed, ReservationStates.None);
            // tell the policy we are closed so that it can clean up if
            // necessary.
            policy.closed(this);
        }

        if (sendNotification) {
            /* we send a failure notification to the client */
            udd.error("Closed while allocating ticket");
            generateUpdate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void probePending() throws Exception {
        if (servicePending != ReservationStates.None) {
            internalError("service overrun in probePending");
        }

        // removed - this makes even debug mode too chatty /ib 08/27/14
        //if (logger.isDebugEnabled()) {
        //    logger.debug("AgentReservation probePending: " + this.toLogString());
        //}

        if (isFailed() && !notifiedFailed) {
            // failNotify("Failed reservation");
            generateUpdate();
            notifiedFailed = true;
        } else {
            switch (pending) {
            case ReservationStates.Ticketing:

                /*
                 * Check for a pending ticket operation that may have completed
                 */
                if (!bidPending && mapAndUpdate(false)) {
                    servicePending = ReservationStates.AbsorbUpdate;
                }

                break;

            case ReservationStates.ExtendingTicket:

                /*
                 * Check for a pending extendTicket operation
                 */
                if (!bidPending && mapAndUpdate(true)) {
                    servicePending = ReservationStates.AbsorbUpdate;
                }

                break;

            case ReservationStates.Redeeming:
                logger.error("AgentReservation in unexpected state");

                break;

            case ReservationStates.Priming:
                servicePending = ReservationStates.AbsorbUpdate;

                break;

            case ReservationStates.None:
                // for exported reservations that have been claimed, we need
                // to
                // schedule a ticketUpdate
                if (mustSendUpdate) {
                    servicePending = ReservationStates.SendUpdate;
                    mustSendUpdate = false;
                }
                break;
            default:
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serviceProbe() throws Exception {
        try {
            switch (servicePending) {
            // case Ticketing:
            // resources.serviceReserve(this);
            // generateUpdate();
            // break;
            // case ExtendingTicket:
            // resources.serviceExtend(term);
            // generateUpdate();
            // break;
            case ReservationStates.AbsorbUpdate:
                resources.serviceUpdate(this);

                if (!isFailed()) {
                    transition("update absorbed",
                            ReservationStates.Ticketed,
                            ReservationStates.None);
                    generateUpdate();
                }

                break;

            case ReservationStates.SendUpdate:
                // This pseudo-state is used to trigger sending an
                // updateTicket
                // back to the client
                generateUpdate();
                break;
            }
        } catch (Exception e) {
            logException("failed while servicing probe", e);
            failNotify(e.toString());
        }

        servicePending = ReservationStates.None;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleDuplicateRequest(final int operation) throws Exception {
        /*
         * The general idea is to do nothing if we are in the process of
         * performing a pending operation or about to reissue a
         * ticket/extendTicket after recovery. If there is nothing pending for
         * this reservation, we resend the last update.
         */
        switch (operation) {
        case RequestTypes.RequestTicket:

            if ((pending == ReservationStates.None) && (state != ReservationStates.Nascent)
                    && !pendingRecover) {
                generateUpdate();
            }

            break;

        case RequestTypes.RequestExtendTicket:

            if ((pending == ReservationStates.None) && !pendingRecover) {
                generateUpdate();
            }

            break;
        case RequestTypes.RequestRelinquish:
            // relinquish does not send a response
            break;
        default:
            throw new Exception("Unsupported operation: " + operation);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void generateUpdate() {
        logger.debug("Generating update");
        if (callback == null) {
            logger.warn("Cannot generate update: no callback");
            return;
        }

        logger.debug("Generating update: update count=" + updateCount);
        try {
            updateCount++;
            sequenceOut++;
            RPCManager.updateTicket(this);
        } catch (Exception e) {
            /*
             * Note that this may result in a "stuck" reservation... not much we
             * can do if the receiver has failed or rejects our update. We will
             * regenerate on any user-initiated probe.
             */
            logRemoteError("Callback failed", e);
        }
    }

    /**
     * Call the policy to fill a request, with associated state transitions.
     * Catch exceptions and report all errors using callback mechanism.
     * 
     * @param ticketed
     *            true iff this is ticketed (i.e., request is extend)
     * @return boolean success
     */
    protected boolean mapAndUpdate(final boolean ticketed) {
        boolean success = false;
        boolean granted = false;

        switch (state) {
        case ReservationStates.Failed:
            /*
             * Must be a previous failure, or policy marked as failed. Send
             * update to reset client. Note: this might be the wrong thing if a
             * bidding protocol allows the caller to retry a denied request,
             * e.g., to bid higher after losing in an auction.
             */
            generateUpdate();
            break;

        case ReservationStates.Nascent:
            if (ticketed) {
                failNotify("reservation is not yet ticketed");
                break;
            }

            try {
                /*
                 * If the policy has processed this reservation, granted should
                 * be set true so that we can send the result back to the
                 * client. If the policy has not yet processed this reservation
                 * (binPending is true) then call the policy. The policy may
                 * choose to process the request immediately (true) or to defer
                 * it (false). In case of a deferred request, we will eventually
                 * come back to this method after the policy has done its job.
                 */
                granted = false;

                if (isBidPending()) {
                    if (!isExporting()) {
                        granted = ((IServerPolicy) policy).bind(this);
                    } else {
                        internalError("Exporting reservations not implemented");
                    }
                } else {
                    granted = true;
                }
                transition("ticket request", ReservationStates.Nascent, ReservationStates.Ticketing);
            } catch (Exception e) {
                logger.error("mapAndUpdate bindTicket failed for ticketRequest:", e);
                failNotify(e.toString());

                break;
            }

            if (granted) {
                try {
                    success = true;
                    term = approvedTerm;
                    resources = approvedResources.abstractClone();
                    resources.update(this, approvedResources);
                    transition("ticketed", ReservationStates.Ticketed, ReservationStates.Priming);
                } catch (Exception e) {
                    logException("mapAndUpdate ticket failed for ticketRequest", e);
                    failNotify(e.toString());
                    break;
                }
            }

            break;

        case ReservationStates.Ticketed:
            if (!ticketed) {
                failNotify("reservation is already ticketed");
                break;
            }

            try {
                transition("extending ticket",
                        ReservationStates.Ticketed,
                        ReservationStates.ExtendingTicket);
                /*
                 * If the policy has processed this reservation, set granted to
                 * true so that we can send the ticket back to the client. If
                 * the policy has not yet processed this reservation (binPending
                 * is true) then call the policy. The plugin may choose to
                 * process the request immediately (true) or to defer it
                 * (false). In case of a deferred request, we will eventually
                 * come back to this method after the policy has done its job.
                 */
                granted = false;

                if (isBidPending()) {
                    granted = ((IServerPolicy) policy).extend(this);
                } else {
                    granted = true;
                }
            } catch (Exception e) {
                logException("mapAndUpdate extendTicket failed", e);
                failNotify(e.toString());

                break;
            }

            if (granted) {
                try {
                    success = true;
                    extended = true;
                    transition("extended ticket",
                            ReservationStates.Ticketed,
                            ReservationStates.Priming);
                    previousTerm = term;
                    /*
                     * Make a clone of the current resources. Preserving the
                     * current ticket will help us do cleanup if the reservation
                     * gets closed before it transitions to Ticketed, None.
                     */
                    previousResources = (ResourceSet) resources.clone();
                    term = approvedTerm;
                    // resources.softChange(this, approvedResources,
                    // approvedTerm);
                    resources.update(this, approvedResources);
                } catch (Exception e) {
                    logException("mapAndUpdate ticket failed", e);
                    failNotify(e.toString());

                    break;
                }
            }

            break;

        default:
            logError("broker mapAndUpdate: unexpected state");
            failNotify("invalid operation for the current reservation state");
        }

        return success;
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
    public IClientReservation getSource() {
        return source;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnits(final Date when) {
        int hold = 0;

        if (!isTerminal()) {
            hold = resources.getConcreteUnits(when);
        }

        return hold;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClosedInPriming() {
        return closedInPriming;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExporting() {
        return exporting;
    }

    /**
     * {@inheritDoc}
     */
    public void setAuthority(final IAuthorityProxy authority) {
        this.authority = authority;
    }

    /**
     * {@inheritDoc}
     */
    public void setExporting() {
        exporting = true;
    }

    /**
     * {@inheritDoc}
     */
    public void setSource(final IClientReservation source) {
        this.source = source;
    }
}
