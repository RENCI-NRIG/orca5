package orca.shirako.proxies.soapaxis2;

/**
 * A <code>SoapAxis2StubException</code> represents a failure to instatiate a stub
 * to communicate with a remote actor.
 * @author aydan
 *
 */
public class SoapAxis2StubException extends Exception {
    private static final long serialVersionUID = 3647734445725299223L;

    public SoapAxis2StubException() {
        super();
    }
    
    public SoapAxis2StubException(String message){
        super(message);
    }
    
    public SoapAxis2StubException(Throwable inner){
        super(inner);
    }
    
    public SoapAxis2StubException(String message, Throwable inner){
        super(message, inner);
    }
}