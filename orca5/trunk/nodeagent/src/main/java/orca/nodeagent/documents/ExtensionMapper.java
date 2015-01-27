
            /**
            * ExtensionMapper.java
            *
            * This file was auto-generated from WSDL
            * by the Apache Axis2 version: #axisVersion# #today#
            */

            package orca.nodeagent.documents;
            /**
            *  ExtensionMapper class
            */
        
        public  class ExtensionMapper{

          public static java.lang.Object getTypeObject(java.lang.String namespaceURI,
                                                       java.lang.String typeName,
                                                       javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{

              
                  if (
                  "http://orca/nodeagent/documents".equals(namespaceURI) &&
                  "propertiesElement".equals(typeName)){
                   
                            return  orca.nodeagent.documents.PropertiesElement.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://orca/nodeagent/documents".equals(namespaceURI) &&
                  "propertyElement".equals(typeName)){
                   
                            return  orca.nodeagent.documents.PropertyElement.Factory.parse(reader);
                        

                  }

              
             throw new java.lang.RuntimeException("Unsupported type " + namespaceURI + " " + typeName);
          }

        }
    