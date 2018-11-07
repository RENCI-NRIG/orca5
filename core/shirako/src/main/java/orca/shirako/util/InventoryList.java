/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.util;

import java.util.Properties;

import orca.shirako.api.IAuthorityProxy;
import orca.util.ResourceType;

/**
 * An <code>InventoryList</code> is a list of available units over a period of
 * time. The list consists of one or more intervals with the corresponding
 * number of units available over the interval. The data structure supports the following operations:
 * <ul>
 *  <li>Addition of new inventory units for a given period of time</li>
 *  <li>Reservation of a number of units over a period of time</li>
 *  <li>Releasing of a number of units over a period of time</li>
 * </ul>
 * @author aydan
 */
public class InventoryList
{
    /**
     * The list head.
     */
    protected ListEntry list;

    protected Properties properties;
    
    protected ResourceType rtype;
    
    protected IAuthorityProxy proxy;
    
    /**
     * Creates a new empty inventory list.
     */
    public InventoryList()
    {
        list = new ListEntry();
        InventoryListEntry.InitializeListHead(list);
        properties = new Properties();
    }

    /**
     * Adds inventory for the specified period of time
     * @param start start of the period (inclusive)
     * @param end end of the period (inclusive)
     * @param units number of units to add
     */
    public synchronized void addInventory(long start, long end, long units)
    {
        add(new Interval(start, end), -units, true);
    }

    /**
     * Marks the specified units as reserved over the given period of time.
     * 
     * @param start start time (inclusive)
     * @param end end time (inclusive)
     * @param units number of units to reserve
     */
    public synchronized void reserve(long start, long end, long units)
    {
        add(new Interval(start, end), units, false);
    }

    /**
     * Releases the specified units. Note: this call must have been preceeded by a call to reserve.
     * @param start start time (inclusive)
     * @param end end time (inclusive)
     * @param units number of units to release
     */
    public synchronized void release(long start, long end, long units)
    {
        add(new Interval(start, end), -units, false);
    }

    /**
     * Adds the specified units over the given period of time.
     * @param _interval interval
     * @param units units (can be negative)
     * @param create if true, new intervals will be added if necessary.
     */
    private void add(Interval _interval, long units, boolean create)
    {
        Interval interval = new Interval(_interval);
        boolean checkAfterLoop = true;

        ListEntry le = list.next;
        while (le != list) {
            InventoryListEntry entry = (InventoryListEntry) le;
            // get the intersection with the current entry
            Interval inter = interval.getIntersection(entry.interval);
            if (inter != null) {
                // we have intersection.
                checkAfterLoop = false;

                // anything from entry to the left of inter?
                if (entry.interval.start < inter.start) {
                    Interval ni = new Interval(entry.interval.start, inter.start - 1);
                    InventoryListEntry ne = new InventoryListEntry(ni, entry.units);
                    ListEntry.InsertBefore(entry, ne);
                }

                // anything from entry to the right of inter?
                if (entry.interval.end > inter.end) {
                    Interval ni = new Interval(inter.end + 1, entry.interval.end);
                    InventoryListEntry ne = new InventoryListEntry(ni, entry.units);
                    ListEntry.InsertAfter(entry, ne);
                }

                // anything from interval to the left of inter?
                if (interval.start < inter.start) {
                    if (create) {
                        Interval ni = new Interval(interval.start, inter.start - 1);
                        InventoryListEntry ne = new InventoryListEntry(ni, -units);
                        ListEntry.InsertBefore(entry, ne);
                    }
                }
                
                // anything from interval to the right of inter?
                if (interval.end > inter.end) {
                    interval.start = inter.end + 1;
                    checkAfterLoop = true;
                }

                // modify entry to cover the intersection region
                // no need to add to the list (this one is already in the list)
                entry.interval.start = inter.start;
                entry.interval.end = inter.end;
                entry.units -= units;
            } else {
                // no intersection
                if (interval.end < entry.interval.start) {
                    return;
                }
            }
            le = le.next;
        }
        
        if (create && checkAfterLoop) {
            InventoryListEntry ne = new InventoryListEntry(interval, -units);
            ListEntry.InsertBefore(list, ne);
        }
    }

    /**
     * Notifies the list of the passage of time. The list will retain
     * only information that represents periods after the specified time.
     * @param time current time
     */
    public synchronized void tick(final long time)
    {
        ListEntry le = list.next;
        while (le != list) {
            InventoryListEntry entry = (InventoryListEntry) le;
            le = le.next;
            if (entry.interval.end <= time) {
                ListEntry.RemoveEntry(entry);
            }
        }
    }

    public synchronized long getMinUnits(final long start, final long end)
    {
        long min = Long.MAX_VALUE;
        ListEntry le = list.next;
        Interval interval = new Interval(start, end);
        boolean checkAfterLoop = true;
        while (le != list) {
            InventoryListEntry entry = (InventoryListEntry) le;
            Interval inter = interval.getIntersection(entry.interval);
            if (inter != null) {
                // we have intersection.
                checkAfterLoop = false;

                // we do not care about anything from entry to the left of inter

                // we do not care about anything from entry to the right of
                // inter
                // anything from interval to the left of inter?
                if (interval.start < inter.start) {
                    // we have a gap
                    min = 0;
                    break;
                }
                // anything from interval to the right of inter?
                if (interval.end > inter.end) {
                    interval.start = inter.end + 1;
                    checkAfterLoop = true;
                }

                if (min > entry.units) {
                    min = entry.units;
                }
            } else {
                // no intersection
                if (interval.end < entry.interval.start) {
                    break;
                }
            }
            le = le.next;
        }
        if (checkAfterLoop) {
            // we have a gap
            min = 0;
        }
        return min;
    }

    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        ListEntry le = list.next;
        while (le != list) {
            InventoryListEntry entry = (InventoryListEntry) le;
            sb.append(entry.interval.toString());
            sb.append("->");
            sb.append(entry.units);
            sb.append("\n");
            le = le.next;
        }
        return sb.toString();
    }
    
    public void setProperties(Properties properties)
    {
        this.properties = properties;
    }
    
    public Properties getProperties()
    {
        return properties;
    }
    
    public void setType(ResourceType type)
    {
        this.rtype = type;
    }
    
    public ResourceType getType()
    {
        return rtype;
    }
    
    public void setProxy(IAuthorityProxy proxy)
    {
        this.proxy = proxy;
    }
    
    public IAuthorityProxy getProxy()
    {
        return proxy;
    }
    
}
