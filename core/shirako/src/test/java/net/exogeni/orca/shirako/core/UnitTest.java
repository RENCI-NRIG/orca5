package net.exogeni.orca.shirako.core;

import java.util.Properties;

import junit.framework.TestCase;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.shirako.kernel.ClientReservationFactory;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.persistence.PersistenceUtils;

public class UnitTest extends TestCase {
    
    public void testUnit() throws Exception {
        Unit u1 = new Unit();
        assertNotNull(u1.getID());
        assertEquals(UnitState.DEFAULT, u1.getState());
        assertNull(u1.getProperty("foo"));
        assertNull(u1.getParentID());
        assertNull(u1.getReservationID());
        assertNull(u1.getSliceID());
        assertNull(u1.getActorID());
        assertEquals(0, u1.getSequence());        
        u1.incrementSequence();
        assertEquals(1, u1.getSequence());
        u1.decrementSequence();
        assertEquals(0, u1.getSequence());
        
        ReservationID rid = new ReservationID();
        SliceID sliceid = new SliceID();
        IReservation r = ClientReservationFactory.getInstance().create(rid);
        ID actorid = new ID();
        
        u1.setActorID(actorid);
        u1.setReservation(r);
        u1.setSliceID(sliceid);
        
        u1.startPrime();
        assertEquals(UnitState.PRIMING, u1.getState());
        u1.setProperty("foo", "bar");
        u1.incrementSequence();
        u1.incrementSequence();
        assertEquals(2, u1.getSequence());
        
        Properties p = PersistenceUtils.save(u1);
        Unit u2 = PersistenceUtils.restore(p);
        
        assertEquals(u1.getID(), u2.getID());
        assertEquals(UnitState.PRIMING, u2.getState());
        assertEquals(2, u2.getSequence());
        assertEquals("bar", u2.getProperty("foo"));
        assertEquals(rid, u2.getReservationID());
        assertEquals(sliceid, u2.getSliceID());
        assertEquals(actorid, u2.getActorID());
    }
}
