/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.policy.core.util;

import net.exogeni.orca.util.PropList;

import java.util.Properties;


/**
 * Base Service Manager bidding policy. Use as a base class - extend to add
 * policies for a SM <br>
 */
public abstract class ResourceProperties
{
    public static final String PropertyPartitionable = "partitionable";
    public static final String PropertyCpu = "cpu";
    public static final String PropertyMemory = "memory";
    public static final String PropertyBandwidth = "bandwidth";
    public static final String PropertyStorage = "storage";
    public static final String Elastic = "elastic";
    public static final String PropertyTime = "time";
    public static final String PropertyUnits = "units";
    public static final String Min = "min";
    public static final String Max = "max";

    public static void setElastic(Properties p, String property, boolean value)
    {
        PropList.setProperty(p, property + "." + Elastic, value);
    }

    public static boolean isElastic(Properties p, String property) throws Exception
    {
        return PropList.getBooleanProperty(p, property + "." + Elastic);
    }

    public static void setMin(Properties p, String property, int value)
    {
        PropList.setProperty(p, property + "." + Min, value);
    }

    public static int getMin(Properties p, String property) throws Exception
    {
        return PropList.getIntegerProperty(p, property + "." + Min);
    }

    public static void setMax(Properties p, String property, int value)
    {
        PropList.setProperty(p, property + "." + Max, value);
    }

    public static int getMax(Properties p, String property) throws Exception
    {
        return PropList.getIntegerProperty(p, property + "." + Max);
    }

    public static boolean containsMax(Properties p, String property)
    {
        return (p.containsKey(property + "." + Max));
    }

    public static boolean containsMin(Properties p, String property)
    {
        return (p.containsKey(property + "." + Min));
    }
}
