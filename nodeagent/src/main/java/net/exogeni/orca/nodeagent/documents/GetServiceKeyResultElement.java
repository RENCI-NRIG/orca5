
/**
* GetServiceKeyResultElement.java
*
* This file was auto-generated from WSDL
* by the Apache Axis2 version: #axisVersion# #today#
*/

package net.exogeni.orca.nodeagent.documents;

/**
 * GetServiceKeyResultElement bean class
 */

public class GetServiceKeyResultElement implements org.apache.axis2.databinding.ADBBean {

    public static final javax.xml.namespace.QName MY_QNAME = new javax.xml.namespace.QName(
            "http://net.exogeni.orca/nodeagent/documents", "getServiceKeyResultElement", "ns1");

    /**
     * field for Code
     */

    protected int localCode;

    /**
     * Auto generated getter method
     * 
     * @return int
     */
    public int getCode() {
        return localCode;
    }

    /**
     * Auto generated setter method
     * 
     * @param param
     *            Code
     */
    public void setCode(int param) {

        this.localCode = param;

    }

    /**
     * field for Key This was an Array!
     */

    protected byte[] localKey;

    /**
     * Auto generated getter method
     * 
     * @return byte[]
     */
    public byte[] getKey() {
        return localKey;
    }

    /**
     * validate the array for Key
     * @param param
     */
    protected void validateKey(byte[] param) {

        if ((param != null) && (param.length < 1)) {
            throw new java.lang.RuntimeException();
        }

    }

    /**
     * Auto generated setter method
     * 
     * @param param
     *            Key
     */
    public void setKey(byte[] param) {

        validateKey(param);

        this.localKey = param;
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

                        xmlWriter.writeStartElement(prefix, "code", namespace);
                        xmlWriter.writeNamespace(prefix, namespace);
                        xmlWriter.setPrefix(prefix, namespace);

                    } else {
                        xmlWriter.writeStartElement(namespace, "code");
                    }

                } else {
                    xmlWriter.writeStartElement("code");
                }

                xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCode));

                xmlWriter.writeEndElement();

                if (localKey != null) {
                    namespace = "";
                    boolean emptyNamespace = namespace == null || namespace.length() == 0;
                    prefix = emptyNamespace ? null : xmlWriter.getPrefix(namespace);
                    for (int i = 0; i < localKey.length; i++) {

                        if (!emptyNamespace) {
                            if (prefix == null) {
                                String prefix2 = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                                xmlWriter.writeStartElement(prefix2, "key", namespace);
                                xmlWriter.writeNamespace(prefix2, namespace);
                                xmlWriter.setPrefix(prefix2, namespace);

                            } else {
                                xmlWriter.writeStartElement(namespace, "key");
                            }

                        } else {
                            xmlWriter.writeStartElement("key");
                        }
                        xmlWriter.writeCharacters(
                                org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localKey[i]));
                        xmlWriter.writeEndElement();

                    }
                } else {

                    throw new RuntimeException("key cannot be null!!");

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

        elementList.add(new javax.xml.namespace.QName("", "code"));

        elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localCode));

        if (localKey != null) {
            for (int i = 0; i < localKey.length; i++) {

                elementList.add(new javax.xml.namespace.QName("", "key"));
                elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localKey[i]));

            }
        } else {

            throw new RuntimeException("key cannot be null!!");

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
       * @return GetServiceKeyResultElement 
       * @throws java.lang.Exception in case of error
         */
        public static GetServiceKeyResultElement parse(javax.xml.stream.XMLStreamReader reader)
                throws java.lang.Exception {
            GetServiceKeyResultElement object = new GetServiceKeyResultElement();
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
                        if (!"getServiceKeyResultElement".equals(type)) {
                            // find namespace for the prefix
                            java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                            return (GetServiceKeyResultElement) net.exogeni.orca.nodeagent.documents.ExtensionMapper
                                    .getTypeObject(nsUri, type, reader);
                        }

                    }

                }

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                reader.next();

                java.util.ArrayList list2 = new java.util.ArrayList();

                while (!reader.isEndElement()) {
                    if (reader.isStartElement()) {

                        if (reader.isStartElement()
                                && new javax.xml.namespace.QName("", "code").equals(reader.getName())) {

                            java.lang.String content = reader.getElementText();

                            object.setCode(org.apache.axis2.databinding.utils.ConverterUtil.convertToInt(content));

                            reader.next();

                        } // End of if for expected property start element

                        else

                        if (reader.isStartElement()
                                && new javax.xml.namespace.QName("", "key").equals(reader.getName())) {

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
                                    if (new javax.xml.namespace.QName("", "key").equals(reader.getName())) {
                                        list2.add(reader.getElementText());

                                    } else {
                                        loopDone2 = true;
                                    }
                                }
                            }
                            // call the converter utility to convert and set the array

                            object.setKey((byte[]) org.apache.axis2.databinding.utils.ConverterUtil
                                    .convertToArray(byte.class, list2));

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
