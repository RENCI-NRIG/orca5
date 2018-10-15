package net.exogeni.orca.shirako.util;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class FreeAllocatedSetTest extends TestCase {
    public void testIt() {
        FreeAllocatedSet<Integer> set = new FreeAllocatedSet<Integer>();
        assertEquals(0, set.free.size());
        assertEquals(0, set.allocated.size());
        
        for (int i = 0; i < 50; i++) {
            assertEquals(i, set.getFree());
            assertEquals(0, set.getAllocated());
            Integer j = new Integer(i);
            assertFalse(set.free.contains(j));
            assertFalse(set.allocated.contains(j));
            set.addInventory(j);
            assertTrue(set.free.contains(j));
            assertFalse(set.allocated.contains(j));
        }
        
        Integer i = set.allocate();
        assertNotNull(i);
        assertEquals(1, set.getAllocated());
        assertEquals(49, set.getFree());
        assertTrue(set.allocated.contains(i));
        assertFalse(set.free.contains(i));
        
        set.free();
        assertEquals(0, set.getAllocated());
        assertEquals(50, set.getFree());
        assertFalse(set.allocated.contains(i));
        assertTrue(set.free.contains(i));

        List<Integer> list = set.allocate(25);
        assertEquals(25, list.size());
        for (Integer j : list) {
            assertEquals(25, set.getAllocated());
            assertEquals(25, set.getFree());
            assertFalse(set.free.contains(j));
            assertTrue(set.allocated.contains(j));
            
        }
        List<Integer> sub1 = list.subList(0, 10);
        List<Integer> sub2 = list.subList(10, 25);
        set.free(sub1);
        
        assertEquals(35, set.getFree());
        assertEquals(15, set.getAllocated());
        
        for (Integer j : sub1) {
            assertTrue(set.free.contains(j));
            assertFalse(set.allocated.contains(j));
        }

        for (Integer j : sub2) {
            assertFalse(set.free.contains(j));
            assertTrue(set.allocated.contains(j));
        }
        
        // free sub2
        set.free(sub2);
        assertEquals(50, set.getFree());
        assertEquals(0, set.getAllocated());
        
        for (Integer j : list) {
            assertTrue(set.free.contains(j));
            assertFalse(set.allocated.contains(j));
        }
    }
    
    public static Test suite() {
        return new TestSuite(FreeAllocatedSetTest.class);
    }
}
