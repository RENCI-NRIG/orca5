package orca.manage;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import orca.manage.beans.PoolInfoMng;
import orca.manage.beans.ProxyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.TicketReservationMng;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.util.ID;
import orca.util.ResourceType;

public interface IOrcaClientActor extends IOrcaComponent{
    /**
     * Adds the reservation to the actor's state and returns the assigned reservation ID.
     * The reservation must refer to a valid slice.
     * The reservation ID is also attached to the passed in reservation object.
     * @param reservation
     * @return null on failure, assigned reservation ID otherwise
     */
    public ReservationID addReservation(TicketReservationMng reservation);
    /**
     * Adds all reservations to the actor's state and returns the assigned reservation ID.
     * Each reservation must refer to a valid slice.
     * The reservation ID is also attached to the passed in reservation object.
     * The operation is atomic: all of the reservations are added or none of them is added.
     * @param reservation
     * @return null on failure, list of assigned ReservationIDs on success.
     */
    public List<ReservationID> addReservations(List<TicketReservationMng> reservation);   
    /**
     * Demands the specified reservation.
     * A reservation can be demanded only if has been added and it is in the Nascent state.
     * @param reservationID
     * @return
     */
    public boolean demand(ReservationID reservationID);
    /**
     * Updates the reservation and issues a demand for it.
     * A reservation can be demanded only if has been added and it is in the Nascent state.
     * The reservation must refer to a valid slice. It can also indicate 
     * redeem predecessors.
     * See {@link IOrcaActor#updateReservation(ReservationMng)}
     * @param reservation
     * @return
     */
	public boolean demand(ReservationMng reservation);
    /**
     * Retuns all brokers known to the actor.
     * @return
     */
	public List<ProxyMng> getBrokers();
	/**
	 * Returns the broker with the specified ID.
	 * @param broker
	 * @return
	 */
	public ProxyMng getBroker(ID broker);
	/**
	 * Adds a new broker.
	 * @param broker
	 * @return
	 */
	public boolean addBroker(ProxyMng broker);
	/**
	 * Obtains the resources available at the specified broker
	 * @param broker
	 * @return
	 */
	public List<PoolInfoMng> getPoolInfo(ID broker);
	/**
	 * Claims resources exported by the specified broker
	 * @param brokerGuid
	 * @param sliceID
	 * @param reservationId
	 * @return
	 */
	public ReservationMng claimResources(ID brokerGuid, SliceID sliceID, ReservationID reservationId);
	/**
	 * Claims resources exported by the specified broker
	 * @param brokerGuid
	 * @param reservationID
	 * @return
	 */
	public ReservationMng claimResources(ID brokerGuid, ReservationID reservationID);

    public boolean extendReservation(ReservationID reservation, 
    								 Date endTime,
    								 int newUnits,
    								 ResourceType newResourceType,
    								 Properties requestProperties, 
    								 Properties configProperties);
    
	public boolean extendReservation(ReservationID reservation, Date newEndTime);
	public boolean extendReservation(ReservationID reservation, Date newEndTime, Properties requestProperties);
	public boolean extendReservation(ReservationID reservation, Date newEndTime, Properties requestProperties, Properties configProperties);
}