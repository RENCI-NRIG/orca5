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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;


public class Serializer
{
    /**
     * Converts a hashtable storing ID objects to a comma separated
     * string
     *
     * @param ids
     *
     * @return
     */
    public static String idsToString(Vector<ID> ids)
    {
        StringBuffer sb = new StringBuffer();

        int count = 0;

        for (int i = 0; i < ids.size(); i++) {
            ID id = ids.get(i);

            if (count > 0) {
                sb.append(",");
            }

            sb.append(id.toString());
        }

        return sb.toString();
    }

    /**
     * Converts a comma separated list of identifiers into a Hashtable
     * of identifier objects.
     *
     * @param ids
     *
     * @return
     */
    public static Vector<ID> stringToIDs(String ids)
    {
        Vector<ID> result = new Vector<ID>();
        StringTokenizer t = new StringTokenizer(ids, ",");

        while (t.hasMoreTokens()) {
            String sid = t.nextToken();
            ID id = new ID(sid);
            result.add(id);
        }

        return result;
    }

    /**
     * Deserializes a string to a properties list.
     *
     * @param s string representation of the properties list
     *
     * @return resulting properties list
     *
     * @throws Exception
     */
    public static Properties toProperties(String s) throws Exception
    {
        Properties properties = new Properties();

        if (s != null) {
            ByteArrayInputStream stream = new ByteArrayInputStream(s.getBytes());
            properties.load(stream);
        }

        return properties;
    }

    /**
     * Converts a string to a properties list.
     *
     * @param s
     *
     * @return
     *
     * @throws Exception
     */
    public static Properties toProperties2(String s) throws Exception
    {
        Properties p = null;

        if (s != null) {
            StringTokenizer t = new StringTokenizer(s, ",");

            while (t.hasMoreTokens()) {
                String token = t.nextToken();
                String[] temp = token.split("=");

                if (temp.length == 2) {
                    if (p == null) {
                        p = new Properties();
                    }

                    p.setProperty(temp[0], temp[1]);
                }
            }
        }

        return p;
    }

    /**
     * Serializes a properties list to a string
     *
     * @param properties The properties list
     *
     * @return The string representation of this properties list
     *
     * @throws Exception
     */
    public static String toString(Properties properties)
    {
        if ((properties != null) && (properties.size() > 0)) {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                properties.store(stream, null);
                String string = new String(stream.toByteArray(), "8859_1");
                String sep = System.getProperty("line.separator");
                return string.substring(string.indexOf(sep) + sep.length());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return "";
        }
    }

    /**
     * Converts a properties list to a string.
     *
     * @param p properties list
     *
     * @return string representation of the properties list
     */
    public static String toString2(Properties p)
    {
        StringBuffer sb = new StringBuffer();
        Iterator<Map.Entry<Object, Object>> i = p.entrySet().iterator();
        int index = 0;

        while (i.hasNext()) {
            Map.Entry<Object, Object> entry = i.next();
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();

            if (index > 0) {
                sb.append(",");
            }

            sb.append(name);
            sb.append("=");
            sb.append(value);
            index++;
        }

        return sb.toString();
    }
}