/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.xmlrpc;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;

import orca.extensions.PackageId;
import orca.extensions.PluginId;
import orca.tests.core.ControllerTest;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
/**
 *
 * @author anirban
 */
public class XmlrpcControllerTest extends ControllerTest
{
    public static final String ActorName = "service";
    public static final PackageId XmlrpcPackageId = XmlrpcControllerConstants.PackageId;
    public static final PluginId XmlrpcControllerPluginId = new PluginId("1");
    public static final String XmlrpcSliceName = "xmlrpcsimpleslice";

    public static final String PropertyTimeToLive = "time.to.live";

    protected int timeToLive = 0;

    public XmlrpcControllerTest(String[] args)
    {
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
        p.setProperty(XmlrpcControllerFactory.PropertySliceName, XmlrpcSliceName);
        startController(ActorName, XmlrpcPackageId, XmlrpcControllerPluginId, p);
    }


    @Override
    protected void runTest()
    {
        System.out.println("Inside runTest()");
        try {
            prepare();
            XmlrpcController cont = (XmlrpcController)getController();
            assert cont != null;
            System.out.println("Xmlrpc Controller started successfully");

            String serverHostname = "localhost";
            int port = 20001;

            try {

                System.out.println("Starting xml-rpc client");

                //XmlRpcClient client = new XmlRpcClient("http://" + serverHostname + ":" + port + "/");

                XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
                config.setServerURL(new URL("http://" + serverHostname + ":" + port + "/"));
                XmlRpcClient client = new XmlRpcClient();
                client.setConfig(config);

                // Build our parameter list.
                //Vector params = new Vector();

                //params.addElement(new Integer(5));
                //params.addElement(new Integer(3));

                // Call the server, and get our result.
                
                //Hashtable result = (Hashtable) client.execute("xmlrpcService.sumAndDifference", params);
                //int sum = ((Integer) result.get("sum")).intValue();
                //int difference = ((Integer) result.get("difference")).intValue();

                // Print out our result.
                //System.out.println("Sum: " + Integer.toString(sum) + ", Difference: " + Integer.toString(difference));


                //params.clear();
                //String res = (String) client.execute("xmlrpcService.discoverResources", params);
                //System.out.println("Result from discoverResources: " + res);

                //Object[] params = new Object[] { new String("cert1"), new String("cert2") };
                Object[] params = new Object[1];
                params[0] = new String[] {new String("cert1"), new String("cert2")};
                //params[0] = new String("cert1");
                //params[1] = new String("cert2");
                params = null;
                String res = (String) client.execute("xmlrpcService.ListResources", params);
                System.out.println("Result from ListResources: " + res);

                /*
                Vector paramVector = new Vector();
                String ndlRequest = readFileAsString("/Users/anirban/Documents/RENCI-research/Codes/orca-trunk/trunk/tools/cmdline/ndl/idRequest3.rdf");
                paramVector.addElement(ndlRequest);
                res = (String) client.execute("xmlrpcService.createSliver", paramVector);
                System.out.println("Result from createSliver: " + res);
                */
                
                while(true){
                    
                }
                
                
            } catch (Exception e) {

                e.printStackTrace();
                System.err.println("XmlrpcTest: " + e.toString());

            }


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Test failed");
            System.exit(-1);
        }

        System.out.println("Test successful");
        System.exit(0);
    }

    /*
     * Reads file contents as a string
     */
    private static String readFileAsString(String filePath) throws java.io.IOException {
        byte[] buffer = new byte[(int) new File(filePath).length()];
        FileInputStream f = new FileInputStream(filePath);
        f.read(buffer);
        return new String(buffer);
    }





    public static void main(String[] args) throws Exception
    {
        if (args.length > 0) {
            XmlrpcControllerTest test = new XmlrpcControllerTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }


}
