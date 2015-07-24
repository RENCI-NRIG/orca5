package orca.security;

public class AccessDeniedException extends Exception {
    private static final long serialVersionUID = -573866779466887696L;

    public AccessDeniedException() {
        super();
    }
    
    public AccessDeniedException(String message){
        super(message);
    }
    
    public AccessDeniedException(Throwable inner){
        super(inner);
    }
    
    public AccessDeniedException(String message, Throwable inner){
        super(message, inner);
    }
}