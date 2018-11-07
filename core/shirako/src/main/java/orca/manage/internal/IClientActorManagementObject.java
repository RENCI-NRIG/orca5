package orca.manage.internal;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import orca.manage.beans.ProxyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.ResultMng;
import orca.manage.beans.ResultPoolInfoMng;
import orca.manage.beans.ResultProxyMng;
import orca.manage.beans.ResultReservationMng;
import orca.manage.beans.ResultStringMng;
import orca.manage.beans.ResultStringsMng;
import orca.manage.beans.TicketReservationMng;
import orca.security.AuthToken;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.util.ID;
import orca.util.ResourceType;

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