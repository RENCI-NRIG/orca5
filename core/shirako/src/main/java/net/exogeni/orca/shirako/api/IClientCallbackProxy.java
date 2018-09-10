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
 * <code>IClientCallbackProxy</code> represents the proxy callback interface to a
 * Shirako actor (only in broker and service manager role).
 */
public interface IClientCallbackProxy extends ICallbackProxy {
    public IRPCRequestState prepareUpdateTicket(IBrokerReservation reservation, UpdateData udd, ICallbackProxy callback, AuthToken caller);
}
