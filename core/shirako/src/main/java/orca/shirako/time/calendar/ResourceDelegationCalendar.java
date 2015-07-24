/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */
package orca.shirako.time.calendar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import orca.shirako.common.ResourceVector;

/**
 * This class maintains a data structure to represent a calendar of units. The
 * calendar maintains information about the availability of units over a period
 * of time. Each calendar is initialized with a period and number of units. The
 * calendar then allows a number of units to be reserved for a period of time.
 * Reserved units can also be released. The calendar can be notified about the
 * passage of time, so that it can discard unnecessary data.
 */
public class ResourceDelegationCalendar
{
    /**
     * List of available units sorted by end time.
     */
    private ArrayList<AvailableResources> list;

    /**
     * Total number of units.
     */
    private long myUnits;

    /**
     * Maximum resource vector.
     */
    private ResourceVector myVector;

    /**
     * Creates a new <code>UnitsList</code>.
     * @param start start time
     * @param end end time
     * @param units number of units
     */
    public ResourceDelegationCalendar(final long start, final long end, final int units, final ResourceVector vector)
    {
        list = new ArrayList<AvailableResources>();

        AvailableResources u = new AvailableResources();
        u.start = start;
        u.end = end;
        u.units = units;
        u.vector = vector;
        addToList(u);

        this.myUnits = units;
        this.myVector = vector;
    }

    /**
     * Copy constructor.
     * @param original calendar to copy
     */
    protected ResourceDelegationCalendar(final ResourceDelegationCalendar original)
    {
        this.list = new ArrayList<AvailableResources>(original.list.size());

        for (int i = 0; i < original.list.size(); i++) {
            AvailableResources u = original.list.get(i);
            this.list.add(i, u.copy());
        }

        this.myUnits = original.myUnits;
        this.myVector = original.myVector;
    }

    /**
     * Adds an element to the linked list, maintaining the list in sorted order.
     * 
     * @param entry element to add
     */
    protected void addToList(final AvailableResources entry)
    {
        int index = Collections.binarySearch(list, entry);

        if (index < 0) {
            index = -index - 1;
        }

        list.add(index, entry);
    }

    /**
     * Creates a deep copy of the calendar.
     * 
     * @return a copy of the calendar
     */
    public synchronized ResourceDelegationCalendar copy()
    {
        return new ResourceDelegationCalendar(this);
    }

    /**
     * Displays the contents of the calendar.
     */
    public void dump()
    {
        int size = list.size();

        System.out.println("==================");

        for (int i = 0; i < size; i++) {
            AvailableResources item = list.get(i);
            System.out.println(item.toString());
        }
    }

    /**
     * Returns the internal interval list maintained by the calendar.
     * 
     * @return interval list
     */
    public ArrayList<AvailableResources> getList()
    {
        return list;
    }

    // FIXME: ignores resource vector
    /**
     * Returns the minimum number of units available over the specified
     * interval.
     * 
     * @param start start time
     * @param end end time
     * 
     * @return minimum number of units available in the interval
     */
    public long getMinUnits(final long start, final long end)
    {
        // XXX: use binary search to select the initial index (will be 0 most of
        // the time)
        int index = 0;
        int size = list.size();
        long result = 0;
        boolean first = true;

        for (; index < size; index++) {
            AvailableResources entry = list.get(index);

            if (entry.end < start) {
                continue;
            }

            if (entry.start > end) {
                break;
            }

            // we have overlap
            if ((entry.units < result) || first) {
                result = entry.units;
                first = false;
            }
        }

        return result;
    }

    /**
     * Checks if there has been a split operation over the specified interval.
     * @param start
     * @param end
     * @return true if a split operation has taken place.
     */
    public boolean hasBeenSplit(final long start, final long end)
    {
        // XXX: use binary search to select the initial index (will be 0 most of
        // the time)
        int index = 0;
        int size = list.size();

        for (; index < size; index++) {
            AvailableResources entry = list.get(index);

            if (entry.end < start) {
                continue;
            }

            if (entry.start > end) {
                break;
            }

            // we have overlap
            if (entry.units < myUnits){
                return true;
            }
        }

        return false;
    }
    
    /**
     * Checks if there has been an extraction operation over the specified interval
     * @param start
     * @param end
     * @return true if an extract operation has taken place, false otherwise
     */
    public boolean hasBeenExtracted(final long start, final long end)
    {
        // XXX: use binary search to select the initial index (will be 0 most of
        // the time)
        int index = 0;
        int size = list.size();

        for (; index < size; index++) {
            AvailableResources entry = list.get(index);

            if (entry.end < start) {
                continue;
            }

            if (entry.start > end) {
                break;
            }

            // we have overlap
            if (myVector.contains(entry.vector)){
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the minimum resource vector available over the specified interval.
     * @param start
     * @param end
     * @return
     */
    public ResourceVector getMinVector(final long start, final long end)
    {
        // XXX: use binary search to select the initial index (will be 0 most of
        // the time)
        int index = 0;
        int size = list.size();
        ResourceVector vector = null;
        
        for (; index < size; index++) {
            AvailableResources entry = list.get(index);

            if (entry.end < start) {
                continue;
            }

            if (entry.start > end) {
                break;
            }

            // we have overlap
            if (vector == null || vector.contains(entry.vector)) {
                vector = entry.vector;
            }
        }

        return vector;
    }

    /**
     * Returns the start time of the calendar.
     * 
     * @return start time
     */
    public synchronized long getStart()
    {
        long result = -1;

        if (list.size() > 0) {
            AvailableResources entry = list.get(0);
            result = entry.start;
        }

        return result;
    }

    /**
     * Returns the capacity of the calendar.
     * 
     * @return number of units
     */
    public long getUnits()
    {
        return myUnits;
    }

    /**
     * Returns the max resource vector.
     * @return
     */
    public ResourceVector getResourceVector()
    {
        return myVector;
    }

    /**
     * Returns the number of units available for the specified time. The result
     * is a list of one or more intervals, each containing the number of units
     * and the resource vector available in the interval.
     * 
     * @param start start time
     * @param end end time
     * 
     * @return list of intervals representing unit availability
     */
    public synchronized AvailableResources[] getAvailability(final long start, final long end)
    {
        // XXX: use binary search to select the initial index (will be 0 most of
        // the time)
        int index = 0;
        int size = list.size();
        ArrayList<AvailableResources> result = new ArrayList<AvailableResources>();

        for (; index < size; index++) {
            AvailableResources entry = list.get(index);

            if (entry.end < start) {
                continue;
            }

            if (entry.start > end) {
                break;
            }

            // we have overlap
            long s;
            long e;

            if (entry.start < start) {
                s = start;
            } else {
                s = entry.start;
            }

            if (entry.end > end) {
                e = end;
            } else {
                e = entry.end;
            }

            result.add(new AvailableResources(s, e, entry.units, entry.vector));
        }

        return (AvailableResources[]) result.toArray();
    }

    /**
     * Releases the given number of units for the time interval. This method
     * verifies if release results in a surplus, more units that the initial
     * capacity of the calendar. If surplus is detected, the method returns a
     * list of intervals with violations (conflict entries).
     * 
     * @param start start time
     * @param end end time
     * @param units number of units
     * 
     * @return list of conflict entries
     * 
     * @throws Exception
     */
    public synchronized AvailableResources[] release(final long start, final long end, final int units) throws Exception
    {
        assert units >= 0;

        AvailableResources u = new AvailableResources();
        u.start = start;
        u.end = end;
        u.units = -units;
        u.vector = ResourceVector.Zero(myVector);

        return reserve(u, false);
    }

    /**
     * Releases the given number of units for the time interval. This method
     * verifies if release results in a surplus, more units that the initial
     * capacity of the calendar. If surplus is detected, the method returns a
     * list of intervals with violations (conflict entries).
     * 
     * @param start start time
     * @param end end time
     * @param units number of units
     * 
     * @return list of conflict entries
     * 
     * @throws Exception
     */
    public synchronized AvailableResources[] release(final long start, final long end, final ResourceVector vector) throws Exception
    {
        assert vector != null;

        AvailableResources u = new AvailableResources();
        u.start = start;
        u.end = end;
        u.units = 0;
        u.vector = ResourceVector.Negate(vector);

        return reserve(u, false);
    }

    /**
     * Releases conditionally the specified units to the list. The units are
     * released only if the calendar still covers a fraction of the specified
     * interval. This method verifies if the calendar results in a deficit in a
     * period of time. If deficit is detected, the method returns a list of
     * intervals with violations.
     * 
     * @param start start time
     * @param end end time
     * @param units number of units
     * 
     * @return list of conflict entries
     * 
     * @throws Exception
     */
    public synchronized AvailableResources[] releaseConditional(final long start, final long end, final int units) throws Exception
    {
        assert units >= 0;

        long mystart = start;
        long listStart = getStart();

        if (listStart > end) {
            // releasing is not necessary
            return null;
        }

        if (mystart < listStart) {
            mystart = listStart;
        }

        AvailableResources u = new AvailableResources();
        u.start = mystart;
        u.end = end;
        u.units = -units;
        u.vector = ResourceVector.Zero(myVector);

        return reserve(u, false);
    }

    /**
     * Releases conditionally the specified resource vector to the list. The
     * units are released only if the calendar still covers a fraction of the
     * specified interval. This method verifies if the calendar results in a
     * deficit in a period of time. If deficit is detected, the method returns a
     * list of intervals with violations.
     * 
     * @param start start time
     * @param end end time
     * @param units number of units
     * 
     * @return list of conflict entries
     * 
     * @throws Exception
     */
    public synchronized AvailableResources[] releaseConditional(final long start, final long end, final ResourceVector vector) throws Exception
    {
        assert vector != null;

        long mystart = start;
        long listStart = getStart();

        if (listStart > end) {
            // releasing is not necessary
            return null;
        }

        if (mystart < listStart) {
            mystart = listStart;
        }

        AvailableResources u = new AvailableResources();
        u.start = mystart;
        u.end = end;
        u.units = 0;
        u.vector = ResourceVector.Negate(vector);

        return reserve(u, false);
    }

    /**
     * Reserves/releases units/resource vector from/to the calendar. A single
     * operation can either reserve a number of units or a resource vector but
     * not both.
     * 
     * @param units units to reserve/release
     * 
     * @return a list of conflict entries
     * 
     * @throws Exception
     */
    protected AvailableResources[] reserve(final AvailableResources units, boolean isReserving) throws Exception
    {
        assert units != null;
        assert units.vector != null;

        List<AvailableResources> result = null;

        int index = 0;

        // XXX: can do a binary search to find the start index

        // int size = list.size();
        for (; index < list.size(); index++) {
            boolean negative = false;
            AvailableResources entry = list.get(index);

            if ((units.end < entry.start) || (units.start > entry.end)) {
                // no overlap
                continue;
            }

            if (units.start < entry.start) {
                throw new Exception("This case should have never happened");
            }

            /*
             * We have some overlap. The resulting units in the overlap region
             * will be entry.units - units.units. Check to ensure that the
             * resulting balance is not negative (in case of subtracting) or
             * larger than myUnits (in case of addition)
             */
            if (isReserving) {
                // we are reserving

                if (units.vector == null) {
                    // check the units
                    if ((entry.units - units.units) < 0) {
                        negative = true;
                    }
                } else {
                    // check the resource vector
                    if (entry.vector.willHaveNegativeDimension(units.vector)) {
                        negative = true;
                    }
                }

            } else {
                // we are releasing
                if (units.vector == null) {
                    // check the units
                    if ((entry.units - units.units) > myUnits) {
                        negative = true;
                    }
                } else {
                    // check the resource vector
                    if (entry.vector.willOverflowOnSubtract(units.vector, myVector)) {
                        negative = true;
                    }
                }
            }

            if (negative) {
                if (result == null) {
                    result = new LinkedList<AvailableResources>();
                }
            }

            if (units.end < entry.end) {
                if (units.start > entry.start) {
                    // [*****entry********]
                    // ----> [units]
                    // ============
                    // [left][units][entry]
                    AvailableResources left = new AvailableResources();
                    left.start = entry.start;
                    left.end = units.start - 1;
                    left.units = entry.units;
                    left.vector = new ResourceVector(entry.vector);

                    entry.start = units.end + 1;

                    units.units = entry.units - units.units;
                    units.vector.subtractMeFromAndUpdateMe(entry.vector);

                    list.add(index, units);
                    list.add(index, left);
                } else {
                    // [*****entry**]
                    // [units]
                    // ============
                    // [units][entry]
                    entry.start = units.end + 1;
                    units.units = entry.units - units.units;
                    units.vector.subtractMeFromAndUpdateMe(entry.vector);

                    list.add(index, units);
                }

                if (negative) {
                    AvailableResources conflict = new AvailableResources(units.start, units.end, units.units, units.vector);
                    result.add(conflict);
                    negative = false;
                }

                // we are done
                break;
            } else {
                if (units.end == entry.end) {
                    if (units.start == entry.start) {
                        // [entry]
                        // [units]
                        // ========
                        // [entry]
                        entry.units -= units.units;
                        entry.vector.subtract(units.vector);
                    } else {
                        // [*entry******]
                        // -----> [units]
                        // ======
                        // [units][entry]
                        long temp = units.start;
                        long temp2 = units.units;
                        units.end = units.start - 1;
                        units.start = entry.start;
                        units.units = entry.units;
                        ResourceVector rv = new ResourceVector(entry.vector);

                        entry.start = temp;
                        entry.units -= temp2;
                        entry.vector.subtract(units.vector);
                        units.vector = rv;

                        list.add(index, units);
                    }

                    if (negative) {
                        AvailableResources conflict = new AvailableResources(entry.start, entry.end, entry.units, entry.vector);
                        result.add(conflict);
                        negative = false;
                    }

                    break;
                } else {
                    if (units.start == entry.start) {
                        // [entry]
                        // [units*******]
                        // ============
                        // [entry][units]
                        entry.units -= units.units;
                        entry.vector.subtract(units.vector);
                        units.start = entry.end + 1;

                        if (negative) {
                            AvailableResources conflict = new AvailableResources(entry.start, entry.end, entry.units, entry.vector);
                            result.add(conflict);
                            negative = false;
                        }
                    } else {
                        // [entry********]
                        // -----> [units********]
                        // =============
                        // [entry][middle][units]
                        AvailableResources middle = new AvailableResources();
                        middle.start = units.start;
                        middle.end = entry.end;
                        middle.units = entry.units - units.units;
                        middle.vector = new ResourceVector(entry.vector);
                        middle.vector.subtract(units.vector);

                        entry.end = units.start - 1;

                        units.start = middle.end + 1;

                        list.add(index + 1, middle);

                        if (negative) {
                            AvailableResources conflict = new AvailableResources(middle.start, middle.end, middle.units, entry.vector);
                            result.add(conflict);
                            negative = false;
                        }
                    }
                }
            }
        }

        if (result != null) {
            return (AvailableResources[]) result.toArray();
        }
        return new AvailableResources[0];
    }

    /**
     * Reserves the given number of units for the time interval. This method
     * verifies if the calendar results in a deficit in a period of time. If
     * deficit is detected, the method returns a list of intervals with
     * violations (conflict entries).
     * 
     * @param start start time
     * @param end end time
     * @param units number of units.
     * 
     * @return list of conflict entries
     * 
     * @throws Exception
     */
    public synchronized AvailableResources[] reserve(final long start, final long end, final int units) throws Exception
    {
        assert units >= 0;

        AvailableResources u = new AvailableResources();
        u.start = start;
        u.end = end;
        u.units = units;
        u.vector = ResourceVector.Zero(myVector);
        return reserve(u, true);
    }

    /**
     * Reserves the given number of units for the time interval. This method
     * verifies if the calendar results in a deficit in a period of time. If
     * deficit is detected, the method returns a list of intervals with
     * violations (conflict entries).
     * 
     * @param start start time
     * @param end end time
     * @param vector resource vector
     * 
     * @return list of conflict entries
     * 
     * @throws Exception
     */
    public synchronized AvailableResources[] reserve(final long start, final long end, final ResourceVector vector) throws Exception
    {
        assert vector != null;

        AvailableResources u = new AvailableResources();
        u.start = start;
        u.end = end;
        u.units = 0;
        u.vector = new ResourceVector(vector);
        return reserve(u, true);
    }

    /**
     * Notifies the calendar of the passage of time. The calendar will retain
     * only information that represents periods after the specified time.
     * 
     * @param time current time
     */
    public synchronized void tick(final long time)
    {
        while (list.size() > 0) {
            AvailableResources entry = list.get(0);

            if (entry.end <= time) {
                list.remove(0);
            } else {
                break;
            }
        }
    }
}
