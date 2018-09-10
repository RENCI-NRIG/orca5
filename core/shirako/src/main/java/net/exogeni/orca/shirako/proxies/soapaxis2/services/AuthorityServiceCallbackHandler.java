
    /**
     * AuthorityServiceCallbackHandler.java
     *
     * This file was auto-generated from WSDL
     * by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
     */
    package net.exogeni.orca.shirako.proxies.soapaxis2.services;

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
     * @return Object
     */

     public Object getClientData() {
        return clientData;
     }

        
           /**
            * auto generated Axis2 call back method for modifyLease method
            * @param param235 param235
            */
           public void receiveResultmodifyLease(
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.ModifyLeaseResponse param235) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           *
           */
            public void receiveErrormodifyLease(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for updateLease method
            * @param param237 param237
            *
            */
           public void receiveResultupdateLease(
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse param237) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorupdateLease(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for extendLease method
            * @param param239 param239
            */
           public void receiveResultextendLease(
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendLeaseResponse param239) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorextendLease(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for updateTicket method
            * @param param241 param241
            */
           public void receiveResultupdateTicket(
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse param241) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorupdateTicket(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for ticket method
            * @param param243 param243
            */
           public void receiveResultticket(
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse param243) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorticket(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for query method
            * @param param245 param245
            */
           public void receiveResultquery(
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse param245) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorquery(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for relinquish method
            * @param param247 param247
            */
           public void receiveResultrelinquish(
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse param247) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorrelinquish(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for redeem method
            * @param param249 param249
            */
           public void receiveResultredeem(
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.RedeemResponse param249) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorredeem(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for failedRPC method
            * @param param251 param251
            */
           public void receiveResultfailedRPC(
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse param251) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorfailedRPC(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for claim method
            * @param param253 param253
            */
           public void receiveResultclaim(
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse param253) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorclaim(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for queryResult method
            * @param param255 param255
            */
           public void receiveResultqueryResult(
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse param255) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorqueryResult(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for extendTicket method
            * @param param257 param257
            */
           public void receiveResultextendTicket(
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse param257) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorextendTicket(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for close method
            * @param param259 param259
            */
           public void receiveResultclose(
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.CloseResponse param259) {
           }

          /**
           * auto generated Axis2 Error handler
           * @param e e
           */
            public void receiveErrorclose(java.lang.Exception e) {
            }
                


    }
    
