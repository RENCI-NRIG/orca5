package orca.shirako.util;

/**
 * A <code>RemoteActorException</code> describes an error that occurred
 * in a remote actor while processing an RPC request.
 * @author aydan
 *
 */
public class RemoteActorException extends Exception {
    private static final long serialVersionUID = 1636244117628001777L;

    public RemoteActorException() {
        super();
    }
    
    public RemoteActorException(String message){
        super(message);
    }
    
    public RemoteActorException(Throwable inner){
        super(inner);
    }
    
    public RemoteActorException(String message, Throwable inner){
        super(message, inner);
    }
}