package net.exogeni.orca.shirako.core;

import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IRPCRequestState;
import net.exogeni.orca.shirako.kernel.RPCRequestType;
import net.exogeni.orca.util.ID;

public class RPCRequestState implements IRPCRequestState {
    protected AuthToken caller;
    protected RPCRequestType type;
    protected String messageID = new ID().toString();
    
    public AuthToken getCaller() {
         return caller;
    }
    
    public void setCaller(AuthToken caller) {
        this.caller = caller;
    }
    
    public RPCRequestType getType() {
        return type;
    }
    
    public void setType(RPCRequestType type) {
        this.type = type;
    }
    
    public String getMessageID() {
        return messageID;
    }
}
