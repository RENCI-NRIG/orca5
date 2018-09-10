package net.exogeni.orca.shirako.container;

public class ContainerRecoveryException extends Exception {
    public ContainerRecoveryException(String message){
        super(message);
    }
    public ContainerRecoveryException(Throwable inner) {
        super(inner);
    }
    public ContainerRecoveryException(String message, Throwable inner) {
        super(message, inner);
    }
}
