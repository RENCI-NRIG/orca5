package orca.boot.util;

import java.io.File;
import java.io.FileOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import orca.util.Base64;
import orca.util.XMLTools;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class AttachFiles {
    public static void main(String[] args) {
        try {
            AttachFiles app = new AttachFiles(args[0], args[1]);
            app.Run();
        } catch (Exception e) {
            System.err.println("An error has occurred.");
            e.printStackTrace();
        }
    }

    public static final String ReferenceTag = "ref";

    public static final String ReferenceAttribute = "href";

    String input;

    String output;

    public AttachFiles(String input, String output) {
        this.input = input;
        this.output = output;
    }

    public void Run() throws Exception {
        Document doc = load(input);
        NodeList nodes = doc.getElementsByTagName(ReferenceTag);

        if (nodes == null) {
            return;
        }

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            NamedNodeMap attrs = node.getAttributes();
            if (attrs.getLength() > 0) {
                Attr attr = (Attr) attrs.item(0);
                if (attr.getNodeName().equals(ReferenceAttribute)) {
                    String file = attr.getNodeValue();
                    String encoded = Base64.encodeFromFile(file);

                    Text newNode = doc.createTextNode(encoded);
                    Node parent = node.getParentNode();
                    parent.replaceChild(newNode, node);
                }
            }
        }

        XMLTools.DumpNode(doc, new FileOutputStream(output));
    }

    public static Document load(String filename) throws Exception {
        // Create a builder factory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);

        // Create the builder and parse the file
        Document doc = factory.newDocumentBuilder().parse(new File(filename));
        return doc;
    }

}
