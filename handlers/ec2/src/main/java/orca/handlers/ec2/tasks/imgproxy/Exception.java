
/**
* Exception.java
*
* This file was auto-generated from WSDL
* by the Apache Axis2 version: #axisVersion# #today#
*/

package orca.handlers.ec2.tasks.imgproxy;

/**
 * Exception bean class
 */

public class Exception implements org.apache.axis2.databinding.ADBBean {
    /*
     * This type was generated from the piece of schema that had name = Exception Namespace URI = http://imageproxy.orca
     * Namespace Prefix = ns1
     */

    /**
     * field for Exception
     */

    protected org.apache.axiom.om.OMElement localException;

    /*
     * This tracker boolean wil be used to detect whether the user called the set method for this attribute. It will be
     * used to determine whether to include this field in the serialized XML
     */
    protected boolean localExceptionTracker = false;

    /**
     * Auto generated getter method
     * 
     * @return org.apache.axiom.om.OMElement
     */
    public org.apache.axiom.om.OMElement getException() {
        return localException;
    }

    /**
     * Auto generated setter method
     * 
     * @param param
     *            Exception
     */
    public void setException(org.apache.axiom.om.OMElement param) {

        if (param != null) {
            // update the setting tracker
            localExceptionTracker = true;
        } else {
            localExceptionTracker = true;

        }

        this.localException = param;

    }

    /**
     * isReaderMTOMAware
     * @param reader reader
     * @return true if the reader supports MTOM
     */
    public static boolean isReaderMTOMAware(javax.xml.stream.XMLStreamReader reader) {
        boolean isReaderMTOMAware = false;

        try {
            isReaderMTOMAware = java.lang.Boolean.TRUE
                .equals(reader.getProperty(org.apache.axiom.om.OMConstants.IS_DATA_HANDLERS_AWARE));
        } catch (java.lang.IllegalArgumentException e) {
            isReaderMTOMAware = false;
        }
        return isReaderMTOMAware;
    }

    /**
     * @param parentQName parentQName
     * @param factory factory
     * @return org.apache.axiom.om.OMElement
     */
    public org.apache.axiom.om.OMElement getOMElement(final javax.xml.namespace.QName parentQName,
            final org.apache.axiom.om.OMFactory factory) {

        org.apache.axiom.om.OMDataSource dataSource = getOMDataSource(parentQName, factory);

        return new org.apache.axiom.om.impl.llom.OMSourcedElementImpl(parentQName, factory, dataSource);

    }

    /**
     * @param parentQName parentQName
     * @param factory factory
     * @return org.apache.axiom.om.OMElement
     */
    public org.apache.axiom.om.OMDataSource getOMDataSource(final javax.xml.namespace.QName parentQName,
            final org.apache.axiom.om.OMFactory factory) {

        org.apache.axiom.om.OMDataSource dataSource = new org.apache.axis2.databinding.ADBDataSource(this,
                parentQName) {

            public void serialize(javax.xml.stream.XMLStreamWriter xmlWriter)
                    throws javax.xml.stream.XMLStreamException {

                    java.lang.String prefix = parentQName.getPrefix();
                    java.lang.String namespace = parentQName.getNamespaceURI();

                    if (namespace != null) {
                        java.lang.String writerPrefix = xmlWriter.getPrefix(namespace);
                        if (writerPrefix != null) {
                            xmlWriter.writeStartElement(namespace, parentQName.getLocalPart());
                        } else {
                            if (prefix == null) {
                                prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
                            }

                            xmlWriter.writeStartElement(prefix, parentQName.getLocalPart(), namespace);
                            xmlWriter.writeNamespace(prefix, namespace);
                            xmlWriter.setPrefix(prefix, namespace);
                        }
                    } else {
                        xmlWriter.writeStartElement(parentQName.getLocalPart());
                    }

                    if (localExceptionTracker) {

                        if (localException != null) {
                            // write null attribute
                            java.lang.String namespace2 = "http://imageproxy.orca";
                            if (!namespace2.equals("")) {
                                java.lang.String prefix2 = xmlWriter.getPrefix(namespace2);

                                if (prefix2 == null) {
                                    prefix2 = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                                    xmlWriter.writeStartElement(prefix2, "Exception", namespace2);
                                    xmlWriter.writeNamespace(prefix2, namespace2);
                                    xmlWriter.setPrefix(prefix2, namespace2);

                                } else {
                                    xmlWriter.writeStartElement(namespace2, "Exception");
                                }

                            } else {
                                xmlWriter.writeStartElement("Exception");
                            }
                            localException.serialize(xmlWriter);
                            xmlWriter.writeEndElement();
                        } else {

                            // write null attribute
                            java.lang.String namespace2 = "http://imageproxy.orca";
                            if (!namespace2.equals("")) {
                                java.lang.String prefix2 = xmlWriter.getPrefix(namespace2);

                                if (prefix2 == null) {
                                    prefix2 = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                                    xmlWriter.writeStartElement(prefix2, "Exception", namespace2);
                                    xmlWriter.writeNamespace(prefix2, namespace2);
                                    xmlWriter.setPrefix(prefix2, namespace2);

                                } else {
                                    xmlWriter.writeStartElement(namespace2, "Exception");
                                }

                            } else {
                                xmlWriter.writeStartElement("Exception");
                            }

                            // write the nil attribute
                            writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "nil", "true", xmlWriter);
                            xmlWriter.writeEndElement();

                        }

                    }

                    xmlWriter.writeEndElement();

            }

            /**
             * Util method to write an attribute with the ns prefix
             * @param prefix prefix
             * @param namespace namespace
             * @param attName attName
             * @param attValue attValue
             * @param xmlWriter xmlWriter
             * @throws javax.xml.stream.XMLStreamException in case of error
             */
            private void writeAttribute(java.lang.String prefix, java.lang.String namespace, java.lang.String attName,
                    java.lang.String attValue, javax.xml.stream.XMLStreamWriter xmlWriter)
                    throws javax.xml.stream.XMLStreamException {
                    if (xmlWriter.getPrefix(namespace) == null) {
                        xmlWriter.writeNamespace(prefix, namespace);
                        xmlWriter.setPrefix(prefix, namespace);

                    }

                    xmlWriter.writeAttribute(namespace, attName, attValue);

            }

            /**
             * Util method to write an attribute without the ns prefix
             * @param namespace namespace
             * @param attName attName
             * @param attValue attValue
             * @param xmlWriter xmlWriter
             * @throws javax.xml.stream.XMLStreamException in case of error
             */
            private void writeAttribute(java.lang.String namespace, java.lang.String attName, java.lang.String attValue,
                    javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {
                if (namespace.equals("")) {
                    xmlWriter.writeAttribute(attName, attValue);
                } else {
                    registerPrefix(xmlWriter, namespace);
                    xmlWriter.writeAttribute(namespace, attName, attValue);
                }
            }

            /**
             * Register a namespace prefix
             * @param namespace namespace
             * @param xmlWriter xmlWriter
             * @return java.lang.String
             * @throws javax.xml.stream.XMLStreamException in case of error
             */
            private java.lang.String registerPrefix(javax.xml.stream.XMLStreamWriter xmlWriter,
                    java.lang.String namespace) throws javax.xml.stream.XMLStreamException {
                java.lang.String prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = createPrefix();

                    while (xmlWriter.getNamespaceContext().getNamespaceURI(prefix) != null) {
                        prefix = createPrefix();
                    }

                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);
                }

                return prefix;
            }

            /**
             * Create a prefix
             * @return java.lang.String
             */
            private java.lang.String createPrefix() {
                return "ns" + (int) Math.random();
            }
        };

        return dataSource;
    }

    /**
     * databinding method to get an XML representation of this object
     * @param qName qName
     * @return javax.xml.stream.XMLStreamReader
     */
    public javax.xml.stream.XMLStreamReader getPullParser(javax.xml.namespace.QName qName) {

        java.util.ArrayList elementList = new java.util.ArrayList();
        java.util.ArrayList attribList = new java.util.ArrayList();

        if (localExceptionTracker) {
            elementList.add(new javax.xml.namespace.QName("http://imageproxy.orca", "Exception"));

            elementList.add(localException == null ? null : localException);
        }

        return new org.apache.axis2.databinding.utils.reader.ADBXMLStreamReaderImpl(qName, elementList.toArray(),
                attribList.toArray());

    }

    /**
     * Factory class that keeps the parse method
     */
    public static class Factory {

        /**
         * static method to create the object Precondition: If this object is an element, the current or next start
         * element starts this object and any intervening reader events are ignorable If this object is not an element,
         * it is a complex type and the reader is at the event just after the outer start element Postcondition: If this
         * object is an element, the reader is positioned at its end element If this object is a complex type, the
         * reader is positioned at the end element of its outer element
         * @param reader reader
         * @return Exception 
         * @throws java.lang.Exception in case of error
         */
        public static Exception parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception {
            Exception object = new Exception();
            int event;
            try {

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance", "type") != null) {
                    java.lang.String fullTypeName = reader
                        .getAttributeValue("http://www.w3.org/2001/XMLSchema-instance", "type");
                    if (fullTypeName != null) {
                        java.lang.String nsPrefix = fullTypeName.substring(0, fullTypeName.indexOf(":"));
                        nsPrefix = nsPrefix == null ? "" : nsPrefix;

                        java.lang.String type = fullTypeName.substring(fullTypeName.indexOf(":") + 1);
                        if (!"Exception".equals(type)) {
                            // find namespace for the prefix
                            java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                            return (Exception) orca.handlers.ec2.tasks.imgproxy.ExtensionMapper.getTypeObject(nsUri,
                                    type, reader);
                        }

                    }

                }

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                reader.next();

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("http://imageproxy.orca", "Exception")
                        .equals(reader.getName())) {

                    boolean loopDone1 = false;
                    javax.xml.namespace.QName startQname1 = new javax.xml.namespace.QName("http://imageproxy.orca",
                            "Exception");

                    while (!loopDone1) {
                        if (reader.isStartElement() && startQname1.equals(reader.getName())) {
                            loopDone1 = true;
                        } else {
                            reader.next();
                        }
                    }

                    // We need to wrap the reader so that it produces a fake START_DOCUEMENT event
                    // this is needed by the builder classes
                    org.apache.axis2.databinding.utils.NamedStaxOMBuilder builder1 = new org.apache.axis2.databinding.utils.NamedStaxOMBuilder(
                            new org.apache.axis2.util.StreamWrapper(reader), startQname1);
                    object.setException(builder1.getOMElement().getFirstElement());

                    reader.next();

                        } // End of if for expected property start element

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();
                if (reader.isStartElement())
                    // A start element we are not expecting indicates a trailing invalid property
                    throw new java.lang.RuntimeException("Unexpected subelement " + reader.getLocalName());

            } catch (javax.xml.stream.XMLStreamException e) {
                throw new java.lang.Exception(e);
            }

            return object;
        }

    }// end of factory class

}
