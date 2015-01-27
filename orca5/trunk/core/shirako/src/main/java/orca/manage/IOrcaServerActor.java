package orca.manage;

import java.security.cert.Certificate;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import orca.manage.beans.ClientMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.SliceMng;
import orca.security.AuthToken;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.util.ID;
import orca.util.ResourceType;

public interface IOrcaServerActor extends IOrcaActor {
	/**
	 * Returns all reservations for which this actor acts as a broker.
	 * @return
	 */
	public List<ReservationMng> getBrokerReservations();	
	/**
	 * Obtains all slices holding inventory, i.e., resources that can be
	 * delegated to other actors.
	 * 
	 * @return
	 */
	public List<SliceMng> getInventorySlices();

	/**
	 * Returns all resources held by this actor that can be used for delegations
	 * to client actors.
	 * 
	 * @return
	 */
	public List<ReservationMng> getInventoryReservations();

	/**
	 * Returns all resources in the specified slice held by this actor that can
	 * be used for delegations to client actors.
	 * 
	 * @return
	 */
	public List<ReservationMng> getInventoryReservations(SliceID sliceID);

	/**
	 * Obtains all slices that hold delegated resources to other actors.
	 * 
	 * @return
	 */
	public List<SliceMng> getClientSlices();

	/**
	 * Adds a new client slice.
	 * 
	 * @param slice
	 * @return
	 */
	public SliceID addClientSlice(SliceMng slice);

	/**
	 * Returns all registered clients of this server actor.
	 * 
	 * @return
	 */
	public List<ClientMng> getClients();

	/**
	 * Returns the specified client record.
	 * 
	 * @param guid
	 * @return
	 */
	public ClientMng getClient(ID guid);

	/**
	 * Returns the certificate of the specified client.
	 * 
	 * @param guid
	 * @return
	 */
	public Certificate getClientCertificate(ID guid);

	/**
	 * Registers a new client
	 * 
	 * @param client
	 * @param certificate
	 * @return
	 */
	public boolean registerClient(ClientMng client, Certificate certificate);

	/**
	 * Unregisters the specified client.
	 * 
	 * @param guid
	 * @return
	 */
	public boolean unregisterClient(ID guid);

	/**
	 * Obtains all client reservations.
	 * 
	 * @return
	 */
	public List<ReservationMng> getClientReservations();

	/**
	 * Obtains all client reservations in the specified slice.
	 * 
	 * @param slice
	 * @return
	 */
	public List<ReservationMng> getClientReservations(SliceID slice);

	/**
	 * Exports resources into the specified client slice from the specified
	 * resource pool using the given source reservation. <code>units</code> number
	 * of units are exported from <code>start</code> to <code>end</code>.
	 * All properties passed into ticketProperties will be part of the ticket and signed.
	 * All properties passed into resourceProperties will be attached as resource properties
	 * to the resource set (unsigned). 
	 * @param clientSliceID
	 * @param poolID
	 * @param start
	 * @param end
	 * @param units
	 * @param ticketProperties
	 * @param resourceProperties
	 * @param ticketId
	 * @return
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
