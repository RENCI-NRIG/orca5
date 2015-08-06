package orca.controllers.xmlrpc.statuswatch;

import orca.shirako.common.ReservationID;

public class ReservationIDWithModifyIndex {
	protected final ReservationID rid;
	protected final int modifyIndex;
	
	public ReservationIDWithModifyIndex(ReservationID r, int i) {
		rid = r; modifyIndex = i;
	}
	
	public ReservationID getReservationID() {
		return rid;
	}
	
	public int getModifyIndex() {
		return modifyIndex;
	}
}
