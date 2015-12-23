package orca.embed;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.jwhoisserver.utils.InetNetworkException;

import com.hp.hpl.jena.ontology.OntResource;

import junit.framework.TestCase;
import orca.embed.cloudembed.controller.InterCloudHandler;
import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.RequestMappingException;
import orca.embed.policyhelpers.SystemNativeError;
import orca.embed.workflow.Domain;
import orca.embed.workflow.RequestWorkflow;
import orca.ndl.DomainResourceType;
import orca.ndl.DomainResources;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.elements.ComputeElement;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkElement;
import orca.shirako.container.Globals;
import orca.util.ResourceType;

public class RequestWorkflowTest extends InterDomainHandlerTest {

	public RequestWorkflow workflow;

	public void setUp() throws Exception {		
		super.setUp();
		requestFileFluke="orca/ndl/request/fluke-request-pegasus.rdf"; //fluke

		requestFileCondor = "src/main/resources/orca/ndl/request/condor-request-1.rdf";  //a condor cluster 1/ 1 master and 5 slaves
		requestFileHadoop = "src/main/resources/orca/ndl/request/hadoop-request-1.rdf";  //a Hadoop cluster 1/ 1 master and 30 slaves
		requestFile0 = "orca/ndl/request/idRequest-uncEuca-renciEuca.rdf";  //UNC/Euca - Renci/Euca
		requestFileNSF = "orca/ndl/request/nsf.rdf"; //unbounded NSF
		requestFileDuke = "src/main/resources/orca/ndl/request/triangle-mp-dukevmsite-request.rdf"; //a Triangle VT in dukevmsite	
		requestFileRenci = "orca/ndl/request/id-mp-rencivmsite-request.rdf"; //a Triangle VT in rencivmsite
		requestFileUNC = "orca/ndl/request/id-mp-uncvmsite-request.rdf"; //a Triangle VT in uncvmsite
		requestFile33 = "orca/ndl/request/id-mp-rencivmsite-request.rdf";//a VM cluster in a VLAN in rencivmsite
		requestFileVMSDuke = "orca/ndl/request/vms-duke.rdf";//5 VMS in dukevmsite
		requestFile = "orca/ndl/request/id-mp-Request2.rdf"; //a Triangle VT inter-cloud 
		
		requestFile2 = "orca/ndl/request/idRequest2.rdf";  //Duke/Euca - Umass
		
		requestFileUncRenci = "orca/ndl/request/idRequest-uncEuca-renciEuca.rdf";  //UNC/Euca - Renci/Euca
		requestFileDukeUnc = "orca/ndl/request/idRequest-dukeEuca-uncEuca.rdf";  //UNC/Euca - Duke/Euca
		
		requestFileConverter = "orca/ndl/request/converter-request-link-term.rdf";  //UNC/Euca - Duke/Euca
		
		requestFileDuke26Node = "src/test/resources/orca/embed/dukevmsite-26node-request.rdf";  // 26 node tree topology in duke vmsite
		requestFileNSF = "src/test/resources/orca/embed/nsf.rdf";
		
		requestFileDukeVMSVlan= "src/main/resources/orca/ndl/request/duke-vms-vlan-request.rdf";  // a vlan connected 6-node cluster in duke vmsite
		
		requestFileRenciVMSVlan= "src/test/resources/orca/embed/TS3-3.rdf";  // a vlan connected 3-node cluster in renci vmsite
		
		requestFileGPO = "src/test/resources/orca/embed/mp.rdf"; 
		
		requestFileGush = "src/test/resources/orca/embed/mp-modify.rdf"; 
		
		requestFileDukeUHouston = "src/main/resources/orca/ndl/request/idRequest-dukeEuca-uhoustonEuca.rdf";
		requestFileDukeRice = "orca/ndl/request/idRequest-dukeEuca-riceEuca.rdf";
		
		workflow=new RequestWorkflow(new InterCloudHandler());
		
		int deltaVM = 10;
		String maxVMDomain = "http://geni-orca.renci.org/owl/dukevmsite.rdf#DukeVMSite/Domain";
		
		substrateFileName = "orca/ndl/substrate/ben-6509.rdf";
	       
        //benhandler = new NetworkHandler(substrateFileName);
        
        rencivmsubstrateFileName = "orca/ndl/substrate/rencivmsite.rdf";
        dukevmsubstrateFileName = "orca/ndl/substrate/dukevmsite.rdf";
        uncvmsubstrateFileName = "orca/ndl/substrate/uncvmsite.rdf";		
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testRun() throws NdlException, IOException, RequestMappingException {
		String reqStr = NdlCommons.readFile(requestFileGPO);
		abstractModels=getAbstractModels();
		DomainResourcePools drp = new DomainResourcePools(); 
		drp.getDomainResourcePools(pools);
		workflow.run(drp, abstractModels, reqStr, null,null, "slice-id");
		String fileName =  "/home/geni-orca//workspace-orca5/orca5/embed/src/test/resources/orca/embed/request-manifest.rdf";
		try {
			OutputStream fsw = new FileOutputStream(fileName);
			workflow.getManifestModel().write(fsw);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LinkedList<NetworkElement> connection = (LinkedList<NetworkElement>) workflow.getBoundElements();
		
		print(connection);
		
		/*/second
		workflow=new RequestWorkflow(new InterCloudHandler());
		
		reqStr = NdlCommons.readFile(requestFileGush);

		workflow.run(drp, abstractModels, reqStr, null,null);
		
		System.out.println(workflow.getErrorMsg());
		
		connection = (LinkedList<NetworkElement>) workflow.getBoundElements();
		
		print(connection);
		*/
		//workflow.getManifestModel().write(System.out);
		
		workflow.closeModel();
		workflow.close();
	}

	public void testModify() throws NdlException, IOException, RequestMappingException, InetNetworkException {
		String parent_prefix = "unit.eth";
		String tag_key="unit.eth2.vlan.tag";
		String index=tag_key.split(parent_prefix)[1];
		String index_end = index.split(".vlan.tag")[0];
		String host_interface = index_end;
		System.out.println("ModifiedRemove: host_interface="+host_interface+";1="+index+";2="+index_end+";tag_key="+tag_key);
		
		String parent_tag_name = parent_prefix.concat(host_interface).concat(".vlan.tag");
		
		String reqStr = NdlCommons.readFile(requestFileGPO);
		String modReq = NdlCommons.readFile(requestFileGush);
		abstractModels=getAbstractModels();
		DomainResourcePools drp = new DomainResourcePools(); 
		drp.getDomainResourcePools(pools);
		
		workflow.run(drp, abstractModels, reqStr, null,null, "slice-id");
		
		Collection<NetworkElement> boundElements = workflow.getBoundElements();
		HashMap <String,DomainElement> firstGroupElement=new HashMap <String,DomainElement>();
		
		ReservationElementCollection elementCollection = new ReservationElementCollection(boundElements, firstGroupElement);
		/*
		String fileName = "/home/geni-orca/workspace-orca5/orca5/embed/src/test/resources/orca/embed/mp-manifest.rdf";
        OutputStream fsw = new FileOutputStream(fileName);
        workflow.getManifestModel().write(fsw);
		*/
		workflow.modify(drp, modReq,"slice-id",elementCollection.NodeGroupMap,elementCollection.firstGroupElement);
		
		LinkedList<NetworkElement> connection = (LinkedList<NetworkElement>) workflow.getBoundElements();
		
		print(connection);
		
		workflow.closeModel();
		workflow.close();
	}
	
	class ReservationElementCollection {
		public static final String NotInGroup = "NotInGroup";
		
		Collection<NetworkElement> elements;
		HashMap <String,Collection <DomainElement>> NodeGroupMap;
		HashMap <String,DomainElement> AllNodeMap;
		HashMap <String,DomainElement> firstGroupElement;
		
		public ReservationElementCollection() {
			this.elements = null; 
		}
		
		public ReservationElementCollection(Collection<NetworkElement> elements,HashMap <String,DomainElement> fgp) {
			this.elements = elements; 
			firstGroupElement = fgp;
			init();
		}
		
		public void setElements(Collection<NetworkElement> elements){
			this.elements = elements; 
		}
		
		public Collection<NetworkElement> getElements(Collection<NetworkElement> elements){
			return elements; 
		}
		
		
		public void init(){
			try{
			NodeGroupMap = new  HashMap <String,Collection <DomainElement>>();
			AllNodeMap = new  HashMap <String,DomainElement>();
			for(NetworkElement device:elements){
				boolean isNetwork=false,isVM = false;
				DomainElement de = (DomainElement) device;
				if(de.getCe()==null)
					isNetwork = true;
				else
					isVM=true;
			
				if (isVM) {
					ComputeElement ce = de.getCe();
					String group = ce.getGroup();
					
					addNodeToGroup(group, de);
					AllNodeMap.put(de.getName(), de);
				}
			}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private void addNodeToGroup(String group, DomainElement ce){
			if(this.NodeGroupMap==null)
				return;

			Collection <DomainElement> ces = null;
			if( (group==null) || (ce.getName().endsWith(group)))
				group = this.NotInGroup;
			if(this.NodeGroupMap.containsKey(group)){
				ces = this.NodeGroupMap.get(group);
			}else{
				ces = new LinkedList <DomainElement>();
				this.NodeGroupMap.put(group,ces);
			}
			ces.add(ce);
			if(!firstGroupElement.containsKey(group))
				this.firstGroupElement.put(group,ce);
		}
	}
	
}
