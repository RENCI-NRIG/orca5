//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.04.06 at 05:12:18 PM EDT 
//


package orca.manage.proxies.soap.beans.container;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import orca.manage.beans.AuthTokenMng;


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
 *         &lt;element name="pluginType" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="actorType" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="packageId" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
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
    "pluginType",
    "actorType",
    "packageId"
})
@XmlRootElement(name = "GetPluginsRequest")
public class GetPluginsRequest {

    @XmlElement(required = true)
    protected AuthTokenMng auth;
    protected int pluginType;
    protected int actorType;
    @XmlElement(required = true)
    protected String packageId;

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
     * Gets the value of the pluginType property.
     * @return value of the pluginType property
     */
    public int getPluginType() {
        return pluginType;
    }

    /**
     * Sets the value of the pluginType property.
     * @param value value of the pluginType property
     */
    public void setPluginType(int value) {
        this.pluginType = value;
    }

    /**
     * Gets the value of the actorType property.
     * @return value of the actorType property
     */
    public int getActorType() {
        return actorType;
    }

    /**
     * Sets the value of the actorType property.
     * @param value value of the actorType property
     */
    public void setActorType(int value) {
        this.actorType = value;
    }

    /**
     * Gets the value of the packageId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPackageId() {
        return packageId;
    }

    /**
     * Sets the value of the packageId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPackageId(String value) {
        this.packageId = value;
    }

}
