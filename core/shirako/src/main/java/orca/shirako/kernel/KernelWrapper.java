/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.kernel;

import java.security.cert.X509Certificate;
import java.util.Properties;

import orca.security.AbacUtil;
import orca.security.AccessMonitor;
import orca.security.AuthToken;
import orca.security.Guard;
import orca.shirako.api.IActor;
import orca.shirako.api.IActorIdentity;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.IClientCallbackProxy;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IPolicy;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManagerCallbackProxy;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.api.IShirakoPlugin;
import orca.shirako.api.ISlice;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.container.Globals;
import orca.shirako.registry.ActorRegistry;
import orca.shirako.time.Term;
import orca.shirako.util.TestException;
import orca.shirako.util.UpdateData;
import orca.util.OrcaException;

import org.apache.log4j.Logger;

/**
 * The <code>KernelWrapper</code>
 * is responsible for validating the arguments to internal kernel methods before
 * invoking these calls. The internal kernel methods can only be invoked through
 * an instance of the kernel wrapper.
 */
public class KernelWrapper {
    /**
     * The actor linked by the wrapper to the kernel.
     */
    private IActor actor;

    /**
     * The kernel instance.
     */
    private Kernel kernel;

    /**
     * Access control monitor.
     */
    private AccessMonitor monitor;

    /**
     * Access control lists.
     */
    private Guard guard;

    /**
     * Logger.
     */
    private Logger logger;

    /*
     * =======================================================================
     * Construction and initialization
     * =======================================================================
     */

    /**
     * Creates a new kernel wrapper for the given actor
     * @param actor actor
     * @param plugin actor's shirako plugin
     * @param policy actor's policy
     * @param monitor access control monitor
     * @param guard source for access control lists
     */
    public KernelWrapper(final IActor actor, final IShirakoPlugin plugin, final IPolicy policy, final AccessMonitor monitor, final Guard guard) {
        this.actor = actor;
        this.kernel = new Kernel(plugin, policy, actor.getLogger());
        this.monitor = monitor;
        this.guard = guard;
        this.logger = actor.getLogger();
    }

	/**
	 * Blocks until there are no more reservations in a pending state.
	 * @throws InterruptedException
	 */
    public void awaitNothingPending() throws InterruptedException {
    	kernel.awaitNothingPending();
    }
    
    /**
     * Processes a request to claim a pre-reserved "will call" ticket.
     * <p>
     * Role: Broker
     * </p>
     * @param reservation reservation describing the claim request
     * @param caller caller identity
     * @param callback callback proxy
     * @throws Exception
     */
    public void claimRequest(final IBrokerReservation reservation, final AuthToken caller, IClientCallbackProxy callback) throws Exception {
        if ((reservation == null) || (caller == null) || (callback == null)) {
            throw new IllegalArgumentException();
        }

        /*
         * Note: for claim we do not need the slice object, so we use
         * validate(ReservationID) instead of validate(Reservation).
         */
        IKernelBrokerReservation exported = (IKernelBrokerReservation) kernel.validate(reservation.getReservationID());
        /* check access */
        monitor.checkReserve(caller, exported.getSlice().getGuard());
        // FIXME: we need to prepare if this reservation is "exported"
        exported.prepare(callback, logger);
        kernel.claim(exported);
    }

    /*
     * =======================================================================
     * Slice management operations
     * =======================================================================
     */

    /**
     * Fails the specified reservation.
     * @param rid reservation id
     * @throws Exception
     */
    public void fail(final ReservationID rid, String message) throws Exception {
        if (rid == null) {
            throw new IllegalArgumentException();
        }
        
        IKernelReservation target = kernel.validate(rid);
        monitor.checkReserve(actor.getIdentity(), target.getSlice().getGuard());
        kernel.fail(target, message);
    }
    
    /**
     * Closes the reservation, potentially initiating a close request to another
     * actor. If the reservation has concrete resources bound to it, this method
     * may return before all close operations have completed. Check the
     * reservation state to determine when close completes.
     * @param rid identifier of reservation to close
     * @throws Exception
     */
    public void close(final ReservationID rid) throws Exception {
        if (rid == null) {
            throw new IllegalArgumentException();
        }

        IKernelReservation target = kernel.validate(rid);
        // NOTE: this call does not require access control check, since
        // it is executed in the context of the actor represented by KernelWrapper.
        //monitor.checkReserve(actor.getIdentity(), target.getSlice().getGuard());
        kernel.close(target);
    }

    public void closeSliceReservations(final SliceID sliceID) throws Exception {
    	if (sliceID == null) {
    		throw new IllegalArgumentException();
    	}
    	
    	if (!kernel.isKnownSlice(sliceID)){
    		throw new IllegalArgumentException("Unknown slice: " + sliceID);
    	}
    	
    	IReservation[] rr = getReservations(sliceID);
    	if (rr == null) {return;}
    	for (IReservation r:rr){
    		try {
    			kernel.close((IKernelReservation)r);
    		} catch (Exception e){
    			logger.error("Error during close", e);
    		}
    	}
    }
    
    /**
     * Processes an incoming request to close a reservation.
     * <p>
     * Role: Authority
     * </p>
     * @param reservation reservation to close.
     * @param caller caller identity
     * @param compareSequenceNumbers if true, the incoming sequence number will
     *            be compared to the local sequence number to detect fresh
     *            requests, if false, no comparison will be performed.
     * @throws Exception
     */
    public void closeRequest(final IReservation reservation, final AuthToken caller, final boolean compareSequenceNumbers) throws Exception {
        if ((reservation == null) || (caller == null)) {
            throw new IllegalArgumentException();
        }

        if (compareSequenceNumbers) {
            IKernelServerReservation target = (IKernelServerReservation) kernel.validate(reservation.getReservationID());
            
            Properties authProperties = reservation.getRequestedResources().getConfigurationProperties();
            
        	/* Check proxy */
         	AuthToken requester = monitor.checkProxy(caller, AbacUtil.getRequesterAuthToken(authProperties));

        	/* Check access */
            monitor.checkReserve(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
            			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
            
            //monitor.checkReserve(caller, target.getSlice().getGuard());

            /*
             * Since close request can arrive at any time (even when some other
             * operation on the reservation is in progress) we must use
             * compareAndUpdateIgnorePending instead of compareAndUpdate.
             */
            switch (kernel.compareAndUpdateIgnorePending((IKernelServerReservation) reservation, target)) {
                case SequenceComparisonCodes.SequenceGreater:
                    kernel.close(target);

                    break;

                case SequenceComparisonCodes.SequenceSmaller:
                    logger.warn("closeRequest with a smaller sequence number");

                    break;

                case SequenceComparisonCodes.SequenceEqual:
                    logger.warn("duplicate closeRequest");
                    kernel.handleDuplicateRequest(target, RequestTypes.RequestClose);

                    break;
            }
        } else {
            IKernelReservation target = kernel.validate((IKernelReservation) reservation);
            Properties authProperties = reservation.getRequestedResources().getConfigurationProperties();
        	/* Check proxy */
         	AuthToken requester = monitor.checkProxy(caller, AbacUtil.getRequesterAuthToken(authProperties));
        	/* Check access */
            monitor.checkReserve(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
            			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
            //monitor.checkReserve(caller, target.getSlice().getGuard());
            kernel.close(target);
        }
    }

    /**
     * Initiates a ticket export.
     * <p>
     * Role: Broker or Authority
     * </p>
     * Prepare/hold a ticket for "will call" claim by a client.
     * @param reservation reservation to be exported
     * @param client client identity
     * @throws Exception
     */
    public void export(final IBrokerReservation reservation, final AuthToken client) throws Exception {
        if ((reservation == null) || (reservation.getSlice() == null)) {
            throw new IllegalArgumentException();
        }

        //monitor.checkOps(client, guard);

        BrokerReservation br = (BrokerReservation) reservation;
        br.prepare(null, logger);
        br.client = client;

        // XXX: uncommenting this line breaks exports for an authority!!!
        // r.setExporting();

        /*
         * For now, the assumption is that we only export to brokers. Hence we
         * will mark the slice as a broker client slice.
         */
        reservation.getSlice().setBrokerClient();

        handleReserve(br, client, false, false);
    }

    /**
     * Initiates a request to extend a lease.
     * <p>
     * Role: Service Manager
     * </p>
     * @param reservation reservation describing the extend request
     * @throws Exception
     */
    public void extendLease(final IServiceManagerReservation reservation) throws Exception {
        if (reservation == null) {
            throw new IllegalArgumentException();
        }

        IKernelServiceManagerReservation target = (IKernelServiceManagerReservation) kernel.validate(reservation.getReservationID());

        if (target == null) {
            logger.error("extendLease for a reservation not registered with the kernel");
        }

        Properties authProperties = reservation.getResources().getRequestProperties();
        
    	/* Check proxy */
     	AuthToken requester = monitor.checkProxy(actor.getIdentity(), AbacUtil.getRequesterAuthToken(authProperties));

    	/* Check access */
        monitor.checkReserve(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
        			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
        
        //monitor.checkReserve(actor.getIdentity(), target.getSlice().getGuard());
        target.validateRedeem();
        kernel.extendLease(target);
    }

    /**
     * Processes an incoming request for a lease extension.
     * <p>
     * Role: Authority
     * </p>
     * @param reservation reservation representing the lease extension request.
     * @param caller caller identity
     * @param compareSequenceNumbers if true, the incoming sequence number will
     *            be compared to the local sequence number to detect fresh
     *            requests, if false, no comparison will be performed.
     * @throws Exception
     */
    public void extendLeaseRequest(final IAuthorityReservation reservation, final AuthToken caller, boolean compareSequenceNumbers) throws Exception {
        if ((reservation == null) || (caller == null)) {
            throw new IllegalArgumentException();
        }

        IKernelAuthorityReservation r = (IKernelAuthorityReservation) reservation;

        if (compareSequenceNumbers) {
            r.validateIncoming();

            IKernelAuthorityReservation target = (IKernelAuthorityReservation) kernel.validate(reservation.getReservationID());
            
            Properties authProperties = reservation.getRequestedResources().getConfigurationProperties();
            
        	/* Check proxy */
         	AuthToken requester = monitor.checkProxy(caller, AbacUtil.getRequesterAuthToken(authProperties));

        	/* Check access */
            monitor.checkReserve(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
            			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
            
            //monitor.checkReserve(caller, target.getSlice().getGuard());

            switch (kernel.compareAndUpdate(r, target)) {
                case SequenceComparisonCodes.SequenceGreater:
                    target.prepareExtendLease();
                    kernel.extendLease(target);

                    break;

                case SequenceComparisonCodes.SequenceSmaller:
                    logger.warn("extendLeaseRequest with a smaller sequence number");

                    break;

                case SequenceComparisonCodes.SequenceEqual:
                    logger.warn("duplicate extendLease request");
                    kernel.handleDuplicateRequest(target, RequestTypes.RequestExtendLease);

                    break;
            }
        } else {
            IKernelAuthorityReservation target = (IKernelAuthorityReservation) kernel.validate(reservation.getReservationID());
            Properties authProperties = reservation.getRequestedResources().getConfigurationProperties();
        	/* Check proxy */
         	AuthToken requester = monitor.checkProxy(caller, AbacUtil.getRequesterAuthToken(authProperties));
        	/* Check access */
            monitor.checkReserve(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
            			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
            //monitor.checkReserve(caller, target.getSlice().getGuard());
            kernel.extendLease(target);
        }
    }

    /**
     * Processes an incoming request for a lease modification.
     * <p>
     * Role: Authority
     * </p>
     * @param reservation reservation representing the lease modification request.
     * @param caller caller identity
     * @param compareSequenceNumbers if true, the incoming sequence number will
     *            be compared to the local sequence number to detect fresh
     *            requests, if false, no comparison will be performed.
     * @throws Exception
     */
    public void modifyLeaseRequest(final IAuthorityReservation reservation, final AuthToken caller, boolean compareSequenceNumbers) throws Exception {
        if ((reservation == null) || (caller == null)) {
            throw new IllegalArgumentException();
        }
        
        IKernelAuthorityReservation r = (IKernelAuthorityReservation) reservation;

        if (compareSequenceNumbers) {
            r.validateIncoming();

            IKernelAuthorityReservation target = (IKernelAuthorityReservation) kernel.validate(reservation.getReservationID());
            
            Properties authProperties = reservation.getRequestedResources().getConfigurationProperties();
            
        	/* Check proxy */
         	AuthToken requester = monitor.checkProxy(caller, AbacUtil.getRequesterAuthToken(authProperties));

        	/* Check access */
            monitor.checkReserve(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
            			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
            
            //monitor.checkReserve(caller, target.getSlice().getGuard());

            switch (kernel.compareAndUpdate(r, target)) {
                case SequenceComparisonCodes.SequenceGreater:
                    target.prepareModifyLease();
                    kernel.modifyLease(target);

                    break;

                case SequenceComparisonCodes.SequenceSmaller:
                    logger.warn("modifyLeaseRequest with a smaller sequence number");

                    break;

                case SequenceComparisonCodes.SequenceEqual:
                    logger.warn("duplicate modifyLease request");
                    kernel.handleDuplicateRequest(target, RequestTypes.RequestModifyLease);

                    break;
            }
        } else {
            IKernelAuthorityReservation target = (IKernelAuthorityReservation) kernel.validate(reservation.getReservationID());
            Properties authProperties = reservation.getRequestedResources().getConfigurationProperties();
        	/* Check proxy */
         	AuthToken requester = monitor.checkProxy(caller, AbacUtil.getRequesterAuthToken(authProperties));
        	/* Check access */
            monitor.checkReserve(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
            			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
            //monitor.checkReserve(caller, target.getSlice().getGuard());
            kernel.modifyLease(target);
        }
    }
    
    
    /**
     * Extends the reservation with the given resources and term.
     * @param rid identifier of reservation to extend
     * @param resources resources for extension
     * @param term term for extension
     * @return 0 on success, a negative exit code on error
     * @throws Exception
     */
    public int extendReservation(final ReservationID rid, final ResourceSet resources, final Term term) throws Exception {
        if ((rid == null) || (resources == null) || (term == null)) {
            throw new IllegalArgumentException();
        }

        return kernel.extendReservation(rid, resources, term);
    }

    /**
     * Initiates a request to extend a ticket.
     * <p>
     * Role: Broker or Service Manager
     * </p>
     * @param reservation reservation describing the ticket extension request
     * @throws Exception
     */
    public void extendTicket(final IClientReservation reservation) throws Exception {
        if (reservation == null) {
            throw new IllegalArgumentException();
        }

        IKernelClientReservation target = (IKernelClientReservation) kernel.validate(reservation.getReservationID());

        if (target == null) {
            throw new Exception("extendTicket on a reservation not registered with the kernel");
        }

        Properties authProperties = reservation.getResources().getRequestProperties();
        
    	/* Check proxy */
     	AuthToken requester = monitor.checkProxy(actor.getIdentity(), AbacUtil.getRequesterAuthToken(authProperties));

    	/* Check access */
        monitor.checkReserve(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
        			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
        
        //monitor.checkReserve(actor.getIdentity(), target.getSlice().getGuard());
        target.validateOutgoing();
        kernel.extendTicket(target);
    }


    /**
     * Processes an incoming request for a ticket extension.
     * <p>
     * Role: Broker
     * </p>
     * @param reservation reservation representing the ticket extension request.
     * @param caller caller identity
     * @param compareSequenceNumbers if true, the incoming sequence number will
     *            be compared to the local sequence number to detect fresh
     *            requests, if false, no comparison will be performed.
     * @throws Exception
     */
    public void extendTicketRequest(final IBrokerReservation reservation, final AuthToken caller, boolean compareSequenceNumbers) throws Exception {
        if ((reservation == null) || (caller == null)) {
            throw new IllegalArgumentException();
        }

        IKernelBrokerReservation r = (IKernelBrokerReservation) reservation;

        if (compareSequenceNumbers) {
            r.validateIncoming();

            IKernelBrokerReservation target = (IKernelBrokerReservation) kernel.validate(reservation.getReservationID());
            
            Properties authProperties = reservation.getRequestedResources().getRequestProperties();
            
        	/* Check proxy */
         	AuthToken requester = monitor.checkProxy(caller, AbacUtil.getRequesterAuthToken(authProperties));

        	/* Check access */
            monitor.checkReserve(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
            			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
            
            //monitor.checkReserve(caller, target.getSlice().getGuard());

            switch (kernel.compareAndUpdate(r, target)) {
                case SequenceComparisonCodes.SequenceGreater:
                    kernel.extendTicket(target);

                    break;

                case SequenceComparisonCodes.SequenceInProgress:
                    logger.warn("New request for a reservation with a pending action");

                    break;

                case SequenceComparisonCodes.SequenceSmaller:
                    logger.warn("Incoming extendTicket request has smaller sequence number");

                    break;

                case SequenceComparisonCodes.SequenceEqual:
                    logger.warn("Duplicate extendTicket request");
                    kernel.handleDuplicateRequest(target, RequestTypes.RequestExtendTicket);

                    break;
            }
        } else {
            IKernelBrokerReservation target = (IKernelBrokerReservation) kernel.validate(reservation.getReservationID());
            Properties authProperties = reservation.getRequestedResources().getRequestProperties();
        	/* Check proxy */
         	AuthToken requester = monitor.checkProxy(caller, AbacUtil.getRequesterAuthToken(authProperties));
        	/* Check access */
            monitor.checkReserve(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
            			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
            //monitor.checkReserve(caller, target.getSlice().getGuard());
            kernel.extendTicket(target);
        }
    }

    /**
     * Initiates a request to modify a lease.
     * <p>
     * Role: Service Manager
     * </p>
     * @param reservation reservation describing the modify request
     * @param modifyProps modify properties
     * @throws Exception
     */
    public void modifyLease(final IServiceManagerReservation reservation) throws Exception {
    	
    	if (reservation == null) {
            throw new IllegalArgumentException();
        }

        IKernelServiceManagerReservation target = (IKernelServiceManagerReservation) kernel.validate(reservation.getReservationID());

        if (target == null) {
            logger.error("modifyLease for a reservation not registered with the kernel");
        }

        Properties authProperties = reservation.getResources().getRequestProperties();
        
    	/* Check proxy */
     	AuthToken requester = monitor.checkProxy(actor.getIdentity(), AbacUtil.getRequesterAuthToken(authProperties));

    	/* Check access */
        monitor.checkReserve(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
        			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
        
        //monitor.checkReserve(actor.getIdentity(), target.getSlice().getGuard());
        target.validateRedeem(); // checks for sanity of the reservation and the resources
        kernel.modifyLease(target);
    }        
    
    
    /**
     * {@inheritDoc}
     */
    public void relinquishRequest(final IBrokerReservation reservation, final AuthToken caller) throws Exception {
        if ((reservation == null) || (caller == null)) {
            throw new IllegalArgumentException();
        }
        IKernelBrokerReservation r = (IKernelBrokerReservation) reservation;

        r.validateIncoming();

        IKernelBrokerReservation target = (IKernelBrokerReservation) kernel.softValidate(reservation.getReservationID());
        if (target == null) {
            Globals.Log.info("Relinquish for non-existent reservation. Reservation has already been closed. Nothing to relinquish");
            return;
        }
        
        Properties authProperties = reservation.getRequestedResources().getRequestProperties();
        
    	/* Check proxy */
     	AuthToken requester = monitor.checkProxy(caller, AbacUtil.getRequesterAuthToken(authProperties));

    	/* Check access */
        monitor.checkReserve(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
        			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
        
        //monitor.checkReserve(caller, target.getSlice().getGuard());

        switch (kernel.compareAndUpdate(r, target)) {
            case SequenceComparisonCodes.SequenceGreater:
            case SequenceComparisonCodes.SequenceInProgress:
                kernel.close(target);
                break;

            case SequenceComparisonCodes.SequenceSmaller:
                logger.warn("Incoming relinquish request has smaller sequence number");
                break;

            case SequenceComparisonCodes.SequenceEqual:
                logger.warn("Duplicate relinquish request");
                kernel.handleDuplicateRequest(target, RequestTypes.RequestRelinquish);
                break;
        }
    }

    /*
     * =======================================================================
     * Reservation management operations
     * =======================================================================
     */

    /**
     * Returns all client slices registered with the kernel.
     * @return an array of client slices registered with the kernel
     */
    public ISlice[] getClientSlices() {
        return kernel.getClientSlices();
    }

    /**
     * Returns all inventory slices registered with the kernel.
     * @return an array of inventory slices registered with the kernel
     */
    public ISlice[] getInventorySlices() {
        return kernel.getInventorySlices();
    }

    /**
    /**
     * Returns the reservation with the given reservation identifier.
     * @param rid reservation identifier
     * @return reservation with the given reservation identifier
     */

    public IReservation getReservation(final ReservationID rid) {
        if (rid == null) {
            throw new IllegalArgumentException();
        }

        return kernel.getReservation(rid);
    }

    /**
     * Returns all reservations in the given slice
     * @param sliceID identifier of slice
     * @return an array of all reservations in the slice
     */
    public IReservation[] getReservations(final SliceID sliceID) {
        if (sliceID == null) {
            throw new IllegalArgumentException();
        }

        return kernel.getReservations(sliceID);
    }

    /**
     * Returns the slice with the given name.
     * @param sliceID identifier of slice to return
     * @return the requested slice or null if no slice with the requested name
     *         is registered with the kernel.
     */
    public ISlice getSlice(final SliceID sliceID) {
        if (sliceID == null) {
            new IllegalArgumentException();
        }

        return kernel.getSlice(sliceID);
    }

    /**
     * Returns all slice registered with the kernel.
     * @return an array of slices registered with the kernel
     */
    public ISlice[] getSlices() {
        return kernel.getSlices();
    }

    /**
     * Handles a reserve, i.e., obtain a new ticket or lease. Called from both
     * client and server side code. If the slice does not exist it will create
     * and register a slice (if <code>createNewSlice</code> is true (server
     * side)). If the slice does not exist and <code>createNewSlice</code> is
     * false (client side), it will register the slice contained in the
     * reservation object.
     * @param reservation the reservation
     * @param identity caller identity
     * @param createNewSlice true -> creates a new slice object, false -> reuses
     *            the slice object in the reservation. This flag is considered
     *            only if the slice referenced by the reservation is not
     *            registered with the kernel.
     * @throws Exception
     */
    private void handleReserve(IKernelReservation reservation, AuthToken identity, boolean createNewSlice, boolean verifyCredentials) throws Exception {
        /* Perform some simple sanity check */
        if ((reservation.getSlice() == null) || (reservation.getSlice().getName() == null) || (reservation.getSlice().getSliceID() == null) || (reservation.getReservationID() == null)) {
            throw new IllegalArgumentException();
        }
        
        Properties authProperties;
        if(reservation instanceof IAuthorityReservation){
    		authProperties = reservation.getRequestedResources().getConfigurationProperties();
    	}else if(reservation instanceof IBrokerReservation){
    		authProperties = reservation.getRequestedResources().getRequestProperties();
    	}else{
    		authProperties = reservation.getResources().getRequestProperties();
    	}
        
        if(verifyCredentials){
         	identity = monitor.checkProxy(identity, AbacUtil.getRequesterAuthToken(authProperties));
        }
        
        /*
         * Obtain the previously created slice or create a new slice. When this
         * function returns we will have a slice object that is registered with
         * the kernel.
         */
        IKernelSlice s = kernel.getOrCreateLocalSlice(identity, reservation, createNewSlice);

        if(verifyCredentials){
        	/* Check access */
            if(authProperties != null){
            	monitor.checkReserve(s.getGuard(), identity, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
            			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
            }
        }
        
        /*
         * Determine if this is a new or an already existing reservation. We
         * will register new reservations and call reserve for them. For
         * existing reservations we will perform amendReserve. XXX: Note, if the
         * caller makes two concurrent requests for a new reservation, it is
         * possible that kernel.softValidate can return null for both, yet only
         * one of them will succeed to register itself. For the second the
         * kernel will throw an exception. This is a limitation that does not
         * seem to be too much of a problem and can be resolved using the same
         * technique we use for creating/registering slices.
         */
        IKernelReservation temp = kernel.softValidate(reservation);

        if (temp == null) {
            kernel.registerReservation(reservation);
            kernel.reserve((IKernelReservation) reservation);
        } else {
            kernel.amendReserve((IKernelReservation) temp);
        }
    }

    /**
     * Amend a reservation request or initiation, i.e., to issue a new bid on a
     * previously filed request.
     * @param r the reservation
     * @param auth the slice owner
     * @throws Exception
     */
    private void handleUpdateReservation(final IKernelReservation r, final AuthToken auth) throws Exception {
    	Properties authProperties = r.getRequestedResources().getRequestProperties();
    	/* Check proxy */
     	AuthToken requester = monitor.checkProxy(auth, AbacUtil.getRequesterAuthToken(authProperties));
    	/* Check access */
        monitor.checkReserve(r.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
        			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
    	
        //monitor.checkReserve(auth, r.getSlice().getGuard());
        kernel.amendReserve(r);
    }

    /**
     * Processes an incoming query request.
     * @param properties query
     * @param caller caller identity
     * @return query response
     */
    public Properties query(Properties properties, AuthToken caller) {
        // XXX: check access control
        return kernel.query(properties);
    }

    /**
     * Initiates a request to redeem a ticketed reservation.
     * <p>
     * Role: Service Manager.
     * </p>
     * @param reservation the reservation being redeemed
     * @throws Exception
     */
    public void redeem(final IServiceManagerReservation reservation) throws Exception {
        if (reservation == null) {
            throw new IllegalArgumentException();
        }

        IKernelServiceManagerReservation target = (IKernelServiceManagerReservation) kernel.validate(reservation.getReservationID());

        if (target == null) {
            logger.error("Redeem on a reservation not registered with the kernel");
        }

        Properties authProperties = reservation.getResources().getRequestProperties();
        
    	/* Check proxy */
     	AuthToken requester = monitor.checkProxy(actor.getIdentity(), AbacUtil.getRequesterAuthToken(authProperties));

    	/* Check access */
        monitor.checkReserve(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
        			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
        
        //monitor.checkReserve(actor.getIdentity(), target.getSlice().getGuard());
        target.validateRedeem();
        kernel.redeem(target);
    }

    /**
     * Processes an incoming request for a new lease.
     * <p>
     * Role: Authority
     * </p>
     * @param reservation reservation representing the lease request. Must
     *            contain a valid ticket.
     * @param caller caller identity
     * @param callback callback object
     * @param compareSequenceNumbers if true, the incoming sequence number will
     *            be compared to the local sequence number to detect fresh
     *            requests, if false, no comparison will be performed.
     * @throws Exception
     */
    public void redeemRequest(final IAuthorityReservation reservation, AuthToken caller, IServiceManagerCallbackProxy callback, boolean compareSequenceNumbers) throws Exception {
        if ((reservation == null) || (caller == null) || (callback == null)) {
            throw new IllegalArgumentException();
        }

        try {
            AuthorityReservation r = (AuthorityReservation) reservation;

            if (compareSequenceNumbers) {
                /*
                 * We do not need to use sequence numbers to detect duplicate
                 * requests in this case. Unlike agent reservations, which can
                 * be amended, currently it is not possible to amend redeeming
                 * authority reservations. If this changes one day, mirror the
                 * code in ticketRequest()
                 */
                r.validateIncoming();
                /*
                 * Marks the slice as a client slice.
                 */
                reservation.getSlice().setClient();
                r.setActor(kernel.getShirakoPlugin().getActor());

                IKernelSlice s = kernel.getSlice(r.getSlice().getSliceID());

                if (s != null) {
                    IKernelAuthorityReservation target = (IKernelAuthorityReservation) kernel.softValidate(reservation.getReservationID());

                    if (target != null) {
                        kernel.handleDuplicateRequest(target, RequestTypes.RequestRedeem);

                        return;
                    }
                }

                r.prepare(callback, logger);
                handleReserve(r, caller, true, true);
            } else {
                /*
                 * This is most likely a reservation being recovered. Do not
                 * compare sequence numbers, just trigger reserve.
                 */
                r.setLogger(logger);
                handleReserve(r, reservation.getClientAuthToken(), true, true);
            }
        } catch (TestException e) {
            /*
             * a test exception should not cause the reservation to fail. Pass
             * it on.
             */
            throw e;
        } catch (Exception e) {
            /*
             * We tried to process the incoming redeem request and it threw an
             * exception. What should we do? If we do nothing, the reservation
             * may remain "stuck" on the client side. If we fail the
             * reservation, we may fail a reservation that could actually be
             * allocated. The problem is that right now we do not have
             * sufficient information to determine if the reservation is
             * severely affected or not. So to avoid stuck reservations on the
             * client side, we will mark the reservation as failed and try to
             * send an update.
             */
            logger.error("redeemRequest", e);

            /*
             * TODO: We may want to delay to sending the message after the call
             * has completed.
             */
            ((ReservationServer) reservation).failNotify(e.getMessage());
        }
    }

    /**
     * Registers the given reservation with the kernel. Adds a database record
     * for the reservation. The containing slice should have been previously
     * registered with the kernel and no database record should exist. When
     * registering a reservation with an existing database record use
     * {@link #reregisterReservation(IReservation)}. Only reservations
     * that are not closed or failed can be registered. Closed or failed
     * reservations will be ignored.
     * @param reservation the reservation to register
     * @throws IllegalArgumentException when the passed in argument is illegal
     * @throws Exception if the reservation has already been registered with the
     *             kernel.
     * @throws RuntimeException when a database error occurs. In this case the
     *             reservation will be unregistered from the kernel data
     *             structures.
     */
    public void registerReservation(final IReservation reservation) throws Exception {
        if ((reservation == null) || !(reservation instanceof IKernelReservation)) {
            throw new IllegalArgumentException();
        }

        // make sure we are adding an instance of Reservation
        kernel.registerReservation((IKernelReservation) reservation);
    }

 
    /**
     * Registers the slice with the kernel: adds the slice object to the kernel
     * data structures and adds a database record for the slice.
     * @param slice slice to register
     * @throws Exception if the slice is already registered or a database error
     *             occurs. If a database error occurs, the slice will be
     *             unregistered.
     */
    public void registerSlice(final ISlice slice) throws Exception {
        if ((slice == null) || (slice.getSliceID() == null) || (!(slice instanceof IKernelSlice))) {
            throw new IllegalArgumentException();
        }
        // FIXME: what it the slice already has an owner, particularly when 
        // we have one sm representing multiple users.
        // It seems that we need to check the owner of the slice and see if the actor
        // can operate on that owner?
        ((IKernelSlice)slice).setOwner(actor.getIdentity());
        kernel.registerSlice((IKernelSlice) slice);
    }

    /**
     * Unregisters the reservation from the kernel data structures and removes
     * its record from the database.
     * <p>
     * <b>Note:</b>Only failed, closed, or close waiting reservations can be
     * removed.
     * </p>
     * @param rid identifier of reservation to remove
     */
    public void removeReservation(final ReservationID rid) throws Exception {
        if (rid == null) {
            throw new IllegalArgumentException();
        }

        kernel.removeReservation(rid);
    }

    /**
     * Unregisters the slice (if it is registered with the kernel) and removes
     * it from the database.
     * <p>
     * <b>Note:</b> A slice can be removed only if it contains only closed or
     * failed reservations.
     * </p>
    * @param sliceID identifier of slice to remove
     * @throws Exception
     */
    public void removeSlice(final SliceID sliceID) throws Exception {
        if (sliceID == null) {
            new IllegalArgumentException();
        }

        kernel.removeSlice(sliceID);
    }

    /**
     * Registers a previously unregistered reservation with the kernel. The
     * containing slice should have been previously registered with the kernel
     * and a database record for the reservation should exist. When registering
     * a reservation without and existing database record use
     * {@link #registerReservation(IReservation)}. Only reservations
     * that are not closed or failed can be registered. Closed or failed
     * reservations will be ignored.
     * @param reservation the reservation to reregister
     * @throws IllegalArgumentException when the passed in argument is illegal
     * @throws Exception if the reservation has already been registered with the
     *             kernel or the reservation does not have a database record. In
     *             the latter case the reservation will be unregistered from the
     *             kernel data structures.
     * @throws RuntimeException if a database error occurs
     */
    public void reregisterReservation(final IReservation reservation) throws Exception {
        if ((reservation == null) || !(reservation instanceof IKernelReservation)) {
            throw new IllegalArgumentException();
        }

        kernel.reregisterReservation((IKernelReservation) reservation);
    }

    /**
     * Registers the slice with the kernel: adds the slice object to the kernel
     * data structures. The slice object must have an existing database record.
     * @param slice slice to register
     * @throws Exception if the slice is already registered or a database error
     *             occurs. If a database error occurs, the slice will be
     *             unregistered.
     */
    public void reregisterSlice(final ISlice slice) throws OrcaException {
        if ((slice == null) || (slice.getSliceID() == null) || (!(slice instanceof IKernelSlice))) {
            throw new IllegalArgumentException();
        }
        
        ((IKernelSlice)slice).setOwner(actor.getIdentity());
        kernel.reregisterSlice((IKernelSlice) slice);
    }

    /**
     * Checks all reservations for completions or problems. We might want to do
     * these a few at a time.
     * @throws Exception
     */
    public void tick() throws Exception {
        kernel.tick();
    }

    /**
     * Initiates a ticket request. If the exported flag is set, this is a claim
     * on a pre-reserved "will call" ticket.
     * <p>
     * Role: Broker or Service Manager.
     * </p>
     * @param reservation reservation parameters for ticket request
     * @param destination identity of the actor the request must be sent to
     * @throws Exception
     */
    public void ticket(final IClientReservation reservation, final IActorIdentity destination) throws Exception {
        if ((reservation == null) || (destination == null) || !(reservation instanceof IKernelClientReservation)) {
            throw new IllegalArgumentException();
        }

        /*
         * Obtain a callback object to communicate with the given actor. The
         * callback type is taken to be the same as the proxy type for the
         * broker we are going to talk to.
         */
        String protocol = reservation.getBroker().getType();
        IClientCallbackProxy callback = (IClientCallbackProxy) ActorRegistry.getCallback(protocol, destination.getName());

        if (callback == null) {
            throw new RuntimeException("Unsupported protocol: " + protocol);
        }

        IKernelClientReservation rc = (IKernelClientReservation) reservation;
        rc.prepare(callback, logger);
        rc.validateOutgoing();

        handleReserve(rc, destination.getIdentity(), false, false);
    }

    /**
     * Processes an incoming request for a new ticket.
     * <p>
     * Role: Broker
     * </p>
     * @param reservation reservation representing the ticket request
     * @param caller caller identity
     * @param callback callback object
     * @param compareSequenceNumbers if true, the incoming sequence number will
     *            be compared to the local sequence number to detect fresh
     *            requests, if false, no comparison will be performed.
     * @throws Exception
     */
    public void ticketRequest(final IBrokerReservation reservation, final AuthToken caller, final IClientCallbackProxy callback, final boolean compareSequenceNumbers) throws Exception {
        if ((reservation == null) || (caller == null) || (callback == null)) {
            throw new IllegalArgumentException();
        }

        try {
            IKernelBrokerReservation r = (IKernelBrokerReservation) reservation;

            if (compareSequenceNumbers) {
                r.validateIncoming();
                /*
                 * Mark the slice as client slice.
                 */
                reservation.getSlice().setClient();
                /*
                 * This reservation has just arrived at this actor from another
                 * actor. Attach the current actor object to the reservation so
                 * that operations on this reservation can get access to it.
                 */
                r.setActor(kernel.getShirakoPlugin().getActor());

                /*
                 * If the slice referenced in the reservation exists, then we
                 * must check the incoming sequence number to determine if this
                 * request is fresh.
                 */
                IKernelSlice s = kernel.getSlice(r.getSlice().getSliceID());

                if (s != null) {
                    IKernelBrokerReservation target = (IKernelBrokerReservation) kernel.softValidate(reservation.getReservationID());

                    if (target != null) {
                        /*
                         * We already know about this reservation. Check the
                         * sequence numbers.
                         */
                        switch (kernel.compareAndUpdate(r, target)) {
                            case SequenceComparisonCodes.SequenceGreater:
                                handleUpdateReservation(target, caller);

                                break;

                            case SequenceComparisonCodes.SequenceSmaller:
                                logger.warn("Incoming request has a smaller sequence number");

                                break;

                            case SequenceComparisonCodes.SequenceInProgress:
                                logger.warn("New request for a reservation with a pending action");

                                break;

                            case SequenceComparisonCodes.SequenceEqual:
                                kernel.handleDuplicateRequest(target, RequestTypes.RequestTicket);

                                break;
                        }
                    } else {
                        /*
                         * This is a new reservation. No need to check sequence
                         * numbers.
                         */
                        r.prepare(callback, logger);
                        handleReserve(r, caller, true, true);
                    }
                } else {
                    /*
                     * New reservation for a new slice.
                     */
                    r.prepare(callback, logger);
                    handleReserve(r, caller, true, true);
                }
            } else {
                /*
                 * This is most likely a reservation being recovered. Do not
                 * compare sequence numbers, just trigger reserve.
                 */
                r.setLogger(logger);
                handleReserve(r, reservation.getClientAuthToken(), true, true);
            }
        } catch (TestException e) {
            /*
             * a test exception should not cause the reservation to fail. Pass
             * it on.
             */
            throw e;
        } catch (Exception e) {
            /*
             * We tried to process the incoming ticket request and it threw an
             * exception. What should we do? If we do nothing, the reservation
             * may remain "stuck" on the client side. If we fail the
             * reservation, we may fail a reservation that could actually be
             * allocated. The problem is that right now we do not have
             * sufficient information to determine if the reservation is
             * severely affected or not. So to avoid stuck reservations on the
             * client side, we will mark the reservation as failed and try to
             * send an update.
             */
            logger.error("ticketRequest", e);

            /*
             * TODO: We may want to delay to sending the message after the call
             * has completed.
             */
            ((BrokerReservation) reservation).failNotify(e.getMessage());
        }
    }

    /**
     * Unregisters the reservation from the kernel data structures.
     * <p>
     * <b>Note:<b> does not remove the reservation database record.
     * </p> *
     * <p>
     * <b>Note:</b>Only failed, closed, or close waiting reservations can be
     * unregistered.
     * </p>
     * @param rid identifier for reservation to unregister
     * @throws IllegalArgumentException when the passed in argument is illegal
     * @throws Exception
     */
    public void unregisterReservation(final ReservationID rid) throws Exception {
        if (rid == null) {
            throw new IllegalArgumentException();
        }

        kernel.unregisterReservation(rid);
    }

    /**
     * Unregisters the slice and releases any resources that it may hold.
     * <p>
     * <b>Note:</b> A slice can be unregistered only if it contains only closed
     * or failed reservations.
     * </p>
     * @param sliceID identifier of slice to unregister
     * @throws Exception
     */
    public void unregisterSlice(final SliceID sliceID) throws Exception {
        if (sliceID == null) {
            new IllegalArgumentException();
        }

        kernel.unregisterSlice(sliceID);
    }

    /**
     * Handles a lease update from an authority.
     * <p>
     * Role: Service Manager
     * </p>
     * @param reservation reservation describing the update
     * @param udd status of the update
     * @param caller identity of the caller
     * @throws Exception
     */
    public void updateLease(final IReservation reservation, final UpdateData udd, final AuthToken caller) throws Exception {
        if ((reservation == null) || (udd == null) || (caller == null)) {
            throw new IllegalArgumentException();
        }

        // XXX: no compare and update
        ReservationClient target = (ReservationClient) kernel.validate(reservation.getReservationID());
        
        Properties authProperties = reservation.getResources().getRequestProperties();
        
    	/* Check proxy */
     	AuthToken requester = monitor.checkProxy(caller, AbacUtil.getRequesterAuthToken(authProperties));

    	/* Check access */
        monitor.checkUpdate(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
        			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
        
        //monitor.checkUpdate(caller, target.getGuard());

        ReservationClient incoming = (ReservationClient) reservation;
        incoming.validateIncomingLease();
        kernel.updateLease(target, incoming, udd);
    }

    /**
     * Handles a ticket update from upstream broker.
     * <p>
     * Role: Agent or Service Manager.
     * </p>
     * @param reservation reservation describing the update
     * @param udd status of the update
     * @param caller identity of the caller
     * @throws Exception
     */
    public void updateTicket(final IReservation reservation, final UpdateData udd, final AuthToken caller) throws Exception {
        if ((reservation == null) || (udd == null) || (caller == null)) {
            throw new IllegalArgumentException();
        }

        /*
         * Note: use validate(ReservationID) instead of validate(Reservation) so
         * that we can make it possible to perform export/claim without knowing
         * local and remote slice identifiers.
         */
        ReservationClient target = (ReservationClient) kernel.validate(reservation.getReservationID());
        
        Properties authProperties = reservation.getResources().getRequestProperties();
        
    	/* Check proxy */
     	AuthToken requester = monitor.checkProxy(caller, AbacUtil.getRequesterAuthToken(authProperties));

    	/* Check access */
        monitor.checkUpdate(target.getSlice().getGuard(), requester, (X509Certificate)actor.getShirakoPlugin().getKeyStore().getActorCertificate(), 
        			actor.getShirakoPlugin().getKeyStore().getActorPrivateKey());
        
        //monitor.checkUpdate(caller, target.getGuard());

        // validate the incoming ticket
        ReservationClient incoming = (ReservationClient) reservation;
        incoming.validateIncomingTicket();

        kernel.updateTicket(target, incoming, udd);
    }
    
    public void processFailedRPC(ReservationID rid, FailedRPC rpc)  {
        if (rid == null) {
            throw new IllegalArgumentException();
        }

        IKernelReservation target = kernel.softValidate(rid);
        if (target == null) {
            logger.warn("Could not find reservation #" + rid.toHashString() + " while processing a failed RPC.");
            return;
        }
        
        kernel.handleFailedRPC(target, rpc);      
    }
}
