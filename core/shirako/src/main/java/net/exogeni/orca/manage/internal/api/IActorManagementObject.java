package net.exogeni.orca.manage.internal.api;

import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.manage.beans.ResultMng;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IActor;

public interface IActorManagementObject extends IManagementObject {
	public void setActor(IActor actor);
	public ResultMng updateReservation(ReservationMng r, AuthToken caller);
}
