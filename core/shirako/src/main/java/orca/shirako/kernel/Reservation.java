/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.kernel;

import java.util.Date;
import java.util.Properties;

import orca.security.Guard;
import orca.shirako.api.IActor;
import orca.shirako.api.IPolicy;
import orca.shirako.api.IReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.container.Globals;
import orca.shirako.time.Term;
import orca.shirako.util.ReservationState;
import orca.shirako.util.UpdateData;
import orca.util.ExceptionUtils;
import orca.util.ID;
import orca.util.OrcaException;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistent;

import org.apache.log4j.Logger;

// NOTE: get rid of synchronized: reservation is accessed only by the actor main thread. No need for additional locking.

/*
 * Programmer's note: no Reservation class should touch fields of ResourceSet
 * directly---use the methods.
 */

/*
 * These are the only methods synchronized on the Reservation object itself. The
 * purpose is to allow an external thread to await a state transition in a
 * Reservation without holding the manager lock. State changes are made only
 * while holding the manager lock, so a manager may examine the state without
 * acquiring the reservation lock.
 */

/*
 * Reservation objects passed into a slices actor to initiate or request new
 * reservations are taken over by the kernel. Validate the passed-in state, mark
 * some context specific to the operation, and clean out the rest of it in
 * preparation to link it into manager structures. No locks are held, and these
 * routines have no side effects other than to the (new) reservation.
 */

/**
 * <code>Reservation</code> is the base for all reservation objects. It
 * implements a part of the <code>IReservation</code> interface and defines the
 * core functions expected by the kernel from all reservation classes. This is
 * an abstract class and is intended as a building block of higher-level
 * reservation classes.
 */
abstract class Reservation implements IKernelReservation {
    public static final String PropertyGuard = "ReservationGuard";
    public static final String PropertyExtended = "ReservationExtended";
    public static final String PropertySliceID = "ReservationSliceID";
    public static final String PropertyPreviousTerm = "ReservationPreviousTerm";
    public static final String PropertyRequestedTerm = "ReservationRequestedTerm";
    public static final String PropertyApprovedTerm = "ReservationApprovedTerm";
    public static final String PropertyRequestedResources = "ReservationRequestedResources";
    public static final String PropertyApprovedResources = "ReservationApprovedResources";
    public static final String PropertyRenewable = "ReservationRenewable";
    public static final String PropertyProperties = "ReservationProperties";
    public static final String PropertyError = "ReservationError";

    /**
     * Cached slice name. Necessary so that we can obtain the slice for
     * reservations that have not been fully recovered.
     */
    @Persistent(key = PropertySlice)
    protected String slicename;

    /**
     * Cached slice id. Necessary so that we can obtain the slice for
     * reservations that have not been fully recovered.
     */
    @Persistent(key = PropertySliceID)
    protected SliceID sliceID;

    /**
     * The unique reservation identifier.
     */
    @Persistent(key = PropertyID)
    protected ReservationID rid;

    /**
     * Reservation category. Subclasses should supply the correct value.
     */
    @Persistent(key = PropertyCategory)
    protected int category = CategoryAll;

    /**
     * Reservation state.
     */
    @Persistent(key = PropertyState)
    protected int state;

    /**
     * Reservation pending state.
     */
    @Persistent(key = PropertyPending)
    protected int pending;

    /**
     * Access control monitor
     */
    @Persistent(key = PropertyGuard)
    protected Guard guard;

    /**
     * Has this reservation ever been extended?
     */
    @Persistent(key = PropertyExtended)
    protected boolean extended = false;

    /**
     * The current resources associated with this reservation.
     */
    @Persistent(key = PropertyResources)
    protected ResourceSet resources;

    /**
     * Resources representing the last request issued/received for this
     * reservation.
     */
    @Persistent(key = PropertyRequestedResources)
    protected ResourceSet requestedResources;

    /**
     * Resources approved by the policy for this reservation. This resource set
     * can be different from what was initially requested (requestedResources)
     * Eventually, resources will be merged with approvedResources.
     */
    @Persistent(key = PropertyApprovedResources)
    protected ResourceSet approvedResources;

    /**
     * The current term of the reservation.
     */
    @Persistent(key = PropertyTerm)
    protected Term term;

    /**
     * The previous term of the reservation.
     */
    @Persistent(key = PropertyPreviousTerm)
    protected Term previousTerm;

    /**
     * The term of the last request issued/received for this reservation.
     */
    @Persistent(key = PropertyRequestedTerm)
    protected Term requestedTerm;

    /**
     * The term the policy approved for this reservation. This term can be
     * different from what was initially requested (requestedTerm). Eventually,
     * term will be set to equal approvedTerm.
     */
    @Persistent(key = PropertyApprovedTerm)
    protected Term approvedTerm;

    /**
     * True if this is a renewable reservation. By default, reservations are not
     * renewable.
     */
    @Persistent(key = PropertyRenewable)
    protected boolean renewable;

    /**
     * Last error message.
     */
    @Persistent(key = PropertyError)
    protected String errMsg;

    /*
     * Fields that are references to other objects and filled in during recovery
     */

    /**
     * Cached pointer to the actor that operates on this reservation.
     */
    @Persistent(reference = true)
    protected IActor actor;

    /**
     * Logger.
     */
    @Persistent(reference = true)
    protected Logger logger;

    /**
     * Slice this reservation belongs to.
     */
    @Persistent(reference = true)
    protected IKernelSlice slice;

    /*
     * Fields that do not have to be persisted.
     */

    /**
     * Indicates if the policy plugin has made a decision about this reservation
     */
    @NotPersistent
    protected boolean approved = false;

    /**
     * The resources assigned to the reservation before the last update.
     */
    @NotPersistent
    protected ResourceSet previousResources;

    /**
     * Is an allocation process in progress?
     */
    @NotPersistent
    protected boolean bidPending;

    /**
     * Dirty flag. Indicates that the state of the reservation object has
     * changed since the last time it was persisted. Currently only transition
     * updates the dirty flag
     */
    @NotPersistent
    protected boolean dirty;

    /**
     * True if this reservation is expired. Used during recovery.
     */
    @NotPersistent
    protected boolean expired = false;

    /**
     * Recovery flag.
     * FIXME: not sure how it is used and whether it is really necessary.
     */
    @NotPersistent
    protected boolean pendingRecover = false;

    /**
     * True if the last state transition is not committed to external storage.
     * false otherwise.
     */
    @NotPersistent
    protected boolean stateTransition = false;

    /**
     * Scratch element to trigger post-actions on a probe.
     */
    @NotPersistent
    protected volatile int servicePending = ReservationStates.None;

    /**
     * Creates an empty instance. Used during recovery.
     */
    protected Reservation() {
        this(null, null, null, null);
    }

    /**
     * Creates a new instance with the given reservation identifier.
     * 
     * @param rid
     *            reservation identifier
     */
    protected Reservation(final ReservationID rid) {
        this(rid, null, null, null);
    }

    /**
     * Creates a new instance.
     * 
     * @param rid
     *            reservation identifier to use
     * @param resources
     *            resource specification
     * @param term
     *            term for the reservation
     * @param slice
     *            slice for the reservation
     */
    protected Reservation(final ReservationID rid, final ResourceSet resources, final Term term,
            IKernelSlice slice) {
        this.state = ReservationStates.Nascent;
        this.pending = ReservationStates.None;
        this.bidPending = false;
        this.dirty = false;
        this.extended = false;
        this.rid = rid;
        this.term = term;
        this.resources = resources;
        this.guard = new Guard();

        setSliceProperly(slice);
    }

    private void setSliceProperly(IKernelSlice slice) {
        if (slice != null) {
            this.slice = slice;
            this.slicename = slice.getName();
            this.sliceID = slice.getSliceID();
        }
    }

    /**
     * Creates a new reservation instance. Generates a new reservation
     * identifier.
     * 
     * @param resources
     *            resource specification
     * @param term
     *            term for the reservation
     * @param slice
     *            slice for the reservation
     */
    protected Reservation(final ResourceSet resources, final Term term, final IKernelSlice slice) {
        this(new ReservationID(), resources, term, slice);
    }

    /**
     * {@inheritDoc}
     */
    public boolean canRedeem() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canRenew() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void claim() throws Exception {
        internalError("abstract claim trap");
    }

    /**
     * {@inheritDoc}
     */
    public void clearDirty() {
        dirty = false;
        stateTransition = false;
    }

    /**
     * Clears all event notices associated with the reservation.
     */
    protected void clearNotice() {
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
    }

    /**
     * Logs an error and throws an exception.
     * 
     * @param err
     *            error message
     * 
     * @throws Exception
     */
    protected void error(final String err) throws OrcaException {
        logger.error("error for reservation: " + this + ": " + err);
        throw new OrcaException(err);
    }

    
    /**
     * {@inheritDoc}
     */
    public void extendLease() throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void modifyLease() throws Exception {
    }
    
    /**
     * {@inheritDoc}
     */
    public void extendTicket(final IActor actor) throws Exception {
        internalError("abstract extendTicket trap");
    }

    /**
     * {@inheritDoc}
     */
    public void fail(final String message) {
        errMsg = message;
        bidPending = false;
        transition(message, ReservationStates.Failed, ReservationStates.None);
        String s = "reservation has failed: " + message + ": [" + toString() + "]";
        logError(s);
    }

    /**
     * {@inheritDoc}
     */
    public void fail(final String message, final Exception e) {
        errMsg = message + ", message=" + e.getMessage() + ", stack="
                + ExceptionUtils.getStackTraceString(e.getStackTrace());
        bidPending = false;
        transition(message, ReservationStates.Failed, ReservationStates.None);
        logger.error(message, e);
    }

    /**
     * {@inheritDoc}
     */
    public void failWarn(final String message) {
        errMsg = message;

        bidPending = false;
        transition(message, ReservationStates.Failed, ReservationStates.None);

        String s = "reservation has failed: " + message + ": [" + toString() + "]";
        logWarning(s);
    }

    /**
     * {@inheritDoc}
     */
    public IActor getActor() {
        return this.actor;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceSet getApprovedResources() {
        return this.approvedResources;
    }

    /**
     * {@inheritDoc}
     */
    public Term getApprovedTerm() {
        return approvedTerm;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceType getApprovedType() {
        if (approvedResources != null) {
            return approvedResources.getType();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public int getApprovedUnits() {
        if (approvedResources == null) {
            return 0;
        } else {
            return approvedResources.getUnits();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getCategory() {
        return category;
    }

    /**
     * Returns the guard associated with the reservation.
     * 
     * @return access control monitor
     */
    protected Guard getGuard() {
        return guard;
    }

    /**
     * {@inheritDoc}
     */
    public IKernelSlice getKernelSlice() {
        return slice;
    }

    /**
     * {@inheritDoc}
     */
    public int getLeasedAbstractUnits() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getLeasedUnits() {
        return 0;
    }

    /**
     * Returns a descriptive string if this reservation requires attention, else
     * null.
     * 
     * @return notices string
     */
    public String getNotices() {
        String msg = "Reservation " + rid + " (Slice " + getSliceName() + ") is in state " + "["
                + ReservationStates.states[state] + "," + ReservationStates.pendings[pending] + "]";

        if (errMsg != null && !errMsg.equals("")) {
            msg += ", err=" + errMsg;
        }
        return msg;
    }

    /**
     * {@inheritDoc}
     */
    public int getPendingState() {
        return pending;
    }

    /**
     * {@inheritDoc}
     */
    public String getPendingStateName() {
        return ReservationStates.getPendingName(pending);
    }

    /**
     * {@inheritDoc}
     */
    public ResourceSet getPreviousResources() {
        return previousResources;
    }

    /**
     * {@inheritDoc}
     */
    public Term getPreviousTerm() {
        return previousTerm;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceSet getRequestedResources() {
        return this.requestedResources;
    }

    /**
     * {@inheritDoc}
     */
    public Term getRequestedTerm() {
        return requestedTerm;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceType getRequestedType() {
        if (requestedResources == null) {
            return null;
        } else {
            return requestedResources.getType();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getRequestedUnits() {
        if (requestedResources == null) {
            return 0;
        } else {
            return requestedResources.getUnits();
        }
    }

    /**
     * {@inheritDoc}
     */
    public ReservationID getReservationID() {
        return rid;
    }

    public ID getReference() {
        return rid;
    }

    /**
     * {@inheritDoc}
     */
    public ReservationState getReservationState() {
        return new ReservationState(this.state, this.pending);
    }

    /**
     * {@inheritDoc}
     */
    public ResourceSet getResources() {
        return resources;
    }

    /**
     * {@inheritDoc}
     */
    public ISlice getSlice() {
        return slice;
    }

    /**
     * {@inheritDoc}
     */
    public SliceID getSliceID() {
        SliceID result = null;

        if (slice != null) {
            result = slice.getSliceID();
        } else {
            result = sliceID;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String getSliceName() {
        String result = null;

        if (slice != null) {
            result = slice.getName();
        } else {
            result = slicename;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public int getState() {
        return state;
    }

    /**
     * {@inheritDoc}
     */
    public String getStateName() {
        return ReservationStates.getStateName(state);
    }

    /**
     * {@inheritDoc}
     */
    public Term getTerm() {
        return term;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceType getType() {
        if (resources != null) {
            return resources.getType();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public int getUnits() {
        if (resources == null) {
            return 0;
        } else {
            return resources.getUnits();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getUnits(final Date time) {
        int hold = 0;

        if (!isTerminal() && (term != null) && term.contains(time)) {
            hold = resources.getConcreteUnits(time);
        }

        return hold;
    }

    /**
     * {@inheritDoc}
     */
    public void handleDuplicateRequest(final int operation) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasUncommittedTransition() {
        return stateTransition;
    }

    /**
     * Logs the specified error and throws an exception.
     * 
     * @param err
     *            error message
     * 
     * @throws Exception
     */
    protected void internalError(final String err) throws OrcaException {
        logger.error("internal error for reservation: " + this + ": " + err);
        throw new OrcaException("internal error: " + err);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isActive() {
        return ((state == ReservationStates.Active) || (state == ReservationStates.ActiveTicketed));
    }

    /**
     * {@inheritDoc}
     */
    public boolean isActiveTicketed() {
        return (state == ReservationStates.ActiveTicketed);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isApproved() {
        return approved;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isBidPending() {
        return bidPending;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClosed() {
        return (state == ReservationStates.Closed);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClosing() {
        return ((pending == ReservationStates.Closing) || (state == ReservationStates.CloseWait));
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExpired() {
        return expired;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExpired(final Date time) {
        if (term == null) {
            return true;
        } else {
            /*
             * Should expire with some configurable grace period per reservation
             * or per concrete set.
             */
            return term.expired(time);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExtended() {
        return extended;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExtendingLease() {
        return (pending == ReservationStates.ExtendingLease);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExtendingTicket() {
        return (pending == ReservationStates.ExtendingTicket);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFailed() {
        return (state == ReservationStates.Failed);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNascent() {
        return (state == ReservationStates.Nascent);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNoPending() {
        return (pending == ReservationStates.None);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPendingRecover() {
        return this.pendingRecover;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPriming() {
        return (pending == ReservationStates.Priming);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRedeeming() {
        return (pending == ReservationStates.Redeeming);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRenewable() {
        return renewable;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTerminal() {
        return isClosed() || isClosing() || isFailed();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTicketed() {
        return (state == ReservationStates.Ticketed);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTicketing() {
        return (pending == ReservationStates.Ticketing);
    }

    
    /**
     * Logs an error.
     * 
     * @param err
     *            error message
     */
    protected void logError(final String message) {
        logger.warn("reservation #" + rid.toHashString() + ": " + message);
    }

    /**
     * Logs an error.
     * 
     * @param err
     *            error message
     */
    protected void logError(final String message, Exception e) {
        logger.warn("reservation #" + rid.toHashString() + ": " + message, e);
    }

    /**
     * Logs an exception related to the reservation.
     * 
     * @param err
     *            error message
     * @param e
     *            exception
     */
    protected void logException(final String err, final Exception e) {
        logError(err, e);
    }

    /**
     * Logs an error that occurred on a peer.
     * 
     * @param err
     *            error message
     */
    protected void logRemoteError(final String err, Exception e) {
        logException("remote error: " + err, e);
    }

    /**
     * Logs a warning about the reservation.
     * 
     * @param err
     *            error message
     */
    protected void logWarning(final String message) {
        logger.warn("reservation #" + rid.toHashString() + ": " + message);
    }

    protected void logDebug(final String message) {
        if (logger.isDebugEnabled()) {
            logger.debug("reservation #" + rid.toHashString() + ": " + message);
        }
    }

    protected void logInfo(final String message) {
        if (logger.isInfoEnabled()) {
            logger.info("reservation #" + rid.toHashString() + ": " + message);
        }
    }

    
    /**
     * Ensures the reservation does not have a pending operation.
     * 
     * @throws Exception
     *             if the reservation has a pending operation.
     */
    protected void nothingPending() throws Exception {
        if (pending != ReservationStates.None) {
            error("reservation has a pending operation");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void prepareProbe() throws Exception {
        // default no-op
    }

    /**
     * {@inheritDoc}
     */
    public void probePending() throws Exception {
        // default no-op
    }

    /**
     * An incoming client request named this validated Reservation object for an
     * existing reservation. Check to be sure that it has not been destroyed in
     * a race since the validate.
     * 
     * @throws Exception
     *             thrown if the state is closed or failed
     */
    protected void ready() throws Exception {
        if ((state == ReservationStates.Closed) || (state == ReservationStates.Failed)) {
            error("invalid Reservation");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void reserve(final IPolicy policy) throws Exception {
    }

    // FIXME: [recovery] a hack for now. Can this be done in a generic way?
    public void setup() {
        if (resources != null) {
            resources.setup(this);
        }

        if (approvedResources != null) {
            approvedResources.setup(this);
        }

        if (requestedResources != null) {
            requestedResources.setup(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Properties saveID() throws Exception {
        Properties p = new Properties();

        PropList.setProperty(p, PropertyID, rid.toString());
        p.setProperty(IReservation.PropertySlice, slice.getName());

        return p;
    }

    /**
     * {@inheritDoc}
     */
    public void serviceClaim() throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void serviceClose() {
        // default no-op
    }

    /**
     * {@inheritDoc}
     */
    public void serviceExtendLease() throws Exception {
        // default no-op
    }

    /**
     * {@inheritDoc}
     */
    public void serviceModifyLease() throws Exception {
        // default no-op
    }    
    
    /**
     * {@inheritDoc}
     */
    public void serviceExtendTicket() throws Exception {
        // default no-op
    }

    /**
     * {@inheritDoc}
     */
    public void serviceProbe() throws Exception {
        // default no-op
    }

    /**
     * {@inheritDoc}
     */
    public void serviceReserve() throws Exception {
        // default no-op
    }

    /**
     * {@inheritDoc}
     */
    public void serviceUpdateLease() throws Exception {
        // default no-op
    }

    /**
     * {@inheritDoc}
     */
    public void serviceUpdateTicket() throws Exception {
        // default no-op
    }

    /**
     * {@inheritDoc}
     */
    public void setActor(final IActor actor) {
        this.actor = actor;
    }

    /**
     * {@inheritDoc}
     */
    public void setApproved() {
        this.approved = true;
    }

    /**
     * {@inheritDoc}
     */
    public void setApproved(final Term approvedTerm, final ResourceSet approvedResources) {
        this.approvedTerm = approvedTerm;
        this.approvedResources = approvedResources;
        this.approved = true;
    }

    /**
     * {@inheritDoc}
     */
    public void setApprovedResources(final ResourceSet approvedResources) {
        this.approvedResources = approvedResources;
    }

    /**
     * {@inheritDoc}
     */
    public void setApprovedTerm(final Term term) {
        this.approvedTerm = term;
    }

    /**
     * {@inheritDoc}
     */
    public void setBidPending(final boolean inbid) {
        bidPending = inbid;
    }

    /**
     * {@inheritDoc}
     */
    public void setDirty() {
        dirty = true;
    }

    /**
     * {@inheritDoc}
     */
    public void setExpired(final boolean value) {
        this.expired = value;
    }

    /**
     * {@inheritDoc}
     */
    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    /**
     * {@inheritDoc}
     */
    public void setPendingRecover(final boolean pendingRecover) {
        this.pendingRecover = pendingRecover;
    }

    /**
     * {@inheritDoc}
     */
    public void setServicePending(int code) {
        this.servicePending = code;
    }

    /**
     * {@inheritDoc}
     */
    public void setSlice(final ISlice slice) {
        setSliceProperly((IKernelSlice) slice);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("res: ");

        if (rid != null) {
            sb.append("#" + rid + " ");
        }

        if (slice != null) {
            sb.append("slice: " + slice.getName() + " ");
        }

        sb.append(ReservationStates.states[state] + " " + ReservationStates.pendings[pending] + " ");

        if (resources != null) {
            sb.append(resources);
        }

        if (term != null) {
            sb.append(" " + term.toString());
        }

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public void transition(final String prefix, final int state, final int pending) {
        if (state == ReservationStates.Failed) {
            logger.debug("failed");
        }

        if ((logger != null) && logger.isDebugEnabled()) {
            logger.debug("Reservation #" + rid.toHashString() + " " + prefix + " transition: "
                    + ReservationStates.states[this.state] + "->" + ReservationStates.states[state]
                    + ", " + ReservationStates.pendings[this.pending] + "->"
                    + ReservationStates.pendings[pending]);
        }

        this.state = state;
        this.pending = pending;

        // dispatch the state transition to whoever is interested
        if (actor != null) {
            Globals.eventManager.dispatchEvent(new ReservationStateTransitionEvent(this,
                    getReservationState()));
        }

        dirty = true;
        stateTransition = true;
    }

    /**
     * {@inheritDoc}
     */
    public void updateLease(final IReservation rarg, final UpdateData udd) throws Exception {
        internalError("abstract updateLease trap");
    }

    /**
     * {@inheritDoc}
     */
    public void updateTicket(final IReservation rarg, final UpdateData udd) throws Exception {
        internalError("abstract updateTicket trap");
    }

    /**
     * Validates the reservation. For use by prepare() methods defined by
     * subclasses.
     * 
     * @throws Exception
     */
    protected void validate() throws Exception {
        assert state == ReservationStates.Nascent;
        nothingPending();

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
    }

    /**
     * {@inheritDoc}
     */
    public void validateIncoming() throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void validateOutgoing() throws Exception {
    }

    public void setLocalProperty(String key, String value) {
        resources.getLocalProperties().setProperty(key, value);
    }

    public String getLocalProperty(String key) {
        return resources.getLocalProperties().getProperty(key);
    }

    public String toLogString() {
        StringBuffer sb = new StringBuffer();
        sb.append("res: ");

        if (rid != null) {
            sb.append(rid.toHashString() + " ");
        }

        if (slice != null) {
            sb.append("slice: " + slice.getName() + " ");
        }

        sb.append(ReservationStates.states[state] + " " + ReservationStates.pendings[pending] + " ");

        if (resources != null) {
            sb.append(resources);
        }

        if (term != null) {
            sb.append(" " + term.toString());
        }

        return sb.toString();
    }

    /**
     * Helper class for counting units.
     */
    protected class CountHelper {
        public int pending = 0;
        public int active = 0;
        public ResourceType type;
    }

}