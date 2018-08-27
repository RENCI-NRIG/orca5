/**
 * FailedRPC.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: #axisVersion# #today#
 */

package orca.shirako.proxies.soapaxis2.services;
/**
 *  FailedRPC bean class
 */
        
public  class FailedRPC
    implements org.apache.axis2.databinding.ADBBean{

    public static final javax.xml.namespace.QName MY_QNAME = new javax.xml.namespace.QName(
            "http://orca/shirako/proxies/soapaxis2/services",
            "failedRPC",
            "ns2");



    /**
     * field for ReservationID
     */

    protected java.lang.String localReservationID ;


    /**
     * Auto generated getter method
     * @return java.lang.String
     */
    public  java.lang.String getReservationID(){
        return localReservationID;
    }



    /**
     * Auto generated setter method
     * @param param ReservationID
     */
    public void setReservationID(java.lang.String param){

        this.localReservationID=param;


    }


    /**
     * field for MessageID
     */

    protected java.lang.String localMessageID ;


    /**
     * Auto generated getter method
     * @return java.lang.String
     */
    public  java.lang.String getMessageID(){
        return localMessageID;
    }



    /**
     * Auto generated setter method
     * @param param MessageID
     */
    public void setMessageID(java.lang.String param){

        this.localMessageID=param;


    }


    /**
     * field for RequestID
     */

    protected java.lang.String localRequestID ;


    /**
     * Auto generated getter method
     * @return java.lang.String
     */
    public  java.lang.String getRequestID(){
        return localRequestID;
    }



    /**
     * Auto generated setter method
     * @param param RequestID
     */
    public void setRequestID(java.lang.String param){

        this.localRequestID=param;


    }


    /**
     * field for ErrorDetails
     */

    protected java.lang.String localErrorDetails ;


    /**
     * Auto generated getter method
     * @return java.lang.String
     */
    public  java.lang.String getErrorDetails(){
        return localErrorDetails;
    }



    /**
     * Auto generated setter method
     * @param param ErrorDetails
     */
    public void setErrorDetails(java.lang.String param){

        this.localErrorDetails=param;


    }


    /**
     * field for RequestType
     */

    protected int localRequestType ;


    /**
     * Auto generated getter method
     * @return int
     */
    public  int getRequestType(){
        return localRequestType;
    }



    /**
     * Auto generated setter method
     * @param param RequestType
     */
    public void setRequestType(int param){

        this.localRequestType=param;


    }


    /**
     * isReaderMTOMAware
     * @param reader reader
     * @return true if the reader supports MTOM
     */
    public static boolean isReaderMTOMAware(javax.xml.stream.XMLStreamReader reader) {
        boolean isReaderMTOMAware = false;

        try{
            isReaderMTOMAware = java.lang.Boolean.TRUE.equals(reader.getProperty(org.apache.axiom.om.OMConstants.IS_DATA_HANDLERS_AWARE));
        }catch(java.lang.IllegalArgumentException e){
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
    public org.apache.axiom.om.OMElement getOMElement(
            final javax.xml.namespace.QName parentQName,
            final org.apache.axiom.om.OMFactory factory){

        org.apache.axiom.om.OMDataSource dataSource = getOMDataSource(parentQName, factory);


        return new org.apache.axiom.om.impl.llom.OMSourcedElementImpl(
                MY_QNAME,factory,dataSource);

            }

    /**
     * @param parentQName parentQName
     * @param factory factory
     * @return org.apache.axiom.om.OMElement
     */
    public org.apache.axiom.om.OMDataSource getOMDataSource(
            final javax.xml.namespace.QName parentQName,
            final org.apache.axiom.om.OMFactory factory){


        org.apache.axiom.om.OMDataSource dataSource =
            new org.apache.axis2.databinding.ADBDataSource(this,parentQName){

                public void serialize(
                        javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {



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
                    if (! namespace.equals("")) {
                        prefix = xmlWriter.getPrefix(namespace);

                        if (prefix == null) {
                            prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                            xmlWriter.writeStartElement(prefix,"reservationID", namespace);
                            xmlWriter.writeNamespace(prefix, namespace);
                            xmlWriter.setPrefix(prefix, namespace);

                        } else {
                            xmlWriter.writeStartElement(namespace,"reservationID");
                        }

                    } else {
                        xmlWriter.writeStartElement("reservationID");
                    }


                    if (localReservationID==null){
                        // write the nil attribute

                        throw new RuntimeException("reservationID cannot be null!!");

                    }else{


                        xmlWriter.writeCharacters(localReservationID);

                    }

                    xmlWriter.writeEndElement();

                    namespace = "";
                    if (! namespace.equals("")) {
                        prefix = xmlWriter.getPrefix(namespace);

                        if (prefix == null) {
                            prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                            xmlWriter.writeStartElement(prefix,"messageID", namespace);
                            xmlWriter.writeNamespace(prefix, namespace);
                            xmlWriter.setPrefix(prefix, namespace);

                        } else {
                            xmlWriter.writeStartElement(namespace,"messageID");
                        }

                    } else {
                        xmlWriter.writeStartElement("messageID");
                    }


                    if (localMessageID==null){
                        // write the nil attribute

                        throw new RuntimeException("messageID cannot be null!!");

                    }else{


                        xmlWriter.writeCharacters(localMessageID);

                    }

                    xmlWriter.writeEndElement();

                    namespace = "";
                    if (! namespace.equals("")) {
                        prefix = xmlWriter.getPrefix(namespace);

                        if (prefix == null) {
                            prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                            xmlWriter.writeStartElement(prefix,"requestID", namespace);
                            xmlWriter.writeNamespace(prefix, namespace);
                            xmlWriter.setPrefix(prefix, namespace);

                        } else {
                            xmlWriter.writeStartElement(namespace,"requestID");
                        }

                    } else {
                        xmlWriter.writeStartElement("requestID");
                    }


                    if (localRequestID==null){
                        // write the nil attribute

                        throw new RuntimeException("requestID cannot be null!!");

                    }else{


                        xmlWriter.writeCharacters(localRequestID);

                    }

                    xmlWriter.writeEndElement();

                    namespace = "";
                    if (! namespace.equals("")) {
                        prefix = xmlWriter.getPrefix(namespace);

                        if (prefix == null) {
                            prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                            xmlWriter.writeStartElement(prefix,"errorDetails", namespace);
                            xmlWriter.writeNamespace(prefix, namespace);
                            xmlWriter.setPrefix(prefix, namespace);

                        } else {
                            xmlWriter.writeStartElement(namespace,"errorDetails");
                        }

                    } else {
                        xmlWriter.writeStartElement("errorDetails");
                    }


                    if (localErrorDetails==null){
                        // write the nil attribute

                        throw new RuntimeException("errorDetails cannot be null!!");

                    }else{


                        xmlWriter.writeCharacters(localErrorDetails);

                    }

                    xmlWriter.writeEndElement();

                    namespace = "";
                    if (! namespace.equals("")) {
                        prefix = xmlWriter.getPrefix(namespace);

                        if (prefix == null) {
                            prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                            xmlWriter.writeStartElement(prefix,"requestType", namespace);
                            xmlWriter.writeNamespace(prefix, namespace);
                            xmlWriter.setPrefix(prefix, namespace);

                        } else {
                            xmlWriter.writeStartElement(namespace,"requestType");
                        }

                    } else {
                        xmlWriter.writeStartElement("requestType");
                    }

                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localRequestType));

                    xmlWriter.writeEndElement();


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
                private void writeAttribute(java.lang.String prefix,java.lang.String namespace,java.lang.String attName,
                        java.lang.String attValue,javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException{
                    if (xmlWriter.getPrefix(namespace) == null) {
                        xmlWriter.writeNamespace(prefix, namespace);
                        xmlWriter.setPrefix(prefix, namespace);

                    }

                    xmlWriter.writeAttribute(namespace,attName,attValue);

                }

                /**
                 * Util method to write an attribute without the ns prefix
                 * @param namespace namespace
                 * @param attName attName
                 * @param attValue attValue
                 * @param xmlWriter xmlWriter
                 * @throws javax.xml.stream.XMLStreamException in case of error
                 */
                private void writeAttribute(java.lang.String namespace,java.lang.String attName,
                        java.lang.String attValue,javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException{
                    if (namespace.equals(""))
                    {
                        xmlWriter.writeAttribute(attName,attValue);
                    }
                    else
                    {
                        registerPrefix(xmlWriter, namespace);
                        xmlWriter.writeAttribute(namespace,attName,attValue);
                    }
                }

                /**
                 * Register a namespace prefix
                 * @param namespace namespace
                 * @param xmlWriter xmlWriter
                 * @return java.lang.String
                 * @throws javax.xml.stream.XMLStreamException in case of error
                 */
                private java.lang.String registerPrefix(javax.xml.stream.XMLStreamWriter xmlWriter, java.lang.String namespace) throws javax.xml.stream.XMLStreamException {
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
                    return "ns" + (int)Math.random();
                }
            };

        return dataSource;
            }


    /**
     * databinding method to get an XML representation of this object
     * @param qName qName
     * @return javax.xml.stream.XMLStreamReader
     */
    public javax.xml.stream.XMLStreamReader getPullParser(javax.xml.namespace.QName qName){



        java.util.ArrayList elementList = new java.util.ArrayList();
        java.util.ArrayList attribList = new java.util.ArrayList();


        elementList.add(new javax.xml.namespace.QName("",
                    "reservationID"));

        if (localReservationID != null){
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localReservationID));
        } else {
            throw new RuntimeException("reservationID cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("",
                    "messageID"));

        if (localMessageID != null){
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localMessageID));
        } else {
            throw new RuntimeException("messageID cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("",
                    "requestID"));

        if (localRequestID != null){
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localRequestID));
        } else {
            throw new RuntimeException("requestID cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("",
                    "errorDetails"));

        if (localErrorDetails != null){
            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localErrorDetails));
        } else {
            throw new RuntimeException("errorDetails cannot be null!!");
        }

        elementList.add(new javax.xml.namespace.QName("",
                    "requestType"));

        elementList.add(
                org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localRequestType));


        return new org.apache.axis2.databinding.utils.reader.ADBXMLStreamReaderImpl(qName, elementList.toArray(), attribList.toArray());



    }



    /**
     *  Factory class that keeps the parse method
     */
    public static class Factory{


        /**
         * static method to create the object
         * Precondition:  If this object is an element, the current or next start element starts this object and any intervening reader events are ignorable
         *                If this object is not an element, it is a complex type and the reader is at the event just after the outer start element
         * Postcondition: If this object is an element, the reader is positioned at its end element
         *                If this object is a complex type, the reader is positioned at the end element of its outer element
         * @param reader reader
         * @return FailedRPC 
         * @throws java.lang.Exception in case of error
         */
        public static FailedRPC parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            FailedRPC object = new FailedRPC();
            int event;
            try {

                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();


                if (reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","type")!=null){
                    java.lang.String fullTypeName = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance",
                            "type");
                    if (fullTypeName!=null){
                        java.lang.String nsPrefix = fullTypeName.substring(0,fullTypeName.indexOf(":"));
                        nsPrefix = nsPrefix==null?"":nsPrefix;

                        java.lang.String type = fullTypeName.substring(fullTypeName.indexOf(":")+1);
                        if (!"failedRPC".equals(type)){
                            //find namespace for the prefix
                            java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                            return (FailedRPC)orca.shirako.proxies.soapaxis2.beans.ExtensionMapper.getTypeObject(
                                    nsUri,type,reader);
                        }

                    }

                }



                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();


                reader.next();


                while (!reader.isStartElement() && !reader.isEndElement()) reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("","reservationID").equals(reader.getName())){

                    java.lang.String content = reader.getElementText();

                    object.setReservationID(
                            org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else{
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new java.lang.RuntimeException("Unexpected subelement " + reader.getLocalName());
                }


                while (!reader.isStartElement() && !reader.isEndElement()) reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("","messageID").equals(reader.getName())){

                    java.lang.String content = reader.getElementText();

                    object.setMessageID(
                            org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else{
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new java.lang.RuntimeException("Unexpected subelement " + reader.getLocalName());
                }


                while (!reader.isStartElement() && !reader.isEndElement()) reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("","requestID").equals(reader.getName())){

                    java.lang.String content = reader.getElementText();

                    object.setRequestID(
                            org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else{
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new java.lang.RuntimeException("Unexpected subelement " + reader.getLocalName());
                }


                while (!reader.isStartElement() && !reader.isEndElement()) reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("","errorDetails").equals(reader.getName())){

                    java.lang.String content = reader.getElementText();

                    object.setErrorDetails(
                            org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));

                    reader.next();

                }  // End of if for expected property start element

                else{
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new java.lang.RuntimeException("Unexpected subelement " + reader.getLocalName());
                }


                while (!reader.isStartElement() && !reader.isEndElement()) reader.next();

                if (reader.isStartElement() && new javax.xml.namespace.QName("","requestType").equals(reader.getName())){

                    java.lang.String content = reader.getElementText();

                    object.setRequestType(
                            org.apache.axis2.databinding.utils.ConverterUtil.convertToInt(content));

                    reader.next();

                }  // End of if for expected property start element

                else{
                    // A start element we are not expecting indicates an invalid parameter was passed
                    throw new java.lang.RuntimeException("Unexpected subelement " + reader.getLocalName());
                }

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

    }//end of factory class



}
           
          
