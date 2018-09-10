package net.exogeni.orca.util;

public class OrcaRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -7705766675835869223L;

    public OrcaRuntimeException() {
    }

    public OrcaRuntimeException(Throwable t) {
        super(t);
    }

    public OrcaRuntimeException(String message) {
        super(message);
    }

    public OrcaRuntimeException(String message, Throwable t) {
        super(message, t);
    }
}
