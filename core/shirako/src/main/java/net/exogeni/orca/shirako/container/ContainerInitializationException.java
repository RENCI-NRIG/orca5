package net.exogeni.orca.shirako.container;

public class ContainerInitializationException extends Exception {
    public ContainerInitializationException(String message){
        super(message);
    }
    public ContainerInitializationException(Throwable inner) {
        super(inner);
    }
}
