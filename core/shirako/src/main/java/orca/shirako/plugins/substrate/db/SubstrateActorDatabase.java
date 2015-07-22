package orca.shirako.plugins.substrate.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.Vector;

import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.common.UnitID;
import orca.shirako.core.Unit;
import orca.shirako.plugins.db.ServerActorDatabase;
import orca.shirako.plugins.substrate.ISubstrateDatabase;
import orca.util.OrcaException;
import orca.util.persistence.PersistenceUtils;

public class SubstrateActorDatabase extends ServerActorDatabase implements ISubstrateDatabase {
    public static final String DefaultMapUrl = "orca/shirako/plugins/substrate/db/map.mysql.substrate.xml";
    public static final String TypeUnit = "unit";
    public static final String TypeInventory = "inventory";

    public SubstrateActorDatabase() {
        super(DefaultMapUrl);
    }

    public SubstrateActorDatabase(String mapFile) {
        super(mapFile);
    }

    protected String getParentUnit(UnitID parent, Connection connection) throws Exception {
        String parentID = null;
        String query = "select unt_id from Units where unt_uid = '" + parent.toString() + "' AND unt_act_id = '" + actorId + "';";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(query);

        if (rs.next()) {
            parentID = rs.getString("unt_id");
        }
        return parentID;
    }

    public void addUnit(Unit u) throws Exception {
        Connection connection = getConnection();
        try {
            // if the unit is already in the database, we must not throw an
            // error
            // and just move on: addUnit should be idempotent.
            if (getParentUnit(u.getID(), connection) != null) {
                logger.info("unit " + u.getID() + " is already present in the database");
                return;
            }
            Properties p = PersistenceUtils.save(u);
            Properties set = mapper.javaToMysql(TypeUnit, p);
            String sliceId = "";
            if (u.getSliceID() != null) {
                sliceId = u.getSliceID().toString();
            }
            String parentId = null;
            if (u.getParentID() != null) {
                parentId = getParentUnit(u.getParentID(), connection);
            }
            String resId = "";
            if (u.getReservationID() != null) {
                resId = u.getReservationID().toString();
            }
            StringBuffer buffer = new StringBuffer("insert into Units set ");
            buffer.append(constructQueryPartial(set));
            buffer.append(", unt_act_id='" + actorId + "', unt_slc_id=(Select slc_id from Slices where slc_act_id='" + actorId + "' AND slc_guid='" + sliceId + "'), unt_rsv_id=(Select rsv_id from Reservations where rsv_resid='" + resId + "' AND rsv_slc_id IN (Select slc_id from Slices where slc_act_id ='" + actorId + "' AND slc_guid='" + sliceId + "') )");
            if (parentId != null) {
                buffer.append(", unt_unt_id=" + parentId + "'");
            }
            buffer.append(";");

            Statement stmt = connection.createStatement();
            String query = buffer.toString();
            //logger.debug("addUnit: " + query);
            stmt.executeUpdate(query);
        } finally {
            returnConnection(connection);
        }
    }

    public Vector<Properties> getUnit(UnitID uid) throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Units where unt_uid='" + uid + "' AND unt_act_id= '" + actorId + "';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeUnit);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getUnits(ReservationID rid) throws OrcaException {
        Vector<Properties> result;
        try {
	    	Connection connection = getConnection();
	
	        try {
	            String query = "select * from Units where unt_rsv_id= (Select rsv_id from Reservations where rsv_resid='" + rid + "' AND rsv_slc_id IN (Select slc_id from Slices where slc_act_id ='" + actorId + "') )";
	            Statement s = connection.createStatement();
	            ResultSet rs = s.executeQuery(query);
	            result = createSearchResultsTyped(rs, TypeUnit);
	        } finally {
	            returnConnection(connection);
	        }
        } catch (OrcaException e) {
        	throw e;
        } catch (Exception e) {
        	throw new OrcaException("Database error", e);
        }
        return result;
    }

    public void removeUnit(UnitID uid) throws Exception {
        Connection connection = getConnection();

        try {
            String sqlquery = "delete from Units where unt_act_id='" + actorId + "' AND unt_uid='" + uid + "'";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery);
        } finally {
            returnConnection(connection);
        }
    }

    public void updateUnit(Unit u) throws Exception {
        // FIXME: check the dirty flag before performing the update. The problem
        // is that the dirty flag is not properly set when the unit is updated.
        // So for now update always.
        Properties p = PersistenceUtils.save(u);
        Properties set = mapper.javaToMysql(TypeUnit, p);
        //logger.debug("updateUnit(): " + actorId + " : unit properties = " + set);
        Connection connection = getConnection();

        try {
            StringBuffer sqlquery = new StringBuffer("update Units set ");
            sqlquery.append(constructQueryPartial(set));
            sqlquery.append("where unt_act_id='" + actorId + "' and unt_uid= '" + u.getID() + "';");
            String sql = sqlquery.toString();
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sql);
        } finally {
            returnConnection(connection);
        }
    }
    
 
    public Vector<Properties> getInventory() throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select A.* from Inventory AS A, InventoryActors AS B WHERE A.inv_id = B.ina_inv_id AND B.ina_act_id='" + actorId + "';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeInventory);
        } finally {
            returnConnection(connection);
        }
        return result;
    }
    
    public Vector<Properties> getInventory(SliceID sliceID) throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select A.* from Inventory AS A, InventorySlices AS B WHERE A.inv_id = B.ins_inv_id AND B.ins_slc_id=(Select slc_id from Slices where slc_guid='" + sliceID + "' AND slc_act_id='" + actorId + "');";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeInventory);
        } finally {
            returnConnection(connection);
        }
        return result;
    }
    
    public void transfer(UnitID unit, SliceID sliceID) throws Exception {
        Connection connection = getConnection();
        try {
            String sql = "Insert into InventorySlices set ins_inv_id=(select inv_id from Inventory where inv_name='" + unit.toString() + 
            "'), ins_slc_id=(select slc_id from Slices where slc_act_id='" + actorId + "' AND slc_guid='" + sliceID + "');";                
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sql);
        } finally {
            returnConnection(connection);
        }
    }
    
    
    public void untransfer(UnitID unit) throws Exception {
        Connection connection = getConnection();
        try {
            String sql = "DELETE FROM InventorySlices WHERE ins_inv_id=(select inv_id from Inventory where inv_name='" + unit.toString() + 
            "') AND ins_slc_id IN (select slc_id from Slices where slc_act_id='" + actorId + "');";                
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sql);
        } finally {
            returnConnection(connection);
        }
    }

}
