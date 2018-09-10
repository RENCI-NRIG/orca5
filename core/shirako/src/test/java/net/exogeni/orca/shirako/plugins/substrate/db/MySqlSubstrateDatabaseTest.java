/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.shirako.plugins.substrate.db;

import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IDatabase;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.core.Unit;
import net.exogeni.orca.shirako.core.UnitState;
import net.exogeni.orca.shirako.kernel.AuthorityReservationFactory;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.kernel.SliceFactory;
import net.exogeni.orca.shirako.plugins.db.ActorDatabaseTest;
import net.exogeni.orca.shirako.plugins.substrate.ISubstrateDatabase;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.util.db.MySqlPropertiesMapper;
import net.exogeni.orca.util.persistence.PersistenceUtils;

public class MySqlSubstrateDatabaseTest extends ActorDatabaseTest
{
    @Override
    protected IDatabase makeActorDatabase() {
        SubstrateActorDatabase db = new SubstrateActorDatabase();
        return db;
    }
 
    public void testMapFile() throws Exception {
        MySqlPropertiesMapper mapper = new MySqlPropertiesMapper(SubstrateActorDatabase.DefaultMapUrl);
        mapper.setFailOnError(true);
        mapper.initialize();
    }

    public void testAddUpdateGetUnit() throws Exception
    {
        IActor actor = prepareActorDatabase();
        ISubstrateDatabase db = (ISubstrateDatabase) actor.getShirakoPlugin().getDatabase();

        // create a slice
        ISlice slice = SliceFactory.getInstance().create("slice");
        db.addSlice(slice);
        // create a reservation
        ResourceSet rset = new ResourceSet();
        long now = System.currentTimeMillis();
        Term term = new Term(new Date(now), new Date(now + 1000 * 60 * 20));
        IReservation r = AuthorityReservationFactory.getInstance().create(rset, term, slice);
        db.addReservation(r);

        // create a unit
        Unit u = new Unit(r.getReservationID(), slice.getSliceID(), actor.getGuid());
        db.addUnit(u);

        // lookup the unit
        Vector<Properties> v = db.getUnit(u.getID());
        assertNotNull(v);
        assertEquals(1, v.size());        

        Unit uu = new Unit();
        PersistenceUtils.restore(uu, v.get(0));

        // make sure what we found matches
        assertEquals(u.getID(), uu.getID());
        assertEquals(u.getState(), uu.getState());

        // change the unit state
        u.startPrime();
        assertEquals(UnitState.PRIMING, u.getState());

        // update the database record
        db.updateUnit(u);

        // look it up
        v = db.getUnit(u.getID());
        assertNotNull(v);
        assertEquals(1, v.size());

        // do they match?
        uu = new Unit();
        PersistenceUtils.restore(uu, v.get(0));        
        assertEquals(u.getID(), uu.getID());
        assertEquals(u.getState(), uu.getState());        
    }
}
