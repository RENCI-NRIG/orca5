//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.2-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.08.11 at 05:51:15 PM EDT 
//


package orca.manage.proxies.soap.beans.serveractor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import orca.manage.beans.AuthTokenMng;
import orca.manage.beans.PropertiesMng;


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
 *         &lt;element name="guid" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="auth" type="{http://www.nicl.duke.edu/orca/manage/beans}authTokenMng"/>
 *         &lt;element name="clientSliceId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="resourcePoolId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ticketId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="resourceType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="startTime" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="stopTime" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="units" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="ticketProperties" type="{http://www.nicl.duke.edu/orca/manage/beans}propertiesMng" minOccurs="0"/>
 *         &lt;element name="resourceProperties" type="{http://www.nicl.duke.edu/orca/manage/beans}propertiesMng" minOccurs="0"/>
 *         &lt;element name="clientName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="clientGuid" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "guid",
    "auth",
    "clientSliceId",
    "resourcePoolId",
    "ticketId",
    "resourceType",
    "startTime",
    "stopTime",
    "units",
    "ticketProperties",
    "resourceProperties",
    "clientName",
    "clientGuid"
})
@XmlRootElement(name = "ExportResourcesRequest")
public class ExportResourcesRequest {

    @XmlElement(required = true)
    protected String guid;
    @XmlElement(required = true)
    protected AuthTokenMng auth;
    @XmlElement(required = true)
    protected String clientSliceId;
    protected String resourcePoolId;
    protected String ticketId;
    protected String resourceType;
    protected long startTime;
    protected long stopTime;
    protected int units;
    protected PropertiesMng ticketProperties;
    protected PropertiesMng resourceProperties;
    protected String clientName;
    protected String clientGuid;

    /**
     * Gets the value of the guid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGuid() {
        return guid;
    }

    /**
     * Sets the value of the guid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGuid(String value) {
        this.guid = value;
    }

    /**
     * Gets the value of the auth property.
     * 
     * @return
     *     possible object is
     *     {@link AuthTokenMng }
     *     
     */
    public AuthTokenMng getAuth() {
        return auth;
    }

    /**
     * Sets the value of the auth property.
     * 
     * @param value
     *     allowed object is
     *     {@link AuthTokenMng }
     *     
     */
    public void setAuth(AuthTokenMng value) {
        this.auth = value;
    }

    /**
     * Gets the value of the clientSliceId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClientSliceId() {
        return clientSliceId;
    }

    /**
     * Sets the value of the clientSliceId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClientSliceId(String value) {
        this.clientSliceId = value;
    }

    /**
     * Gets the value of the resourcePoolId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResourcePoolId() {
        return resourcePoolId;
    }

    /**
     * Sets the value of the resourcePoolId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResourcePoolId(String value) {
        this.resourcePoolId = value;
    }

    /**
     * Gets the value of the ticketId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTicketId() {
        return ticketId;
    }

    /**
     * Sets the value of the ticketId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTicketId(String value) {
        this.ticketId = value;
    }

    /**
     * Gets the value of the resourceType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Sets the value of the resourceType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setResourceType(String value) {
        this.resourceType = value;
    }

    /**
     * Gets the value of the startTime property.
     * 
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Sets the value of the startTime property.
     * 
     */
    public void setStartTime(long value) {
        this.startTime = value;
    }

    /**
     * Gets the value of the stopTime property.
     * 
     */
    public long getStopTime() {
        return stopTime;
    }

    /**
     * Sets the value of the stopTime property.
     * 
     */
    public void setStopTime(long value) {
        this.stopTime = value;
    }

    /**
     * Gets the value of the units property.
     * 
     */
    public int getUnits() {
        return units;
    }

    /**
     * Sets the value of the units property.
     * 
     */
    public void setUnits(int value) {
        this.units = value;
    }

    /**
     * Gets the value of the ticketProperties property.
     * 
     * @return
     *     possible object is
     *     {@link PropertiesMng }
     *     
     */
    public PropertiesMng getTicketProperties() {
        return ticketProperties;
    }

    /**
     * Sets the value of the ticketProperties property.
     * 
     * @param value
     *     allowed object is
     *     {@link PropertiesMng }
     *     
     */
    public void setTicketProperties(PropertiesMng value) {
        this.ticketProperties = value;
    }

    /**
     * Gets the value of the resourceProperties property.
     * 
     * @return
     *     possible object is
     *     {@link PropertiesMng }
     *     
     */
    public PropertiesMng getResourceProperties() {
        return resourceProperties;
    }

    /**
     * Sets the value of the resourceProperties property.
     * 
     * @param value
     *     allowed object is
     *     {@link PropertiesMng }
     *     
     */
    public void setResourceProperties(PropertiesMng value) {
        this.resourceProperties = value;
    }

    /**
     * Gets the value of the clientName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Sets the value of the clientName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClientName(String value) {
        this.clientName = value;
    }

    /**
     * Gets the value of the clientGuid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClientGuid() {
        return clientGuid;
    }

    /**
     * Sets the value of the clientGuid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClientGuid(String value) {
        this.clientGuid = value;
    }

}