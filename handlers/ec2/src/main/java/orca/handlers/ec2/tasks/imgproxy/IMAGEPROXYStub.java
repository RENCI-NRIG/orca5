
/**
* IMAGEPROXYStub.java
*
* This file was auto-generated from WSDL
* by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
*/
package orca.handlers.ec2.tasks.imgproxy;

import java.util.UUID;

/*
*  IMAGEPROXYStub java implementation
*/

public class IMAGEPROXYStub extends org.apache.axis2.client.Stub {
    protected org.apache.axis2.description.AxisOperation[] _operations;

    // hashmaps to keep the fault mapping
    private java.util.HashMap faultExeptionNameMap = new java.util.HashMap();
    private java.util.HashMap faultExeptionClassNameMap = new java.util.HashMap();
    private java.util.HashMap faultMessageMap = new java.util.HashMap();

    private void populateAxisService() throws org.apache.axis2.AxisFault {

        // creating the Service with a unique name
        // hashCode creates collisions because Axis does not properly clean things up
        // see https://issues.apache.org/jira/browse/AXIS2-1182 07/26/12 /ib

        // _service = new org.apache.axis2.description.AxisService("IMAGEPROXY" + this.hashCode());
        _service = new org.apache.axis2.description.AxisService("IMAGEPROXY" + UUID.randomUUID().toString());

        // creating the operations
        org.apache.axis2.description.AxisOperation __operation;

        _operations = new org.apache.axis2.description.AxisOperation[1];

        __operation = new org.apache.axis2.description.OutInAxisOperation();

        __operation.setName(new javax.xml.namespace.QName("", "RegisterImage"));
        _service.addOperation(__operation);

        _operations[0] = __operation;

    }

    // populates the faults
    private void populateFaults() {

        faultExeptionNameMap.put(new javax.xml.namespace.QName("http://imageproxy.orca", "Exception"),
                "orca.handlers.ec2.tasks.imgproxy.ExceptionException");
        faultExeptionClassNameMap.put(new javax.xml.namespace.QName("http://imageproxy.orca", "Exception"),
                "orca.handlers.ec2.tasks.imgproxy.ExceptionException");
        faultMessageMap.put(new javax.xml.namespace.QName("http://imageproxy.orca", "Exception"),
                "orca.handlers.ec2.tasks.imgproxy.Exception0");

    }

    /**
     * Constructor that takes in a configContext
     * @param configurationContext configurationContext
     * @param targetEndpoint targetEndpoint
     * @throws org.apache.axis2.AxisFault in case of error
     */
    public IMAGEPROXYStub(org.apache.axis2.context.ConfigurationContext configurationContext,
            java.lang.String targetEndpoint) throws org.apache.axis2.AxisFault {
        // To populate AxisService
        populateAxisService();
        populateFaults();

        _serviceClient = new org.apache.axis2.client.ServiceClient(configurationContext, _service);

        configurationContext = _serviceClient.getServiceContext().getConfigurationContext();

        _serviceClient.getOptions().setTo(new org.apache.axis2.addressing.EndpointReference(targetEndpoint));

    }

    /**
     * Default Constructor
     * @throws org.apache.axis2.AxisFault in case of error
     */
    public IMAGEPROXYStub() throws org.apache.axis2.AxisFault {

        this("http://localhost:8080/axis2/services/IMAGEPROXY.IMAGEPROXYHttpSoap11Endpoint/");

    }

    /**
     * Constructor taking the target endpoint
     * @param targetEndpoint targetEndpoint
     * @throws org.apache.axis2.AxisFault in case of error
     */
    public IMAGEPROXYStub(java.lang.String targetEndpoint) throws org.apache.axis2.AxisFault {
        this(null, targetEndpoint);
    }

    /**
     * Auto generated method signature
     * 
     * @param param18 param18
     * @throws java.rmi.RemoteException in case of error
     * @throws orca.handlers.ec2.tasks.imgproxy.ExceptionException in case of error
     * @return orca.handlers.ec2.tasks.imgproxy.RegisterImageResponse
     * 
     */
    public orca.handlers.ec2.tasks.imgproxy.RegisterImageResponse RegisterImage(

            orca.handlers.ec2.tasks.imgproxy.RegisterImage param18) throws java.rmi.RemoteException

            , orca.handlers.ec2.tasks.imgproxy.ExceptionException {
        try {
            org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                    .createClient(_operations[0].getName());
            _operationClient.getOptions().setAction("urn:RegisterImage");
            _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

            // create SOAP envelope with that payload
            org.apache.axiom.soap.SOAPEnvelope env = null;

            // Style is Doc.

            env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param18,
                    optimizeContent(new javax.xml.namespace.QName("", "RegisterImage")));

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
                    orca.handlers.ec2.tasks.imgproxy.RegisterImageResponse.class, getEnvelopeNamespaces(_returnEnv));
            _messageContext.getTransportOut().getSender().cleanup(_messageContext);
            return (orca.handlers.ec2.tasks.imgproxy.RegisterImageResponse) object;

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

                        if (ex instanceof orca.handlers.ec2.tasks.imgproxy.ExceptionException) {
                            throw (orca.handlers.ec2.tasks.imgproxy.ExceptionException) ex;
                        }

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
     * @param param18 param18
     * @param callback callback
     * @throws java.rmi.RemoteException in case of error
     * 
     */
    public void startRegisterImage(

            orca.handlers.ec2.tasks.imgproxy.RegisterImage param18,

            final orca.handlers.ec2.tasks.imgproxy.IMAGEPROXYCallbackHandler callback)

            throws java.rmi.RemoteException {

        org.apache.axis2.client.OperationClient _operationClient = _serviceClient
                .createClient(_operations[0].getName());
        _operationClient.getOptions().setAction("urn:RegisterImage");
        _operationClient.getOptions().setExceptionToBeThrownOnSOAPFault(true);

        // create SOAP envelope with that payload
        org.apache.axiom.soap.SOAPEnvelope env = null;

        // Style is Doc.

        env = toEnvelope(getFactory(_operationClient.getOptions().getSoapVersionURI()), param18,
                optimizeContent(new javax.xml.namespace.QName("", "RegisterImage")));

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
                        orca.handlers.ec2.tasks.imgproxy.RegisterImageResponse.class,
                        getEnvelopeNamespaces(result.getResponseEnvelope()));
                callback.receiveResultRegisterImage((orca.handlers.ec2.tasks.imgproxy.RegisterImageResponse) object);
            }

            public void onError(java.lang.Exception e) {
                callback.receiveErrorRegisterImage(e);
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

    // http://localhost:8080/axis2/services/IMAGEPROXY.IMAGEPROXYHttpSoap11Endpoint/
    private org.apache.axiom.om.OMElement toOM(orca.handlers.ec2.tasks.imgproxy.RegisterImage param,
            boolean optimizeContent) {

        return param.getOMElement(orca.handlers.ec2.tasks.imgproxy.RegisterImage.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.handlers.ec2.tasks.imgproxy.RegisterImageResponse param,
            boolean optimizeContent) {

        return param.getOMElement(orca.handlers.ec2.tasks.imgproxy.RegisterImageResponse.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.om.OMElement toOM(orca.handlers.ec2.tasks.imgproxy.Exception0 param,
            boolean optimizeContent) {

        return param.getOMElement(orca.handlers.ec2.tasks.imgproxy.Exception0.MY_QNAME,
                org.apache.axiom.om.OMAbstractFactory.getOMFactory());

    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.handlers.ec2.tasks.imgproxy.RegisterImage param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(orca.handlers.ec2.tasks.imgproxy.RegisterImage.MY_QNAME, factory));

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

            if (orca.handlers.ec2.tasks.imgproxy.RegisterImage.class.equals(type)) {

                return orca.handlers.ec2.tasks.imgproxy.RegisterImage.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.handlers.ec2.tasks.imgproxy.RegisterImageResponse.class.equals(type)) {

                return orca.handlers.ec2.tasks.imgproxy.RegisterImageResponse.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

            if (orca.handlers.ec2.tasks.imgproxy.Exception0.class.equals(type)) {

                return orca.handlers.ec2.tasks.imgproxy.Exception0.Factory
                        .parse(param.getXMLStreamReaderWithoutCaching());

            }

        } catch (java.lang.Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private void setOpNameArray() {
        opNameArray = null;
    }

}
