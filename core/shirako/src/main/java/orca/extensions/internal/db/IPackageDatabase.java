/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.extensions.internal.db;

import orca.extensions.PackageId;
import orca.extensions.PluginId;
import orca.extensions.internal.ExtensionPackage;
import orca.extensions.internal.Plugin;

import java.util.Properties;
import java.util.Vector;


/**
 * PackageDatabase describes the database interface required by the
 * PackageManager.
 */
public interface IPackageDatabase
{
    /**
     * Add a new extension package record
     * @param p
     * @throws Exception
     */
    public void addPackage(ExtensionPackage p) throws Exception;

    /**
     * Removes the specified package record
     * @param id
     * @throws Exception
     */
    public void removePackage(PackageId id) throws Exception;

    /**
     * Return the specified package record
     * @param id
     * @return
     * @throws Exception
     */
    public Vector<Properties> getPackage(PackageId id) throws Exception;

    /**
     * Return all package records
     * @return
     * @throws Exception
     */
    public Vector<Properties> getPackages() throws Exception;

    /**
     * Adds a new plugin record
     * @param plugin
     * @throws Exception
     */
    public void addPlugin(Plugin plugin) throws Exception;

    /**
     * Removes the specified plugin record
     * @param packageId
     * @param pluginId
     * @return
     * @throws Exception
     */
    public void removePlugin(PackageId packageId, PluginId pluginId) throws Exception;

    /**
     * Removes all plugin records for the specified package
     * @param packageId
     * @throws Exception
     */
    public void removePlugins(PackageId packageId) throws Exception;

    /**
     * Returns the specified plugin record
     * @param packageId
     * @param pluginId
     * @return
     * @throws Exception
     */
    public Vector<Properties> getPlugin(PackageId packageId, PluginId pluginId)
                                 throws Exception;

    /**
     * Returns all plugins that belong to the specified package, match the type,
     * and are associated with the given actor type.
     * @param packageId  package identifier, if null it is ignored
     * @param pluginType see Plugin.Type*
     * @param actorType see OrcaConstants.ActorType*
     * @return
     * @throws Exception
     */
    public Vector<Properties> getPlugins(PackageId packageId, int pluginType, int actorType)
                                  throws Exception;
}