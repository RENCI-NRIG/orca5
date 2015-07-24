package orca.manage.internal.local;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import orca.manage.IOrcaActor;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.OrcaConverter;
import orca.manage.beans.PoolInfoMng;
import orca.manage.beans.ProxyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.ResultMng;
import orca.manage.beans.ResultPoolInfoMng;
import orca.manage.beans.ResultProxyMng;
import orca.manage.beans.ResultReservationMng;
import orca.manage.beans.ResultStringMng;
import orca.manage.beans.ResultStringsMng;
import orca.manage.beans.ResultUnitMng;
import orca.manage.beans.TicketReservationMng;
import orca.manage.beans.UnitMng;
import orca.manage.internal.ManagementObject;
import orca.manage.internal.ServiceManagerManagementObject;
import orca.security.AuthToken;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.util.ID;
import orca.util.ResourceType;

public class LocalServiceManager extends LocalActor implements IOrcaServiceManager {
	protected ServiceManagerManagementObject manager;
	public LocalServiceManager(ManagementObject manager, AuthToken auth) {
		super(manager, auth);
		if (!(manager instanceof ServiceManagerManagementObject)) {
			throw new RuntimeException("Invalid manager object. Required: "
					+ ServiceManagerManagementObject.class.getCanonicalName());
		}
		this.manager = (ServiceManagerManagementObject)manager;
	}

	public boolean addBroker(ProxyMng broker) {
		clearLast();
		try {
			ResultMng tmp = manager.addBroker(broker, auth);
			lastStatus = tmp;
			return (lastStatus.getCode() == 0);
		}catch (Exception e){
			lastException = e;
		}
		return false;
	}


	public List<ProxyMng> getBrokers() {
		clearLast();
		try {
			ResultProxyMng tmp = manager.getBrokers(auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0){
				return tmp.getResult();
			}
		}catch (Exception e){
			lastException = e;
		}
		return null;
	}

	public ProxyMng getBroker(ID broker) {
		clearLast();
		try {
			ResultProxyMng tmp = manager.getBroker(broker, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0){
				return getFirst(tmp.getResult());
			}
		}catch (Exception e){
			lastException = e;
		}
		return null;
	}

	public List<PoolInfoMng> getPoolInfo(ID broker){
		clearLast();
		try {
			ResultPoolInfoMng tmp = manager.getPoolInfo(broker, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0){
				return tmp.getResult();
			}
		}catch (Exception e){
			lastException = e;
		}
		return null;
	}

	public ReservationMng claimResources(ID brokerGuid, SliceID sliceID, ReservationID reservationID) {
		clearLast();
		try {
			ResultReservationMng tmp = manager.claimResources(brokerGuid, sliceID, reservationID, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0){
				return getFirst(tmp.getResult());
			}
		}catch (Exception e){
			lastException = e;
		}
		return null;
	}

	public ReservationMng claimResources(ID brokerGuid, ReservationID reservationID) {
		clearLast();
		try {
			ResultReservationMng tmp = manager.claimResources(brokerGuid, reservationID, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0){
				return getFirst(tmp.getResult());
			}
		}catch (Exception e){
			lastException = e;
		}
		return null;
	}

	public List<UnitMng> getUnits(ReservationID reservationID) throws Exception {
		clearLast();
		try {
			ResultUnitMng tmp = manager.getReservationUnits(reservationID, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0){
				return tmp.getResult();
			}
		}catch (Exception e){
			lastException = e;
		}
		return null;		
	}
	
	public ReservationID addReservation(TicketReservationMng reservation) {
    	clearLast();
		try {
			ResultStringMng tmp = manager.addReservation(reservation, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0 && tmp.getResult() != null) {
				reservation.setReservationID(tmp.getResult());
				return new ReservationID(tmp.getResult());
			}
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public List<ReservationID> addReservations(List<TicketReservationMng> reservations) {
    	clearLast();
		try {
			ResultStringsMng tmp = manager.addReservations(reservations, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0 && tmp.getResult() != null) {
				List<ReservationID> list = new ArrayList<ReservationID>(reservations.size());
				for (int i=0; i < reservations.size(); i++) {
					ReservationMng r = reservations.get(i);
					ReservationID rid = new ReservationID(tmp.getResult().get(i));
					r.setReservationID(rid.toString());
					list.add(rid);
				}
				return list;
			}
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public boolean demand(ReservationID reservationID) {
    	clearLast();
		try {
			ResultMng tmp = manager.demandReservation(reservationID, auth);
			lastStatus = tmp;
			return (lastStatus.getCode() == 0);
		}catch(Exception e){
			lastException = e;
		}
		return false;
	}

	public boolean demand(ReservationMng reservation) {
    	clearLast();
		try {
			ResultMng tmp = manager.demandReservation(reservation, auth);
			lastStatus = tmp;
			return (lastStatus.getCode() == 0);
		}catch(Exception e){
			lastException = e;
		}
		return false;
	}

	public IOrcaActor clone() {
		return new LocalServiceManager(manager, auth);
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
    
    public boolean extendReservation(ReservationID reservation, 
			 Date endTime,
			 int newUnits,
			 ResourceType newResourceType,
			 Properties requestProperties, 
			 Properties configProperties){
    	clearLast();
    	try {
    		lastStatus = manager.extendReservation(reservation, endTime, newUnits, newResourceType, requestProperties, configProperties, auth);
    		return (lastStatus.getCode() == 0);
    	} catch (Exception e){
    		lastException = e;
    	}
    	return false;	
    }
    
    public boolean modifyReservation(ReservationID reservation,  
			 Properties modifyProperties){
    	clearLast();
    	if (reservation == null || modifyProperties == null) {
    		lastException = new IllegalArgumentException();
    		return false;
    	}
    	try {
    		lastStatus = manager.modifyReservation(reservation, modifyProperties, auth);
    		return (lastStatus.getCode() == 0);
    	} catch (Exception e){
    		lastException = e;
    	}
    	return false;
    	
   }
    

}