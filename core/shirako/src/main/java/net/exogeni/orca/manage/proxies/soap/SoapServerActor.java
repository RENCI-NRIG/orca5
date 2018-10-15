package net.exogeni.orca.manage.proxies.soap;

import java.security.cert.Certificate;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import net.exogeni.orca.manage.IOrcaActor;
import net.exogeni.orca.manage.IOrcaServerActor;
import net.exogeni.orca.manage.OrcaManagementException;
import net.exogeni.orca.manage.beans.CertificateMng;
import net.exogeni.orca.manage.beans.ClientMng;
import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.manage.beans.SliceMng;
import net.exogeni.orca.manage.internal.Converter;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.AddClientSliceRequest;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.AddClientSliceResponse;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.ExportResourcesRequest;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.ExportResourcesResponse;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetBrokerReservationsRequest;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetBrokerReservationsResponse;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetClientCertificateRequest;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetClientCertificateResponse;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetClientRequest;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetClientReservationsRequest;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetClientReservationsResponse;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetClientResponse;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetClientSlicesRequest;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetClientSlicesResponse;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetClientsRequest;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetClientsResponse;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetInventoryReservationsRequest;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetInventoryReservationsResponse;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetInventorySlicesRequest;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.GetInventorySlicesResponse;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.RegisterClientRequest;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.RegisterClientResponse;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.UnregisterClientRequest;
import net.exogeni.orca.manage.proxies.soap.beans.serveractor.UnregisterClientResponse;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.util.CertificateUtils;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.ResourceType;

public class SoapServerActor extends SoapActor implements IOrcaServerActor {
	public SoapServerActor(ID managementID, String url, AuthToken auth) {
		super(managementID, url, auth);
	}

	public List<SliceMng> getInventorySlices() {
		clearLast();
		try {
			GetInventorySlicesRequest req = new GetInventorySlicesRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);

			GetInventorySlicesResponse resp = (GetInventorySlicesResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getSlices();
			}
			return null;
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public List<ReservationMng> getBrokerReservations() {
		clearLast();
		try {
			GetBrokerReservationsRequest req = new GetBrokerReservationsRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);

			GetBrokerReservationsResponse resp = (GetBrokerReservationsResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getReservations();
			}
			return null;
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	protected List<ReservationMng> doGetInventoryReservations(SliceID sliceID) {
		clearLast();
		try {
			GetInventoryReservationsRequest req = new GetInventoryReservationsRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			if (sliceID != null) {
				req.setSliceId(sliceID.toString());
			}
			GetInventoryReservationsResponse resp = (GetInventoryReservationsResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getReservations();
			}
			return null;
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public List<ReservationMng> getInventoryReservations() {
		return doGetInventoryReservations(null);
	}

	public List<ReservationMng> getInventoryReservations(SliceID sliceID) {
		clearLast();
		if (sliceID == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		return doGetInventoryReservations(sliceID);
	}

	public List<SliceMng> getClientSlices() {
		clearLast();
		try {
			GetClientSlicesRequest req = new GetClientSlicesRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);

			GetClientSlicesResponse resp = (GetClientSlicesResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getSlices();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public SliceID addClientSlice(SliceMng slice) {
		clearLast();
		try {
			AddClientSliceRequest req = new AddClientSliceRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setSlice(slice);

			AddClientSliceResponse resp = (AddClientSliceResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return new SliceID(resp.getSliceId());
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public List<ClientMng> getClients() {
		clearLast();
		try {
			GetClientsRequest req = new GetClientsRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);

			GetClientsResponse resp = (GetClientsResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getClients();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public ClientMng getClient(ID guid) {
		clearLast();
		if (guid == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			GetClientRequest req = new GetClientRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setClientId(guid.toString());

			GetClientResponse resp = (GetClientResponse) client.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getClient();
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public Certificate getClientCertificate(ID guid) {
		clearLast();
		try {
			if (guid == null) {
				throw new IllegalArgumentException();
			}

			GetClientCertificateRequest req = new GetClientCertificateRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setClientId(guid.toString());
			GetClientCertificateResponse resp = (GetClientCertificateResponse) client
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

	public boolean registerClient(ClientMng client, Certificate certificate) {
		clearLast();
		if (certificate == null || client == null) {
			lastException = new IllegalArgumentException();
			return false;
		}
		try {
			RegisterClientRequest req = new RegisterClientRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setClient(client);
			req.setCertificate(new CertificateMng());
			req.getCertificate().setContents(certificate.getEncoded());

			RegisterClientResponse resp = (RegisterClientResponse) this.client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return lastStatus.getCode() == 0;
		} catch (Exception e) {
			lastException = e;
		}
		return false;
	}

	public boolean unregisterClient(ID guid) {
		clearLast();
		try {
			if (guid == null) {
				throw new IllegalArgumentException();
			}

			UnregisterClientRequest req = new UnregisterClientRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setClientId(guid.toString());

			UnregisterClientResponse resp = (UnregisterClientResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}

	protected List<ReservationMng> doGetClientReservations(SliceID sliceID) {
		clearLast();
		try {
			GetClientReservationsRequest req = new GetClientReservationsRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			if (sliceID != null) {
				req.setSliceId(sliceID.toString());
			}
			GetClientReservationsResponse resp = (GetClientReservationsResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return resp.getReservations();
			}
			return null;
		} catch (Exception e) {
			lastException = e;
			return null;
		}
	}

	public List<ReservationMng> getClientReservations() {
		return doGetClientReservations(null);
	}

	public List<ReservationMng> getClientReservations(SliceID sliceID) {
		clearLast();
		if (sliceID == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		return doGetClientReservations(sliceID);
	}

	public ReservationID exportResources(SliceID clientSliceID, SliceID poolID,
			Date start, Date end, int units, Properties ticketProperties, Properties resourceProperties,
			ReservationID sourceTicketID) {
		clearLast();
		if (clientSliceID == null || poolID == null || sourceTicketID == null | start == null
				|| end == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			ExportResourcesRequest req = new ExportResourcesRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setClientSliceId(clientSliceID.toString());
			req.setResourcePoolId(poolID.toString());
			if (sourceTicketID != null) {
				req.setTicketId(sourceTicketID.toString());
			}
			req.setStartTime(start.getTime());
			req.setStopTime(end.getTime());
			req.setUnits(units);
			if (ticketProperties != null) {
				req.setTicketProperties(Converter.fill(ticketProperties));
			}
			if (resourceProperties != null) {
				req.setResourceProperties(Converter.fill(resourceProperties));
			}

			ExportResourcesResponse resp = (ExportResourcesResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return new ReservationID(resp.getReservationId());
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public ReservationID exportResources(SliceID poolID,
			Date start, Date end, int units, Properties ticketProperties, Properties resourceProperties,
			ReservationID sourceTicketID, AuthToken clientToExportTo) {
		clearLast();
		if (clientToExportTo == null || clientToExportTo.getName() == null || clientToExportTo.getGuid() == null || poolID == null || sourceTicketID == null | start == null
				|| end == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			ExportResourcesRequest req = new ExportResourcesRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setClientName(clientToExportTo.getName());
			req.setClientGuid(clientToExportTo.getGuid().toString());
			req.setResourcePoolId(poolID.toString());
			if (sourceTicketID != null) {
				req.setTicketId(sourceTicketID.toString());
			}
			req.setStartTime(start.getTime());
			req.setStopTime(end.getTime());
			req.setUnits(units);
			if (ticketProperties != null) {
				req.setTicketProperties(Converter.fill(ticketProperties));
			}
			if (resourceProperties != null) {
				req.setResourceProperties(Converter.fill(resourceProperties));
			}

			ExportResourcesResponse resp = (ExportResourcesResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return new ReservationID(resp.getReservationId());
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public ReservationID exportResources(SliceID clientSliceID, ResourceType resourceType, 
			Date start, Date end, int units, Properties ticketProperties,
			Properties resourceProperties,
			ReservationID sourceTicketID) {
		clearLast();
		if (clientSliceID == null || resourceType == null | start == null
				|| end == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			ExportResourcesRequest req = new ExportResourcesRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setClientSliceId(clientSliceID.toString());
			req.setResourceType(resourceType.toString());
			if (sourceTicketID != null) {
				req.setTicketId(sourceTicketID.toString());
			}
			req.setStartTime(start.getTime());
			req.setStopTime(end.getTime());
			req.setUnits(units);
			if (ticketProperties != null) {
				req.setTicketProperties(Converter.fill(ticketProperties));
			}
			if (resourceProperties != null) {
				req.setResourceProperties(Converter.fill(resourceProperties));
			}

			ExportResourcesResponse resp = (ExportResourcesResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return new ReservationID(resp.getReservationId());
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public ReservationID exportResources(ResourceType resourceType, 
			Date start, Date end, int units, Properties ticketProperties,
			Properties resourceProperties,
			ReservationID sourceTicketID, AuthToken clientToExportTo) {
		clearLast();
		if (clientToExportTo == null || clientToExportTo.getName() == null || clientToExportTo.getGuid() == null || resourceType == null | start == null
				|| end == null) {
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			ExportResourcesRequest req = new ExportResourcesRequest();
			req.setGuid(managementID.toString());
			req.setAuth(authMng);
			req.setClientName(clientToExportTo.getName());
			req.setClientGuid(clientToExportTo.getGuid().toString());
			req.setResourceType(resourceType.toString());
			if (sourceTicketID != null) {
				req.setTicketId(sourceTicketID.toString());
			}
			req.setStartTime(start.getTime());
			req.setStopTime(end.getTime());
			req.setUnits(units);
			if (ticketProperties != null) {
				req.setTicketProperties(Converter.fill(ticketProperties));
			}
			if (resourceProperties != null) {
				req.setResourceProperties(Converter.fill(resourceProperties));
			}

			ExportResourcesResponse resp = (ExportResourcesResponse) client
					.marshalSendAndReceive(req);
			lastStatus = resp.getStatus();
			if (lastStatus.getCode() == 0) {
				return new ReservationID(resp.getReservationId());
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public IOrcaActor clone() {
		return new SoapServerActor(managementID, url, auth);
	}

}
