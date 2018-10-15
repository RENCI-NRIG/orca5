
/**
* UnregisterKeyResultElement.java
*
* This file was auto-generated from WSDL
* by the Apache Axis2 version: #axisVersion# #today#
*/

package net.exogeni.orca.nodeagent.documents;

/**
 * UnregisterKeyResultElement bean class
 */

public class UnregisterKeyResultElement implements org.apache.axis2.databinding.ADBBean {

    public static final javax.xml.namespace.QName MY_QNAME = new javax.xml.namespace.QName(
            "http://net.exogeni.orca/nodeagent/documents", "unregisterKeyResultElement", "ns1");

    /**
     * field for UnregisterKeyResultElement
     */

    protected int localUnregisterKeyResultElement;

    /**
     * Auto generated getter method
     * 
     * @return int
     */
    public int getUnregisterKeyResultElement() {
        return localUnregisterKeyResultElement;
    }

    /**
     * Auto generated setter method
     * 
     * @param param
     *            UnregisterKeyResultElement
     */
    public void setUnregisterKeyResultElement(int param) {

        this.localUnregisterKeyResultElement = param;

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
     *
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

                // We can safely assume an element has only one type associated with it

                java.lang.String namespace = "http://net.exogeni.orca/nodeagent/documents";
                java.lang.String localName = "unregisterKeyResultElement";

                if (!namespace.equals("")) {
                    java.lang.String prefix = xmlWriter.getPrefix(namespace);

                    if (prefix == null) {
                        prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                        xmlWriter.writeStartElement(prefix, localName, namespace);
                        xmlWriter.writeNamespace(prefix, namespace);
                        xmlWriter.setPrefix(prefix, namespace);

                    } else {
                        xmlWriter.writeStartElement(namespace, localName);
                    }

                } else {
                    xmlWriter.writeStartElement(localName);
                }

                xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil
                        .convertToString(localUnregisterKeyResultElement));

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
       * @param prefix prefix
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

        // We can safely assume an element has only one type associated with it
        return new org.apache.axis2.databinding.utils.reader.ADBXMLStreamReaderImpl(MY_QNAME, new java.lang.Object[] {
                org.apache.axis2.databinding.utils.reader.ADBXMLStreamReader.ELEMENT_TEXT,
                org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localUnregisterKeyResultElement) },
                null);

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
       * @return UnregisterKeyResultElement 
       * @throws java.lang.Exception in case of error
         */
        public static UnregisterKeyResultElement parse(javax.xml.stream.XMLStreamReader reader)
                throws java.lang.Exception {
            UnregisterKeyResultElement object = new UnregisterKeyResultElement();
            int event;
            try {

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();

                while (!reader.isEndElement()) {
                    if (reader.isStartElement()) {

                        if (reader.isStartElement() && new javax.xml.namespace.QName("http://net.exogeni.orca/nodeagent/documents",
                                "unregisterKeyResultElement").equals(reader.getName())) {

                            java.lang.String content = reader.getElementText();

                            object.setUnregisterKeyResultElement(
                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToInt(content));

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
