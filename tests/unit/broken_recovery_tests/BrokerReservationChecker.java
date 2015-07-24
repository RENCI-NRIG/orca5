package orca.tests.unit.recovery;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import orca.shirako.common.ReservationState;
import orca.shirako.kernel.ReservationStates;

public class BrokerReservationChecker
{
    public static final String LogFile = "states.agentreservation.log";
    public static final long LeaseLength = 15;
    public static final int Units = 2;

    protected ArrayList<StateToCheck> statesToCheck;
    protected PrintWriter writer;

    public BrokerReservationChecker()
    {
        statesToCheck = new ArrayList<StateToCheck>();

        try {
            writer = new PrintWriter(new FileWriter(LogFile));
        } catch (Exception e) {
            throw new RuntimeException("cannot create output file", e);
        }

        populate();
    }

    protected void populate()
    {
        StateToCheck state = new StateToCheck(new ReservationState(ReservationStates.Nascent, ReservationStates.None));
        state.expectedImmediate.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        statesToCheck.add(state);

        state = new StateToCheck(new ReservationState(ReservationStates.Nascent, ReservationStates.Ticketing));
        state.expectedImmediate.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        statesToCheck.add(state);

        state = new StateToCheck(new ReservationState(ReservationStates.Ticketed, ReservationStates.Priming));
        state.expectedImmediate.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Failed, ReservationStates.None, ReservationStates.NoJoin));
        statesToCheck.add(state);

        state = new StateToCheck(new ReservationState(ReservationStates.Ticketed, ReservationStates.ExtendingTicket));
        state.expectedImmediate.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Closed, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Failed, ReservationStates.None, ReservationStates.NoJoin));
        statesToCheck.add(state);

        state = new StateToCheck(new ReservationState(ReservationStates.Ticketed, ReservationStates.None));
        state.expectedImmediate.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        // if recovery takes a bit longer the sm will issue close before it gets the ticket extension
        state.expectedImmediate.add(new ReservationState(ReservationStates.Closed, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Closed, ReservationStates.None, ReservationStates.NoJoin));
        statesToCheck.add(state);
    }

    public void check()
    {
        checkImmediate();
        checkExpired();
    }

    protected void checkImmediate()
    {
        for (int i = 0; i < statesToCheck.size(); i++) {
            StateToCheck state = statesToCheck.get(i);

            writer.println("*********************************");
            writer.println("(Immediate) Testing state: " + state.state);
            writer.println("*********************************");

            RecoveryTester tester = new RecoveryTester(RecoveryTester.TestModeAgent, state.state);
            tester.setCyclesToWait(0);
            tester.setElasticTime(true);
            tester.setLeaseLength(LeaseLength);
            tester.setUnits(Units);
            tester.setWriter(writer);
            RecoveryResult result = tester.runTest();

            if (result.code != RecoveryTester.ExitCodeOK) {
                String msg = "Recovery for state " + state.state.toString() + " failed. Code: " + Integer.toString(result.code);
                writer.println(msg);
                writer.flush();
                throw new RuntimeException(msg);
            } else {
                if (state.expectedImmediate.contains(result.state)) {
                    writer.println("Recovery successful");
                    writer.flush();
                } else {
                    String msg = "Recovery for state " + state.state.toString() + " failed. Unexpected end state. Expected: " + state.getString(state.expectedImmediate) + ", actual: " + result.state;
                    writer.println(msg);
                    writer.flush();
                    throw new RuntimeException(msg);
                }
            }
        }
    }

    protected void checkExpired()
    {
        for (int i = 0; i < statesToCheck.size(); i++) {
            StateToCheck state = statesToCheck.get(i);

            writer.println("*********************************");
            writer.println("(Expired) Testing state: " + state.state);
            writer.println("*********************************");

            RecoveryTester tester = new RecoveryTester(RecoveryTester.TestModeAgent, state.state);
            tester.setCyclesToWait(2 * LeaseLength);
            tester.setElasticTime(true);
            tester.setLeaseLength(LeaseLength);
            tester.setUnits(Units);
            tester.setWriter(writer);
            RecoveryResult result = tester.runTest();

            if (result.code != RecoveryTester.ExitCodeOK) {
                String msg = "Recovery for state " + state.state.toString() + " failed. Code: " + Integer.toString(result.code);
                writer.println(msg);
                writer.flush();
                throw new RuntimeException(msg);
            } else {
                if (state.expectedExpired.contains(result.state)) {
                    writer.println("Recovery successful");
                    writer.flush();
                } else {
                    String msg = "Recovery for state " + state.state.toString() + " failed. Unexpected end state. Expected: " + state.getString(state.expectedExpired) + ", actual: " + result.state;
                    writer.println(msg);
                    writer.flush();
                    throw new RuntimeException(msg);
                }
            }
        }
    }
}
