//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.04.06 at 05:09:56 PM EDT 
//


package net.exogeni.orca.manage.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for eventMng complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="eventMng"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="properties" type="{http://www.nicl.duke.edu/orca/manage/beans}propertiesMng" minOccurs="0"/&gt;
 *         &lt;element name="actorId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "eventMng", propOrder = {
    "properties",
    "actorId"
})
@XmlSeeAlso({
    ReservationStateTransitionEventMng.class,
    GenericEventMng.class
})
public abstract class EventMng {

    protected PropertiesMng properties;
    protected String actorId;

    /**
     * Gets the value of the properties property.
     * 
     * @return
     *     possible object is
     *     {@link PropertiesMng }
     *     
     */
    public PropertiesMng getProperties() {
        return properties;
    }

    /**
     * Sets the value of the properties property.
     * 
     * @param value
     *     allowed object is
     *     {@link PropertiesMng }
     *     
     */
    public void setProperties(PropertiesMng value) {
        this.properties = value;
    }

    /**
     * Gets the value of the actorId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getActorId() {
        return actorId;
    }

    /**
     * Sets the value of the actorId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setActorId(String value) {
        this.actorId = value;
    }

}
