package orca.handlers.network.core;

import java.util.Properties;

import org.apache.oro.text.perl.MalformedPerl5PatternException;
import org.apache.oro.text.perl.Perl5Util;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ActionScriptHandler extends ScriptHandler {
    private String send = null;
    private String expect = null;
    private String timeout = "2000";
    private Perl5Util util = new Perl5Util();
    private boolean ignoreScript = false; // flag for missing parameters that are required

    public ActionScriptHandler(ConsoleDevice console, String basepath, Properties props) {
        super(console, basepath, props);
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        // immediately return if we are ignoring the script
        if (ignoreScript) {
            return;
        }

        if (localName.equals("exchange")) {
            try {
                console.executeCommand(send, expect, timeout);
            } catch (CommandException e) {
                throw new RuntimeException("Command execution failed", e);
            }
        } else if (localName.equals("send")) {
            send = replaceVars(new String(cdata));
        } else if (localName.equals("expect")) {
            expect = replaceVars(new String(cdata));
        }
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {

        if (localName.equals("exchange")) {
            send = null;
            expect = null;
        } else if (localName.equals("expect")) {
            if (attributes != null) {
                timeout = attributes.getValue("timeout");
            }
        } else if (localName.equals("action")) {
            // if an action depends on a paramter, execute this action
            // only if the parameter is present
            if (attributes != null) {
                String parmRequired = attributes.getValue("parameter");
                if (parmRequired != null) {
                    boolean parmSupplied = props.containsKey(parmRequired);
                    if (!parmSupplied) {
                        ignoreScript = true;
                    }
                }
            }
        }

        // guarantee timeout has non-null value
        if (timeout == null) {
            timeout = "2000";
        }
    }

    protected String replaceVars(String data) {
        String cmd = data;
        String tagPattern = "/\\{\\$([^\\{\\}]+)\\}/";
        String subPattern = null;
        String val = null;

        try {
            while (util.match(tagPattern, cmd)) {
                String tag = util.group(1);
                val = props.getProperty(tag);
                if (val == null)
                    throw new RuntimeException("Unable to find property " + tag + " expected by the script!");

                val = val.replace("/", "\\/");
                val = val.replace("$", "\\$");
                subPattern = "s/\\{\\$" + tag + "\\}/" + val + "/";
                cmd = util.substitute(subPattern, cmd);
            }
        } catch (MalformedPerl5PatternException e) {
            logger.error("Substitution pattern malformed: " + subPattern, e);
            throw new RuntimeException(e);
        }

        return cmd;
    }

}
