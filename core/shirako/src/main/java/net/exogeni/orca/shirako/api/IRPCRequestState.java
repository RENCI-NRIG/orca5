package net.exogeni.orca.shirako.api;

import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.kernel.RPCRequestType;

public interface IRPCRequestState {
    public AuthToken getCaller();
    public void setCaller(AuthToken caller);
    public RPCRequestType getType();
    public void setType(RPCRequestType type);
    public String getMessageID();
}
