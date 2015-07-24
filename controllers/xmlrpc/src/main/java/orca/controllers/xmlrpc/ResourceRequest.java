package orca.controllers.xmlrpc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConverter;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.ReservationStateMng;
import orca.shirako.common.ReservationID;
import orca.util.ID;

public class ResourceRequest {
    public ID requestId;
    public List<ReservationMng> listInterDomainReservations;
    public boolean closed = false;

/*
 *  WRITEME: there are a number of ways to implement these methods:
 *  1. Make individual getReservation calls to get the most recent reservation state.
 *  2. Add a lighter weight getReservationState that returns the reservation state only [this is good anyway]
 *  3. Add 
 *  	boolean isActive(List<ReservationID>)
 *  	boolean isTerminal(List<ReseravtionID>)
 *  	boolean	isClosed(List<ReservationID>)
 *  	boolean hasAtLeastOneTerminatl(List<ReservationID>)
 *  	boolean hasAtLeastOneFailed(List<ReservationID>)
 *  	
 *  	List<ReservationStateMng> getState(List<ReservationID>)
 */

    
//
//    public boolean isActive() {
//
//        boolean isActive = true; // initialize true since this is an &&
//        // operation
//
//        Iterator<IServiceManagerReservation> it = listInterDomainReservations.iterator();
//        while (it.hasNext()) {
//            IServiceManagerReservation currReservation = (IServiceManagerReservation) it.next();
//            isActive = (isActive && currReservation.isActive());
//        }
//
//        return isActive;
//
//    }
//
//    public boolean isTerminal() {
//
//        boolean isTerminal = true; // initialize true since this is an &&
//        // operation
//
//        Iterator<IServiceManagerReservation> it = listInterDomainReservations.iterator();
//        while (it.hasNext()) {
//            IServiceManagerReservation currReservation = (IServiceManagerReservation) it.next();
//            isTerminal = (isTerminal && currReservation.isTerminal());
//        }
//
//        return isTerminal;
//
//    }
//
    public boolean isClosed(IOrcaServiceManager sm) {
    	List<ReservationID> rids = new ArrayList<ReservationID>();
    	for (ReservationMng r : listInterDomainReservations){
    		if (r.getReservationID() != null){
    			rids.add(new ReservationID(r.getReservationID()));
    		}
    	}
    	
    	List<ReservationStateMng> states = sm.getReservationState(rids);
    	if (states == null){
    		return false;
    	}
    	
    	return OrcaConverter.areClosed(states);
    }

//
//    public boolean hasAtLeastOneTerminal() {
//
//        boolean hasAtLeastOneTerminal = false; // initialize false since
//        // this is an || operation
//
//        Iterator<IServiceManagerReservation> it = listInterDomainReservations.iterator();
//        while (it.hasNext()) {
//            IServiceManagerReservation currReservation = (IServiceManagerReservation) it.next();
//            hasAtLeastOneTerminal = (hasAtLeastOneTerminal || currReservation.isTerminal());
//        }
//
//        return hasAtLeastOneTerminal;
//
//    }
//
//    public boolean hasAtLeastOneFailed() {
//
//        boolean hasAtLeastOneFailed = false; // initialize false since this
//        // is an || operation
//
//        Iterator<IServiceManagerReservation> it = listInterDomainReservations.iterator();
//        while (it.hasNext()) {
//            IServiceManagerReservation currReservation = (IServiceManagerReservation) it.next();
//            hasAtLeastOneFailed = (hasAtLeastOneFailed || currReservation.isFailed());
//        }
//
//        return hasAtLeastOneFailed;
//
//    }
}
