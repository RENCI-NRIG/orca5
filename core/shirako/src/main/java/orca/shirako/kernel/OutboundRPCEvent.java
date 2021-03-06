package orca.shirako.kernel;

import java.util.Properties;

import orca.shirako.api.IEvent;
import orca.util.ID;

public class OutboundRPCEvent implements IEvent {
    private RPCRequest request;

    public OutboundRPCEvent(RPCRequest request) {
        this.request = request;
    }

    public ID getActorID() {
        return request.getActor().getGuid();
    }

    public Properties getProperties() {
        return null;
    }
    
    public RPCRequest getRequest() {
        return request;
    }
}