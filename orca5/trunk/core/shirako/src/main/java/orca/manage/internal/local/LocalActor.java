/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.manage.internal.local;

import java.security.cert.Certificate;
import java.util.List;

import orca.manage.IOrcaActor;
import orca.manage.beans.CertificateMng;
import orca.manage.beans.EventMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.ReservationStateMng;
import orca.manage.beans.ResultCertificateMng;
import orca.manage.beans.ResultEventMng;
import orca.manage.beans.ResultMng;
import orca.manage.beans.ResultReservationMng;
import orca.manage.beans.ResultReservationStateMng;
import orca.manage.beans.ResultSliceMng;
import orca.manage.beans.ResultStringMng;
import orca.manage.beans.SliceMng;
import orca.manage.internal.ActorManagementObject;
import orca.manage.internal.Converter;
import orca.manage.internal.ManagementObject;
import orca.security.AuthToken;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.util.CertificateUtils;
import orca.util.ID;

public class LocalActor extends LocalProxy implements IOrcaActor {
	protected ActorManagementObject manager;
	
	public LocalActor(ManagementObject manager, AuthToken auth) {
		super(manager, auth);
		if (!(manager instanceof ActorManagementObject)) {
			throw new RuntimeException("Invalid manager object. Required: "
					+ ActorManagementObject.class.getCanonicalName());
		}
		this.manager = (ActorManagementObject)manager;
	}

	public Certificate getCertificate() {
		clearLast();
		try {
			ResultCertificateMng c = manager.getCertificate();		
			lastStatus = c.getStatus();
			if (c.getStatus().getCode() == 0){
				return CertificateUtils.decode(c.getResult().get(0).getContents());
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public Certificate getCertificate(String alias) {
		clearLast();
		try {
			ResultCertificateMng c = manager.getCertificate(alias, auth);		
			lastStatus = c.getStatus();
			if (c.getStatus().getCode() == 0){
				return CertificateUtils.decode(c.getResult().get(0).getContents());
			}
		} catch (Exception e) {
			lastException = e;
		}
		return null;
	}

	public boolean registerCertificate(Certificate certificate, String alias) {
		clearLast();
		try {
			CertificateMng mng = Converter.fill(certificate);
			lastStatus = manager.registerCertificate(mng, alias, auth);
			return (lastStatus.getCode() == 0);
		} catch (Exception e) {
			lastException = e;
			return false;
		}
	}
	
	public List<SliceMng> getSlices() {
		clearLast();
		try {
			ResultSliceMng r =  manager.getSlices(auth);
			lastStatus = r.getStatus();
			return r.getResult();
		} catch (Exception e){
			lastException = e;
			return null;
		}
	}

	public SliceMng getSlice(String sliceId) {
		clearLast();
		try {
			ResultSliceMng r =  ((ActorManagementObject)manager).getSlice(new SliceID(sliceId), auth);
			lastStatus = r.getStatus();
			return getFirst(r.getResult());
		} catch (Exception e){
			lastException = e;
			return null;
		}
	}
	
	public boolean removeSlice(String sliceId) {
		clearLast();
		try {
			ResultMng r =  ((ActorManagementObject)manager).removeSlice(new SliceID(sliceId), auth);
			lastStatus = r;
			return (r.getCode() == 0);
		} catch (Exception e){
			lastException = e;
			return false;
		}
	}

	public List<ReservationMng> getReservations() {
		clearLast();
		try {
			ResultReservationMng r = ((ActorManagementObject)manager).getReservations(auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0){
				return r.getResult();
			}
		} catch (Exception e){
			lastException = e;
		}
		return null;
	}
	
	public List<ReservationMng> getReservations(int state) {
		clearLast();
		try {
			ResultReservationMng r = ((ActorManagementObject)manager).getReservations(state, auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0){
				return r.getResult();
			}
		} catch (Exception e){
			lastException = e;
		}
		return null;
	}

    public boolean removeReservation(String reservationID) {
    	clearLast();
    	try {
    		lastStatus = manager.removeReservation(new ReservationID(reservationID), auth);
    		return (lastStatus.getCode() == 0);
    	} catch (Exception e){
    		lastException = e;
    	}
    	return false;
    }

    public boolean closeReservation(String reservationID) {
    	clearLast();
    	try {
    		lastStatus = manager.closeReservation(new ReservationID(reservationID), auth);
    		return (lastStatus.getCode() == 0);
    	} catch (Exception e){
    		lastException = e;
    	}
    	return false;
    }
    
    public String getName() {
    	return manager.getActorName();
    }
    
    public ID getGuid() {
    	return manager.getActor().getGuid();
    }

	public boolean unregisterCertificate(String alias) {
    	clearLast();
    	try {
    		lastStatus = manager.unregisterCertificate(alias, auth);
    		return (lastStatus.getCode() == 0);
    	} catch (Exception e){
    		lastException = e;
    	}
    	return false;
	}

	public SliceMng getSlice(SliceID sliceId) {
    	clearLast();
		try {
			ResultSliceMng tmp = manager.getSlice(sliceId, auth);
			lastStatus = tmp.getStatus();
			return getFirst(tmp.getResult());
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public boolean removeSlice(SliceID sliceId) {
    	clearLast();
    	try {
    		lastStatus = manager.removeSlice(sliceId, auth);
    		return (lastStatus.getCode() == 0);
    	} catch (Exception e){
    		lastException = e;
    	}
    	return false;
	}

	public List<ReservationMng> getReservations(SliceID sliceID) {
    	clearLast();
		try {
			ResultReservationMng tmp = manager.getReservations(sliceID, auth);
			lastStatus = tmp.getStatus();
			return tmp.getResult();
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public ReservationMng getReservation(ReservationID reservationID) {
    	clearLast();
		try {
			ResultReservationMng tmp = manager.getReservation(reservationID, auth);
			lastStatus = tmp.getStatus();
			return getFirst(tmp.getResult());
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public boolean closeReservation(ReservationID reservationID) {
    	clearLast();
		try {
			ResultMng tmp = manager.closeReservation(reservationID, auth);
			lastStatus = tmp;
			return tmp.getCode() == 0;
		}catch(Exception e){
			lastException = e;
		}
		return false;
	}

	public boolean removeReservation(ReservationID reservationID) {
    	clearLast();
		try {
			ResultMng tmp = manager.removeReservation(reservationID, auth);
			lastStatus = tmp;
			return tmp.getCode() == 0;
		}catch(Exception e){
			lastException = e;
		}
		return false;
	}

	public SliceID addSlice(SliceMng slice) {
    	clearLast();
		try {
			ResultStringMng tmp = manager.addSlice(slice, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0 && tmp.getResult() != null) {
				slice.setSliceID(tmp.getResult());
				return new SliceID(tmp.getResult());
			}
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public boolean updateSlice(SliceMng slice) {
    	clearLast();
		try {
			ResultMng tmp = manager.updateSlice(slice, auth);
			lastStatus = tmp;
			return tmp.getCode() == 0;
		}catch(Exception e){
			lastException = e;
		}
		return false;
	}


	public ID createEventSubscription() {
		clearLast();
		try {
			ResultStringMng tmp = manager.createEventSubscription(auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0 && tmp.getResult() != null) {
				return new ID(tmp.getResult());
			}
		} catch (Exception e){
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
			lastStatus = manager.deleteEventSubscription(subscriptionID, auth);
			return lastStatus.getCode() == 0;
		}catch (Exception e){
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
			ResultEventMng tmp = manager.drainEvents(subscriptionID, timeout, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0){
				return tmp.getResult();
			}
		}catch (Exception e){
			lastException = e;
		}
		return null;
	}
	
	public IOrcaActor clone() {
		return new LocalActor(manager, auth);
	}

	public List<ReservationMng> getReservations(SliceID sliceID, int state) {
		clearLast();
		try {
			ResultReservationMng r = ((ActorManagementObject)manager).getReservations(sliceID, state, auth);
			lastStatus = r.getStatus();
			if (r.getStatus().getCode() == 0){
				return r.getResult();
			}
		} catch (Exception e){
			lastException = e;
		}
		return null;
	}

	public boolean updateReservation(ReservationMng reservation) {
		clearLast();
		try {
			ResultMng r = ((ActorManagementObject)manager).updateReservation(reservation, auth);
			lastStatus = r;
			return (lastStatus.getCode() == 0);
		} catch (Exception e){
			lastException = e;
		}
		return false;
	}

	public boolean closeReservations(SliceID sliceID) {
		clearLast();
		try {
			ResultMng r = ((ActorManagementObject)manager).closeSliceReservations(sliceID, auth);
			lastStatus = r;
			return (lastStatus.getCode() == 0);
		} catch (Exception e){
			lastException = e;
		}
		return false;
	}

	public ReservationStateMng getReservationState(ReservationID reservationID) {
		clearLast();
		try {
			ResultReservationStateMng tmp = manager.getReservationState(reservationID, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0){
				return getFirst(tmp.getResult());
			}
		}catch (Exception e){
			lastException = e;
		}
		return null;
	}

	public List<ReservationStateMng> getReservationState(List<ReservationID> reservations) {
		clearLast();
		try {
			ResultReservationStateMng tmp = manager.getReservationState(reservations, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0){
				return tmp.getResult();
			}
		}catch (Exception e){
			lastException = e;
		}
		return null;
	}
}