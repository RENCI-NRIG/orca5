package net.exogeni.orca.shirako.util;

/**
 * <code>InventoryListEntry</code> represents and interval with available units, which can be inserted in an
 * <code>InventoryList</code>
 * @author aydan
 *
 */
public class InventoryListEntry extends ListEntry
{    
    /**
     * Interval for the entry.
     */
    protected Interval interval;
    /**
     * Number of units available in the interval.
     */
    protected long units;  
    
    /**
     * Creates a new entry.
     * @param interval interval
     * @param units number of units
     */
    public InventoryListEntry(Interval interval, long units)
    {
        this.interval = interval;
        this.units = units;
    }        
}
