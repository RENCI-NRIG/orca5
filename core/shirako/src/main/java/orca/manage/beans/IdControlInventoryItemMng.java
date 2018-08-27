//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.04.06 at 05:09:56 PM EDT 
//


package orca.manage.beans;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for IdControlInventoryItemMng complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="IdControlInventoryItemMng"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="nodeID" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="nodeName" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="available" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="free" type="{http://www.w3.org/2001/XMLSchema}long" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IdControlInventoryItemMng", propOrder = {
    "nodeID",
    "nodeName",
    "available",
    "free"
})
public class IdControlInventoryItemMng {

    @XmlElement(required = true)
    protected String nodeID;
    @XmlElement(required = true)
    protected String nodeName;
    protected boolean available;
    @XmlElement(type = Long.class)
    protected List<Long> free;

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

    /**
     * Gets the value of the nodeName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * Sets the value of the nodeName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNodeName(String value) {
        this.nodeName = value;
    }

    /**
     * Gets the value of the available property.
     * @return value of available property
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Sets the value of the available property.
     * @param value available property
     */
    public void setAvailable(boolean value) {
        this.available = value;
    }

    /**
     * Gets the value of the free property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the free property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFree().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Long }
     * 
     * @return list of free 
     */
    public List<Long> getFree() {
        if (free == null) {
            free = new ArrayList<Long>();
        }
        return this.free;
    }

}
