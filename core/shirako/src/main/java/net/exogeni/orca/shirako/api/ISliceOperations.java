/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.shirako.api;

import net.exogeni.orca.shirako.common.SliceID;


/**
 * <code>ISliceOperations</code> defines a common set of management operations
 * for slices. This interface is implemented by each Shirako actor.
 */
public interface ISliceOperations
{
    /**
     * Returns all client slices registered with the actor.
     *
     * @return an array of client slices
     */
    public ISlice[] getClientSlices();

    /**
     * Returns all inventory slices registered with the actor.
     *
     * @return an array of inventory slices
     */
    public ISlice[] getInventorySlices();

    /**
     * Returns the slice with the given name.
     *
     * @param sliceID slice identifier
     *
     * @return the slice
     */
    public ISlice getSlice(SliceID sliceID);

    /**
     * Returns all slices registered with the actor.
     *
     * @return an array of all slices
     */
    public ISlice[] getSlices();

    /**
     * Registers the slice with the actor. The slice must be a newly
     * created one without a database record. If the slice is a
     * recovered/previously unregistered one use #reregisterSlice(Slice)
     * instead.
     *
     * @param slice slice to register
     *
     * @throws Exception if a slice with the same name has already been
     *         registered
     */
    public void registerSlice(ISlice slice) throws Exception;

    /**
     * Removes the specified slice. Purges slice-related state from the
     * database.
     *
     * @param slice slice to remove
     *
     * @throws Exception in case of error
     */
    public void removeSlice(ISlice slice) throws Exception;

    /**
     * Removes the specified slice. Purges slice-related state from the
     * database.
     *
     * @param sliceID slice identifier
     *
     * @throws Exception in case of error
     */
    public void removeSlice(SliceID sliceID) throws Exception;

    /**
     * Re-registers the slice with the actor. The slice must already
     * have a database record.
     *
     * @param slice slice to register
     *
     * @throws Exception if a slice with the same name has already been
     *         registered
     */
    public void reregisterSlice(ISlice slice) throws Exception;

    /**
     * Unregisters the slice. Does not purge slice-related state from
     * the database.
     *
     * @param slice slice to unregister
     *
     * @throws Exception if the specified slice is not registered
     */
    public void unregisterSlice(ISlice slice) throws Exception;

    /**
     * Unregisters the specified slice. Does not purge slice-related
     * state from the database.
     *
     * @param sliceID slice identifier
     *
     * @throws Exception if the specified slice is not registered
     */
    public void unregisterSlice(SliceID sliceID) throws Exception;
    
    public void closeSliceReservations(SliceID sliceID) throws Exception;
}
