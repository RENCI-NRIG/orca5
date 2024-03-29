
    /**
     * ActorServiceCallbackHandler.java
     *
     * This file was auto-generated from WSDL
     * by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
     */
    package orca.shirako.proxies.soapaxis2.services;

    /**
     *  ActorServiceCallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class ActorServiceCallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public ActorServiceCallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public ActorServiceCallbackHandler(){
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
            * auto generated Axis2 call back method for updateLease method
            * @param param91 param91
            */
           public void receiveResultupdateLease(
                    orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse param91) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorupdateLease(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for updateTicket method
            * @param param93 param93
            */
           public void receiveResultupdateTicket(
                    orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse param93) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorupdateTicket(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for query method
            * @param param95 param95
            */
           public void receiveResultquery(
                    orca.shirako.proxies.soapaxis2.services.QueryResponse param95) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorquery(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for failedRPC method
            * @param param97 param97
            */
           public void receiveResultfailedRPC(
                    orca.shirako.proxies.soapaxis2.services.FailedRPCResponse param97) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorfailedRPC(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for queryResult method
            * @param param99 param99
            */
           public void receiveResultqueryResult(
                    orca.shirako.proxies.soapaxis2.services.QueryResultResponse param99) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorqueryResult(java.lang.Exception e) {
            }
                


    }
    