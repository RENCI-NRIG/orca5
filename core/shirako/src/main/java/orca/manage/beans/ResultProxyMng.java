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
 * <p>Java class for resultProxyMng complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="resultProxyMng"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="status" type="{http://www.nicl.duke.edu/orca/manage/beans}resultMng"/&gt;
 *         &lt;element name="result" type="{http://www.nicl.duke.edu/orca/manage/beans}proxyMng" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "resultProxyMng", propOrder = {
    "status",
    "result"
})
public class ResultProxyMng {

    @XmlElement(required = true)
    protected ResultMng status;
    protected List<ProxyMng> result;

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
     * Gets the value of the result property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the result property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getResult().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ProxyMng }
     * 
     * 
     * @return list of proxy 
     */
    public List<ProxyMng> getResult() {
        if (result == null) {
            result = new ArrayList<ProxyMng>();
        }
        return this.result;
    }

}
