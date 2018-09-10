package net.exogeni.orca.policy.core.util;

import java.util.Properties;

import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IAuthorityProxy;
import net.exogeni.orca.shirako.api.ICallbackProxy;
import net.exogeni.orca.shirako.api.IClientCallbackProxy;
import net.exogeni.orca.shirako.api.IRPCRequestState;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.api.IServiceManagerCallbackProxy;
import net.exogeni.orca.shirako.api.IServiceManagerReservation;

public class DummyAuthorityProxy extends DummyProxy implements IAuthorityProxy {
    public DummyAuthorityProxy() {
    }
    
    public DummyAuthorityProxy(AuthToken auth) {
        super(auth);
    }
    
    public IRPCRequestState prepareRedeem(IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback, AuthToken caller) {
        return null;
    }
    
    public IRPCRequestState prepareExtendLease(IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback, AuthToken caller) {
        return null;
    }    
    
    public IRPCRequestState prepareModifyLease(IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback, AuthToken caller) {
        return null;
    }
    
    public IRPCRequestState prepareClose(IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback, AuthToken caller) {
        return null;
    }
    
    public IRPCRequestState prepareTicket(IReservation reservation, IClientCallbackProxy callback, AuthToken caller) {
        return null;
    }

    public IRPCRequestState prepareClaim(IReservation reservation, IClientCallbackProxy callback, AuthToken caller) {
        return null;
    }
    
    public IRPCRequestState prepareExtendTicket(IReservation reservation, IClientCallbackProxy callback, AuthToken caller){
        return null;
    }

    public IRPCRequestState prepareRelinquish(IReservation reservation, IClientCallbackProxy callback, AuthToken caller) {
        return null;
    }

    public IRPCRequestState prepareQuery(ICallbackProxy callback, Properties query, AuthToken caller) {
        return null;
    }
}
