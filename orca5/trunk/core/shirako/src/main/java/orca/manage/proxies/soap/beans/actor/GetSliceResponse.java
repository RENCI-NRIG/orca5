//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.2-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.08.11 at 05:51:15 PM EDT 
//


package orca.manage.proxies.soap.beans.actor;

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
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="status" type="{http://www.nicl.duke.edu/orca/manage/beans}resultMng"/>
 *         &lt;element name="slice" type="{http://www.nicl.duke.edu/orca/manage/beans}sliceMng"/>
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
    "status",
    "slice"
})
@XmlRootElement(name = "GetSliceResponse")
public class GetSliceResponse {

    @XmlElement(required = true)
    protected ResultMng status;
    @XmlElement(required = true)
    protected SliceMng slice;

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
     * Gets the value of the slice property.
     * 
     * @return
     *     possible object is
     *     {@link SliceMng }
     *     
     */
    public SliceMng getSlice() {
        return slice;
    }

    /**
     * Sets the value of the slice property.
     * 
     * @param value
     *     allowed object is
     *     {@link SliceMng }
     *     
     */
    public void setSlice(SliceMng value) {
        this.slice = value;
    }

}
