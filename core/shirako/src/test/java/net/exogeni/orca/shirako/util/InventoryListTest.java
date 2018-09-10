package net.exogeni.orca.shirako.util;

import java.util.ArrayList;

import junit.framework.TestCase;

public class InventoryListTest extends TestCase
{
    class IntervalState
    {
        public long start;
        public long end;
        public long units;
        
        public IntervalState(long start, long end, long units)
        {
            this.start = start;
            this.end = end;
            this.units = units;
        }
    }
    
    
    private void testList(InventoryList list, ArrayList<IntervalState> expected)
    {
        int index = 0;
        ListEntry le = list.list.next;
        while (le != list.list){
            InventoryListEntry entry = (InventoryListEntry)le;
            System.out.println(entry.interval.toString() + "->" + entry.units);
            IntervalState state = expected.get(index++);
            assertEquals(state.start, entry.interval.start);
            assertEquals(state.end, entry.interval.end);
            assertEquals(state.units, entry.units);
            le = le.next;
        }
        assertEquals(expected.size(), index);        
    }
    
    public void testInventory()
    {
        ArrayList<IntervalState> state = new ArrayList<IntervalState>();
        InventoryList list = new InventoryList();
        
        list.addInventory(10, 20, 10);
        state.clear();
        state.add(new IntervalState(10, 20, 10));
        list.addInventory(30, 40, 5);
        state.add(new IntervalState(30, 40, 5));        
        testList(list, state);
        
        state.clear();
        list.addInventory(15, 30, 3);
        state.add(new IntervalState(10, 14, 10));
        state.add(new IntervalState(15, 20, 13));
        state.add(new IntervalState(21, 29, 3));
        state.add(new IntervalState(30, 30, 8));
        state.add(new IntervalState(31, 40, 5));
        testList(list, state);
        
    }
}
