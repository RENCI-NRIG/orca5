package orca.controllers.xmlrpc.statuswatch;

import orca.shirako.common.ReservationID;

public class ReservationIDWithModifyIndex {
	protected final ReservationID rid;
	protected int modifyIndex;
	
	public ReservationIDWithModifyIndex(ReservationID r, int i) {
		rid = r; modifyIndex = i;
	}
	
	public ReservationID getReservationID() {
		return rid;
	}
	
	public void overrideModifyIndex(int i) {
		modifyIndex = i;
	}
	
	public int getModifyIndex() {
		return modifyIndex;
	}
	
	public String toString() {
		return rid.toHashString() + "[ " + modifyIndex + " ]";
	}
	
	@Override
    public boolean equals(Object other) {
		if (!(other instanceof ReservationIDWithModifyIndex))
			return false;
		
		ReservationIDWithModifyIndex ridwmi = (ReservationIDWithModifyIndex)other;
		
		if (ridwmi.rid.equals(rid) && (ridwmi.modifyIndex == modifyIndex))
			return true;
		
		return false;
	}
}
