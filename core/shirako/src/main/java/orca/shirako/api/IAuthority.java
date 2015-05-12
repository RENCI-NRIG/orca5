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

import orca.shirako.kernel.ResourceSet;


/**
 * <code>IAuthority</code> defines the interface for a Shirako actor acting in
 * the authority role.
 */
public interface IAuthority extends IServerActor, IAuthorityPublic
{
    /**
     * Informs the actor that the following resources are available for
     * allocation.
     *
     * @param resources
     *
     * @throws Exception
     */
    public void available(ResourceSet resources) throws Exception;

    /**
     * Accepts concrete resources to be used for allocation of client
     * requests.
     *
     * @param resources resource set representing resources to be used for
     *        allocation
     *
     * @throws Exception
     */
    public void donate(ResourceSet resources) throws Exception;

    /**
     * Ejects the specified resources from the inventory.
     *
     * @param resources
     *
     * @throws Exception
     */
    public void eject(ResourceSet resources) throws Exception;

    /**
     * Processes an extend lease request for the reservation.
     *
     * @param reservation reservation representing a request for a lease
     *        extension
     *
     * @throws Exception
     */
    public void extendLease(IAuthorityReservation reservation) throws Exception;

    /**
     * Processes an modify lease request for the reservation.
     *
     * @param reservation reservation representing a request for a lease
     *        modification
     *
     * @throws Exception
     */
    public void modifyLease(IAuthorityReservation reservation) throws Exception;
    
    /**
     * Informs the actor that the given resources are no longer in use
     * and can be considered as free, regardless of the state of the
     * individual units.
     *
     * @param resources resource set representing freed resources
     *
     * @throws Exception
     */
    public void freed(ResourceSet resources) throws Exception;

    /**
     * Processes a redeem request for the reservation.
     *
     * @param reservation reservation representing a request for a new lease
     *
     * @throws Exception
     */
    public void redeem(IAuthorityReservation reservation) throws Exception;

    /**
     * Informs the actor that previously donated resources are no
     * longer available for allocation.
     *
     * @param resources
     *
     * @return DOCUMENT ME!
     *
     * @throws Exception
     */
    public int unavailable(ResourceSet resources) throws Exception;

    // XXX: it may be necessary to include extendTicket(BrokerReservation) and
    // ticket(BrokerReservation) from IBroker.
}