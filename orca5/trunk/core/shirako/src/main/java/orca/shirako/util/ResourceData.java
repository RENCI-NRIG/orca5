/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.util;


import java.util.Properties;

import orca.util.PropList;
import orca.util.persistence.Persistable;
import orca.util.persistence.Persistent;


/**
 * A <code>ResourceData</code> contains several collections of properties
 * describing resources. Some of these collections are passed between actors
 * during calls.
 */
public class ResourceData implements Cloneable, Persistable
{
    public static final String PropertyLocalProperties = "local";
    public static final String PropertyRequestProperties = "request";
    public static final String PropertyResourceProperties = "resource";
    public static final String PropertyConfigurationProperties = "config";

    /**
     * Merges both properties lists. Elements in from overwrite
     * elements in two.
     *
     * @param from from list
     * @param to to list
     */
    public static void mergeProperties(final Properties from, final Properties to)
    {
    	PropList.mergeProperties(from, to);
    }
    
    /**
     * Merges both properties lists. Properties in to have priority
     * over properties in from.
     *
     * @param from from list
     * @param to to list
     */
    public static void mergePropertiesPriority(final Properties from, final Properties to)
    {
    	PropList.mergePropertiesPriority(from, to);
    }

    /**
     * Local properties: kept locally.
     */
    @Persistent (key = PropertyLocalProperties)
    private Properties localProperties;

    /**
     * Request properties: sent to brokers.
     */
    @Persistent (key = PropertyRequestProperties)
    private Properties requestProperties;

    /**
     * Resource properties: received from brokers. Describe the
     * resource type.
     */
    @Persistent (key = PropertyResourceProperties)
    private Properties resourceProperties;

    /**
     * Configuration properties: sent to authorities.
     */
    @Persistent (key = PropertyConfigurationProperties)
    private Properties configurationProperties;

    /**
     * Creates a new instance will all properties lists.
     */
    public ResourceData()
    {
        localProperties = new Properties();
        requestProperties = new Properties();
        resourceProperties = new Properties();
        configurationProperties = new Properties();
    }

    /**
         * Copy constructor.
         * @param original original
         */
    private ResourceData(final ResourceData original)
    {
        this.localProperties = (Properties) original.localProperties.clone();
        this.requestProperties = (Properties) original.requestProperties.clone();
        this.resourceProperties = (Properties) original.resourceProperties.clone();
        this.configurationProperties = (Properties) original.configurationProperties.clone();
    }

    /**
     * Makes a clone of this <code>ResourceData</code> object.
     *
     * @return DOCUMENT ME!
     */
    public Object clone()
    {
        return new ResourceData(this);
    }

    /**
     * Returns the configuration properties list.
     *
     * @return configuration properties list
     */
    public Properties getConfigurationProperties()
    {
        return configurationProperties;
    }

    /**
     * Returns the local properties list.
     *
     * @return local properties list
     */
    public Properties getLocalProperties()
    {
        return localProperties;
    }

    /**
     * Returns the request properties list.
     *
     * @return request properties list
     */
    public Properties getRequestProperties()
    {
        return requestProperties;
    }

    /**
     * Returns the resource properties list.
     *
     * @return resource properties list
     */
    public Properties getResourceProperties()
    {
        return resourceProperties;
    }

    /**
     * Merges two instances.
     *
     * @param other other instance
     */
    public void merge(final ResourceData other)
    {
        if (other != null) {
            mergeProperties(other.localProperties, this.localProperties);
            mergeProperties(other.requestProperties, this.requestProperties);
            mergeProperties(other.resourceProperties, this.resourceProperties);
            mergeProperties(other.configurationProperties, this.configurationProperties);
        }
    }
}