package net.exogeni.orca.shirako.kernel;

import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IRPCResponseHandler;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.util.RPCError;
import net.exogeni.orca.shirako.util.RPCException;

/**
 * <code>FailedRPC</code> describes an RPC request that has failed. Use
 * {@link #hasRequest()} to determine if a <code>FailedRPC</code> contains the
 * RPCRequest object that describes the request that failed.
 * {@link #getErrorType()} describes the type of the error. Some errors are
 * transient, others are permanent. In general, errors marked as network are
 * considered transient, and all others--permanent. <br> {@link #getRequestType()}
 * describes the request type of the failed RPC. {@link #getReservationID()}
 * returns the id of the reservation the RPC was made for, if the RPC was for a
 * reservation, or null otherwise.
 * @author aydan
 */
public class FailedRPC {
    protected RPCRequest request;
    protected RPCException error;
    protected ReservationID rid;
    protected RPCRequestType requestType;
    protected AuthToken remoteAuth;

    public FailedRPC(RPCException error, RPCRequest request) {
        if (error == null) {
            throw new IllegalArgumentException("error cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        this.error = error;
        this.request = request;
        this.requestType = request.getRequestType();
        if (request.getReservation() != null) {
            rid = request.getReservation().getReservationID();
        }
        this.remoteAuth = request.proxy.getIdentity();
    }

    public FailedRPC(RPCException error, RPCRequestType requestType, ReservationID rid, AuthToken remoteAuth) {
        if (error == null) {
            throw new IllegalArgumentException("error cannot be null");
        }
        if (rid == null) {
            throw new IllegalArgumentException("rid cannot be null");
        }
        this.error = error;
        this.requestType = requestType;
        this.rid = rid;
        this.remoteAuth = remoteAuth;
    }

    public FailedRPC(RPCException error, RPCRequestType requestType, AuthToken remoteAuth) {
        if (error == null) {
            throw new IllegalArgumentException("error cannot be null");
        }
        this.error = error;
        this.requestType = requestType;
        this.remoteAuth = remoteAuth;
    }

    public boolean isReservationRPC() {
        return rid != null;
    }

    public boolean hasRequest() {
        return request != null;
    }

    public RPCRequest getRequest() {
        return request;
    }

    public RPCException getError() {
        return error;
    }

    public RPCRequestType getRequestType() {
        return requestType;
    }

    public RPCError getErrorType() {
        return error.getErrorType();
    }

    public int getRetryCount() {
        if (request == null) {
            return 0;
        }
        return request.retryCount;
    }

    public ReservationID getReservationID() {
        return rid;
    }

    public IRPCResponseHandler getHandler() {
        if (request != null) {
            return request.getHandler();
        }
        return null;
    }
    
    public AuthToken getRemoteAuth() {
        return remoteAuth;
    }

}
