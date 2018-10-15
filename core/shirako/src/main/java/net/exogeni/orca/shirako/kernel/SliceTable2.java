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

import java.util.HashMap;

import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.util.OrcaException;


class SliceTable2
{
    protected HashMap<SliceID, IKernelSlice> slices;
    protected HashMap<SliceID, IKernelSlice> inventorySlices;
    protected HashMap<SliceID, IKernelSlice> clientSlices;
    protected HashMap<SliceID, IKernelSlice> brokerClientSlices;

    /**
     * A map of sets of slices indexed by name. Name is not guaranteed
     * to be unique per slice.
     */
    protected HashMap<String, HashMap<SliceID, IKernelSlice>> slicesByName;

    /**
         * Creates a new instance.
         */
    public SliceTable2()
    {
        slices = new HashMap<SliceID, IKernelSlice>();
        inventorySlices = new HashMap<SliceID, IKernelSlice>();
        clientSlices = new HashMap<SliceID, IKernelSlice>();
        brokerClientSlices = new HashMap<SliceID, IKernelSlice>();
        slicesByName = new HashMap<String, HashMap<SliceID, IKernelSlice>>();
    }

    /**
     * Adds the given slice to the slice table.
     *
     * @param slice slice to add
     *
     * @throws Exception if the slice is invalid
     * @throws Exception if the slice is already present in the table
     */
    public synchronized void add(IKernelSlice slice) throws OrcaException
    {
        if (slice == null) {
            throw new IllegalArgumentException();
        }

        if (slice.isInventory()) {
            add(slice, inventorySlices);
        } else if (slice.isBrokerClient()) {
            add(slice, brokerClientSlices);
        } else if (slice.isClient()) {
            add(slice, clientSlices);
        } else {
            throw new RuntimeException("Unsupported slice type");
        }
    }

    protected void add(IKernelSlice slice, HashMap<SliceID, IKernelSlice> map)
                throws OrcaException
    {
        if ((slice.getSliceID() == null) || (slice.getName() == null)) {
            throw new IllegalArgumentException();
        }

        /* index by sliceID */
        if (slices.containsKey(slice.getSliceID())) {
            throw new OrcaException("already registered");
        }

        slices.put(slice.getSliceID(), slice);
        map.put(slice.getSliceID(), slice);

        /* index by name */
        HashMap<SliceID, IKernelSlice> entry = slicesByName.get(slice.getName());

        if (entry == null) {
            entry = new HashMap<SliceID, IKernelSlice>();
            slicesByName.put(slice.getName(), entry);
        }

        entry.put(slice.getSliceID(), slice);
    }

    /**
     * Checks if the specified slice is contained in the table.
     *
     * @param sliceID identifier of slice to check
     *
     * @return true if the slice is contained in the table, false otherwise
     */
    public synchronized boolean contains(SliceID sliceID)
    {
        return slices.containsKey(sliceID);
    }

    /**
     * Returns the specified slice.
     *
     * @param sliceID identifier of slice to return
     *
     * @return slice or null if the slice is not present in the table
     */
    public synchronized IKernelSlice get(SliceID sliceID)
    {
        if (sliceID == null) {
            throw new IllegalArgumentException();
        }

        return slices.get(sliceID);
    }

    /**
     * Returns all slices with the given name.
     *
     * @param sliceName slice name
     *
     * @return an array of slices with the given name
     */
    public synchronized IKernelSlice[] get(String sliceName)
    {
        IKernelSlice[] result = null;
        HashMap<SliceID, IKernelSlice> entry = slicesByName.get(sliceName);

        if (entry == null) {
            result = new IKernelSlice[0];
        } else {
            result = new IKernelSlice[entry.size()];

            int i = 0;

            for (IKernelSlice slice : entry.values()) {
                result[i++] = slice;
            }
        }

        return result;
    }

    /**
     * Returns all broker client slices in the table.
     *
     * @return an array of broker client slices
     */
    public synchronized IKernelSlice[] getBrokerClientSlices()
    {
        IKernelSlice[] result = new IKernelSlice[brokerClientSlices.size()];
        int i = 0;

        for (IKernelSlice slice : brokerClientSlices.values()) {
            result[i++] = slice;
        }

        return result;
    }

    /**
     * Returns all client slices in the table.
     *
     * @return an array of client slices
     */
    public synchronized IKernelSlice[] getClientSlices()
    {
        IKernelSlice[] result = new IKernelSlice[clientSlices.size() + brokerClientSlices.size()];
        int i = 0;

        for (IKernelSlice slice : clientSlices.values()) {
            result[i++] = slice;
        }

        for (IKernelSlice slice : brokerClientSlices.values()) {
            result[i++] = slice;
        }

        return result;
    }

    /**
     * Returns the specified slice or throws an exception if the slice
     * is not present in the table.
     *
     * @param sliceID identifier of slice to return
     *
     * @return slice
     *
     * @throws Exception if the slice is not present in the table
     */
    public synchronized IKernelSlice getException(SliceID sliceID) throws Exception
    {
        IKernelSlice result = get(sliceID);

        if (result == null) {
            throw new Exception("not registered");
        }

        return result;
    }

    /**
     * Returns all inventory slices in the table.
     *
     * @return an array of inventory slices
     */
    public synchronized IKernelSlice[] getInventorySlices()
    {
        IKernelSlice[] result = new IKernelSlice[inventorySlices.size()];
        int i = 0;

        for (IKernelSlice slice : inventorySlices.values()) {
            result[i++] = slice;
        }

        return result;
    }

    /**
     * Returns all slices in the table.
     *
     * @return an array of slices
     */
    public synchronized IKernelSlice[] getSlices()
    {
        IKernelSlice[] result = new IKernelSlice[slices.size()];
        int i = 0;

        for (IKernelSlice slice : slices.values()) {
            result[i++] = slice;
        }

        return result;
    }

    /**
     * Removes the specified slice.
     *
     * @param sliceID identifier of slice to remove
     *
     * @throws Exception if the specified slice is not in the table
     */
    public synchronized void remove(SliceID sliceID) throws OrcaException
    {
        if (sliceID == null) {
            throw new IllegalArgumentException();
        }

        IKernelSlice slice = slices.remove(sliceID);

        if (slice == null) {
            throw new OrcaException("not registered");
        }

        if (slice.isInventory()) {
            inventorySlices.remove(sliceID);
        } else if (slice.isBrokerClient()) {
            brokerClientSlices.remove(sliceID);
        } else if (slice.isClient()) {
            clientSlices.remove(sliceID);
        } else {
            throw new RuntimeException("Unsupported slice type");
        }

        HashMap<SliceID, IKernelSlice> entry = slicesByName.get(slice.getName());

        if (entry == null) {
            throw new RuntimeException("entry is null");
        }

        entry.remove(sliceID);

        if (entry.size() == 0) {
            slicesByName.remove(slice.getName());
        }
    }
}
