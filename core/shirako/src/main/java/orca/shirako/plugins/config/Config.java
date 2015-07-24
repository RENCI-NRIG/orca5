/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.plugins.config;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import orca.shirako.container.Globals;
import orca.shirako.plugins.ShirakoPlugin;
import orca.shirako.util.SemaphoreMap;
import orca.util.Initializable;
import orca.util.OrcaException;
import orca.util.PropList;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistable;
import orca.util.persistence.Persistent;

import org.apache.log4j.Logger;

public class Config implements Initializable, Persistable
{
    /**
     * Specifies a configuration file or action to execute.
     */
    public static final String PropertyConfig = "config.action";

    /**
     * Any property in the ant project file starting with this prefix will be
     * passed back to the java code.
     */
    public static final String PropertyOutputPrefix = "shirako.";
    /**
     * If execution of an action encounters an unhandled exception, this
     * property will contain the exception message.
     */
    public static final String PropertyExceptionMessage = "shirako.exception.message";
    /**
     * If execution of an action encounters an unhandled exception, this
     * property will contain the exception stack.
     */
    public static final String PropertyExceptionStack = "shirako.exception.stack";
    public static final String PropertyTargetName = "shirako.target.name";
    public static final String PropertyTargetResultCode = "shirako.target.code";
    public static final String PropertyTargetResultCodeMessage = "shirako.target.code.message";
    public static final String PropertyConfigurationProperties = "shirako.config";
    /**
     * Properties starting with this prefix are passed back to the java code.
     */
    public static final String PropertySavePrefix = PropertyOutputPrefix + "save.";
    /**
     * Properties starting with this prefix specify a new value for a
     * configuration property. If a handler invocation is successful, the java
     * property corresponding to the configuration property to the right of this
     * prefix is updated to the specified value.
     * 
     * Example: to overwrite the service IP address of a node a handler can specify the following property:<br>
     * <br>
     * shirako.update.unit.net.ip=192.168.2.3
     */
    public static final String PropertyUpdatePrefix = PropertyOutputPrefix + "update.";

    /**
     * Sequence number for the configuration action.
     */
    public static final String PropertyActionSequenceNumber = "action.sequence";
    public static final int ResultCodeException = -1;
    public static final int ResultCodeOK = 0;
    public static final String TargetJoin = "join";
    public static final String TargetLeave = "leave";
    public static final String TargetModify = "modify";
    public static final String PropertyResourceType = "unit.resourceType";  // if you change this, you must also change Unit.PropertyResourceType
    public static final String PropertyUnitAll = "unit.all";
    
    /**
     * For modify sequence number
     */
    
    public static final String PropertyModifyPropertySavePrefix = "shirako.save.unit.modify";
    public static final String PropertyModifySequenceNumber = "shirako.save.unit.modify.sequencenum";
    
    @Persistent
    protected boolean isSynchronous = false;

    @Persistent (reference=true)
    protected ShirakoPlugin plugin;
    @Persistent (reference=true)
    protected Logger logger;
        
    @NotPersistent
    protected boolean initialized = false;
    @NotPersistent
    protected Object actorConfigurationLock = new Object();
    
    // to support start.atomic.sequence/stop.atomic.sequence in handlers 10/30/11 /ib
    @NotPersistent
    protected SemaphoreMap handlerSemaphoreMap = Globals.handlerSemaphoreMap;
    // ... and secure random number generation in tasks
    @NotPersistent
    protected SecureRandom secureRandom = Globals.secureRandom;

    public Config()
    {
    }

    public void initialize() throws OrcaException
    {
        if (!initialized) {
        	if (plugin == null) {
        		throw new OrcaException("Missing plugin");
        	}
            logger = plugin.getLogger();
            initialized = true;
        }
    }


    protected void printProperties(Properties p)
    {
        Iterator i = p.entrySet().iterator();

        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            logger.debug(entry.getKey() + "=" + entry.getValue());
        }
    }

    /**
     * @param token A token to use for the callback
     * @param p
     * @throws Exception
     */
    public void join(ConfigToken token, Properties p) throws Exception
    {
        logger.info("Executing join");
        if (logger.isDebugEnabled()) {
            printProperties(p);
        }

        Properties result = new Properties();
        result.setProperty(Config.PropertyTargetName, TargetJoin);
        result.setProperty(Config.PropertyTargetResultCode, Integer.toString(ResultCodeOK));
        // pass the configuration sequence number back
        PropList.setProperty(result, Config.PropertyActionSequenceNumber, p.getProperty(Config.PropertyActionSequenceNumber));
        // pass the resource type if present
        String rtype = p.getProperty(PropertyResourceType);
        if (rtype != null) {
            result.setProperty(Config.PropertyResourceType, rtype);
        }
        plugin.configurationComplete(token, result);
    }

    public void leave(ConfigToken token, Properties p) throws Exception
    {
        logger.info("Executing leave");

        Properties result = new Properties();
        result.setProperty(Config.PropertyTargetName, TargetLeave);
        result.setProperty(Config.PropertyTargetResultCode, Integer.toString(ResultCodeOK));
        // pass the configuration sequence number back
        PropList.setProperty(result, Config.PropertyActionSequenceNumber, p.getProperty(Config.PropertyActionSequenceNumber));
        // pass the resource type if present
        String rtype = p.getProperty(PropertyResourceType);
        if (rtype != null) {
            result.setProperty(Config.PropertyResourceType, rtype);
        }
        plugin.configurationComplete(token, result);
    }

    public void modify(ConfigToken token, Properties p) throws Exception
    {
        logger.info("Executing modify");

        if (logger.isDebugEnabled()) {
            printProperties(p);
        }

        Properties result = new Properties();
        result.setProperty(Config.PropertyTargetName, TargetModify);
        result.setProperty(Config.PropertyTargetResultCode, Integer.toString(ResultCodeOK));
        // pass the configuration sequence number back
        PropList.setProperty(result, Config.PropertyActionSequenceNumber, p.getProperty(Config.PropertyActionSequenceNumber));
        // pass the resource type if present
        String rtype = p.getProperty(PropertyResourceType);
        if (rtype != null) {
            result.setProperty(Config.PropertyResourceType, rtype);
        }
        plugin.configurationComplete(token, result);
    }

    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }

    public void setSlicesPlugin(ShirakoPlugin plugin)
    {
        this.plugin = plugin;
    }

    public static void setAll(Properties p)
    {
        StringBuffer buffer = new StringBuffer();
        Iterator i = p.entrySet().iterator();

        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();
            buffer.append(name);
            buffer.append("=");
            buffer.append(value);
            buffer.append(" ");
        }

        p.setProperty(PropertyUnitAll, buffer.toString());
    }

    public static void setActionSequenceNumber(Properties p, long sequence)
    {
        if (p == null) {
            throw new IllegalArgumentException();
        }

        PropList.setProperty(p, Config.PropertyActionSequenceNumber, sequence);
    }

    public static long getActionSequenceNumber(Properties p)
    {
        if (p == null) {
            throw new IllegalArgumentException();
        }

        try {
            return PropList.getRequiredLongProperty(p, Config.PropertyActionSequenceNumber);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
