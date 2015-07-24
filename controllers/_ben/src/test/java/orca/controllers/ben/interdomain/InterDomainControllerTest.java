package orca.controllers.ben.interdomain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Properties;

import orca.controllers.ben.BenConstants;
import orca.shirako.api.IServiceManagerReservation;
import orca.shirako.time.Term;
import orca.tests.core.ControllerTest;
import orca.util.ID;

public class InterDomainControllerTest extends ControllerTest implements BenConstants {
    public static final String PropertyTimeToLive = "time.to.live";

    protected int timeToLive = 0;

    public InterDomainControllerTest(String[] args) {
        super(args);
    }

    @Override
    protected void readParameters() throws Exception {
        super.readParameters();

        String temp = properties.getProperty(PropertyTimeToLive);

        if (temp != null) {
            timeToLive = Integer.parseInt(temp);
        }
    }

    protected void prepare() throws Exception {
        Properties p = new Properties();
        p.setProperty(InterDomainControllerFactory.PropertySliceName, "bensimpleslice");
        startController("service", MyPackageId, InterndomainControllerId, p);
    }

    public String getNdl(String file) {
        try {
            InputStream s = getClass().getClassLoader().getResource(file).openStream();
            StringBuffer sb = new StringBuffer();

            BufferedReader r = new BufferedReader(new InputStreamReader(s));
            String line = null;
            while ((line = r.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void testRequest(InterDomainController cont, String request) throws Exception {
        System.out.println("Testing with request: " + request);
        long now = System.currentTimeMillis();
        Term term = new Term(new Date(now), new Date(now + 1000 * 60 * 20));
        ID rid = cont.addRequest(getNdl(request), term);
        if (rid == null) {
            throw new RuntimeException("Test failed: controller did not return request id");
        }

        InterDomainRequest r = cont.getRequest(rid);

        System.out.println("Added request. ID=" + rid);
        boolean closed = false;
        try {
            while (true) {
                if (r.isActive() && !closed) {
                    System.out.println("Request is active");
                    System.out.println("TimeToLive is: " + timeToLive);
                    System.out.println("Waiting...");
                    Thread.sleep(timeToLive);
                    System.out.println("Closing request");

                    cont.close(rid);
                    closed = true;
                } else {
                    if (closed) {
                        if (r.isClosed()) {
                            System.out.println("Request closed successfully");
                            break;
                        } else {
                            if (r.hasAtLeastOneFailed()) {
                                System.out.println("A failure accurred while closing request");
                                throw new RuntimeException("A failure occurred while closing request");
                            }
                        }
                    } else {
                        if (r.hasAtLeastOneTerminal()) {
                            System.out.println("A failure accurred while instantiating request");
                            throw new RuntimeException("A failure occurred while instantiating request");
                        }
                    }

                    System.out.println("***************");
                    for (IServiceManagerReservation res : r.listInterDomainReservations) {
                        System.out.println(res.getType() + " " + res.getReservationState());
                    }
                }
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            System.out.println("Test for request: " + request + " failed");
            throw e;
        }
        System.out.println("Test for request: " + request + " succeeded");
    }

    @Override
    protected void runTest() {
        try {
            prepare();
            InterDomainController cont = (InterDomainController) getController();
            assert cont != null;
            System.out.println("Ben NLR Controller started successfully");
            //testRequest(cont, "orca/network/id-mp-Request1.rdf"); // duke-renci-unc (triangle)
            //testRequest(cont, "orca/network/idRequest1.rdf");  // duke-renci
            //testRequest(cont, "orca/network/idRequest2.rdf");  // duke-umass
            testRequest(cont, "orca/network/idRequest3.rdf");  // duke-port to renci-port
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
            InterDomainControllerTest test = new InterDomainControllerTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }
}
