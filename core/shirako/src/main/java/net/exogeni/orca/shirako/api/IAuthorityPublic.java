/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.api;

import net.exogeni.orca.security.AuthToken;


/**
 * <code>IAuthorityPublic</code> represents the public cross-actor interface
 * for a Shirako site authority.
 */
public interface IAuthorityPublic
{
    /**
     * Closes the reservation.
     *
     * @param reservation the reservation
     * @param caller the slice owner
     *
     * @throws Exception in case of error
     */
    public void close(IReservation reservation, AuthToken caller) throws Exception;

    /**
     * Extends a lease.
     *
     * @param reservation reservation to extend
     * @param caller owner of the reservation
     *
     * @throws Exception in case of error
     */
    public void extendLease(IReservation reservation, AuthToken caller) throws Exception;

    /**
     * Modifies a lease.
     *
     * @param reservation reservation to modify
     * @param caller owner of the reservation
     *
     * @throws Exception in case of error
     */
    public void modifyLease(IReservation reservation, AuthToken caller) throws Exception;
    
    /**
     * Redeems a lease.
     *
     * @param reservation reservation to redeem
     * @param callback callback object
     * @param caller owner of the reservation
     *
     * @throws Exception in case of error
     */
    public void redeem(IReservation reservation, IServiceManagerCallbackProxy callback,
                       AuthToken caller) throws Exception;
}
