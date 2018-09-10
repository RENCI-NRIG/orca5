package net.exogeni.orca.manage.proxies.soap;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import net.exogeni.orca.manage.IOrcaActor;
import net.exogeni.orca.manage.OrcaConstants;
import net.exogeni.orca.manage.OrcaManagementException;
import net.exogeni.orca.manage.beans.CertificateMng;
import net.exogeni.orca.manage.beans.EventMng;
import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.manage.beans.ReservationStateMng;
import net.exogeni.orca.manage.beans.SliceMng;
import net.exogeni.orca.manage.proxies.soap.beans.actor.AddSliceRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.AddSliceResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.CloseReservationRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.CloseReservationResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.CloseSliceReservationsRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.CloseSliceReservationsResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.CreateEventSubscriptionRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.CreateEventSubscriptionResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.DeleteEventSubscriptionRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.DeleteEventSubscriptionResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.DrainEventsRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.DrainEventsResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetActorNameRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetActorNameResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetCertificateRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetCertificateResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetReservationRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetReservationResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetReservationStateRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetReservationStateResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetReservationsRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetReservationsResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetReservationsStateRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetReservationsStateResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetSliceRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetSliceResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetSlicesRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.GetSlicesResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.RegisterCertificateRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.RegisterCertificateResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.RemoveReservationRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.RemoveReservationResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.RemoveSliceRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.RemoveSliceResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.UnregisterCertificateRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.UnregisterCertificateResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.UpdateReservationRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.UpdateReservationResponse;
import net.exogeni.orca.manage.proxies.soap.beans.actor.UpdateSliceRequest;
import net.exogeni.orca.manage.proxies.soap.beans.actor.UpdateSliceResponse;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.util.CertificateUtils;
import net.exogeni.orca.util.ID;

public class SoapActor extends SoapProxy implements IOrcaActor {
	protected String actorName;

	public SoapActor(ID managementID, String url, AuthToken auth) {
		super(managementID, url, auth);
		loggedIn = true;
	}

	public Certificate getCertificate() {
		clearLast();
		try {
			GetCertificateRequest req = new GetCertificateRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
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

	public boolean registerCertificate(Certificate certificate, String alias) {
		clearLast();
		try {
			if (alias == null || certificate == null) {
				throw new IllegalArgumentException();
			}

			RegisterCertificateRequest req = new RegisterCertificateRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setAlias(alias);
			req.setCertificate(new CertificateMng());
			req.getCertificate().setContents(certificate.getEncoded());
			RegisterCertificateResponse resp = (RegisterCertificateResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return false;
			}
			return true;
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public boolean unregisterCertificate(String alias) {
		clearLast();
		try {
			if (alias == null) {
				throw new IllegalArgumentException();
			}

			UnregisterCertificateRequest req = new UnregisterCertificateRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setAlias(alias);
			UnregisterCertificateResponse resp = (UnregisterCertificateResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return false;
			}
			return true;
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public Certificate getCertificate(String alias) {
		clearLast();
		try {
			if (alias == null) {
				throw new IllegalArgumentException();
			}

			GetCertificateRequest req = new GetCertificateRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setAlias(alias);
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

	public List<SliceMng> getSlices() {
		clearLast();
		try {
			GetSlicesRequest req = new GetSlicesRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			GetSlicesResponse resp = (GetSlicesResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			return resp.getSlices();
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public SliceMng getSlice(SliceID sliceId) {
		clearLast();
		try {
			GetSliceRequest req = new GetSliceRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setSliceId(sliceId.toString());
			GetSliceResponse resp = (GetSliceResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() != 0) {
				return null;
			}
			return resp.getSlice();
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public boolean removeSlice(SliceID sliceId) {
		clearLast();
		try {
			RemoveSliceRequest req = new RemoveSliceRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setSliceId(sliceId.toString());
			RemoveSliceResponse resp = (RemoveSliceResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	public SliceID addSlice(SliceMng slice) {
		clearLast();
		try {
			AddSliceRequest req = new AddSliceRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setSlice(slice);
			AddSliceResponse resp = (AddSliceResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				slice.setSliceID(resp.getSliceId());
				return new SliceID(resp.getSliceId());
			}
			return null;
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public boolean updateSlice(SliceMng slice) {
		clearLast();
		try {
			UpdateSliceRequest req = new UpdateSliceRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setSlice(slice);
			UpdateSliceResponse resp = (UpdateSliceResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	protected List<ReservationMng> doGetReservations(SliceID sliceID, int state) {
		clearLast();
		try {
			GetReservationsRequest req = new GetReservationsRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setReservationState(state);
			if (sliceID != null){
				req.setSliceId(sliceID.toString());
			}
			GetReservationsResponse resp = (GetReservationsResponse)client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getReservations();
			}
			return null;
		} catch (Exception e){
			lastException = e;
			return null;
		}
	}
	
	public List<ReservationMng> getReservations() {
		return doGetReservations(null, OrcaConstants.AllReservationStates);
	}

	public List<ReservationMng> getReservations(int state) {
		return doGetReservations(null, state);
	}

	public List<ReservationMng> getReservations(SliceID sliceID) {
		clearLast();
		if (sliceID == null){
			lastException = new IllegalArgumentException();
			return null;
		}
		return doGetReservations(sliceID, OrcaConstants.AllReservationStates);
	}

	public List<ReservationMng> getReservations(SliceID slice, int state) {
		return doGetReservations(slice, state);
	}

	public ReservationMng getReservation(ReservationID reservationID) {
		clearLast();
		if (reservationID == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			GetReservationRequest req = new GetReservationRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setReservationId(reservationID.toString());
			GetReservationResponse resp = (GetReservationResponse)client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getReservation();
			}
			return null;
		} catch (Exception e){
			lastException = e;
			return null;
		}
	}

	public boolean removeReservation(ReservationID reservationID) {
		clearLast();
		if (reservationID == null) {
			lastException = new IllegalArgumentException();
			return false;
		}
		try {
			RemoveReservationRequest req = new RemoveReservationRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setReservationId(reservationID.toString());
			RemoveReservationResponse resp = (RemoveReservationResponse)client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return lastStatus.getCode() == 0;
		} catch (Exception e){
			lastException = e;
			return false;
		}
	}

	public boolean closeReservation(ReservationID reservationID) {
		clearLast();
		if (reservationID == null) {
			lastException = new IllegalArgumentException();
			return false;
		}
		try {
			CloseReservationRequest req = new CloseReservationRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setReservationId(reservationID.toString());
			CloseReservationResponse resp = (CloseReservationResponse)client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return lastStatus.getCode() == 0;
		} catch (Exception e){
			lastException = e;
			return false;
		}
	}
	
	public boolean closeReservations(SliceID sliceID) {
		clearLast();
		if (sliceID == null) {
			lastException = new IllegalArgumentException();
			return false;
		}
		try {
			CloseSliceReservationsRequest req = new CloseSliceReservationsRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setSliceId(sliceID.toString());
			CloseSliceReservationsResponse resp = (CloseSliceReservationsResponse)client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return lastStatus.getCode() == 0;
		} catch (Exception e){
			lastException = e;
			return false;
		}
	}

	public boolean updateReservation(ReservationMng reservation){
		clearLast();
		if (reservation == null) {
			lastException = new IllegalArgumentException();
			return false;
		}
		try {
			UpdateReservationRequest req = new UpdateReservationRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setReservation(reservation);
			UpdateReservationResponse resp = (UpdateReservationResponse)client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return lastStatus.getCode() == 0;
		} catch (Exception e){
			lastException = e;
			return false;
		}
		
	}
	public String getName() {
		if (actorName != null) {return actorName;}
		clearLast();
		try {
			GetActorNameRequest req = new GetActorNameRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			GetActorNameResponse resp = (GetActorNameResponse)client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				actorName = resp.getName();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return actorName;
	}

	public ID getGuid() {
		return managementID;
	}

	public ID createEventSubscription() {
		clearLast();
		try {
			CreateEventSubscriptionRequest req = new CreateEventSubscriptionRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			CreateEventSubscriptionResponse resp = (CreateEventSubscriptionResponse)client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0 && resp.getSubscriptionId() != null) {
				return new ID(resp.getSubscriptionId());
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public boolean deleteEventSubscription(ID subscriptionID) {
		clearLast();
		if (subscriptionID == null){
			lastException = new IllegalArgumentException();
			return false;
		}
		try {
			DeleteEventSubscriptionRequest req = new DeleteEventSubscriptionRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setSubscriptionId(subscriptionID.toString());
			DeleteEventSubscriptionResponse resp = (DeleteEventSubscriptionResponse)client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return lastStatus.getCode() == 0;
		} catch (Exception e) {
			lastException = e;
		}
		return false;
	}

	public List<EventMng> drainEvents(ID subscriptionID, int timeout) {
		clearLast();
		if (subscriptionID == null){
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			DrainEventsRequest req = new DrainEventsRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setSubscriptionId(subscriptionID.toString());
			req.setTimeout(timeout);
			DrainEventsResponse resp = (DrainEventsResponse)client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (resp.getStatus().getCode() == 0){
				return resp.getEvents();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}
	
	public IOrcaActor clone() {
		return new SoapActor(managementID, url, auth);
	}

	public ReservationStateMng getReservationState(ReservationID reservationID) {
		clearLast();
		if (reservationID == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			GetReservationStateRequest req = new GetReservationStateRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setReservationId(reservationID.toString());
			GetReservationStateResponse resp = (GetReservationStateResponse)client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getReservationState();
			}
		} catch (Exception e){
			lastException = e;
		}
		return null;
	}

	public List<ReservationStateMng> getReservationState(List<ReservationID> reservations) {
		clearLast();
		if (reservations == null || reservations.size() == 0) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			GetReservationsStateRequest req = new GetReservationsStateRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			for (ReservationID rid : reservations){
				req.getReservationId().add(rid.toString());
			}
			GetReservationsStateResponse resp = (GetReservationsStateResponse)client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				if (resp.getReservationState().size() != reservations.size()){
					throw new OrcaManagementException("The server returned invalid number of reservation ids: expected=" + reservations.size() + " actual=" + resp.getReservationState().size());
				}
				return resp.getReservationState();
			}
		} catch (Exception e){
			lastException = e;
		}
		return null;
	}
}
