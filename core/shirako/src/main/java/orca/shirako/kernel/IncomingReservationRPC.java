package orca.shirako.kernel;

import orca.security.AuthToken;
import orca.shirako.api.IClientCallbackProxy;
import orca.shirako.api.IReservation;
import orca.shirako.util.UpdateData;

public class IncomingReservationRPC extends IncomingRPC {    
    protected IReservation reservation;
    protected UpdateData udd;
    
    public IncomingReservationRPC(String messageID, RPCRequestType requestType, IReservation reservation, IClientCallbackProxy callback, UpdateData udd, AuthToken caller) {
        super(messageID, requestType, callback, caller);
        this.reservation = reservation;
        this.udd = udd;
    }

    public IncomingReservationRPC(String messageID, RPCRequestType requestType, IReservation reservation, IClientCallbackProxy callback, AuthToken caller) {
        this(messageID, requestType, reservation, callback, null, caller);
    }
    
    public IncomingReservationRPC(String messageID, RPCRequestType requestType, IReservation reservation, UpdateData udd, AuthToken caller) {
        super(messageID, requestType, caller);
        this.reservation = reservation;
        this.udd = udd;
    }

    public IncomingReservationRPC(String messageID, RPCRequestType requestType, IReservation reservation, AuthToken caller) {
        this(messageID, requestType, reservation, (UpdateData)null, caller);
    }

    public IReservation getReservation() {
        return reservation;
    }
    
    public UpdateData getUpdateData() {
        return udd;
    }
    
    @Override
    public String toString() {
        String result = super.toString();
        result += " rid=" + reservation.toString();
        return result;
    }        
}