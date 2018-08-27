/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.container;

import java.io.File;
import java.util.Properties;
import java.util.Vector;

import orca.manage.internal.ContainerManagementObject;
import orca.shirako.container.api.IConfigurationLoader;
import orca.shirako.container.api.IOrcaAdminConfiguration;
import orca.shirako.container.api.IOrcaConfiguration;
import orca.shirako.container.db.OrcaContainerDatabase;
import orca.tools.axis2.Axis2ClientSecurityConfigurator;
import orca.util.KeystoreManager;
import orca.util.PropList;

/**
 * <code>ContainerAdministrativeConfiguration</code> stores the admin-level
 * configuration information for a Shirako container.
 * @author aydan
 */
public class OrcaAdminConfiguration implements IOrcaAdminConfiguration {

    public static final String AdminName = "admin.name";
    public static final String AdminKeystorePassword = "admin.keystore.password";
    public static final String AdminPrivateKeyPassword = "admin.keystore.key.password";
    public static final String AdminImageManagerClass = "admin.imagemanager.class";
    public static final String AdminPluginPrefix = "admin.plugin.";
    public static final String AdminManagementDefaultsInstaller = "admin.management.defaults.installer.class";
    public static final String AdminManagerObject = "admin.container.manager.object.class";

    public static final String AdminPrefix = "admin.";
    public static final String AdminConfigurationLoader = "admin.configuration.loader.class";

    /**
     * Configuration property name: container reset.
     */
    public static final String AdminContainerReset = "admin.container.reset";

    /**
     * Configuration property prefix: container database.
     */
    public static final String AdminContainerDatabasePrefix = "admin.container.database.";

    /**
     * Configuration property name: container database class.
     */
    public static final String AdminContainerDatabaseClass = "admin.container.database.class";

    /**
     * Prefix to use as a replacement of AdminContainerDatabasePrefix
     */
    public static final String AdminContainerDatabaseNewPrefix = "db.";
    /**
     * The original container configuration.
     */
    protected Properties properties;

    protected Properties defaultProperties;
    /**
     * The container context: configuration accessible to code outside of the
     * management code.
     */
    protected OrcaConfiguration context;

    /**
     * The class implementing IConfigurationLoader.
     */
    protected String configurationLoaderClass = "orca.boot.ConfigurationLoader";

    /**
     * Administrator name.
     */
    protected String adminID = "admin";

    /**
     * Administrator keystore password.
     */
    protected String adminKeystorePassword = "clientkeystorepass";
    /**
     * Administrator private key password.
     */
    protected String adminPrivateKeyPassword = "clientkeypass";
    /**
     * Administrator keystore.
     */
    protected KeystoreManager store;

    /**
     * Image manager class name.
     */
    protected String imageManagerClass;

    /**
     * List of plugins to be instantiated when the container is ready.
     */
    protected Vector<String> plugins;
    protected String adminManagerObjectClass;

    public OrcaAdminConfiguration() {
    	properties = new Properties();
    	defaultProperties = new Properties();
        plugins = new Vector<String>();
        loadDefaults();
    }

    private void loadDefaults() {
    	defaultProperties.setProperty(AdminContainerDatabaseClass, OrcaContainerDatabase.class.getName());
    	defaultProperties.setProperty(AdminManagerObject, ContainerManagementObject.class.getName());
    	defaultProperties.setProperty(AdminConfigurationLoader, "orca.boot.ConfigurationLoader");
    
    }
    
    public void initialize(Properties p) throws ContainerInitializationException {
        properties.clear();
        PropList.mergeProperties(defaultProperties, properties);
        PropList.mergeProperties(p, properties);
     
        /* load the configuration properties */
        processConfiguration();

        /* create a properties list without the admin properties */
        Properties copy = new Properties();

        for (Object key : properties.keySet()) {
            String skey = (String) key;

            if (!skey.startsWith(AdminPrefix)) {
                copy.setProperty(skey, properties.getProperty(skey));
            }
        }

        context = new OrcaConfiguration(copy);
        /* create the admin key store */
        String path = getAdminKeystorePath();
        File f = new File(path);
        if (!f.exists()) {
			Axis2ClientSecurityConfigurator conf = Axis2ClientSecurityConfigurator.getInstance();
			if (conf.createActorConfiguration(Globals.HomeDirectory, "admin") != 0) {
				throw new ContainerInitializationException("cannot create security files");
			}
        }
        store = new KeystoreManager(getAdminKeystorePath(), adminKeystorePassword, adminPrivateKeyPassword);
        try {
            store.initialize();
        } catch (Exception e) {
            throw new ContainerInitializationException(e);
        }
    }

    /**
     * Returns the container configuration loader.
     * @return the container configuration loader
     * @throws Exception in case of error
     */
    public IConfigurationLoader getConfigurationLoader() throws Exception {
        Class<?> c = Class.forName(configurationLoaderClass);
        IConfigurationLoader loader = (IConfigurationLoader) c.newInstance();

        return loader;
    }

    public String getContainerDatabaseClass() {
        return properties.getProperty(AdminContainerDatabaseClass);
    }

    /**
     * Returns the container database configuration.
     * @return container database configuration or null if no configuration has
     *         been specified
     */
    public Properties getContainerDatabaseConfiguration() {
        String temp = properties.getProperty(AdminContainerDatabaseClass);

        if (temp != null) {
            temp = temp.trim();

            if (temp.length() != 0) {
                return getProperties(AdminContainerDatabasePrefix, AdminContainerDatabaseNewPrefix);
            }
        }

        // if no database class name is specified, return null
        return null;
    }

    public OrcaConfiguration getContext() {
        return context;
    }

    /**
     * Returns a properties list that contains a subset of the configuration
     * properties with names starting with the specified prefix.
     * @param prefix property name prefix
     * @return properties list
     */
    protected Properties getProperties(final String prefix) {
        Properties result = new Properties();

        for (Object o : properties.keySet()) {
            String s = (String) o;

            if (s.startsWith(prefix)) {
                result.setProperty(s, properties.getProperty(s));
            }
        }

        return result;
    }

    /**
     * Returns a properties list that contains a subset of the configuration
     * properties with names starting with the specified prefix. Each property
     * name will be modified so that the original prefix will be replaced with
     * <code>newPrefix</code>
     * @param prefix property name prefix
     * @param newPrefix new property name prefix
     * @return properties list
     */
    protected Properties getProperties(final String prefix, final String newPrefix) {
        Properties result = new Properties();

        for (Object o : properties.keySet()) {
            String s = (String) o;

            if (s.startsWith(prefix)) {
                result.setProperty(s.replaceFirst(prefix, newPrefix), properties.getProperty(s));
            }
        }

        return result;
    }

    /**
     * Processes the container configuration
     * @throws ContainerInitializationException in case of error 
     */
    protected void processConfiguration() throws ContainerInitializationException {
        configurationLoaderClass = properties.getProperty(AdminConfigurationLoader);

        if (configurationLoaderClass == null) {
            throw new ContainerInitializationException("No configuration loader class is specified in the container configuration");
        }

        if (properties.containsKey(AdminName)) {
            adminID = properties.getProperty(AdminName);
        }

        if (properties.containsKey(AdminKeystorePassword)) {
            adminKeystorePassword = properties.getProperty(AdminKeystorePassword);
        }

        if (properties.containsKey(AdminPrivateKeyPassword)) {
            adminPrivateKeyPassword = properties.getProperty(AdminPrivateKeyPassword);
        }

        if (properties.containsKey(AdminManagerObject)) {
            adminManagerObjectClass = properties.getProperty(AdminManagerObject);
        }

        /**
         * Image manager class.
         */
        imageManagerClass = properties.getProperty(AdminImageManagerClass);
        /**
         * Populate the plugins list.
         */
        loadPlugins();

    }

    /**
     * Checks if when the container starts it should purge its database.
     * @return true if the container should start reset its state and start
     *         "clean".
     */
    public boolean shouldResetContainer() {
        // default is false: NO RESET
        boolean result = false;

        if (properties.containsKey(AdminContainerReset)) {
            try {
                boolean temp = PropList.getBooleanProperty(properties, AdminContainerReset);

                return temp;
            } catch (Exception e) {
                // ignore the error and return the default value
            }
        }

        return result;
    }

    public IOrcaConfiguration getConfiguration() {
        return context;
    }

    public String getAdminIdentifier() {
        return adminID;
    }

    public String getAdminKeyStorePass() {
        return adminKeystorePassword;
    }

    public String getAdminPrivateKeyPassword() {
        return adminPrivateKeyPassword;
    }

    public String getAdminKeystorePath() {
        return Axis2ClientSecurityConfigurator.getInstance().getKeyStorePath(Globals.HomeDirectory, adminID);
    }

    public String getContainerManagerObjectClass() {
        return adminManagerObjectClass;
    }

    public String getImageManagerClass() {
        return imageManagerClass;
    }

    public KeystoreManager getKeyStore() {
        return store;
    }

    public Vector<String> getPlugins() {
        return plugins;
    }

    /**
     * Populates the list of plugins to be instantiated when the container
     * boots.
     */
    protected void loadPlugins() {
        for (Object key : properties.keySet()) {
            String skey = (String) key;

            if (skey.startsWith(AdminPluginPrefix)) {
                plugins.add(properties.getProperty(skey));
            }
        }
    }

}
