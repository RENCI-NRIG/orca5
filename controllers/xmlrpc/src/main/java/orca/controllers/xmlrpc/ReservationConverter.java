package orca.controllers.xmlrpc;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import orca.controllers.OrcaController;
import orca.controllers.xmlrpc.geni.IGeniAmV2Interface.GeniStates;
import orca.embed.cloudembed.controller.InterCloudHandler;
import orca.embed.workflow.ModifyReservations;
import orca.embed.workflow.RequestWorkflow;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.OrcaConverter;
import orca.manage.beans.LeaseReservationMng;
import orca.manage.beans.PropertyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.ReservationPredecessorMng;
import orca.manage.beans.TicketReservationMng;
import orca.manage.beans.UnitMng;
import orca.ndl.DomainResource;
import orca.ndl.INdlModifyModelListener.ModifyType;
import orca.ndl.LayerConstant;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlModel;
import orca.ndl.OntProcessor;
import orca.ndl.elements.ComputeElement;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.IPAddressRange;
import orca.ndl.elements.Interface;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.OrcaReservationTerm;
import orca.ndl.elements.RequestSlice;
import orca.ndl.elements.SwitchingAction;
import orca.shirako.common.ReservationID;
import orca.shirako.common.meta.ConfigurationProperties;
import orca.shirako.common.meta.RequestProperties;
import orca.shirako.common.meta.UnitProperties;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.ResultBinding;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;

public class ReservationConverter implements LayerConstant {
	static final String SUDO_NO = "no";
	static final String SUDO_YES = "yes";
	static final String OPENSTACK_MAC_PREFIX = "fe:16:3e:00:";
	public static final String UNIT_URL_RES = UnitProperties.UnitURL;
	private static final long TWO_WEEKS = (14*24*3600*1000);
	private static final long ONE_DAY = (24*3600*1000);
	public static final long DEFAULT_MAX_DURATION = TWO_WEEKS;
	public static final long DEFAULT_DURATION = ONE_DAY;
	public static final String NO_SSH_KEY_SPECIFIED_STRING = "NO-SSH-KEY-SPECIFIED";
	public static final String PropertyRequestNdl = "request.ndl";
	public static final String PropertyUnitEC2InstanceType = "unit.ec2.instance.type";
	public static final String PropertyConfigDuration = "config.duration";
	public static final String PropertyConfigStartTime = "config.start_time";
	public static final String PropertyConfigEndTime = "config.end_time";
	public static final String PropertyConfigUnitTag = "config.unit.tag";
	public static final String PropertyUnitOFSliceCtrlUrl = "unit.openflow.slice.ctrl.url";
	public static final String PropertyUnitOFSliceEmail = "unit.openflow.slice.email";
	public static final String PropertyUnitOFSlicePass = "unit.openflow.slice.pass";
	public static final String PropertyUnitEC2Instance = UnitProperties.UnitEC2Instance;
	public static final String PropertyUnitEC2Host = "unit.ec2.host";
	public static final String PropertyUnitSliceName = UnitProperties.UnitSliceName;
	public static final String PropertyParentNumInterface = "unit.number.interface";
	public static final String PropertyParentNumStorage = "unit.number.storage";
	
	public static final String PropertyModifyVersion = "modify.version";
	public static final String PropertyNumExistParentReservations = "num.parent.exist";
	public static final String PropertyNumNewParentReservations = "num.parent.new";
	public static final String PropertyExistParent = "parent.exist.";
	public static final String PropertyNewParent = "parent.new.";
	public static final String PropertyIsNetwork= "local.isNetwork";
	public static final String PropertyIsLUN= "local.isLUN";
	public static final String PropertyIsVM= "local.isVM";
	public static final String PropertyElementGUID= "element.GUID";
	
	public static final String LOGIN_FIELD = "login";
	public static final String KEYS_FIELD = "keys";
	public static final String SUDO_FIELD = "sudo";
	public static final String URN_FIELD = "urn";
	
	private static final Pattern macNamePattern = Pattern.compile(UnitProperties.UnitEthPrefix + "[\\d]+" + UnitProperties.UnitEthMacSuffix);
	
	public static final String ISCSI_Initiator_Iqn_prefix = "iqn.2012-02.net.exogeni:";
	
	protected static String noopConfigFile = "handlers/common/noop.xml";

	public Logger logger;

	// constructor
	protected Properties usersSSHProperties;

	// constructor
	IOrcaServiceManager sm;
	
	// constructor
	XmlrpcControllerSlice ndlSlice;
	
	// set by setter
	protected Date leaseStart;
	protected Date leaseEnd;
	
	// static
	private final XmlrpcOrcaState orca_state_instance = XmlrpcOrcaState.getInstance();

	// restored from manifest model (by workflow parser)
	ReservationElementCollection elementCollection;
	// restored from manifest model (by workflow parser)
	HashMap <String,DomainElement> firstGroupElement;

	public ReservationConverter() {
		logger = NdlCommons.getNdlLogger();
	}

	public ReservationConverter(List<Map<String, ?>> u, IOrcaServiceManager asm, XmlrpcControllerSlice sl) throws ReservationConverterException {
		
		// convert list of logins/keys to properties
		usersSSHProperties = generateSSHProperties(u);
		sm = asm;
		ndlSlice=sl;
		logger = NdlCommons.getNdlLogger();
	}

	public ReservationElementCollection getElementCollection() {
		return elementCollection;
	}
	
	protected ReservationElementCollection recoverElementCollection(Collection<NetworkElement> boundElements) {
		if(firstGroupElement==null)
			firstGroupElement=new HashMap <String,DomainElement>();
		if(boundElements==null){
			logger.error("No deviceList in the reservation!");
			return null;
		}
		elementCollection = new ReservationElementCollection(boundElements,firstGroupElement,logger);
		return elementCollection;
	}
	
	public ArrayList<TicketReservationMng> getReservations(IOrcaServiceManager sm, Collection<NetworkElement> boundElements, HashMap<String, SiteResourceTypes> typesMap,OrcaReservationTerm OTerm, RequestSlice rSlice)  throws ReservationConverterException, NdlException {
		setLeaseTerm(OTerm, false);
		
		if(firstGroupElement==null)
			firstGroupElement=new HashMap <String,DomainElement>();
		//System.out.println("Total elements:"+boundElements.size());		
		elementCollection = new ReservationElementCollection(boundElements,firstGroupElement,logger);
		
		HashMap<String, ReservationRequest> map= formReservations(sm, boundElements, typesMap, rSlice);
		return setDependency(boundElements, map);
	}
	
	public void setRequestConstraints(Properties request, NetworkElement element){
		String pro_prefix = "request.";
		String p=null;
		HashMap<String, DomainResource> map = element.getResourcesMap();
		if(map==null){
			logger.error("ReservationConverter:No constraints on VM request!");
			return;
		}
		for(Entry<String, DomainResource>entry: map.entrySet()){
			p = pro_prefix + entry.getKey();
			request.setProperty(p,String.valueOf(entry.getValue().getBandwidth()));
		}
	}

	public HashMap<String, ReservationRequest> formReservations(IOrcaServiceManager sm, Collection<NetworkElement> boundElements, HashMap<String, SiteResourceTypes> typesMap, RequestSlice rSlice) throws NdlException {
		HashMap<String, ReservationRequest> map = new HashMap<String, ReservationRequest>();

		if(typesMap==null){
			logger.error("typsMap from the broker is null!");
			return null;
		}
		
		List <ReservationMng> extra_ar = new ArrayList <ReservationMng> ();//redundant due to modify, not added to SM
		
		for(NetworkElement device:boundElements){
			DomainElement de = (DomainElement) device;
			if(de.isAllocatable()==false)
				continue;
			
			String domain = getDomainName(device);
			logger.debug("Domain name in RR:"+domain+":device url:"+device.getURI()+"name:"+device.getName());
			if(typesMap.get(domain)==null){
				logger.error("No type in typesMap for domain:"+domain);
				continue;
			}
			SiteResourceType type = typesMap.get(domain).getDefaultResource();
			LeaseReservationMng r = new LeaseReservationMng();
			r.setUnits(device.getResourceType().getCount());
			r.setResourceType(type.getResourceType().toString());
			r.setSliceID(ndlSlice.getSliceID());
			r.setStart(leaseStart.getTime());
			r.setEnd(leaseEnd.getTime());
			r.setRenewable(false);
	
			Properties local = new Properties();
			Properties config = new Properties();
			Properties request = new Properties();
			
			// no SM-side handler
			// WRITEME:
			//local.setProperty(AntConfig.PropertyXmlFile, noopConfigFile);
			config.setProperty(PropertyConfigDuration, String.valueOf(leaseEnd.getTime()/1000-leaseStart.getTime()/1000));
			if(leaseStart!=null){
				long relative_start = leaseStart.getTime()/1000 - System.currentTimeMillis()/1000;
				if(relative_start<0)
					relative_start=0;
				config.setProperty(PropertyConfigStartTime, String.valueOf(relative_start));
			}
			if(leaseEnd!=null){
				long relative_end = leaseEnd.getTime()/1000 - System.currentTimeMillis()/1000;
				if(relative_end<0)
					relative_end=0;
				config.setProperty(PropertyConfigEndTime, String.valueOf(relative_end));
			}
			
			// merge the accounts/ssh keys structure
			config.putAll(usersSSHProperties);

			ReservationRequest resrequest = new ReservationRequest();
			resrequest.uuid = UUID.randomUUID().toString();
			resrequest.reservation = r;
			resrequest.domain = domain;
			resrequest.domain_url = device.getName();
			
			if(de.getCe()==null){
				resrequest.isNetwork = true;
				local.setProperty(PropertyIsNetwork,"1");
			}else if(de.getCastType()!=null && de.getCastType().equalsIgnoreCase(NdlCommons.multicast)){
				resrequest.isNetwork = true;
				local.setProperty(PropertyIsNetwork,"1");
			}else if(type.getResourceType().toString().endsWith("lun")){
				resrequest.isLUN=true;
				local.setProperty(PropertyIsLUN,"1");
			}else{
				resrequest.isVM=true;
				local.setProperty(PropertyIsVM,"1");
            }

			if(map.containsKey(device.getName()) && resrequest.isVM)
				extra_ar.add(r);
			else
				map.put(device.getName(), resrequest);
			
			if(ndlSlice.getSliceUrn()!=null){
				config.setProperty(PropertyUnitSliceName, ndlSlice.getSliceUrn());
				local.setProperty(PropertyUnitSliceName, ndlSlice.getSliceUrn());
			}

			String device_name=device.getName();
			logger.debug(UNIT_URL_RES+" "+device_name+";uni.domain="+device.getURI());
			local.setProperty(UNIT_URL_RES, device_name);
			local.setProperty(UnitProperties.UnitHostName,getDomainLocalName(device_name));
			config.setProperty(UnitProperties.UnitDomain,device.getURI());
			if (resrequest.isVM) {
				config.setProperty(UnitProperties.UnitHostNameUrl, device_name);
						
				ComputeElement ce = de.getCe();
				if (ce.getVMImageURL() != null) {
					config.setProperty(ConfigurationProperties.ConfigImageUrl, ce.getVMImageURL());
				}
				if (ce.getVMImageHash() != null) {
					config.setProperty(ConfigurationProperties.ConfigImageGuid,ce.getVMImageHash());
				}
				if (ce.getSpecificCEType() != null) {
					config.setProperty(PropertyUnitEC2InstanceType,ce.getSpecificCEType());
				}
				if(de.getGUID()!=null){
					local.setProperty(PropertyElementGUID,de.getGUID());
					config.setProperty(PropertyElementGUID,de.getGUID());
				}
				
				//set reconstraints
				setRequestConstraints(request, ce);
			}

			if(resrequest.isLUN){
				ComputeElement ce = de.getCe();
				
				config.setProperty(UnitProperties.UnitHostNameUrl, device_name);
	
				config.setProperty(UnitProperties.UnitTargetPrefix+".segment_size","128");   
				config.setProperty(UnitProperties.UnitISCSIInitiatorIQNPrefix,ISCSI_Initiator_Iqn_prefix);
						
				String CHAP_User = NdlCommons.getBigString(32);
				String CHAP_Password = NdlCommons.getBigString(130);
				ce.setCHAP_User(CHAP_User);
				ce.setCHAP_Password(CHAP_Password);
				config.setProperty(UnitProperties.UnitTargetPrefix+".chap_user",CHAP_User); 
				config.setProperty(UnitProperties.UnitTargetPrefix+".chap_password",CHAP_Password);

				config.setProperty(UnitProperties.UnitSliceGuid,r.getSliceID());
				
				logger.debug("lun_r:hostName="+device_name+";chap="+CHAP_User);
				
				//set reconstraints
				setRequestConstraints(request, ce);
			}
			
			if (resrequest.isNetwork) {
				// pass the NDL request to the authority
				// NOTE: for now, our broker is not aware of NDL, only some of the
				// sites, e.g., BEN know how to handle NDL.
				// get the NDL request for that domain
				OntModel model = de.domainRequest();
				OutputStream out = new ByteArrayOutputStream();            
				model.write(out);   
				String ndlRequest = out.toString();

				NdlModel.closeModel(model);
				
				if ((ndlRequest == null)
						|| (ndlRequest.length() == 0)) {
					throw new RuntimeException("getReservations: ndlRequest is null or zero-length");
				}
				config.setProperty(PropertyRequestNdl, ndlRequest);
				config.setProperty(UnitProperties.UnitVlanUrl, device_name);

				int staticLabel = (int) de.getStaticLabel();
				if(staticLabel>0){
					config.setProperty(PropertyConfigUnitTag,String.valueOf(staticLabel));
					logger.debug("Static Tag:"+staticLabel + "---------- for domain:" +domain+"\n");
				}
				
				//--deal with NSI actor, as it needs a range of available tags to try
				String availableLabel_str = de.getAvailableLabelSet();
				if(availableLabel_str!=null){
					config.setProperty(UnitProperties.UnitVlanTags,availableLabel_str);
					logger.debug("Available tag list="+availableLabel_str);
				}
				
				if(rSlice!=null){
					logger.debug("OF Slice:"+rSlice.getOfCtrlUrl()+":"+rSlice.getOfUserEmail()+":"+rSlice.getOfSlicePass());
					if(rSlice.getOfCtrlUrl()!=null)
						config.setProperty(PropertyUnitOFSliceCtrlUrl,rSlice.getOfCtrlUrl().toString());
					if(rSlice.getOfUserEmail()!=null)
						config.setProperty(PropertyUnitOFSliceEmail,rSlice.getOfUserEmail());
					if(rSlice.getOfSlicePass()!=null)
						config.setProperty(PropertyUnitOFSlicePass,rSlice.getOfSlicePass());
				}
				LinkedList<SwitchingAction> actions = de.getActionList();
				if (actions != null) {
					String from = null;
					String to = null;
					String bw = null;
					if (actions.size() > 1) {
						throw new RuntimeException("More than one switching action for domain: " + domain);
					}
					SwitchingAction a = actions.getFirst();
					if (a.getDefaultAction() == null) {
						logger.info("Default action is null!");
						continue;
					}
					if (!a.getDefaultAction().equals(LayerConstant.Action.VLANtag.toString())) {
						continue;
					}

					LinkedList<Interface> ifs = a.getClientInterface();
					int count = 0;
					String ports = null;
					if (ifs == null) {
						logger.info("Interface list is empty for this action!");
					} else {
						for (Interface iff : ifs) {
							if (iff == null) {
								logger.info("Interface is null for this interface list!");
							}
							if (iff.getName() != null) {
								if (ports == null) {
									ports = iff.getName();
								} else {
									ports = ports.concat("," + iff.getName());
								}
								config.setProperty("config.interface." + (count + 1), iff.getName());
								config.setProperty("config.interface.url." + (count + 1), iff.getURI());
								logger.debug("domain=" + domain + " setting property config.interface." 
								+ (count + 1) + "=" + iff.getName()+";url="+iff.getURI());
								count++;
							}
						}
					}
					if(ports!=null)
						config.setProperty("config.interface.ports", ports);
					else
						logger.error("ERROR: config.interface.ports is null!");
					
					bw = String.valueOf(a.getBw());
					//from = ifs.get(0).getURI();
					from = checkBorderInterface(ifs.get(0), a.getAtLayer());
					if (count > 1) {
						//to = ifs.get(1).getURI();
						to = checkBorderInterface(ifs.get(1),a.getAtLayer());
					}
					logger.debug("From:" + from + " To: " + to + " BW:" + bw + ":" + ports + "\n");
					if (bw != null) {
						if (from == null && to == null) {
							throw new RuntimeException("Bandwidth requested, but no interface specified for domain: " + domain);
						}

						// set the bandwidth
						request.setProperty(RequestProperties.RequestBandwidth, bw);
						logger.debug("Request Properties:" + RequestProperties.RequestBandwidth + ":" + bw);
						// start interface
						if (from != null) {
							request.setProperty(RequestProperties.RequestStartIface, from);
						}

						// end interface
						if (to != null) {
							request.setProperty(RequestProperties.RequestEndIface, to);
						}
					}
				}
			}
			
			r.setLocalProperties(OrcaConverter.fill(local));
			r.setConfigurationProperties(OrcaConverter.fill(config));
			r.setRequestProperties(OrcaConverter.fill(request));
			r.setBroker(orca_state_instance.getBroker());
			// register the reservation with Orca, so that it is assigned an id
			
			if(!extra_ar.contains(r))
				if (sm.addReservation(r) == null) {
					throw new RuntimeException("Could not add reservations " + sm.getLastError());
				}
		}
		
		return map;
	}
	
	//to deal with the stitching port that is adapted to the real border interface as a resource in the broker
	public String checkBorderInterface(Interface intf, String layer){
		String intf_str=intf.getURI();
		ResultSet results=NdlCommons.getLayerAdapatationOf(intf.getModel(),intf.getURI());
		String varName=(String) results.getResultVars().get(0); 
		Resource intf_rs_base = null;
		if (results.hasNext()){
			intf_rs_base=results.nextSolution().getResource(varName);
			String base_layer=NdlCommons.findLayer(intf.getModel(),intf_rs_base);
			if(layer.contains(base_layer))
				intf_str=intf_rs_base.getURI();
			logger.debug("intf_rs_base="+intf_rs_base.getURI()+";base_layer="+base_layer);
		}	
		return intf_str;
	}

	public ArrayList<TicketReservationMng> setDependency(Collection<NetworkElement> boundElements,HashMap<String, ReservationRequest> map){		
		
		ScriptConstructor scriptConstructor = new ScriptConstructor(elementCollection, ndlSlice);
		
		HashMap <DomainElement, LinkedList <ReservationRequest> > ip_r_collection = new HashMap <DomainElement, LinkedList <ReservationRequest> >();
		
		OntModel manifestModel = ndlSlice.getWorkflow().getManifestModel();
		for(NetworkElement device:boundElements){
			DomainElement de = (DomainElement) device;
			String de_domain = getDomainName(de);
			if(de_domain!=null)
				de_domain = de_domain.split("\\/")[0];
			logger.debug("current reservation:"+device.getName()+":allocatable:"+de.isAllocatable());
			if(de.isAllocatable()==false)
				continue;

			ReservationRequest r = map.get(device.getName());
			if (r == null) {
				logger.error("Missing reservation from domain " + device.getName());
                continue;
			}
			logger.debug("from "+ device.getName() + r.domain + " units=" + r.reservation.getUnits() + " type=" + r.reservation.getResourceType());
			ComputeElement ce = de.getCe();

			Properties config = new Properties();
			Properties local =new Properties();
			Properties pr_config = new Properties();
		
			//handle postBootScript
			if(r.isVM){          
				String bootScript = ce.getPostBootScript();   
				try {
					if (bootScript != null) {
						logger.debug("Generating postbootscript for " + device.getName());
						bootScript = scriptConstructor.constructScript(bootScript, device.getName());
						if (bootScript != null) {
							logger.debug(UnitProperties.UnitInstanceConfig + "\n" + bootScript);
							config.setProperty(UnitProperties.UnitInstanceConfig, bootScript);
							local.setProperty(UnitProperties.UnitInstanceConfig, bootScript);
							de.getCe().setPostBootScript(bootScript);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			HashMap<DomainElement, OntResource> preds = de.getPrecededBy();
			if (preds == null) {
				// update the local properties
				r.reservation.setLocalProperties(OrcaConverter.merge(local, r.reservation.getLocalProperties()));
				// update the configuration properties
				r.reservation.setConfigurationProperties(OrcaConverter.merge(config, r.reservation.getConfigurationProperties()));
				continue;
			}            
			boolean prNetwork=false;
			int num_interface=de.getNumInterface();
			r.networkDependencies = de.getNumInterface();
			for (Entry<DomainElement, OntResource> parent : de.getPrecededBySet()) {
				String intf_name = null;
				String parent_tag_name = UnitProperties.UnitEthPrefix;
				String parent_ip_addr = UnitProperties.UnitEthPrefix;
				String parent_mac_addr = UnitProperties.UnitEthPrefix;
				String parent_quantum_uuid = UnitProperties.UnitEthPrefix;
				String parent_interface_uuid = UnitProperties.UnitEthPrefix;
				DomainElement parent_de = parent.getKey();
				ReservationRequest pr = map.get(parent_de.getName());
				if (pr == null) {
					logger.warn("Could not find reservation from domain " + parent.getKey().getName());
					if(parent_de.getStaticLabel()>0)
						prNetwork=true;	
				}else{
					prNetwork = pr.isNetwork;
				}
				
				logger.debug("child is VM:"+r.isVM+";parent is network:"+prNetwork);

				/*if(pr!=null && pr.isLUN)
					r.numStorageDependencies++;
				if (prNetwork)
					r.networkDependencies++;
				*/
				
				String pdomain = getDomainName(parent_de);
				if(pdomain==null){
					logger.error("Parent domain name is null:"+ parent_de.getURI());
					continue;
				}
				pdomain = pdomain.split("\\/")[0];

				logger.info("ReservationConverter: parent domain name:"+pdomain);
				
				Properties filter = new Properties();
				Properties interfaces = new Properties();

				String mappedVlanProperty = UnitProperties.UnitVlanTag;
				String mappedPortListProperty = UnitProperties.UnitPortList;
				// 1. if both reservations are for network resources we must map the vlan tag from pr into a prefixed vlan tag
				// so that it does not collide with the vlan tags of other dependent reservations
				//2. for two reservations from the same domain, parent could be the one w/ static tag, in which case, the interface value is null
				if (r.isNetwork && parent.getValue()!=null) {
					mappedVlanProperty = pdomain + "." + mappedVlanProperty;
					mappedPortListProperty = pdomain + "." + mappedPortListProperty;
					logger.debug("Mapped Parent Vlan Property:" + mappedVlanProperty+";Parent static tag="+parent_de.getStaticLabel());
					if((parent_de.getStaticLabel()!=0) && (pr==null)){
						config.setProperty(mappedVlanProperty,String.valueOf( (int) parent.getKey().getStaticLabel()));
						local.setProperty(mappedVlanProperty,String.valueOf( (int) parent.getKey().getStaticLabel()));
					}else{
						filter.setProperty(UnitProperties.UnitVlanTag, mappedVlanProperty);
						filter.setProperty(UnitProperties.UnitPortList, mappedPortListProperty);
					}
					if (prNetwork) {
						Statement intf_st = parent.getValue().getProperty(NdlCommons.RDFS_Label);
						intf_name = intf_st == null ? " " : intf_st.getString();

						interfaces.setProperty(pdomain + ".edge.interface", intf_name);
						
						config.setProperty(pdomain + ".edge.interface.url",  parent.getValue().getURI());
						local.setProperty(pdomain + ".edge.interface.url",  parent.getValue().getURI());
						config.setProperty(pdomain + ".edge.interface", intf_name);
						local.setProperty(pdomain + ".edge.interface", intf_name);
						
						//-------deal with reversed dependency: ION-NLR, BEN-NLR
						//if(parent_de.isNeedFollowerInterface()==true){
							Resource pr_interface = parent_de.getFollowerInterface();
							if(pr_interface!=null){
								pr_config.setProperty(de_domain +  ".edge.interface.url", pr_interface.getURI());
								Statement pr_intf_st = pr_interface.getProperty(NdlCommons.RDFS_Label);
								String pr_intf_name = pr_intf_st == null ? " " : pr_intf_st.getString();
								pr_config.setProperty(de_domain +  ".edge.interface", pr_intf_name);
								if(pr!=null && pr.reservation!=null){
									pr.reservation.setConfigurationProperties(OrcaConverter.merge(pr_config, pr.reservation.getConfigurationProperties()));
									pr.reservation.setLocalProperties(OrcaConverter.merge(pr_config, pr.reservation.getLocalProperties()));
								}
							}
						//}
					}
				}
				if(r.isVM || r.isLUN){
					logger.debug("ReservationConverter:label_rs="+parent.getValue().getProperty(NdlCommons.layerLabelIdProperty));	
					num_interface++;

					if (parent.getValue() == null) {
						logger.error("Edge interface name is unknown!"+parent.getValue().getURI());
					}else{
						String element_guid = UUID.randomUUID().toString();
						parent.getValue().addProperty(NdlCommons.hasGUIDProperty, element_guid);
						//depending on storage
						if(pr!=null){
							if(pr.isLUN){//now do it latter when all reservations are collected
								r.numStorageDependencies++;
								num_interface--;
								ComputeElement parent_ce = parent_de.getCe();
								if(parent_ce!=null){
									setVMISCSIParam(config, parent_ce,r.numStorageDependencies);
									setVMISCSIParam(local, parent_ce,r.numStorageDependencies);
									parent_ce.setDoFormat(false);
									r.storageDependencies.put(pr.uuid, String.valueOf(r.numStorageDependencies));
								}
							}
						}
						
						if(parent.getValue().getProperty(NdlCommons.layerLabelIdProperty)!=null)
							intf_name = parent.getValue().getProperty(NdlCommons.layerLabelIdProperty).getString();
						int index=0;
						if (prNetwork) {
							r.networkDependencies++;

							String ip_addr = null, mac_addr=null, host_interface = null;
							
							String site_host_interface = getSiteHostInterface(parent);

							if (site_host_interface == null) {
								logger.error("Host Interface Definition not here: neither up neighbor or down neighbor!!");
								site_host_interface = "none";
							}else{
								if(parent.getValue().getProperty(NdlCommons.hostInterfaceName)==null)
									manifestModel.add(parent.getValue(),NdlCommons.hostInterfaceName,site_host_interface);
							}
								
							logger.debug("Site host interface:" + site_host_interface+";intf="+parent.getValue().getProperty(NdlCommons.hostInterfaceName));
							if(intf_name!=null)	                            
								index = intf_name.indexOf("@");
							logger.debug("$$$$$$$$$$$$$$$$$ intf_name:" + intf_name + ", index: " + index);
							if (index > 0) {
								ip_addr = intf_name.substring(0, index);
								host_interface = String.valueOf(Integer.valueOf(intf_name.substring(index + 1)).intValue() + 1);
								parent_ip_addr = parent_ip_addr.concat(host_interface).concat(".ip");
								parent_mac_addr = parent_mac_addr.concat(host_interface).concat(".mac");
								parent_interface_uuid = parent_interface_uuid.concat(host_interface).concat(".uuid");
								config.setProperty(UnitProperties.UnitEthPrefix + host_interface + ".hosteth", site_host_interface);
								local.setProperty(UnitProperties.UnitEthPrefix + host_interface + ".hosteth", site_host_interface);
								if (de.getPrecededBySet().size() >= 1) {
									parent_tag_name = parent_tag_name.concat(host_interface).concat(".vlan.tag");
									parent_quantum_uuid = parent_quantum_uuid.concat(host_interface).concat(UnitProperties.UnitEthNetworkUUIDSuffix);
								}
							} else {
								ip_addr = intf_name;
								host_interface = String.valueOf(r.networkDependencies);
								logger.debug("setDependency:host_interface="+host_interface);
								parent_mac_addr = parent_mac_addr.concat(host_interface).concat(".mac");
								parent_ip_addr = parent_ip_addr.concat(host_interface).concat(".ip");
								parent_interface_uuid = parent_interface_uuid.concat(host_interface).concat(".uuid");
								if (de.getPrecededBySet().size() >= 1) {
									config.setProperty(UnitProperties.UnitEthPrefix + host_interface + ".hosteth", site_host_interface);
									local.setProperty(UnitProperties.UnitEthPrefix + host_interface + ".hosteth", site_host_interface);
									parent_tag_name = parent_tag_name.concat(host_interface).concat(".vlan.tag");
									parent_quantum_uuid = parent_quantum_uuid.concat(host_interface).concat(UnitProperties.UnitEthNetworkUUIDSuffix);
									if(pr!=null){
										pr_config.setProperty(UnitProperties.UnitQuantumNetname,site_host_interface);	
										pr.reservation.setConfigurationProperties(OrcaConverter.merge(pr_config, pr.reservation.getConfigurationProperties()));
										pr.reservation.setLocalProperties(OrcaConverter.merge(pr_config, pr.reservation.getLocalProperties()));
									}
								}
							}
							if(parent.getKey().getStaticLabel()!=0){
								config.setProperty(parent_tag_name,String.valueOf( (int) parent.getKey().getStaticLabel()));
								local.setProperty(parent_tag_name,String.valueOf( (int) parent.getKey().getStaticLabel()));
							}
							else
								filter.setProperty(UnitProperties.UnitVlanTag, parent_tag_name);

							if(parent.getKey().getNetUUID()!=null) {
								config.setProperty(parent_quantum_uuid,parent.getKey().getNetUUID());
								local.setProperty(parent_quantum_uuid,parent.getKey().getNetUUID());
							}
							else
								filter.setProperty(UnitProperties.UnitQuantumNetUUID,parent_quantum_uuid);
							
							if(parent.getValue().getProperty(NdlCommons.ipMacAddressProperty)!=null)
								mac_addr=parent.getValue().getProperty(NdlCommons.ipMacAddressProperty).getString();
							else
								mac_addr = generateNewMAC();
							if(mac_addr!=null){
								parent.getValue().addProperty(NdlCommons.ipMacAddressProperty, mac_addr);
								logger.debug("ReservationConverter:mac property:"+parent.getValue().getProperty(NdlCommons.ipMacAddressProperty)); 
								local.setProperty(parent_mac_addr, mac_addr);
								config.setProperty(parent_mac_addr, mac_addr);
							}
							else
								logger.error("No available MAC address:"+parent_mac_addr);
							try {
								if(ip_addr!=null){
									InetAddress addr1 = InetAddress.getByName(ip_addr.split("/")[0]);  //this is only for throwing an exception for a mal-formated IP address.
									config.setProperty(parent_ip_addr, ip_addr);
									local.setProperty(parent_ip_addr, ip_addr);
								}
							} catch (UnknownHostException e) {
								logger.error("Not a Valid IP address:" + parent_ip_addr + ":" + ip_addr);
							}
							logger.debug("ReservationConverter: parent_ip_addr="+parent_ip_addr+"="+ip_addr);
						
							local.setProperty(UnitProperties.UnitEthPrefix+ host_interface + ".parent.url", parent_de.getName());
							
							config.setProperty(parent_interface_uuid,element_guid);
							local.setProperty(parent_interface_uuid,element_guid);
							
							if(parent_de.getIp_range()!=null){		//means a storage network vlan link
								parent_de.getIp_range().setHostInterface(host_interface);
								processIPCollection(ip_r_collection,parent_de,r);
							}
						}
					}
				}
								
				if( (pr!=null) && (prNetwork)){
					logger.debug("   depends on: " + pr.domain + " filter: " + filter.toString() + " interfaces: " + interfaces.toString());
					ReservationPredecessorMng pred = new ReservationPredecessorMng();
					pred.setReservationID(pr.reservation.getReservationID());
					pred.setFilter(OrcaConverter.fill(filter));
					r.reservation.getRedeemPredecessors().add(pred);
				}
			}
			if(r.isVM){
                local.setProperty(PropertyParentNumInterface,String.valueOf(num_interface));
                config.setProperty(PropertyParentNumInterface,String.valueOf(num_interface));
                
                local.setProperty(PropertyParentNumStorage,String.valueOf(r.numStorageDependencies));
                config.setProperty(PropertyParentNumStorage,String.valueOf(r.numStorageDependencies));
			}
			
			// update the local properties
			r.reservation.setLocalProperties(OrcaConverter.merge(local, r.reservation.getLocalProperties()));
			// update the configuration properties
			r.reservation.setConfigurationProperties(OrcaConverter.merge(config, r.reservation.getConfigurationProperties()));
		}
		
		if(ip_r_collection.size()>0)
			processIPCollectionProperty(ip_r_collection);
		
		ArrayList<TicketReservationMng> smReservations=new ArrayList<TicketReservationMng>(map.size());
		for (ReservationRequest r : map.values()) {
			smReservations.add(r.reservation);
		}
		
		return smReservations;
	}
	
	public Properties formInterfaceProperties(OntModel manifestModel,DomainElement dd,Entry<DomainElement, OntResource> parent,String site_host_interface, int num_parent, int num){		
		Properties property=new Properties();
		String ip_addr = null, mac_addr=null, host_interface = null, intf_name=null;
		String parent_ip_addr = UnitProperties.UnitEthPrefix;
		String parent_mac_addr = UnitProperties.UnitEthPrefix;
		String parent_quantum_uuid = UnitProperties.UnitEthPrefix;
		String parent_interface_uuid = UnitProperties.UnitEthPrefix;
		
		if(parent.getValue().getProperty(NdlCommons.layerLabelIdProperty)!=null)
			intf_name = parent.getValue().getProperty(NdlCommons.layerLabelIdProperty).getString();
		int index=0;
		
		//String site_host_interface = getSiteHostInterface(parent);

		if (site_host_interface == null) {
			logger.error("Host Interface Definition not here: neither up neighbor or down neighbor!!");
			site_host_interface = "none";
		}
			
		logger.debug("Site host interface:" + site_host_interface+";intf="+parent.getValue().getProperty(NdlCommons.hostInterfaceName));
		if(intf_name!=null)	                            
			index = intf_name.indexOf("@");
		logger.debug("$$$$$$$$$$$$$$$$$ intf_name:" + intf_name + ", index: " + index);
		if (index > 0) {
			ip_addr = intf_name.substring(0, index);
			host_interface = String.valueOf(Integer.valueOf(intf_name.substring(index + 1)).intValue() + 1);
		} else {
			ip_addr = intf_name;
			host_interface = String.valueOf(num_parent+num);
		}
		parent_ip_addr = parent_ip_addr.concat(host_interface).concat(".ip");
		parent_mac_addr = parent_mac_addr.concat(host_interface).concat(".mac");
		parent_interface_uuid = parent_interface_uuid.concat(host_interface).concat(".uuid");
			
		property.setProperty(UnitProperties.UnitEthPrefix + host_interface + ".hosteth", site_host_interface);

		parent_quantum_uuid = parent_quantum_uuid.concat(host_interface).concat(UnitProperties.UnitEthNetworkUUIDSuffix);

		if(parent.getKey().getNetUUID()!=null)
			property.setProperty(parent_quantum_uuid,parent.getKey().getNetUUID());
		
		if(parent.getValue().getProperty(NdlCommons.ipMacAddressProperty)!=null)
			mac_addr=parent.getValue().getProperty(NdlCommons.ipMacAddressProperty).getString();
		else
			mac_addr = generateNewMAC();
		if(mac_addr!=null){
			parent.getValue().addProperty(NdlCommons.ipMacAddressProperty, mac_addr);
			logger.debug("ReservationConverter:mac property:"+parent.getValue().getProperty(NdlCommons.ipMacAddressProperty)); 
			property.setProperty(parent_mac_addr, mac_addr);
		}
		else
			logger.error("No available MAC address:"+parent_mac_addr);
		try {
			if(ip_addr!=null){
				InetAddress addr1 = InetAddress.getByName(ip_addr.split("/")[0]);  //this is only for throwing an exception for a mal-formated IP address.
				logger.debug("ReservationConverter:ip property:"+parent_ip_addr+"="+ip_addr); 
				property.setProperty(parent_ip_addr, ip_addr);
			}
		} catch (UnknownHostException e) {
			logger.error("Not a Valid IP address:" + parent_ip_addr + ":" + ip_addr);
		}
		
		property.setProperty(UnitProperties.UnitEthPrefix+ host_interface + ".parent.url", parent.getKey().getName());
		/*
		String type="request:Manifest";
		OntResource manifest=NdlCommons.getOntOfType(manifestModel, type);
		String element_guid=dd.getGUID();
		Resource ob =null;
		for (StmtIterator j=manifest.listProperties(NdlCommons.collectionElementProperty);j.hasNext();){
			ob = j.next().getResource();
			if(ob.hasProperty(NdlCommons.hasGUIDProperty)){
				String guid = ob.getProperty(NdlCommons.hasGUIDProperty).getString();
				if(element_guid!=null && guid.equals(element_guid))
					break;
			}
		}
		if(ob==null)
			logger.warn("No modified in manifest:"+dd.getName()+",element_guid="+element_guid);
		else{
			if(parent.getValue().hasProperty(NdlCommons.hasGUIDProperty))
				property.setProperty(PropertyElementGUID, parent.getValue().getProperty(NdlCommons.hasGUIDProperty).getString());
			else{
				element_guid = UUID.randomUUID().toString();
				parent.getValue().addProperty(NdlCommons.hasGUIDProperty, element_guid);
			}
			property.setProperty(parent_interface_uuid,element_guid);
			addDependencyProperty(parent.getKey(),parent.getValue(), manifestModel.getOntResource(ob), manifestModel);
		}
		logger.debug("ReservationConverter: parent_ip_addr="+parent_ip_addr+"="+ip_addr);
		*/
		return property;
	}
	
	public String getSiteHostInterface(Entry<DomainElement, OntResource> parent){
		String site_host_interface=null;
		DomainElement p_de=parent.getKey();
		OntResource parent_intf_ont=parent.getValue();
		OntModel idm = ((InterCloudHandler)ndlSlice.getWorkflow().getEmbedderAlgorithm()).getIdm();
		
		if (parent_intf_ont.getProperty(NdlCommons.hostInterfaceName) != null) {
			site_host_interface = parent_intf_ont.getProperty(NdlCommons.hostInterfaceName).getString();
		}
		if (site_host_interface == null) {
			logger.debug("Host Interface Definition not here, "
					+ "because IP address is used as the parent value or its neighbors are network domains!!");
			logger.debug("parent="+p_de.getURI()
					+";parent intf="+parent_intf_ont.getURI()
					+";downNeighbour="+p_de.getDownNeighbourUri()
					+";upNeighbour="+p_de.getUpNeighbourUri());
			if (p_de.getDownNeighbour(idm) != null) {
				if (p_de.getDownNeighbour(idm).getProperty(NdlCommons.hostInterfaceName) != null) {
					site_host_interface = p_de.getDownNeighbour(idm).getProperty(NdlCommons.hostInterfaceName).getString();
				} else {
					if (p_de.getUpNeighbour(idm) != null) {
						if (p_de.getUpNeighbour(idm).getProperty(NdlCommons.hostInterfaceName) != null) {
							site_host_interface = p_de.getUpNeighbour(idm).getProperty(NdlCommons.hostInterfaceName).getString();
						}
					}
				}
			} else {
				if (p_de.getUpNeighbour(idm) != null) {
					if (p_de.getUpNeighbour(idm).getProperty(NdlCommons.hostInterfaceName) != null) {
						site_host_interface = p_de.getUpNeighbour(idm).getProperty(NdlCommons.hostInterfaceName).getString();
					}
				}
			}
		}
		return site_host_interface;	
	}
	
	public void processIPCollectionProperty(HashMap <DomainElement, LinkedList <ReservationRequest> > collection){
		//reset r.storageDependencies==0
		for(Entry<DomainElement, LinkedList <ReservationRequest>> entry:collection.entrySet()){
			LinkedList <ReservationRequest> r_collection = entry.getValue();
			for(ReservationRequest r:r_collection){
				r.numStorageDependencies=0;
			}
		}
		HashMap <ReservationRequest,String> lun_r_uuid_map = new HashMap <ReservationRequest,String>();
		HashMap <ReservationRequest,String> lun_r_ip_map = new HashMap <ReservationRequest,String>();
		ReservationRequest lun_r=null;
		String lun_uuid = null,r_uuid=null;
		for(Entry<DomainElement, LinkedList <ReservationRequest>> entry:collection.entrySet()){
			DomainElement p_de = entry.getKey();
			IPAddressRange ip_range = p_de.getIp_range();

			HashMap <ReservationRequest,String> r_uuid_map = new HashMap <ReservationRequest,String>();
			String r_uuid_list_str="",r_ip_list_str="";

			LinkedList <ReservationRequest> r_collection = entry.getValue();
			for(ReservationRequest r:r_collection){
				if(r.isLUN){
					lun_r=r;
					if(lun_r.uuid==null){
						lun_uuid = UUID.randomUUID().toString();
						lun_r.uuid=lun_uuid;
					}else
						lun_uuid=lun_r.uuid;
				}
				if(r.isVM){
					if(r.uuid==null){
						r_uuid = UUID.randomUUID().toString();
						r.uuid=r_uuid;
					}else
						r_uuid=r.uuid;
					r_uuid_map.put(r,r_uuid);
					r.numStorageDependencies++;
				}
			}
			if(lun_r==null){
				logger.error("No ISCSI reservation exists for this link="+p_de.getName());
				continue;
			}
			int i=0;
			for(Entry<ReservationRequest,String> r_entry:r_uuid_map.entrySet()){
				i++;
				Properties config = new Properties();
				Properties local =new Properties();
				r_uuid = r_entry.getValue();
				ReservationRequest r= r_entry.getKey();
				
				//lun num comes from the lun reservation, via property substitution in the predecessor
				String host_interface = null;
				if(r.storageDependencies.get(lun_uuid)!=null)
					host_interface = r.storageDependencies.get(lun_uuid);
				else
					host_interface = String.valueOf(r.numStorageDependencies);
				
				String parent_tag_name = UnitProperties.UnitStoragePrefix+host_interface+ UnitProperties.UnitTargetLunSuffix;
				Properties filter = new Properties();
				filter.setProperty(UnitProperties.UnitLUNTag, parent_tag_name);
				logger.debug("   depends on: " + lun_r.uuid + " filter: " + filter.toString() );
				ReservationPredecessorMng pred = new ReservationPredecessorMng();
				pred.setReservationID(lun_r.reservation.getReservationID());
				pred.setFilter(OrcaConverter.fill(filter));
				r.reservation.getRedeemPredecessors().add(pred);
				
				//set up other properties
				setVMISCSIProperty(config,r_uuid,lun_uuid,ip_range,host_interface);
				setVMISCSIProperty(local,r_uuid,lun_uuid,ip_range,host_interface);
				
				r.reservation.setLocalProperties(OrcaConverter.merge(local, r.reservation.getLocalProperties()));
				r.reservation.setConfigurationProperties(OrcaConverter.merge(config, r.reservation.getConfigurationProperties()));

				OntResource intf_ont = p_de.getFollowedBySetByElement(r.domain_url);
				Resource ip_rs=null;
				String ip_str=null;
				if(intf_ont!=null && (intf_ont.getProperty(NdlCommons.ip4LocalIPAddressProperty)!=null)){
					ip_rs = intf_ont.getProperty(NdlCommons.ip4LocalIPAddressProperty).getResource();
					ip_str = ip_rs.getProperty(NdlCommons.layerLabelIdProperty).getString();
				}
				logger.debug("ip_str="+ip_str+";ip_rs="+ip_rs+";intf_ont="+intf_ont
						+";rDomain="+r.domain_url+";pDomain="+p_de.getName());
				if(i==1){
					r_uuid_list_str = r_uuid;
					r_ip_list_str = ip_str;
				}
				else{
					r_uuid_list_str = r_uuid_list_str+ "," + r_uuid;
					if(ip_str!=null){
						if(r_ip_list_str == null)
							r_ip_list_str = ip_str;
						else
							r_ip_list_str =r_ip_list_str+","+ip_str;
					}
				}
			}
			if(lun_r_uuid_map.containsKey(lun_r)){
				r_uuid_list_str = lun_r_uuid_map.get(lun_r)+","+r_uuid_list_str;
			}
			lun_r_uuid_map.put(lun_r,r_uuid_list_str);
			if(lun_r_ip_map.containsKey(lun_r)){
				r_ip_list_str = lun_r_ip_map.get(lun_r)+","+r_ip_list_str;
			}
			lun_r_ip_map.put(lun_r,r_ip_list_str);
			logger.debug("r_ip_list_str=" + r_ip_list_str);
			
			Properties config = new Properties();
			Properties local =new Properties();
			
			config.setProperty(UnitProperties.UnitLUNGuid ,lun_uuid);
			local.setProperty(UnitProperties.UnitLUNGuid ,lun_uuid);
				
			lun_r.reservation.setLocalProperties(OrcaConverter.merge(local, lun_r.reservation.getLocalProperties()));
			lun_r.reservation.setConfigurationProperties(OrcaConverter.merge(config, lun_r.reservation.getConfigurationProperties()));
		}
		//assign the complete vm uuid list to the lun
		String r_uuid_list_str = null,r_ip_list_str = null;
		for(Entry<ReservationRequest,String> r_entry:lun_r_uuid_map.entrySet()){
			lun_r = r_entry.getKey();
			r_uuid_list_str = r_entry.getValue();
			r_ip_list_str = lun_r_ip_map.get(lun_r);
			Properties config = new Properties();
			Properties local =new Properties();
			config.setProperty(UnitProperties.UnitVMGuid ,r_uuid_list_str);
			local.setProperty(UnitProperties.UnitVMGuid ,r_uuid_list_str);
			if(r_ip_list_str!=null){
				config.setProperty(UnitProperties.UnitVMIP ,r_ip_list_str);
				local.setProperty(UnitProperties.UnitVMIP ,r_ip_list_str);
			}
			logger.debug("lun_r="+lun_r.uuid +": r_uuid_list_str=" + r_uuid_list_str+
					": r_ip_list_str=" + r_ip_list_str);
			
			lun_r.reservation.setLocalProperties(OrcaConverter.merge(local, lun_r.reservation.getLocalProperties()));
			lun_r.reservation.setConfigurationProperties(OrcaConverter.merge(config, lun_r.reservation.getConfigurationProperties()));
		}		
	}
	
	public void setVMISCSIProperty(Properties p, String r_uuid, String lun_uuid,IPAddressRange ip_range,String host_interface){
		String base_ip_addr = null;
		if(ip_range!=null){
			if(ip_range.getBase_IP()!=null){
				base_ip_addr = ip_range.getBase_ip_addr().address;
			} else
				logger.warn("Reservationconverter: VMISCSI: base_ip_addr is null");
		} else
			logger.warn("Reservationconverter: VMISCSI: ip_range is null");
		String storagePrefix = UnitProperties.UnitStoragePrefix+host_interface;
		//String storageTargetPrefix = storagePrefix + ".target"; 
		//String storageFSPrefix = storagePrefix + ".fs"; 
		
		//Other properties 
		p.setProperty(UnitProperties.UnitISCSIInitiatorIQN, ISCSI_Initiator_Iqn_prefix+r_uuid);		
		p.setProperty(UnitProperties.UnitVMGuid,r_uuid);
		

		if (base_ip_addr!=null)
			p.setProperty(storagePrefix + UnitProperties.UnitTargetIPSuffix,base_ip_addr);

		p.setProperty(storagePrefix + UnitProperties.UnitTargetLunGuid,lun_uuid);
	}
	
	public void setVMISCSIParam(Properties p,ComputeElement s_ce,int s_num){
		String storagePrefix = UnitProperties.UnitStoragePrefix+s_num;
		//String storageTargetPrefix = storagePrefix + ".target"; 
		//String storageFSPrefix = storagePrefix + ".fs"; 
		if(s_ce.getFSType()!=null)
			p.setProperty(storagePrefix + UnitProperties.UnitFSTypeSuffix,s_ce.getFSType());
		else
			p.setProperty(storagePrefix + UnitProperties.UnitFSTypeSuffix,"ext3");
		
		if(s_ce.getFSParam()!=null)
			p.setProperty(storagePrefix + UnitProperties.UnitFSOptionsSuffix,s_ce.getFSParam());
		else
			p.setProperty(storagePrefix + UnitProperties.UnitFSOptionsSuffix,"-F -b 1024");
		
		if(s_ce.getMntPoint()!=null)
			p.setProperty(storagePrefix + UnitProperties.UnitFSMountPointSuffix,s_ce.getMntPoint());
		else
			p.setProperty(storagePrefix + UnitProperties.UnitFSMountPointSuffix,"/mnt/target/"+String.valueOf(s_num));
		if(s_ce.isDoFormat())
			p.setProperty(storagePrefix + UnitProperties.UnitFSShouldFormatSuffix,SUDO_YES);
		else
			p.setProperty(storagePrefix + UnitProperties.UnitFSShouldFormatSuffix,SUDO_NO);
		
		p.setProperty(storagePrefix + UnitProperties.UnitTargetChapUserSuffix,s_ce.getCHAP_User());
		p.setProperty(storagePrefix + UnitProperties.UnitTargetChapSecretSuffix,s_ce.getCHAP_Password());
		
		p.setProperty(storagePrefix+UnitProperties.UnitStoreTypeSuffix,"iscsi");
		p.setProperty(storagePrefix + UnitProperties.UnitTargetPortSuffix,"3260");
		
		p.setProperty(storagePrefix + UnitProperties.UnitTargetShouldAttachSuffix,SUDO_YES);
		logger.debug("setVMISCSIParam():"+storagePrefix + UnitProperties.UnitFSMountPointSuffix + "="+s_ce.getMntPoint()+":"+storagePrefix + UnitProperties.UnitTargetChapUserSuffix + "=" + s_ce.getCHAP_User());
	}

	public void processIPCollection(HashMap <DomainElement, LinkedList <ReservationRequest> > collection, DomainElement p_de, ReservationRequest r){
		LinkedList <ReservationRequest> r_collection = collection.get(p_de);
		if(r_collection!=null){
			r_collection.add(r);
		}else{
			r_collection = new LinkedList <ReservationRequest>();
			r_collection.add(r);
			collection.put(p_de,r_collection);
		}
			
	}
	
	public void updateGeniStates(OntModel manifestModel,GeniStates geniStates){
		String query = OntProcessor.createQueryStringReservationTerm();
		ResultSet rs = OntProcessor.rdfQuery(manifestModel, query);
		
		String state_url = NdlCommons.ORCA_NS+"geni.owl#"+geniStates.name;
		Resource genistate_rs = manifestModel.createIndividual(state_url,NdlCommons.sliceGeniStateClass);
		
		if (rs.hasNext()) {
			ResultBinding result = (ResultBinding)rs.next();
			Resource res = (Resource)result.get("reservation");
			OntResource res_ont = manifestModel.getOntResource(res);
			updateState(manifestModel,res_ont,NdlCommons.hasSliceGeniState,genistate_rs);
		}
	}

	public void updateTerm(OntModel manifestModel){
		// reservation query from which everything flows
		String query = OntProcessor.createQueryStringReservationTerm();
		ResultSet rs = OntProcessor.rdfQuery(manifestModel, query);
		
		if (rs.hasNext()) {
			ResultBinding result = (ResultBinding)rs.next();
			Resource res = (Resource)result.get("reservation");
			// term duration
			Resource term_rs = (Resource)result.get("term");
			// start date
			Date sd = null;
			if (leaseStart != null){
				sd = leaseStart;
			}

			if((term_rs!=null) && (sd!=null) ){
				String sd_str = term_rs.getNameSpace()+UUID.randomUUID().toString();
				Resource st_rs = manifestModel.createIndividual(sd_str, NdlCommons.instantOntClass);
				Calendar cal = Calendar.getInstance();
				cal.setTime(sd);
				st_rs.addProperty(NdlCommons.inXSDDateTime,DatatypeConverter.printDateTime(cal),XSDDatatype.XSDdateTime);	
				OntResource term_ont=manifestModel.getOntResource(term_rs);
				updateState(manifestModel,term_ont,NdlCommons.hasBeginningObjectProperty, st_rs);
			}
		}
	}
	
	public String getManifest(OntModel manifestModel,LinkedList <OntResource> domainInConnectionList,Collection<NetworkElement> deviceList,
			List<ReservationMng> allRes) {
		OntModel mm= getManifestModel(manifestModel, domainInConnectionList, deviceList, allRes);
		OutputStream out = new ByteArrayOutputStream();
		//TDB.sync(manifestModel);
		manifestModel.write(out);
		return out.toString();
	}
	
	public OntModel getManifestModel(OntModel manifestModel,LinkedList <OntResource> domainInConnectionList,Collection<NetworkElement> deviceList,
			List<ReservationMng> allRes) {
		
		HashMap <String, OntResource> vmReservationList = new HashMap <String, OntResource>();
		HashMap <String, OntResource> vlanReservationList = new HashMap <String, OntResource>();
		logger.debug("getManifestModel(): start associating manifest with ORCA reservation states...");		
		OntResource domain_ont;
		String domain_ont_url=null,domain = null,rDomain,rType,type;
		int units=0;
		boolean ready=false;

		if(allRes.size()<=0){
			logger.warn("getManifestModel(): No reservations found");
			return manifestModel;
		}
			
		int [] orcaReservationState = new int [allRes.size()];
		int i=0;
		for(ReservationMng r:allRes){
			orcaReservationState[i] = r.getState();
			i++;
		}
		
		logger.debug("getManifestMode(): there are " + domainInConnectionList.size() + " domains on the list");
		for (i = 0; i < domainInConnectionList.size(); i++) {
			domain_ont = domainInConnectionList.get(i);
			
			if(domain_ont.hasProperty(NdlCommons.hasURLProperty)){  
				domain_ont_url=domain_ont.getProperty(NdlCommons.hasURLProperty).getString();
				domain = getDomainName(domain_ont_url);   //e.g., ben/vlan
			}
			
			logger.info("getManifest: iteration="+i+";domain_ont="+domain_ont.getURI()
					+";domain_ont_url="+domain_ont_url
					+";domain="+domain);
			
			if(domain==null){
				logger.warn("no domain:"+domain_ont.getURI());
				continue;
			}
			
			boolean inConnection = false; //if true in interdomain connection request, or the same reservation in cloudhandler
			if(domain_ont.getProperty(NdlCommons.inConnection)!=null){  //defined in InterDomainHandler.createManifest()
				if(domain_ont.getProperty(NdlCommons.inConnection).getBoolean()==true){
					inConnection=true;
				}
			}
			
			for (int j = 0; j < allRes.size(); j++) {
				ReservationMng r = allRes.get(j);
				type = r.getResourceType();
				units = r.getUnits();
				if (!(orcaReservationState[j] != OrcaConstants.ReservationStateActive)) {
					ready = false;
				}
				rDomain = type.split("\\.")[0];
				rType = type.split("\\.")[1];

				//logger.info("getManifest: domain=" + domain_ont.getURI() + " ;aDomian = "+domain+" :rDomain=" + rDomain +" rType="+rType);
				if ((domain.equalsIgnoreCase(rDomain)) && (domain_ont_url.endsWith(rType))) {
					Properties local = OrcaConverter.fill(r.getLocalProperties());
					String unit_url = local.getProperty(UNIT_URL_RES);
					logger.debug("getManifest:unit.url="+unit_url+"domain_ont="+domain_ont.getURI());
					if( (domain_ont.getURI().equals(unit_url)) || (inConnection ==true) ){
						try {
							String notice = r.getNotices();
							int state_id = orcaReservationState[j];
							String state= OrcaConstants.getReservationStateName(state_id);
							logger.info("getManifest: notice="+notice+":state="+state+"!");
							if( (state_id == OrcaConstants.ReservationStateCloseWait) || (state_id == OrcaConstants.ReservationPendingStateClosing) ){
								logger.warn("Filtering reservation in CloseWait state!");	
								continue;
							}
							Resource reservationState = manifestModel.createIndividual(NdlCommons.ORCA_NS+"request.owl#"+state, NdlCommons.requestReservationStateClass);

							logger.debug("getManifest:unit.url="+unit_url);
							List<UnitMng> un = sm.getUnits(new ReservationID(r.getReservationID()));
							if (un != null) {
								logger.info("getManifest:un.size()="+un.size());	
							
								if (un.size() > 0) {
									for (UnitMng u : un) {
										Properties p = OrcaConverter.fill(u.getProperties());
										//Network reservation
										if ( (rType.equalsIgnoreCase("vlan")) && (domain_ont.getURI().equals(unit_url)) ) {	
											String vlan_url = unit_url;
											logger.info("getManifest: unit.vlan.url="+vlan_url);
											if (vlan_url == null) {
												logger.error("unit.vlan.url is null");
												vlan_url = domain_ont.getURI() + "/" + p.getProperty(UnitProperties.UnitReservationID);
											}
										
											if (p.getProperty(UnitProperties.UnitVlanTag) != null) {
												domain_ont.addProperty(NdlCommons.RDFS_Label, p.getProperty(UnitProperties.UnitVlanTag));
											} else {
												logger.error(UnitProperties.UnitVlanTag + " is null");
											}
											if (p.getProperty(UnitProperties.UnitVlanQoSRate) != null) {
												Statement st_bw = domain_ont.getProperty(NdlCommons.layerBandwidthProperty);
												long bw = Long.valueOf(p.getProperty(UnitProperties.UnitVlanQoSRate));
												if(st_bw!=null)
													st_bw.changeLiteralObject(bw);
												else
													domain_ont.addLiteral(NdlCommons.layerBandwidthProperty,bw);
											} else {
												logger.error(UnitProperties.UnitVlanQoSRate + " is null");
											}
											if(p.getProperty(UnitProperties.UnitQuantumNetUUID)!=null){
												domain_ont.addProperty(NdlCommons.quantumNetUUIDProperty,p.getProperty(UnitProperties.UnitQuantumNetUUID));
											}
											updateState(manifestModel,domain_ont,NdlCommons.requestMessage, notice);
											updateState(manifestModel,domain_ont,NdlCommons.requestHasReservationState,reservationState);
											vlanReservationList.put(vlan_url, domain_ont);
											if(!inConnection)
												domain_ont.removeAll(NdlCommons.topologyHasInterfaceProperty);
										}
										//VM reservation, need to generate new resources when multiple units.
										if (rType.equalsIgnoreCase("vm") || rType.endsWith("baremetalce")  || rType.equalsIgnoreCase("lun")) {
											String vm_url = unit_url;
											if (vm_url == null) {
												logger.error("unit.hostname.url is null");
												vm_url = domain_ont.getURI() + "/" + p.getProperty(UnitProperties.UnitReservationID);
											}
											logger.debug("getManifest:domain_ont_url="+domain_ont_url+";vm_url="+vm_url); 
									
											Individual vm_ont = manifestModel.getIndividual(vm_url); 
											if(vm_ont==null){
												if(rType.equalsIgnoreCase("lun"))
													vm_ont = manifestModel.createIndividual(vm_url, NdlCommons.networkStorageClass);
												else
													vm_ont = manifestModel.createIndividual(vm_url, NdlCommons.computeElementClass);
											}
											Resource inDomain_rs = null;
											if(domain_ont.hasProperty(NdlCommons.inDomainProperty)){
												inDomain_rs = domain_ont.getProperty(NdlCommons.inDomainProperty).getResource();
											}
											vm_ont.removeAll(NdlCommons.inDomainProperty);
											vm_ont.addProperty(NdlCommons.inDomainProperty,inDomain_rs);
											vm_ont.addProperty(NdlCommons.hasURLProperty, domain_ont_url);
											
											vmReservationList.put(vm_url, vm_ont);
											
											if (p.getProperty(PropertyUnitEC2Instance) != null) {
												vm_ont.addProperty(NdlCommons.hasInstanceIDProperty, p.getProperty(PropertyUnitEC2Instance));
											} else {
												logger.warn("unit.ec2.instance is null");
											}
											if (p.getProperty(PropertyUnitEC2Host) != null) {
												vm_ont.addProperty(NdlCommons.workerNodeIDProperty, p.getProperty(PropertyUnitEC2Host));
											} else {
												logger.warn("unit.ec2.host is null");
											}
											
											logger.info("VM config properties: 1="+p.getProperty("unit.eth1.hosteth") + " 2="+p.getProperty("unit.eth2.hosteth")+ " 2="+p.getProperty("unit.eth3.hosteth"));

											Individual service_ont = manifestModel.createIndividual(vm_url + "/Service", NdlCommons.networkServiceClass);
											service_ont.addProperty(NdlCommons.domainHasAccessMethod,NdlCommons.domainSSHServiceClass);
											vm_ont.addProperty(NdlCommons.domainHasServiceProperty,service_ont);                                        
											//Individual vm_IP = manifestModel.createIndividual(vm_url + "/IP", NdlCommons.IPAddressOntClass);
											//logger.info("Service:"+service_ont.getURI()+";"+vm_IP.getURI());
											String user_login = ndlSlice.getLoginsAsString();
											if(user_login!=null)
												service_ont.addProperty(NdlCommons.topologyHasLogin, user_login);
											String unitManagementIP = p.getProperty(UnitProperties.UnitManagementIP);
											String unitManagementPort = p.getProperty(UnitProperties.UnitManagementPort);
											if (unitManagementIP != null) {
												//vm_IP.addProperty(NdlCommons.layerLabelIdProperty, unitManagementIP);
												service_ont.addProperty(NdlCommons.topologyManagementIP, unitManagementIP);
												if(unitManagementPort!=null){
													service_ont.addProperty(NdlCommons.topologyManagementPort, unitManagementPort);
												} else {
													logger.error(UnitProperties.UnitManagementIP + " is null");
												}
												//if( (unitManagementPort==null) || (unitManagementPort.equals("22")) ){  //No DNAT
												//	vm_ont.addProperty(NdlCommons.ip4LocalIPAddressProperty, vm_IP);
												//}
											} else {
												logger.error(UnitProperties.UnitManagementIP + " is null");
											}
											if (p.getProperty(UnitProperties.UnitInstanceConfig) != null) {
												vm_ont.addProperty(NdlCommons.requestPostBootScriptProperty, p.getProperty(UnitProperties.UnitInstanceConfig));
											} else {
												logger.error(UnitProperties.UnitInstanceConfig + " is null");
											}
												
											updateState(manifestModel,vm_ont,NdlCommons.requestMessage, notice);
											updateState(manifestModel,vm_ont,NdlCommons.requestHasReservationState,reservationState);
										}
									
										if (logger.isDebugEnabled()) {
											for (Enumeration<?> e = p.propertyNames(); e.hasMoreElements();) {
												String key = (String) e.nextElement();
												Formatter fmt = new Formatter(); 
												// like Twitter 
												fmt.format("%.140s", p.getProperty(key)); 
												//logger.debug("{Property key: " + key + " | Property value: " + fmt + "}"); 
												fmt.close(); 											
											}                           
										}
									}      
								} else {	//unsuccessful reservations
									if (rType.equalsIgnoreCase("vlan")) {
										if( (domain_ont.getURI().equals(unit_url)) || (domain_ont_url.equals(unit_url)) ){
											updateState(manifestModel,domain_ont,NdlCommons.requestMessage, notice);
											updateState(manifestModel,domain_ont,NdlCommons.requestHasReservationState,reservationState);
											vlanReservationList.put(unit_url, domain_ont);
											if(!inConnection)
												domain_ont.removeAll(NdlCommons.topologyHasInterfaceProperty);
										}
									}
									if (rType.equalsIgnoreCase("vm") || rType.endsWith("baremetalce") || rType.equalsIgnoreCase("lun")) {
										if(domain_ont.getURI().equals(unit_url))  //
											inConnection = true;
										if(inConnection==true){
											Individual vm_ont = manifestModel.getIndividual(unit_url); 
											if(vm_ont==null)
												vm_ont = manifestModel.createIndividual(unit_url, NdlCommons.computeElementClass);
											Resource inDomain_rs = null;
											if(domain_ont.hasProperty(NdlCommons.inDomainProperty)){
												inDomain_rs = domain_ont.getProperty(NdlCommons.inDomainProperty).getResource();
											}
											vm_ont.removeAll(NdlCommons.inDomainProperty);
											vm_ont.addProperty(NdlCommons.inDomainProperty,inDomain_rs);
											        							
											vmReservationList.put(unit_url, vm_ont);
											Resource rc=null;
											List <Statement> st_list=domain_ont.listProperties(NdlCommons.inRequestNetworkConnection).toList();
											for (Statement st:st_list){
												rc = st.getResource();
												rc.addProperty(NdlCommons.collectionItemProperty, vm_ont);
											}
											updateState(manifestModel,vm_ont,NdlCommons.requestMessage, notice);
											updateState(manifestModel,vm_ont,NdlCommons.requestHasReservationState,reservationState);
										}
									}
								}
							}
						}catch (Exception ex) {
							logger.error("Exception caught while getting manifest (may be normal): " + ex + ". Continuing.");
							ex.printStackTrace(System.out);
						}
					}  
				}
			}
		}

		// add postscript for vm reservation
		for(NetworkElement ne:deviceList){
			DomainElement d = (DomainElement) ne;
			logger.debug("d=:"+d.getName()+";resourceType="+d.getResourceType().toString());
			rType = d.getResourceType().getResourceType();
			if(rType==null)
				continue;
			if(rType.endsWith("vm") || rType.endsWith("baremetalce") || rType.equalsIgnoreCase("lun")){
				logger.info("vm: d="+d.getName()+":match vmreservation?size="+vmReservationList.size());
				for(Entry <String, OntResource> entry:vmReservationList.entrySet()){
					OntResource vm_reservation_ont = entry.getValue();
					if(d.getName().equalsIgnoreCase(entry.getKey())){
						logger.info("vmReservation="+entry.getKey());
						ComputeElement ce = d.getCe();
						if(ce.getType()!=null)
							vm_reservation_ont.addProperty(NdlCommons.domainHasResourceTypeProperty, ce.getResourceType().getTypeResource());
						if(ce.getNodeGroupName()!=null)
							vm_reservation_ont.addProperty(NdlCommons.hasRequestGroupURL, ce.getNodeGroupName());
						if(ce.getImage()!=null && !vm_reservation_ont.hasProperty(NdlCommons.diskImageProperty)){
		            		Individual image = manifestModel.createIndividual(ce.getImage(), NdlCommons.diskImageClass);
		            		vm_reservation_ont.addProperty(NdlCommons.diskImageProperty, image);
		            		if(ce.getVMImageURL()!=null)
		            			image.addProperty(NdlCommons.hasURLProperty, ce.getVMImageURL());
		            		if(ce.getVMImageHash()!=null)
		            			image.addProperty(NdlCommons.hasGUIDProperty,ce.getVMImageHash());
		            	}
						if(ce.getSpecificCETypeurl()!=null){
							Resource ceType_rs=vm_reservation_ont.getModel().createResource(ce.getSpecificCETypeurl());
							vm_reservation_ont.addProperty(NdlCommons.specificCEProperty,ceType_rs);
						}
						if(ce.getPostBootScript()!=null){
							if(vm_reservation_ont.hasProperty(NdlCommons.requestPostBootScriptProperty)){
								vm_reservation_ont.setPropertyValue(NdlCommons.requestPostBootScriptProperty, manifestModel.createLiteral(ce.getPostBootScript()));
							}else{
								vm_reservation_ont.addProperty(NdlCommons.requestPostBootScriptProperty, ce.getPostBootScript());
							}
						}
						else{
							logger.warn(entry.getKey()+":d.getPostBootScript() returned null");
						}
						//add VM to be part of the networkconnection
						ComputeElement d_ce = d.getCe();
						if(d_ce!=null && d_ce.getInterfaces()!=null){
							Resource rc=null;
							Set <NetworkConnection>rc_list = d_ce.getInterfaces().keySet();
							for (NetworkConnection rcc:rc_list){
								rc=manifestModel.createIndividual(rcc.getURI(), NdlCommons.topologyNetworkConnectionClass);
								rc.addProperty(NdlCommons.collectionItemProperty, vm_reservation_ont);
								logger.debug("GetManifester:rc="+rc.getURI()+";vm="+vm_reservation_ont.getURI()
										+";property="+rc.hasProperty(NdlCommons.collectionItemProperty, vm_reservation_ont));
							}
						}else
							logger.debug("GetManifester:rc is null, d="+d.getURI()+";vm="+vm_reservation_ont.getURI());
							
						//associate VM reservations to its vlan parent
						if(d.getPrecededBySet()!=null)
							logger.info("vm: d dependency="+d.getPrecededBySet().size());
						int k=0;
						if(d.getPrecededBySet()!=null){
							for (Entry<DomainElement, OntResource> parent : d.getPrecededBySet()) {
								k++;
								DomainElement parentDevice = parent.getKey();
								if(parentDevice.isAllocatable()==false){
									//Resource parentDevice_rs=manifestModel.getResource(parentDevice.getResource().getURI());
									//logger.error("Remove interface:"+parentDevice.getName()
									//		+";uri="+parentDevice.getResource().getURI()
									//		+";rs="+parentDevice_rs);
									//if(parentDevice_rs!=null)
									//	parentDevice_rs.removeAll(NdlCommons.topologyHasInterfaceProperty);
									//parentDevice.getResource().addProperty(NdlCommons.topologyHasInterfaceProperty, parent.getValue());
									//entry.getValue().addProperty(NdlCommons.topologyHasInterfaceProperty, parent.getValue());
									//adding the dependency information
				                	addDependencyProperty(parentDevice,parent.getValue(), entry.getValue(), manifestModel);
									continue;
								}
								if(vlanReservationList.entrySet()==null){
									logger.warn("No VLAN reservation....");
									continue;
								}
								for(Entry <String, OntResource> vlanentry:vlanReservationList.entrySet()){
									String vlanDeviceName=vlanentry.getKey();
									logger.debug("vlanDeviceName="+vlanDeviceName+";parentDevice="+parentDevice.getName());
									if(vlanDeviceName.equals(parentDevice.getName())){
										OntResource vlan_ont = vlanentry.getValue();
										if(vlan_ont.hasProperty(NdlCommons.RDFS_Label)){
											parentDevice.setStaticLabel(vlan_ont.getProperty(NdlCommons.RDFS_Label).getFloat());
										}
										vlanentry.getValue().addProperty(NdlCommons.topologyHasInterfaceProperty, parent.getValue());
										entry.getValue().addProperty(NdlCommons.topologyHasInterfaceProperty,parent.getValue());
										//adding the dependency information
					                	addDependencyProperty(parentDevice,parent.getValue(), entry.getValue(), manifestModel);
									}
								}
							}
						}
					}
				}
			}
		}
		//TDB.sync(manifestModel);
		return manifestModel;
	}
	
	//adding the dependency information
	public void addDependencyProperty(DomainElement parent_device,OntResource intf_ont,OntResource child_ont, OntModel manifestModel){
		OntResource parent_device_ont=null;
		String local_name = null;
		
		String parent_url = parent_device.getName();
		OntResource parent_ont = manifestModel.createOntResource(parent_url);
		
		if(existingParent(child_ont,parent_ont)){
			logger.warn("addDependencyProperty:Existing parent:child="+child_ont.getURI()+";parent="+parent_url);
			return;
		}
		if(child_ont.getURI().indexOf("#")>0)
			local_name=child_ont.getURI().split("#")[1];
		else 
			local_name = NdlCommons.getTrueName(child_ont);

		parent_device_ont = manifestModel.createIndividual
					(parent_device.getURI()+"/"+local_name, NdlCommons.networkDomainOntClass);
		
		parent_device_ont.addProperty(NdlCommons.collectionElementProperty, parent_ont);
		if(!parent_ont.hasProperty(NdlCommons.topologyHasInterfaceProperty,intf_ont))
			parent_ont.addProperty(NdlCommons.topologyHasInterfaceProperty,intf_ont);
		
		Individual intf_ind=manifestModel.createIndividual(intf_ont.getURI(), NdlCommons.interfaceOntClass);
		parent_ont.addProperty(NdlCommons.topologyHasInterfaceProperty,intf_ind);
		child_ont.addProperty(NdlCommons.topologyHasInterfaceProperty,intf_ind);
		System.out.println("intf_ont="+intf_ont.getURI());
		if(intf_ont.hasProperty(NdlCommons.ip4LocalIPAddressProperty)){
			Resource ip_rs = intf_ont.getProperty(NdlCommons.ip4LocalIPAddressProperty).getResource();
			Individual ip_ind = manifestModel.createIndividual(ip_rs.getURI(), NdlCommons.IPAddressOntClass);
			intf_ind.addProperty(NdlCommons.ip4LocalIPAddressProperty, ip_ind);
			if(ip_rs.hasProperty(NdlCommons.layerLabelIdProperty))
				ip_ind.addProperty(NdlCommons.layerLabelIdProperty, ip_rs.getProperty(NdlCommons.layerLabelIdProperty).getString());
		}
		
		if(intf_ont.hasProperty(NdlCommons.hasGUIDProperty))
			intf_ind.addProperty(NdlCommons.hasGUIDProperty, intf_ont.getProperty(NdlCommons.hasGUIDProperty).getString());
		
		child_ont.addProperty(NdlCommons.manifestHasParent, parent_device_ont);
		logger.debug("Add parent: child="+child_ont.getURI()
				+";parent="+parent_device_ont.getURI()
				+";parent de="+parent_ont
				+";intf="+intf_ont
				+";ip="+intf_ont.getProperty(NdlCommons.layerLabelIdProperty)
				+";id="+intf_ont.getProperty(NdlCommons.ip4LocalIPAddressProperty));
	}
	
	public boolean existingParent(OntResource c_ont,OntResource p_ont){
		boolean e=false;
		Resource parent_rs=null,parent_e_rs=null;
		for (StmtIterator j=c_ont.listProperties(NdlCommons.manifestHasParent);j.hasNext();){
			parent_rs=j.next().getResource();
			if(parent_rs.hasProperty(NdlCommons.collectionElementProperty)){
				parent_e_rs = parent_rs.getProperty(NdlCommons.collectionElementProperty).getResource();
				if(parent_e_rs.getURI().equals(p_ont.getURI())){
					e=true;
					break;
				}	
			}
		}
		return e;
	}
	
	//Modify slice reservations: add, remove, or modify
	public HashMap<String, List<ReservationMng>> modifyReservations(OntModel manifestModel,List<ReservationMng>  allRes
			,HashMap<String, SiteResourceTypes> typesMap, RequestSlice slice, ModifyReservations modifies
			, LinkedList <NetworkElement> addedDevices,LinkedList <NetworkElement> modifiedDevices) throws Exception{
		HashMap <String, List<ReservationMng>> m_map = new HashMap <String, List<ReservationMng>> ();
		LinkedList <OntResource> reservations = modifies.getRemovedElements();	
		List<ReservationMng> m_reservations=null;
		if(reservations!=null){
			m_reservations = removeReservations(manifestModel,allRes,reservations,false);
			m_map.put(ModifyType.REMOVE.toString(), m_reservations);
		}
		reservations = modifies.getModifiedRemoveElements();
		if(reservations!=null){
			m_reservations = removeReservations(manifestModel,allRes,reservations,true);
			m_map.put(ModifyType.MODIFYREMOVE.toString(), m_reservations);
		}
		if(addedDevices!=null){
			for(int i=0;i<addedDevices.size();i++){
				DomainElement ne = (DomainElement) addedDevices.get(i);
				if(ne.getCe()!=null && (ne.getCe().isModify()==false))
					elementCollection.add_vm((DomainElement) ne);
			}	
			reservations = modifies.getAddedElements();	
			m_reservations = addReservations(manifestModel,allRes,typesMap,slice,reservations,addedDevices);
			m_map.put(ModifyType.ADD.toString(), m_reservations);
		}else{
			logger.warn("No added reservation!");
		}
		if(modifiedDevices!=null){	
			//reservations = modifies.getAddedElements();	//for possible dependency 
			m_reservations = modifyReservations(manifestModel,allRes,m_reservations,modifiedDevices);
			m_map.put(ModifyType.MODIFY.toString(), m_reservations);
		}else{
			logger.warn("No modified reservation!");
		}
		//TDB.sync(manifestModel);
		return m_map;
	}
	//Modify existing reservations: dependencies.
	public List<ReservationMng> modifyReservations(OntModel manifestModel,List<ReservationMng> allRes
			,List<ReservationMng> added_reservations, LinkedList <NetworkElement> modifiedDevices) throws Exception{
		if(modifiedDevices==null)
			return null;
		
		HashMap <String, ReservationMng> r_map = new HashMap <String, ReservationMng> ();
		for(ReservationMng rmg:allRes){
			if (rmg.getState() != OrcaConstants.ReservationStateActive) 
				continue;
			Properties local = OrcaConverter.fill(rmg.getLocalProperties());
			String unit_url = local.getProperty(UNIT_URL_RES);
			String element_guid = local.getProperty(PropertyElementGUID);
			if(unit_url!=null)
				r_map.put(unit_url,rmg);
			else if(element_guid!=null)
				r_map.put(element_guid,rmg);
			logger.debug("ReservationConverter.modifyReservation:"+unit_url+";guid"+element_guid);
		}
		
		HashMap <String, ReservationMng> m_r_map = new HashMap <String, ReservationMng> ();
		if(added_reservations!=null){
			for(ReservationMng rmg:added_reservations){
				Properties local = OrcaConverter.fill(rmg.getLocalProperties());
				String unit_url = local.getProperty(UNIT_URL_RES);
				String element_guid = local.getProperty(PropertyElementGUID);
				if(unit_url!=null)
					m_r_map.put(unit_url,rmg);
				else if(element_guid!=null)
					m_r_map.put(element_guid,rmg);
			}
		}	
		logger.debug("Modify reservations:r_map="+r_map.size()+":m_r_map="+m_r_map.size());
		ArrayList<ReservationMng> reservations = new ArrayList<ReservationMng> ();
		//since each modify was created as a new device, but same ORCA reservation, need to distinguish......
		HashMap <ReservationMng,Integer> existMap = new HashMap <ReservationMng,Integer>();
		HashMap <ReservationMng,Integer> newMap = new HashMap <ReservationMng,Integer>();
		ArrayList<ReservationMng> p_r = null;
		ArrayList<ReservationMng> m_p_r = null;
		HashMap <ReservationMng,ArrayList<ReservationMng>> p_r_Map = new HashMap <ReservationMng,ArrayList<ReservationMng>>();
		HashMap <ReservationMng,ArrayList<ReservationMng>> m_p_r_Map = new HashMap <ReservationMng,ArrayList<ReservationMng>>();
		//count the number of storage parent
		HashMap <ReservationMng,Integer> m_p_storage_Map = new HashMap <ReservationMng,Integer> ();
		for(NetworkElement ne:modifiedDevices){
			DomainElement dd = (DomainElement) ne;
			String d_uri=dd.getName();
			ReservationMng rmg = r_map.get(d_uri);
			if(rmg==null && dd.getGUID()!=null)
				rmg = r_map.get(dd.getGUID());
			int numStorage=0;
			logger.debug("ModifiedReservation storage:d_uri="+d_uri+";"+dd.getGUID()+";reservation="+rmg);
			if(rmg!=null){
				if(m_p_storage_Map.containsKey(rmg))
					numStorage=m_p_storage_Map.get(rmg);
				else
					continue;
				
				if(dd.getPrecededBySet()==null)
					continue;
				for (Entry<DomainElement, OntResource> parent : dd.getPrecededBySet()) {
					DomainElement parent_de = parent.getKey();
					if(!parent_de.getResourceType().getResourceType().endsWith("lun")){
						numStorage++;
					}
				}
				m_p_storage_Map.put(rmg, numStorage);
			}
		}
		//form dependency properties		
		for(NetworkElement ne:modifiedDevices){
			DomainElement dd = (DomainElement) ne;
			String d_uri=dd.getName();
			ReservationMng rmg = r_map.get(d_uri);
			if(rmg==null && dd.getGUID()!=null)
				rmg = r_map.get(dd.getGUID());
			if(rmg!=null){
				logger.debug("ModifiedReservation:d_uri="+d_uri+";"+dd.getGUID()+";reservation="+rmg.getReservationID());
				Properties local = OrcaConverter.fill(rmg.getLocalProperties());
				local.setProperty(ReservationConverter.PropertyModifyVersion, String.valueOf(ne.getModifyVersion()));
				//modify properties for adding/deleting interfaces from links
				String num_interface_str=local.getProperty(ReservationConverter.PropertyParentNumInterface);
				int num_interface = 0;
				if(num_interface_str!=null)
					num_interface = Integer.valueOf(num_interface_str);
				HashMap<DomainElement, OntResource> preds = dd.getPrecededBy();
				if (preds == null){
					logger.warn("Modify reservations, No parent:"+dd);
					continue;
				}
				int numStorage=0;
				if(m_p_storage_Map.containsKey(rmg))
					numStorage=m_p_storage_Map.get(rmg);
				num_interface=num_interface+numStorage;
				int p=0,m_p=0,num=0;	
				p_r=p_r_Map.get(rmg);
				if(p_r==null){
					p_r = new ArrayList<ReservationMng> ();
					p_r_Map.put(rmg, p_r);
				}
				m_p_r=m_p_r_Map.get(rmg);
				if(m_p_r==null){
					m_p_r = new ArrayList<ReservationMng> ();
					m_p_r_Map.put(rmg, m_p_r);
				}
				if(existMap.containsKey(rmg))
					p=existMap.get(rmg);
				if(newMap.containsKey(rmg))
					m_p=newMap.get(rmg);
				num=p+m_p;
				for (Entry<DomainElement, OntResource> parent : dd.getPrecededBySet()) {
					DomainElement parent_de = parent.getKey();
					String p_uri = parent_de.getName();
					num++;
					//Parented by an existing reservation: joining a existing shared link
					logger.debug("ModifiedReservation:parent="+p_uri);
					ReservationMng p_rmg = r_map.get(p_uri);
					if(p_rmg!=null  && !p_r.contains(p_rmg)){
						logger.debug("ModifiedReservation Parent exiting:"+p_uri+";p_rmg="+p_rmg);	
						p++;
						p_r.add(p_rmg);
					}	
					
					//Parented by an added reservation: new link
					p_rmg = m_r_map.get(p_uri);
					if(p_rmg!=null && !m_p_r.contains(p_rmg)){
						logger.debug("ModifiedReservation Parent new:"+p_uri+";p_rmg="+p_rmg);
						m_p++;
						m_p_r.add(p_rmg);
						if(!parent_de.getResourceType().getResourceType().endsWith("lun")){
							String site_host_interface = getSiteHostInterface(parent);
							if(site_host_interface!=null){
								Properties p_property = new Properties();
								p_property.setProperty(UnitProperties.UnitQuantumNetname,site_host_interface);	
								p_rmg.setConfigurationProperties(OrcaConverter.merge(p_property, p_rmg.getConfigurationProperties()));
								p_rmg.setLocalProperties(OrcaConverter.merge(p_property, p_rmg.getLocalProperties()));
							}	
							Properties property = formInterfaceProperties(manifestModel, dd, parent,site_host_interface, num_interface, m_p);
							rmg.setLocalProperties(OrcaConverter.merge(property,rmg.getLocalProperties()));
						}
					}
				}
				
				//create properties to remember its parent reservations
				int ori_p=0;
				if(existMap.containsKey(rmg))
					ori_p=existMap.get(rmg);
				if(p>ori_p){
					local.setProperty(ReservationConverter.PropertyNumExistParentReservations, String.valueOf(p));
					for(int i=ori_p;i<p;i++){
						String key=ReservationConverter.PropertyExistParent + String.valueOf(i);
						local.setProperty(key, p_r.get(i).getReservationID());
					}
					existMap.put(rmg, p);
				}
				ori_p=0;
				if(newMap.containsKey(rmg))
					ori_p=newMap.get(rmg);
				if(m_p>ori_p){
					local.setProperty(ReservationConverter.PropertyNumNewParentReservations, String.valueOf(m_p));
					for(int i=ori_p;i<m_p;i++){
						String key=ReservationConverter.PropertyNewParent + String.valueOf(i);
						local.setProperty(key, m_p_r.get(i).getReservationID());
						logger.debug("ModifiedReservation Parent new property:"+key+";p_r="+m_p_r.get(i).getReservationID());
					}
					newMap.put(rmg, m_p);
				}
				rmg.setLocalProperties(OrcaConverter.merge(local, rmg.getLocalProperties()));
				if(!reservations.contains(rmg))
					reservations.add(rmg);
			}else
				logger.error("No reservation:"+dd.getName());
		}
		
		return reservations;
	}
	
	public List<ReservationMng> addReservations(OntModel manifestModel,List<ReservationMng> allRes
			,HashMap<String, SiteResourceTypes> typesMap, RequestSlice slice,LinkedList <OntResource>addedReservations
			, LinkedList <NetworkElement> addedDevices) throws Exception{
		if(addedReservations==null)
			return null;
		Collection <NetworkElement> boundElements = addedDevices;
		HashMap<String, ReservationRequest> map= formReservations(sm,boundElements, typesMap,slice);
		
		ArrayList<TicketReservationMng> add_reservations = setDependency(boundElements,map);
		
		ArrayList<ReservationMng> reservations = new ArrayList<ReservationMng> ();
		reservations.addAll(add_reservations);

		logger.debug("ReservationConverter addReservations:"+boundElements.size()+";"+map.size()+";"+reservations.size());
		
		return reservations;
	}
	
	public List<ReservationMng> removeReservations(OntModel manifestModel
			,List<ReservationMng> allRes,LinkedList <OntResource>removedReservations, boolean isModify) throws Exception{
		//Remove reservations
		if(removedReservations==null){
			logger.warn("remove collection is null!");
			return null;
		}
		
		List<ReservationMng> remove_reservations = new LinkedList <ReservationMng>();
		
		OntResource domain_ont=null;
		Iterator <OntResource> ont_it = removedReservations.iterator();
		String domain = null,domain_ont_url = null,domain_ont_name,type,rDomain,rType;
		
		while(ont_it.hasNext()){
			domain_ont = ont_it.next();
			if(domain_ont.hasProperty(NdlCommons.hasURLProperty))  
				domain_ont_url=domain_ont.getProperty(NdlCommons.hasURLProperty).getString();
			else
				domain_ont_url=domain_ont.getURI();
			domain = getDomainName(domain_ont_url);   //e.g., ben/vlan
			domain_ont_name = domain_ont.getURI();
			//logger.debug("ReservationConverter:domain_ont_url="+domain_ont_url+";domain="+domain+";domain_ont_name="+domain_ont_name);
			if(domain==null)
				continue;
			for (ReservationMng r: allRes) {
				Properties local = OrcaConverter.fill(r.getLocalProperties());
				type = r.getResourceType();
				rDomain = type.split("\\.")[0];
				rType = type.split("\\.")[1];
				String unit_url = local.getProperty(UNIT_URL_RES);
				if ((domain.equalsIgnoreCase(rDomain)) && (domain_ont_url.endsWith(rType)) && (domain_ont_name.equals(unit_url))) {
					if(!remove_reservations.contains(r)){
						logger.debug("Add reservation to be removed:"+domain_ont_url);
						remove_reservations.add((ReservationMng) r);
					}
				}
			}
			if(!isModify){
				removeManifest(domain_ont,manifestModel);			
				//remove it from the postboot scipt constructor
				elementCollection.remove_vm(domain_ont.getURI());
			}
		}
			
		return remove_reservations;
	}

	
	public void removeManifest(OntResource e_ont,OntModel manifestOntModel){
		LinkedList <OntResource> removed_rs = new LinkedList<OntResource>(); 
		ResIterator r_it = manifestOntModel.listResourcesWithProperty(NdlCommons.collectionElementProperty);
		Resource p_rs=null,o_rs=null;
		OntResource p_ont=null;
		while(r_it.hasNext()){
			p_rs = r_it.next();
			logger.info("modifyManifest:p_rs="+p_rs.getURI()+";e_ont="+e_ont.getURI());
			if(p_rs.hasProperty(NdlCommons.collectionElementProperty,e_ont)){
				p_ont=manifestOntModel.getOntResource(p_rs);
				removed_rs.add(p_ont);
			}
		}	
		//link in interdomain case
		r_it = manifestOntModel.listResourcesWithProperty(NdlCommons.collectionItemProperty);
		while(r_it.hasNext()){
			p_rs = r_it.next();
			logger.info("modifyManifest:remove item from p_rs="+p_rs.getURI()+";e_ont="+e_ont.getURI());
			if(p_rs.hasProperty(NdlCommons.collectionItemProperty,e_ont)){
				p_ont=manifestOntModel.getOntResource(p_rs);
				removed_rs.add(p_ont);
			}
		}
		for(OntResource pp_ont:removed_rs){
			if(pp_ont.hasProperty(NdlCommons.collectionElementProperty, e_ont))
				pp_ont.removeProperty(NdlCommons.collectionElementProperty, e_ont);
			if(pp_ont.hasProperty(NdlCommons.collectionItemProperty, e_ont))
				pp_ont.removeProperty(NdlCommons.collectionItemProperty, e_ont);
		}
		
		//removed properties of e_ont
		//TODO:reduce numCE by 1 in the nodegroup in request
		List <Statement> r_stmts = new LinkedList <Statement>();
		for (StmtIterator j=e_ont.listProperties();j.hasNext();){
			r_stmts.add(j.next());
			//p_rs=j.next().getResource();
			if(e_ont.hasProperty(NdlCommons.topologyHasInterfaceProperty)){
				o_rs = e_ont.getProperty(NdlCommons.topologyHasInterfaceProperty).getResource();
				if(o_rs.listProperties().hasNext())
					r_stmts.addAll(o_rs.listProperties().toList());
			}
			if(e_ont.hasProperty(NdlCommons.domainHasServiceProperty)){
				o_rs = e_ont.getProperty(NdlCommons.domainHasServiceProperty).getResource();
				if(o_rs.listProperties().hasNext())
					r_stmts.addAll(o_rs.listProperties().toList());
			}
		}
		manifestOntModel.remove(r_stmts);
	}
	
	public void updateState(OntModel manifestModel,OntResource v_ont,Property p,String new_str){
		clearProperty(manifestModel,v_ont,p);
		v_ont.addProperty(p, new_str);
	}
	
	public void updateState(OntModel manifestModel,OntResource v_ont,Property p,Resource new_r){
		clearProperty(manifestModel,v_ont,p);
		v_ont.addProperty(p, new_r);
	}
	
	public void clearProperty(OntModel manifestModel,OntResource v_ont,Property p){
		ExtendedIterator <OntModel> subList = manifestModel.listSubModels(true);
    	OntModel subModel=null;
    	OntResource vs_ont = null;
    	while(subList.hasNext()){
    		subModel = subList.next();
    		if(subModel.isInBaseModel(v_ont)){
    			vs_ont=subModel.getOntResource(v_ont);
    			vs_ont.removeAll(p);
    			break;
    		}
    	}
		v_ont.removeAll(p);
	}
	
	public void modifyTerm(OntModel manifestModel,OrcaReservationTerm term){
		String query = OntProcessor.createQueryStringReservationTerm();
		ResultSet rs = OntProcessor.rdfQuery(manifestModel, query);
		
		if (rs.hasNext()) {
			ResultBinding result = (ResultBinding)rs.next();

			// term 
			Resource term_rs = (Resource)result.get("term");

			// term 
			Resource duration_rs = (Resource)result.get("duration");
			if(duration_rs!=null){
				OntResource duration_ont = manifestModel.getOntResource(duration_rs);
				clearProperty(manifestModel, duration_ont, NdlCommons.daysProperty);
				duration_ont.addProperty(NdlCommons.daysProperty, String.valueOf(term.getDurationDays()),XSDDatatype.XSDdecimal);
				clearProperty(manifestModel, duration_ont, NdlCommons.hoursProperty);
				duration_ont.addProperty(NdlCommons.hoursProperty, String.valueOf(term.getDurationHours()),XSDDatatype.XSDdecimal);
				clearProperty(manifestModel, duration_ont, NdlCommons.minutesProperty);
				duration_ont.addProperty(NdlCommons.minutesProperty, String.valueOf(term.getDurationMins()), XSDDatatype.XSDdecimal);
				clearProperty(manifestModel, duration_ont, NdlCommons.secondsProperty);
				duration_ont.addProperty(NdlCommons.secondsProperty, String.valueOf(term.getDurationSecs()), XSDDatatype.XSDdecimal);
			}
			
			// end date
			Resource ed = (Resource)result.get("end");
			if(ed!=null){
				OntResource ed_ont = manifestModel.getOntResource(ed);
				clearProperty(manifestModel, ed_ont, NdlCommons.inXSDDateTime);
				Calendar cal = Calendar.getInstance();
				cal.setTime(term.getEnd());
				ed_ont.addProperty(NdlCommons.inXSDDateTime,DatatypeConverter.printDateTime(cal),XSDDatatype.XSDdateTime);
			}

		}
		//TDB.sync(manifestModel);
	}
	
	/**
	 * Set the term, skip total duration check if recovery set to true
	 * @param term
	 * @param recovery
	 * @throws ReservationConverterException
	 */
	public void setLeaseTerm(OrcaReservationTerm term, boolean recovery)  throws ReservationConverterException {
		long termDuration = DEFAULT_DURATION; //milliseconds
		if(term!=null){
			termDuration = ((long) term.getDurationInSeconds())*1000;
			if (term.getStart() == null){
				leaseStart = new Date();
			}
		}
		if (leaseStart == null){
			leaseStart=term.getStart();
		}
		
		// after recovery duration can be longer than allowed due to extensions /ib
		if(!recovery && termDuration > OrcaXmlrpcHandler.MaxReservationDuration) {
			throw new ReservationConverterException("Slice terms are limited to " + OrcaXmlrpcHandler.MaxReservationDuration/1000/3600 + " hours");
		}
		leaseEnd = new Date(leaseStart.getTime() + termDuration);
	}

	public Date getLeaseStart() {
		return leaseStart;
	}
	
	public Date getLeaseEnd() {
		return leaseEnd;
	}
	
	public static final String ActionVlanTag = "VLANtag";
	public static final String Server = "server";
	public static final String UriSeparator = "#";
	public static final String UriSuffix = "/Domain";
	public static final String FilenameSeparator = "\\.";

	public static String getDomainName(NetworkElement d) {
		String temp = null;

		if((d.getResource()!=null) && NdlCommons.isStitchingNodeInManifest(d.getResource())){
			if(d.getResource().hasProperty(NdlCommons.topologyHasName)){
				temp = d.getResource().getProperty(NdlCommons.topologyHasName).getString();
			}
		}else{
			String domain_url = d.getInDomain();
			temp = getDomainName(domain_url);
		}
		if(temp!=null)
			return temp+"/"+d.getType();

		return null;
	}

	public static String getDomainName(String domain_url) {
		if(domain_url==null)
			return null;
		int index = domain_url.indexOf(UriSeparator);
		if (index >= 0) {
			int index2 = domain_url.indexOf(UriSuffix, index);
			if (index2 >= 0) {
				return domain_url.substring(index+1, index2);
			}
			else{
				return domain_url.substring(index+1, domain_url.length());
			}
		}
		return null;
	}
	public static String getDomainLocalName(String domain_url) {
		if(domain_url==null)
			return null;
		String localName=null;
		int index = domain_url.indexOf(UriSeparator);
		if (index >= 0) {
			int index2 = domain_url.indexOf("/", index);
			if (index2 >= 0) {
				localName = domain_url.substring(index+1, index2) + "-" + domain_url.substring(index2+1, domain_url.length());
			}else
				localName = domain_url.substring(index+1, domain_url.length());
		}
		return localName;
	}
	
	protected class ReservationRequest {
		public String domain;
		public String domain_url;
		public LeaseReservationMng reservation;
		public boolean isNetwork;
		public boolean isVM;
		public boolean isLUN;
		public int networkDependencies;
		public int numStorageDependencies;
		public HashMap <String,String> storageDependencies = new HashMap <String,String>();
		public String uuid;
	}

	public static class ReservationConverterException extends Exception{
		public ReservationConverterException(String msg){
			super(msg);
		}
		
	}
    private String generateNewMAC(){
        //Generates libvirt compliant random mac addr (hopefully this.hashCode() is random enough
    	SecureRandom secureRandom = new SecureRandom();
    	String new_mac = null;
        while(true){
                long l = secureRandom.nextLong();

                l = Math.abs(l);

                StringBuffer m = new StringBuffer(Long.toString(l, 16));
                while (m.length() < 4) m.insert(0, "0");

                new_mac=OPENSTACK_MAC_PREFIX + m.substring(0,2) + ":" + m.substring(2,4);

                if(!orca_state_instance.existingUsedMac(new_mac)){
                        orca_state_instance.setUsedMac(new_mac);
                        break;
                }
        }
        return new_mac;
    }
    
    /**
     * Create login properties from users structure ("urn" and "keys" fields in each map, "keys" is a list)
     * @param users
     * @return
     */
    @SuppressWarnings("unchecked")
	public static Properties generateSSHProperties(List<Map<String, ?>> users) throws ReservationConverterException {
    	Properties sshProperties = new Properties();
    	
    	int loginCount = 1;
    	for(Map<String, ?> e: users) {

    		String userName = null;
    		try {
    			userName = (String)e.get(LOGIN_FIELD);
    		} catch (ClassCastException cce) {
    			continue;
    		}
    		
    		if (userName == null)
    			continue;
    		
    		// concatenate SSH keys for this login
    		List<String> userKeys = null;
    		try {
    			userKeys = (List<String>)e.get(KEYS_FIELD);
    		} catch (ClassCastException cce) {
    			try {
    				userKeys = new ArrayList<String>(Arrays.asList((Object[])e.get(KEYS_FIELD)));
    			} catch (ClassCastException cce1) {
    				try {
    					userKeys = Collections.singletonList((String)e.get(KEYS_FIELD));
    				} catch (ClassCastException cce2) {
    					continue;
    				}
    			}
    		}
    		
    		// get the sudo
    		String sudo = null;
    		try {
    			sudo = (String)e.get(SUDO_FIELD);
    			if (SUDO_YES.equalsIgnoreCase(sudo))
    				sudo = SUDO_YES;
    			else
    				sudo = SUDO_NO;
    		} catch (ClassCastException cce3) {
    			sudo = SUDO_NO;
    			
    		}
    		
    		// get the urn if present
    		String urn = null;
    		try {
    			urn = (String)e.get(URN_FIELD);
    		} catch (ClassCastException cce4) {
    			;
    		}
    		
    		StringBuilder sb = new StringBuilder();
    		for(String k: userKeys) {
    			sb.append(k);
    			sb.append("\n");
    		}
    		
    		// sanitize login
    		userName = userName.replaceAll("[\\W]|_", "");

    		sshProperties.setProperty(String.format(ConfigurationProperties.ConfigSSHLoginPattern, loginCount), userName);
    		sshProperties.setProperty(String.format(ConfigurationProperties.ConfigSSHKeyPattern, loginCount), sb.toString());
    		sshProperties.setProperty(String.format(ConfigurationProperties.ConfigSSHSudoPattern, loginCount), sudo);
    		if (urn != null)
    			sshProperties.setProperty(String.format(ConfigurationProperties.ConfigSSHUrnPattern, loginCount), urn);
    		
    		loginCount++;
    	}
    	
    	if (loginCount == 1) 
    		throw new ReservationConverterException("No valid logins are specified for compute resources");
    	
    	// simplify the life of ant handler
    	// by generating iterator of available login/key pairs ("1,2,3")
    	StringBuilder iterate = new StringBuilder();
    	iterate.append("1");
    	
    	for(int i = 2; i < loginCount; i++)
    		iterate.append("," + i);
    	
    	sshProperties.setProperty(ConfigurationProperties.ConfigSSHNumLogins, iterate.toString());
    	
    	return sshProperties;
    }
    
    
    private static String configUser = "config.ssh.user([\\d]+).";
    private static Pattern loginPattern = Pattern.compile(configUser + LOGIN_FIELD);
    private static Pattern sudoPattern = Pattern.compile(configUser + SUDO_FIELD);
    private static Pattern keysPattern = Pattern.compile(configUser + KEYS_FIELD);
    private static Pattern sshUrnPattern = Pattern.compile(configUser + URN_FIELD);
    /**
     * Convert from properties back into the users structure - a list of maps with login, key, sudo and urn fields.
     * Note that upon restoration it concatenates all keys into a single string and puts it in list of size one.
     * @param props
     * @return
     */
    public static List<Map<String, ?>> restoreUsers(List<PropertyMng> props) {
    	List<Map<String, ?>> users = new ArrayList<Map<String, ?>>();
    	
    	Map<Integer, String>logins = new HashMap<Integer, String>();
    	Map<Integer, String> urns = new HashMap<Integer, String>();
    	Map<Integer, Boolean> sudo = new HashMap<Integer, Boolean>();
    	Map<Integer, String> keys = new HashMap<Integer, String>();
    	
    	Integer maxIndex = 0;
    	for (PropertyMng p: props) {
    		Matcher m = loginPattern.matcher(p.getName());
    		if (m.matches()) {
    			String index = m.group(1);
    			Integer iIndex = Integer.parseInt(index);
    			logins.put(iIndex, p.getValue().trim());
    			maxIndex = (maxIndex > iIndex ? maxIndex : iIndex);
    		}
    		
    		m = sudoPattern.matcher(p.getName());
    		if (m.matches()) {
    			String index = m.group(1);
    			Integer iIndex = Integer.parseInt(index);
    			sudo.put(iIndex, Boolean.parseBoolean(p.getValue().trim()));
    			maxIndex = (maxIndex > iIndex ? maxIndex : iIndex);
    		}
    		
    		m = keysPattern.matcher(p.getName());
    		if (m.matches()) {
    			String index = m.group(1);
    			Integer iIndex = Integer.parseInt(index);
    			keys.put(iIndex, p.getValue().trim());
    			maxIndex = (maxIndex > iIndex ? maxIndex : iIndex);
    		}
    		
    		m = sshUrnPattern.matcher(p.getName());
    		if (m.matches()) {
    			String index = m.group(1);
    			Integer iIndex = Integer.parseInt(index);
    			urns.put(iIndex, p.getValue().trim());
    			maxIndex = (maxIndex > iIndex ? maxIndex : iIndex);
    		}
    	}
    	
    	// now convert these into a single datastructure
    	for (Integer index = 0; index <= maxIndex; index++) {
    		if ((logins.get(index) != null) && 
    				(sudo.get(index) != null) &&
    				(keys.get(index) != null)) {
    			Map<String, Object> toList = new HashMap<String, Object>();
    			toList.put(LOGIN_FIELD, logins.get(index));
    			toList.put(KEYS_FIELD, Collections.singletonList(keys.get(index)));
    			toList.put(SUDO_FIELD, sudo.get(index));
    			if (urns.get(index) != null)
    				toList.put(URN_FIELD, urns.get(index));
    			users.add(toList);
    		}
    	}
    	
    	return users;
    }
    
    /**
     * Recover reservation converter fields from the previously recovered RequestWorkflow
     * @param w
     */
    public void recover(RequestWorkflow w) {
    	// 
    	logger.info("Recovering ReservationConverter for workflow");
    	try {
			setLeaseTerm(w.getTerm(), true);
		} catch (ReservationConverterException e) {
			e.printStackTrace();
		}
    	recoverElementCollection(w.getBoundElements());
    }
    
    /**
     * Return a constant or a controller.max.duration property value (a long)
     * @return
     */
    public static long getMaxDuration() {
    	String durString = OrcaController.getProperty(OrcaXmlrpcHandler.MAX_DURATION_PROP);
    	Long ret = 0L;
    	if (durString != null) {
    		try {
    			ret = Long.parseLong(durString);
    		} catch (NumberFormatException nfe) {
    			;
    		}
    	}
    	if (ret == 0L)
    		return DEFAULT_MAX_DURATION;
    	else
    		return ret;
    }
    
//    public static void main(String[] argv) {
//    	System.out.println("HELLO");
//    	
//    	List<PropertyMng> toRestore = new ArrayList<PropertyMng>();
//    	PropertyMng pm = new PropertyMng();
//
//    	pm.setName("config.ssh.user1.login");
//    	pm.setValue("ibaldin");
//    	toRestore.add(pm);
//    	
//    	pm = new PropertyMng();
//    	pm.setName("config.ssh.user2.login");
//    	pm.setValue("someone");
//    	toRestore.add(pm);
//    	
//    	pm = new PropertyMng();
//    	pm.setName("config.ssh.user3.login");
//    	pm.setValue("else");
//    	toRestore.add(pm);
//    	
//    	pm = new PropertyMng();
//    	pm.setName("config.ssh.user1.sudo");
//    	pm.setValue("true");
//    	toRestore.add(pm);
//    	
//    	pm = new PropertyMng();
//    	pm.setName("config.ssh.user2.sudo");
//    	pm.setValue("false");
//    	toRestore.add(pm);
//    	
//    	pm = new PropertyMng();
//    	pm.setName("config.ssh.user3.sudo");
//    	pm.setValue("true");
//    	toRestore.add(pm);
//    	
//    	pm = new PropertyMng();
//    	pm.setName("config.ssh.user1.urn");
//    	pm.setValue("IDN:urn+user+1");
//    	toRestore.add(pm);
//    	
//    	pm = new PropertyMng();
//    	pm.setName("config.ssh.user2.urn");
//    	pm.setValue("IDN:urn+user+2");
//    	toRestore.add(pm);
//    	
//    	pm = new PropertyMng();
//    	pm.setName("config.ssh.user3.urn");
//    	pm.setValue("IDN:urn+user+3");
//    	toRestore.add(pm);
//    	
//    	pm = new PropertyMng();
//    	pm.setName("config.ssh.user1.keys");
//    	pm.setValue("ssh-key1\nssh-key2\n");
//    	toRestore.add(pm);
//    	
//    	pm = new PropertyMng();
//    	pm.setName("config.ssh.user2.keys");
//    	pm.setValue("ssh-key3");
//    	toRestore.add(pm);
//    	
//    	pm = new PropertyMng();
//    	pm.setName("config.ssh.user3.keys");
//    	pm.setValue("ssh-key4\nssh-key5\n");
//    	toRestore.add(pm);
//    	
//    	List<Map<String, ?>> users = restoreUsers(toRestore);
//    	
//    	System.out.println(users);
//    }
    
}
