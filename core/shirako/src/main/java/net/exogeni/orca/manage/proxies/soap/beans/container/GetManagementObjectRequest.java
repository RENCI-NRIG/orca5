//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.04.06 at 05:12:18 PM EDT 
//


package net.exogeni.orca.manage.proxies.soap.beans.container;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import net.exogeni.orca.manage.beans.AuthTokenMng;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="auth" type="{http://www.nicl.duke.edu/orca/manage/beans}authTokenMng"/&gt;
 *         &lt;element name="managementObjectID" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "auth",
    "managementObjectID"
})
@XmlRootElement(name = "GetManagementObjectRequest")
public class GetManagementObjectRequest {

    @XmlElement(required = true)
    protected AuthTokenMng auth;
    @XmlElement(required = true)
    protected String managementObjectID;

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
     * Gets the value of the managementObjectID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getManagementObjectID() {
        return managementObjectID;
    }

    /**
     * Sets the value of the managementObjectID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setManagementObjectID(String value) {
        this.managementObjectID = value;
    }

}
