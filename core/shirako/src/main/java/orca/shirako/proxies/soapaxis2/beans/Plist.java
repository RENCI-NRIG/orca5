
            /**
            * Plist.java
            *
            * This file was auto-generated from WSDL
            * by the Apache Axis2 version: #axisVersion# #today#
            */

            package orca.shirako.proxies.soapaxis2.beans;
            /**
            *  Plist bean class
            */
        
        public  class Plist
        implements org.apache.axis2.databinding.ADBBean{
        /* This type was generated from the piece of schema that had
                name = plist
                Namespace URI = http://orca/shirako/proxies/soapaxis2/beans
                Namespace Prefix = ns1
                */
            

                        /**
                        * field for PlistNode
                        * This was an Array!
                        */

                        protected orca.shirako.proxies.soapaxis2.beans.PlistNode[] localPlistNode ;
                        
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localPlistNodeTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return orca.shirako.proxies.soapaxis2.beans.PlistNode[]
                           */
                           public  orca.shirako.proxies.soapaxis2.beans.PlistNode[] getPlistNode(){
                               return localPlistNode;
                           }

                           
                        


                               
                              /**
                               * validate the array for PlistNode
                               * @param param param
                               */
                              protected void validatePlistNode(orca.shirako.proxies.soapaxis2.beans.PlistNode[] param){
                             
                              }


                             /**
                              * Auto generated setter method
                              * @param param PlistNode
                              */
                              public void setPlistNode(orca.shirako.proxies.soapaxis2.beans.PlistNode[] param){
                              
                                   validatePlistNode(param);

                               
                                          if (param != null){
                                             //update the setting tracker
                                             localPlistNodeTracker = true;
                                          } else {
                                             localPlistNodeTracker = false;
                                                 
                                          }
                                      
                                      this.localPlistNode=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param orca.shirako.proxies.soapaxis2.beans.PlistNode
                             */
                             public void addPlistNode(orca.shirako.proxies.soapaxis2.beans.PlistNode param){
                                   if (localPlistNode == null){
                                   localPlistNode = new orca.shirako.proxies.soapaxis2.beans.PlistNode[]{};
                                   }

                            
                                 //update the setting tracker
                                localPlistNodeTracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localPlistNode);
                               list.add(param);
                               this.localPlistNode =
                             (orca.shirako.proxies.soapaxis2.beans.PlistNode[])list.toArray(
                            new orca.shirako.proxies.soapaxis2.beans.PlistNode[list.size()]);

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

                
                if (localPlistNodeTracker){
                             if (localPlistNode!=null){
                                    for (int i = 0;i < localPlistNode.length;i++){
                                        if (localPlistNode[i] != null){
                                         localPlistNode[i].getOMDataSource(
                                                   new javax.xml.namespace.QName("","plistNode"),
                                                   factory).serialize(xmlWriter);
                                        } else {
                                           
                                                // we don't have to do any thing since minOccures is zero
                                            
                                        }

                                    }
                             } else {
                                
                                       throw new RuntimeException("plistNode cannot be null!!");
                                
                            }
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
          * @return prefix
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

                 if (localPlistNodeTracker){
                             if (localPlistNode!=null) {
                                 for (int i = 0;i < localPlistNode.length;i++){

                                    if (localPlistNode[i] != null){
                                         elementList.add(new javax.xml.namespace.QName("",
                                                                          "plistNode"));
                                         elementList.add(localPlistNode[i]);
                                    } else {
                                        
                                                // nothing to do
                                            
                                    }

                                 }
                             } else {
                                 
                                        throw new RuntimeException("plistNode cannot be null!!");
                                    
                             }

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
        * @return Plist
        * @throws java.lang.Exception in case of error
        */
        public static Plist parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            Plist object = new Plist();
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
                    if (!"plist".equals(type)){
                        //find namespace for the prefix
                        java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                        return (Plist)orca.shirako.proxies.soapaxis2.beans.ExtensionMapper.getTypeObject(
                             nsUri,type,reader);
                      }

                  }

                }
                

                
                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();
                
                    
                    reader.next();
                
                        java.util.ArrayList list1 = new java.util.ArrayList();
                    
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("","plistNode").equals(reader.getName())){
                                
                                    
                                    
                                    // Process the array and step past its final element's end.
                                    list1.add(orca.shirako.proxies.soapaxis2.beans.PlistNode.Factory.parse(reader));
                                            
                                            //loop until we find a start element that is not part of this array
                                            boolean loopDone1 = false;
                                            while(!loopDone1){
                                                // We should be at the end element, but make sure
                                                while (!reader.isEndElement())
                                                    reader.next();
                                                // Step out of this element
                                                reader.next();
                                                // Step to next element event.
                                                while (!reader.isStartElement() && !reader.isEndElement())
                                                    reader.next();
                                                if (reader.isEndElement()){
                                                    //two continuous end elements means we are exiting the xml structure
                                                    loopDone1 = true;
                                                } else {
                                                    if (new javax.xml.namespace.QName("","plistNode").equals(reader.getName())){
                                                        list1.add(orca.shirako.proxies.soapaxis2.beans.PlistNode.Factory.parse(reader));
                                                        
                                                    }else{
                                                        loopDone1 = true;
                                                    }
                                                }
                                            }
                                            // call the converter utility  to convert and set the array
                                            
                                                    object.setPlistNode((orca.shirako.proxies.soapaxis2.beans.PlistNode[])
                                                        org.apache.axis2.databinding.utils.ConverterUtil.convertToArray(
                                                            orca.shirako.proxies.soapaxis2.beans.PlistNode.class,
                                                            list1));
                                                
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
           
          
