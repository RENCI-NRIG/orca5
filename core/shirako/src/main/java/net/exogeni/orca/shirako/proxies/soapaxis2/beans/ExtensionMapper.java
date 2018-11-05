
            /**
            * ExtensionMapper.java
            *
            * This file was auto-generated from WSDL
            * by the Apache Axis2 version: #axisVersion# #today#
            */

            package net.exogeni.orca.shirako.proxies.soapaxis2.beans;
            /**
            *  ExtensionMapper class
            */
        
        public  class ExtensionMapper{

          public static java.lang.Object getTypeObject(java.lang.String namespaceURI,
                                                       java.lang.String typeName,
                                                       javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{

              
                  if (
                  "http://net/exogeni/orca/shirako/proxies/soapaxis2/beans".equals(namespaceURI) &&
                  "reservation".equals(typeName)){
                   
                            return  net.exogeni.orca.shirako.proxies.soapaxis2.beans.Reservation.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://net/exogeni/orca/shirako/proxies/soapaxis2/beans".equals(namespaceURI) &&
                  "plist".equals(typeName)){
                   
                            return  net.exogeni.orca.shirako.proxies.soapaxis2.beans.Plist.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://net/exogeni/orca/shirako/proxies/soapaxis2/beans".equals(namespaceURI) &&
                  "term".equals(typeName)){
                   
                            return  net.exogeni.orca.shirako.proxies.soapaxis2.beans.Term.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://net/exogeni/orca/shirako/proxies/soapaxis2/beans".equals(namespaceURI) &&
                  "resourceSet".equals(typeName)){
                   
                            return  net.exogeni.orca.shirako.proxies.soapaxis2.beans.ResourceSet.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://net/exogeni/orca/shirako/proxies/soapaxis2/beans".equals(namespaceURI) &&
                  "properties".equals(typeName)){
                   
                            return  net.exogeni.orca.shirako.proxies.soapaxis2.beans.Properties.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://net/exogeni/orca/shirako/proxies/soapaxis2/beans".equals(namespaceURI) &&
                  "updateData".equals(typeName)){
                   
                            return  net.exogeni.orca.shirako.proxies.soapaxis2.beans.UpdateData.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://net/exogeni/orca/shirako/proxies/soapaxis2/beans".equals(namespaceURI) &&
                  "property".equals(typeName)){
                   
                            return  net.exogeni.orca.shirako.proxies.soapaxis2.beans.Property.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://net/exogeni/orca/shirako/proxies/soapaxis2/beans".equals(namespaceURI) &&
                  "slice".equals(typeName)){
                   
                            return  net.exogeni.orca.shirako.proxies.soapaxis2.beans.Slice.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://net/exogeni/orca/shirako/proxies/soapaxis2/beans".equals(namespaceURI) &&
                  "plistNode".equals(typeName)){
                   
                            return  net.exogeni.orca.shirako.proxies.soapaxis2.beans.PlistNode.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://net/exogeni/orca/shirako/proxies/soapaxis2/beans".equals(namespaceURI) &&
                  "resourceData".equals(typeName)){
                   
                            return  net.exogeni.orca.shirako.proxies.soapaxis2.beans.ResourceData.Factory.parse(reader);
                        

                  }

              
             throw new java.lang.RuntimeException("Unsupported type " + namespaceURI + " " + typeName);
          }

        }
    
