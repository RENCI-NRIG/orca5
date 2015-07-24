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


/**
 * <code>IBrokerProxy</code> represents the proxy interface to a Shirako actor
 * acting in the broker role.
 */
public interface IBrokerProxy extends IServerProxy
{
    public IRPCRequestState prepareClaim(IReservation reservation, IClientCallbackProxy callback, AuthToken caller);
    public IRPCRequestState prepareTicket(IReservation reservation, IClientCallbackProxy callback, AuthToken caller);
    public IRPCRequestState prepareExtendTicket(IReservation reservation, IClientCallbackProxy callback, AuthToken caller);
    public IRPCRequestState prepareRelinquish(IReservation reservation, IClientCallbackProxy callback, AuthToken caller);
}