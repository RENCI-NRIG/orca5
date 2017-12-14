package orca.embed.policyhelpers;

public class SystemNativeError {
    private int error;
    private String message;
    private String additional;

    public String getAdditional() {
        return additional;
    }

    public void setAdditional(String additional) {
        this.additional = additional;
    }

    public final int getErrno() {
        return this.error;
    }

    public final String getMessage() {
        return this.message;
    }

    public boolean isError() {
        return error > 0 ? true : false;
    }

    public void setErrno(int error) {
        this.error = error;
    }

    public void setMessage(String message) {
        this.message = message
                + ".\n Please see https://geni-orca.renci.org/trac/wiki/orca-errors for possible solutions.";
    }

    public final String toString() {
        return this.error + " (" + this.message + ")";
    }
}
