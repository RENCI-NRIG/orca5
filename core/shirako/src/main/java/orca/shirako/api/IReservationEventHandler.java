/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.api;


/**
 * <code>IReservationEventHandler</code> makes it possible to subscribe for
 * event notification when certain reservation events take place.
 * <p>
 * <b>Note:</b>This interface is still immature and may change in the future.
 * </p>
 */
public interface IReservationEventHandler
{
    /**
     * Initializes the handler.
     *
     * @param s service manager
     */
    public void initialize(IServiceManager s);

    public void onBeforeExtendTicket(IReservation r);

    public void onClose(IReservation r);

    public void onCloseComplete(IReservation r);

    public void onExtendLease(IReservation r);

    public void onExtendLeaseComplete(IReservation r);

    public void onExtendTicket(IReservation r);

    public void onExtendTicketComplete(IReservation r);

    public void onLease(IReservation r);

    public void onLeaseComplete(IReservation r);

    public void onTicket(IReservation r);

    public void onTicketComplete(IReservation r);
}