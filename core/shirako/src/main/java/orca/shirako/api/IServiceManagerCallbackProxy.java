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
 * <code>IServiceManagerCallbackProxy</code> represents the proxy callback interface to a
 * Shirako actor acting in the service manager role.
 */
public interface IServiceManagerCallbackProxy extends IClientCallbackProxy {
    public IRPCRequestState prepareUpdateLease(IAuthorityReservation reservation, UpdateData udd, ICallbackProxy callback, AuthToken caller);
}