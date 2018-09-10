
            /**
            * Reservation.java
            *
            * This file was auto-generated from WSDL
            * by the Apache Axis2 version: #axisVersion# #today#
            */

            package net.exogeni.orca.shirako.proxies.soapaxis2.beans;
            /**
            *  Reservation bean class
            */
        
        public  class Reservation
        implements org.apache.axis2.databinding.ADBBean{
        /* This type was generated from the piece of schema that had
                name = reservation
                Namespace URI = http://orca/shirako/proxies/soapaxis2/beans
                Namespace Prefix = ns1
                */
            

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
                        * field for Slice
                        */

                        protected net.exogeni.orca.shirako.proxies.soapaxis2.beans.Slice localSlice ;
                        

                           /**
                           * Auto generated getter method
                           * @return net.exogeni.orca.shirako.proxies.soapaxis2.beans.Slice
                           */
                           public  net.exogeni.orca.shirako.proxies.soapaxis2.beans.Slice getSlice(){
                               return localSlice;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Slice
                               */
                               public void setSlice(net.exogeni.orca.shirako.proxies.soapaxis2.beans.Slice param){
                            
                                    this.localSlice=param;
                            

                               }
                            

                        /**
                        * field for ResourceSet
                        */

                        protected net.exogeni.orca.shirako.proxies.soapaxis2.beans.ResourceSet localResourceSet ;
                        
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localResourceSetTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return net.exogeni.orca.shirako.proxies.soapaxis2.beans.ResourceSet
                           */
                           public  net.exogeni.orca.shirako.proxies.soapaxis2.beans.ResourceSet getResourceSet(){
                               return localResourceSet;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ResourceSet
                               */
                               public void setResourceSet(net.exogeni.orca.shirako.proxies.soapaxis2.beans.ResourceSet param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localResourceSetTracker = true;
                                       } else {
                                          localResourceSetTracker = false;
                                              
                                       }
                                   
                                    this.localResourceSet=param;
                            

                               }
                            

                        /**
                        * field for Term
                        */

                        protected net.exogeni.orca.shirako.proxies.soapaxis2.beans.Term localTerm ;
                        
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localTermTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return net.exogeni.orca.shirako.proxies.soapaxis2.beans.Term
                           */
                           public  net.exogeni.orca.shirako.proxies.soapaxis2.beans.Term getTerm(){
                               return localTerm;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Term
                               */
                               public void setTerm(net.exogeni.orca.shirako.proxies.soapaxis2.beans.Term param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localTermTracker = true;
                                       } else {
                                          localTermTracker = false;
                                              
                                       }
                                   
                                    this.localTerm=param;
                            

                               }
                            

                        /**
                        * field for Sequence
                        */

                        protected long localSequence ;
                        

                           /**
                           * Auto generated getter method
                           * @return long
                           */
                           public  long getSequence(){
                               return localSequence;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Sequence
                               */
                               public void setSequence(long param){
                            
                                    this.localSequence=param;
                            

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
               parentQName,factory,dataSource);
            
       }

     /**
     *
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
                             
                                    if (localSlice==null){
                                         throw new RuntimeException("slice cannot be null!!");
                                    }
                                   localSlice.getOMDataSource(
                                       new javax.xml.namespace.QName("","slice"),
                                       factory).serialize(xmlWriter);
                                 if (localResourceSetTracker){
                                    if (localResourceSet==null){
                                         throw new RuntimeException("resourceSet cannot be null!!");
                                    }
                                   localResourceSet.getOMDataSource(
                                       new javax.xml.namespace.QName("","resourceSet"),
                                       factory).serialize(xmlWriter);
                                } if (localTermTracker){
                                    if (localTerm==null){
                                         throw new RuntimeException("term cannot be null!!");
                                    }
                                   localTerm.getOMDataSource(
                                       new javax.xml.namespace.QName("","term"),
                                       factory).serialize(xmlWriter);
                                }
                                    namespace = "";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                                            xmlWriter.writeStartElement(prefix,"sequence", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"sequence");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("sequence");
                                    }
                                
                                       xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSequence));
                                    
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
         * @param xmlWriter xmlWriter
         * @param namespace namespace
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
          * @return java.lang.String
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
        *
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
                                                                      "slice"));
                            
                            
                                    if (localSlice==null){
                                         throw new RuntimeException("slice cannot be null!!");
                                    }
                                    elementList.add(localSlice);
                                 if (localResourceSetTracker){
                            elementList.add(new javax.xml.namespace.QName("",
                                                                      "resourceSet"));
                            
                            
                                    if (localResourceSet==null){
                                         throw new RuntimeException("resourceSet cannot be null!!");
                                    }
                                    elementList.add(localResourceSet);
                                } if (localTermTracker){
                            elementList.add(new javax.xml.namespace.QName("",
                                                                      "term"));
                            
                            
                                    if (localTerm==null){
                                         throw new RuntimeException("term cannot be null!!");
                                    }
                                    elementList.add(localTerm);
                                }
                             elementList.add(new javax.xml.namespace.QName("",
                                                                      "sequence"));
                            
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSequence));
                            

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
        * @return Reservation
        * @throws java.lang.Exception in case of error
        */
        public static Reservation parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            Reservation object = new Reservation();
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
                    if (!"reservation".equals(type)){
                        //find namespace for the prefix
                        java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                        return (Reservation)net.exogeni.orca.shirako.proxies.soapaxis2.beans.ExtensionMapper.getTypeObject(
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
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("","slice").equals(reader.getName())){
                                
                                        object.setSlice(net.exogeni.orca.shirako.proxies.soapaxis2.beans.Slice.Factory.parse(reader));
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                            
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new java.lang.RuntimeException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("","resourceSet").equals(reader.getName())){
                                
                                        object.setResourceSet(net.exogeni.orca.shirako.proxies.soapaxis2.beans.ResourceSet.Factory.parse(reader));
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("","term").equals(reader.getName())){
                                
                                        object.setTerm(net.exogeni.orca.shirako.proxies.soapaxis2.beans.Term.Factory.parse(reader));
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("","sequence").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setSequence(
                                        org.apache.axis2.databinding.utils.ConverterUtil.convertToLong(content));
                                              
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
           
          
