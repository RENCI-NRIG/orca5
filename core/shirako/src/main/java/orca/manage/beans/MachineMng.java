//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.04.06 at 05:09:56 PM EDT 
//


package orca.manage.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for machineMng complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="machineMng"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="state" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="pending" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="macs" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="memory" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="currentIP" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="currentHostName" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="dhcpHWAddress" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="dhcpStatements" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="dhcpOption" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="site" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="poolName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="poolID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="poolResourceType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="available" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="nodeID" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "machineMng", propOrder = {
    "name",
    "state",
    "pending",
    "macs",
    "memory",
    "currentIP",
    "currentHostName",
    "dhcpHWAddress",
    "dhcpStatements",
    "dhcpOption",
    "site",
    "poolName",
    "poolID",
    "poolResourceType",
    "available",
    "nodeID"
})
public class MachineMng {

    @XmlElement(required = true)
    protected String name;
    protected int state;
    protected int pending;
    @XmlElement(required = true)
    protected String macs;
    protected int memory;
    @XmlElement(required = true)
    protected String currentIP;
    @XmlElement(required = true)
    protected String currentHostName;
    @XmlElement(required = true)
    protected String dhcpHWAddress;
    @XmlElement(required = true)
    protected String dhcpStatements;
    @XmlElement(required = true)
    protected String dhcpOption;
    @XmlElement(required = true)
    protected String site;
    protected String poolName;
    protected String poolID;
    protected String poolResourceType;
    protected boolean available;
    @XmlElement(required = true)
    protected String nodeID;

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
     * Gets the value of the state property.
     * @return value of the state property
     */
    public int getState() {
        return state;
    }

    /**
     * Sets the value of the state property.
     * @param value value of the state property
     */
    public void setState(int value) {
        this.state = value;
    }

    /**
     * Gets the value of the pending property.
     * @return value of the pending property
     */
    public int getPending() {
        return pending;
    }

    /**
     * Sets the value of the pending property.
     * @param value value of the pending property
     */
    public void setPending(int value) {
        this.pending = value;
    }

    /**
     * Gets the value of the macs property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMacs() {
        return macs;
    }

    /**
     * Sets the value of the macs property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMacs(String value) {
        this.macs = value;
    }

    /**
     * Gets the value of the memory property.
     * @return value of the memory property
     */
    public int getMemory() {
        return memory;
    }

    /**
     * Sets the value of the memory property.
     * @param value value of the memory property
     */
    public void setMemory(int value) {
        this.memory = value;
    }

    /**
     * Gets the value of the currentIP property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCurrentIP() {
        return currentIP;
    }

    /**
     * Sets the value of the currentIP property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCurrentIP(String value) {
        this.currentIP = value;
    }

    /**
     * Gets the value of the currentHostName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCurrentHostName() {
        return currentHostName;
    }

    /**
     * Sets the value of the currentHostName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCurrentHostName(String value) {
        this.currentHostName = value;
    }

    /**
     * Gets the value of the dhcpHWAddress property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDhcpHWAddress() {
        return dhcpHWAddress;
    }

    /**
     * Sets the value of the dhcpHWAddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDhcpHWAddress(String value) {
        this.dhcpHWAddress = value;
    }

    /**
     * Gets the value of the dhcpStatements property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDhcpStatements() {
        return dhcpStatements;
    }

    /**
     * Sets the value of the dhcpStatements property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDhcpStatements(String value) {
        this.dhcpStatements = value;
    }

    /**
     * Gets the value of the dhcpOption property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDhcpOption() {
        return dhcpOption;
    }

    /**
     * Sets the value of the dhcpOption property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDhcpOption(String value) {
        this.dhcpOption = value;
    }

    /**
     * Gets the value of the site property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSite() {
        return site;
    }

    /**
     * Sets the value of the site property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSite(String value) {
        this.site = value;
    }

    /**
     * Gets the value of the poolName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPoolName() {
        return poolName;
    }

    /**
     * Sets the value of the poolName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPoolName(String value) {
        this.poolName = value;
    }

    /**
     * Gets the value of the poolID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPoolID() {
        return poolID;
    }

    /**
     * Sets the value of the poolID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPoolID(String value) {
        this.poolID = value;
    }

    /**
     * Gets the value of the poolResourceType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPoolResourceType() {
        return poolResourceType;
    }

    /**
     * Sets the value of the poolResourceType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPoolResourceType(String value) {
        this.poolResourceType = value;
    }

    /**
     * Gets the value of the available property.
     * @return value of the available property
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Sets the value of the available property.
     * @param value value of the available property
     */
    public void setAvailable(boolean value) {
        this.available = value;
    }

    /**
     * Gets the value of the nodeID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNodeID() {
        return nodeID;
    }

    /**
     * Sets the value of the nodeID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNodeID(String value) {
        this.nodeID = value;
    }

}
