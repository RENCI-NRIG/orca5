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
 * <p>Java class for actorMng complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="actorMng"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="type" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="owner" type="{http://www.nicl.duke.edu/orca/manage/beans}authTokenMng"/&gt;
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="policyClass" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="eventHandler" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="loadSource" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="actorClass" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="online" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="managementClass" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="ID" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="policyGuid" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actorMng", propOrder = {

})
public class ActorMng {

    @XmlElement(required = true)
    protected String name;
    protected int type;
    @XmlElement(required = true)
    protected AuthTokenMng owner;
    @XmlElement(required = true)
    protected String description;
    @XmlElement(required = true)
    protected String policyClass;
    protected String eventHandler;
    protected String loadSource;
    @XmlElement(required = true)
    protected String actorClass;
    protected boolean online;
    protected String managementClass;
    @XmlElement(name = "ID", required = true)
    protected String id;
    @XmlElement(required = true)
    protected String policyGuid;

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
     * Gets the value of the type property.
     * @return type of property
     */
    public int getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * @param value type of property
     */
    public void setType(int value) {
        this.type = value;
    }

    /**
     * Gets the value of the owner property.
     * 
     * @return
     *     possible object is
     *     {@link AuthTokenMng }
     *     
     */
    public AuthTokenMng getOwner() {
        return owner;
    }

    /**
     * Sets the value of the owner property.
     * 
     * @param value
     *     allowed object is
     *     {@link AuthTokenMng }
     *     
     */
    public void setOwner(AuthTokenMng value) {
        this.owner = value;
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
     * Gets the value of the policyClass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPolicyClass() {
        return policyClass;
    }

    /**
     * Sets the value of the policyClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPolicyClass(String value) {
        this.policyClass = value;
    }

    /**
     * Gets the value of the eventHandler property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEventHandler() {
        return eventHandler;
    }

    /**
     * Sets the value of the eventHandler property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEventHandler(String value) {
        this.eventHandler = value;
    }

    /**
     * Gets the value of the loadSource property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLoadSource() {
        return loadSource;
    }

    /**
     * Sets the value of the loadSource property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLoadSource(String value) {
        this.loadSource = value;
    }

    /**
     * Gets the value of the actorClass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getActorClass() {
        return actorClass;
    }

    /**
     * Sets the value of the actorClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setActorClass(String value) {
        this.actorClass = value;
    }

    /**
     * Gets the value of the online property.
     * @return value of online property
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Sets the value of the online property.
     * @param value value of online property
     */
    public void setOnline(boolean value) {
        this.online = value;
    }

    /**
     * Gets the value of the managementClass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getManagementClass() {
        return managementClass;
    }

    /**
     * Sets the value of the managementClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setManagementClass(String value) {
        this.managementClass = value;
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
     * Gets the value of the policyGuid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPolicyGuid() {
        return policyGuid;
    }

    /**
     * Sets the value of the policyGuid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPolicyGuid(String value) {
        this.policyGuid = value;
    }

}
