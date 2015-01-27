/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.kernel;

import java.util.Properties;

import orca.shirako.api.ISlice;
import orca.shirako.api.ISliceFactory;
import orca.shirako.common.SliceID;
import orca.shirako.util.ResourceData;
import orca.util.persistence.PersistenceUtils;


public class SliceFactory implements ISliceFactory
{
    private static final SliceFactory instance = new SliceFactory();

    /**
     * Creates a slice object from a saved properties list.
     *
     * @param p properties list describing a previous slice object instance
     *
     * @return DOCUMENT ME!
     */
    public static IKernelSlice createInstance(Properties p) throws Exception
    {
        return PersistenceUtils.restore(p);
    }

    public static ISliceFactory getInstance()
    {
        return instance;
    }

    /**
     * Returns the slice name stored in a saved properties list.
     *
     * @param p properties list
     *
     * @return slice name
     */
    public static String getName(Properties p)
    {
        return p.getProperty(ISlice.PropertyName);
    }

    /**
     * Returns the slice identifier stored in a saved properties list.
     *
     * @param p properties list
     *
     * @return slice identifier
     */
    public static SliceID getSliceID(Properties p)
    {
        if (p.getProperty(ISlice.PropertyGuid) == null) {
            return null;
        }

        return new SliceID(p.getProperty(ISlice.PropertyGuid));
    }

    /**
     * Checks if the saved properties list represents a broker client
     * slice.
     *
     * @param p properties list
     *
     * @return true if the saved properties list represents a broker client
     *         slice.
     */
    public static boolean isBrokerClient(Properties p)
    {
        String type = p.getProperty(ISlice.PropertyType);

        if (type == null) {
            return false;
        } else {
            return type.equals(SliceTypes.BrokerClientSlice);
        }
    }

    /**
     * Checks if the saved properties list represents a client slice.
     *
     * @param p properties list
     *
     * @return true if the saved properties list represents a client slice.
     */
    public static boolean isClient(Properties p)
    {
        return !SliceFactory.isInventory(p);
    }

    /**
     * Checks if the saved properties list represents an inventory
     * slice.
     *
     * @param p properties list
     *
     * @return true if the saved properties list represents an inventory slice
     */
    public static boolean isInventory(Properties p)
    {
        String type = p.getProperty(ISlice.PropertyType);

        if (type == null) {
            return false;
        } else {
            return type.equals(SliceTypes.InventorySlice);
        }
    }

    protected SliceFactory()
    {
    }

    /**
     * {@inheritDoc}
     */
    public ISlice create(SliceID sliceID, String name)
    {
        return new Slice(sliceID, name);
    }

    /**
     * {@inheritDoc}
     */
    public ISlice create(SliceID sliceID, String name, ResourceData data)
    {
        return new Slice(sliceID, name, data);
    }

    /**
     * {@inheritDoc}
     */
    public ISlice create(String sliceName)
    {
        return new Slice(sliceName);
    }

    /**
     * {@inheritDoc}
     */
    public ISlice create(String name, ResourceData data)
    {
        return new Slice(name, data);
    }
}