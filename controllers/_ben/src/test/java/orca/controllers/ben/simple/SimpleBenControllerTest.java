package orca.controllers.ben.simple;

import java.util.Date;
import java.util.Properties;

import orca.controllers.ben.BenConstants;
import orca.controllers.ben.simple.BenSimpleController.BenRequest;
import orca.shirako.time.Term;
import orca.tests.core.ControllerTest;
import orca.util.ID;

public class SimpleBenControllerTest extends ControllerTest implements BenConstants {
    public SimpleBenControllerTest(String[] args) {
        super(args);
    }

    protected void prepare() throws Exception {
        Properties p = new Properties();
        p.setProperty(BenSimpleControllerFactory.PropertySliceName, "bensimpleslice");
        startController("service", MyPackageId, SimpleBenControllerId, p);
    }

    @Override
    protected void runTest() {
        try {
            prepare();
            BenSimpleController cont = (BenSimpleController) getController();
            assert cont != null;
            System.out.println("Ben Simple Controller started successfully");

            long now = System.currentTimeMillis();
            Term term = new Term(new Date(now), new Date(now + 1000 * 60 * 2));
            ID rid = cont.addRequest(term, 1, 1);
            BenRequest r = cont.getRequest(rid);

            System.out.println("Added request. ID=" + rid);

            while (true) {
                if (r.isAcive()) {
                    System.out.println("Request is active");
                    break;
                } else {
                    if (r.isTerminal()) {
                        System.out.println("A failure accurred");
                        throw new RuntimeException("BEN request is terminal");
                    }
                }
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Test failed");
            System.exit(-1);
        }

        System.out.println("Test successful");
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            SimpleBenControllerTest test = new SimpleBenControllerTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }
}
