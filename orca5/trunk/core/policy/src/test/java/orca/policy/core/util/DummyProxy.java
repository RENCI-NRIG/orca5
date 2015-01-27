package orca.policy.core.util;

import orca.security.AuthToken;
import orca.shirako.api.IRPCRequestState;
import orca.shirako.api.IReservation;
import orca.shirako.core.RPCRequestState;
import orca.shirako.proxies.Proxy;

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
