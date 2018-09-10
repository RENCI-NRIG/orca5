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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.kernel.ClientReservationFactory;

import java.util.Iterator;


/**
 * Unit tests for <code>ReservationList</code>.
 */
public class ReservationListTest extends TestCase
{
    public void testCreate()
    {
        ReservationList list = new ReservationList();

        assertEquals(list.size(), 0);
        assertNotNull(list.list);
        assertNotNull(list.map);
        assertEquals(list.count, 0);
    }

    private IReservation makeReservation(String id)
    {
        IReservation r = ClientReservationFactory.getInstance().create(new ReservationID(id));

        return r;
    }

    public void testAddReservation()
    {
        ReservationList list = new ReservationList();

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                int c = (i * 10) + j;
                IReservation r = makeReservation(Integer.toString(c));
                list.addReservation(r, i);
                assertEquals(c + 1, list.size());
            }
        }
    }

    public void testGetReservations()
    {
        ReservationList list = new ReservationList();

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                int c = (i * 10) + j;
                IReservation r = makeReservation(Integer.toString(c));
                list.addReservation(r, i);
                assertEquals(c + 1, list.size());
            }
        }

        for (long i = 0; i < 10; i++) {
            int low = (int) (10 * i);
            int high = (int) (10 * (i + 1));

            ReservationSet set = list.getReservations(i);
            assertNotNull(set);
            assertEquals(10, set.size());

            Iterator<?> iter = set.iterator();

            while (iter.hasNext()) {
                IReservation r = (IReservation) iter.next();
                int value = Integer.parseInt(r.getReservationID().toString());
                // System.out.println(value);
                assertTrue((value >= low) && (value < high));
            }
        }

        ReservationSet set = list.getReservations(10);
        assertNotNull(set);
        assertEquals(0, set.size());
    }

    public void testTick()
    {
        ReservationList list = new ReservationList();

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                int c = (i * 10) + j;
                IReservation r = makeReservation(Integer.toString(c));
                list.addReservation(r, i);
                assertEquals(c + 1, list.size());
                assertEquals(c + 1, list.reservationToCycle.size());
            }
        }

        for (long i = 0; i < 10; i++) {
            ReservationSet set = list.getReservations(i);
            assertNotNull(set);
            assertEquals(10, set.size());

            int expectedSize = (10 - (int) (i)) * 10;
            assertEquals(expectedSize, list.size());
            list.tick(i);

            set = list.getReservations(i);

            assertNotNull(set);
            assertEquals(0, set.size());
        }

        assertEquals(0, list.size());
        assertEquals(0, list.reservationToCycle.size());
    }

    public void testRemoveReservation() throws Exception
    {
        ReservationList list = new ReservationList();

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                int c = (i * 10) + j;
                IReservation r = makeReservation(Integer.toString(c));
                list.addReservation(r, i);
                assertEquals(c + 1, list.size());
                assertEquals(c + 1, list.reservationToCycle.size());
            }
        }

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                int c = (i * 10) + j;

                IReservation r = makeReservation(Integer.toString(c));
                boolean exists = checkExists(list, r, (long) i);
                assertTrue(exists);
                list.removeReservation(r);
                exists = checkExists(list, r, (long) i);
                assertTrue(!exists);
            }
        }
    }

    public void testRemoveIterate()
    {
        ReservationList list = new ReservationList();

        for (int i = 0; i < 10; i++) {
            int c = i;
            IReservation r = makeReservation(Integer.toString(c));
            list.addReservation(r, i);
            assertEquals(c + 1, list.size());
        }

        ReservationSet set = list.getReservations(0);
        Iterator<?> iter = set.iterator();

        while (iter.hasNext()) {
            IReservation r = (IReservation) iter.next();
            iter.remove();
        }
    }

    private boolean checkExists(ReservationList list, IReservation r, long cycle)
    {
        ReservationSet set = list.getReservations(cycle);

        if (set.size() == 0) {
            return false;
        }

        return set.contains(r.getReservationID());
    }

    public static Test suite()
    {
        return new TestSuite(ReservationListTest.class);
    }
}
