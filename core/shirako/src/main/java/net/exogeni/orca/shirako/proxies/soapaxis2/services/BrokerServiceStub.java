/**
 * BrokerServiceStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
 */
package net.exogeni.orca.shirako.proxies.soapaxis2.services;
        
/*
 *  BrokerServiceStub java implementation
 */
        
public class BrokerServiceStub extends org.apache.axis2.client.Stub
{
    protected org.apache.axis2.description.AxisOperation[] _operations;
    //hashmaps to keep the fault mapping
    private java.util.HashMap faultExeptionNameMap = new java.util.HashMap();
    private java.util.HashMap faultExeptionClassNameMap = new java.util.HashMap();
    private java.util.HashMap faultMessageMap = new java.util.HashMap();

    private void populateAxisService() throws org.apache.axis2.AxisFault {
        //creating the Service with a unique name
        _service = new org.apache.axis2.description.AxisService("BrokerService" + this.hashCode());


        //creating the operations
        org.apache.axis2.description.AxisOperation __operation;

        _operations = new org.apache.axis2.description.AxisOperation[9];

        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "updateLease"));
        _service.addOperation(__operation);



        _operations[0]=__operation;


        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "updateTicket"));
        _service.addOperation(__operation);



        _operations[1]=__operation;


        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "ticket"));
        _service.addOperation(__operation);



        _operations[2]=__operation;


        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "query"));
        _service.addOperation(__operation);



        _operations[3]=__operation;


        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "relinquish"));
        _service.addOperation(__operation);



        _operations[4]=__operation;


        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "failedRPC"));
        _service.addOperation(__operation);



        _operations[5]=__operation;


        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "claim"));
        _service.addOperation(__operation);



        _operations[6]=__operation;


        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "queryResult"));
        _service.addOperation(__operation);



        _operations[7]=__operation;


        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "extendTicket"));
        _service.addOperation(__operation);



        _operations[8]=__operation;


    }
    //populates the faults
    private void populateFaults(){

    }
    /**
      Constructor that takes in a configContext
      @param configurationContext configurationContext
      @param targetEndpoint targetEndpoint
      @throws org.apache.axis2.AxisFault in case of error
      */
    public BrokerServiceStub(org.apache.axis2.context.ConfigurationContext configurationContext,
            java.lang.String targetEndpoint)
            throws org.apache.axis2.AxisFault {
            //To populate AxisService
            populateAxisService();
            populateFaults();
            _serviceClient = new org.apache.axis2.client.ServiceClient(configurationContext,_service);


            configurationContext = _serviceClient.getServiceContext().getConfigurationContext();
            _serviceClient.getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(
                        targetEndpoint));
            _serviceClient.getOptions().setTimeOutInMilliSeconds(1000000000);


    }
    /**
     * Default Constructor
     * @throws org.apache.axis2.AxisFault in case of error
     */
    public BrokerServiceStub() throws org.apache.axis2.AxisFault {

        this("http://localhost:8080/services/BrokerService" );

    }
    /**
     * Constructor taking the target endpoint
     @param targetEndpoint targetEndpoint
     @throws org.apache.axis2.AxisFault in case of error
     */
    public BrokerServiceStub(java.lang.String targetEndpoint) throws org.apache.axis2.AxisFault {
        this(null,targetEndpoint);
    }

    /**
     * Auto generated method signature
     * @param param180 param180
     * @param authToken authToken
     * @return net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse
     * @throws java.rmi.RemoteException in case of error
     */
    public net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse updateLease(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease param180, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException

    {
        try{
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[0].getName());
            _operationClient.getOptions().setAction("updateLease");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            //Style is Doc.


            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                    param180,
                    optimizeContent(new javax.xml.namespace.QName("",
                            "updateLease")));

            //adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
            _messageContext.setEnvelope(env);
            net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);
            //execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient.getMessageContext(
                    org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();


            java.lang.Object object = fromOM(
                    _returnEnv.getBody().getFirstElement() ,
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse.class,
                    getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse)object;

        }catch(org.apache.axis2.AxisFault f){
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt!=null){
                if (faultExeptionNameMap.containsKey(faultElt.getQName())){
                    //make the fault by reflection
                    try{
                        java.lang.String exceptionClassName = (java.lang.String)faultExeptionClassNameMap.get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex=
                            (java.lang.Exception) exceptionClass.newInstance();
                        //message class
                        java.lang.String messageClassName = (java.lang.String)faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt,messageClass,null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[]{messageClass});
                        m.invoke(ex,new java.lang.Object[]{messageObject});

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    }catch(java.lang.ClassCastException e){
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }  catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }   catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                }else{
                    throw f;
                }
            }else{
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
     * @param param180 param180
     * @param authToken authToken
     * @param callback callback
     * @throws java.rmi.RemoteException in case of error
*/
    public  void startupdateLease(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease param180,
            final net.exogeni.orca.shirako.proxies.soapaxis2.services.BrokerServiceCallbackHandler callback, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException{
        org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[0].getName());
        _operationClient.getOptions().setAction("updateLease");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env=null;

        //Style is Doc.


        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                param180,
                optimizeContent(new javax.xml.namespace.QName("",
                        "updateLease")));

        //adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
        _messageContext.setEnvelope(env);
        net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(
                    org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope())
                        );
                callback.receiveResultupdateLease((net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLeaseResponse) object);
                    }
            public void onError(java.lang.Exception e) {
                callback.receiveErrorupdateLease(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if ( _operations[0].getMessageReceiver()==null &&  _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[0].setMessageReceiver(
                    _callbackReceiver);
        }
        //execute the operation client
        _operationClient.execute(false);
            }

    /**
     * Auto generated method signature
      * @param param182 param182
      * @param authToken authToken
      * @return net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse
      * @throws java.rmi.RemoteException in case of error
*/
    public net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse updateTicket(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket param182, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException

    {
        try{
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[1].getName());
            _operationClient.getOptions().setAction("updateTicket");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            //Style is Doc.


            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                    param182,
                    optimizeContent(new javax.xml.namespace.QName("",
                            "updateTicket")));

            //adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
            _messageContext.setEnvelope(env);
            net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);
            //execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient.getMessageContext(
                    org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();


            java.lang.Object object = fromOM(
                    _returnEnv.getBody().getFirstElement() ,
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse.class,
                    getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse)object;

        }catch(org.apache.axis2.AxisFault f){
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt!=null){
                if (faultExeptionNameMap.containsKey(faultElt.getQName())){
                    //make the fault by reflection
                    try{
                        java.lang.String exceptionClassName = (java.lang.String)faultExeptionClassNameMap.get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex=
                            (java.lang.Exception) exceptionClass.newInstance();
                        //message class
                        java.lang.String messageClassName = (java.lang.String)faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt,messageClass,null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[]{messageClass});
                        m.invoke(ex,new java.lang.Object[]{messageObject});

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    }catch(java.lang.ClassCastException e){
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }  catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }   catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                }else{
                    throw f;
                }
            }else{
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
       * @param param182 param182
       * @param authToken authToken
       * @param callback callback
       * @throws java.rmi.RemoteException in case of error
*/
    public  void startupdateTicket(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket param182,
            final net.exogeni.orca.shirako.proxies.soapaxis2.services.BrokerServiceCallbackHandler callback, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException{
        org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[1].getName());
        _operationClient.getOptions().setAction("updateTicket");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env=null;

        //Style is Doc.


        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                param182,
                optimizeContent(new javax.xml.namespace.QName("",
                        "updateTicket")));

        //adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
        _messageContext.setEnvelope(env);
        net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(
                    org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope())
                        );
                callback.receiveResultupdateTicket((net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicketResponse) object);
                    }
            public void onError(java.lang.Exception e) {
                callback.receiveErrorupdateTicket(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if ( _operations[1].getMessageReceiver()==null &&  _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[1].setMessageReceiver(
                    _callbackReceiver);
        }
        //execute the operation client
        _operationClient.execute(false);
            }

    /**
     * Auto generated method signature
        * @param param184 param184
        * @param authToken authToken
        * @return net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse
        * @throws java.rmi.RemoteException in case of error
*/
    public net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse ticket(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.Ticket param184, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException

    {
        try{
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[2].getName());
            _operationClient.getOptions().setAction("ticket");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            //Style is Doc.


            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                    param184,
                    optimizeContent(new javax.xml.namespace.QName("",
                            "ticket")));

            //adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
            _messageContext.setEnvelope(env);
            net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);
            //execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient.getMessageContext(
                    org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();


            java.lang.Object object = fromOM(
                    _returnEnv.getBody().getFirstElement() ,
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse.class,
                    getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse)object;

        }catch(org.apache.axis2.AxisFault f){
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt!=null){
                if (faultExeptionNameMap.containsKey(faultElt.getQName())){
                    //make the fault by reflection
                    try{
                        java.lang.String exceptionClassName = (java.lang.String)faultExeptionClassNameMap.get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex=
                            (java.lang.Exception) exceptionClass.newInstance();
                        //message class
                        java.lang.String messageClassName = (java.lang.String)faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt,messageClass,null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[]{messageClass});
                        m.invoke(ex,new java.lang.Object[]{messageObject});

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    }catch(java.lang.ClassCastException e){
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }  catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }   catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                }else{
                    throw f;
                }
            }else{
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
         * @param param184 param184
         * @param authToken authToken
         * @param callback callback
         * @throws java.rmi.RemoteException in case of error
*/
    public  void startticket(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.Ticket param184,
            final net.exogeni.orca.shirako.proxies.soapaxis2.services.BrokerServiceCallbackHandler callback, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException{
        org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[2].getName());
        _operationClient.getOptions().setAction("ticket");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env=null;

        //Style is Doc.


        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                param184,
                optimizeContent(new javax.xml.namespace.QName("",
                        "ticket")));

        //adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
        _messageContext.setEnvelope(env);
        net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(
                    org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope())
                        );
                callback.receiveResultticket((net.exogeni.orca.shirako.proxies.soapaxis2.services.TicketResponse) object);
                    }
            public void onError(java.lang.Exception e) {
                callback.receiveErrorticket(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if ( _operations[2].getMessageReceiver()==null &&  _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[2].setMessageReceiver(
                    _callbackReceiver);
        }
        //execute the operation client
        _operationClient.execute(false);
            }

    /**
     * Auto generated method signature
         * @param param186 param186
         * @param authToken authToken
         * @return net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse
         * @throws java.rmi.RemoteException in case of error
*/
    public net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse query(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.Query param186, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException

    {
        try{
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[3].getName());
            _operationClient.getOptions().setAction("query");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            //Style is Doc.


            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                    param186,
                    optimizeContent(new javax.xml.namespace.QName("",
                            "query")));

            //adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
            _messageContext.setEnvelope(env);
            net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);
            //execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient.getMessageContext(
                    org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();


            java.lang.Object object = fromOM(
                    _returnEnv.getBody().getFirstElement() ,
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse.class,
                    getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse)object;

        }catch(org.apache.axis2.AxisFault f){
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt!=null){
                if (faultExeptionNameMap.containsKey(faultElt.getQName())){
                    //make the fault by reflection
                    try{
                        java.lang.String exceptionClassName = (java.lang.String)faultExeptionClassNameMap.get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex=
                            (java.lang.Exception) exceptionClass.newInstance();
                        //message class
                        java.lang.String messageClassName = (java.lang.String)faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt,messageClass,null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[]{messageClass});
                        m.invoke(ex,new java.lang.Object[]{messageObject});

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    }catch(java.lang.ClassCastException e){
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }  catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }   catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                }else{
                    throw f;
                }
            }else{
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
         * @param param186 param186
         * @param authToken authToken
         * @param callback callback
         * @throws java.rmi.RemoteException in case of error
*/
    public  void startquery(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.Query param186,
            final net.exogeni.orca.shirako.proxies.soapaxis2.services.BrokerServiceCallbackHandler callback, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException{
        org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[3].getName());
        _operationClient.getOptions().setAction("query");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env=null;

        //Style is Doc.


        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                param186,
                optimizeContent(new javax.xml.namespace.QName("",
                        "query")));

        //adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
        _messageContext.setEnvelope(env);
        net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(
                    org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope())
                        );
                callback.receiveResultquery((net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResponse) object);
                    }
            public void onError(java.lang.Exception e) {
                callback.receiveErrorquery(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if ( _operations[3].getMessageReceiver()==null &&  _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[3].setMessageReceiver(
                    _callbackReceiver);
        }
        //execute the operation client
        _operationClient.execute(false);
            }

    /**
     * Auto generated method signature
         * @param param188 param188
         * @param authToken authToken
         * @return net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse
         * @throws java.rmi.RemoteException in case of error
*/
    public net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse relinquish(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.Relinquish param188, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException

    {
        try{
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[4].getName());
            _operationClient.getOptions().setAction("relinquish");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            //Style is Doc.


            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                    param188,
                    optimizeContent(new javax.xml.namespace.QName("",
                            "relinquish")));

            //adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
            _messageContext.setEnvelope(env);
            net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);
            //execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient.getMessageContext(
                    org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();


            java.lang.Object object = fromOM(
                    _returnEnv.getBody().getFirstElement() ,
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse.class,
                    getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse)object;

        }catch(org.apache.axis2.AxisFault f){
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt!=null){
                if (faultExeptionNameMap.containsKey(faultElt.getQName())){
                    //make the fault by reflection
                    try{
                        java.lang.String exceptionClassName = (java.lang.String)faultExeptionClassNameMap.get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex=
                            (java.lang.Exception) exceptionClass.newInstance();
                        //message class
                        java.lang.String messageClassName = (java.lang.String)faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt,messageClass,null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[]{messageClass});
                        m.invoke(ex,new java.lang.Object[]{messageObject});

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    }catch(java.lang.ClassCastException e){
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }  catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }   catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                }else{
                    throw f;
                }
            }else{
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
         * @param param188 param188
         * @param authToken authToken
         * @param callback callback
         * @throws java.rmi.RemoteException in case of error
*/
    public  void startrelinquish(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.Relinquish param188,
            final net.exogeni.orca.shirako.proxies.soapaxis2.services.BrokerServiceCallbackHandler callback, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException{
        org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[4].getName());
        _operationClient.getOptions().setAction("relinquish");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env=null;

        //Style is Doc.


        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                param188,
                optimizeContent(new javax.xml.namespace.QName("",
                        "relinquish")));

        //adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
        _messageContext.setEnvelope(env);
        net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(
                    org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope())
                        );
                callback.receiveResultrelinquish((net.exogeni.orca.shirako.proxies.soapaxis2.services.RelinquishResponse) object);
                    }
            public void onError(java.lang.Exception e) {
                callback.receiveErrorrelinquish(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if ( _operations[4].getMessageReceiver()==null &&  _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[4].setMessageReceiver(
                    _callbackReceiver);
        }
        //execute the operation client
        _operationClient.execute(false);
            }

    /**
     * Auto generated method signature
          * @param param190 param190
          * @param authToken authToken
          * @return net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse
          * @throws java.rmi.RemoteException in case of error
*/
    public net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse failedRPC(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC param190, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException

    {
        try{
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[5].getName());
            _operationClient.getOptions().setAction("failedRPC");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            //Style is Doc.


            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                    param190,
                    optimizeContent(new javax.xml.namespace.QName("",
                            "failedRPC")));

            //adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
            _messageContext.setEnvelope(env);
            net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);
            //execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient.getMessageContext(
                    org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();


            java.lang.Object object = fromOM(
                    _returnEnv.getBody().getFirstElement() ,
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse.class,
                    getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse)object;

        }catch(org.apache.axis2.AxisFault f){
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt!=null){
                if (faultExeptionNameMap.containsKey(faultElt.getQName())){
                    //make the fault by reflection
                    try{
                        java.lang.String exceptionClassName = (java.lang.String)faultExeptionClassNameMap.get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex=
                            (java.lang.Exception) exceptionClass.newInstance();
                        //message class
                        java.lang.String messageClassName = (java.lang.String)faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt,messageClass,null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[]{messageClass});
                        m.invoke(ex,new java.lang.Object[]{messageObject});

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    }catch(java.lang.ClassCastException e){
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }  catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }   catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                }else{
                    throw f;
                }
            }else{
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
           * @param param190 param190
           * @param authToken authToken
           * @param callback callback
           * @throws java.rmi.RemoteException in case of error
*/
    public  void startfailedRPC(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC param190,
            final net.exogeni.orca.shirako.proxies.soapaxis2.services.BrokerServiceCallbackHandler callback, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException{
        org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[5].getName());
        _operationClient.getOptions().setAction("failedRPC");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env=null;

        //Style is Doc.


        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                param190,
                optimizeContent(new javax.xml.namespace.QName("",
                        "failedRPC")));

        //adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
        _messageContext.setEnvelope(env);
        net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(
                    org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope())
                        );
                callback.receiveResultfailedRPC((net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPCResponse) object);
                    }
            public void onError(java.lang.Exception e) {
                callback.receiveErrorfailedRPC(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if ( _operations[5].getMessageReceiver()==null &&  _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[5].setMessageReceiver(
                    _callbackReceiver);
        }
        //execute the operation client
        _operationClient.execute(false);
            }

    /**
     * Auto generated method signature
           * @param param192 param192
           * @param authToken authToken
           * @return net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse
           * @throws java.rmi.RemoteException in case of error
*/
    public net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse claim(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.Claim param192, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException

    {
        try{
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[6].getName());
            _operationClient.getOptions().setAction("claim");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            //Style is Doc.


            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                    param192,
                    optimizeContent(new javax.xml.namespace.QName("",
                            "claim")));

            //adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
            _messageContext.setEnvelope(env);
            net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);
            //execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient.getMessageContext(
                    org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();


            java.lang.Object object = fromOM(
                    _returnEnv.getBody().getFirstElement() ,
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse.class,
                    getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse)object;

        }catch(org.apache.axis2.AxisFault f){
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt!=null){
                if (faultExeptionNameMap.containsKey(faultElt.getQName())){
                    //make the fault by reflection
                    try{
                        java.lang.String exceptionClassName = (java.lang.String)faultExeptionClassNameMap.get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex=
                            (java.lang.Exception) exceptionClass.newInstance();
                        //message class
                        java.lang.String messageClassName = (java.lang.String)faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt,messageClass,null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[]{messageClass});
                        m.invoke(ex,new java.lang.Object[]{messageObject});

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    }catch(java.lang.ClassCastException e){
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }  catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }   catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                }else{
                    throw f;
                }
            }else{
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
           * @param param192 param192
           * @param authToken authToken
           * @param callback callback
           * @throws java.rmi.RemoteException in case of error
*/
    public  void startclaim(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.Claim param192,
            final net.exogeni.orca.shirako.proxies.soapaxis2.services.BrokerServiceCallbackHandler callback, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException{
        org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[6].getName());
        _operationClient.getOptions().setAction("claim");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env=null;

        //Style is Doc.


        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                param192,
                optimizeContent(new javax.xml.namespace.QName("",
                        "claim")));

        //adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
        _messageContext.setEnvelope(env);
        net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(
                    org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope())
                        );
                callback.receiveResultclaim((net.exogeni.orca.shirako.proxies.soapaxis2.services.ClaimResponse) object);
                    }
            public void onError(java.lang.Exception e) {
                callback.receiveErrorclaim(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if ( _operations[6].getMessageReceiver()==null &&  _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[6].setMessageReceiver(
                    _callbackReceiver);
        }
        //execute the operation client
        _operationClient.execute(false);
            }

    /**
     * Auto generated method signature
           * @param param194 param194
           * @param authToken authToken
           * @return net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse
           * @throws java.rmi.RemoteException in case of error
*/
    public net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse queryResult(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult param194, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException

    {
        try{
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[7].getName());
            _operationClient.getOptions().setAction("queryResult");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            //Style is Doc.


            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                    param194,
                    optimizeContent(new javax.xml.namespace.QName("",
                            "queryResult")));

            //adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
            _messageContext.setEnvelope(env);
            net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);
            //execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient.getMessageContext(
                    org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();


            java.lang.Object object = fromOM(
                    _returnEnv.getBody().getFirstElement() ,
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse.class,
                    getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse)object;

        }catch(org.apache.axis2.AxisFault f){
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt!=null){
                if (faultExeptionNameMap.containsKey(faultElt.getQName())){
                    //make the fault by reflection
                    try{
                        java.lang.String exceptionClassName = (java.lang.String)faultExeptionClassNameMap.get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex=
                            (java.lang.Exception) exceptionClass.newInstance();
                        //message class
                        java.lang.String messageClassName = (java.lang.String)faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt,messageClass,null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[]{messageClass});
                        m.invoke(ex,new java.lang.Object[]{messageObject});

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    }catch(java.lang.ClassCastException e){
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }  catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }   catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                }else{
                    throw f;
                }
            }else{
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
           * @param param194 param194
           * @param authToken authToken
           * @param callback callback
           * @throws java.rmi.RemoteException in case of error
*/
    public  void startqueryResult(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult param194,
            final net.exogeni.orca.shirako.proxies.soapaxis2.services.BrokerServiceCallbackHandler callback, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException{
        org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[7].getName());
        _operationClient.getOptions().setAction("queryResult");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env=null;

        //Style is Doc.


        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                param194,
                optimizeContent(new javax.xml.namespace.QName("",
                        "queryResult")));

        //adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
        _messageContext.setEnvelope(env);
        net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(
                    org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope())
                        );
                callback.receiveResultqueryResult((net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResultResponse) object);
                    }
            public void onError(java.lang.Exception e) {
                callback.receiveErrorqueryResult(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if ( _operations[7].getMessageReceiver()==null &&  _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[7].setMessageReceiver(
                    _callbackReceiver);
        }
        //execute the operation client
        _operationClient.execute(false);
            }

    /**
     * Auto generated method signature
           * @param param196 param196
           * @param authToken authToken
           * @return net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse
           * @throws java.rmi.RemoteException in case of error
*/
    public net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse extendTicket(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicket param196, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException

    {
        try{
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[8].getName());
            _operationClient.getOptions().setAction("extendTicket");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            //Style is Doc.


            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                    param196,
                    optimizeContent(new javax.xml.namespace.QName("",
                            "extendTicket")));

            //adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
            _messageContext.setEnvelope(env);
            net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);
            //execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient.getMessageContext(
                    org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();


            java.lang.Object object = fromOM(
                    _returnEnv.getBody().getFirstElement() ,
                    net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse.class,
                    getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse)object;

        }catch(org.apache.axis2.AxisFault f){
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt!=null){
                if (faultExeptionNameMap.containsKey(faultElt.getQName())){
                    //make the fault by reflection
                    try{
                        java.lang.String exceptionClassName = (java.lang.String)faultExeptionClassNameMap.get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex=
                            (java.lang.Exception) exceptionClass.newInstance();
                        //message class
                        java.lang.String messageClassName = (java.lang.String)faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt,messageClass,null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[]{messageClass});
                        m.invoke(ex,new java.lang.Object[]{messageObject});

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    }catch(java.lang.ClassCastException e){
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }  catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }   catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                }else{
                    throw f;
                }
            }else{
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
           * @param param196 param196
           * @param authToken authToken
           * @param callback callback
           * @throws java.rmi.RemoteException in case of error
*/
    public  void startextendTicket(
            net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicket param196,
            final net.exogeni.orca.shirako.proxies.soapaxis2.services.BrokerServiceCallbackHandler callback, net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException{
        org.apache.axis2.client.OperationClient _operationClient = _serviceClient.createClient(_operations[8].getName());
        _operationClient.getOptions().setAction("extendTicket");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env=null;

        //Style is Doc.


        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()),
                param196,
                optimizeContent(new javax.xml.namespace.QName("",
                        "extendTicket")));

        //adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext() ;
        _messageContext.setEnvelope(env);
        net.exogeni.orca.shirako.proxies.soapaxis2.util.ContextTools.setMyAuthToken(_messageContext, authToken);        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(
                    org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope())
                        );
                callback.receiveResultextendTicket((net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicketResponse) object);
                    }
            public void onError(java.lang.Exception e) {
                callback.receiveErrorextendTicket(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if ( _operations[8].getMessageReceiver()==null &&  _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[8].setMessageReceiver(
                    _callbackReceiver);
        }
        //execute the operation client
        _operationClient.execute(false);
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


    private javax.xml.namespace.QName[] opNameArray = null;
    private boolean optimizeContent(javax.xml.namespace.QName opName) {

        if (opNameArray == null) {
            return false;
        }
        for (int i = 0; i < opNameArray.length; i++) {
            if (opName.equals(opNameArray[i])) {
                return true;   
            }
        }
        return false;
    }
    //http://localhost:8080/services/BrokerService
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


    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease param, boolean optimizeContent){
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateLease.MY_QNAME,factory));

        return emptyEnvelope;
    }


    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket param, boolean optimizeContent){
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.UpdateTicket.MY_QNAME,factory));

        return emptyEnvelope;
    }


    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.Ticket param, boolean optimizeContent){
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.Ticket.MY_QNAME,factory));

        return emptyEnvelope;
    }


    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.Query param, boolean optimizeContent){
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.Query.MY_QNAME,factory));

        return emptyEnvelope;
    }


    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.Relinquish param, boolean optimizeContent){
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.Relinquish.MY_QNAME,factory));

        return emptyEnvelope;
    }


    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC param, boolean optimizeContent){
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.FailedRPC.MY_QNAME,factory));

        return emptyEnvelope;
    }


    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.Claim param, boolean optimizeContent){
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.Claim.MY_QNAME,factory));

        return emptyEnvelope;
    }


    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult param, boolean optimizeContent){
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.QueryResult.MY_QNAME,factory));

        return emptyEnvelope;
    }


    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicket param, boolean optimizeContent){
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.shirako.proxies.soapaxis2.services.ExtendTicket.MY_QNAME,factory));

        return emptyEnvelope;
    }

    /**
     *  get the default envelope
     *  @param factory factory
     *  @return org.apache.axiom.soap.SOAPEnvelope
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

    private void setOpNameArray(){
        opNameArray = null;
    }

}
   
