package orca.ndl;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import orca.util.CompressEncode;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * This class generates NDL 
 * @author ibaldin
 *
 */
public class NdlGenerator {

	private static final String XML_SCHEMA_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";
	private static final String STITCHING_DOMAIN_URL = "http://geni-orca.renci.org/owl/orca.rdf#Stitching/Domain";
	private Logger l;
	public static final Set<String> externalSchemas = NdlCommons.externalSchemas.keySet();
	protected OntModel blank;
	private ReferenceModel ref = null;
    protected String requestId;
	
    
	/**
	 * Reference model built out of existing ontologies.
	 * We consult it when necessary. It is not meant to be modified.
	 * 
	 * @author ibaldin
	 *
	 */
	private static class ReferenceModel {
        String[] owls = {"topology.owl", "compute.owl", "ethernet.owl", "collections.owl",
        		"layer.owl", "domain.owl", "ip4.owl", "request.owl", "eucalyptus.owl", "ec2.owl", 
        		"planetlab.owl", "protogeni.owl", "kansei.owl", "openflow.owl", "geni.owl", 
        		"exogeni.owl", "modify.owl", "app-color.owl"};
        OntModel m;

        /**
         * If you want to use schema files embedded in the package, call this
         * before instantiating any Ndl generators. Otherwise schemas will be
         * loaded from URL
         */
        
		public ReferenceModel() {
			// create a OWL model
			m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

			// don't count on global redirections to be set
			NdlModel.setJenaRedirections(m.getDocumentManager());
			
			for (String owl: externalSchemas) {
				m.read(owl);
			}
			
			for (String owl: owls) {
				m.read(NdlCommons.ORCA_NS + owl);
			}
			
			NdlCommons.setPrefix(m);
	        m.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
	        m.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
	        m.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
	        m.setNsPrefix("time", "http://www.w3.org/2006/time#");
		}
		
		protected void done() {
			if (m != null) {
				m.close();
				m = null;
			}
		}
		
		@Override
		protected void finalize() {
			done();
		}
		
		public OntClass getOntClass(String s) {
			return m.getOntClass(s);
		}
		
		public DatatypeProperty getDataTypeProperty(String s) {
			return m.getDatatypeProperty(s);
		}
		
		public Property getProperty(String s) {
			return m.getProperty(s);
		}
		
		public String getNsPrefixUri(String prefix) {
			return m.getNsPrefixURI(prefix);
		}
	}

	private void setRequestId(String req) {
		requestId = req;
	}

	private static String massageName(String nm) {
		try {
			return URLEncoder.encode(nm, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return nm;
		}
		//return nm.replaceAll("[ \t#/]", "-");
	}
	
	public NdlGenerator(Logger log) {
		l = log;
		l.info("Initializing reference model");
		ref = new ReferenceModel();
        initializeRequest(null);
	}
	
	public NdlGenerator(String guid, Logger log) {
		l = log;
		l.info("Initializing reference model");
		ref = new ReferenceModel();
        initializeRequest(guid);
	}
	
	/**
	 * if modify set to true, initializes a modify request
	 * @param log
	 * @param modify
	 */
	public NdlGenerator(String guid, Logger log, boolean modify) {
		l = log;
		l.info("Initializing reference model");
		ref = new ReferenceModel();
		if (!modify) {
			initializeRequest(guid);
		} else {
			initializeModify(guid);
		}
	}
	
	/**
	 * Remove the model and the reference model. 
	 * Making calls on NdlGenerator after calling done() will result in NPEs
	 */
	public void done() {
		ref.done();
		if (blank != null) {
			blank.close();
			blank = null;
		}
	}
	
	@Override
	protected void finalize() {
		done();
	}
	
	/**
	 * Get the working model
	 * @return
	 */
	public OntModel getWorkingModel() {
		return blank;
	}
	
	private void initializeModify(String guid) {
		// create a blank model
        blank = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        
        // declare useful namespaces
        NdlCommons.setPrefix(blank);
        blank.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        blank.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        blank.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        blank.setNsPrefix("time", "http://www.w3.org/2006/time#");
        
        // generate a default UUID (can be over-written)
        if (guid == null)
        	requestId = UUID.randomUUID().toString();
        else
        	requestId = guid;
        blank.setNsPrefix("modify", NdlCommons.ORCA_NS + requestId +"#");
	}
	
	private void initializeRequest(String guid) {
		// create a blank model
        blank = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        
        // declare useful namespaces
        NdlCommons.setPrefix(blank);
        blank.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        blank.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        blank.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        blank.setNsPrefix("time", "http://www.w3.org/2006/time#");
        
        // generate a default UUID (can be over-written)
        if (guid == null)
        	requestId = UUID.randomUUID().toString();
        else
        	requestId = guid;
        blank.setNsPrefix("request", NdlCommons.ORCA_NS + requestId +"#");
	}
	
	/** 
	 * Add individual of a class from reference model. If it exists it will be reused.
	 * 
	 * @param indName
	 * @param clsName
	 * @return Individual
	 */
	private Individual addIndividual(String indName, String prefix, String clsName) throws NdlException {
		OntClass cls = ref.getOntClass(ref.getNsPrefixUri(prefix) + clsName);
		if (null == cls)
			throw new NdlException("Unable to find class " + clsName);

		return blank.createIndividual(NdlCommons.ORCA_NS + indName, cls);
	}
	
	/**
	 * Create an anonymous individual of a specified class
	 * @param prefix
	 * @param clsName
	 * @return
	 * @throws NdlException
	 */
	private Individual addAnonIndividual(String prefix, String clsName) throws NdlException {
		OntClass cls = ref.getOntClass(ref.getNsPrefixUri(prefix) + clsName);
		if (null == cls)
			throw new NdlException("Unable to find class " + clsName);
		return blank.createIndividual(cls);
	}
	
	/**
	 * Add a guaranteed unique individual to the model
	 * @param indName
	 * @param prefix
	 * @param clsName
	 * @return
	 * @throws NdlException
	 */
	private Individual addUniqueIndividual(String indName, String prefix, String clsName) throws NdlException {
		OntClass cls = ref.getOntClass(ref.getNsPrefixUri(prefix) + clsName);
		if (null == cls)
			throw new NdlException("Unable to find class " + clsName);
		if (blank.getIndividual(NdlCommons.ORCA_NS + indName) != null)
			throw new NdlException("Attempting to create a resource that already exists in the model: " + indName);
		return blank.createIndividual(NdlCommons.ORCA_NS + indName, cls);
	}
	
	/**
	 * add a property to an individual with value from model
	 * @param i
	 * @param prefix
	 * @param pName
	 * @param uri
	 * @return
	 */
	private Resource addProperty(Individual i, String prefix, String pName, String uri) throws NdlException {
		Property pr = ref.getProperty(ref.getNsPrefixUri(prefix) + pName);
		if (null == pr)
			throw new NdlException("Unable to find property " + pName);
		return i.addProperty(pr, blank.getResource(uri));
	}
	
	/**
	 * Add an property with RDFNode (can be a literal)
	 * @param i
	 * @param prefix
	 * @param pName
	 * @param n
	 * @return
	 * @throws NdlException
	 */
	private Resource addProperty(Individual i, String prefix, String pName, RDFNode n) throws NdlException {
		Property pr = ref.getProperty(ref.getNsPrefixUri(prefix) + pName);
		if (null == pr)
			throw new NdlException("Unable to find property " + pName);
		return i.addProperty(pr, n);
	}
	
	/**
	 * Remove property from an individual
	 * @param i
	 * @param prefix
	 * @param pName
	 * @param from
	 * @throws NdlException
	 */
	private void removeProperty(Individual i, String prefix, String pName, String toRemove) throws NdlException {
		Property pr = ref.getProperty(ref.getNsPrefixUri(prefix) + pName);
		if (null == pr) 
			throw new NdlException("Unable to find property " + pName);
		i.removeProperty(pr, blank.getResource(toRemove));
	}
	
	
	/**
	 * add a property to an individual
	 * @param i
	 * @param p
	 * @param ob
	 * @return
	 * @throws NdlException
	 */
	private Resource addProperty(Individual i, Property p, Resource ob) throws NdlException {
		return i.addProperty(p, ob);
	}
	
	/**
	 * add typed property to an individual
	 * @param i
	 * @param prefix
	 * @param pName
	 * @param val
	 * @param type
	 * @return
	 */
	public Resource addTypedProperty(Individual i, String prefix, String pName, String val, RDFDatatype type) throws NdlException {
		DatatypeProperty dpr = ref.getDataTypeProperty(ref.getNsPrefixUri(prefix) + pName);
		if (null == dpr)
			throw new NdlException("Unable to find datatype property " + pName);
		return i.addProperty(dpr, val, type);
	}
	
	/**
	 * Add simple literal property
	 * @param i
	 * @param prefix
	 * @param pName
	 * @param val
	 * @return
	 * @throws NdlException
	 */
	public Resource addSimpleProperty(Individual i, String prefix, String pName, String val) throws NdlException {
		Property pr = ref.getProperty(ref.getNsPrefixUri(prefix) + pName);
		return i.addProperty(pr, val);
	}
	
	/**
	 * Create a typed literal
	 * @param s
	 * @param typeURI
	 * @return
	 * @throws NdlException
	 */
	private Literal addTypedLiteral(String s, String typeURI) throws NdlException {
		return blank.createTypedLiteral(s, typeURI);
	}
	
	/**
	 * declare a ComputeElement in the request model 
	 * @param name
	 * @return
	 */
	public Individual declareComputeElement(String name) throws NdlException {
		Individual in = addIndividual(requestId + "#" + massageName(name), "compute", "ComputeElement");
		return in;
	}
	
	/**
	 * Declare a ComputeElement with existing url (from manifest) that will be modified. Modifies always require a guid.
	 * @param url
	 * @param guid
	 * @return
	 * @throws NdlException
	 */
	public Individual declareModifiedComputeElement(String url, String guid) throws NdlException {
		OntClass cls = ref.getOntClass(ref.getNsPrefixUri("compute") + "ComputeElement");
		
		if (null == cls)
			throw new NdlException("Unable to find class compute:ComputeElement");

		Individual in = blank.createIndividual(url, cls);
		
		addGuid(in, guid);
		addTypedProperty(in, "modify-schema", "isModify", "true", XSDDatatype.XSDboolean);
		
		return in;
	}
	
	/**
	 * declare a stitching node 
	 * @param name
	 * @return
	 * @throws NdlException
	 */
	public Individual declareStitchingNode(String name) throws NdlException {
		Individual in = addIndividual(requestId + "#" + massageName(name), "topology", "Device");
		addProperty(in, "request-schema", "inDomain", STITCHING_DOMAIN_URL);
		return in;
	}
	
	/**
	 * Declare storage node without parameters
	 * @param name
	 * @return
	 */
	public Individual declareISCSIStorageNode(String name) throws NdlException {
		Individual in = addIndividual(requestId + "#" + massageName(name), "storage", "ISCSI");
		addProperty(in, "domain", "hasResourceType", NdlCommons.ORCA_NS + "storage.owl#LUN");
		return in;
	}
	
	/**
	 * Declare an ISCSI storage node
	 * @param name
	 * @param capacity
	 * @param fstype - can be null
	 * @param fsparam - can be null
	 * @param fsmnt point - can be null
	 * @param doFormat
	 * @return
	 * @throws NdlException
	 */
	public Individual declareISCSIStorageNode(String name, long capacity, String fstype, String fsparam, String fsmntpoint, boolean doFormat) throws NdlException {
		Individual in = addIndividual(requestId + "#" + massageName(name), "storage", "ISCSI");
		addProperty(in, "domain", "hasResourceType", NdlCommons.ORCA_NS + "storage.owl#LUN");
		addTypedProperty(in, "storage", "storageCapacity", capacity + "", 
				TypeMapper.getInstance().getTypeByName(XML_SCHEMA_INTEGER));
		if (fstype != null)
			addTypedProperty(in, "storage", "hasFSType", fstype, XSDDatatype.XSDstring);
		if (fsparam != null)
			addTypedProperty(in, "storage", "hasFSParam", fsparam, XSDDatatype.XSDstring);
		if (fsmntpoint != null)
			addTypedProperty(in, "storage", "hasMntPoint", fsmntpoint, XSDDatatype.XSDstring);
		addTypedProperty(in, "storage", "doFormat", "" + doFormat, XSDDatatype.XSDboolean);
		return in;
	}
	
	/**
	 * declare a ServerCloud in the request model (with splittable property)
	 * @param name
	 * @param splittable
	 * @return
	 */
	public Individual declareServerCloud(String name, boolean splittable) throws NdlException {
		Individual ind = addIndividual(requestId + "#" + massageName(name), "compute", "ServerCloud");
		addTypedProperty(ind, "topology", "splittable", "" + splittable, XSDDatatype.XSDboolean);
		addSimpleProperty(ind, "request", "groupName", massageName(name));
		return ind;
		
	}
	
	/**
	 * declare an unsplittable (by default) ServerCloud in the request model 
	 * @param name
	 * @return
	 */
	public Individual declareServerCloud(String name) throws NdlException {
		Individual group = addIndividual(requestId + "#" + massageName(name), "compute", "ServerCloud");
		addSimpleProperty(group, "request", "groupName",  massageName(name));
		return group;
	}
	
	/**
	 * declare an interface in the request model
	 * @param name
	 * @return
	 */
	public Individual declareInterface(String name) throws NdlException {
		return addIndividual(requestId + "#" + massageName(name), "topology", "Interface");
	}
	
	/**
	 * Declare an existing interface for e.g. stitching (not in request namespace)
	 * @param url
	 * @return
	 * @throws NdlException
	 */
	public Individual declareExistingInterface(String url) throws NdlException {
		OntClass cls = ref.getOntClass(ref.getNsPrefixUri("topology") + "Interface");
		if (null == cls)
			throw new NdlException("Unable to find class " + "Interface");

		return blank.createIndividual(url, cls);
	}
	
	/**
	 * Declare a unique stitchport interface  based on URL and label with TaggedEthernet adaptation and a label
	 * @param url
	 * @param label
	 * @return
	 * @throws NdlException
	 */
	public Individual declareStitchportInterface(String url, String label) throws NdlException {
		Individual retI = declareExistingInterface(generateStitchPortInterfaceUrl(url, label));
		addLabelToIndividual(retI, label);
		// declare adaptation
		Individual intM = declareExistingInterface(url);
		addEthernetAdaptation(intM, retI);
		return retI;
	}
	
	/**
	 * Generate unique name for stitchport interface based on URL and label
	 * @param osp
	 * @return
	 * @throws NdlException
	 */
	private String generateStitchPortInterfaceUrl(String url, String label) throws NdlException {
		if ((url == null) || (label == null))
			throw new NdlException("Stitchport does not specify URL or label");
		
		if (url.endsWith("/"))
			return url + label;
		else
			return url + "/" + label;
	}
	
	/**
	 * declare a disk image
	 * @param name
	 * @param url
	 * @param guid
	 * @return
	 */
	public Individual declareDiskImage(String url, String guid) throws NdlException {
		if ((url == null) || (guid == null))
			throw new NdlException("Both image url and guid must be specified");
		Individual vmIm = addIndividual(requestId + "#" + massageName(guid), "compute", "DiskImage");
		addSimpleProperty(vmIm, "topology", "hasGUID", guid);
		addSimpleProperty(vmIm, "topology", "hasURL", url);
		return vmIm;
	}
	
	/**
	 * Declare an image with a short name
	 * @param url
	 * @param guid
	 * @param shortName
	 * @return
	 * @throws NdlException
	 */
	public Individual declareDiskImage(String url, String guid, String shortName) throws NdlException {
		if ((url == null) || (guid == null) || (shortName == null))
			throw new NdlException("Image url, guid and short name must be specified");
		Individual vmIm = addIndividual(requestId + "#" + massageName(shortName), "compute", "DiskImage");
		addSimpleProperty(vmIm, "topology", "hasGUID", guid);
		addSimpleProperty(vmIm, "topology", "hasURL", url);
		addTypedProperty(vmIm, "topology", "hasName", shortName, XSDDatatype.XSDstring);

		return vmIm;
	}
	/**
	 * declare a network point-to-point connection at specific layer
	 * @param name
	 * @param bandwidth
	 * @return
	 */
	public Individual addLayerToConnection(Individual conn, String prefix, String layer) throws NdlException {

		addProperty(conn, "layer", "atLayer", 
				ref.getOntClass(ref.getNsPrefixUri(prefix) + layer).getURI());
		return conn;
	}
	
	/***
	 * declare a network connection w/o layer
	 * @param name
	 * @param bandwidth
	 * @return
	 */
	public Individual addBandwidthToConnection(Individual conn, Long bandwidth) throws NdlException {
		addTypedProperty(conn, "layer", "bandwidth", bandwidth.toString(), 
				TypeMapper.getInstance().getTypeByName(XML_SCHEMA_INTEGER));
		return conn;
	}
	
	/**
	 * some connections don't declare bandwidth. you can add label or bandwidth to it.
	 * @param name
	 * @return
	 */
	public Individual declareNetworkConnection(String name) throws NdlException {
		Individual conn = addIndividual(requestId + "#" + massageName(name), "topology", "NetworkConnection");
		return conn;
	}
	
	/**
	 * Declare a broadcast connection. You can add label or bandwidth to it
	 * @param name
	 * @return
	 * @throws NdlException
	 */
	public Individual declareBroadcastConnection(String name) throws NdlException {
		Individual conn = addIndividual(requestId + "#" + massageName(name), "topology", "BroadcastConnection");
		return conn;
	}
	
	/**
	 * Declare a broadcast connection that already exists and will be modified
	 * @param url
	 * @return
	 * @throws NdlException
	 */
	public Individual declareModifiedBroadcastConnection(String url) throws NdlException {
		OntClass cls = ref.getOntClass(ref.getNsPrefixUri("topology") + "BroadcastConnection");
		
		if (null == cls)
			throw new NdlException("Unable to find class topology:BroadcastConnection");

		Individual in = blank.createIndividual(url, cls);
		
		addTypedProperty(in, "modify-schema", "isModify", "true", XSDDatatype.XSDboolean);
		
		return in;
	}
	
	/**
	 * Add a layer:Label to an individual (e.g. VLAN tag to connection)
	 * @param c
	 * @param label
	 * @throws NdlException
	 */
	public void addLabelToIndividual(Individual c, String label) throws NdlException {
		Individual lab = addIndividual(requestId + "#Label-" + massageName(label), "layer", "Label");
		addTypedProperty(lab, "layer", "label_ID", label, 
				TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#Literal"));
		addProperty(c, "layer", "label", lab.getURI());
	}
	
	/**
	 * Add a literal rdfs:label to something
	 * @param c
	 * @param label
	 * @throws NdlException
	 */
	public void addRDFSLabelToIndividual(Individual c, String label) throws NdlException {
		addTypedProperty(c, "rdfs", "label", label, 
				TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#Literal"));
	}
	/** 
	 * add interface to e.g. server or connection
	 * @param iface
	 * @param ind
	 */
	public void addInterfaceToIndividual(Individual iface, Individual ind) throws NdlException {
		addProperty(ind, "topology", "hasInterface", iface.getURI());
	}
	
	/**
	 * Remove a previously linked interface from individual
	 * @param iface
	 * @param ind
	 * @throws NdlException
	 */
	public void removeInterfaceFromIndividual(String iface, Individual ind) throws NdlException {
		removeProperty(ind, "topology", "hasInterface", iface);
	}
	
	/**
	 * add IP address to individual e.g. Interface. Note that adding the same IP
	 * will result in re-using a previously created individual, so if a model needs to
	 * contain multiple definitions of similar IP addresses, use addUniqueIPIndividual() instead
	 * @param ip
	 * @param ind
	 */
	public Individual addIPToIndividual(String ip, Individual ind) throws NdlException {
		String indName = requestId + "#ip-" + ip.replace('.', '-');
		// create ip Individual if not already there
		Individual ipInd = addIndividual(indName, "ip4", "IPAddress");
		// add label_ID property
		addTypedProperty(ipInd, "layer", "label_ID", ip, 
				TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#Literal"));
		// add property local IP address to the individual
		addProperty(ind, "ip4", "localIPAddress", ipInd.getURI());
		return ipInd;
	}
	
	/**
	 * Add a unique IP individual using e.g. VLAN name. Also see addIPIndividual().
	 * @param ip - ip address string
	 * @param name - e.g. vlan name
	 * @param ind
	 * @throws NdlException
	 */
	public Individual addUniqueIPToIndividual(String ip, String name, Individual ind) throws NdlException {
		String indName = requestId + "#" + massageName(name) + "-ip-" + ip.replace('.', '-');
		
		// create ip Individual if not already there
		Individual ipInd = addUniqueIndividual(indName, "ip4", "IPAddress");
		// add label_ID property
		addTypedProperty(ipInd, "layer", "label_ID", ip, 
				TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#Literal"));
		// add property local IP address to the individual
		addProperty(ind, "ip4", "localIPAddress", ipInd.getURI());
		
		return ipInd;
	}
	
	/**
	 * add hostinterface name literal to interface
	 * @param interfaceName
	 * @param intf
	 * @throws NdlException
	 */
	public void addNameToInterface(String interfaceName, Individual intf) throws NdlException {
		if (interfaceName != null)
			addTypedProperty(intf, "topology", "hostInterfaceName", interfaceName,
					TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#Literal"));
	}
	
	/**
	 * Add properties to storage node
	 * @param capacity
	 * @param fstype
	 * @param fsparam
	 * @param fsmntpoint
	 * @param doFormat
	 * @param in individual to add properties to
	 * @throws NdlException
	 */
	public void addPropertiesToStorage(long capacity, String fstype, String fsparam, String fsmntpoint, boolean doFormat, Individual in) throws NdlException {
		addTypedProperty(in, "storage", "storageCapacity", capacity + "", 
				TypeMapper.getInstance().getTypeByName(XML_SCHEMA_INTEGER));
		if (fstype != null)
			addTypedProperty(in, "storage", "hasFSType", fstype, XSDDatatype.XSDstring);
		if (fsparam != null)
			addTypedProperty(in, "storage", "hasFSParam", fsparam, XSDDatatype.XSDstring);
		if (fsmntpoint != null)
			addTypedProperty(in, "storage", "hasMntPoint", fsmntpoint, XSDDatatype.XSDstring);
		addTypedProperty(in, "storage", "doFormat", "" + doFormat, XSDDatatype.XSDboolean);
	}
	
	/**
	 * add netmask to IP address individual. Note that this creates IPaddress individual if it
	 * is not there. THIS METHOD SHOULD NOT BE USED.
	 * @param name
	 * @return
	 */
//	public void addNetmaskToIP(String ip, String netmask) throws NdlException {
//		String indName = requestId + "#ip-" + ip.replace('.', '-');
//		// create ip Individual if not already there
//		Individual ipInd = addIndividual(indName, "ip4", "IPAddress");
//		addTypedProperty(ipInd, "ip4", "netmask", netmask, 
//				TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#Literal"));
//		
//	}
	
	/**
	 * Add netmask to an individual IP subject
	 * @param ip
	 * @param netmask
	 * @throws NdlException
	 */
	public void addNetmaskToIP(Individual ip, String netmask) throws NdlException {
		addTypedProperty(ip, "ip4", "netmask", netmask, 
				TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#Literal"));
	}
	
	/***
	 * Add a URN to individual (hasURN)
	 * @param urn
	 * @param ind
	 */
	public void addURNToIndividual(Individual ind, String urn) throws NdlException {
		addTypedProperty(ind, "topology", "hasURN", urn,
				TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#anyURI"));
	}
	
	/**
	 * Declare beginning of a term
	 * @param date
	 * @return
	 * @throws NdlException
	 */
	public Individual declareTermBeginning(Date date) throws NdlException {
		Individual res = addIndividual(requestId + "#TermBeginning", "time", "Instant");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		addTypedProperty(res, "time", "inXSDDateTime", DatatypeConverter.printDateTime(cal),
				TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#dateTime"));
		return res;
	}
	
	/**
	 * Declare term end
	 * @param date
	 * @return
	 * @throws NdlException
	 */
	public Individual declareTermEnd(Date date) throws NdlException {
		Individual res = addIndividual(requestId + "#TermEnd", "time", "Instant");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		addTypedProperty(res, "time", "inXSDDateTime", DatatypeConverter.printDateTime(cal),
				TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#dateTime"));
		return res;
	}
	
	private void addToTerm(Individual term, long dur, int units) throws NdlException {
		switch(units) {
		case java.util.Calendar.YEAR:
			addTypedProperty(term, "time", "years", "" + dur, 
					TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#decimal"));
			break;
		case java.util.Calendar.MONTH:
			addTypedProperty(term, "time", "months", "" + dur, 
					TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#decimal"));
			break;
		case java.util.Calendar.DAY_OF_YEAR:
			addTypedProperty(term, "time", "days", "" + dur, 
					TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#decimal"));
			break;
		case java.util.Calendar.HOUR:
			addTypedProperty(term, "time", "hours", "" + dur, 
					TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#decimal"));
			break;
		case java.util.Calendar.MINUTE:
			addTypedProperty(term, "time", "minutes", "" + dur, 
					TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#decimal"));
			break;
		case java.util.Calendar.SECOND:
			addTypedProperty(term, "time", "seconds", "" + dur, 
					TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#decimal"));
			break;
		default: 
			throw new NdlException("declareTermDuration: unknown units, only YEAR, MONTH, DAY_OF_YEAR (DAY), HOUR, MINUTE and SECOND are allowed");
		}
	}
	
	/**
	 * Declare term duration in units from java.util.Calendar
	 * @param dur
	 * @param units
	 * @return
	 * @throws NdlException
	 */
	public Individual declareTermDuration(long dur, int units) throws NdlException {
		Individual res = addIndividual(requestId + "#TermDuration", "time", "DurationDescription");
		
		addToTerm(res, dur, units);
		return res;
	}
	
	/**
	 * declare term duration of days, hrs and minutes (can be 0)
	 * @param d
	 * @param hr
	 * @param min
	 * @return
	 * @throws NdlException
	 */
	public Individual declareTermDuration(long d, long hr, long min) throws NdlException {
		Individual t = addIndividual(requestId + "#TermDuration", "time", "DurationDescription");
		
		if (d > 0)
			addToTerm(t, d, java.util.Calendar.DAY_OF_YEAR);
		if (hr > 0)
			addToTerm(t, hr, java.util.Calendar.HOUR);
		if (min > 0)
			addToTerm(t, min, java.util.Calendar.MINUTE);
		
		return t;
	}
	
	/**
	 * Declare a term from now using duration, units comes from java.util.Calandar
	 * @param dur
	 * @param units
	 * @return
	 * @throws NdlException
	 */
	public Individual declareTerm() throws NdlException {
		Individual res = addIndividual(requestId + "#Term", "time", "Interval");
		
		return res;
	}
	
	public void addBeginningToTerm(Individual begI, Individual termI) throws NdlException {
		addProperty(termI, "time", "hasBeginning", begI.getURI());
	}
	
	public void addEndToTerm(Individual endI, Individual termI) throws NdlException {
		addProperty(termI, "time", "hasEnd", endI.getURI());
	}
	
	public void addDurationToTerm(Individual durI, Individual termI) throws NdlException {
		addProperty(termI, "time", "hasDurationDescription", durI.getURI());
	}
	
	/**
	 * Declare Reservation individual with start and end date
	 * @param id
	 * @param start
	 * @param end
	 */
	public Individual declareReservation(Date start, Date end) throws NdlException {
		Individual res = addIndividual(requestId + "#", "request-schema", "Reservation");
		
		// create and add a term
		Individual term = declareTerm();
		addBeginningToTerm(declareTermBeginning(start), term);
		addEndToTerm(declareTermEnd(end), term);
		addTermToReservation(term, res);
		return res;
	}
	
	/**
	 * Declare a reservation with a start time and duration
	 * @param start
	 * @param dur
	 * @param units
	 * @return
	 * @throws NdlException
	 */
	public Individual declareReservation(Date start, long dur, int units) throws NdlException {
		Individual res = addIndividual(requestId + "#", "request-schema", "Reservation");
		// create and add a term
		Individual term = declareTerm();
		addDurationToTerm(declareTermDuration(dur, units), term);
		addTermToReservation(term, res);
		return res;	
	}
	
	/**
	 * Declare an empty reservation
	 * @return
	 * @throws NdlException
	 */
	public Individual declareReservation() throws NdlException {
		Individual res = addIndividual(requestId + "#", "request-schema", "Reservation");

		return res;
	}

	/**
	 * Declare a modify reservation with this name
	 * @param name
	 * @return
	 * @throws NdlException
	 */
	public Individual declareModifyReservation(String name) throws NdlException {
		Individual mres = addIndividual(requestId + "#" + name, "modify-schema", "ModifyReservation");
		
		if (name != null)
			addTypedProperty(mres, "topology", "hasName", name, XSDDatatype.XSDstring);
		
		return mres;
	}
	
	/**
	 * Declare an element of a modify reservation that increases node count in a node group. 
	 * @param modify reservation individual
	 * @param ngUrl - URL of node group in question
	 * @param count - the number to increase node count by
	 * @return
	 * @throws NdlException
	 */
	public Individual declareModifyElementNGIncreaseBy(Individual mresI, String ngUrl, Integer count) throws NdlException {
		if (ngUrl == null)
			throw new NdlException("Group URL cannot be null");
		Individual melI = null;
		melI = addIndividual(requestId + "#modifyElement/" + UUID.randomUUID().toString(), "modify-schema", "ModifyElement");

		addProperty(mresI, "collections", "element", melI.getURI());
		
		addProperty(melI, "modify-schema", "modifySubject", ngUrl);
		
		addTypedProperty(melI, "modify-schema", "increaseBy", count.toString(), XSDDatatype.XSDinteger);
		
		return melI;
	}
	
	/**
	 * Declare an element of a modify reservation that deletes a node from a node group
	 * @param mresI
	 * @param ngUrl
	 * @param nodeUrl
	 * @return
	 * @throws NdlException
	 */
	public Individual declareModifyElementNGDeleteNode(Individual mresI, String ngUrl, String nodeUrl) throws NdlException {
		if ((ngUrl == null) || (nodeUrl == null))
			throw new NdlException("Group URL and node URL must not be null");
		
		Individual melI = null;
		melI = addIndividual(requestId + "#modifyElement/" + UUID.randomUUID().toString(), "modify-schema", "ModifyElement");

		addProperty(mresI, "collections", "element", melI.getURI());
		
		addProperty(melI, "modify-schema", "modifySubject", ngUrl);
		
		addProperty(melI, "modify-schema", "removeElement", nodeUrl);
		
		return melI;
	}
	
	/**
	 * Declare an element of a modify reservation that deletes a single node
	 * @param mresI
	 * @param link
	 * @return
	 * @throws NdlException
	 */
	public Individual declareModifyElementRemoveNode(Individual mresI, String elUrl, String guid) throws NdlException {
		
		// retrieve or create an individual
		Individual el = blank.getIndividual(elUrl);
		if (el == null) {
			OntClass cls = ref.getOntClass(ref.getNsPrefixUri("compute") + "ComputeElement");
			el = blank.createIndividual(elUrl, cls);
		}
		
		if (guid != null)
			addGuid(el, guid);
		
		Individual melI = null;
		melI = addIndividual(requestId + "#modifyElement/" + UUID.randomUUID().toString(), "modify-schema", "ModifyElement");
		
		addProperty(mresI, "collections", "element", melI.getURI());
		addProperty(melI, "modify-schema", "modifySubject", el.getURI());
		addProperty(melI, "modify-schema", "removeElement", el.getURI());
		
		return melI;
	}
	
	/**
	 * Declare an element of a modify reservation that deletes a single link
	 * @param mresI
	 * @param link
	 * @return
	 * @throws NdlException
	 */
	public Individual declareModifyElementRemoveLink(Individual mresI, String elUrl, String guid) throws NdlException {
		
		// retrieve or create an individual
		Individual el = blank.getIndividual(elUrl);
		if (el == null) {
			OntClass cls = ref.getOntClass(ref.getNsPrefixUri("topology") + "NetworkConnection");
			el = blank.createIndividual(elUrl, cls);
		}
		
		if (guid != null)
			addGuid(el, guid);
		
		Individual melI = null;
		melI = addIndividual(requestId + "#modifyElement/" + UUID.randomUUID().toString(), "modify-schema", "ModifyElement");
		
		addProperty(mresI, "collections", "element", melI.getURI());
		addProperty(melI, "modify-schema", "modifySubject", el.getURI());
		addProperty(melI, "modify-schema", "removeElement", el.getURI());
		
		return melI;
	}
	
	/**
	 * Declare an element of a modify reservation that deletes a single element individual
	 * @param mresI
	 * @param link
	 * @return
	 * @throws NdlException
	 */
	public Individual declareModifyElementRemoveElement(Individual mresI, Individual el) throws NdlException {
		Individual melI = null;
		melI = addIndividual(requestId + "#modifyElement/" + UUID.randomUUID().toString(), "modify-schema", "ModifyElement");
		
		addProperty(mresI, "collections", "element", melI.getURI());
		addProperty(melI, "modify-schema", "modifySubject", el.getURI());
		addProperty(melI, "modify-schema", "removeElement", el.getURI());
		
		return melI;
	}
	
	
	/**
	 * Declare an element of a modify reservation that adds a single element (node or link)
	 * @param mresI
	 * @param el
	 * @return
	 * @throws NdlException
	 */
	public Individual declareModifyElementAddElement(Individual mresI, Individual el) throws NdlException {
		Individual melI = null;
		melI = addIndividual(requestId + "#modifyElement/" + UUID.randomUUID().toString(), "modify-schema", "ModifyElement");
		
		addProperty(mresI, "collections", "element", melI.getURI());
		addProperty(melI, "modify-schema", "modifySubject", el.getURI());
		addProperty(melI, "modify-schema", "addElement", el.getURI());
		
		return melI;
	}
	
	/**
	 * Declare an element to modify an existing node (add interface)
	 * @param mresI
	 * @param node
	 * @return
	 * @throws NdlException
	 */
	public Individual declareModifyElementModifyNode(Individual mresI, Individual node) throws NdlException {
		Individual melI = null;
		melI = addIndividual(requestId + "#modifyElement/" + UUID.randomUUID().toString(), "modify-schema", "ModifyElement");
		
		addProperty(mresI, "collections", "element", melI.getURI());
		addProperty(melI, "modify-schema", "modifySubject", node.getURI());
		addProperty(melI, "modify-schema", "modifyElement", node.getURI());
		
		return melI;
	}
	
	/**
	 * Declare a slice with a name 
	 * @param sname
	 * @return
	 * @throws NdlException
	 */
	public Individual declareSlice(String sname) throws NdlException {
		Individual s = addUniqueIndividual(requestId + "#" + massageName(sname), "geni", "Slice");
		
		addTypedProperty(s, "topology", "hasName", sname, XSDDatatype.XSDstring);
		
		return s;
	}
	
	/**
	 * Declare an openflow slice
	 * @param sname
	 * @return
	 * @throws NdlException
	 */
	public Individual declareOfSlice(String sname) throws NdlException {
		Individual s = addUniqueIndividual(requestId + "#" + massageName(sname), "openflow", "OFSlice");
		
		addTypedProperty(s, "topology", "hasName", sname, XSDDatatype.XSDstring);
		
		return s;
	}
	
	/**
	 * Declare an openflow controller with this URL
	 * @param name
	 * @param url
	 * @return
	 * @throws NdlException
	 */
	public Individual declareOfController(String name, String url) throws NdlException {
		Individual c =  addUniqueIndividual(requestId + "#" + massageName(name), "openflow", "OFController");
		
		addTypedProperty(c, "topology", "hasURL", url, XSDDatatype.XSDanyURI);
		
		return c;
	}
	
	/**
	 * Declare a TCP proxy
	 * @param suffix
	 * @return
	 * @throws NdlException
	 */
	public Individual declareTCPProxy(String suffix) throws NdlException {
		Individual prx = addUniqueIndividual(requestId + "#" + massageName(suffix), "domain", "TCPProxy");
		return prx;
	}
	
	/**
	 * Add a proxy to (most likely node)
	 * @param proxy
	 * @param resI
	 * @throws NdlException
	 */
	public void addProxyToIndividual(Individual proxy, Individual resI) throws NdlException {
		addProperty(resI, "domain", "proxy", proxy.getURI());
	}
	
	/**
	 * add a resource to the reservation
	 * @param resI
	 * @param toAdd
	 * @throws NdlException
	 */
	public void addResourceToReservation(Individual resI, Individual toAdd) throws NdlException {
		addProperty(resI, "collections", "element", toAdd.getURI());
	}
	
	/**
	 * Add a sub-slice to reservation
	 * @param resI
	 * @param sl
	 * @throws NdlException
	 */
	public void addSliceToReservation(Individual resI, Individual sl) throws NdlException {
		addProperty(resI, "geni", "slice", sl.getURI());
	}
	
	/**
	 * Add DiskImage to reservation or node
	 * @param resI
	 * @param diskImage
	 * @throws NdlException
	 */
	public void addDiskImageToIndividual(Individual diskImage, Individual resI) throws NdlException {
		addProperty(resI, "compute", "diskImage", diskImage.getURI());
	}
		
	
	/**
	 * Add dependency to an individual (both should be  NetworkElement subclasses)
	 * @param dependency
	 * @param ind
	 * @throws NdlException
	 */
	public void addDependOnToIndividual(Individual dependency, Individual ind) throws NdlException {
		addProperty(ind, "request-schema", "dependOn", dependency.getURI());
	}
	
	/**
	 * Add resource type to e.g. server from topology.owl
	 * @param resType
	 * @param ind
	 * @throws NdlException
	 */
//	public void addResourceTypeToServer(Individual ser, String topoRes) throws NdlException {
//		addProperty(ser, "topology", "hasResourceType", 
//					ref.getNsPrefixUri("topology") + topoRes);
//	}
	
	/**
	 * Add a count of specific type of units to individual 
	 */
//	public void addUnitCountToServer(Individual ser, String unitDomain, String unitType, Integer unitCount) throws NdlException {
//		addTypedProperty(ser, unitDomain, unitType, unitCount.toString(), 
//				TypeMapper.getInstance().getTypeByName(XML_SCHEMA_INTEGER));
//	}
	
	/**
	 * Add a term to a reservation
	 * @param resI
	 * @param Term
	 * @throws NdlException
	 */
	public void addTermToReservation(Individual termI, Individual resI) throws NdlException {
		addProperty(resI, "request-schema", "hasTerm", termI.getURI());
	}
	
	/**
	 *  try to get individual with this name from the request
	 * @param name
	 * @return
	 */
	public Individual getRequestIndividual(String name) {
		return blank.getIndividual(NdlCommons.ORCA_NS + requestId + "#" + massageName(name));
	}
	
	/**
	 * get an individual that is not part of request namespace
	 * @param name
	 * @return
	 */
	public Individual getNonrequestIndividual(String name) {
		return blank.getIndividual(NdlCommons.ORCA_NS + name);
	}
	
	/**
	 * declare a domain site (appends /Domain to name as per convention;
	 * declares it in the general http://geni-orca.renci.org/owl prefix, not request prefix)
	 * @param file
	 * @return
	 * @throws NdlException
	 */
	public Individual declareDomain(String name) throws NdlException {
		return addIndividual(name + "/Domain", "topology", "NetworkDomain");
	}
	
	/**
	 * Make individual openflow capable (with version 1.0, 1.1 or 1.2)
	 * @param res
	 * @param version
	 * @throws NdlException
	 */
	public void addOpenFlowCapable(Individual res, String version) throws NdlException {
		if ("1.0".equals(version)) {
			addProperty(res, NdlCommons.openflowCapableProperty, NdlCommons.openflowV1_0Ind);
		} else if ("1.1".equals(version)) {
			addProperty(res, NdlCommons.openflowCapableProperty, NdlCommons.openflowV1_1Ind);
		} else if ("1.2".equals(version)) {
			addProperty(res, NdlCommons.openflowCapableProperty, NdlCommons.openflowV1_2Ind);
		}
	}
	
	/**
	 * Bind node to domain
	 * @param dom
	 * @param node
	 * @throws NdlException
	 */
	public void addNodeToDomain(Individual dom, Individual node) throws NdlException {
		addProperty(node, "request-schema", "inDomain", dom.getURI());
	}
	
	/**
	 * Add 'inDomain' property to individual
	 * @param dom
	 * @param ind
	 * @throws NdlException
	 */
	public void addDomainToIndividual(Individual dom, Individual ind) throws NdlException {
		addProperty(ind, "request-schema", "inDomain", dom.getURI());
	}
	
	/**
	 * Add node type from particular namespace to node
	 * @param ns
	 * @param tp
	 * @param node
	 * @throws NdlException
	 */
	public void addNodeTypeToCE(String ns, String tp, Individual node) throws NdlException {
		addProperty(node, "compute", "specificCE", ref.getNsPrefixUri(ns) + tp);
	}
	
	/**
	 * Add a post boot script string to a node
	 * @param pbscript
	 * @param node
	 * @throws NdlException
	 */
	public void addPostBootScriptToCE(String pbscript, Individual node) throws NdlException {
		addTypedProperty(node, "request-schema", "postBootScript", pbscript,  XSDDatatype.XSDstring);
	}
	
	/**
	 * Add a port to be proxied to a proxy (no information how it will be proxied)
	 * @param port
	 * @param proxy
	 * @throws NdlException
	 */
	public void addPortToProxy(String port, Individual prx) throws NdlException {
		addTypedProperty(prx, "topology", "proxiedPort", port, XSDDatatype.XSDunsignedShort);  
	}
	
	/**
	 * Add a port with proxy information about it (new port and new IP address)
	 * @param port
	 * @param newPort
	 * @param newIp
	 * @param prx
	 * @throws NdlException
	 */
	public void addPortToProxy(String port, String newPort, String newIp, Individual prx) throws NdlException {
		addTypedProperty(prx, "topology", "proxiedPort", port, XSDDatatype.XSDunsignedShort);
		addTypedProperty(prx, "topology", "managementIP", newIp, XSDDatatype.XSDstring);
		addTypedProperty(prx, "topology", "managementPort", newPort, XSDDatatype.XSDstring);
	}
	
	/**
	 * Add number of compute elements to cluster
	 * <layer:numCE rdf:datatype="&xsd;integer">2</layer:numCE>
	 * @param ns
	 * @param cluster
	 * @throws NdlException
	 */
	public void addNumCEsToCluster(Integer ns, Individual cluster) throws NdlException {
		addTypedProperty(cluster, "layer", "numCE", ns.toString(), XSDDatatype.XSDinteger);
	}
	
	/**
	 * Add a VM domain resource property (for Euca clusters)
	 * @param cluster
	 */
	public void addVMDomainProperty(Individual cluster) throws NdlException {
		addProperty(cluster, "domain", "hasResourceType", NdlCommons.ORCA_NS + "compute.owl#VM");
	}
	
	public void addBareMetalDomainProperty(Individual cluster) throws NdlException {
		addProperty(cluster, "domain", "hasResourceType", NdlCommons.ORCA_NS + "compute.owl#BareMetalCE");
	}
	
	public void addFourtyGBareMetalDomainProperty(Individual cluster) throws NdlException {
		addProperty(cluster, "domain", "hasResourceType", NdlCommons.ORCA_NS + "compute.owl#FourtyGBareMetalCE");
	}
	
	/**
	 * Add OpenFlow-relevant properties to a slice individual
	 * @param userEmail
	 * @param slicePass
	 * @param userCtrl
	 * @param slice
	 * @throws NdlException
	 */
	public void addOfPropertiesToSlice(String userEmail, String slicePass, String userCtrl, Individual slice) throws NdlException {
		addTypedProperty(slice, "topology", "hasEmail", userEmail, XSDDatatype.XSDstring);
		addTypedProperty(slice, "openflow", "hasSlicePassword", slicePass, XSDDatatype.XSDstring);
		Individual ctrl = declareOfController(slice.getLocalName() + "-of-ctrl", userCtrl);
		addProperty(slice, "openflow", "controller", ctrl.getURI());
	}
	
	public void addUserDNProperty(String dn, Individual i) throws NdlException {
		addTypedProperty(i, "topology", "userDN", dn, XSDDatatype.XSDinteger);
	}
	
	/*
	 * Color properties
	 */
	
	/**
	 * Declare a new color individual
	 * @param label - label
	 * @param keys - map of key value pairs
	 * @param blob - text blob
	 * @param isXML - is it XML?
	 * @return
	 */
	public Individual declareColor(String label, Map<String, String> keys, String blob, boolean isXML) throws NdlException {
		Individual in = addAnonIndividual("app-color", "Color");
		addSimpleProperty(in, "app-color", "hasColorLabel", label);
		addKeysOnColor(in, keys);
		addBlobOnColor__(in, blob, isXML, null);
		return in;
	}
	
	/**
	 * add keys to color
	 * @param color
	 * @param keys
	 */
	public void addKeysOnColor(Individual color, Map<String, String> keys) throws NdlException {
		if (keys == null)
			return;
		for(Entry<String, String> e: keys.entrySet()) {
			Individual ca = addAnonIndividual("app-color", "ColorAttribute");
			addSimpleProperty(ca, "app-color", "hasColorKey", e.getKey());
			addSimpleProperty(ca, "app-color", "hasColorValue", e.getValue());
			addProperty(color, "app-color", "hasColorAttribute", ca);
		}
	}
	
	/**
	 * Add text blob to color
	 * @param color - color individual
	 * @param blob - text blob
	 * @param isXML - is it XML?
	 */
	public Individual addBlobOnColor(Individual color, String blob, boolean isXML) throws NdlException {
		if (color == null)
			return color;
		if (blob == null)
			return color;
		if (isXML) {
			Literal blobLit = addTypedLiteral(blob, "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral");
			assert(blobLit.isWellFormedXML());
			addProperty(color, "app-color", "hasColorXMLBlob", blobLit);
		}
		else 
			addSimpleProperty(color, "app-color", "hasColorBlob", blob);
		return color;
	}
	
	/**
	 * Use compression instead of xml literal to get around Jena bugs for XML literals
	 * @param color
	 * @param blob
	 * @param isXML
	 * @param xmlns - namespace for inner xml
	 * @return
	 * @throws NdlException
	 */
	public Individual addBlobOnColor__(Individual color, String blob, boolean isXML, String xmlns) throws NdlException {
		if (color == null)
			return color;
		if (blob == null)
			return color;
		if (isXML) {
			addSimpleProperty(color, "app-color", "hasColorXMLCompressedBlob", compressEncode(blob));
		}
		else 
			addSimpleProperty(color, "app-color", "hasColorBlob", blob);
		return color;
	}
	
	/**
	 * Encode a color dependency between two network elements
	 * @param from - network element 
	 * @param to - network element
	 * @param color - color
	 */
	public void encodeColorDependency(Individual from, Individual to, Individual color) throws NdlException {
		addProperty(from, "app-color", "toColorDependency", color);
		addProperty(to, "app-color", "fromColorDependency", color);
	}
	
	public Individual addColorToIndividual(Individual ne, Individual color) throws NdlException {
		addProperty(ne, "app-color", "hasColor", color);
		return ne;
	}
	
	/**
	 * Add ethernet adaptation to another interface
	 * @param intf - interface 
	 * @param adaptTo - interface to be adapted
	 * @return
	 * @throws NdlException
	 */
	public Individual addEthernetAdaptation(Individual intf, Individual adaptTo) throws NdlException {
		addProperty(intf, "ethernet", "Tagged-Ethernet", adaptTo);
		return intf;
	}
	
	/**
	 * Add addGuid property to this individual
	 * @param in
	 * @param guid
	 * @throws NdlException
	 */
	public void addGuid(Individual in, String guid) throws NdlException { 
		addSimpleProperty(in, "topology", "hasGUID", guid);
	}
	
	/**
	 * produce N3 output of the model
	 */
	public String toN3String() {
		StringWriter sw = new StringWriter();
		blank.write(sw, "N3-PP");
		return sw.toString();
	}
	
	@Override
	public String toString() {
		return toN3String();
	}
	
	/**
	 * produce RDF-XML output of the model
	 * @return
	 */
	public String toXMLString() {
		StringWriter sw = new StringWriter();
		blank.write(sw);
		return sw.toString();
	}
	
	/**
	 * Produce Graphviz output for visualization
	 * @return
	 */
	public String getGVOutput() {
		return OntProcessor.substrateDotString(blank);
	}
	
    static String compressEncode(String inputString) {
    	return CompressEncode.compressEncode(inputString);
    	// changed after we had to create dependency on orca.core.util
    	// which has these /ib 04/15/14
//        // gzip
//        byte[] inputBytes;
//        ByteArrayOutputStream baos = null; 
//        GZIPOutputStream gzos = null;
//
//        if (inputString == null) return null;
//
//        try {
//            baos = new ByteArrayOutputStream();
//            gzos = new GZIPOutputStream(baos);
//
//            try {
//                inputBytes = inputString.getBytes("UTF-8");
//            }
//            catch (UnsupportedEncodingException uee) {
//                inputBytes = inputString.getBytes();
//            }
//
//            gzos.write(inputBytes);
//            gzos.close();
//        }   
//        catch (IOException ioe) {
//            ioe.printStackTrace();
//            return null;
//        }
//        finally {
//            try { gzos.close(); } catch (Exception e) {}
//            try { baos.close(); } catch (Exception e) {}
//        }
//        
//        // base64-encode
//        return Base64.encodeBase64String(baos.toByteArray());
    }
    
    
    static String decodeDecompress(String inputString) {
    	try {
    		return CompressEncode.decodeDecompress(inputString);
    	} catch (Exception e) {
    		return null;
    	}
    	// changed after we had to create dependency on orca.core.util
    	// which has these /ib 04/15/14
//        // base64-decode and gunzip
//        ByteArrayInputStream bais = null;
//        GZIPInputStream gzis = null;
//        ByteArrayOutputStream baos = null;
//
//        if (inputString == null) return null;
//
//        try {
//            bais = new ByteArrayInputStream(Base64.decodeBase64(inputString));
//            gzis = new GZIPInputStream(bais);
//            baos = new ByteArrayOutputStream();
//            byte[] buf = new byte[2048];
//            int len = 0;
//
//            while ((len = gzis.read(buf)) >= 0) {
//                baos.write(buf, 0, len);
//            }
//        }   
//        catch (IOException ioe) {
//            ioe.printStackTrace();
//            return null;
//        }
//        finally {
//            try { baos.close(); } catch (Exception e) {}
//            try { gzis.close(); } catch (Exception e) {}
//            try { bais.close(); } catch (Exception e) {}
//        }
//
//        try {
//            return new String(baos.toByteArray(), "UTF-8");
//        }
//        catch (UnsupportedEncodingException uue) {
//            return new String(baos.toByteArray());
//        }
    }


	public static void main(String[] argv) {
//		Date now = new Date();
//		Calendar calLocal = Calendar.getInstance();
//		calLocal.setTime(now);
//		Calendar calUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
//		calUTC.setTime(now);
//		System.out.println("Local " + DatatypeConverter.printDateTime(calLocal));
//		System.out.println("UTC " + DatatypeConverter.printDateTime(calUTC));
////		Logger myLog = Logger.getLogger("NdlGenerator");
////		NdlCommons.setGlobalJenaRedirections();
////		NdlGenerator ngen = new NdlGenerator(null, myLog, true);
////		
////		try {
////			Individual i = ngen.declareModifyReservation("my-modify");
////			ngen.declareModifyElementNGIncreaseBy(i, "http://some.resource/nodeGroup-1", 5);
////			ngen.declareModifyElementNGDeleteNode(i, "http://some.resource/nodeGroup-1", "http://some.resource/nodeGroup-1/0");
////		} catch (NdlException e) {
////			System.err.println("NdlException " + e);
////		}
////		
////		System.out.println("Modify \n" + ngen.toXMLString());
////		myLog.info("Done");
		
		NdlGenerator ngen = new NdlGenerator("some-unique-guid", Logger.getLogger("my logger"));
//		String blob = "<gemininode type=\"mp_node\" > \n" + 
//						"<geminiservices>\n" + 
//						"<geminiactive install=\"yes\" enable=\"yes\"/>\n" +
//						"<geminipassive install=\"yes\" enable=\"yes\"/>\n" +
//						"</geminiservices>\n" +
//						"</gemininode>";
		String blob = "<gemininode> \n" + 
				"<geminiservices>\n" + 
				"<geminiactive install=\"yes\" enable=\"yes\">blah1</geminiactive>\n" +
				"<geminipassive>blah2</geminipassive>\n" +
				"</geminiservices>\n" +
				"</gemininode>";
		

		try {
			ngen.declareColor("test color", null, blob, true);
		} catch (NdlException ee) {
			System.out.println("Exception: " + ee);
		}
		
		System.out.println(ngen.toXMLString());
	}
	
}
