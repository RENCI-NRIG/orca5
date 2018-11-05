package net.exogeni.orca.manage.internal.soap;

import java.util.Date;
import java.util.Properties;

import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.manage.OrcaConverter;
import net.exogeni.orca.manage.beans.ResultMng;
import net.exogeni.orca.manage.beans.ResultUnitMng;
import net.exogeni.orca.manage.internal.Converter;
import net.exogeni.orca.manage.internal.IClientActorManagementObject;
import net.exogeni.orca.manage.internal.ServiceManagerManagementObject;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.ExtendReservationResponse;
import net.exogeni.orca.manage.proxies.soap.beans.servicemanager.GetReservationUnitsRequest;
import net.exogeni.orca.manage.proxies.soap.beans.servicemanager.GetReservationUnitsResponse;
import net.exogeni.orca.manage.proxies.soap.beans.servicemanager.ModifyReservationRequest;
import net.exogeni.orca.manage.proxies.soap.beans.servicemanager.ModifyReservationResponse;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.util.ExceptionUtils;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.ResourceType;

import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;


public class SoapServiceManagerService extends SoapService {
	public static final String SERVICE_MANAGER_NS = "http://www.nicl.duke.edu/net/exogeni/orca/manage/services/servicemanager";

	public SoapServiceManagerService(){
	}
	
	protected ServiceManagerManagementObject getActorMO(ID guid) {
		try {
		return (ServiceManagerManagementObject)Globals.getContainer().getManagementObjectManager().getManagementObject(guid);
		} catch (Exception e){
			throw new RuntimeException("Invalid actor guid: " + guid);
		}
	}
	
	@PayloadRoot(localPart = "GetReservationUnitsRequest", namespace = SERVICE_MANAGER_NS)
	public @ResponsePayload
	GetReservationUnitsResponse getReservationUnits(@RequestPayload GetReservationUnitsRequest request) {
		ResultMng status = new ResultMng();
		GetReservationUnitsResponse response = new GetReservationUnitsResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getReservationId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ServiceManagerManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultUnitMng tmp = mo.getReservationUnits(new ReservationID(request.getReservationId()), auth);
			updateStatus(tmp.getStatus(), status);
			response.getUnits().addAll(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}
	
	@PayloadRoot(localPart = "ModifyReservationRequest", namespace = SERVICE_MANAGER_NS)
	public @ResponsePayload
	ModifyReservationResponse modifyReservation(@RequestPayload ModifyReservationRequest request) {
		ResultMng status = new ResultMng();
		ModifyReservationResponse response = new ModifyReservationResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getReservationID() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			ServiceManagerManagementObject mo = getActorMO(new ID(request.getGuid()));
			Properties modifyProperties = OrcaConverter.fill(request.getModifyProperties());
			ResultMng tmp = mo.modifyReservation(new ReservationID(request.getReservationID()), modifyProperties, auth);
			updateStatus(tmp, status);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}	
	
	
	

}
