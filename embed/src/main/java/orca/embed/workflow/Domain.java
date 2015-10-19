package orca.embed.workflow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import orca.ndl.DomainResource;
import orca.ndl.DomainResourceType;
import orca.ndl.DomainResources;
import orca.ndl.NdlCommons;
import orca.ndl.NdlModel;
import orca.ndl.NdlException;
import orca.ndl.OntProcessor;
import orca.ndl.elements.DomainElement;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class Domain implements IDomainAbstractor{
	HashMap <Resource, Resource> scList;
	ArrayList <OntResource> intfList;
	protected String domain_model_str;
	protected DomainElement domainElement;
	
	protected String[] inferenceModels = {"geni.owl"};
	
	public static Logger logger = NdlCommons.getNdlLogger();
	
	public static void setLogger(Logger l) {
		logger = l;
	}
	
	public Domain(){};
	
	public Domain(String inputFile) throws IOException, NdlException {
		OntModel model = NdlModel.getModelFromFile(inputFile,OntModelSpec.OWL_MEM_RDFS_INF, true);
		init(model);
	}

	public Domain(InputStream stream) throws IOException, NdlException {
		OntModel model = NdlModel.getModelFromStream(stream, OntModelSpec.OWL_MEM_RDFS_INF,true);
		init(model);
	}
	
	private void init(OntModel model) throws NdlException {
		NdlCommons.setGlobalJenaRedirections();
		OntResource domain_ont = getDomain(model);
		
		if (domain_ont == null)
			throw new NdlException("Domain.init(): getDomain() failed to return domain resource:");

		domainElement = new DomainElement(model,domain_ont);
		scList = new HashMap <Resource,Resource> ();
		intfList=findBorderInterface(model);	
	}
	
	public String delegateFullModelToString(){
		String substrate=null;
		OntModel model=domainElement.getModel();
		if(model!=null){
			OutputStream out = new ByteArrayOutputStream();
			model.write(out);
			substrate = out.toString();
		}
		return substrate;
	}
	
	public String updateAbstactModel(String str, int total, BitSet bSet) throws NdlException{
		OntModel delegateModel = NdlModel.getModelFromString(str, OntModelSpec.OWL_MEM_RDFS_INF, true);
		for (String model: inferenceModels)
			delegateModel.read(NdlCommons.ORCA_NS + model);
        Resource domain=null;
        
        try {
        	for(ResIterator j=delegateModel.listResourcesWithProperty(NdlCommons.RDF_TYPE, NdlCommons.networkDomainOntClass);j.hasNext();){
        		domain = j.next();
        		break;
        	}
        } catch(ArrayIndexOutOfBoundsException e){
        	logger.error("updateAbstractModel(): Jena threw an ArrayIndexOutOfBoundsException, returning empty set of resources.");
        	return null;
        }
        
        Resource networkService = domain.getProperty(NdlCommons.domainHasServiceProperty).getResource();

        DomainResources res = new DomainResources();
        DomainResourceType resType=null;
        Resource set=null;
        int units=0, total_units=0;
        String type = null;
        LinkedList <Resource> unit_change_list = new LinkedList<Resource>(); 
        for (StmtIterator j=networkService.listProperties(NdlCommons.availableLabelSet);j.hasNext();){
			set = j.next().getResource();
			if(set.hasProperty(NdlCommons.domainIsAllocatable)){
				if(set.getProperty(NdlCommons.domainIsAllocatable).getBoolean()==false){
					continue;
				}
			}
			units = set.getProperty(NdlCommons.collectionSizeProperty).getInt();
			type = set.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource().getLocalName(); 
			if (type == null) {
				logger.error("Unable to find resource name in RDF Resource " + 
						set.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource() + ", continuing. Check your configuration.");
				continue;
			}
			
			int resourceTypeRank=0;
			if(!res.hasType(type)) {
				resType = new DomainResourceType(type, units);
				Statement typeRank = set.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource().getProperty(NdlCommons.resourceTypeRank);
				if(typeRank!=null)
					resourceTypeRank=typeRank.getInt();
				else
					resourceTypeRank = 20;
				resType.setRank(resourceTypeRank);
				resType.setDomainURL(domain.getURI());
				res.addResourceType(resType);
			}
			else res.addResourceType(type,units);

			if(units>total){
				logger.warn("Delegated less units:"+total+";units="+units);
				units=total;
			}
			total_units=total_units+units;
			unit_change_list.add(set);
        }
        
        if(total_units>=total){
			logger.warn("Modifying the abstract model with different delegated units from the config.xml or the available units from broker when updating in controller");
        	for(Resource change_set : unit_change_list){
        		Statement state = change_set.getProperty(NdlCommons.collectionSizeProperty);
        		if(total>0){
        			state.changeLiteralObject(total);
        			total=0;
        		}
        		else
        			state.changeLiteralObject(0);
        		if(bSet!=null){
        			String bSet_str=bSet.toString().replace("{", "").replace("}", "");
        			state = change_set.getProperty(NdlCommons.layerUsedLabels);
        			if(state==null)
        				change_set.addProperty(NdlCommons.layerUsedLabels, bSet_str);
        			else
        				state.changeObject(bSet_str);
        		}
        		logger.warn("Modifying set:"+change_set.getURI()+";New units="+change_set.getProperty(NdlCommons.collectionSizeProperty)
        				+";usedLabels="+change_set.getProperty(NdlCommons.layerUsedLabels));
        	}
		}else{
			logger.error(domain+":"+type+":Too many delegated units from the config.xml, total="+total+", than specified in the site RDF units="+total_units+", please modify and restart the actor!\n");
			throw new RuntimeException(domain+":"+type+";Too many delegated units from the config.xml, total="+total+", than specified in the site RDF units="+total_units+", please modify and restart the actor!\n");
		}
        
		//Since it's modified by the actual exported units, to be read by the broker policy
		OutputStream out = new ByteArrayOutputStream();
		delegateModel.write(out);
		domain_model_str=out.toString();
		
		//close the delegateModel
		NdlModel.closeModel(delegateModel);
		
		return domain_model_str;
	}
	
	// to be called by the broker policy for vlan resource, to get the allocatable resource <type, unit>, and <interface,bw>, etc constraints 
	// for VM/BareMetal, <metric, value> constraints on size*hasUnitServer values on cpu, memory, and storage
	public DomainResources getDomainResources(String str, int total) throws IOException, NdlException {
		OntModel delegateModel = NdlModel.getModelFromString(str, OntModelSpec.OWL_MEM_RDFS_INF, true);
		for (String model: inferenceModels)
			delegateModel.read(NdlCommons.ORCA_NS + model);
		
		DomainResources d_r = getDomainResources(delegateModel,total);
				
		return d_r;
	}
	
	public DomainResources getDomainResources(OntModel delegateModel, int total) throws IOException,RuntimeException{
        Resource domain=null;
        
        try {
        	for(ResIterator j=delegateModel.listResourcesWithProperty(NdlCommons.RDF_TYPE, NdlCommons.networkDomainOntClass);j.hasNext();){
        		domain = j.next();
        		break;
        	}
        } catch(ArrayIndexOutOfBoundsException e){
        	logger.error("getDomainResources(): Jena threw an ArrayIndexOutOfBoundsException, returning empty set of resources.");
        	return new DomainResources();
        }
        
        Resource networkService = domain.getProperty(NdlCommons.domainHasServiceProperty).getResource();

        DomainResources res = new DomainResources();
        DomainResourceType resType=null;
        Resource set=null;
        int units=0, total_units=0;
        String type = null;
        LinkedList <Resource> unit_change_list = new LinkedList<Resource>(); 
        for (StmtIterator j=networkService.listProperties(NdlCommons.availableLabelSet);j.hasNext();){
			set = j.next().getResource();
			if(set.hasProperty(NdlCommons.domainIsAllocatable)){
				if(set.getProperty(NdlCommons.domainIsAllocatable).getBoolean()==false){
					continue;
				}
			}
			units = set.getProperty(NdlCommons.collectionSizeProperty).getInt();
			type = set.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource().getLocalName(); 
			if (type == null) {
				logger.error("Unable to find resource name in RDF Resource " + 
						set.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource() + ", continuing. Check your configuration.");
				continue;
			}
			
			int resourceTypeRank=0;
			if(!res.hasType(type)) {
				resType = new DomainResourceType(type, units);
				Statement typeRank = set.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource().getProperty(NdlCommons.resourceTypeRank);
				if(typeRank!=null)
					resourceTypeRank=typeRank.getInt();
				else
					resourceTypeRank = 20;
				resType.setRank(resourceTypeRank);
				resType.setDomainURL(domain.getURI());
				res.addResourceType(resType);
			}
			else res.addResourceType(type,units);

			if(units>total){
				logger.warn("Delegated less units:"+total+";units="+units);
				units=total;
			}
			
			logger.info("Resource pool:"+domain+":"+type+":"+units+":"+resourceTypeRank+"\n");
			
			//if it is vm or baremetal, set up constraints: #cores, memory, storage for a site
			if(type.endsWith("VM") || type.endsWith("BareMetalCE")){
				if(set.hasProperty(NdlCommons.hasUnitServer)){
					Resource unitServer_rs = set.getProperty(NdlCommons.hasUnitServer).getResource();
					OntClass unitServer_cls = delegateModel.getOntClass(unitServer_rs.getURI());
					for(Iterator <OntClass> i = unitServer_cls.listSuperClasses();i.hasNext();){
						OntClass c=i.next();
						
						if(c!=null && c.isRestriction()){
							Restriction r = c.asRestriction();
							if(r.isHasValueRestriction())
								if(r.asHasValueRestriction().getHasValue().isLiteral()){
									String key = r.asHasValueRestriction().getOnProperty().getLocalName();
									int value = r.asHasValueRestriction().getHasValue().as(Literal.class).getInt();
									DomainResource constraint = new DomainResource(key);
									constraint.setBandwidth(value*units);
									res.addResource(constraint);
									logger.debug("vm constraint:"+key+";value="+value);
								}
						}
					}
				}
			}
			//if it is storage resource w/ lun
			if(type.endsWith("LUN")){
				if(set.hasProperty(NdlCommons.storageCapacity)){
					String key = NdlCommons.storageCapacity.getLocalName();
					int capacity = set.getProperty(NdlCommons.storageCapacity).getInt();
					DomainResource constraint = new DomainResource(key);
					constraint.setBandwidth(capacity);
					logger.debug("Lun constraint:"+key+";value="+capacity);
					res.addResource(constraint);
				}
			}			
			total_units=total_units+units;
			unit_change_list.add(set);
        }
        
        if(total_units>=total){
			logger.warn("Modifying the abstract model with different delegated units from the config.xml or the available units from broker when updating in controller");
        	for(Resource change_set : unit_change_list){
        		Statement state = change_set.getProperty(NdlCommons.collectionSizeProperty);
        		if(total>0){
        			state.changeLiteralObject(total);
        			total=0;
        		}
        		else
        			state.changeLiteralObject(0);
        		logger.warn("Modifying set:"+change_set.getURI()+";New units="+change_set.getProperty(NdlCommons.collectionSizeProperty));
        	}
		}else{
			logger.error(domain+":"+type+":Too many delegated units from the config.xml, total="+total+", than specified in the site RDF units="+total_units+", please modify and restart the actor!\n");
			throw new RuntimeException(domain+":"+type+";Too many delegated units from the config.xml, total="+total+", than specified in the site RDF units="+total_units+", please modify and restart the actor!\n");
		}
		
        //get <interface, bandwidth>
		OntResource intf;
		Literal bw=null;
		Resource rs=null;
		ResultSet results=null;
		String varName=null;
		for(StmtIterator i=domain.listProperties(NdlCommons.topologyHasInterfaceProperty);i.hasNext();){
			intf=delegateModel.getOntResource(i.next().getResource());
			String currentIntf = intf.getURI();
			while(true){			
				if(intf.hasProperty(NdlCommons.RDF_TYPE,delegateModel.createResource(NdlCommons.ORCA_NS+"ethernet.owl#EthernetNetworkElement"))){
					rs=intf;
					break;
				}	
				results=NdlCommons.getLayerAdapatation(delegateModel,intf.getURI());
				varName=(String) results.getResultVars().get(0); 
				if (results.hasNext()){
					rs=results.nextSolution().getResource(varName);				
				}
				else break;
				
				if(rs.hasProperty(NdlCommons.RDF_TYPE,delegateModel.createResource(NdlCommons.ORCA_NS+"ethernet.owl#EthernetNetworkElement"))){
					intf = delegateModel.getOntResource(rs);
					break;
				}
				else{
					intf = delegateModel.getOntResource(rs);
					continue;
				}
			}
			DomainResource r = new DomainResource(currentIntf);
			Resource ls = null;
			if(intf.getProperty(NdlCommons.availableLabelSet)!=null){
				ls = intf.getProperty(NdlCommons.availableLabelSet).getResource();
				if(ls.hasProperty(NdlCommons.domainHasResourceTypeProperty)){
					type = ls.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource().getLocalName();
					r.setLabel_list(NdlCommons.getLabelSet(ls.getURI(), type, delegateModel));
				}
			}

			if(intf.getProperty(NdlCommons.layerBandwidthProperty)!=null) 
				bw=intf.getProperty(NdlCommons.layerBandwidthProperty).getLiteral();

			long blong = 0;
			if(bw!=null) 
				blong=Long.parseLong(bw.getValue().toString());
			r.setBandwidth(blong);
			res.addResource(r);
			logger.info("Current Interface constraints:"+r.toString()+"\n");
		}
        
		//Since it's modified by the actual exported units, to be read by the broker policy
		OutputStream out = new ByteArrayOutputStream();
		delegateModel.write(out);
		domain_model_str=out.toString();
		
		//close the delegateModel
		NdlModel.closeModel(delegateModel);
		
		return res;
	}
	
	public String delegateDomainModelToString(String resourceType){
		OutputStream out = new ByteArrayOutputStream();
		OntModel delegateModel = delegateDomainModel(resourceType);
		delegateModel.write(out);
		NdlModel.closeModel(delegateModel);
        return out.toString();
	}
	
	public String delegateDomainModelToString(){
		OutputStream out = new ByteArrayOutputStream();
		OntModel delegateModel = delegateDomainModel(null);
		delegateModel.write(out);
		NdlModel.closeModel(delegateModel);
        return out.toString();
	}
	
	public OntModel delegateDomainModel(String resourceType){
		String rType=null;
		if(resourceType !=null){
			if(resourceType.indexOf(".")>=0)
				rType=resourceType.split("\\.")[1];
			else
				rType = resourceType;
		}else{
			logger.error("Domain Delegation Error:Required type is NULL!");
			return null;
		}
		
		HashMap <String,String> delegate = new HashMap <String,String> ();
		
		//1. Get the abstract NDL model, NetworService(delegated resource,etc.) and edge ports resource information will be added to it too. 
		//it will be converted to string and put at the end of the HashMap
		OntModel abstractModel = abstractDomain(rType);
        //model.write(System.out);
        //2. get the resource type and amount to be delegated
        Resource domain=getDomain();
        
        Resource networkService = domain.getRequiredProperty(NdlCommons.domainHasServiceProperty).getResource();
        Property [] serviceProperty = {NdlCommons.domainHasAccessMethod,NdlCommons.domainHasAggregateManager,NdlCommons.domainHasTopology,NdlCommons.atLayer,NdlCommons.hasCastType};
        for(Property i:serviceProperty){
        	for (StmtIterator j=networkService.listProperties(i);j.hasNext();){
        		Statement se_st = j.next();
        		abstractModel.add(se_st);
        		Resource se_rs=se_st.getResource();
        		if(se_rs!=null){
        			for (StmtIterator k=se_rs.listProperties();k.hasNext();){
        				abstractModel.add(k.next());
        			}
        		}
        	}
        }
        
        //FixMe: may need to add multiple layer types. only EthernetNetworkElement for now.
        //Probably it is OK, because only VLAN needs to be delegated as a domain-wide resource.
        
        //String layer = networkService.getProperty(atLayer).getResource().getLocalName();
        
        //String prefix = Layer.valueOf(layer).getPrefix().toString();
        //String aSet=NdlCommons.ORCA_NS+prefix+".owl#"+Layer.valueOf(layer).getASet();
		
		Resource set=null,type_rs=null,label_set=null;
		Statement unitServerStm,typeStm,setStm,elementStm;
		for (StmtIterator j=networkService.listProperties(NdlCommons.availableLabelSet);j.hasNext();){
			setStm = j.next();
			set = setStm.getResource();

			typeStm=set.getProperty(NdlCommons.domainHasResourceTypeProperty);
			type_rs=typeStm.getResource();	
			String local_rType = type_rs.getLocalName();
			if(local_rType.equalsIgnoreCase(rType)) {
				abstractModel.add(setStm);
				if(set.hasProperty(NdlCommons.domainIsAllocatable)){
					abstractModel.add(set.getProperty(NdlCommons.domainIsAllocatable));
				}
				abstractModel.add(set.getProperty(NdlCommons.collectionSizeProperty));
				if(set.hasProperty(NdlCommons.storageCapacity))
					abstractModel.add(set.getProperty(NdlCommons.storageCapacity));
				
				//resourceType properties
				abstractModel.add(typeStm);
				for (StmtIterator k=type_rs.listProperties();k.hasNext();){
					abstractModel.add(k.next());
				}
				unitServerStm =  set.getProperty(NdlCommons.hasUnitServer);
				if(unitServerStm != null){  //to get possible unit server (VM) specification
					abstractModel.add(unitServerStm);
					Resource unitServer=unitServerStm.getResource();
					for (StmtIterator element=unitServer.listProperties();element.hasNext();){
						abstractModel.add(element.next());
					}
				}
				
				//labelSet properties to be carried on
				for (StmtIterator element=set.listProperties(NdlCommons.collectionElementProperty);element.hasNext();){
					elementStm=element.next();
					abstractModel.add(elementStm);
					label_set=elementStm.getResource();
					getLabelSetProperties(label_set, abstractModel);
				}
			}
			else{
				abstractModel.removeAll(networkService, NdlCommons.availableLabelSet, set);
			}
		}
		return abstractModel;
	}
	
	//abstract domain to a device with all the border interfaces
	@SuppressWarnings("static-access")
	public OntModel abstractDomain(OntModel model, String rType){

		//OntModel abstractModel=ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

		OntModel abstractModel= null;
		try {
			abstractModel = NdlModel.createModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF, true);
		} catch (NdlException e1) {
			logger.error("abstractDomain(): unable to create abstract model: " + e1);
			e1.printStackTrace();
		}
        
		if (abstractModel == null) {
			logger.error("abstractDomain(): abstract model is null!");
		}
		
		ArrayList <OntResource> borderInterfaceList=intfList;
		OntResource intf,idm_intf=null;
		Statement statement;

		OntResource domain=getDomain(model);
		String domainName=null;
		if(rType==null) domainName= domain.getURI();
		else domainName= domain.getURI()+"/"+rType;
		
		OntResource device_rs=abstractModel.createIndividual(domainName,NdlCommons.deviceOntClass);
		for (StmtIterator j=domain.listProperties();j.hasNext();){
			statement = (Statement) j.next();
			device_rs.addProperty(statement.getPredicate(),statement.getObject());
		}
		
		Resource pop_rs,cluster_rs,set_rs,type_rs;
		Statement cluster_st,set_st;
		boolean include_pop = false,include_cluster=false;
		for (StmtIterator i=domain.listProperties(NdlCommons.collectionElementProperty);i.hasNext();){
			include_pop=false;
			pop_rs = i.next().getResource();
			for (StmtIterator j=pop_rs.listProperties(NdlCommons.collectionElementProperty);j.hasNext();){
				cluster_st=j.next();
				cluster_rs = cluster_st.getResource();
				include_cluster=false;
				for (StmtIterator k=cluster_rs.listProperties(NdlCommons.availableLabelSet);k.hasNext();){
					set_st=k.next();
					set_rs = set_st.getResource();
					if(set_rs.hasProperty(NdlCommons.domainHasResourceTypeProperty)){
						type_rs=set_rs.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource();					
						if(type_rs.getLocalName().equalsIgnoreCase(rType)){
							abstractModel.add(set_st);
							include_cluster=true;
							abstractModel.add(set_rs.getProperty(NdlCommons.domainHasResourceTypeProperty));
						}
					}
				}
				if(include_cluster){
					abstractModel.add(cluster_st);
					for (StmtIterator l=cluster_rs.listProperties(NdlCommons.RDF_TYPE);l.hasNext();){
						device_rs.addProperty(NdlCommons.RDF_TYPE,l.next().getResource());
					}
					include_pop=true;
				}
			}
			if(include_pop==true){
				device_rs.addProperty(NdlCommons.collectionElementProperty,pop_rs);
				abstractModel.add(pop_rs.listProperties().toList());
				if(pop_rs.hasProperty(NdlCommons.locationLocatedAtProperty)){
					Resource location_rs=pop_rs.getProperty(NdlCommons.locationLocatedAtProperty).getResource();
					for (StmtIterator l=location_rs.listProperties();l.hasNext();){
						abstractModel.add(l.next());
					}
				}
			}
		}
		
		device_rs.setLabel(domainName, "en");
		logger.info("Domain="+domainName+" :rType="+rType);
		for(Iterator <OntResource> i=borderInterfaceList.iterator();i.hasNext();){
			intf=i.next();
			//System.out.println("Interface="+intf.getURI());
			//also collect necessary adapted client interface information
			ResultSet results=null;
			Resource set=null,type=null;
			Statement s=null;
			boolean intf_type=false;
			if(rType!=null){
				if(!intf.listProperties(NdlCommons.availableLabelSet).hasNext()){
					if((rType.equalsIgnoreCase("vm")) || (rType.endsWith("baremetalce")) || (rType.equalsIgnoreCase("lun"))){
						if(isBorderInterface(intf,rType)){
							intf_type=true;
						}else{
							if(intf.hasProperty(NdlCommons.layerLabel) && rType.equalsIgnoreCase("lun"))
								intf_type=true;
						}
					}
					if(rType.equalsIgnoreCase("vlan")){
						if(!intf.hasProperty(NdlCommons.layerLabel)){
							intf_type=true;
							//break;	
						}
					}
				} else{
					for (StmtIterator j=intf.listProperties(NdlCommons.availableLabelSet);j.hasNext();){
						set = j.next().getResource();
						type=set.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource();
						//System.out.println(": type="+type+" :rType="+rType);
						if(type.getLocalName().equalsIgnoreCase(rType)){
							intf_type=true;
							break;
						}
						if(type.getLocalName().equalsIgnoreCase("VLAN")){
							if(isBorderInterface(intf,rType)){
								if(!intf.hasProperty(NdlCommons.layerLabel)){
									intf_type=true;
									break;
								}
							}
						}
					}
				}
			}
			else intf_type=true;
			
			if(intf_type){
				idm_intf=abstractModel.createIndividual(intf.getURI(),NdlCommons.interfaceOntClass);
				
				idm_intf.addProperty(NdlCommons.RDFS_SeeAlso,intf.getURI().split("\\#")[0]);
				addSwitchingMatrix(abstractModel,device_rs,intf);
				device_rs.addProperty(NdlCommons.topologyHasInterfaceProperty,idm_intf);
				idm_intf.addProperty(NdlCommons.topologyInterfaceOfProperty, device_rs);
			
				Resource currentIntf_rs = intf;

				while(true){	
					for (StmtIterator j=currentIntf_rs.listProperties();j.hasNext();){
						statement = (Statement) j.next();
						abstractModel.add(statement);
						/*if(statement.getPredicate().equals(NdlCommons.RDF_TYPE)){
							for (StmtIterator k=statement.getResource().listProperties();k.hasNext();){
								abstractModel.add(k.next());
							}
						}*/
					}
					for (StmtIterator j=currentIntf_rs.listProperties(NdlCommons.linkTo);j.hasNext();){
						set = j.next().getResource();
						if(set.hasProperty(NdlCommons.hasURNProperty)){
							abstractModel.add(set.getProperty(NdlCommons.hasURNProperty));
						}
					}
					
					for (StmtIterator j=currentIntf_rs.listProperties(NdlCommons.layerLabel);j.hasNext();){
						set = j.next().getResource();
						if(set.hasProperty(NdlCommons.layerLabelIdProperty)){
							abstractModel.add(set.getProperty(NdlCommons.layerLabelIdProperty));
						}
						Resource ip_addr=null;
						for (StmtIterator k=set.listProperties(NdlCommons.ip4LocalIPAddressProperty);k.hasNext();){
							statement = (Statement) k.next();
							abstractModel.add(statement);
							ip_addr = statement.getResource();
							if(ip_addr.hasProperty(NdlCommons.layerLabelIdProperty))
								abstractModel.add(ip_addr.getProperty(NdlCommons.layerLabelIdProperty));
							if(ip_addr.hasProperty(NdlCommons.ip4NetmaskProperty))
								abstractModel.add(ip_addr.getProperty(NdlCommons.ip4NetmaskProperty));
							if(ip_addr.hasProperty(NdlCommons.layerLabelIsPrimary))
								abstractModel.add(ip_addr.getProperty(NdlCommons.layerLabelIsPrimary));
						}
					}
					
					for (StmtIterator j=currentIntf_rs.listProperties(NdlCommons.availableLabelSet);j.hasNext();){
						set = j.next().getResource();
						if(set.getProperty(NdlCommons.domainHasResourceTypeProperty)!=null)
							type=set.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource();

						for (StmtIterator k=set.listProperties();k.hasNext();){
							s=k.next();
							abstractModel.add(s);
						}
						//labelSet properties to be carried on
						Resource label_set=null;
						Statement elementStm;
						for (StmtIterator element=set.listProperties(NdlCommons.collectionElementProperty);element.hasNext();){
							elementStm=element.next();
							abstractModel.add(elementStm);
							label_set=elementStm.getResource();
							getLabelSetProperties(label_set, abstractModel);
						}
						
						if(type!=null){
							for (StmtIterator k=type.listProperties();k.hasNext();){
								s=k.next();
								abstractModel.add(s);
							}
						}
					}
					
					results=NdlCommons.getLayerAdapatation(domainElement.getModel(),currentIntf_rs.getURI());
					String varName=(String) results.getResultVars().get(0); 
					if (results.hasNext()){
						currentIntf_rs=results.nextSolution().getResource(varName);				
					}
					else break;					
				}				
				
			}
		}

		return abstractModel;	
	}

	public void getLabelSetProperties(Resource label_set, OntModel abstractModel){
		Resource label=null;
		Statement setStm,lbStm,ubStm,swapStm;
		lbStm=label_set.getProperty(NdlCommons.lowerBound);
		ubStm=label_set.getProperty(NdlCommons.upperBound);
		if(lbStm!=null){
			abstractModel.add(lbStm);
			label=lbStm.getResource();
			for (StmtIterator labelIt=label.listProperties();labelIt.hasNext();){
				abstractModel.add(labelIt.next());
			}
		}
		if(ubStm!=null){
			abstractModel.add(ubStm);
			label=ubStm.getResource();
			for (StmtIterator labelIt=label.listProperties();labelIt.hasNext();){
				abstractModel.add(labelIt.next());
			}
		}
		//single label
		if((lbStm==null) && (ubStm==null)){
			setStm=label_set.getProperty(NdlCommons.layerLabelIdProperty);
			if(setStm!=null){
				abstractModel.add(setStm);
			}
			setStm=label_set.getProperty(NdlCommons.layerSwapLabelProperty);
			if(setStm!=null){
				abstractModel.add(setStm);
				swapStm = setStm.getResource().getProperty(NdlCommons.layerLabelIdProperty);
				if(swapStm!=null)
					abstractModel.add(swapStm);
			}
		}
	}
	//abstract domain to a device
	public OntModel abstractDomain(String rType){

		return this.abstractDomain(domainElement.getModel(),rType);	
	}
	

	public boolean isBorderInterface(OntResource intf_ont, String rType){
		boolean is = false;
		Resource type=null;
		//if(intf_ont.hasProperty(NdlCommons.topologyInterfaceOfProperty)){	
			//Resource edge = intf_ont.getProperty(NdlCommons.topologyInterfaceOfProperty).getResource();
		for(StmtIterator i=intf_ont.listProperties(NdlCommons.topologyInterfaceOfProperty);i.hasNext();){
			Resource edge = i.next().getResource();
			if(edge.hasProperty(NdlCommons.availableLabelSet)){
				Resource set = edge.getProperty(NdlCommons.availableLabelSet).getResource();
				if(set.getProperty(NdlCommons.domainHasResourceTypeProperty)!=null)
					type=set.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource();
					if(type.getLocalName().equalsIgnoreCase(rType)){
						is=true;
					}
			}
		}
		
		return is;
	}
	
	@SuppressWarnings("static-access")
	public void addSwitchingMatrix(OntModel abstractModel,OntResource device,OntResource intf){
		ResultSet results=NdlCommons.getInterfaceOfSwitching(domainElement.getModel(),intf.getURI());
		String var0=(String) results.getResultVars().get(0);
		Resource sm,sc,sw,st;
		OntResource sm_ont;
		while(results.hasNext()){
			sm=results.nextSolution().getResource(var0);
			sc=sm.getProperty(NdlCommons.switchingCapability).getResource();
			sw=sm.getProperty(NdlCommons.swappingCapability)==null?null:sm.getProperty(NdlCommons.swappingCapability).getResource();
			st=sm.getProperty(NdlCommons.tunnelingCapability)==null?null:sm.getProperty(NdlCommons.tunnelingCapability).getResource();
			//System.out.print("TunnelCapability:"+sm+"::"+st+"\n");
			if(!scList.isEmpty()) {
				if(scList.get(sc)!=null)
					if(scList.get(sc).equals(sw)) continue;
			}
			else {
				sm_ont=abstractModel.createIndividual(domainElement.getURI()+"/SwitchingMatrix",NdlCommons.switchingMatrixOntClass);
				device.addProperty(NdlCommons.hasSwitchMatrix, sm_ont);
				sm_ont.addProperty(NdlCommons.switchingCapability, sc);
				if(sw!=null) {
					sm_ont.addProperty(NdlCommons.swappingCapability, sw);
					scList.put(sc,sw);
				}
				if(st!=null){
					sm_ont.addProperty(NdlCommons.tunnelingCapability, st);
				}
			}
		}
	}
	
	@SuppressWarnings("static-access")
	public ArrayList <OntResource> findBorderInterface(OntModel model){
		intfList=new ArrayList<OntResource>();
		
		String queryPhrase=NdlCommons.createQueryStringLinkClass("<"+NdlCommons.interfaceOntClass.getURI()+">");
		ResultSet results = OntProcessor.rdfQuery(model,queryPhrase);
		String var0=(String) results.getResultVars().get(0);
		String var1=(String) results.getResultVars().get(1);
		Resource rs1,rs2;
		
		QuerySolution solution;
		while(results.hasNext()){
			solution=results.nextSolution();
			rs1=solution.getResource(var0);
			rs2=solution.getResource(var1);
			String name = domainElement.getName();
			if(!name.equals(rs2.getURI().split("\\#")[0])){
				if(!name.split("\\.rdf")[0].split("\\-\\w*$")[0].equals(rs2.getURI().split("\\#")[0].split("\\.rdf")[0].split("\\-\\w*$")[0])){
					intfList.add(model.getOntResource(rs1));
					//logger.info("Border Interface:"+rs1);
				}
			}
		}
		
		return intfList;
	}
	
	@SuppressWarnings("static-access")
	public OntResource getDomain(OntModel aM) {
		String type="ndl:NetworkDomain";
		
		return NdlCommons.getOntOfType(aM, type);
	}
	
	public ArrayList<OntResource> getIntfList() {
		return intfList;
	}

	public void setIntfList(ArrayList<OntResource> intfList) {
		this.intfList = intfList;
	}	
	
	public DomainElement getDomainElement(){
		return domainElement;
	}
	
	public OntResource getDomain(){
		return getDomain(domainElement.getModel());
	}
	
	public void print(Logger logger){
		domainElement.print(logger);
	}

	public HashMap<Resource, Resource> getScList() {
		return scList;
	}

	public void setScList(HashMap<Resource, Resource> scList) {
		this.scList = scList;
	}

	public String getDomain_model_str() {
		return domain_model_str;
	}

	public void setDomain_model_str(String domain_model_str) {
		this.domain_model_str = domain_model_str;
	}
}
