//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.2-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.04.08 at 05:33:55 PM AST 
//


package net.exogeni.orca.boot.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for actor complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="actor"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="type" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="guid" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="instance" type="{http://issg.cs.duke.edu/sharp/boot}instance"/&gt;
 *         &lt;element name="owner" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="publicKey" type="{http://issg.cs.duke.edu/sharp/boot}cryptoKey" minOccurs="0"/&gt;
 *         &lt;element name="privateKey" type="{http://issg.cs.duke.edu/sharp/boot}cryptoKey" minOccurs="0"/&gt;
 *         &lt;element name="monitor" type="{http://issg.cs.duke.edu/sharp/boot}instance" minOccurs="0"/&gt;
 *         &lt;element name="mapper" type="{http://issg.cs.duke.edu/sharp/boot}instance" minOccurs="0"/&gt;
 *         &lt;element name="plugin" type="{http://issg.cs.duke.edu/sharp/boot}instance" minOccurs="0"/&gt;
 *         &lt;element name="slice" type="{http://issg.cs.duke.edu/sharp/boot}slice" minOccurs="0"/&gt;
 *         &lt;element name="siteName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="pools" type="{http://issg.cs.duke.edu/sharp/boot}pools" minOccurs="0"/&gt;
 *         &lt;element name="controls" type="{http://issg.cs.duke.edu/sharp/boot}controls" minOccurs="0"/&gt;
 *         &lt;element name="inventory" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="policy" type="{http://issg.cs.duke.edu/sharp/boot}policy" minOccurs="0"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actor", propOrder = {

})
public class Actor {

    @XmlElement(required = true)
    protected String type;
    @XmlElement(required = true)
    protected String name;
    protected String guid;
    @XmlElement(required = true)
    protected String description;
    @XmlElement(required = true)
    protected Instance instance;
    protected String owner;
    protected CryptoKey publicKey;
    protected CryptoKey privateKey;
    protected Instance monitor;
    protected Instance mapper;
    protected Instance plugin;
    protected Slice slice;
    protected String siteName;
    protected Pools pools;
    protected Controls controls;
    protected String inventory;
    protected Policy policy;

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

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
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the instance property.
     * 
     * @return
     *     possible object is
     *     {@link Instance }
     *     
     */
    public Instance getInstance() {
        return instance;
    }

    /**
     * Sets the value of the instance property.
     * 
     * @param value
     *     allowed object is
     *     {@link Instance }
     *     
     */
    public void setInstance(Instance value) {
        this.instance = value;
    }

    /**
     * Gets the value of the owner property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Sets the value of the owner property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOwner(String value) {
        this.owner = value;
    }

    /**
     * Gets the value of the publicKey property.
     * 
     * @return
     *     possible object is
     *     {@link CryptoKey }
     *     
     */
    public CryptoKey getPublicKey() {
        return publicKey;
    }

    /**
     * Sets the value of the publicKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link CryptoKey }
     *     
     */
    public void setPublicKey(CryptoKey value) {
        this.publicKey = value;
    }

    /**
     * Gets the value of the privateKey property.
     * 
     * @return
     *     possible object is
     *     {@link CryptoKey }
     *     
     */
    public CryptoKey getPrivateKey() {
        return privateKey;
    }

    /**
     * Sets the value of the privateKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link CryptoKey }
     *     
     */
    public void setPrivateKey(CryptoKey value) {
        this.privateKey = value;
    }

    /**
     * Gets the value of the monitor property.
     * 
     * @return
     *     possible object is
     *     {@link Instance }
     *     
     */
    public Instance getMonitor() {
        return monitor;
    }

    /**
     * Sets the value of the monitor property.
     * 
     * @param value
     *     allowed object is
     *     {@link Instance }
     *     
     */
    public void setMonitor(Instance value) {
        this.monitor = value;
    }

    /**
     * Gets the value of the mapper property.
     * 
     * @return
     *     possible object is
     *     {@link Instance }
     *     
     */
    public Instance getMapper() {
        return mapper;
    }

    /**
     * Sets the value of the mapper property.
     * 
     * @param value
     *     allowed object is
     *     {@link Instance }
     *     
     */
    public void setMapper(Instance value) {
        this.mapper = value;
    }

    /**
     * Gets the value of the plugin property.
     * 
     * @return
     *     possible object is
     *     {@link Instance }
     *     
     */
    public Instance getPlugin() {
        return plugin;
    }

    /**
     * Sets the value of the plugin property.
     * 
     * @param value
     *     allowed object is
     *     {@link Instance }
     *     
     */
    public void setPlugin(Instance value) {
        this.plugin = value;
    }

    /**
     * Gets the value of the slice property.
     * 
     * @return
     *     possible object is
     *     {@link Slice }
     *     
     */
    public Slice getSlice() {
        return slice;
    }

    /**
     * Sets the value of the slice property.
     * 
     * @param value
     *     allowed object is
     *     {@link Slice }
     *     
     */
    public void setSlice(Slice value) {
        this.slice = value;
    }

    /**
     * Gets the value of the siteName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSiteName() {
        return siteName;
    }

    /**
     * Sets the value of the siteName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSiteName(String value) {
        this.siteName = value;
    }

    /**
     * Gets the value of the pools property.
     * 
     * @return
     *     possible object is
     *     {@link Pools }
     *     
     */
    public Pools getPools() {
        return pools;
    }

    /**
     * Sets the value of the pools property.
     * 
     * @param value
     *     allowed object is
     *     {@link Pools }
     *     
     */
    public void setPools(Pools value) {
        this.pools = value;
    }

    /**
     * Gets the value of the controls property.
     * 
     * @return
     *     possible object is
     *     {@link Controls }
     *     
     */
    public Controls getControls() {
        return controls;
    }

    /**
     * Sets the value of the controls property.
     * 
     * @param value
     *     allowed object is
     *     {@link Controls }
     *     
     */
    public void setControls(Controls value) {
        this.controls = value;
    }

    /**
     * Gets the value of the inventory property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInventory() {
        return inventory;
    }

    /**
     * Sets the value of the inventory property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInventory(String value) {
        this.inventory = value;
    }

    /**
     * Gets the value of the policy property.
     * 
     * @return
     *     possible object is
     *     {@link Policy }
     *     
     */
    public Policy getPolicy() {
        return policy;
    }

    /**
     * Sets the value of the policy property.
     * 
     * @param value
     *     allowed object is
     *     {@link Policy }
     *     
     */
    public void setPolicy(Policy value) {
        this.policy = value;
    }

}
