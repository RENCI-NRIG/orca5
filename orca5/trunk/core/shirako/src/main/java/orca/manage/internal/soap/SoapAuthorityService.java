package orca.manage.internal.soap;

import orca.manage.OrcaConstants;
import orca.manage.beans.ResultMng;
import orca.manage.beans.ResultReservationMng;
import orca.manage.beans.ResultUnitMng;
import orca.manage.internal.AuthorityManagementObject;
import orca.manage.internal.Converter;
import orca.manage.proxies.soap.beans.authority.GetAuthorityReservationsRequest;
import orca.manage.proxies.soap.beans.authority.GetAuthorityReservationsResponse;
import orca.manage.proxies.soap.beans.authority.GetInventoryRequest;
import orca.manage.proxies.soap.beans.authority.GetInventoryResponse;
import orca.manage.proxies.soap.beans.authority.GetReservationUnitsRequest;
import orca.manage.proxies.soap.beans.authority.GetReservationUnitsResponse;
import orca.manage.proxies.soap.beans.authority.GetUnitRequest;
import orca.manage.proxies.soap.beans.authority.GetUnitResponse;
import orca.manage.proxies.soap.beans.authority.TransferInventoryRequest;
import orca.manage.proxies.soap.beans.authority.TransferInventoryResponse;
import orca.manage.proxies.soap.beans.authority.UntransferInventoryRequest;
import orca.manage.proxies.soap.beans.authority.UntransferInventoryResponse;
import orca.security.AuthToken;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.common.UnitID;
import orca.shirako.container.Globals;
import orca.util.ExceptionUtils;
import orca.util.ID;

import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

public class SoapAuthorityService extends SoapService {
	public static final String AUTHORITY_NS = "http://www.nicl.duke.edu/orca/manage/services/authority";

	public SoapAuthorityService(){
	}
	
	protected AuthorityManagementObject getActorMO(ID guid) {
		try {
		return (AuthorityManagementObject)Globals.getContainer().getManagementObjectManager().getManagementObject(guid);
		} catch (Exception e){
			throw new RuntimeException("Invalid actor guid: " + guid);
		}
	}
	
	@PayloadRoot(localPart = "GetAuthorityReservationsRequest", namespace = AUTHORITY_NS)
	public @ResponsePayload
	GetAuthorityReservationsResponse getBrokerReservations(@RequestPayload GetAuthorityReservationsRequest request) {
		ResultMng status = new ResultMng();
		GetAuthorityReservationsResponse response = new GetAuthorityReservationsResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			AuthorityManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultReservationMng tmp = mo.getAuthorityReservations(auth);
			updateStatus(tmp.getStatus(), status);
			response.getReservations().addAll(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "GetReservationUnitsRequest", namespace = AUTHORITY_NS)
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
			AuthorityManagementObject mo = getActorMO(new ID(request.getGuid()));
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
	
	@PayloadRoot(localPart = "GetInventoryRequest", namespace = AUTHORITY_NS)
	public @ResponsePayload
	GetInventoryResponse getInventory(@RequestPayload GetInventoryRequest request) {
		ResultMng status = new ResultMng();
		GetInventoryResponse response = new GetInventoryResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			AuthorityManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultUnitMng tmp = null;
			if (request.getSliceId() == null){
				tmp = mo.getInventory(auth);
			} else {
				tmp = mo.getInventory(new SliceID(request.getSliceId()), auth);
			}
			updateStatus(tmp.getStatus(), status);
			response.getInventory().addAll(tmp.getResult());
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "GetUnitRequest", namespace = AUTHORITY_NS)
	public @ResponsePayload
	GetUnitResponse getUnit(@RequestPayload GetUnitRequest request) {
		ResultMng status = new ResultMng();
		GetUnitResponse response = new GetUnitResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getUnitId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			AuthorityManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultUnitMng tmp = mo.getUnit(new UnitID(request.getUnitId()), auth);
			updateStatus(tmp.getStatus(), status);
			response.setUnit(getFirst(tmp.getResult()));
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}
		
		return response;
	}

	@PayloadRoot(localPart = "TransferInventoryRequest", namespace = AUTHORITY_NS)
	public @ResponsePayload
	TransferInventoryResponse transferInventory(@RequestPayload TransferInventoryRequest request) {
		ResultMng status = new ResultMng();
		TransferInventoryResponse response = new TransferInventoryResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getUnitId() == null || request.getSliceId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			AuthorityManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultMng tmp = mo.transferInventory(new SliceID(request.getSliceId()), new UnitID(request.getUnitId()), auth);
			updateStatus(tmp, status);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}

	@PayloadRoot(localPart = "UntransferInventoryRequest", namespace = AUTHORITY_NS)
	public @ResponsePayload
	UntransferInventoryResponse untransferInventory(@RequestPayload UntransferInventoryRequest request) {
		ResultMng status = new ResultMng();
		UntransferInventoryResponse response = new UntransferInventoryResponse();
		response.setStatus(status);
		
		try {
			if (request.getGuid() == null || request.getUnitId() == null) {
				status.setCode(OrcaConstants.ErrorInvalidArguments);
				return response;
			}
			AuthToken auth = Converter.fill(request.getAuth());
			AuthorityManagementObject mo = getActorMO(new ID(request.getGuid()));
			ResultMng tmp = mo.untransferInventory(new UnitID(request.getUnitId()), auth);
			updateStatus(tmp, status);
		}catch (Exception e){
			status.setCode(OrcaConstants.ErrorInternalError);
			status.setMessage(e.getMessage());
			status.setDetails(ExceptionUtils.getStackTraceString(e.getStackTrace()));
		}		
		return response;
	}

}