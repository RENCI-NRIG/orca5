package orca.shirako.api;

import orca.shirako.util.ReservationState;

public interface IReservationCallback {
	public void eventOccurred(IReservation reservation, ReservationState state, boolean purged);
}