
/**
* RegisterImageResponse.java
*
* This file was auto-generated from WSDL
* by the Apache Axis2 version: #axisVersion# #today#
*/

package orca.handlers.ec2.tasks.imgproxy;

/**
 * RegisterImageResponse bean class
 */

public class RegisterImageResponse implements org.apache.axis2.databinding.ADBBean {

    public static final javax.xml.namespace.QName MY_QNAME = new javax.xml.namespace.QName("http://imageproxy.orca",
            "RegisterImageResponse", "ns1");

    /**
     * field for _return
     */

    protected java.lang.String local_return;

    /*
     * This tracker boolean wil be used to detect whether the user called the set method for this attribute. It will be
     * used to determine whether to include this field in the serialized XML
     */
    protected boolean local_returnTracker = false;

    /**
     * Auto generated getter method
     * 
     * @return java.lang.String
     */
    public java.lang.String get_return() {
        return local_return;
    }

    /**
     * Auto generated setter method
     * 
     * @param param
     *            _return
     */
    public void set_return(java.lang.String param) {

        if (param != null) {
            // update the setting tracker
            local_returnTracker = true;
        } else {
            local_returnTracker = true;

        }

        this.local_return = param;

    }

    /**
     * isReaderMTOMAware
     * 
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
     *
     * @param parentQName
     * @param factory
     * @return org.apache.axiom.om.OMElement
     */
    public org.apache.axiom.om.OMElement getOMElement(final javax.xml.namespace.QName parentQName,
            final org.apache.axiom.om.OMFactory factory) {

        org.apache.axiom.om.OMDataSource dataSource = getOMDataSource(parentQName, factory);

        return new org.apache.axiom.om.impl.llom.OMSourcedElementImpl(MY_QNAME, factory, dataSource);

    }

    /**
     *
     * @param parentQName
     * @param factory
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

                if (local_returnTracker) {
                    namespace = "http://imageproxy.orca";
                    if (!namespace.equals("")) {
                        prefix = xmlWriter.getPrefix(namespace);

                        if (prefix == null) {
                            prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                            xmlWriter.writeStartElement(prefix, "return", namespace);
                            xmlWriter.writeNamespace(prefix, namespace);
                            xmlWriter.setPrefix(prefix, namespace);

                        } else {
                            xmlWriter.writeStartElement(namespace, "return");
                        }

                    } else {
                        xmlWriter.writeStartElement("return");
                    }

                    if (local_return == null) {
                        // write the nil attribute

                        writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "nil", "true", xmlWriter);

                    } else {

                        xmlWriter.writeCharacters(local_return);

                    }

                    xmlWriter.writeEndElement();
                }

                xmlWriter.writeEndElement();

            }

            /**
             * Util method to write an attribute with the ns prefix
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
             */
            private java.lang.String createPrefix() {
                return "ns" + (int) Math.random();
            }
        };

        return dataSource;
    }

    /**
     * databinding method to get an XML representation of this object
     *
     */
    public javax.xml.stream.XMLStreamReader getPullParser(javax.xml.namespace.QName qName) {

        java.util.ArrayList elementList = new java.util.ArrayList();
        java.util.ArrayList attribList = new java.util.ArrayList();

        if (local_returnTracker) {
            elementList.add(new javax.xml.namespace.QName("http://imageproxy.orca", "return"));

            elementList.add(local_return == null ? null
                    : org.apache.axis2.databinding.utils.ConverterUtil.convertToString(local_return));
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
         */
        public static RegisterImageResponse parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception {
            RegisterImageResponse object = new RegisterImageResponse();
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
                        if (!"RegisterImageResponse".equals(type)) {
                            // find namespace for the prefix
                            java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                            return (RegisterImageResponse) orca.handlers.ec2.tasks.imgproxy.ExtensionMapper
                                    .getTypeObject(nsUri, type, reader);
                        }

                    }

                }

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                reader.next();

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                if (reader.isStartElement()
                        && new javax.xml.namespace.QName("http://imageproxy.orca", "return").equals(reader.getName())) {

                    if (!"true".equals(reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance", "nil"))) {

                        java.lang.String content = reader.getElementText();

                        object.set_return(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    } else {
                        reader.getElementText(); // throw away text nodes if any.
                    }

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
