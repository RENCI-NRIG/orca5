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

import orca.shirako.api.IAuthorityReservation;


/**
 * Kernel-level interface for authority reservations.
 */
public interface IKernelAuthorityReservation extends IKernelServerReservation, IAuthorityReservation
{
    /**
     * Prepare for an incoming extend request on this existing
     * reservation. Note: unlocked
     *
     * @throws Exception thrown if request is rejected (e.g., ticket not valid)
     */
    public void prepareExtendLease() throws Exception;
    public void prepareModifyLease() throws Exception;
}