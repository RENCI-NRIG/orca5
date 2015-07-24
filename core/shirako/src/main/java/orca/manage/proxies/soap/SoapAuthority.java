package orca.manage.proxies.soap;

import java.util.List;

import orca.manage.IOrcaActor;
import orca.manage.IOrcaAuthority;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.UnitMng;
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
import orca.util.ID;

public class SoapAuthority extends SoapServerActor implements IOrcaAuthority {
	public SoapAuthority(ID managementID, String url, AuthToken auth) {
		super(managementID, url, auth);
	}

	public List<ReservationMng> getAuthorityReservations() {
		clearLast();
		try {
			GetAuthorityReservationsRequest req = new GetAuthorityReservationsRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);

			GetAuthorityReservationsResponse resp = (GetAuthorityReservationsResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getReservations();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<UnitMng> getUnits(ReservationID reservationID) {
		clearLast();
		if (reservationID == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			GetReservationUnitsRequest req = new GetReservationUnitsRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setReservationId(reservationID.toString());
			GetReservationUnitsResponse resp = (GetReservationUnitsResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getUnits();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<UnitMng> getInventory() {
		clearLast();
		try {
			GetInventoryRequest req = new GetInventoryRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);

			GetInventoryResponse resp = (GetInventoryResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getInventory();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<UnitMng> getInventory(SliceID sliceId) {
		clearLast();
		if (sliceId == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			GetInventoryRequest req = new GetInventoryRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setSliceId(sliceId.toString());
			GetInventoryResponse resp = (GetInventoryResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getInventory();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public UnitMng getUnit(UnitID unit) {
		clearLast();
		if (unit == null) {
			lastException = new IllegalArgumentException();
			return null;
		}

		try {
			GetUnitRequest req = new GetUnitRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setUnitId(unit.toString());

			GetUnitResponse resp = (GetUnitResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getUnit();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public boolean transferInventory(SliceID sliceId, UnitID unit) {
		clearLast();
		if (sliceId == null || unit == null) {
			lastException = new IllegalArgumentException();
			return false;
		}
		try {
			TransferInventoryRequest req = new TransferInventoryRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setSliceId(sliceId.toString());
			req.setUnitId(unit.toString());

			TransferInventoryResponse resp = (TransferInventoryResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
		}
		return false;
	}

	public boolean untransferInventory(UnitID unit) {
		clearLast();
		if (unit == null) {
			lastException = new IllegalArgumentException();
			return false;
		}
		try {
			UntransferInventoryRequest req = new UntransferInventoryRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setUnitId(unit.toString());

			UntransferInventoryResponse resp = (UntransferInventoryResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
		}
		return false;
	}
	
	public IOrcaActor clone() {
		return new SoapAuthority(managementID, url, auth);
	}

}