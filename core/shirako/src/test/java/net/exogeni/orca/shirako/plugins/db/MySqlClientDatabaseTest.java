/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.plugins.db;

import net.exogeni.orca.shirako.api.IDatabase;
import net.exogeni.orca.shirako.plugins.db.ServerActorDatabase;

import net.exogeni.orca.util.db.MySqlPropertiesMapper;


public class MySqlClientDatabaseTest extends ActorDatabaseTest
{
    public IDatabase getCleanDatabase() throws Exception
    {
        ServerActorDatabase db = new ServerActorDatabase();
        db.setDb(MySqlDatabaseName);
        db.setMySqlServer(MySqlDatabaseHost);
        db.setMySqlUser(MySqlDatabaseUser);

        db.setActorName(ActorName);
        db.setResetState(true);
        db.initialize();

        return db;
    }

    public void testMapFile() throws Exception
    {
        MySqlPropertiesMapper mapper = new MySqlPropertiesMapper(
            ServerActorDatabase.DefaultClientsUrl);
        mapper.setFailOnError(true);
        mapper.initialize();
    }
}
