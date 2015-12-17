package orca.shirako.kernel;

import orca.shirako.api.IActor;
import orca.shirako.api.IActorEvent;
import orca.shirako.api.IQueryResponseHandler;
import orca.shirako.common.ReservationID;

public class FailedRPCEvent implements IActorEvent {
	protected FailedRPC failed;
	protected IActor actor;

	public FailedRPCEvent(IActor actor, FailedRPC failed) {
		this.actor = actor;
		this.failed = failed;
	}

	public void process() throws Exception {
		actor.getLogger().debug("Processing failed RPC (" + failed.getRequestType() + ")");
        switch (failed.getRequestType()) {
            case Query: {
                // We do not retry automatically and let the caller decide if
                // they would want to retry.
                IQueryResponseHandler handler = (IQueryResponseHandler) failed.getRequest().getHandler();
                if (handler != null) {
                    handler.handle(failed.getError(), null);
                } else {
                	actor.getLogger().warn("Query failed, but not handler is present");
                }
            }
                break;
            case QueryResult: {
                // QueryResult cannot generate FailedRPC
                assert failed.hasRequest();
                if (failed.getRetryCount() < 10) {
                    RPCManager.retry(failed.getRequest());
                } else {
                	actor.getLogger().warn("Cannot send query response. Giving up after 10 retries", failed.getError());
                }
            }
                break;
            case Claim:
            case Ticket:
            case ExtendTicket:
            case Relinquish:
            case UpdateTicket:
            case Redeem:
            case ExtendLease:
            case ModifyLease:
            	System.out.println("Failed RPC for ModifyLease: in FailedRPCEvent.process()");
            	break;
            case Close: {
                ReservationID rid;
                try {
                    rid = failed.getReservationID();
                } catch (Exception e) {
                	actor.getLogger().error("Could not process failed RPC: could not extract reservation id", e);
                    return;
                }

                if (rid == null) {
                    actor.getLogger().error("Could not process failed RPC: reservation id is null");
                    return;
                }
                
                actor.handleFailedRPC(rid, failed);
            }
                break;
            default:
                throw new IllegalArgumentException("Unsupported RPC request type: " + failed.getRequest().getRequestType());
        }	
	}
}