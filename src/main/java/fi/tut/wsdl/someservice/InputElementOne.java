//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-520 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.12.10 at 08:04:54 PM EET 
//


package fi.tut.wsdl.someservice;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


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
 *         &lt;element name="K1" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="K2" type="{http://www.w3.org/2001/XMLSchema}double"/>
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
    "k1",
    "k2"
})
@XmlRootElement(name = "InputElementOne")
public class InputElementOne {

    @XmlElement(name = "K1")
    protected double k1;
    @XmlElement(name = "K2")
    protected double k2;

    /**
     * Gets the value of the k1 property.
     * 
     */
    public double getK1() {
        return k1;
    }

    /**
     * Sets the value of the k1 property.
     * 
     */
    public void setK1(double value) {
        this.k1 = value;
    }

    /**
     * Gets the value of the k2 property.
     * 
     */
    public double getK2() {
        return k2;
    }

    /**
     * Sets the value of the k2 property.
     * 
     */
    public void setK2(double value) {
        this.k2 = value;
    }

}