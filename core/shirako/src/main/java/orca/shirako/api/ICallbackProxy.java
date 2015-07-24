/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.api;

import java.util.Properties;

import orca.security.AuthToken;
import orca.shirako.common.ReservationID;
import orca.shirako.kernel.RPCRequestType;

/**
 * <code>ICallbackProxy</code> represents the proxy callback interface to a
 * Shirako actor.
 */
public interface ICallbackProxy extends IProxy {
    public IRPCRequestState prepareQueryResult(String requestID, Properties response, AuthToken caller);
    public IRPCRequestState prepareFailedRPC(String requestID, RPCRequestType failedRequestType, ReservationID failedReservationID, String errorDetail, AuthToken caller); 
}
