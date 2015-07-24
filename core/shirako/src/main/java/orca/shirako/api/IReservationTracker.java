package orca.shirako.api;

import orca.shirako.common.ReservationID;

public interface IReservationTracker {
	public void registerCallback(IReservationCallback cb);
	public void unregisterCallback(IReservationCallback cb);
	public void registerCallback(ReservationID rid, IReservationCallback cb);
	public void unregisterCallback(ReservationID rid,IReservationCallback cb);
	
	public void awaitTicketed(ReservationID rid);
}