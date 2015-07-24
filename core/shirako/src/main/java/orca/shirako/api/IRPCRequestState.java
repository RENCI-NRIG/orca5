package orca.shirako.api;

import orca.security.AuthToken;
import orca.shirako.kernel.RPCRequestType;

public interface IRPCRequestState {
    public AuthToken getCaller();
    public void setCaller(AuthToken caller);
    public RPCRequestType getType();
    public void setType(RPCRequestType type);
    public String getMessageID();
}