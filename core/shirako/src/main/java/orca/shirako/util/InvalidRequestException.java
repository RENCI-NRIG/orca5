package orca.shirako.util;

public class InvalidRequestException extends Exception {
    public InvalidRequestException() {
        super();
    }

    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(Throwable inner) {
        super(inner);
    }

    public InvalidRequestException(String message, Throwable inner) {
        super(message, inner);
    }
}
