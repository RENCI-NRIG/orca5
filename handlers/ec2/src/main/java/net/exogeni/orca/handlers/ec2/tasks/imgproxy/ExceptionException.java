
/**
 * ExceptionException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
 */
package net.exogeni.orca.handlers.ec2.tasks.imgproxy;

public class ExceptionException extends java.lang.Exception {

    private Exception0 faultMessage;

    public ExceptionException() {
        super("ExceptionException");
    }

    public ExceptionException(java.lang.String s) {
        super(s);
    }

    public ExceptionException(java.lang.String s, java.lang.Throwable ex) {
        super(s, ex);
    }

    public void setFaultMessage(Exception0 msg) {
        faultMessage = msg;
    }

    public Exception0 getFaultMessage() {
        return faultMessage;
    }
}
