/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.container;

import orca.shirako.container.api.IOrcaContainerDatabase;
import orca.shirako.container.db.OrcaContainerDatabase;
import orca.util.db.MySqlPropertiesMapper;

public class MySqlShirakoContainerDatabaseTest extends ContainerDatabaseTest
{
    public IOrcaContainerDatabase getCleanDatabase() throws Exception
    {
        OrcaContainerDatabase db = new OrcaContainerDatabase();

        db.setDb(MySqlDatabaseName);
        db.setMySqlServer(MySqlDatabaseHost);
        db.setMySqlUser(MySqlDatabaseUser);

        db.setResetState(true);
        db.initialize();

        return db;
    }

    public void testMapFile() throws Exception
    {
        MySqlPropertiesMapper mapper = new MySqlPropertiesMapper(OrcaContainerDatabase.DefaultCodManagementMapUrl);
        mapper.setFailOnError(true);
        mapper.initialize();
    }
}
