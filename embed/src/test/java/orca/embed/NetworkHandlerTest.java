package orca.embed;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Properties;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;

import net.jwhoisserver.utils.InetNetworkException;
import orca.ndl.LayerConstant;
import orca.ndl.NdlModel;
import orca.ndl.NdlException;
import orca.ndl.NdlModel.ModelType;
import orca.ndl.elements.Device;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.SwitchingAction;
import orca.embed.cloudembed.NetworkHandler;
import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.util.PropList;
import junit.framework.TestCase;

public class NetworkHandlerTest extends TestCase {
	public static final String rootURL = "http://geni-orca.renci.org/owl/";
	String requestFileUNCRenci,requestFileDukeRenci, requestFileRenciDuke,requestFileDukeUNC, substrateFileName;
	String request1, request2,request3,requestFileDukeNCSU, requestFileRenciNCSU;
	NetworkHandler handler;

	protected void setUp() throws Exception {
		super.setUp();
		requestFileRenciNCSU = "orca/ndl/request/request-renci-ncsu.rdf";  //UNC - RENCI
        requestFileUNCRenci = "orca/ndl/request/request-6509.rdf";  //UNC - RENCI
        requestFileDukeNCSU = "orca/ndl/request/request-6509-1.rdf";  //Duke - NCSU
        requestFileRenciDuke = "orca/ndl/request/request-6509-2.rdf"; // RENCI-Duke
        requestFileDukeUNC = "orca/ndl/request/request-6509-3.rdf"; //Duke - UNC
        substrateFileName = "orca/ndl/substrate/ben-dell.rdf";
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public String modelToString(String requestFile) throws NdlException{
		OntModel rm = NdlModel.getModelFromFile(requestFile, OntModelSpec.OWL_MEM_RDFS_INF, true);
		OutputStream out = new ByteArrayOutputStream();
		rm.write(out);
		String request = out.toString();
		
		NdlModel.closeModel(rm);
		
		return request; 
	}
	
	   public void testHandleMapping() throws IOException, InetNetworkException, NdlException
	    {
	        handler = new NetworkHandler(substrateFileName);

	        //handler.getMapper().getOntModel().write(System.out);
	        for(int i=0;i<2;i++){
	        	System.out.println("Test No."+i);
	        	String m1 = modelToString(requestFileRenciNCSU);
	    		RequestReservation request1 = handler.getRequestReservation(m1);

	        	handler.handleRequest(request1);
	        
	        	NetworkConnection con = handler.getDeviceConnection();
	        	print(con);
	        
	        	//handler.getConnectionTeardownActions(request1);
	        	//handler.releaseReservation(request1);
	        }
	        /*
	        handler.handleRequestFile(requestFileUNCRenci);
	        request2=handler.getRequest().getReservation();
	        
	        NetworkConnection con2 = handler.getDeviceConnection();
	        print(con2);
	        
	        handler.getConnectionTeardownActions(request2);
	        handler.releaseReservation(request2);
	        
	        handler.handleRequestFile(requestFileDukeUNC);
	        
	        request3=handler.getRequest().getReservation();
	        
	        NetworkConnection con3 = handler.getDeviceConnection();
	        print(con3);

	        handler.getConnectionTeardownActions(request3);
	        handler.releaseReservation(request3);
	        */
	        // model.write(System.out);
	    }
	
    public void print(LinkedList<Device> list) {
        for (Device d : list) {
            System.out.println("===> Device name=" + d.getName() + "(" + d.getURI() + ")" + " action count=" + d.getActionCount());
            LinkedList<SwitchingAction> actions = d.getActionList();
            for (SwitchingAction a : actions) {
                System.out.print("Action=" + a.getDefaultAction() + " Label=" + a.getLabel_ID());
    
                LinkedList<Interface> ifs = a.getClientInterface();
                for (Interface iff : ifs) {
                    System.out.print(" Interface=" + iff.getName());
                }
    
                System.out.println();
    
            }
        }
    }
    
    
    public void print2(String site, LinkedList<Device> list, Properties p) {
        for (Device d : list) {
            String name = d.getType().toLowerCase().trim();
            if (!name.equals("vm")) {            
                LinkedList<SwitchingAction> actions = d.getActionList();
                int actionCount=actions==null ? 0:actions.size();
                
                int anum = 0;
                
                for (int i = 0; i < actionCount; i++) {
                    SwitchingAction a = actions.get(i);
                    if (Objects.equals(a.getDefaultAction(), LayerConstant.Action.Temporary.toString())) {
                        continue;
                    }
                    if (Objects.equals(a.getDefaultAction(), "VLANtag")) {
                        PropList.setProperty(p, "vlan.tag", (int)a.getLabel_ID());
                        PropList.setProperty(p, "vlan.bandwidth", a.getBw());
                        System.out.println("vlan.tag="+a.getLabel_ID()+";vlan.bandwidth="+a.getBw());
                    } // ignore label id otherwise
                   
                    LinkedList<Interface> ifs = a.getClientInterface();
                    /*System.out.println(site+":"+name+":"+ifs.size());
                    for(int j=0;j<ifs.size();j++){
                    	System.out.println(ifs.get(j).resource+"\n");
                    }*/
                    if (ifs.size() != 2) {
                        System.out.println("OOOOOOOOPs! Can only handle two interfaces:"+name+":"+d.getType());
                    }else{
                    	PropList.setProperty(p, site + "." + name + ".action." + (anum+1) + ".sport", ifs.get(0).getName());
                    	PropList.setProperty(p, site + "." + name + ".action." + (anum+1) + ".dport", ifs.get(1).getName());                   
                    	System.out.println(site + "." + name + ".action." + (anum+1) + ".sport="+ ifs.get(0).getName());
                    	System.out.println(site + "." + name + ".action." + (anum+1) + ".dport="+ ifs.get(1).getName());
                    	anum++;
                    }
                }        
                
                // unc.polatis.actions=2
                PropList.setProperty(p, site + "." + name + ".actions", anum);
                System.out.println(site + "." + name + ".actions="+ anum);
                
                String alist = "";
                 for (int i = 0; i < anum; i++) {
                    alist += Integer.toString(i+1) + " ";
                 }
                 alist = alist.trim();
                    
                 PropList.setProperty(p, site + "." + name + ".actionslist", alist);
                 System.out.println(site + "." + name + ".actionslist="+ alist);
            }
        }
    }
    
    public void print(NetworkConnection con)
    {
    	if(con==null) {
    		System.out.println("No connection being set up");
    		return;
    	}
        LinkedList<?> list = con.getConnection();
        LinkedList<Device> unc = new LinkedList<Device>();
        LinkedList<Device> renci = new LinkedList<Device>();
        LinkedList<Device> duke = new LinkedList<Device>();
        LinkedList<Device> ncsu = new LinkedList<Device>();
        for (Object o : list) {
            if (o instanceof Device) {
                Device d = (Device)o;
                String uri = d.getURI();
                if (uri.indexOf("#Renci") != -1) {
                    // this is a renci device
                    renci.add(d);
                } else if (uri.indexOf("#UNC") != -1) {
                    // this a UNC device
                    unc.add(d);
                } else if (uri.indexOf("#Duke") != -1) {
                    // this a UNC device
                    duke.add(d);
                } else if (uri.indexOf("#NCSU") != -1) {
                    // this a UNC device
                    ncsu.add(d);
                }
                else {
                    throw new RuntimeException("Device is from an unknown site: " + uri);
                }
            }
        }
        
        Properties p = new Properties();
        print2("unc", unc, p);
        print2("renci", renci, p);
        print2("duke", duke, p);
        print2("ncsu", ncsu, p);
        System.out.println(p.toString());
        
    }

}
