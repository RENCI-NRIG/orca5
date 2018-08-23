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

import orca.shirako.api.IReservation;
import orca.shirako.common.ReservationID;

import orca.util.PropList;
import orca.util.persistence.PersistenceUtils;

import java.util.Properties;


public class ReservationFactory
{
    /**
     * Creates and initializes a new reservation from a saved
     * properties list.
     *
     * @param p properties list
     *
     * @return reservation instance
     *
     * @throws Exception in case of error
     */
    public static IReservation createInstance(Properties p) throws Exception
    {
    	return PersistenceUtils.restore(p);
    }

    /**
     * Extracts the reservation category embedded in the properties
     * list
     *
     * @param p properties list
     *
     * @return reservation category or -1 if an error occurs
     */
    public static int getCategory(Properties p)
    {
        int code = -1;

        if (p != null) {
            try {
                code = PropList.getIntegerProperty(p, IReservation.PropertyCategory);
            } catch (Exception e) {
            }
        }

        return code;
    }

    /**
     * Extracts the reservation identifier from the properties list.
     *
     * @param p properties list
     *
     * @return reservation identifier
     *
     * @throws Exception if the properties list does not contain a reservation
     *         identifier
     */
    public static ReservationID getReservationID(final Properties p) throws Exception
    {
        return new ReservationID(PropList.getRequiredProperty(p, IReservation.PropertyID));
    }

    /**
     * Extracts the slice name from the properties list.
     *
     * @param p properties list
     *
     * @return slice name
     *
     * @throws Exception if the properties list does not contain a slice name
     */
    public static String getSliceName(Properties p) throws Exception
    {
        String name = PropList.getRequiredProperty(p, IReservation.PropertySlice);

        return name;
    }
}
