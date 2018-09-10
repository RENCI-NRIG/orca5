
/**
 * IMAGEPROXYCallbackHandler.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
 */
package net.exogeni.orca.handlers.ec2.tasks.imgproxy;

/**
 * IMAGEPROXYCallbackHandler Callback class, Users can extend this class and implement their own receiveResult and
 * receiveError methods.
 */
public abstract class IMAGEPROXYCallbackHandler {

    protected Object clientData;

    /**
     * User can pass in any object that needs to be accessed once the NonBlocking Web service call is finished and
     * appropriate method of this CallBack is called.
     * 
     * @param clientData
     *            Object mechanism by which the user can pass in user data that will be avilable at the time this
     *            callback is called.
     */
    public IMAGEPROXYCallbackHandler(Object clientData) {
        this.clientData = clientData;
    }

    /**
     * Please use this constructor if you don't want to set any clientData
     */
    public IMAGEPROXYCallbackHandler() {
        this.clientData = null;
    }

    /**
     * Get the client data
     * @return Object
     */

    public Object getClientData() {
        return clientData;
    }

    /**
     * auto generated Axis2 call back method for RegisterImage method
     * @param param17 param17
     */
    public void receiveResultRegisterImage(RegisterImageResponse param17) {
    }

    /**
     * auto generated Axis2 Error handler
     * @param e e
     */
    public void receiveErrorRegisterImage(java.lang.Exception e) {
    }

}
