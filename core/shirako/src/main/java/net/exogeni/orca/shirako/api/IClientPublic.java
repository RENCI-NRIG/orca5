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

import net.exogeni.orca.shirako.util.UpdateData;


/**
 * <code>IClientPublic</code> represents the public cross-actor interface for
 * actors acting as clients of other actors.
 */
public interface IClientPublic
{
    /**
     * Handles an incoming ticket update.
     *
     * @param reservation reservation represented by this update. The
     *        reservation object will contain the ticket (if any) as well
     *        information about the actually allocated resources.
     * @param udd status of the remote operation.
     * @param caller identity of the caller
     *
     * @throws Exception in case of error
     */
    public void updateTicket(IReservation reservation, UpdateData udd, AuthToken caller)
                      throws Exception;
}
