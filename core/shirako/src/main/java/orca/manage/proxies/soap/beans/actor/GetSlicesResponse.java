//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.04.06 at 05:12:19 PM EDT 
//


package orca.manage.proxies.soap.beans.actor;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import orca.manage.beans.ResultMng;
import orca.manage.beans.SliceMng;


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
 *         &lt;element name="status" type="{http://www.nicl.duke.edu/orca/manage/beans}resultMng"/&gt;
 *         &lt;element name="slices" type="{http://www.nicl.duke.edu/orca/manage/beans}sliceMng" maxOccurs="unbounded" minOccurs="0"/&gt;
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
    "status",
    "slices"
})
@XmlRootElement(name = "GetSlicesResponse")
public class GetSlicesResponse {

    @XmlElement(required = true)
    protected ResultMng status;
    protected List<SliceMng> slices;

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link ResultMng }
     *     
     */
    public ResultMng getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link ResultMng }
     *     
     */
    public void setStatus(ResultMng value) {
        this.status = value;
    }

    /**
     * Gets the value of the slices property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the slices property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSlices().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SliceMng }
     * 
     * @return list of slice 
     * 
     */
    public List<SliceMng> getSlices() {
        if (slices == null) {
            slices = new ArrayList<SliceMng>();
        }
        return this.slices;
    }

}
