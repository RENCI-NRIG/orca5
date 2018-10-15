/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.util.db;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;

import net.exogeni.orca.util.Serializer;
import net.exogeni.orca.util.db.beans.Mapping;


/**
 * Maps java property names to MySQL table attributes and vise versa.
 */
public class MySqlPropertiesMapper extends PropertiesMapper
{
    /**
     * Name for the shirako properties field.
     */
    public static final String PropertyShirakoProperties = "shirakoproperties";

    /**
     * Location of the the map file.
     */
    protected String mysqlMapUrl;

    /**
         * Creates a new instance.
         * @param location mapping file location
         */
    public MySqlPropertiesMapper(final String location)
    {
        mysqlMapUrl = location;
    }

    /**
     * {@inheritDoc}
     */
    protected void createMapping() throws Exception
    {
        // read the mapping
        Mapping map = readMysqlMapping();

        // empty map?
        if ((map.getMysql() == null) || (map.getMysql().getEntry() == null)) {
            return;
        }

        Iterator<?> iter = map.getMysql().getEntry().iterator();

        while (iter.hasNext()) {
            net.exogeni.orca.util.db.beans.Mysqlentry entry = (net.exogeni.orca.util.db.beans.Mysqlentry) iter.next();

            MapEntry eMysqlToJava = new MapEntry();
            MapEntry eJavaToMysql = new MapEntry();

            // load the mapping
            Iterator<?> i = entry.getAttributes().getMap().iterator();

            while (i.hasNext()) {
                net.exogeni.orca.util.db.beans.Map m = (net.exogeni.orca.util.db.beans.Map) i.next();

                if (m.isIgnore()) {
                    continue;
                }

                String from = m.getFrom();
                String to = m.getTo();

                if (!to.equals("")) {
                    to = getClassField(to);
                }

                MapEntryItem mei = null;

                if (!from.equals("")) {
                    // mysql to java
                    mei = (MapEntryItem) eMysqlToJava.map.get(from);

                    if (mei == null) {
                        mei = new MapEntryItem(m.isRequiredFrom(), m.isIgnoreFrom());
                        eMysqlToJava.map.put(from, mei);
                    }

                    if ((to != null) && !to.equals("")) {
                        mei.entries.add(to);
                    }
                }

                if ((to != null) && !to.equals("")) {
                    mei = (MapEntryItem) eJavaToMysql.map.get(to);

                    if (mei == null) {
                        mei = new MapEntryItem(m.isRequiredTo(), m.isIgnoreTo());
                        eJavaToMysql.map.put(to, mei);
                    }

                    if (!from.equals("")) {
                        mei.entries.add(from);
                    }
                }
            }

            toJava.put(entry.getName(), eMysqlToJava);
            fromJava.put(entry.getName(), eJavaToMysql);
        }
    }

    /**
     * Translates java to MySql properties. Translates all properties
     * for which there is a defined mapping. All properties without a defined
     * mapping are serialized to a string and placed under the property
     * PropertyShirakoProperties.
     *
     * @param category category
     * @param p Properties describing the object
     *
     * @return converted properties
     *
     * @throws Exception in case of error
     */
    public Properties javaToMysql(final String category, final Properties p)
                           throws Exception
    {
        Properties undefined = new Properties();
        Properties mysqlProperties = new Properties();

        // obtain the map for this object type
        MapEntry map = getFromJava(category);

        // go thorough all properties
        Iterator<?> iter = p.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();

            // is there a mapping?
            MapEntryItem item = (MapEntryItem) map.map.get(name);

            if (item != null) {
                if (item.ignore) {
                    continue;
                }

                // escate single quotes
                value = value.replace("'", "\\'");

                for (int i = 0; i < item.entries.size(); i++) {
                    String newName = (String) item.entries.get(i);
                    mysqlProperties.put(newName, value);
                }
            } else {
                undefined.setProperty(name, value);
            }
        }

        // attach the properties with no mapping
        if (undefined.size() > 0) {
            String temp = Serializer.toString(undefined);
            temp = temp.replace("\\", "\\\\");
            temp = temp.replace("'", "\\'");
            mysqlProperties.put(PropertyShirakoProperties, temp);
        }

        return mysqlProperties;
    }

    /**
     * Converts mysql properties to java properties.
     *
     * @param category category
     * @param mysqlProperties mysql properties
     *
     * @return java properties
     *
     * @throws Exception in case of error
     */
    public Properties mysqlToJava(String category, Properties mysqlProperties)
                           throws Exception
    {
        // obtain the mapping
        MapEntry map = getToJava(category);
        Properties p = null;

        // check for shirakoProperties
        String tmp = mysqlProperties.getProperty(PropertyShirakoProperties);

        if (tmp != null) {
            // System.out.println("*******");
            // System.out.println(tmp);
            p = Serializer.toProperties(tmp);
            mysqlProperties.remove(PropertyShirakoProperties);
        } else {
            p = new Properties();
        }

        Iterator<?> iter = mysqlProperties.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<?, ?> a = (Map.Entry<?, ?>) iter.next();
            String name = a.getKey().toString();
            String value = a.getValue().toString();

            // obtain the mapping
            MapEntryItem item = (MapEntryItem) map.map.get(name);

            if (item != null) {
                if (item.ignore) {
                    continue;
                }

                for (int i = 0; i < item.entries.size(); i++) {
                    String newName = (String) item.entries.get(i);
                    p.setProperty(newName, value);
                }
            } else {
                p.setProperty(name, value);
            }
        }

        return p;
    }

    /**
     * Reads the configuration map.
     *
     * @return map
     *
     * @throws IOException in case of error
     * @throws JAXBException in case of error
     */
    protected Mapping readMysqlMapping() throws IOException, JAXBException
    {
        logger.debug("Reading MySql mapping file from " + mysqlMapUrl);
    	URL url = this.getClass().getClassLoader().getResource(mysqlMapUrl);
        if (url == null) {
            throw new FileNotFoundException("Cannot find configuration mapping file: " + mysqlMapUrl);
        }

        JAXBContext context = JAXBContext.newInstance("net.exogeni.orca.util.db.beans");
        Unmarshaller um = context.createUnmarshaller();
        InputStream is = url.openStream();

        return (Mapping) um.unmarshal(is);
    }
}
