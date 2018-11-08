/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;


/**
 * This is a utility class for use in mappers: it manages a pool of resource
 * "bins" (any Object), indexed by the number of units allocated or free from
 * each bin.
 */
public class BinPool
{
    private Vector bins;
    private int ballsPerBin;

    public BinPool(int ballsPerBin)
    {
        bins = new Vector();
        this.ballsPerBin = ballsPerBin;

        /*
         * Install a hash set for bins with each possible number of free units.
         */
        for (int i = 0; i <= ballsPerBin; i++) {
            HashSet set = new HashSet();
            bins.insertElementAt(set, i);
        }
    }

    /**
     * Installs a new bin with all elements free.
     * @param o the bin
     */
    public void addBin(Object o)
    {
        /*
         * Initially all (ballsPerBin) units in the bin are free.
         */
        storeBin(o, ballsPerBin);
    }

    /**
     * Deallocates units from a bin, increasing its number of free units. Note:
     * the names of 'fill' and 'drain' concepts seem bass-ackwards. Fill
     * increases the number of units allocated from the bin, reducing the number
     * of free units. Drain reduces the number of units allocated from the bin,
     * increasing the number of free units. So we have to think of bins as
     * 'holding' the units allocated from them, and 'free' as their remaining
     * capacity. How very strange and confusing.
     * @param o the bin
     * @param balls how many units the bin has free
     * @param draining how many units being restored
     */
    public void drainBin(Object o, int balls, int draining)
    {
        int free = ballsPerBin - balls;
        extractBin(o, free);
        storeBin(o, free + draining);
    }

    /**
     * Extracts a bin from the HashSet for a given number of free elements.
     * Indicates that this bin recently had 'free' elements free, but the number
     * of free units in the bin is changing.
     * @param o the bin
     * @param free how many elements this bin previously had free
     */
    private void extractBin(Object o, int free)
    {
        HashSet set = (HashSet) bins.elementAt(free);
        boolean present = set.remove(o);
        assert present;
    }

    /**
     * Allocates units from/to a bin, reducing the number of free units.
     * @param o the bin
     * @param balls how many units the bin currently has allocated
     * @param filling how many new units being allocated
     */
    public void fillBin(Object o, int balls, int filling)
    {
        int free = ballsPerBin - balls;
        extractBin(o, free);
        storeBin(o, free - filling);
    }

    /**
     * Finds a bin with a minimum number of units free, and allocate more units
     * from (to?) it, reducing the number of free units.
     * @param balls number of units needed
     * @return the selected bin
     * @throws Exception in case of error
     */
    public Object findAndFillBin(int balls) throws Exception
    {
        Object bin = null;

        /*
         * Policy is we spread load evenly: always allocate from the bin with
         * the most capacity remaining (worst fit).
         */
        for (int i = ballsPerBin; i >= balls; i--) {
            HashSet set = (HashSet) bins.elementAt(i);
            Iterator iter = set.iterator();

            if (iter.hasNext()) {
                bin = iter.next();
                iter.remove();
                storeBin(bin, i - balls);

                break;
            }
        }

        return bin;
    }

    public int getBinSize()
    {
        return ballsPerBin;
    }

    /**
     * Stores a bin on the HashSet for a given number of free elements.
     * Indicates that this bin has 'free' elements free.
     * @param o the bin
     * @param free how many elements this bin has free
     */
    private void storeBin(Object o, int free)
    {
        HashSet set = (HashSet) bins.elementAt(free);
        boolean unique = set.add(o);
        assert unique;
    }
}
