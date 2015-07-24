package orca.shirako.kernel;

public enum RPCRequestType {
    Unknown,
    Claim,
    Ticket,
    ExtendTicket,
    Relinquish,
    Redeem,
    ExtendLease,
    ModifyLease,
    UpdateTicket,
    UpdateLease,
    Close,
    Query,
    QueryResult,
    FailedRPC;
    
    public static RPCRequestType convert(int ordinal) {
        for (RPCRequestType t : values()) {
            if (t.ordinal() == ordinal) {
                return t;
            }
        }
        return Unknown;
    }
}