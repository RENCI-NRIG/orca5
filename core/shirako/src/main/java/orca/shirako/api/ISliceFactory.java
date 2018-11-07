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

import orca.shirako.common.SliceID;
import orca.shirako.util.ResourceData;


/**
 * Factory for slice objects.
 */
public interface ISliceFactory
{
    /**
     * Creates a new slice with the specified slice id and name.
     *
     * @param sliceID slice id
     * @param name slice name
     *
     * @return slice object
     */
    public ISlice create(SliceID sliceID, String name);

    /**
     * Creates a new slice with the specified id, name, and resource
     * properties.
     *
     * @param sliceID slice id
     * @param name slice name
     * @param data slice properties
     *
     * @return slice object
     */
    public ISlice create(SliceID sliceID, String name, ResourceData data);

    /**
     * Creates a new slice with the specified name.
     *
     * @param sliceName slice name
     *
     * @return slice object
     */
    public ISlice create(String sliceName);

    /**
     * Creates a new slice with the specified name and resource
     * properties.
     *
     * @param name slice name
     * @param data slice properties
     *
     * @return slice object
     */
    public ISlice create(String name, ResourceData data);
}