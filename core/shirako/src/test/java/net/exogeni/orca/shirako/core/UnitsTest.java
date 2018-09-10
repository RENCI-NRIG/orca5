package net.exogeni.orca.shirako.core;

import junit.framework.TestCase;

public class UnitsTest extends TestCase {
    public void testUnits() {
        Units set = new Units();
        
        assertEquals(0, set.size());
        Unit u = new Unit();
        set.add(u);
        assertEquals(1, set.size());
        
        Unit u2 = set.get(u.getID());
        assertNotNull(u2);
        assertSame(u2, u);
        
        // add it again
        set.add(u);
        assertEquals(1, set.size());
    }
}
