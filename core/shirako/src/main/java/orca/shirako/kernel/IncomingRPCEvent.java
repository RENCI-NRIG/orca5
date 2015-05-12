package orca.shirako.kernel;

import java.util.Properties;

import orca.shirako.api.IActor;
import orca.shirako.api.IActorEvent;
import orca.shirako.api.IAuthority;
import orca.shirako.api.IBroker;
import orca.shirako.api.IClientActor;
import orca.shirako.api.IClientCallbackProxy;
import orca.shirako.api.IQueryResponseHandler;
import orca.shirako.api.IServerActor;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.IServiceManagerCallbackProxy;

public class IncomingRPCEvent implements IActorEvent {
	protected IncomingRPC rpc;
	protected IActor actor;

	public IncomingRPCEvent(IActor actor, IncomingRPC rpc) {
		this.actor = actor;
		this.rpc = rpc;
	}

	protected boolean doProcess(IActor actor) throws Exception {
		boolean processed = true;
		switch (rpc.getRequestType()) {
		case Query: {
			IncomingQueryRPC qrpc = (IncomingQueryRPC) rpc;
			actor.getLogger().info("Processing query from <" + qrpc.getCaller().getName() + ">");
			Properties result = actor.query(qrpc.get(), qrpc.getCaller());
			// send the response
			RPCManager.queryResult(actor, qrpc.getCallback(), qrpc.getMessageID(), result, actor.getIdentity());
		}
			break;
		case QueryResult: {
			IncomingQueryRPC qrpc = (IncomingQueryRPC) rpc;
			actor.getLogger().info("Processing queryResponse from <" + qrpc.getCaller().getName() + ">");
			Properties result = qrpc.get();
			if (qrpc.getResponseHandler() != null) {
				// invoke the handler
				IQueryResponseHandler handler = (IQueryResponseHandler) qrpc.getResponseHandler();
				handler.handle(qrpc.getError(), result);
			} else {
				actor.getLogger().warn(
						"No response handler is associated with the queryResponse. Ignoring queryResponse");
			}
		}
			break;
		default:
			processed = false;
		}
		return processed;
	}

	protected boolean doProcess(IClientActor client) throws Exception {
		boolean processed = true;
		switch (rpc.getRequestType()) {
		case UpdateTicket: {
			IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
			client.getLogger().info(
					"Processing updateTicket from <" + rrpc.getCaller().getName() + ">: "
							+ rrpc.getReservation().toLogString());
			client.updateTicket(rrpc.getReservation(), rrpc.getUpdateData(), rpc.getCaller());
		}
			break;
		default:
			processed = doProcess((IActor)client);
		}
		return processed;
	}

	protected boolean doProcess(IServerActor server) throws Exception {
		boolean processed = true;

		switch (rpc.getRequestType()) {
		case Claim: {
			IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
			server.getLogger().info(
					"Processing claim request from <" + rrpc.getCaller().getName() + ">: "
							+ rrpc.getReservation().toLogString());
			server.claim(rrpc.getReservation(), (IClientCallbackProxy) rrpc.getCallback(), rrpc.getCaller());
		}
			break;
		case Ticket: {
			IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
			server.getLogger().info(
					"Processing ticket request from <" + rpc.getCaller().getName() + ">: "
							+ rrpc.getReservation().toLogString());
			server.ticket(rrpc.getReservation(), (IClientCallbackProxy) rpc.getCallback(), rpc.getCaller());
		}
			break;
		case ExtendTicket: {
			IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
			server.getLogger().info(
					"Processing extendTicket request from <" + rpc.getCaller().getName() + ">: "
							+ rrpc.getReservation().toLogString());
			server.extendTicket(rrpc.getReservation(), rpc.getCaller());
		}
			break;
		case Relinquish: {
			IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
			server.getLogger().info(
					"Processing relinquish request from <" + rpc.getCaller().getName() + ">: "
							+ rrpc.getReservation().toLogString());
			server.relinquish(rrpc.getReservation(), rpc.getCaller());
		}
			break;
		default:
			processed = doProcess((IActor)server);
		}
		return processed;
	}

	protected boolean doProcess(IBroker broker) throws Exception {
		// first try as a server
		boolean processed = doProcess((IServerActor)broker);
		if (!processed) {
			// next try as a client
			processed = doProcess((IClientActor)broker);
		}
		return processed;
	}
	
	protected boolean doProcess(IAuthority authority) throws Exception {
		boolean processed = true;
		switch (rpc.getRequestType()) {
		case Redeem: {
			IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
			authority.getLogger().info(
					"Processing redeem request from <" + rpc.getCaller().getName() + ">: "
							+ rrpc.getReservation().toLogString());
			authority.redeem(rrpc.getReservation(), (IServiceManagerCallbackProxy) rpc.getCallback(), rrpc.getCaller());
		}
			break;
		case ExtendLease: {
			IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
			authority.getLogger().info(
					"Processing extendLease request from <" + rrpc.getCaller().getName() + ">: "
							+ rrpc.getReservation().toLogString());
			authority.extendLease(rrpc.getReservation(), rrpc.getCaller());
		}
			break;
		case ModifyLease: {
			IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
			authority.getLogger().info(
					"Processing modifyLease request from <" + rrpc.getCaller().getName() + ">: "
							+ rrpc.getReservation().toLogString());
			authority.modifyLease(rrpc.getReservation(), rrpc.getCaller());
		}
			break;
		case Close: {
			IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
			authority.getLogger().info(
					"Processing close request from <" + rrpc.getCaller().getName() + ">: "
							+ rrpc.getReservation().toLogString());
			authority.close(rrpc.getReservation(), rpc.getCaller());
		}
			break;
		default:
			processed = doProcess((IServerActor)authority);
		}
		return processed;
	}

	protected boolean doProcess(IServiceManager sm) throws Exception {
		boolean processed = true;
		switch (rpc.getRequestType()) {
		case UpdateLease: {
			IncomingReservationRPC rrpc = (IncomingReservationRPC) rpc;
			sm.getLogger().info(
					"Processing updateLease from <" + rrpc.getCaller().getName() + ">: "
							+ rrpc.getReservation().toLogString());
			if (rrpc.getReservation().getResources().getResources() != null) {
				sm.getLogger().info(
						"inbound lease is: " + rrpc.getReservation().getResources().getResources().toString());
			}
			sm.updateLease(rrpc.getReservation(), rrpc.getUpdateData(), rpc.getCaller());
		}
			break;
		default:
			processed = doProcess((IClientActor)sm);
		}
		return processed;
	}
	
	public void process() throws Exception {
		// In theory, an actor can implement any combination of the available actor roles.
		// This is why we route the RPC through every supported role until we find a match.

		boolean done = false;
		
		if (actor instanceof IAuthority) {
			done = doProcess((IAuthority) actor);
			if (done) {
				return;
			}
		}

		if (actor instanceof IBroker) {
			done = doProcess((IBroker) actor);
			if (done) {
				return;
			}
		}
		
		if (actor instanceof IServiceManager) {
			done = doProcess((IServiceManager) actor);
			if (done) {
				return;
			}
		}

		throw new IllegalArgumentException("Unsupported RPC request type: " + rpc.getRequestType());
	}
}