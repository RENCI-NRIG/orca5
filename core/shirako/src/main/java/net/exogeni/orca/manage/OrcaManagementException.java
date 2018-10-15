package net.exogeni.orca.manage;

public class OrcaManagementException extends RuntimeException {
    public static final int ERROR_CODE_UNSPECIFIED = - 1;
    
    protected int errorCode = 0;
    
    public OrcaManagementException() {
        this(ERROR_CODE_UNSPECIFIED);
    }
    
    public OrcaManagementException(int errorCode) {
        super();
        this.errorCode = errorCode;
    }
    public OrcaManagementException(String message) {
        this(message, ERROR_CODE_UNSPECIFIED);
    }
    public OrcaManagementException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }   
    
    public OrcaManagementException(String message, Throwable inner) {
        this(message, inner, ERROR_CODE_UNSPECIFIED);
    }
    
    public OrcaManagementException(Throwable inner) {
        this(inner, ERROR_CODE_UNSPECIFIED);
    }
    
    public OrcaManagementException(Throwable inner, int errorCode) {
        super(inner);
        this.errorCode = errorCode;
    }
    
    public OrcaManagementException(String message, Throwable inner, int errorCode) {
        super(message, inner);
        this.errorCode = errorCode;
    }
    
    public OrcaManagementException(String message, OrcaError error) {
    	super(message, error.getException());
    	this.errorCode = error.getStatus().getCode();
    }
    
    public int getErrorCode() {
        return errorCode;
    }
}
