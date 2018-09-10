/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.nodeagent;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;

import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class KeyMasterMessage {
    Document xmldoc = null;

    public KeyMasterMessage() {
    }

    public void createXMLDocument(String[] nodes) {
        xmldoc = new DocumentImpl();

        Element root = xmldoc.createElement("keymastermessage");

        Element[] e = new Element[nodes.length];

        for (int i = 0; i < e.length; i++) {
            e[i] = xmldoc.createElementNS(null, nodes[i]);
            root.appendChild(e[i]);
        }

        xmldoc.appendChild(root);
    }

    public void setNode(String nodeName, String value) {
        NodeList nl = xmldoc.getChildNodes();
        Node keyMasterMessageNode = nl.item(0);

        if (keyMasterMessageNode.getNodeName().compareToIgnoreCase("keymastermessage") == 0) {
            NodeList nl1 = keyMasterMessageNode.getChildNodes();

            for (int i = 0; i < nl1.getLength(); i++) {
                Node n = nl1.item(i);

                if (n.getLocalName().compareToIgnoreCase(nodeName) == 0) {
                    nl1.item(i).setTextContent(value);

                    break;
                }
            }
        }
    }

    public void serializeXMLDocument() {
        try {
            OutputFormat of = new OutputFormat("XML", "ISO-8859-1", true);
            FileOutputStream fos = new FileOutputStream("km.xml");
            XMLSerializer xmlSerializer = new XMLSerializer(fos, of);
            xmlSerializer.asDOMSerializer();
            xmlSerializer.serialize(xmldoc.getDocumentElement());
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getXMLDocumentAsString() {
        try {
            StringWriter strWriter = new StringWriter();
            OutputFormat of = new OutputFormat("XML", "ISO-8859-1", true);
            of.setIndent(1);
            of.setIndenting(true);

            XMLSerializer xmlSerializer = new XMLSerializer(strWriter, of);
            xmlSerializer.asDOMSerializer();
            xmlSerializer.serialize(xmldoc.getDocumentElement());

            StringBuffer strBuffer = strWriter.getBuffer();
            String str = strBuffer.toString();

            return str;
        } catch (Exception ex) {
            return null;
        }
    }

    public void buildXMLDocument(String xmlString) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = factory.newDocumentBuilder();
            InputSource in = new InputSource();
            in.setCharacterStream(new StringReader(xmlString));
            xmldoc = db.parse(in);

            // logger.debug("xmldoc parsed");
        } catch (Exception ex) {
            // logger.debug("exception while creating xmldoc");
            ex.printStackTrace();
        }

        if (xmldoc == null) {
            // logger.debug("xmldoc is null");
        } else {
            // logger.debug("xmldoc is not null");
        }
    }

    public String getNode(String nodeName) {
        NodeList nl = xmldoc.getChildNodes();
        Node keyMasterMessageNode = nl.item(0);

        if (keyMasterMessageNode.getNodeName().compareToIgnoreCase("keymastermessage") == 0) {
            NodeList nl1 = keyMasterMessageNode.getChildNodes();

            for (int i = 0; i < nl1.getLength(); i++) {
                Node n = nl1.item(i);

                if (n.getNodeName().compareToIgnoreCase(nodeName) == 0) {
                    return nl1.item(i).getTextContent();
                }
            }
        }

        return null;
    }

    public byte[] getBytes() {
        StringWriter sw = new StringWriter();
        OutputFormat of = new OutputFormat("XML", "ISO-8859-1", true);
        of.setIndent(1);
        of.setIndenting(true);

        XMLSerializer xmlSerializer = new XMLSerializer(sw, of);

        try {
            xmlSerializer.asDOMSerializer();
            xmlSerializer.serialize(xmldoc.getDocumentElement());
        } catch (Exception ex) {
            ex.printStackTrace();

            return null;
        }

        byte[] res = sw.toString().getBytes();

        return res;
    }

    public static void main(String[] args) {
        /*
         * KeyMasterMessage km = new KeyMasterMessage(); km.setSSHPublicKey("1234567890abcdef");
         * km.setNodeIP("192.168.1.100"); km.setAuthorityIP("192.168.1.1"); km.setTimestamp("2007:04:30");
         * km.setEpoch("1984"); km.serializeXMLDocument();
         */
    }
}