package orca.shirako.kernel;

import orca.security.AuthToken;
import orca.shirako.common.ReservationID;

public class IncomingFailedRPC extends IncomingRPC {
    protected String errorDetails;
    protected RPCRequestType failedRequestType;
    protected ReservationID failedReservationID;
    protected String requestID;
    
    public IncomingFailedRPC(String messageID, RPCRequestType failedRequestType, String requestID, ReservationID failedReservationID, String errorDetails, AuthToken caller) {
        super(messageID, RPCRequestType.FailedRPC, caller);
        this.failedRequestType = failedRequestType;
        this.failedReservationID = failedReservationID;
        this.errorDetails = errorDetails;
        this.requestID = requestID;
    }        

    public IncomingFailedRPC(String messageID, RPCRequestType failedRequestType, String requestID, String errorDetails, AuthToken caller) {
        this(messageID, failedRequestType, requestID, null, errorDetails, caller);
    }

    public String getErrorDetails() {
        return errorDetails;
    }
    
    public RPCRequestType getFailedRequestType() {
        return failedRequestType;
    }
    
    public ReservationID getFailedReservationID() {
        return failedReservationID;
    }
}
