package orca.shirako.util;

import junit.framework.TestCase;

public class IntervalTest extends TestCase
{    
    public void testIntersects() throws Exception
    {
        Interval a = new Interval(10, 20);
        // self
        assertTrue(a.intersects(a));        
        assertTrue(a.intersects(new Interval(10, 10)));
        assertTrue(a.intersects(new Interval(20, 20)));
        assertTrue(a.intersects(new Interval(9, 10)));
        assertTrue(a.intersects(new Interval(9, 11)));
        assertTrue(a.intersects(new Interval(9, 20)));
        assertTrue(a.intersects(new Interval(9, 21)));
        assertTrue(a.intersects(new Interval(15, 20)));
        assertTrue(a.intersects(new Interval(15, 21)));     
        assertTrue(a.intersects(new Interval(20, 30)));
        assertTrue(a.intersects(new Interval(15, 19)));
        assertTrue(!a.intersects(new Interval(1, 9)));
        assertTrue(!a.intersects(new Interval(21, 40)));        
    }
    
    public void testIntersection() throws Exception
    {
        Interval a = new Interval(10, 20);
        assertEquals(a, a.getIntersection(a));
        assertEquals(new Interval(10, 10), a.getIntersection(new Interval(10, 10)));
        assertEquals(new Interval(20, 20), a.getIntersection(new Interval(20, 20)));
        assertEquals(new Interval(10, 10), a.getIntersection(new Interval(9, 10)));
        assertEquals(new Interval(10, 11), a.getIntersection(new Interval(9, 11)));
        assertEquals(new Interval(10, 20), a.getIntersection(new Interval(9, 20)));
        assertEquals(new Interval(10, 20), a.getIntersection(new Interval(9, 21)));
        assertEquals(new Interval(15, 20), a.getIntersection(new Interval(15, 20)));
        assertEquals(new Interval(15, 20), a.getIntersection(new Interval(15, 21)));
        assertEquals(new Interval(20, 20), a.getIntersection(new Interval(20, 30)));
        assertEquals(new Interval(15, 19), a.getIntersection(new Interval(15, 19)));
        assertEquals(null, a.getIntersection(new Interval(1, 9)));
        assertEquals(null, a.getIntersection(new Interval(21, 40)));
    }
}
