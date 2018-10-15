/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.kernel;

import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.security.Guard;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.util.ReservationSet;
import net.exogeni.orca.util.OrcaException;


/**
 * Kernel-level interface for slice objects.
 */
public interface IKernelSlice extends ISlice
{
    /**
     * Returns the slice guard.
     *
     * @return the guard
     */
    public Guard getGuard();

    /**
     * Marks the slice as a broker client slice (a client slice within
     * an authority that represents a broker).
     */
    public void setBrokerClient();

    /**
     * Marks the slice as a client slice.
     */
    public void setClient();
    
    /**
     * Returns the reservation set.
     *
     * @return reservation set
     */
    public ReservationSet getReservations();

    /**
     * Returns the reservation set represented as an array. Must be
     * called with the kernel lock on to prevent exceptions due to concurrent
     * iteration.
     *
     * @return an array of reservations included in the array
     */
    public IKernelReservation[] getReservationsArray();

    /**
     * Checks if the slice is empty.
     *
     * @return true if there are no reservations in the slice
     */
    public boolean isEmpty();

    /**
     * Prepares to register a new slice.  Clears previous state, such
     * as list of reservations in the slice.
     *
     * @throws OrcaException if validity checks fail
     */
    public void prepare() throws OrcaException;

    /**
     * Registers a new reservation.
     *
     * @param reservation reservation to register
     *
     * @throws OrcaException in case of error
     */
    public void register(IKernelReservation reservation) throws Exception;

    /**
     * Looks up a reservation by ID but does not throw error if the
     * reservation is not present in the slice.
     *
     * @param rid the reservation ID
     *
     * @return the reservation with that ID
     *
     */
    public IKernelReservation softLookup(ReservationID rid);

    /**
     * Unregisters the reservation from the slice.
     *
     * @param reservation reservation to unregister
     */
    public void unregister(IKernelReservation reservation);
    
    /**
     * Sets the slice owner.
     *
     * @param auth the slice owner
     */
    public void setOwner(AuthToken auth);
}
