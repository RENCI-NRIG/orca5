package orca.ndl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import orca.ndl.LayerConstant.Layer;
import orca.ndl.elements.ComputeElement;
import orca.ndl.elements.Label;
import orca.ndl.elements.LabelSet;
import orca.ndl.elements.NetworkElement;
import orca.ndl.util.ModelFolders;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.sparql.core.ResultBinding;
import com.hp.hpl.jena.util.LocatorURL;
import com.hp.hpl.jena.util.TypedStream;

/** 
 * Common declarations for use in NDL 
 * @author ibaldin
 *
 */
public class NdlCommons {
	public static final String ORCA_NDL_SCHEMA = "orca/ndl/schema/";
	public static final String ORCA_NDL_SUBSTRATE = "orca/ndl/substrate/";

	private static final String NDL_LOGGER = "ndl.logger";

	public static final String ORCA_NS = "http://geni-orca.renci.org/owl/";
	public static final String W3_NS = "http://www.w3.org/";
	
	public static final long Default_Bandwidth = 10000000;
	public static final int max_vlan_tag = 4095;
	
	public static final String multicast = "Multicast";
	public static final String unicast = "Unicast";
	public static final String stitching_domain_str = "Stitching/Domain";
	
	public static String ontPrefix = 
       	"PREFIX gleen:<java:edu.washington.sig.gleen.>"+
       	"PREFIX owl:<http://www.w3.org/2002/07/owl#>" +
    	"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+ 
    	"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>"+
    	"PREFIX geo:<http://www.w3.org/2003/01/geo/wgs84_pos#>"+
    	"PREFIX fn:<http://www.w3.org/2005/xpath-functions#>"+ 
    	"PREFIX wdm:<http://geni-orca.renci.org/owl/dtn.owl#>"+
    	"PREFIX ethernet:<http://geni-orca.renci.org/owl/ethernet.owl#>"+
    	"PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>"+ 
    	"PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>"+
    	"PREFIX time:<http://www.w3.org/2006/time#>"+
    	"PREFIX ndl:<http://geni-orca.renci.org/owl/topology.owl#>"+
    	"PREFIX location:<http://geni-orca.renci.org/owl/location.owl#>"+
    	"PREFIX layer:<http://geni-orca.renci.org/owl/layer.owl#>"+
    	"PREFIX topology:<http://geni-orca.renci.org/owl/topology.owl#>"+
    	"PREFIX collections:<http://geni-orca.renci.org/owl/collections.owl#>"+
    	"PREFIX request:<http://geni-orca.renci.org/owl/request.owl#>"+
    	"PREFIX dtn:<http://geni-orca.renci.org/owl/dtn.owl#>"+
    	"PREFIX ethernet:<http://geni-orca.renci.org/owl/ethernet.owl#>"+
    	"PREFIX compute:<http://geni-orca.renci.org/owl/compute.owl#>"+
    	"PREFIX storage:<http://geni-orca.renci.org/owl/storage.owl#>"+
    	"PREFIX exogeni:<http://geni-orca.renci.org/owl/exogeni.owl#>"+
    	"PREFIX orca:<http://geni-orca.renci.org/owl/orca.rdf#>"+
    	"PREFIX domain:<http://geni-orca.renci.org/owl/domain.owl#>"+
    	"PREFIX ip4:<http://geni-orca.renci.org/owl/ip4.owl#>"+
    	"PREFIX geni:<http://geni-orca.renci.org/owl/geni.owl#>" +
    	"PREFIX modify:<http://geni-orca.renci.org/owl/modify.owl#>" +
    	"PREFIX manifest:<http://geni-orca.renci.org/owl/manifest.owl#>" +
    	"PREFIX app-color:<http://geni-orca.renci.org/owl/app-color.owl#>";
	
	public static final Property collectionElementProperty,  collectionItemProperty,collectionSizeProperty, RDF_TYPE, RDFS_Label, RDFS_SeeAlso, OWL_sameAs,
	numCEProperty, requestGroupNameProperty, hasEmailProperty, hasSlicePasswordProperty, geniSliceProperty, hasSliceGeniState,
	inDomainProperty, diskImageProperty, topologySplittableProperty, domainHasServiceProperty, domainProxyProperty,
	hasURLProperty, hasGUIDProperty,hasURNProperty, specificCEProperty, virtualizeProperty, numCPUCore, domainHasResourceTypeProperty, topologyHasInterfaceProperty,topologyInterfaceOfProperty,
	layerBandwidthProperty, layerLatencyProperty, layerLabelIdProperty, layerLabelIsPrimary,layerUsedLabels,ip4LocalIPAddressProperty, ip4NetmaskProperty,ipMacAddressProperty,
	requestDependOnProperty, requestPostBootScriptProperty,  domainHasAccessMethod, domainHasTopology, domainIsAllocatable,domainHasAggregateManager,domainHasController,
	requestHasReservationState, requestMessage, hasDNProperty, 
	hasInstanceIDProperty,workerNodeIDProperty, 
	hasBeginningObjectProperty,hasEndObjectProperty,hasDurationDescriptionObjectProperty,inXSDDateTime,daysProperty,hoursProperty,minutesProperty,secondsProperty,
	locationLocatedAtProperty,locationLatProperty,locationLongProperty;

	public static final Property hasInputInterface, connectedTo, linkTo,switchedTo,hasSwitchMatrix,
	hasRequestGroupURL,inRequestNetworkConnection,
    hasOutputInterface, adaptationProperty, adaptationPropertyOf, taggedEthernetProperty, carryReservation, atLayer,hasCastType,
    switchingCapability, swappingCapability, tunnelingCapability, connectionDirection, vlan, ocgLine,layerSwapLabelProperty,
    portOccupied, inConnection, visited, numHop, openflowCapableProperty, modifySubjectProperty, modifyAddElementProperty, modifyElementProperty, isModifyProperty,
    modifyRemoveElementProperty, modifyIncreaseByProperty,manifestHasParent,manifestHasChild;

	
	public static final Property topologyHasName,topologyHasURL, hostName_p,topologyManagementIP, topologyManagementPort, topologyHasLogin, isLabelProducer,
	availableLabelSet, lowerBound, upperBound, resourceTypeRank, hasUnitServer, numResource, hostInterfaceName, layerLabel,
	memoryCapacity, cpuCapacity, topologyProxiedPort, openflowControllerProperty, layerHasBitRate, quantumNetUUIDProperty,
	storageCapacity,hasFSParam,hasFSType,hasMntPoint,doFormat;
	
	public static final Property hasColorAttribute, hasColorBlob, hasColorKey, hasColorLabel, hasColorValue, hasColorXMLBlob, hasColorXMLCompressedBlob;
	
	public static final Resource networkStorageClass, computeElementClass, serverCloudClass, topologyNetworkConnectionClass, topologyBroadcastConnectionClass, 
	vmResourceTypeClass,bmResourceTypeClass,fourtygbmResourceTypeClass,lunResourceTypeClass,ethernetNetworkElementClass, multicastOntClass,
	topologyCrossConnectClass, topologyLinkConnectionClass,deviceOntClass, switchingMatrixOntClass,interfaceOntClass,vlanResourceTypeClass,
	networkDomainOntClass, networkServiceClass, domainSSHServiceClass, reservationOntClass, manifestOntClass, domainAggregateManagerClass,domainControllerClass,
	requestReservationStateClass, requestActiveState, requestActiveTicketedState, topologyLinkClass, 
	requestCloseWaitState, requestClosedState, requestFailedState, requestNascentState, requestNoState, requestTicketedState,
	labelRangeOntClass, labelOntClass, labelSetOntClass, IPAddressOntClass, proxyClass, tcpProxyClass, sliceClass, ofSliceClass,sliceGeniStateClass,
	instantOntClass, geniSliceStateConfiguring, geniSliceStateFailed, geniSliceStateReady, geniSliceStateUnknown;
	
	public static final Resource openflowV1_0Ind, openflowV1_1Ind, openflowV1_2Ind, openflowV1_3Ind, bitRate1G, bitRate10G, stitchingDomain, diskImageClass;
	
	static {	
		hasURLProperty = new PropertyImpl(ORCA_NS + "topology.owl#hasURL");
		hasURNProperty = new PropertyImpl(ORCA_NS + "topology.owl#hasURN");
		hasGUIDProperty = new PropertyImpl(ORCA_NS + "topology.owl#hasGUID");
		topologyHasInterfaceProperty = new PropertyImpl(ORCA_NS + "topology.owl#hasInterface");
		topologyInterfaceOfProperty = new PropertyImpl(ORCA_NS + "topology.owl#interfaceOf");
        hasInputInterface = new PropertyImpl(ORCA_NS + "topology.owl#hasInputInterface");
        hasOutputInterface = new PropertyImpl(ORCA_NS + "topology.owl#hasOutputInterface");
        connectedTo = new PropertyImpl(ORCA_NS + "topology.owl#connectedTo");
        linkTo = new PropertyImpl(ORCA_NS + "topology.owl#linkTo");
        switchedTo = new PropertyImpl(ORCA_NS + "topology.owl#switchedTo");
        hasSwitchMatrix = new PropertyImpl(ORCA_NS + "topology.owl#hasSwitchMatrix");
		topologySplittableProperty = new PropertyImpl(ORCA_NS + "topology.owl#splittable");
		topologyManagementIP = new PropertyImpl(ORCA_NS + "topology.owl#managementIP");
		topologyProxiedPort = new PropertyImpl(ORCA_NS + "topology.owl#proxiedPort");
		topologyManagementPort = new PropertyImpl(ORCA_NS + "topology.owl#managementPort");
		topologyHasLogin = new PropertyImpl(ORCA_NS + "topology.owl#hasLogin");
		topologyHasName = new PropertyImpl(ORCA_NS + "topology.owl#hasName");
		topologyHasURL = new PropertyImpl(ORCA_NS + "topology.owl#hasURL");
        hostName_p = new PropertyImpl(ORCA_NS + "topology.owl#hostName");
        hostInterfaceName  = new PropertyImpl(ORCA_NS + "topology.owl#hostInterfaceName");
        
		layerSwapLabelProperty =  new PropertyImpl(ORCA_NS + "layer.owl#" + "swapLabel");
        adaptationPropertyOf = new PropertyImpl(ORCA_NS + "layer.owl#adaptationPropertyOf");
        adaptationProperty = new PropertyImpl(ORCA_NS + "layer.owl#adaptationProperty");
        taggedEthernetProperty = new PropertyImpl(ORCA_NS + "ethernet.owl#Tagged-Ethernet");
        availableLabelSet = new PropertyImpl(ORCA_NS + "layer.owl#availableLabelSet");
        lowerBound = new PropertyImpl(ORCA_NS + "layer.owl#lowerBound");
        upperBound = new PropertyImpl(ORCA_NS + "layer.owl#upperBound");
        switchingCapability = new PropertyImpl(ORCA_NS + "layer.owl#switchingCapability");
        swappingCapability = new PropertyImpl(ORCA_NS + "layer.owl#swappingCapability");
        tunnelingCapability = new PropertyImpl(ORCA_NS + "layer.owl#tunnelingCapability");
        connectionDirection = new PropertyImpl(ORCA_NS + "topology.owl#connectionDirection");
        atLayer=new PropertyImpl(ORCA_NS + "layer.owl#atLayer");
        hasCastType=new PropertyImpl(ORCA_NS + "layer.owl#hasCastType");
		layerBandwidthProperty = new PropertyImpl(ORCA_NS + "layer.owl#bandwidth");
		layerLatencyProperty = new PropertyImpl(ORCA_NS + "layer.owl#latency");
		layerLabel = new PropertyImpl(ORCA_NS + "layer.owl#label");
		layerLabelIdProperty = new PropertyImpl(ORCA_NS + "layer.owl#label_ID");
		layerLabelIsPrimary = new PropertyImpl(ORCA_NS + "layer.owl#isPrimary");
		layerUsedLabels = new PropertyImpl(ORCA_NS + "layer.owl#usedLabels");
		numCEProperty = new PropertyImpl(ORCA_NS + "layer.owl#numCE");
        isLabelProducer = new PropertyImpl(ORCA_NS + "layer.owl#isLabelProducer");
        modifySubjectProperty = new PropertyImpl(ORCA_NS + "modify.owl#modifySubject");
        modifyAddElementProperty = new PropertyImpl(ORCA_NS + "modify.owl#addElement");
        modifyElementProperty = new PropertyImpl(ORCA_NS + "modify.owl#modifyElement");
        isModifyProperty = new PropertyImpl(ORCA_NS + "modify.owl#isModify");
        modifyRemoveElementProperty = new PropertyImpl(ORCA_NS + "modify.owl#removeElement");
        modifyIncreaseByProperty = new PropertyImpl(ORCA_NS + "modify.owl#increaseBy");
        manifestHasParent = new PropertyImpl(ORCA_NS + "manifest.owl#hasParent");
        manifestHasChild  = new PropertyImpl(ORCA_NS + "manifest.owl#hasChild");

		// properties
		collectionElementProperty = new PropertyImpl(ORCA_NS + "collections.owl#element");
		collectionItemProperty = new PropertyImpl(ORCA_NS + "collections.owl#item");
		collectionSizeProperty = new PropertyImpl(ORCA_NS + "collections.owl#size");
		
        carryReservation=new PropertyImpl(ORCA_NS+"request.owl#carryReservation");		
		requestHasReservationState = new PropertyImpl(ORCA_NS + "request.owl#hasReservationState");
		requestMessage = new PropertyImpl(ORCA_NS + "request.owl#message");
		requestPostBootScriptProperty = new PropertyImpl(ORCA_NS + "request.owl#postBootScript");
		inDomainProperty = new PropertyImpl(ORCA_NS + "request.owl#inDomain");
		requestGroupNameProperty = new PropertyImpl(ORCA_NS + "request.owl#groupName");
		hasRequestGroupURL = new PropertyImpl(ORCA_NS + "compute.owl#hasRequestGroupURL");
		inRequestNetworkConnection = new PropertyImpl(ORCA_NS + "compute.owl#inRequestNetworkConnection");
        cpuCapacity = new PropertyImpl(ORCA_NS + "compute.owl#cpuCapacity");
        memoryCapacity = new PropertyImpl(ORCA_NS + "compute.owl#memoryCapacity");
		diskImageProperty = new PropertyImpl(ORCA_NS + "compute.owl#diskImage");
		specificCEProperty = new PropertyImpl(ORCA_NS + "compute.owl#specificCE");
		hasUnitServer = new PropertyImpl(ORCA_NS + "compute.owl#hasUnitServer");	
		numResource= new PropertyImpl(ORCA_NS + "layer.owl#numResource");
		numHop=new PropertyImpl(ORCA_NS + "topology.owl#numHop");
		numCPUCore =  new PropertyImpl(ORCA_NS + "compute.owl#numCPUCore");
		virtualizeProperty =  new PropertyImpl(ORCA_NS + "compute.owl#virtualize");
		
		storageCapacity =  new PropertyImpl(ORCA_NS + "storage.owl#storageCapacity");
		hasFSParam =  new PropertyImpl(ORCA_NS + "storage.owl#hasFSParam");
		hasFSType =  new PropertyImpl(ORCA_NS + "storage.owl#hasFSType");
		doFormat =  new PropertyImpl(ORCA_NS + "storage.owl#doFormat");
		hasMntPoint =  new PropertyImpl(ORCA_NS + "storage.owl#hasMntPoint");
		
		ipMacAddressProperty = new PropertyImpl(ORCA_NS + "ip4.owl#macAddress");
		ip4LocalIPAddressProperty = new PropertyImpl(ORCA_NS + "ip4.owl#localIPAddress");
		ip4NetmaskProperty = new PropertyImpl(ORCA_NS + "ip4.owl#netmask");
		requestDependOnProperty = new PropertyImpl(ORCA_NS + "request.owl#dependOn");

        resourceTypeRank=new PropertyImpl(ORCA_NS + "domain.owl#resourceTypeRank");		
		domainHasResourceTypeProperty = new PropertyImpl(ORCA_NS + "domain.owl#hasResourceType");
		domainHasServiceProperty = new PropertyImpl(ORCA_NS + "domain.owl#hasService");
		domainProxyProperty = new PropertyImpl(ORCA_NS + "domain.owl#proxy");
		domainHasAccessMethod = new PropertyImpl(ORCA_NS + "domain.owl#hasAccessMethod");
		domainHasTopology = new PropertyImpl(ORCA_NS + "domain.owl#hasTopology");
		domainIsAllocatable = new PropertyImpl(ORCA_NS + "domain.owl#isAllocatable");
		domainHasAggregateManager = new PropertyImpl(ORCA_NS + "domain.owl#hasAggregateManager");
		domainHasController = new PropertyImpl(ORCA_NS + "domain.owl#hasController");
		
		openflowCapableProperty = new PropertyImpl(ORCA_NS + "openflow.owl#openflowCapable");
		quantumNetUUIDProperty  = new PropertyImpl(ORCA_NS + "openflow.owl#quantumNetUUID");
		
		daysProperty = new PropertyImpl(W3_NS + "2006/time#"+"days");
		hoursProperty = new PropertyImpl(W3_NS + "2006/time#"+"hours");
		minutesProperty = new PropertyImpl(W3_NS + "2006/time#"+"minutes");
		secondsProperty = new PropertyImpl(W3_NS + "2006/time#"+"seconds");
		inXSDDateTime = new PropertyImpl(W3_NS + "2006/time#"+"inXSDDateTime");
		hasBeginningObjectProperty = new PropertyImpl(W3_NS + "2006/time#"+"hasBeginning");
		hasEndObjectProperty = new PropertyImpl(W3_NS + "2006/time#"+"hasEnd");
		hasDurationDescriptionObjectProperty = new PropertyImpl(W3_NS + "2006/time#"+"hasDurationDescription");
		locationLocatedAtProperty=new PropertyImpl(ORCA_NS + "location.owl#locatedAt");
		locationLatProperty = new PropertyImpl(W3_NS+"2003/01/geo/wgs84_pos#lat");
		locationLongProperty = new PropertyImpl(W3_NS+"2003/01/geo/wgs84_pos#long");
		
		RDF_TYPE = new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		RDFS_Label = new PropertyImpl("http://www.w3.org/2000/01/rdf-schema#label");
		RDFS_SeeAlso = new PropertyImpl("http://www.w3.org/2000/01/rdf-schema#", "SeeAlso");
		OWL_sameAs = new PropertyImpl(W3_NS + "2002/07/owl#"+"sameAs");
	
        vlan = new PropertyImpl(ORCA_NS + "ethernet.owl#vlan");
        ocgLine = new PropertyImpl(ORCA_NS + "dtn.owl#ocgLine");
        portOccupied = new PropertyImpl(ORCA_NS + "topology.owl#portOccupied");
        inConnection = new PropertyImpl(ORCA_NS + "topology.owl#inConnection");
        visited = new PropertyImpl(ORCA_NS + "topology.owl#visited");
        
        layerHasBitRate = new PropertyImpl(ORCA_NS + "layer.owl#hasBitRate");
        hasEmailProperty = new PropertyImpl(ORCA_NS + "topology.owl#hasEmail");
        hasDNProperty = new PropertyImpl(ORCA_NS + "topology.owl#hasDN");
        hasSlicePasswordProperty = new PropertyImpl(ORCA_NS + "openflow.owl#hasSlicePassword");
        geniSliceProperty = new PropertyImpl(ORCA_NS + "geni.owl#slice");
        openflowControllerProperty = new PropertyImpl(ORCA_NS + "openflow.owl#controller");
		hasSliceGeniState = new PropertyImpl(ORCA_NS + "geni.owl#hasSliceGeniState");
        hasInstanceIDProperty = new PropertyImpl(ORCA_NS + "ec2.owl#hasInstanceID");
        workerNodeIDProperty = new PropertyImpl(ORCA_NS + "ec2.owl#workerNodeID");
		
        // classes/types
        networkStorageClass = new ResourceImpl(ORCA_NS + "storage.owl#NetworkStorage");
        topologyNetworkConnectionClass = new ResourceImpl(ORCA_NS + "topology.owl#NetworkConnection");
        topologyBroadcastConnectionClass = new ResourceImpl(ORCA_NS + "topology.owl#BroadcastConnection");
		topologyLinkConnectionClass = new ResourceImpl(ORCA_NS + "topology.owl#LinkConnection");
		topologyLinkClass = new ResourceImpl(ORCA_NS + "topology.owl#Link");
		deviceOntClass = new ResourceImpl(ORCA_NS + "topology.owl#Device");
        switchingMatrixOntClass = new ResourceImpl(ORCA_NS + "topology.owl#SwitchingMatrix");
        interfaceOntClass = new ResourceImpl(ORCA_NS + "topology.owl#Interface");
        vlanResourceTypeClass = new ResourceImpl(ORCA_NS + "domain.owl#VLAN");
        ethernetNetworkElementClass = new ResourceImpl(ORCA_NS + "ethernet.owl#EthernetNetworkElement");
		computeElementClass = new ResourceImpl(ORCA_NS + "compute.owl#ComputeElement");
		vmResourceTypeClass = new ResourceImpl(ORCA_NS + "compute.owl#VM");
		bmResourceTypeClass = new ResourceImpl(ORCA_NS + "compute.owl#BareMetalCE");
		fourtygbmResourceTypeClass = new ResourceImpl(ORCA_NS + "compute.owl#FourtyGBareMetalCE");
		lunResourceTypeClass = new ResourceImpl(ORCA_NS + "storage.owl#LUN");
		
		serverCloudClass = new ResourceImpl(ORCA_NS + "compute.owl#ServerCloud");
		topologyCrossConnectClass = new ResourceImpl(ORCA_NS + "topology.owl#CrossConnect");
		domainAggregateManagerClass = new ResourceImpl(ORCA_NS + "domain.owl#AggregateManager");
		domainControllerClass = new ResourceImpl(ORCA_NS + "domain.owl#Controller");
		
		domainSSHServiceClass = new ResourceImpl(ORCA_NS + "domain.owl#SSH");
		requestActiveState = new ResourceImpl(ORCA_NS + "request.owl#Active"); 
		requestActiveTicketedState = new ResourceImpl(ORCA_NS + "request.owl#ActiveTicketed");
		requestCloseWaitState = new ResourceImpl(ORCA_NS + "request.owl#CloseWait");
		requestClosedState = new ResourceImpl(ORCA_NS + "request.owl#Closed");
		requestFailedState = new ResourceImpl(ORCA_NS + "request.owl#Failed");
		requestNascentState = new ResourceImpl(ORCA_NS + "request.owl#Nascent");
		requestNoState = new ResourceImpl(ORCA_NS + "request.owl#No_state");
		requestTicketedState = new ResourceImpl(ORCA_NS + "request.owl#Ticketed");
		requestReservationStateClass = new ResourceImpl(ORCA_NS + "request.owl#ReservationState");
		
		geniSliceStateConfiguring = new ResourceImpl(ORCA_NS + "geni.owl#configuring"); 
		geniSliceStateFailed = new ResourceImpl(ORCA_NS + "geni.owl#failed");
		geniSliceStateReady = new ResourceImpl(ORCA_NS + "geni.owl#ready");
		geniSliceStateUnknown = new ResourceImpl(ORCA_NS + "geni.owl#unknown");
		
        reservationOntClass = new ResourceImpl(ORCA_NS + "request.owl#" + "Reservation");
        manifestOntClass = new ResourceImpl(ORCA_NS + "request.owl#" + "Manifest");
		instantOntClass = new ResourceImpl(W3_NS + "2006/time.owl#" + "Instant");
        
        networkDomainOntClass = new ResourceImpl(ORCA_NS + "topology.owl#NetworkDomain");
		networkServiceClass = new ResourceImpl(ORCA_NS + "domain.owl#NetworkService");
		
		multicastOntClass =  new ResourceImpl(ORCA_NS + "layer.owl#" + "Multicast");
		labelRangeOntClass =  new ResourceImpl(ORCA_NS + "layer.owl#" + "LabelRange");
		labelOntClass = new ResourceImpl(ORCA_NS + "layer.owl#" + "Label");
		labelSetOntClass = new ResourceImpl(ORCA_NS + "layer.owl#" + "LabelSet");
    	IPAddressOntClass = new ResourceImpl(ORCA_NS + "ip4.owl#IPAddress");
    	proxyClass = new ResourceImpl(ORCA_NS + "domain.owl#Proxy");
    	tcpProxyClass = new ResourceImpl(ORCA_NS + "domain.owl#TCPProxy");
    	sliceClass = new ResourceImpl(ORCA_NS + "geni.owl#Slice");
    	ofSliceClass = new ResourceImpl(ORCA_NS + "openflow.owl#OFSlice");
    	sliceGeniStateClass = new ResourceImpl(ORCA_NS + "geni.owl#SliceGeniState");
    	
    	openflowV1_0Ind = new ResourceImpl(ORCA_NS + "openflow.owl#OpenFlow-1.0");
    	openflowV1_1Ind = new ResourceImpl(ORCA_NS + "openflow.owl#OpenFlow-1.1");
    	openflowV1_2Ind = new ResourceImpl(ORCA_NS + "openflow.owl#OpenFlow-1.2");
    	openflowV1_3Ind = new ResourceImpl(ORCA_NS + "openflow.owl#OpenFlow-1.3");
    	
    	bitRate1G = new ResourceImpl(ORCA_NS + "layer.owl#1G");
    	bitRate10G = new ResourceImpl(ORCA_NS + "layer.owl#10G");
    	
    	stitchingDomain = new ResourceImpl(ORCA_NS + "orca.rdf#Stitching/Domain");
    	
    	diskImageClass = new ResourceImpl(ORCA_NS + "exogeni.owl#DiskImage");
    	
    	hasColorAttribute = new PropertyImpl(ORCA_NS + "app-color.owl#hasColorAttribute"); 
    	hasColorBlob = new PropertyImpl(ORCA_NS + "app-color.owl#hasColorBlob");
    	hasColorKey = new PropertyImpl(ORCA_NS + "app-color.owl#hasColorKey");
    	hasColorLabel = new PropertyImpl(ORCA_NS + "app-color.owl#hasColorLabel");
    	hasColorValue = new PropertyImpl(ORCA_NS + "app-color.owl#hasColorValue");
    	hasColorXMLBlob = new PropertyImpl(ORCA_NS + "app-color.owl#hasColorXMLBlob");
    	hasColorXMLCompressedBlob = new PropertyImpl(ORCA_NS + "app-color.owl#hasColorXMLCompressedBlob");
	}
	
	// map from 
	public static final String[] orcaSchemaFiles = { 
		"collections.owl",
		"compute.owl",
		"domain.owl",
		"dtn.owl",
		"ec2.owl",
		"ethernet.owl",
		"eucalyptus.owl",
		"exogeni.owl",
		"geni.owl",
		"ip4.owl",
		"itu-grid.owl",
		"kansei.owl",
		"layer.owl",
		"location.owl",
		"manifest.owl",
		"modify.owl",
		"openflow.owl",
		"planetlab.owl",
		"protogeni.owl",
		"request.owl",
		"storage.owl",
		"tcp.owl",
		"topology.owl",
		"app-color.owl"
	};
	
	public static final String[] orcaSubstrateFiles = {
		"orca.rdf", "ben.rdf", "ben-dtn.rdf", "ben-6509.rdf"
	};
	
	public static final Map<String, String> externalSchemas; 
	static {
		Map<String, String> m = new HashMap<String, String>();
		m.put("http://www.w3.org/2006/time", "time.owl");
		externalSchemas = Collections.unmodifiableMap(m);
	}
	
	/**
	 * Jena does not handle java jar:file: URLs, so we need this locator
	 * @author ibaldin
	 *
	 */
	public static class LocatorJarURL extends LocatorURL {
		public LocatorJarURL() {
			super();
		}
		
		@Override
		public TypedStream open(String filenameOrURI) {
			try {
				URL u = new URL(filenameOrURI);
				if (filenameOrURI.startsWith("jar:")) {
					JarURLConnection jarConnection = (JarURLConnection)u.openConnection();
					return new TypedStream(jarConnection.getInputStream());
				}
				return new TypedStream(u.openStream());
			} catch (MalformedURLException e) {
				;
			} catch (IOException i) {
				;
			}
			
			return super.open(filenameOrURI);
		}
		
		@Override
		public String getName() {
			return ("LocatorJarURL");
		}
		
	}
	
	/**
	 * Set global DocumentManager redirections for Jena not to look for schema files
	 * on the internet, but to use files in this package instead.
	 */
	private static boolean globalRedirections = false;
	
	public static void setGlobalJenaRedirections() {
		
		// idempotent
		if (globalRedirections)
			return;
		globalRedirections = true;
		
		//ClassLoader cl = NdlCommons.class.getClassLoader();
		//ClassLoader cl = ClassLoader.getSystemClassLoader();
		ClassLoader cl = NdlCommons.class.getProtectionDomain().getClassLoader();
		
		OntDocumentManager dm = OntDocumentManager.getInstance();
		dm.getFileManager().addLocator(new LocatorJarURL());
		
		for (String s: orcaSchemaFiles) { 
			dm.addAltEntry(NdlCommons.ORCA_NS + s, cl.getResource(ORCA_NDL_SCHEMA + s).toString());
		}
		
		for (String s: orcaSubstrateFiles) { 
			dm.addAltEntry(NdlCommons.ORCA_NS + s, cl.getResource(ORCA_NDL_SUBSTRATE + s).toString());
		}
		
		 //deal with odd ones we didn't create (time etc)
		for (String s: externalSchemas.keySet()) { 
			dm.addAltEntry(s, cl.getResource(ORCA_NDL_SCHEMA + externalSchemas.get(s)).toString());
		}
	}
	
	/**
	 * Find all things connected to this network connection (typically in request)
	 * @param nc
	 * @return
	 */
	public static String createQueryConnectedToNetworkConnection(Resource nc) {
		String selectStr = "SELECT DISTINCT ?interface ";
		String whereStr = "WHERE { " +
				//"<" + nc.getURI() + "> rdf:type topology:NetworkConnection. " +
				"<" + nc.getURI() + "> topology:hasInterface ?interface. " +
				//"?element topology:hasInterface ?interface. " + 
				"   }";
		
		return createQueryString(selectStr, "", whereStr);
	}
	
	/**
	 * Find the things sharing interface with this resource (usually connections)
	 * @param n
	 * @return
	 */
	public static String createQueryPeers(Resource n) {
		String selectStr = "SELECT DISTINCT ?peer ?interface";
		String whereStr = "WHERE {  " +
				"<" + n + "> topology:hasInterface ?interface. " +
				"?peer topology:hasInterface ?interface. " +
				"FILTER(?peer != <" + n.getURI() + ">) " + 
				" }";
		return createQueryString(selectStr, "", whereStr);
	}
	
	/**
	 * return peers of this resource across a connection (that aren't itself)
	 * @param n
	 * @return
	 */
	public static String createQueryConnectionPeers(Resource n) {
		String selectStr = "SELECT DISTINCT ?peer ?interface ";
		String whereStr = "WHERE {  " +
				"<" + n + "> topology:hasInterface ?if. " +
				"?conn topology:hasInterface ?if. " +
				"{{?conn rdf:type topology:LinkConnection.} UNION {?conn rdf:type topology:NetworkConnection}}. " +
				"?conn topology:hasInterface ?interface . " +
				"?peer topology:hasInterface ?interface . " +
				"FILTER(?peer != <" + n.getURI() + "> && ?conn != ?peer) " +
				" }";
		return createQueryString(selectStr, "", whereStr);
	}
	
	/**
	 * to support coloring extension, return a query that looks for
	 * colors attached to network elements
	 * @return
	 */
	public static String createQueryStringHasColor() {
		String selectStr = "SELECT DISTINCT ?netelement ?color ";
		String whereStr = "WHERE {" +
				"?netelement rdf:type topology:NetworkElement. " +
				"?color rdf:type app-color:Color. " +
				"?netelement app-color:hasColor ?color. " +
				"   }";
		return createQueryString(selectStr, "", whereStr);
	}
	
	public static String createQueryStringColorDependency() {
		String selectStr = "SELECT DISTINCT ?fromNe ?toNe ?color ";
		String whereStr = "WHERE {" +
				"?fromNe rdf:type topology:NetworkElement. " +
				"?toNe rdf:type topology:NetworkElement. " +
				"?color rdf:type app-color:Color. " +
				"?fromNe app-color:toColorDependency ?color. " +
				"?toNe app-color:fromColorDependency ?color. " +
				"    }";
		return createQueryString(selectStr, "", whereStr);
	}
	
	/**
	 * Get storage properties like fs_param, format, fs_type, mnt_point, capacity
	 * @param st
	 * @return
	 */
	public static String createQueryISCSIStorageProperties(Resource st) {
		String selectStr = "SELECT DISTINCT ?fsParam ?fsType ?format ?mntPoint ?capacity ";
		String whereStr = "WHERE {" +
				"<" + st.getURI() + "> rdf:type storage:ISCSI. " +
				"<" + st.getURI() + "> domain:hasResourceType storage:LUN. " +
				"<" + st.getURI() + "> storage:storageCapacity ?capacity. " +
				"<" + st.getURI() + "> storage:hasFSParam ?fsParam. " + 
				"<" + st.getURI() + "> storage:hasFSType ?fsType. " +
				"<" + st.getURI() + "> storage:doFormat ?format. " +
				"<" + st.getURI() + "> storage:hasMntPoint ?mntPoint. " +
				"}  ";
		return createQueryString(selectStr, "", whereStr);
	}
	
	/**
	 * Get the query for reservation and its term details
	 * @return
	 */
	public static String createQueryStringReservationTerm() {
	    String selectStr = "SELECT DISTINCT ?reservation ?term ?beginning ?beginningTime ?end ?endTime ?duration ?years ?months ?weeks ?days ?hours ?minutes ?seconds ";
	    String fromStr = "";
	    String whereStr = "WHERE {" + 
	    				"?reservation rdf:type request:Reservation. " +
	    				"OPTIONAL {?reservation request:hasTerm ?term.}. " +
	    				"OPTIONAL {?term time:hasBeginning ?beginning. ?beginning time:inXSDDateTime ?beginningTime.}. " +
	    				"OPTIONAL {?term time:hasEnd ?end. ?end time:inXSDDateTime ?endTime.}. " +
	    				"OPTIONAL {?term time:hasDurationDescription ?duration.}. " +
	    				"OPTIONAL {?duration time:years ?years.}. " +
	    				"OPTIONAL {?duration time:months ?months.}. " +
	    				"OPTIONAL {?duration time:weeks ?weeks.}. " +
	    				"OPTIONAL {?duration time:days ?days.}. " +
	    				"OPTIONAL {?duration time:hours ?hours.}. " +
	    				"OPTIONAL {?duration time:minutes ?minutes.}. " +
	    				"OPTIONAL {?duration time:seconds ?seconds.}. " +
	    	"      }";
	    return createQueryString(selectStr, fromStr, whereStr);
	}
	
	/**
	 * Query for modification details
	 * @return
	 */
	public static String createQueryStringModifyReservation() {
		String selectStr = "SELECT DISTINCT ?modifyReservation ?modifyName";
		String fromStr = "";
		String whereStr = "WHERE {" +
						"?modifyReservation rdf:type modify:ModifyReservation. " +
						"OPTIONAL {?modifyReservation topology:hasName ?modifyName.}. " +
						"    }";
		return createQueryString(selectStr, fromStr, whereStr);
	}

	/**
	 * Query for domain location 
	 * @return
	 */
	public static String createQueryStringDomainLocationDetails() {
		String selectStr = "SELECT ?domain ?popUri ?lat ?lon ";
		String fromStr = "";
		String whereStr = "WHERE {"  +
	    	"?domain <http://geni-orca.renci.org/owl/collections.owl#element> ?popUri . " +
	        "?popUri <http://geni-orca.renci.org/owl/location.owl#locatedAt> ?loc . " +
	        "?loc <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat . " +
	        "?loc <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?lon .  " +
		" }";
		return createQueryString(selectStr, fromStr, whereStr);
	}
	
	public static Resource getDomainHasCastType(String cast, String property, OntModel ontModel){
		ResultSet results = NdlCommons.getDomainHasCastType(property,ontModel);
		if (!results.hasNext()) {
			return null;
		}
		QuerySolution solution=null;
		String domain=(String) results.getResultVars().get(0);
		String castType=(String) results.getResultVars().get(1);
		Resource domain_rs=null,castType_rs=null;
		while (results.hasNext()){
			solution=results.nextSolution();
			domain_rs=solution.getResource(domain);
			castType_rs=solution.getResource(castType);
			if(castType_rs.getLocalName().contains(cast))
				break;
		}
		
		return domain_rs;
	}	
	
	public static ResultSet getDomainHasCastType(String property, OntModel ontModel) {
		String selectStr = "SELECT ?domain ?castType ";
		String fromStr = "";
		String whereStr = "WHERE {"  +
	    	"?domain "+ property +" ?service. " +
	        "?service layer:hasCastType ?castType. " +
		" }";
		String queryPhrase = createQueryString(selectStr, fromStr, whereStr);
        ResultSet  results = rdfQuery(ontModel, queryPhrase);
        
        //outputQueryResult(results);
        //results = rdfQuery(ontModel, queryPhrase);
        return results;
	}
	
	/** get a query for an interface object on who hasInterface on it
	 * 
	 * @param intf
	 * @return
	 */
	public static String createQueryStringWhoHasInterface(Resource intf) {
		// FIXME: we SHOULD check the type (10/03/2011 /ib), but manifest
		// currently does not declare interface individuals
	    String selectStr = "SELECT DISTINCT ?item ";
	    String fromStr = "";
	    String whereStr = "WHERE {" + 
	    				//"<" + intf.getURI() + "> rdf:type topology:Interface. " +
	    				"?item topology:hasInterface <" + intf.getURI() + ">." +  
	    				"      }";
	    return createQueryString(selectStr, fromStr, whereStr);
	}

	public static String createQueryStringWhoHaveInterface(Resource intf) {
		// FIXME: we SHOULD check the type (10/03/2011 /ib), but manifest
		// currently does not declare interface individuals
	    String selectStr = "SELECT DISTINCT ?node ?conn ";
	    String fromStr = "";
	    String whereStr = "WHERE {" + 
	    				//"<" + intf.getURI() + "> rdf:type topology:Interface. " +
	    				"?node topology:hasInterface <" + intf.getURI() + ">." + 
	    				"?conn topology:hasInterface <" + intf.getURI() + ">." +
	    				"?conn rdf:type ndl:NetworkConnection. " +
	    				"{{?node rdf:type compute:ServerCloud.} UNION {?node rdf:type compute:ComputeElement.} UNION {?node rdf:type ndl:Device.}}. " +
	    				"      }";
	    return createQueryString(selectStr, fromStr, whereStr);
	}

	
	public static String createQueryStringManifestDetails() {
		String selectStr = "SELECT DISTINCT ?manifest ";
	    String fromStr = "";
	    String whereStr = "WHERE {" + 
	    "?manifest rdf:type request:Manifest. " +
	    "   }";
	    return createQueryString(selectStr, fromStr, whereStr);
	}
	
	public static String createQueryStringSite() {
	    String selectStr = "SELECT DISTINCT  ?connection ?resource ?rType ?numResource ?bw  ?ip1_addr ?ip1_netmask ?vmImageURL ?vmImageGUID ?parent ?postBoot ?inDomain ";
	    String fromStr = "";
	    String whereStr = "WHERE {" + 
	    				"{?reservation collections:element ?resource. " +
	    				"?reservation rdf:type request:Reservation. " +
	    				"{{?resource rdf:type compute:ServerCloud.} UNION {?resource rdf:type compute:ComputeElement.}}. " +
	    				"OPTIONAL {?resource layer:numCE ?numResource.}"+
	    				"OPTIONAL {?resource domain:hasResourceType ?rType.}"+
	    				"OPTIONAL {?resource compute:diskImage ?vmImage. ?vmImage ndl:hasURL ?vmImageURL. ?vmImage ndl:hasGUID ?vmImageGUID.}"+
	    				"OPTIONAL{?resource ndl:hasInterface ?intf1. ?intf1 ip4:localIPAddress ?ip1. ?ip1 layer:label_ID ?ip1_addr. ?ip1 ip4:netmask ?ip1_netmask.}"+
	    				"OPTIONAL {?resource request:dependOn ?parent.}"+
	    				"OPTIONAL {?resource request:postBootScript ?postBoot.}"+
	    				"OPTIONAL {?resource request:inDomain ?inDomain.}"+
	    				"} UNION " +
	            		"{?connection a ndl:NetworkConnection."+
	            		"?connection ndl:hasInterface ?resource."+
	            		"OPTIONAL {?resource layer:numCE ?numResource.}."+
	            		"OPTIONAL {?connection layer:bandwidth ?bw.} "+
	            		"OPTIONAL{?resource ip4:localIPAddress ?ip1. ?ip1 layer:label_ID ?ip1_addr. ?ip1 ip4:netmask ?ip1_netmask.}"+
	            		"OPTIONAL {?connection domain:hasResourceType ?rType.}."+
	            		"}"+
	    	"      }";
	    return createQueryString(selectStr, fromStr, whereStr);
	}

	public static String createQueryStringConnect() {
	    String selectStr = "SELECT DISTINCT ?resource ?object ?connection ?bw ?ip1_addr ?ip1_netmask " +
	    		"?ip2_addr ?ip2_netmask ?intf1_hostInterfaceName ?intf2_hostInterfaceName ?resourceDomain ?objectDomain ";
	    String fromStr = "";
	    String whereStr = "WHERE {" + 
	    				"{?resource ndl:connectedTo ?object. "+
	    				"?resource a compute:ServerCloud. ?object a compute:ServerCloud. "+
	    				"FILTER(?resource != ?object)" +
	    				"OPTIONAL{?resource request:inDomain ?resourceDomain.}"+
	    				"OPTIONAL{?object request:inDomain ?objectDomain.}"+
	    				"} UNION " +
	    				"{?resource ndl:hasInterface ?intf1. ?object ndl:hasInterface ?intf2. "+
	    				"?intf1 ndl:connectedTo ?intf2. "+
	    				"FILTER(?resource != ?object)" +
	    				"OPTIONAL{?resource request:inDomain ?resourceDomain.}"+
	    				"OPTIONAL{?object request:inDomain ?objectDomain.}"+
	            		"} UNION "+
	            		"{?connection a ndl:NetworkConnection. " +
	    				"?connection ndl:hasInterface ?intf1. ?connection ndl:hasInterface ?intf2. " +
	    				"?resource ndl:hasInterface ?intf1. ?object ndl:hasInterface ?intf2."+
	    				"{{?resource a compute:ComputeElement.} UNION {?resource a ndl:Device.}}."+
	    				"{{?object a compute:ComputeElement.} UNION {?object a ndl:Device.}}. "+
	    				"FILTER(?resource != ?object)" +
	    				"OPTIONAL{?connection layer:bandwidth ?bw.}"+
	    				"OPTIONAL{?resource request:inDomain ?resourceDomain.}"+
	    				"OPTIONAL{?object request:inDomain ?objectDomain.}"+
	    				"OPTIONAL{?intf1 ndl:hostInterfaceName ?intf1_hostInterfaceName. ?intf2 ndl:hostInterfaceName ?intf2_hostInterfaceName.}"+
	    				"OPTIONAL{?intf1 ip4:localIPAddress ?ip1. ?ip1 layer:label_ID ?ip1_addr. ?ip1 ip4:netmask ?ip1_netmask.}"+
	    				"OPTIONAL{?intf2 ip4:localIPAddress ?ip2. ?ip2 layer:label_ID ?ip2_addr. ?ip2 ip4:netmask ?ip2_netmask.}"+
	    				"}"+
	    	"      }"+	   
			"ORDER BY DESC(?ip1_addr)";
	    return createQueryString(selectStr, fromStr, whereStr);
	}

    // get availableLable Set: rsURI=intf.getURI()
    public static ResultSet getAvailableLabelSet(OntModel ontModel, String rsURI, String availableLableSet_str){
    	ResultSet results = null;

        String s = "SELECT ?lv ?uv ?l ?u ?b ?r ";
        String f = "";
        String w = "WHERE {" + 
        		"<" + rsURI + "> " +availableLableSet_str + " ?r." + 
        		"?r collections:element ?b. " + 
        		"OPTIONAL{"+
        			"?b layer:upperBound ?u. " + 
        			"?u layer:label_ID ?uv. " + 
        			"?b layer:lowerBound ?l. " + 
        			"?l layer:label_ID ?lv." +
        		"}."+
        "}"+
        "ORDER BY ASC(?lv)";
        String queryPhrase = createQueryString(s, f, w);

        results = rdfQuery(ontModel, queryPhrase);

        return results;
    }
	
	public static String createQueryStringLinkClass(String ob) {
	    String selectStr = "SELECT ?resource ?object ";
	    String fromStr = "";
	    String whereStr = "WHERE {" + "?resource a " + ob + "." + "?resource " + "ndl:linkTo " + "?object" + "      }";
	    return createQueryString(selectStr, fromStr, whereStr);
	}

	public static String createQueryStringOnPath(String subject, String sub) {
	    String onPath = "gleen:OnPath";
	    String selectStr = "SELECT ?object ";
	    String fromStr = "";
	    String whereStr = "WHERE {" + "<" + subject + "> " + onPath + sub + " ?object" + "). FILTER(?object !=" + "<" + subject + "> " + ")" + "      }";
	
	    return createQueryString(selectStr, fromStr, whereStr);
	}

	public static String createQueryStringObject(String sb, String p) {
	    String selectStr = "SELECT ?object ";
	    String fromStr = "";
	    if (sb == null)
	        sb = "?resource ";
	    else
	        sb = "<" + sb + ">";
	    String whereStr = "WHERE {" + sb + p + " ?object" + "      }";
	    return createQueryString(selectStr, fromStr, whereStr);
	}
    // find the layer:Layer
    public static String findLayer(OntModel m,Resource resource)
    {

        ResultSet results = getLayer(m, resource.getURI());

        String varName = (String) results.getResultVars().get(0);

        if (results.hasNext())
            return results.nextSolution().getResource(varName).getLocalName();
        else
            return null;
    }

    // get layer:Layer
    public static ResultSet getLayer(OntModel m, String rsURI)
    {

        String s = "SELECT ?r ";
        String f = "";
        String w = "WHERE {" + "<" + rsURI + ">" + " ?p ?r." + " ?r rdf:type " + "layer:Layer" + "      }";
        String queryPhrase = createQueryString(s, f, w);

        ResultSet results = rdfQuery(m, queryPhrase);

        return results;
    }

    public static ResultSet getLayerAdapatation(OntModel m, String rsURI)
    {
        String s = "SELECT ?r ";
        String f = "";
        String w = "WHERE {" + 
        	"{<" + rsURI + ">" + " ?p ?r." + " ?p rdf:type layer:AdaptationProperty} UNION{" +
        	"<" + rsURI + ">"+" layer:AdaptationProperty ?r}"+
        	"      }";
        String queryPhrase = createQueryString(s, f, w);

        ResultSet results = rdfQuery(m, queryPhrase);

        return results;
    }
    
    public static ResultSet getLayerAdapatationOf(OntModel m, String rsURI)
    {
        String s = "SELECT ?r ";
        String f = "";
        String w = "WHERE {" + 
        	"{?r ?p "+ "<" + rsURI + ">" +"." + " ?p rdf:type layer:AdaptationProperty.} "
        			+ "UNION{" + "?r layer:AdaptationProperty " +"<" + rsURI + ">}"+
        	"      }";
        String queryPhrase = createQueryString(s, f, w);

        ResultSet results = rdfQuery(m, queryPhrase);

        return results;
    }
    
    public static Resource parentGetInterface(OntModel m, String node, String parent)
    {
    	String s = "SELECT ?intf ";
        String f = "";
        String w = "WHERE {" + "<" + node + ">" + " ndl:hasInterface "+ "?intf." 
        				+  "<" + parent + ">" + " ndl:hasInterface "+ "?intf."
        						+ "}";
        String queryPhrase = createQueryString(s, f, w);

        ResultSet results = rdfQuery(m, queryPhrase);
        
        Resource intf_rs=null,first_intf_rs=null;
        while(results.hasNext()) {
			ResultBinding result = (ResultBinding)results.next();
			if (result != null) {
				first_intf_rs = (Resource)result.get("intf");
				if(first_intf_rs.hasProperty(ip4LocalIPAddressProperty)){
					intf_rs=first_intf_rs;
					break;
				}
			}
		}
        if(intf_rs==null)
        	intf_rs=first_intf_rs;
        return intf_rs;
    }

    /**
     * Run a query on a model
     * @param model
     * @param queryString
     * @return
     */
    public static ResultSet rdfQuery(OntModel model, String queryString)
    {

        Query query = QueryFactory.create(queryString);

        // Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet results = qe.execSelect();
        //qe.close();
        return results;
    }
   
	public static void outputQueryResult(ResultSet results, Query query) {
	    ResultSetFormatter.out(System.out, results, query);
	}

	public static void outputQueryResult(ResultSet results) {
	    ResultSetFormatter.out(System.out, results);
	}

	/**
	 * Get a list of interfaces (similar to getInterfacesOf)
	 * @param r
	 * @return
	 */
	public static List<Resource> getResourceInterfaces(Resource r) {
		assert(r != null);
		List<Resource> ifs = new ArrayList<Resource>();
		for (StmtIterator resEl = r.listProperties(topologyHasInterfaceProperty); resEl.hasNext();) {
			Statement s = resEl.next();
			ifs.add(s.getResource());
		}
		return ifs;
	}
	
	public static Resource getLayer(Resource c) {
		assert(c != null);
		Statement conBwStmt = c.getProperty(atLayer);
		if (conBwStmt != null) {
			return conBwStmt.getResource();
		}
		return null;
	}
	
	public static long getResourceBandwidth(Resource c) {
		assert(c != null);
		Statement conBwStmt = c.getProperty(layerBandwidthProperty);
		if (conBwStmt != null) {
			return conBwStmt.getLong();
		}
		return 0;
	}

	/**
	 * Get storage capacity property of the resource
	 * @param c
	 * @return
	 */
	public static long getResourceStorageCapacity(Resource c) {
		assert(c != null);
		Statement storCap = c.getProperty(storageCapacity);
		if (storCap != null) {
			return storCap.getLong();
		}
		return 0;
	}
	
	/**
	 * Get hasFSParam property of the resource
	 * @param c
	 * @return
	 */
	public static String getResourceStorageFSParam(Resource c) {
		assert(c != null);
		Statement stor = c.getProperty(hasFSParam);
		if (stor != null) {
			return stor.getString();
		}
		return null;
	}
	
	/**
	 * Get hasFSType property of the resource
	 * @param c
	 * @return
	 */
	public static String getResourceStorageFSType(Resource c) {
		assert(c != null);
		Statement stor = c.getProperty(hasFSType);
		if (stor != null) {
			return stor.getString();
		}
		return null;
	}
	
	/**
	 * Get hasMntPoint property of the resource
	 * @param c
	 * @return
	 */
	public static String getResourceStorageMntPoint(Resource c) {
		assert(c != null);
		Statement stor = c.getProperty(hasMntPoint);
		if (stor != null) {
			return stor.getString();
		}
		return null;
	}
	
	/**
	 * Get doFormat property of the resource
	 * @param c
	 * @return
	 */
	public static Boolean getResourceStorageDoFormat(Resource c) {
		assert(c != null);
		Statement stor = c.getProperty(doFormat);
		if (stor != null) {
			return stor.getBoolean();
		}
		return false;
	}
	
	public static long getResourceLatency(Resource c) {
		assert(c != null);
		Statement conLatStmt = c.getProperty(layerLatencyProperty);
		if (conLatStmt != null) {
			return conLatStmt.getLong();
		}
		return 0;
	}

	public static String getAddressIP(Resource i) {
		return getStringProperty(i, layerLabelIdProperty);
	}

	public static String getAddressNetmask(Resource i) {
		return getStringProperty(i, ip4NetmaskProperty);
	}
	
	public static String getAddressMAC(Resource i) {
		return getStringProperty(i, ipMacAddressProperty);
	}

	public static String getInterfaceIP(Resource i) {
		
		assert(i != null);
		
		if (i.hasProperty(ip4LocalIPAddressProperty)) {
			Statement st = i.getProperty(ip4LocalIPAddressProperty);
			return getAddressIP(st.getResource());
		}
		return null;
	}
	
	public static String getInterfaceNetmask(Resource i) {
		
		assert(i != null);
		
		if (i.hasProperty(ip4LocalIPAddressProperty)) {
			Statement st = i.getProperty(ip4LocalIPAddressProperty);
			return getAddressNetmask(st.getResource());
		}
		return null;
	}
	
	/** return owl:sameAs property resource, except when pointing to self
	 * 
	 * @param i
	 * @return
	 */
	public static Resource getSameAsResource(Resource i) {
		Statement st = i.getProperty(OWL_sameAs);
		
		if ((st != null) && (!st.getResource().equals(i))) {
			return st.getResource();
		}
		return null;
	}
	
	public static String getEC2InstanceId(Resource i) {
		return getStringProperty(i, hasInstanceIDProperty);
	}
	
	public static String getEC2WorkerNodeId(Resource i) {
		return getStringProperty(i, workerNodeIDProperty);
	}
	
	/**
	 * Get a list of services node supports. Return a URI for the service
	 * e.g. ssh://user@hostname:port
	 * @param i
	 * @return
	 */
	public static List<String> getNodeServices(Resource node) {
		assert (node != null);
		
		List<String> services = new ArrayList<String>();
		for(StmtIterator si = node.listProperties(domainHasServiceProperty); si.hasNext(); ) {
			Resource srv = si.next().getResource();
			
			// see what type it is
			Statement accessStmt = srv.getRequiredProperty(domainHasAccessMethod);
			String service = null;
			if (accessStmt.getResource().equals(domainSSHServiceClass)) {
				service = "ssh://";
			} 
			
			List<String> logins = new ArrayList<String>();
			Statement loginStmt = srv.getProperty(topologyHasLogin);
			if (loginStmt != null) {
				String loginString = loginStmt.getString();
				if ((loginString != null) && (loginString.length() != 0)) {
					String[] tmp = loginString.split(",");
					logins.addAll(Arrays.asList(tmp));
				}
			} else {
				logins.add("root");
			}
			
			for(String l: logins) {
				// see if it has management IP/port properties, which are literals
				String serviceURI = null;
				if (srv.getProperty(topologyManagementIP) != null) {
					serviceURI = service + l + "@" + srv.getProperty(topologyManagementIP).getString();
					if (srv.getProperty(topologyManagementPort) != null) {
						serviceURI += ":" + srv.getProperty(topologyManagementPort).getString();
					}
				} 
				if (serviceURI != null)
					services.add(serviceURI);
			}
		}
		
		return services;
	}
	
	/**
	 * Get only the logins of the node, don't add root
	 * @param node
	 * @return
	 */
	public static List<String> getNodeLogins(Resource node) {
		assert (node != null);
		
		Set<String> logins = new HashSet<String>();
		for(StmtIterator si = node.listProperties(domainHasServiceProperty); si.hasNext(); ) {
			Resource srv = si.next().getResource();
			
			// see what type it is
			Statement accessStmt = srv.getRequiredProperty(domainHasAccessMethod);
			String service = null;
			if (accessStmt.getResource().equals(domainSSHServiceClass)) {
				service = "ssh://";
			} 
			
			Statement loginStmt = srv.getProperty(topologyHasLogin);
			if (loginStmt != null) {
				String loginString = loginStmt.getString();
				if ((loginString != null) && (loginString.length() != 0)) {
					String[] tmp = loginString.split(",");
					logins.addAll(Arrays.asList(tmp));
				}
			} 
		}
		List<String> loginsList = new ArrayList<String>();
		loginsList.addAll(logins);
		
		return loginsList;
	}
	
	/**
	 * Class to return proxy for TCP ports info
	 * @author ibaldin
	 *
	 */
	public static class ProxyFields {
		public String mgtIp;
		public short mgtPort, proxiedPort;
	}
	/**
	 * Get the list of ports proxied for this node. For each
	 * proxied port (field "proxiedPort") it can return, if available
	 * the ip and port on the proxy ("mgtPort", "mgtIp")
	 * @param node
	 * @return
	 */
	public static List<ProxyFields> getNodeProxiedPorts(Resource node) {
		List<ProxyFields> ret = new ArrayList<ProxyFields>(); 
		
		for(StmtIterator si = node.listProperties(domainProxyProperty); si.hasNext(); ) {
			ProxyFields pf = new ProxyFields();
			
			Resource prx = si.next().getResource();

			Statement proxiedStmt = prx.getRequiredProperty(topologyProxiedPort);
			
			pf.proxiedPort = proxiedStmt.getShort();
			
			if ((prx.getProperty(topologyManagementIP) != null) &&
					(prx.getProperty(topologyManagementPort) != null)) {
				pf.mgtPort = prx.getProperty(topologyManagementPort).getShort();
				pf.mgtIp = prx.getProperty(topologyManagementIP).getString();
			}
			ret.add(pf);
		}
		return ret;
	}
	
	public static int getLiteralInt(Literal i) {
		if (i != null)
			return i.getInt();
		else
			return 0;
	}

	public static Date getTermDate(Literal d) {
		if (d == null)
			return null;
		Calendar cal = DatatypeConverter.parseDateTime(d.getString());
		return cal.getTime();
	}

	/**
	 * Get diskImage of a resource; null otherwise
	 * @param i
	 * @return
	 */
	public static Resource getDiskImage(Resource i) {
		assert(i != null);
		Statement nodeImageStmt = i.getProperty(diskImageProperty);
		if (nodeImageStmt != null) 
			return nodeImageStmt.getResource();
		return null;
	}
	
	/**
	 * Get OF controller of most-likely a slice
	 * @param sl
	 * @return
	 */
	public static Resource getOfCtrl(Resource sl) {
		assert(sl != null);
		
		Statement ofCtrlStmt = sl.getProperty(openflowControllerProperty);
		if (ofCtrlStmt != null)
			return ofCtrlStmt.getResource();
		return null;
	}
	
	/**
	 * Get the version of openflow (usually of a reservation).
	 * Null if none.
	 * @param res
	 * @return
	 */
	public static String getOpenFlowVersion(Resource res) {
		assert(res != null);
		String ret = null;
		Statement ofCapable = res.getProperty(openflowCapableProperty);
		if (ofCapable != null) {
			if (ofCapable.getResource().equals(openflowV1_0Ind))
				ret = "1.0";
			else if (ofCapable.getResource().equals(openflowV1_1Ind))
				ret = "1.1";
			else if (ofCapable.getResource().equals(openflowV1_2Ind))
				ret = "1.2";
			else if (ofCapable.getResource().equals(openflowV1_3Ind))
				ret = "1.3";
		}
		return ret;
	}
	

	public static String getURL(Resource di) {
		return getStringProperty(di, hasURLProperty);
	}
	
	/**
	 * Assumes di is the DiskImage resource; null if unspecified
	 * @param di
	 * @return
	 */
	public static String getImageURL(Resource di) {
		return getURL(di);
	}
	
	/**
	 * Assumes di is a DiskImage resource. null if unspecified
	 * @param di
	 * @return
	 */
	public static String getImageHash(Resource di) {
		assert(di != null);
		Statement diGUIDP = di.getProperty(hasGUIDProperty);
		String diGUID = null;
		if (diGUIDP != null)
			diGUID = diGUIDP.getString();
		return diGUID;
	}
	
	/**
	 * Assumes i is a node-like resource that hasDiskImage on
	 * disk image resource or null
	 * @param i
	 * @return
	 */
	public static String getIndividualsImageURL(Resource i) {
		assert(i != null);
		Statement nodeImageStmt = i.getProperty(diskImageProperty);
		if (nodeImageStmt != null) {
			Resource di = nodeImageStmt.getResource();
			return getImageURL(di);
		}
		return null;
	}
	
	/**
	 * Assumes i is a node-like resource that hasDiskImage on
	 * disk image resource or null
	 * @param i
	 * @return
	 */
	public static String getIndividualsImageHash(Resource i) {
		assert(i != null);
		Statement nodeImageStmt = i.getProperty(diskImageProperty);
		if (nodeImageStmt != null) {
			Resource di = nodeImageStmt.getResource();
			return getImageHash(di);
		}
		return null;
	}
	
	/**
	 * Get RDF_TYPE property of resource
	 * @param r
	 * @return
	 */
	public static Resource getResourceType(Resource r) {
		assert(r != null);
		for (StmtIterator j=r.listProperties(RDF_TYPE);j.hasNext();){
			Resource type=j.next().getResource();
			if(type.getURI().endsWith("NamedIndividual"))
				continue;
			return type;
		}
		return null;
	}
	
	/**
	 * Check that this resource has this type
	 * @param r
	 * @param t
	 * @return
	 */
	public static boolean hasResourceType(Resource r, Resource t) {
		return r.hasProperty(RDF_TYPE, t);
	}
	
	/**
	 * Get RDFS_Label property of resource
	 * @param r
	 * @return
	 */
	public static String getResourceLabel(Resource r) {
		return getStringProperty(r, RDFS_Label);
	}
	
	/**
	 * Get the value of the post boot script property
	 * @param i
	 * @return
	 */
	public static String getPostBootScript(Resource i) {
		return getStringProperty(i, requestPostBootScriptProperty);
	}
	
	/**
	 * Get the value of the groupName property
	 * @param i
	 * @return
	 */
	public static String getGroupName(Resource i) {
		assert(i != null);
		Statement gnStmt = i.getProperty(requestGroupNameProperty);
		if (gnStmt != null) {
			String sc = gnStmt.getString();
			return (sc.length() > 0 ? sc : null);
		}
		return null;
	}

	public static ResultSet getInterfaceOfSwitching(OntModel ontModel,String rs)
	{

		String s = "SELECT ?sm ";
		String f = "";
		String w = "WHERE {" + 
		"?r ndl:hasInterface " + "<" + rs + ">." + 
		" ?r rdf:type ndl:Device." + 
		"?r ndl:hasSwitchMatrix ?sm" +
		"}";
		String queryPhrase = createQueryString(s, f, w);

		ResultSet results = rdfQuery(ontModel, queryPhrase);

		return results;
	}	

    // individuals type of "ob"
    public static String createQueryStringType(String ob)
    {
        return createQueryStringSubjectData("rdf:type", ob);
    }

    // when the object is a resource
    public static String createQueryStringSubject(String p, String ob)
    {
        return createQueryStringSubjectData(p, "<" + ob + ">");
    }

    // the object could be a literal
    public static String createQueryStringSubjectData(String p, String ob)
    {
        String selectStr = "SELECT ?subject";
        String fromStr = "";
        String whereStr = " WHERE {" + "?subject " + p + " " + ob + " .}";
        return createQueryString(selectStr, fromStr, whereStr);
    }

    public static String createQueryString(String selectStr, String fromStr, String whereStr)
    {
        String queryString = ontPrefix + selectStr + " " + fromStr + " " + whereStr;
       
        return queryString;
    }

	public static OntResource getOntOfType(OntModel aM, String type){
		Resource rs=null;
		String queryPhrase=createQueryStringType(type);
		ResultSet results = rdfQuery(aM,queryPhrase);
		String var0=(String) results.getResultVars().get(0);
		//NdlCommons.outputQueryResult(results);
		if(results.hasNext()) {
			rs = results.nextSolution().getResource(var0);
			return aM.getOntResource(rs); 
		}
		return null;
	}
    
	/**
	 * Get domain inDomain property of Resource (reservation or node) or null
	 * @param res
	 * @return
	 */
	public static Resource getDomain(Resource res) {
		assert(res != null);
		Statement domainStmt = res.getProperty(inDomainProperty);
		if (domainStmt != null)
			return domainStmt.getResource();
		else
			return null;
	}
	
	/**
	 * Do domain name (NDL -> short name) from domain resource
	 * @param dom
	 * @return
	 */
	public static String getDomainName(Resource dom) {
		if (dom == null)
			return null;
		// strip off name space and "/Domain"
		String domainName = StringUtils.removeStart(dom.getURI(), NdlCommons.ORCA_NS);
		domainName = StringUtils.removeEnd(domainName, "/Domain");
		
		return domainName;
	}
	
	/**
	 * Really short name equal to the one used in config.xml
	 * @param dom
	 * @return
	 */
	public static final String UriSeparator = "#";
	public static final String UriSuffix = "/Domain";
	public static String getOrcaDomainName(String domain_url) {
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
	
	public static String getOrcaDomainName(Resource dom) {
		if (dom == null)
			return null;
		// strip off name space and "/Domain"

		String domainName = dom.getURI();
		
		return getOrcaDomainName(domainName);
	}
	
	/**
	 * Get possible ExoGENI virtualization types based on resource type ("VM", "VLAN" or ...)
	 * Returns empty list if none are advertised.
	 * @param abstractModel
	 * @param rType
	 * @return
	 */
	public static List <Resource> getXOVirtualizationType(OntModel abstractModel, String rType){
		abstractModel.read(ORCA_NS + "exogeni.owl");

		String type="ndl:Device";
		ArrayList <Resource> virtList=new ArrayList<Resource>();
		try {
			Resource domain_rs = NdlCommons.getOntOfType(abstractModel, type);
			Resource pop_rs,cluster_rs,set_rs,type_rs,class_rs,virtType;

			boolean isType=false;
			for (StmtIterator i=domain_rs.listProperties(collectionElementProperty);i.hasNext();){
				pop_rs = i.next().getResource();
				for (StmtIterator j=pop_rs.listProperties(collectionElementProperty);j.hasNext();){
					isType=false;
					cluster_rs = j.next().getResource();
					for (StmtIterator k=cluster_rs.listProperties(NdlCommons.availableLabelSet);k.hasNext();){
						set_rs = k.next().getResource();
						if(set_rs.hasProperty(NdlCommons.domainHasResourceTypeProperty)){
							type_rs=set_rs.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource();					
							if(type_rs.getLocalName().equalsIgnoreCase(rType)){
								isType=true;
								break;
							}
						}
					}
					if(isType){
						for (StmtIterator l=domain_rs.listProperties(RDF_TYPE);l.hasNext();){
							class_rs = l.next().getResource();
							OntClass class_class;
							try {
								class_class = class_rs.as(OntClass.class);
							} catch (Exception ee) {
								continue;
							}
							for(Iterator <OntClass> m = class_class.listSuperClasses();m.hasNext();){
								OntClass c = m.next();
								if(c.isRestriction()){
									Restriction r = c.asRestriction();
									if(r.onProperty(virtualizeProperty)){
										if(r.isHasValueRestriction()){
											if(r.asHasValueRestriction().getHasValue().isResource()){
												virtType = r.asHasValueRestriction().getHasValue().as(Resource.class);
												virtList.add(virtType);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			return virtList;
		}
		return virtList;
	}
	
	/**
	 * Is this resource splittable? (default false, if unspecified)
	 * @param res
	 * @return
	 */
	public static boolean isSplittable(Resource res) {
		assert(res != null);
		Statement splittableStmt = res.getProperty(topologySplittableProperty);
		if (splittableStmt != null) {
			return splittableStmt.getBoolean();
		}
		return false;
	}
	
	/**
	 * Is this element a modified element? (default false, if unspecified)
	 * @param res
	 * @return
	 */
	public static boolean isModify(Resource res) {
		assert(res != null);
		Statement isModifyStmt = res.getProperty(isModifyProperty);
		if (isModifyStmt != null) {
			return isModifyStmt.getBoolean();
		}
		return false;
	}
	
	/**
	 * Return numCE property or 0
	 * @param res
	 * @return
	 */
	public static int getNumCE(Resource res) {
		assert(res != null);
		Statement numCEStmt = res.getProperty(numCEProperty);
		// what is the node count
		int numCE = 0;
		if (numCEStmt != null)
			numCE = numCEStmt.getInt();
		return numCE;
	}
	
	/**
	 * get specificCE property of a resource
	 * @return
	 */
	public static Resource getSpecificCE(Resource res) {
		assert(res != null);
		// is there a specific CE type
		Statement nodeTypeStmt = res.getProperty(specificCEProperty);
		Resource ceType = null;
		if (nodeTypeStmt != null) {
			ceType = nodeTypeStmt.getResource();
		}
		return ceType;
	}
	
	public static void getResourceConstraints(Resource ceType,NetworkElement element,int count) {
		assert(ceType != null);
		//int numCore = 0;
		OntClass instance_class = ceType.as(OntClass.class);
		for(Iterator <OntClass> i = instance_class.listSuperClasses();i.hasNext();){
			OntClass c = i.next();
			if(c.isRestriction()){
				Restriction r = c.asRestriction();
				if(r.isHasValueRestriction())
					if(r.asHasValueRestriction().getHasValue().isLiteral()){
						String key = r.asHasValueRestriction().getOnProperty().getLocalName();
						int value = r.asHasValueRestriction().getHasValue().as(Literal.class).getInt();
						DomainResource constraint = new DomainResource(key);
						constraint.setBandwidth(value);
						element.addResource(constraint);
					}
			}
		}
	}
	/**
	 * Get the hasResourceType property of resource
	 * @param r
	 * @return
	 */
	public static DomainResourceType getDomainResourceType(Resource r,NetworkElement element) {
		DomainResourceType type = getDomainResourceType(r);
		int count = type.getCount();
		Resource ceType = NdlCommons.getSpecificCE(r);
		// VM capacity requirement: core, memory,storage
		if (ceType != null){
			NdlCommons.getResourceConstraints(ceType,element,count);
		}
		// Storage capacity requirement
		if(r.hasProperty(NdlCommons.storageCapacity)){
			int value = r.getProperty(NdlCommons.storageCapacity).getInt();
			DomainResource constraint = new DomainResource(NdlCommons.storageCapacity.getLocalName());
			constraint.setBandwidth(value);
			element.addResource(constraint);
			setStorageParam(r,element);
		}
		return type;
	}
	
	public static void setStorageParam(Resource r, NetworkElement element){
		ComputeElement ce_element = (ComputeElement) element;
		if(r.hasProperty(hasFSParam)){
			ce_element.setFSParam(r.getProperty(hasFSParam).getString());
		}
		if(r.hasProperty(hasFSType)){
			ce_element.setFSType(r.getProperty(hasFSType).getString());
		}
		if(r.hasProperty(hasMntPoint)){
			ce_element.setMntPoint(r.getProperty(hasMntPoint).getString());
		}
		if(r.hasProperty(doFormat)){
			ce_element.setDoFormat(r.getProperty(doFormat).getBoolean());
		}
	}
	
	public static DomainResourceType getDomainResourceType(Resource r) {
		assert(r != null);
    	int rank = 0;
    	String rType_str=null;
	   	DomainResourceType type = new DomainResourceType();
		Statement rtStmt = r.getProperty(domainHasResourceTypeProperty);
		Resource rType = null;
		if (rtStmt != null){
			rType = rtStmt.getResource();
			if(rType!=null){
				rType_str=rType.getURI();
				int i = rType_str.indexOf('#');
				if(i>0){
					rType_str=rType_str.substring(i+1).toLowerCase();
				}
				if(rType.getProperty(resourceTypeRank)!=null)
					rank=rType.getProperty(resourceTypeRank).getInt();
			}
		}else{
			rtStmt = r.getProperty(atLayer);
			if (rtStmt != null){
				rType_str = Layer.valueOf(rtStmt.getResource().getLocalName()).getLabelP().toString();
				rank=Layer.valueOf(rtStmt.getResource().getLocalName()).rank();
			}
		}
		type.setResourceType(rType_str);
		type.setRank(rank);
		
		rtStmt = r.getProperty(numCEProperty);
		if (rtStmt != null){
			int numResource = rtStmt.getInt();
			type.setCount(numResource);
		}else{
			type.setCount(1);
		}

    	return type;
	}
	
	// sometimes getLocalName is not good enough
	public static String getTrueName(Resource r) {
		if (r == null)
			return null;
		
		return StringUtils.removeStart(r.getURI(), ORCA_NS);
	}
	
	/**
	 * Convert the state into a string (s is state)
	 * @param s
	 * @return
	 */
	public static String getStateAsString(Resource s) {
		assert(s != null);
		
		// check the type
//		Statement t = s.getRequiredProperty(RDF_TYPE);
//		if (!t.equals(requestReservationStateClass))
//			return "Unspecified";
		
		if (s.equals(requestActiveState))
			return "Active";
		if (s.equals(requestActiveTicketedState))
			return "ActiveTicketed";
		if (s.equals(requestCloseWaitState))
			return "CloseWait";
		if (s.equals(requestClosedState))
			return "Closed";
		if (s.equals(requestFailedState))
			return "Failed";
		if (s.equals(requestNascentState))
			return "Nascent";
		if (s.equals(requestNoState))
			return "No State";
		if (s.equals(requestTicketedState))
			return "Ticketed";
		return "Unspecified";
	}
	
	/**
	 * Get the state of the resource as string (checks that it has state, otherwise
	 * returns null), s is resource possessing state.
	 * @param s
	 * @return
	 */
	public static String getResourceStateAsString(Resource s) {
		assert(s != null);
        
        Statement hasState = s.getProperty(requestHasReservationState);
        if ((hasState != null) && (hasState.getResource() != null))
            return getStateAsString(hasState.getResource());
        return null;
	}
	
	/**
	 * get reservation notice of the resource
	 * @param s
	 * @return
	 */
	public static String getResourceReservationNotice(Resource s) {
		return getStringProperty(s, requestMessage);
	}
	
	/**
	 * Return resources that have this interface
	 * @param i
	 * @param m
	 * @return
	 */
	public static List<Resource> getWhoHasInterface(Resource i, OntModel m) {
		// FIXME: HACK - should be done via inference and inverse property;
		// query should be avoided
		String query = NdlCommons.createQueryStringWhoHasInterface(i);
		ResultSet rs = OntProcessor.rdfQuery(m, query);
		List<Resource> ret = new ArrayList<Resource>();
		while(rs.hasNext()) {
			ResultBinding result = (ResultBinding)rs.next();
			if (result != null) {
				Resource u = (Resource)result.get("item");
				if (u != null){
					ret.add(u);
				}
			}
		}
		if (ret.size() > 0)
			return ret;
		else
			return null;
	}
	
	/**
	 * Get all subjects connectedTo this one (or null)
	 * @param i
	 * @return
	 */
	public static List<Resource> getConnectedToInterfaces(Resource i) {
		List<Resource> ret = new ArrayList<Resource>();
		
		for (StmtIterator sti = i.listProperties(connectedTo); sti.hasNext();) {
			Resource ic = sti.next().getResource();
			ret.add(ic);
		}
		return (ret.size() > 0 ? ret : null);
	}
	
	/**
	 * If the resource hasUrn property, return its value, null otherwise
	 * @param i
	 * @return
	 */
	public static String getUrn(Resource i) {
		return getStringProperty(i, hasURNProperty);
	}
	
	public static String getEmail(Resource i) {
		return getStringProperty(i, hasEmailProperty);
	}
	
	public static String getSlicePassword(Resource i) {
		return getStringProperty(i, hasSlicePasswordProperty);
	}
	
	public static double getBitRate(Resource i) {
		if (i.hasProperty(layerHasBitRate, bitRate1G))
			return 1e9;
		if (i.hasProperty(layerHasBitRate, bitRate10G))
			return 1e10;
		return 0;
	}
	
	public static String getBitRateAsString(Resource i) {
		if (i.hasProperty(layerHasBitRate, bitRate1G))
			return "1000000000";
		if (i.hasProperty(layerHasBitRate, bitRate10G))
			return "10000000000";
		return null;
	}
	
	/**
	 * Get some string property of a resource
	 * @param r
	 * @param p
	 * @return
	 */
	protected static String getStringProperty(Resource r, Property p) {
		assert(r != null);	
		Statement s = r.getProperty(p);
		if (s != null) {
			String sc = s.getString();
			return (sc.length() > 0 ? sc : null);
		}
		return null;
	}
	
	/**
	 * get a size of something
	 * @param i
	 * @return
	 */
	public static int getSizeProperty(Resource i) {
		return getIntProperty(i, collectionSizeProperty);
	}
	
	/**
	 * Get a DN property of something
	 * @param i
	 * @return
	 */
	public static String getDNProperty(Resource i) {
		return getStringProperty(i, hasDNProperty);
	}
	
	/**
	 * Get hasURN property of something
	 * @param i
	 * @return
	 */
	public static String getHasURLProperty(Resource i) {
		return getStringProperty(i, topologyHasURL);
	}
	
	/**
	 * Nodes created from groups have this set to point to parent
	 * @param i
	 * @return
	 */
	public static String getRequestGroupURLProperty(Resource i) {
		return getStringProperty(i, hasRequestGroupURL);
	}
	
	/**
	 * get a Name property of something
	 * 
	 */
	public static String getNameProperty(Resource i) {
		return getStringProperty(i, topologyHasName);
	}
	
	public static String getGuidProperty(Resource i) {
		return getStringProperty(i, hasGUIDProperty);
	}
	
	public static String getGeniSliceStateName(Resource i) {
		Statement st = i.getProperty(hasSliceGeniState);
		
		if (st == null)
			return "unknown";
		
		Resource r = st.getResource();
		
		if (r.equals(geniSliceStateConfiguring))
			return "configuring";
		if (r.equals(geniSliceStateFailed))
			return "failed";
		if (r.equals(geniSliceStateReady))
			return "ready";
		return "unknown";
	}
	
	/**
	 * Get an integer property or Integer.MIN_VALUE
	 * @param r
	 * @param p
	 * @return
	 */
	protected static int getIntProperty(Resource r, Property p) {
		assert(r != null);
		Statement s = r.getProperty(p);
		if (s != null) {
			int ret = s.getInt();
			return ret;
		}
		return Integer.MIN_VALUE;
	}
	
	/**
	 * get a floating point property or Float.MIN_VALUE
	 * @param r
	 * @param p
	 * @return
	 */
	protected static float getFloatProperty(Resource r, Property p) {
		assert(r != null);
		Statement s = r.getProperty(p);
		if (s != null) {
			float ret = s.getFloat();
			return ret;
		}
		return Float.MIN_VALUE;
	}
	
	/**
	 * get a literal property value
	 * @param r
	 * @param p
	 * @return
	 */
	protected static String getLiteralProperty(Resource r, Property p) {
		assert(r != null);
		Statement s = r.getProperty(p);
		if (s != null) {
			return s.getLiteral().toString();
		}
		return null;
	}
	
	/**
	 * Typically for the ads. If the resource is a domain and it has a aggregate manager, get the
	 * controller URLs as a list of strings
	 * @param d
	 */
	public static List<String> getDomainControllerUrls(Resource d) {
		// check type
		if (!hasResourceType(d, networkDomainOntClass))
			return null;
		
		Statement s1 = d.getProperty(domainHasServiceProperty);
		if (s1 == null)
			return null;
		
		Resource ns = s1.getResource();
		List<String> ret = new ArrayList<String>();
		for(StmtIterator si = ns.listProperties(domainHasAggregateManager); si.hasNext(); ) {
			Resource am = si.next().getResource();
			if (!hasResourceType(am, domainAggregateManagerClass)) {
				continue;
			}
			ret.add(getURL(am));
		}
		return ret;
	}
	
	/**
	 * Get the controller url for a manifest
	 * @param m
	 * @return
	 */
	public static String getManifestControllerUrl(Resource m) {
		
		if (!hasResourceType(m, manifestOntClass)) 
			return null;
		
		Statement s1 = m.getProperty(domainHasController);
		if (s1 == null)
			return null;
		
		Resource c = s1.getResource();
		
		Statement s2 = c.getProperty(hasURLProperty);
		if (s2 == null)
			return null;
		
		return s2.getLiteral().toString();	
	}
	
	/**
	 * get locations of a domain
	 * @param d
	 * @return
	 */
	public static List<Resource> getDomainLocations(Resource d) {

		List<Resource> ret = new ArrayList<Resource>();
		for(StmtIterator si = d.listProperties(collectionElementProperty); si.hasNext();) {
			Resource pop = si.next().getResource();
			if (pop.getProperty(locationLocatedAtProperty) != null) {
				Statement st = pop.getProperty(locationLocatedAtProperty);
				ret.add(st.getResource());
			}
		}
		// see if there is a simple located at on the domain itself
		if (d.getProperty(locationLocatedAtProperty) != null) {
			Statement st = d.getProperty(locationLocatedAtProperty);
			ret.add(st.getResource());
		}
		return ret;
	}
	
	/**
	 * get latitude of a location
	 * @param loc
	 * @return
	 */
	public static float getLat(Resource loc) {
		return getFloatProperty(loc, locationLatProperty);
	}
	
	/**
	 * get longitude of a location
	 * @param loc
	 * @return
	 */
	public static float getLon(Resource loc) {
		return getFloatProperty(loc, locationLongProperty);
	}
	
	
	/**
	 * Get linkTo resource of this resource
	 * @param r
	 * @return
	 */
	public static Resource getLinkTo(Resource r) {
		if (r == null)
			return null;
		Statement s = r.getProperty(linkTo);
		if (s != null) {
			return s.getResource();
		}
		return null;
	}

	/**
	 * Read a file into a string
	 * @param aFile
	 * @return
	 */
	public static String readFile(String aFile){
		BufferedReader bin = null; 
		StringBuilder sb = new StringBuilder();
		try {
			FileInputStream is = new FileInputStream(aFile);
			bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			
			String line = null;
			while((line = bin.readLine()) != null) {
				sb.append(line);
				sb.append(System.getProperty("line.separator"));
			}
			
			bin.close();
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		} 
		
		return sb.toString();
	}
	
	/**
	 * Get upper label of a resource 
	 * @param ls
	 * @return
	 */
	public static Label getUpperLabel(Resource lsr) {
		Label l = null;
		
		if (lsr.hasProperty(upperBound)) {
			Resource upperLabel = lsr.getProperty(upperBound).getResource();
			return parseLayerLabel(upperLabel);
		}
		return l;
	}
	
	/**
	 * Get lower label of a resource
	 * @param ls
	 * @return
	 */
	public static Label getLowerLabel(Resource lsr) {
		Label l = null;
				
		if (lsr.hasProperty(lowerBound)) {
			Resource lowerLabel = lsr.getProperty(lowerBound).getResource();

			return parseLayerLabel(lowerLabel);
		}
		return l;
	}
	
	/**
	 * Get label_ID property attached to the resource
	 * @param r
	 * @return
	 */
	public static String getLabelID(Resource r) {
		String ret = null;
		
		if (r.hasProperty(layerLabelIdProperty)) {
			ret = r.getProperty(layerLabelIdProperty).getString();
		}
		return ret;
	}
	
	/**
	 * Get the literal behind 
	 * resource layer:Label layer:label_ID literal
	 * @param r
	 * @return
	 */
	public static String getLayerLabelLiteral(Resource r) {
		String ret = null;
		if (r.hasProperty(layerLabel)) {
			Resource label = r.getProperty(layerLabel).getResource();
			if (label.hasProperty(layerLabelIdProperty)) {
				ret = label.getProperty(layerLabelIdProperty).getString();
			}
		}
		return ret;
	}
	
	/**
	 * parse out a layer.owl#Label
	 * @param r
	 * @return
	 */
	public static Label parseLayerLabel(Resource r) {
		Label l = null;
		// FIXME: (02/12) for now type is always VLAN and there are
		// duplicate declarations of VLAN in domain.owl and ethernet.owl
		// The one in domain.owl should go away and the same type should be used
		// to declare LabelSet hasResourceType and Label rdf:type
		if (hasResourceType(r, labelOntClass)) {
			if (r.hasProperty(layerLabelIdProperty)) {
				Float lid = r.getProperty(layerLabelIdProperty).getFloat();
				l = new Label(r.getLocalName(), lid, DomainResourceType.VLAN_RESOURCE_TYPE);
			}
		}
		return l;
	}
	
	/**
	 * Parse out a label, but don't assume the type is specified
	 * @param r
	 * @return
	 */
	public static Label parseLayerLabel_1(Resource r) {
		Label l = null;
		if (r.hasProperty(layerLabelIdProperty)) {
			Float lid = r.getProperty(layerLabelIdProperty).getFloat();
			l = new Label(r.getLocalName(), lid, DomainResourceType.VLAN_RESOURCE_TYPE);
		}
		return l;
	}
	
    /**
     * Get a ResultSet from collection:element query (upper, lower bounds and label IDs)
     * @param set URI
     * @param ontModel
     * @return
     */
    public static ResultSet getLabelSet(String set, OntModel ontModel){
    	ResultSet results = null;

        String s = "SELECT ?lv ?uv ?l ?u ?b ";
        String f = "";
        String w = "WHERE {" +  
        		"<" + set + "> " + " collections:element ?b. " + 
        		"OPTIONAL{"+
        			"?b layer:upperBound ?u. " + 
        			"?u layer:label_ID ?uv. " + 
        			"?b layer:lowerBound ?l. " + 
        			"?l layer:label_ID ?lv." +
        		"}."+
        "}"+
        "ORDER BY ASC(?lv)";
        String queryPhrase = createQueryString(s, f, w);

        results = rdfQuery(ontModel, queryPhrase);

        return results;
    } 
	
    /**
     * Get a list of labelsets from a set URI
     * @param set URI
     * @param rType resource type
     * @param ontModel
     * @return
     */
    public static LinkedList <LabelSet> getLabelSet(String set, String rType, OntModel ontModel) {
    	
    	ResultSet results = getLabelSet(set,ontModel);
    	
		int lower=0;
		int upper=0;
		String lowerBound=(String) results.getResultVars().get(0);
		String upperBound=(String) results.getResultVars().get(1);
		String l=(String) results.getResultVars().get(2);
		String u=(String) results.getResultVars().get(3);
		String setElement = (String) results.getResultVars().get(4);
		 
		Resource labelRange_rs=null;
		QuerySolution solution=null;
		if (!results.hasNext()){
			return null;
		}
		LinkedList <LabelSet> lSetList = new LinkedList <LabelSet>();		
		Label lowLabel=null, upLabel=null;
		while (results.hasNext()){
			solution=results.nextSolution();
			labelRange_rs=solution.getResource(setElement);
			Resource lowerLabel=null,upperLabel=null;
			if(solution.getLiteral(lowerBound) != null){
				lower=solution.getLiteral(lowerBound).getInt();
				upper=solution.getLiteral(upperBound).getInt();
				lowerLabel=solution.getResource(l);
				upperLabel=solution.getResource(u);
			}
			else{
				lowerLabel=labelRange_rs;
				lower = lowerLabel.getProperty(NdlCommons.layerLabelIdProperty).getInt();
			}
			lowLabel = new Label(ontModel.getOntResource(lowerLabel),lower,rType); 
			lowLabel.swap = swapLabel(lowerLabel);
			if(upperLabel!=null){
				upLabel = new Label(ontModel.getOntResource(upperLabel),upper,rType);
				upLabel.swap = swapLabel(upperLabel);
			}
			LabelSet labelSet=new LabelSet(lowLabel,upLabel,labelRange_rs);
			lSetList.add(labelSet);
		}
		return lSetList;
    }
    
    public static Float swapLabel(Resource l_rs){
    	int s_id = 0;
    	if(l_rs.hasProperty(NdlCommons.layerSwapLabelProperty)){
    		Resource s_rs = l_rs.getProperty(NdlCommons.layerSwapLabelProperty).getResource();
    		s_id = s_rs.getProperty(NdlCommons.layerLabelIdProperty).getInt();
    	}
    	Float ret = s_id + 0f;
    	return ret;
    }
    
    /*
     * Color extensions support
     */
    
    /**
     * Get key/value attributes from a color node as a single map. Returns
     * non-null (possibly empty) map
     * @param color
     * @return
     */
    public static Map<String, String> getColorKeys(Resource color) {
    	assert(color != null);
    	Map<String, String > ret = new HashMap<String, String>();
    	// collect all color attributes
    	for (StmtIterator si = color.listProperties(hasColorAttribute); si.hasNext();) {
    		Resource ca = si.next().getResource();
    		if (ca == null)
    			continue;
    		Statement sk = ca.getProperty(hasColorKey);
    		Statement sv = ca.getProperty(hasColorValue);
    		if ((sk == null) || (sv == null))
    			continue;
    		ret.put(sk.getLiteral().getString(), sv.getLiteral().getString());
    	}
    	return ret;
    }
    
    /**
     * get the text blob on color. Returns null or string
     * @param color
     * @return
     */
    public static String getColorBlob(Resource color) {
    	assert(color != null);
    	
    	Statement bl = color.getProperty(hasColorBlob);
    	if (bl != null)
    		return bl.getLiteral().getString();
    	return null;
    }
    
    /**
     * get the XML blob on color. Returns null or string 
     * @param color
     * @return
     */
    public static String getColorBlobXML(Resource color, boolean compressed) {
    	assert(color != null);
    	
    	if (compressed) {
    		Statement xbl = color.getProperty(hasColorXMLCompressedBlob);
    		if (xbl != null) {
    			return NdlGenerator.decodeDecompress(xbl.getLiteral().getString());
    		}
    	}
    	else {
    		Statement xbl = color.getProperty(hasColorXMLBlob);
    		if (xbl != null)
    			return xbl.getLiteral().getString();
    	}
    	
    	return null;
    }
    
    /*
     * query-like functions
     */
    
    /**
     * Is this a LinkConnection?
     * @param r
     * @return
     */
    public static boolean isLinkConnection(Resource r) {
    	return r.hasProperty(RDF_TYPE, topologyLinkConnectionClass);
    }
    
	/**
	 * Is this a baremetal or VM?
	 * @param r
	 * @return
	 */
	public static boolean isBareMetal(Resource r) {
		if (r.hasProperty(domainHasResourceTypeProperty)) {
			Statement st = r.getProperty(domainHasResourceTypeProperty);
			if (st.getResource().equals(bmResourceTypeClass) || (st.getResource().equals(fourtygbmResourceTypeClass))) 
				return true;
		}
		return false;
	}
	/**
	 * Is this an *ISCSI* networkStorage node?
	 * @param r
	 * @return
	 */
	public static boolean isISCSINetworkStorage(Resource r) {
		if (!isNetworkStorage(r))
			return false;
		if (r.hasProperty(domainHasResourceTypeProperty)) {
			Statement st = r.getProperty(domainHasResourceTypeProperty);
			if (st.getResource().equals(lunResourceTypeClass)) 
				return true;
		}
		return false;
	}
	
	public static boolean isNetworkStorage(Resource r) {
		return r.hasProperty(RDF_TYPE, networkStorageClass);
	}

	/**
	 * Get peers across the connection or their interfaces. Note that the number of interfaces
	 * returned and the number of nodes may not be the same, as a node may have multiple interfaces
	 * to the link shared with st
	 * @param st
	 * @param peers
	 * @return
	 */
	public static Set<Resource> getPeersOrInterfaces(Resource st, boolean peers) {
		String query = NdlCommons.createQueryConnectionPeers(st);
		
		ResultSet rs = NdlCommons.rdfQuery((OntModel)st.getModel(), query);
		Set<Resource> storagePeers = new HashSet<Resource>();
		while(rs.hasNext()) {
			ResultBinding result = (ResultBinding)rs.next();
			// repeats are possible, because each peer may have multiple interfaces to the common link
			if (peers)
				storagePeers.add(result.getResource("peer"));
			else
				storagePeers.add(result.getResource("interface"));
		}
		return storagePeers;
	}
	
	/**
	 * Is this a multicast/broadcast DEVICE??
	 * @param r - resource in question
	 */
	public static boolean isMulticastDevice(Resource r) {
		if (!r.hasProperty(RDF_TYPE, deviceOntClass))
			return false;
		if (r.hasProperty(hasCastType)) {
			Statement st = r.getProperty(hasCastType);
			if (st.getResource().equals(multicastOntClass))
				return true;
		} 
		return false;
	}
	
	/**
	 * Is this a stitching node?
	 * @param r
	 * @return
	 */
	public static boolean isStitchingNode(Resource r) {
		if (hasResourceType(r, deviceOntClass)) {
			Resource dom = getDomain(r);
			if(dom==null)
				return false;	
			if (dom.getURI().contains(stitching_domain_str))
				return true;
		}
		return false;
	}
	
	/* Is this a stitching node?
	 * @param r
	 * @return
	 */
	public static boolean isStitchingNodeInManifest(Resource r) {
		if(r==null)
			return false;
		if (hasResourceType(r, deviceOntClass)) {
			Resource dom = getDomain(r);
			if(dom==null)
				return false;	

			if(dom.getURI().contains(stitching_domain_str))
				return true;
		}
		return false;
	}	
	
	/**
	 * Get the specific type of baremetal node for this resource
	 * @param r
	 * @return
	 */
	public static String getBareMetalType(Resource r) {
		if (!isBareMetal(r))
			return null;
		
		Resource spec = getSpecificCE(r);
		
		if (spec != null)
			return spec.getLocalName();
		
		return null;
	}
	
	/**
	 * Get a string corresponding to VM size in EC2 nomenclature (so EucaM1Small and EC2M1Small will both
	 * be m1.small)
	 * @param r
	 * @return
	 */
	public static String getEC2VMSize(Resource r) {
		
		Resource spec = getSpecificCE(r);
		
		if (spec == null)
			return null;
		
		String indName = spec.getLocalName();
		
		if (indName == null)
			return null;
		
		String prefix = null;
		if (indName.startsWith("EC2")) 
			prefix = "EC2";
		else if (indName.startsWith("Euca"))
			prefix = "Euca";
		else if (indName.startsWith("XO"))
			prefix = "XO";
		// unknown prefix
		if (prefix == null)
			return null;
		if(!prefix.equals("XO")){
			indName = indName.replaceFirst(prefix, "").toLowerCase();
			// m1small to m1.small
			indName = indName.replaceFirst("1", "1.");
		}else{
			indName = indName.replaceFirst(prefix, "xo.").toLowerCase();
		}
		return indName;
	}
	
	/**
	 * Get a preconfigured Logger for all things NDL
	 * @return
	 */
	public static Logger getNdlLogger() {
		return Logger.getLogger(NDL_LOGGER);
	}
	
	/**
	 * Define useful namespaces
	 * @param model
	 */
	public static void setPrefix(OntModel model)
	{
		
	    model.setNsPrefix("dtn", ORCA_NS + "dtn.owl#");
	    model.setNsPrefix("ethernet", ORCA_NS + "ethernet.owl#");
	    model.setNsPrefix("topology", ORCA_NS + "topology.owl#");
	    model.setNsPrefix("collections", ORCA_NS + "collections.owl#");
	    model.setNsPrefix("layer", ORCA_NS + "layer.owl#");
	    model.setNsPrefix("ethernet", ORCA_NS + "ethernet.owl#");
	    model.setNsPrefix("request-schema", ORCA_NS + "request.owl#");
	    model.setNsPrefix("ip4", ORCA_NS + "ip4.owl#");
	    model.setNsPrefix("compute", ORCA_NS + "compute.owl#");
	    model.setNsPrefix("exogeni", ORCA_NS + "exogeni.owl#");
	    model.setNsPrefix("orca", ORCA_NS + "orca.rdf#");
	    model.setNsPrefix("domain", ORCA_NS + "domain.owl#");
	    model.setNsPrefix("eucalyptus", ORCA_NS + "eucalyptus.owl#");
	    model.setNsPrefix("ec2", ORCA_NS + "ec2.owl#" );
	    model.setNsPrefix("kansei", ORCA_NS + "kansei.owl#");
	    model.setNsPrefix("planetlab", ORCA_NS + "planetlab.owl#");
	    model.setNsPrefix("openflow", ORCA_NS + "openflow.owl#");
	    model.setNsPrefix("geni", ORCA_NS + "geni.owl#");
	    model.setNsPrefix("modify-schema", ORCA_NS + "modify.owl#");
	    model.setNsPrefix("app-color",  ORCA_NS + "app-color.owl#");
	}
	
	public static String getBigString(int size){
		  SecureRandom random = new SecureRandom();
		  return new BigInteger(size, random).toString(32);
	}
	
	/**
	 * Initialize Ndl Commons
	 */
	public static void init() {
		setGlobalJenaRedirections();
		ModelFolders.getInstance();
	}
}
