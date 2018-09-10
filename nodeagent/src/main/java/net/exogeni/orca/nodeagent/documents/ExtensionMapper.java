
/**
* ExtensionMapper.java
*
* This file was auto-generated from WSDL
* by the Apache Axis2 version: #axisVersion# #today#
*/

package net.exogeni.orca.nodeagent.documents;

/**
 * ExtensionMapper class
 */

public class ExtensionMapper {

    public static java.lang.Object getTypeObject(java.lang.String namespaceURI, java.lang.String typeName,
            javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception {

        if ("http://net.exogeni.orca/nodeagent/documents".equals(namespaceURI) && "propertiesElement".equals(typeName)) {

            return net.exogeni.orca.nodeagent.documents.PropertiesElement.Factory.parse(reader);

        }

        if ("http://net.exogeni.orca/nodeagent/documents".equals(namespaceURI) && "propertyElement".equals(typeName)) {

            return net.exogeni.orca.nodeagent.documents.PropertyElement.Factory.parse(reader);

        }

        throw new java.lang.RuntimeException("Unsupported type " + namespaceURI + " " + typeName);
    }

}
