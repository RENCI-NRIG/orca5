package orca.tests.unit.recovery;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import orca.shirako.common.ReservationState;
import orca.shirako.kernel.ReservationStates;

public class ReservationClientChecker
{
    public static final String LogFile = "states.reservationclient.log";
    public static final long LeaseLength = 30;
    public static final int Units = 2;

    protected ArrayList<StateToCheck> statesToCheck;
    protected PrintWriter writer;

    public ReservationClientChecker()
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
        /*
         * Reservations in this state represent requests that were submitted to
         * the actor to obtain tickets for them, but it is unclear if the actor
         * actually issued the request to the broker.
         */
        StateToCheck state = new StateToCheck(new ReservationState(ReservationStates.Nascent, ReservationStates.None, ReservationStates.NoJoin));
        /*
         * For immediate recovery, operations should proceed as if the problem
         * never occurred.
         */
        state.expectedImmediate.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));

        /*
         * When recovery is delayed pass the expiration of the reservation, the
         * outcome will depend on whether the ticket request was issued to the
         * broker before the failure occurred. If the request was not issued,
         * the broker will consider the subsequent ticket request as a new
         * request and process it correctly. If the request was issued, there
         * are two cases: (1) the reservation is still in the Kernel data
         * structures. In this case, the kernel will try to do an amendReserve,
         * which should fail because the reservation is not in the Nascent state
         * anymore. (2) the reservation is not in the Kernel data structures. In
         * this case the Kernel will try to register it and add a database
         * record. If the previous database record is still present, the
         * registration will fail and the reservation will be failed. If the
         * previous record is not present, the reservation will eventually be
         * given a ticket.
         */
        state.expectedExpired.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Failed, ReservationStates.None, ReservationStates.NoJoin));
        statesToCheck.add(state);

        /*
         * Reservations in this state have already issued a ticket request to the
         * broker. They may or may not have received the update ticket from the
         * broker.
         */
        state = new StateToCheck(new ReservationState(ReservationStates.Nascent, ReservationStates.Ticketing, ReservationStates.NoJoin));
        /*
         * For immediate recovery, operations should proceed as if the problem
         * never occurred.
         */
        state.expectedImmediate.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        /*
         * The actual end state depends on whether the broker removes the
         * reservation record from the database after the reservation expires or
         * not. If the reservation record is removed and the reservation request
         * is elastic, the end state should be (Active, None, NoJoin). If the
         * reservation record is not removed, the end state will be (Failed,
         * None, NoJoin) because the broker will fail to register the
         * reservation in the database. Right now, our implementation keeps
         * reservation records until they are manually removed by the
         * administrator.
         */
        state.expectedExpired.add(new ReservationState(ReservationStates.Failed, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));        
        statesToCheck.add(state);

        /*
         * Reservations in this state have been issued a ticket. They may or may not have issued a redeem for the ticket. 
         */
        state = new StateToCheck(new ReservationState(ReservationStates.Ticketed, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedImmediate.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Closed, ReservationStates.None, ReservationStates.NoJoin));
        statesToCheck.add(state);

        state = new StateToCheck(new ReservationState(ReservationStates.Ticketed, ReservationStates.Redeeming, ReservationStates.NoJoin));
        state.expectedImmediate.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Failed, ReservationStates.None, ReservationStates.NoJoin));
        statesToCheck.add(state);

        state = new StateToCheck(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.Joining));
        state.expectedImmediate.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Closed, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Failed, ReservationStates.None, ReservationStates.Joining));
        statesToCheck.add(state);

        state = new StateToCheck(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedImmediate.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Closed, ReservationStates.None, ReservationStates.NoJoin));
        statesToCheck.add(state);

        state = new StateToCheck(new ReservationState(ReservationStates.Active, ReservationStates.ExtendingTicket, ReservationStates.NoJoin));
        state.expectedImmediate.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedImmediate.add(new ReservationState(ReservationStates.Closed, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Closed, ReservationStates.None, ReservationStates.NoJoin));
        statesToCheck.add(state);

        state = new StateToCheck(new ReservationState(ReservationStates.ActiveTicketed, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedImmediate.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedImmediate.add(new ReservationState(ReservationStates.Closed, ReservationStates.None, ReservationStates.NoJoin));
        state.expectedExpired.add(new ReservationState(ReservationStates.Closed, ReservationStates.None, ReservationStates.NoJoin));
        statesToCheck.add(state);

        state = new StateToCheck(new ReservationState(ReservationStates.ActiveTicketed, ReservationStates.ExtendingLease, ReservationStates.NoJoin));
        state.expectedImmediate.add(new ReservationState(ReservationStates.Active, ReservationStates.None, ReservationStates.NoJoin));
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

            RecoveryTester tester = new RecoveryTester(RecoveryTester.TestModeServiceManager, state.state);
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

            RecoveryTester tester = new RecoveryTester(RecoveryTester.TestModeServiceManager, state.state);
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
