package net.exogeni.orca.handlers.network.core;

import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ScriptHandler extends DefaultHandler {
    protected Logger logger = Logger.getLogger(this.getClass().getCanonicalName());
    protected ConsoleDevice console = null;
    protected String cdata = null;
    protected Locator locator = null;
    protected String basepath;
    protected Properties props;

    public ScriptHandler(ConsoleDevice console, String basepath, Properties props) {
        this.console = console;
        this.basepath = basepath;
        this.props = props;
    }

    protected InputSource loadScript(String scriptNm) {
        assert basepath != null;

        String path = basepath + "/" + scriptNm + ".xml";
        URL url = this.getClass().getResource(path);
        return new InputSource(url.toString());
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        cdata = new String(ch, start, length);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }
}
