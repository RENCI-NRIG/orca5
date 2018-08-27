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


/**
 * <code>IBroker</code> defines the interface for a Shirako actor acting in
 * the broker role.
 */
public interface IBroker extends IClientActor, IServerActor
{
    /**
     * Processes an extend ticket request for the reservation.
     *
     * @param reservation reservation representing a request for a ticket
     *        extension
     *
     * @throws Exception in case of error
     */
    public void extendTicket(IBrokerReservation reservation) throws Exception;

    /**
     * Processes a ticket request for the reservation.
     *
     * @param reservation reservation representing a request for a new ticket
     *
     * @throws Exception in case of error
     */
    public void ticket(IBrokerReservation reservation) throws Exception;
}
