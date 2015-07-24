
    /**
     * NodeAgentServiceCallbackHandler.java
     *
     * This file was auto-generated from WSDL
     * by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
     */
    package orca.nodeagent;

    /**
     *  NodeAgentServiceCallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class NodeAgentServiceCallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public NodeAgentServiceCallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public NodeAgentServiceCallbackHandler(){
        this.clientData = null;
    }

    /**
     * Get the client data
     */

     public Object getClientData() {
        return clientData;
     }

        
           /**
            * auto generated Axis2 call back method for uninstallDriver method
            *
            */
           public void receiveResultuninstallDriver(
                    orca.nodeagent.documents.ResultElement param199) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErroruninstallDriver(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for executeScript method
            *
            */
           public void receiveResultexecuteScript(
                    orca.nodeagent.documents.ScriptResultElement param201) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorexecuteScript(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for testFunc method
            *
            */
           public void receiveResulttestFunc(
                    orca.nodeagent.documents.TestFuncResultElement param203) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrortestFunc(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for executeObjectDriver method
            *
            */
           public void receiveResultexecuteObjectDriver(
                    orca.nodeagent.documents.ResultElement param205) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorexecuteObjectDriver(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for unregisterKey method
            *
            */
           public void receiveResultunregisterKey(
                    orca.nodeagent.documents.UnregisterKeyResultElement param207) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorunregisterKey(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for executeDriver method
            *
            */
           public void receiveResultexecuteDriver(
                    orca.nodeagent.documents.ResultElement param209) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorexecuteDriver(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for upgradeDriver method
            *
            */
           public void receiveResultupgradeDriver(
                    orca.nodeagent.documents.ResultElement param211) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorupgradeDriver(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for registerAuthorityKey method
            *
            */
           public void receiveResultregisterAuthorityKey(
                    orca.nodeagent.documents.RegisterAuthorityKeyResultElement param213) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorregisterAuthorityKey(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getServiceKey method
            *
            */
           public void receiveResultgetServiceKey(
                    orca.nodeagent.documents.GetServiceKeyResultElement param215) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorgetServiceKey(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for registerKey method
            *
            */
           public void receiveResultregisterKey(
                    orca.nodeagent.documents.RegisterKeyResultElement param217) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorregisterKey(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for installDriver method
            *
            */
           public void receiveResultinstallDriver(
                    orca.nodeagent.documents.ResultElement param219) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorinstallDriver(java.lang.Exception e) {
            }
                


    }
    