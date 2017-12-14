/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.nodeagent.util;

import orca.nodeagent.documents.PropertiesElement;
import orca.nodeagent.documents.PropertyElement;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Serializer {
    /**
     * Serializes a properties list to a properties element
     * 
     * @param p
     * @return
     */
    public static PropertiesElement serialize(Properties p) {
        PropertiesElement result = new PropertiesElement();

        if (p != null) {
            Set set = p.entrySet();
            Iterator iter = set.iterator();

            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                PropertyElement property = new PropertyElement();
                property.setName((String) entry.getKey());
                property.setValue((String) entry.getValue());
                result.addProperty(property);
            }
        }

        return result;
    }

    /**
     * Serializes a properties element to a properties list
     * 
     * @param e
     */
    public static Properties serialize(PropertiesElement e) {
        Properties p = new Properties();

        if (e != null) {
            PropertyElement[] list = e.getProperty();

            if (list != null) {
                for (int i = 0; i < list.length; i++) {
                    p.setProperty(list[i].getName(), list[i].getValue());
                }
            }
        }

        return p;
    }
}