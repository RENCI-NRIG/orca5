
/**
* DriverElement.java
*
* This file was auto-generated from WSDL
* by the Apache Axis2 version: #axisVersion# #today#
*/

package orca.nodeagent.documents;

/**
 * DriverElement bean class
 */

public class DriverElement implements org.apache.axis2.databinding.ADBBean {

    public static final javax.xml.namespace.QName MY_QNAME = new javax.xml.namespace.QName(
            "http://orca/nodeagent/documents", "driverElement", "ns1");

    /**
     * field for DriverId
     */

    protected java.lang.String localDriverId;

    /**
     * Auto generated getter method
     * 
     * @return java.lang.String
     */
    public java.lang.String getDriverId() {
        return localDriverId;
    }

    /**
     * Auto generated setter method
     * 
     * @param param
     *            DriverId
     */
    public void setDriverId(java.lang.String param) {

        this.localDriverId = param;

    }

    /**
     * field for ClassName
     */

    protected java.lang.String localClassName;

    /*
     * This tracker boolean wil be used to detect whether the user called the set method for this attribute. It will be
     * used to determine whether to include this field in the serialized XML
     */
    protected boolean localClassNameTracker = false;

    /**
     * Auto generated getter method
     * 
     * @return java.lang.String
     */
    public java.lang.String getClassName() {
        return localClassName;
    }

    /**
     * Auto generated setter method
     * 
     * @param param
     *            ClassName
     */
    public void setClassName(java.lang.String param) {

        if (param != null) {
            // update the setting tracker
            localClassNameTracker = true;
        } else {
            localClassNameTracker = false;

        }

        this.localClassName = param;

    }

    /**
     * field for Path
     */

    protected java.lang.String localPath;

    /*
     * This tracker boolean wil be used to detect whether the user called the set method for this attribute. It will be
     * used to determine whether to include this field in the serialized XML
     */
    protected boolean localPathTracker = false;

    /**
     * Auto generated getter method
     * 
     * @return java.lang.String
     */
    public java.lang.String getPath() {
        return localPath;
    }

    /**
     * Auto generated setter method
     * 
     * @param param
     *            Path
     */
    public void setPath(java.lang.String param) {

        if (param != null) {
            // update the setting tracker
            localPathTracker = true;
        } else {
            localPathTracker = false;

        }

        this.localPath = param;

    }

    /**
     * field for Pkg
     */

    protected javax.activation.DataHandler localPkg;

    /*
     * This tracker boolean wil be used to detect whether the user called the set method for this attribute. It will be
     * used to determine whether to include this field in the serialized XML
     */
    protected boolean localPkgTracker = false;

    /**
     * Auto generated getter method
     * 
     * @return javax.activation.DataHandler
     */
    public javax.activation.DataHandler getPkg() {
        return localPkg;
    }

    /**
     * Auto generated setter method
     * 
     * @param param
     *            Pkg
     */
    public void setPkg(javax.activation.DataHandler param) {

        if (param != null) {
            // update the setting tracker
            localPkgTracker = true;
        } else {
            localPkgTracker = false;

        }

        this.localPkg = param;

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

        return new org.apache.axiom.om.impl.llom.OMSourcedElementImpl(MY_QNAME, factory, dataSource);

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

                namespace = "";
                if (!namespace.equals("")) {
                    prefix = xmlWriter.getPrefix(namespace);

                    if (prefix == null) {
                        prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                        xmlWriter.writeStartElement(prefix, "driverId", namespace);
                        xmlWriter.writeNamespace(prefix, namespace);
                        xmlWriter.setPrefix(prefix, namespace);

                    } else {
                        xmlWriter.writeStartElement(namespace, "driverId");
                    }

                } else {
                    xmlWriter.writeStartElement("driverId");
                }

                if (localDriverId == null) {
                    // write the nil attribute

                    throw new RuntimeException("driverId cannot be null!!");

                } else {

                    xmlWriter.writeCharacters(localDriverId);

                }

                xmlWriter.writeEndElement();
                if (localClassNameTracker) {
                    namespace = "";
                    if (!namespace.equals("")) {
                        prefix = xmlWriter.getPrefix(namespace);

                        if (prefix == null) {
                            prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                            xmlWriter.writeStartElement(prefix, "className", namespace);
                            xmlWriter.writeNamespace(prefix, namespace);
                            xmlWriter.setPrefix(prefix, namespace);

                        } else {
                            xmlWriter.writeStartElement(namespace, "className");
                        }

                    } else {
                        xmlWriter.writeStartElement("className");
                    }

                    if (localClassName == null) {
                        // write the nil attribute

                        throw new RuntimeException("className cannot be null!!");

                    } else {

                        xmlWriter.writeCharacters(localClassName);

                    }

                    xmlWriter.writeEndElement();
                }
                if (localPathTracker) {
                    namespace = "";
                    if (!namespace.equals("")) {
                        prefix = xmlWriter.getPrefix(namespace);

                        if (prefix == null) {
                            prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                            xmlWriter.writeStartElement(prefix, "path", namespace);
                            xmlWriter.writeNamespace(prefix, namespace);
                            xmlWriter.setPrefix(prefix, namespace);

                        } else {
                            xmlWriter.writeStartElement(namespace, "path");
                        }

                    } else {
                        xmlWriter.writeStartElement("path");
                    }

                    if (localPath == null) {
                        // write the nil attribute

                        throw new RuntimeException("path cannot be null!!");

                    } else {

                        xmlWriter.writeCharacters(localPath);

                    }

                    xmlWriter.writeEndElement();
                }
                if (localPkgTracker) {
                    namespace = "";
                    if (!namespace.equals("")) {
                        prefix = xmlWriter.getPrefix(namespace);

                        if (prefix == null) {
                            prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                            xmlWriter.writeStartElement(prefix, "pkg", namespace);
                            xmlWriter.writeNamespace(prefix, namespace);
                            xmlWriter.setPrefix(prefix, namespace);

                        } else {
                            xmlWriter.writeStartElement(namespace, "pkg");
                        }

                    } else {
                        xmlWriter.writeStartElement("pkg");
                    }

                    if (localPkg != null) {
                        org.apache.axiom.om.impl.llom.OMTextImpl localPkg_binary = new org.apache.axiom.om.impl.llom.OMTextImpl(
                                localPkg, org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        localPkg_binary.internalSerializeAndConsume(xmlWriter);
                    }

                    xmlWriter.writeEndElement();
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
             * @return String
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
     *
     */
    public javax.xml.stream.XMLStreamReader getPullParser(javax.xml.namespace.QName qName) {

        java.util.ArrayList elementList = new java.util.ArrayList();
        java.util.ArrayList attribList = new java.util.ArrayList();

        elementList.add(new javax.xml.namespace.QName("", "driverId"));

        if (localDriverId != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDriverId));
        } else {
            throw new RuntimeException("driverId cannot be null!!");
        }
        if (localClassNameTracker) {
            elementList.add(new javax.xml.namespace.QName("", "className"));

            if (localClassName != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localClassName));
            } else {
                throw new RuntimeException("className cannot be null!!");
            }
        }
        if (localPathTracker) {
            elementList.add(new javax.xml.namespace.QName("", "path"));

            if (localPath != null) {
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localPath));
            } else {
                throw new RuntimeException("path cannot be null!!");
            }
        }
        if (localPkgTracker) {
            elementList.add(new javax.xml.namespace.QName("", "pkg"));
            elementList.add(localPkg);
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
       * @return DriverElement 
       * @throws java.lang.Exception in case of error
         */
        public static DriverElement parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception {
            DriverElement object = new DriverElement();
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
                        if (!"driverElement".equals(type)) {
                            // find namespace for the prefix
                            java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                            return (DriverElement) orca.nodeagent.documents.ExtensionMapper.getTypeObject(nsUri, type,
                                    reader);
                        }

                    }

                }

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                reader.next();

                while (!reader.isEndElement()) {
                    if (reader.isStartElement()) {

                        if (reader.isStartElement()
                                && new javax.xml.namespace.QName("", "driverId").equals(reader.getName())) {

                            java.lang.String content = reader.getElementText();

                            object.setDriverId(
                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                            reader.next();

                        } // End of if for expected property start element

                        else

                        if (reader.isStartElement()
                                && new javax.xml.namespace.QName("", "className").equals(reader.getName())) {

                            java.lang.String content = reader.getElementText();

                            object.setClassName(
                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                            reader.next();

                        } // End of if for expected property start element

                        else

                        if (reader.isStartElement()
                                && new javax.xml.namespace.QName("", "path").equals(reader.getName())) {

                            java.lang.String content = reader.getElementText();

                            object.setPath(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                            reader.next();

                        } // End of if for expected property start element

                        else

                        if (reader.isStartElement()
                                && new javax.xml.namespace.QName("", "pkg").equals(reader.getName())) {
                            reader.next();
                            if (isReaderMTOMAware(reader) && java.lang.Boolean.TRUE
                                    .equals(reader.getProperty(org.apache.axiom.om.OMConstants.IS_BINARY))) {
                                // MTOM aware reader - get the datahandler directly and put it in the object
                                object.setPkg((javax.activation.DataHandler) reader
                                        .getProperty(org.apache.axiom.om.OMConstants.DATA_HANDLER));
                            } else {
                                if (reader.getEventType() == javax.xml.stream.XMLStreamConstants.START_ELEMENT
                                        && reader.getName()
                                                .equals(new javax.xml.namespace.QName(
                                                        org.apache.axiom.om.impl.MTOMConstants.XOP_NAMESPACE_URI,
                                                        org.apache.axiom.om.impl.MTOMConstants.XOP_INCLUDE))) {
                                    java.lang.String id = org.apache.axiom.om.util.ElementHelper.getContentID(reader,
                                            "UTF-8");
                                    object.setPkg(
                                            ((org.apache.axiom.soap.impl.builder.MTOMStAXSOAPModelBuilder) ((org.apache.axiom.om.impl.llom.OMStAXWrapper) reader)
                                                    .getBuilder()).getDataHandler(id));
                                    reader.next();

                                    reader.next();

                                } else if (reader.hasText()) {
                                    // Do the usual conversion
                                    java.lang.String content = reader.getText();
                                    object.setPkg(org.apache.axis2.databinding.utils.ConverterUtil
                                            .convertToBase64Binary(content));

                                    reader.next();

                                }
                            }

                            reader.next();

                        } // End of if for expected property start element

                        else {
                            // A start element we are not expecting indicates an invalid parameter was passed
                            throw new java.lang.RuntimeException("Unexpected subelement " + reader.getLocalName());
                        }

                    } else
                        reader.next();
                } // end of while loop

            } catch (javax.xml.stream.XMLStreamException e) {
                throw new java.lang.Exception(e);
            }

            return object;
        }

    }// end of factory class

}
