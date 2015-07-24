package orca.embed.cloudembed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.ndl.DomainResourceType;
import orca.ndl.LayerConstant;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.elements.Label;
import orca.ndl.elements.NetworkConnection;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class PortHandler  extends NetworkHandler implements LayerConstant{
	
	private HashMap <String, ArrayList <Label>> requestMap;
	
	public PortHandler(String substrateFile) throws IOException, NdlException {
		super(substrateFile);
	}

	@Override
	public SystemNativeError handleRequest(RequestReservation request){
		DomainResourceType rType=null;
		OntModel requestModel = request.getModel();
		//mapper.requestModel.write(System.out);
		String queryPhrase=mapper.createQueryStringSite();
        ResultSet results = mapper.rdfQuery(requestModel, queryPhrase);
        //mapper.outputQueryResult(results);
        //results = mapper.rdfQuery(mapper.requestModel,queryPhrase);
        
		String var0=(String) results.getResultVars().get(0);//connection
		String var1=(String) results.getResultVars().get(1);//node
		String var2=(String) results.getResultVars().get(2);//resource type
		String var3=(String) results.getResultVars().get(3);//units
		String var4=(String) results.getResultVars().get(4);//bw
		
		QuerySolution solution=null;
		Resource rs0 = null,rs1 = null,rs2=null;
		String rs3_str=null,rs4_str=null;
		long bw=0;
		int units=0;
		ArrayList <Label> portList=null;
		while(results.hasNext()){
			solution=results.nextSolution();
			rs0=solution.getResource(var0);
			rs1=solution.getResource(var1);
			rs2=solution.getResource(var2);
			if(solution.getLiteral(var3)!=null){
				rs3_str=solution.getLiteral(var3).getValue().toString();
				units=Integer.valueOf(rs3_str).intValue();
			}
			if(solution.getLiteral(var4)!=null){
				rs4_str=solution.getLiteral(var4).getValue().toString();
				bw=Long.valueOf(rs4_str).longValue();
			}
			
			//allocation of port			
			//rType=domainResources.getResourceType(rs2.getLocalName());
			//System.out.println(domainResources.toString()+":"+rs2+":"+rType);
			int avail = rType.getCount();
			if (rType!=null){
				if(units<rType.getCount()){
					rType.setCount(avail-units);
					portList = getPortInterface(rType.getResourceType(),units);
				}
				else{
					logger.fatal("Not Enough Resource: "+rType+" Requested: "+units);
					break;
				}
			}
			else {
				logger.fatal("Not Such Resource Type: "+rs2);
				break;
			}
			
			if( (request.getReservation()!=null) && portList!=null) 
				requestMap.put(request.getReservation(), portList);	
		}	
		return null;
	}	
	
	public ArrayList <Label> getPortInterface(String rType,int units){

		ArrayList <OntResource> borderInterfaceList = null;//=domain.getIntfList();
		Resource intf=null;
		ArrayList <Label> portList= new ArrayList <Label>();
		
		for(Iterator <OntResource> i=borderInterfaceList.iterator();i.hasNext();){
			intf=i.next();
			Resource set=null,type=null;
			boolean intf_type=false;
			//if(rType!=null){
				for (StmtIterator j=intf.listProperties(mapper.availableLabelSet);j.hasNext();){
					set = j.next().getResource();
					type=set.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource();
					if(type.getLocalName().equalsIgnoreCase(rType)){
						intf_type=true;
						break;
					}
				}
			//}
			//else intf_type=true;
			ResultSet results=null;
			QuerySolution solution=null;
			if(intf_type){
				String rsURI = intf.getURI();
		        String availableLabelSet = "compute:availablePortLabelSet";
		        String usedLabelSet = "compute:usedPortLabelSet";
		        String usedLabelSet_str = NdlCommons.ORCA_NS+"compute.owl#usedPortLabelSet";

		        String usedSet = intf.getURI() + "/" + "usedPortLabelSet";
		        
		        Resource label_rs=null;
		        
		        for(int j=0;j<units;j++){
		        	label_rs=mapper.processLabel(null, rsURI,availableLabelSet,usedLabelSet,usedLabelSet_str,usedSet,null,null,null);
		        	if(label_rs==null){
		        		logger.error("No Label assigned!");
		        	}
		        	else if(label_rs.getProperty(mapper.RDFS_Label) == null){
		        		logger.error("No Name :"+label_rs);
		        	}
		        	else {
		        		Label label = new Label (idm.getOntResource(label_rs),label_rs.getProperty(mapper.RDFS_Label).getString(),0,rType);
		        		portList.add(label);
		        	}
		        }
			}
			
		}
		return portList;
	}

	public NetworkConnection releaseReservation(String requestID){
		ArrayList <Label> portList=requestMap.get(requestID);
		if(portList==null) {
			logger.error("No ports to be released :"+requestID);
			return null;
		}
		for(int i=0;i<portList.size();i++){
			Label label=portList.get(i);
			
			
		}
		return null;
	}

	public String getPortListToString(String uri){
		ArrayList <Label> portList=requestMap.get(uri);
		if(portList==null) return null;
		String ports_str=new String(portList.get(0).name);
		for(int i=1;i<portList.size();i++){
			ports_str=ports_str.concat(","+portList.get(i));
		}
		return ports_str;
	}
	
	public ArrayList<Label> getPortList(String uri) {
		return requestMap.get(uri);
	}
}
