/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.core;

import net.exogeni.orca.shirako.kernel.ResourceSet;

import net.exogeni.orca.util.PropList;

import java.util.Properties;


public class PropertiesManager
{
    /**
     * The resource request is flexible in size.
     */
    public static final String ElasticSize = "request.elasticSize";

    /**
     * The resource request is flexible in time.
     *
     */
    public static final String ElasticTime = "request.elasticTime";

    protected static Properties getRequestProperties(ResourceSet set, boolean create)
    {
        Properties p = set.getRequestProperties();

        if ((p == null) && create) {
            p = new Properties();
            set.setRequestProperties(p);
        }

        return p;
    }

    /**
     * Marks this resource set to indicate that the request that it represents
     * is elastic in size (can use less units than requested)
     * @param set set
     * @param value value
     */
    public static void setElasticSize(ResourceSet set, boolean value)
    {
        PropList.setBooleanProperty(getRequestProperties(set, true), ElasticSize, value);
    }

    /**
     * Checks if the request represented by this resource set is elastic in size
     * @param set set
     * @return true or false
     */
    public static boolean isElasticSize(ResourceSet set)
    {
        boolean result = false;
        Properties p = set.getRequestProperties();

        if (p != null) {
            try {
                result = PropList.getBooleanProperty(p, ElasticSize);
            } catch (Exception e) {
                result = false;
            }
        }

        return result;
    }

    /**
     * Marks this resource set to indicate that the request that it represents
     * is elastic in size (can use less units than requested)
     * @param set set
     * @param value value
     */
    public static void setElasticTime(ResourceSet set, boolean value)
    {
        PropList.setBooleanProperty(getRequestProperties(set, true), ElasticTime, value);
    }

    /**
     * Checks if the request represented by this resource set is elastic in size
     * @param set set
     * @return true or false
     */
    public static boolean isElasticTime(ResourceSet set)
    {
        boolean result = false;
        Properties p = set.getRequestProperties();

        if (p != null) {
            try {
                result = PropList.getBooleanProperty(p, ElasticTime);
            } catch (Exception e) {
                result = false;
            }
        }

        return result;
    }
}
