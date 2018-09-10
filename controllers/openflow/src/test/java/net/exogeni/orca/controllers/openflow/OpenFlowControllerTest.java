package net.exogeni.orca.controllers.openflow;

import java.util.Date;
import java.util.Properties;

import net.exogeni.orca.controllers.openflow.OpenFlowController.OpenFlowRequest;

import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.tests.core.ControllerTest;
import net.exogeni.orca.util.ID;

public class OpenFlowControllerTest extends ControllerTest implements OpenFlowControllerConstants {
    public OpenFlowControllerTest(String[] args) {
        super(args);
    }

    protected void prepare() throws Exception {
        Properties p = new Properties();

        p.setProperty(OpenFlowControllerFactory.PropertySliceName, "openflowsimpleslice");
        startController("service", MyPackageId, OpenFlowControllerId, p);
    }

    @Override
    protected void runTest() {
        try {
            prepare();
            OpenFlowController cont = (OpenFlowController) getController();
            assert cont != null;

            System.out.println("OpenFlow Controller started successfully");

            long now = System.currentTimeMillis();
            Term term = new Term(new Date(now), new Date(now + 1000 * 60 * 2));
            ID rid = cont.addRequest(term, 1, 1);

            OpenFlowRequest r = cont.getRequest(rid);
            System.out.println("Added request. ID=" + rid);
            while (true) {
                if (r.isAcive()) {
                    System.out.println("Request is active");
                    break;
                } else {
                    if (r.isTerminal()) {
                        System.out.println("A failure accurred");
                        throw new RuntimeException("OpenFlow request is terminal");
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
            OpenFlowControllerTest test = new OpenFlowControllerTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }
}
