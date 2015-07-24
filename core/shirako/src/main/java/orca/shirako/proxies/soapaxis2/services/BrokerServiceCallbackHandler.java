
    /**
     * BrokerServiceCallbackHandler.java
     *
     * This file was auto-generated from WSDL
     * by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
     */
    package orca.shirako.proxies.soapaxis2.services;

    /**
     *  BrokerServiceCallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class BrokerServiceCallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public BrokerServiceCallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public BrokerServiceCallbackHandler(){
        this.clientData = null;
    }

    /**
     * Get the client data
     */

     public Object getClientData() {
        return clientData;
     }

        
           /**
            * auto generated Axis2 call back method for updateLease method
            *
            */
           public void receiveResultupdateLease(
                    orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse param163) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorupdateLease(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for updateTicket method
            *
            */
           public void receiveResultupdateTicket(
                    orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse param165) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorupdateTicket(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for ticket method
            *
            */
           public void receiveResultticket(
                    orca.shirako.proxies.soapaxis2.services.TicketResponse param167) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorticket(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for query method
            *
            */
           public void receiveResultquery(
                    orca.shirako.proxies.soapaxis2.services.QueryResponse param169) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorquery(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for relinquish method
            *
            */
           public void receiveResultrelinquish(
                    orca.shirako.proxies.soapaxis2.services.RelinquishResponse param171) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorrelinquish(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for failedRPC method
            *
            */
           public void receiveResultfailedRPC(
                    orca.shirako.proxies.soapaxis2.services.FailedRPCResponse param173) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorfailedRPC(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for claim method
            *
            */
           public void receiveResultclaim(
                    orca.shirako.proxies.soapaxis2.services.ClaimResponse param175) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorclaim(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for queryResult method
            *
            */
           public void receiveResultqueryResult(
                    orca.shirako.proxies.soapaxis2.services.QueryResultResponse param177) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorqueryResult(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for extendTicket method
            *
            */
           public void receiveResultextendTicket(
                    orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse param179) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorextendTicket(java.lang.Exception e) {
            }
                


    }
    