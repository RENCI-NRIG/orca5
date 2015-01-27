/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.extensions.internal;

import java.util.Properties;
import java.util.Vector;

import orca.extensions.PackageId;
import orca.extensions.PluginId;
import orca.extensions.internal.db.IPackageDatabase;
import orca.shirako.container.Globals;

import org.apache.log4j.Logger;

/**
 * The <code>PluginManager</code> is responsible for registering/unregistering
 * plugin descriptors. The <code>PluginManager</code> registers each installed
 * plugin with the backend database.
 * @author aydan
 */
public class PluginManager
{
    /**
     * The database.
     */
    protected IPackageDatabase database;

    /**
     * The logger.
     */
    protected Logger logger;

    /**
     * Initialization status.
     */
    private boolean initialized = false;

    /**
     * Creates new instance.
     */
    public PluginManager()
    {
        this.logger = Globals.getLogger(this.getClass().getCanonicalName());
    }

    /**
     * Returns the specified plugin.
     * @param packageId package identifier
     * @param pluginId plugin identifier
     * @return
     */
    public Plugin getPlugin(PackageId packageId, PluginId pluginId)
    {
        Plugin result = null;

        try {
            Vector<Properties> v = database.getPlugin(packageId, pluginId);

            if ((v != null) && (v.size() > 0)) {
                Properties p = v.get(0);
                result = new Plugin();
                result.reset(p);
            }
        } catch (Exception e) {
            logger.error("getPlugin", e);
        }

        return result;
    }

 

    /**
     * Returns an array of installed plugins.
     * @param packageId package identifier
     * @param type plugin type
     * @param actorType plugin actor type
     * @return
     */
    public Plugin[] getPlugins(PackageId packageId, int type, int actorType)
    {
        Plugin[] result = null;

        try {
            Vector<Properties> v = database.getPlugins(packageId, type, actorType);

            if ((v != null) && (v.size() > 0)) {
                result = new Plugin[v.size()];

                for (int i = 0; i < v.size(); i++) {
                    Properties p = v.get(i);
                    result[i] = new Plugin();
                    result[i].reset(p);
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }

        return result;
    }

    /**
     * Initializes the plugin manager. Called by the management layer.
     * @param manager reference to the management layer
     * @throws Exception
     */
    public void initialize(IPackageDatabase database) throws Exception
    {
        if (database == null) {
            throw new IllegalArgumentException("database cannot be null");
        }

        if (!initialized) {
            this.database = database;
            initialized = false;
        }
    }

    /**
     * Checks if the specified plugin has been already registered.
     * @param packageId package id
     * @param pluginId plugin id
     * @return true | false
     * @throws Exception
     */
    protected boolean isRegistered(PackageId packageId, PluginId pluginId) throws Exception
    {
        Vector v = database.getPlugin(packageId, pluginId);

        return ((v != null) && (v.size() > 0));
    }

    /**
     * Registers this plugin. Throws an exception if an instance of this plugin
     * has already been registered. Registration involves a database access.
     * @param plugin plugin instance
     * @throws Exception
     */
    public void register(Plugin plugin) throws Exception
    {
        // check if this plugin has been registered:
        if (isRegistered(plugin.getPackageId(), plugin.getId())) {
            throw new RuntimeException("A plugin with this id/package id already exists");
        } else {
            database.addPlugin(plugin);
        }
    }

    /**
     * Unregisters all plugins for the specified package.
     * @param packageId package identifier
     */
    public void unregister(PackageId packageId) throws Exception
    {
        database.removePlugins(packageId);
    }

    /**
     * Unregisters this plugin.
     * @param packageId package identifier
     * @param pluginId plugin identifier
     */
    public void unregister(PackageId packageId, PluginId pluginId) throws Exception
    {
        database.removePlugin(packageId, pluginId);
    }
}
