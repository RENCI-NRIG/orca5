package orca.shirako.kernel;

import java.util.Properties;

import orca.shirako.api.IEvent;
import orca.shirako.api.IReservation;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.util.ID;

public class ReservationPurgedEvent implements IEvent{
	private final ID aid;
	private final ReservationID rid;
	private final SliceID sid;
	
	public ReservationPurgedEvent(IReservation reservation) {
		aid = reservation.getActor().getGuid();
		rid = reservation.getReservationID();
		sid = reservation.getSliceID();
	}
	
	public ID getActorID() {
		return aid;
	}
	
	public ReservationID getReservationID(){
		return rid;
	}
	
	public SliceID getSliceID() {
		return sid;
	}
	
	public Properties getProperties() {
		return null;
	}
}