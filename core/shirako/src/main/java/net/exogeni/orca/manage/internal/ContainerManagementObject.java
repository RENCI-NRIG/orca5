/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.manage.internal;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import net.exogeni.orca.extensions.IActorFactory;
import net.exogeni.orca.extensions.IControllerFactory;
import net.exogeni.orca.extensions.IPluginFactory;
import net.exogeni.orca.extensions.PackageId;
import net.exogeni.orca.extensions.PluginId;
import net.exogeni.orca.extensions.internal.ExtensionPackage;
import net.exogeni.orca.extensions.internal.Plugin;
import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.manage.OrcaManagementException;
import net.exogeni.orca.manage.OrcaProxyProtocolDescriptor;
import net.exogeni.orca.manage.beans.ActorCreateMng;
import net.exogeni.orca.manage.beans.PluginCreateMng;
import net.exogeni.orca.manage.beans.ProxyMng;
import net.exogeni.orca.manage.beans.ResultActorMng;
import net.exogeni.orca.manage.beans.ResultCertificateMng;
import net.exogeni.orca.manage.beans.ResultMng;
import net.exogeni.orca.manage.beans.ResultPackageMng;
import net.exogeni.orca.manage.beans.ResultPluginMng;
import net.exogeni.orca.manage.beans.ResultProxyMng;
import net.exogeni.orca.manage.beans.ResultStringMng;
import net.exogeni.orca.manage.beans.ResultUnitMng;
import net.exogeni.orca.manage.beans.ResultUserMng;
import net.exogeni.orca.manage.beans.UnitMng;
import net.exogeni.orca.manage.beans.UserMng;
import net.exogeni.orca.manage.internal.api.IManagementObject;
import net.exogeni.orca.manage.internal.local.LocalContainer;
import net.exogeni.orca.manage.proxies.soap.SoapContainer;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IBrokerProxy;
import net.exogeni.orca.shirako.api.IPolicy;
import net.exogeni.orca.shirako.api.IProxy;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.common.UnitID;
import net.exogeni.orca.shirako.common.meta.UnitProperties;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.shirako.container.api.IOrcaContainerDatabase;
import net.exogeni.orca.shirako.core.Actor;
import net.exogeni.orca.shirako.core.Broker;
import net.exogeni.orca.shirako.core.ServiceManager;
import net.exogeni.orca.shirako.core.Unit;
import net.exogeni.orca.shirako.core.UnitState;
import net.exogeni.orca.shirako.kernel.SliceFactory;
import net.exogeni.orca.shirako.registry.ActorRegistry;
import net.exogeni.orca.tools.axis2.Axis2ClientSecurityConfigurator;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.persistence.PersistenceUtils;

public class ContainerManagementObject extends ManagementObject {
	public ContainerManagementObject() {
		super();
		id = OrcaConstants.ContainerManagmentObjectID;
		axis2ServiceDescriptor = "net/exogeni/orca/manage/extensions/standard/container/proxies/soapaxis2/services/standard.container.xml";
	}

	@Override
	protected void registerProtocols() {
		OrcaProxyProtocolDescriptor local = new OrcaProxyProtocolDescriptor(OrcaConstants.ProtocolLocal,
				LocalContainer.class.getName());
		OrcaProxyProtocolDescriptor soap = new OrcaProxyProtocolDescriptor(OrcaConstants.ProtocolSoap, SoapContainer.class.getName());
		proxies = new OrcaProxyProtocolDescriptor[] { local, soap };
	}

	public IOrcaContainerDatabase getContainerManagementDatabase() {
		return Globals.getContainer().getDatabase();
	}

	/*
	 * ========================================================================
	 * Log in/out
	 * ========================================================================
	 */

	/**
	 * Authentication entry point for external callers.
	 * @param login login
	 * @param password password
	 * @return result
	 */
	public ResultStringMng login(String login, String password) {
		ResultStringMng result = new ResultStringMng();
		result.setStatus(new ResultMng());
		try {
			String hash = hashPassword(password);
			Vector<Properties> v = getContainerManagementDatabase().getUser(login, hash);
			if (v.size() == 0) {
				result.getStatus().setCode(OrcaConstants.ErrorAccessDenied);
			}else {
				User user = Globals.getContainer().getUsers().getUser(login);
				String loginToken = getLoginToken(login);
				user.setLoginToken(loginToken);
				result.setResult(loginToken);
			}
		}catch (Exception e) {
			logger.error("setUserPassword", e);
			result.getStatus().setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result.getStatus(), e);
		}
		return result;
	}

	/**
	 * Pseudo-authentication entry point for internal callers.
	 * Internal callers are trusted and do not require credentials.
	 * This method produces an AuthToken that grants code running
	 * in the JVM admin rights.
	 * @return auth token
	 * @throws Exception in case of error
	 */
	public AuthToken loginInternal() throws Exception {
		// generate a new random user login
		String login = (new ID()).toString();
		// create a new user
		User user = new User();
		user.setLogin(login);
		user.setRoles(new String[] {OrcaConstants.RoleAdmin});
		String loginToken = getLoginToken(login);
		user.setLoginToken(loginToken);
		// add the user to the user set
		Globals.getContainer().getUsers().addInternalAdminUser(user);
		AuthToken auth = new AuthToken(login);
		auth.setLoginToken(loginToken);
		return auth;
	}
	
	/**
	 * Performs logout for the given user
	 * 
	 * @param user to log out
	 */
	public void logout(AuthToken user) {
		if (user != null) {
			Globals.getContainer().getUsers().flushUser(user.getName());
		}
	}

	/*
	 * =========================================================================
	 * User management
	 * =========================================================================
	 */

	/**
	 * Obtains all user records
	 * 
	 * @param auth Credentials of the caller
	 * @return all user records
	 */
	public ResultUserMng getUsers(AuthToken auth) {
		ResultUserMng result = new ResultUserMng();
		result.setStatus(new ResultMng());

		if (auth == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(auth)) {
					result.getStatus().setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					Vector<Properties> v = getContainerManagementDatabase().getUsers();
					Converter.fillUser(result.getResult(), v);
				}
			} catch (Exception e) {
				logger.error("getUsers", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Retrieves the specified user record
	 * 
	 * @param name user name
	 * @param auth Credentials of the caller
	 * @return specified user
	 */
	public ResultUserMng getUser(String name, AuthToken auth) {
		ResultUserMng result = new ResultUserMng();
		result.setStatus(new ResultMng());

		if (auth == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(auth)) {
					result.getStatus().setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					Vector<Properties> v = getContainerManagementDatabase().getUser(name);
					Converter.fillUser(result.getResult(), v);
				}
			} catch (Exception e) {
				logger.error("getUser", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Adds a new user
	 * 
	 * @param user User description
	 * @param auth Credentials of the caller
	 * @return result
	 */
	public ResultMng addUser(UserMng user, AuthToken auth) {
		ResultMng result = new ResultMng();

		if (auth == null) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(auth)) {
					result.setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					getContainerManagementDatabase().addUser(Converter.fill(user));
				}
			} catch (Exception e) {
				logger.error("addUser", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}
	
	public ResultMng setUserPassword(String login, String password, AuthToken auth) {
		ResultMng result = new ResultMng();

		if (auth == null) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(auth)) {
					result.setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					getContainerManagementDatabase().setUserPassword(login, hashPassword(password));
				}
			} catch (Exception e) {
				logger.error("setUserPassword", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}
	

	/**
	 * Updates the specified user record
	 * 
	 * @param user User record
	 * @param auth Credentials of the caller
	 * @return result
	 */
	public ResultMng updateUser(UserMng user, AuthToken auth) {
		ResultMng result = new ResultMng();

		if (auth == null) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(auth)) {
					result.setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					// flush cached state
					Globals.getContainer().getUsers().flushUser(user.getLogin());
					// update the database
					getContainerManagementDatabase().updateUser(Converter.fill(user));
				}
			} catch (Exception e) {
				logger.error("updateUser", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	/**
	 * Removes the specified user record
	 * 
	 * @param user User record
	 * @param auth Credentials of the caller
	 * @return result
	 */
	public ResultMng removeUser(String user, AuthToken auth) {
		ResultMng result = new ResultMng();

		if (auth == null) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(auth)) {
					result.setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					getContainerManagementDatabase().removeUser(user);
				}
			} catch (Exception e) {
				logger.error("removeUser", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	/**
	 * Installs an extension package
	 * 
	 * @param bytes Package bytes
	 * @param caller Identity of the caller
	 * @return result
	 */
	public ResultMng installPackage(byte[] bytes, AuthToken caller) {
		ResultMng result = new ResultMng();

		if ((bytes == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(caller)) {
					result.setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					result.setCode(Globals.getContainer().getPackageManager().installPackage(bytes, true));
				}
			} catch (Exception e) {
				logger.error("installPackage", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	/**
	 * Upgrades an installed package
	 * 
	 * @param packageID Package identifier
	 * @param bytes Package bytes
	 * @param caller Identity of the caller
	 * @return result
	 */
	public ResultMng upgradePackage(PackageId packageID, byte[] bytes, AuthToken caller) {
		ResultMng result = new ResultMng();

		if ((packageID == null) || (bytes == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(caller)) {
					result.setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					result.setCode(Globals.getContainer().getPackageManager()
							.upgradePackage(packageID, bytes));
				}
			} catch (Exception e) {
				logger.error("upgradePackage", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	/**
	 * Uninstalls an installed extension package
	 * 
	 * @param packageID Package identifier
	 * @param caller Identity of the caller
	 * @return result
	 */
	public ResultMng uninstallPackage(PackageId packageID, AuthToken caller) {
		ResultMng result = new ResultMng();

		if ((packageID == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(caller)) {
					result.setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					result.setCode(Globals.getContainer().getPackageManager()
							.uninstallPackage(packageID));
				}
			} catch (Exception e) {
				logger.error("uninstallPackage", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	/**
	 * Retrieves the specified package
	 * 
	 * @param packageID Package identifier
	 * @param caller Identity of the caller
	 * @return package
	 */
	public ResultPackageMng getPackage(PackageId packageID, AuthToken caller) {
		ResultPackageMng result = new ResultPackageMng();
		result.setStatus(new ResultMng());

		if ((packageID == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(caller)) {
					result.getStatus().setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					ExtensionPackage ext = Globals.getContainer().getPackageManager()
							.getPackage(packageID);

					if (ext != null) {
						result.getResult().add(Converter.fill(ext));
					}
				}
			} catch (Exception e) {
				logger.error("getPackage", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Retrieves all installed extension packages
	 * 
	 * @param caller Identity of the caller
	 * @return package
	 */
	public ResultPackageMng getPackages(AuthToken caller) {
		ResultPackageMng result = new ResultPackageMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(caller)) {
					result.getStatus().setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					ExtensionPackage[] exts = Globals.getContainer().getPackageManager().getPackages();
					Converter.fillPackages(result.getResult(), exts);
				}
			} catch (Exception e) {
				logger.error("getPackage", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Retrieves the specified plugin
	 * 
	 * @param packageID Package identifier
	 * @param pluginID Plugin identifier
	 * @param caller Identity of the caller
	 * @return plugin
	 */
	public ResultPluginMng getPlugin(PackageId packageID, PluginId pluginID, AuthToken caller) {
		ResultPluginMng result = new ResultPluginMng();
		result.setStatus(new ResultMng());

		if ((packageID == null) || (pluginID == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				// XXX: this is unnecessarily restrictive: prevents valid
				// queries from sites/brokers/sms
				// if (!isAdmin(caller)) {
				// result.getStatus().setCode(ManageExtensionsApiConstants.ErrorAccessDenied);
				// } else {
				Plugin plg = Globals.getContainer().getPluginManager()
						.getPlugin(packageID, pluginID);

				if (plg != null) {
					result.getResult().add(Converter.fill(plg));
				}
				// }
			} catch (Exception e) {
				logger.error("getPlugin", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Retrieves all plugins from the specified type and actor type
	 * 
	 * @param packageID Package identifier
	 * @param type Plugin type
	 * @param actorType Actor type
	 * @param caller Identity of the caller
	 * @return plugin
	 */
	public ResultPluginMng getPlugins(PackageId packageID, int type, int actorType, AuthToken caller) {
		ResultPluginMng result = new ResultPluginMng();
		result.setStatus(new ResultMng());

		if ((packageID == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				// if (!isAdmin(caller)) {
				// result.getStatus().setCode(ManageExtensionsApiConstants.ErrorAccessDenied);
				// } else {
				Plugin[] plgs = Globals.getContainer().getPluginManager()
						.getPlugins(packageID, type, actorType);
				Converter.fillPlugin(result.getResult(), plgs);
				// }
			} catch (Exception e) {
				logger.error("getPlugins", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/*
	 * ========================================================================
	 * Configuration Management
	 * ========================================================================
	 */

	/**
	 * Integrates the specified XML configuration into the container
	 * 
	 * @param configuration Serialization of the configuration
	 * @param caller Identity of the caller
	 * @return result
	 */
	public synchronized ResultMng addConfiguration(byte[] configuration, AuthToken caller) {
		ResultMng result = new ResultMng();

		if ((configuration == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(caller)) {
					result.setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					Globals.getContainer().loadConfiguration(configuration);
				}
			} catch (Exception e) {
				logger.error("addConfiguration", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	/**
	 * Integrates the specified XMl configuration into the container
	 * 
	 * @param stream Input stream for the configuration file
	 * @param caller Identity of the caller
	 * @return result
	 */
	public synchronized ResultMng addConfiguration(InputStream stream, AuthToken caller) {
		ResultMng result = new ResultMng();

		if ((stream == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(caller)) {
					result.setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					Globals.getContainer().loadConfiguration(stream);
				}
			} catch (Exception e) {
				logger.error("addConfiguration", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	/**
	 * Configures the container.
	 * 
	 * @param configuration Serialization of the configuration
	 * @param caller Identity of the caller
	 * @return result
	 */
	public synchronized ResultMng configure(byte[] configuration, AuthToken caller) {
		ResultMng result = new ResultMng();

		if ((configuration == null) || (caller == null)) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(caller)) {
					result.setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					Globals.getContainer().loadConfiguration(configuration);
				}
			} catch (Exception e) {
				logger.error("configure", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	/*
	 * ========================================================================
	 * Actor Management
	 * ========================================================================
	 */

	/**
	 * Returns all actors operable by the specified user
	 * 
	 * @param caller User credentials
	 * @return all actors
	 */
	public ResultActorMng getActors(AuthToken caller) {
		ResultActorMng result = new ResultActorMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				ArrayList<IActor> list = getActors(OrcaConstants.ActorTypeAll, caller);
				Converter.fillActor(result.getResult(), list);
			} catch (Exception e) {
				logger.error("getActors", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Obtains all actors operable by the specified user. (Accesses information
	 * from the database).
	 * 
	 * @param caller User credentials
	 * @return all actors
	 */
	public ResultActorMng getActorsFromDatabase(AuthToken caller) {
		ResultActorMng result = new ResultActorMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				/*
				 * Obtain all actors from the database and return only the
				 * actors operable by the user
				 */
				Vector<Properties> v = null;
				boolean go = true;

				try {
					v = getContainerManagementDatabase().getActors();
				} catch (Exception e) {
					logger.error("getActorsDB:db", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go && (v != null) && (v.size() > 0)) {
					for (int i = 0; i < v.size(); i++) {
						Properties p = (Properties) v.get(i);
						String actorName = Actor.getName(p);

						if (checkAccess(actorName, caller)) {
							result.getResult().add(Converter.fillActor(p));
						}
					}
				}
			} catch (Exception e) {
				logger.error("getActorsDB", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Obtains all actors operable by the specified user. (Accesses information
	 * from the database).
	 * 
	 * @param name Actor name search string
	 * @param type Actor type
	 * @param status Actor status
	 * @param caller User credentials
	 * @return returns all actors
	 */
	public ResultActorMng getActorsFromDatabase(String name, int type, int status, AuthToken caller) {
		ResultActorMng result = new ResultActorMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				/*
				 * Obtain all actors from the database and return only the
				 * actors operable by the user
				 */
				Vector v = null;
				boolean go = true;

				try {
					v = getContainerManagementDatabase().getActors(name, type);
				} catch (Exception e) {
					logger.error("getActorsDB:db", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go && (v != null) && (v.size() > 0)) {
					for (int i = 0; i < v.size(); i++) {
						Properties p = (Properties) v.get(i);
						String actorName = Actor.getName(p);

						if (checkAccess(actorName, caller)) {
							// filter by status
							boolean ok = false;

							switch (status) {
							case 0:
								ok = true;

								break;

							case 1:
								ok = (ActorRegistry.getActor(actorName) != null);

								break;

							case 2:
								ok = (ActorRegistry.getActor(actorName) == null);

								break;
							}

							if (ok) {
								result.getResult().add(Converter.fillActor(p));
							}
						}
					}
				}
			} catch (Exception e) {
				logger.error("getActorsDB", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Returns all service managers operable by this actor (uses the
	 * ActorRegistry).
	 * 
	 * @param caller User credentials
	 * @return all service managers 
	 */
	public ResultActorMng getServiceManagers(AuthToken caller) {
		ResultActorMng result = new ResultActorMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				ArrayList<IActor> list = getActors(OrcaConstants.ActorTypeServiceManager, caller);
				Converter.fillActor(result.getResult(), list);
			} catch (Exception e) {
				logger.error("getServiceManagers", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Returns all brokers operable by this actor (uses the ActorRegistry)
	 * 
	 * @param caller User credentials
	 * @return all brokers 
	 */
	public ResultActorMng getBrokers(AuthToken caller) {
		ResultActorMng result = new ResultActorMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				ArrayList<IActor> list = getActors(OrcaConstants.ActorTypeBroker, caller);
				Converter.fillActor(result.getResult(), list);
			} catch (Exception e) {
				logger.error("getBrokers", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Obtains all sites operable by the specified user (uses the ActorRegistry)
	 * 
	 * @param caller User credentials
	 * @return all sites 
	 */
	public ResultActorMng getAuthorities(AuthToken caller) {
		ResultActorMng result = new ResultActorMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				ArrayList<IActor> list = getActors(OrcaConstants.ActorTypeSiteAuthority, caller);
				Converter.fillActor(result.getResult(), list);
			} catch (Exception e) {
				logger.error("getAuthorities", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Creates a new actor
	 * 
	 * @param actor Actor description
	 * @param brokers Brokers to associate this actor with caller Identity of the
	 *            caller
	 * @param caller caller auth token 
	 * @return returns the result 
	 */
	public ResultMng addActor(ActorCreateMng actor, ProxyMng[] brokers, AuthToken caller) {
		ResultMng result = new ResultMng();

		if ((actor == null) || (caller == null)) {
			// XXX: probably we need some more argument checking here.
			// For now some errors that should result in InvalidArguments will
			// be mapped to InternalError.
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(caller)) {
					result.setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					/* create the actor object */
					IActor myactor = createActor(actor);

					/*
					 * recover is now not part of initialize: we must invoke it
					 * even for newly created actors.
					 */
					myactor.recover();
					/* attach brokers */
					attachBrokers(myactor, brokers);
					/* start ticking */
					Globals.getContainer().register(myactor);
					/* start applications */
					installApplications(actor, myactor, caller);
				}
			} catch (Exception e) {
				logger.error("addActor", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}

		return result;
	}

	/**
	 * Starts an inactive actor
	 * 
	 * @param actorName Name of the actor
	 * @param user User credentials
	 * @return result
	 */
	public ResultMng startActor(String actorName, AuthToken user) {
		ResultMng result = new ResultMng();

		try {
			if ((actorName == null) || (user == null)) {
				result.setCode(OrcaConstants.ErrorInvalidArguments);
			} else if (!checkAccess(actorName, user)) {
				result.setCode(OrcaConstants.ErrorAccessDenied);
			} else if (ActorRegistry.getActor(actorName) != null) {
				result.setCode(OrcaConstants.ErrorActorIsActive);
			} else {
				Properties p = getActorDB(actorName);

				if (p == null) {
					result.setCode(OrcaConstants.ErrorInvalidActor);
				} else {
					Globals.getContainer().recoverActor(p);
				}
			}
		} catch (Exception e) {
			logger.error("startActor", e);
			result.setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result, e);
		}

		return result;
	}

	/**
	 * Stops an active actor
	 * 
	 * @param actorName actor name
	 * @param user user
	 * @return result
	 */
	public ResultMng stopActor(String actorName, AuthToken user) {
		ResultMng result = new ResultMng();

		try {
			if ((actorName == null) || (user == null)) {
				result.setCode(OrcaConstants.ErrorInvalidArguments);
			} else if (!checkAccess(actorName, user)) {
				result.setCode(OrcaConstants.ErrorAccessDenied);
			} else {
				IActor actor = ActorRegistry.getActor(actorName);

				if (actor == null) {
					result.setCode(OrcaConstants.ErrorActorIsNotActive);
				} else {
					actor.stop();
					Globals.getContainer().unregisterActor(actor);

					// by now there should be no references to this actor in the
					// system and the garbage collector eventually should remove
					// it completely
				}
			}
		} catch (Exception e) {
			logger.error("stop", e);
			result.setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result, e);
		}

		return result;
	}
	
	/**
	 * Removes an actor.
	 * 
	 * @param actorName Name of the actor
	 * @param user User credentials
	 * @return result
	 */
	public ResultMng removeActor(String actorName, AuthToken user) {
		ResultMng result = null;

		if ((actorName == null) || (user == null)) {
			result = new ResultMng();
			result.setCode(OrcaConstants.ErrorInvalidArguments);

			return result;
		}

		if (ActorRegistry.getActor(actorName) != null) {
			// this actor is online
			result = stopActor(actorName, user);

			if (result.getCode() != 0) {
				return result;
			}
		}

		result = new ResultMng();

		try {
			if (!checkAccess(actorName, user)) {
				result.setCode(OrcaConstants.ErrorAccessDenied);
			} else {
				// remove the actor metadata;
				Globals.getContainer().removeActor(actorName);
				Globals.getContainer().removeActorDatabase(actorName);
			}
		} catch (Exception e) {
			logger.error("removeActor", e);
			result.setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result, e);
		}

		return result;
	}

	/*
	 * ========================================================================
	 * Helper functions for actor management
	 * ========================================================================
	 */

	/**
	 * Installs requested applications for this actor
	 * 
	 * @param actor Actor description
	 * @param myactor Actor object
	 * @param caller caller 
	 * @throws Exception in case of error
	 */
	protected void installApplications(ActorCreateMng actor, IActor myactor, AuthToken caller) throws Exception {
//		boolean createSlice = true;
//
//		if (myactor.getType() == IActor.TypeSiteAuthority) {
//			createSlice = false;
//		}
//
//		ManagementObject mo = getManagerObject(myactor.getGuid(), caller);
//
//		if (mo instanceof ServiceManagerManagerObject) {
//			if (actor.getApplicationPlugins() != null) {
//				List<PluginCreateMng> apps = actor.getApplicationPlugins().getPlugins();
//
//				for (PluginCreateMng p : apps) {
//					((ServiceManagerManagerObject) mo).addApplication(p, caller);
//					createSlice = false;
//				}
//			}
//		}
//
//		if (createSlice) {
//			attachDefaultSlice(myactor);
//		}
	}

	protected void attachDefaultSlice(IActor actor) throws Exception {
		ISlice slice = (ISlice) SliceFactory.getInstance().create(actor.getName());
		actor.registerSlice(slice);
	}

	/**
	 * Attaches the brokers to this actor
	 * 
	 * @param actor Actor object
	 * @param brokers Array of brokers
	 * @throws Exception in case of errors
	 */
	protected void attachBrokers(IActor actor, ProxyMng[] brokers) throws Exception {
		if (actor.getType() != OrcaConstants.ActorTypeSiteAuthority) {
			if (brokers != null) {
				for (int i = 0; i < brokers.length; i++) {
					IBrokerProxy proxy = Converter.getAgentProxy(brokers[i]);

					if (proxy != null) {
						if (actor instanceof ServiceManager) {
							((ServiceManager) actor).addBroker(proxy);
						} else if (actor instanceof Broker) {
							((Broker) actor).addBroker(proxy);
						} else {
							throw new Exception("Unuspported actor type: " + actor.getClass().getCanonicalName());
						}
					} else {
						logger.error("Invalid proxy");
					}
				}
			}
		}
	}

	/**
	 * Attaches the actor controller
	 * 
	 * @param actor Actor object
	 * @param controller Controller description
	 * @return controller factory
	 * @throws Exception in case of error
	 */
	protected IControllerFactory attachController(IActor actor, PluginCreateMng controller) throws Exception {
		Plugin controllerPlugin = Globals.getContainer().getPluginManager()
				.getPlugin(new PackageId(controller.getPackageId()), new PluginId(controller.getId()));

		if (controllerPlugin == null) {
			throw new Exception("Missing actor plugin");
		}

		if (controllerPlugin.getPluginType() != Plugin.TypeActorController) {
			throw new Exception("Invalid plugin type");
		}

		if (!controllerPlugin.isFactory()) {
			throw new Exception("The controller plugin is not a factory");
		}

		if (controllerPlugin.getClassName() == null) {
			throw new Exception("Missing plugin class name");
		}

		Class c = Class.forName(controllerPlugin.getClassName());
		Object obj = c.newInstance();

		if (!(obj instanceof IControllerFactory && obj instanceof IPluginFactory)) {
			throw new Exception("The controller plugin does not implement all required interfaces");
		}

		IControllerFactory controllerFactory = (IControllerFactory) obj;
		IPluginFactory pluginFactory = (IPluginFactory) obj;

		if (controller.getConfigurationString() != null) {
			pluginFactory.configure(controller.getConfigurationString());
		}

		controllerFactory.setActor(actor);
		pluginFactory.create();

		if (!(pluginFactory.getObject() instanceof IPolicy)) {
			throw new Exception("factory did not return an instance of IPolicy");
		}

		/* attach the policy */
		IPolicy policy = (IPolicy) pluginFactory.getObject();
		actor.setPolicy(policy);

		// XXX: we cannot attach these here yet: the actor is not registered
		// with the database
		return controllerFactory;
	}

	/**
	 * Creates the actor object
	 * 
	 * @param actor Actor description
	 * @return actor created
	 * @throws Exception in case of error
	 */
	protected IActor createActor(ActorCreateMng actor) throws Exception {
		PluginCreateMng actorPluginMng = actor.getActorPlugin();

		if (actorPluginMng == null) {
			throw new Exception("Missing actor plugin");
		}

		Plugin actorPlugin = Globals.getContainer().getPluginManager()
				.getPlugin(new PackageId(actorPluginMng.getPackageId()), new PluginId(actorPluginMng.getId()));

		if (actorPlugin == null) {
			throw new Exception("Missing actor plugin");
		}

		if (!(actorPlugin.getPluginType() == Plugin.TypeActorObject)) {
			throw new Exception("Invalid plugin type");
		}

		if (!actorPlugin.isFactory()) {
			throw new Exception("The actor plugin is not a factory");
		}

		if (actorPlugin.getClassName() == null) {
			throw new Exception("Missing plugin class name");
		}

		Class c = Class.forName(actorPlugin.getClassName());
		Object obj = c.newInstance();

		if (!(obj instanceof IActorFactory)) {
			throw new Exception("The actor plugin does not implement all required interfaces");
		}

		IActorFactory actorFactory = (IActorFactory) obj;

		actorFactory.setName(actor.getName());
		actorFactory.setDescription(actor.getName());

		if (actorPlugin.getConfigProperties() != null) {
			actorFactory.configure(actorPlugin.getConfigProperties());
		}

		if (actorPluginMng.getConfigurationString() != null) {
			actorFactory.configure(actorPluginMng.getConfigurationString());
		}

		// if (actorFactory instanceof SiteFactory) {
		// ((SiteFactory) actorFactory).setSiteName(actor.getSiteName());
		// }
		actorFactory.create();

		IActor result = actorFactory.getActor();

		if (result == null) {
			throw new Exception("Factory failed to create actor object");
		}

		IControllerFactory controllerFactory = null;

		if (actor.getControllerPlugin() != null) {
			/* attach the actor controller */
			controllerFactory = attachController(result, actor.getControllerPlugin());

			/* generate the security configuration for the actor */
			Axis2ClientSecurityConfigurator conf = Axis2ClientSecurityConfigurator.getInstance();

			if (conf.createActorConfiguration(Globals.HomeDirectory, result.getGuid().toString()) != 0) {
				throw new Exception("cannot create security files");
			}

			/* we must initialize the keystore */
			logger.debug("initializing actor keystore for: " + actor.getName());
			result.initializeKeyStore();
			logger.debug("initializing actor keystore for: " + actor.getName() + " OK");
			/* now we can initialize the actor */
			result.initialize();
			/* register the actor with the container */
			Globals.getContainer().registerActor(result);
		} else {
			throw new Exception("Custom creation not supported yet");
		}

		/*
		 * Ass of now we can only register one manager object and portal plugin
		 * for the actor and one manager object and portal plugin for its
		 * policy. We should enhance the interface to support multiple managers
		 * and portal plugin descriptors.
		 */

		/* register the actor manager and plugin descriptor */
		ManagementObject man = actorFactory.getManager();

		if (man != null) {
			Globals.getContainer().getManagementObjectManager().registerManagerObject(man);
		} else {
			throw new Exception("No manager object!");
		}

		/* register the policy manager and plugin descriptor */
		man = controllerFactory.getManager();

		if (man != null) {
			Globals.getContainer().getManagementObjectManager().registerManagerObject(man);
		}

		return result;
	}

	/*
	 * ========================================================================
	 * Manager and portal plugin objects
	 * ========================================================================
	 */

	/**
	 * Returns the specified management object. 
	 * @param objectID management object identifier
	 * @return the ManagementObject on success, or null if not found
	 * @throws  OrcaManagementException in case of error
	 */
	public IManagementObject getManagementObject(ID objectID) throws OrcaManagementException {
		return Globals.getContainer().getManagementObjectManager().getManagementObject(objectID);
	}


	/*
	 * ========================================================================
	 * Proxies
	 * ========================================================================
	 */

	/**
	 * Returns all broker proxies registered in this container (uses the
	 * ActorRegistry/ProxyRegistry).
	 * 
	 * @param protocol Protocol name
	 * @param caller User credentials
	 * @return all broker proxies
	 */
	public ResultProxyMng getBrokerProxies(String protocol, AuthToken caller) {
		ResultProxyMng result = new ResultProxyMng();
		result.setStatus(new ResultMng());

		if ((protocol == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				/*
				 * XXX: for now anyone can call this function
				 */
				IProxy[] proxies = ActorRegistry.getBrokerProxies(protocol);
				Converter.fillProxy(result.getResult(), proxies);
			} catch (Exception e) {
				logger.error("getBrokerProxies", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Returns all site proxies registered in this container (uses the
	 * ActorRegistry/ProxyRegistry)
	 * 
	 * @param protocol Protocol name
	 * @param caller User credentials
	 * @return all broker proxies
	 */
	public ResultProxyMng getSiteProxies(String protocol, AuthToken caller) {
		ResultProxyMng result = new ResultProxyMng();
		result.setStatus(new ResultMng());

		if ((protocol == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				/*
				 * XXX: for now anyone can call this function
				 */
				IProxy[] proxies = ActorRegistry.getSiteProxies(protocol);
				Converter.fillProxy(result.getResult(), proxies);
			} catch (Exception e) {
				logger.error("getSiteProxies", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Returs all broker and site proxies registered in this container.
	 * 
	 * @param protocol Protocol
	 * @param caller Caller identity
	 * @return all broker and site proxies
	 */
	public ResultProxyMng getProxies(String protocol, AuthToken caller) {
		ResultProxyMng result = new ResultProxyMng();
		result.setStatus(new ResultMng());

		if ((protocol == null) || (caller == null)) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				/*
				 * XXX: for now anyone can call this function
				 */
				IProxy[] proxies = ActorRegistry.getProxies(protocol);
				Converter.fillProxy(result.getResult(), proxies);
			} catch (Exception e) {
				logger.error("getProxies", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	public ResultCertificateMng getCertificate() {
		ResultCertificateMng result = new ResultCertificateMng();
		result.setStatus(new ResultMng());

		try {
			Certificate cert = Globals.getContainer().getCertificate();
			result.getResult().add(Converter.fill(cert));
		} catch (Exception e) {
			logger.error("getCertificate", e);
			result.getStatus().setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result.getStatus(), e);
		}
		return result;
	}
	
	public ResultCertificateMng getCertificate(String actorName) {
		ResultCertificateMng result = new ResultCertificateMng();
		result.setStatus(new ResultMng());
		try {
			if (actorName == null) {
				result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
				return result;
			}
			IActor actor = ActorRegistry.getActor(actorName);
			if (actor == null) {
				result.getStatus().setCode(OrcaConstants.ErrorNoSuchActor);
				return result;
			}
			ID guid = actor.getGuid();
			return getCertificate(guid);
		} catch (Exception e) {
			logger.error("getCertificate", e);
			result.getStatus().setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result.getStatus(), e);
		}
		return result;
	}

	public ResultCertificateMng getCertificate(ID actorGuid) {
		ResultCertificateMng result = new ResultCertificateMng();
		result.setStatus(new ResultMng());

		try {
			if (actorGuid == null) {
				result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
				return result;
			}
			Certificate cert = Globals.getContainer().getCertificate(actorGuid);
			result.getResult().add(Converter.fill(cert));
		} catch (Exception e) {
			logger.error("getCertificate", e);
			result.getStatus().setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result.getStatus(), e);
		}
		return result;
	}
	
	/*
	 * ========================================================================
	 * Actor creation
	 * ========================================================================
	 */

	/**
	 * Returns all actors of the specified type operable by the specified user
	 * 
	 * @param type Actor type (Converter.Type*)
	 * @param user <code>AuthToken</code> of the user
	 * @return list of all actors of the specified type operable by the user
	 */
	protected ArrayList<IActor> getActors(int type, AuthToken user) {
		ArrayList<IActor> list = new ArrayList<IActor>();
		IActor[] actors = ActorRegistry.getActors();

		if (actors != null) {
			for (int i = 0; i < actors.length; i++) {
				IActor actor = actors[i];
				int atype = actor.getType();

				if ((type == OrcaConstants.ActorTypeAll) || (type == atype)) {
					if (checkAccess(actor.getName(), user)) {
						list.add(actors[i]);
					}
				}
			}
		}

		return list;
	}

	/*
	 * ========================================================================
	 * Fetching actors from the database (helper methods)
	 * ========================================================================
	 */

	/**
	 * Obtains the specified actor from the database
	 * 
	 * @param name actor name
	 * @return actor properties
	 */
	protected Properties getActorDB(String name) {
		try {
			Vector<Properties> v = Globals.getContainer().getDatabase().getActor(name);

			if ((v != null) && (v.size() > 0)) {
				return (Properties) v.get(0);
			}

			return null;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Obtains all actors from the database
	 * 
	 * @return vector of properties
	 */
	protected Vector<Properties> getActorsDB() {
		try {
			return Globals.getContainer().getDatabase().getActors();
		} catch (Exception e) {
			logger.error("getActors", e);

			return null;
		}
	}

	/**
	 * Obtains all sites from the database
	 * 
	 * @return vector of properties
	 */
	protected Vector<Properties> getSitesDB() {
		Vector<Properties> result = null;
		Vector<Properties> myactors = getActorsDB();

		if (myactors != null) {
			result = new Vector<Properties>();

			for (int i = 0; i < myactors.size(); i++) {
				Properties p = (Properties) myactors.get(i);

				if (Actor.getType(p) == OrcaConstants.ActorTypeSiteAuthority) {
					result.add(p);
				}
			}
		}

		return result;
	}

	protected Unit getInventory(String name) {
		try {
			Properties p = Globals.getContainer().getDatabase().getInventory(name);
			if (p != null) {
				Unit u = new Unit();
				PersistenceUtils.restore(u, p);
				return u;
			}

			return null;
		} catch (Exception e) {
			logger.error("getInventory", e);

			return null;
		}
	}
	
	public ResultMng transferInventory(String name, String actorName, AuthToken token) {
		ResultMng result = new ResultMng();

		if (name == null || actorName == null || token == null) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(token)) {
					result.setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					IActor actor = ActorRegistry.getActor(actorName);
					if (actor == null) {
						result.setCode(OrcaConstants.ErrorNoSuchActor);
					}
					if (logger.isDebugEnabled()) {
						logger.debug("transferring inventory item to actor: inventory=" + name + ",actor="
								+ actor.getGuid());
					}

					/* register all keys */
					int code = 0;

					/* update the database */
					Globals.getContainer().getDatabase().transferInventory(name, actor.getGuid());

					result.setCode(code);
				}
			} catch (Exception e) {
				logger.error("untransferInventory", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}
		return result;
	}

	public ResultMng untransferInventory(String name, String actorName, AuthToken token) {
		ResultMng result = new ResultMng();

		if (name == null || actorName == null || token == null) {
			result.setCode(OrcaConstants.ErrorInvalidArguments);
		} else {
			try {
				if (!isAdmin(token)) {
					result.setCode(OrcaConstants.ErrorAccessDenied);
				} else {
					IActor actor = ActorRegistry.getActor(actorName);
					if (actor == null) {
						result.setCode(OrcaConstants.ErrorNoSuchActor);
					}
					if (logger.isDebugEnabled()) {
						logger.debug("transferring inventory item to actor: inventory=" + name + ",actor="
								+ actor.getGuid());
					}

					// fixme: unregister the keys
					/* update the database */
					Globals.getContainer().getDatabase().untransferInventory(name, actor.getGuid());

				}
			} catch (Exception e) {
				logger.error("untransferInventory", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}
		return result;
	}

	/**
	 * Retrieves the complete inventory
	 * @param caller caller auth token 
	 * @return complete inventory
	 */
	public ResultUnitMng getInventory(AuthToken caller) {
		ResultUnitMng result = new ResultUnitMng();
		result.setStatus(new ResultMng());

		if (caller == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else if (!isAdmin(caller)) {
			result.getStatus().setCode(OrcaConstants.ErrorAccessDenied);
		} else {
			try {
				Vector<Properties> v = null;
				boolean go = true;

				try {
					v = Globals.getContainer().getDatabase().getInventory();
				} catch (Exception e) {
					logger.error("getInventory:db", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					Converter.fillUnits(result.getResult(), v);
				}
			} catch (Exception e) {
				logger.error("getInventory", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	/**
	 * Retrieves all machines
	 * @param unit unit id
	 * @param caller caller auth token 
	 * @return Retrieves all machines
	 */
	public ResultUnitMng getInventory(UnitID unit, AuthToken caller) {
		ResultUnitMng result = new ResultUnitMng();
		result.setStatus(new ResultMng());

		if (caller == null || unit == null) {
			result.getStatus().setCode(OrcaConstants.ErrorInvalidArguments);
		} else if (!isAdmin(caller)) {
			result.getStatus().setCode(OrcaConstants.ErrorAccessDenied);
		} else {
			try {
				Properties p = null;
				boolean go = true;

				try {
					p = Globals.getContainer().getDatabase().getInventory(unit.toString());
				} catch (Exception e) {
					logger.error("getInventory:db", e);
					result.getStatus().setCode(OrcaConstants.ErrorDatabaseError);
					setExceptionDetails(result.getStatus(), e);
					go = false;
				}

				if (go) {
					result.getResult().add(Converter.fillUnit(p));
				}
			} catch (Exception e) {
				logger.error("getInventory", e);
				result.getStatus().setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result.getStatus(), e);
			}
		}

		return result;
	}

	public ResultMng addInventory(UnitMng unit, AuthToken caller) {
		ResultMng result = new ResultMng();
		if (unit == null || caller == null) {
			result.setCode(OrcaConstants.ErrorNoSuchActor);
		} else if (!isAdmin(caller)) {
			result.setCode(OrcaConstants.ErrorAccessDenied);
		} else {
			try {
				Properties p = Converter.fill(unit.getProperties());
				p.setProperty(UnitProperties.UnitID, new UnitID().toString());
				p.setProperty(UnitProperties.UnitInternalState, Integer.toString(UnitState.ACTIVE.ordinal()));
				Unit u = new Unit();
				PersistenceUtils.restore(u, p);
				Globals.getContainer().getDatabase().addInventory(u);
			} catch (Exception e) {
				logger.error("addInventory", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}

		}
		return result;
	}

	public ResultMng updateInventory(UnitMng unit, AuthToken caller) {
		ResultMng result = new ResultMng();
		if (unit == null || caller == null) {
			result.setCode(OrcaConstants.ErrorNoSuchActor);
		} else if (!isAdmin(caller)) {
			result.setCode(OrcaConstants.ErrorAccessDenied);
		} else {
			try {
				Unit u = Converter.fill(unit);
				Globals.getContainer().getDatabase().updateInventory(u);
			} catch (Exception e) {
				logger.error("updateInventory", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}

		}
		return result;
	}

	public ResultMng removeInventory(String invName, AuthToken caller) {
		Properties p;
		ResultMng result = new ResultMng();
		try {
			p = Globals.getContainer().getDatabase().getInventory(invName);
			Globals.getContainer().getDatabase().removeInventory(new UnitID(p.getProperty("unit.id")));
		} catch (Exception e) {
			logger.error("removeInventory", e);
			result.setCode(OrcaConstants.ErrorInternalError);
			setExceptionDetails(result, e);
		}
		return result;
	}

	public ResultMng removeInventory(UnitID unitId, AuthToken caller) {
		ResultMng result = new ResultMng();
		if (unitId == null || caller == null) {
			result.setCode(OrcaConstants.ErrorNoSuchActor);
		} else if (!isAdmin(caller)) {
			result.setCode(OrcaConstants.ErrorAccessDenied);
		} else {
			try {
				Globals.getContainer().getDatabase().removeInventory(unitId);
			} catch (Exception e) {
				logger.error("removeInventory", e);
				result.setCode(OrcaConstants.ErrorInternalError);
				setExceptionDetails(result, e);
			}
		}
		return result;
	}
	
	// CHECKBEFORESHIPPING: move this to the database and generate it at random for each new net.exogeni.orca container
	private static byte[] PASSWORD_HASH_SALT = new byte[] { 10, 20, 13, 23, 56, 78, 95, 76, 28, 34, 22, 12, 0, 0, 9, 39};

	public static String hashPassword(String password) throws Exception {
		KeySpec spec = new PBEKeySpec(password.toCharArray(), PASSWORD_HASH_SALT, 2048, 160);
		SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		byte[] hash = f.generateSecret(spec).getEncoded();
		return new BigInteger(1, hash).toString(16);
	}

	private static byte[] LOGIN_HASH_SALT = new byte[] { 100, 27, 9, 0, 7, 8, 9, 15, 8, 4, 99, 3, 45, 75, 94, 93};
	public static String getLoginToken(String login) throws Exception {
		KeySpec spec = new PBEKeySpec(login.toCharArray(), LOGIN_HASH_SALT, 2048, 160);
		SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		byte[] hash = f.generateSecret(spec).getEncoded();
		return new BigInteger(1, hash).toString(16);
	}
}
