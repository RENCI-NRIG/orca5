/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.core;

import orca.shirako.api.IReservation;
import orca.shirako.api.IReservationEventHandler;
import orca.shirako.api.IServiceManager;


public class EventHandler implements IReservationEventHandler
{
    protected IServiceManager sm;

    public EventHandler()
    {
    }

    public void initialize(IServiceManager s)
    {
        this.sm = s;
    }

    public void onBeforeExtendTicket(IReservation r)
    {
    }

    public void onClose(IReservation r)
    {
    }

    public void onCloseComplete(IReservation r)
    {
    }

    public void onExtendLease(IReservation r)
    {
    }

    public void onExtendLeaseComplete(IReservation r)
    {
    }

    public void onExtendTicket(IReservation r)
    {
    }

    public void onExtendTicketComplete(IReservation r)
    {
    }

    public void onLease(IReservation r)
    {
    }

    public void onLeaseComplete(IReservation r)
    {
    }

    public void onTicket(IReservation r)
    {
    }

    public void onTicketComplete(IReservation r)
    {
    }
}