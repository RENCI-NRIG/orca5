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
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * This class generates NDL
 * 
 * @author ibaldin
 *
 */
public class NdlGenerator {

    private static final String MODIFY_NS = "modify";
    private static final String REQUEST_NS = "request";
    private static final String XML_SCHEMA_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";
    private static final String STITCHING_DOMAIN_URL = "http://geni-orca.renci.org/owl/orca.rdf#Stitching/Domain";
    private Logger l;
    public static final Set<String> externalSchemas = NdlModel.externalSchemas.keySet();
    protected OntModel blank;
    private ReferenceModel ref = null;
    protected String requestId;

    /**
     * Reference model built out of existing ontologies. We consult it when necessary. It is not meant to be modified.
     * 
     * @author ibaldin
     *
     */
    private static class ReferenceModel {
        String[] owls = { "topology.owl", "compute.owl", "ethernet.owl", "collections.owl", "layer.owl", "domain.owl",
                "ip4.owl", "request.owl", "eucalyptus.owl", "ec2.owl", "planetlab.owl", "protogeni.owl", "kansei.owl",
                "openflow.owl", "geni.owl", "exogeni.owl", "modify.owl", "app-color.owl" };
        OntModel m;

        /**
         * If you want to use schema files embedded in the package, call this before instantiating any Ndl generators.
         * Otherwise schemas will be loaded from URL
         */

        public ReferenceModel() {
            // create a OWL model
            m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

            // don't count on global redirections to be set
            NdlModel.setJenaRedirections(m.getDocumentManager());

            for (String owl : externalSchemas) {
                m.read(owl);
            }

            for (String owl : owls) {
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
        // return nm.replaceAll("[ \t#/]", "-");
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
     * @param guid guid
     * @param log log
     * @param modify modify
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
     * Build a generator based on pre-existing request model in the parser.
     * 
     * @param p p
     * @param log log
     * @throws NdlException in case of error
     */
    public NdlGenerator(NdlRequestParser p, Logger log) throws NdlException {
        l = log;
        l.info("Initializing model from parser");
        blank = p.getModel();
        ref = new ReferenceModel();
        // get request id from URL
        String nsUri = blank.getNsPrefixURI(REQUEST_NS);
        if (nsUri == null)
            throw new NdlException("Model in NdlRequestParser doesn't contain request namespace!");
        requestId = nsUri.replace(NdlCommons.ORCA_NS, "");
        requestId = requestId.replace("#", "");
    }

    /**
     * Build a generator based on pre-existing modify model in the parser
     * 
     * @param p p
     * @param log log
     * @throws NdlException in case of error 
     */
    public NdlGenerator(NdlModifyParser p, Logger log) throws NdlException {
        l = log;
        l.info("Initializing model from parser");
        blank = p.getModel();
        ref = new ReferenceModel();
        // get request id from URL
        String nsUri = blank.getNsPrefixURI(MODIFY_NS);
        if (nsUri == null)
            throw new NdlException("Model in NdlModifyParser doesn't contain modify namespace!");
        requestId = nsUri.replace(NdlCommons.ORCA_NS, "");
        requestId = requestId.replace("#", "");
    }

    /**
     * Remove the model and the reference model. Making calls on NdlGenerator after calling done() will result in NPEs
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
     * 
     * @return OntModel
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
        blank.setNsPrefix(MODIFY_NS, NdlCommons.ORCA_NS + requestId + "#");
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
        blank.setNsPrefix(REQUEST_NS, NdlCommons.ORCA_NS + requestId + "#");
    }

    /**
     * Add individual of a class from reference model. If it exists it will be reused.
     * 
     * @param indName
     * @param clsName
     * @return Individual
     * @throws NdlException in case of error
     */
    private Individual addIndividual(String indName, String prefix, String clsName) throws NdlException {
        assert (indName != null);
        assert (prefix != null);
        assert (clsName != null);

        OntClass cls = ref.getOntClass(ref.getNsPrefixUri(prefix) + clsName);
        if (null == cls)
            throw new NdlException("Unable to find class " + clsName);

        return blank.createIndividual(NdlCommons.ORCA_NS + indName, cls);
    }

    /**
     * Create an anonymous individual of a specified class
     * 
     * @param prefix prefix
     * @param clsName
     * @return Individual
     * @throws NdlException in case of error
     */
    private Individual addAnonIndividual(String prefix, String clsName) throws NdlException {
        assert (prefix != null);
        assert (clsName != null);

        OntClass cls = ref.getOntClass(ref.getNsPrefixUri(prefix) + clsName);
        if (null == cls)
            throw new NdlException("Unable to find class " + clsName);
        return blank.createIndividual(cls);
    }

    /**
     * Add a guaranteed unique individual to the model
     * 
     * @param indName
     * @param prefix prefix
     * @param clsName
     * @return Individual
     * @throws NdlException in case of error
     */
    private Individual addUniqueIndividual(String indName, String prefix, String clsName) throws NdlException {
        assert (indName != null);
        assert (prefix != null);
        assert (clsName != null);

        OntClass cls = ref.getOntClass(ref.getNsPrefixUri(prefix) + clsName);
        if (null == cls)
            throw new NdlException("Unable to find class " + clsName);
        if (blank.getIndividual(NdlCommons.ORCA_NS + indName) != null)
            throw new NdlException("Attempting to create a resource that already exists in the model: " + indName);
        return blank.createIndividual(NdlCommons.ORCA_NS + indName, cls);
    }

    /**
     * add a property to an individual with value from model
     * 
     * @param i i
     * @param prefix prefix
     * @param pName pName
     * @param uri
     * @return Resource 
     * @throws NdlException in case of error
     */
    private Resource addProperty(Individual i, String prefix, String pName, String uri) throws NdlException {
        assert (i != null);
        assert (prefix != null);
        assert (pName != null);
        assert (uri != null);

        Property pr = ref.getProperty(ref.getNsPrefixUri(prefix) + pName);
        if (null == pr)
            throw new NdlException("Unable to find property " + pName);
        return i.addProperty(pr, blank.getResource(uri));
    }

    /**
     * Add an property with RDFNode (can be a literal)
     * 
     * @param i i
     * @param prefix prefix
     * @param pName pName
     * @param n
     * @return Resource 
     * @throws NdlException in case of error
     */
    private Resource addProperty(Individual i, String prefix, String pName, RDFNode n) throws NdlException {
        assert (i != null);
        assert (prefix != null);
        assert (pName != null);
        assert (n != null);

        Property pr = ref.getProperty(ref.getNsPrefixUri(prefix) + pName);
        if (null == pr)
            throw new NdlException("Unable to find property " + pName);
        return i.addProperty(pr, n);
    }

    /**
     * Remove property from an individual
     * 
     * @param i i
     * @param prefix prefix
     * @param pName pName
     * @param from
     * @throws NdlException in case of error
     */
    private void removeProperty(Individual i, String prefix, String pName, String toRemove) throws NdlException {
        assert (i != null);
        assert (prefix != null);
        assert (pName != null);
        assert (toRemove != null);

        Property pr = ref.getProperty(ref.getNsPrefixUri(prefix) + pName);
        if (null == pr)
            throw new NdlException("Unable to find property " + pName);
        i.removeProperty(pr, blank.getResource(toRemove));
    }

    /**
     * add a property to an individual
     * 
     * @param i i
     * @param p
     * @param ob
     * @return Resource 
     * @throws NdlException in case of error
     */
    private Resource addProperty(Individual i, Property p, Resource ob) throws NdlException {
        assert (i != null);
        assert (p != null);
        assert (ob != null);

        return i.addProperty(p, ob);
    }

    /**
     * add typed property to an individual
     * 
     * @param i i
     * @param prefix prefix
     * @param pName pName
     * @param val val
     * @param type type
     * @return Resource 
     * @throws NdlException in case of error
     */
    public Resource addTypedProperty(Individual i, String prefix, String pName, String val, RDFDatatype type)
            throws NdlException {
        if (null == i) {
            throw new IllegalArgumentException("Individual `i` cannot be null");
        }

        DatatypeProperty dpr = ref.getDataTypeProperty(ref.getNsPrefixUri(prefix) + pName);
        if (null == dpr)
            throw new NdlException("Unable to find datatype property " + pName);
        return i.addProperty(dpr, val, type);
    }

    /**
     * Add simple literal property
     * 
     * @param i i
     * @param prefix prefix
     * @param pName pName
     * @param val val
     * @return Resource 
     * @throws NdlException in case of error
     */
    public Resource addSimpleProperty(Individual i, String prefix, String pName, String val) throws NdlException {
        if (null == i) {
            throw new IllegalArgumentException("Individual `i` cannot be null");
        }

        Property pr = ref.getProperty(ref.getNsPrefixUri(prefix) + pName);
        return i.addProperty(pr, val);
    }

    /**
     * Create a typed literal
     * 
     * @param s s
     * @param typeURI typeURI
     * @return Literal 
     * @throws NdlException in case of error
     */
    private Literal addTypedLiteral(String s, String typeURI) throws NdlException {
        return blank.createTypedLiteral(s, typeURI);
    }

    /**
     * declare a ComputeElement in the request model
     * 
     * @param name name
     * @return Individual
     * @throws NdlException in case of error
     */
    public Individual declareComputeElement(String name) throws NdlException {
        Individual in = addIndividual(requestId + "#" + massageName(name), "compute", "ComputeElement");
        return in;
    }

    /**
     * Declare a ComputeElement with existing url (from manifest) that will be modified. Modifies always require a guid.
     * 
     * @param url url
     * @param guid guid
     *            (can be null)
     * @return Individual
     * @throws NdlException in case of error
     */
    public Individual declareModifiedComputeElement(String url, String guid) throws NdlException {

        OntClass cls = ref.getOntClass(ref.getNsPrefixUri("compute") + "ComputeElement");

        if (null == cls)
            throw new NdlException("Unable to find class compute:ComputeElement");

        Individual in = blank.createIndividual(url, cls);

        if (guid != null)
            addGuid(in, guid);
        addTypedProperty(in, "modify-schema", "isModify", "true", XSDDatatype.XSDboolean);

        return in;
    }

    /**
     * declare a stitching node
     * 
     * @param name name
     * @return Individual
     * @throws NdlException in case of error
     */
    public Individual declareStitchingNode(String name) throws NdlException {
        Individual in = addIndividual(requestId + "#" + massageName(name), "topology", "Device");
        addProperty(in, "request-schema", "inDomain", STITCHING_DOMAIN_URL);
        return in;
    }

    /**
     * Declare storage node without parameters
     * 
     * @param name name
     * @return Individual
     * @throws NdlException in case of error
     */
    public Individual declareISCSIStorageNode(String name) throws NdlException {
        Individual in = addIndividual(requestId + "#" + massageName(name), "storage", "ISCSI");
        addProperty(in, "domain", "hasResourceType", NdlCommons.ORCA_NS + "storage.owl#LUN");
        return in;
    }

    /**
     * Declare an ISCSI storage node
     * 
     * @param name name
     * @param capacity capacity
     * @param fstype
     *            - can be null
     * @param fsparam
     *            - can be null
     * @param fsmntpoint 
     *            point - can be null
     * @param doFormat doFormat
     * @return Individual
     * @throws NdlException in case of error
     */
    public Individual declareISCSIStorageNode(String name, long capacity, String fstype, String fsparam,
            String fsmntpoint, boolean doFormat) throws NdlException {
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
     * 
     * @param name name
     * @param splittable splittable
     * @return Individual
     * @throws NdlException in case of error
     */
    public Individual declareServerCloud(String name, boolean splittable) throws NdlException {
        Individual ind = addIndividual(requestId + "#" + massageName(name), "compute", "ServerCloud");
        addTypedProperty(ind, "topology", "splittable", "" + splittable, XSDDatatype.XSDboolean);
        addSimpleProperty(ind, REQUEST_NS, "groupName", massageName(name));
        return ind;

    }

    /**
     * declare an unsplittable (by default) ServerCloud in the request model
     * 
     * @param name name
     * @return Individual
     * @throws NdlException in case of error
     */
    public Individual declareServerCloud(String name) throws NdlException {
        Individual group = addIndividual(requestId + "#" + massageName(name), "compute", "ServerCloud");
        addSimpleProperty(group, REQUEST_NS, "groupName", massageName(name));
        return group;
    }

    /**
     * declare an interface in the request model
     * 
     * @param name name
     * @throws NdlException in case of error
     * @return Individual
     */
    public Individual declareInterface(String name) throws NdlException {
        return addIndividual(requestId + "#" + massageName(name), "topology", "Interface");
    }

    /**
     * Declare an existing interface for e.g. stitching (not in request namespace)
     * 
     * @param url url
     * @throws NdlException in case of error
     * @return Individual
     */
    public Individual declareExistingInterface(String url) throws NdlException {
        OntClass cls = ref.getOntClass(ref.getNsPrefixUri("topology") + "Interface");
        if (null == cls)
            throw new NdlException("Unable to find class " + "Interface");

        return blank.createIndividual(url, cls);
    }

    /**
     * Declare a unique stitchport interface based on URL and label with TaggedEthernet adaptation and a label
     * 
     * @param url url
     * @param label label
     * @throws NdlException in case of error
     * @return Individual
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
     * 
     * @param url url 
     * @param  label label
     * @throws NdlException in case of error
     * @return String 
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
     * 
     * @param url url
     * @param guid guid
     * @throws NdlException in case of error
     * @return Individual 
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
     * 
     * @param url url
     * @param guid guid
     * @param shortName shortName
     * @throws NdlException in case of error
     * @return Individual 
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
     * 
     * @param conn conn 
     * @param prefix prefix
     * @param layer layer
     * @throws NdlException in case of error
     * @return Individual 
     */
    public Individual addLayerToConnection(Individual conn, String prefix, String layer) throws NdlException {
        if (null == conn) {
            throw new IllegalArgumentException("Individual `conn` cannot be null");
        }

        addProperty(conn, "layer", "atLayer", ref.getOntClass(ref.getNsPrefixUri(prefix) + layer).getURI());
        return conn;
    }

    /***
     * declare a network connection w/o layer
     * 
     * @param conn conn 
     * @param bandwidth bandwidth
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual addBandwidthToConnection(Individual conn, Long bandwidth) throws NdlException {
        if (null == conn) {
            throw new IllegalArgumentException("Individual `conn` cannot be null");
        }

        addTypedProperty(conn, "layer", "bandwidth", bandwidth.toString(),
                TypeMapper.getInstance().getTypeByName(XML_SCHEMA_INTEGER));
        return conn;
    }

    /**
     * some connections don't declare bandwidth. you can add label or bandwidth to it.
     * 
     * @param name name
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareNetworkConnection(String name) throws NdlException {
        Individual conn = addIndividual(requestId + "#" + massageName(name), "topology", "NetworkConnection");
        return conn;
    }

    /**
     * Declare a broadcast connection. You can add label or bandwidth to it
     * 
     * @param name name
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareBroadcastConnection(String name) throws NdlException {
        Individual conn = addIndividual(requestId + "#" + massageName(name), "topology", "BroadcastConnection");
        return conn;
    }

    /**
     * Declare a broadcast connection that already exists and will be modified
     * 
     * @param url url
     * @throws NdlException in case of error
     * @return Individual    
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
     * 
     * @param c c
     * @param label label
     * @throws NdlException in case of error
     */
    public void addLabelToIndividual(Individual c, String label) throws NdlException {
        if (null == c) {
            throw new IllegalArgumentException("Individual `c` cannot be null");
        }

        Individual lab = addIndividual(requestId + "#Label-" + massageName(label), "layer", "Label");
        addTypedProperty(lab, "layer", "label_ID", label,
                TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#Literal"));
        addProperty(c, "layer", "label", lab.getURI());
    }

    /**
     * Add a literal rdfs:label to something
     * 
     * @param c c
     * @param label label
     * @throws NdlException in case of error
     */
    public void addRDFSLabelToIndividual(Individual c, String label) throws NdlException {
        if (null == c) {
            throw new IllegalArgumentException("Individual `c` cannot be null");
        }

        addTypedProperty(c, "rdfs", "label", label,
                TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#Literal"));
    }

    /**
     * add interface to e.g. server or connection
     * 
     * @param iface iface
     * @param ind ind
     * @throws NdlException in case of error
     */
    public void addInterfaceToIndividual(Individual iface, Individual ind) throws NdlException {
        if (null == iface) {
            throw new IllegalArgumentException("Individual `iface` cannot be null");
        }
        if (null == ind) {
            throw new IllegalArgumentException("Individual `ind` cannot be null");
        }

        addProperty(ind, "topology", "hasInterface", iface.getURI());
    }

    /**
     * Remove a previously linked interface from individual
     * 
     * @param iface iface
     * @param ind ind
     * @throws NdlException in case of error
     */
    public void removeInterfaceFromIndividual(String iface, Individual ind) throws NdlException {
        if (null == iface) {
            throw new IllegalArgumentException("Individual `iface` cannot be null");
        }
        if (null == ind) {
            throw new IllegalArgumentException("Individual `ind` cannot be null");
        }

        removeProperty(ind, "topology", "hasInterface", iface);
    }

    /**
     * add IP address to individual e.g. Interface. Note that adding the same IP will result in re-using a previously
     * created individual, so if a model needs to contain multiple definitions of similar IP addresses, use
     * addUniqueIPIndividual() instead. It disassociates previously created IP individual from the interface.
     * 
     * @param ip ip
     * @param ind ind
     * @throws NdlException in case of error
     * @return Individual
     */
    public Individual addIPToIndividual(String ip, Individual ind) throws NdlException {
        ind.removeAll(NdlCommons.ip4LocalIPAddressProperty);
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
     * Add a unique IP individual using e.g. VLAN name. Also see addIPIndividual(). It disassociates previously created
     * IP individual from the interface.
     * 
     * @param ip
     *            - ip address string
     * @param name
     *            - e.g. vlan name
     * @param ind ind
     * @throws NdlException in case of error
     * @return Individual
     */
    public Individual addUniqueIPToIndividual(String ip, String name, Individual ind) throws NdlException {
        if (null == ind) {
            throw new IllegalArgumentException("Individual `ind` cannot be null");
        }

        ind.removeAll(NdlCommons.ip4LocalIPAddressProperty);
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
     * Add netmask to an individual IP subject (the subject must have been added using addIPToIndividual or
     * addUniqueIPToIndividual
     * 
     * @param ip ip
     * @param netmask netmask
     * @throws NdlException in case of error
     */
    public void addNetmaskToIP(Individual ip, String netmask) throws NdlException {
        if (null == ip) {
            throw new IllegalArgumentException("Individual `ip` cannot be null");
        }

        addTypedProperty(ip, "ip4", "netmask", netmask,
                TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#Literal"));
    }

    /**
     * Set IP on interface individual, including overwriting any existing IP (but not netmask). This function is
     * complimentary to the addIPToIndividual() and can undo its results.
     * 
     * @param ip ip
     * @param intf intf
     * @throws NdlException in case of error
     */
    public void setInterfaceIP(String ip, Individual intf) throws NdlException {
        if (null == intf) {
            throw new IllegalArgumentException("Individual `intf` cannot be null");
        }

        Statement s = intf.getProperty(NdlCommons.ip4LocalIPAddressProperty);

        Individual ipInd = null;
        if (s != null) {
            ipInd = blank.getIndividual(s.getResource().getURI());
            ipInd.removeAll(NdlCommons.layerLabelIdProperty);
        } else {
            // /ib anonymous didn't work because they have no URI to retrieve an individual from, so use guids
            ipInd = addIndividual("ip-" + UUID.randomUUID().toString(), "ip4", "IPAddress");
            addProperty(intf, "ip4", "localIPAddress", ipInd.getURI());
        }
        addTypedProperty(ipInd, "layer", "label_ID", ip,
                TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#Literal"));

    }

    /**
     * Set netmask on interface, including overwriting any existing netmask (but not IP). This function is complimentary
     * to addNetmaskToIP() and can undo its results.
     * 
     * @param nm nm
     * @param intf intf
     * @throws NdlException in case of error
     */
    public void setInterfaceNetmask(String nm, Individual intf) throws NdlException {
        if (null == intf) {
            throw new IllegalArgumentException("Individual `intf` cannot be null");
        }

        Statement s = intf.getProperty(NdlCommons.ip4LocalIPAddressProperty);

        Individual ipInd = null;
        if (s != null) {
            ipInd = blank.getIndividual(s.getResource().getURI());
            ipInd.removeAll(NdlCommons.ip4NetmaskProperty);
        } else {
            // /ib anonymous didn't work because they have no URI to retrieve an individual from, so use guids
            ipInd = addIndividual("ip-" + UUID.randomUUID().toString(), "ip4", "IPAddress");
            addProperty(intf, "ip4", "localIPAddress", ipInd.getURI());
        }
        addTypedProperty(ipInd, "ip4", "netmask", nm,
                TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#Literal"));
    }

    /**
     * add hostinterface name literal to interface
     * 
     * @param interfaceName interfaceName
     * @param intf intf
     * @throws NdlException in case of error
     */
    public void addNameToInterface(String interfaceName, Individual intf) throws NdlException {
        if (interfaceName != null)
            addTypedProperty(intf, "topology", "hostInterfaceName", interfaceName,
                    TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#Literal"));
    }

    /**
     * Add properties to storage node
     * 
     * @param capacity capacity
     * @param fstype fstype
     * @param fsparam fsparam
     * @param fsmntpoint fsmntpoint
     * @param doFormat doFormat
     * @param in 
     *            individual to add properties to
     * @throws NdlException in case of error
     */
    public void addPropertiesToStorage(long capacity, String fstype, String fsparam, String fsmntpoint,
            boolean doFormat, Individual in) throws NdlException {
        if (null == in) {
            throw new IllegalArgumentException("Individual `in` cannot be null");
        }

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

    /***
     * Add a URN to individual (hasURN)
     * 
     * @param urn urn
     * @param ind ind
     * @throws NdlException in case of error
     */
    public void addURNToIndividual(Individual ind, String urn) throws NdlException {
        if (null == ind) {
            throw new IllegalArgumentException("Individual `ind` cannot be null");
        }

        addTypedProperty(ind, "topology", "hasURN", urn,
                TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#anyURI"));
    }

    /**
     * Declare beginning of a term
     * 
     * @param date date
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareTermBeginning(Date date) throws NdlException {
        if (null == date) {
            // cal.setTime() doesn't valid inputs, so we should do it here
            throw new IllegalArgumentException("Date cannot be null");
        }

        Individual res = addIndividual(requestId + "#TermBeginning", "time", "Instant");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        addTypedProperty(res, "time", "inXSDDateTime", DatatypeConverter.printDateTime(cal),
                TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#dateTime"));
        return res;
    }

    /**
     * Declare term end
     * 
     * @param date date
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareTermEnd(Date date) throws NdlException {
        if (null == date) {
            // cal.setTime() doesn't valid inputs, so we should do it here
            throw new IllegalArgumentException("Date cannot be null");
        }

        Individual res = addIndividual(requestId + "#TermEnd", "time", "Instant");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        addTypedProperty(res, "time", "inXSDDateTime", DatatypeConverter.printDateTime(cal),
                TypeMapper.getInstance().getTypeByName("http://www.w3.org/2001/XMLSchema#dateTime"));
        return res;
    }

    private void addToTerm(Individual term, long dur, int units) throws NdlException {
        if (null == term) {
            throw new IllegalArgumentException("Individual `term` cannot be null");
        }

        switch (units) {
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
            throw new NdlException(
                    "declareTermDuration: unknown units, only YEAR, MONTH, DAY_OF_YEAR (DAY), HOUR, MINUTE and SECOND are allowed");
        }
    }

    /**
     * Declare term duration in units from java.util.Calendar
     * 
     * @param dur dur
     * @param units units
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareTermDuration(long dur, int units) throws NdlException {
        Individual res = addIndividual(requestId + "#TermDuration", "time", "DurationDescription");

        addToTerm(res, dur, units);
        return res;
    }

    /**
     * declare term duration of days, hrs and minutes (can be 0)
     * 
     * @param d d
     * @param hr hr
     * @param min min
     * @throws NdlException in case of error
     * @return Individual    
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
     * 
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareTerm() throws NdlException {
        Individual res = addIndividual(requestId + "#Term", "time", "Interval");

        return res;
    }

    public void addBeginningToTerm(Individual begI, Individual termI) throws NdlException {
        if (null == begI) {
            throw new IllegalArgumentException("Individual begI cannot be null");
        }
        if (null == termI) {
            throw new IllegalArgumentException("Individual termI cannot be null");
        }

        addProperty(termI, "time", "hasBeginning", begI.getURI());
    }

    public void addEndToTerm(Individual endI, Individual termI) throws NdlException {
        if (null == endI) {
            throw new IllegalArgumentException("Individual endI cannot be null");
        }
        if (null == termI) {
            throw new IllegalArgumentException("Individual termI cannot be null");
        }

        addProperty(termI, "time", "hasEnd", endI.getURI());
    }

    public void addDurationToTerm(Individual durI, Individual termI) throws NdlException {
        if (null == durI) {
            throw new IllegalArgumentException("Individual durI cannot be null");
        }
        if (null == termI) {
            throw new IllegalArgumentException("Individual termI cannot be null");
        }

        addProperty(termI, "time", "hasDurationDescription", durI.getURI());
    }

    /**
     * Declare Reservation individual with start and end date
     *
     * @param start start
     * @param end end
     * @throws NdlException in case of error
     * @return Individual
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
     * 
     * @param start start
     * @param dur dur
     * @param units units
     * @throws NdlException in case of error
     * @return Individual    
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
     * 
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareReservation() throws NdlException {
        Individual res = addIndividual(requestId + "#", "request-schema", "Reservation");

        return res;
    }

    /**
     * Declare a modify reservation with this name
     * 
     * @param name name
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareModifyReservation(String name) throws NdlException {
        Individual mres = addIndividual(requestId + "#" + name, "modify-schema", "ModifyReservation");

        if (name != null)
            addTypedProperty(mres, "topology", "hasName", name, XSDDatatype.XSDstring);

        return mres;
    }

    /**
     * Declare an element of a modify reservation that increases node count in a node group.
     * 
     * @param mresI 
     *            reservation individual
     * @param ngUrl
     *            - URL of node group in question
     * @param count
     *            - the number to increase node count by
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareModifyElementNGIncreaseBy(Individual mresI, String ngUrl, Integer count)
            throws NdlException {
        if (null == mresI) {
            throw new IllegalArgumentException("Individual mresI cannot be null");
        }

        if (ngUrl == null)
            throw new NdlException("Group URL cannot be null");

        Individual melI = null;
        melI = addIndividual(requestId + "#modifyElement/" + UUID.randomUUID().toString(), "modify-schema",
                "ModifyElement");

        addProperty(mresI, "collections", "element", melI.getURI());

        addProperty(melI, "modify-schema", "modifySubject", ngUrl);

        addTypedProperty(melI, "modify-schema", "increaseBy", count.toString(), XSDDatatype.XSDinteger);

        return melI;
    }

    /**
     * Declare an element of a modify reservation that deletes a node from a node group
     * 
     * @param mresI mresI
     * @param ngUrl ngUrl
     * @param nodeUrl nodeUrl
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareModifyElementNGDeleteNode(Individual mresI, String ngUrl, String nodeUrl)
            throws NdlException {
        if (null == mresI) {
            throw new IllegalArgumentException("Individual mresI cannot be null");
        }

        if ((ngUrl == null) || (nodeUrl == null))
            throw new NdlException("Group URL and node URL must not be null");

        Individual melI = null;
        melI = addIndividual(requestId + "#modifyElement/" + UUID.randomUUID().toString(), "modify-schema",
                "ModifyElement");

        addProperty(mresI, "collections", "element", melI.getURI());

        addProperty(melI, "modify-schema", "modifySubject", ngUrl);

        addProperty(melI, "modify-schema", "removeElement", nodeUrl);

        return melI;
    }

    /**
     * Declare an element of a modify reservation that deletes a single node
     * 
     * @param mresI mresI
     * @param elUrl elUrl
     * @param guid guid
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareModifyElementRemoveNode(Individual mresI, String elUrl, String guid) throws NdlException {
        if (null == mresI) {
            throw new IllegalArgumentException("Individual mresI cannot be null");
        }

        // retrieve or create an individual
        Individual el = blank.getIndividual(elUrl);
        if (el == null) {
            OntClass cls = ref.getOntClass(ref.getNsPrefixUri("compute") + "ComputeElement");
            el = blank.createIndividual(elUrl, cls);
        }

        if (guid != null)
            addGuid(el, guid);

        return doModifyElement(mresI, el, "removeElement");
    }

    /**
     * Declare an element of a modify reservation that deletes a single link
     * 
     * @param mresI mresI
     * @param elUrl elUrl
     * @param guid guid
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareModifyElementRemoveLink(Individual mresI, String elUrl, String guid) throws NdlException {
        if (null == mresI) {
            throw new IllegalArgumentException("Individual mresI cannot be null");
        }

        // retrieve or create an individual
        Individual el = blank.getIndividual(elUrl);
        if (el == null) {
            OntClass cls = ref.getOntClass(ref.getNsPrefixUri("topology") + "NetworkConnection");
            el = blank.createIndividual(elUrl, cls);
        }

        if (guid != null)
            addGuid(el, guid);

        return doModifyElement(mresI, el, "removeElement");
    }

    /**
     * Declare an element of a modify reservation that deletes a single element individual
     * 
     * @param mresI mresI
     * @param el el 
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareModifyElementRemoveElement(Individual mresI, Individual el) throws NdlException {
        // inputs are checked in doModifyElement()
        return doModifyElement(mresI, el, "removeElement");
    }

    /**
     *
     * @param mresI mresI
     * @param el el
     * @param modifyAction modifyAction
     * @throws NdlException in case of error
     * @return Individual    
     */
    protected Individual doModifyElement(Individual mresI, Individual el, String modifyAction) throws NdlException {
        if (null == mresI) {
            throw new IllegalArgumentException("Individual mresI cannot be null");
        }
        if (null == el) {
            throw new IllegalArgumentException("Individual el cannot be null");
        }

        Individual melI;
        melI = addIndividual(requestId + "#modifyElement/" + UUID.randomUUID().toString(), "modify-schema",
                "ModifyElement");

        addProperty(mresI, "collections", "element", melI.getURI());
        addProperty(melI, "modify-schema", "modifySubject", el.getURI());
        addProperty(melI, "modify-schema", modifyAction, el.getURI());

        return melI;
    }

    /**
     * Declare an element of a modify reservation that adds a single element (node or link)
     * 
     * @param mresI mresI
     * @param el el
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareModifyElementAddElement(Individual mresI, Individual el) throws NdlException {
        // inputs are checked in doModifyElement()
        return doModifyElement(mresI, el, "addElement");
    }

    /**
     * Declare an element to modify an existing node (add interface)
     * 
     * @param mresI mresI
     * @param node node
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareModifyElementModifyNode(Individual mresI, Individual node) throws NdlException {
        // inputs are checked in doModifyElement()
        return doModifyElement(mresI, node, "modifyElement");
    }

    /**
     * Declare a slice with a name
     * 
     * @param sname sname
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareSlice(String sname) throws NdlException {
        Individual s = addUniqueIndividual(requestId + "#" + massageName(sname), "geni", "Slice");

        addTypedProperty(s, "topology", "hasName", sname, XSDDatatype.XSDstring);

        return s;
    }

    /**
     * Declare an openflow slice
     * 
     * @param sname sname
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareOfSlice(String sname) throws NdlException {
        Individual s = addUniqueIndividual(requestId + "#" + massageName(sname), "openflow", "OFSlice");

        addTypedProperty(s, "topology", "hasName", sname, XSDDatatype.XSDstring);

        return s;
    }

    /**
     * Declare an openflow controller with this URL
     * 
     * @param name name
     * @param url url
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareOfController(String name, String url) throws NdlException {
        Individual c = addUniqueIndividual(requestId + "#" + massageName(name), "openflow", "OFController");

        addTypedProperty(c, "topology", "hasURL", url, XSDDatatype.XSDanyURI);

        return c;
    }

    /**
     * Declare a TCP proxy
     * 
     * @param suffix suffix
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareTCPProxy(String suffix) throws NdlException {
        Individual prx = addUniqueIndividual(requestId + "#" + massageName(suffix), "domain", "TCPProxy");
        return prx;
    }

    /**
     * Add a proxy to (most likely node)
     * 
     * @param proxy proxy
     * @param resI resI
     * @throws NdlException in case of error
     */
    public void addProxyToIndividual(Individual proxy, Individual resI) throws NdlException {
        if (null == proxy) {
            throw new IllegalArgumentException("Individual proxy cannot be null");
        }
        if (null == resI) {
            throw new IllegalArgumentException("Individual resI cannot be null");
        }

        addProperty(resI, "domain", "proxy", proxy.getURI());
    }

    /**
     * add a resource to the reservation
     * 
     * @param resI resI
     * @param toAdd toAdd
     * @throws NdlException in case of error
     */
    public void addResourceToReservation(Individual resI, Individual toAdd) throws NdlException {
        if (null == resI) {
            throw new IllegalArgumentException("Individual resI cannot be null");
        }
        if (null == toAdd) {
            throw new IllegalArgumentException("Individual toAdd cannot be null");
        }

        addProperty(resI, "collections", "element", toAdd.getURI());
    }

    /**
     * Add a sub-slice to reservation
     * 
     * @param resI resI
     * @param sl sl
     * @throws NdlException in case of error
     */
    public void addSliceToReservation(Individual resI, Individual sl) throws NdlException {
        if (null == resI) {
            throw new IllegalArgumentException("Individual resI cannot be null");
        }
        if (null == sl) {
            throw new IllegalArgumentException("Individual sl cannot be null");
        }

        addProperty(resI, "geni", "slice", sl.getURI());
    }

    /**
     * Add DiskImage to reservation or node
     * 
     * @param resI resI
     * @param diskImage diskImage
     * @throws NdlException in case of error
     */
    public void addDiskImageToIndividual(Individual diskImage, Individual resI) throws NdlException {
        if (null == diskImage) {
            throw new IllegalArgumentException("Individual diskImage cannot be null");
        }
        if (null == resI) {
            throw new IllegalArgumentException("Individual resI cannot be null");
        }

        addProperty(resI, "compute", "diskImage", diskImage.getURI());
    }

    /**
     * Add dependency to an individual (both should be NetworkElement subclasses)
     * 
     * @param dependency dependency
     * @param ind ind
     * @throws NdlException in case of error
     */
    public void addDependOnToIndividual(Individual dependency, Individual ind) throws NdlException {
        if (null == dependency) {
            throw new IllegalArgumentException("Individual dependency cannot be null");
        }
        if (null == ind) {
            throw new IllegalArgumentException("Individual ind cannot be null");
        }

        addProperty(ind, "request-schema", "dependOn", dependency.getURI());
    }

    /**
     * Add resource type to e.g. server from topology.owl
     * 
     * @param resType resType
     * @param ind ind
     * @throws NdlException in case of error
     */
    // public void addResourceTypeToServer(Individual ser, String topoRes) throws NdlException {
    // addProperty(ser, "topology", "hasResourceType",
    // ref.getNsPrefixUri("topology") + topoRes);
    // }

    /**
     * Add a count of specific type of units to individual
     */
    // public void addUnitCountToServer(Individual ser, String unitDomain, String unitType, Integer unitCount) throws
    // NdlException {
    // addTypedProperty(ser, unitDomain, unitType, unitCount.toString(),
    // TypeMapper.getInstance().getTypeByName(XML_SCHEMA_INTEGER));
    // }

    /**
     * Add a term to a reservation
     * 
     * @param resI resI
     * @param termI Term
     * @throws NdlException in case of error
     */
    public void addTermToReservation(Individual termI, Individual resI) throws NdlException {
        if (null == termI) {
            throw new IllegalArgumentException("Individual termI cannot be null");
        }
        if (null == resI) {
            throw new IllegalArgumentException("Individual resI cannot be null");
        }

        addProperty(resI, "request-schema", "hasTerm", termI.getURI());
    }

    /**
     * try to get individual with this name from the request
     * 
     * @param name name
     * @return Individual    
     */
    public Individual getRequestIndividual(String name) {
        return blank.getIndividual(NdlCommons.ORCA_NS + requestId + "#" + massageName(name));
    }

    /**
     * get an individual that is not part of request namespace
     * 
     * @param name name
     * @return Individual    
     */
    public Individual getNonrequestIndividual(String name) {
        return blank.getIndividual(NdlCommons.ORCA_NS + name);
    }

    /**
     * declare a domain site (appends /Domain to name as per convention; declares it in the general
     * http://geni-orca.renci.org/owl prefix, not request prefix)
     * 
     * @param name name 
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareDomain(String name) throws NdlException {
        return addIndividual(name + "/Domain", "topology", "NetworkDomain");
    }

    /**
     * Make individual openflow capable (with version 1.0, 1.1 or 1.2)
     * 
     * @param res res
     * @param version version
     * @throws NdlException in case of error
     */
    public void addOpenFlowCapable(Individual res, String version) throws NdlException {
        if (null == res) {
            throw new IllegalArgumentException("Individual res cannot be null");
        }

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
     * 
     * @param dom dom
     * @param node node
     * @throws NdlException in case of error
     */
    public void addNodeToDomain(Individual dom, Individual node) throws NdlException {
        if (null == dom) {
            throw new IllegalArgumentException("Individual dom cannot be null");
        }
        if (null == node) {
            throw new IllegalArgumentException("Individual node cannot be null");
        }

        addProperty(node, "request-schema", "inDomain", dom.getURI());
    }

    /**
     * Add 'inDomain' property to individual
     * 
     * @param dom dom
     * @param ind ind
     * @throws NdlException in case of error
     */
    public void addDomainToIndividual(Individual dom, Individual ind) throws NdlException {
        if (null == dom) {
            throw new IllegalArgumentException("Individual dom cannot be null");
        }
        if (null == ind) {
            throw new IllegalArgumentException("Individual ind cannot be null");
        }

        addProperty(ind, "request-schema", "inDomain", dom.getURI());
    }

    /**
     * Add node type from particular namespace to node
     * 
     * @param ns ns
     * @param tp tp
     * @param node node
     * @throws NdlException in case of error
     */
    public void addNodeTypeToCE(String ns, String tp, Individual node) throws NdlException {
        if (null == node) {
            throw new IllegalArgumentException("Individual node cannot be null");
        }

        addProperty(node, "compute", "specificCE", ref.getNsPrefixUri(ns) + tp);
    }

    /**
     * Remove specific node type from node individual
     * 
     * @param ns ns
     * @param tp tp
     * @param node node
     * @throws NdlException in case of error
     */
    public void removeNodeTypeFromCE(String ns, String tp, Individual node) throws NdlException {
        if (null == node) {
            throw new IllegalArgumentException("Individual node cannot be null");
        }

        Property pr = ref.getProperty(ref.getNsPrefixUri("compute") + "specificCE");
        if (null == pr)
            throw new NdlException("Unable to find property " + "specificCE");
        node.removeProperty(pr, blank.getResource(ref.getNsPrefixUri(ns) + tp));
    }

    /**
     * Removes ALL node types from particular node individual
     * 
     * @param node node
     * @throws NdlException in case of error
     */
    public void removeNodeTypeFromCE(Individual node) throws NdlException {
        if (null == node) {
            throw new IllegalArgumentException("Individual node cannot be null");
        }

        Property pr = ref.getProperty(ref.getNsPrefixUri("compute") + "specificCE");
        if (null == pr)
            throw new NdlException("Unable to find property " + "specificCE");
        node.removeAll(pr);
    }

    /**
     * Add a post boot script string to a node
     * 
     * @param pbscript pbscript
     * @param node node
     * @throws NdlException in case of error
     */
    public void addPostBootScriptToCE(String pbscript, Individual node) throws NdlException {
        if (null == node) {
            throw new IllegalArgumentException("Individual node cannot be null");
        }

        addTypedProperty(node, "request-schema", "postBootScript", pbscript, XSDDatatype.XSDstring);
    }

    /**
     * Add a port to be proxied to a proxy (no information how it will be proxied)
     * 
     * @param port port
     * @param prx proxy
     * @throws NdlException in case of error
     */
    public void addPortToProxy(String port, Individual prx) throws NdlException {
        if (null == prx) {
            throw new IllegalArgumentException("Individual prx cannot be null");
        }

        addTypedProperty(prx, "topology", "proxiedPort", port, XSDDatatype.XSDunsignedShort);
    }

    /**
     * Add a port with proxy information about it (new port and new IP address)
     * 
     * @param port port
     * @param newPort newport
     * @param newIp newip
     * @param prx prx
     * @throws NdlException in case of error
     */
    public void addPortToProxy(String port, String newPort, String newIp, Individual prx) throws NdlException {
        if (null == prx) {
            throw new IllegalArgumentException("Individual prx cannot be null");
        }

        addTypedProperty(prx, "topology", "proxiedPort", port, XSDDatatype.XSDunsignedShort);
        addTypedProperty(prx, "topology", "managementIP", newIp, XSDDatatype.XSDstring);
        addTypedProperty(prx, "topology", "managementPort", newPort, XSDDatatype.XSDstring);
    }

    /**
     * Add number of compute elements to cluster 
     * 
     * @param ns ns
     * @param cluster cluster
     * @throws NdlException in case of error
     */
    public void addNumCEsToCluster(Integer ns, Individual cluster) throws NdlException {
        if (null == ns) {
            throw new IllegalArgumentException("Individual ns cannot be null");
        }
        if (null == cluster) {
            throw new IllegalArgumentException("Individual cluster cannot be null");
        }

        addTypedProperty(cluster, "layer", "numCE", ns.toString(), XSDDatatype.XSDinteger);
    }

    /**
     * Add a VM domain resource property (for Euca clusters)
     * 
     * @param cluster cluster
     * @throws NdlException in case of error
     */
    public void addVMDomainProperty(Individual cluster) throws NdlException {
        if (null == cluster) {
            throw new IllegalArgumentException("Individual cluster cannot be null");
        }

        addProperty(cluster, "domain", "hasResourceType", NdlCommons.ORCA_NS + "compute.owl#VM");
    }

    public void addBareMetalDomainProperty(Individual cluster) throws NdlException {
        if (null == cluster) {
            throw new IllegalArgumentException("Individual cluster cannot be null");
        }

        addProperty(cluster, "domain", "hasResourceType", NdlCommons.ORCA_NS + "compute.owl#BareMetalCE");
    }

    public void addFourtyGBareMetalDomainProperty(Individual cluster) throws NdlException {
        if (null == cluster) {
            throw new IllegalArgumentException("Individual cluster cannot be null");
        }

        addProperty(cluster, "domain", "hasResourceType", NdlCommons.ORCA_NS + "compute.owl#FourtyGBareMetalCE");
    }

    /**
     * Add OpenFlow-relevant properties to a slice individual
     * 
     * @param userEmail useremail
     * @param slicePass slicepass
     * @param userCtrl userctl
     * @param slice slice
     * @throws NdlException in case of error
     */
    public void addOfPropertiesToSlice(String userEmail, String slicePass, String userCtrl, Individual slice)
            throws NdlException {
        if (null == slice) {
            throw new IllegalArgumentException("Individual slice cannot be null");
        }

        addTypedProperty(slice, "topology", "hasEmail", userEmail, XSDDatatype.XSDstring);
        addTypedProperty(slice, "openflow", "hasSlicePassword", slicePass, XSDDatatype.XSDstring);
        Individual ctrl = declareOfController(slice.getLocalName() + "-of-ctrl", userCtrl);
        addProperty(slice, "openflow", "controller", ctrl.getURI());
    }

    public void addUserDNProperty(String dn, Individual i) throws NdlException {
        if (null == i) {
            throw new IllegalArgumentException("Individual i cannot be null");
        }

        addTypedProperty(i, "topology", "userDN", dn, XSDDatatype.XSDinteger);
    }

    /*
     * Color properties
     */

    /**
     * Declare a new color individual
     * 
     * @param label
     *            - label
     * @param keys
     *            - map of key value pairs
     * @param blob
     *            - text blob
     * @param isXML
     *            - is it XML?
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual declareColor(String label, Map<String, String> keys, String blob, boolean isXML)
            throws NdlException {
        Individual in = addAnonIndividual("app-color", "Color");
        addSimpleProperty(in, "app-color", "hasColorLabel", label);
        addKeysOnColor(in, keys);
        addBlobOnColor__(in, blob, isXML, null);
        return in;
    }

    /**
     * add keys to color
     * 
     * @param color color
     * @param keys keys
     * @throws NdlException in case of error
     */
    public void addKeysOnColor(Individual color, Map<String, String> keys) throws NdlException {
        if (keys == null || color == null)
            return;
        for (Entry<String, String> e : keys.entrySet()) {
            Individual ca = addAnonIndividual("app-color", "ColorAttribute");
            addSimpleProperty(ca, "app-color", "hasColorKey", e.getKey());
            addSimpleProperty(ca, "app-color", "hasColorValue", e.getValue());
            addProperty(color, "app-color", "hasColorAttribute", ca);
        }
    }

    /**
     * Add text blob to color
     * 
     * @param color
     *            - color individual
     * @param blob
     *            - text blob
     * @param isXML
     *            - is it XML?
     * @return Individual
     * @throws NdlException in case of error
     */
    public Individual addBlobOnColor(Individual color, String blob, boolean isXML) throws NdlException {
        if (color == null)
            return color;
        if (blob == null)
            return color;
        if (isXML) {
            Literal blobLit = addTypedLiteral(blob, "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral");
            assert (blobLit.isWellFormedXML());
            addProperty(color, "app-color", "hasColorXMLBlob", blobLit);
        } else
            addSimpleProperty(color, "app-color", "hasColorBlob", blob);
        return color;
    }

    /**
     * Use compression instead of xml literal to get around Jena bugs for XML literals
     * 
     * @param color color
     * @param blob blob
     * @param isXML ixXML
     * @param xmlns
     *            - namespace for inner xml
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual addBlobOnColor__(Individual color, String blob, boolean isXML, String xmlns) throws NdlException {
        if (color == null)
            return color;
        if (blob == null)
            return color;
        if (isXML) {
            addSimpleProperty(color, "app-color", "hasColorXMLCompressedBlob", compressEncode(blob));
        } else
            addSimpleProperty(color, "app-color", "hasColorBlob", blob);
        return color;
    }

    /**
     * Encode a color dependency between two network elements
     * 
     * @param from
     *            - network element
     * @param to
     *            - network element
     * @param color
     *            - color
     * @throws NdlException in case of error
     */
    public void encodeColorDependency(Individual from, Individual to, Individual color) throws NdlException {
        if (null == from) {
            throw new IllegalArgumentException("Individual from cannot be null");
        }
        if (null == to) {
            throw new IllegalArgumentException("Individual to cannot be null");
        }
        if (null == color) {
            throw new IllegalArgumentException("Individual color cannot be null");
        }

        addProperty(from, "app-color", "toColorDependency", color);
        addProperty(to, "app-color", "fromColorDependency", color);
    }

    public Individual addColorToIndividual(Individual ne, Individual color) throws NdlException {
        if (null == ne) {
            throw new IllegalArgumentException("Individual ne cannot be null");
        }
        if (null == color) {
            throw new IllegalArgumentException("Individual color cannot be null");
        }

        addProperty(ne, "app-color", "hasColor", color);
        return ne;
    }

    /**
     * Add ethernet adaptation to another interface
     * 
     * @param intf
     *            - interface
     * @param adaptTo
     *            - interface to be adapted
     * @throws NdlException in case of error
     * @return Individual    
     */
    public Individual addEthernetAdaptation(Individual intf, Individual adaptTo) throws NdlException {
        if (null == intf) {
            throw new IllegalArgumentException("Individual intf cannot be null");
        }
        if (null == adaptTo) {
            throw new IllegalArgumentException("Individual adaptTo cannot be null");
        }

        addProperty(intf, "ethernet", "Tagged-Ethernet", adaptTo);
        return intf;
    }

    /**
     * Add addGuid property to this individual
     * 
     * @param in in
     * @param guid guid
     * @throws NdlException in case of error
     */
    public void addGuid(Individual in, String guid) throws NdlException {
        if (null == in) {
            throw new IllegalArgumentException("Individual in cannot be null");
        }

        addSimpleProperty(in, "topology", "hasGUID", guid);
    }

    /**
     * produce N3 output of the model
     * @return String
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
     * 
     * @return String
     */
    public String toXMLString() {
        StringWriter sw = new StringWriter();
        blank.write(sw);
        return sw.toString();
    }

    /**
     * Produce Graphviz output for visualization
     * 
     * @return String
     */
    public String getGVOutput() {
        return OntProcessor.substrateDotString(blank);
    }

    static String compressEncode(String inputString) {
        return CompressEncode.compressEncode(inputString);
        // changed after we had to create dependency on orca.core.util
        // which has these /ib 04/15/14
        // // gzip
        // byte[] inputBytes;
        // ByteArrayOutputStream baos = null;
        // GZIPOutputStream gzos = null;
        //
        // if (inputString == null) return null;
        //
        // try {
        // baos = new ByteArrayOutputStream();
        // gzos = new GZIPOutputStream(baos);
        //
        // try {
        // inputBytes = inputString.getBytes("UTF-8");
        // }
        // catch (UnsupportedEncodingException uee) {
        // inputBytes = inputString.getBytes();
        // }
        //
        // gzos.write(inputBytes);
        // gzos.close();
        // }
        // catch (IOException ioe) {
        // ioe.printStackTrace();
        // return null;
        // }
        // finally {
        // try { gzos.close(); } catch (Exception e) {}
        // try { baos.close(); } catch (Exception e) {}
        // }
        //
        // // base64-encode
        // return Base64.encodeBase64String(baos.toByteArray());
    }

    static String decodeDecompress(String inputString) {
        try {
            return CompressEncode.decodeDecompress(inputString);
        } catch (Exception e) {
            return null;
        }
        // changed after we had to create dependency on orca.core.util
        // which has these /ib 04/15/14
        // // base64-decode and gunzip
        // ByteArrayInputStream bais = null;
        // GZIPInputStream gzis = null;
        // ByteArrayOutputStream baos = null;
        //
        // if (inputString == null) return null;
        //
        // try {
        // bais = new ByteArrayInputStream(Base64.decodeBase64(inputString));
        // gzis = new GZIPInputStream(bais);
        // baos = new ByteArrayOutputStream();
        // byte[] buf = new byte[2048];
        // int len = 0;
        //
        // while ((len = gzis.read(buf)) >= 0) {
        // baos.write(buf, 0, len);
        // }
        // }
        // catch (IOException ioe) {
        // ioe.printStackTrace();
        // return null;
        // }
        // finally {
        // try { baos.close(); } catch (Exception e) {}
        // try { gzis.close(); } catch (Exception e) {}
        // try { bais.close(); } catch (Exception e) {}
        // }
        //
        // try {
        // return new String(baos.toByteArray(), "UTF-8");
        // }
        // catch (UnsupportedEncodingException uue) {
        // return new String(baos.toByteArray());
        // }
    }

    public static void main(String[] argv) {
        // Date now = new Date();
        // Calendar calLocal = Calendar.getInstance();
        // calLocal.setTime(now);
        // Calendar calUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // calUTC.setTime(now);
        // System.out.println("Local " + DatatypeConverter.printDateTime(calLocal));
        // System.out.println("UTC " + DatatypeConverter.printDateTime(calUTC));
        //// Logger myLog = Logger.getLogger("NdlGenerator");
        //// NdlCommons.setGlobalJenaRedirections();
        //// NdlGenerator ngen = new NdlGenerator(null, myLog, true);
        ////
        //// try {
        //// Individual i = ngen.declareModifyReservation("my-modify");
        //// ngen.declareModifyElementNGIncreaseBy(i, "http://some.resource/nodeGroup-1", 5);
        //// ngen.declareModifyElementNGDeleteNode(i, "http://some.resource/nodeGroup-1",
        // "http://some.resource/nodeGroup-1/0");
        //// } catch (NdlException e) {
        //// System.err.println("NdlException " + e);
        //// }
        ////
        //// System.out.println("Modify \n" + ngen.toXMLString());
        //// myLog.info("Done");

        NdlGenerator ngen = new NdlGenerator("some-unique-guid", Logger.getLogger("my logger"));
        // String blob = "<gemininode type=\"mp_node\" > \n" +
        // "<geminiservices>\n" +
        // "<geminiactive install=\"yes\" enable=\"yes\"/>\n" +
        // "<geminipassive install=\"yes\" enable=\"yes\"/>\n" +
        // "</geminiservices>\n" +
        // "</gemininode>";
        String blob = "<gemininode> \n" + "<geminiservices>\n"
                + "<geminiactive install=\"yes\" enable=\"yes\">blah1</geminiactive>\n"
                + "<geminipassive>blah2</geminipassive>\n" + "</geminiservices>\n" + "</gemininode>";

        try {
            ngen.declareColor("test color", null, blob, true);

            // nodes
            Individual n = ngen.declareComputeElement("mynode");
            Individual n1 = ngen.declareComputeElement("mynode1");
            ngen.addNodeTypeToCE("exogeni", "XOMedium", n1);
            ngen.addNodeTypeToCE("exogeni", "XOMedium", n);
            // ngen.removeNodeTypeFromCE(n);
            ngen.removeNodeTypeFromCE("exogeni", "XOMedium", n);

            // ip
            Individual intf = ngen.declareInterface("if1");
            ngen.addInterfaceToIndividual(intf, n);
            // Individual ipInd = ngen.addIPToIndividual("1.2.3.4", intf);
            // ngen.addNetmaskToIP(ipInd, "255.255.255.0");

            // ipInd = ngen.addIPToIndividual("4.3.2.1", intf);
            // ngen.addNetmaskToIP(ipInd, "255.255.0.0");

            ngen.setInterfaceIP("5.5.5.5", intf);
            ngen.setInterfaceNetmask("255.0.0.0", intf);

            ngen.setInterfaceIP("6.6.6.6", intf);
            ngen.setInterfaceNetmask("255.255.255.0", intf);

        } catch (NdlException ee) {
            System.out.println("Exception: " + ee);
        }

        System.out.println(ngen.toXMLString());
    }

}
