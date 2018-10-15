
/**
 * NodeAgentServiceCallbackHandler.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
 */
package net.exogeni.orca.nodeagent;

import net.exogeni.orca.nodeagent.documents.*;

/**
 * NodeAgentServiceCallbackHandler Callback class, Users can extend this class and implement their own receiveResult and
 * receiveError methods.
 */
public abstract class NodeAgentServiceCallbackHandler {

    protected Object clientData;

    /**
     * User can pass in any object that needs to be accessed once the NonBlocking Web service call is finished and
     * appropriate method of this CallBack is called.
     * 
     * @param clientData
     *            Object mechanism by which the user can pass in user data that will be avilable at the time this
     *            callback is called.
     */
    public NodeAgentServiceCallbackHandler(Object clientData) {
        this.clientData = clientData;
    }

    /**
     * Please use this constructor if you don't want to set any clientData
     */
    public NodeAgentServiceCallbackHandler() {
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
     * auto generated Axis2 call back method for uninstallDriver method
     * @param param199 param199
     */
    public void receiveResultuninstallDriver(ResultElement param199) {
    }

    /**
     * auto generated Axis2 Error handler
     * @param e e
     */
    public void receiveErroruninstallDriver(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for executeScript method
     * @param param201 param201
     */
    public void receiveResultexecuteScript(ScriptResultElement param201) {
    }

    /**
     * auto generated Axis2 Error handler
     * @param e e
     */
    public void receiveErrorexecuteScript(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for testFunc method
     * @param param203 param203
     */
    public void receiveResulttestFunc(TestFuncResultElement param203) {
    }

    /**
     * auto generated Axis2 Error handler
     * @param e e
     */
    public void receiveErrortestFunc(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for executeObjectDriver method
     * @param param205 param205
     */
    public void receiveResultexecuteObjectDriver(ResultElement param205) {
    }

    /**
     * auto generated Axis2 Error handler
     * @param e e
     */
    public void receiveErrorexecuteObjectDriver(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for unregisterKey method
     * @param param207 param207
     */
    public void receiveResultunregisterKey(UnregisterKeyResultElement param207) {
    }

    /**
     * auto generated Axis2 Error handler
     * @param e e
     */
    public void receiveErrorunregisterKey(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for executeDriver method
     * @param param209 param209
     */
    public void receiveResultexecuteDriver(ResultElement param209) {
    }

    /**
     * auto generated Axis2 Error handler
     * @param e e
     */
    public void receiveErrorexecuteDriver(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for upgradeDriver method
     * @param param211  param211
     */
    public void receiveResultupgradeDriver(ResultElement param211) {
    }

    /**
     * auto generated Axis2 Error handler
     * @param e e
     */
    public void receiveErrorupgradeDriver(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for registerAuthorityKey method
     * @param param213 param213
     */
    public void receiveResultregisterAuthorityKey(RegisterAuthorityKeyResultElement param213) {
    }

    /**
     * auto generated Axis2 Error handler
     * @param e e 
     */
    public void receiveErrorregisterAuthorityKey(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for getServiceKey method
     * @param param215 param215
     */
    public void receiveResultgetServiceKey(GetServiceKeyResultElement param215) {
    }

    /**
     * auto generated Axis2 Error handler
     * @param e e
     */
    public void receiveErrorgetServiceKey(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for registerKey method
     * @param param217 param217
     */
    public void receiveResultregisterKey(RegisterKeyResultElement param217) {
    }

    /**
     * auto generated Axis2 Error handler
     * @param e e
     */
    public void receiveErrorregisterKey(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for installDriver method
     * @param param219 param219
     */
    public void receiveResultinstallDriver(ResultElement param219) {
    }

    /**
     * auto generated Axis2 Error handler
     * @param e e 
     */
    public void receiveErrorinstallDriver(java.lang.Exception e) {
    }

}
