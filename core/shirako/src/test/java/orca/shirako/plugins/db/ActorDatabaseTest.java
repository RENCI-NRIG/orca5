/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.plugins.db;

import java.util.Properties;
import java.util.Vector;

import orca.shirako.api.IActor;
import orca.shirako.api.IDatabase;
import orca.shirako.api.ISlice;
import orca.shirako.common.SliceID;
import orca.shirako.container.OrcaTestCase;
import orca.shirako.container.api.IOrcaContainerDatabase;
import orca.shirako.kernel.SliceFactory;

public abstract class ActorDatabaseTest extends OrcaTestCase
{
    protected IDatabase getCleanDatabase() throws Exception {
        ActorDatabase db = (ActorDatabase)getActorDatabase();
        db.setDb(MySqlDatabaseName);
        db.setMySqlServer(MySqlDatabaseHost);
        db.setMySqlUser(MySqlDatabaseUser);
        db.setActorName(ActorName);
        db.setResetState(true);
        db.initialize();
        return db;
    }
    
    /**
     * Tests if the database class can be created.
     * @throws Exception
     */
    public void testCreate() throws Exception
    {
        getCleanDatabase();
    }

    protected IActor prepareActorDatabase() throws Exception
    {
        IOrcaContainerDatabase cont = getContainerDatabase();
        IActor actor = getActor();
        cont.removeActor(actor.getName());
        cont.addActor(actor);
        actor.actorAdded();
        return actor;
    }

    public void testCreate2() throws Exception
    {
        prepareActorDatabase();
    }

    protected IDatabase getDatabaseToTest() throws Exception {
        IActor actor = prepareActorDatabase();
        return actor.getShirakoPlugin().getDatabase();
    }

    public void testAddSlice() throws Exception
    {
        IDatabase db = getDatabaseToTest();
        
        String sliceName = "SliceToAdd";
        ISlice slice = SliceFactory.getInstance().create(sliceName);
        assertEquals(sliceName, slice.getName());
        SliceID id = slice.getSliceID();
        
        db.addSlice(slice);
        Vector<Properties> v = db.getSlice(id);
        assertNotNull(v);
        assertEquals(1, v.size());
        
        ISlice slice2 = SliceFactory.createInstance(v.get(0));
        assertNotNull(slice2);
        // FIXME: add an equals method to slice
        // assertEquals(slice, slice2);
       
    }

    public void testAddReservation() throws Exception
    {
        // IReservation r =
        // AuthorityReservationFactory.getInstance().create(resources, term,
        // slice);
    }
}
