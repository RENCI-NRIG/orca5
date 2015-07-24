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

import java.util.Properties;

import orca.shirako.api.IActor;
import orca.shirako.api.ICallbackProxy;
import orca.shirako.api.IPolicy;
import orca.shirako.api.IReservation;
import orca.shirako.api.ISlice;
import orca.shirako.util.ReservationState;
import orca.shirako.util.UpdateData;

import org.apache.log4j.Logger;


/**
 * Kernel-level reservation interface.
 */
interface IKernelReservation extends IReservation
{
    /**
     * Sets the slice the reservation belongs to.
     *
     * @param slice slice the reservation belongs to
     */
    public void setSlice(ISlice slice);
    
    /**
     * Checks if the reservation can be redeemed at the current time.
     *
     * @return true if the reservation's current state allows it to be redeemed
     */
    public boolean canRedeem();

    /**
     * Checks if this reservation can be renewed at the current time.
     *
     * @return true if the reservation's current state allows it to be renewed
     */
    public boolean canRenew();

    /**
     * Claims an exported "will call" reservation.
     *
     * @throws Exception
     */
    public void claim() throws Exception;

    /**
     * Closes the reservation. Locked with the kernel lock.
     */
    public void close();

    /**
     * Extends the reservation.
     *
     * @throws Exception
     */
    public void extendLease() throws Exception;
    
    /**
     * Modifies the reservation.
     *
     * @throws Exception
     */
    public void modifyLease() throws Exception;
    
    // XXX: we pass the actor to extendTicket, so that we can distinguish
    // between service managers and brokers.
    // This is a hack. The proper solution is to separate ReservationClient into
    // two files. Would/should do it some day.
    /**
     * Extends the ticket.
     *
     * @param actor actor
     *
     * @throws Exception
     */
    public void extendTicket(IActor actor) throws Exception;

    /**
     * Returns the kernel slice.
     *
     * @return kernel slice
     */
    public IKernelSlice getKernelSlice();

    /**
     * Handles a duplicate request.
     *
     * @param operation operation type code
     */
    public void handleDuplicateRequest(int operation) throws Exception;
    /**
     * Prepares for a ticket request on a new reservation object.
     *
     * @param callback callback object
     * @param logger for diagnostic logging
     */
    public void prepare(ICallbackProxy callback, Logger logger) throws Exception;

    /**
     * Prepares a reservation probe.
     *
     * @throws Exception
     */
    public void prepareProbe() throws Exception;

    /**
     * Probe a reservation with a pending request. On server, if the
     * operation completed, handle it and generate an update. If no pending
     * request completed then do nothing.
     *
     * @throws Exception
     */
    public void probePending() throws Exception;

    /**
     * Reserve resources: ticket() initiate or request, or redeem()
     * request. New reservation.
     *
     * @param policy the mapper for the reservation
     *
     * @throws Exception
     */
    public void reserve(IPolicy policy) throws Exception;

    /**
     * Saves enough information to identify the reservation.
     *
     * @return properties list with identifying information.
     *
     * @throws Exception
     */
    public Properties saveID() throws Exception;

    /**
     * Finishes processing claim.
     *
     * @throws Exception
     */
    public void serviceClaim() throws Exception;

    /**
     * Finishes processing close.
     */
    public void serviceClose();

    /**
     * Finishes processing extend lease.
     *
     * @throws Exception
     */
    public void serviceExtendLease() throws Exception;

    /**
     * Finishes processing extend lease.
     *
     * @throws Exception
     */
    public void serviceModifyLease() throws Exception;
    
    /**
     * Finishes processing extend ticket.
     *
     * @throws Exception
     */
    public void serviceExtendTicket() throws Exception;

    /**
     * Finishes processing probe.
     *
     * @throws Exception
     */
    public void serviceProbe() throws Exception;

    /**
     * Finishes processing reserve.
     *
     * @throws Exception
     */
    public void serviceReserve() throws Exception;

    /**
     * Finishes processing update lease.
     */
    public void serviceUpdateLease() throws Exception;

    /**
     * Finishes processing update ticket.
     *
     * @throws Exception
     */
    public void serviceUpdateTicket() throws Exception;

    /**
     * Sets the actor in control of the reservation.
     *
     * @param actor actor in control of the reservation
     */
    public void setActor(IActor actor);

    /**
     * Attaches the logger to use for the reservation.
     *
     * @param logger logger object
     */
    public void setLogger(Logger logger);

    /**
     * Indicates there is a pending operation on the reservation.
     *
     * @param code operation code
     */
    public void setServicePending(int code);

    /**
     * Handles an incoming lease update.
     *
     * @param incoming incoming lease update
     * @param udd update data
     *
     * @throws Exception thrown if lease update is for an un-ticketed
     *         reservation
     * @throws Exception thrown if lease update is for closed reservation
     */
    public void updateLease(IReservation incoming, UpdateData udd) throws Exception;

    /**
     * Handles an incoming ticket update.
     *
     * @param incoming incoming ticket update
     * @param udd update data
     *
     * @throws Exception
     */
    public void updateTicket(IReservation incoming, UpdateData udd) throws Exception;

    /**
     * Validates a reservation as it arrives at an actor.
     */
    public void validateIncoming() throws Exception;

    /**
     * Validates a reservation as it is about to leave an actor.
     *
     * @throws Exception
     */
    public void validateOutgoing() throws Exception;

    /**
     * Processes a failed RPC request.
     * @param failed
     */
    public void handleFailedRPC(FailedRPC failed);
}