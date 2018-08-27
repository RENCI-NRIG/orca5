/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.policy.core.util;

import orca.util.ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;


/**
 * Sorted in ascending order
 * @author grit
 */
public class ResourceTable
{
    /**
     * List of all of the resources in this table
     */
    protected ArrayList list;
    protected Hashtable table;

    /**
     * Inventory this table is derived from
     */
    protected LogicalInventory inventory;

    public ResourceTable()
    {
        list = new ArrayList();
        table = new Hashtable();
    }

    /**
     * Remove the entry from the linked list
     * @param entry
     */
    private void removeFromList(ResourceEntry entry)
    {
        // This binary search does not seam to work
        // int index = Collections.binarySearch(list, entry);
        // if (index >= 0) {
        // list.remove(index);
        // }
        boolean located = list.remove(entry);
        assert located;
    }

    /**
     * Add an entry to the linked list
     * @param entry
     */
    private void addToList(ResourceEntry entry)
    {
        int index = Collections.binarySearch(list, entry);

        if (index < 0) {
            index = -index - 1;
        }

        list.add(index, entry);
    }

    /**
     * Sort the entry to its new position in the list
     * @param entry entry
     */
    public void sort(ResourceEntry entry)
    {
        removeFromList(entry);
        addToList(entry);
    }

    public int size()
    {
        return list.size();
    }

    public void add(ResourceEntry entry)
    {
        addToList(entry);
        table.put(entry.getId(), entry);
    }

    /**
     * Return the ResourceEntry at index
     * @param index index
     * @return ResourceEntry at index
     */
    public ResourceEntry get(int index)
    {
        return (ResourceEntry) list.get(index);
    }

    public ResourceEntry get(ID id)
    {
        return (ResourceEntry) table.get(id);
    }

    /**
     * Clear the table
     */
    public void clear()
    {
        list.clear();
    }

    public void setLogicalInventory(LogicalInventory inventory)
    {
        this.inventory = inventory;
    }

    public LogicalInventory getLogicalInventory()
    {
        return inventory;
    }
}
