
            /**
            * ResourceSet.java
            *
            * This file was auto-generated from WSDL
            * by the Apache Axis2 version: #axisVersion# #today#
            */

            package orca.shirako.proxies.soapaxis2.beans;
            /**
            *  ResourceSet bean class
            */
        
        public  class ResourceSet
        implements org.apache.axis2.databinding.ADBBean{
        /* This type was generated from the piece of schema that had
                name = resourceSet
                Namespace URI = http://orca/shirako/proxies/soapaxis2/beans
                Namespace Prefix = ns1
                */
            

                        /**
                        * field for Units
                        */

                        protected int localUnits ;
                        

                           /**
                           * Auto generated getter method
                           * @return int
                           */
                           public  int getUnits(){
                               return localUnits;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Units
                               */
                               public void setUnits(int param){
                            
                                    this.localUnits=param;
                            

                               }
                            

                        /**
                        * field for Type
                        */

                        protected java.lang.String localType ;
                        

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getType(){
                               return localType;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Type
                               */
                               public void setType(java.lang.String param){
                            
                                    this.localType=param;
                            

                               }
                            

                        /**
                        * field for ResourceData
                        */

                        protected orca.shirako.proxies.soapaxis2.beans.ResourceData localResourceData ;
                        
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localResourceDataTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return orca.shirako.proxies.soapaxis2.beans.ResourceData
                           */
                           public  orca.shirako.proxies.soapaxis2.beans.ResourceData getResourceData(){
                               return localResourceData;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ResourceData
                               */
                               public void setResourceData(orca.shirako.proxies.soapaxis2.beans.ResourceData param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localResourceDataTracker = true;
                                       } else {
                                          localResourceDataTracker = false;
                                              
                                       }
                                   
                                    this.localResourceData=param;
                            

                               }
                            

                        /**
                        * field for Concrete
                        */

                        protected orca.shirako.proxies.soapaxis2.beans.Plist localConcrete ;
                        
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localConcreteTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return orca.shirako.proxies.soapaxis2.beans.Plist
                           */
                           public  orca.shirako.proxies.soapaxis2.beans.Plist getConcrete(){
                               return localConcrete;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Concrete
                               */
                               public void setConcrete(orca.shirako.proxies.soapaxis2.beans.Plist param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localConcreteTracker = true;
                                       } else {
                                          localConcreteTracker = false;
                                              
                                       }
                                   
                                    this.localConcrete=param;
                            

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

                                            xmlWriter.writeStartElement(prefix,"units", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"units");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("units");
                                    }
                                
                                       xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localUnits));
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();

                                            xmlWriter.writeStartElement(prefix,"type", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"type");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("type");
                                    }
                                

                                          if (localType==null){
                                              // write the nil attribute
                                              
                                                     throw new RuntimeException("type cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localType);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                              if (localResourceDataTracker){
                                    if (localResourceData==null){
                                         throw new RuntimeException("resourceData cannot be null!!");
                                    }
                                   localResourceData.getOMDataSource(
                                       new javax.xml.namespace.QName("","resourceData"),
                                       factory).serialize(xmlWriter);
                                } if (localConcreteTracker){
                                    if (localConcrete==null){
                                         throw new RuntimeException("concrete cannot be null!!");
                                    }
                                   localConcrete.getOMDataSource(
                                       new javax.xml.namespace.QName("","concrete"),
                                       factory).serialize(xmlWriter);
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
          * @param java.lang.String
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
                                                                      "units"));
                            
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localUnits));
                            
                             elementList.add(new javax.xml.namespace.QName("",
                                                                      "type"));
                            
                                        if (localType != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localType));
                                        } else {
                                           throw new RuntimeException("type cannot be null!!");
                                        }
                                     if (localResourceDataTracker){
                            elementList.add(new javax.xml.namespace.QName("",
                                                                      "resourceData"));
                            
                            
                                    if (localResourceData==null){
                                         throw new RuntimeException("resourceData cannot be null!!");
                                    }
                                    elementList.add(localResourceData);
                                } if (localConcreteTracker){
                            elementList.add(new javax.xml.namespace.QName("",
                                                                      "concrete"));
                            
                            
                                    if (localConcrete==null){
                                         throw new RuntimeException("concrete cannot be null!!");
                                    }
                                    elementList.add(localConcrete);
                                }

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
        * @return ResourceSet
        * @throws java.lang.Exception in case of error
        */
        public static ResourceSet parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            ResourceSet object = new ResourceSet();
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
                    if (!"resourceSet".equals(type)){
                        //find namespace for the prefix
                        java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                        return (ResourceSet)orca.shirako.proxies.soapaxis2.beans.ExtensionMapper.getTypeObject(
                             nsUri,type,reader);
                      }

                  }

                }
                

                
                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();
                
                    
                    reader.next();
                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("","units").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setUnits(
                                        org.apache.axis2.databinding.utils.ConverterUtil.convertToInt(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                            
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new java.lang.RuntimeException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("","type").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setType(
                                        org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                            
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new java.lang.RuntimeException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("","resourceData").equals(reader.getName())){
                                
                                        object.setResourceData(orca.shirako.proxies.soapaxis2.beans.ResourceData.Factory.parse(reader));
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("","concrete").equals(reader.getName())){
                                
                                        object.setConcrete(orca.shirako.proxies.soapaxis2.beans.Plist.Factory.parse(reader));
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                              
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
           
          
