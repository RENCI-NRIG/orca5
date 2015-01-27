/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.policy.core;

import java.util.Properties;

import orca.security.AuthToken;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.ICallbackProxy;
import orca.shirako.api.IClientCallbackProxy;
import orca.shirako.api.IProxy;
import orca.shirako.api.IRPCRequestState;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServerReservation;
import orca.shirako.common.ReservationID;
import orca.shirako.container.Globals;
import orca.shirako.core.RPCRequestState;
import orca.shirako.kernel.RPCRequestType;
import orca.shirako.proxies.local.LocalReturn;
import orca.shirako.registry.ActorRegistry;
import orca.shirako.util.UpdateData;
import orca.util.ID;

import org.apache.log4j.Logger;

public class ClientCallbackHelper implements IClientCallbackProxy {
    protected class MyRequestState extends RPCRequestState {
        IReservation r;
        UpdateData udd;
    };
    
    protected AuthToken token;
    protected int called = 0;
    protected int prepared = 0;
    
    protected IReservation reservation;

    public ClientCallbackHelper(String name, ID id) {
        token = new AuthToken(name, id);
    }
    
    public int getCalled() {
        return called;
    }

    public ID getGuid() {
        return token.getGuid();
    }

    public AuthToken getIdentity() {
        return token;
    }

    public String getName() {
        return token.getName();
    }

    public IReservation getReservation() {
        return reservation;
    }

    public String getType() {
        return IProxy.ProxyTypeLocal;
    }

    public void _reset(Properties p) {
    }

    public Properties _save() {
        Properties p = new Properties();
        _save(p);

        return p;
    }

    public void _save(Properties p) {
    }

    public IRPCRequestState prepareUpdateTicket(IBrokerReservation reservation, UpdateData udd, ICallbackProxy callback, AuthToken caller) {
        MyRequestState state = new MyRequestState();
        state.r = LocalReturn.passReservation((IServerReservation) reservation, ActorRegistry.getActor(token.getName()).getShirakoPlugin());
        prepared++;
        return state;
    }
    
    public void execute(IRPCRequestState state) {
        if (state.getType() == RPCRequestType.UpdateTicket) {
            called++;
            this.reservation = ((MyRequestState)state).r;
        }
    }
    
    public Logger getLogger() {
        return Globals.Log;
    }

    public IRPCRequestState prepareQueryResult(String requestID, Properties response, AuthToken caller) {
        throw new RuntimeException("Not implemented");
    }
    
    public IRPCRequestState prepareFailedRPC(String requestID, RPCRequestType failedRequestType, ReservationID failedReservationID, String errorDetail, AuthToken caller) {
        throw new RuntimeException("Not implemented");
    }        
}
