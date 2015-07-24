package orca.manage.internal.soap;

import java.util.ArrayList;
import java.util.List;

import orca.manage.OrcaConstants;
import orca.manage.beans.ResultCertificateMng;
import orca.manage.beans.ResultEventMng;
import orca.manage.beans.ResultMng;
import orca.manage.beans.ResultReservationMng;
import orca.manage.beans.ResultReservationStateMng;
import orca.manage.beans.ResultSliceMng;
import orca.manage.beans.ResultStringMng;
import orca.manage.internal.ActorManagementObject;
import orca.manage.internal.Converter;
import orca.manage.proxies.soap.beans.actor.AddSliceRequest;
import orca.manage.proxies.soap.beans.actor.AddSliceResponse;
import orca.manage.proxies.soap.beans.actor.CloseReservationRequest;
import orca.manage.proxies.soap.beans.actor.CloseReservationResponse;
import orca.manage.proxies.soap.beans.actor.CloseSliceReservationsRequest;
import orca.manage.proxies.soap.beans.actor.CloseSliceReservationsResponse;
import orca.manage.proxies.soap.beans.actor.CreateEventSubscriptionRequest;
import orca.manage.proxies.soap.beans.actor.CreateEventSubscriptionResponse;
import orca.manage.proxies.soap.beans.actor.DeleteEventSubscriptionRequest;
import orca.manage.proxies.soap.beans.actor.DeleteEventSubscriptionResponse;
import orca.manage.proxies.soap.beans.actor.DrainEventsRequest;
import orca.manage.proxies.soap.beans.actor.DrainEventsResponse;
import orca.manage.proxies.soap.beans.actor.GetActorNameRequest;
import orca.manage.proxies.soap.beans.actor.GetActorNameResponse;
import orca.manage.proxies.soap.beans.actor.GetCertificateRequest;
import orca.manage.proxies.soap.beans.actor.GetCertificateResponse;
import orca.manage.proxies.soap.beans.actor.GetReservationRequest;
import orca.manage.proxies.soap.beans.actor.GetReservationResponse;
import orca.manage.proxies.soap.beans.actor.GetReservationStateRequest;
import orca.manage.proxies.soap.beans.actor.GetReservationStateResponse;
import orca.manage.proxies.soap.beans.actor.GetReservationsRequest;
import orca.manage.proxies.soap.beans.actor.GetReservationsResponse;
import orca.manage.proxies.soap.beans.actor.GetReservationsStateRequest;
import orca.manage.proxies.soap.beans.actor.GetReservationsStateResponse;
import orca.manage.proxies.soap.beans.actor.GetSliceRequest;
import orca.manage.proxies.soap.beans.actor.GetSliceResponse;
import orca.manage.proxies.soap.beans.actor.GetSlicesRequest;
import orca.manage.proxies.soap.beans.actor.GetSlicesResponse;
import orca.manage.proxies.soap.beans.actor.RegisterCertificateRequest;
import orca.manage.proxies.soap.beans.actor.RegisterCertificateResponse;
import orca.manage.proxies.soap.beans.actor.RemoveReservationRequest;
import orca.manage.proxies.soap.beans.actor.RemoveReservationResponse;
import orca.manage.proxies.soap.beans.actor.RemoveSliceRequest;
import orca.manage.proxies.soap.beans.actor.RemoveSliceResponse;
import orca.manage.proxies.soap.beans.actor.UnregisterCertificateRequest;
import orca.manage.proxies.soap.beans.actor.UnregisterCertificateResponse;
import orca.manage.proxies.soap.beans.actor.UpdateReservationRequest;
import orca.manage.proxies.soap.beans.actor.UpdateReservationResponse;
import orca.manage.proxies.soap.beans.actor.UpdateSliceRequest;
import orca.manage.proxies.soap.beans.actor.UpdateSliceResponse;
import orca.security.AuthToken;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.container.Globals;
import orca.util.ExceptionUtils;
import orca.util.ID;

import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

public class SoapActorService extends SoapService {
	public static final String ACTOR_NS = "http://www.nicl.duke.edu/orca/manage/services/actor";

	public SoapActorService(){
	}
	
	protected ActorManagementObject getActorMO(ID guid) {
		try {
		return (ActorManagementObject)Globals.getContainer().getManagementObjectManager().getManagementObject(guid);
		} catch (Exception e){
			throw new RuntimeException("Invalid actor guid: " + guid);
		}
	}
	
	@PayloadRoot(localPart = "GetCertificateRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	GetCertificateResponse getCertificate(@RequestPayload GetCertificateRequest request) {
		ResultMng status = new ResultMng();
		GetCertificateResponse response = new GetCertificateResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultCertificateMng tmp;
			if (request.getAlias() == null){
				tmp = mo.getCertificate();
			} else {
				tmp = mo.getCertificate(request.getAlias(), auth);
			}
			updateStatus(tmp.getStatus(), status);
			response.setCertificate(getFirst(tmp.getResult()));
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}
	
	@PayloadRoot(localPart = "RegisterCertificateRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	RegisterCertificateResponse registerCertificate(@RequestPayload RegisterCertificateRequest request) {
		ResultMng status = new ResultMng();
		RegisterCertificateResponse response = new RegisterCertificateResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultMng tmp = mo.registerCertificate(request.getCertificate(), request.getAlias(), auth);
			updateStatus(tmp, status);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}
	
	@PayloadRoot(localPart = "UnregisterCertificateRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	UnregisterCertificateResponse unregisterCertificate(@RequestPayload UnregisterCertificateRequest request) {
		ResultMng status = new ResultMng();
		UnregisterCertificateResponse response = new UnregisterCertificateResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultMng tmp = mo.unregisterCertificate(request.getAlias(), auth);
			updateStatus(tmp, status);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "GetSlicesRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	GetSlicesResponse getSlices(@RequestPayload GetSlicesRequest request) {
		ResultMng status = new ResultMng();
		GetSlicesResponse response = new GetSlicesResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultSliceMng tmp = mo.getSlices(auth);
			updateStatus(tmp.getStatus(), status);
			response.getSlices().addAll(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}
	
	@PayloadRoot(localPart = "GetSliceRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	GetSliceResponse getSlice(@RequestPayload GetSliceRequest request) {
		ResultMng status = new ResultMng();
		GetSliceResponse response = new GetSliceResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getSliceId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultSliceMng tmp = mo.getSlice(new SliceID(request.getSliceId()), auth);
			updateStatus(tmp.getStatus(), status);
			response.setSlice(getFirst(tmp.getResult()));
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}
	
	@PayloadRoot(localPart = "AddSliceRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	AddSliceResponse addSlice(@RequestPayload AddSliceRequest request) {
		ResultMng status = new ResultMng();
		AddSliceResponse response = new AddSliceResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultStringMng tmp = mo.addSlice(request.getSlice(), auth);
			updateStatus(tmp.getStatus(), status);
			response.setSliceId(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "UpdateSliceRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	UpdateSliceResponse updateSlice(@RequestPayload UpdateSliceRequest request) {
		ResultMng status = new ResultMng();
		UpdateSliceResponse response = new UpdateSliceResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultMng tmp = mo.updateSlice(request.getSlice(), auth);
			updateStatus(tmp, status);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}
	
	@PayloadRoot(localPart = "RemoveSliceRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	RemoveSliceResponse removeSlice(@RequestPayload RemoveSliceRequest request) {
		ResultMng status = new ResultMng();
		RemoveSliceResponse response = new RemoveSliceResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getSliceId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultMng tmp = mo.removeSlice(new SliceID(request.getSliceId()), auth);
			updateStatus(tmp, status);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}
	
	@PayloadRoot(localPart = "GetReservationsRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	GetReservationsResponse getReservations(@RequestPayload GetReservationsRequest request) {
		ResultMng status = new ResultMng();
		GetReservationsResponse response = new GetReservationsResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultReservationMng tmp = null;
			if (request.getSliceId() == null){
				if (request.getReservationState() == OrcaConstants.AllReservationStates){				
					tmp = mo.getReservations(auth);
				}else {
					tmp = mo.getReservations(request.getReservationState(), auth);
				}
			} else {
				if (request.getReservationState() == OrcaConstants.AllReservationStates){				
					tmp = mo.getReservations(new SliceID(request.getSliceId()), auth);
				}else {
					tmp = mo.getReservations(new SliceID(request.getSliceId()), request.getReservationState(), auth);				
				}
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
	
	@PayloadRoot(localPart = "GetReservationRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	GetReservationResponse getReservation(@RequestPayload GetReservationRequest request) {
		ResultMng status = new ResultMng();
		GetReservationResponse response = new GetReservationResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getReservationId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultReservationMng tmp = mo.getReservation(new ReservationID(request.getReservationId()), auth);
			updateStatus(tmp.getStatus(), status);
			response.setReservation(getFirst(tmp.getResult()));
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "UpdateReservationRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	UpdateReservationResponse updateReservation(@RequestPayload UpdateReservationRequest request) {
		ResultMng status = new ResultMng();
		UpdateReservationResponse response = new UpdateReservationResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultMng tmp = mo.updateReservation(request.getReservation(), auth);
			updateStatus(tmp, status);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	
	@PayloadRoot(localPart = "RemoveReservationRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	RemoveReservationResponse removeReservation(@RequestPayload RemoveReservationRequest request) {
		ResultMng status = new ResultMng();
		RemoveReservationResponse response = new RemoveReservationResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getReservationId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultMng tmp = mo.removeReservation(new ReservationID(request.getReservationId()), auth);
			updateStatus(tmp, status);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "CloseReservationRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	CloseReservationResponse closeReservation(@RequestPayload CloseReservationRequest request) {
		ResultMng status = new ResultMng();
		CloseReservationResponse response = new CloseReservationResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getReservationId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultMng tmp = mo.closeReservation(new ReservationID(request.getReservationId()), auth);
			updateStatus(tmp, status);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "CloseSliceReservationsRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	CloseSliceReservationsResponse closeSliceReservations(@RequestPayload CloseSliceReservationsRequest request) {
		ResultMng status = new ResultMng();
		CloseSliceReservationsResponse response = new CloseSliceReservationsResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getSliceId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultMng tmp = mo.closeSliceReservations(new SliceID(request.getSliceId()), auth);
			updateStatus(tmp, status);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "GetActorNameRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	GetActorNameResponse getActorName(@RequestPayload GetActorNameRequest request) {
		ResultMng status = new ResultMng();
		GetActorNameResponse response = new GetActorNameResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			response.setName(mo.getActorName());
			status.setCode(OrcaConstants.ErrorNone);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "CreateEventSubscriptionRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	CreateEventSubscriptionResponse createEventSubscription(@RequestPayload CreateEventSubscriptionRequest request) {
		ResultMng status = new ResultMng();
		CreateEventSubscriptionResponse response = new CreateEventSubscriptionResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultStringMng tmp = mo.createEventSubscription(auth);
			updateStatus(tmp.getStatus(), status);
			if (tmp.getStatus().getCode() == 0){
				response.setSubscriptionId(tmp.getResult());
			}			
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}

	@PayloadRoot(localPart = "DeleteEventSubscriptionRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	DeleteEventSubscriptionResponse deleteEventSubscription(@RequestPayload DeleteEventSubscriptionRequest request) {
		ResultMng status = new ResultMng();
		DeleteEventSubscriptionResponse response = new DeleteEventSubscriptionResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getSubscriptionId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ID subscriptionID = new ID(request.getSubscriptionId());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultMng tmp = mo.deleteEventSubscription(subscriptionID, auth);
			updateStatus(tmp, status);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}

	@PayloadRoot(localPart = "DrainEventsRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	DrainEventsResponse drainEvents(@RequestPayload DrainEventsRequest request) {
		ResultMng status = new ResultMng();
		DrainEventsResponse response = new DrainEventsResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getSubscriptionId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ID subscriptionID = new ID(request.getSubscriptionId());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultEventMng tmp = mo.drainEvents(subscriptionID, request.getTimeout(), auth);
			updateStatus(tmp.getStatus(), status);
			response.getEvents().addAll(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}

	@PayloadRoot(localPart = "GetReservationStateRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	GetReservationStateResponse getReservationState(@RequestPayload GetReservationStateRequest request) {
		ResultMng status = new ResultMng();
		GetReservationStateResponse response = new GetReservationStateResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getReservationId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultReservationStateMng tmp = mo.getReservationState(new ReservationID(request.getReservationId()), auth);
			updateStatus(tmp.getStatus(), status);
			response.setReservationState(getFirst(tmp.getResult()));
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}

	@PayloadRoot(localPart = "GetReservationsStateRequest", namespace = ACTOR_NS)
	public @ResponsePayload
	GetReservationsStateResponse getReservationsState(@RequestPayload GetReservationsStateRequest request) {
		ResultMng status = new ResultMng();
		GetReservationsStateResponse response = new GetReservationsStateResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			List<ReservationID> rids = new ArrayList<ReservationID>(request.getReservationId().size());
			for (String rid : request.getReservationId()){
				if (rid == null){
					status.setCode(OrcaConstants.ErrorInvalidArguments);
					return response;					
				}
				rids.add(new ReservationID(rid));
			}
			
			ActorManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultReservationStateMng tmp = mo.getReservationState(rids, auth);
			updateStatus(tmp.getStatus(), status);
			response.getReservationState().addAll(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}
}