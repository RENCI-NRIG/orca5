/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.kernel;

import net.exogeni.orca.shirako.api.IServiceManagerReservation;


/**
 * Kernel-level interface for service manager reservations.
 */
public interface IKernelServiceManagerReservation extends IKernelClientReservation,
                                                          IServiceManagerReservation
{
    /**
     * Prepares the reservation for processing a join operation.
     * Invoked internally before processing joins on an arriving initial lease
     * for a reservation. This gives subclasses an opportunity to manipulate
     * the property list or other attributes prior to the join. void.
     * @throws Exception in case of error
     */
    public void prepareJoin() throws Exception;

    /**
     * Prepares the reservation for processing a redeem operation.
     * Invoked internally before any initial redeem operation on a
     * reservation. This gives subclasses an opportunity to manipulate the
     * property list or other attributes prior to the redeem.
     * @throws Exception in case of error
     */
    public void prepareRedeem() throws Exception;

    /**
     * Validates an outgoing redeem request.
     *
     * @throws Exception if validation fails
     */
    public void validateRedeem() throws Exception;
}
