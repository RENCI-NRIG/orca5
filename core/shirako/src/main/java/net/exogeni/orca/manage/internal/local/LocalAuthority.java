package net.exogeni.orca.manage.internal.local;

import java.util.List;

import net.exogeni.orca.manage.IOrcaActor;
import net.exogeni.orca.manage.IOrcaAuthority;
import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.manage.beans.ResultMng;
import net.exogeni.orca.manage.beans.ResultReservationMng;
import net.exogeni.orca.manage.beans.ResultUnitMng;
import net.exogeni.orca.manage.beans.UnitMng;
import net.exogeni.orca.manage.internal.AuthorityManagementObject;
import net.exogeni.orca.manage.internal.ManagementObject;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.shirako.common.UnitID;

public class LocalAuthority extends LocalServerActor implements IOrcaAuthority {
	protected AuthorityManagementObject manager;
	public LocalAuthority(ManagementObject manager, AuthToken auth) {
		super(manager, auth);
		if (!(manager instanceof AuthorityManagementObject)) {
			throw new RuntimeException("Invalid manager object. Required: "
					+ AuthorityManagementObject.class.getCanonicalName());
		}
		this.manager = (AuthorityManagementObject)manager;
	}

	public List<ReservationMng> getAuthorityReservations() {
		clearLast();
		try {
			ResultReservationMng tmp = manager.getAuthorityReservations(auth);
			lastStatus = tmp.getStatus();
			return tmp.getResult();
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public List<UnitMng> getUnits(ReservationID reservationID) {
		clearLast();
		try {
			ResultUnitMng tmp = manager.getReservationUnits(reservationID, auth);
			lastStatus = tmp.getStatus();
			return tmp.getResult();
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public List<UnitMng> getInventory() {
		clearLast();
		try {
			ResultUnitMng tmp = manager.getInventory(auth);
			lastStatus = tmp.getStatus();
			return tmp.getResult();
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public List<UnitMng> getInventory(SliceID sliceId) {
		clearLast();
		try {
			ResultUnitMng tmp = manager.getInventory(sliceId, auth);
			lastStatus = tmp.getStatus();
			return tmp.getResult();
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public UnitMng getUnit(UnitID unit) {
		clearLast();
		try {
			ResultUnitMng tmp = manager.getUnit(unit, auth);
			lastStatus = tmp.getStatus();
			if (lastStatus.getCode() == 0){
				return getFirst(tmp.getResult());
			}
		}catch(Exception e){
			lastException = e;
		}
		return null;
	}

	public boolean transferInventory(SliceID sliceId, UnitID unit) {
		clearLast();
		try {
			ResultMng tmp = manager.transferInventory(sliceId, unit, auth);
			lastStatus = tmp;
			return (lastStatus.getCode() == 0);
		}catch(Exception e){
			lastException = e;
		}
		return false;
	}

	public boolean untransferInventory(UnitID unit) {
		clearLast();
		try {
			ResultMng tmp = manager.untransferInventory(unit, auth);
			lastStatus = tmp;
			return (lastStatus.getCode() == 0);
		}catch(Exception e){
			lastException = e;
		}
		return false;
	}	
	
	public IOrcaActor clone() {
		return new LocalAuthority(manager, auth);
	}

}
