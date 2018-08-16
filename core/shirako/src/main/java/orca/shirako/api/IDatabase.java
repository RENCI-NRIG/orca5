/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.api;

import java.util.List;
import java.util.Properties;
import java.util.Vector;

import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.plugins.config.ConfigurationMapping;
import orca.util.Initializable;
import orca.util.ResourceType;
import orca.util.persistence.Recoverable;
import orca.util.persistence.Persistable;

import org.apache.log4j.Logger;


/**
 * <code>IDatabase</code> is the base database layer interface. It specifies
 * methods for managing slices, reservations, broker proxies, and configuration
 * mapping files.
 */
public interface IDatabase extends Initializable, Persistable, Recoverable
{
    /**
     * Performs initialization actions as a result of the actor being
     * added to the container.
     *
     * @throws Exception in case of error
     */
    public void actorAdded() throws Exception;

    /**
     * Adds a new broker proxy record.
     *
     * @param broker broker proxy
     *
     * @throws Exception in case of error
     */
    public void addBroker(IBrokerProxy broker) throws Exception;

    /**
     * Adds a new configuration mapping record.
     *
     * @param key key for the record
     * @param map mapping object
     *
     * @throws Exception in case of error
     */
    public void addConfigurationMapping(String key, ConfigurationMapping map)
                                 throws Exception;

    /**
     * Adds a new record to the database representing this reservation
     * object.
     *
     * @param reservation reservation
     *
     * @throws Exception in case of error
     */
    public void addReservation(IReservation reservation) throws Exception;

    /**
     * Adds a new record to the database representing this slice
     * object.
     *
     * @param slice Slice object
     *
     * @throws Exception in case of error
     */
    public void addSlice(ISlice slice) throws Exception;

    /**
     * Retrieves all reservations for which this actor acts as a
     * broker.
     *
     * @return vector of properties
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getBrokerReservations() throws Exception;

    /**
     * Retrieves all reservations for which this actor acts as a site.
     *
     * @return vector of properties
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getAuthorityReservations() throws Exception;

    /**
     * Retrieves all broker proxies.
     *
     * @return vector of properties
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getBrokers() throws Exception;

    /**
     * Retrieves all reservations that represent clients of this actor.
     * For a broker, should return only agent reservations. For a site, should
     * return both agent and authority reservations.
     *
     * @return vector of properties
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getClientReservations() throws Exception;

    public Vector<Properties> getClientReservations(SliceID sliceID) throws Exception;
    
    /**
     * Retrieves all client slice records.
     *
     * @return a vector containing one or more properties lists representing
     *         serialized client slices
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getClientSlices() throws Exception;

    /**
     * Retrieves the specified configuration mapping record.
     *
     * @param key record key
     *
     * @return a vector containing a properties list
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getConfigurationMapping(String key) throws Exception;

    /**
     * Retrieves all configuration mapping records.
     *
     * @return a vector containing one or more properties lists representing
     *         configuration mapping records
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getConfigurationMappings() throws Exception;

    /**
     * Retrieves all reservations representing resources held by this
     * actor Broker/service manager.
     *
     * @return vector of properties
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getHoldings() throws Exception;

    /**
     * Retrieves all reservations representing resources held by this
     * actor Broker/service manager.
     *
     * @return vector of properties
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getHoldings(SliceID sliceId) throws Exception;

    /**
     * Retrieves all inventory slice records.
     *
     * @return a vector containing one or more properties lists representing
     *         serialized inventory slices
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getInventorySlices() throws Exception;

    /**
     * Retrieves the specified reservation record.
     *
     * @param rid Reservation identifier
     *
     * @return vector of properties
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getReservation(ReservationID rid) throws Exception;

    /**
     * Retrieves all reservation records.
     *
     * @return vector of properties
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getReservations() throws Exception;
    
    /**
     * Retrieves all reservation records matching SQL pattern (not REGEX!)
     *
     * @return vector of properties
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getReservations(String sqlPat) throws Exception;
    
    
    /**
     * Retrieves all reservation records matching this state
     * @param state
     * @return vector of properties
     * @throws Exception in case of error
     */
    public Vector<Properties> getReservations(Integer state) throws Exception;
    
    /**
     * Retrieves the specified reservation records.
     * The order in the return vector is the same order as @rids
     * @param rids
     * @return vector of properties
     * @throws Exception in case of error
     */
    public Vector<Properties> getReservations(List<ReservationID> rids) throws Exception;
    /**
     * Retrieves all reservation records that belong to the specified
     * slice.
     *
     * @param sliceID slice id
     *
     * @return vector of properties
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getReservations(SliceID sliceID) throws Exception;

    /**
     * Retrieves all reservation records that belong to the specified
     * slice and match the specified string
     *
     * @param sliceID slice id
     *
     * @return vector of properties
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getReservations(SliceID sliceID, String str) throws Exception;

    /**
     * Retrieves all reservations with a specific state in this slice
     * @param sliceID slice id
     * @param state state
     * @return vector of properties
     * @throws Exception in case of error
     */
    public Vector<Properties> getReservations(SliceID sliceID, Integer state) throws Exception;
    
    /*
     * Peer registry functions: may need to be redone one day
     */

    /**
     * Retrieves the specified slice record.
     *
     * @param type resource type associated with the slice
     *
     * @return a vector containing a properties list
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getSlice(ResourceType type) throws Exception;

    /**
     * Retrieves the specified slice record.
     *
     * @param sliceID slice name
     *
     * @return a vector containing a properties list
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getSlice(SliceID sliceID) throws Exception;

    /**
     * Retrieves all slice records.
     *
     * @return a vector containing one or more properties lists representing
     *         serialized slices
     *
     * @throws Exception in case of error
     */
    public Vector<Properties> getSlices() throws Exception;

    /*
     * Configuration mappings
     */

    /**
     * Removes the specified broker proxy record.
     *
     * @param broker broker proxy
     *
     * @throws Exception in case of error
     */
    public void removeBroker(IBrokerProxy broker) throws Exception;

    /**
     * Removes the specified configuration mapping record.
     *
     * @param key key
     *
     * @throws Exception in case of error
     */
    public void removeConfigurationMapping(String key) throws Exception;

    /**
     * Removes the corresponding reservation object.
     *
     * @param rid reservation id
     *
     * @throws Exception in case of error
     */
    public void removeReservation(ReservationID rid) throws Exception;

    /**
     * Removes the corresponding database slice record.
     *
     * @param sliceID slice name
     *
     * @throws Exception in case of error
     */
    public void removeSlice(SliceID sliceID) throws Exception;

    /**
     * Sets the name of the actor this database belongs to.
     *
     * @param name actor name
     */
    public void setActorName(String name);

    /**
     * Sets the logger object to be used by the database layer
     * implementation.
     *
     * @param logger logger object
     */
    public void setLogger(Logger logger);

    /**
     * Indicates whether the database class should erase any previous
     * database state. If true, when the database class starts it will clean
     * any state in the database. Default: false.
     *
     * @param value value
     */
    public void setResetState(boolean value);

    /**
     * Updates the specified broker proxy record.
     *
     * @param broker broker proxy
     *
     * @throws Exception in case of error
     */
    public void updateBroker(IBrokerProxy broker) throws Exception;

    /**
     * Updates the specified configuration mapping record.
     *
     * @param key record key
     * @param map mapping object
     *
     * @throws Exception in case of error
     */
    public void updateConfigurationMapping(String key, ConfigurationMapping map)
                                    throws Exception;

    /**
     * Updates the corresponding reservation object.
     *
     * @param reservation reservation
     *
     * @throws Exception in case of error
     */
    public void updateReservation(IReservation reservation) throws Exception;

    /**
     * Updates the corresponding database slice record.
     *
     * @param slice slice object
     *
     * @throws Exception in case of error
     */
    public void updateSlice(ISlice slice) throws Exception;
}
