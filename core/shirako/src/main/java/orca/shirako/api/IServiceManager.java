/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.api;

import java.util.Properties;

import orca.shirako.util.ReservationSet;


/**
 * <code>IServiceManager</code> defines the interface for a Shirako actor
 * acting in the service manager role.
 */
public interface IServiceManager extends IClientActor, IServiceManagerPublic
{
    /**
     * Issues an extend lease request for the given reservation. Note:
     * the reservation must have already been registered with the actor.
     *
     * @param reservation reservation to be redeemed
     *
     * @throws Exception
     *
     * @see IReservationOperations#register(orca.shirako.kernel.Reservation)
     */
    public void extendLease(IServiceManagerReservation reservation) throws Exception;

    /**
     * Issues an extend lease request for every reservation in the set.
     * All exceptions are caught and logged but no exception is propagated. No
     * information will be delivered to indicate that some failure has taken
     * place, e.g., failure to communicate with a broker. Inspect the state of
     * individual reservations to determine whether/what failures have taken
     * place.
     *
     * @param set set of reservations to extend the lease for
     */
    public void extendLease(ReservationSet set) throws Exception;

    /**
     * Issues a redeem request for the given reservation. Note: the
     * reservation must have already been registered with the actor.
     *
     * @param reservation reservation to be redeemed
     *
     * @throws Exception
     *
     * @see IReservationOperations#register(orca.shirako.kernel.Reservation)
     */
    public void redeem(IServiceManagerReservation reservation) throws Exception;

    /**
     * Issues a redeem request for every reservation in the set. All
     * exceptions are caught and logged but no exception is propagated. No
     * information will be delivered to indicate that some failure has taken
     * place, e.g., failure to communicate with a broker. Inspect the state of
     * individual reservations to determine whether/what failures have taken
     * place.
     *
     * @param set set of reservations to redeem
     */
    public void redeem(ReservationSet set);
    
}