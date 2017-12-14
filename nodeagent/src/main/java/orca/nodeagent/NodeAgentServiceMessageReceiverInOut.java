
/**
* NodeAgentServiceMessageReceiverInOut.java
*
* This file was auto-generated from WSDL
* by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
*/
package orca.nodeagent;

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

                    orca.nodeagent.documents.ResultElement param45 = null;

                    // doc style
                    orca.nodeagent.documents.DriverElement wrappedParam = (orca.nodeagent.documents.DriverElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            orca.nodeagent.documents.DriverElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param45 = skel.uninstallDriver(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param45, false);

                }

                if ("executeScript".equals(methodName)) {

                    orca.nodeagent.documents.ScriptResultElement param47 = null;

                    // doc style
                    orca.nodeagent.documents.ScriptElement wrappedParam = (orca.nodeagent.documents.ScriptElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            orca.nodeagent.documents.ScriptElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param47 = skel.executeScript(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param47, false);

                }

                if ("testFunc".equals(methodName)) {

                    orca.nodeagent.documents.TestFuncResultElement param49 = null;

                    // doc style
                    orca.nodeagent.documents.TestFuncElement wrappedParam = (orca.nodeagent.documents.TestFuncElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            orca.nodeagent.documents.TestFuncElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param49 = skel.testFunc(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param49, false);

                }

                if ("executeObjectDriver".equals(methodName)) {

                    orca.nodeagent.documents.ResultElement param51 = null;

                    // doc style
                    orca.nodeagent.documents.DriverObjectRequestElement wrappedParam = (orca.nodeagent.documents.DriverObjectRequestElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            orca.nodeagent.documents.DriverObjectRequestElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param51 = skel.executeObjectDriver(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param51, false);

                }

                if ("unregisterKey".equals(methodName)) {

                    orca.nodeagent.documents.UnregisterKeyResultElement param53 = null;

                    // doc style
                    orca.nodeagent.documents.UnregisterKeyElement wrappedParam = (orca.nodeagent.documents.UnregisterKeyElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            orca.nodeagent.documents.UnregisterKeyElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param53 = skel.unregisterKey(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param53, false);

                }

                if ("executeDriver".equals(methodName)) {

                    orca.nodeagent.documents.ResultElement param55 = null;

                    // doc style
                    orca.nodeagent.documents.DriverRequestElement wrappedParam = (orca.nodeagent.documents.DriverRequestElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            orca.nodeagent.documents.DriverRequestElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param55 = skel.executeDriver(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param55, false);

                }

                if ("upgradeDriver".equals(methodName)) {

                    orca.nodeagent.documents.ResultElement param57 = null;

                    // doc style
                    orca.nodeagent.documents.DriverElement wrappedParam = (orca.nodeagent.documents.DriverElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            orca.nodeagent.documents.DriverElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param57 = skel.upgradeDriver(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param57, false);

                }

                if ("registerAuthorityKey".equals(methodName)) {

                    orca.nodeagent.documents.RegisterAuthorityKeyResultElement param59 = null;

                    // doc style
                    orca.nodeagent.documents.RegisterAuthorityKeyElement wrappedParam = (orca.nodeagent.documents.RegisterAuthorityKeyElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            orca.nodeagent.documents.RegisterAuthorityKeyElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param59 = skel.registerAuthorityKey(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param59, false);

                }

                if ("getServiceKey".equals(methodName)) {

                    orca.nodeagent.documents.GetServiceKeyResultElement param61 = null;

                    // doc style
                    orca.nodeagent.documents.GetServiceKeyElement wrappedParam = (orca.nodeagent.documents.GetServiceKeyElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            orca.nodeagent.documents.GetServiceKeyElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param61 = skel.getServiceKey(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param61, false);

                }

                if ("registerKey".equals(methodName)) {

                    orca.nodeagent.documents.RegisterKeyResultElement param63 = null;

                    // doc style
                    orca.nodeagent.documents.RegisterKeyElement wrappedParam = (orca.nodeagent.documents.RegisterKeyElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            orca.nodeagent.documents.RegisterKeyElement.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param63 = skel.registerKey(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param63, false);

                }

                if ("installDriver".equals(methodName)) {

                    orca.nodeagent.documents.ResultElement param65 = null;

                    // doc style
                    orca.nodeagent.documents.DriverElement wrappedParam = (orca.nodeagent.documents.DriverElement) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            orca.nodeagent.documents.DriverElement.class,
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
            orca.nodeagent.documents.ResultElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(param.getOMElement(orca.nodeagent.documents.ResultElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.ScriptResultElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(orca.nodeagent.documents.ScriptResultElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.TestFuncResultElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(orca.nodeagent.documents.TestFuncResultElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.UnregisterKeyResultElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(orca.nodeagent.documents.UnregisterKeyResultElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.RegisterAuthorityKeyResultElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody().addChild(
                param.getOMElement(orca.nodeagent.documents.RegisterAuthorityKeyResultElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.GetServiceKeyResultElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(orca.nodeagent.documents.GetServiceKeyResultElement.MY_QNAME, factory));

        return emptyEnvelope;
    }

    private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory,
            orca.nodeagent.documents.RegisterKeyResultElement param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(orca.nodeagent.documents.RegisterKeyResultElement.MY_QNAME, factory));

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
