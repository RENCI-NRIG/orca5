//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.2-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.07.29 at 01:30:39 PM EDT 
//


package orca.manage.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for nodeMng complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="nodeMng">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ID" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="state" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="ip" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="publicIP" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="donated" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="available" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="type" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="sliceName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="sliceGuid" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="reservationGuid" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="realHost" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="vlanTag" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "nodeMng", propOrder = {
    "name",
    "id",
    "state",
    "ip",
    "publicIP",
    "donated",
    "available",
    "type",
    "sliceName",
    "sliceGuid",
    "reservationGuid",
    "realHost",
    "vlanTag"
})
public class NodeMng {

    @XmlElement(required = true)
    protected String name;
    @XmlElement(name = "ID", required = true)
    protected String id;
    protected int state;
    @XmlElement(required = true)
    protected String ip;
    protected String publicIP;
    @XmlElement(defaultValue = "false")
    protected boolean donated;
    @XmlElement(defaultValue = "false")
    protected boolean available;
    protected int type;
    @XmlElement(required = true)
    protected String sliceName;
    @XmlElement(required = true)
    protected String sliceGuid;
    @XmlElement(required = true)
    protected String reservationGuid;
    protected String realHost;
    protected String vlanTag;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getID() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setID(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the state property.
     * 
     */
    public int getState() {
        return state;
    }

    /**
     * Sets the value of the state property.
     * 
     */
    public void setState(int value) {
        this.state = value;
    }

    /**
     * Gets the value of the ip property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIp() {
        return ip;
    }

    /**
     * Sets the value of the ip property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIp(String value) {
        this.ip = value;
    }

    /**
     * Gets the value of the publicIP property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPublicIP() {
        return publicIP;
    }

    /**
     * Sets the value of the publicIP property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPublicIP(String value) {
        this.publicIP = value;
    }

    /**
     * Gets the value of the donated property.
     * 
     */
    public boolean isDonated() {
        return donated;
    }

    /**
     * Sets the value of the donated property.
     * 
     */
    public void setDonated(boolean value) {
        this.donated = value;
    }

    /**
     * Gets the value of the available property.
     * 
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Sets the value of the available property.
     * 
     */
    public void setAvailable(boolean value) {
        this.available = value;
    }

    /**
     * Gets the value of the type property.
     * 
     */
    public int getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     */
    public void setType(int value) {
        this.type = value;
    }

    /**
     * Gets the value of the sliceName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSliceName() {
        return sliceName;
    }

    /**
     * Sets the value of the sliceName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSliceName(String value) {
        this.sliceName = value;
    }

    /**
     * Gets the value of the sliceGuid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSliceGuid() {
        return sliceGuid;
    }

    /**
     * Sets the value of the sliceGuid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSliceGuid(String value) {
        this.sliceGuid = value;
    }

    /**
     * Gets the value of the reservationGuid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReservationGuid() {
        return reservationGuid;
    }

    /**
     * Sets the value of the reservationGuid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReservationGuid(String value) {
        this.reservationGuid = value;
    }

    /**
     * Gets the value of the realHost property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRealHost() {
        return realHost;
    }

    /**
     * Sets the value of the realHost property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRealHost(String value) {
        this.realHost = value;
    }

    /**
     * Gets the value of the vlanTag property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVlanTag() {
        return vlanTag;
    }

    /**
     * Sets the value of the vlanTag property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVlanTag(String value) {
        this.vlanTag = value;
    }

}
