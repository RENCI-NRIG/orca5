/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.plugins.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import orca.shirako.api.IActor;
import orca.shirako.api.IBrokerProxy;
import orca.shirako.api.IDatabase;
import orca.shirako.api.IReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.container.Globals;
import orca.shirako.kernel.ReservationDatabaseUpdateEvent;
import orca.shirako.kernel.SliceTypes;
import orca.shirako.plugins.config.ConfigurationMapping;
import orca.util.OrcaException;
import orca.util.ResourceType;
import orca.util.db.MySqlBase;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.PersistenceUtils;


public class ActorDatabase extends MySqlBase implements IDatabase
{
    private static final int MAX_FETCH_ROWS = 10;
	/**
     * Default Mapping file
     */
    public static final String DefaultConfigUrl = "orca/shirako/plugins/db/map.mysqldb.xml";
    public static final String PropertyReservationID = "shirakoReservationID";
    public static final String DefaultType = "mysql";

    /**
     * Cache the Actor Name and unique ID
     */
    @NotPersistent
    protected String actorName;
    @NotPersistent
    protected String actorId;
    @NotPersistent
    private boolean initialized = false;

    public ActorDatabase()
    {
        this(DefaultConfigUrl);
    }

    public ActorDatabase(String configFile)
    {
        super(configFile);
    }

    @Override
    public void initialize() throws OrcaException
    {
        if (!initialized) {
            super.initialize();

            if (actorName == null) {
                throw new OrcaException("Missing actor name");
            }

            initialized = true;
        }
    }

    public void actorAdded() throws Exception
    {
        this.actorId = getActorIdFromName(actorName);
        if (actorId == null) {
            throw new Exception("Actor record is not present in the database: " + actorName);
        }
    }

    public void revisit(IActor a, Properties p)
    {
    }

    protected String getActorIdFromName(String name, Connection connection)
                                 throws Exception
    {
        if (name == null) {
            return null;
        }

        String query = "select act_id from Actors where act_name = '" + name + "';";
        Statement s = connection.createStatement();
        ResultSet rs = s.executeQuery(query);

        if (rs.next()) {
            return rs.getString("act_id");
        } else {
            return null;
        }
    }

    protected String getActorIdFromName(String name) throws Exception
    {
        Connection connection = getConnection();
        String result;

        try {
            result = getActorIdFromName(name, connection);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    /**
     * Get the auto-generated key from the Slice Name
     * @param sliceName
     * @return key
     * @throws Exception
     */
    protected String getSliceIdFromGuid(SliceID guid) throws Exception
    {
        String SliceId;
        Connection connection = getConnection();

        try {
            SliceId = getSliceIdFromGuid(guid, connection);
        } finally {
            returnConnection(connection);
        }

        return SliceId;
    }

    protected String getSliceIdFromGuid(SliceID guid, Connection connection)
                                 throws Exception
    {
        String sliceId = null;
        String query = "select slc_id from Slices where slc_guid = '" + guid +
                       "' AND slc_act_id = '" + actorId + "';";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(query);

        if (rs.next()) {
            sliceId = rs.getString("slc_id");
        }

        return sliceId;
    }

    protected Vector<Properties> createSearchResults(ResultSet rs, String prefix)
                                              throws Exception
    {
        return createSearchResultsTyped(rs, DefaultType);
    }


    /*
     * Interface implementation.
     */
    public void addSlice(ISlice slice) throws Exception
    {
        Properties p = PersistenceUtils.save(slice);
        Properties set = mapper.javaToMysql(DefaultType, p);
        set.put("Slices.slc_act_id", actorId);

        Connection connection = getConnection();

        try {
            StringBuffer sqlquery = new StringBuffer("insert into Slices set ");
            sqlquery.append(constructQueryPartial(set));

            Statement stmt = connection.createStatement();
            String str = sqlquery.toString();
            stmt.executeUpdate(str);
        } finally {
            returnConnection(connection);
        }
    }

    public void updateSlice(ISlice slice) throws Exception
    {
        Properties p = PersistenceUtils.save(slice);
        Properties set = mapper.javaToMysql(DefaultType, p);
        Connection connection = getConnection();

        try {
            StringBuffer sqlquery = new StringBuffer("update Slices set ");
            sqlquery.append(constructQueryPartial(set));
            sqlquery.append(
                " where slc_act_id = '" + actorId + "' AND Slices.slc_guid='" + slice.getSliceID() +
                "';");

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery.toString());
        } finally {
            returnConnection(connection);
        }
    }

    public void removeSlice(SliceID sliceID) throws Exception
    {
        Connection connection = getConnection();

        try {
            String sqlquery = "delete from Slices where Slices.slc_guid = '" + sliceID +
                              "' AND Slices.slc_act_id='" + actorId + "';";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery);
        } finally {
            returnConnection(connection);
        }
    }

    /**
     * Get results of a query from a connection
     * @param query
     * @return
     */
    private Vector<Properties> getQueryResult(String query, Connection c, String nm) throws Exception {
    	
    	// make sure mysql streams the results
    	PreparedStatement ps = c.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    	ps.setFetchSize(MAX_FETCH_ROWS);
    	ResultSet rs = ps.executeQuery();
    	Vector<Properties> result = createSearchResults(rs, nm);
    	ps.close();
    	return result;
    }
    
    public Vector<Properties> getSlices() throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Slices where Slices.slc_act_id ='" + actorId + "';";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Slices");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getInventorySlices() throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Slices where slc_type='" + SliceTypes.InventorySlice +
                           "' AND slc_act_id='" + actorId + "';";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Slices");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getClientSlices() throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Slices where (slc_type='" + SliceTypes.ClientSlice +
                           "' OR slc_type='" + SliceTypes.BrokerClientSlice +
                           "') AND slc_act_id='" + actorId + "';";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Slices");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getSlice(SliceID sliceID) throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Slices where slc_guid = '" + sliceID +
                           "' AND slc_act_id='" + actorId + "';";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Slices");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getSlice(ResourceType type) throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Slices where slc_resource_type= '" + type +
                           "' AND slc_act_id='" + actorId + "';";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Slices");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public void addReservation(IReservation reservation) throws Exception
    {
        logger.info(
            "Adding reservation " + reservation.getReservationID().toHashString() + " to slice " +
            reservation.getSlice());

        Properties p = PersistenceUtils.save(reservation);
        reservation.clearDirty();

        if (logger.isTraceEnabled()){
            logger.trace("Reservation has persistence properties: " + Arrays.toString(p.entrySet().toArray()));
        }

        Properties set = mapper.javaToMysql(DefaultType, p);
        if (logger.isTraceEnabled()){
            logger.trace("Reservation has mapped properties: " + Arrays.toString(set.entrySet().toArray()));
        }

        Connection connection = getConnection();
        
        try {
            if (reservation.getActor() != null) {
                Globals.eventManager.dispatchEvent(new ReservationDatabaseUpdateEvent(reservation, true));
            }
            StringBuffer sqlquery = new StringBuffer("insert into Reservations set ");
            sqlquery.append(constructQueryPartial(set));

            // String sliceName = reservation.getSlice().getName();
            SliceID sliceID = reservation.getSlice().getSliceID();
            sqlquery.append(
                ", Reservations.rsv_slc_id = (select slc_id from Slices where slc_guid='" +
                sliceID + "' AND slc_act_id = '" + actorId + "');");

            Statement stmt = connection.createStatement();
            String str = sqlquery.toString();
            stmt.executeUpdate(str);
            if (reservation.getActor() != null) {
                Globals.eventManager.dispatchEvent(new ReservationDatabaseUpdateEvent(reservation, false));
            }
        } finally {
            returnConnection(connection);
        }
    }

    public void updateReservation(IReservation reservation) throws Exception
    {
        Properties p = null;
        if (reservation.isDirty()) {
            p = PersistenceUtils.save(reservation);
            reservation.clearDirty();
        }

        if (p != null) {
            logger.info(
                "Updating reservation " + reservation.getReservationID().toHashString() + " in slice " +
                reservation.getSlice());

            Properties set = mapper.javaToMysql(DefaultType, p);
            Connection connection = getConnection();

            try {
                Globals.eventManager.dispatchEvent(new ReservationDatabaseUpdateEvent(reservation, true));

                StringBuffer sqlquery = new StringBuffer("update Reservations set ");
                sqlquery.append(constructQueryPartial(set));

                SliceID sliceID = reservation.getSlice().getSliceID();
                String resId = reservation.getReservationID().toString();
                sqlquery.append(
                    "where Reservations.rsv_slc_id = (select slc_id from Slices where slc_guid='" +
                    sliceID + "' AND slc_act_id= '" + actorId +
                    "') AND Reservations.rsv_resid = '" + resId + "';");

                Statement stmt = connection.createStatement();
                stmt.executeUpdate(sqlquery.toString());
                Globals.eventManager.dispatchEvent(new ReservationDatabaseUpdateEvent(reservation, false));
            } finally {
                returnConnection(connection);
            }

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "No need to update reservation " + reservation.getReservationID().toHashString() +
                    " in slice " + reservation.getSlice());
            }
        }
    }

    public void removeReservation(ReservationID rid) throws Exception
    {
        Connection connection = getConnection();

        try {
            String sqlquery = "delete from Reservations where Reservations.rsv_resid = '" + rid +
                              "' AND Reservations.rsv_slc_id IN ( select slc_id from Slices where slc_act_id='" +
                              actorId + "');";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery);
        } finally {
            returnConnection(connection);
        }
    }

    public Vector<Properties> getReservations(SliceID sliceID) throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Reservations where rsv_slc_id = (select slc_id from Slices where slc_guid='" +
                           sliceID + "' and slc_act_id='" + actorId + "');";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Reservations");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    /**
     * Use SQL pattern to find matching reservtions. Notice, this is NOT a regex!
     */
    public Vector<Properties> getReservations(SliceID sliceID, String sqlPattern) throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
    	  String query = "select * from Reservations where rsv_slc_id = (select slc_id from Slices where slc_guid='" +
                  sliceID + "' and slc_act_id='" + actorId + "') AND (shirakoproperties LIKE '" + sqlPattern + "');";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Reservations");
        } finally {
            returnConnection(connection);
        }

        return result;
    }
    
    /**
     * Use state id to find matching reservations. 
     */
    public Vector<Properties> getReservations(SliceID sliceID, Integer state) throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
    	  String query = "select * from Reservations where rsv_slc_id = (select slc_id from Slices where slc_guid='" +
                  sliceID + "' and slc_act_id='" + actorId + "') AND (rsv_state = '" + state + "');";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Reservations");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getClientReservations() throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Reservations where rsv_slc_id IN (select slc_id from Slices where slc_act_id = '" +
                           actorId + "') AND (rsv_category = '" + IReservation.CategoryBroker +
                           "' OR rsv_category = '" + IReservation.CategoryAuthority + "');";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Reservations");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getClientReservations(SliceID sliceID) throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Reservations where rsv_slc_id IN (select slc_id from Slices where slc_guid='" +
                           sliceID + "' AND slc_act_id = '" +
                           actorId + "') AND (rsv_category = '" + IReservation.CategoryBroker +
                           "' OR rsv_category = '" + IReservation.CategoryAuthority + "');";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResults(rs, "Reservations");
        } finally {
            returnConnection(connection);
        }

        return result;
    }
    
    public Vector<Properties> getHoldings() throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Reservations where rsv_slc_id IN (select slc_id from Slices where slc_act_id = '" +
                           actorId + "') AND rsv_category = '" + IReservation.CategoryClient +
                           "';";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Reservations");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getHoldings(SliceID sliceID) throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Reservations where rsv_slc_id IN (select slc_id from Slices where slc_act_id = '" +
                           actorId + "' AND slc_guid='" + sliceID.toString() + "') AND rsv_category = '" + IReservation.CategoryClient +
                           "';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResults(rs, "Reservations");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getBrokerReservations() throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Reservations where rsv_slc_id IN (select slc_id from Slices where slc_act_id = '" +
                           actorId + "') AND rsv_category = '" + IReservation.CategoryBroker +
                           "';";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Reservations");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getAuthorityReservations() throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Reservations where rsv_slc_id IN (select slc_id from Slices where slc_act_id = '" +
                           actorId + "') AND rsv_category = '" + IReservation.CategoryAuthority +
                           "';";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Reservations");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getReservations() throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Reservations where rsv_slc_id IN (select slc_id from Slices where slc_act_id = '" +
                           actorId + "');";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Reservations");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    /**
     * Use SQL pattern to find matching reservtions. Notice, this is NOT a regex!
     */
    public Vector<Properties> getReservations(String sqlPattern) throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Reservations where (rsv_slc_id IN (select slc_id from Slices where slc_act_id = '" +
                           actorId + "')) AND (shirakoproperties LIKE '" + sqlPattern + "');";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Reservations");
        } finally {
            returnConnection(connection);
        }

        return result;
    }
    
    public Vector<Properties> getReservations(Integer state) throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Reservations where (rsv_slc_id IN (select slc_id from Slices where slc_act_id = '" +
                           actorId + "')) AND (rsv_state = '" + state + "');";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Reservations");
        } finally {
            returnConnection(connection);
        }

        return result;
    }
    
    public Vector<Properties> getReservation(ReservationID rid) throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Reservations where Reservations.rsv_resid = '" + rid +
                           "' AND Reservations.rsv_slc_id IN (select slc_id from Slices where slc_act_id='" +
                           actorId + "');";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Reservations");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getReservations(List<ReservationID> rids) throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        StringBuilder sb = new StringBuilder();
        for (ReservationID rid : rids){
        	if (sb.length() > 0){
        		sb.append(',');
        	}
        	sb.append('\'');
        	sb.append(rid.toString());
        	sb.append('\'');
        }
      
        String list = sb.toString();
        try {
            String query = "select * from Reservations where Reservations.rsv_resid IN (" + list +
                           ") AND Reservations.rsv_slc_id IN (select slc_id from Slices where slc_act_id='" +
                           actorId + "') ORDER BY FIELD (rsv_resid," + list + ");";
            result = getQueryResult(query, connection, "Reservations");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public void addBroker(IBrokerProxy broker) throws Exception
    {
        Properties p = PersistenceUtils.save(broker);
        Connection connection = getConnection();

        try {
            Properties set = mapper.javaToMysql(DefaultType, p);
            set.put("Proxies.prx_act_id", actorId);

            StringBuffer sqlquery = new StringBuffer("insert into Proxies set ");
            sqlquery.append(constructQueryPartial(set));

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery.toString());
        } finally {
            returnConnection(connection);
        }
    }

    public void removeBroker(IBrokerProxy broker) throws Exception
    {
        Connection connection = getConnection();

        try {
            String query = "delete from Proxies where prx_act_id = '" + actorId +
                           "' AND prx_name='" + broker.getName() + "';";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(query);
        } finally {
            returnConnection(connection);
        }
    }

    public void updateBroker(IBrokerProxy broker) throws Exception
    {
        Properties p = PersistenceUtils.save(broker);
        Properties set = mapper.javaToMysql(DefaultType, p);
        Connection connection = getConnection();

        try {
            StringBuffer sqlquery = new StringBuffer("update Proxies set ");
            sqlquery.append(constructQueryPartial(set));
            sqlquery.append(
                " where prx_act_id = '" + actorId + "' AND prx_name='" + broker.getName() + "';");

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery.toString());
        } finally {
            returnConnection(connection);
        }
    }

    public Vector<Properties> getBrokers() throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Proxies where prx_act_id = '" + actorId + "';";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "Proxies");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getConfigurationMappings() throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from ConfigMappings where cfgm_act_id = '" + actorId + "';";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "ConfigMappings");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getConfigurationMapping(String key) throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from ConfigMappings where cfgm_act_id = '" + actorId +
                           "' AND cfgm_type='" + key + "';";
            //Statement s = connection.createStatement();
            //ResultSet rs = s.executeQuery(query);
            result = getQueryResult(query, connection, "ConfigMappings");
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public void addConfigurationMapping(String key, ConfigurationMapping map)
                                 throws Exception
    {
        Properties p = PersistenceUtils.save(map);
        Connection connection = getConnection();

        try {
            Properties set = mapper.javaToMysql(DefaultType, p);
            set.put("ConfigMappings.cfgm_act_id", actorId);

            StringBuffer sqlquery = new StringBuffer("insert into ConfigMappings set ");
            sqlquery.append(constructQueryPartial(set));

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery.toString());
        } finally {
            returnConnection(connection);
        }
    }

    public void updateConfigurationMapping(String key, ConfigurationMapping map)
                                    throws Exception
    {
        Properties p = PersistenceUtils.save(map);
        Properties set = mapper.javaToMysql(DefaultType, p);
        Connection connection = getConnection();

        try {
            StringBuffer sqlquery = new StringBuffer("update ConfigMappings set ");
            sqlquery.append(constructQueryPartial(set));
            sqlquery.append(" where cfgm_act_id = '" + actorId + "' AND cfgm_type = '" + key +
                            "';");

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery.toString());
        } finally {
            returnConnection(connection);
        }
    }

    public void removeConfigurationMapping(String key) throws Exception
    {
        Connection connection = getConnection();

        try {
            String query = "delete from ConfigMappings where cfgm_act_id ='" + actorId +
                           "' AND cfgm_type='" + key + "';";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(query);
        } finally {
            returnConnection(connection);
        }
    }

    public void setActorName(String actorName)
    {
        this.actorName = actorName;
    }
}