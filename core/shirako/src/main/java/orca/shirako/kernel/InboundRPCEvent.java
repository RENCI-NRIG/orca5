package orca.shirako.kernel;

import java.util.Properties;

import orca.shirako.api.IActor;
import orca.shirako.api.IEvent;
import orca.util.ID;

public class InboundRPCEvent implements IEvent {
    private IncomingRPC request;
    private IActor actor;
    
    public InboundRPCEvent(IncomingRPC request, IActor actor) {
        this.request = request;
        this.actor = actor;
    }

    public ID getActorID() {
        return actor.getGuid();
    }

    public Properties getProperties() {
        return null;
    }
    
    public IncomingRPC GetRequest() {
        return request;
    }
    
    public IActor getActor() {
        return actor;
    }
}