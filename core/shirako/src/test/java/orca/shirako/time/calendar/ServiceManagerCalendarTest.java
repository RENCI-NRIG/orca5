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

import orca.shirako.api.IReservation;
import orca.shirako.time.ActorClock;
import orca.shirako.util.ReservationSet;


/**
 * Unit tests for <code>ClientCalendar</code>.
 */
public class ServiceManagerCalendarTest extends ClientCalendarTest
{
    @Override
    protected ClientCalendar getCalendar()
    {
        ActorClock clock = new ActorClock(Offset, Length);

        return new ServiceManagerCalendar(clock);
    }

    public void testCreate2()
    {
        ServiceManagerCalendar cal = (ServiceManagerCalendar) getCalendar();

        assertNotNull(cal.closing);
        assertNotNull(cal.redeeming);

        assertNotNull(cal.getClosing(1000));
        assertNotNull(cal.getRedeeming(1000));
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