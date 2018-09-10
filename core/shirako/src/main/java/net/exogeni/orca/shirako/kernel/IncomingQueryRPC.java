package net.exogeni.orca.shirako.kernel;

import java.util.Properties;

import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.ICallbackProxy;

public class IncomingQueryRPC extends IncomingRPC {
    protected Properties query;
    
    public IncomingQueryRPC(String messageID, Properties query, ICallbackProxy callback, AuthToken caller) {
        super(messageID, RPCRequestType.Query, callback, caller);
        this.query = query;
    }        

    public IncomingQueryRPC(String messageID, String requestID, Properties query, AuthToken caller) {
        super(messageID, RPCRequestType.QueryResult, caller);
        this.query = query;
        this.requestID = requestID;
    }        
    
    public Properties get() {
        return query;
    }
}
