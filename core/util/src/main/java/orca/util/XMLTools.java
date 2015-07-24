/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.OutputStream;
import java.io.StringWriter;

import java.security.Provider;

import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class XMLTools
{
    public static void DumpNode(Node node, OutputStream stream) throws Exception
    {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();

        t.transform(new DOMSource(node), new StreamResult(stream));
    }

    public static Document getDocument()
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();

            return db.newDocument();
        } catch (ParserConfigurationException e) {
            // NOTE: need to log this exception
            return null;
        }
    }

    public static XMLSignatureFactory getSignatureFactory() throws Exception
    {
        String providerName = System.getProperty("jsr105Provider",
                                                 "org.jcp.xml.dsig.internal.dom.XMLDSigRI");

        return XMLSignatureFactory.getInstance("DOM",
                                               (Provider) Class.forName(providerName).newInstance());
    }

    /**
     * Converts the specified DOM node into an xml string
     *
     * @param node
     *
     * @return
     */
    public static String xmlToString(Node node) throws Exception
    {
        StringWriter writer = new StringWriter();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer trans = tf.newTransformer();
        trans.transform(new DOMSource(node), new StreamResult(writer));

        String result = writer.toString();
        int index = result.indexOf('>');

        if (index > -1) {
            return result.substring(index + 1);
        }

        return result;
    }
}