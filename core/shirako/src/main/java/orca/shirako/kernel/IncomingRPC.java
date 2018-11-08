package orca.shirako.kernel;

import orca.security.AuthToken;
import orca.shirako.api.ICallbackProxy;
import orca.shirako.api.IRPCResponseHandler;
import orca.shirako.util.RPCException;

public class IncomingRPC {
    protected RPCRequestType requestType;
    protected ICallbackProxy callback;
    protected AuthToken caller;
    protected String messageID;
    protected String requestID;
    protected IRPCResponseHandler responseHandler;
    protected RPCException error;

    public IncomingRPC(String messageID, RPCRequestType requestType, ICallbackProxy callback, AuthToken caller) {
        this.requestType = requestType;
        this.messageID = messageID;
        this.callback = callback;
        this.caller = caller;
    }

    public IncomingRPC(String messageID, RPCRequestType requestType, AuthToken caller) {
        this(messageID, requestType, null, caller);
    }

    public RPCRequestType getRequestType() {
        return requestType;
    }

    public ICallbackProxy getCallback() {
        return callback;
    }

    public AuthToken getCaller() {
        return caller;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setResponseHandler(IRPCResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    public IRPCResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public void setError(RPCException error) {
        this.error = error;
    }

    public RPCException getError() {
        return error;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public String getRequestID() {
        return requestID;
    }
    
    @Override
    public String toString() {
        return "MessageID=" + messageID + " requestType=" + requestType + " caller=" + caller.getName() + ":" + caller.getGuid();
    }
}
