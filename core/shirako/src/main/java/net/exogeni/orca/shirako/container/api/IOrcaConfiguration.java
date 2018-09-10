/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.shirako.container.api;

import net.exogeni.orca.util.ID;

/**
 * Interface for the configuration of an application container. A container's
 * configuration consists of (name, value) pairs. Values can be strings or any
 * Java objects. During boot, the container manager will read the application
 * configuration file and create an instance of
 * <code>IContainerConfiguration</code>.
 */
public interface IOrcaConfiguration {
    /**
     * Name for the property storing the container manager class name.
     */
    public static final String PropertyContainerManagerClass = "container.manager.class";

    /**
     * Number of Jetty container threads to start for non-SSL Actor Container connections
     * See: https://github.com/RENCI-NRIG/net.exogeni.orca5/issues/89
     */
    public static final String PropertyContainerThreads = "container.threads";

    public static final String PropertySoapAxis2Url = "protocols.soapaxis2.url";

    public static final String RemoteRegistryCacheClass = "RemoteRegistryCache.class";
    /**
     * Returns the value of the property with the given key.
     * @param key key
     * @return property value
     */
    String getProperty(String key);

    /**
     * Sets the value of the property with the given key. If the property is
     * already set, it will be overwritten.
     * @param key key
     * @param value value
     */
    void setProperty(String key, String value);

    /**
     * Returns the object stored under the given key.
     * @param key key
     * @return object stored under the given key
     */
    Object getObject(String key);

    /**
     * Stores the object under the given key.
     * @param key key
     * @param value value
     */
    void setObect(String key, Object value);

    /**
     * Returns the container GUID.
     * @return container guid
     */
    ID getContainerGUID();

    long getTimeStart();

    long getCycleMillis();

    boolean getManualTime();

    String getTicketFactoryClassName();

    boolean isSecureCommunication();

    boolean isSecureActorCommunication();

    public boolean isEmulation();

    public String getAxis2ClientRepository();

    public String getNodeAgentPortNumber();

    public String getNodeAgentProtocol();

    public String getNodeAgentServiceUrl(String address);

    public String getNodeAgentUri();

    public String getCometHost();

    public String getCometCaCert();

    public String getCometClientCertKeyStore();

    public String getCometClientCertKeyStorePwd();

    public String getCometClientCert();

    public String getCometClientKey();
}
