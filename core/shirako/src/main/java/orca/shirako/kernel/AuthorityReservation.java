/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.kernel;

import java.util.Properties;

import orca.security.AuthToken;
import orca.shirako.api.IAuthority;
import orca.shirako.api.IAuthorityPolicy;
import orca.shirako.api.ICallbackProxy;
import orca.shirako.api.IConcreteSet;
import orca.shirako.api.IPolicy;
import orca.shirako.common.ReservationID;
import orca.shirako.time.Term;
import orca.shirako.util.Notice;
import orca.shirako.util.TestException;
import orca.util.OrcaException;
import orca.util.PropList;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistent;
import orca.util.persistence.RecoverParent;

import org.apache.log4j.Logger;

/**
 * AuthorityReservation controls the state machine for a reservation on the
 * authority side. It coordinates resource allocation, lease generation,
 * priming, and shutdown of reservations.
 */
class AuthorityReservation extends ReservationServer implements IKernelAuthorityReservation {
    /**
     * The ticket.
     */
    @Persistent(key = PropertyTicket)
    protected ResourceSet ticket;
    /**
     * Policies use this flag to instruct the core to send reservations to the
     * client even if they have deficit.
     */
    @Persistent
    protected boolean sendWithDeficit = true;
    /**
     * True if we notified the client about the fact that the reservation had
     * failed.
     */
    @NotPersistent
    protected boolean notifiedAboutFailure;

    /**
     * Creates a new "blank" reservation instance. Used during recovery.
     */
    public AuthorityReservation() {
        this.category = CategoryAuthority;
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
    public AuthorityReservation(final ReservationID rid, final ResourceSet resources,
            final Term term, final IKernelSlice slice) {
        super(rid, resources, term, slice);
        this.category = CategoryAuthority;
    }

    /**
     * Creates a new instance.
     * 
     * @param resources
     *            requested resources
     * @param term
     *            requested term
     * @param slice
     *            containing slice
     */
    public AuthorityReservation(final ResourceSet resources, final Term term,
            final IKernelSlice slice) {
        this(new ReservationID(), resources, term, slice);
    }

    @Override
    public void prepare(final ICallbackProxy srt, final Logger logger) throws Exception {
        setLogger(logger);
        callback = srt;
        requestedResources.validateIncomingTicket(requestedTerm);

        if (rid == null) {
            error("no reservation ID specified for request");
        }

        state = ReservationStates.Ticketed;
    }

    @Override
    public void reserve(IPolicy policy) throws Exception {
        nothingPending();
        incomingRequest();

        if (isActive()) {
            error("reservation already holds a lease");
        }

        this.policy = policy;
        approved = false;
        bidPending = true;
        pendingRecover = false;
        mapAndUpdate(false);
    }

    @Override
    public void serviceReserve() throws Exception {
        try {
            if (resources != null) {
                resources.serviceReserveSite();
            }
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            logException("authority failed servicing reserve", e);
            failNotify(e.toString());
        }
    }

    @Override
    public void extendLease() throws Exception {
    	
        nothingPending();
        incomingRequest();

        if (!isActive()) {
            error("reservation does not yet hold a lease");
        }

        if (!requestedTerm.extendsTerm(term)) {
            error("requested term does not extend current term for extendLease");
        }

        approved = false;
        bidPending = true;
        pendingRecover = false;
        mapAndUpdate(true);
    }

    @Override
    public void modifyLease() throws Exception {
    	
        nothingPending();
        incomingRequest();

        if (!isActive()) {
            error("reservation does not yet hold a lease");
        }

        // anirban@ 04/07/15: If we decide to call a modify policy at some later point of time, set approved to false
        approved = true;
        bidPending = true;
        pendingRecover = false;
        mapAndUpdateModifyLease();
    }
    
    @Override
    public void serviceExtendLease() throws Exception {
        assert ((state == ReservationStates.Failed) && (pending == ReservationStates.None))
                || ((pending == ReservationStates.ExtendingLease) || (pending == ReservationStates.Priming));
        
        try {
            if (pending == ReservationStates.Priming) {
                resources.serviceExtend();
            }
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            logException("authority failed servicing extendLease", e);
            failNotify(e.toString());
        }
    }

    @Override
    public void serviceModifyLease() throws Exception {
    	
        assert ((state == ReservationStates.Failed) && (pending == ReservationStates.None))
                || ((pending == ReservationStates.ModifyingLease) || (pending == ReservationStates.Priming));

        try {
            if (pending == ReservationStates.Priming) {
                resources.serviceModify();
            }
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            logException("authority failed servicing modifyLease", e);
            failNotify(e.toString());
        }
    }
    
    @Override
    public void close() {
        if (logger.isDebugEnabled()) {
            logger.debug("Processing  close for #" + rid.toHashString());
        }
        transition("external close", state, ReservationStates.Closing);
    }

    @Override
    public void serviceClose() {
        if (resources != null) {
            resources.close();
        }
    }

    @Override
    public void handleDuplicateRequest(final int operation) throws Exception {
        /*
         * The general idea is to do nothing if we are in the process of
         * performing a pending operation or about to reissue a
         * ticket/extendTicket after recovery. If there is nothing pending for
         * this reservation, we resend the last update.
         */
        switch (operation) {
        case RequestTypes.RequestRedeem:
            if ((pending == ReservationStates.None) && !bidPending && !pendingRecover) {
                generateUpdate();
            }
            break;
        case RequestTypes.RequestExtendLease:
            if ((pending == ReservationStates.None) && !bidPending && !pendingRecover) {
                generateUpdate();
            }
            break;
        case RequestTypes.RequestModifyLease:
            if ((pending == ReservationStates.None) && !bidPending && !pendingRecover) {
                generateUpdate();
            }
            break;    
        default:
            throw new OrcaException("Unsupported operation: " + operation);
        }
    }

    /**
     * Calls the policy to fill a request, with associated state transitions.
     * 
     * @param extend
     *            true iff this request is an extend
     * @return boolean success
     */
    protected boolean mapAndUpdate(final boolean extend) {
        boolean success = false;
        boolean granted = false;

        switch (state) {
        case ReservationStates.Failed:
            /*
             * Must be a previous failure, or policy marked it as failed. Send
             * update to reset client.
             */
            generateUpdate();
            break;

        case ReservationStates.Ticketed:
            assert !extend;
            try {
                transition("redeeming", ReservationStates.Ticketed, ReservationStates.Redeeming);
                /*
                 * If the policy has processed this reservation, set granted to
                 * true so that we can start priming the resources. If the
                 * policy has not yet processed this reservation (binPending is
                 * true) then call the policy. The policy may choose to process
                 * the request immediately (true) or to defer it (false). In
                 * case of a deferred request, we will eventually come back to
                 * this method after the policy has done its job.
                 */
                if (isBidPending()) {
                    granted = ((IAuthorityPolicy) policy).bind(this);
                } else {
                    granted = true;
                }
            } catch (Exception e) {
                logException("authority policy bind", e);
                failNotify(e.toString());
                break;
            }

            if (granted) {
                try {
                    success = true;
                    ticket = requestedResources;
                    term = approvedTerm;
                    // note: clones all properties as well
                    resources = requestedResources.abstractClone();
                    // reset the unit count
                    resources.units = 0;
                    resources.update(this, approvedResources);
                    transition("redeem", ReservationStates.Ticketed, ReservationStates.Priming);
                } catch (Exception e) {
                    logException("authority redeem", e);
                    failNotify(e.toString());
                }
            }
            break;

        case ReservationStates.Active:
            assert extend;
            try {
                transition("extending lease",
                        ReservationStates.Active,
                        ReservationStates.ExtendingLease);
                /*
                 * If the policy has processed this reservation, set granted to
                 * true so that we can start priming the resources. If the
                 * policy has not yet processed this reservation (binPending is
                 * true) then call the policy. The policy may choose to process
                 * the request immediately (true) or to defer it (false). In
                 * case of a deferred request, we will eventually come back to
                 * this method after the policy has done its job.
                 */
                if (isBidPending()) {
                    granted = ((IAuthorityPolicy) policy).extend(this);
                } else {
                    granted = true;
                }

                if (granted) {
                    success = true;
                    extended = true;
                    previousTerm = term;
                    // FIXME: should we preserve previous resources?
                    ticket = requestedResources;
                    term = approvedTerm;
                    // attach the configuration properties to the approved
                    // resources
                    if (requestedResources.getConfigurationProperties() != null) {
                        approvedResources.setConfigurationProperties(requestedResources.getConfigurationProperties());
                    }
                    resources.update(this, approvedResources);
                    // transition to priming, in case we added new resources
                    transition("extend lease", ReservationStates.Active, ReservationStates.Priming);
                }
            } catch (Exception e) {
                logException("authority mapper extend", e);
                failNotify(e.toString());
            }
            break;
        default:
            fail("mapAndUpdate: unexpected state");
        }

        return success;
    }

    /**
     * Calls the policy to fill a request, with associated state transitions.
     * 
     * @param modify
     *            true iff this request is an modify
     * @return boolean success
     */
    protected boolean mapAndUpdateModifyLease() {
    	
        boolean success = false;
        boolean granted = false;

        switch (state) {
        case ReservationStates.Failed:
            /*
             * Must be a previous failure, or policy marked it as failed. Send
             * update to reset client.
             */
            generateUpdate();
            break;

        case ReservationStates.Active:
            try {
                transition("modifying lease",
                        ReservationStates.Active,
                        ReservationStates.ModifyingLease);
//                /*
//                 * If the policy has processed this reservation, set granted to
//                 * true so that we can start priming the resources. If the
//                 * policy has not yet processed this reservation (binPending is
//                 * true) then call the policy. The policy may choose to process
//                 * the request immediately (true) or to defer it (false). In
//                 * case of a deferred request, we will eventually come back to
//                 * this method after the policy has done its job.
//                 */
//                if (isBidPending()) {
//                    granted = ((IAuthorityPolicy) policy).extend(this);
//                } else {
//                    granted = true;
//                }
                
                // anirban@ 04/07/15: If we decide to have a modify policy, call policy here, as above
            	// For now, assume modify is always granted
                granted = true;

                if (granted) {
                    success = true;
                    ticket = requestedResources;
                    // attach the configuration properties to the approved resources
                    // requestedResources.getConfigurationProperties() contains the modifyProperties
                    
                    // TODO: merge the configuration properties of approved and requested resources and put it in approved resources instead ?
                    logger.debug("requestedResources.getConfigurationProperties() = " + requestedResources.getConfigurationProperties());
                    logger.debug("approvedResources.getConfigurationProperties() = " + approvedResources.getConfigurationProperties());
                    if (requestedResources.getConfigurationProperties() != null) {
                    	if (approvedResources.getConfigurationProperties() == null)
                    		approvedResources.setConfigurationProperties(requestedResources.getConfigurationProperties());
                    	else
                    		PropList.mergePropertiesPriority(requestedResources.getConfigurationProperties(), approvedResources.getConfigurationProperties());
                    }
                    logger.debug("approvedResources.getConfigurationProperties() = " + approvedResources.getConfigurationProperties());
                    resources.updateProps(this, approvedResources);
                    // transition to priming
                    transition("modify lease", ReservationStates.Active, ReservationStates.Priming);
                }
            } catch (Exception e) {
                logException("authority mapper modify", e);
                failNotify(e.toString());
            }
            break;
        default:
            fail("mapAndUpdateModifyLease: unexpected state");
        }

        return success;
    }
    
    
    @Override
    protected void generateUpdate() {
        if (callback == null) {
            logWarning("cannot generate update: no callback");
            return;
        }

        try {
            updateCount++;
            sequenceOut++;
            RPCManager.updateLease(this);
        } catch (Exception e) {
            logRemoteError("callback failed", e);
        }
    }

    public void handleFailedRPC(FailedRPC rpc) {
        // make sure that the failed RPC came from the callback identity
        AuthToken remoteAuth = rpc.getRemoteAuth();
        switch (rpc.getRequestType()) {
        case UpdateLease:
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

    public void prepareExtendLease() throws Exception {
        requestedResources.validateIncomingTicket(requestedTerm);
    }

    public void prepareModifyLease() throws Exception {
        requestedResources.validateIncomingTicket(requestedTerm);
    }
    
    @Override
    public void prepareProbe() throws Exception {
        try {
            if (resources != null) {
                resources.prepareProbe();
            }
        } catch (Exception e) {
            logException("exception in authority prepareProbe", e);
        }
    }

    @Override
    public void probePending() throws Exception {
//        if (logger.isDebugEnabled()) {
//            logger.debug("AuthorityReservation probePending: " + this.toLogString());
//        }
    	
        if (servicePending != ReservationStates.None) {
            logError("service overrun in probePending");
            return;
        }

        reap();

        switch (pending) {
        case ReservationStates.None:
            // generate an update if reservation failed, else nothing to do
            if (isFailed() && !notifiedAboutFailure) {
                generateUpdate();
                notifiedAboutFailure = true;
            }
            
            break;

        case ReservationStates.Redeeming:
            /*
             * We are an authority trying to satisfy a ticket redeem on behalf
             * of a client. Retry policy bind.
             */
            assert state == ReservationStates.Ticketed;
            if (!bidPending && mapAndUpdate(false)) {
                logger.debug("Resource assignment (redeem) for #" + rid.toHashString() + " completed");
                servicePending = ReservationStates.Redeeming;
            }
            break;

        case ReservationStates.ExtendingLease:
            assert state == ReservationStates.Active;
            if (!bidPending && mapAndUpdate(true)) {
                logger.debug("Resource assignment (extend) for #" + rid.toHashString() + " completed");
                servicePending = ReservationStates.ExtendingLease;
            }
            break;

        // This case will not arise if there are no modify policies, which might defer modify    
        case ReservationStates.ModifyingLease:
            assert state == ReservationStates.Active;
            logger.info("In AuthorityReservation.probePending(): pending state is ModifyingLease and res state is Active");
            if (!bidPending && mapAndUpdateModifyLease()) {
                logger.debug("Resource assignment (modify) for #" + rid.toHashString() + " completed");
                servicePending = ReservationStates.ModifyingLease;
            }
            break;
            
        case ReservationStates.Closing:
            if ((resources == null) || resources.isClosed()) {
                transition("close complete", ReservationStates.Closed, ReservationStates.None);
                pendingRecover = false;
                generateUpdate();
            }
            break;

        case ReservationStates.Priming:
            /*
             * We are an authority filling a ticket claim. Got resources? Note
             * that active() just means no primes/closes/modifies are still in
             * progress. The primes/closes/modifies could have failed. If
             * something succeeded, then we report what we got as active, else
             * it's a complete bust.
             */
        	
            if (resources.isActive()) {
                /*
                 * If something failed or we are recovering, we need to correct
                 * the deficit. For a recovering reservation we need to call
                 * correctDeficit regardless of whether there is a real deficit,
                 * since the individual nodes may be inconsistent with what the
                 * client/broker wanted. For example, they may have the wrong
                 * logical ids and resource shares.
                 */
                if (pendingRecover || (getDeficit() != 0)) {
                    /*
                     * The abstract and the concrete units may be different. We
                     * need to adjust the abstract to equal concrete so that
                     * future additions of resources will not result in
                     * inconsistent abstract unit count.
                     */
                    resources.fixAbstractUnits();
                    /*
                     * Policies can instruct us to let go a reservation with a
                     * deficit. For example, a policy failed adding resources to
                     * the reservation multiple times and it wants to prevent
                     * exhausting its inventory from servicing this particular
                     * request: probably something is wrong with the request.
                     */
                    if (!sendWithDeficit) {
                        /* Call the policy to correct the deficit */
                        ((IAuthorityPolicy) policy).correctDeficit(this);
                        /*
                         * XXX: be careful here. we are reusing extending for
                         * the purpose of triggering configuration actions on
                         * the newly assigned nodes. If this is not appropriate,
                         * we may need a new servicePending value
                         */
                        servicePending = ReservationStates.ExtendingLease;
                    } else {
                        pendingRecover = false;
                        // reservations with 0 units should be failed
                        if (resources.getResources().getUnits() == 0) {
                            String message = (udd.getEvents() != null ? udd.getEvents()
                                    : "no information available");
                            fail("all units failed priming: " + message);
                            udd.clear();
                        } else {
                            transition("prime complete1",
                                    ReservationStates.Active,
                                    ReservationStates.None);
                        }
                        generateUpdate();
                    }
                } else {
                    pendingRecover = false;
                    transition("prime complete2", ReservationStates.Active, ReservationStates.None);
                    generateUpdate();
                }
            }
            
            // If no unit is priming or closing, but modification is in process

            break;
        }

    }

    @Override
    public void serviceProbe() throws Exception {
        /*
         * An exception in one of these service routines should mean some
         * unrecoverable, reservation-wide failure. It should not occur, e.g.,
         * if some subset of the resources fail.
         */
        try {
            switch (servicePending) {
            case ReservationStates.Redeeming:
                serviceReserve();
                break;

            case ReservationStates.ExtendingLease:
                serviceExtendLease();
                break;
             
            case ReservationStates.ModifyingLease:
                serviceModifyLease();
                break;                    
            }
            
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            logException("authority failed servicing probe", e);
            failNotify("post-op exception: " + e.toString());
        }

        servicePending = ReservationStates.None;
    }

    /**
     * Reaps any failed or closed resources. Need more here: if reservation has
     * a deficit due to failures then we need to find some replacements.
     * 
     * @throws Exception
     */
    private void reap() throws Exception {
        try {
            if (resources != null) {
                ResourceSet released = resources.collectReleased();
                if (released != null) {
                    if (!released.getNotices().isEmpty()) {
                        udd.post(released.getNotices().getNotice());
                    }
                    ((IAuthorityPolicy) policy).release(released);
                }
            }
        } catch (Exception e) {
            logException("exception in authority reap", e);
        }
    }

    public void recover(RecoverParent parent, Properties savedState) throws OrcaException {
        try {
            switch (state) {
            case ReservationStates.Ticketed:
                switch (pending) {
                case ReservationStates.None:
                    // Redeem did not start
                    ((IAuthority) actor).redeem(this);
                    break;
                case ReservationStates.Redeeming:
                    // Redeem started, but the policy had not finished assigning
                    // resources
                    transition("[recover]", state, ReservationStates.None);
                    ((IAuthority) actor).redeem(this);
                    break;
                case ReservationStates.Priming:
                    // The policy assigned resources and they were in the
                    // process of being setup.
                    pendingRecover = true;
                    // FIXME: simple for now, try to do better in the future
                    ((IAuthority) actor).close(this);
                    break;
                case ReservationStates.Closing:
                    // A close was issued before the reservation became active
                    ((IAuthority) actor).close(this);
                    break;
                default:
                    throw new OrcaException("Unexpected reservation state: state=" + state
                            + " pending=" + pending);
                }
                break;
            case ReservationStates.Active:
                switch (pending) {
                case ReservationStates.None:
                    break; // nothing to do
                case ReservationStates.ExtendingLease:
                    // ExtendLease started but the policy did not finish
                    // assigning resources
                    transition("[recover]", state, ReservationStates.None);
                    ((IAuthority) actor).extendLease(this);
                    break;
                case ReservationStates.Priming:
                    // The policy assigned resources, but the substrate actions
                    // did not complete
                    pendingRecover = true;
                    // FIXME: simple for now, try to do better in the future
                    ((IAuthority) actor).close(this);
                    break;
                case ReservationStates.Closing:
                    // A close was issued
                    ((IAuthority) actor).close(this);
                    break;
                default:
                    throw new OrcaException("Unexpected reservation state: state=" + state
                            + " pending=" + pending);
                }

                break;
            default:
                throw new OrcaException("Unexpected reservation state: state=" + state
                        + " pending=" + pending);
            }
        } catch (OrcaException e) {
            throw e;
        } catch (Exception e) {
            throw new OrcaException(e);
        }
    }

    public void setSendWithDeficit(final boolean value) {
        this.sendWithDeficit = value;
    }

    public int getDeficit() {
        int result = 0;

        if (requestedResources != null) {
            result = requestedResources.getUnits();
        }

        if (resources != null) {
            IConcreteSet cs = resources.getResources();
            if (cs != null) {
                result -= cs.getUnits();
            }
        }

        return result;
    }

    @Override
    public int getLeasedUnits() {
        if (resources != null) {
            IConcreteSet cs = resources.getResources();
            if (cs != null) {
                return cs.getUnits();
            }
        }
        return 0;
    }

    @Override
    public String getNotices() {
        String s = super.getNotices();

        if (resources != null) {
            if (resources.getResources() != null) {
                IConcreteSet cs = resources.getResources();
                Notice n = cs.getNotices();
                if (n.getNotice() != null)
                    s += "\n" + n.getNotice();
            }
        }

        return s;
    }

    public ResourceSet getTicket() {
        return ticket;
    }
}
