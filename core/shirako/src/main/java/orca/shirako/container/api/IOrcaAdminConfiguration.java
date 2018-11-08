/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.container.api;

import java.util.Properties;
import java.util.Vector;

import orca.shirako.container.ContainerInitializationException;
import orca.util.KeystoreManager;


/**
 * Represents the portion of an application's configuration, which is used
 * solely for administrative purposes. This interface can be used to separate
 * configuration data, which, for some reason, is not appropriate to be exposed
 * to the rest of the application. For example, passwords, user names, etc.
 */
public interface IOrcaAdminConfiguration
{
    /**
     * Initializes the configuration.
     *
     * @param p properties list
     *
     * @throws ContainerInitializationException if an error occurs during initialization
     */
    void initialize(Properties p) throws ContainerInitializationException;

    /**
     * Returns the container configuration (public part).
     *
     * @return container configuration
     */
    IOrcaConfiguration getConfiguration();
    
    Properties getContainerDatabaseConfiguration();
    boolean shouldResetContainer();
    public String getContainerManagerObjectClass();
    public Vector<String> getPlugins();
    public KeystoreManager getKeyStore();
    public String getAdminIdentifier();
}