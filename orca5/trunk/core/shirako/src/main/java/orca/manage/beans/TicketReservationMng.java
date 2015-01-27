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
 * <p>Java class for ticketReservationMng complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ticketReservationMng">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.nicl.duke.edu/orca/manage/beans}reservationMng">
 *       &lt;sequence>
 *         &lt;element name="broker" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ticketProperties" type="{http://www.nicl.duke.edu/orca/manage/beans}propertiesMng" minOccurs="0"/>
 *         &lt;element name="renewable" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ticketReservationMng", propOrder = {
    "broker",
    "ticketProperties",
    "renewable"
})
public class TicketReservationMng
    extends ReservationMng
{

    @XmlElement(required = true)
    protected String broker;
    protected PropertiesMng ticketProperties;
    protected boolean renewable;

    /**
     * Gets the value of the broker property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBroker() {
        return broker;
    }

    /**
     * Sets the value of the broker property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBroker(String value) {
        this.broker = value;
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
     * Gets the value of the renewable property.
     * 
     */
    public boolean isRenewable() {
        return renewable;
    }

    /**
     * Sets the value of the renewable property.
     * 
     */
    public void setRenewable(boolean value) {
        this.renewable = value;
    }

}
