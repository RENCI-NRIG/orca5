/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.manage;

import java.security.cert.Certificate;
import java.util.List;

import orca.extensions.PackageId;
import orca.extensions.PluginId;
import orca.manage.beans.ActorCreateMng;
import orca.manage.beans.ActorMng;
import orca.manage.beans.PackageMng;
import orca.manage.beans.PluginMng;
import orca.manage.beans.ProxyMng;
import orca.manage.beans.ResultMng;
import orca.manage.beans.UnitMng;
import orca.manage.beans.UserMng;
import orca.security.AuthToken;
import orca.shirako.common.UnitID;
import orca.util.ID;

/**
 * The base interface to the management layer. Provides functions for managing:
 * <ul>
 * <li>users</li>
 * <li>packages</li>
 * <li>plugins</li>
 * <li>container configuration</li>
 * <li>proxies</li>
 * <li>recovery</li>
 * </ul>
 * <p>
 * This interface specifies the basic API exposed by the management layer. All
 * methods require an {@link AuthToken} argumnent and return ether a
 * {@link ResultMng} object or an object that contains an instance of
 * <code>ResultMng</code>. For objects that contain an instance of
 * <code>ResultMng</code>, the ResultMng instance can be obtained by invoking
 * the containing object's <code>getStatus</code> method. Each
 * <code>ResultMng</code> object provides details about the outcome of the
 * operation, e.g, exit code, exception details, etc. <code>ResultMng</code>
 * objects should always be checked before using other fields of return objects.
 * All methods return non-null values.
 * </p>
 * <p>
 * All functions that return a given type of object, for example
 * {@link IOrcaContainer#getPlugins(int, int)}, return an array of the requested
 * objects, which can be obtained by invoking the <code>getResult</code> method.
 * Note that, even for functions that always return a single element, for
 * example {@link #getPlugin(PackageId, PluginId)}, that object will be
 * wrapped inside an array of size one. Array objects are not guaranteed to be
 * non-null if the array should contain 0 elements. Always check the return of
 * getResult() for null.
 * </p>
 * <p>
 * Each method can also throw an exception. The exact exception class may vary
 * depending on the specific protocol used to communicate with the management
 * layer. In general, any thrown exceptions will be due to the interface
 * implementation, rather than the management layer. Every API function in the
 * management layer catches all exceptions internally.
 * </p>
 */
public interface IOrcaContainer extends IOrcaComponent {
	/**
	 * Obtains the certificate of the orca container.
	 * 
	 * @return container certificate
	 */
	public Certificate getCertificate();

	/**
	 * Authenticates the specified user
	 * 
	 * @param user
	 *            user name
	 * @param password
	 *            password
	 * @return true on success, false otherwise
	 */
	public boolean login(String user, String password);

	/**
	 * Terminates the current user session.
	 * 
	 * @return true on success, false otherwise
	 */
	public boolean logout();

	/**
	 * Checks if an authenticated session exists.
	 * 
	 * @return true if the user has been successfully authenticated, false
	 *         otherwise
	 */
	public boolean isLogged();

	/**
	 * Adds a new user.
	 * 
	 * @param user
	 *            user
	 * @return true on success, false otherwise
	 */
	public boolean addUser(UserMng user);

	/**
	 * Sets the user password.
	 * 
	 * @param login
	 *            user login
	 * @param password
	 *            password
	 * @return true for sucess; false otherwise
	 */
	public boolean setUserPassword(String login, String password);

	/**
	 * Obtains the specified user.
	 * 
	 * @param login
	 *            user login
	 * @return the user record on success, null otherwise
	 */
	public UserMng getUser(String login);

	/**
	 * Obtains a list of all users.
	 * 
	 * @return a list of users. Always non-null.
	 */
	public List<UserMng> getUsers();

	/**
	 * Removes the specified user.
	 * 
	 * @param login
	 *            user login
	 * @return true on success, false otherwise
	 */
	public boolean removeUser(String login);

	/**
	 * Updates the specified user.
	 * 
	 * @param user
	 *            user record
	 * @return true on success, false otherwise
	 */
	public boolean updateUser(UserMng user);

	/**
	 * Obtains the certificate of the specified actor.
	 * 
	 * @param guid 
	 *            guid 
	 * @return returns the certificate
	 */
	public Certificate getCertificate(ID guid);

	/**
	 * Obtains the specified actor.
	 * 
	 * @param guid
	 *            actor guid
	 * @return proxy to the actor on success, null otherwise. The returned proxy
	 *         will share the same credentials.
	 */
	public IOrcaActor getActor(ID guid);

	/**
	 * Obtains the specified service manager.
	 * @param guid guid
	 * @return specified service manager
	 */
	public IOrcaServiceManager getServiceManager(ID guid);
	
	/**
	 * Obtains the specified broker.
	 * @param guid broker guid
	 * @return specified broker
	 */
	public IOrcaBroker getBroker(ID guid);

	/**
	 * Obtains the specified site authority.
	 * @param guid guid
	 * @return specified site authority
	 */
	public IOrcaAuthority getAuthority(ID guid);

	/**
	 * Creates actors in the Orca container based on the passed in
	 * configuration.
	 * 
	 * @param configuration
	 *            XML actor configuration file in byte array form
	 * @return true on success, false otherwise FIXME: should be atomic: either
	 *         the whole configuration is processed with an error, or all
	 *         changes introduced by this call are rolled back.
	 */
	public boolean configure(byte[] configuration);

	/**
	 * Creates a new actor.
	 * 
	 * @param actor
	 *            actor description
	 * @param brokers
	 *            array of brokers
	 * @return true on success, false otherwise FIXME: must be atomic.
	 */
	public boolean addActor(ActorCreateMng actor, ProxyMng[] brokers);

	/**
	 * Obtains a list of all active actors in the container.
	 * 
	 * @return list of actors. Always non-null.
	 */
	public List<ActorMng> getActors();

	/**
	 * Obtains a list of all active and suspended actors in the container.
	 * 
	 * @return list of actors. Always non-null.
	 */
	public List<ActorMng> getActorsFromDatabase();

	/**
	 * Obtains a list of all active site authorities.
	 * 
	 * @return list of actors. Always non-null.
	 */
	public List<ActorMng> getAuthorities();

	/**
	 * Obtains a list of all active brokers.
	 * 
	 * @return list of actors. Always non-null.
	 */
	public List<ActorMng> getBrokers();

	/**
	 * Obtains a list of all service managers.
	 * 
	 * @return list of actors. Always non-null.
	 */
	public List<ActorMng> getServiceManagers();

	/**
	 * Removes the specified actor.
	 * 
	 * @param actorGuid
	 *            actor guid
	 * @return true on success, false otherwise
	 */
	public boolean removeActor(ID actorGuid);

	/**
	 * Starts the specified actor.
	 * 
	 * @param actorGuid
	 *            actor guid
	 * @return true on success, false otherwise
	 */
	public boolean startActor(ID actorGuid);

	/**
	 * Stops the specified actor.
	 * 
	 * @param actorGuid
	 *            actor guid
	 * @return true on success, false otherwise
	 */
	public boolean stopActor(ID actorGuid);

	// proxies

	/**
	 * Obtains a list of all proxies to actors using the specified protocol
	 * 
	 * @param protocol
	 *            protocol name
	 * @return list of proxies. Always non-null.
	 */
	public List<ProxyMng> getProxies(String protocol);

	/**
	 * Obtains a list of all proxies to brokers using the specified protocol
	 * 
	 * @param protocol
	 *            protocol name
	 * @return list of proxies. Always non-null.
	 */
	public List<ProxyMng> getBrokerProxies(String protocol);

	/**
	 * Obtains a list of all proxies to authorities using the specified protocol
	 * 
	 * @param protocol
	 *            protocol name
	 * @return list of proxies. Always non-null.
	 */
	public List<ProxyMng> getAuthorityProxies(String protocol);

	// package management

	/**
	 * Obtains the specified package
	 * 
	 * @param packageID
	 *            package id
	 * @return package descriptor on success, null otherwise
	 */
	public PackageMng getPackage(PackageId packageID);

	/**
	 * Obtains a list of all installed packages.
	 * 
	 * @return list of packages. Always non-null.
	 */
	public List<PackageMng> getPackages();

	/**
	 * Installs the specified package.
	 * 
	 * @param bytes
	 *            package contents
	 * @return true on success, false othersie FIXME: must be atomic
	 */
	public boolean installPackage(byte[] bytes);

	/**
	 * Uninstalls the specified package
	 * 
	 * @param packageID
	 *            package id
	 * @return true on success, false otherwise
	 */
	public boolean uninstallPackage(PackageId packageID);

	/**
	 * Upgrades the specified package.
	 * 
	 * @param packageID
	 *            package id
	 * @param bytes
	 *            package contents
	 * @return true on success, false otherwise
	 */
	public boolean upgradePackage(PackageId packageID, byte[] bytes);

	// plugins

	/**
	 * Obtains the specified plugin.
	 * 
	 * @param packageID
	 *            package id
	 * @param pluginID
	 *            plugin id
	 * @return plugin descriptor on success, null otherwise
	 */
	public PluginMng getPlugin(PackageId packageID, PluginId pluginID);

	/**
	 * Obtains a list of plugins of the specified type.
	 * 
	 * @param type
	 *            plugin time [FIXME: make it an enum]
	 * @return list of plugins. Always non-null.
	 */
	public List<PluginMng> getPlugins(int type);

	/**
	 * Obtains a list of plugins of the specified type.
	 * 
	 * @param type
	 *            plugin time [FIXME: make it an enum]
	 * @param actorType
	 *            actor the plugins should apply to
	 * @return list of plugins. Always non-null.
	 */
	public List<PluginMng> getPlugins(int type, int actorType);

	/**
	 * Obtains a list of plugins of the specified type.
	 * 
	 * @param packageID
	 *            package id
	 * @param type
	 *            plugin time [FIXME: make it an enum]
	 * @return list of plugins. Always non-null.
	 */
	public List<PluginMng> getPlugins(PackageId packageID, int type);

	/**
	 * Obtains a list of all plugins from the specified package.
	 * 
	 * @param packageID
	 *            package id
	 * @return list of plugins. Always non-null.
	 */
	public List<PluginMng> getPlugins(PackageId packageID);

	/**
	 * Obtains a list of plugins of the specified type.
	 * 
	 * @param packageID
	 *            package id
	 * @param type
	 *            plugin time [FIXME: make it an enum]
	 * @param actorType
	 *            actor the plugins should apply to
	 * @return list of plugins. Always non-null.
	 */
	public List<PluginMng> getPlugins(PackageId packageID, int type, int actorType);

	// Inventory management

	/**
	 * Obtains all units owned by the container
	 * 
	 * @return list of units. Always non-null.
	 */
	public List<UnitMng> getInventory();

	/**
	 * Obtains the specified unit.
	 * 
	 * @param unit 
	 *            unit id 
	 * @return the unit record, null otherwise
	 */
	public UnitMng getInventory(UnitID unit);

	/**
	 * Adds a new unit record.
	 * 
	 * @param unit
	 *            unit
	 * @return true on success, false otherwise
	 */
	public boolean addInventory(UnitMng unit);

	/**
	 * Updates the specified unit.
	 * 
	 * @param unit
	 *            unit record
	 * @return true on success, false otherwise
	 */
	public boolean updateInventory(UnitMng unit);

	/**
	 * Removes the specified unit.
	 * 
	 * @param unit
	 *            unit
	 * @return true on success, false otherwise
	 */
	public boolean removeInventory(UnitID unit);

	/**
	 * Transfers the specified unit to the specified actor.
	 * 
	 * @param unit
	 *            unit
	 * @param actorGuid
	 *            actor guid
	 * @return true on success, false otherwise
	 */
	public boolean transferInventory(UnitID unit, ID actorGuid);

	/**
	 * Transfer the specified unit back to the container.
	 * 
	 * @param unit 
	 *            unit
	 * @param actorGuid 
	 *            actor guid
	 * @return true on success, false otherwise
	 */
	public boolean untransferInventory(UnitID unit, ID actorGuid);

	// Extensibility

	/**
	 * Obtains the specified management object
	 * 
	 * @param key
	 *            management object id
	 * @return management object proxy on success, null otherwise. The returned
	 *         proxy will share the same credentials.
	 */
	public IOrcaComponent getManagementObject(ID key);
}
