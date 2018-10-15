package net.exogeni.orca.manage;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import net.exogeni.orca.manage.beans.PoolInfoMng;
import net.exogeni.orca.manage.beans.ProxyMng;
import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.manage.beans.TicketReservationMng;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.ResourceType;

public interface IOrcaClientActor extends IOrcaComponent{
    /**
     * Adds the reservation to the actor's state and returns the assigned reservation ID.
     * The reservation must refer to a valid slice.
     * The reservation ID is also attached to the passed in reservation object.
     * @param reservation reservation
     * @return null on failure, assigned reservation ID otherwise
     */
    public ReservationID addReservation(TicketReservationMng reservation);
    /**
     * Adds all reservations to the actor's state and returns the assigned reservation ID.
     * Each reservation must refer to a valid slice.
     * The reservation ID is also attached to the passed in reservation object.
     * The operation is atomic: all of the reservations are added or none of them is added.
     * @param reservation reservation
     * @return null on failure, list of assigned ReservationIDs on success.
     */
    public List<ReservationID> addReservations(List<TicketReservationMng> reservation);   
    /**
     * Demands the specified reservation.
     * A reservation can be demanded only if has been added and it is in the Nascent state.
     * @param reservationID reservation id
     * @return true for sucess; false otherwise
     */
    public boolean demand(ReservationID reservationID);
    /**
     * Updates the reservation and issues a demand for it.
     * A reservation can be demanded only if has been added and it is in the Nascent state.
     * The reservation must refer to a valid slice. It can also indicate 
     * redeem predecessors.
     * See {@link IOrcaActor#updateReservation(ReservationMng)}
     * @param reservation reservation
     * @return true for sucess; false otherwise
     */
	public boolean demand(ReservationMng reservation);
    /**
     * Retuns all brokers known to the actor.
     * @return list of all brokers
     */
	public List<ProxyMng> getBrokers();
	/**
	 * Returns the broker with the specified ID.
	 * @param broker broker id
	 * @return returns specified broker
	 */
	public ProxyMng getBroker(ID broker);
	/**
	 * Adds a new broker.
	 * @param broker broker
	 * @return true for sucess; false otherwise
	 */
	public boolean addBroker(ProxyMng broker);
	/**
	 * Obtains the resources available at the specified broker
	 * @param broker broker
	 * @return list of pool info
	 */
	public List<PoolInfoMng> getPoolInfo(ID broker);
	/**
	 * Claims resources exported by the specified broker
	 * @param brokerGuid broker guid
	 * @param sliceID slice id
	 * @param reservationId reservation id
	 * @return reservation
	 */
	public ReservationMng claimResources(ID brokerGuid, SliceID sliceID, ReservationID reservationId);
	/**
	 * Claims resources exported by the specified broker
	 * @param brokerGuid broker guid
	 * @param reservationID reservation id
	 * @return reservation
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
