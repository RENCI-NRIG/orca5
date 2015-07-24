/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.container.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Vector;

import orca.extensions.PackageId;
import orca.extensions.PluginId;
import orca.extensions.internal.ExtensionPackage;
import orca.extensions.internal.Plugin;
import orca.manage.OrcaConstants;
import orca.manage.internal.ContainerManagementObject;
import orca.manage.internal.User;
import orca.manage.internal.api.IManagementObject;
import orca.shirako.api.IActor;
import orca.shirako.api.IActorIdentity;
import orca.shirako.common.UnitID;
import orca.shirako.container.Globals;
import orca.shirako.container.api.IOrcaContainerDatabase;
import orca.shirako.core.Actor;
import orca.shirako.core.Unit;
import orca.util.ID;
import orca.util.OrcaException;
import orca.util.db.MySqlBase;
import orca.util.persistence.PersistenceUtils;
import orca.util.persistence.Persistent;

public class OrcaContainerDatabase extends MySqlBase implements IOrcaContainerDatabase {   
    public static final String TypeInventory = "inventory";
    public static final String PropertyAdminFirst = "db.admin.first";
    public static final String PropertyAdminLast = "db.admin.last";
    public static final String PropertyAdminLogin = "db.admin.login";
    public static final String PropertyAdminPassword = "db.admin.password";
    public static final String DefaultCodManagementMapUrl = "orca/manage/container/db/map.container.mysql.xml";
    public static final String SiteName = "shirakoSiteName";

    /**
     * Admin name and login.
     */
    @Persistent(key=PropertyAdminFirst)
    protected String adminFirst;

    /**
     * Admin name and login.
     */
    @Persistent(key=PropertyAdminLast)
    protected String adminLast;

    /**
     * Admin name and login.
     */
    @Persistent(key=PropertyAdminLogin)
    protected String adminLogin;
    /**
     * Admin password.
     */
    @Persistent(key=PropertyAdminPassword)
    protected String adminPassword;

    public OrcaContainerDatabase() {
        super(DefaultCodManagementMapUrl);
    }

    public OrcaContainerDatabase(String mapFile) {
        super(mapFile);
    }

    @Override
    public void initialize() throws OrcaException {
        if (!initialized) {
            super.initialize();
            try {
	            if (resetState) {
	                addAdminUser();
	            }
	            initialized = true;
            } catch (OrcaException e) {
            	throw e;
            } catch (Exception e) {
            	throw new OrcaException("Cannot initialize", e);
            }
        }
    }

    public void revisit(IActorIdentity actor, Properties properties) throws Exception {
    }

    /**
     * Empty the database of all previous state
     */
    protected void resetDB(Connection connection) throws SQLException {
        super.resetDB(connection);

        Statement st = connection.createStatement();

        st.executeUpdate("DELETE FROM Clients");
        st.executeUpdate("DELETE FROM ConfigMappings");
        st.executeUpdate("DELETE FROM Miscellaneous");
        st.executeUpdate("DELETE FROM Plugins");
        st.executeUpdate("DELETE FROM Packages");
        st.executeUpdate("DELETE FROM Proxies");
        st.executeUpdate("DELETE FROM Reservations");
        st.executeUpdate("DELETE FROM Slices");
        st.executeUpdate("DELETE FROM Users");    
        st.executeUpdate("DELETE FROM ManagerObjects");
        st.executeUpdate("DELETE FROM Units");
        // note: do not delete Inventory!
        st.executeUpdate("DELETE from InventorySlices");
        st.executeUpdate("DELETE from InventoryActors");      
        st.executeUpdate("DELETE FROM Actors");
    }
        
    /**
     * {@inheritDoc}
     */
    public void addActor(IActor actor) throws Exception {
        Properties p = PersistenceUtils.save(actor);
                
        Properties set = mapper.javaToMysql(TypeDefault, p);
        Connection connection = getConnection();

        try {
            StringBuffer sqlquery = new StringBuffer("insert into Actors set ");
            sqlquery.append(constructQueryPartial(set));
            Statement stmt = connection.createStatement();
            String str = sqlquery.toString();
            stmt.executeUpdate(str);
        } finally {
            returnConnection(connection);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeActor(String actorName) throws Exception {
        Connection connection = getConnection();

        try {
            String sqlquery = "delete from Actors where act_name='" + actorName + "';";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery);
        } finally {
            returnConnection(connection);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeActorDatabase(String actorName) throws Exception {
        removeActor(actorName);
    }

    /**
     * {@inheritDoc}
     */
    public void updateActor(IActor actor) throws Exception {
        Properties p = PersistenceUtils.save(actor);
        Properties set = mapper.javaToMysql(TypeDefault, p);
        Connection connection = getConnection();

        try {
            StringBuffer sqlquery = new StringBuffer("update Actors set ");
            sqlquery.append(constructQueryPartial(set));
            sqlquery.append(" where act_name = '" + Actor.getName(p) + "';");
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery.toString());
        } finally {
            returnConnection(connection);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Vector<Properties> getActors() throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Actors;";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Vector<Properties> getActors(String actorName, int type) throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            if (actorName == null) {
                actorName = "";
            }

            String actor = actorName.replaceAll("\\*", "?");
            String query = "";

            if (type != OrcaConstants.ActorTypeAll) {
                query = "select * from Actors where Actors.act_type='" + type + "' AND act_name LIKE '%" + actor + "%';";
            } else {
                query = "select * from Actors where act_name LIKE '%" + actor + "%';";
            }

            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Vector<Properties> getActor(String actorName) throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Actors where act_name='" + actorName + "';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void addTime(Properties p) throws Exception {
        Properties set = mapper.javaToMysql(TypeDefault, p);
        Connection connection = getConnection();

        try {
            StringBuffer sqlquery = new StringBuffer("insert into Miscellaneous set ");
            sqlquery.append(constructQueryPartial(set));
            sqlquery.append(", Miscellaneous.msc_path='time'");
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery.toString());
        } finally {
            returnConnection(connection);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Vector<Properties> getTime() throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Miscellaneous where msc_path='time';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public void addContainerProperties(Properties p) throws Exception {
        Properties set = mapper.javaToMysql(TypeDefault, p);
        Connection connection = getConnection();

        try {
            StringBuffer sqlquery = new StringBuffer("insert into Miscellaneous set ");
            sqlquery.append(constructQueryPartial(set));
            sqlquery.append(", Miscellaneous.msc_path='container'");

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery.toString());
        } finally {
            returnConnection(connection);
        }
    }

    public void updateContainerProperties(Properties p) throws Exception {
        Properties set = mapper.javaToMysql(TypeDefault, p);
        Connection connection = getConnection();

        try {
            StringBuffer sqlquery = new StringBuffer("update Miscellaneous set ");
            sqlquery.append(constructQueryPartial(set));
            sqlquery.append(" where msc_path='container'");

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery.toString());
        } finally {
            returnConnection(connection);
        }
    }

    public Vector<Properties> getContainerProperties() throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Miscellaneous where msc_path='container';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    /**
     * Adds a record for the admin user, if necessary
     * @throws Exception
     */
    protected void addAdminUser() throws Exception {
        if (adminLogin != null) {
            User user = new User();
            user.setLogin(adminLogin);
            user.setFirst(adminFirst);
            user.setLast(adminLast);

            if (OrcaConstants.Roles != null) {
                String[] roles = new String[OrcaConstants.Roles.length];
                System.arraycopy(OrcaConstants.Roles, 0, roles, 0, roles.length);
                user.setRoles(roles);
            }

            addUser(user);
            adminPassword = ContainerManagementObject.hashPassword(adminPassword);
            setUserPassword(adminLogin, adminPassword);
        }
    }

    public Vector<Properties> getUsers() throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Users;";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getUser(String userName) throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Users where Users.usr_name='" + userName + "';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getUser(String userName, String password) throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Users where Users.usr_name='" + userName + "' AND Users.usr_password='" + password +"';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }
    
    public void addUser(User user) throws Exception {
        Connection connection = getConnection();
        Properties p = user.save();
        Properties set = mapper.javaToMysql(TypeDefault, p);

        try {
            StringBuffer sqlquery = new StringBuffer("insert into Users set ");
            sqlquery.append(constructQueryPartial(set));

            Statement stmt = connection.createStatement();
            String str = sqlquery.toString();
            stmt.executeUpdate(str);
        } finally {
            returnConnection(connection);
        }
    }

    public void setUserPassword(String login, String password) throws Exception {
        if (login == null) {
            throw new IllegalArgumentException("login cannot be null");
        }
        String thePass = password;
        if (thePass == null) {
            thePass = "";
        }

        Connection connection = getConnection();

        try {
            String sql = "Update Users Set usr_password=? WHERE usr_name=?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, thePass);
            pstmt.setString(2, login);
            pstmt.executeUpdate();
        } finally {
            returnConnection(connection);
        }
    }

    public void updateUser(User user) throws Exception {
        Properties p = user.save();
        Properties set = mapper.javaToMysql(TypeDefault, p);
        Connection connection = getConnection();

        try {
            StringBuffer sqlquery = new StringBuffer("update Users set ");
            sqlquery.append(constructQueryPartial(set));
            sqlquery.append(" where  Users.usr_name= '" + user.getLogin() + "';");

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery.toString());
        } finally {
            returnConnection(connection);
        }
    }

    public void removeUser(String user) throws Exception {
        Connection connection = getConnection();

        try {
        	String sqlquery = "delete from Users where Users.usr_name='" + user + "';";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery);
        } finally {
            connection.setAutoCommit(true);
            returnConnection(connection);
        }
    }

    public void addPackage(ExtensionPackage pack) throws Exception {
        Connection connection = getConnection();
        Properties p = pack.save();
        Properties set = mapper.javaToMysql(TypeDefault, p);

        try {
            StringBuffer sqlquery = new StringBuffer("insert into Packages set ");
            sqlquery.append(constructQueryPartial(set));

            Statement stmt = connection.createStatement();
            String str = sqlquery.toString();
            // System.out.println(str);
            stmt.executeUpdate(str);
        } finally {
            returnConnection(connection);
        }
    }

    public void removePackage(PackageId id) throws Exception {
        Connection connection = getConnection();

        try {
            String sqlquery = "delete from Packages where Packages.pkg_guid='" + id + "';";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery);
        } finally {
            returnConnection(connection);
        }
    }

    public Vector<Properties> getPackage(PackageId id) throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Packages where Packages.pkg_guid='" + id + "';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getPackages() throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Packages;";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    protected String getPackageKeyFromId(PackageId id, Connection connection) throws Exception {
        if (id == null) {
            return null;
        }

        String query = "select pkg_id from Packages where pkg_guid = '" + id + "';";
        Statement s = connection.createStatement();
        ResultSet rs = s.executeQuery(query);

        if (rs.next()) {
            return rs.getString("pkg_id");
        }

        return null;
    }

    protected String getPackageKeyFromId(PackageId id) throws Exception {
        String result;
        Connection connection = getConnection();

        try {
            result = getPackageKeyFromId(id, connection);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public void addPlugin(Plugin plg) throws Exception {
        Connection connection = getConnection();
        Properties p = plg.save();
        Properties set = mapper.javaToMysql(TypeDefault, p);
        set.put("Plugins.plg_pkg_id", getPackageKeyFromId(plg.getPackageId(), connection));

        try {
            StringBuffer sqlquery = new StringBuffer("insert into Plugins set ");
            sqlquery.append(constructQueryPartial(set));

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery.toString());
        } finally {
            returnConnection(connection);
        }
    }

    public void removePlugin(PackageId packageId, PluginId pluginId) throws Exception {
        Connection connection = getConnection();

        try {
            String sqlquery = "delete from Plugins where Plugins.plg_pkg_id=(select pkg_id from Packages where pkg_guid = '" + packageId + "') AND Plugins.plg_local_id='" + pluginId + "';";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery);
        } finally {
            returnConnection(connection);
        }
    }

    public void removePlugins(PackageId packageId) throws Exception {
        Connection connection = getConnection();

        try {
            String sqlquery = "delete from Plugins where Plugins.plg_pkg_id=(select pkg_id from Packages where pkg_guid='" + packageId + "');";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery);
        } finally {
            returnConnection(connection);
        }
    }

    public Vector<Properties> getPlugins(PackageId packageId, int type, int actorType) throws Exception {
        boolean where = false;
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            StringBuffer sb = new StringBuffer();
            sb.append("select * from Plugins");

            if (packageId != null) {
                sb.append(" where Plugins.plg_pkg_id=(select pkg_id from Packages where pkg_guid='");
                sb.append(packageId.toString());
                sb.append("')");
                where = true;
            }

            if (type != Plugin.TypeAll) {
                if (where) {
                    sb.append(" AND ");
                } else {
                    sb.append(" WHERE ");
                    where = true;
                }

                sb.append("Plugins.plg_type='");
                sb.append(type);
                sb.append("'");
            }

            if (actorType != OrcaConstants.ActorTypeAll) {
                if (where) {
                    sb.append(" AND ");
                } else {
                    sb.append(" WHERE ");
                    where = true;
                }

                sb.append("Plugins.plg_actor_type='");
                sb.append(actorType);
                sb.append("'");
            }

            String str = sb.toString();

            // System.out.println(str);
            // String query = "select * from Plugins where
            // Plugins.plg_pkg_id=(select pkg_id from Packages where pkg_guid =
            // '" + packageId + "') AND Plugins.plg_type='" + type + "' AND
            // Plugins.plg_actor_type='" + actorType + "';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(str);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getPlugin(PackageId packageId, PluginId pluginId) throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Plugins where Plugins.plg_pkg_id=(select pkg_id from Packages where pkg_guid = '" + packageId + "') AND Plugins.plg_local_id='" + pluginId + "';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public void addManagerObject(IManagementObject manager) throws Exception {
        Connection connection = getConnection();
        Properties p = manager.save();
        Properties set = mapper.javaToMysql(TypeDefault, p);

        try {
            StringBuffer sqlquery = new StringBuffer("insert into ManagerObjects set ");
            sqlquery.append(constructQueryPartial(set));

            if (manager.getActorName() != null) {
                sqlquery.append(", mo_act_id=(select act_id from Actors where act_name='");
                sqlquery.append(manager.getActorName());
                sqlquery.append("')");
            }

            Statement stmt = connection.createStatement();
            String str = sqlquery.toString();
            // System.out.println(str);
            stmt.executeUpdate(str);
        } finally {
            returnConnection(connection);
        }
    }

    public void removeManagerObject(ID id) throws Exception {
        Connection connection = getConnection();

        try {
            String sqlquery = "delete from ManagerObjects where mo_key='" + id + "'";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery);
        } finally {
            returnConnection(connection);
        }
    }

    public void removeManagerObjects(String actorName) throws Exception {
        Connection connection = getConnection();

        try {
            String sqlquery = "delete from ManagerObjects where mo_id in (select ManagerObjects.mo_id from ManagerObjects JOIN (Select * from Actors where act_name='" + actorName + "') AS A ON mo_act_id=act_id)";

            // System.out.println(sqlquery);
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery);
        } finally {
            returnConnection(connection);
        }
    }

    public Vector<Properties> getManagerObject(ID id) throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from ManagerObjects where mo_key='" + id + "'";

            // System.out.println(query);
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getManagerObjects() throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from ManagerObjects";

            // System.out.println(query);
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getManagerObjects(String actorName) throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String sqlquery = "select ManagerObjects.* from ManagerObjects JOIN (Select * from Actors where act_name='" + actorName + "') AS A ON mo_act_id=act_id";

            // System.out.println(sqlquery);
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(sqlquery);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public Vector<Properties> getManagerObjectsContainer() throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String sqlquery = "select ManagerObjects.* from ManagerObjects where mo_act_id IS NULL";

            // System.out.println(sqlquery);
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(sqlquery);
            result = createSearchResultsTyped(rs, TypeDefault);
        } finally {
            returnConnection(connection);
        }

        return result;
    }
    
    public void addInventory(Unit u) throws Exception {
        Connection connection = getConnection();
        try {
            Properties p = PersistenceUtils.save(u);
            Properties set = mapper.javaToMysql(TypeInventory, p);
            StringBuffer buffer = new StringBuffer("insert into Inventory set ");
            buffer.append(constructQueryPartial(set));
            buffer.append(";");

            Statement stmt = connection.createStatement();
            String query = buffer.toString();
            logger.debug("addInventory: " + query);
            stmt.executeUpdate(query);
        } finally {
            returnConnection(connection);
        }
    }
    
    public void removeInventory(UnitID uid) throws Exception {
        Connection connection = getConnection();

        try {
            String sqlquery = "delete from Inventory where inv_uid='" + uid + "'";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery);
        } finally {
            returnConnection(connection);
        }
    }

    public void updateInventory(Unit u) throws Exception {
        // FIXME: check the dirty flag before performing the update. The problem
        // is that the dirty flag is not properly set when the unit is updated.
        // So for now update always.
        Properties p = PersistenceUtils.save(u);
        Properties set = mapper.javaToMysql(TypeInventory, p);
        Connection connection = getConnection();

        try {
            StringBuffer sqlquery = new StringBuffer("update Inventory set ");
            sqlquery.append(constructQueryPartial(set));
            sqlquery.append("where inv_uid= '" + u.getID() + "';");
            String sql = sqlquery.toString();
            logger.debug("updateInventory: " + sql);
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sql);
        } finally {
            returnConnection(connection);
        }
    }
    
    public Properties getInventory(String name) throws Exception {
        Connection connection = getConnection();
        Properties result = null;

        try {
            String query = "select * from Inventory where inv_name='" + name + "';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            Vector<Properties> v = createSearchResultsTyped(rs, TypeInventory);
            if (v.size() > 0){
                result = v.get(0);
            }
        } finally {
            returnConnection(connection);
        }

        return result;
    }
    
    public Vector<Properties> getInventory() throws Exception {
        Connection connection = getConnection();
        Vector<Properties> result = null;

        try {
            String query = "select * from Inventory;";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, TypeInventory);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    public void transferInventory(String inventoryName, ID actorGuid) throws Exception {
        Connection connection = getConnection();
        try {
            String sql = "Insert into InventoryActors set ina_inv_id=(select inv_id from Inventory where inv_name='" + inventoryName + 
            "'), ina_act_id=(select act_id from Actors where act_guid='" + actorGuid + "');";
            	
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sql);
        } finally {
            returnConnection(connection);
        }
    }

    public void untransferInventory(String inventoryName, ID actorGuid) throws Exception {
        Connection connection = getConnection();
        try {
            String sql = "Delete from InventoryActors WHERE ina_inv_id=(select inv_id from Inventory where inv_name='" + inventoryName + 
            "') AND ina_act_id=(select act_id from Actors where act_guid='" + actorGuid + "');";
                
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sql);
        } finally {
            returnConnection(connection);
        }
    }
}
