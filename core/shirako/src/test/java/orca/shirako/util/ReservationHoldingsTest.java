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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import orca.shirako.api.IReservation;
import orca.shirako.common.ReservationID;
import orca.shirako.kernel.ClientReservationFactory;


/**
 * Unit tests for <code>ReservationHoldings</code>.
 */
public class ReservationHoldingsTest extends TestCase
{
    public void testCreate()
    {
        ReservationHoldings holdings = new ReservationHoldings();

        assertNotNull(holdings.list);
        assertNotNull(holdings.map);
        assertNotNull(holdings.set);
    }

    private IReservation makeReservation(String id)
    {
        IReservation r = ClientReservationFactory.getInstance().create(new ReservationID(id));

        return r;
    }

    private boolean checkExists(ReservationHoldings holdings, IReservation reservation)
    {
        ReservationSet set = holdings.getReservations();

        return set.contains(reservation.getReservationID());
    }

    protected void printList(ReservationHoldings holdings)
    {
        // check that the list is sorted
        for (int k = 0; k < holdings.list.size(); k++) {
            ReservationHoldings.ReservationWrapper entry = (ReservationHoldings.ReservationWrapper) holdings.list.get(
                k);
            System.out.print(
                entry.end + ":" + entry.start + ":" + entry.end + ":" +
                entry.reservation.getReservationID() + ",");
        }

        System.out.println();
    }

    public void testAddReservatoion()
    {
        ReservationHoldings holdings = new ReservationHoldings();

        long length = 5;
        long i = 0;

        while (i <= 5) {
            assertEquals((int) i, holdings.size());

            IReservation r = makeReservation(Long.toString(i));

            boolean exists = checkExists(holdings, r);
            assertFalse(exists);
            holdings.addReservation(r, 5 - i, 5 - i + length);
            exists = checkExists(holdings, r);
            assertTrue(exists);
            assertEquals((int) (i + 1), holdings.list.size());
            assertEquals((int) (i + 1), holdings.map.size());
            assertEquals((int) (i + 1), holdings.set.size());

            // printList(holdings);
            i++;
        }

        IReservation r = makeReservation(Long.toString(100));
        holdings.addReservation(r, 0, 8);

        // printList(holdings);
    }

    public void testRemoveReservation()
    {
        ReservationHoldings holdings = new ReservationHoldings();

        long length = 5;
        long i = 0;

        while (i <= 5) {
            assertEquals((int) i, holdings.size());

            IReservation r = makeReservation(Long.toString(i));

            boolean exists = checkExists(holdings, r);
            assertFalse(exists);
            holdings.addReservation(r, 5 - i, 5 - i + length);
            exists = checkExists(holdings, r);
            assertTrue(exists);
            assertEquals((int) (i + 1), holdings.list.size());
            assertEquals((int) (i + 1), holdings.map.size());
            assertEquals((int) (i + 1), holdings.set.size());

            // printList(holdings);
            i++;
        }

        IReservation r = makeReservation(Long.toString(100));
        holdings.addReservation(r, 0, 8);

        // printList(holdings);
        boolean exists = checkExists(holdings, r);
        assertTrue(exists);
        assertEquals((int) (i + 1), holdings.list.size());
        assertEquals((int) (i + 1), holdings.map.size());
        assertEquals((int) (i + 1), holdings.set.size());

        holdings.removeReservation(r);

        // printList(holdings);
        exists = checkExists(holdings, r);
        assertFalse(exists);
        assertEquals((int) (i), holdings.list.size());
        assertEquals((int) (i), holdings.map.size());
        assertEquals((int) (i), holdings.set.size());

        i = 0;

        while (i <= 5) {
            r = makeReservation(Long.toString(i));

            exists = checkExists(holdings, r);
            assertTrue(exists);
            assertEquals((int) (6 - i), holdings.list.size());
            assertEquals((int) (6 - i), holdings.map.size());
            assertEquals((int) (6 - i), holdings.set.size());

            holdings.removeReservation(r);

            // printList(holdings);
            exists = checkExists(holdings, r);
            assertFalse(exists);
            assertEquals((int) (5 - i), holdings.list.size());
            assertEquals((int) (5 - i), holdings.map.size());
            assertEquals((int) (5 - i), holdings.set.size());

            i++;
        }
    }

    public void testTick()
    {
        ReservationHoldings holdings = new ReservationHoldings();

        long length = 5;
        long i = 0;

        while (i <= 5) {
            assertEquals((int) i, holdings.size());

            IReservation r = makeReservation(Long.toString(i));

            boolean exists = checkExists(holdings, r);
            assertFalse(exists);
            holdings.addReservation(r, 5 - i, 5 - i + length);
            exists = checkExists(holdings, r);
            assertTrue(exists);
            assertEquals((int) (i + 1), holdings.list.size());
            assertEquals((int) (i + 1), holdings.map.size());
            assertEquals((int) (i + 1), holdings.set.size());

            // printList(holdings);
            i++;
        }

        IReservation r = makeReservation(Long.toString(100));
        holdings.addReservation(r, 0, 8);

        // printList(holdings);
        for (i = 0; i < 12; i++) {
            holdings.tick(i);

            int size;

            if (i < 5) {
                size = 7;
            } else {
                size = 0;

                switch ((int) i) {
                    case 5:
                        size = 6;

                        break;

                    case 6:
                        size = 5;

                        break;

                    case 7:
                        size = 4;

                        break;

                    case 8:
                        size = 2;

                        break;

                    case 9:
                        size = 1;

                        break;
                }
            }

            assertEquals(size, holdings.list.size());
            assertEquals(size, holdings.map.size());
            assertEquals(size, holdings.set.size());
        }
    }

    public void testIntersection()
    {
        ReservationHoldings holdings = new ReservationHoldings();

        long length = 5;
        long i = 0;

        while (i <= 5) {
            assertEquals((int) i, holdings.size());

            IReservation r = makeReservation(Long.toString(i));

            boolean exists = checkExists(holdings, r);
            assertFalse(exists);
            holdings.addReservation(r, 5 - i, 5 - i + length);
            exists = checkExists(holdings, r);
            assertTrue(exists);
            assertEquals((int) (i + 1), holdings.list.size());
            assertEquals((int) (i + 1), holdings.map.size());
            assertEquals((int) (i + 1), holdings.set.size());

            // printList(holdings);
            i++;
        }

        int[] results = { 1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1, 0, 0 };
        long[] points = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

        for (int j = 0; j < points.length; j++) {
            ReservationSet set = holdings.getReservations(points[j]);
            assertNotNull(set);
            assertEquals(results[j], set.size());
        }
    }

    public static Test suite()
    {
        return new TestSuite(ReservationHoldingsTest.class);
    }
}