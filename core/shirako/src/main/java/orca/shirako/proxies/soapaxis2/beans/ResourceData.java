
            /**
            * ResourceData.java
            *
            * This file was auto-generated from WSDL
            * by the Apache Axis2 version: #axisVersion# #today#
            */

            package orca.shirako.proxies.soapaxis2.beans;
            /**
            *  ResourceData bean class
            */
        
        public  class ResourceData
        implements org.apache.axis2.databinding.ADBBean{
        /* This type was generated from the piece of schema that had
                name = resourceData
                Namespace URI = http://orca/shirako/proxies/soapaxis2/beans
                Namespace Prefix = ns1
                */
            

                        /**
                        * field for RequestProperties
                        */

                        protected orca.shirako.proxies.soapaxis2.beans.Properties localRequestProperties ;
                        
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localRequestPropertiesTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return orca.shirako.proxies.soapaxis2.beans.Properties
                           */
                           public  orca.shirako.proxies.soapaxis2.beans.Properties getRequestProperties(){
                               return localRequestProperties;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param RequestProperties
                               */
                               public void setRequestProperties(orca.shirako.proxies.soapaxis2.beans.Properties param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localRequestPropertiesTracker = true;
                                       } else {
                                          localRequestPropertiesTracker = false;
                                              
                                       }
                                   
                                    this.localRequestProperties=param;
                            

                               }
                            

                        /**
                        * field for ConfigurationProperties
                        */

                        protected orca.shirako.proxies.soapaxis2.beans.Properties localConfigurationProperties ;
                        
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localConfigurationPropertiesTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return orca.shirako.proxies.soapaxis2.beans.Properties
                           */
                           public  orca.shirako.proxies.soapaxis2.beans.Properties getConfigurationProperties(){
                               return localConfigurationProperties;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ConfigurationProperties
                               */
                               public void setConfigurationProperties(orca.shirako.proxies.soapaxis2.beans.Properties param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localConfigurationPropertiesTracker = true;
                                       } else {
                                          localConfigurationPropertiesTracker = false;
                                              
                                       }
                                   
                                    this.localConfigurationProperties=param;
                            

                               }
                            

                        /**
                        * field for ResourceProperties
                        */

                        protected orca.shirako.proxies.soapaxis2.beans.Properties localResourceProperties ;
                        
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localResourcePropertiesTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return orca.shirako.proxies.soapaxis2.beans.Properties
                           */
                           public  orca.shirako.proxies.soapaxis2.beans.Properties getResourceProperties(){
                               return localResourceProperties;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ResourceProperties
                               */
                               public void setResourceProperties(orca.shirako.proxies.soapaxis2.beans.Properties param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localResourcePropertiesTracker = true;
                                       } else {
                                          localResourcePropertiesTracker = false;
                                              
                                       }
                                   
                                    this.localResourceProperties=param;
                            

                               }
                            

     /**
     * isReaderMTOMAware
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
        * @param parentQName
        * @param factory
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
     * @param parentQName
     * @param factory
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

                
                if (localRequestPropertiesTracker){
                                    if (localRequestProperties==null){
                                         throw new RuntimeException("requestProperties cannot be null!!");
                                    }
                                   localRequestProperties.getOMDataSource(
                                       new javax.xml.namespace.QName("","requestProperties"),
                                       factory).serialize(xmlWriter);
                                } if (localConfigurationPropertiesTracker){
                                    if (localConfigurationProperties==null){
                                         throw new RuntimeException("configurationProperties cannot be null!!");
                                    }
                                   localConfigurationProperties.getOMDataSource(
                                       new javax.xml.namespace.QName("","configurationProperties"),
                                       factory).serialize(xmlWriter);
                                } if (localResourcePropertiesTracker){
                                    if (localResourceProperties==null){
                                         throw new RuntimeException("resourceProperties cannot be null!!");
                                    }
                                   localResourceProperties.getOMDataSource(
                                       new javax.xml.namespace.QName("","resourceProperties"),
                                       factory).serialize(xmlWriter);
                                }
                   
               xmlWriter.writeEndElement();
            
            

        }

         /**
          * Util method to write an attribute with the ns prefix
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
          */
          private java.lang.String createPrefix() {
                return "ns" + (int)Math.random();
          }
        };

        return dataSource;
    }

  
        /**
        * databinding method to get an XML representation of this object
        *
        */
        public javax.xml.stream.XMLStreamReader getPullParser(javax.xml.namespace.QName qName){


        
                 java.util.ArrayList elementList = new java.util.ArrayList();
                 java.util.ArrayList attribList = new java.util.ArrayList();

                 if (localRequestPropertiesTracker){
                            elementList.add(new javax.xml.namespace.QName("",
                                                                      "requestProperties"));
                            
                            
                                    if (localRequestProperties==null){
                                         throw new RuntimeException("requestProperties cannot be null!!");
                                    }
                                    elementList.add(localRequestProperties);
                                } if (localConfigurationPropertiesTracker){
                            elementList.add(new javax.xml.namespace.QName("",
                                                                      "configurationProperties"));
                            
                            
                                    if (localConfigurationProperties==null){
                                         throw new RuntimeException("configurationProperties cannot be null!!");
                                    }
                                    elementList.add(localConfigurationProperties);
                                } if (localResourcePropertiesTracker){
                            elementList.add(new javax.xml.namespace.QName("",
                                                                      "resourceProperties"));
                            
                            
                                    if (localResourceProperties==null){
                                         throw new RuntimeException("resourceProperties cannot be null!!");
                                    }
                                    elementList.add(localResourceProperties);
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
        */
        public static ResourceData parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            ResourceData object = new ResourceData();
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
                    if (!"resourceData".equals(type)){
                        //find namespace for the prefix
                        java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                        return (ResourceData)orca.shirako.proxies.soapaxis2.beans.ExtensionMapper.getTypeObject(
                             nsUri,type,reader);
                      }

                  }

                }
                

                
                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();
                
                    
                    reader.next();
                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("","requestProperties").equals(reader.getName())){
                                
                                        object.setRequestProperties(orca.shirako.proxies.soapaxis2.beans.Properties.Factory.parse(reader));
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("","configurationProperties").equals(reader.getName())){
                                
                                        object.setConfigurationProperties(orca.shirako.proxies.soapaxis2.beans.Properties.Factory.parse(reader));
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("","resourceProperties").equals(reader.getName())){
                                
                                        object.setResourceProperties(orca.shirako.proxies.soapaxis2.beans.Properties.Factory.parse(reader));
                                      
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
           
          