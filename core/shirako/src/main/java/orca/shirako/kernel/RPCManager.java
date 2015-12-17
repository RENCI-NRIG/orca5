package orca.shirako.kernel;

import java.util.HashMap;
import java.util.Properties;
import java.util.TimerTask;

import orca.security.AuthToken;
import orca.shirako.api.IActor;
import orca.shirako.api.IActorProxy;
import orca.shirako.api.IAuthorityProxy;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.api.ICallbackProxy;
import orca.shirako.api.IClientCallbackProxy;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IQueryResponseHandler;
import orca.shirako.api.IRPCRequestState;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManagerCallbackProxy;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.common.ReservationID;
import orca.shirako.container.Globals;
import orca.shirako.proxies.Proxy;
import orca.shirako.util.RPCError;
import orca.shirako.util.RPCException;
import orca.shirako.util.RemoteActorException;
import orca.shirako.util.UpdateData;
import orca.util.ExceptionUtils;
import orca.util.IOrcaTimerTask;
import orca.util.OrcaRuntimeException;
import orca.util.OrcaThreadPool;
import orca.util.OrcaThreadPool.CannotExecuteException;
import orca.util.OrcaTimer;

public class RPCManager {
    public static final long CLAIM_TIMEOUT_MS = 120000;
    public static final long QUERY_TIMEOUT_MS = 120000;

    private static RPCManager instance = new RPCManager();

    /**
     * Table of pending RPC requests.
     */
    private HashMap<String, RPCRequest> pending;

    private volatile boolean started = false;

    private RPCManager() {
        pending = new HashMap<String, RPCRequest>();
    }

    public static void start() {
        instance.doStart();
    }

    public static void stop() {
        instance.doStop();
    }

    private static void validate(IReservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("Missing reservation");
        }

        if (reservation.getSlice() == null) {
            throw new IllegalArgumentException("Missing slice");
        }

        /*
         * if (reservation.getSlice().getOwner() == null) { throw new
         * IllegalArgumentException("Missing owner/caller"); }
         */
    }

    private static void validate(IClientReservation reservation, boolean checkRequested) {
        validate((IReservation) reservation);
        if (checkRequested) {
            if (reservation.getRequestedResources() == null) {
                throw new IllegalArgumentException("Missing requested resources");
            }

            if (reservation.getRequestedTerm() == null) {
                throw new IllegalArgumentException("Missing requested term");
            }
        }

        if (reservation.getBroker() == null) {
            throw new IllegalArgumentException("Missing broker proxy");
        }

        if (reservation.getClientCallbackProxy() == null) {
            throw new IllegalArgumentException("Missing client callback proxy");
        }
    }

    private static void validate(IServiceManagerReservation reservation, boolean checkRequested) {
        validate((IReservation) reservation);
        if (checkRequested) {
            if (reservation.getRequestedResources() == null) {
                throw new IllegalArgumentException("Missing requested resources");
            }

            if (reservation.getRequestedTerm() == null) {
                throw new IllegalArgumentException("Missing requested term");
            }
        }

        if (reservation.getAuthority() == null) {
            throw new IllegalArgumentException("Missing authority proxy");
        }

        if (reservation.getClientCallbackProxy() == null) {
            throw new IllegalArgumentException("Missing client callback proxy");
        }
    }

    public static void retry(RPCRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Missing request");
        }
        instance.doRetry(request);
    }

    public static void failedRPC(IActor actor, IncomingRPC rpc, Throwable cause) {
        if (actor == null) {
            throw new IllegalArgumentException("Missing actor");
        }
        if (rpc == null) {
            throw new IllegalArgumentException("Missing rpc");
        }
        if (rpc.getCallback() == null) {
            throw new IllegalArgumentException("No callback in rpc.");
        }
        if (rpc instanceof IncomingFailedRPC) {
            throw new IllegalArgumentException("Cannot reply to a FailedRPC witha FailedRPC");
        }
        instance.doFailedRPC(actor, rpc.getCallback(), rpc, cause, actor.getIdentity());
    }

    public static void claim(IClientReservation reservation) {
        validate(reservation, false);
        instance.doClaim(reservation.getActor(),
                reservation.getBroker(),
                reservation,
                reservation.getClientCallbackProxy(),
                reservation.getSlice().getOwner());
    }

    public static void ticket(IClientReservation reservation) {
        validate(reservation, true);
        instance.doTicket(reservation.getActor(),
                reservation.getBroker(),
                reservation,
                reservation.getClientCallbackProxy(),
                reservation.getSlice().getOwner());
    }

    public static void extendTicket(IClientReservation reservation) {
        validate(reservation, true);
        instance.doExtendTicket(reservation.getActor(),
                reservation.getBroker(),
                reservation,
                reservation.getClientCallbackProxy(),
                reservation.getSlice().getOwner());
    }

    public static void relinquish(IClientReservation reservation) {
        validate(reservation, false);
        instance.doRelinquish(reservation.getActor(),
                reservation.getBroker(),
                reservation,
                reservation.getClientCallbackProxy(),
                reservation.getSlice().getOwner());
    }

    public static void redeem(IServiceManagerReservation reservation) {
        validate(reservation, true);
        instance.doRedeem(reservation.getActor(),
                reservation.getAuthority(),
                reservation,
                (IServiceManagerCallbackProxy) reservation.getClientCallbackProxy(),
                reservation.getSlice().getOwner());
    }

    public static void extendLease(IAuthorityProxy proxy, IServiceManagerReservation reservation,
            AuthToken caller) {
        validate(reservation, true);
        instance.doExtendLease(reservation.getActor(),
                reservation.getAuthority(),
                reservation,
                (IServiceManagerCallbackProxy) reservation.getClientCallbackProxy(),
                reservation.getSlice().getOwner());
    }
    
    public static void modifyLease(IAuthorityProxy proxy, IServiceManagerReservation reservation,
            AuthToken caller) {
        validate(reservation, true);
        instance.doModifyLease(reservation.getActor(),
                reservation.getAuthority(),
                reservation,
                (IServiceManagerCallbackProxy) reservation.getClientCallbackProxy(),
                reservation.getSlice().getOwner());
    }

    public static void close(IServiceManagerReservation reservation) {
        validate(reservation, false);
        instance.doClose(reservation.getActor(),
                reservation.getAuthority(),
                reservation,
                (IServiceManagerCallbackProxy) reservation.getClientCallbackProxy(),
                reservation.getSlice().getOwner());
    }

    public static void updateTicket(IBrokerReservation reservation) {
        validate(reservation);
        // get a callback to the actor calling updateTicket, so that any
        // failures in the remote actor can be delivered back
        ICallbackProxy callback = Proxy.getCallback(reservation.getActor(),
                reservation.getCallback().getType());
        if (callback == null) {
            throw new RuntimeException("Missing callback");
        }
        instance.doUpdateTicket(reservation.getActor(),
                (IClientCallbackProxy) reservation.getCallback(),
                reservation,
                reservation.getUpdateData(),
                callback,
                reservation.getActor().getIdentity());
    }

    public static void updateLease(IAuthorityReservation reservation) {
        validate(reservation);
        // get a callback to the actor calling updateTicket, so that any
        // failures in the remote actor can be delivered back
        ICallbackProxy callback = Proxy.getCallback(reservation.getActor(),
                reservation.getCallback().getType());
        if (callback == null) {
            throw new RuntimeException("Missing callback");
        }
        instance.doUpdateLease(reservation.getActor(),
                (IServiceManagerCallbackProxy) reservation.getCallback(),
                reservation,
                reservation.getUpdateData(),
                callback,
                reservation.getActor().getIdentity());
    }

    public static void query(IActor actor, IActorProxy remoteActor, ICallbackProxy callback,
            Properties query, IQueryResponseHandler handler) {
        if (actor == null) {
            throw new IllegalArgumentException("Missing actor");
        }
        if (remoteActor == null) {
            throw new IllegalArgumentException("Missing remoteActor");
        }
        if (callback == null) {
            throw new IllegalArgumentException("Missing callback");
        }
        if (query == null) {
            throw new IllegalArgumentException("Missing query");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Missing handler");
        }
        instance.doQuery(actor, remoteActor, callback, query, handler, callback.getIdentity());
    }

    public static void queryResult(IActor actor, ICallbackProxy remoteActor, String requestID,
            Properties response, AuthToken caller) {
        if (actor == null) {
            throw new IllegalArgumentException("Missing actor");
        }
        if (remoteActor == null) {
            throw new IllegalArgumentException("Missing remoteActor");
        }
        if (requestID == null) {
            throw new IllegalArgumentException("Missing requestID");
        }
        if (response == null) {
            throw new IllegalArgumentException("Missing response");
        }
        if (caller == null) {
            throw new IllegalArgumentException("Missing caller");
        }
        instance.doQueryResult(actor, remoteActor, requestID, response, caller);
    }

    public static void dispatchIncoming(IActor actor, IncomingRPC rpc) throws RemoteActorException {
        try {
            if (actor == null) {
                throw new IllegalArgumentException("actor cannot be null");
            }
            if (rpc == null) {
                throw new IllegalArgumentException("rpc cannot be null");
            }
            instance.doDispatchIncomingRPC(actor, rpc);
        } catch (Exception e) {
            throw new RemoteActorException("An error occurred while dispatching inbound RPC", e);
        }
    }

    public static void awaitNothingPending() throws InterruptedException {
        instance.doAwaitNothingPending();
    }

    private void doAwaitNothingPending() throws InterruptedException {
        synchronized (statsLock) {
            while (numQueued > 0) {
                statsLock.wait();
            }
        }
    }

    private void doStart() {
        synchronized (pending) {
            pending.clear();
        }
        started = true;
    }

    private void doStop() {
        started = false;
        synchronized (pending) {
            pending.clear();
        }
    }

    private void doFailedRPC(IActor actor, ICallbackProxy proxy, IncomingRPC rpc, Throwable cause,
            AuthToken caller) {
        proxy.getLogger().info("Outbound failedRPC request from <" + caller.getName()
                + ">: requestID=" + rpc.getMessageID());
        String message = "RPC failed at remote actor.";
        if (cause != null) {
            message += " message:" + cause.getMessage();
            message += " " + ExceptionUtils.getStackTraceString(cause.getStackTrace());
        }
        // extract a reservation ID, if possible
        ReservationID rid = null;
        if (rpc instanceof IncomingReservationRPC) {
            rid = ((IncomingReservationRPC) rpc).getReservation().getReservationID();
        }
        // prepare the arguments for the RPC
        IRPCRequestState state = proxy.prepareFailedRPC(rpc.getMessageID(),
                rpc.getRequestType(),
                rid,
                message,
                caller);
        state.setCaller(caller);
        state.setType(RPCRequestType.FailedRPC);
        // create the request object
        RPCRequest outgoing = new RPCRequest(state, actor, proxy);
        // schedule this RPC call for execution
        enqueue(outgoing);
    }

    private void doClaim(IActor actor, IBrokerProxy proxy, IClientReservation reservation,
            IClientCallbackProxy callback, AuthToken caller) {
        proxy.getLogger().info("Outbound claim request from <" + caller.getName() + ">: "
                + reservation.toLogString());
        // call the proxy to prepare the state for the RPC request.
        IRPCRequestState state = proxy.prepareClaim(reservation, callback, caller);
        state.setCaller(caller);
        state.setType(RPCRequestType.Claim);
        // create the request object
        RPCRequest rpc = new RPCRequest(state,
                actor,
                proxy,
                reservation,
                reservation.getTicketSequenceOut());
        // schedule a timeout timer
        rpc.timer = OrcaTimer.schedule(actor, new ClaimTimeout(rpc), CLAIM_TIMEOUT_MS);
        // schedule this RPC call for execution
        enqueue(rpc);
    }

    private void doTicket(IActor actor, IBrokerProxy proxy, IClientReservation reservation,
            IClientCallbackProxy callback, AuthToken caller) {
        proxy.getLogger().info("Outbound ticket request from <" + caller.getName() + ">: "
                + reservation.toLogString());
        IRPCRequestState state = proxy.prepareTicket(reservation, callback, caller);
        state.setCaller(caller);
        state.setType(RPCRequestType.Ticket);
        RPCRequest rpc = new RPCRequest(state,
                actor,
                proxy,
                reservation,
                reservation.getTicketSequenceOut());
        // schedule this RPC call for execution
        enqueue(rpc);
    }

    private void doExtendTicket(IActor actor, IBrokerProxy proxy, IClientReservation reservation,
            IClientCallbackProxy callback, AuthToken caller) {
        proxy.getLogger().info("Outbound extendTicket request from <" + caller.getName() + ">: "
                + reservation.toLogString());
        IRPCRequestState state = proxy.prepareExtendTicket(reservation, callback, caller);
        state.setCaller(caller);
        state.setType(RPCRequestType.ExtendTicket);
        RPCRequest rpc = new RPCRequest(state,
                actor,
                proxy,
                reservation,
                reservation.getTicketSequenceOut());
        // schedule this RPC call for execution
        enqueue(rpc);
    }

    private void doRelinquish(IActor actor, IBrokerProxy proxy, IClientReservation reservation,
            IClientCallbackProxy callback, AuthToken caller) {
        proxy.getLogger().info("Outbound relinquish request from <" + caller.getName() + ">: "
                + reservation.toLogString());
        IRPCRequestState state = proxy.prepareRelinquish(reservation, callback, caller);
        state.setCaller(caller);
        state.setType(RPCRequestType.Relinquish);
        RPCRequest rpc = new RPCRequest(state,
                actor,
                proxy,
                reservation,
                reservation.getTicketSequenceOut());
        // schedule this RPC call for execution
        enqueue(rpc);
    }

    private void doRedeem(IActor actor, IAuthorityProxy proxy,
            IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback,
            AuthToken caller) {
        proxy.getLogger().info("Outbound redeem request from <" + caller.getName() + ">: "
                + reservation.toLogString());
        // call the proxy to prepare the state for the RPC request.
        IRPCRequestState state = proxy.prepareRedeem(reservation, callback, caller);
        state.setCaller(caller);
        state.setType(RPCRequestType.Redeem);
        // create the request object
        RPCRequest rpc = new RPCRequest(state,
                actor,
                proxy,
                reservation,
                reservation.getLeaseSequenceOut());
        // schedule this RPC call for execution
        enqueue(rpc);
    }

    private void doExtendLease(IActor actor, IAuthorityProxy proxy,
            IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback,
            AuthToken caller) {
        proxy.getLogger().info("Outbound extendLease request from <" + caller.getName() + ">: "
                + reservation.toLogString());
        // call the proxy to prepare the state for the RPC request.
        IRPCRequestState state = proxy.prepareExtendLease(reservation, callback, caller);
        // create the request object
        state.setCaller(caller);
        state.setType(RPCRequestType.ExtendLease);
        RPCRequest rpc = new RPCRequest(state,
                actor,
                proxy,
                reservation,
                reservation.getLeaseSequenceOut());
        // schedule this RPC call for execution
        enqueue(rpc);
    }

    private void doModifyLease(IActor actor, IAuthorityProxy proxy,
            IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback,
            AuthToken caller) {
        proxy.getLogger().info("Outbound modifyLease request from <" + caller.getName() + ">: "
                + reservation.toLogString());
        // call the proxy to prepare the state for the RPC request.
        IRPCRequestState state = proxy.prepareModifyLease(reservation, callback, caller);
        // create the request object
        state.setCaller(caller);
        state.setType(RPCRequestType.ModifyLease);
        RPCRequest rpc = new RPCRequest(state,
                actor,
                proxy,
                reservation,
                reservation.getLeaseSequenceOut());
        // schedule this RPC call for execution
        enqueue(rpc);
    }
    
    private void doClose(IActor actor, IAuthorityProxy proxy,
            IServiceManagerReservation reservation, IServiceManagerCallbackProxy callback,
            AuthToken caller) {
        proxy.getLogger().info("Outbound close request from <" + caller.getName() + ">: "
                + reservation.toLogString());
        // call the proxy to prepare the state for the RPC request.
        IRPCRequestState state = proxy.prepareClose(reservation, callback, caller);
        state.setCaller(caller);
        state.setType(RPCRequestType.Close);
        // create the request object
        RPCRequest rpc = new RPCRequest(state,
                actor,
                proxy,
                reservation,
                reservation.getLeaseSequenceOut());
        // schedule this RPC call for execution
        enqueue(rpc);
    }

    private void doUpdateTicket(IActor actor, IClientCallbackProxy proxy,
            IBrokerReservation reservation, UpdateData udd, ICallbackProxy callback,
            AuthToken caller) {
        proxy.getLogger().info("Outbound updateTicket from <" + caller.getName() + "> for #"
                + reservation.getReservationID().toHashString() + ": r="
                + reservation.toLogString());
        // call the proxy to prepare the state for the RPC request.
        IRPCRequestState state = proxy.prepareUpdateTicket(reservation, udd, callback, caller);
        state.setCaller(caller);
        state.setType(RPCRequestType.UpdateTicket);
        // create the request object
        RPCRequest rpc = new RPCRequest(state,
                actor,
                proxy,
                reservation,
                reservation.getSequenceOut());
        // schedule this RPC call for execution
        enqueue(rpc);
    }

    private void doUpdateLease(IActor actor, IServiceManagerCallbackProxy proxy,
            IAuthorityReservation reservation, UpdateData udd, ICallbackProxy callback,
            AuthToken caller) {
        proxy.getLogger().info("Outbound updateLease from <" + caller.getName() + "> for #"
                + reservation.getReservationID().toHashString() + ": r="
                + reservation.toLogString());
        // call the proxy to prepare the state for the RPC request.
        IRPCRequestState state = proxy.prepareUpdateLease(reservation, udd, callback, caller);
        state.setCaller(caller);
        state.setType(RPCRequestType.UpdateLease);
        // create the request object
        RPCRequest rpc = new RPCRequest(state,
                actor,
                proxy,
                reservation,
                reservation.getSequenceOut());
        // schedule this RPC call for execution
        enqueue(rpc);
    }

    private void doQuery(IActor actor, IActorProxy remoteActor, ICallbackProxy localActor,
            Properties query, IQueryResponseHandler handler, AuthToken caller) {
        remoteActor.getLogger().info("Outbound query from <" + caller.getName() + ">");
        // call the proxy to prepare the state for the RPC request.
        IRPCRequestState state = remoteActor.prepareQuery(localActor, query, caller);
        state.setCaller(caller);
        state.setType(RPCRequestType.Query);
        // create the request object
        RPCRequest rpc = new RPCRequest(state, actor, remoteActor, handler);
        rpc.timer = new QueryTimeout(rpc);
        Globals.Timer.schedule(rpc.timer, QUERY_TIMEOUT_MS);
        // schedule this RPC call for execution
        enqueue(rpc);
    }

    private void doQueryResult(IActor actor, ICallbackProxy remoteActor, String requestID,
            Properties response, AuthToken caller) {
        remoteActor.getLogger().info("Outbound queryResult from <" + caller.getName() + ">");
        // call the proxy to prepare the state for the RPC request.
        IRPCRequestState state = remoteActor.prepareQueryResult(requestID, response, caller);
        state.setCaller(caller);
        state.setType(RPCRequestType.QueryResult);
        // create the request object
        RPCRequest rpc = new RPCRequest(state, actor, remoteActor);
        // schedule this RPC call for execution
        enqueue(rpc);
    }

    public void doDispatchIncomingRPC(IActor actor, IncomingRPC rpc) {
        // see if this is a response for an earlier request that has an
        // associated handler function. If a handler exists, attach the handler
        // to the incoming rpc object.
        RPCRequest request = null;
        if (rpc.getRequestID() != null) {
            request = removePendingRequest(rpc.getRequestID());
            if (request != null) {
                if (request.handler != null) {
                    rpc.setResponseHandler(request.handler);
                }
            }
        }

        // log the incoming message
        switch (rpc.getRequestType()) {
        case Query:
            actor.getLogger().info("Inbound query from <" + rpc.getCaller().getName() + ">");
            break;
        case QueryResult:
            actor.getLogger()
                    .info("Inbound queryResponse from <" + rpc.getCaller().getName() + ">");
            if (request == null) {
                actor.getLogger()
                        .warn("No queryRequest to match to inbound queryResponse. Ignoring response");
                return;
            }
            break;
        case Claim: {
            IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
            actor.getLogger().info("Inbound claim request from <" + rrpc.getCaller().getName()
                    + ">: " + rrpc.getReservation().toLogString());
        }
            break;
        case Ticket: {
            IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
            actor.getLogger().info("Inbound ticket request from <" + rpc.getCaller().getName()
                    + ">: " + rrpc.getReservation().toLogString());
        }
            break;
        case ExtendTicket: {
            IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
            actor.getLogger().info("Inbound extendTicket request from <"
                    + rpc.getCaller().getName() + ">: " + rrpc.getReservation().toLogString());
        }
            break;
        case Relinquish: {
            IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
            actor.getLogger().info("Inbound relinquish request from <" + rpc.getCaller().getName()
                    + ">: " + rrpc.getReservation().toLogString());
        }
            break;
        case UpdateTicket: {
            IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
            actor.getLogger().info("Inbound updateTicket from <" + rpc.getCaller().getName()
                    + ">: " + rrpc.getReservation().toLogString());
        }
            break;
        case Redeem: {
            IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
            actor.getLogger().info("Inbound redeem request from <" + rpc.getCaller().getName()
                    + ">: " + rrpc.getReservation().toLogString());
        }
            break;
        case ExtendLease: {
            IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
            actor.getLogger().info("Inbound extendLease request from <"
                    + rrpc.getCaller().getName() + ">: " + rrpc.getReservation().toLogString());
        }
            break;
        case Close: {
            IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
            actor.getLogger().info("Inbound close request from <" + rrpc.getCaller().getName()
                    + ">: " + rrpc.getReservation().toLogString());
        }
            break;
        case UpdateLease: {
            IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
            actor.getLogger().info("Inbound updateLease from <" + rrpc.getCaller().getName()
                    + ">: " + rrpc.getReservation().toLogString());
            if (rrpc.getReservation().getResources().getResources() != null) {
                actor.getLogger().info("inbound lease is: "
                        + rrpc.getReservation().getResources().getResources().toString());
            }
        }
            break;
        case FailedRPC: {
            IncomingFailedRPC frpc = (IncomingFailedRPC) rpc;
            actor.getLogger().info("Inbound FailedRPC from <" + frpc.getCaller().getName()
                    + ">: requestID=" + frpc.getRequestID());
        }
            break;
        }

        switch (rpc.getRequestType()) {
        case FailedRPC: {
            FailedRPC failed = null;
            IncomingFailedRPC frpc = (IncomingFailedRPC) rpc;
            RPCException remoteException = new RPCException(frpc.getErrorDetails(),
                    RPCError.RemoteError);
            if (request != null) {
                // the caller must match the identity of the proxy
                if (request.proxy.getIdentity().equals(frpc.caller)) {
                    failed = new FailedRPC(remoteException, request);
                } else {
                    actor.getLogger().warn("Failed RPC from an unauthorized caller: expected="
                            + request.proxy.getIdentity() + " but was:" + frpc.getCaller());
                }
            } else if (frpc.getFailedReservationID() != null) {
                // we delay access control until we get to the reservation
                failed = new FailedRPC(remoteException,
                        frpc.getFailedRequestType(),
                        frpc.getFailedReservationID(),
                        frpc.caller);
            } else {
                // we delay access control until later
                failed = new FailedRPC(remoteException, frpc.getFailedRequestType(), frpc.caller);
            }

            // queue this RPC to the failed RPC actor queue, since this is
            // not a new RPC, but an indication
            // that an RPC we made has failed.
            if (failed != null) {
                actor.queueEvent(new FailedRPCEvent(actor, failed));
            }
        }
            break;
        default: {
            Globals.eventManager.dispatchEvent(new InboundRPCEvent(rpc, actor));

            // queue this incoming RPC to the actor queue.
            // when the actor executes it, it will invoke the handler, if
            // one was attached.
            actor.queueEvent(new IncomingRPCEvent(actor, rpc));
        }
            break;
        }
    }

    private void doRetry(RPCRequest rpc) {
        rpc.retryCount++;
        String msg = "Retrying RPC(" + rpc.getRequestType() + ") count=" + rpc.retryCount
                + " actor=" + rpc.getActor().getName();
        if (rpc.getReservation() != null) {
            msg += " reservation #" + rpc.getReservation().getReservationID().toHashString();
        }
        Globals.Log.debug(msg);
        enqueue(rpc);
    }

    private void addPendingRequest(String guid, RPCRequest request) {
        synchronized (pending) {
            pending.put(guid, request);
        }
    }

    protected RPCRequest removePendingRequest(String guid) {
        RPCRequest request = null;
        synchronized (pending) {
            request = pending.remove(guid);
        }
        return request;
    }

    class QueryTimeout extends TimerTask {
        private RPCRequest request;

        public QueryTimeout(RPCRequest request) {
            this.request = request;
        }

        public void run() {
            RPCRequest pending = removePendingRequest(request.request.getMessageID());
            if (pending != null) {
                FailedRPC failed = new FailedRPC(new RPCException("Timeout while waiting for query response",
                        RPCError.Timeout),
                        request);
                request.actor.queueEvent(new FailedRPCEvent(request.actor, failed));
            }
        }
    }

    private Object statsLock = new Object();

    private int numQueued;

    private void queued() {
        synchronized (statsLock) {
            numQueued++;
        }
    }

    private void dequeued() {
        synchronized (statsLock) {
            if (numQueued == 0) {
                throw new RuntimeException("Dequeued invoked, but nothing is queued!!!");
            }
            numQueued--;
            if (numQueued == 0) {
                statsLock.notifyAll();
            }
        }
    }

    private void enqueue(RPCRequest rpc) {
        // We check at the beginning. If we are stopped somewhere in the middle
        // of the function
        // we let the call out

        if (!started) {
            Globals.Log.warn("Ignoring RPC request: container is shutting down");
            return;
        }

        if (rpc.handler != null) {
            // this RPC has a completion handler
            addPendingRequest(rpc.request.getMessageID(), rpc);
        }

        Globals.eventManager.dispatchEvent(new OutboundRPCEvent(rpc));

        try {
            queued();
            OrcaThreadPool.invokeLater(new RPCExecutor(rpc));
        } catch (CannotExecuteException e) {
            dequeued();
            if (rpc.handler != null) {
                removePendingRequest(rpc.request.getMessageID());
            }
            // FIXME: for now just wrapping in an unchecked exception to avoid
            // having to change the signature of all functions in this file!
            throw new OrcaRuntimeException(e);
        }
    }

    class RPCExecutor implements Runnable {
        private RPCRequest request;

        public RPCExecutor(RPCRequest request) {
            this.request = request;
        }

        private void postException(RPCException e) {
            try {
                Globals.Log.error("An error occurred while performing RPC. Error type="
                        + e.getErrorType(),
                        e);
                // this request is no longer pending
                instance.removePendingRequest(request.request.getMessageID());

                // NOTE: we always forward the failure to the actor (even in the
                // case of a network error), since
                // the RPC may no longer be needed (the reservation is closed).
                FailedRPC failed = new FailedRPC(e, request);
                request.actor.queueEvent(new FailedRPCEvent(request.actor, failed));
            } catch (Exception ee) {
                Globals.Log.error("postException failed", ee);
            }
        }

        public void run() {
            // mark this thread as an RPC thread
            String threadName = Thread.currentThread().getName();
            Thread.currentThread().setName("RPC");
            Globals.Log.debug("Performing RPC: type=" + request.request.getType() + " to:"
                    + request.proxy.getName());
            // perform the RPC call
            try {
                request.proxy.execute(request.request);
                request.cancelTimer();
            } catch (RPCException e) {
                postException(e);
            } finally {
                Globals.Log.debug("Completed RPC: type=" + request.request.getType() + " to:"
                        + request.proxy.getName());
                Thread.currentThread().setName(threadName);
                dequeued();
            }
        }
    }

    // executed by the actor main thread
    static class ClaimTimeout implements IOrcaTimerTask {
        RPCRequest req;

        public ClaimTimeout(RPCRequest req) {
            this.req = req;
        }

        public void execute() throws Exception {
            req.actor.getLogger().debug("Claim timeout. Reservation="
                    + req.reservation.toLogString());
            if (req.reservation.isTicketing()) {
                // no claim response yet: fail the reservation
                req.actor.getLogger().error("Failing reservation: " + req.reservation.toLogString()
                        + " due to expired claim timeout");
                req.actor.fail(req.getReservation().getReservationID(),
                        "Timeout during Claim. Please remove the reservation and retry later");
            } else {
                // the claim response has been received
                req.actor.getLogger().debug("Claim has already completed");
            }
        }
    }
}
