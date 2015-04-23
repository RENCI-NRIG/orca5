//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.2-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.08.11 at 05:51:16 PM EDT 
//


package orca.manage.proxies.soap.beans.clientactor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import orca.manage.beans.AuthTokenMng;
import orca.manage.beans.TicketReservationMng;


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
 *         &lt;element name="reservation" type="{http://www.nicl.duke.edu/orca/manage/beans}ticketReservationMng"/>
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
    "reservation"
})
@XmlRootElement(name = "AddReservationRequest")
public class AddReservationRequest {

    @XmlElement(required = true)
    protected String guid;
    @XmlElement(required = true)
    protected AuthTokenMng auth;
    @XmlElement(required = true)
    protected TicketReservationMng reservation;

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
     * Gets the value of the reservation property.
     * 
     * @return
     *     possible object is
     *     {@link TicketReservationMng }
     *     
     */
    public TicketReservationMng getReservation() {
        return reservation;
    }

    /**
     * Sets the value of the reservation property.
     * 
     * @param value
     *     allowed object is
     *     {@link TicketReservationMng }
     *     
     */
    public void setReservation(TicketReservationMng value) {
        this.reservation = value;
    }

}