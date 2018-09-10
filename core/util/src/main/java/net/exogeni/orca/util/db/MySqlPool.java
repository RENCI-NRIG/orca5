/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.util.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.jocl.JOCLContentHandler;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * Single class to pool a collection of MySql connections.
 * @author varun
 * @author aydan
 */
public abstract class MySqlPool {

    /**
     * The pooling driver.
     */
    private static PoolingDriver driver = new PoolingDriver();

    /**
     * Obtains an input stream to the default pool configuration file.
     * @return
     * @throws IOException
     */
    private static InputStream getDefaultConfiguration() throws IOException {
        URL defaultConfig = MySqlPool.class.getClassLoader().getResource("net/exogeni/orca/util/db/pool.jocl");
        if (defaultConfig == null) {
            throw new RuntimeException("Could not find default pool configuration");
        }
        return defaultConfig.openStream();
    }

    /**
     * Create the connection pool from the parameters found in the specified
     * JOCL file. If the JOCL file cannot be found, then the default
     * configuration is used.
     * @param loc location of the JOCL file (can be null)
     * @return The connection pool
     * @throws Exception
     */
    private static GenericObjectPool createConnectionPool(String loc) {
        try {
            System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");

            InputStream is = null;
            if (loc == null) {
                is = getDefaultConfiguration();
            } else {
                File f = new File(loc);
                if (!f.exists()) {
                    is = getDefaultConfiguration();
                } else {
                    is = new FileInputStream(f);
                }
            }
            JOCLContentHandler handler = JOCLContentHandler.parse(is);
            return (GenericObjectPool) handler.getValue(0);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create connection pool", e);
        }
    }

    public static synchronized void registerPool(DriverManagerConnectionFactory factory, String joclFileLocation, String poolName) throws Exception {
        if (factory == null) {
            throw new IllegalArgumentException("factory cannot be null");
        }
        if (poolName == null) {
            throw new IllegalArgumentException("poolName cannot be null");
        }

        // register the pool only if this is a new pool
        boolean exists = false;
        String[] pools = driver.getPoolNames();
        for (int i = 0; i < pools.length; i++) {
            if (pools[i].equals(poolName)){
                exists = true;
                break;
            }
        }
                
        if (!exists) {
            GenericObjectPool connectionPool = createConnectionPool(joclFileLocation);
            PoolableConnectionFactory pcf = new PoolableConnectionFactory(factory, connectionPool, null, null, false, true);
            driver.registerPool(poolName, pcf.getPool());
        }
    }

    private MySqlPool() {
    }
}
