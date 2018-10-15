package net.exogeni.orca.shirako.api;

import net.exogeni.orca.shirako.util.ReservationState;

public interface IReservationCallback {
	public void eventOccurred(IReservation reservation, ReservationState state, boolean purged);
}
