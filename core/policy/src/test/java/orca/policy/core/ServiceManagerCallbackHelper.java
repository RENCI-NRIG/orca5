package orca.policy.core;

import orca.security.AuthToken;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.ICallbackProxy;
import orca.shirako.api.IRPCRequestState;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServerReservation;
import orca.shirako.api.IServiceManagerCallbackProxy;
import orca.shirako.kernel.RPCRequestType;
import orca.shirako.proxies.local.LocalReturn;
import orca.shirako.registry.ActorRegistry;
import orca.shirako.util.UpdateData;
import orca.util.ID;
import orca.util.persistence.NotPersistent;

public class ServiceManagerCallbackHelper extends ClientCallbackHelper implements IServiceManagerCallbackProxy {
    public interface IUpdateLeaseHandler {
        public void handleUpdateLease(IReservation reservation, UpdateData udd, AuthToken caller);

        public void checkTermination();
    }

    @NotPersistent
    protected int calledForLease = 0;

    @NotPersistent
    protected IReservation lease;

    @NotPersistent
    protected IUpdateLeaseHandler updateLeaseHandler;

    public ServiceManagerCallbackHelper(String name, ID id) {
        super(name, id);
    }

    public IRPCRequestState prepareUpdateLease(IAuthorityReservation reservation, UpdateData udd, ICallbackProxy callback, AuthToken caller) {
       MyRequestState state = new MyRequestState();
       state.r = LocalReturn.passReservation((IServerReservation) reservation, ActorRegistry.getActor(token.getName()).getShirakoPlugin());
       state.udd = new UpdateData();
       state.udd.absorb(udd);
       return state;
    }

    public void execute(IRPCRequestState state) {
        MyRequestState my = (MyRequestState)state;
        if (state.getType() == RPCRequestType.UpdateLease) {
            lease = my.r;
            calledForLease++;
    
            this.lease = my.r;
    
            if (updateLeaseHandler != null) {
                updateLeaseHandler.handleUpdateLease(lease, my.udd, state.getCaller());
            }
        } else {
            super.execute(state);
        }
    }

    public IReservation getLease() {
        return lease;
    }

    public int getCalledForLease() {
        return calledForLease;
    }

    public void setUpdateLeaseHandler(IUpdateLeaseHandler handler) {
        this.updateLeaseHandler = handler;
    }
}
