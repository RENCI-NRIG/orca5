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

import orca.security.AuthToken;

import orca.shirako.util.UpdateData;


/**
 * <code>IServiceManagerPublic</code> represents the public cross-actor
 * interface for actors acting in the service manager role.
 */
public interface IServiceManagerPublic
{
    /**
     * Handles an incoming lease update.
     *
     * @param reservation reservation represented by this update. The
     *        reservation object will contain the lease (if any) as well
     *        information about the actually leased resources.
     * @param udd status of the remote operation.
     * @param caller identity of the caller
     *
     * @throws Exception in case of error
     */
    public void updateLease(IReservation reservation, UpdateData udd, AuthToken caller)
                     throws Exception;
}
