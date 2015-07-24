package orca.manage.proxies.soap;

import java.security.cert.Certificate;
import java.util.List;

import orca.extensions.PackageId;
import orca.extensions.PluginId;
import orca.extensions.internal.Plugin;
import orca.manage.IOrcaActor;
import orca.manage.IOrcaAuthority;
import orca.manage.IOrcaBroker;
import orca.manage.IOrcaComponent;
import orca.manage.IOrcaContainer;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.OrcaConverter;
import orca.manage.OrcaManagementException;
import orca.manage.beans.ActorCreateMng;
import orca.manage.beans.ActorMng;
import orca.manage.beans.PackageMng;
import orca.manage.beans.PluginMng;
import orca.manage.beans.ProxyMng;
import orca.manage.beans.UnitMng;
import orca.manage.beans.UserMng;
import orca.manage.proxies.soap.beans.container.GetActorsFromDatabaseRequest;
import orca.manage.proxies.soap.beans.container.GetActorsFromDatabaseResponse;
import orca.manage.proxies.soap.beans.container.GetActorsRequest;
import orca.manage.proxies.soap.beans.container.GetActorsResponse;
import orca.manage.proxies.soap.beans.container.GetCertificateRequest;
import orca.manage.proxies.soap.beans.container.GetCertificateResponse;
import orca.manage.proxies.soap.beans.container.GetInventoryRequest;
import orca.manage.proxies.soap.beans.container.GetInventoryResponse;
import orca.manage.proxies.soap.beans.container.GetManagementObjectRequest;
import orca.manage.proxies.soap.beans.container.GetManagementObjectResponse;
import orca.manage.proxies.soap.beans.container.GetPackagesRequest;
import orca.manage.proxies.soap.beans.container.GetPackagesResponse;
import orca.manage.proxies.soap.beans.container.GetPluginRequest;
import orca.manage.proxies.soap.beans.container.GetPluginResponse;
import orca.manage.proxies.soap.beans.container.GetPluginsRequest;
import orca.manage.proxies.soap.beans.container.GetPluginsResponse;
import orca.manage.proxies.soap.beans.container.GetProxiesRequest;
import orca.manage.proxies.soap.beans.container.GetProxiesResponse;
import orca.manage.proxies.soap.beans.container.GetUnitRequest;
import orca.manage.proxies.soap.beans.container.GetUnitResponse;
import orca.manage.proxies.soap.beans.container.GetUserRequest;
import orca.manage.proxies.soap.beans.container.GetUserResponse;
import orca.manage.proxies.soap.beans.container.GetUsersRequest;
import orca.manage.proxies.soap.beans.container.GetUsersResponse;
import orca.manage.proxies.soap.beans.container.LoginRequest;
import orca.manage.proxies.soap.beans.container.LoginResponse;
import orca.manage.proxies.soap.beans.container.LogoutRequest;
import orca.manage.proxies.soap.beans.container.LogoutResponse;
import orca.manage.proxies.soap.beans.container.SetUserPasswordRequest;
import orca.manage.proxies.soap.beans.container.SetUserPasswordResponse;
import orca.security.AuthToken;
import orca.shirako.common.UnitID;
import orca.util.CertificateUtils;
import orca.util.ID;
import orca.util.ReflectionUtils;

public class SoapContainer extends SoapProxy implements IOrcaContainer {
	public SoapContainer(ID managementID, String url, AuthToken auth) {
		super(managementID, url, auth);
	}

	public Certificate getCertificate() {
		clearLast();
		try {
			GetCertificateRequest req = new GetCertificateRequest();
			GetCertificateResponse resp = (GetCertificateResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			try {
				return CertificateUtils.decode(resp.getCertificate().getContents());
			} catch (Exception e) {
				throw new OrcaManagementException("Could not obtain actor certificate", e);
			}
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public Certificate getCertificate(ID actorGuid) {
		clearLast();
		try {
			if (actorGuid == null) {
				throw new IllegalArgumentException("actorGuid");
			}
			GetCertificateRequest req = new GetCertificateRequest();
			req.setActorGuid(actorGuid.toString());
			GetCertificateResponse resp = (GetCertificateResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			try {
				return CertificateUtils.decode(resp.getCertificate().getContents());
			} catch (Exception e) {
				throw new OrcaManagementException("Could not obtain actor certificate", e);
			}
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public boolean login(String user, String password) {
		clearLast();
		try {
			LoginRequest req = new LoginRequest();
			req.setLogin(user);
			req.setPassword(password);
			LoginResponse resp = (LoginResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return false;
			}
			auth = new AuthToken(user);
			auth.setLoginToken(resp.getToken());
			authMng = OrcaConverter.fill(auth);
			loggedIn = true;
			return true;
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public boolean logout() {
		clearLast();
		try {
			LogoutRequest req = new LogoutRequest();
			req.setAuth(authMng);
			LogoutResponse resp = (LogoutResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return false;
			}
			loggedIn = false;
			return true;
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public boolean isLogged() {
		return loggedIn;
	}

	public boolean addUser(UserMng user) {
		throw new RuntimeException("Not implemented");
	}

	public boolean setUserPassword(String login, String password) {
		clearLast();
		try {
			SetUserPasswordRequest req = new SetUserPasswordRequest();
			req.setAuth(authMng);
			req.setLogin(login);
			req.setPassword(password);
			SetUserPasswordResponse resp = (SetUserPasswordResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}
	
	public UserMng getUser(String login) {
		clearLast();
		try {
			GetUserRequest req = new GetUserRequest();
			req.setAuth(authMng);
			req.setUserLogin(login);
			GetUserResponse resp = (GetUserResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			return resp.getUser();
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public List<UserMng> getUsers() {
		clearLast();
		try {
			GetUsersRequest req = new GetUsersRequest();
			req.setAuth(authMng);
			GetUsersResponse resp = (GetUsersResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			return resp.getResult();
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public boolean removeUser(String login) {
		throw new RuntimeException("Not implemented");
	}

	public boolean updateUser(UserMng user) {
		throw new RuntimeException("Not implemented");
	}

	public IOrcaActor getActor(ID guid) {
		IOrcaComponent comp = getManagementObject(guid);
		if (comp == null) {return null;}
		clearLast();
		try {
			return (IOrcaActor)comp;
		}catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public IOrcaBroker getBroker(ID guid) {
		IOrcaComponent comp = getManagementObject(guid);
		if (comp == null) {return null;}
		clearLast();
		try {
			return (IOrcaBroker)comp;
		}catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public IOrcaAuthority getAuthority(ID guid) {
		IOrcaComponent comp = getManagementObject(guid);
		if (comp == null) {return null;}
		clearLast();
		try {
			return (IOrcaAuthority)comp;
		}catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public IOrcaServiceManager getServiceManager(ID guid) {
		IOrcaComponent comp = getManagementObject(guid);
		if (comp == null) {return null;}
		clearLast();
		try {
			return (IOrcaServiceManager)comp;
		}catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public boolean configure(byte[] configuration) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}

	public boolean addActor(ActorCreateMng actor, ProxyMng[] brokers) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}

	public List<ActorMng> getActors() {
		return doGetActors(OrcaConstants.ActorTypeAll);
	}

	protected List<ActorMng> doGetActors(int type) {
		clearLast();
		try {
			GetActorsRequest req = new GetActorsRequest();
			req.setActorType(type);
			req.setAuth(authMng);
			GetActorsResponse resp = (GetActorsResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			return resp.getActors();
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}
	
	public List<ActorMng> getActorsFromDatabase() {
		clearLast();
		try {
			GetActorsFromDatabaseRequest req = new GetActorsFromDatabaseRequest();
			req.setAuth(authMng);
			GetActorsFromDatabaseResponse resp = (GetActorsFromDatabaseResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			return resp.getActors();
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public List<ActorMng> getAuthorities() {
		return doGetActors(OrcaConstants.ActorTypeSiteAuthority);
	}

	public List<ActorMng> getBrokers() {
		return doGetActors(OrcaConstants.ActorTypeBroker);
	}

	public List<ActorMng> getServiceManagers() {
		return doGetActors(OrcaConstants.ActorTypeServiceManager);
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

	protected List<ProxyMng> doGetProxies(String protocol, int type) {
		clearLast();
		try {
			GetProxiesRequest req = new GetProxiesRequest();
			req.setActorType(type);
			req.setAuth(authMng);
			req.setProtocol(protocol);
			GetProxiesResponse resp = (GetProxiesResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			return resp.getProxies();
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}
	
	public List<ProxyMng> getProxies(String protocol) {
		return doGetProxies(protocol, OrcaConstants.ActorTypeAll);
	}

	public List<ProxyMng> getBrokerProxies(String protocol) {
		return doGetProxies(protocol, OrcaConstants.ActorTypeBroker);
	}

	public List<ProxyMng> getAuthorityProxies(String protocol) {
		return doGetProxies(protocol, OrcaConstants.ActorTypeSiteAuthority);
	}

	public PackageMng getPackage(PackageId packageID) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return null;
	}

	public List<PackageMng> getPackages() {
		clearLast();
		try {
			GetPackagesRequest req = new GetPackagesRequest();
			req.setAuth(authMng);
			GetPackagesResponse resp = (GetPackagesResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			return resp.getPackages();
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public boolean installPackage(byte[] bytes) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}

	public boolean uninstallPackage(PackageId packageID) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}

	public boolean upgradePackage(PackageId packageID, byte[] bytes) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}

	public PluginMng getPlugin(PackageId packageID, PluginId pluginID) {
		clearLast();
		try {
			GetPluginRequest req = new GetPluginRequest();
			req.setAuth(authMng);
			if (packageID == null || pluginID == null) {
				throw new IllegalArgumentException();
			}
			req.setPackageId(packageID.toString());
			req.setPluginId(pluginID.toString());
			GetPluginResponse resp = (GetPluginResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			return resp.getPlugin();
		} catch (Exception e) {
			lastException = e;
			return null;
		}	}

	public List<PluginMng> getPlugins(int pluginType) {
		return getPlugins(null, pluginType, OrcaConstants.ActorTypeAll);
	}

	public List<PluginMng> getPlugins(int pluginType, int actorType) {
		return getPlugins(null, pluginType, actorType);
	}

	public List<PluginMng> getPlugins(PackageId packageID, int pluginType) {
		return getPlugins(packageID, pluginType, OrcaConstants.ActorTypeAll);
	}
	
	public List<PluginMng> getPlugins(PackageId packageID) {
		return getPlugins(packageID, Plugin.TypeAll, OrcaConstants.ActorTypeAll);
	}
	
	public List<PluginMng> getPlugins(PackageId packageID, int pluginType, int actorType) {
		clearLast();
		try {
			GetPluginsRequest req = new GetPluginsRequest();
			req.setAuth(authMng);
			if (packageID != null){
				req.setPackageId(packageID.toString());
			}
			req.setPluginType(pluginType);
			req.setActorType(actorType);
			GetPluginsResponse resp = (GetPluginsResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			return resp.getPlugins();
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public List<UnitMng> getInventory() {
		clearLast();
		try {
			GetInventoryRequest req = new GetInventoryRequest();
			req.setAuth(authMng);
			GetInventoryResponse resp = (GetInventoryResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			return resp.getInventory();
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public UnitMng getInventory(UnitID name) {
		clearLast();
		try {
			GetUnitRequest req = new GetUnitRequest();
			req.setAuth(authMng);
			if (name == null){
				throw new IllegalArgumentException();
			}
			GetUnitResponse resp = (GetUnitResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			return resp.getUnit();
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public boolean addInventory(UnitMng unit) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}

	public boolean updateInventory(UnitMng unit) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}

	public boolean removeInventory(UnitID name) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}

	public boolean transferInventory(UnitID name, ID actorGuid) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}

	public boolean untransferInventory(UnitID name, ID actorGuid) {
		clearLast();
		lastException = new RuntimeException("Not implemented");
		return false;
	}

	public IOrcaComponent getManagementObject(ID key) {
		clearLast();
		try {
			GetManagementObjectRequest req = new GetManagementObjectRequest();
			req.setAuth(authMng);
			if (key == null){
				throw new IllegalArgumentException();
			}
			req.setManagementObjectID(key.toString());
			GetManagementObjectResponse resp = (GetManagementObjectResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			
			IOrcaComponent cont = (IOrcaComponent)ReflectionUtils.createInstance(resp.getProxyClass(), 
									new Class<?>[] {ID.class, String.class, AuthToken.class}, 
									key, url, auth);
			return cont;
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}
}