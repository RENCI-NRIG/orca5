/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;


/**
 * RecyclePool is a utility class for use in name allocation. It manages a pool
 * of integers in some range. Integers may be allocated and released. On
 * release, the integer is put on a free list for recycling. This version
 * attempts to keep the space compact by recycling before using a fresh element.
 */
public class RecyclePool
{
    private long min;
    private long max; // numbers in range [min..max]
    private long nextNumber; // next unused integer
    private HashSet recycled;
    private Properties props;

    public RecyclePool()
    {
        props = new Properties();
        recycled = new HashSet();
    }

    /**
     * Create a RecyclePool for numbers in range [min...max].
     * @param min lowest number to allocate
     * @param max highest number to allocate
     */
    public RecyclePool(long min, long max)
    {
        this.min = min;
        this.max = max;
        nextNumber = min;
        recycled = new HashSet();
        props = new Properties();
    }

    /**
     * Allocate a unique number. DENSE.
     * @return reserved number
     */

    /*
     * public long allocate() throws Exception { long number = 0; if
     * (recycled.isEmpty()) { if (nextNumber > max) error("overflow: no free
     * identifiers"); number = nextNumber; nextNumber += 1; } else { Iterator i =
     * recycled.iterator(); assert i.hasNext(); Long mine = (Long)i.next();
     * number = mine.longValue(); i.remove(); } return number; }
     */

    /**
     * Allocate a unique number. SPARSE, with delayed recycling.
     * @return reserved number
     * @throws Exception in case of error
     */
    public long allocate() throws Exception
    {
        long number = 0;

        // XXX: The line below used to read:
        //if (nextNumber <= max) {
        // This has shown to be an off by one error.
        // May need further testing to confirm.
        if (nextNumber < max) {
            number = nextNumber;
            nextNumber += 1;
        } else {
            if (recycled.isEmpty()) {
                error("overflow: no free identifiers");
            }

            Iterator i = recycled.iterator();
            assert i.hasNext();

            Long mine = (Long) i.next();
            number = mine.longValue();
            i.remove();
        }

        return number;
    }

    private void error(String s) throws Exception
    {
        throw new Exception("RecyclePool: " + s);
    }

    /**
     * Release a previously allocated number.
     * @param number released number
     * @throws Exception in case of error
     */
    public void release(long number) throws Exception
    {
        if (number > nextNumber) {
            error("internal error: out-of-range release");
        }

        boolean unique = recycled.add(new Long(number));

        if (!unique) {
            error("internal error: double release");
        }
    }

    /**
     * Indicate that a number is reserved. For use in recovery.
     * @param number reserved number
     * @throws Exception in case of error
     */
    public void reserve(long number) throws Exception
    {
        if (number > max) {
            //XXX On recovery we are out of range since we do not re-reserve closing nodes
            // We do not want to necessarily fail here.  Just log for now.
            //	error("internal error: out-of-range reserve");
        }

        if (number == nextNumber) {
            nextNumber += 1;
        } else if (number < nextNumber) {
            Long numb = new Long(number);
            boolean removed = recycled.remove(numb);

            if (!removed) {
                error("internal error: reserving reserved identifier");
            }
        } else if (number > nextNumber) {
            for (; nextNumber < number; nextNumber++) {
                release(nextNumber);
            }

            nextNumber += 1;
        }
    }

    /**
     * Indicate that all unused numbers in the range are reserved.
     * @throws Exception in case of error
     */
    public void reserveAll() throws Exception
    {
        nextNumber = max;
    }

    public void reset(String s) throws Exception
    {
        ByteArrayInputStream b = new ByteArrayInputStream(s.getBytes());
        props.load(b);
        min = Long.parseLong(props.getProperty("min"));
        max = Long.parseLong(props.getProperty("max"));
        nextNumber = min;

        return;
    }

    /*
     * Not sure why this save/reset is needed, and why revisit machinery isn't
     * sufficient. XXX -- Chase
     */
    public String save() throws Exception
    {
        props.setProperty("min", Long.toString(min));
        props.setProperty("max", Long.toString(max));

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        props.store(b, null);

        return b.toString();
    }
}
