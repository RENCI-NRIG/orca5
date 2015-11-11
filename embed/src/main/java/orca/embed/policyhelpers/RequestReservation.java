package orca.embed.policyhelpers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.emory.mathcs.backport.java.util.Collections;
import orca.ndl.DomainResourceType;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.ComputeElement;
import orca.ndl.elements.OrcaReservationTerm;
import orca.ndl.elements.RequestSlice;
import orca.util.PathGuesser;

public class RequestReservation {
	protected OntModel model = null;
	protected Collection<NetworkElement> elements;
	protected HashMap <String,Integer> typeTotalUnits;
	protected HashMap <String, RequestReservation> domainRequestReservation;
	protected Collection<NetworkElement> markedElements;
	
	protected int numNetworkConnection=0;
	
	private OrcaReservationTerm term;
	private String reservationDomain = null,ori_reservationDomain=null;
	private String reservation = null;
	private Resource reservation_rs = null;
	private RequestSlice slice;
	
	public static String gomory_Input_File=PathGuesser.getOrcaControllerHome() + "logs" +
					       System.getProperty("file.separator") + "input3.in";
	public static String Interdomain_Domain = NdlCommons.ORCA_NS+"InterDomain";
	public static String Unbound_Domain = NdlCommons.ORCA_NS+"UnboundDomain";
	public static String MultiPoint_Domain = NdlCommons.ORCA_NS+"MultiPointDomain";
	
	SystemNativeError err;	
	private Logger logger = NdlCommons.getNdlLogger();
	
	public void setError(SystemNativeError e){
		err = e;	
	}
	
	public SystemNativeError getError(){
		return err;
	}

	public void setDomainRequestReservation(
			HashMap<String, RequestReservation> domainRequestReservation) {
		this.domainRequestReservation = domainRequestReservation;
	}
	
	public void setRequest(OntModel m,Collection<NetworkElement> e,OrcaReservationTerm t,String reservationD,String r, Resource r_rs){
		model=m;
		elements=e;
		term=t;
		reservationDomain=reservationD;
		reservation = r;
		reservation_rs = r_rs;
		typeTotalUnits = new HashMap <String,Integer>();
		domainRequestReservation = new HashMap <String, RequestReservation>();
	}
	
	public void setRequest(OntModel m,NetworkElement e,OrcaReservationTerm t,String reservationD,String r, Resource r_rs){
		model=m;
		term=t;
		reservationDomain=reservationD;
		reservation = r;
		reservation_rs = r_rs;
		typeTotalUnits = new HashMap <String,Integer>();
		if(domainRequestReservation ==null)
			domainRequestReservation = new HashMap <String, RequestReservation>();
		setRequest(e);
	}
	
	@SuppressWarnings("unchecked")
	public void setRequest(NetworkElement e){
		if(elements == null)
			elements = new HashSet <NetworkElement>();
		elements.add(e);
		
		if(e instanceof NetworkConnection){
			NetworkConnection ne = (NetworkConnection) e;
			if(ne.getLabel_ID()!=0 && !ne.isModify())		//user specified tag doesn't count
				this.setPureType(e.getResourceType(),this.typeTotalUnits);
			numNetworkConnection++;
			NetworkConnection requestConnection = (NetworkConnection) e;
			if(markedElements==null)
            	markedElements = new HashSet <NetworkElement>();
			
			//broadcast connection
            if(requestConnection.getConnection()!=null){
            	LinkedList <NetworkElement> bcNodeList = (LinkedList<NetworkElement>)requestConnection.getConnection();
            	for(int i =0;i<bcNodeList.size();i++){
            		if(!markedElements.contains(bcNodeList.get(i))){
            			if(!bcNodeList.get(i).isModify())
            				setPureType(bcNodeList.get(i).getResourceType(),typeTotalUnits);
            			markedElements.add(bcNodeList.get(i));
            		}
            	}
            }

            //regular connection
            NetworkElement edge=null;
            edge=requestConnection.getNe1();
			if(edge!=null){
				if(!markedElements.contains(edge)){	
					if(!edge.isModify())
						setPureType(edge.getResourceType(),typeTotalUnits);
					markedElements.add(edge);
				}
			}
			edge=requestConnection.getNe2();
			if(edge!=null){
				if(!markedElements.contains(edge)){	
					if(!edge.isModify())
						setPureType(edge.getResourceType(),typeTotalUnits);
					markedElements.add(edge);
				}
			}
		}else{
			if(!e.isModify())
				this.setPureType(e.getResourceType(),this.typeTotalUnits);
		}
	}
	
	//convert the request topology into graph with node numbered by sn.
	public boolean generateGraph(Collection<NetworkElement> rElements) throws  NdlException, IOException{       
		boolean requestBounded = true, intraSite = true, mixDomain = false;
		int numNode=-1;
		if((rElements==null) || (rElements.isEmpty())) 
			return false;
		
		LinkedList<NetworkElement> requestElements = new LinkedList<NetworkElement>();
		for(NetworkElement ne:rElements){
			if(ne instanceof NetworkConnection)
				requestElements.addLast(ne);
			else
				requestElements.addFirst(ne);
		}
		HashMap <Integer,String> nodeMap = new HashMap <Integer, String> ();
		Map<String, NetworkConnection> links = new HashMap<String, NetworkConnection>();
		String rs1_str = null,rs2_str = null;
		NetworkElement ne1,ne2,element=null;
    	NetworkConnection requestConnection=null;
    	//numbering the nodes using the integer stqrting from 1
		for(Iterator <NetworkElement> j=requestElements.iterator();j.hasNext();){	
			element=j.next();
			logger.debug("generateGraph: element="+element.getURI()+";inDomain="+element.getInDomain());
			//System.out.println("Doamin--element:"+element.getName()+":inDomain="+element.getInDomain()+";reservation domain="+reservationDomain);
			if(!(element instanceof NetworkConnection)){
				if(element instanceof ComputeElement){  //out of the request parser, compute elements are always added first
					ComputeElement ce = (ComputeElement) element;
					String elementDomain = element.getInDomain();
					if(elementDomain!=null){
						if(reservationDomain==null){
							if(mixDomain==false)
								//&& !elementDomain.contains(NdlCommons.stitching_domain_str))
								reservationDomain = elementDomain;
						}			
						else if( !(reservationDomain.equals(elementDomain)) ){
							mixDomain = true;
							this.reservationDomain=null;				
						}
					}else{
						element.setInDomain(reservationDomain);
					}
					
					if(!ce.existConnectionInterface()){
						if( (reservationDomain==null) && (elementDomain==null) ){
							element.setInDomain(RequestReservation.Unbound_Domain);
							requestBounded = false;	
						}
						setDomainRequestReservation(element,domainRequestReservation);
						//setPureType(element.getResourceType(),typeTotalUnits);
					}
				}
				continue;
			}

			if(this.reservationDomain!=null && this.reservationDomain.contains(NdlCommons.stitching_domain_str))
				this.reservationDomain=null;
			numNetworkConnection++;
			requestConnection = (NetworkConnection) element;
			links.put(requestConnection.getResource().getLocalName(), requestConnection);
			if(!requestConnection.isModify())
				setPureType(requestConnection.getResourceType(),typeTotalUnits);
			if( (reservationDomain==null) ){
					//|| (requestConnection.getCastType()!=null && requestConnection.getCastType().equalsIgnoreCase("Multicast") && requestConnection.isModify()) ){	
				intraSite=false;
				if(requestConnection.getConnection().size()>0){//broadcast tree request
					String connection_domain = null;
					//if(requestConnection.getCastType().equalsIgnoreCase("Multicast"))
					//	connection_domain=this.MultiPoint_Domain;
					//else
					connection_domain=ifMPConnection(requestConnection);
					if(connection_domain.equals(RequestReservation.Unbound_Domain))
						requestBounded = false;
					element.setInDomain(connection_domain);
					setDomainRequestReservation(element,domainRequestReservation);
					reservationDomain=null;
					continue;
				}
			}
			
			//path request
			String ne1_domain=null,ne2_domain=null,ne_domain=null;
			ne1=requestConnection.getNe1();
			if(ne1!=null){	
				rs1_str=ne1.getURI();
				ne1_domain = ne1.getInDomain();
			}
			if(rs1_str!=null){
				if(!nodeMap.containsValue(rs1_str)){
					numNode++;
					nodeMap.put(numNode,rs1_str);
					ne1.setSn(getNodeMapKey(nodeMap,rs1_str));
				}
			}
			ne2=requestConnection.getNe2();
			if(ne2!=null){
				rs2_str=ne2.getURI();
				ne2_domain=ne2.getInDomain();
			}
			if(rs2_str!=null){
				if(!nodeMap.containsValue(rs2_str)){
					numNode++;
					nodeMap.put(numNode,rs2_str);
					ne2.setSn(getNodeMapKey(nodeMap,rs2_str));
				}
			}
			
			if((ne1_domain!=null)&&(ne2_domain!=null)){
				if(ne1_domain.equals(ne2_domain)){
					if(intraSite)
						this.reservationDomain=ne1_domain;
					element.setInDomain(ne1_domain);
				}else{
					intraSite=false;
					this.reservationDomain = null;
					requestBounded = false;
					element.setInDomain(Interdomain_Domain);
				}
			}else{
				if((ne1_domain!=null)&&(ne1_domain!=RequestReservation.Unbound_Domain))
					ne_domain=ne1_domain;
				if((ne2_domain!=null) &&(ne2_domain!=RequestReservation.Unbound_Domain))
					ne_domain=ne2_domain;
				if(ne_domain==null)
					ne_domain=reservationDomain;
				
				if(ne_domain==null){
					element.setInDomain(RequestReservation.Unbound_Domain);
					requestBounded = false;
				}				
				else{
					element.setInDomain(ne_domain);
				}
			}
			logger.debug("Doamin--element:"+element.getInDomain()+";ne1 domain="+ne1_domain+";ne2 domain="+ne2_domain+";reservation domain="+reservationDomain);
			setDomainRequestReservation(element,domainRequestReservation);
		}
		//System.out.println("RequestReservation Doamin--element:"+element.getInDomain()+";reservation domain="+reservationDomain);	
		//outputGraph(gomory_Input_File,numNode,links);
		
		return requestBounded;
	}
	
	//decide to call unboundhandler or mphandler which is for bounded inter-domain mp connection
	@SuppressWarnings("unchecked")
	public String ifMPConnection(NetworkConnection rc){
		LinkedList <NetworkElement> con_elements = (LinkedList<NetworkElement>)rc.getConnection();
		String e_domain=null,r_domain=con_elements.getFirst().getInDomain();
		String connection_domain=null;
		if(con_elements.size()>0){
			for(NetworkElement e:con_elements){
				e_domain = e.getInDomain();
				if(e_domain==null){
					connection_domain = RequestReservation.Unbound_Domain;
					break;
				}else if(!e_domain.equals(r_domain)){
					connection_domain = RequestReservation.MultiPoint_Domain;
					r_domain = connection_domain;
				}else{
					connection_domain = e_domain;
				}
			}
		}
		return connection_domain;
	}
	
	public void setDomainRequestReservation(NetworkElement element, HashMap <String, RequestReservation> dRR){
		String domain = element.getInDomain();
		RequestReservation rr = null;
		if(domain==null)
			return;
		if(dRR.containsKey(domain)){
			rr = dRR.get(domain);
			rr.setRequest(element);
		}else{
			rr=new RequestReservation();
			rr.setRequest(model,element,term,domain,reservation,reservation_rs);
			dRR.put(domain, rr);
		}
		addTypeToUnits(rr.getTypeTotalUnits());
	}
	
	public void addTypeToUnits(HashMap <String,Integer> t_u){
		if(this.getTypeTotalUnits()!=null){
			this.typeTotalUnits.putAll(t_u);
		}else{
			this.setTypeTotalUnits(t_u);
		}
	}
	
	public void setTypeTotalUnits(HashMap<String, Integer> typeTotalUnits) {
		this.typeTotalUnits = typeTotalUnits;
	}

//	public void setdomainRequestReservation(String domain,RequestReservation rr){
//		if(this.domainRequestReservation==null)
//			domainRequestReservation= new HashMap<String, RequestReservation>();
//		domainRequestReservation.put(domain, rr);
//	}
	
	public RequestReservation getDomainRequestReservation(String domain){
		if(this.domainRequestReservation==null)
			return null;
		return this.domainRequestReservation.get(domain);
	}

	public void setPureType(DomainResourceType rType,HashMap <String, Integer> typeTotalUnits){
		if(rType==null)
			rType = new DomainResourceType("vm",1);
		String pureType = rType.getResourceType();		
        int count = 0;
        if((typeTotalUnits ==null) || (pureType==null)){
        	System.out.println("Null pointer:"+typeTotalUnits+":"+pureType);
        	return;
        }
		if(typeTotalUnits.containsKey(pureType)){
        	count=typeTotalUnits.get(pureType);
        	count=count+rType.getCount();
        	typeTotalUnits.remove(pureType);
        	typeTotalUnits.put(pureType, count);
        }
        else{
        	count = rType.getCount();
        	typeTotalUnits.put(pureType, count);
        }
	}
	
	public int getPureTypeUnits(String type){
		return typeTotalUnits.get(type);
	}
	
	//mapping node url and sn
	public Integer getNodeMapKey(HashMap <Integer,String> nodeMap, String rs){
		Integer sn=0;
		for(Entry <Integer,String> entry:nodeMap.entrySet()){
			if(rs==entry.getValue()){
				sn=entry.getKey();
				break;
			}
		}
		return sn;
	}
	
	// output the graph to the format file as the input to the gomory code
	public void outputGraph(String outputFile, int numNode, Map<String, NetworkConnection> links) throws IOException{
	    Writer output = null;

	    File file = new File(outputFile);
	    output = new BufferedWriter(new FileWriter(file));
	    output.write(numNode+"\n");
	    output.write(links.size()+"\n");
	    
	    NetworkConnection requestConnection=null;
	    int rs1_sn = 0,rs2_sn = 0,i=1;
		long bw = 0;
	    for(Entry <String,NetworkConnection > entry:links.entrySet()){	
	    	requestConnection=entry.getValue();
	    	if((requestConnection.getNe1()==null) || (requestConnection.getNe2()==null))
	    		continue;
			rs1_sn=  requestConnection.getNe1().getSn();
			rs2_sn = requestConnection.getNe2().getSn();
			bw=requestConnection.getBandwidth();
			if(bw==0) bw=100;
			output.write(rs1_sn+" "+rs2_sn+" " + bw +"\n");
			i++;
	    }
	    
	    output.close();
	}
	
	public HashMap<String, RequestReservation> getDomainRequestReservation() {
		return domainRequestReservation;
	}
	
	public void setElements(Collection<NetworkElement> elements) {
		this.elements = elements;
	}
	
	public Collection<NetworkElement> getElements() {
		if ((elements == null) || (elements.size() == 0)) {
			err = new SystemNativeError();
			err.setErrno(-1);
			err.setMessage("Ndl request parser unable to parse request");
		}
		return elements;
	}

	public HashMap<String, Integer> getTypeTotalUnits() {
		return typeTotalUnits;
	}

	public void setModel(OntModel model) {
		this.model = model;
	}

	public OntModel getModel() {
		return model;
	}

	public OrcaReservationTerm getTerm() {
		return term;
	}

	public void setReservationDomain(String reservationDomain) {
		this.reservationDomain = reservationDomain;
	}

	public String getReservationDomain() {
		return reservationDomain;
	}

	public String getOri_reservationDomain() {
		return ori_reservationDomain;
	}

	public void setOri_reservationDomain(String ori_reservationDomain) {
		this.ori_reservationDomain = ori_reservationDomain;
	}

	public String getReservation() {
		return reservation;
	}

	public Resource getReservation_rs() {
		return reservation_rs;
	}

	public void setReservation_rs(Resource reservation_rs) {
		this.reservation_rs = reservation_rs;
	}

	public int getNumNetworkConnection() {
		return numNetworkConnection;
	}

	public RequestSlice getSlice() {
		return slice;
	}

	public void setSlice(RequestSlice slice) {
		this.slice = slice;
	}
	
}
