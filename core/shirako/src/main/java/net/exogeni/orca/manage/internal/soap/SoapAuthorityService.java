package net.exogeni.orca.manage.internal.soap;

import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.manage.beans.ResultMng;
import net.exogeni.orca.manage.beans.ResultReservationMng;
import net.exogeni.orca.manage.beans.ResultUnitMng;
import net.exogeni.orca.manage.internal.AuthorityManagementObject;
import net.exogeni.orca.manage.internal.Converter;
import net.exogeni.orca.manage.proxies.soap.beans.authority.GetAuthorityReservationsRequest;
import net.exogeni.orca.manage.proxies.soap.beans.authority.GetAuthorityReservationsResponse;
import net.exogeni.orca.manage.proxies.soap.beans.authority.GetInventoryRequest;
import net.exogeni.orca.manage.proxies.soap.beans.authority.GetInventoryResponse;
import net.exogeni.orca.manage.proxies.soap.beans.authority.GetReservationUnitsRequest;
import net.exogeni.orca.manage.proxies.soap.beans.authority.GetReservationUnitsResponse;
import net.exogeni.orca.manage.proxies.soap.beans.authority.GetUnitRequest;
import net.exogeni.orca.manage.proxies.soap.beans.authority.GetUnitResponse;
import net.exogeni.orca.manage.proxies.soap.beans.authority.TransferInventoryRequest;
import net.exogeni.orca.manage.proxies.soap.beans.authority.TransferInventoryResponse;
import net.exogeni.orca.manage.proxies.soap.beans.authority.UntransferInventoryRequest;
import net.exogeni.orca.manage.proxies.soap.beans.authority.UntransferInventoryResponse;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.shirako.common.UnitID;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.util.ExceptionUtils;
import net.exogeni.orca.util.ID;

import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

public class SoapAuthorityService extends SoapService {
	public static final String AUTHORITY_NS = "http://www.nicl.duke.edu/net/exogeni/orca/manage/services/authority";

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
