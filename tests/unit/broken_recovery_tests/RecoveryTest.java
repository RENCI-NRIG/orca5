package orca.tests.unit.recovery;

import orca.tests.core.ShirakoTest;

public class RecoveryTest extends ShirakoTest {
    public RecoveryTest(String[] args) {
        super(args);
    }

    @Override
    protected void runTest() {
        try {
            ReservationClientChecker checkRC = new ReservationClientChecker();
            checkRC.check();
            //
            // BrokerReservationChecker checkAR = new BrokerReservationChecker();
            // checkAR.check();
            //
            // AuthorityReservationChecker checkSR = new AuthorityReservationChecker();
            // checkSR.check();

            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            ShirakoTest test = new RecoveryTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }
}
