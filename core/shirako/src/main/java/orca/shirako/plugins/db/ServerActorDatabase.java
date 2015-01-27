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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.Vector;

import orca.shirako.util.Client;
import orca.util.ID;
import orca.util.persistence.PersistenceUtils;


public class ServerActorDatabase extends ActorDatabase implements ClientDatabase
{
    public static final String DefaultClientsUrl = "orca/shirako/plugins/db/map.clients.mysql.xml";
    public static String Clients = "clients";
    public static String Client = "client";

    public ServerActorDatabase()
    {
        super(DefaultClientsUrl);
    }

    public ServerActorDatabase(String mapFile)
    {
        super(mapFile);
    }

    /**
     * {@inheritDoc}
     */
    public void addClient(Client client) throws Exception
    {
        Properties p = PersistenceUtils.save(client);
        Properties set = mapper.javaToMysql(DefaultType, p);
        set.put("Clients.clt_act_id", actorId);

        Connection connection = getConnection();

        try {
            StringBuffer sqlquery = new StringBuffer("insert into Clients set ");
            sqlquery.append(constructQueryPartial(set));

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery.toString());
        } finally {
            returnConnection(connection);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateClient(Client client) throws Exception
    {
        Properties p = PersistenceUtils.save(client);
        Properties set = mapper.javaToMysql(DefaultType, p);
        Connection connection = getConnection();

        try {
            StringBuffer sqlquery = new StringBuffer("update Clients set ");
            sqlquery.append(constructQueryPartial(set));
            sqlquery.append(
                " where clt_act_id = '" + actorId + "' AND clt_name = '" + client.getName() + "';");

            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery.toString());
        } finally {
            returnConnection(connection);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeClient(String name) throws Exception
    {
        Connection connection = getConnection();

        try {
            String sqlquery = "delete from Slices where Clients.clt_name = '" + name +
                              "' AND Clients.clt_act_id='" + actorId + "';";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery);
        } finally {
            returnConnection(connection);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeClient(ID guid) throws Exception
    {
        Connection connection = getConnection();

        try {
            String sqlquery = "delete from Clients where Clients.clt_guid = '" + guid.toString() +
                              "' AND Clients.clt_act_id='" + actorId + "';";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sqlquery);
        } finally {
            returnConnection(connection);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Vector<Properties> getClient(String name) throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Clients where clt_name = '" + name +
                           "' AND clt_act_id='" + actorId + "';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, DefaultType);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Vector<Properties> getClient(ID guid) throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Clients where clt_guid = '" + guid.toString() +
                           "' AND clt_act_id='" + actorId + "';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, DefaultType);
        } finally {
            returnConnection(connection);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Vector<Properties> getClients() throws Exception
    {
        Connection connection = getConnection();
        Vector<Properties> result;

        try {
            String query = "select * from Clients where clt_act_id='" + actorId + "';";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(query);
            result = createSearchResultsTyped(rs, DefaultType);
        } finally {
            returnConnection(connection);
        }

        return result;
    }
}