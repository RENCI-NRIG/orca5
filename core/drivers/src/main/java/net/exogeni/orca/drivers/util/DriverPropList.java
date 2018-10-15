/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.drivers.util;

import net.exogeni.orca.util.PropList;

import java.util.ArrayList;
import java.util.Properties;


public class DriverPropList
{
    static String PropertyCount = "count";
    static String PropertyPrefix = "item.";
    static String PropertyClass = "className";

    public static Properties setProperty(Properties p, String name, ArrayList list)
                                  throws Exception
    {
        if ((list == null) | (list.size() == 0)) {
            return null;
        }

        Properties result = new Properties();
        int count = 0;

        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);

            if (obj instanceof Serializable) {
                Serializable s = (Serializable) obj;
                Properties temp = new Properties();
                temp.setProperty(PropertyClass, s.getClass().getCanonicalName());
                s.save(temp);
                PropList.setProperty(result, PropertyPrefix + count, temp);
                count++;
            }
        }

        PropList.setProperty(result, PropertyCount, count);
        PropList.setProperty(p, name, result);

        return result;
    }

    public static ArrayList toArrayList(Properties p) throws Exception
    {
        if (p == null) {
            return null;
        }

        ArrayList<Serializable> list = new ArrayList<Serializable>();
        int count = PropList.getIntegerProperty(p, PropertyCount);

        for (int i = 0; i < count; i++) {
            Properties pp = PropList.getPropertiesProperty(p, PropertyPrefix + i);
            String className = pp.getProperty(PropertyClass);
            Class c = Class.forName(className, true, Thread.currentThread().getContextClassLoader());

            if (c != null) {
                Serializable s = (Serializable) c.newInstance();
                s.reset(pp);
                list.add(s);
            }
        }

        return list;
    }
}