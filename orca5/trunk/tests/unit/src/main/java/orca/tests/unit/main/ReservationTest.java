/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.tests.unit.main;

import orca.tests.core.ShirakoTest;

import orca.util.PropList;


/**
 * This class tests the processing of a single reservation. Execute it using the
 * following parameters:
 * <p>
 * <b>&lt;config file&gt; do.not.recover=true manual=false</b>
 * </p>
 * <p>
 * Optional arguments:
 * <ul>
 * <li>leaseLength: lease length (in cycles)</li>
 * <li>units: number of units</li>
 * </ul>
 */
public class ReservationTest extends ShirakoTest
{
    public static final String PropertyLeaseLength = "leaseLength";
    public static final String PropertyUnits = "units";

    public static void main(String[] args) throws Exception
    {
        if (args.length > 0) {
            ShirakoTest test = new ReservationTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }

    /**
     * Lease length in cycles.
     */
    protected int leaseLength = 30;

    /**
     * Number of units.
     */
    protected int units = 2;

    public ReservationTest(String[] args)
    {
        super(args);
    }

    @Override
    protected void readParameters() throws Exception
    {
        super.readParameters();

        String temp = properties.getProperty(PropertyLeaseLength);

        if (temp != null) {
            leaseLength = PropList.getIntegerProperty(properties, PropertyLeaseLength);
        }

        temp = properties.getProperty(PropertyUnits);

        if (temp != null) {
            units = PropList.getIntegerProperty(properties, PropertyUnits);
        }
    }

    @Override
    protected void runTest()
    {
        try {
            DefaultReservationTester tester = new DefaultReservationTester();
            tester.setLeaseLength(leaseLength);
            tester.setUnits(units);
            tester.runTest();
        } catch (Exception e) {
            logger.error("runTest", e);
            logger.error("Test failed.");
            System.out.println("Test failed");
            System.exit(-1);
        }

        System.out.println("Test successful");
        logger.info("Test successful");
        System.exit(0);
    }
}