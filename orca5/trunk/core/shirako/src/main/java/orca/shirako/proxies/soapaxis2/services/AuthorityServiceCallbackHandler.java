
    /**
     * AuthorityServiceCallbackHandler.java
     *
     * This file was auto-generated from WSDL
     * by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
     */
    package orca.shirako.proxies.soapaxis2.services;

    /**
     *  AuthorityServiceCallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class AuthorityServiceCallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public AuthorityServiceCallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public AuthorityServiceCallbackHandler(){
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
                    orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse param217) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorupdateLease(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for extendLease method
            *
            */
           public void receiveResultextendLease(
                    orca.shirako.proxies.soapaxis2.services.ExtendLeaseResponse param219) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorextendLease(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for updateTicket method
            *
            */
           public void receiveResultupdateTicket(
                    orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse param221) {
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
                    orca.shirako.proxies.soapaxis2.services.TicketResponse param223) {
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
                    orca.shirako.proxies.soapaxis2.services.QueryResponse param225) {
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
                    orca.shirako.proxies.soapaxis2.services.RelinquishResponse param227) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorrelinquish(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for redeem method
            *
            */
           public void receiveResultredeem(
                    orca.shirako.proxies.soapaxis2.services.RedeemResponse param229) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorredeem(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for failedRPC method
            *
            */
           public void receiveResultfailedRPC(
                    orca.shirako.proxies.soapaxis2.services.FailedRPCResponse param231) {
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
                    orca.shirako.proxies.soapaxis2.services.ClaimResponse param233) {
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
                    orca.shirako.proxies.soapaxis2.services.QueryResultResponse param235) {
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
                    orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse param237) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorextendTicket(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for close method
            *
            */
           public void receiveResultclose(
                    orca.shirako.proxies.soapaxis2.services.CloseResponse param239) {
           }

          /**
           * auto generated Axis2 Error handler
           *
           */
            public void receiveErrorclose(java.lang.Exception e) {
            }
                


    }
    