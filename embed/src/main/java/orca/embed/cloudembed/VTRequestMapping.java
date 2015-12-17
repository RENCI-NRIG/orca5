package orca.embed.cloudembed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import orca.embed.policyhelpers.DomainResourcePools;
import orca.embed.policyhelpers.RequestReservation;
import orca.ndl.DomainResourceType;
import orca.ndl.NdlCommons;
import orca.ndl.elements.ComputeElement;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.emory.mathcs.backport.java.util.Collections;

public class VTRequestMapping extends RequestMapping {

	protected GomoryHuTree gomory;
	
	public VTRequestMapping(OntModel substrateModel)
			throws IOException {
		super(substrateModel);
	}

	public void handleMapping(){
		
	}

	public RequestReservation handleMapping(RequestReservation rr,
			DomainResourcePools pools, String type) {
		
		LinkedList <DomainResourceType> sites = pools.getDomainResourceTypeList(type);		
		int totalNumSites = sites.size();		
		gomory = new GomoryHuTree(sites);
		
		int numVM = rr.getPureTypeUnits(type);
		int p,minPartition=0,totalResource=0;
		for(p=0;p<=totalNumSites;p++){
			totalResource=totalResource + sites.get(p).getCount();
			System.out.println("total="+totalResource+":numVM="+numVM);
			if(numVM<totalResource){
				minPartition=p+1;
				break;
			}
		}
		
		//Build the G-H tree.
		GomoryHuTreeEdge [] tree;
		LinkedList <GomoryHuTreeEdge> tree_c = new LinkedList<GomoryHuTreeEdge>(); 
		gomory.createGraph(numVM,rr.getElements());
		tree=gomory.gomory(gomory.numVertices, gomory.numEdges, gomory.Edges, gomory.capacities, gomory.Adj);
		int i;
		System.out.print("tree returned to Java:\n");
		for(i=0;i<gomory.numVertices-1;i++){
			tree_c.add(tree[i]);
			//tree[i].print();
		}
		
		Collections.sort(tree_c);
		GomoryHuTreeEdge ghEdge=null;
		Iterator <GomoryHuTreeEdge> tree_c_it = tree_c.iterator();
		while(tree_c_it.hasNext()){
			ghEdge=tree_c_it.next();
			ghEdge.print();
		}
		
		embedding(minPartition, totalNumSites,gomory.numVertices,gomory.numEdges,gomory.Adj,tree_c);
		
		RequestReservation request = null;

		return request;
	}
	
	public void embedding(int minPartition, int totalNumSites,int numVM,int numEdges,int[][] Adj,LinkedList <GomoryHuTreeEdge> tree_c){
		int [] partition;		
		float lastCost=100000000,currentCost=0;
		for(int p =minPartition;p<=totalNumSites;p++){
			partition = gomory.partition(numVM,p,gomory.resourcePools);
						
			for(int q=0;q<p;q++)
				System.out.println("Partition:"+q+"="+partition[q]+";");
			
			GomoryHuTreeEdge [] tree_c_array = tree_c.toArray(new GomoryHuTreeEdge [numVM-1]);
			
			gomory.setpartition(numVM,numEdges, partition, Adj,tree_c_array);
			
			break;
			
			//generate balanced partition from the GH tree
				
			/*request = generateVCConnection(rr,partition,pools,type);
			
			currentCost = evaluateEmbeddingCost();
			
			if(currentCost>=lastCost)
				break;
*/
		}
	}
	
	public float evaluateEmbeddingCost(){
		
		return 0;
	}
	
	//generate a inter-cloud request containing a connection with two clusters for routing using InterdomainHandler
	public RequestReservation generateVCConnection(RequestReservation rr, int[] partition,DomainResourcePools pools,String rType){
		RequestReservation request=new RequestReservation();
		request.setModel(rr.getModel());
		OntModel requestModel=request.getModel();
		String reservation = rr.getReservation();
		int part = partition.length;
		LinkedList <DomainResourceType> sites = pools.getDomainResourceTypeList(rType);
		String domain_url = sites.get(0).getDomainURL();
		Resource max_domain_rs=this.getOntModel().createResource(domain_url);
		String master_str = reservation+"/MasterGroup";
		Individual master_ont =  requestModel.createIndividual(master_str,NdlCommons.computeElementClass);
		if(max_domain_rs!=null)
			master_ont.addProperty(NdlCommons.inDomainProperty , max_domain_rs);
		DomainResourceType dType = new DomainResourceType(rType,partition[0]);
		dType.setRank(10);
		ComputeElement master_ce = new ComputeElement(requestModel,master_ont);
		master_ce.setResourceType(dType);
		Resource slave_domain_rs;	
		String slave_str_pre = reservation+"/SlaveGroup",slave_str="";
		Collection<NetworkElement> requestElements = new HashSet<NetworkElement>();
		for(int i=1;i<part;i++){
			domain_url=sites.get(i).getDomainURL();
			slave_domain_rs=this.getOntModel().createResource(domain_url);
			String connectionName = reservation+"/Connection"+"/"+String.valueOf(i);
			Individual rs_connection = requestModel.createIndividual(connectionName,NdlCommons.networkDomainOntClass);
			rs_connection.addProperty(NdlCommons.domainHasResourceTypeProperty, NdlCommons.vlanResourceTypeClass);
			DomainResourceType cType = NdlCommons.getDomainResourceType(rs_connection);		

			NetworkConnection connection=new NetworkConnection(requestModel,rs_connection);
			connection.setBandwidth(NdlCommons.Default_Bandwidth);		
			connection.setResourceType(cType);

			slave_str=slave_str_pre+"/"+String.valueOf(i);
			Individual slave_ont =  requestModel.createIndividual(slave_str,NdlCommons.computeElementClass);
			slave_ont.addProperty(NdlCommons.inDomainProperty , slave_domain_rs);

			dType = new DomainResourceType(rType,partition[i]);
			dType.setRank(10);
			ComputeElement slave_ce = new ComputeElement(requestModel,slave_ont);
			slave_ce.setResourceType(dType);
			//add interfaces....
		
			connection.setNe1(master_ce);
			connection.setNe2(slave_ce);

			//process node dependency,image information, etc.
		
			requestElements.add(connection);
		
			logger.info("InterCloud VC connection:dType="+rType+"--"+partition[0]+":"+partition[i]+";"+connectionName+":"+master_str+":"+slave_str+":"+max_domain_rs+":"+slave_domain_rs);
		}
		request.setRequest(requestModel, requestElements, rr.getTerm(), rr.getReservationDomain(),rr.getReservation(),rr.getReservation_rs());
		return request;
	}

}
