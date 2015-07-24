package orca.manage.internal.api;

import orca.manage.beans.ReservationMng;
import orca.manage.beans.ResultMng;
import orca.security.AuthToken;
import orca.shirako.api.IActor;

public interface IActorManagementObject extends IManagementObject {
	public void setActor(IActor actor);
	public ResultMng updateReservation(ReservationMng r, AuthToken caller);
}