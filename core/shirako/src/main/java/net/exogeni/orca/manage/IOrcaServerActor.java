package net.exogeni.orca.manage;

import java.security.cert.Certificate;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import net.exogeni.orca.manage.beans.ClientMng;
import net.exogeni.orca.manage.beans.ReservationMng;
import net.exogeni.orca.manage.beans.SliceMng;
import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.ResourceType;

public interface IOrcaServerActor extends IOrcaActor {
	/**
	 * Returns all reservations for which this actor acts as a broker.
	 * @return list of the reservations
	 */
	public List<ReservationMng> getBrokerReservations();	
	/**
	 * Obtains all slices holding inventory, i.e., resources that can be
	 * delegated to other actors.
	 * 
	 * @return list of slices
	 */
	public List<SliceMng> getInventorySlices();

	/**
	 * Returns all resources held by this actor that can be used for delegations
	 * to client actors.
	 * 
	 * @return list of reservations
	 */
	public List<ReservationMng> getInventoryReservations();

	/**
	 * Returns all resources in the specified slice held by this actor that can
	 * be used for delegations to client actors.
	 * 
	 * @param sliceID slice id
	 * @return list of reservations for specific slice
	 */
	public List<ReservationMng> getInventoryReservations(SliceID sliceID);

	/**
	 * Obtains all slices that hold delegated resources to other actors.
	 * 
	 * @return list of client slices
	 */
	public List<SliceMng> getClientSlices();

	/**
	 * Adds a new client slice.
	 * 
	 * @param slice slice to be added
	 * @return sliceid of the added slice
	 */
	public SliceID addClientSlice(SliceMng slice);

	/**
	 * Returns all registered clients of this server actor.
	 * 
	 * @return list of clients
	 */
	public List<ClientMng> getClients();

	/**
	 * Returns the specified client record.
	 * 
	 * @param guid client guid 
	 * @return specified client record
	 */
	public ClientMng getClient(ID guid);

	/**
	 * Returns the certificate of the specified client.
	 * 
	 * @param guid client guid
	 * @return specified client certificate
	 */
	public Certificate getClientCertificate(ID guid);

	/**
	 * Registers a new client
	 * 
	 * @param client client
	 * @param certificate certificate
	 * @return true for success; false otherwise
	 */
	public boolean registerClient(ClientMng client, Certificate certificate);

	/**
	 * Unregisters the specified client.
	 * 
	 * @param guid client guid
	 * @return true for success; false otherwise
	 */
	public boolean unregisterClient(ID guid);

	/**
	 * Obtains all client reservations.
	 * 
	 * @return list of client reservations
	 */
	public List<ReservationMng> getClientReservations();

	/**
	 * Obtains all client reservations in the specified slice.
	 * 
	 * @param slice slice id
	 * @return list of reservations
	 */
	public List<ReservationMng> getClientReservations(SliceID slice);

	/**
	 * Exports resources into the specified client slice from the specified
	 * resource pool using the given source reservation. <code>units</code> number
	 * of units are exported from <code>start</code> to <code>end</code>.
	 * All properties passed into ticketProperties will be part of the ticket and signed.
	 * All properties passed into resourceProperties will be attached as resource properties
	 * to the resource set (unsigned). 
	 * @param clientSliceID client slice id
	 * @param poolID pool slice id
	 * @param start start date
	 * @param end end date
	 * @param units units
	 * @param ticketProperties ticket properties
	 * @param resourceProperties resource properties
	 * @param ticketId ticket id
	 * @return returns the reservation id
	 */
	public ReservationID exportResources(SliceID clientSliceID, SliceID poolID,
			Date start, Date end, int units, Properties ticketProperties, Properties resourceProperties,
			ReservationID ticketId);

	public ReservationID exportResources(SliceID poolID,
			Date start, Date end, int units, Properties ticketProperties, Properties resourceProperties,
			ReservationID sourceTicketID, AuthToken clientToExportTo);

	public ReservationID exportResources(SliceID clientSliceID, ResourceType resourceType,
			Date start, Date end, int units, Properties ticketProperties, Properties resourceProperties,
			ReservationID ticketId);
	
	public ReservationID exportResources(ResourceType resourceType, 
			Date start, Date end, int units, Properties ticketProperties,
			Properties resourceProperties,
			ReservationID sourceTicketID, AuthToken clientToExportTo);
}
