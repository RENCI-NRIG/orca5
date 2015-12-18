package orca.controllers.xmlrpc;

import java.util.*;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntResource;

import orca.ndl.NdlCommons;
import orca.ndl.elements.*;
import orca.shirako.container.Globals;

/**
 *
 * @author pruth
 */



public class ReservationElementCollection {
	public static final String NotInGroup = "NotInGroup";
	
	Collection<NetworkElement> elements;
	HashMap <String,Collection <DomainElement>> NodeGroupMap;
	HashMap <String,DomainElement> AllNodeMap;
	HashMap <String,DomainElement> firstGroupElement;
	
	public Logger logger;
	
	public ReservationElementCollection() {
		this.elements = null; 
	}
	
	public ReservationElementCollection(Collection<NetworkElement> elements,HashMap <String,DomainElement> fge, Logger log) {
		this.elements = elements; 
		this.firstGroupElement = fge;
		logger=log;
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
				addMacAddress(de);
				
				addNodeToGroup(group, de);
				AllNodeMap.put(de.getName(), de);
			}
		}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void addMacAddress(DomainElement de){
		LinkedList <Interface> interfaces = de.getClientInterface();
		if(interfaces!=null){
			for(Interface intf:interfaces){
				OntResource intf_ont = intf.getResource();
				if(intf_ont==null){
					logger.error("addMacAddress: intf has no Resource"+intf.getURI()+";name="+intf.getName());
					continue;
				}
				if(intf_ont.getProperty(NdlCommons.ipMacAddressProperty)==null)
					intf_ont.addProperty(NdlCommons.ipMacAddressProperty,generateNewMAC());
			}
		}
	}
	
	private final XmlrpcOrcaState orca_state_instance = XmlrpcOrcaState.getInstance();
    private String generateNewMAC(){
        //Generates libvirt compliant random mac addr (hopefully this.hashCode() is random enough
    	String new_mac = null;
    	while(true){
    		long l = Globals.secureRandom.nextLong();
         
    		l = Math.abs(l);

    		StringBuffer m = new StringBuffer(Long.toString(l, 16));
    		while (m.length() < 4) m.insert(0, "0");
    		
    		new_mac="fe:16:3e:00:" + m.substring(0,2) + ":" + m.substring(2,4);
    		
    		if(!orca_state_instance.existingUsedMac(new_mac)){
    			orca_state_instance.setUsedMac(new_mac);
    			break;
    		}
    	}
        return new_mac;
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
		if(!this.firstGroupElement.containsKey(group))
			this.firstGroupElement.put(group,ce);
	}
	
	private Collection<String> getNameCollection(Collection ces){
		Collection <String> nameColection = null;
		if(ces!=null){
			nameColection = new LinkedList <String>();
			Iterator <NetworkElement>ces_it = ces.iterator();
			while(ces_it.hasNext()){
				DomainElement ne = (DomainElement) ces_it.next();
				nameColection.add(ne.getName());
			}
		}
		return nameColection;
	}
	
	 //Returns the MAC of an iface of a vm attached to a given link
	 private OntResource vm_getIntfOnt(String vm, String link){
		 DomainElement de = AllNodeMap.get(vm);
		 OntResource intf_ont=null;
		 if(de!=null){
			if(de.getPrecededBySet()!=null){
			 for (Entry<DomainElement, OntResource> parent : de.getPrecededBySet()) {
				if(parent.getKey().getName().equals(link)){
					 intf_ont = parent.getValue();
					 break;
				 }
			 }
			}
		 }
		 return intf_ont;
	 }

	private String getLocalName(String n){
		if(n==null)
			return null;
		int i = n.lastIndexOf("#");
		
		return i>=0 ? n.substring(i+1):n;
	}

	/**
	 * 
	 *  The remainder of the methods are stubs that are needed for the velocity bootscripts
	 * 
	 *  Yufeng needs to fill them in.
	 * 
	 *  I am assuming that the vms and links are known by URL and that the velocity template 
	 *  does not know anything about the format of the URL.
	 */
	
	/* methods for vm info */
	
	//Returns the URL names of all VMs
	public Collection <String> vm_getVMs(){
		return AllNodeMap.keySet();
	 }
	
	//Returns the URL names of all VMs not in a group
	public Collection<String> vm_getVMsNotInGroup(){
		 Collection <DomainElement> ces =  this.NodeGroupMap.get(NotInGroup);
		 return getNameCollection(ces);
	}
	
	//Returns the name of the VM (name seen in flukes, not the URL)
	public String vm_getName(String vm){
		DomainElement de = AllNodeMap.get(vm);
		if(de==null){
			return null;
		}
		String name = de.getName();
		return this.getLocalName(name);
	 }
	
	 //Returns the IP address of an iface of a vm attached to a given link
	 public String vm_getIfaceIP(String vm, String link){
		 String interfaceIP=null;
		 OntResource intf_ont=this.vm_getIntfOnt(vm, link);
		 if(intf_ont==null)
			return null;
		 if(intf_ont.getProperty(NdlCommons.layerLabelIdProperty)!=null)
				interfaceIP=intf_ont.getProperty(NdlCommons.layerLabelIdProperty).getString();
		return interfaceIP;
	 }
	
	 //Returns the MAC of an iface of a vm attached to a given link
	 public String vm_getIfaceMAC(String vm, String link){
		 String interfaceMac=null;
		 OntResource intf_ont=this.vm_getIntfOnt(vm, link);
		 if(intf_ont==null)
                        return null;
		 if(intf_ont.getProperty(NdlCommons.ipMacAddressProperty)!=null)
				interfaceMac=intf_ont.getProperty(NdlCommons.ipMacAddressProperty).getString();
		return interfaceMac;
	 }

	//Returns the URL names of all links attached to a vm
	public Collection<String> vm_getAllLinks(String vm){
		 DomainElement de = AllNodeMap.get(vm);
		 Set <String> linkCollection = new HashSet();
		 if(de!=null){
			if(de.getPrecededBySet()!=null){
			 for (Entry<DomainElement, OntResource> parent : de.getPrecededBySet()) {
				 DomainElement parent_de = parent.getKey();
				 String de_url = parent_de.getName();
				 linkCollection.add(de_url); 
			 }
			}
		 }
		return linkCollection;
	 }
	  
	/* methods for group info */
	
	// Returns the URL names of all groups)
	public Collection<String> group_getGroups(){
		
			 return NodeGroupMap.keySet();
	}
	
	// Returns the URL names of all vms in a group indexed consistently
	public List<String> group_getVMsInGroup(String group){
		Collection <DomainElement> ces =  this.NodeGroupMap.get(group);
		if(ces==null)
			logger.error("ReservationElement: ces is null, group="+group+"NodeGroupMap.szie="+NodeGroupMap.size());
		return (LinkedList<String>) getNameCollection(ces);
	 }
	
	 
	//Returns the name of the group (name seen in flukes, not the URL)
	public String group_getName(String group){
		 return this.getLocalName(group);
	 }
		
	//Returns the  number of vms in a group
	public int group_getSize(String group){
		Collection <DomainElement> ces =  this.NodeGroupMap.get(group);
		if(ces!=null)
			return ces.size();
		 return 0;
	 }
	
	 //Returns the IP address of an iface attached to a given link for a specific vm in a group 
	 public String group_getIfaceIP(String group, int vmIndex, String link){
		 Collection <DomainElement> ces =  this.NodeGroupMap.get(group);
		 LinkedList <String> nameList = (LinkedList<String>) getNameCollection(ces);
		 String vm = nameList.get(vmIndex);
		 
		 String IPAddr = vm_getIfaceIP(vm, link);
		 return IPAddr;
	 }
		
	 //Returns the MAC of an iface attached to a given link for a specific vm in a group
	 public String group_getIfaceMAC(String group, int vmIndex, String link){
		 Collection <DomainElement> ces =  this.NodeGroupMap.get(group);
		 LinkedList <String> nameList = (LinkedList<String>) getNameCollection(ces);
		 String vm = nameList.get(vmIndex);
		 
		 String MacAddr = vm_getIfaceMAC(vm, link);
		 return MacAddr;
	 }

	//Returns the URL names of all links attached to a group
	public Collection<String> group_getAllLinks(String group){
		Collection <DomainElement> ces =  this.NodeGroupMap.get(group);
		Set <String> linkCollection = new HashSet();
		for(DomainElement de:ces){
		     if(de.getPrecededBySet()!=null){		
			for (Entry<DomainElement, OntResource> parent : de.getPrecededBySet()) {
				 DomainElement parent_de = parent.getKey();
				 String de_url = parent_de.getURI();
				 linkCollection.add(de_url);
			 }
		     }
		}
		 return linkCollection;
	 }
	 
	/* Methods for Link info */
	 
	//Returns the name of the link (name seen in flukes, not the URL)
	public String link_getName(String link,String vm){
		 DomainElement de = AllNodeMap.get(vm);
		 if(de==null)
			 return null;
		 OntResource intf_ont=null;

		 if(de.getPrecededBySet()!=null){
			 for (Entry<DomainElement, OntResource> parent : de.getPrecededBySet()) {
				if(parent.getKey().getName().equals(link)){
					 intf_ont = parent.getValue();
					 break;
				 }
			 }
		 }
		 String connectionName = null;	
		 ComputeElement ce=(ComputeElement)de.getCe();
		 if((ce!=null) && (intf_ont!=null)){
			NetworkConnection nc = ce.getConnectionByInterfaceURI(intf_ont.getURI());
		 	if(nc!=null)
				connectionName = nc.getName();	
		 }else{
			System.out.println("ReservationElementCollection: no ce or intf to find the link!");
		 }
		 return this.getLocalName(connectionName);
	}
	
	public void add_vm(DomainElement de){
		if(de==null)
			return;
		String name = de.getName();
		this.addMacAddress(de);
		if(!AllNodeMap.containsKey(name))
			AllNodeMap.put(name,de);
	}
	
	public void remove_vm(String name){
		NetworkElement ne = AllNodeMap.get(name);
		Collection <DomainElement> ces=null;
		if(ne!=null){
			AllNodeMap.remove(name);
			for(Entry <String,Collection <DomainElement>> entry: NodeGroupMap.entrySet()){
				ces = entry.getValue();
				if(ces.contains(ne))
					ces.remove(ne);
			}			
		}
	}
	
	//Returns the vlan tag assigned to a link
	//public String link_getVlanTag(String link){
	//	return null;
	//}
	
	
	
	
}
