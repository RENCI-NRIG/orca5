//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.04.06 at 05:09:56 PM EDT 
//


package net.exogeni.orca.manage.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for termMng complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="termMng"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="startTime" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *         &lt;element name="endTime" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *         &lt;element name="ticketTime" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *         &lt;element name="newStartTime" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "termMng", propOrder = {
    "startTime",
    "endTime",
    "ticketTime",
    "newStartTime"
})
public class TermMng {

    protected long startTime;
    protected long endTime;
    protected long ticketTime;
    protected long newStartTime;

    /**
     * Gets the value of the startTime property.
     * @return value of the startTime property
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Sets the value of the startTime property.
     * @param value value of the startTime property
     */
    public void setStartTime(long value) {
        this.startTime = value;
    }

    /**
     * Gets the value of the endTime property.
     * @return value of the endTime property
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Sets the value of the endTime property.
     * @param value value of the endTime property
     */
    public void setEndTime(long value) {
        this.endTime = value;
    }

    /**
     * Gets the value of the ticketTime property.
     * @return value of the ticketTime property
     */
    public long getTicketTime() {
        return ticketTime;
    }

    /**
     * Sets the value of the ticketTime property.
     * @param value value of the ticketTime property
     */
    public void setTicketTime(long value) {
        this.ticketTime = value;
    }

    /**
     * Gets the value of the newStartTime property.
     * @return value of the newStartTime property
     */
    public long getNewStartTime() {
        return newStartTime;
    }

    /**
     * Sets the value of the newStartTime property.
     * @param value value of the newStartTime property
     */
    public void setNewStartTime(long value) {
        this.newStartTime = value;
    }

}
