package net.exogeni.orca.manage.internal;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import net.exogeni.orca.manage.beans.ProxyMng;
import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.manage.beans.ResultMng;
import net.exogeni.orca.manage.beans.ResultPoolInfoMng;
import net.exogeni.orca.manage.beans.ResultProxyMng;
import net.exogeni.orca.manage.beans.ResultReservationMng;
import net.exogeni.orca.manage.beans.ResultStringMng;
import net.exogeni.orca.manage.beans.ResultStringsMng;
import net.exogeni.orca.manage.beans.TicketReservationMng;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.ResourceType;

public interface IClientActorManagementObject {
	public ResultStringMng addReservation(TicketReservationMng reservation, AuthToken caller);
	public ResultStringsMng addReservations(List<TicketReservationMng> reservations, AuthToken caller);
	public ResultMng extendReservation(ReservationID reservation, 
			   Date newEndTime, 
			   int newUnits,
			   ResourceType newResourceType,
			   Properties requestProperties, 
			   Properties configProperties, 
			   AuthToken caller);

	public ResultMng demandReservation(ReservationMng reservation, AuthToken caller);
	public ResultMng demandReservation(ReservationID reservation, AuthToken caller);
	
	public ResultProxyMng getBrokers(AuthToken caller);
	public ResultProxyMng getBroker(ID brokerID, AuthToken caller);
	public ResultMng addBroker(ProxyMng brokerProxy, AuthToken caller);

	public ResultReservationMng claimResources(ID broker, SliceID sliceID, ReservationID reservationID, AuthToken caller);
	public ResultReservationMng claimResources(ID broker, ReservationID reservationID, AuthToken caller);

	public ResultPoolInfoMng getPoolInfo(ID broker, AuthToken caller);
}
