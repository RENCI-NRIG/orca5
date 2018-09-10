package net.exogeni.orca.manage.proxies.soap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import net.exogeni.orca.manage.IOrcaActor;
import net.exogeni.orca.manage.IOrcaBroker;
import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.manage.OrcaConverter;
import net.exogeni.orca.manage.OrcaManagementException;
import net.exogeni.orca.manage.beans.PoolInfoMng;
import net.exogeni.orca.manage.beans.ProxyMng;
import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.manage.beans.TicketReservationMng;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.AddBrokerRequest;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.AddBrokerResponse;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.AddReservationRequest;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.AddReservationResponse;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.AddReservationsRequest;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.AddReservationsResponse;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.ClaimResourcesRequest;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.ClaimResourcesResponse;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.DemandReservationRequest;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.DemandReservationResponse;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.ExtendReservationRequest;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.ExtendReservationResponse;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.GetBrokerRequest;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.GetBrokerResponse;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.GetBrokersRequest;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.GetBrokersResponse;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.GetPoolInfoRequest;
import net.exogeni.orca.manage.proxies.soap.beans.clientactor.GetPoolInfoResponse;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.ResourceType;

public class SoapBroker extends SoapServerActor implements IOrcaBroker {

	public SoapBroker(ID managementID, String url, AuthToken auth) {
		super(managementID, url, auth);
	}

	public ReservationID addReservation(TicketReservationMng reservation) {
		clearLast();
		if (reservation == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			AddReservationRequest req = new AddReservationRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setReservation(reservation);
			AddReservationResponse resp = (AddReservationResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				// store the reservation ID on the passed-in reservation object
				// as well
				reservation.setReservationID(resp.getReservationId());
				return new ReservationID(resp.getReservationId());
			}
			return null;
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public List<ReservationID> addReservations(List<TicketReservationMng> reservations) {
		clearLast();
		if (reservations == null || reservations.size() == 0) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			AddReservationsRequest req = new AddReservationsRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.getReservations().addAll(reservations);
			AddReservationsResponse resp = (AddReservationsResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				if (resp.getReservationIds().size() != reservations.size()) {
					throw new OrcaManagementException(
							"The server returned invalid number of reservation ids: expected="
									+ reservations.size() + " actual="
									+ resp.getReservationIds().size());
				}
				List<ReservationID> result = new ArrayList<ReservationID>(reservations.size());
				for (int i = 0; i < resp.getReservationIds().size(); i++) {
					result.add(new ReservationID(resp.getReservationIds().get(i)));
					reservations.get(i).setReservationID(resp.getReservationIds().get(i));
				}
				return result;
			}
			return null;
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public List<ProxyMng> getBrokers() {
		clearLast();
		try {
			GetBrokersRequest req = new GetBrokersRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);

			GetBrokersResponse resp = (GetBrokersResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getBrokers();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public ProxyMng getBroker(ID broker) {
		clearLast();
		if (broker == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			GetBrokerRequest req = new GetBrokerRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setBrokerId(broker.toString());

			GetBrokerResponse resp = (GetBrokerResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getBroker();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public boolean addBroker(ProxyMng broker) {
		clearLast();
		if (broker == null) {
			lastException = new IllegalArgumentException();
			return false;
		}
		try {
			AddBrokerRequest req = new AddBrokerRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setBroker(broker);

			AddBrokerResponse resp = (AddBrokerResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
		}
		return false;
	}

	public ReservationMng claimResources(ID brokerGuid, SliceID sliceID, ReservationID reservationID) {
		clearLast();
		if (brokerGuid == null || sliceID == null || reservationID == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			ClaimResourcesRequest req = new ClaimResourcesRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setBrokerId(brokerGuid.toString());
			req.setSliceId(sliceID.toString());
			req.setReservationId(reservationID.toString());

			ClaimResourcesResponse resp = (ClaimResourcesResponse)client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getReservation();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public ReservationMng claimResources(ID brokerGuid, ReservationID reservationID) {
		clearLast();
		if (brokerGuid == null || reservationID == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			ClaimResourcesRequest req = new ClaimResourcesRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setBrokerId(brokerGuid.toString());
			req.setReservationId(reservationID.toString());

			ClaimResourcesResponse resp = (ClaimResourcesResponse)client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getReservation();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<PoolInfoMng> getPoolInfo(ID broker) {
		clearLast();
		if (broker == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			GetPoolInfoRequest req = new GetPoolInfoRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setBrokerId(broker.toString());
			GetPoolInfoResponse resp = (GetPoolInfoResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getPoolInfo();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public IOrcaActor clone() {
		return new SoapBroker(managementID, url, auth);
	}

	public boolean demand(ReservationID reservationID) {
		clearLast();
		if (reservationID == null) {
			lastException = new IllegalArgumentException();
			return false;
		}
		try {
			DemandReservationRequest req = new DemandReservationRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setReservationId(reservationID.toString());
			DemandReservationResponse resp = (DemandReservationResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
		}
		return false;
	}

	public boolean demand(ReservationMng reservation) {
		clearLast();
		if (reservation == null) {
			lastException = new IllegalArgumentException();
			return false;
		}
		try {
			DemandReservationRequest req = new DemandReservationRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setReservation(reservation);
			DemandReservationResponse resp = (DemandReservationResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
		}
		return false;
	}

	public boolean extendReservation(ReservationID reservation, Date newEndTime){
		return extendReservation(reservation, newEndTime, OrcaConstants.ExtendSameUnits, null, null, null);
	}

	public boolean extendReservation(ReservationID reservation, Date newEndTime, Properties requestProperties){
		return extendReservation(reservation, newEndTime, OrcaConstants.ExtendSameUnits, null, requestProperties, null);
	}

	public boolean extendReservation(ReservationID reservation, Date newEndTime, Properties requestProperties, Properties configProperties){
		return extendReservation(reservation, newEndTime, OrcaConstants.ExtendSameUnits, null, requestProperties, configProperties);
	}

	public boolean extendReservation(ReservationID reservation, Date newEndTime, int newUnits,
			ResourceType newResourceType, Properties requestProperties, Properties configProperties) {
		clearLast();
		if (reservation == null || newEndTime == null) {
			lastException = new IllegalArgumentException();
			return false;
		}
		try {
			ExtendReservationRequest req = new ExtendReservationRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setReservationID(reservation.toString());
			req.setNewUnits(newUnits);
			req.setEndTime(newEndTime.getTime());
			if (newResourceType != null) {
				req.setNewResourceType(newResourceType.toString());
			}
			if (requestProperties != null) {
				req.setRequestProperties(OrcaConverter.fill(requestProperties));
			}
			if (configProperties != null) {
				req.setConfigProperties(OrcaConverter.fill(configProperties));
			}
			ExtendReservationResponse resp = (ExtendReservationResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return lastStatus.getCode() == 0;
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

}
