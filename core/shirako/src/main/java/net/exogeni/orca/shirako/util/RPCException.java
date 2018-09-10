package net.exogeni.orca.shirako.util;


public class RPCException extends Exception {
    private static final long serialVersionUID = 8268718430105967166L;

    protected RPCError error;
    
    public RPCException(String message, RPCError error, Throwable inner){
        super(message, inner);
        this.error = error;
    }

    public RPCException(String message, RPCError error){
        super(message);
        this.error = error;
    }

    public RPCException(RPCError error, Throwable inner){
        super(inner);
        this.error = error;
    }
    
    public RPCException(RPCException inner) {
        super(inner);
        this.error = inner.error;
    }
    
    public RPCError getErrorType() {
        return error;
    }       
}
