package orca.shirako.kernel;

import java.util.TimerTask;

import orca.shirako.api.IActor;
import orca.shirako.api.IProxy;
import orca.shirako.api.IRPCRequestState;
import orca.shirako.api.IRPCResponseHandler;
import orca.shirako.api.IReservation;

/**
 * Represents an outgoing RCP request managed by the <code>RPCManager</code>
 * @author aydan
 *
 */
public class RPCRequest {
    protected IRPCRequestState request;
    protected IActor actor;
    protected IProxy proxy;
    protected IReservation reservation;
    protected int sequence;
    protected IRPCResponseHandler handler;
    protected int retryCount;
    protected TimerTask timer;
    
    public RPCRequest(IRPCRequestState request, IActor actor, IProxy proxy, IReservation reservation, int sequence) {
        this.request = request;
        this.actor = actor;
        this.proxy = proxy;
        this.reservation = reservation;
        this.sequence = sequence;
    }
    
    public RPCRequest(IRPCRequestState state, IActor actor, IProxy proxy, IRPCResponseHandler handler) {
        this(state, actor, proxy, null, 0);
        this.handler = handler;
    }
    
    public RPCRequest(IRPCRequestState state, IActor actor, IProxy proxy) {
        this(state, actor, proxy, null);
    }

    public IActor getActor() {
        return actor;
    }
    
    public IReservation getReservation() {
        return reservation;
    }
    
    public IRPCResponseHandler getHandler() {
        return handler;
    }
    
    public RPCRequestType getRequestType() {
        return request.getType();
    }
    
    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }
}