/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.manage.internal.local;

import java.security.cert.Certificate;
import java.util.List;

import net.exogeni.orca.extensions.PackageId;
import net.exogeni.orca.extensions.PluginId;
import net.exogeni.orca.extensions.internal.Plugin;
import net.exogeni.orca.manage.IOrcaActor;
import net.exogeni.orca.manage.IOrcaAuthority;
import net.exogeni.orca.manage.IOrcaBroker;
import net.exogeni.orca.manage.IOrcaComponent;
import net.exogeni.orca.manage.IOrcaContainer;
import net.exogeni.orca.manage.IOrcaServiceManager;
import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.manage.OrcaManagementException;
import net.exogeni.orca.manage.OrcaProxyProtocolDescriptor;
import net.exogeni.orca.manage.beans.ActorCreateMng;
import net.exogeni.orca.manage.beans.ActorMng;
import net.exogeni.orca.manage.beans.PackageMng;
import net.exogeni.orca.manage.beans.PluginMng;
import net.exogeni.orca.manage.beans.ProxyMng;
import net.exogeni.orca.manage.beans.ResultActorMng;
import net.exogeni.orca.manage.beans.ResultCertificateMng;
import net.exogeni.orca.manage.beans.ResultPackageMng;
import net.exogeni.orca.manage.beans.ResultPluginMng;
import net.exogeni.orca.manage.beans.ResultProxyMng;
import net.exogeni.orca.manage.beans.ResultUnitMng;
import net.exogeni.orca.manage.beans.ResultUserMng;
import net.exogeni.orca.manage.beans.UnitMng;
import net.exogeni.orca.manage.beans.UserMng;
import net.exogeni.orca.manage.internal.ContainerManagementObject;
import net.exogeni.orca.manage.internal.ManagementObject;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.common.UnitID;
import net.exogeni.orca.util.CertificateUtils;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.ReflectionUtils;

public class LocalContainer extends LocalProxy implements IOrcaContainer {
	protected ContainerManagementObject manager;

	public LocalContainer(ManagementObject manager, AuthToken auth) {
		super(manager, auth);
		if (!(manager instanceof ContainerManagementObject)) {
			throw new RuntimeException("Invalid management object. Required: "
					+ ContainerManagementObject.class.getCanonicalName());
		}
		this.manager = (ContainerManagementObject) manager;
	}

	public boolean isLogged() {
		return auth != null;
	}

	public boolean login() {
		clearLast();
		try {
			// NOTE: We consider callers of the management interface
			// that reside in the same JVM to be trusted and we do not
			// require credentials for using the management interface
			// from within the JVM.
			auth = manager.loginInternal();
		} catch (Exception e) {
			lastException = e;
		}
		return false;
	}

	public boolean login(String user, String password) {
		clearLast();
		lastException = new RuntimeException(
				"Authentication is not supported/required for local proxies");
		return false;
	}

	public boolean logout() throws OrcaManagementException {
		clearLast();
		try {
			manager.logout(auth);
			return true;
		} catch (Exception e) {
			lastException = e;
			return false;
		} finally {
			auth = null;
		}
	}

	public IOrcaComponent getManagementObject(ID key) throws OrcaManagementException {
		try {
			// find the manager object
			ManagementObject obj = (ManagementObject) manager.getManagementObject(key);
			if (obj == null) {
				return null;
			}

			// go through the set of supported proxies and find
			// the record for the current protocol (local)
			OrcaProxyProtocolDescriptor[] desc = obj.getProxies();
			if (desc == null) {
				throw new OrcaManagementException("Management object did not specify any proxies");
			}
			OrcaProxyProtocolDescriptor d = null;
			for (int i = 0; i < desc.length; i++) {
				if (OrcaConstants.ProtocolLocal.equals(desc[i].getProtocol())) {
					d = desc[i];
					break;
				}
			}

			if (d == null || d.getProxyClass() == null) {
				throw new OrcaManagementException("Manager object did not specify local proxy");
			}

			// instantiate the proxy passing the manager object and the auth
			// token to it
			try {
				return (IOrcaComponent) ReflectionUtils.createInstance(d.getProxyClass(),
						new Class<?>[] { ManagementObject.class, AuthToken.class }, obj, auth);
			} catch (Exception e) {
				throw new OrcaManagementException("Could not instantiate proxy", e);
			}
		} catch (OrcaManagementException e) {
			throw e;
		} catch (Exception e) {
			throw new OrcaManagementException("Could not obtain proxy", e);
		}
	}

	public IOrcaActor getActor(ID guid) {
		IOrcaComponent p = getManagementObject(guid);
		if (p != null) {
			try {
				return (IOrcaActor) p;
			} catch (Exception e) {
				lastException = e;
			}
		}
		return null;
	}

	public boolean addUser(UserMng user) {
		clearLast();
		try {
			lastStatus = manager.addUser(user, auth);
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public UserMng getUser(String login) {
		clearLast();
		try {
			ResultUserMng r = manager.getUser(login, auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult().get(0);
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<UserMng> getUsers() {
		clearLast();
		try {
			ResultUserMng r = manager.getUsers(auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public boolean removeUser(String user) {
		clearLast();
		try {
			lastStatus = manager.removeUser(user, auth);
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public boolean updateUser(UserMng user) {
		clearLast();
		try {
			lastStatus = manager.updateUser(user, auth);
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public boolean configure(byte[] configuration) {
		clearLast();
		try {
			lastStatus = manager.configure(configuration, auth);
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public boolean addConfiguration(byte[] configuration) {
		clearLast();
		try {
			lastStatus = manager.addConfiguration(configuration, auth);
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public boolean addActor(ActorCreateMng actor, ProxyMng[] brokers) {
		clearLast();
		try {
			lastStatus = manager.addActor(actor, brokers, auth);
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public Certificate getCertificate(ID actorGuid) {
		clearLast();
		try {
			ResultCertificateMng c = manager.getCertificate(actorGuid);
			lastStatus = c.getStatus();
			if (c.getStatus().getCode() != 0) {
				return null;
			}

			try {
				return CertificateUtils.decode(c.getResult().get(0).getContents());
			} catch (Exception e) {
				throw new OrcaManagementException("Could not obtain actor certificate", e);
			}
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public Certificate getCertificate() {
		clearLast();
		try {
			ResultCertificateMng c = manager.getCertificate();
			lastStatus = c.getStatus();
			if (c.getStatus().getCode() != 0) {
				return null;
			}

			try {
				return CertificateUtils.decode(c.getResult().get(0).getContents());
			} catch (Exception e) {
				throw new OrcaManagementException("Could not obtain actor certificate", e);
			}
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public List<ActorMng> getActors() {
		clearLast();
		try {
			ResultActorMng r = manager.getActors(auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<ActorMng> getActorsFromDatabase() {
		clearLast();
		try {
			ResultActorMng r = manager.getActorsFromDatabase(auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<ActorMng> getActorsFromDatabase(String name, int type, int status) {
		clearLast();
		try {
			ResultActorMng r = manager.getActorsFromDatabase(name, type, status, auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<ActorMng> getAuthorities() {
		clearLast();
		try {
			ResultActorMng r = manager.getAuthorities(auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<ActorMng> getBrokers() {
		clearLast();
		try {
			ResultActorMng r = manager.getBrokers(auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<ActorMng> getServiceManagers() {
		clearLast();
		try {
			ResultActorMng r = manager.getServiceManagers(auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<ProxyMng> getProxies(String protocol) {
		clearLast();
		try {
			ResultProxyMng r = manager.getProxies(protocol, auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<ProxyMng> getBrokerProxies(String protocol) {
		clearLast();
		try {
			ResultProxyMng r = manager.getBrokerProxies(protocol, auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<ProxyMng> getAuthorityProxies(String protocol) {
		clearLast();
		try {
			ResultProxyMng r = manager.getSiteProxies(protocol, auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public PackageMng getPackage(PackageId packageID) {
		clearLast();
		try {
			ResultPackageMng r = manager.getPackage(packageID, auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult().get(0);
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<PackageMng> getPackages() {
		clearLast();
		try {
			ResultPackageMng r = manager.getPackages(auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public boolean installPackage(byte[] bytes) {
		clearLast();
		try {
			lastStatus = manager.installPackage(bytes, auth);
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public boolean uninstallPackage(PackageId packageID) {
		clearLast();
		try {
			lastStatus = manager.uninstallPackage(packageID, auth);
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public boolean upgradePackage(PackageId packageID, byte[] bytes) {
		clearLast();
		try {
			lastStatus = manager.upgradePackage(packageID, bytes, auth);
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public PluginMng getPlugin(PackageId packageID, PluginId pluginID) {
		clearLast();
		try {
			ResultPluginMng r = manager.getPlugin(packageID, pluginID, auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult().get(0);
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<PluginMng> getPlugins(int type) {
		return getPlugins(null, type, OrcaConstants.ActorTypeAll);
	}

	public List<PluginMng> getPlugins(int type, int actorType) {
		return getPlugins(null, type, actorType);
	}

	public List<PluginMng> getPlugins(PackageId packageID, int type) {
		return getPlugins(packageID, type, OrcaConstants.ActorTypeAll);
	}

	public List<PluginMng> getPlugins(PackageId packageID) {
		return getPlugins(packageID, Plugin.TypeAll, OrcaConstants.ActorTypeAll);
	}

	public List<PluginMng> getPlugins(PackageId packageID, int type, int actorType) {
		clearLast();
		try {
			ResultPluginMng r = manager.getPlugins(packageID, type, actorType, auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0) {
				return r.getResult();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<UnitMng> getInventory() {
		clearLast();
		try {
			ResultUnitMng r = manager.getInventory(auth);
			lastStatus = r.getStatus();
			if (lastStatus.getCode() == 0) {
				return r.getResult();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public UnitMng getInventory(UnitID name) {
		clearLast();
		try {
			ResultUnitMng r = manager.getInventory(name, auth);
			lastStatus = r.getStatus();
			if (lastStatus.getCode() == 0) {
				return r.getResult().get(0);
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public boolean addInventory(UnitMng unit) {
		clearLast();
		try {
			lastStatus = manager.addInventory(unit, auth);
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public boolean updateInventory(UnitMng unit) {
		clearLast();
		try {
			lastStatus = manager.addInventory(unit, auth);
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public boolean removeInventory(UnitID name) {
		clearLast();
		try {
			lastStatus = manager.removeInventory(name, auth);
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public boolean setUserPassword(String login, String password) {
		clearLast();
		try {
			lastStatus = manager.setUserPassword(login, password, auth);
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public IOrcaServiceManager getServiceManager(ID guid) {
		IOrcaComponent comp = getManagementObject(guid);
		if (comp == null) {
			return null;
		}
		clearLast();
		try {
			return (IOrcaServiceManager) comp;
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public IOrcaBroker getBroker(ID guid) {
		IOrcaComponent comp = getManagementObject(guid);
		if (comp == null) {
			return null;
		}
		clearLast();
		try {
			return (IOrcaBroker) comp;
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public IOrcaAuthority getAuthority(ID guid) {
		IOrcaComponent comp = getManagementObject(guid);
		if (comp == null) {
			return null;
		}
		clearLast();
		try {
			return (IOrcaAuthority) comp;
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public boolean removeActor(ID actorGuid) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}

	public boolean startActor(ID actorGuid) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}

	public boolean stopActor(ID actorGuid) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}

	public boolean transferInventory(UnitID unit, ID actorGuid) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}

	public boolean untransferInventory(UnitID unit, ID actorGuid) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}
}
