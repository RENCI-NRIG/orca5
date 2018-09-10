/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * This class maintains a data structure to represent a calendar of units.
 * The calendar maintains information about the availability of units over a
 * period of time. Each calendar is initialized with a period and number of
 * units. The calendar then allows a number of units to be reserved for a
 * period of time. Reserved units can also be released. The calendar can be
 * notified about the passage of time, so that it can discard unnecessary
 * data.<p>This class is reentrant.</p>
 */
public class UnitsList
{
    /**
     * List of available units sorted by end time.
     */
    private ArrayList<AvailableUnits> list;

    /**
     * Total number of units.
     */
    private long myUnits;

    /**
         * Creates a new <code>UnitsList</code>.
         * @param start start time
         * @param end end time
         * @param units number of units
         */
    public UnitsList(final long start, final long end, final long units)
    {
        list = new ArrayList<AvailableUnits>();

        AvailableUnits u = new AvailableUnits();
        u.start = start;
        u.end = end;
        u.units = units;
        addToList(u);
    }

    /**
         * Copy constructor.
         * @param original calendar to copy
         */
    protected UnitsList(final UnitsList original)
    {
        this.list = new ArrayList<AvailableUnits>(original.list.size());

        for (int i = 0; i < original.list.size(); i++) {
            AvailableUnits u = original.list.get(i);
            this.list.add(i, u.copy());
        }

        this.myUnits = original.myUnits;
    }

    /**
     * Adds an element to the linked list, maintaining the list in
     * sorted order.
     *
     * @param entry element to add
     */
    protected void addToList(final AvailableUnits entry)
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
    public synchronized UnitsList copy()
    {
        return new UnitsList(this);
    }

    /**
     * Displays the contents of the calendar.
     */
    public synchronized void dump()
    {
        int size = list.size();

        System.out.println("==================");

        for (int i = 0; i < size; i++) {
            AvailableUnits item = list.get(i);
            System.out.println(item.toString());
        }
    }

    /**
     * Returns the internal interval list maintained by the calendar.
     *
     * @return interval list
     */
    public ArrayList<AvailableUnits> getList()
    {
        return list;
    }

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
            AvailableUnits entry = list.get(index);

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
     * Returns the start time of the calendar.
     *
     * @return start time
     */
    public synchronized long getStart()
    {
        long result = -1;

        if (list.size() > 0) {
            AvailableUnits entry = list.get(0);
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
     * Returns the number of units available for the specified time.
     * The result is a list of one or more intervals, each containing the
     * number of units available in the interval.
     *
     * @param start start time
     * @param end end time
     *
     * @return list of intervals representing unit availability
     */
    public synchronized List<AvailableUnits> getUnits(final long start, final long end)
    {
        // XXX: use binary search to select the initial index (will be 0 most of
        // the time)
        int index = 0;
        int size = list.size();
        ArrayList<AvailableUnits> result = new ArrayList<AvailableUnits>();

        for (; index < size; index++) {
            AvailableUnits entry = list.get(index);

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

            result.add(new AvailableUnits(s, e, entry.units));
        }

        return result;
    }

    /**
     * Releases the given number of units for the time interval. This
     * method verifies if release results in a surplus, more units that the
     * initial capacity of the calendar. If surplus is detected, the method
     * returns a list of intervals with violations (conflict entries).
     *
     * @param start start time
     * @param end end time
     * @param units number of units
     *
     * @return list of conflict entries
     *
     * @throws Exception in case of error
     */
    public synchronized List<AvailableUnits> release(final long start, final long end,
                                                     final long units) throws Exception
    {
        assert units >= 0;

        AvailableUnits u = new AvailableUnits();
        u.start = start;
        u.end = end;
        u.units = -units;

        return reserve(u);
    }

    /**
     * Releases conditionally the specified units to the list. The
     * units are release only if the calendar still covers a fraction of the
     * specified interval. This method verifies if the reservation results in
     * a deficit in a period of time contained in the calendar. If deficit is
     * detected, the method returns a list of intervals with violations
     * (conflict entries).
     *
     * @param start start time
     * @param end end time
     * @param units number of units
     *
     * @return list of conflict entries
     *
     * @throws Exception in case of error
     */
    public synchronized List<AvailableUnits> releaseConditional(final long start, final long end,
                                                                final long units)
                                                         throws Exception
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

        AvailableUnits u = new AvailableUnits();
        u.start = mystart;
        u.end = end;
        u.units = -units;

        return reserve(u);
    }

    /**
     * Reserves/releases units from/to the calendar.
     *
     * @param units units to reserve/release
     *
     * @return a list of conflict entries
     *
     * @throws Exception in case of error
     */
    protected List<AvailableUnits> reserve(final AvailableUnits units) throws Exception
    {
        List<AvailableUnits> result = null;

        int index = 0;

        // XXX: can do a binary search to find the start index

        // int size = list.size();
        for (; index < list.size(); index++) {
            boolean negative = false;
            AvailableUnits entry = list.get(index);

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
            if (units.units >= 0) {
                if ((entry.units - units.units) < 0) {
                    negative = true;
                }
            } else {
                if ((entry.units - units.units) > myUnits) {
                    negative = true;
                }
            }

            if (negative) {
                if (result == null) {
                    result = new LinkedList<AvailableUnits>();
                }
            }

            if (units.end < entry.end) {
                if (units.start > entry.start) {
                    // [*****entry********]
                    // ----> [units]
                    // ============
                    // [left][units][entry]
                    AvailableUnits left = new AvailableUnits();
                    left.start = entry.start;
                    left.end = units.start - 1;
                    left.units = entry.units;

                    entry.start = units.end + 1;

                    units.units = entry.units - units.units;

                    list.add(index, units);
                    list.add(index, left);
                } else {
                    // [*****entry**]
                    // [units]
                    // ============
                    // [units][entry]
                    entry.start = units.end + 1;
                    units.units = entry.units - units.units;
                    list.add(index, units);
                }

                if (negative) {
                    AvailableUnits conflict = new AvailableUnits(units.start, units.end, units.units);
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
                        entry.start = temp;
                        entry.units -= temp2;
                        list.add(index, units);
                    }

                    if (negative) {
                        AvailableUnits conflict = new AvailableUnits(entry.start,
                                                                     entry.end,
                                                                     entry.units);
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
                        units.start = entry.end + 1;

                        if (negative) {
                            AvailableUnits conflict = new AvailableUnits(entry.start,
                                                                         entry.end,
                                                                         entry.units);
                            result.add(conflict);
                            negative = false;
                        }
                    } else {
                        // [entry********]
                        // -----> [units********]
                        // =============
                        // [entry][middle][units]
                        AvailableUnits middle = new AvailableUnits();
                        middle.start = units.start;
                        middle.end = entry.end;
                        middle.units = entry.units - units.units;

                        entry.end = units.start - 1;

                        units.start = middle.end + 1;

                        list.add(index + 1, middle);

                        if (negative) {
                            AvailableUnits conflict = new AvailableUnits(middle.start,
                                                                         middle.end,
                                                                         middle.units);
                            result.add(conflict);
                            negative = false;
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Reserves the given number of units for the time interval. This
     * method verifies if the reservation results in a deficit in a period of
     * time contained in the calendar. If deficit is detected, the method
     * returns a list of intervals with violations (conflict entries).
     *
     * @param start start time
     * @param end end time
     * @param units number of units.
     *
     * @return list of conflict entries
     *
     * @throws Exception in case of error
     */
    public synchronized List<AvailableUnits> reserve(final long start, final long end,
                                                     final long units) throws Exception
    {
        assert units >= 0;

        AvailableUnits u = new AvailableUnits();
        u.start = start;
        u.end = end;
        u.units = units;

        return reserve(u);
    }

    /**
     * Notifies the calendar of the passage of time. The calendar will
     * retain only information that represents periods after the specified
     * time.
     *
     * @param time current time
     */
    public synchronized void tick(final long time)
    {
        while (list.size() > 0) {
            AvailableUnits entry = list.get(0);

            if (entry.end <= time) {
                list.remove(0);
            } else {
                break;
            }
        }
    }
}
