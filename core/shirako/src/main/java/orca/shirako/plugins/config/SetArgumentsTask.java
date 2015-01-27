/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.plugins.config;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;


public class SetArgumentsTask extends Task
{
    protected String outputProperty;

    public SetArgumentsTask()
    {
    }

    public void setOutputProperty(String property)
    {
        this.outputProperty = property;
    }

    protected void setPropertiesString()
    {
        Hashtable p = getProject().getProperties();

        StringBuffer buffer = new StringBuffer();

        Iterator i = p.entrySet().iterator();

        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();

            if (name.startsWith("host") || name.startsWith("unit") ||
                    (name.startsWith("new") && !name.equals(outputProperty))) {
                String value = (String) entry.getValue();
                buffer.append(name);
                //                buffer.append("=");
                buffer.append("=\'");
                buffer.append(value);
                //buffer.append(" ");
                buffer.append("\' ");
            }
        }

        String result = buffer.toString();
        getProject().setProperty(outputProperty, result);
    }

    public void execute() throws BuildException
    {
        try {
            setPropertiesString();
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }
}