package net.exogeni.orca.shirako.kernel;

import java.util.Properties;

import net.exogeni.orca.shirako.api.IEvent;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.shirako.util.ReservationState;
import net.exogeni.orca.util.ID;

public class ReservationStateTransitionEvent implements IEvent{
	private final ID aid;
	private final ReservationID rid;
	private final SliceID sid;
	private final ReservationState state;
	private final IReservation reservation;
	
	public ReservationStateTransitionEvent(IReservation reservation, ReservationState state) {
		aid = reservation.getActor().getGuid();
		rid = reservation.getReservationID();
		sid = reservation.getSliceID();
		this.state = state;
		this.reservation = reservation;
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
	
	public ReservationState getState() {
		return state;
	}
	
	public IReservation getReservation() {
		return reservation;
	}
	
	public Properties getProperties() {
		return null;
	}
}
