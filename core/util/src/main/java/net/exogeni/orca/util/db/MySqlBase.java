/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.util.db;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import net.exogeni.orca.util.OrcaException;
import net.exogeni.orca.util.PathGuesser;
import net.exogeni.orca.util.persistence.NotPersistent;
import net.exogeni.orca.util.persistence.Persistent;

import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.log4j.Logger;

public class MySqlBase implements DatabaseBase
{
    /**
     * Default type for mapping.
     */
    public static final String TypeDefault = "mysql";

    // configuration properties
    public static final String PropertyMySqlServer = "db.mysql.server";
    public static final String PropertyMySqlServerPort = "db.mysql.port";
    public static final String PropertyMySqlUser = "db.mysql.user";
    public static final String PropertyMySqlPassword = "db.mysql.password";
    public static final String PropertyMySqlDb = "db.mysql.db";
    public static final String PropertyMySqlPool = "db.mysql.pool";
    public static final String PropertyMySqlConnectionOptions = "db.mysql.connection-options";

    /**
     * Helper function to construct a query. Constructs a query of the form
     * key1='value1', key2='value2' etc from the Properties list it is passed
     * @param p Properties list
     * @return query string
     */
    public static String constructQueryPartial(Properties p)
    {
        StringBuffer query = new StringBuffer("");
        Iterator<?> i = p.entrySet().iterator();

        while (i.hasNext()) {
            if (query.length() > 0) {
                query.append(", ");
            }

            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();
            query.append(name + "='" + value + "'");
        }

        return query.toString();
    }

    /**
     * Create a Properties list from a ResultSet obtained from a query
     * @param rs The ResultSet
     * @param type Object type (node, machine, etc)
     * @return Translated properties list
     * @throws Exception in case of error
     */
    protected Vector<Properties> createSearchResultsTyped(ResultSet rs, String type) throws Exception {
        Vector<Properties> result = new Vector<Properties>();
        ResultSetMetaData rsm = rs.getMetaData();
        int numberOfCols = rsm.getColumnCount();
        String[] Columns = new String[numberOfCols];

        for (int i = 0; i < numberOfCols; i++) {
            Columns[i] = rsm.getColumnName(i + 1);
        }

        while (rs.next()) {
            Properties p = new Properties();

            for (int i = 0; i < numberOfCols; i++) {
                String tmp = rs.getString(i + 1);

                if (tmp != null) {
                    p.put(Columns[i], tmp);
                }
            }

            Properties set = mapper.mysqlToJava(type, p);
            result.add(set);
        }

        return result;
    }


    /**
     * Reset flag. Note: the default for MySQL when running all actors from the
     * same database is not to reset. The reset operation must be done only once
     * and this is taken care of by the container database. This differs from
     * the LDAP implementation in which each actor has its own branch in the
     * database that it can safely reset.
     */
    @NotPersistent
    protected boolean resetState = false;
    /**
     * Initialization flag.
     */
    @NotPersistent
    protected boolean initialized = false;
    /**
     * The logging tool.
     */
    @NotPersistent
    protected Logger logger;
    /**
     * The name of the pool. Can be anything.
     */
    @Persistent(key = PropertyMySqlPool)
    protected String pool = "ShirakoPool";
    /**
     * Server must be set. Port is 3306 by default
     */
    @Persistent(key = PropertyMySqlServer)
    protected String mySqlServer = null;
    /**
     * Server port.
     */
    @Persistent(key = PropertyMySqlServerPort)
    protected String mySqlServerPort = "3306";
    /**
     * Database name to use. Must be set
     */
    @Persistent(key = PropertyMySqlDb)
    protected String db;
    /**
     * MySql Username. Must be set
     */
    @Persistent(key = PropertyMySqlUser)
    protected String mySqlUser;
    /**
     * MySql password. Default no password
     */
    @Persistent(key = PropertyMySqlPassword)
    protected String mySqlPasswd;
    /**
     * MySql Connections Options. Default empty.
     * e.g. "{@literal &}verifyServerCertificate=false{@literal &}useSSL=true"
     * https://github.com/RENCI-NRIG/exogeni/issues/173
     * https://github.com/RENCI-NRIG/net.exogeni.orca5/pull/156
     */
    @Persistent(key = PropertyMySqlConnectionOptions)
    protected String mySqlConnectionOptions = "";
    /**
     * Location of the file to use to create the connection pool
     */
    protected final String joclFileLocation = PathGuesser.getRealBase() + "/config/shirako.jocl";
    /**
     * source URL
     */
    protected final String source = "jdbc:apache:commons:dbcp:";
    /**
     * JDBC drivers we are using.
     */
    protected final String[] driverPath = { "com.mysql.jdbc.Driver", "org.apache.commons.dbcp.PoolingDriver" };
    /**
     * Connection pool.
     */
    @NotPersistent
    protected MySqlPool mySqlPool;
    /**
     * Mapping file.
     */
    @NotPersistent
    protected String mapFile;
    /**
     * Mapper to MySql and from Java
     */
    @NotPersistent
    protected MySqlPropertiesMapper mapper;
    /**
     * Creation flag
     */
    @NotPersistent
    protected boolean create;

    /**
     * Creates a new instance.
     * @param mapFile mapFile name
     */
    public MySqlBase(String mapFile)
    {
        if (mapFile == null) {
            throw new IllegalArgumentException("mapFile cannot be null");
        }
        this.mapFile = mapFile;
    }

    /**
     * Checks the database and resets it if needed.
     * @throws SQLException in case of error
     */
    protected void checkDb() throws SQLException
    {
        Connection connection = getConnection();
        try {
            if (resetState) {
                resetDB(connection);
            }
        } finally {
            returnConnection(connection);
        }
    }

    /**
     * Deregister the sun ODBC bridge driver. We don't need it and it is broken
     * on some platforms
     */
    protected void checkDrivers()
    {
        try {
            logger.debug(DriverManager.getDriver(source + pool));

            Enumeration<Driver> drivers = DriverManager.getDrivers();
            Driver odbcDriver;

            if ((odbcDriver = drivers.nextElement()) != null) {
                if (odbcDriver.getClass().getName().matches(".*odbc.*")) {
                    DriverManager.deregisterDriver(odbcDriver);
                    logger.debug("Deregistering " + odbcDriver);
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * Get a connection from the pool.
     * @return connection
     * @throws SQLException in case of error
     */
    public Connection getConnection() throws SQLException
    {
        return DriverManager.getConnection(source + pool, mySqlUser, mySqlPasswd);
    }

    public String getDb()
    {
        return db;
    }

    public Logger getLogger()
    {
        return logger;
    }

    public String getMySqlPasswd()
    {
        return mySqlPasswd;
    }

    public String getMySqlServer()
    {
        return mySqlServer;
    }

    public String getMySqlServerPort()
    {
        return mySqlServerPort;
    }

    public String getMySqlUser()
    {
        return mySqlUser;
    }

    public String getPoolConfigLocation()
    {
        return joclFileLocation;
    }

    public Connection getServerConnection() throws SQLException
    {
        throw new SQLException("Not implemented");
    }

    public void initialize() throws OrcaException
    {
        if (!initialized) {
        	try {
	            if (logger == null) {
	                logger = Logger.getLogger(this.getClass().getCanonicalName());
	            }
	            if (db == null) {
	                throw new OrcaException("Missing database name");
	            }
	            if (mapFile != null) {
	                mapper = new MySqlPropertiesMapper(mapFile);
	            }

	            loadDrivers();

	            /*
	             * If either username or password is null,
	             * DriverManagerConnectionFactory will use nulls for both. This is a
	             * "feature". We work around this by setting the username as part of
	             * the URL
	             */
                String url = "jdbc:mysql://" + mySqlServer + ":" + mySqlServerPort + "/" + db + "?user=" + mySqlUser + mySqlConnectionOptions;
                logger.debug("mysql database: " + url);

	            // register the connection pool
	            DriverManagerConnectionFactory factory = new DriverManagerConnectionFactory(url, mySqlUser, mySqlPasswd);
	            MySqlPool.registerPool(factory, joclFileLocation, pool);

	            if (mapper != null) {
	                mapper.initialize();
	            }

	            checkDb();
	            initialized = true;
        	} catch (OrcaException e) {
        		throw e;
        	} catch (Exception e) {
        		throw new OrcaException("Cannot initialize", e);
        	}
        }
    }

    /**
     * Load the JDBC drivers. The driver's init blocks will register themselves
     * with the DriverManager
     * @throws Exception in case of error
     */
    protected void loadDrivers() throws Exception
    {
        for (int i = 0; i < driverPath.length; i++) {
            Class.forName(driverPath[i]).newInstance();
        }
    }

    /**
     * Resets the database to a clean state.
     * @param connection connection object
     * @throws SQLException in case of error
     */
    protected void resetDB(Connection connection) throws SQLException
    {
    }

    /**
     * Return the connection to the pool
     * @param connection connection object
     * @throws SQLException in case of error
     */
    public void returnConnection(Connection connection) throws SQLException
    {
        connection.close();
    }

    public void setDb(String db)
    {
        this.db = db;
    }

    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }

    public void setMySqlPasswd(String pass)
    {
        this.mySqlPasswd = pass;
    }

    public void setMySqlServer(String server)
    {
        this.mySqlServer = server;
    }

    public void setMySqlServerPort(String serverPort)
    {
        this.mySqlServerPort = serverPort;
    }

    public void setMySqlUser(String user)
    {
        this.mySqlUser = user;
    }

    public void setResetState(boolean state)
    {
        resetState = state;
    }
}
