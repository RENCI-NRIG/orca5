
/**
* NodeAgentServiceStub.java
*
* This file was auto-generated from WSDL
* by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
*/
package orca.nodeagent;

/*
*  NodeAgentServiceStub java implementation
*/

public class NodeAgentServiceStub extends org.apache.axis2.client.Stub {
    protected org.apache.axis2.description.AxisOperation[] _operations;

    // hashmaps to keep the fault mapping
    private java.util.HashMap faultExeptionNameMap = new java.util.HashMap();
    private java.util.HashMap faultExeptionClassNameMap = new java.util.HashMap();
    private java.util.HashMap faultMessageMap = new java.util.HashMap();

    private void populateAxisService() throws org.apache.axis2.AxisFault {

        // creating the Service with a unique name
        _service = new org.apache.axis2.description.AxisService("NodeAgentService" + this.hashCode());

        // creating the operations
        org.apache.axis2.description.AxisOperation __operation;

        _operations = new org.apache.axis2.description.AxisOperation[11];

        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "uninstallDriver"));
        _service.addOperation(__operation);

        _operations[0] = __operation;

        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "executeScript"));
        _service.addOperation(__operation);

        _operations[1] = __operation;

        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "testFunc"));
        _service.addOperation(__operation);

        _operations[2] = __operation;

        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "executeObjectDriver"));
        _service.addOperation(__operation);

        _operations[3] = __operation;

        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "unregisterKey"));
        _service.addOperation(__operation);

        _operations[4] = __operation;

        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "executeDriver"));
        _service.addOperation(__operation);

        _operations[5] = __operation;

        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "upgradeDriver"));
        _service.addOperation(__operation);

        _operations[6] = __operation;

        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "registerAuthorityKey"));
        _service.addOperation(__operation);

        _operations[7] = __operation;

        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "getServiceKey"));
        _service.addOperation(__operation);

        _operations[8] = __operation;

        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "registerKey"));
        _service.addOperation(__operation);

        _operations[9] = __operation;

        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "installDriver"));
        _service.addOperation(__operation);

        _operations[10] = __operation;

    }

    // populates the faults
    private void populateFaults() {

    }

    /**
     * Constructor that takes in a configContext
     */
    public NodeAgentServiceStub(org.apache.axis2.context.ConfigurationContext configurationContext,
            java.lang.String targetEndpoint) throws org.apache.axis2.AxisFault {
        // To populate AxisService
        populateAxisService();
        populateFaults();

        _serviceClient = new org.apache.axis2.client.ServiceClient(configurationContext, _service);

        configurationContext = _serviceClient.getServiceContext().getConfigurationContext();

        _serviceClient.getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(targetEndpoint));
        _serviceClient.getOptions().setTimeOutInMilliSeconds(1000000000);

    }

    /**
     * Default Constructor
     */
    public NodeAgentServiceStub() throws org.apache.axis2.AxisFault {

        this("http://localhost:8080/shirako/services/NodeAgentService");

    }

    /**
     * Constructor taking the target endpoint
     */
    public NodeAgentServiceStub(java.lang.String targetEndpoint) throws org.apache.axis2.AxisFault {
        this(null, targetEndpoint);
    }

    /**
     * Auto generated method signature
     * 
     * @see orca.nodeagent.NodeAgentService#uninstallDriver
     * @param param220
     * 
     */
    public orca.nodeagent.documents.ResultElement uninstallDriver(

            orca.nodeagent.documents.DriverElement param220) throws java.rmi.RemoteException

    {
        try {
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                    .createClient(_operations[0].getName());
            _operationClient.getOptions().setAction("uninstallDriver");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            // Style is Doc.

            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param220,
                    optimizeContent(new javax.xml.namespace.QName("", "uninstallDriver")));

            // adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
            _messageContext.setEnvelope(env);

            // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);

            // execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient
                    .getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();

            java.lang.Object object = fromOM(_returnEnv.getBody().getFirstElement(),
                    orca.nodeagent.documents.ResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (orca.nodeagent.documents.ResultElement) object;

        } catch (org.apache.axis2.AxisFault f) {
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt != null) {
                if (faultExeptionNameMap.containsKey(faultElt.getQName())) {
                    // make the fault by reflection
                    try {
                        java.lang.String exceptionClassName = (java.lang.String) faultExeptionClassNameMap
                                .get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex = (java.lang.Exception) exceptionClass.newInstance();
                        // message class
                        java.lang.String messageClassName = (java.lang.String) faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt, messageClass, null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[] { messageClass });
                        m.invoke(ex, new java.lang.Object[] { messageObject });

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    } catch (java.lang.ClassCastException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                } else {
                    throw f;
                }
            } else {
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
     * 
     * @see orca.nodeagent.NodeAgentService#startuninstallDriver
     * @param param220
     * 
     */
    public void startuninstallDriver(

            orca.nodeagent.documents.DriverElement param220,

            final orca.nodeagent.NodeAgentServiceCallbackHandler callback)

            throws java.rmi.RemoteException {

        org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                .createClient(_operations[0].getName());
        _operationClient.getOptions().setAction("uninstallDriver");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env = null;

        // Style is Doc.

        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param220,
                optimizeContent(new javax.xml.namespace.QName("", "uninstallDriver")));

        // adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
        _messageContext.setEnvelope(env);

        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        orca.nodeagent.documents.ResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultuninstallDriver((orca.nodeagent.documents.ResultElement) object);
            }

            public void onError(java.lang.Exception e) {
                callback.receiveErroruninstallDriver(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if (_operations[0].getMessageReceiver() == null && _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[0].setMessageReceiver(_callbackReceiver);
        }

        // execute the operation client
        _operationClient.execute(false);

    }

    /**
     * Auto generated method signature
     * 
     * @see orca.nodeagent.NodeAgentService#executeScript
     * @param param222
     * 
     */
    public orca.nodeagent.documents.ScriptResultElement executeScript(

            orca.nodeagent.documents.ScriptElement param222) throws java.rmi.RemoteException

    {
        try {
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                    .createClient(_operations[1].getName());
            _operationClient.getOptions().setAction("executeScript");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            // Style is Doc.

            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param222,
                    optimizeContent(new javax.xml.namespace.QName("", "executeScript")));

            // adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
            _messageContext.setEnvelope(env);

            // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);

            // execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient
                    .getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();

            java.lang.Object object = fromOM(_returnEnv.getBody().getFirstElement(),
                    orca.nodeagent.documents.ScriptResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (orca.nodeagent.documents.ScriptResultElement) object;

        } catch (org.apache.axis2.AxisFault f) {
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt != null) {
                if (faultExeptionNameMap.containsKey(faultElt.getQName())) {
                    // make the fault by reflection
                    try {
                        java.lang.String exceptionClassName = (java.lang.String) faultExeptionClassNameMap
                                .get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex = (java.lang.Exception) exceptionClass.newInstance();
                        // message class
                        java.lang.String messageClassName = (java.lang.String) faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt, messageClass, null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[] { messageClass });
                        m.invoke(ex, new java.lang.Object[] { messageObject });

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    } catch (java.lang.ClassCastException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                } else {
                    throw f;
                }
            } else {
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
     * 
     * @see orca.nodeagent.NodeAgentService#startexecuteScript
     * @param param222
     * 
     */
    public void startexecuteScript(

            orca.nodeagent.documents.ScriptElement param222,

            final orca.nodeagent.NodeAgentServiceCallbackHandler callback)

            throws java.rmi.RemoteException {

        org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                .createClient(_operations[1].getName());
        _operationClient.getOptions().setAction("executeScript");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env = null;

        // Style is Doc.

        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param222,
                optimizeContent(new javax.xml.namespace.QName("", "executeScript")));

        // adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
        _messageContext.setEnvelope(env);

        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        orca.nodeagent.documents.ScriptResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultexecuteScript((orca.nodeagent.documents.ScriptResultElement) object);
            }

            public void onError(java.lang.Exception e) {
                callback.receiveErrorexecuteScript(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if (_operations[1].getMessageReceiver() == null && _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[1].setMessageReceiver(_callbackReceiver);
        }

        // execute the operation client
        _operationClient.execute(false);

    }

    /**
     * Auto generated method signature
     * 
     * @see orca.nodeagent.NodeAgentService#testFunc
     * @param param224
     * 
     */
    public orca.nodeagent.documents.TestFuncResultElement testFunc(

            orca.nodeagent.documents.TestFuncElement param224) throws java.rmi.RemoteException

    {
        try {
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                    .createClient(_operations[2].getName());
            _operationClient.getOptions().setAction("testFunc");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            // Style is Doc.

            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param224,
                    optimizeContent(new javax.xml.namespace.QName("", "testFunc")));

            // adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
            _messageContext.setEnvelope(env);

            // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);

            // execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient
                    .getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();

            java.lang.Object object = fromOM(_returnEnv.getBody().getFirstElement(),
                    orca.nodeagent.documents.TestFuncResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (orca.nodeagent.documents.TestFuncResultElement) object;

        } catch (org.apache.axis2.AxisFault f) {
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt != null) {
                if (faultExeptionNameMap.containsKey(faultElt.getQName())) {
                    // make the fault by reflection
                    try {
                        java.lang.String exceptionClassName = (java.lang.String) faultExeptionClassNameMap
                                .get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex = (java.lang.Exception) exceptionClass.newInstance();
                        // message class
                        java.lang.String messageClassName = (java.lang.String) faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt, messageClass, null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[] { messageClass });
                        m.invoke(ex, new java.lang.Object[] { messageObject });

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    } catch (java.lang.ClassCastException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                } else {
                    throw f;
                }
            } else {
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
     * 
     * @see orca.nodeagent.NodeAgentService#starttestFunc
     * @param param224
     * 
     */
    public void starttestFunc(

            orca.nodeagent.documents.TestFuncElement param224,

            final orca.nodeagent.NodeAgentServiceCallbackHandler callback)

            throws java.rmi.RemoteException {

        org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                .createClient(_operations[2].getName());
        _operationClient.getOptions().setAction("testFunc");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env = null;

        // Style is Doc.

        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param224,
                optimizeContent(new javax.xml.namespace.QName("", "testFunc")));

        // adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
        _messageContext.setEnvelope(env);

        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        orca.nodeagent.documents.TestFuncResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResulttestFunc((orca.nodeagent.documents.TestFuncResultElement) object);
            }

            public void onError(java.lang.Exception e) {
                callback.receiveErrortestFunc(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if (_operations[2].getMessageReceiver() == null && _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[2].setMessageReceiver(_callbackReceiver);
        }

        // execute the operation client
        _operationClient.execute(false);

    }

    /**
     * Auto generated method signature
     * 
     * @see orca.nodeagent.NodeAgentService#executeObjectDriver
     * @param param226
     * 
     */
    public orca.nodeagent.documents.ResultElement executeObjectDriver(

            orca.nodeagent.documents.DriverObjectRequestElement param226) throws java.rmi.RemoteException

    {
        try {
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                    .createClient(_operations[3].getName());
            _operationClient.getOptions().setAction("executeObjectDriver");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            // Style is Doc.

            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param226,
                    optimizeContent(new javax.xml.namespace.QName("", "executeObjectDriver")));

            // adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
            _messageContext.setEnvelope(env);

            // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);

            // execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient
                    .getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();

            java.lang.Object object = fromOM(_returnEnv.getBody().getFirstElement(),
                    orca.nodeagent.documents.ResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (orca.nodeagent.documents.ResultElement) object;

        } catch (org.apache.axis2.AxisFault f) {
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt != null) {
                if (faultExeptionNameMap.containsKey(faultElt.getQName())) {
                    // make the fault by reflection
                    try {
                        java.lang.String exceptionClassName = (java.lang.String) faultExeptionClassNameMap
                                .get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex = (java.lang.Exception) exceptionClass.newInstance();
                        // message class
                        java.lang.String messageClassName = (java.lang.String) faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt, messageClass, null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[] { messageClass });
                        m.invoke(ex, new java.lang.Object[] { messageObject });

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    } catch (java.lang.ClassCastException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                } else {
                    throw f;
                }
            } else {
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
     * 
     * @see orca.nodeagent.NodeAgentService#startexecuteObjectDriver
     * @param param226
     * 
     */
    public void startexecuteObjectDriver(

            orca.nodeagent.documents.DriverObjectRequestElement param226,

            final orca.nodeagent.NodeAgentServiceCallbackHandler callback)

            throws java.rmi.RemoteException {

        org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                .createClient(_operations[3].getName());
        _operationClient.getOptions().setAction("executeObjectDriver");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env = null;

        // Style is Doc.

        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param226,
                optimizeContent(new javax.xml.namespace.QName("", "executeObjectDriver")));

        // adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
        _messageContext.setEnvelope(env);

        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        orca.nodeagent.documents.ResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultexecuteObjectDriver((orca.nodeagent.documents.ResultElement) object);
            }

            public void onError(java.lang.Exception e) {
                callback.receiveErrorexecuteObjectDriver(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if (_operations[3].getMessageReceiver() == null && _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[3].setMessageReceiver(_callbackReceiver);
        }

        // execute the operation client
        _operationClient.execute(false);

    }

    /**
     * Auto generated method signature
     * 
     * @see orca.nodeagent.NodeAgentService#unregisterKey
     * @param param228
     * 
     */
    public orca.nodeagent.documents.UnregisterKeyResultElement unregisterKey(

            orca.nodeagent.documents.UnregisterKeyElement param228) throws java.rmi.RemoteException

    {
        try {
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                    .createClient(_operations[4].getName());
            _operationClient.getOptions().setAction("unregisterKey");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            // Style is Doc.

            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param228,
                    optimizeContent(new javax.xml.namespace.QName("", "unregisterKey")));

            // adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
            _messageContext.setEnvelope(env);

            // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);

            // execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient
                    .getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();

            java.lang.Object object = fromOM(_returnEnv.getBody().getFirstElement(),
                    orca.nodeagent.documents.UnregisterKeyResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (orca.nodeagent.documents.UnregisterKeyResultElement) object;

        } catch (org.apache.axis2.AxisFault f) {
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt != null) {
                if (faultExeptionNameMap.containsKey(faultElt.getQName())) {
                    // make the fault by reflection
                    try {
                        java.lang.String exceptionClassName = (java.lang.String) faultExeptionClassNameMap
                                .get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex = (java.lang.Exception) exceptionClass.newInstance();
                        // message class
                        java.lang.String messageClassName = (java.lang.String) faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt, messageClass, null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[] { messageClass });
                        m.invoke(ex, new java.lang.Object[] { messageObject });

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    } catch (java.lang.ClassCastException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                } else {
                    throw f;
                }
            } else {
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
     * 
     * @see orca.nodeagent.NodeAgentService#startunregisterKey
     * @param param228
     * 
     */
    public void startunregisterKey(

            orca.nodeagent.documents.UnregisterKeyElement param228,

            final orca.nodeagent.NodeAgentServiceCallbackHandler callback)

            throws java.rmi.RemoteException {

        org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                .createClient(_operations[4].getName());
        _operationClient.getOptions().setAction("unregisterKey");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env = null;

        // Style is Doc.

        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param228,
                optimizeContent(new javax.xml.namespace.QName("", "unregisterKey")));

        // adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
        _messageContext.setEnvelope(env);

        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        orca.nodeagent.documents.UnregisterKeyResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultunregisterKey((orca.nodeagent.documents.UnregisterKeyResultElement) object);
            }

            public void onError(java.lang.Exception e) {
                callback.receiveErrorunregisterKey(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if (_operations[4].getMessageReceiver() == null && _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[4].setMessageReceiver(_callbackReceiver);
        }

        // execute the operation client
        _operationClient.execute(false);

    }

    /**
     * Auto generated method signature
     * 
     * @see orca.nodeagent.NodeAgentService#executeDriver
     * @param param230
     * 
     */
    public orca.nodeagent.documents.ResultElement executeDriver(

            orca.nodeagent.documents.DriverRequestElement param230) throws java.rmi.RemoteException

    {
        try {
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                    .createClient(_operations[5].getName());
            _operationClient.getOptions().setAction("executeDriver");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            // Style is Doc.

            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param230,
                    optimizeContent(new javax.xml.namespace.QName("", "executeDriver")));

            // adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
            _messageContext.setEnvelope(env);

            // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);

            // execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient
                    .getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();

            java.lang.Object object = fromOM(_returnEnv.getBody().getFirstElement(),
                    orca.nodeagent.documents.ResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (orca.nodeagent.documents.ResultElement) object;

        } catch (org.apache.axis2.AxisFault f) {
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt != null) {
                if (faultExeptionNameMap.containsKey(faultElt.getQName())) {
                    // make the fault by reflection
                    try {
                        java.lang.String exceptionClassName = (java.lang.String) faultExeptionClassNameMap
                                .get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex = (java.lang.Exception) exceptionClass.newInstance();
                        // message class
                        java.lang.String messageClassName = (java.lang.String) faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt, messageClass, null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[] { messageClass });
                        m.invoke(ex, new java.lang.Object[] { messageObject });

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    } catch (java.lang.ClassCastException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                } else {
                    throw f;
                }
            } else {
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
     * 
     * @see orca.nodeagent.NodeAgentService#startexecuteDriver
     * @param param230
     * 
     */
    public void startexecuteDriver(

            orca.nodeagent.documents.DriverRequestElement param230,

            final orca.nodeagent.NodeAgentServiceCallbackHandler callback)

            throws java.rmi.RemoteException {

        org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                .createClient(_operations[5].getName());
        _operationClient.getOptions().setAction("executeDriver");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env = null;

        // Style is Doc.

        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param230,
                optimizeContent(new javax.xml.namespace.QName("", "executeDriver")));

        // adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
        _messageContext.setEnvelope(env);

        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        orca.nodeagent.documents.ResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultexecuteDriver((orca.nodeagent.documents.ResultElement) object);
            }

            public void onError(java.lang.Exception e) {
                callback.receiveErrorexecuteDriver(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if (_operations[5].getMessageReceiver() == null && _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[5].setMessageReceiver(_callbackReceiver);
        }

        // execute the operation client
        _operationClient.execute(false);

    }

    /**
     * Auto generated method signature
     * 
     * @see orca.nodeagent.NodeAgentService#upgradeDriver
     * @param param232
     * 
     */
    public orca.nodeagent.documents.ResultElement upgradeDriver(

            orca.nodeagent.documents.DriverElement param232) throws java.rmi.RemoteException

    {
        try {
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                    .createClient(_operations[6].getName());
            _operationClient.getOptions().setAction("upgradeDriver");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            // Style is Doc.

            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param232,
                    optimizeContent(new javax.xml.namespace.QName("", "upgradeDriver")));

            // adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
            _messageContext.setEnvelope(env);

            // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);

            // execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient
                    .getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();

            java.lang.Object object = fromOM(_returnEnv.getBody().getFirstElement(),
                    orca.nodeagent.documents.ResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (orca.nodeagent.documents.ResultElement) object;

        } catch (org.apache.axis2.AxisFault f) {
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt != null) {
                if (faultExeptionNameMap.containsKey(faultElt.getQName())) {
                    // make the fault by reflection
                    try {
                        java.lang.String exceptionClassName = (java.lang.String) faultExeptionClassNameMap
                                .get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex = (java.lang.Exception) exceptionClass.newInstance();
                        // message class
                        java.lang.String messageClassName = (java.lang.String) faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt, messageClass, null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[] { messageClass });
                        m.invoke(ex, new java.lang.Object[] { messageObject });

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    } catch (java.lang.ClassCastException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                } else {
                    throw f;
                }
            } else {
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
     * 
     * @see orca.nodeagent.NodeAgentService#startupgradeDriver
     * @param param232
     * 
     */
    public void startupgradeDriver(

            orca.nodeagent.documents.DriverElement param232,

            final orca.nodeagent.NodeAgentServiceCallbackHandler callback)

            throws java.rmi.RemoteException {

        org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                .createClient(_operations[6].getName());
        _operationClient.getOptions().setAction("upgradeDriver");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env = null;

        // Style is Doc.

        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param232,
                optimizeContent(new javax.xml.namespace.QName("", "upgradeDriver")));

        // adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
        _messageContext.setEnvelope(env);

        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        orca.nodeagent.documents.ResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultupgradeDriver((orca.nodeagent.documents.ResultElement) object);
            }

            public void onError(java.lang.Exception e) {
                callback.receiveErrorupgradeDriver(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if (_operations[6].getMessageReceiver() == null && _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[6].setMessageReceiver(_callbackReceiver);
        }

        // execute the operation client
        _operationClient.execute(false);

    }

    /**
     * Auto generated method signature
     * 
     * @see orca.nodeagent.NodeAgentService#registerAuthorityKey
     * @param param234
     * 
     */
    public orca.nodeagent.documents.RegisterAuthorityKeyResultElement registerAuthorityKey(

            orca.nodeagent.documents.RegisterAuthorityKeyElement param234) throws java.rmi.RemoteException

    {
        try {
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                    .createClient(_operations[7].getName());
            _operationClient.getOptions().setAction("registerAuthorityKey");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            // Style is Doc.

            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param234,
                    optimizeContent(new javax.xml.namespace.QName("", "registerAuthorityKey")));

            // adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
            _messageContext.setEnvelope(env);

            // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);

            // execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient
                    .getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();

            java.lang.Object object = fromOM(_returnEnv.getBody().getFirstElement(),
                    orca.nodeagent.documents.RegisterAuthorityKeyResultElement.class,
                    getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (orca.nodeagent.documents.RegisterAuthorityKeyResultElement) object;

        } catch (org.apache.axis2.AxisFault f) {
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt != null) {
                if (faultExeptionNameMap.containsKey(faultElt.getQName())) {
                    // make the fault by reflection
                    try {
                        java.lang.String exceptionClassName = (java.lang.String) faultExeptionClassNameMap
                                .get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex = (java.lang.Exception) exceptionClass.newInstance();
                        // message class
                        java.lang.String messageClassName = (java.lang.String) faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt, messageClass, null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[] { messageClass });
                        m.invoke(ex, new java.lang.Object[] { messageObject });

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    } catch (java.lang.ClassCastException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                } else {
                    throw f;
                }
            } else {
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
     * 
     * @see orca.nodeagent.NodeAgentService#startregisterAuthorityKey
     * @param param234
     * 
     */
    public void startregisterAuthorityKey(

            orca.nodeagent.documents.RegisterAuthorityKeyElement param234,

            final orca.nodeagent.NodeAgentServiceCallbackHandler callback)

            throws java.rmi.RemoteException {

        org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                .createClient(_operations[7].getName());
        _operationClient.getOptions().setAction("registerAuthorityKey");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env = null;

        // Style is Doc.

        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param234,
                optimizeContent(new javax.xml.namespace.QName("", "registerAuthorityKey")));

        // adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
        _messageContext.setEnvelope(env);

        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        orca.nodeagent.documents.RegisterAuthorityKeyResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultregisterAuthorityKey(
                        (orca.nodeagent.documents.RegisterAuthorityKeyResultElement) object);
            }

            public void onError(java.lang.Exception e) {
                callback.receiveErrorregisterAuthorityKey(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if (_operations[7].getMessageReceiver() == null && _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[7].setMessageReceiver(_callbackReceiver);
        }

        // execute the operation client
        _operationClient.execute(false);

    }

    /**
     * Auto generated method signature
     * 
     * @see orca.nodeagent.NodeAgentService#getServiceKey
     * @param param236
     * 
     */
    public orca.nodeagent.documents.GetServiceKeyResultElement getServiceKey(

            orca.nodeagent.documents.GetServiceKeyElement param236) throws java.rmi.RemoteException

    {
        try {
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                    .createClient(_operations[8].getName());
            _operationClient.getOptions().setAction("getServiceKey");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            // Style is Doc.

            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param236,
                    optimizeContent(new javax.xml.namespace.QName("", "getServiceKey")));

            // adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
            _messageContext.setEnvelope(env);

            // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);

            // execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient
                    .getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();

            java.lang.Object object = fromOM(_returnEnv.getBody().getFirstElement(),
                    orca.nodeagent.documents.GetServiceKeyResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (orca.nodeagent.documents.GetServiceKeyResultElement) object;

        } catch (org.apache.axis2.AxisFault f) {
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt != null) {
                if (faultExeptionNameMap.containsKey(faultElt.getQName())) {
                    // make the fault by reflection
                    try {
                        java.lang.String exceptionClassName = (java.lang.String) faultExeptionClassNameMap
                                .get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex = (java.lang.Exception) exceptionClass.newInstance();
                        // message class
                        java.lang.String messageClassName = (java.lang.String) faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt, messageClass, null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[] { messageClass });
                        m.invoke(ex, new java.lang.Object[] { messageObject });

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    } catch (java.lang.ClassCastException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                } else {
                    throw f;
                }
            } else {
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
     * 
     * @see orca.nodeagent.NodeAgentService#startgetServiceKey
     * @param param236
     * 
     */
    public void startgetServiceKey(

            orca.nodeagent.documents.GetServiceKeyElement param236,

            final orca.nodeagent.NodeAgentServiceCallbackHandler callback)

            throws java.rmi.RemoteException {

        org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                .createClient(_operations[8].getName());
        _operationClient.getOptions().setAction("getServiceKey");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env = null;

        // Style is Doc.

        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param236,
                optimizeContent(new javax.xml.namespace.QName("", "getServiceKey")));

        // adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
        _messageContext.setEnvelope(env);

        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        orca.nodeagent.documents.GetServiceKeyResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultgetServiceKey((orca.nodeagent.documents.GetServiceKeyResultElement) object);
            }

            public void onError(java.lang.Exception e) {
                callback.receiveErrorgetServiceKey(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if (_operations[8].getMessageReceiver() == null && _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[8].setMessageReceiver(_callbackReceiver);
        }

        // execute the operation client
        _operationClient.execute(false);

    }

    /**
     * Auto generated method signature
     * 
     * @see orca.nodeagent.NodeAgentService#registerKey
     * @param param238
     * 
     */
    public orca.nodeagent.documents.RegisterKeyResultElement registerKey(

            orca.nodeagent.documents.RegisterKeyElement param238) throws java.rmi.RemoteException

    {
        try {
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                    .createClient(_operations[9].getName());
            _operationClient.getOptions().setAction("registerKey");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            // Style is Doc.

            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param238,
                    optimizeContent(new javax.xml.namespace.QName("", "registerKey")));

            // adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
            _messageContext.setEnvelope(env);

            // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);

            // execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient
                    .getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();

            java.lang.Object object = fromOM(_returnEnv.getBody().getFirstElement(),
                    orca.nodeagent.documents.RegisterKeyResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (orca.nodeagent.documents.RegisterKeyResultElement) object;

        } catch (org.apache.axis2.AxisFault f) {
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt != null) {
                if (faultExeptionNameMap.containsKey(faultElt.getQName())) {
                    // make the fault by reflection
                    try {
                        java.lang.String exceptionClassName = (java.lang.String) faultExeptionClassNameMap
                                .get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex = (java.lang.Exception) exceptionClass.newInstance();
                        // message class
                        java.lang.String messageClassName = (java.lang.String) faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt, messageClass, null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[] { messageClass });
                        m.invoke(ex, new java.lang.Object[] { messageObject });

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    } catch (java.lang.ClassCastException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                } else {
                    throw f;
                }
            } else {
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
     * 
     * @see orca.nodeagent.NodeAgentService#startregisterKey
     * @param param238
     * 
     */
    public void startregisterKey(

            orca.nodeagent.documents.RegisterKeyElement param238,

            final orca.nodeagent.NodeAgentServiceCallbackHandler callback)

            throws java.rmi.RemoteException {

        org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                .createClient(_operations[9].getName());
        _operationClient.getOptions().setAction("registerKey");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env = null;

        // Style is Doc.

        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param238,
                optimizeContent(new javax.xml.namespace.QName("", "registerKey")));

        // adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
        _messageContext.setEnvelope(env);

        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        orca.nodeagent.documents.RegisterKeyResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultregisterKey((orca.nodeagent.documents.RegisterKeyResultElement) object);
            }

            public void onError(java.lang.Exception e) {
                callback.receiveErrorregisterKey(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if (_operations[9].getMessageReceiver() == null && _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[9].setMessageReceiver(_callbackReceiver);
        }

        // execute the operation client
        _operationClient.execute(false);

    }

    /**
     * Auto generated method signature
     * 
     * @see orca.nodeagent.NodeAgentService#installDriver
     * @param param240
     * 
     */
    public orca.nodeagent.documents.ResultElement installDriver(

            orca.nodeagent.documents.DriverElement param240) throws java.rmi.RemoteException

    {
        try {
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                    .createClient(_operations[10].getName());
            _operationClient.getOptions().setAction("installDriver");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            // Style is Doc.

            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param240,
                    optimizeContent(new javax.xml.namespace.QName("", "installDriver")));

            // adding SOAP headers
            _serviceClient.addHeadersToEnvelope(env);
            // create message context with that soap envelope
            org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
            _messageContext.setEnvelope(env);

            // add the message contxt to the operation client
            _operationClient.addMessageContext(_messageContext);

            // execute the operation client
            _operationClient.execute(true);

            org.apache.axis2.context.MessageContext _returnMessageContext = _operationClient
                    .getMessageContext(org.apache.axis2.wsdl.WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            org.apache.axiom.soap.SOAPEnvelope _returnEnv = _returnMessageContext.getEnvelope();

            java.lang.Object object = fromOM(_returnEnv.getBody().getFirstElement(),
                    orca.nodeagent.documents.ResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (orca.nodeagent.documents.ResultElement) object;

        } catch (org.apache.axis2.AxisFault f) {
            org.apache.axiom.om.OMElement faultElt = f.getDetail();
            if (faultElt != null) {
                if (faultExeptionNameMap.containsKey(faultElt.getQName())) {
                    // make the fault by reflection
                    try {
                        java.lang.String exceptionClassName = (java.lang.String) faultExeptionClassNameMap
                                .get(faultElt.getQName());
                        java.lang.Class exceptionClass = java.lang.Class.forName(exceptionClassName);
                        java.lang.Exception ex = (java.lang.Exception) exceptionClass.newInstance();
                        // message class
                        java.lang.String messageClassName = (java.lang.String) faultMessageMap.get(faultElt.getQName());
                        java.lang.Class messageClass = java.lang.Class.forName(messageClassName);
                        java.lang.Object messageObject = fromOM(faultElt, messageClass, null);
                        java.lang.reflect.Method m = exceptionClass.getMethod("setFaultMessage",
                                new java.lang.Class[] { messageClass });
                        m.invoke(ex, new java.lang.Object[] { messageObject });

                        throw new java.rmi.RemoteException(ex.getMessage(), ex);
                    } catch (java.lang.ClassCastException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.ClassNotFoundException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.NoSuchMethodException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.IllegalAccessException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    } catch (java.lang.InstantiationException e) {
                        // we cannot intantiate the class - throw the original Axis fault
                        throw f;
                    }
                } else {
                    throw f;
                }
            } else {
                throw f;
            }
        }
    }

    /**
     * Auto generated method signature for Asynchronous Invocations
     * 
     * @see orca.nodeagent.NodeAgentService#startinstallDriver
     * @param param240
     * 
     */
    public void startinstallDriver(

            orca.nodeagent.documents.DriverElement param240,

            final orca.nodeagent.NodeAgentServiceCallbackHandler callback)

            throws java.rmi.RemoteException {

        org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                .createClient(_operations[10].getName());
        _operationClient.getOptions().setAction("installDriver");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env = null;

        // Style is Doc.

        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param240,
                optimizeContent(new javax.xml.namespace.QName("", "installDriver")));

        // adding SOAP headers
        _serviceClient.addHeadersToEnvelope(env);
        // create message context with that soap envelope
        org.apache.axis2.context.MessageContext _messageContext = new org.apache.axis2.context.MessageContext();
        _messageContext.setEnvelope(env);

        // add the message contxt to the operation client
        _operationClient.addMessageContext(_messageContext);

        _operationClient.setCallback(new org.apache.axis2.client.async.Callback() {
            public void onComplete(org.apache.axis2.client.async.AsyncResult result) {
                java.lang.Object object = fromOM(result.getResponseEnvelope().getBody().getFirstElement(),
                        orca.nodeagent.documents.ResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultinstallDriver((orca.nodeagent.documents.ResultElement) object);
            }

            public void onError(java.lang.Exception e) {
                callback.receiveErrorinstallDriver(e);
            }
        });

        org.apache.axis2.util.CallbackReceiver _callbackReceiver = null;
        if (_operations[10].getMessageReceiver() == null && _operationClient.getOptions().isUseSeparateListener()) {
            _callbackReceiver = new org.apache.axis2.util.CallbackReceiver();
            _operations[10].setMessageReceiver(_callbackReceiver);
        }

        // execute the operation client
        _operationClient.execute(false);

    }

    /**
     * A utility method that copies the namepaces from the SOAPEnvelope
     */
    private java.util.Map getEnvelopeNamespaces(org.apache.axiom.soap.SOAPEnvelope env) {
        java.util.Map returnMap = new java.util.HashMap();
        java.util.Iterator namespaceIterator = env.getAllDeclaredNamespaces();
        while (namespaceIterator.hasNext()) {
            org.apache.axiom.om.OMNamespace ns = (org.apache.axiom.om.OMNamespace) namespaceIterator.next();
            returnMap.put(ns.getPrefix(), ns.getNamespaceURI());
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

    // http://localhost:8080/shirako/services/NodeAgentService
    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.DriverElement param, boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.DriverElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.ResultElement param, boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.ResultElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.ScriptElement param, boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.ScriptElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.ScriptResultElement param,
            boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.ScriptResultElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.TestFuncElement param,
            boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.TestFuncElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.TestFuncResultElement param,
            boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.TestFuncResultElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.DriverObjectRequestElement param,
            boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.DriverObjectRequestElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.UnregisterKeyElement param,
            boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.UnregisterKeyElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.UnregisterKeyResultElement param,
            boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.UnregisterKeyResultElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.DriverRequestElement param,
            boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.DriverRequestElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.RegisterAuthorityKeyElement param,
            boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.RegisterAuthorityKeyElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.RegisterAuthorityKeyResultElement param,
            boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.RegisterAuthorityKeyResultElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.GetServiceKeyElement param,
            boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.GetServiceKeyElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.GetServiceKeyResultElement param,
            boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.GetServiceKeyResultElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.RegisterKeyElement param,
            boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.RegisterKeyElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.nodeagent.documents.RegisterKeyResultElement param,
            boolean optimizeContent) {

        return param.getOMElement(orca.nodeagent.documents.RegisterKeyResultElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.DriverElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(orca.nodeagent.documents.DriverElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.ScriptElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(orca.nodeagent.documents.ScriptElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.TestFuncElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(orca.nodeagent.documents.TestFuncElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.DriverObjectRequestElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(orca.nodeagent.documents.DriverObjectRequestElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.UnregisterKeyElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(orca.nodeagent.documents.UnregisterKeyElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.DriverRequestElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(orca.nodeagent.documents.DriverRequestElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.RegisterAuthorityKeyElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(orca.nodeagent.documents.RegisterAuthorityKeyElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.GetServiceKeyElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(orca.nodeagent.documents.GetServiceKeyElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.RegisterKeyElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(orca.nodeagent.documents.RegisterKeyElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    /**
     * get the default envelope
     */
    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory) {
        return factory.getDefaultEnvelope();
    }

    private java.lang.Object fromOM(org.apache.axiom.om.OMElement param, java.lang.Class type,
            java.util.Map extraNamespaces) {

        try {

            if (orca.nodeagent.documents.DriverElement.class.equals(type)) {

                return orca.nodeagent.documents.DriverElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.ResultElement.class.equals(type)) {

                return orca.nodeagent.documents.ResultElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.ScriptElement.class.equals(type)) {

                return orca.nodeagent.documents.ScriptElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.ScriptResultElement.class.equals(type)) {

                return orca.nodeagent.documents.ScriptResultElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.TestFuncElement.class.equals(type)) {

                return orca.nodeagent.documents.TestFuncElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.TestFuncResultElement.class.equals(type)) {

                return orca.nodeagent.documents.TestFuncResultElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.DriverObjectRequestElement.class.equals(type)) {

                return orca.nodeagent.documents.DriverObjectRequestElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.ResultElement.class.equals(type)) {

                return orca.nodeagent.documents.ResultElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.UnregisterKeyElement.class.equals(type)) {

                return orca.nodeagent.documents.UnregisterKeyElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.UnregisterKeyResultElement.class.equals(type)) {

                return orca.nodeagent.documents.UnregisterKeyResultElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.DriverRequestElement.class.equals(type)) {

                return orca.nodeagent.documents.DriverRequestElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.ResultElement.class.equals(type)) {

                return orca.nodeagent.documents.ResultElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.DriverElement.class.equals(type)) {

                return orca.nodeagent.documents.DriverElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.ResultElement.class.equals(type)) {

                return orca.nodeagent.documents.ResultElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.RegisterAuthorityKeyElement.class.equals(type)) {

                return orca.nodeagent.documents.RegisterAuthorityKeyElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.RegisterAuthorityKeyResultElement.class.equals(type)) {

                return orca.nodeagent.documents.RegisterAuthorityKeyResultElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.GetServiceKeyElement.class.equals(type)) {

                return orca.nodeagent.documents.GetServiceKeyElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.GetServiceKeyResultElement.class.equals(type)) {

                return orca.nodeagent.documents.GetServiceKeyResultElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.RegisterKeyElement.class.equals(type)) {

                return orca.nodeagent.documents.RegisterKeyElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.RegisterKeyResultElement.class.equals(type)) {

                return orca.nodeagent.documents.RegisterKeyResultElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.DriverElement.class.equals(type)) {

                return orca.nodeagent.documents.DriverElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.nodeagent.documents.ResultElement.class.equals(type)) {

                return orca.nodeagent.documents.ResultElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private void setOpNameArray() {
        opNameArray = null;
    }

}
