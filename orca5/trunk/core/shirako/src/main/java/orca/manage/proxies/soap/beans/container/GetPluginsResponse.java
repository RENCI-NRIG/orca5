//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.2-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.08.11 at 05:51:15 PM EDT 
//


package orca.manage.proxies.soap.beans.container;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import orca.manage.beans.PluginMng;
import orca.manage.beans.ResultMng;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="status" type="{http://www.nicl.duke.edu/orca/manage/beans}resultMng"/>
 *         &lt;element name="plugins" type="{http://www.nicl.duke.edu/orca/manage/beans}pluginMng" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "status",
    "plugins"
})
@XmlRootElement(name = "GetPluginsResponse")
public class GetPluginsResponse {

    @XmlElement(required = true)
    protected ResultMng status;
    protected List<PluginMng> plugins;

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link ResultMng }
     *     
     */
    public ResultMng getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link ResultMng }
     *     
     */
    public void setStatus(ResultMng value) {
        this.status = value;
    }

    /**
     * Gets the value of the plugins property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the plugins property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPlugins().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PluginMng }
     * 
     * 
     */
    public List<PluginMng> getPlugins() {
        if (plugins == null) {
            plugins = new ArrayList<PluginMng>();
        }
        return this.plugins;
    }

}
