

        /**
        * BrokerServiceMessageReceiverInOut.java
        *
        * This file was auto-generated from WSDL
        * by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
        */
        package net.exogeni.orca.shirako.proxies.soapaxis2.services;

        /**
        *  BrokerServiceMessageReceiverInOut message receiver
        */

        public class BrokerServiceMessageReceiverInOut extends org.apache.axis2.receivers.AbstractInOutSyncMessageReceiver{


        public void invokeBusinessLogic(org.apache.axis2.context.MessageContext msgContext, org.apache.axis2.context.MessageContext newMsgContext)
        throws org.apache.axis2.AxisFault{

        try {

        // get the implementation class for the Web Service
        Object obj = getTheImplementationObject(msgContext);

        BrokerServiceSkeleton skel = (BrokerServiceSkeleton)obj;
        //Out Envelop
        org.apache.axiom.soap.SOAPEnvelope envelope = null;
        //Find the axisOperation that has been set by the Dispatch phase.
        org.apache.axis2.description.AxisOperation op = msgContext.getOperationContext().getAxisOperation();
        if (op == null) {
        throw new org.apache.axis2.AxisFault("Operation is not located, if this is doclit style the SOAP-ACTION should specified via the SOAP Action to use the RawXMLProvider");
        }

        java.lang.String methodName;
        if(op.getName() != null & (methodName = org.apache.axis2.util.JavaUtils.xmlNameToJava(op.getName().getLocalPart())) != null){

        

            if("updateLease".equals(methodName)){

            
            net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse param37 = null;
                    
                            //doc style
                            net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease wrappedParam =
                                                 (net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease)fromOM(
                        msgContext.getEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease.class,
                        getEnvelopeNamespaces(msgContext.getEnvelope()));
                                    
                                   param37 =
                                             skel.updateLease(wrappedParam) ;
                                        
                                    envelope = toEnvelope(getSOAPFactory(msgContext), param37, false);
                                

            }
        

            if("updateTicket".equals(methodName)){

            
            net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse param39 = null;
                    
                            //doc style
                            net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket wrappedParam =
                                                 (net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket)fromOM(
                        msgContext.getEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket.class,
                        getEnvelopeNamespaces(msgContext.getEnvelope()));
                                    
                                   param39 =
                                             skel.updateTicket(wrappedParam) ;
                                        
                                    envelope = toEnvelope(getSOAPFactory(msgContext), param39, false);
                                

            }
        

            if("ticket".equals(methodName)){

            
            net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse param41 = null;
                    
                            //doc style
                            net.exogeni.orca.shirako.proxies.soapaxis2.services.Ticket wrappedParam =
                                                 (net.exogeni.orca.shirako.proxies.soapaxis2.services.Ticket)fromOM(
                        msgContext.getEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.Ticket.class,
                        getEnvelopeNamespaces(msgContext.getEnvelope()));
                                    
                                   param41 =
                                             skel.ticket(wrappedParam) ;
                                        
                                    envelope = toEnvelope(getSOAPFactory(msgContext), param41, false);
                                

            }
        

            if("query".equals(methodName)){

            
            net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse param43 = null;
                    
                            //doc style
                            net.exogeni.orca.shirako.proxies.soapaxis2.services.Query wrappedParam =
                                                 (net.exogeni.orca.shirako.proxies.soapaxis2.services.Query)fromOM(
                        msgContext.getEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.Query.class,
                        getEnvelopeNamespaces(msgContext.getEnvelope()));
                                    
                                   param43 =
                                             skel.query(wrappedParam) ;
                                        
                                    envelope = toEnvelope(getSOAPFactory(msgContext), param43, false);
                                

            }
        

            if("relinquish".equals(methodName)){

            
            net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse param45 = null;
                    
                            //doc style
                            net.exogeni.orca.shirako.proxies.soapaxis2.services.Relinquish wrappedParam =
                                                 (net.exogeni.orca.shirako.proxies.soapaxis2.services.Relinquish)fromOM(
                        msgContext.getEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.Relinquish.class,
                        getEnvelopeNamespaces(msgContext.getEnvelope()));
                                    
                                   param45 =
                                             skel.relinquish(wrappedParam) ;
                                        
                                    envelope = toEnvelope(getSOAPFactory(msgContext), param45, false);
                                

            }
        

            if("failedRPC".equals(methodName)){

            
            net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse param47 = null;
                    
                            //doc style
                            net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC wrappedParam =
                                                 (net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC)fromOM(
                        msgContext.getEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC.class,
                        getEnvelopeNamespaces(msgContext.getEnvelope()));
                                    
                                   param47 =
                                             skel.failedRPC(wrappedParam) ;
                                        
                                    envelope = toEnvelope(getSOAPFactory(msgContext), param47, false);
                                

            }
        

            if("claim".equals(methodName)){

            
            net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse param49 = null;
                    
                            //doc style
                            net.exogeni.orca.shirako.proxies.soapaxis2.services.Claim wrappedParam =
                                                 (net.exogeni.orca.shirako.proxies.soapaxis2.services.Claim)fromOM(
                        msgContext.getEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.Claim.class,
                        getEnvelopeNamespaces(msgContext.getEnvelope()));
                                    
                                   param49 =
                                             skel.claim(wrappedParam) ;
                                        
                                    envelope = toEnvelope(getSOAPFactory(msgContext), param49, false);
                                

            }
        

            if("queryResult".equals(methodName)){

            
            net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse param51 = null;
                    
                            //doc style
                            net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult wrappedParam =
                                                 (net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult)fromOM(
                        msgContext.getEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult.class,
                        getEnvelopeNamespaces(msgContext.getEnvelope()));
                                    
                                   param51 =
                                             skel.queryResult(wrappedParam) ;
                                        
                                    envelope = toEnvelope(getSOAPFactory(msgContext), param51, false);
                                

            }
        

            if("extendTicket".equals(methodName)){

            
            net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse param53 = null;
                    
                            //doc style
                            net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicket wrappedParam =
                                                 (net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicket)fromOM(
                        msgContext.getEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicket.class,
                        getEnvelopeNamespaces(msgContext.getEnvelope()));
                                    
                                   param53 =
                                             skel.extendTicket(wrappedParam) ;
                                        
                                    envelope = toEnvelope(getSOAPFactory(msgContext), param53, false);
                                

            }
        

        newMsgContext.setEnvelope(envelope);
        }
        }
        catch (Exception e) {
        throw org.apache.axis2.AxisFault.makeFault(e);
        }
        }
        
        //
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.Ticket param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.Ticket.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.Query param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.Query.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.Relinquish param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.Relinquish.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.Claim param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.Claim.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicket param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicket.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse param, boolean optimizeContent){
                      org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                       
                                emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse.MY_QNAME,factory));
                            

                     return emptyEnvelope;
                    }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse param, boolean optimizeContent){
                      org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                       
                                emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse.MY_QNAME,factory));
                            

                     return emptyEnvelope;
                    }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse param, boolean optimizeContent){
                      org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                       
                                emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse.MY_QNAME,factory));
                            

                     return emptyEnvelope;
                    }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse param, boolean optimizeContent){
                      org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                       
                                emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse.MY_QNAME,factory));
                            

                     return emptyEnvelope;
                    }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse param, boolean optimizeContent){
                      org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                       
                                emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse.MY_QNAME,factory));
                            

                     return emptyEnvelope;
                    }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse param, boolean optimizeContent){
                      org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                       
                                emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse.MY_QNAME,factory));
                            

                     return emptyEnvelope;
                    }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse param, boolean optimizeContent){
                      org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                       
                                emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse.MY_QNAME,factory));
                            

                     return emptyEnvelope;
                    }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse param, boolean optimizeContent){
                      org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                       
                                emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse.MY_QNAME,factory));
                            

                     return emptyEnvelope;
                    }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse param, boolean optimizeContent){
                      org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                       
                                emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse.MY_QNAME,factory));
                            

                     return emptyEnvelope;
                    }
                    


        /**
        *  get the default envelope
        *  @param factory factory
        *  @return SOAPEnvelope
        */
        private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory){
        return factory.getDefaultEnvelope();
        }


        private  java.lang.Object fromOM(
        org.apache.axiom.om.OMElement param,
        java.lang.Class type,
        java.util.Map extraNamespaces){

        try {
        
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.Ticket.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.Ticket.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.Query.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.Query.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.Relinquish.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.Relinquish.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.Claim.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.Claim.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicket.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicket.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
        } catch (Exception e) {
        throw new RuntimeException(e);
        }
           return null;
        }



    

        /**
        *  A utility method that copies the namepaces from the SOAPEnvelope
        *  @param env env
        *  @return Map
        */
        private java.util.Map getEnvelopeNamespaces(org.apache.axiom.soap.SOAPEnvelope env){
        java.util.Map returnMap = new java.util.HashMap();
        java.util.Iterator namespaceIterator = env.getAllDeclaredNamespaces();
        while (namespaceIterator.hasNext()) {
        org.apache.axiom.om.OMNamespace ns = (org.apache.axiom.om.OMNamespace) namespaceIterator.next();
        returnMap.put(ns.getPrefix(),ns.getNamespaceURI());
        }
        return returnMap;
        }

        private org.apache.axis2.AxisFault createAxisFault(java.lang.Exception e) {
        org.apache.axis2.AxisFault f;
        Throwable cause = e.getCause();
        if (cause != null) {
            f = new org.apache.axis2.AxisFault(e.getMessage(), cause);
        } else {
            f = new org.apache.axis2.AxisFault(e.getMessage());
        }

        return f;
    }

        }//end of class
    
