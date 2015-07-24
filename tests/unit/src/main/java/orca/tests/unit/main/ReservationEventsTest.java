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

import orca.shirako.api.IActor;
import orca.shirako.common.ReservationState;
import orca.shirako.kernel.ReservationStates;

import orca.tests.core.ShirakoTest;

import orca.util.PropList;

import java.util.HashMap;


/**
 * This class tests the processing of a single reservation. Execute it using the
 * following parameters:
 * <p>
 * <b>&lt;config file&gt; do.not.recover=true manual=false mode=[sm|broker|site]
 * state=state,pending,joining</b>
 * </p>
 */
public class ReservationEventsTest extends ShirakoTest
{
    public static final String PropertyState = "state";
    public static final String PropertyMode = "mode";

    /**
     * Lease length (in cycles).
     */
    public static final int LeaseLength = 30;

    /**
     * Number of units to request.
     */
    public static final int Units = 2;

    public static void main(String[] args) throws Exception
    {
        if (args.length > 0) {
            ShirakoTest test = new ReservationEventsTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }

    protected ReservationState state;
    protected int mode;
    protected HashMap<String, Integer> mapState;
    protected HashMap<String, Integer> mapPending;
    protected HashMap<String, Integer> mapJoining;

    public ReservationEventsTest(String[] args)
    {
        super(args);
        mapState = new HashMap<String, Integer>();
        mapPending = new HashMap<String, Integer>();
        mapJoining = new HashMap<String, Integer>();
        populateMaps();
    }

    private ReservationState parseState(String str) throws Exception
    {
        String[] temp = str.split(",");

        if (temp.length < 2) {
            throw new RuntimeException("Invalid state string");
        }

        ReservationState result = null;

        Integer state;
        Integer pending;
        state = mapState.get(temp[0]);

        if (state == null) {
            throw new RuntimeException("Invalid state name: " + temp[0]);
        }

        pending = mapPending.get(temp[1]);

        if (pending == null) {
            throw new RuntimeException("Invalid pending state name: " + temp[1]);
        }

        if (mode == IActor.TypeServiceManager) {
            if (temp.length != 3) {
                throw new RuntimeException("Missing joining state");
            }

            Integer joining = mapJoining.get(temp[2]);

            if (joining == null) {
                throw new RuntimeException("Invalid joining state name: " + temp[2]);
            }

            result = new ReservationState(state.intValue(), pending.intValue(), joining.intValue());
        } else {
            result = new ReservationState(state.intValue(), pending.intValue());
        }

        return result;
    }

    private void populateMaps()
    {
        mapState.put("nascent", new Integer(ReservationStates.Nascent));
        mapState.put("ticketed", new Integer(ReservationStates.Ticketed));
        mapState.put("active", new Integer(ReservationStates.Active));
        mapState.put("activeticketed", new Integer(ReservationStates.ActiveTicketed));
        mapState.put("closed", new Integer(ReservationStates.Closed));
        mapState.put("failed", new Integer(ReservationStates.Failed));

        mapPending.put("none", new Integer(ReservationStates.None));
        mapPending.put("ticketing", new Integer(ReservationStates.Ticketing));
        mapPending.put("redeeming", new Integer(ReservationStates.Redeeming));
        mapPending.put("extendingticket", new Integer(ReservationStates.ExtendingTicket));
        mapPending.put("extending", new Integer(ReservationStates.ExtendingLease));
        mapPending.put("closing", new Integer(ReservationStates.Closing));

        mapJoining.put("nojoin", new Integer(ReservationStates.NoJoin));
        mapJoining.put("blockedjoin", new Integer(ReservationStates.BlockedJoin));
        mapJoining.put("joining", new Integer(ReservationStates.Joining));
    }

    @Override
    protected void readParameters() throws Exception
    {
        super.readParameters();
        mode = PropList.getIntegerProperty(properties, PropertyMode);

        if ((mode < IActor.TypeServiceManager) || (mode > IActor.TypeSiteAuthority)) {
            throw new RuntimeException("Invalid mode");
        }

        String temp = properties.getProperty(PropertyState);

        if (temp == null) {
            throw new RuntimeException("No state specified");
        }

        state = parseState(temp);
    }

    @Override
    protected void runTest()
    {
        try {
            /* set the test parameters */
            ReservationEventsTestTool tester = new ReservationEventsTestTool();
            tester.setElasticTime(true);
            tester.setLeaseLength(LeaseLength);
            tester.setUnits(Units);
            tester.mode = mode;
            tester.desiredState = state;

            /* run the test */
            int code = tester.runTest();

            /* compare the exit code */
            if (ReservationTestTool.ExitCodeOK != code) {
                throw new RuntimeException("exit code: " + code);
            }
        } catch (Exception e) {
            logger.error("runTest", e);
            logger.error("Test failed: " + e.getMessage());
            System.out.println("Test failed: " + e.getMessage());
            System.exit(-1);
        }

        System.out.println("Test successful");
        logger.info("Test successful");
        System.exit(0);
    }
}