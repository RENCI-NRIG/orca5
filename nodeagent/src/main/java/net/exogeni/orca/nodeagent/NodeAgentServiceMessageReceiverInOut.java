
/**
* NodeAgentServiceMessageReceiverInOut.java
*
* This file was auto-generated from WSDL
* by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
*/
package net.exogeni.orca.nodeagent;

import net.exogeni.orca.nodeagent.documents.*;

/**
 * NodeAgentServiceMessageReceiverInOut message receiver
 */

public class NodeAgentServiceMessageReceiverInOut extends org.apache.axis2.receivers.AbstractInOutSyncMessageReceiver {

    public void invokeBusinessLogic(org.apache.axis2.context.MessageContext msgContext,
            org.apache.axis2.context.MessageContext newMsgContext) throws org.apache.axis2.AxisFault {

        try {

            // get the implementation class for the Web Service
            Object obj = getTheImplementationObject(msgContext);

            NodeAgentServiceSkeleton skel = (NodeAgentServiceSkeleton) obj;
            // Out Envelop
            org.apache.axiom.soap.SOAPEnvelope envelope = null;
            // Find the axisOperation that has been set by the Dispatch phase.
            org.apache.axis2.description.AxisOperation op = msgContext.getOperationContext().getAxisOperation();
            if (op == null) {
                throw new org.apache.axis2.AxisFault(
                        "Operation is not located, if this is doclit style the SOAP-ACTION should specified via the SOAP Action to use the RawXMLProvider");
            }

            java.lang.String methodName;
            if (op.getName() != null & (methodName = org.apache.axis2.util.JavaUtils
                    .xmlNameToJava(op.getName().getLocalPart())) != null) {

                if ("uninstallDriver".equals(methodName)) {

                    ResultElement param45 = null;

                    // doc style
                    DriverElement wrappedParam = (DriverElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            DriverElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param45 = skel.uninstallDriver(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param45, false);

                }

                if ("executeScript".equals(methodName)) {

                    ScriptResultElement param47 = null;

                    // doc style
                    ScriptElement wrappedParam = (ScriptElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            ScriptElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param47 = skel.executeScript(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param47, false);

                }

                if ("testFunc".equals(methodName)) {

                    TestFuncResultElement param49 = null;

                    // doc style
                    TestFuncElement wrappedParam = (TestFuncElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            TestFuncElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param49 = skel.testFunc(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param49, false);

                }

                if ("executeObjectDriver".equals(methodName)) {

                    ResultElement param51 = null;

                    // doc style
                    DriverObjectRequestElement wrappedParam = (DriverObjectRequestElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            DriverObjectRequestElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param51 = skel.executeObjectDriver(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param51, false);

                }

                if ("unregisterKey".equals(methodName)) {

                    UnregisterKeyResultElement param53 = null;

                    // doc style
                    UnregisterKeyElement wrappedParam = (UnregisterKeyElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            UnregisterKeyElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param53 = skel.unregisterKey(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param53, false);

                }

                if ("executeDriver".equals(methodName)) {

                    ResultElement param55 = null;

                    // doc style
                    DriverRequestElement wrappedParam = (DriverRequestElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            DriverRequestElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param55 = skel.executeDriver(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param55, false);

                }

                if ("upgradeDriver".equals(methodName)) {

                    ResultElement param57 = null;

                    // doc style
                    DriverElement wrappedParam = (DriverElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            DriverElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param57 = skel.upgradeDriver(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param57, false);

                }

                if ("registerAuthorityKey".equals(methodName)) {

                    RegisterAuthorityKeyResultElement param59 = null;

                    // doc style
                    RegisterAuthorityKeyElement wrappedParam = (RegisterAuthorityKeyElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            RegisterAuthorityKeyElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param59 = skel.registerAuthorityKey(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param59, false);

                }

                if ("getServiceKey".equals(methodName)) {

                    GetServiceKeyResultElement param61 = null;

                    // doc style
                    GetServiceKeyElement wrappedParam = (GetServiceKeyElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            GetServiceKeyElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param61 = skel.getServiceKey(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param61, false);

                }

                if ("registerKey".equals(methodName)) {

                    RegisterKeyResultElement param63 = null;

                    // doc style
                    RegisterKeyElement wrappedParam = (RegisterKeyElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            RegisterKeyElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param63 = skel.registerKey(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param63, false);

                }

                if ("installDriver".equals(methodName)) {

                    ResultElement param65 = null;

                    // doc style
                    DriverElement wrappedParam = (DriverElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            DriverElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param65 = skel.installDriver(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param65, false);

                }

                newMsgContext.setEnvelope(envelope);
            }
        } catch (Exception e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    //
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
                                                          ResultElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.ResultElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          ScriptResultElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.ScriptResultElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          TestFuncResultElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.TestFuncResultElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          UnregisterKeyResultElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.UnregisterKeyResultElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          RegisterAuthorityKeyResultElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(
                param.getOMElement(net.exogeni.orca.nodeagent.documents.RegisterAuthorityKeyResultElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          GetServiceKeyResultElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.GetServiceKeyResultElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
                                                          RegisterKeyResultElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(net.exogeni.orca.nodeagent.documents.RegisterKeyResultElement.MY_QNAME, factory));

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

}// end of class
