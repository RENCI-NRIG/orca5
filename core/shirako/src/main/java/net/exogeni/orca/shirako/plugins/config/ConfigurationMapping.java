/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.plugins.config;

import java.util.Properties;

import net.exogeni.orca.util.PropList;
import net.exogeni.orca.util.persistence.Persistable;
import net.exogeni.orca.util.persistence.PersistenceUtils;
import net.exogeni.orca.util.persistence.Persistent;


/**
 * A configuration mapping record represents a mapping between a resource pool and a configuration handler.
 * In its current version, each resource pool can be associated with exactly one configuration handler.
 * The handler may have specific configuration properties, which must be passed down to the handler during each
 * invocation.
 */
public class ConfigurationMapping implements Persistable
{
    public static final String PropertyType = "mapping.type";
    public static final String PropertyFile = "mapping.file";
    public static final String PropertyProperties = "mapping.properties";
    
    /**
     * Creates a new instance from a previously serialized instance.
     * @param p properties list containing a serialized mapping
     * @return new mapping instance
     * @throws Exception in case of error
     */
    public static ConfigurationMapping newInstance(final Properties p) throws Exception
    {
    	return PersistenceUtils.restore(p);
    }

    
    /**
     * The mapping key.
     */
    @Persistent (key = PropertyType)
    protected String type;    
    /**
     * Path to the handler file.
     */
    @Persistent (key = PropertyFile)
    protected String configFile;
    
    /**
     *  Properties to be passed to the handler.
     */
    @Persistent (key = PropertyProperties)
    protected Properties properties;

    /**
     * Creates a new instance.
     */
    public ConfigurationMapping()
    {
    }

    /**
     * Returns the path to the handler file.
     * @return path to the handler file
     */
    public String getConfigFile()
    {
        return configFile;
    }

    /**
     * Returns the mapping key.
     * @return mapping key
     */
    public String getKey()
    {
        return type;
    }

    /**
     * Returns the handler properties (by reference).
     * @return properties
     */
    public Properties getProperties()
    {
        return properties;
    }

    /**
     * Sets the path to the handler file.
     * @param configFile path to the handler 
     */
    public void setConfigFile(final String configFile)
    {
        this.configFile = configFile;
    }

    /**
     * Sets the mapping key.
     * @param key mapping key
     */
    public void setKey(final String key)
    {
        this.type = key;
    }
    
    /**
     * Sets the handler properties.
     * @param p handler properties
     */
    public void setProperties(final Properties p)
    {
        this.properties = p;
    }
}
