package orca.policy.core.util;

import junit.framework.TestCase;

public class IPv4SetTest extends TestCase {    
    public void testConversion() throws Exception{
        assertEquals(0, IPv4Set.toIPv4("0.0.0.0"));
        assertEquals("0.0.0.0", IPv4Set.toString(0));
        assertEquals(2130706433, IPv4Set.toIPv4("127.0.0.1"));        
        assertEquals("127.0.0.1", IPv4Set.toString(2130706433));
        assertEquals(4244635902L, IPv4Set.toIPv4("253.0.0.254"));        
        assertEquals("253.0.0.254", IPv4Set.toString(4244635902L));
        
        System.out.println(IPv4Set.toIPv4("127.0.0.0"));
        System.out.println(IPv4Set.toIPv4("127.0.0.1"));
        System.out.println(IPv4Set.toIPv4("127.0.0.2"));
    }
    
    public void testRange() throws Exception {
        IPv4Set set = new IPv4Set("192.168.1.10-20");
        long temp = IPv4Set.toIPv4("192.168.1.10");
        for (int i = 0; i < 11; i++) {
            assertTrue(set.isFree(temp + i));
            assertFalse(set.isAllocated(temp + i));
        }
        assertEquals(11, set.getFreeCount());
        set = new IPv4Set("192.168.1.10-192.168.1.90");
        assertEquals(81, set.getFreeCount());
        temp = IPv4Set.toIPv4("192.168.1.10");
        for (int i = 0; i < 81; i++) {
            assertTrue(set.isFree(temp + i));
            assertFalse(set.isAllocated(temp + i));
        }
        set = new IPv4Set("192.168.0.1-192.168.1.90");
        assertEquals(346, set.getFreeCount());
        temp = IPv4Set.toIPv4("192.168.0.1");
        for (int i = 0; i < 346; i++) {
            assertTrue(set.isFree(temp + i));
            assertFalse(set.isAllocated(temp + i));
        }
    }
    
    public void testSubnet() throws Exception {
        IPv4Set set = new IPv4Set("192.168.1.0/24");
        assertEquals(255, set.getFreeCount());
        set = new IPv4Set("192.168.1.10/24");
        assertEquals(245, set.getFreeCount());
         set = new IPv4Set("192.168.0.0/16");
        assertEquals(256*256-1, set.getFreeCount());
    }
}