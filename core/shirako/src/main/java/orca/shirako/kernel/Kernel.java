/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.kernel;

import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import orca.security.AuthToken;
import orca.shirako.api.IPolicy;
import orca.shirako.api.IReservation;
import orca.shirako.api.IShirakoPlugin;
import orca.shirako.api.ISlice;
import orca.shirako.common.Constants;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.container.Globals;
import orca.shirako.time.Term;
import orca.shirako.util.ReservationSet;
import orca.shirako.util.ResourceData;
import orca.shirako.util.TestException;
import orca.shirako.util.UpdateData;
import orca.util.OrcaException;

import org.apache.log4j.Logger;

class Kernel
{
    /**
     * The shirako plugin.
     */
    private IShirakoPlugin plugin;

    /**
     * The policy.
     */
    private IPolicy policy;

    /**
     * The logger.
     */
    private Logger logger;

    /**
     * All slices managed by the kernel.
     */
    private SliceTable2 slices;

    /**
     * All reservations managed by the kernel.
     */
    private ReservationSet reservations;

    /**
     * Creates a new kernel instance.
     * @param plugin Shirako plugin instance
     * @param mapper policy
     * @param logger logger
     */
    public Kernel(final IShirakoPlugin plugin, final IPolicy mapper, final Logger logger)
    {
        this.plugin = plugin;
        this.policy = mapper;
        this.logger = logger;

        slices = new SliceTable2();
        reservations = new ReservationSet();
    }

    /**
     * Amends a previous reserve operation (both client and server side) for the
     * reservation.
     * @param reservation reservation
     * @throws Exception
     * @see #reserve(Reservation)
     */
    protected void amendReserve(final IKernelReservation reservation) throws Exception
    {
        try {
            reservation.reserve(policy);
            plugin.getDatabase().updateReservation(reservation);
            if (!reservation.isFailed()) {
                reservation.serviceReserve();
            }
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            error("An error occurred during amend reserve for reservation #" + reservation.getReservationID().toHashString(), e);
        }
    }

    /**
     * Processes a requests to claim new ticket for previously exported
     * resources (broker role). On the client side this request is issued by
     * {@link #reserve(Reservation)}.
     * @param reservation the reservation being claimed
     * @throws Exception
     */
    protected void claim(final IKernelBrokerReservation reservation) throws Exception
    {
        try {
            // Note: this call simply indicates that we
            // need to send a ticket update on the next probePending
            reservation.claim();
            // commit the reservation to the database so that we
            // remember the client proxy
            plugin.getDatabase().updateReservation(reservation);
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            error("An error occurred during claim for reservation #" + reservation.getReservationID().toHashString(), e);
        }
    }

    protected void fail(final IKernelReservation reservation, String message) throws Exception {
        if (!reservation.isFailed() && !reservation.isClosed()) {
            reservation.fail(message);
        }
        plugin.getDatabase().updateReservation(reservation);
    }
    
    /**
     * Handles a close operation for the reservation.
     * <p>
     * Client: perform local close operations and issue close request to
     * authority.
     * </p>
     * <p>
     * Broker: perform local close operations
     * </p>
     * <p>
     * Authority: process a close request
     * </p>
     * @param reservation reservation for which to perform close
     * @throws Exception
     */
    protected void close(final IKernelReservation reservation) throws Exception
    {
        try {
            if (!reservation.isClosed() && !reservation.isClosing()) {
            	// notify the policy that we are about to close the reservation
            	policy.close(reservation);
                // start closing the reservation
                reservation.close();
                // commit the state transition
                plugin.getDatabase().updateReservation(reservation);
                // issue the close actions
                reservation.serviceClose();
            }
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            error("An error occurred during close for reservation #" + reservation.getReservationID().toHashString(), e);
        }
    }

    /**
     * Compares the incoming request to the corresponding reservation stored at
     * this actor. First compares sequence numbers. If the incoming request has
     * a larger sequence number and there is no pending operation for this
     * reservation, we update the sequence number of the current reservation and
     * set the requestedTerm and requestedResources fields.
     * 
     * @param incoming the incoming request
     * @param current the corresponding reservation stored at the server
     * @return a comparison status flag (see Sequence*)
     */
    protected int compareAndUpdate(final IKernelServerReservation incoming, final IKernelServerReservation current)
    {
        int code = SequenceComparisonCodes.SequenceEqual;

        if (current.getSequenceIn() < incoming.getSequenceIn()) {
            if (current.isNoPending()) {
                // this is a valid request
                code = SequenceComparisonCodes.SequenceGreater;

                current.setSequenceIn(incoming.getSequenceIn());

                current.setRequestedResources(incoming.getRequestedResources());
                current.setRequestedTerm(incoming.getRequestedTerm());
            } else {
                code = SequenceComparisonCodes.SequenceInProgress;
            }
        } else {
            if (current.getSequenceIn() > incoming.getSequenceIn()) {
                code = SequenceComparisonCodes.SequenceSmaller;
            }
        }

        return code;
    }

    /**
     * Compares the sequence numbers of both reservations. Returns:
     * SequenceSmaller if (incoming < current) SequenceEqual if (incoming =
     * current) SequenceGreater if (incoming > current) If the result is
     * SequenceGreater, updates the sequence number of current to the one of
     * incoming
     */
    protected int compareAndUpdate(final ReservationClient incoming, final ReservationClient current, final boolean ticket)
    {
        int code = SequenceComparisonCodes.SequenceEqual;

        if (ticket) {
            if (current.getTicketSequenceIn() < incoming.getTicketSequenceIn()) {
                code = SequenceComparisonCodes.SequenceGreater;
                current.setTicketSequenceIn(incoming.getTicketSequenceIn());
            } else {
                if (current.getTicketSequenceIn() > incoming.getTicketSequenceIn()) {
                    code = SequenceComparisonCodes.SequenceSmaller;
                }
            }
        } else {
            if (current.getLeaseSequenceIn() < incoming.getLeaseSequenceIn()) {
                code = SequenceComparisonCodes.SequenceGreater;
                current.setLeaseSequenceIn(incoming.getLeaseSequenceIn());
            } else {
                if (current.getLeaseSequenceIn() > incoming.getLeaseSequenceIn()) {
                    code = SequenceComparisonCodes.SequenceSmaller;
                }
            }
        }

        return code;
    }

    /**
     * Similar to compareAndUpdate but will update the reservation even if there
     * is a pending operation currently in progress. Use with caution.
     * @see #compareAndUpdate(ReservationServer, ReservationServer)
     * @param incoming the incoming request
     * @param current the corresponding reservation stored at the server
     * @return a comparison status flag (see Sequence*)
     */
    protected int compareAndUpdateIgnorePending(final IKernelServerReservation incoming, final IKernelServerReservation current)
    {
        int code = SequenceComparisonCodes.SequenceEqual;

        if (current.getSequenceIn() < incoming.getSequenceIn()) {
            // this is a valid request
            code = SequenceComparisonCodes.SequenceGreater;
            current.setSequenceIn(incoming.getSequenceIn());
            current.setRequestedResources(incoming.getRequestedResources());
            current.setRequestedTerm(incoming.getRequestedTerm());
        } else {
            if (current.getSequenceIn() > incoming.getSequenceIn()) {
                code = SequenceComparisonCodes.SequenceSmaller;
            }
        }

        return code;
    }

    /**
     * Logs an error and throws an Exception
     * @param err error message
     */
    private void error(final String err)
    {
        logger.error("caller error: " + err);
        throw new RuntimeException(err);
    }

    /**
     * Logs the specified exception and re-throws it.
     * @param err error message
     * @param e exception
     */
    private void error(final String err, final Exception e)
    {
        logger.error(err, e);
        throw new RuntimeException(err, e);
    }

    /**
     * Handles an extend lease operation for the reservation.
     * <p>
     * Client: issue an extend lease request.
     * </p>
     * <p>
     * Authority: process a request for a lease extension.
     * </p>
     * @param reservation reservation for which to perform extend lease
     * @throws Exception
     */
    protected void extendLease(final IKernelReservation reservation) throws Exception
    {
        try {
            reservation.extendLease();
            plugin.getDatabase().updateReservation(reservation);
            if (!reservation.isFailed()) {
                reservation.serviceExtendLease();
            }
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            error("An error occurred during extend lease for reservation #" + reservation.getReservationID().toHashString(), e);
        }
    }

    /**
     * Handles a modify lease operation for the reservation.
     * <p>
     * Client: issue a modify lease request.
     * </p>
     * <p>
     * Authority: process a request for a modifying a lease.
     * </p>
     * @param reservation reservation for which to perform modify lease
     * @throws Exception
     */
    protected void modifyLease(final IKernelReservation reservation) throws Exception
    {
        try {
            reservation.modifyLease();
            plugin.getDatabase().updateReservation(reservation);
            if (!reservation.isFailed()) {
                reservation.serviceModifyLease();
            }
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            error("An error occurred during modifying lease for reservation #" + reservation.getReservationID().toHashString(), e);
        }
    }
    
    
    
    /**
     * Extends the reservation with the given resources and term.
     * @param reservation reservation to extend
     * @param resources resources to use for the extension
     * @param term term to use for the extension
     * @return 0 if the reservation extension operation can be initiated,
     *         {@link ImageManagerConstants.ReservationHasPendingOperation} if
     *         the reservation has a pending operation, which prevents the
     *         extend operation from being initiated.
     * @throws Exception
     */
    protected int extendReservation(final IKernelReservation reservation, final ResourceSet resources, final Term term) throws Exception
    {
        return extendReservation(reservation.getReservationID(), resources, term);
    }

    /*
     * =======================================================================
     * Register/unregister/lookup for reservations
     * =======================================================================
     */

    /**
     * Extends the reservation with the given resources and term.
     * @param reservation reservation identifier of reservation to extend
     * @param resources resources to use for the extension
     * @param term term to use for the extension
     * @return 0 if the reservation extension operation can be initiated,
     *         {@link IConstants.ReservationHasPendingOperation} if the
     *         reservation has a pending operation, which prevents the extend
     *         operation from being initiated.
     * @throws Exception
     */
    protected int extendReservation(final ReservationID rid, final ResourceSet rset, final Term term) throws Exception
    {
        IKernelReservation real = null;
        boolean ticket = true;

        /* check if this reservation is managed by us */
        real = (IKernelReservation) reservations.get(rid);
        
        if (real == null) {
            throw new RuntimeException("Unknown reservation: rid=" + rid);
        }

        /*
         * check for a pending operation: we cannot service the extend if
         * there is another operation in progress.
         */
        if (real.getPendingState() != ReservationStates.None) {
            return Constants.ReservationHasPendingOperation;
        }

        /* attach the desired extension term and resource set */
        real.setApproved(term, rset);
        // XXX: we may need to set/unset bid pending
        /* notify the policy that a reservation is about to be extended */
        policy.extend(real, rset, term);

        /* determine if it's a ticket or a lease extension */
        if (real instanceof AuthorityReservation) {
            ticket = false;
        }

        /* trigger the operation */
        if (ticket) {
            real.extendTicket(plugin.getActor());
        } else {
            real.extendLease();
        }

        /* update the database */
        plugin.getDatabase().updateReservation(real);

        /* check if the operation has completed */
        if (!real.isFailed()) {
            if (ticket) {
                real.serviceExtendTicket();
            } else {
                real.serviceExtendLease();
            }
        }

        return 0;
    }

    /**
     * Handles an extend ticket operation for the reservation.
     * <p>
     * Client: issue an extend ticket request.
     * </p>
     * <p>
     * Broker: process a request for a ticket extension.
     * </p>
     * @param reservation reservation for which to perform extend ticket
     * @throws Exception
     */
    protected void extendTicket(final IKernelReservation reservation) throws Exception
    {
        try {
            if (reservation.canRenew()) {
                reservation.extendTicket(plugin.getActor());
            } else {
                /*
                 * XXX: For now we will throw an exception. It may be better
                 * to change the method signature and return an exit code
                 * instead.
                 */
                throw new RuntimeException("The reservation state prevents it from extending its ticket.");
            }

            plugin.getDatabase().updateReservation(reservation);

            if (!reservation.isFailed()) {
                reservation.serviceExtendTicket();
            }
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            error("An error occurred during extend ticket for reservation #" + reservation.getReservationID().toHashString(), e);
        }
    }
    
    /**
     * Returns all client slices.
     * @return an array of client slices
     */
    public IKernelSlice[] getClientSlices()
    {
        return slices.getClientSlices();
    }

    /**
     * Returns all inventory slices.
     * @return an array of inventory slices
     */
    public IKernelSlice[] getInventorySlices()
    {
        return slices.getInventorySlices();
    }

    /**
     * Returns the slice object registered with the kernel that corresponds to
     * the argument.
     * @param slice incoming slice object
     * @return the locally registered slice object
     * @throws IllegalArgumentException if the arguments are invalid
     * @throws Exception if no locally registered slice object exists
     */
    protected IKernelSlice getLocalSlice(final ISlice slice) throws Exception
    {
        if ((slice == null) || (slice.getSliceID() == null)) {
            throw new IllegalArgumentException();
        }

        return slices.getException(slice.getSliceID());
    }

    /**
     * Returns the slice specified in the reservation or creates a new slice
     * with the given parameters. Newly created slices are registered with the
     * kernel.
     * @param identity actor identity
     * @param name slice name
     * @param resourceData slice parameters
     * @param agentClient true if this slice represents an broker acting as a
     *            client
     * @param other additional parameters
     * @return the slice object
     * @throws Exception
     */
    protected IKernelSlice getOrCreateLocalSlice(final AuthToken identity, final IKernelReservation reservation, final boolean createNewSlice) throws Exception
    {
        IKernelSlice result = null;

        String sliceName = reservation.getSlice().getName();
        SliceID sliceID = reservation.getSlice().getSliceID();

         /*
         * Try to obtain the slice.
         */
        result = getSlice(sliceID);

        if (result == null) {
            /* No such slice: must create it or take it from the reservation */
            if (createNewSlice) {
                result = (IKernelSlice) plugin.createSlice(sliceID, sliceName, new ResourceData(), null);

                if (reservation.getSlice().isBrokerClient()) {
                    result.setBrokerClient();
                } else {
                    if (reservation.getSlice().isClient()) {
                        result.setClient();
                    }
                }
            } else {
                result = reservation.getKernelSlice();
            }

            result.setOwner(identity);
            registerSlice(result);
        }
        
        return result;
    }

    /**
     * Returns the specified reservation.
     * @param rid reservation id
     * @return reservation
     */
    public IKernelReservation getReservation(final ReservationID rid)
    {
        IKernelReservation result = null;

        if (rid != null) {
            result = (IKernelReservation) reservations.get(rid);
        }

        return result;
    }

    /**
     * Returns all reservations in the specified slice.
     * @param sliceID slice id
     * @return an array of reservations
     */
    public IKernelReservation[] getReservations(final SliceID sliceID)
    {
        IKernelReservation[] result = null;
        IKernelSlice s = slices.get(sliceID);

        if (s != null) {
            result = s.getReservationsArray();
        }

        return result;
    }

    /**
     * Returns the shirako plugin.
     * @return
     */
    public IShirakoPlugin getShirakoPlugin()
    {
        return plugin;
    }

    /**
     * Returns a slice previously registered with the kernel.
     * @param sliceID slice identifier
     * @return slice object
     */
    public IKernelSlice getSlice(final SliceID sliceID)
    {
        if (sliceID == null) {
            throw new IllegalArgumentException();
        }

        return slices.get(sliceID);
    }

    public boolean isKnownSlice(final SliceID sliceID){
    	if (sliceID == null){
    		throw new IllegalArgumentException();
    	}
    	return slices.contains(sliceID);
    }
    
    /**
     * Returns all registered slices.
     * @return an array of slices
     */
    public IKernelSlice[] getSlices()
    {
        return slices.getSlices();
    }
    
    /**
     * Handles a duplicate request.
     * @param current reservation
     * @param operation operation code
     */
    protected void handleDuplicateRequest(final IKernelReservation current, final int operation) throws Exception
    {
        current.handleDuplicateRequest(operation);
    }

    /**
     * Probes to check for completion of pending operation.
     * @param reservation the reservation being probed
     * @throws Exception rare
     */
    protected void probePending(final IKernelReservation reservation) throws Exception
    {
        try {
            reservation.prepareProbe();
            reservation.probePending();
            plugin.getDatabase().updateReservation(reservation);
            reservation.serviceProbe();
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            error("An error occurred during probe pending for reservation #" + reservation.getReservationID().toHashString(), e);
        }
    }

    /**
     * Purges all closed reservations.
     * @throws Exception
     */
    private void purge()
    {
        Iterator<IReservation> i = reservations.iterator();

        while (i.hasNext()) {
            IKernelReservation r = (IKernelReservation) i.next();

            if (r.isClosed()) {
                try {
                    r.getKernelSlice().unregister(r);
                } catch (Exception e) {
                    logger.error("exception during purge for reservation #" + r.getReservationID().toHashString(), e);
                } finally {
            		Globals.eventManager.dispatchEvent(new ReservationPurgedEvent(r));
                    i.remove();
                }
            }
        }
    }

    /**
     * Processes a query request.
     * @param properties query
     * @return query response
     */
    public Properties query(final Properties properties)
    {
        return policy.query(properties);
    }

    /**
     * Issues a redeem for a new ticket in Service Manager role. On the server
     * (authority) side this request comes comes in as a reserve().
     * @param reservation reservation being redeemed
     * @throws Exception
     */
    protected void redeem(final IKernelServiceManagerReservation reservation) throws Exception
    {
        try {
            if (reservation.canRedeem()) {
                reservation.reserve(policy);
            } else {
                throw new RuntimeException("The current reservation state prevent it from being redeemed");
            }

            plugin.getDatabase().updateReservation(reservation);

            if (!reservation.isFailed()) {
                reservation.serviceReserve();
            }
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            error("An error occurred during redeem for reservation #" + reservation.getReservationID().toHashString(), e);
        }
    }

    /**
     * Registers a new reservation with its slice and the kernel reservation
     * table. Must be called with the kernel lock on.
     * @param reservation local reservation object
     * @param slice local slice object. The slice must have previously been
     *            registered with the kernel.
     * @return true if the reservation was registered, false otherwise
     * @throws Exception
     */
    private boolean register(final IKernelReservation reservation, final IKernelSlice slice) throws Exception
    {
        boolean add = false;

        reservation.setLogger(logger);
        
        /*
         * Ignore closed.
         */
        if (!reservation.isClosed()) {
            /*
             * Note: as of now slice.register must be the first operation in
             * this method. slice.register will throw an exception if the
             * reservation is already present in the slice table.
             */

            /* register with the local slice */
            slice.register(reservation);

            /* register with the reservations table */
            if (reservations.contains(reservation.getReservationID())) {
                slice.unregister(reservation);
                throw new Exception("There is already a reservation with the given identifier");
            }

            reservations.add(reservation);

            /* attach actor to the reservation */
            ((Reservation) reservation).setActor(plugin.getActor());
            /* attach the local slice object */
            reservation.setSlice(slice);
            add = true;
        } else {
            logger.warn("Attempting to register a closed reservation #" + reservation.getReservationID().toHashString());
        }

        return add;
    }

    /**
     * Re-registers the reservation.
     * @param reservation reservation
     * @throws Exception
     */
    public void registerReservation(final IKernelReservation reservation) throws Exception
    {
        if ((reservation == null) || (reservation.getReservationID() == null) || (reservation.getSlice() == null) || (reservation.getSlice().getName() == null)) {
            throw new IllegalArgumentException();
        }

        IKernelSlice localSlice = null;
        boolean add = false;

        localSlice = slices.getException(reservation.getSlice().getSliceID());
        add = register(reservation, localSlice);

        if (add) {
            try {
                plugin.getDatabase().addReservation((Reservation) reservation);
            } catch (TestException e) {
                /* just pass it on */
                throw e;
            } catch (Exception e) {
                unregisterNoCheck(reservation, localSlice);
                throw new RuntimeException("database error", e);
            }
        }
    }

    /**
     * Registers the specified slice with the kernel.
     * @param slice slice to register
     * @throws Exception if the slice cannot be registered
     */
    public void registerSlice(final IKernelSlice slice) throws Exception
    {
        slice.prepare();
        slices.add(slice);

        // add the slice record to the database
        try {
            plugin.getDatabase().addSlice(slice);
        } catch (Exception e) {
            slices.remove(slice.getSliceID());
            error("could not register slice", e);
        }
    }

    /**
     * Removes the reservation.
     * @param rid reservation id.
     * @throws Exception
     */
    public void removeReservation(final ReservationID rid) throws Exception
    {
        if (rid == null) {
            throw new IllegalArgumentException();
        }

        IReservation real = reservations.get(rid);

        if (real != null) {
            if (real.isClosed() || real.isFailed() || (real.getState() == ReservationStates.CloseWait)) {
                unregisterReservation(rid);
            } else {
                throw new RuntimeException("Only reservations in failed, closed, or closewait state can be removed.");
            }
        }

        plugin.getDatabase().removeReservation(rid);
    }

    /**
     * Removes the specified slice.
     * @param sliceID slice identifier
     * @throws Exception if the slice contains active reservations or removal
     *             fails
     */
    public void removeSlice(final SliceID sliceID) throws Exception
    {
        if (sliceID == null) {
            throw new IllegalArgumentException();
        }

        boolean possible = false;
        IKernelSlice slice = getSlice(sliceID);

        if (slice == null) {
            /* not registered with the kernel. just remove from the database */
            plugin.getDatabase().removeSlice(sliceID);
        } else {
            possible = (slice.getReservations().size() == 0);

            if (possible) {
                /* remove the slice from the slices table */
                slices.remove(sliceID);
                /*
                 * release any resources assigned to the slice: unlocked,
                 * because it may be blocking. The plugin is responsible for
                 * synchronization.
                 */
                plugin.releaseSlice(slice);
                /* remove from the database */
                plugin.getDatabase().removeSlice(sliceID);
            }
        }
    }

    /**
     * Re-registers the reservation.
     * @param reservation reservation
     * @throws Exception
     */
    public void reregisterReservation(final IKernelReservation reservation) throws Exception
    {
        if ((reservation == null) || (reservation.getReservationID() == null) || (reservation.getSlice() == null) || (reservation.getSlice().getSliceID() == null)) {
            throw new IllegalArgumentException();
        }

        IKernelSlice localSlice = null;

        localSlice = slices.getException(reservation.getSlice().getSliceID());

        if (localSlice == null) {
            throw new Exception("slice not registered with the kernel");
        } else {
            register(reservation, localSlice);
        }

        /*
         * Check if the reservation has a database record.
         */
        Vector<Properties> temp = null;

        try {
            temp = plugin.getDatabase().getReservation(reservation.getReservationID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if ((temp == null) || (temp.size() == 0)) {
            // force unregistration !!!
            unregisterNoCheck(reservation, localSlice);
            throw new Exception("The reservation has no database record");
        }
    }

    /**
     * Re-registers the specified slice with the kernel.
     * @param slice slice to re-register
     * @throws Exception if the slice cannot be registered
     */
    public void reregisterSlice(final IKernelSlice slice) throws OrcaException
    {
        slice.prepare();
        slices.add(slice);

        try {
            /*
             * Check if the slice exists in the database.
             */
            Vector<Properties> temp = plugin.getDatabase().getSlice(slice.getSliceID());

            if ((temp != null) && (temp.size() == 0)) {
                throw new Exception("The slice does not have a database record");
            }
        } catch (Exception e) {
            synchronized (this) {
                slices.remove(slice.getSliceID());
            }

            error("could not reregister slice", e);
        }
    }

    /**
     * Handles a reserve operation for the reservation.
     * <p>
     * Client: issue a ticket request or a claim request.
     * </p>
     * <p>
     * Broker: process a request for a new ticket. Claims for previously
     * exported tickets are handled by {@link #claim(BrokerReservation)}.
     * </p>
     * <p>
     * Authority: process a request for a new lease.
     * </p>
     * @param reservation reservation for which to perform redeem
     * @throws Exception
     */
    protected void reserve(final IKernelReservation reservation) throws Exception
    {
        try {
            reservation.reserve(policy);
            plugin.getDatabase().updateReservation(reservation);
            if (!reservation.isFailed()) {
                reservation.serviceReserve();
            }
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            error("An error occurred during reserve for reservation #" + reservation.getReservationID().toHashString(), e);
        }
    }

    /**
     * Retrieves the locally registered reservation that corresponds to the
     * passed reservation. Obtains the reservation from the containing slice
     * object.
     * @param reservation reservation being validated
     * @return the locally registered reservation that corresponds to the passed
     *         reservation or null if no local reservation exists
     * @throws IllegalArgumentException if the arguments are invalid
     * @throws Exception if the slice referenced by the incoming reservation is
     *             not locally registered
     */
    protected IKernelReservation softValidate(final IKernelReservation r) throws Exception
    {
        if (r == null) {
            throw new IllegalArgumentException();
        }

        /*
         * Each local reservation is indexed in two places: (1) The reservation
         * set in the kernel, and (2) inside the local slice object. Here we
         * will check to see if the local slice exists and will retrieve the
         * reservation from the local slice.
         */
        IKernelSlice s = getLocalSlice(r.getSlice());

        return s.softLookup(r.getReservationID());
    }

    /**
     * Retrieves the locally registered reservation that corresponds to the
     * passed reservation. Does not obtain the reservation from the containing
     * slice object.
     * @param rid reservation identifier or reservation being validated
     * @return the locally registered reservation that corresponds to the passed
     *         reservation or null if no local reservation exists
     * @throws IllegalArgumentException if the arguments are invalid
     */
    protected IKernelReservation softValidate(final ReservationID rid)
    {
        if (rid == null) {
            throw new IllegalArgumentException();
        }

        return (IKernelReservation) reservations.get(rid);
    }

    /**
     * Timer interrupt.
     * @throws Exception
     */
    public void tick() throws Exception
    {
        try {
            Iterator<?> i = reservations.iterator();

            while (i.hasNext()) {
                Reservation r = (Reservation) i.next();
                /*
                 * Check for completed pending operations (e.g., resources
                 * becoming active or closing). In an ideal world these would
                 * come from some external notification.
                 */
                probePending(r);
            }

            purge();
            checkNothingPending();
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            error("exception in Kernel.tick", e);
        }
    }

    private Object nothingPendingLock = new Object();
    
    private boolean hasSomethingPending() {
		for (IReservation r : reservations){
			if (!r.isTerminal() && (r.isNascent() || !r.isNoPending())){
				return true;
			}
		}
    	return false;    	
    }
    
    private void checkNothingPending() {
    	if (!hasSomethingPending()) {
    		synchronized(nothingPendingLock) {
    			nothingPendingLock.notifyAll();
    		}
    	}
    }
    
    public void awaitNothingPending() throws InterruptedException {
    	synchronized(nothingPendingLock) {
    		nothingPendingLock.wait();
    	}
    }

    /**
     * Unregisters a reservation from the kernel data structures. Must be called
     * with the kernel lock on. Performs state checks.
     * @param reservation reservation to unregister
     * @param slice local slice object
     * @throws Exception
     */
    private void unregister(final IKernelReservation reservation, final IKernelSlice slice) throws Exception
    {
        if (reservation.isClosed() || reservation.isFailed() || (reservation.getState() == ReservationStates.CloseWait)) {
            slice.unregister(reservation);
            reservations.remove(reservation);
        } else {
            throw new RuntimeException("Only reservations in failed, closed, or closewait state can be unregistered.");
        }
    }

    /**
     * Unregisters a reservation from the kernel data structures. Must be called
     * with the kernel lock on. Does not perform state checks.
     * @param reservation reservation to unregister
     * @param slice local slice object
     * @throws Exception
     */
    private void unregisterNoCheck(final IKernelReservation reservation, final IKernelSlice slice) throws Exception
    {
        slice.unregister(reservation);
        reservations.remove(reservation);
    }

    /**
     * Unregisters the reservation
     * @param rid reservation id
     * @throws Exception
     */
    public void unregisterReservation(final ReservationID rid) throws Exception
    {
        if (rid == null) {
            throw new IllegalArgumentException();
        }

        IKernelReservation localReservation = (IKernelReservation) reservations.getException(rid);
        unregister(localReservation, localReservation.getKernelSlice());
        /*
         * inform the policy it is no longer responsible for this
         * reservation
         */
        policy.remove(localReservation);
    }

    /**
     * Unregisters the specified slice.
     * @param sliceID slice id
     * @throws Exception if the slice cannot be unregistered (it has active
     *             reservations) or has not been previously registered with the
     *             kernel
     */
    public void unregisterSlice(final SliceID sliceID) throws Exception
    {
        if (sliceID == null) {
            throw new IllegalArgumentException();
        }

        IKernelSlice slice = getSlice(sliceID);

        if (slice == null) {
            throw new Exception("Trying to unregister a slice, which is not registered with the kernel");
        }

        if (slice.getReservations().size() == 0) {
            slices.remove(sliceID);
            plugin.releaseSlice(slice);
        } else {
            throw new Exception("Slice cannot be unregistered: not empty");
        }
    }

    /**
     * Handles an incoming update lease operation (client side only).
     * @param reservation local reservation
     * @param update update sent from site authority
     * @param udd status of the operation the authority is informing us about
     * @throws Exception
     */
    protected void updateLease(final IKernelReservation reservation, final Reservation update, final UpdateData udd) throws Exception
    {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("updateLease: Incoming term" + update.getTerm().toString());
            }

            reservation.updateLease(update, udd);

            /*
             * NOTE: the database update has to happen BEFORE the service
             * update: we need to record the fact that we received a concrete
             * set, so that on recovery we can go and recover the concrete set.
             * If the database update is after the service call, we may end up
             * in Ticketed, Redeeming with state in the database that will
             * prevent us from incorporating a leaseUpdate.
             */
            plugin.getDatabase().updateReservation(reservation);

            if (!reservation.isFailed()) {
                reservation.serviceUpdateLease();
            }
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            error("An error occurred during update lease for reservation #" + reservation.getReservationID().toHashString(), e);
        }
    }

    /**
     * Handles an incoming update ticket operation (client side only).
     * @param reservation local reservation
     * @param update update sent from upstream broker
     * @param udd status of the operation the broker is informing us about
     * @throws Exception
     */
    protected void updateTicket(final IKernelReservation reservation, final Reservation update, final UpdateData udd) throws Exception
    {
        try {
            reservation.updateTicket(update, udd);
            plugin.getDatabase().updateReservation(reservation);
            if (!reservation.isFailed()) {
                reservation.serviceUpdateTicket();
            }
        } catch (TestException e) {
            throw e;
        } catch (Exception e) {
            error("An error occurred during update ticket for reservation #" + reservation.getReservationID().toHashString(), e);
        }
    }

    /**
     * Retrieves the locally registered reservation that corresponds to the
     * passed reservation. Obtains the reservation from the containing slice
     * object.
     * @param reservation reservation being validated
     * @return the locally registered reservation that corresponds to the passed
     *         reservation
     * @throws Exception if there is no local reservation that corresponds to
     *             the passed reservation
     */
    protected IKernelReservation validate(final IKernelReservation reservation) throws Exception
    {
        IKernelReservation local = softValidate(reservation);

        if (local == null) {
            error("reservation not found");
        }

        return local;
    }

    /**
     * Retrieves the locally registered reservation that corresponds to the
     * passed reservation. Does not obtain the reservation from the containing
     * slice object.
     * @param rid reservation identifier of reservation being validated
     * @return the locally registered reservation that corresponds to the passed
     *         reservation
     * @throws Exception if there is no local reservation that corresponds to
     *             the passed reservation
     */
    protected IKernelReservation validate(final ReservationID rid) throws Exception
    {
        IKernelReservation local = softValidate(rid);

        if (local == null) {
            error("reservation not found");
        }

        return local;
    }
    
    protected void handleFailedRPC(IKernelReservation r, FailedRPC rpc) {
        r.handleFailedRPC(rpc);
    }
}
