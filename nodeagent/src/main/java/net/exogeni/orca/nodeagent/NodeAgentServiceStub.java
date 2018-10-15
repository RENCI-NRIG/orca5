
/**
* NodeAgentServiceStub.java
*
* This file was auto-generated from WSDL
* by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
*/
package net.exogeni.orca.nodeagent;

/*
*  NodeAgentServiceStub java implementation
*/

import net.exogeni.orca.nodeagent.documents.*;

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
     * @param configurationContext configurationContext
     * @param targetEndpoint targetEndpoint
     * @throws org.apache.axis2.AxisFault in case of error
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
     * @throws org.apache.axis2.AxisFault in case of error
     */
    public NodeAgentServiceStub() throws org.apache.axis2.AxisFault {

        this("http://localhost:8080/shirako/services/NodeAgentService");

    }

    /**
     * Constructor taking the target endpoint
     * @param targetEndpoint targetEndpoint
     * @throws org.apache.axis2.AxisFault in case of error
     */
    public NodeAgentServiceStub(java.lang.String targetEndpoint) throws org.apache.axis2.AxisFault {
        this(null, targetEndpoint);
    }

    /**
     * Auto generated method signature
     * 
     * @param param220 param220
     * @return net.exogeni.orca.nodeagent.documents.ResultElement
     * @throws java.rmi.RemoteException in case of error
     * 
     */
    public ResultElement uninstallDriver(

            DriverElement param220) throws java.rmi.RemoteException

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
                    ResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (ResultElement) object;

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
     * @param param220 param220
     * @param callback callback  
     * @throws java.rmi.RemoteException in case of error
     * 
     */
    public void startuninstallDriver(

            DriverElement param220,

            final NodeAgentServiceCallbackHandler callback)

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
                        ResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultuninstallDriver((ResultElement) object);
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
      * @param param222 param222
      * @return net.exogeni.orca.nodeagent.documents.ScriptResultElement
      * @throws java.rmi.RemoteException in case of error
     * 
     */
    public ScriptResultElement executeScript(

            ScriptElement param222) throws java.rmi.RemoteException

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
                    ScriptResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (ScriptResultElement) object;

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
      * @param param222 param222
      * @param callback callback
      * @throws java.rmi.RemoteException in case of error     
     */
    public void startexecuteScript(

            ScriptElement param222,

            final NodeAgentServiceCallbackHandler callback)

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
                        ScriptResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultexecuteScript((ScriptResultElement) object);
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
      * @param param224 param224
      * @return net.exogeni.orca.nodeagent.documents.TestFuncResultElement
      * @throws java.rmi.RemoteException in case of error
     */
    public TestFuncResultElement testFunc(

            TestFuncElement param224) throws java.rmi.RemoteException

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
                    TestFuncResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (TestFuncResultElement) object;

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
      * @param param224 param224
      * @param callback callback
      * @throws java.rmi.RemoteException in case of error
     */
    public void starttestFunc(

            TestFuncElement param224,

            final NodeAgentServiceCallbackHandler callback)

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
                        TestFuncResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResulttestFunc((TestFuncResultElement) object);
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
      * @param param226 param226
      * @return orca.nodeagent.documents.ResultElement
      * @throws java.rmi.RemoteException in case of error
     */
    public ResultElement executeObjectDriver(

            DriverObjectRequestElement param226) throws java.rmi.RemoteException

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
                    ResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (ResultElement) object;

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
      * @param param226 param226
      * @param callback callback
      * @throws java.rmi.RemoteException in case of error
     * 
     */
    public void startexecuteObjectDriver(

            DriverObjectRequestElement param226,

            final NodeAgentServiceCallbackHandler callback)

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
                        ResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultexecuteObjectDriver((ResultElement) object);
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
      * @param param228 param228
      * @return orca.nodeagent.documents.UnregisterKeyResultElement
      * @throws java.rmi.RemoteException in case of error
     * 
     */
    public UnregisterKeyResultElement unregisterKey(

            UnregisterKeyElement param228) throws java.rmi.RemoteException

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
                    UnregisterKeyResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (UnregisterKeyResultElement) object;

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
      * @param param228 param228
      * @param callback callback
      * @throws java.rmi.RemoteException in case of error
     * 
     */
    public void startunregisterKey(

            UnregisterKeyElement param228,

            final NodeAgentServiceCallbackHandler callback)

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
                        UnregisterKeyResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultunregisterKey((UnregisterKeyResultElement) object);
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
      * @param param230 param230
      * @return orca.nodeagent.documents.ResultElement
      * @throws java.rmi.RemoteException in case of error
     * 
     */
    public ResultElement executeDriver(

            DriverRequestElement param230) throws java.rmi.RemoteException

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
                    ResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (ResultElement) object;

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
      * @param param230 param230
      * @param callback callback
      * @throws java.rmi.RemoteException in case of error
     */
    public void startexecuteDriver(

            DriverRequestElement param230,

            final NodeAgentServiceCallbackHandler callback)

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
                        ResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultexecuteDriver((ResultElement) object);
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
      * @param param232 param232
      * @return orca.nodeagent.documents.ResultElement
      * @throws java.rmi.RemoteException in case of error
     */
    public ResultElement upgradeDriver(

            DriverElement param232) throws java.rmi.RemoteException

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
                    ResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (ResultElement) object;

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
      * @param param232 param232
      * @param callback callback
      * @throws java.rmi.RemoteException in case of error
     */
    public void startupgradeDriver(

            DriverElement param232,

            final NodeAgentServiceCallbackHandler callback)

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
                        ResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultupgradeDriver((ResultElement) object);
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
      * @param param234 param234
      * @return orca.nodeagent.documents.RegisterAuthorityKeyResultElement
      * @throws java.rmi.RemoteException in case of error
     */
    public RegisterAuthorityKeyResultElement registerAuthorityKey(

            RegisterAuthorityKeyElement param234) throws java.rmi.RemoteException

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
                    RegisterAuthorityKeyResultElement.class,
                    getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (RegisterAuthorityKeyResultElement) object;

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
      * @param param234 param234
      * @param callback callback
      * @throws java.rmi.RemoteException in case of error
     */
    public void startregisterAuthorityKey(

            RegisterAuthorityKeyElement param234,

            final NodeAgentServiceCallbackHandler callback)

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
                        RegisterAuthorityKeyResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultregisterAuthorityKey(
                        (RegisterAuthorityKeyResultElement) object);
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
      * @param param236 param236
      * @return orca.nodeagent.documents.GetServiceKeyResultElement
      * @throws java.rmi.RemoteException in case of error
     */
    public GetServiceKeyResultElement getServiceKey(

            GetServiceKeyElement param236) throws java.rmi.RemoteException

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
                    GetServiceKeyResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (GetServiceKeyResultElement) object;

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
      * @param param236 param236
      * @param callback callback
      * @throws java.rmi.RemoteException in case of error
     * 
     */
    public void startgetServiceKey(

            GetServiceKeyElement param236,

            final NodeAgentServiceCallbackHandler callback)

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
                        GetServiceKeyResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultgetServiceKey((GetServiceKeyResultElement) object);
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
      * @param param238 param238
      * @return orca.nodeagent.documents.RegisterKeyResultElement
      * @throws java.rmi.RemoteException in case of error
     * 
     */
    public RegisterKeyResultElement registerKey(

            RegisterKeyElement param238) throws java.rmi.RemoteException

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
                    RegisterKeyResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (RegisterKeyResultElement) object;

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
      * @param param238 param238
      * @param callback callback
      * @throws java.rmi.RemoteException in case of error
     */
    public void startregisterKey(

            RegisterKeyElement param238,

            final NodeAgentServiceCallbackHandler callback)

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
                        RegisterKeyResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultregisterKey((RegisterKeyResultElement) object);
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
      * @param param240 param240
      * @return orca.nodeagent.documents.ResultElement
      * @throws java.rmi.RemoteException in case of error
     */
    public ResultElement installDriver(

            DriverElement param240) throws java.rmi.RemoteException

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
                    ResultElement.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (ResultElement) object;

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
      * @param param240 param240
      * @param callback callback
      * @throws java.rmi.RemoteException in case of error
     */
    public void startinstallDriver(

            DriverElement param240,

            final NodeAgentServiceCallbackHandler callback)

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
                        ResultElement.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultinstallDriver((ResultElement) object);
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
     * @param env env
     * @return Map
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
    private org.apache.axiom.om.OMElement toOM(DriverElement param, boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.DriverElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(ResultElement param, boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.ResultElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(ScriptElement param, boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.ScriptElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(ScriptResultElement param,
            boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.ScriptResultElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(TestFuncElement param,
            boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.TestFuncElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(TestFuncResultElement param,
            boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.TestFuncResultElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(DriverObjectRequestElement param,
            boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.DriverObjectRequestElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(UnregisterKeyElement param,
            boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.UnregisterKeyElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(UnregisterKeyResultElement param,
            boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.UnregisterKeyResultElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(DriverRequestElement param,
            boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.DriverRequestElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(RegisterAuthorityKeyElement param,
            boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.RegisterAuthorityKeyElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(RegisterAuthorityKeyResultElement param,
            boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.RegisterAuthorityKeyResultElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(GetServiceKeyElement param,
            boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.GetServiceKeyElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(GetServiceKeyResultElement param,
            boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.GetServiceKeyResultElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(RegisterKeyElement param,
            boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.RegisterKeyElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(RegisterKeyResultElement param,
            boolean optimizeContent) {

        return param.getOMElement(net.exogeni.orca.nodeagent.documents.RegisterKeyResultElement.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          DriverElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.DriverElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          ScriptElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.ScriptElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          TestFuncElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.TestFuncElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          DriverObjectRequestElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.DriverObjectRequestElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          UnregisterKeyElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.UnregisterKeyElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          DriverRequestElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.DriverRequestElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          RegisterAuthorityKeyElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.RegisterAuthorityKeyElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          GetServiceKeyElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.GetServiceKeyElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          RegisterKeyElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.RegisterKeyElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    /**
     * get the default envelope
     * @param factory factory
     * @return SOAPEnvelope
     */
    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory) {
        return factory.getDefaultEnvelope();
    }

    private java.lang.Object fromOM(org.apache.axiom.om.OMElement param, java.lang.Class type,
            java.util.Map extraNamespaces) {

        try {

            if (DriverElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.DriverElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (ResultElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.ResultElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (ScriptElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.ScriptElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (ScriptResultElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.ScriptResultElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (TestFuncElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.TestFuncElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (TestFuncResultElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.TestFuncResultElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (DriverObjectRequestElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.DriverObjectRequestElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (ResultElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.ResultElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (UnregisterKeyElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.UnregisterKeyElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (UnregisterKeyResultElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.UnregisterKeyResultElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (DriverRequestElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.DriverRequestElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (ResultElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.ResultElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (DriverElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.DriverElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (ResultElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.ResultElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (RegisterAuthorityKeyElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.RegisterAuthorityKeyElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (RegisterAuthorityKeyResultElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.RegisterAuthorityKeyResultElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (GetServiceKeyElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.GetServiceKeyElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (GetServiceKeyResultElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.GetServiceKeyResultElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (RegisterKeyElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.RegisterKeyElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (RegisterKeyResultElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.RegisterKeyResultElement.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (DriverElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.DriverElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (ResultElement.class.equals(type)) {

                return net.exogeni.orca.nodeagent.documents.ResultElement.Factory.parse(param.getXMLStreamReaderWithoutCaching());

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
