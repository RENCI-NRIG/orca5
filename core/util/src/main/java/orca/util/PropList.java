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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;


public class PropList
{
    public static final int DefaultIntegerValue = 0;
    public static final long DefaultLongValue = 0;
    public static final boolean DefaultBooleanValue = false;
    public static final Double DefaultDoubleValue = 0.0;
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public static boolean getBooleanProperty(Properties properties, String name)
    {
        return getBooleanProperty(properties, name, false);
    }

    public static boolean getBooleanProperty(Properties properties, String name, boolean required)
    {
        try {
            String temp = getProperty(properties, name, required);

            if (temp != null) {
                return Boolean.parseBoolean(temp);
            } else {
                return DefaultBooleanValue;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a boolean property from a property list. The value defaults
     * to false if the property is not present.
     *
     * @param p
     * @param descr
     * @param pname
     *
     * @return false by default, true if property set
     *
     * @throws Exception
     */
    public static boolean getBooleanProperty(Properties p, String descr, String pname)
                                      throws Exception
    {
        boolean attr = false;
        String s = p.getProperty(pname);

        if (s != null) {
            try {
                Boolean battr = new Boolean(s);
                attr = battr.booleanValue();
            } catch (Exception e) {
                throw new Exception(
                    "invalid boolean value for property " + pname + " for " + descr + ": " +
                    e.toString());
            }
        }

        return attr;
    }

    public static double getDoubleProperty(Properties properties, String name)
                                    throws Exception
    {
        return getDoubleProperty(properties, name, false);
    }

    public static double getDoubleProperty(Properties properties, String name, boolean required)
                                    throws Exception
    {
        String temp = getProperty(properties, name, required);

        if (temp == null) {
            return DefaultDoubleValue;
        } else {
            return Double.parseDouble(temp);
        }
    }

    public static InetAddress getInetProperty(Properties properties, String name)
                                       throws Exception
    {
        return getInetProperty(properties, name, false);
    }

    /**
     * Get a property whose value is of type InetAddress from the
     * slice's property list.
     *
     * @param name property name
     * @param name DOCUMENT ME!
     * @param required true iff property is required
     *
     * @return DOCUMENT ME!
     *
     * @throws exception if property is malformed, or if required and missing
     */
    public static InetAddress getInetProperty(Properties properties, String name, boolean required)
                                       throws Exception
    {
        InetAddress addr = null;

        String s = properties.getProperty(name);

        if (s == null) {
            if (required) {
                throw new Exception("Missing property " + name);
            }
        } else {
            addr = InetAddress.getByName(s);
        }

        return addr;
    }

    public static int getIntegerProperty(Properties properties, String name)
    {
        return getIntegerProperty(properties, name, false);
    }

    public static int getIntegerProperty(Properties properties, String name, boolean required)
    {
        String temp = getProperty(properties, name, required);

        if (temp == null) {
            return DefaultIntegerValue;
        } else {
            try {
                return Integer.parseInt(temp);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Gets an integer property from a property list.
     *
     * @param p
     * @param descr
     * @param pname
     *
     * @return 0 if property does not exist, otherwise the integer property
     *
     * @throws Exception
     */
    public static int getIntProperty(Properties p, String descr, String pname)
                              throws Exception
    {
        int i = 0;
        String s = p.getProperty(pname);

        if (s == null) {
            return 0;
        }

        try {
            Integer bigi = new Integer(s);
            i = bigi.intValue();
        } catch (Exception e) {
            throw new Exception(
                "invalid integer value for property " + pname + " for " + descr + ": " +
                e.toString());
        }

        return i;
    }

    public static long getLongProperty(Properties properties, String name)
                                throws Exception
    {
        return getLongProperty(properties, name, false);
    }

    public static long getLongProperty(Properties properties, String name, boolean required)
                                throws Exception
    {
        String temp = getProperty(properties, name, required);

        if (temp == null) {
            return DefaultLongValue;
        } else {
            return Long.parseLong(temp);
        }
    }

    public static Properties getPropertiesProperty(Properties properties, String name)
                                            throws Exception
    {
        return getPropertiesProperty(properties, name, false);
    }

    public static Properties getPropertiesProperty(Properties properties, String name,
                                                   boolean required) throws Exception
    {
        String temp = getProperty(properties, name, required);

        if (temp == null) {
            return null;
        } else {
            return Serializer.toProperties(temp);
        }
    }

    public static String getProperty(Properties properties, String name)
    {
        if (properties != null && name != null) {
            return properties.getProperty(name);
        }
        return null;
    }

    public static String getProperty(Properties properties, String name, boolean required)
    {
        String result = null;

        if (properties != null) {
            result = properties.getProperty(name);

            if (required && (name == null)) {
                throw new RuntimeException("Missing property: " + name);
            }
        }

        return result;
    }

    /**
     * Gets a required integer property from a property list, and
     * throws a meaningful exception if it is not present or if it is
     * malformed.
     *
     * @param p
     * @param descr
     * @param pname
     *
     * @return 0 if no property, otherwise the integer property
     *
     * @throws Exception
     */
    public static int getReqIntProperty(Properties p, String descr, String pname)
                                 throws Exception
    {
        int i = 0;
        String s = getReqProperty(p, descr, pname);

        try {
            Integer bigi = new Integer(s);
            i = bigi.intValue();
        } catch (Exception e) {
            throw new Exception(
                "invalid integer value for required property " + pname + " for " + descr + ": " +
                e.toString());
        }

        return i;
    }

    /**
     * Gets a required property from a property list, and throws a
     * meaningful exception if it is not present.
     *
     * @param p
     * @param descr
     * @param pname
     *
     * @return string of the required property
     *
     * @throws Exception
     */
    public static String getReqProperty(Properties p, String descr, String pname)
                                 throws Exception
    {
        String s = p.getProperty(pname);

        if (s == null) {
            String err = descr + " is missing a required property: " + pname;
            throw new Exception(err);
        }

        return s;
    }

    public static boolean getRequiredBooleanProperty(Properties properties, String name)
    {
        return getBooleanProperty(properties, name, true);
    }

    public static double getRequiredDoubleProperty(Properties properties, String name)
                                            throws Exception
    {
        return getDoubleProperty(properties, name, true);
    }

    public static InetAddress getRequiredInetProperty(Properties properties, String name)
                                               throws Exception
    {
        return getInetProperty(properties, name, true);
    }

    public static int getRequiredIntegerProperty(Properties properties, String name)
                                          throws Exception
    {
        return getIntegerProperty(properties, name, true);
    }

    public static long getRequiredLongProperty(Properties properties, String name)
                                        throws Exception
    {
        return getLongProperty(properties, name, true);
    }

    public static Properties getRequiredPropertiesProperty(Properties properties, String name)
                                                    throws Exception
    {
        return getPropertiesProperty(properties, name, true);
    }

    public static String getRequiredProperty(Properties properties, String name)
                                      throws Exception
    {
        return getProperty(properties, name, true);
    }

    public static String[] getStringArrayProperty(Properties p, String name)
    {
        String[] result = null;

        if ((p != null) && (name != null)) {
            String temp = p.getProperty(name);

            if (temp != null) {
                result = temp.split(",");
            }
        }

        return result;
    }

    public static Properties map2Properties(Map<?, ?> h)
    {
        Set<?> s = h.keySet();
        Iterator<?> i = s.iterator();
        Properties p = new Properties();

        while (i.hasNext()) {
            Object key = i.next();
            Object value = h.get(key);

            if (value instanceof Integer) {
                value = value.toString();
            }

            if ((key instanceof String) && (value instanceof String)) {
                p.put(key, value);
            } else {
                return null;
            }
        }

        return p;
    }

    /**
     * Unsets properties that are present in unset the the properties
     * list passed as to #list
     * @param unset list containing properties to unset
     * @param list list of properties to operate on
     */
    public static void unsetProperties(Properties unset, Properties list)
    {
        if (unset == null || list == null){
            return;
        }
        
        Enumeration<?> e = unset.propertyNames();

        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            list.remove(name);
        }
    }
    
    public static void mergeProperties(Properties from, Properties to)
    {
        // no merging if the same list
        if (from == to) {
            return;
        }

        if ((from != null) && (to != null)) {
            Enumeration<?> e = from.propertyNames();

            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                to.setProperty(name, from.getProperty(name));
            }
        }
    }

    /**
     * Merge two non-null lists (if either is null, nothing happens)
     * @param from
     * @param to
     */
    public static void mergePropertiesPriority(Properties from, Properties to)
    {
        // no merging if the same list
        if (from == to) {
            return;
        }

        if ((from != null) && (to != null)) {
            Enumeration<?> e = from.propertyNames();

            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();

                if (!to.contains(name)) {
                    to.setProperty(name, from.getProperty(name));
                }
            }
        }
    }
    
    /**
     * Reads the value of the given property. It is expected that the
     * value is of boolean type.
     *
     * @param p
     * @param property
     *
     * @return
     *
     * @throws Exception if the value of the given property cannot be
     *         recognized as a valid boolean
     */
    public static boolean readBooleanProperty(Properties p, String property)
                                       throws Exception
    {
        boolean result = false;

        if (p != null) {
            String temp = p.getProperty(property);

            if (temp != null) {
                Boolean b = new Boolean(temp);
                result = b.booleanValue();
            }
        }

        return result;
    }

    /**
     * Saves an int value in the prop list
     *
     * @param p
     * @param value
     * @param descr
     * @param pname
     *
     * @return an integer of the return from setProperty, 0 otherwise
     *
     * @throws Exception
     */
    public static int saveIntProperty(Properties p, int value, String descr, String pname)
                               throws Exception
    {
        Integer bigi = new Integer(value);
        Object oldi = p.setProperty(pname, bigi.toString());

        if (oldi != null) {
            return new Integer((String) oldi).intValue();
        } else {
            return 0;
        }
    }

    /**
     * Set a boolean property to true.
     *
     * @param p The properties list
     * @param property The property name
     */
    public static void setBooleanProperty(Properties p, String property)
    {
        setBooleanProperty(p, property, true);
    }

    /**
     * Set a boolean property
     *
     * @param p The properties list
     * @param property The property name
     * @param value True/False
     */
    public static void setBooleanProperty(Properties p, String property, boolean value)
    {
        if (p != null) {
            if (value) {
                p.setProperty(property, TRUE);
            } else {
                p.setProperty(property, FALSE);
            }
        }
    }

    public static void setProperty(Properties p, String name, boolean value)
    {
        setBooleanProperty(p, name, value);
    }

    public static void setProperty(Properties p, String name, double value)
    {
        p.setProperty(name, Double.toString(value));
    }

    public static void setProperty(Properties p, String name, int value)
    {
        p.setProperty(name, Integer.toString(value));
    }

    public static void setProperty(Properties p, String name, Integer value)
    {
        if (value != null) {
            p.setProperty(name, value.toString());
        }
    }

    public static void setProperty(Properties p, String name, long value)
    {
        p.setProperty(name, Long.toString(value));
    }

    public static void setProperty(Properties p, String name, Properties value)
    {
        p.setProperty(name, Serializer.toString(value));
    }

    /*
     * See also getInetProperty in cod.CodSlice.
     */
    public static void setProperty(Properties p, String name, String value)
    {
        if ((value != null) && (p != null) && (name != null)) {
            p.setProperty(name, value);
        }
    }

    public static void setProperty(Properties p, String name, ID value)
    {
        if ((value != null) && (p != null) && (name != null)) {
            p.setProperty(name, value.toString());
        }    
    }
    
    public static ID getIDProperty(Properties properties, String name)
    {
        return getIDProperty(properties, name, false);
    }

    public static ID getIDProperty(Properties properties, String name, boolean required)
    {
        String temp = getProperty(properties, name, required);

        if (temp == null){
            if (required){
                throw new RuntimeException("Missing required property: " + name);
            } else {
                return null;
            }
        } 
        
        return new ID(temp);
    }

    
    public static void setProperty(Properties p, String name, String[] values)
    {
        if ((values != null) && (values.length > 0) && (name != null) && (p != null)) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }

                sb.append(values[i]);
            }

            p.setProperty(name, sb.toString());
        }
    }

    /**
     * Set a string property
     *
     * @param p The properties list
     * @param property The property name
     * @param value True/False
     */
    public static void setReqProperty(Properties p, String property, String value)
    {
        if (p != null) {
            p.setProperty(property, value);
        }
    }

    /**
     * Deserializes a string to a properties list
     *
     * @param s The string representation of this properties list
     *
     * @return The resulting properties list
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
     * Converts a string to a properties list
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
    public static String toString(Properties properties) throws Exception
    {
        if ((properties != null) && (properties.size() > 0)) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            properties.store(stream, null);

            return stream.toString();
        } else {
            return "";
        }
    }

    /**
     * Converts a properties list to a string
     *
     * @param p
     *
     * @return
     */
    public static String toString2(Properties p)
    {
        StringBuffer sb = new StringBuffer();
        Iterator<Map.Entry<Object,Object>> i = p.entrySet().iterator();
        int index = 0;

        while (i.hasNext()) {
            Map.Entry<Object,Object> entry = i.next();
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
    
    /**
     * Find the highest index of the property name of a pattern 
     * keyStartWith.[index]. Character '.' can be used as a separator
     * inside the keyStartsWith pattern.
     * @param p
     * @param keyStartsWith
     * @return
     */
    public static int highestModifyIndex(Properties p, String keyStartsWith) {
        int highestIndex = 0;
        int countSeparators = keyStartsWith.split("\\.").length;
        for(String key : p.stringPropertyNames()) {        	
        	if(key.startsWith(keyStartsWith)){
        		String indexString = key.split("\\.")[countSeparators];
        		int index = Integer.parseInt(indexString);
        		if(index >= highestIndex){
        			highestIndex = index;
        		}
        	}
        }
        return highestIndex;
    }
    
    /**
     * Prepend string to all property names
     * @param p
     * @param prefix
     */
    public static void renamePropertyNames(Properties p, String prefix) {
    	Iterator<Map.Entry<Object,Object>> i = p.entrySet().iterator();

    	List<String> toRemove = new ArrayList<String>();
    	Properties tmp = new Properties();
        while (i.hasNext()) {
            Map.Entry<Object,Object> entry = i.next();
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();
            tmp.setProperty(prefix + name, value);
            toRemove.add(name);
        }
        i = tmp.entrySet().iterator();
        while(i.hasNext()) {
            Map.Entry<Object,Object> entry = i.next();
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();
            p.setProperty(name, value);
        }
        for(String rem: toRemove) {
        	p.remove(rem);
        }
    }
}