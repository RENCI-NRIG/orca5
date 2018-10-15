

        /**
        * ServiceManagerServiceMessageReceiverInOut.java
        *
        * This file was auto-generated from WSDL
        * by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
        */
        package net.exogeni.orca.shirako.proxies.soapaxis2.services;

        /**
        *  ServiceManagerServiceMessageReceiverInOut message receiver
        */

        public class ServiceManagerServiceMessageReceiverInOut extends org.apache.axis2.receivers.AbstractInOutSyncMessageReceiver{


        public void invokeBusinessLogic(org.apache.axis2.context.MessageContext msgContext, org.apache.axis2.context.MessageContext newMsgContext)
        throws org.apache.axis2.AxisFault{

        try {

        // get the implementation class for the Web Service
        Object obj = getTheImplementationObject(msgContext);

        ServiceManagerServiceSkeleton skel = (ServiceManagerServiceSkeleton)obj;
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

            
            net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse param21 = null;
                    
                            //doc style
                            net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease wrappedParam =
                                                 (net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease)fromOM(
                        msgContext.getEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease.class,
                        getEnvelopeNamespaces(msgContext.getEnvelope()));
                                    
                                   param21 =
                                             skel.updateLease(wrappedParam) ;
                                        
                                    envelope = toEnvelope(getSOAPFactory(msgContext), param21, false);
                                

            }
        

            if("updateTicket".equals(methodName)){

            
            net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse param23 = null;
                    
                            //doc style
                            net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket wrappedParam =
                                                 (net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket)fromOM(
                        msgContext.getEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket.class,
                        getEnvelopeNamespaces(msgContext.getEnvelope()));
                                    
                                   param23 =
                                             skel.updateTicket(wrappedParam) ;
                                        
                                    envelope = toEnvelope(getSOAPFactory(msgContext), param23, false);
                                

            }
        

            if("query".equals(methodName)){

            
            net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse param25 = null;
                    
                            //doc style
                            net.exogeni.orca.shirako.proxies.soapaxis2.services.Query wrappedParam =
                                                 (net.exogeni.orca.shirako.proxies.soapaxis2.services.Query)fromOM(
                        msgContext.getEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.Query.class,
                        getEnvelopeNamespaces(msgContext.getEnvelope()));
                                    
                                   param25 =
                                             skel.query(wrappedParam) ;
                                        
                                    envelope = toEnvelope(getSOAPFactory(msgContext), param25, false);
                                

            }
        

            if("failedRPC".equals(methodName)){

            
            net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse param27 = null;
                    
                            //doc style
                            net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC wrappedParam =
                                                 (net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC)fromOM(
                        msgContext.getEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC.class,
                        getEnvelopeNamespaces(msgContext.getEnvelope()));
                                    
                                   param27 =
                                             skel.failedRPC(wrappedParam) ;
                                        
                                    envelope = toEnvelope(getSOAPFactory(msgContext), param27, false);
                                

            }
        

            if("queryResult".equals(methodName)){

            
            net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse param29 = null;
                    
                            //doc style
                            net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult wrappedParam =
                                                 (net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult)fromOM(
                        msgContext.getEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult.class,
                        getEnvelopeNamespaces(msgContext.getEnvelope()));
                                    
                                   param29 =
                                             skel.queryResult(wrappedParam) ;
                                        
                                    envelope = toEnvelope(getSOAPFactory(msgContext), param29, false);
                                

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
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.Query param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.Query.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse.MY_QNAME,
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
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult.MY_QNAME,
                                  org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse param, boolean optimizeContent){
            
                     return param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse.MY_QNAME,
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
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse param, boolean optimizeContent){
                      org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                       
                                emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse.MY_QNAME,factory));
                            

                     return emptyEnvelope;
                    }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse param, boolean optimizeContent){
                      org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                       
                                emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse.MY_QNAME,factory));
                            

                     return emptyEnvelope;
                    }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse param, boolean optimizeContent){
                      org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                       
                                emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse.MY_QNAME,factory));
                            

                     return emptyEnvelope;
                    }
                    


        /**
        *  get the default envelope
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
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.Query.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.Query.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse.class.equals(type)){
                
                           return net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
        } catch (Exception e) {
        throw new RuntimeException(e);
        }
           return null;
        }



    

        /**
        *  A utility method that copies the namepaces from the SOAPEnvelope
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
    
