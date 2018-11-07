/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.time.calendar;

import junit.framework.TestCase;

import orca.shirako.api.IReservation;
import orca.shirako.common.ReservationID;
import orca.shirako.kernel.ClientReservationFactory;
import orca.shirako.time.ActorClock;
import orca.shirako.util.ReservationSet;


/**
 * Unit tests for <code>ClientCalendar</code>.
 */
public class ClientCalendarTest extends TestCase
{
    public static final long Offset = 1000;
    public static final long Length = 10;

    protected ClientCalendar getCalendar()
    {
        ActorClock clock = new ActorClock(Offset, Length);

        return new ClientCalendar(clock);
    }

    protected IReservation makeReservation(String id)
    {
        IReservation r = ClientReservationFactory.getInstance().create(new ReservationID(id));

        return r;
    }

    public void testCreate()
    {
        ClientCalendar cal = getCalendar();

        assertNotNull(cal.clock);
        assertNotNull(cal.demand);
        assertNotNull(cal.holdings);
        assertNotNull(cal.pending);
        assertNotNull(cal.renewing);

        assertNotNull(cal.getDemand());
        assertNotNull(cal.getHoldings());
        assertNotNull(cal.getPending());
        assertNotNull(cal.getRenewing(1000));
    }

    public void testDemand()
    {
        ClientCalendar cal = getCalendar();

        ReservationSet rset = new ReservationSet();

        for (int i = 0; i < 5; i++) {
            IReservation r = makeReservation(Integer.toString(i));
            rset.add(r);
            // add to the list
            cal.addDemand(r);

            // get the list and check it
            ReservationSet temp = cal.getDemand();
            checkSet(rset, temp);
            // remove from the returned set
            temp.remove(r);
            // make sure this did not affect the parent data structure
            temp = cal.getDemand();
            checkSet(rset, temp);
        }

        // test removal
        for (int i = 0; i < 5; i++) {
            IReservation r = makeReservation(Integer.toString(i));
            rset.remove(r);
            // add to the list
            cal.removeDemand(r);

            // get the list and check it
            ReservationSet temp = cal.getDemand();
            checkSet(rset, temp);
            // remove from the returned set
            temp.remove(r);
            // make sure this did not affect the parent data structure
            temp = cal.getDemand();
            checkSet(rset, temp);
        }
    }

    public void testPending()
    {
        ClientCalendar cal = getCalendar();

        ReservationSet rset = new ReservationSet();

        for (int i = 0; i < 5; i++) {
            IReservation r = makeReservation(Integer.toString(i));
            rset.add(r);
            // add to the list
            cal.addPending(r);

            // get the list and check it
            ReservationSet temp = cal.getPending();
            checkSet(rset, temp);
            // remove from the returned set
            temp.remove(r);
            // make sure this did not affect the parent data structure
            temp = cal.getPending();
            checkSet(rset, temp);
        }

        // test removal
        for (int i = 0; i < 5; i++) {
            IReservation r = makeReservation(Integer.toString(i));
            rset.remove(r);
            // add to the list
            cal.removePending(r);

            // get the list and check it
            ReservationSet temp = cal.getPending();
            checkSet(rset, temp);
            // remove from the returned set
            temp.remove(r);
            // make sure this did not affect the parent data structure
            temp = cal.getPending();
            checkSet(rset, temp);
        }
    }

    protected void checkSet(ReservationSet rset, ReservationSet check)
    {
        assertNotNull(check);
        assertEquals(rset.size(), check.size());

        for (IReservation res : rset) {
            assertTrue(check.contains(res));
        }
    }
}