package orca.manage.internal.soap;

import java.util.Date;
import java.util.Properties;

import orca.manage.OrcaConstants;
import orca.manage.beans.ResultCertificateMng;
import orca.manage.beans.ResultClientMng;
import orca.manage.beans.ResultMng;
import orca.manage.beans.ResultReservationMng;
import orca.manage.beans.ResultSliceMng;
import orca.manage.beans.ResultStringMng;
import orca.manage.internal.Converter;
import orca.manage.internal.ServerActorManagementObject;
import orca.manage.proxies.soap.beans.serveractor.AddClientSliceRequest;
import orca.manage.proxies.soap.beans.serveractor.AddClientSliceResponse;
import orca.manage.proxies.soap.beans.serveractor.ExportResourcesRequest;
import orca.manage.proxies.soap.beans.serveractor.ExportResourcesResponse;
import orca.manage.proxies.soap.beans.serveractor.GetBrokerReservationsRequest;
import orca.manage.proxies.soap.beans.serveractor.GetBrokerReservationsResponse;
import orca.manage.proxies.soap.beans.serveractor.GetClientCertificateRequest;
import orca.manage.proxies.soap.beans.serveractor.GetClientCertificateResponse;
import orca.manage.proxies.soap.beans.serveractor.GetClientRequest;
import orca.manage.proxies.soap.beans.serveractor.GetClientReservationsRequest;
import orca.manage.proxies.soap.beans.serveractor.GetClientReservationsResponse;
import orca.manage.proxies.soap.beans.serveractor.GetClientResponse;
import orca.manage.proxies.soap.beans.serveractor.GetClientSlicesRequest;
import orca.manage.proxies.soap.beans.serveractor.GetClientSlicesResponse;
import orca.manage.proxies.soap.beans.serveractor.GetClientsRequest;
import orca.manage.proxies.soap.beans.serveractor.GetClientsResponse;
import orca.manage.proxies.soap.beans.serveractor.GetInventoryReservationsRequest;
import orca.manage.proxies.soap.beans.serveractor.GetInventoryReservationsResponse;
import orca.manage.proxies.soap.beans.serveractor.GetInventorySlicesRequest;
import orca.manage.proxies.soap.beans.serveractor.GetInventorySlicesResponse;
import orca.manage.proxies.soap.beans.serveractor.RegisterClientRequest;
import orca.manage.proxies.soap.beans.serveractor.RegisterClientResponse;
import orca.manage.proxies.soap.beans.serveractor.UnregisterClientRequest;
import orca.manage.proxies.soap.beans.serveractor.UnregisterClientResponse;
import orca.security.AuthToken;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.container.Globals;
import orca.util.ExceptionUtils;
import orca.util.ID;
import orca.util.ResourceType;

import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

public class SoapServerActorService extends SoapService {
	public static final String SERVER_ACTOR_NS = "http://www.nicl.duke.edu/orca/manage/services/serveractor";

	public SoapServerActorService(){
	}
	
	protected ServerActorManagementObject getActorMO(ID guid) {
		try {
		return (ServerActorManagementObject)Globals.getContainer().getManagementObjectManager().getManagementObject(guid);
		} catch (Exception e){
			throw new RuntimeException("Invalid actor guid: " + guid);
		}
	}

	@PayloadRoot(localPart = "GetBrokerReservationsRequest", namespace = SERVER_ACTOR_NS)
	public @ResponsePayload
	GetBrokerReservationsResponse getBrokerReservations(@RequestPayload GetBrokerReservationsRequest request) {
		ResultMng status = new ResultMng();
		GetBrokerReservationsResponse response = new GetBrokerReservationsResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ServerActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultReservationMng tmp = mo.getBrokerReservations(auth);
			updateStatus(tmp.getStatus(), status);
			response.getReservations().addAll(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "GetInventorySlicesRequest", namespace = SERVER_ACTOR_NS)
	public @ResponsePayload
	GetInventorySlicesResponse getInventorySlices(@RequestPayload GetInventorySlicesRequest request) {
		ResultMng status = new ResultMng();
		GetInventorySlicesResponse response = new GetInventorySlicesResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ServerActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultSliceMng tmp = mo.getInventorySlices(auth);
			updateStatus(tmp.getStatus(), status);
			response.getSlices().addAll(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "GetInventoryReservationsRequest", namespace = SERVER_ACTOR_NS)
	public @ResponsePayload
	GetInventoryReservationsResponse getInventoryReservations(@RequestPayload GetInventoryReservationsRequest request) {
		ResultMng status = new ResultMng();
		GetInventoryReservationsResponse response = new GetInventoryReservationsResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ServerActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultReservationMng tmp = null;
			if (request.getSliceId() == null){
				tmp = mo.getInventoryReservations(auth);
			} else {
				tmp = mo.getInventoryReservations(new SliceID(request.getSliceId()), auth);
			}
			updateStatus(tmp.getStatus(), status);
			response.getReservations().addAll(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	
	@PayloadRoot(localPart = "GetClientSlicesRequest", namespace = SERVER_ACTOR_NS)
	public @ResponsePayload
	GetClientSlicesResponse getClientSlices(@RequestPayload GetClientSlicesRequest request) {
		ResultMng status = new ResultMng();
		GetClientSlicesResponse response = new GetClientSlicesResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ServerActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultSliceMng tmp = mo.getClientSlices(auth);
			updateStatus(tmp.getStatus(), status);
			response.getSlices().addAll(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}
	
	@PayloadRoot(localPart = "AddClientSliceRequest", namespace = SERVER_ACTOR_NS)
	public @ResponsePayload
	AddClientSliceResponse addClientSlice(@RequestPayload AddClientSliceRequest request) {
		ResultMng status = new ResultMng();
		AddClientSliceResponse response = new AddClientSliceResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ServerActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultStringMng tmp = mo.addClientSlice(request.getSlice(), auth);
			updateStatus(tmp.getStatus(), status);
			response.setSliceId(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "GetClientsRequest", namespace = SERVER_ACTOR_NS)
	public @ResponsePayload
	GetClientsResponse getClients(@RequestPayload GetClientsRequest request) {
		ResultMng status = new ResultMng();
		GetClientsResponse response = new GetClientsResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ServerActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultClientMng tmp = mo.getClients(auth);
			updateStatus(tmp.getStatus(), status);
			response.getClients().addAll(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "GetClientRequest", namespace = SERVER_ACTOR_NS)
	public @ResponsePayload
	GetClientResponse getClient(@RequestPayload GetClientRequest request) {
		ResultMng status = new ResultMng();
		GetClientResponse response = new GetClientResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getClientId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ServerActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultClientMng tmp = mo.getClient(new ID(request.getClientId()), auth);
			updateStatus(tmp.getStatus(), status);
			response.setClient(getFirst(tmp.getResult()));
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "GetClientCertificateRequest", namespace = SERVER_ACTOR_NS)
	public @ResponsePayload
	GetClientCertificateResponse getClientCertificate(@RequestPayload GetClientCertificateRequest request) {
		ResultMng status = new ResultMng();
		GetClientCertificateResponse response = new GetClientCertificateResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getClientId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ServerActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultCertificateMng tmp = mo.getClientCertificate(new ID(request.getClientId()), auth);
			updateStatus(tmp.getStatus(), status);
			response.setCertificate(getFirst(tmp.getResult()));
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}
	
	@PayloadRoot(localPart = "RegisterClientRequest", namespace = SERVER_ACTOR_NS)
	public @ResponsePayload
	RegisterClientResponse registerClient(@RequestPayload RegisterClientRequest request) {
		ResultMng status = new ResultMng();
		RegisterClientResponse response = new RegisterClientResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ServerActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultMng tmp = mo.registerClient(request.getClient(), request.getCertificate(), auth);
			updateStatus(tmp, status);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "UnregisterClientRequest", namespace = SERVER_ACTOR_NS)
	public @ResponsePayload
	UnregisterClientResponse unregisterClient(@RequestPayload UnregisterClientRequest request) {
		ResultMng status = new ResultMng();
		UnregisterClientResponse response = new UnregisterClientResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getClientId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ServerActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultMng tmp = mo.unregisterClient(new ID(request.getClientId()), auth);
			updateStatus(tmp, status);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}


	@PayloadRoot(localPart = "GetClientReservationsRequest", namespace = SERVER_ACTOR_NS)
	public @ResponsePayload
	GetClientReservationsResponse getClientReservations(@RequestPayload GetClientReservationsRequest request) {
		ResultMng status = new ResultMng();
		GetClientReservationsResponse response = new GetClientReservationsResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ServerActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultReservationMng tmp = null;
			if (request.getSliceId() == null){
				tmp = mo.getClientReservations(auth);
			} else {
				tmp = mo.getClientReservations(new SliceID(request.getSliceId()), auth);
			}
			updateStatus(tmp.getStatus(), status);
			response.getReservations().addAll(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}
	
	@PayloadRoot(localPart = "ExportResourcesRequest", namespace = SERVER_ACTOR_NS)
	public @ResponsePayload
	ExportResourcesResponse exportResources(@RequestPayload ExportResourcesRequest request) {
		ResultMng status = new ResultMng();
		ExportResourcesResponse response = new ExportResourcesResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || (request.getResourcePoolId() == null && request.getResourceType() == null)) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			
			// when exporting without using clientSliceId, we need to have the client name and guid
			if (request.getClientSliceId() == null && (request.getClientName() == null || request.getClientGuid() == null)) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;				
			}
			
			AuthToken auth = Converter.fill(request.getAuth());
			ServerActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			Properties ticketProperties = Converter.fill(request.getTicketProperties());
			Properties resourceProperties = Converter.fill(request.getResourceProperties());

			ResultStringMng tmp;
			if (request.getResourceType() == null){
				if (request.getClientSliceId() != null) {
					tmp = mo.exportResources(
		                       new SliceID(request.getClientSliceId()), 
							   new SliceID(request.getResourcePoolId()),
							   new Date(request.getStartTime()), new Date(request.getStopTime()),
							   request.getUnits(),
							   ticketProperties,
							   resourceProperties,
							   request.getTicketId()!= null? (new ReservationID(request.getTicketId())):null,
							   auth);
				}else {
					tmp = mo.exportResources( 
							   new SliceID(request.getResourcePoolId()),
							   new Date(request.getStartTime()), new Date(request.getStopTime()),
							   request.getUnits(),
							   ticketProperties,
							   resourceProperties,
							   request.getTicketId()!= null? (new ReservationID(request.getTicketId())):null,
							   new AuthToken(request.getClientName(), new ID(request.getClientGuid())),
							   auth);
				}
			}else {
				if (request.getClientSliceId() != null){
					tmp = mo.exportResources(
							new SliceID(request.getClientSliceId()),
						   new ResourceType(request.getResourceType()),
						   new Date(request.getStartTime()), new Date(request.getStopTime()),
						   request.getUnits(),
						   ticketProperties,
						   resourceProperties,
						   request.getTicketId()!= null? (new ReservationID(request.getTicketId())):null,
						   auth);
				}else {
					tmp = mo.exportResources(
							   new ResourceType(request.getResourceType()),
							   new Date(request.getStartTime()), new Date(request.getStopTime()),
							   request.getUnits(),
							   ticketProperties,
							   resourceProperties,
							   request.getTicketId()!= null? (new ReservationID(request.getTicketId())):null,
						       new AuthToken(request.getClientName(), new ID(request.getClientGuid())),
							   auth);
				}

			}			
			updateStatus(tmp.getStatus(), status);
			response.setReservationId(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}
}