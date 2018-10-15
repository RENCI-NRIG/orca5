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

import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.common.ReservationState;
import net.exogeni.orca.shirako.core.Actor;
import net.exogeni.orca.shirako.kernel.ReservationStates;

public class ReservationEventsTestTool extends ReservationTestTool {
    protected int mode;
    protected ReservationState desiredState;

    public ReservationEventsTestTool() {
    }

    @Override
    protected int checkFinalStates(boolean close) {
        printFinalStates();

        switch (mode) {
        case Actor.TypeServiceManager:

            if (!close) {
                if (finalStateSM.getState() != ReservationStates.Closed) {
                    return ExitCodeUnexpectedStateSM;
                }

                if (finalStateSite != null) {
                    if (finalStateSite.getState() != ReservationStates.Closed) {
                        return ExitCodeUnexpectedStateSite;
                    }
                }
            }

            break;
        }

        return ExitCodeOK;
    }

    @Override
    protected void init() throws Exception {
        super.init();
    }

    protected void printFinalStates() {
        if (finalStateSM != null) {
            System.out.println("Final state sm: " + finalStateSM.toString());
        }

        if (finalStateAgent != null) {
            System.out.println("Final state broker: " + finalStateAgent.toString());
        } else {
            System.out.println("Final state broker: no reservation");
        }

        if (finalStateSite != null) {
            System.out.println("Final state site: " + finalStateSite.toString());
        } else {
            System.out.println("Final state site: no reservation");
        }
    }

    @Override
    protected synchronized void reservationTransition(IReservation r, ReservationState from, ReservationState to) {
        super.reservationTransition(r, from, to);

        IActor actor = r.getActor();
        System.out.println(actor.getName() + " : " + getStates(from, to));

        switch (mode) {
        case Actor.TypeServiceManager:

            if (actor.getType() == Actor.TypeServiceManager) {
                if (to.equals(desiredState)) {
                    try {
                        sm.close(currentReservationSM);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            break;
        }
    }

    public int runTest() throws Exception {
        try {
            init();
            issueRequest();

            int code = monitor(false);

            if (code == ExitCodeOK) {
                if (mismatchCount > 0) {
                    code = ExitCodeUnitsError;
                }
            }

            return code;
        } finally {
            clean();
        }
    }
}