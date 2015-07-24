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

import orca.security.AuthToken;
import orca.shirako.api.ICallbackProxy;
import orca.shirako.api.IPolicy;
import orca.shirako.api.IReservation;
import orca.shirako.common.ReservationID;
import orca.shirako.time.Term;
import orca.shirako.util.RPCError;
import orca.shirako.util.ResourceCount;
import orca.shirako.util.UpdateData;
import orca.util.ResourceType;
import orca.util.persistence.Persistent;

import org.apache.log4j.Logger;

/*
 * Implementation note on error handling. There are several kinds of errors,
 * all of which are logged: - Error on an incoming operation, caught with no
 * side effects: these are reflected as exceptions. - Reservation failure.
 * Mark reservation as failed, report failure in next update, accept no
 * further operations except probe and close, close when ready. - Resource
 * failure: log as an event, report notification in next update, release
 * failed resources, leave reservation state unchanged, complete operations
 * with reduced resource set. Someday obtain new resources to fill the
 * deficit. - Probe errors. Log and report. For externally initiated probes,
 * always send a lease update. - Asserts. Throw exception if asserts are
 * enabled. Errors should generally not throw exceptions up into the calling
 * code, with a few exceptions: asserts, and incoming call errors with no
 * side effects. Should update exception signatures on all reservation
 * classes to reflect this convention.
 */
abstract class ReservationServer extends Reservation implements IKernelServerReservation {
    public static final String PropertyCallback = "ReservationServerCallback";
    public static final String PropertyOwner = "ReservationServerOwner";
    public static final String PropertyClient = "ReservationServerClient";
    public static final String PropertyUpdateData = "ReservationServerUpdateData";
    public static final String PropertyUpdateCount = "ReservationServerUpdateCount";
    public static final String PropertySequenceNumberIn = "ReservationServerSequenceIn";
    public static final String PropertySequenceNumberOut = "ReservationServerSequenceOut";

    /**
     * Sequence number for incoming messages.
     */
    @Persistent(key = PropertySequenceNumberIn)
    protected int sequenceIn = 0;

    /**
     * Sequence number for outgoing messages.
     */
    @Persistent(key = PropertySequenceNumberOut)
    protected int sequenceOut = 0;

    /**
     * Callback proxy.
     */
    @Persistent(key = PropertyCallback)
    protected ICallbackProxy callback;

    /**
     * Status of the last server-side operation for the reservation.
     */
    @Persistent(key = PropertyUpdateData)
    protected UpdateData udd;

    /**
     * How many update messages have been sent to the client.
     */
    @Persistent(key = PropertyUpdateCount)
    protected int updateCount;

    /**
     * Identity of the client actor.
     */
    @Persistent(key = PropertyClient)
    protected AuthToken client;

    /**
     * Identity of the server actor.
     */
    @Persistent(key = PropertyOwner, reference = true)
    protected AuthToken owner;

    /**
     * Policy in control of the reservation.
     */
    @Persistent(reference = true)
    protected IPolicy policy;

    /**
     * Creates a new blank reservation instance. Generates a new identifier.
     */
    protected ReservationServer() {
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
     *            slice for the reservation
     */
    protected ReservationServer(final ReservationID rid, final ResourceSet resources,
            final Term term, final IKernelSlice slice) {
        super(rid, null, null, slice);

        udd = new UpdateData();
        updateCount = 0;
        servicePending = ReservationStates.None;
        approved = false;

        this.requestedResources = resources;
        this.requestedTerm = term;
    }

    /**
     * Creates a new instance. Generates a new identifier.
     * 
     * @param resources
     *            requested resources
     * @param term
     *            requested term
     * @param slice
     *            slice for the reservation
     */
    protected ReservationServer(final ResourceSet resources, final Term term,
            final IKernelSlice slice) {
        this(new ReservationID(), resources, term, slice);
    }

    /**
     * Attaches state to a new reservation.
     * 
     * @param callback
     *            client callback
     * @param logger
     *            logger
     * 
     * @throws Exception
     */
    public void prepare(ICallbackProxy callback, Logger logger) throws Exception {
        internalError("abstract method");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateIncoming() throws Exception {
        if (slice == null) {
            error("Missing slice");
        }

        if (requestedResources == null) {
            error("Missing resource set");
        }

        if (requestedTerm == null) {
            error("Missing term");
        }

        requestedResources.validateIncoming();
        requestedTerm.validate();
    }
    

    /**
     * Checks reservation state prior to handling an incoming request. These
     * checks are not applied to probes or closes.
     * 
     * @throws Exception
     */
    protected void incomingRequest() throws Exception {
        assert slice != null;

        /*
         * Disallow a request on a failed reservation, but always send an update
         * to reset the client.
         */
        if (isFailed()) {
            generateUpdate();
            error("server cannot satisfy request (marked failed)");
        }

        /*
         * Disallow any further requests on a closing reservation. Generate and
         * update to reset the client.
         */
        if (isClosed() || (pending == ReservationStates.Closing)) {
            generateUpdate();
            error("server cannot satisfy request (closing)");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateLease(final IReservation incoming, final UpdateData udd) throws Exception {
        internalError("Cannot update a server-side reservation");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateTicket(final IReservation incoming, final UpdateData udd) throws Exception {
        internalError("Cannot update a server-side reservation");
    }

    public void handleFailedRPC(FailedRPC rpc) {
        // If the error is in the network: keep retrying if possible
        if (rpc.getErrorType() == RPCError.NetworkError) {
            // do not retry if failed|closed
            if (isFailed() || isClosed()) {
                return;
            }
            // the reservation is still viable. Retry the RPC.
            assert rpc.hasRequest();
            RPCManager.retry(rpc.getRequest());
            return;
        }

        // non-recoverable failure: fail the reservation
        // FIXME: maybe too aggressive?
        fail("Failing reservation due to non-recoverable RPC error (" + rpc.getErrorType() + ")",
                rpc.getError());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void clearNotice() {
        udd.clear();
    }

    /**
     * Counts the number of active and pending units in the reservation at the
     * given time instance.
     * 
     * @param when
     *            time instance
     * 
     * @return counter of active and pending units
     */
    protected CountHelper count(final Date when) {
        CountHelper c = new CountHelper();

        if (isTerminal()) {
            // nothing to report
        } else {
            if ((term != null) && term.contains(when) && (resources != null)) {
                c.active = resources.getConcreteUnits(when);
                c.type = resources.type;
            } else {
                if (approved && (approvedTerm != null) && (approvedResources != null)) {
                    if (approvedTerm.contains(when)) {
                        c.active = approvedResources.units;
                        c.type = approvedResources.type;
                    }
                } else {
                    if ((requestedTerm != null) && requestedTerm.contains(when)
                            && (requestedResources != null)) {
                        c.pending = requestedResources.units;
                        c.type = requestedResources.type;
                    }
                }
            }
        }

        return c;
    }

    /**
     * {@inheritDoc}
     */
    public void count(final ResourceCount rc, final Date when) {
        switch (state) {
        case ReservationStates.Nascent:
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
                rc.tallyClose(resources.type, resources.getUnits());
            }
            break;

        case ReservationStates.Failed:
            if (resources != null) {
                rc.tallyFailed(resources.type, resources.getUnits());
            }
            break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fail(final String message) {
        // This results in errors on AM printed twice: once from udd, once from
        // errmsg in reservation. Not easily fixable because both udd and errmsg field are used
        // sometimes independently /ib 08/16/2013
        udd.error(message);
        super.fail(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fail(final String message, final Exception e) {
        // This results in errors on AM printed twice: once from udd, once from
        // errmsg in reservation. Not easily fixable because both udd and errmsg field are used
        // sometimes independently /ib 08/16/2013
        udd.error(message);
        super.fail(message, e);
    }

    /**
     * Reports an operation failure and notifies the client. If this is an
     * extend, then the resources in the update may be a copy of the previous
     * ticket or lease, which may still be valid. In any case, there is no need
     * to close: the client will do it, or the reservation expires normally.
     * 
     * @param message
     *            the error message
     */
    protected void failNotify(final String message) {
        fail(message);
        generateUpdate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void failWarn(final String message) {
        udd.error(message);
        super.failWarn(message);
    }

    /**
     * Generates an update to the callback object (if any) for this reservation.
     */
    protected abstract void generateUpdate();

    /**
     * {@inheritDoc}
     */
    public ICallbackProxy getCallback() {
        return callback;
    }

    /**
     * {@inheritDoc}
     */
    public AuthToken getClientAuthToken() {
        if (callback != null) {
            return callback.getIdentity();
        } else {
            return client;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getSequenceIn() {
        return sequenceIn;
    }

    /**
     * {@inheritDoc}
     */
    public int getSequenceOut() {
        return sequenceOut;
    }

    /**
     * {@inheritDoc}
     */
    public AuthToken getServerAuthToken() {
        return owner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceType getType() {
        // XXX: this seems wrong. stick to the base definition
        if (resources != null) {
            return resources.type;
        } else {
            if (requestedResources != null) {
                return requestedResources.type;
            }
        }

        return null;
    }

    public UpdateData getUpdateData() {
        UpdateData result = new UpdateData();
        result.absorb(udd);
        return result;
    }

    @Override
    public String getNotices() {
        String s = super.getNotices();
        String notices = udd.getEvents();

        if (notices != null) {
            s = s + "\n" + notices;
        }

        notices = udd.getMessage();
        if (notices != null) {
            s += "\n" + notices;
        }

        return s;
    }

    /**
     * {@inheritDoc}
     */
    public void setOwner(final AuthToken owner) {
        this.owner = owner;
    }

    /**
     * {@inheritDoc}
     */
    public void setRequestedResources(final ResourceSet set) {
        this.requestedResources = set;
    }

    /**
     * {@inheritDoc}
     */
    public void setRequestedTerm(final Term term) {
        this.requestedTerm = term;
    }

    /**
     * {@inheritDoc}
     */
    public void setSequenceIn(final int sequence) {
        this.sequenceIn = sequence;
    }

    /**
     * {@inheritDoc}
     */
    public void setSequenceOut(final int sequence) {
        this.sequenceOut = sequence;
    }
}