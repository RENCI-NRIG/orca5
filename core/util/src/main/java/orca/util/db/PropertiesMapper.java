/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.util.db;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 * <code>PropertiesMapper</code> provides helper methods to implement converters
 * from and to Java properties. Each mapper maintains two top-level maps: one
 * for each direction (to java and from java). Each map consists of multiple
 * maps grouped by category. The map for a category maps a property name to a
 * set of property names. Map entries can indicate that a property should be
 * ignored during mapping or that the proeprty's presence is required.
 */
public abstract class PropertiesMapper
{
    /**
     * Used to represent the mapping for a given category.
     */
    protected class MapEntry
    {
        /**
         * Key map.
         */
        public Hashtable<String, MapEntryItem> map;

        public MapEntry()
        {
            map = new Hashtable<String, MapEntryItem>();
        }
    }

    /**
     * Internal class to represent the entries being mapped to a given key.
     */
    protected class MapEntryItem
    {
        /**
         * All entries being mapped to the key associated with this entry.
         */
        public ArrayList<String> entries;

        /**
         * If true, the key will be ignored and not mapped to entries.
         */
        public boolean ignore;

        /**
         * If true, the key is required.
         */
        public boolean required;

        /**
         * Creates a new instance.
         * @param required does the instance represent a required key
         * @param ignore does the instance represent a key that should be
         *            ignored
         */
        public MapEntryItem(final boolean required, final boolean ignore)
        {
            this.required = required;
            this.ignore = ignore;
            entries = new ArrayList<String>();
        }
    }

    /**
     * Maps a string type to the corresponding map. Used when converting to
     * Java.
     */
    protected Hashtable<String, MapEntry> fromJava;

    /**
     * Maps a string type to the corresponding map. Used when converting from
     * Java.
     */
    protected Hashtable<String, MapEntry> toJava;

    /**
     * Logger.
     */
    protected Logger logger;

    /**
     * Empty map: used to simplify coding.
     */
    protected MapEntry emptyMap;

    /**
     * If true, failure to resolve a class field will propagate.
     */
    protected boolean failOnError = false;

    /**
     * Initialization flag
     */
    private boolean initialized = false;

    /**
     * Creates a new instance.
     */
    protected PropertiesMapper()
    {
        logger = Logger.getLogger(this.getClass().getCanonicalName());
        fromJava = new Hashtable<String, MapEntry>();
        toJava = new Hashtable<String, MapEntry>();
    }

    /**
     * Creates the mapping.
     * @throws Exception in case of error
     */
    protected abstract void createMapping() throws Exception;

    /**
     * Returns the value of the specified static string field.
     * @param str fully qualified field name (package.class.field)
     * @return field value
     */
    protected String getClassField(final String str)
    {
        String result = null;

        try {
            int index = str.lastIndexOf(".");
            String className = str.substring(0, index);
            String fieldName = str.substring(index + 1);

            Class<?> c = Class.forName(className);
            Field field = c.getField(fieldName);

            try {
                result = (String) field.get(null);
            } catch (NullPointerException e) {
                Object obj = c.newInstance();
                result = (String) field.get(obj);
            }
        } catch (Exception e) {
            logger.error("Problems resolving " + str, e);
            result = str;

            if (failOnError) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    /**
     * Returns the specified category map (from Java).
     * @param category category
     * @return category map
     * @throws RuntimeException in case of error
     */
    protected MapEntry getFromJava(final String category) throws RuntimeException
    {
        MapEntry map = null;

        if (category == null) {
            map = emptyMap;
        } else {
            map = fromJava.get(category);
        }

        if (map == null) {
            throw new RuntimeException("Unsupported category: " + category);
        }

        return map;
    }

    /**
     * Returns the specified category map (to Java).
     * @param category category
     * @return category map
     * @throws RuntimeException in case of error
     */
    protected MapEntry getToJava(final String category) throws RuntimeException
    {
        MapEntry map = null;

        if (category == null) {
            map = emptyMap;
        } else {
            map = toJava.get(category);
        }

        if (map == null) {
            throw new RuntimeException("Unsupported category: " + category);
        }

        return map;
    }

    /**
     * Parses the map file and creates the mapping.
     * @throws Exception in case of error
     */
    public void initialize() throws Exception
    {
        if (!initialized) {
            createMapping();
            initialized = true;
        }
    }

    /**
     * Sets the fail on error flag. If true, failures to resolve a mapping will
     * propagate up the call stack. This method is intended primarily for
     * testing. The default value of the flag is false.
     * @param value flag value
     */
    public void setFailOnError(final boolean value)
    {
        this.failOnError = value;
    }
}
