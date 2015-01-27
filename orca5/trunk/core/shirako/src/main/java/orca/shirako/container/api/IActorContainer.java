/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.container.api;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.Properties;

import orca.extensions.PackageId;
import orca.extensions.internal.PackageManager;
import orca.extensions.internal.PluginManager;
import orca.manage.internal.ManagementObjectManager;
import orca.manage.internal.UserSet;
import orca.shirako.api.IActor;
import orca.shirako.container.ContainerInitializationException;
import orca.shirako.container.ContainerRecoveryException;
import orca.shirako.container.ProtocolDescriptor;
import orca.util.ID;
import orca.util.KeystoreManager;

/**
 * <code>IShirakoContainerManager</code> is the public interface for a Shirako
 * container.
 */
public interface IActorContainer extends IContainerClock
{
    /**
     * Initializes the container manager.
     * @param configuration container configuration
     * @throws Exception if the configuration is invalid
     */
    void initialize(IOrcaAdminConfiguration configuration) throws ContainerInitializationException;

    /**
     * Returns the container GUID.
     * @return container GUID
     */
    ID getGuid();

    /**
     * Returns the container database.
     * @return container database
     */
    IOrcaContainerDatabase getDatabase();


    /**
     * Returns the container configuration.
     * @return container configuration
     */
    IOrcaConfiguration getConfiguration();

    /**
     * Checks if the container has completed recovery.
     * @return TRUE if recovery is complete, FALSE otherwise
     */
    boolean isRecovered();

    /**
     * Shuts down the container.
     */
    void shutdown();

    /**
     * Registers a communication protocol with the container. This protocol
     * applies only for internal communication among actors; this is not a
     * protocol used for managing the container.
     * @param protocol protocol to register
     */
    public void registerProtocol(ProtocolDescriptor protocol);

    public void loadConfiguration() throws Exception;

    /**
     * Loads the container configuration.
     * @param config serialized configuration.
     * @throws Exception
     */
    public void loadConfiguration(byte[] config) throws Exception;

    /**
     * Loads the container configuration.
     * @param config input stream with configuration information
     * @throws Exception
     */
    public void loadConfiguration(InputStream config) throws Exception;

    /**
     * Loads the container configuration.
     * @param fileName container configuration file
     * @throws Exception
     */
    public void loadConfiguration(String fileName) throws Exception;

    /**
     * Registers a new actor: adds the actor to the database, deploys services
     * required by the actor, registers actor proxies and callbacks. Must not
     * register the actor with the clock! Clock registration is a separate
     * phase.
     * @param actor actor to register
     * @throws Exception
     */
    public void registerActor(IActor actor) throws Exception;

    public String getAxis2ClientRepository();

    public String getAxis2Configuration(String actorID);

    public String getAxis2UnsecureConfiguration(String actorID);

    public String getAxis2ClientPropertiesRelativePath(String actorID);

    public PluginManager getPluginManager();

    public ManagementObjectManager getManagementObjectManager();

    public ProtocolDescriptor getProtocolDescriptor(String protocol);

    public boolean isFresh();

    public String getPackageRootFolder(PackageId id);

    public IActor recoverActor(Properties p) throws ContainerRecoveryException;

    // FIXME: this method should not be public
    public KeystoreManager getKeyStore();

    public String getAdminIdentifier();

    public UserSet getUsers();

    public PackageManager getPackageManager();

    public void unregisterActor(IActor actor) throws Exception;

    public void removeActor(String actorName) throws Exception;

    public void removeActorDatabase(String actorName) throws Exception;

    /**
     * Returns the container certificate.
     * @return
     */
    public Certificate getCertificate();
    
    /**
     * Returns the certificate of the specified actor.
     * @param actorGuid actor guid
     * @return certificate if guid refers to a known local actor, false otherwise
     */
    public Certificate getCertificate(ID actorGuid);
}
