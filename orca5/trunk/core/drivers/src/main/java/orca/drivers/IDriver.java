/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.drivers;

import java.util.Properties;


/**
 * Base interface for all resource drivers
 */
public interface IDriver
{
    /**
     * Returns the unique driver identifier.
     * @return
     */
    public DriverId getId();

    /**
     * Initialize the driver
     * @return
     * @throws Exception
     */
    public int initialize() throws Exception;

    /**
     * Cleanup any relevant state.
     * @return
     * @throws Exception
     */
    public int cleanup() throws Exception;

    /**
     * Main driver dispatch routine.
     * This function is the main entry point in the driver.
     * @param actionId action identifier
     * @param in Properties list describing the request
     * @param out Output properties list
     * @return
     * @throws Exception
     */
    public int dispatch(String actionId, Properties in, Properties out) throws Exception;

    /**
     * Main driver dispatch routine for objects.
     * This function is the main entry point in the driver.
     * @param actionId action identifier
     * @param in Properties list describing the request
     * @param out Output properties list
     * @return
     * @throws Exception
     */
    public int dispatch2(String objectId, String actionId, Properties in, Properties out)
                  throws Exception;

    /**
     * If the driver maintains state between calls, this method should return true.
     * @return
     */
    public boolean isStateful();

    /**
     * Sets the factory used to create this driver
     * @param factory
     */
    public void setFactory(DriverFactory factory);
}