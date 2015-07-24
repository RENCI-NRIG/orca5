package orca.controllers.ben.nlr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Properties;

import orca.controllers.ben.BenConstants;
import orca.controllers.ben.BenControllerFactory;
import orca.shirako.time.Term;
import orca.tests.core.ControllerTest;
import orca.util.ID;


public class BenNlrControllerTest extends ControllerTest implements BenConstants
{    
    public static final String PropertyTimeToLive = "time.to.live";
    
    protected int timeToLive = 0;
    
    public BenNlrControllerTest(String[] args){
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
        p.setProperty(BenControllerFactory.PropertySliceName, "bensimpleslice");
        startController("service", MyPackageId, BenNlrControllerId, p);
    }
    
    public String getNdl(String file)
    {
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

    @Override
    protected void runTest()
    {
        try {
            prepare();            
            BenNlrController cont = (BenNlrController)getController();    
            assert cont != null;
            System.out.println("Ben NLR Controller started successfully");
            
            long now = System.currentTimeMillis();
            Term term = new Term(new Date(now), new Date(now + 1000 * 60 * 20));
            ID rid = cont.addRequest(term, 1, 1, 1, getNdl("orca/network/request-6509.rdf"));
            BenNlrController.BenNlrRequest r = cont.getRequest(rid);
            
            System.out.println("Added request. ID=" + rid);
            boolean closed = false;
            
            while(true) {
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
                        if (r.isClosed()){
                            System.out.println("Request closed successfully");
                            break;
                        }else {
                            if (r.hasAtLeastOneFailed()){
                                System.out.println("A failure accurred while closing request");
                                throw new RuntimeException("A failure occurred while closing request");
                            }
                        }
                    }else {
                        if (r.hasAtLeastOneTerminal()){
                            System.out.println("A failure accurred while instantiating request");
                            throw new RuntimeException("A failure occurred while instantiating request");
                        }
                    }
                            
                    
                    System.out.println("NLR: " + r.nlrReservation.getReservationState().toString() + " " + r.nlrReservation.getType());
                    System.out.println("BEN: " + r.benReservation.getReservationState().toString()+ " " + r.benReservation.getType());
                    System.out.println("DUKENET: " + r.dukeReservation.getReservationState().toString()+ " " + r.dukeReservation.getType());
                    System.out.println("VMRENCI: " + r.vmReservationRenci.getReservationState().toString());
                    System.out.println("VMDUKE: " + r.vmReservationDuke.getReservationState().toString());
                    System.out.println("VMUNC: " + r.vmReservationUnc.getReservationState().toString());
                }
                Thread.sleep(5000);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Test failed");
            System.exit(-1);
        }

        System.out.println("Test successful");
        System.exit(0);
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length > 0) {
            BenNlrControllerTest test = new BenNlrControllerTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }
}
