/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.extensions.internal;

import orca.extensions.PackageId;
import orca.extensions.PluginId;
import orca.util.PropList;

import java.util.Properties;


/**
 * The <code>Plugin</code> class describes metadatata about an plugin supplied
 * by an extension package.
 */
public class Plugin
{
    /*
     * Serialization/deserialization constants.
     */
    public static final String PropertyPluginId = "PluginId";
    public static final String PropertyPackageId = "PackageId";
    public static final String PropertyPluginType = "PluginType";
    public static final String PropertyFactory = "PluginFactory";
    public static final String PropertyName = "PluginName";
    public static final String PropertyDescription = "PluginDesc";
    public static final String PropertyClassName = "PluginClass";
    public static final String PropertyConfigProperties = "PluginProperties";
    public static final String PropertyConfigTemplate = "PluginTemplate";
    public static final String PropertyPortalLevel = "PluginPortalLevel";
    public static final String PropertyActorType = "PluginActorType";

    /*
     * Plugin types.
     */
    public static final int TypeAll = 0;

    /**
     * Specifies a plugin that provides a new actor implementation.
     */
    public static final int TypeActorObject = 1;

    /**
     * Specified a plugin that provides a new policy implementation.
     */
    public static final int TypePolicy = 2;

    /**
     * Specifies a plugin that provides a new manager object implementation.
     */
    public static final int TypeManagerObject = 3;

    /**
     * Specifies a plugin that provides a new portal plugin implementation.
     */
    public static final int TypePortalPlugin = 4;

    /**
     * Specifies a plugin that provides a new actor controller implementation.
     */
    public static final int TypeActorController = 5;

    /**
     * Specified a new plugin that provides an new application implementation.
     */
    public static final int TypeApplicationController = 6;

    /**
     * Specified a new plugin that provides a new workload implementation.
     */
    public static final int TypeWorkloadController = 7;

    /**
     * Specified a new plugin that provides a new site control implementation.
     */
    public static final int TypeSiteControl = 8;
    /**
     * Specifies a handler support library: not a real handler.
     */
    public static final int TypeHandlerSupportLibrary = 9;
    
    /**
     * Specifies a configuration handler.
     */
    public static final int TypeHandler = 10;
    
    /*
     * Level at which the portal plugin must be instantiated.
     */
    public static final int PortalPluginLevelUnknown = 0;
    public static final int PortalPluginLevelTab = 1;
    public static final int PortalPluginLevelActor = 2;
    public static final int PortalPluginLevelSlice = 3;

    /**
     * Plugin property specifying the handler file.
     */
    public static final String PluginPropertyHandlerFile = "handler.file";
    /**
     * Identifier assigned to this plugin.
     */
    protected PluginId id;

    /**
     * Identifier of the containing package.
     */
    protected PackageId packageId;

    /**
     * Type of this plugin: see Type* constants.
     */
    protected int pluginType;

    /**
     * If true, this object is a factory that creates the plugin object.
     */
    protected boolean factory = false;

    /**
     * Human-readable name.
     */
    protected String name = null;

    /**
     * Plugin description.
     */
    protected String description = null;

    /**
     * Class implementing this plugin.
     */
    protected String className = null;

    /**
     * Configuration properties to be passed to the plugin's
     * <code>configure(Properties)</code>method.
     */
    protected Properties configProperties = null;

    /**
     * Velocity template to be used to configure this object.
     */
    protected String configTemplate = null;

    /**
     * The level at which this portal plugin should be activated (applies only
     * to portal plugins).
     */
    protected int portalLevel = PortalPluginLevelUnknown;

    /**
     * Actor type required by this plugin.
     */
    protected int actorType = 0;

    /**
     * Serializes th object into a properties list.
     * @return properties list representing the object
     * @throws Exception
     */
    public Properties save() throws Exception
    {
        Properties p = new Properties();
        save(p);

        return p;
    }

    /**
     * Serializes the object into the given properties list.
     * @param p properties list
     * @throws Exception
     */
    public void save(Properties p) throws Exception
    {
        PropList.setProperty(p, PropertyPluginId, id.toString());
        PropList.setProperty(p, PropertyPackageId, packageId.toString());
        PropList.setProperty(p, PropertyPluginType, pluginType);
        PropList.setProperty(p, PropertyFactory, factory);
        PropList.setProperty(p, PropertyName, name);
        PropList.setProperty(p, PropertyDescription, description);
        PropList.setProperty(p, PropertyClassName, className);
        PropList.setProperty(p, PropertyConfigProperties, configProperties);
        PropList.setProperty(p, PropertyConfigTemplate, configTemplate);
        PropList.setProperty(p, PropertyPortalLevel, portalLevel);
        PropList.setProperty(p, PropertyActorType, actorType);
    }

    /**
     * Deserializes the object from the given properties list.
     * @param p properties list
     * @throws Exception
     */
    public void reset(Properties p) throws Exception
    {
        id = new PluginId(p.getProperty(PropertyPluginId));
        packageId = new PackageId(p.getProperty(PropertyPackageId));
        pluginType = PropList.getIntegerProperty(p, PropertyPluginType);
        factory = PropList.getBooleanProperty(p, PropertyFactory);
        name = PropList.getProperty(p, PropertyName);
        description = PropList.getProperty(p, PropertyDescription);
        className = PropList.getProperty(p, PropertyClassName);
        configProperties = PropList.getPropertiesProperty(p, PropertyConfigProperties);
        configTemplate = PropList.getProperty(p, PropertyConfigTemplate);
        portalLevel = PropList.getIntegerProperty(p, PropertyPortalLevel);
        actorType = PropList.getIntegerProperty(p, PropertyActorType);
    }

    /**
     * Checks if this plugin is a factory.
     * @return true if the plugin represents a factory
     */
    public boolean isFactory()
    {
        return this.factory;
    }

    /**
     * Sets the factory flag. A plugin is a factory if it is used to create the
     * actual plugin rather than it being the plugin itself.
     * @param factory true|false
     */
    public void setFactory(boolean factory)
    {
        this.factory = factory;
    }

    /**
     * Returns the plugin identifier.
     * @return the id
     */
    public PluginId getId()
    {
        return this.id;
    }

    /**
     * @param id the id to set
     */
    public void setId(PluginId id)
    {
        this.id = id;
    }

    /**
     * @return the packageId
     */
    public PackageId getPackageId()
    {
        return this.packageId;
    }

    /**
     * @param packageId the packageId to set
     */
    public void setPackageId(PackageId packageId)
    {
        this.packageId = packageId;
    }

    /**
     * @return the pluginType
     */
    public int getPluginType()
    {
        return this.pluginType;
    }

    /**
     * @param pluginType the pluginType to set
     */
    public void setPluginType(int pluginType)
    {
        this.pluginType = pluginType;
    }

    /**
     * @return the className
     */
    public String getClassName()
    {
        return this.className;
    }

    /**
     * @param className the className to set
     */
    public void setClassName(String className)
    {
        this.className = className;
    }

    /**
     * @return the configProperties
     */
    public Properties getConfigProperties()
    {
        return this.configProperties;
    }

    /**
     * @param configProperties the configProperties to set
     */
    public void setConfigProperties(Properties configProperties)
    {
        this.configProperties = configProperties;
    }

    /**
     * @return the configTemplate
     */
    public String getConfigTemplate()
    {
        return this.configTemplate;
    }

    /**
     * @param configTemplate the configTemplate to set
     */
    public void setConfigTemplate(String configTemplate)
    {
        this.configTemplate = configTemplate;
    }

    /**
     * @return the description
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the actorType
     */
    public int getActorType()
    {
        return this.actorType;
    }

    /**
     * @param actorType the actorType to set
     */
    public void setActorType(int actorType)
    {
        this.actorType = actorType;
    }

    /**
     * @return the portalLevel
     */
    public int getPortalLevel()
    {
        return this.portalLevel;
    }

    /**
     * @param portalLevel the portalLevel to set
     */
    public void setPortalLevel(int portalLevel)
    {
        this.portalLevel = portalLevel;
    }
}