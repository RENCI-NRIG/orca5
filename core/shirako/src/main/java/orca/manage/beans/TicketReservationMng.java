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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ticketReservationMng complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ticketReservationMng"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://www.nicl.duke.edu/orca/manage/beans}reservationMng"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="broker" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="ticketProperties" type="{http://www.nicl.duke.edu/orca/manage/beans}propertiesMng" minOccurs="0"/&gt;
 *         &lt;element name="renewable" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="renewTime" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ticketReservationMng", propOrder = {
    "broker",
    "ticketProperties",
    "renewable",
    "renewTime"
})
@XmlSeeAlso({
    LeaseReservationMng.class
})
public class TicketReservationMng
    extends ReservationMng
{

    @XmlElement(required = true)
    protected String broker;
    protected PropertiesMng ticketProperties;
    protected boolean renewable;
    protected long renewTime;

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
     * @return value of the renewable property
     */
    public boolean isRenewable() {
        return renewable;
    }

    /**
     * Sets the value of the renewable property.
     * @param value value of the renewable property
     */
    public void setRenewable(boolean value) {
        this.renewable = value;
    }

    /**
     * Gets the value of the renewTime property.
     * @return value of the renewTime property
     */
    public long getRenewTime() {
        return renewTime;
    }

    /**
     * Sets the value of the renewTime property.
     * @param value value of the renewTime property
     */
    public void setRenewTime(long value) {
        this.renewTime = value;
    }

}
