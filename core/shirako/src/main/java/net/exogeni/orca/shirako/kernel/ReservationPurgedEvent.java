package net.exogeni.orca.shirako.kernel;

import java.util.Properties;

import net.exogeni.orca.shirako.api.IEvent;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.util.ID;

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
