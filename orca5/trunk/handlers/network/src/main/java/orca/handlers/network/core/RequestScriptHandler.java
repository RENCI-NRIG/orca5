package orca.handlers.network.core;

import java.io.IOException;
import java.util.Properties;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class RequestScriptHandler extends ScriptHandler {
    public RequestScriptHandler(ConsoleDevice console, String basepath, Properties props) {
        super(console, basepath, props);
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {

        if (localName.equals("script")) {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(new ActionScriptHandler(console, basepath, props));
            try {
                reader.parse(loadScript(cdata));
            } catch (IOException e) {
                logger.error(e);
                throw new RuntimeException(e);
            }
        }
    }
}
