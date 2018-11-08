
/**
* IMAGEPROXYMessageReceiverInOut.java
*
* This file was auto-generated from WSDL
* by the Apache Axis2 version: 1.1.1 Jan 09, 2007 (06:20:51 LKT)
*/
package orca.handlers.ec2.tasks.imgproxy;

/**
 * IMAGEPROXYMessageReceiverInOut message receiver
 */

public class IMAGEPROXYMessageReceiverInOut extends org.apache.axis2.receivers.AbstractInOutSyncMessageReceiver {

    public void invokeBusinessLogic(org.apache.axis2.context.MessageContext msgContext,
            org.apache.axis2.context.MessageContext newMsgContext) throws org.apache.axis2.AxisFault {

        try {

            // get the implementation class for the Web Service
            Object obj = getTheImplementationObject(msgContext);

            IMAGEPROXYSkeleton skel = (IMAGEPROXYSkeleton) obj;
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

                if ("RegisterImage".equals(methodName)) {

                    orca.handlers.ec2.tasks.imgproxy.RegisterImageResponse param5 = null;

                    // doc style
                    orca.handlers.ec2.tasks.imgproxy.RegisterImage wrappedParam = (orca.handlers.ec2.tasks.imgproxy.RegisterImage) fromOM(
                            msgContext.getEnvelope().getBody().getFirstElement(),
                            orca.handlers.ec2.tasks.imgproxy.RegisterImage.class,
                            getEnvelopeNamespaces(msgContext.getEnvelope()));

                    param5 = skel.RegisterImage(wrappedParam);

                    envelope = toEnvelope(getSOAPFactory(msgContext), param5, false);

                }

                newMsgContext.setEnvelope(envelope);
            }
        } catch (ExceptionException e) {

            org.apache.axis2.AxisFault f = createAxisFault(e);

            f.setDetail(toOM(e.getFaultMessage(), false));

            throw f;
        }

        catch (java.lang.Exception e) {
            throw org.apache.axis2.AxisFault.makeFault(e);
        }
    }

    //
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
            orca.handlers.ec2.tasks.imgproxy.RegisterImageResponse param, boolean optimizeContent) {
        org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();

        emptyEnvelope.getBody()
                .addChild(param.getOMElement(orca.handlers.ec2.tasks.imgproxy.RegisterImageResponse.MY_QNAME, factory));

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
