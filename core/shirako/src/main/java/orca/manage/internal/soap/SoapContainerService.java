package orca.manage.internal.soap;

import orca.extensions.PackageId;
import orca.extensions.PluginId;
import orca.manage.OrcaConstants;
import orca.manage.OrcaManagementException;
import orca.manage.OrcaProxyProtocolDescriptor;
import orca.manage.beans.ResultActorMng;
import orca.manage.beans.ResultCertificateMng;
import orca.manage.beans.ResultMng;
import orca.manage.beans.ResultPackageMng;
import orca.manage.beans.ResultPluginMng;
import orca.manage.beans.ResultProxyMng;
import orca.manage.beans.ResultStringMng;
import orca.manage.beans.ResultUnitMng;
import orca.manage.beans.ResultUserMng;
import orca.manage.internal.ContainerManagementObject;
import orca.manage.internal.Converter;
import orca.manage.internal.ManagementObject;
import orca.manage.internal.api.IManagementObject;
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
import orca.shirako.container.Globals;
import orca.util.ExceptionUtils;
import orca.util.ID;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class SoapContainerService extends SoapService {
	public static final String CONTAINER_NS = "http://www.nicl.duke.edu/orca/manage/services/container";

	protected final ContainerManagementObject mo;

	public SoapContainerService() {
		IManagementObject man = Globals.getContainer().getManagementObjectManager()
				.getManagementObject(OrcaConstants.ContainerManagmentObjectID);
		if (man == null && !(man instanceof ContainerManagementObject)) {
			throw new RuntimeException("Could not obtain the ContainerManagementObject");
		}
		mo = (ContainerManagementObject) man;
	}

	@PayloadRoot(localPart = "GetManagementObjectRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	GetManagementObjectResponse getManagementObject(@RequestPayload GetManagementObjectRequest request) {
		ResultMng result = new ResultMng();
		GetManagementObjectResponse response = new GetManagementObjectResponse();
		response.setStatus(result);

		try {
			if (request.getManagementObjectID() == null) {
				result.setCode(OrcaConstants.ErrorInvalidArguments);
				result.setMessage("Missing objectID");
				return response;
			}

			ID objectID = new ID(request.getManagementObjectID());
			// find the management object
			ManagementObject obj = (ManagementObject)mo.getManagementObject(objectID);
			if (obj == null) {
				result.setCode(OrcaConstants.ErrorNoSuchManagementObject);
				return response;
			}

			// go through the set of supported proxies and find
			// the record for the current protocol (soap)
			OrcaProxyProtocolDescriptor[] desc = obj.getProxies();
			if (desc == null) {
				throw new OrcaManagementException("Management object did not specify any proxies");
			}

			OrcaProxyProtocolDescriptor d = null;
			for (int i = 0; i < desc.length; i++) {
				if (OrcaConstants.ProtocolSoap.equals(desc[i].getProtocol())) {
					d = desc[i];
					break;
				}
			}
			if (d == null || d.getProxyClass() == null) {
				throw new OrcaManagementException("Management object did not specify a soap proxy");
			}

			response.setProxyClass(d.getProxyClass());
		} catch (Exception e) {
			result.setCode(OrcaConstants.ErrorInternalError);
			result.setMessage(e.getMessage());
			result.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		return response;
	}
	
	@PayloadRoot(localPart = "GetCertificateRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	GetCertificateResponse getCertificate(@RequestPayload GetCertificateRequest request) {
		ResultMng status = new ResultMng();
		GetCertificateResponse response = new GetCertificateResponse();
		response.setStatus(status);
		
		try {
			ResultCertificateMng tmp = null;
			if (request.getActorGuid() != null){
				tmp = mo.getCertificate(new ID(request.getActorGuid()));
			} else {
				tmp = mo.getCertificate();
			}
			updateStatus(tmp.getStatus(), status);
			response.setCertificate(getFirst(tmp.getResult()));
		} catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}
	
	@PayloadRoot(localPart = "GetUsersRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	GetUsersResponse getUsers(@RequestPayload GetUsersRequest request) {
		ResultMng status = new ResultMng();
		GetUsersResponse response = new GetUsersResponse();
		response.setStatus(status);
		
		try {
			AuthToken auth = Converter.fill(request.getAuth());
			ResultUserMng tmp = mo.getUsers(auth);
			updateStatus(tmp.getStatus(), status);
			response.getResult().addAll(tmp.getResult());
		} catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}

	@PayloadRoot(localPart = "GetUserRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	GetUserResponse getUsers(@RequestPayload GetUserRequest request) {
		ResultMng status = new ResultMng();
		GetUserResponse response = new GetUserResponse();
		response.setStatus(status);
		
		try {
			AuthToken auth = Converter.fill(request.getAuth());
			ResultUserMng tmp = mo.getUsers(auth);
			updateStatus(tmp.getStatus(), status);
			response.setUser(getFirst(tmp.getResult()));
		} catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}
	
	@PayloadRoot(localPart = "SetUserPasswordRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	SetUserPasswordResponse setUserPassword(@RequestPayload SetUserPasswordRequest request) {
		ResultMng status = new ResultMng();
		SetUserPasswordResponse response = new SetUserPasswordResponse();
		response.setStatus(status);
		
		try {
			AuthToken auth = Converter.fill(request.getAuth());
			ResultMng tmp = mo.setUserPassword(request.getLogin(), request.getPassword(), auth);
			updateStatus(tmp, status);
		} catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}
	
	@PayloadRoot(localPart = "LoginRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	LoginResponse login(@RequestPayload LoginRequest request) {
		ResultMng status = new ResultMng();
		LoginResponse response = new LoginResponse();
		response.setStatus(status);
		try {
			ResultStringMng tmp = mo.login(request.getLogin(), request.getPassword());
			response.setToken(tmp.getResult());
			updateStatus(tmp.getStatus(), status);
		} catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}

	@PayloadRoot(localPart = "LogoutRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	LogoutResponse logout(@RequestPayload LogoutRequest request) {
		ResultMng status = new ResultMng();
		LogoutResponse response = new LogoutResponse();
		response.setStatus(status);
		try {
			AuthToken auth = Converter.fill(request.getAuth());
			mo.logout(auth);
		} catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}
	
	
	@PayloadRoot(localPart = "GetActorsRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	GetActorsResponse getActors(@RequestPayload GetActorsRequest request) {
		ResultMng status = new ResultMng();
		GetActorsResponse response = new GetActorsResponse();
		response.setStatus(status);
		try {
			AuthToken auth = Converter.fill(request.getAuth());
			ResultActorMng tmp = null;
			switch(request.getActorType()) {
				case OrcaConstants.ActorTypeAll:
					tmp = mo.getActors(auth);
					break;
				case OrcaConstants.ActorTypeServiceManager:
					tmp = mo.getServiceManagers(auth);
					break;
				case OrcaConstants.ActorTypeBroker:
					tmp = mo.getBrokers(auth);
					break;
				case OrcaConstants.ActorTypeSiteAuthority:
					tmp = mo.getAuthorities(auth);
					break;
				default:
					status.setCode(OrcaConstants.ErrorInvalidArguments);
					return response;
			}
			updateStatus(tmp.getStatus(), status);
			response.getActors().addAll(tmp.getResult());
		} catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}

	@PayloadRoot(localPart = "GetActorsFromDatabaseRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	GetActorsFromDatabaseResponse getActorsFromDatabase(@RequestPayload GetActorsFromDatabaseRequest request) {
		ResultMng status = new ResultMng();
		GetActorsFromDatabaseResponse response = new GetActorsFromDatabaseResponse();
		response.setStatus(status);
		try {
			AuthToken auth = Converter.fill(request.getAuth());
			ResultActorMng tmp = mo.getActorsFromDatabase(auth);
			updateStatus(tmp.getStatus(), status);
			response.getActors().addAll(tmp.getResult());
		} catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}
	
	@PayloadRoot(localPart = "GetProxiesRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	GetProxiesResponse geProxies(@RequestPayload GetProxiesRequest request) {
		ResultMng status = new ResultMng();
		GetProxiesResponse response = new GetProxiesResponse();
		response.setStatus(status);
		try {
			AuthToken auth = Converter.fill(request.getAuth());
			ResultProxyMng tmp = null;
			switch(request.getActorType()) {
				case OrcaConstants.ActorTypeAll:
					tmp = mo.getProxies(request.getProtocol(), auth);
					break;
				case OrcaConstants.ActorTypeBroker:
					tmp = mo.getBrokerProxies(request.getProtocol(), auth);
					break;
				case OrcaConstants.ActorTypeSiteAuthority:
					tmp = mo.getSiteProxies(request.getProtocol(), auth);
					break;
				default:
					status.setCode(OrcaConstants.ErrorInvalidArguments);
					return response;
			}
			updateStatus(tmp.getStatus(), status);
			response.getProxies().addAll(tmp.getResult());
		} catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}
	
	@PayloadRoot(localPart = "GetPackagesRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	GetPackagesResponse getPackages(@RequestPayload GetPackagesRequest request) {
		ResultMng status = new ResultMng();
		GetPackagesResponse response = new GetPackagesResponse();
		response.setStatus(status);
		try {
			AuthToken auth = Converter.fill(request.getAuth());
			ResultPackageMng tmp = mo.getPackages(auth);
			updateStatus(tmp.getStatus(), status);
			response.getPackages().addAll(tmp.getResult());
		} catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}
	
	
	@PayloadRoot(localPart = "GetPluginsRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	GetPluginsResponse getPlugins(@RequestPayload GetPluginsRequest request) {
		ResultMng status = new ResultMng();
		GetPluginsResponse response = new GetPluginsResponse();
		response.setStatus(status);
		try {
			AuthToken auth = Converter.fill(request.getAuth());
			PackageId pkg = null;
			if (request.getPackageId() != null) {
				pkg = new PackageId(request.getPackageId());
			}
			ResultPluginMng tmp = mo.getPlugins(pkg, request.getPluginType(), request.getActorType(), auth);
			updateStatus(tmp.getStatus(), status);
			response.getPlugins().addAll(tmp.getResult());
		} catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}
	
	@PayloadRoot(localPart = "GetPluginRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	GetPluginResponse getPlugin(@RequestPayload GetPluginRequest request) {
		ResultMng status = new ResultMng();
		GetPluginResponse response = new GetPluginResponse();
		response.setStatus(status);
		try {
			AuthToken auth = Converter.fill(request.getAuth());
			if (request.getPackageId() == null || request.getPluginId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			ResultPluginMng tmp = mo.getPlugin(new PackageId(request.getPackageId()), new PluginId(request.getPluginId()), auth);
			updateStatus(tmp.getStatus(), status);
			response.setPlugin(getFirst(tmp.getResult()));
		} catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}
	
	@PayloadRoot(localPart = "GetInventoryRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	GetInventoryResponse getInventory(@RequestPayload GetInventoryRequest request) {
		ResultMng status = new ResultMng();
		GetInventoryResponse response = new GetInventoryResponse();
		response.setStatus(status);
		try {
			AuthToken auth = Converter.fill(request.getAuth());
			ResultUnitMng tmp = mo.getInventory(auth);
			updateStatus(tmp.getStatus(), status);
			response.getInventory().addAll(tmp.getResult());
		} catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}
	
	@PayloadRoot(localPart = "GetUnitRequest", namespace = CONTAINER_NS)
	public @ResponsePayload
	GetUnitResponse getUnit(@RequestPayload GetUnitRequest request) {
		ResultMng status = new ResultMng();
		GetUnitResponse response = new GetUnitResponse();
		response.setStatus(status);
		try {
			AuthToken auth = Converter.fill(request.getAuth());
			if (request.getUnitId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			ResultUnitMng tmp = mo.getInventory(new UnitID(request.getUnitId()), auth);
			updateStatus(tmp.getStatus(), status);
			response.setUnit(getFirst(tmp.getResult()));
		} catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}
}