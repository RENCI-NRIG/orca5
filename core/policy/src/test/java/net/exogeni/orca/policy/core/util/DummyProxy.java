package net.exogeni.orca.policy.core.util;

import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IRPCRequestState;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.core.RPCRequestState;
import net.exogeni.orca.shirako.proxies.Proxy;

public class DummyProxy extends Proxy {
    protected class MyRequestState extends RPCRequestState {
        public IReservation r;
    };
    
    public DummyProxy() {
        proxyType = ProxyTypeLocal;
    }

    public DummyProxy(AuthToken auth) {
    	super(auth);
        proxyType = ProxyTypeLocal;
    }

    public void execute(IRPCRequestState state) {
        throw new RuntimeException("Not supported");
    }
}
