package net.exogeni.orca.manage.internal.local;

import java.security.cert.Certificate;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import net.exogeni.orca.manage.IOrcaActor;
import net.exogeni.orca.manage.IOrcaServerActor;
import net.exogeni.orca.manage.beans.CertificateMng;
import net.exogeni.orca.manage.beans.ClientMng;
import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.manage.beans.ResultCertificateMng;
import net.exogeni.orca.manage.beans.ResultClientMng;
import net.exogeni.orca.manage.beans.ResultMng;
import net.exogeni.orca.manage.beans.ResultReservationMng;
import net.exogeni.orca.manage.beans.ResultSliceMng;
import net.exogeni.orca.manage.beans.ResultStringMng;
import net.exogeni.orca.manage.beans.SliceMng;
import net.exogeni.orca.manage.internal.Converter;
import net.exogeni.orca.manage.internal.ManagementObject;
import net.exogeni.orca.manage.internal.ServerActorManagementObject;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.util.CertificateUtils;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.ResourceType;

public class LocalServerActor extends LocalActor implements IOrcaServerActor {
	protected ServerActorManagementObject manager;

	public LocalServerActor(ManagementObject manager, AuthToken auth) {
		super(manager, auth);
		if (!(manager instanceof ServerActorManagementObject)) {
			throw new RuntimeException("Invalid manager object. Required: "
					+ ServerActorManagementObject.class.getCanonicalName());
		}
		this.manager = (ServerActorManagementObject)manager;

	}

	public List<SliceMng> getClientSlices() {
		clearLast();
		try {
			ResultSliceMng tmp = manager.getClientSlices(auth);
			lastStatus = tmp.getStatus();
			return tmp.getResult();
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public List<ClientMng> getClients() {
		clearLast();
		try {
			ResultClientMng tmp = manager.getClients(auth);
			lastStatus = tmp.getStatus();
			return tmp.getResult();
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public ClientMng getClient(ID guid) {
		clearLast();
		if (guid == null){
			lastException = new IllegalArgumentException();
			return null;
		}
		
		try {
			ResultClientMng tmp = manager.getClient(guid, auth);
			lastStatus = tmp.getStatus();
			return getFirst(tmp.getResult());
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public Certificate getClientCertificate(ID guid) {
		clearLast();
		if (guid == null){
			lastException = new IllegalArgumentException();
			return null;
		}
		try {
			ResultCertificateMng c =  manager.getClientCertificate(guid, auth);		
			lastStatus = c.getStatus();
			if (c.getStatus().getCode() == 0){
				return CertificateUtils.decode(getFirst(c.getResult()).getContents());
			}
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public boolean registerClient(ClientMng client, Certificate certificate) {
		clearLast();
		if (client == null || certificate == null){
			lastException = new IllegalArgumentException();
			return false;
		}
		try {
			CertificateMng mng = Converter.fill(certificate);
			ResultMng tmp = manager.registerClient(client, mng, auth);
			lastStatus = tmp;
			return (lastStatus.getCode() == 0);
		}catch(Exception e){
			lastException = e;
		}
		return false;
	}

	public boolean unregisterClient(ID guid) {
		clearLast();
		if (guid == null){
			lastException = new IllegalArgumentException();
			return false;
		}
		
		try {
			ResultMng tmp = manager.unregisterClient(guid, auth);
			lastStatus = tmp;
			return (lastStatus.getCode() == 0);			
		}catch(Exception e){
			lastException = e;
		}
		return false;
	}

	public List<ReservationMng> getClientReservations() {
		clearLast();
		try {
			ResultReservationMng tmp = manager.getClientReservations(auth);
			lastStatus = tmp.getStatus();
			return tmp.getResult();
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public List<ReservationMng> getBrokerReservations() {
		clearLast();
		try {
			ResultReservationMng tmp = manager.getBrokerReservations(auth);
			lastStatus = tmp.getStatus();
			return tmp.getResult();			
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}


	public List<SliceMng> getInventorySlices() {
		clearLast();
		try {
			ResultSliceMng tmp = manager.getInventorySlices(auth);
			lastStatus = tmp.getStatus();
			return tmp.getResult();
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public List<ReservationMng> getInventoryReservations() {
		clearLast();
		try {
			ResultReservationMng tmp = manager.getInventoryReservations(auth);
			lastStatus = tmp.getStatus();
			return tmp.getResult();
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public List<ReservationMng> getInventoryReservations(SliceID sliceID) {
		clearLast();
		if (sliceID == null){
			lastException = new IllegalArgumentException();
			return null;
		}
		
		try {
			ResultReservationMng tmp = manager.getInventoryReservations(sliceID, auth);
			lastStatus = tmp.getStatus();
			return tmp.getResult();
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public SliceID addClientSlice(SliceMng slice) {
		clearLast();
		if (slice == null){
			lastException = new IllegalArgumentException();
			return null;			
		}
		
		try {
			ResultStringMng tmp = manager.addClientSlice(slice, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0 && tmp.getResult() != null){
				return new SliceID(tmp.getResult());
			}
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public List<ReservationMng> getClientReservations(SliceID sliceID) {
		clearLast();
		if (sliceID == null){
			lastException = new IllegalArgumentException();
			return null;
		}
		
		try {
			ResultReservationMng tmp = manager.getClientReservations(sliceID, auth);
			lastStatus = tmp.getStatus();
			return tmp.getResult();
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public ReservationID exportResources(SliceID clientSliceID, SliceID poolID,
			Date start, Date end, int units, Properties ticketProperties, Properties resourceProperties,
			ReservationID ticketId) {
		try {
			ResultStringMng tmp = manager.exportResources(clientSliceID, poolID,
					start, end, units, ticketProperties, resourceProperties, ticketId, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0 && tmp.getResult() != null){
				return new ReservationID(tmp.getResult());
			}
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public ReservationID exportResources(SliceID poolID, Date start, Date end, int units,
			Properties ticketProperties, Properties resourceProperties,
			ReservationID sourceTicketID, AuthToken clientToExportTo) {
		try {
			ResultStringMng tmp = manager.exportResources(poolID,
					start, end, units, ticketProperties, resourceProperties, sourceTicketID, clientToExportTo, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0 && tmp.getResult() != null){
				return new ReservationID(tmp.getResult());
			}			
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public ReservationID exportResources(SliceID clientSliceID, ResourceType resourceType,
			Date start, Date end, int units, Properties ticketProperties,
			Properties resourceProperties, ReservationID ticketId) {
		try {
			ResultStringMng tmp = manager.exportResources(clientSliceID, resourceType,
					start, end, units, ticketProperties, resourceProperties, ticketId, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0 && tmp.getResult() != null){
				return new ReservationID(tmp.getResult());
			}
			
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public ReservationID exportResources(ResourceType resourceType, Date start, Date end,
			int units, Properties ticketProperties, Properties resourceProperties,
			ReservationID sourceTicketID, AuthToken clientToExportTo) {
		try {
			ResultStringMng tmp = manager.exportResources(resourceType,
					start, end, units, ticketProperties, resourceProperties, sourceTicketID, clientToExportTo, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0 && tmp.getResult() != null){
				return new ReservationID(tmp.getResult());
			}			
			
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}	
	
	public IOrcaActor clone() {
		return new LocalServerActor(manager, auth);
	}

}
