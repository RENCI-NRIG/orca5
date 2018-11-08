
/**
* RegisterAuthorityKeyElement.java
*
* This file was auto-generated from WSDL
* by the Apache Axis2 version: #axisVersion# #today#
*/

package orca.nodeagent.documents;

/**
 * RegisterAuthorityKeyElement bean class
 */

public class RegisterAuthorityKeyElement implements org.apache.axis2.databinding.ADBBean {

    public static final javax.xml.namespace.QName MY_QNAME = new javax.xml.namespace.QName(
            "http://orca/nodeagent/documents", "registerAuthorityKeyElement", "ns1");

    /**
     * field for Alias
     */

    protected java.lang.String localAlias;

    /**
     * Auto generated getter method
     * 
     * @return java.lang.String
     */
    public java.lang.String getAlias() {
        return localAlias;
    }

    /**
     * Auto generated setter method
     * 
     * @param param
     *            Alias
     */
    public void setAlias(java.lang.String param) {

        this.localAlias = param;

    }

    /**
     * field for Certificate This was an Array!
     */

    protected byte[] localCertificate;

    /**
     * Auto generated getter method
     * 
     * @return byte[]
     */
    public byte[] getCertificate() {
        return localCertificate;
    }

    /**
     * validate the array for Certificate
     * @param param param
     */
    protected void validateCertificate(byte[] param) {

        if ((param != null) && (param.length < 1)) {
            throw new java.lang.RuntimeException();
        }

    }

    /**
     * Auto generated setter method
     * 
     * @param param
     *            Certificate
     */
    public void setCertificate(byte[] param) {

        validateCertificate(param);

        this.localCertificate = param;
    }

    /**
     * field for Request This was an Array!
     */

    protected byte[] localRequest;

    /**
     * Auto generated getter method
     * 
     * @return byte[]
     */
    public byte[] getRequest() {
        return localRequest;
    }

    /**
     * validate the array for Request
     * @param param param
     */
    protected void validateRequest(byte[] param) {

        if ((param != null) && (param.length < 1)) {
            throw new java.lang.RuntimeException();
        }

    }

    /**
     * Auto generated setter method
     * 
     * @param param
     *            Request
     */
    public void setRequest(byte[] param) {

        validateRequest(param);

        this.localRequest = param;
    }

    /**
     * field for Signature This was an Array!
     */

    protected byte[] localSignature;

    /**
     * Auto generated getter method
     * 
     * @return byte[]
     */
    public byte[] getSignature() {
        return localSignature;
    }

    /**
     * validate the array for Signature
     * @param param param
     */
    protected void validateSignature(byte[] param) {

        if ((param != null) && (param.length < 1)) {
            throw new java.lang.RuntimeException();
        }

    }

    /**
     * Auto generated setter method
     * 
     * @param param
     *            Signature
     */
    public void setSignature(byte[] param) {

        validateSignature(param);

        this.localSignature = param;
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
     *
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

                        xmlWriter.writeStartElement(prefix, "alias", namespace);
                        xmlWriter.writeNamespace(prefix, namespace);
                        xmlWriter.setPrefix(prefix, namespace);

                    } else {
                        xmlWriter.writeStartElement(namespace, "alias");
                    }

                } else {
                    xmlWriter.writeStartElement("alias");
                }

                if (localAlias == null) {
                    // write the nil attribute

                    throw new RuntimeException("alias cannot be null!!");

                } else {

                    xmlWriter.writeCharacters(localAlias);

                }

                xmlWriter.writeEndElement();

                if (localCertificate != null) {
                    namespace = "";
                    boolean emptyNamespace = namespace == null || namespace.length() == 0;
                    prefix = emptyNamespace ? null : xmlWriter.getPrefix(namespace);
                    for (int i = 0; i < localCertificate.length; i++) {

                        if (!emptyNamespace) {
                            if (prefix == null) {
                                String prefix2 = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                                xmlWriter.writeStartElement(prefix2, "certificate", namespace);
                                xmlWriter.writeNamespace(prefix2, namespace);
                                xmlWriter.setPrefix(prefix2, namespace);

                            } else {
                                xmlWriter.writeStartElement(namespace, "certificate");
                            }

                        } else {
                            xmlWriter.writeStartElement("certificate");
                        }
                        xmlWriter.writeCharacters(
                                org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCertificate[i]));
                        xmlWriter.writeEndElement();

                    }
                } else {

                    throw new RuntimeException("certificate cannot be null!!");

                }

                if (localRequest != null) {
                    namespace = "";
                    boolean emptyNamespace = namespace == null || namespace.length() == 0;
                    prefix = emptyNamespace ? null : xmlWriter.getPrefix(namespace);
                    for (int i = 0; i < localRequest.length; i++) {

                        if (!emptyNamespace) {
                            if (prefix == null) {
                                String prefix2 = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                                xmlWriter.writeStartElement(prefix2, "request", namespace);
                                xmlWriter.writeNamespace(prefix2, namespace);
                                xmlWriter.setPrefix(prefix2, namespace);

                            } else {
                                xmlWriter.writeStartElement(namespace, "request");
                            }

                        } else {
                            xmlWriter.writeStartElement("request");
                        }
                        xmlWriter.writeCharacters(
                                org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localRequest[i]));
                        xmlWriter.writeEndElement();

                    }
                } else {

                    throw new RuntimeException("request cannot be null!!");

                }

                if (localSignature != null) {
                    namespace = "";
                    boolean emptyNamespace = namespace == null || namespace.length() == 0;
                    prefix = emptyNamespace ? null : xmlWriter.getPrefix(namespace);
                    for (int i = 0; i < localSignature.length; i++) {

                        if (!emptyNamespace) {
                            if (prefix == null) {
                                String prefix2 = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                                xmlWriter.writeStartElement(prefix2, "signature", namespace);
                                xmlWriter.writeNamespace(prefix2, namespace);
                                xmlWriter.setPrefix(prefix2, namespace);

                            } else {
                                xmlWriter.writeStartElement(namespace, "signature");
                            }

                        } else {
                            xmlWriter.writeStartElement("signature");
                        }
                        xmlWriter.writeCharacters(
                                org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSignature[i]));
                        xmlWriter.writeEndElement();

                    }
                } else {

                    throw new RuntimeException("signature cannot be null!!");

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
     */
    public javax.xml.stream.XMLStreamReader getPullParser(javax.xml.namespace.QName qName) {

        java.util.ArrayList elementList = new java.util.ArrayList();
        java.util.ArrayList attribList = new java.util.ArrayList();

        elementList.add(new javax.xml.namespace.QName("", "alias"));

        if (localAlias != null) {
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localAlias));
        } else {
            throw new RuntimeException("alias cannot be null!!");
        }

        if (localCertificate != null) {
            for (int i = 0; i < localCertificate.length; i++) {

                elementList.add(new javax.xml.namespace.QName("", "certificate"));
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCertificate[i]));

            }
        } else {

            throw new RuntimeException("certificate cannot be null!!");

        }

        if (localRequest != null) {
            for (int i = 0; i < localRequest.length; i++) {

                elementList.add(new javax.xml.namespace.QName("", "request"));
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localRequest[i]));

            }
        } else {

            throw new RuntimeException("request cannot be null!!");

        }

        if (localSignature != null) {
            for (int i = 0; i < localSignature.length; i++) {

                elementList.add(new javax.xml.namespace.QName("", "signature"));
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSignature[i]));

            }
        } else {

            throw new RuntimeException("signature cannot be null!!");

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
       * @return Close
       * @throws java.lang.Exception in case of error
         */
        public static RegisterAuthorityKeyElement parse(javax.xml.stream.XMLStreamReader reader)
                throws java.lang.Exception {
            RegisterAuthorityKeyElement object = new RegisterAuthorityKeyElement();
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
                        if (!"registerAuthorityKeyElement".equals(type)) {
                            // find namespace for the prefix
                            java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                            return (RegisterAuthorityKeyElement) orca.nodeagent.documents.ExtensionMapper
                                    .getTypeObject(nsUri, type, reader);
                        }

                    }

                }

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                reader.next();

                java.util.ArrayList list2 = new java.util.ArrayList();

                java.util.ArrayList list3 = new java.util.ArrayList();

                java.util.ArrayList list4 = new java.util.ArrayList();

                while (!reader.isEndElement()) {
                    if (reader.isStartElement()) {

                        if (reader.isStartElement()
                                && new javax.xml.namespace.QName("", "alias").equals(reader.getName())) {

                            java.lang.String content = reader.getElementText();

                            object.setAlias(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                            reader.next();

                        } // End of if for expected property start element

                        else

                        if (reader.isStartElement()
                                && new javax.xml.namespace.QName("", "certificate").equals(reader.getName())) {

                            // Process the array and step past its final element's end.
                            list2.add(reader.getElementText());

                            // loop until we find a start element that is not part of this array
                            boolean loopDone2 = false;
                            while (!loopDone2) {
                                // Ensure we are at the EndElement
                                while (!reader.isEndElement()) {
                                    reader.next();
                                }
                                // Step out of this element
                                reader.next();
                                // Step to next element event.
                                while (!reader.isStartElement() && !reader.isEndElement())
                                    reader.next();
                                if (reader.isEndElement()) {
                                    // two continuous end elements means we are exiting the xml structure
                                    loopDone2 = true;
                                } else {
                                    if (new javax.xml.namespace.QName("", "certificate").equals(reader.getName())) {
                                        list2.add(reader.getElementText());

                                    } else {
                                        loopDone2 = true;
                                    }
                                }
                            }
                            // call the converter utility to convert and set the array

                            object.setCertificate((byte[]) org.apache.axis2.databinding.utils.ConverterUtil
                                    .convertToArray(byte.class, list2));

                        } // End of if for expected property start element

                        else

                        if (reader.isStartElement()
                                && new javax.xml.namespace.QName("", "request").equals(reader.getName())) {

                            // Process the array and step past its final element's end.
                            list3.add(reader.getElementText());

                            // loop until we find a start element that is not part of this array
                            boolean loopDone3 = false;
                            while (!loopDone3) {
                                // Ensure we are at the EndElement
                                while (!reader.isEndElement()) {
                                    reader.next();
                                }
                                // Step out of this element
                                reader.next();
                                // Step to next element event.
                                while (!reader.isStartElement() && !reader.isEndElement())
                                    reader.next();
                                if (reader.isEndElement()) {
                                    // two continuous end elements means we are exiting the xml structure
                                    loopDone3 = true;
                                } else {
                                    if (new javax.xml.namespace.QName("", "request").equals(reader.getName())) {
                                        list3.add(reader.getElementText());

                                    } else {
                                        loopDone3 = true;
                                    }
                                }
                            }
                            // call the converter utility to convert and set the array

                            object.setRequest((byte[]) org.apache.axis2.databinding.utils.ConverterUtil
                                    .convertToArray(byte.class, list3));

                        } // End of if for expected property start element

                        else

                        if (reader.isStartElement()
                                && new javax.xml.namespace.QName("", "signature").equals(reader.getName())) {

                            // Process the array and step past its final element's end.
                            list4.add(reader.getElementText());

                            // loop until we find a start element that is not part of this array
                            boolean loopDone4 = false;
                            while (!loopDone4) {
                                // Ensure we are at the EndElement
                                while (!reader.isEndElement()) {
                                    reader.next();
                                }
                                // Step out of this element
                                reader.next();
                                // Step to next element event.
                                while (!reader.isStartElement() && !reader.isEndElement())
                                    reader.next();
                                if (reader.isEndElement()) {
                                    // two continuous end elements means we are exiting the xml structure
                                    loopDone4 = true;
                                } else {
                                    if (new javax.xml.namespace.QName("", "signature").equals(reader.getName())) {
                                        list4.add(reader.getElementText());

                                    } else {
                                        loopDone4 = true;
                                    }
                                }
                            }
                            // call the converter utility to convert and set the array

                            object.setSignature((byte[]) org.apache.axis2.databinding.utils.ConverterUtil
                                    .convertToArray(byte.class, list4));

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
