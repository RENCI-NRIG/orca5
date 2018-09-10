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


public class MySqlDatabaseTest extends ActorDatabaseTest
{
    public IDatabase getCleanDatabase() throws Exception
    {
        ActorDatabase db = new ActorDatabase();
        db.setDb(MySqlDatabaseName);
        db.setMySqlServer(MySqlDatabaseHost);
        db.setMySqlUser(MySqlDatabaseUser);

        db.setActorName(ActorName);
        db.setResetState(true);
        db.initialize();

        return db;
    }
}
