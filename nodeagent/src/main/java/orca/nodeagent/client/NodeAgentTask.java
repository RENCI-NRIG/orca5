/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.nodeagent.client;

import orca.nodeagent.NodeAgentServiceStub;
import orca.nodeagent.documents.ScriptElement;
import orca.nodeagent.documents.ScriptResultElement;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class NodeAgentTask extends Task {
    protected String script;
    protected String arguments = "";
    protected String address;
    protected boolean generate = true;
    protected String stdErrorProperty;
    protected String stdOutProperty;
    protected String exitCodeProperty;

    public NodeAgentTask() {
    }

    public void setStdErrorProperty(String p) {
        this.stdErrorProperty = p;
    }

    public void setStdOutProperty(String p) {
        this.stdOutProperty = p;
    }

    public void setExitCodeProperty(String p) {
        this.exitCodeProperty = p;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public void setGenerate(boolean generate) {
        this.generate = generate;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    protected String makePropertiesString() {
        Hashtable p = getProject().getProperties();

        StringBuffer buffer = new StringBuffer();

        Iterator i = p.entrySet().iterator();

        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();

            if (name.startsWith("host") || name.startsWith("unit")
                    || (name.startsWith("new") && !name.equals("unit.all"))) {
                String value = (String) entry.getValue();
                buffer.append(name);
                buffer.append("=\'");
                // buffer.append("=");
                buffer.append(value);
                // buffer.append(" ");
                buffer.append("\' ");
            }
        }

        String result = buffer.toString();

        return result;

        // getProject().setProperty(outputProperty, result);
    }

    protected void callNodeManager(String args) throws Exception {
        NodeAgentServiceStub stub = new NodeAgentServiceStub(address);

        ScriptElement el = new ScriptElement();
        el.setScript(script);
        el.setArguments(args);

        ScriptResultElement res = stub.executeScript(el);

        if (res != null) {
            if (exitCodeProperty != null) {
                getProject().setProperty(exitCodeProperty, Integer.toString(res.getCode()));
            }

            if (stdOutProperty != null) {
                getProject().setProperty(stdOutProperty, res.getStdOut());
            }

            if (stdErrorProperty != null) {
                getProject().setProperty(stdErrorProperty, res.getStdError());
            }
        }
    }

    public void execute() throws BuildException {
        try {
            String args = arguments;

            if (generate) {
                args = makePropertiesString();
            }

            callNodeManager(args);
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }
}