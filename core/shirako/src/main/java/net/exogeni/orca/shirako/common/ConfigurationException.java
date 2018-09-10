package net.exogeni.orca.shirako.common;

import net.exogeni.orca.manage.OrcaError;

@SuppressWarnings("serial")
public class ConfigurationException extends Exception {
    protected OrcaError error;

    public ConfigurationException() {
        super();
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(Throwable inner) {
        super(inner);
    }

    public ConfigurationException(String message, Throwable inner) {
        super(message, inner);
    }

    public ConfigurationException(String message, OrcaError error) {
        super(message, error.getException());
        this.error = error;
    }

    @Override
    public String getMessage() {
        String m = super.getMessage();
        if (error != null) {
            m += error.toString();
        }
        return m;
    }
}
