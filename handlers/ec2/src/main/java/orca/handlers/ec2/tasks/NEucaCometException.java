/*
 *
 * @class
 *
 * @brief This class implements exception raised in case of any failure on comet interface
 *
 *
 */
package orca.handlers.ec2.tasks;

public class NEucaCometException extends Exception {

    public NEucaCometException() {}

    public NEucaCometException(Throwable throwable) {
        super(throwable);
    }

    public NEucaCometException(String message) {
        super(message);
    }

}