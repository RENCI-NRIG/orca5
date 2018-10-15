/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.tests.unit.main;

/**
 * Runs a set of default tests for a reservation.
 */
public class DefaultReservationTester {
    /**
     * Lease length (in cycles).
     */
    protected int leaseLength = 30;

    /**
     * Number of units to request.
     */
    protected int units = 1;

    /**
     * Runs a test.
     * 
     * @throws Exception
     */
    public void runTest() throws Exception {
        /* set the test parameters */
        ReservationTestTool tester = new ReservationTestTool();
        tester.setElasticTime(true);
        tester.setLeaseLength(leaseLength);
        tester.setUnits(units);

        /* run the test */
        int code = tester.runTest();

        /* compare the exit code */
        if (ReservationTestTool.ExitCodeOK != code) {
            throw new RuntimeException("exit code: " + code);
        }
    }

    public void setLeaseLength(int leaseLength) {
        this.leaseLength = leaseLength;
    }

    public void setUnits(int units) {
        this.units = units;
    }
}