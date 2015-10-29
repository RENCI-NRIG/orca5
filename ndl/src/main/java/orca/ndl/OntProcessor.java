package orca.ndl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Queue;

import orca.ndl.elements.Device;
import orca.ndl.elements.DomainElement;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.util.persistence.Persistable;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class OntProcessor extends NdlCommons implements LayerConstant
{
	// main namespace prefix for NDL-OWL definitions
	
    public String inputFileName;
    public String outputFileName;
    protected OntModel ontModel;

    protected Logger logger=NdlCommons.getNdlLogger();
    
    public OntProcessor(OntModel model)
    {
        ontModel = model;
    }

    public OntProcessor(String fileName) throws IOException
    {
        setInputFileName(fileName);
        ontModel = ontCreate(fileName);
        // OntModel model=ontCreate(fileName);
        // ontModel =
        // ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF,model);
    }

    public OntProcessor(InputStream stream) throws IOException
    {
        ontModel = ontCreate(stream);
        // OntModel model=ontCreate(fileName);
        // ontModel =
        // ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF,model);
        
    }
    
    public OntProcessor()
    {
    }
    
    // static code
    public static OntModel ontCreate(InputStream stream) throws IOException
    {
        OntModel model_base = ModelFactory.createOntologyModel();
        model_base.read(stream, "");
        stream.close();
        return model_base;
    }

    public static OntModel ontCreate(String aFile) throws IOException
    {
        OntModel model_base = ModelFactory.createOntologyModel();

        InputStream in = FileManager.get().openNoMap(aFile);
        if (in == null) {
            throw new IllegalArgumentException("File: " + aFile + " not found");
        }
        OntDocumentManager dm = model_base.getDocumentManager();

        dm.setProcessImports(true);
        // read the RDF/XML file
        model_base.read(in, "");
        
        model_base.setDynamicImports(true);
        /*List rules = Rule.rulesFromURL("file:orca/network/schema/ben.rule");
    	Reasoner reasoner = new GenericRuleReasoner(rules);
    	reasoner.setDerivationLogging(true);
        InfModel infModel = ModelFactory.createInfModel(reasoner,model_base);
        
        FileOutputStream out = new FileOutputStream("ben-inf.rdf");
        if (out == null) {
            throw new IllegalArgumentException("File: " + aFile + " not found");
        }
        infModel.write(out);
        */
        in.close();
        return model_base;
    }

    /*public OntModel ontCreate(String aFile, boolean importFlag) throws IOException
    {

        OntModel model_base = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        InputStream in = FileManager.get().openNoMap(aFile);
        if (in == null) {
            throw new IllegalArgumentException("File: " + aFile + " not found");
        }
        OntDocumentManager dm = model_base.getDocumentManager();

        dm.setProcessImports(importFlag);

        model_base.read(in, "");
        in.close();
        return model_base;
    }*/

    public void ontWrite(String aFile, OntModel ontModel) throws IOException
    {

        FileOutputStream out = new FileOutputStream(aFile);
        if (out == null) {
            throw new IllegalArgumentException("File: " + aFile + " not found");
        }
        ontModel.write(out,"RDF/XML");
        
        // write the RDF/XML file
        // ontModel.write(out, "");

        //BufferedWriter ou = new BufferedWriter(out);
        //ou.write("Hello Java");
        //ou.close();
        // Close the output stream
        out.flush();
        out.close();

    }

    public String deviceType(Resource rs_device)
    {

        OntResource rs_ont = ontModel.getOntResource(rs_device);
        String type = rs_ont.getLabel(null);

        // System.out.println(rs_ont.getURI()+":"+type);

        return type;
    }


    public int numResource(OntModel model, String queryPhrase)
    {
        ResultSet results = rdfQuery(model, queryPhrase);
        int size = 0;
        Binding binding = results.nextBinding();

        while (results.hasNext()) {
            size++;
            binding = results.nextBinding();
        }
        return size;
    }

    public boolean existResourceNode(Node nd, OntModel model, String queryPhrase)
    {
        Resource rs = model.createResource(nd.getURI());

        return existResource(rs, model, queryPhrase);
    }

    public boolean existResourceStr(String url, OntModel model, String queryPhrase)
    {

        Resource rs = model.createResource(url);

        return existResource(rs, model, queryPhrase);
    }

    public boolean existResource(Resource rs, OntModel model, String queryPhrase)
    {

        ResultSet results = rdfQuery(model, queryPhrase);

        return existResource(rs, results);
    }

    public boolean existResource(Resource rs, ResultSet results)
    {
        boolean exist = false;

        Resource resource = null;
        String varName = (String) results.getResultVars().get(0);

        QuerySolution solution;
        //System.out.println(rs+"\n");
        while (results.hasNext()) {
            solution = results.nextSolution();
            resource = solution.getResource(varName);
            //System.out.println(resource);
            if (resource.equals(rs)) {
                exist = true;
                break;
            }
        }
        // System.out.println(exist);
        return exist;
    }

    public boolean removeFromImport(Resource availableSet_rs,Property p, Resource labelRange_rs){
    	boolean removed=false;
    	if(labelRange_rs == null)
    		return false;
    	logger.info("Num of Sub models:"+ontModel.countSubModels()+"\n");
    	ExtendedIterator <OntModel> subList = ontModel.listSubModels(true);
    	OntModel subModel=null;
    	OntResource set_ont = null;
	while(subList.hasNext()){
    		subModel = subList.next();
    		if(subModel.isInBaseModel(availableSet_rs)){
    			
    			set_ont = subModel.getOntResource(availableSet_rs);
    			labelRange_rs=subModel.getOntResource(labelRange_rs);
		
    			set_ont.removeProperty(p, labelRange_rs);
    			removed=true;
    			break;
    		}
    	}
    	set_ont = ontModel.getOntResource(availableSet_rs);
	labelRange_rs=ontModel.getOntResource(labelRange_rs);
	set_ont.removeProperty(p, labelRange_rs);	
    	return removed;
    }    
    
    // update the data range with the new lowerBound
    public Resource ontLabelUpdate(Resource availableSet_rs, Resource labelRange_rs, int low, int upper, Resource lowerLabel, Resource upperLabel,int stackLabel)
    {
    	Resource pick=null;
    	if( (labelRange_rs==null) || (availableSet_rs==null) )
    		return null;
    	logger.info("labelRange_rs:"+labelRange_rs.getURI()+":"+ontModel.isInBaseModel(availableSet_rs));
    	//if low == upper, i.e. the range shrinked to one label
    	// or it is a single label
    	if(availableSet_rs!=null){
    		if((low==upper) || (upperLabel==null)){
    			//for the one element, it is the same to pick the label locally and use the stack label
    			if(labelRange_rs!=null){
    				/*if(ontModel.isInBaseModel(availableSet_rs)){
    					OntResource set_ont = ontModel.getOntResource(availableSet_rs);
    					labelRange_rs=ontModel.getOntResource(labelRange_rs);
    					set_ont.removeProperty(NdlCommons.collectionElementProperty, labelRange_rs);
    				}
    				else{*/
    					removeFromImport(availableSet_rs,NdlCommons.collectionElementProperty,labelRange_rs);
    				//}
    			}
    			pick=lowerLabel;
    			logger.debug("Label removed:"+availableSet_rs+":"+NdlCommons.collectionElementProperty+":"+labelRange_rs+"\n"+availableSet_rs.hasProperty(NdlCommons.collectionElementProperty,labelRange_rs));
    		}
    		else{ //for the range case	
    			if((stackLabel==0) || (low==stackLabel)){ //not stack label or the stacked one equals the lower bound, pick locally, increase the lowBound by 1
    				changeAvailableLabelSetLowerBound(labelRange_rs, low);
    				pick=lowerLabel;
    			}
    			else{//split the range to 2 ranges and return the stackLabel
    				pick=changeAvailabelLabelSet(availableSet_rs, labelRange_rs,lowerLabel, upperLabel,stackLabel);
    			}
    		}
    	}
    	return pick;
    }
    
    public Resource changeAvailabelLabelSet(Resource availableSet_rs, Resource labelRange_rs,Resource lowerLabel, Resource upperLabel,int stackLabel){
    	//1. remove the label range
    	/*if(ontModel.isInBaseModel(availableSet_rs)){
			OntResource set_ont = ontModel.getOntResource(availableSet_rs);
			labelRange_rs=ontModel.getOntResource(labelRange_rs);
			set_ont.removeProperty(NdlCommons.collectionElementProperty, labelRange_rs);
		}
		else{*/
			removeFromImport(availableSet_rs,NdlCommons.collectionElementProperty,labelRange_rs);
		//}
    	
        String rangeLabel = null;
        String labelStr = null;
    	String labelRange_uri=labelRange_rs.getURI();
    	if(labelRange_rs.getProperty(NdlCommons.RDFS_Label)!=null){
        	rangeLabel = labelRange_rs.getProperty(NdlCommons.RDFS_Label).getString();
        }
    	
    	//2. form the 1st range: lowerLabel - new_upper
    	String new_upper =String.valueOf(stackLabel-1);
    	String new_upper_uri=lowerLabel.getURI().replaceAll("\\d+$", new_upper);
        
    	if(rangeLabel!=null){
        	labelStr=rangeLabel.concat(new_upper);
        }
        else
        	labelStr=new_upper;
        
        OntResource rs_new_upper= ontModel.createOntResource(new_upper_uri);
        rs_new_upper.addRDFType(NdlCommons.labelOntClass);
        rs_new_upper.addProperty(NdlCommons.layerLabelIdProperty, new_upper, XSDDatatype.XSDfloat);
        rs_new_upper.addProperty(NdlCommons.RDFS_Label, labelStr);
        
        String rs_new_range_uri=labelRange_uri.replaceAll("\\d+$", new_upper);
        OntResource rs_new_range= newLabelRange(rs_new_range_uri,labelStr, lowerLabel, rs_new_upper);
        availableSet_rs.addProperty(NdlCommons.collectionElementProperty, rs_new_range);
        
        
    	//3. form the 2nd range: new_lower - upperLabel
    	String new_lower =String.valueOf(stackLabel+1);
    	String new_lower_uri=lowerLabel.getURI().replaceAll("\\d+$", new_lower);
    	if(rangeLabel!=null){
        	labelStr=rangeLabel.concat(new_lower);
        }
        else
        	labelStr=new_lower;
 
        OntResource rs_new_lower= ontModel.createOntResource(new_lower_uri);
        rs_new_lower.addRDFType(NdlCommons.labelOntClass);
        rs_new_lower.addProperty(NdlCommons.layerLabelIdProperty, new_lower, XSDDatatype.XSDfloat);
        rs_new_lower.addProperty(NdlCommons.RDFS_Label, labelStr);
        
        rs_new_range_uri=labelRange_uri.replaceAll("\\d+$", new_lower);
        rs_new_range= newLabelRange(rs_new_range_uri,labelStr, rs_new_lower, upperLabel);
        availableSet_rs.addProperty(NdlCommons.collectionElementProperty, rs_new_range);
    	
    	//4. return the stackLabel resource
    	String pick_str =String.valueOf(stackLabel);
    	String pick_uri=lowerLabel.getURI().replaceAll("\\d+$", pick_str);
    	if(rangeLabel!=null){
        	labelStr=rangeLabel.concat(pick_str);
        }
        else
        	labelStr=pick_str;
 
        OntResource rs_pick= ontModel.createOntResource(pick_uri);
        rs_pick.addRDFType(NdlCommons.labelOntClass);
        rs_pick.addProperty(NdlCommons.layerLabelIdProperty, pick_str, XSDDatatype.XSDfloat);
        rs_pick.addProperty(NdlCommons.RDFS_Label, labelStr);       
        
    	return rs_pick;
    }
    
    public OntResource newLabelRange(String rs_new_range_uri,String rangeLabel,Resource lowerLabel, Resource upperLabel){
    	OntResource rs_new_range= ontModel.createIndividual(rs_new_range_uri,NdlCommons.labelRangeOntClass);
        rs_new_range.addRDFType(NdlCommons.labelRangeOntClass);
        rs_new_range.addProperty(NdlCommons.RDFS_Label, rangeLabel);
        rs_new_range.addProperty(NdlCommons.lowerBound,lowerLabel);
        rs_new_range.addProperty(NdlCommons.upperBound,upperLabel);
        
    	return rs_new_range;
    }
    
    public void changeAvailableLabelSetLowerBound(Resource labelRange_rs, float low)
    {
    	if(labelRange_rs==null)
    		return;
    	
    	OntResource labelRange_rs_on=ontModel.getOntResource(labelRange_rs);
    	String lower = new String();
		lower = String.valueOf((int) low +1);
        Resource usedLabel_rs = labelRange_rs.getProperty(NdlCommons.lowerBound).getResource();
        String lowerURI=usedLabel_rs.getURI().replaceAll("\\d+$", lower);
        
        Resource lb=null;
        
        if(labelRange_rs.getProperty(NdlCommons.lowerBound)!=null){
        	lb = labelRange_rs.getProperty(NdlCommons.lowerBound).getResource();
        	labelRange_rs_on.removeProperty(NdlCommons.lowerBound,lb);
        	removeFromImport(labelRange_rs,NdlCommons.lowerBound,lb);
        }
        //*if(ontModel.isInBaseModel(labelRange_rs)){
        //	labelRange_rs_on.removeProperty(NdlCommons.lowerBound,lb);
        //}
        //else{*/
        	//removeFromImport(labelRange_rs,NdlCommons.lowerBound,labelRange_rs.getProperty(NdlCommons.lowerBound).getResource());
       // }
        logger.info("Lower bound removed:"+labelRange_rs+":"+NdlCommons.lowerBound + ":" + labelRange_rs.hasProperty(NdlCommons.lowerBound,lb));
        
        String rangeLabel = null;
        String labelStr = null,usedLabelStr=null;
        if(labelRange_rs.getProperty(NdlCommons.RDFS_Label)!=null){
        	rangeLabel = labelRange_rs.getProperty(NdlCommons.RDFS_Label).getLiteral().toString();
        	labelStr=rangeLabel.concat(String.valueOf(lower));
        	usedLabelStr=rangeLabel.concat(String.valueOf((int)low));
        }
        else
        	labelStr=String.valueOf(lower);
        
        OntResource rs_lower = ontModel.createIndividual(lowerURI,NdlCommons.labelOntClass);
       
        rs_lower.addRDFType(NdlCommons.labelOntClass);
        rs_lower.addProperty(NdlCommons.layerLabelIdProperty, lower, XSDDatatype.XSDfloat);
        rs_lower.addProperty(NdlCommons.RDFS_Label, labelStr);
        if(usedLabelStr!=null)
        	usedLabel_rs.addProperty(NdlCommons.RDFS_Label, usedLabelStr);
        
        labelRange_rs.addProperty(NdlCommons.lowerBound, rs_lower);
        logger.info("------New LabelSet----" + labelRange_rs + ":" + labelRange_rs.getProperty(NdlCommons.lowerBound).getResource()+":"+labelStr+":"+labelRange_rs.getProperty(NdlCommons.upperBound).getResource());
    }
    
    public Resource getLabelResource(Resource rs, float label)
    {
    	if(rs==null) return null;
    	String labelstr=String.valueOf((int) label);
    	String uri=rs.getURI();
        String URI = uri.replaceAll("\\d+$", labelstr);
        Resource label_rs = ontModel.createIndividual(URI,NdlCommons.labelOntClass);
        label_rs.addProperty(NdlCommons.layerLabelIdProperty, String.valueOf((int)label), XSDDatatype.XSDfloat);
        return label_rs;
    }

    // get availableLable Set: rsURI=intf.getURI()
    public ResultSet getAvailableLabelSet(String rsURI, String availableLableSet_str){
	    return getAvailableLabelSet(ontModel, rsURI, availableLableSet_str);
    }
    
    public void setUsedLabelSet(String rsURI, String usedLabelSet_str,String usedSet_str,Resource usedSet_rs, Resource label)
    {
        Property usedLabelSet=ontModel.createProperty(usedLabelSet_str);
        Resource rs=ontModel.getResource(rsURI);
        if (usedSet_rs==null) {
            usedSet_rs = ontModel.createResource(usedSet_str,NdlCommons.labelSetOntClass);
            rs.addProperty(usedLabelSet, usedSet_rs);
        } 

        if (label != null)
            usedSet_rs.addProperty(NdlCommons.collectionElementProperty, label);
        logger.info("---Used label added:" + rsURI +":"+usedLabelSet+":"+usedSet_rs.hasProperty(NdlCommons.collectionElementProperty,label)+":"+label);
    }
    
    //get the usedLabelSet of a Interface: rsURI=intf.getURI()
    public  ResultSet getUsedLabelSet(String rsURI, String usedLabelSet){
    	ResultSet results;
    	
    	String s = "SELECT ?b ?r ";
        String f = "";
        String w = "WHERE {" + "<" + rsURI + "> " + usedLabelSet + " ?r." + "?r collections:element ?b. " + "}";
        String queryPhrase = createQueryString(s, f, w);

        results = rdfQuery(ontModel, queryPhrase);

        return results;
    }  
    
    // get layer:Layer
    public ResultSet getCRSDevice(String rs)
    {

        String s = "SELECT ?r ?intf ";
        String f = "";
        String w = "WHERE {" + "<" + rs + ">" + " ndl:hasInterface ?intf." + 
        							"?intf ndl:interfaceOf ?r." + 
        							" ?r rdf:type ndl:Device." + "      }";
        String queryPhrase = createQueryString(s, f, w);

        ResultSet results = rdfQuery(ontModel, queryPhrase);
    
        return results;
    }

    public ResultSet getSwitchedToInterface(String rsURI)
    {

        String selectStr = "SELECT ?intf ?intf_peer ?c ";
        String fromStr = "";
        String whereStr = "WHERE {" +
        	// "?p a layer:AdaptationProperty. "+
        	"<" + rsURI + ">" + " ndl:hasInterface ?intf. " + 
        	"{?intf ndl:connectedTo ?intf_peer} UNION {?intf ndl:linkTo ?intf_peer}." +
        	// "?intf ?p ?a."+
        	"?intf_peer ndl:inConnection true." + 
        	"?intf_peer ?l ?r." + 
        	"?r rdf:type layer:Layer." + 
        	"?intf_peer ndl:interfaceOf ?b." + 
        	"?b ndl:hasSwitchMatrix ?sw. " + 
        	"?sw layer:switchingCapability ?c" + 
        "      }";

        String queryPhrase = createQueryString(selectStr, fromStr, whereStr);

        ResultSet results = rdfQuery(ontModel, queryPhrase);

        return results;

    }

    public ResultSet getSwitchedToAdaptation(String rsURI)
    {
        String selectStr = "SELECT ?intf ?intf_peer ?c ";
        String fromStr = "";
        String whereStr = "WHERE {" + "?p a layer:AdaptationProperty. " + 
        					"<" + rsURI + ">" + " ndl:hasInterface ?intf. " + 
        					"{?intf ndl:connectedTo ?intf_peer} UNION {?intf ndl:linkTo ?intf_peer}." + "?intf ?p ?a." + 
        					//"?intf_peer ndl:inConnection true." + 
        					"?intf_peer ?l ?r." + "?r rdf:type layer:Layer." + 
        					"?intf_peer ndl:interfaceOf ?b." + "?b ndl:hasSwitchMatrix ?sw. " + "?sw layer:switchingCapability ?c" + 
        "      }";

        String queryPhrase = createQueryString(selectStr, fromStr, whereStr);

        ResultSet results = rdfQuery(ontModel, queryPhrase);

        return results;

    }

    public ResultSet getConnectionSubGraphSwitchedTo(String url, String url2)
    {

        String s = "SELECT ?a ?b ?c ";
        String f = "";
        String w = "WHERE {" + "(<" + url + "> '([ndl:connectedTo]/[ndl:switchedTo]?/[ndl:connectedTo]*)+ ' <" + 
        	url2 + ">) gleen:Subgraph (?a ?b ?c)." + 
        "      }  ";

        String queryPhrase = createQueryString(s, f, w);

        // System.out.println(queryPhrase);

        ResultSet results = rdfQuery(ontModel, queryPhrase);

        // outputQueryResult(results);

        return results;
    }

    public ResultSet getConnectionSubGraphSwitchedToAdaptation(String url, String url2)
    {

        String s = "SELECT ?a ?b ?c ";
        String f = "";
        String w = "WHERE {" + 
        		"(<" + url + "> '([ndl:connectedTo]/[layer:adaptationProperty]+/[ndl:switchedTo]?/[layer:adaptationPropertyOf]*)+ ' <" + url2 + ">) " +
        		"gleen:Subgraph (?a ?b ?c)" + 
        		"      } ";

        String queryPhrase = createQueryString(s, f, w);

        // System.out.println(queryPhrase);

        ResultSet results = rdfQuery(ontModel, queryPhrase);

        //outputQueryResult(results);

        return results;
    }

    // Get the subgraph between two directly connected devices
   public ResultSet getSubGraphLinkToOri(String url1, String url2)
    {
        String s = "SELECT ?a ?b ?c ";
        String f = "";
        String w = "WHERE {" + "(<" + url1 + 
        		"> '[ndl:hasInterface]+/([ndl:hasInputInterface]|[ndl:hasOutputInterface])*/([ndl:linkTo]|[ndl:connectedTo])+/[ndl:interfaceOf]+' <" + 
        		url2 + ">) " +
        		"gleen:Subgraph (?a ?b ?c). ?a rdf:type ndl:Interface.?c rdf:type ndl:Interface." + 
        		"      }";

        String queryPhrase = createQueryString(s, f, w);

        ResultSet results = rdfQuery(ontModel, queryPhrase);
        
        return results;
    }

   public ResultSet getSubGraphLinkTo(String url1, String url2)
   {
       String s = "SELECT DISTINCT ?a ?c ";
       String f = "";
       String w = "WHERE {" + "(<" + url1 + 
       		"> '[ndl:hasInterface]+/([ndl:connectedTo]|[ndl:switchedTo]|[ndl:linkTo])*/[ndl:interfaceOf]+" +
       		"' <" + url2 + ">) " +
       		"gleen:Subgraph (?a ?b ?c). ?a rdf:type ndl:Interface.?c rdf:type ndl:Interface. "+
       		//"{{?c ndl:interfaceOf " + " <" + url2 + ">} UNION" +
       		//"{?a ndl:interfaceOf <" + url1 + "> }}"+
       		"      }";

       String queryPhrase = createQueryString(s, f, w);

       ResultSet results = rdfQuery(ontModel, queryPhrase);
       
       return results;
   }

    // Get the subgraph between two directly connected devices
    public ResultSet getSubGraphConnectedTo(String url1, String url2)
    {
        String s = "SELECT ?a ?b ?c ";
        String f = "";
        String w = "WHERE {" + "(<" + url1 + "> '[ndl:hasInterface]+/[ndl:connectedTo]+/[ndl:interfaceOf]+' <" + url2 + 
        ">) gleen:Subgraph (?a ?b ?c)" + "      }";
        String queryPhrase = createQueryString(s, f, w);

        // System.out.println(queryPhrase);

        ResultSet results = rdfQuery(ontModel, queryPhrase);

        // outputQueryResult(results);

        return results;
    }

    public String createQueryStringInterfaceOfDevice(String rs)
    {

        String s = "SELECT ?r ";
        String f = "";
        String w = "WHERE {" + 
        				"{?r ndl:hasInterface " + "<" + rs + ">." + 
        				" ?r rdf:type ndl:Device.}" + "UNION"+ 
        				"{ ?r ndl:hasInterface " + "<" + rs + ">." + 
        				" ?r rdf:type compute:ServerCloud.}" +   
        			"}";
        String queryPhrase = createQueryString(s, f, w);

        return queryPhrase;
    }
    
    public ResultSet getNetworkConnection(String rs1, String rs2)
    {
        String s = "SELECT ?r ";
        String f = "";
        String w = "WHERE {" + "?r ndl:hasInterface " + "<" + rs1 + ">." + 
        					"?r ndl:hasInterface " + "<" + rs2 + ">." + 
        					" ?r rdf:type " + "ndl:NetworkConnection" + 
        	"      }";
        String queryPhrase = createQueryString(s, f, w);

        ResultSet results = rdfQuery(ontModel, queryPhrase);

        return results;
    }

    public ResultSet interfaceOfNetworkConnection(String rs)
    {
        String onPath = "gleen:OnPath";
        String subPath = " ('[layer:adaptationPropertyOf]*'";

        String selectStr = "SELECT ?nc ";
        String fromStr = "";
        String whereStr = "WHERE {" + "<" + rs + "> " + onPath + subPath + " ?object" + ")." + 
        	"?nc ndl:hasInterface ?object." + "?nc rdf:type ndl:NetworkConnection" + "   }";

        String queryPhrase = createQueryString(selectStr, fromStr, whereStr);
        // System.out.println(queryPhrase);
        ResultSet results = rdfQuery(ontModel, queryPhrase);

        return results;
    }

    public String getInputFileName()
    {
        return inputFileName;
    }

    public void setOntModel(OntModel ontModel)
    {
        this.ontModel = ontModel;
    }

    public OntResource addResource(String URI)
    {
        return ontModel.createOntResource(URI);

    }

    public void addProperty(OntResource aResource, Property aProperty, RDFNode aObject)
    {
        aResource.addProperty(aProperty, aObject);
    }

    public void setInputFileName(String aName)
    {
        inputFileName = aName;
    }

    public OntModel getOntModel()
    {
        return ontModel;
    }

    // List all neighbors of rs1 with destination rs2 (possible A* algorithm)

    LinkedList<Resource> listConnectedDeviceSort(OntModel m, Resource rs1, Resource rs2)
    {

        LinkedList<Resource> resourceList = new LinkedList();

        String url1 = rs1.getURI();
        String url2 = rs2.getURI();
        
        ResultSet results = listConnectedDevice(url1, m);
        //System.out.println(url1+":"+url2+"\n");
        //outputQueryResult(results);
        //results=listConnectedDevice(url1,m);

        Resource rs = null;
        String var0 = (String) results.getResultVars().get(0);

        while (results.hasNext()) {
            rs = results.nextSolution().getResource(var0);
            if(!m.getOntResource(rs).hasRDFType(NdlCommons.deviceOntClass)) {
            	if(rs.getURI()!=rs2.getURI())
            		continue;
            }
            if (rs.getNameSpace().equals(rs1.getNameSpace()))
                continue;
            if (rs.getNameSpace().equals(rs2.getNameSpace()))
                resourceList.add(0, rs);
            else if (isSameLayerDevice(rs, rs1))
                resourceList.add(0, rs);
            else
                resourceList.add(rs);
        }
        
        return resourceList;
    }

    // List all directly connected devices to the URL
    public ResultSet listConnectedDevice(String deviceURL, OntModel model, String subPath)
    {
        String queryPhrase = createQueryStringOnPath(deviceURL, subPath);
     
        ResultSet results = rdfQuery(model, queryPhrase);

        return results;
    }

    // List all directly connected devices to the URL
    public ResultSet listConnectedDevice(String deviceURL, OntModel model)
    {
        String subPath = " ('[ndl:hasInterface]*/([ndl:connectedTo]|[ndl:switchedTo]|[ndl:linkTo])+/[ndl:interfaceOf]*'";

        return listConnectedDevice(deviceURL, model, subPath);
    }

    // find the layer:Layer
    public boolean isSameLayerDevice(Resource rs1, Resource rs2)
    {
        boolean same = false;

        StmtIterator stit1 = rs1.listProperties(NdlCommons.hasSwitchMatrix);
        StmtIterator stit2 = rs2.listProperties(NdlCommons.hasSwitchMatrix);
        Statement st1 = null;
        Statement st2 = null;
        Resource r1, r2;
        String cap1 = null;
        String cap2 = null;

        if (stit1 != null) {
            while (stit1.hasNext()) {
                st1 = stit1.nextStatement();
                r1 = st1.getResource();
                st1 = r1.getProperty(NdlCommons.switchingCapability);
                if (st1 != null)
                    cap1 = st1.getResource().getLocalName();
                if (cap1 == null)
                    continue;
                while (stit2.hasNext()) {
                    st2 = stit2.nextStatement();
                    r2 = st2.getResource();
                    st2 = r2.getProperty(NdlCommons.switchingCapability);
                    if (st2 != null)
                        cap2 = st2.getResource().getLocalName();
                    if (cap1.equals(cap2)) {
                        same = true;
                        break;
                    }
                }
                if (same)
                    break;

            }
        }

        return same;
    }

    // find the constrained shortest path between two resources using an heuristic based on the breadth first search
    // each path meeting the destination, check if it's valid:
    // 1. enough interface bandwidth to meet "bw"?
    // 2. maintain a layer adaptation stack for each path, check if it is empty at the destination.
    // 3. sort the neighbor list at every step: (1) same layer first;
    // 4. FIXME: label availability check, it is not easy in general case, may need to implement the k-shortest path.
   // PATH structure: ArrayList of (device, interface, bw) (in ArrayList)
    
    public ArrayList<ArrayList<OntResource>> findShortestPath(OntModel m, Resource start, Resource end,long bw,String rType1_str,String rType2_str,String nc_of_version)
    {
        Queue<ArrayList<Resource>> neighborQueue = new LinkedList<ArrayList<Resource>>();

        ArrayList<ArrayList<OntResource>> solution = null;
        
        HashSet<String> seen = new HashSet<String>();
        seen.add(start.getURI());

        LinkedList<Resource> resourceList = listConnectedDeviceSort(m, start, end);
        ListIterator<Resource> rlIterator = resourceList.listIterator();
        Resource rs = null;
        OntResource rs_ont = null;
        while (rlIterator.hasNext()) {
            rs = (Resource) rlIterator.next();
            rs_ont = m.getOntResource(rs);

            ArrayList<Resource> path = new ArrayList<Resource>();
            path.add(rs);
            
            neighborQueue.add(path);
        }

        ArrayList<Resource> candidate, expand;
        boolean valid;
        while (solution == null && !neighborQueue.isEmpty()) {
            candidate = neighborQueue.remove();
            valid=true;
            if (candidate.contains(end)) {
                
                solution = validPath(start,candidate,bw,rType1_str,rType2_str,nc_of_version);
            		
                if(solution!=null){	
                	logger.info("Found the path.\n");
                    break;
                }
                else{
                	logger.info("The path is not valid.\n");
                }
                
            } else {
                rs = candidate.get(candidate.size() - 1);
                seen.add(rs.getURI());
                resourceList = listConnectedDeviceSort(m, rs, end);
                rlIterator = resourceList.listIterator();

                while (rlIterator.hasNext()) {
                    rs = (Resource) rlIterator.next();

                    if (!seen.contains(rs.getURI())) {
                        expand = new ArrayList<Resource>(candidate);
                        //logger.debug("Is it in the queue?"+rs.getURI());
                        expand.add(rs);
                        neighborQueue.add(expand);
                    }
                }
            }
        }
        //Iterator it = seen.iterator();
        //while(it.hasNext()){
        //	logger.debug(it.next());
        //}
        
        return solution;
    }

    public ArrayList <ArrayList<OntResource>> validPath(Resource start,ArrayList<Resource> candidate, long bw,String rType1_str,String rType2_str,String nc_of_version){
    	boolean valid=true;
    	ArrayList <ArrayList<OntResource>> pathHopList = new ArrayList <ArrayList<OntResource>> ();
    	/*for (StmtIterator j=start.listProperties();j.hasNext();){
			System.out.println(j.next());
		}*/
    	//1. if the ethernet interface connecting an Ethernet switch to its neighbor has enough bandwidth
    	//2. for now, only check layer adaptation stack availability. 
    	//3. Label availability check is left to the site when provisioning.
    	Resource next=null;
    	OntResource rs_ont=null,intf_ont=null;
    	ArrayList <OntResource> pair=null;
    	int isEndPoint=0;
    	System.out.println("Candidate path length="+candidate.size());
    	for(int i=0;i<candidate.size();i++){
    		next=candidate.get(i);
    		if(!ontModel.getOntResource(next).hasRDFType(NdlCommons.deviceOntClass))
				next = (Resource) ontModel.getOntResource(next).getPropertyValue(NdlCommons.topologyInterfaceOfProperty);

    		if(!ontModel.getOntResource(start).hasRDFType(NdlCommons.deviceOntClass)){
    			start = (Resource) ontModel.getOntResource(start).getPropertyValue(NdlCommons.topologyInterfaceOfProperty);
    		}
    		if(i==0) isEndPoint = -1;
    		else if (i==candidate.size()-1) isEndPoint=1;
    		else isEndPoint=0;
    		ArrayList<ArrayList<OntResource>> intf_List = findInterface(start,next,rType1_str,rType2_str,isEndPoint,nc_of_version);
    		if(intf_List.size()<2){
    			System.out.println("Intf list size<2:start="+start.getURI()+";next="+next.getURI());
    			valid=false;
    			break;
    		}
    		for (int j=0;j<intf_List.size();j++){
    			
    			pair = intf_List.get(j);
    			rs_ont=pair.get(0);
            	intf_ont=pair.get(1);
            	
            	long intf_bw=checkBW(intf_ont,bw);
            	
            	pair.add(ontModel.createOntResource(String.valueOf(intf_bw)));
            	
            	if(intf_bw>0){
            		valid = bw-intf_bw <=0 ? true:false;
            	}
            	else{ //means non ethernet switch for now
            		valid=true; 
            	}
            	
            	if((bw!=0) && (!valid)) {
            		logger.error("Not enough bandwidth:"+intf_ont);
            		break;
            	}
            	
            	pathHopList.add(pair);
            }
    		if(!valid) break;
    		 		
    		start=next;
    	}
    	
    	if (valid) return pathHopList;
    	else return null;
    }
    
    public long minBW(ArrayList <ArrayList<OntResource>> path){

    	ArrayList <OntResource> pair=null;
    	pair=path.get(0);
    	long minBW=-1;
    	long bw;
    	for(int i=0;i<path.size();i++){
    		pair=path.get(i);
    		bw=Long.valueOf(pair.get(2).getURI());
    		if(bw>0){
    			if(minBW<0){
    				minBW=bw;
    			}
    			if(minBW>bw){
    				minBW=bw;
    			}
    		}
    	}
    	
    	return minBW;
    }
    
    public long checkBW(OntResource intf,long bw){
    	//ontModel.write(System.out,null);
    	//System.exit(0);
    	HashSet <Resource> sc_List = new HashSet <Resource> (); 
    	boolean valid = true;
    	
    	Resource eth = ontModel.createResource(ORCA_NS+"ethernet.owl#EthernetNetworkElement");
    	//check if it's a ethernet switch
    	OntResource device_rs=(OntResource) intf.getPropertyValue(NdlCommons.topologyInterfaceOfProperty);
    	Resource sm=null,sc=null;
    	for (StmtIterator j=device_rs.listProperties(NdlCommons.hasSwitchMatrix);j.hasNext();){
 			sm = j.next().getResource();
 			sc=sm.getProperty(switchingCapability).getResource();
 			sc_List.add(sc);
 			if(sc.equals(eth)) {
 				valid=false;
 				break;
 			}
        }
    	//not EthernetElement, no need to check bw, may need to check other metrics later..
    	if(valid==true) return -1;
    	
    	//check if the ethernet intf has enough bw.
    	Resource rs=null;
    	long intf_bw=0;
    	ResultSet results=null;
    	String varName=null;
    	while(true){		
			if(intf.hasProperty(RDF_TYPE,eth)){
				rs=intf;
				break;
			}
			
			results=getLayerAdapatation(intf.getURI());
			varName=(String) results.getResultVars().get(0);
			if (results.hasNext()){
				rs=results.nextSolution().getResource(varName);
			}
			
			if (rs!=null){
				if(rs.hasProperty(RDF_TYPE,eth)){
					intf = ontModel.getOntResource(rs);
					break;
				}
				else{
					intf = ontModel.getOntResource(rs);					
					continue;
				}
			}
			else 
				break;
		}
    	
		if(intf.getProperty(NdlCommons.layerBandwidthProperty)!=null) 
			intf_bw=intf.getProperty(NdlCommons.layerBandwidthProperty).getLiteral().getLong();

		valid = bw-intf_bw <=0 ? true:false;

    	return intf_bw;
    }
    // get the list of only device_OntResource from the path find results <device, interface>
    public LinkedList <OntResource> getDeviceListInPath(ArrayList<ArrayList<OntResource>> path){

    	LinkedList <OntResource> deviceList = null;
    	OntResource device_ont,intf_ont;
    	if(path!=null) {
			if (!path.isEmpty()) {
				ArrayList<OntResource> intf_List;	
				deviceList = new LinkedList <OntResource> ();
				for(int i=0;i<path.size();i++){
					intf_List=path.get(i);
					for (int j=0;j<intf_List.size();j++){
						device_ont=intf_List.get(0);
						intf_ont=intf_List.get(1);
						if(!deviceList.contains(device_ont))
								deviceList.add(device_ont);
					}
				}
			}
		}
    	return deviceList;
    }
    
    //find the interfaces connecting two connected devices
    //ArrayList <ArrayList <OntResource>> = <Device,Interface>, <Device,Interface>
    public ArrayList<ArrayList<OntResource>> findInterface(Resource rs0,Resource rs1,String rType1_str,String rType2_str,int isEndPoint,String nc_of_version){
    	ArrayList<ArrayList <OntResource>> intf_List = new ArrayList<ArrayList <OntResource>> ();

    	ResultSet results = getSubGraphLinkTo(rs0.getURI(), rs1.getURI());
        //NdlCommons.outputQueryResult(results);
        //results = getSubGraphLinkTo(rs0.getURI(), rs1.getURI());
        int results_size=0;
        while (results.hasNext()) {
        	results.next();
        	results_size++;
        }
        results = getSubGraphLinkTo(rs0.getURI(), rs1.getURI());
        
    	OntResource rs0_ont = ontModel.getOntResource(rs0);
    	OntResource rs1_ont = ontModel.getOntResource(rs1);
        
        String var0 = (String) results.getResultVars().get(0); // ?a
        //String var1 = (String) results.getResultVars().get(1); // ?b
        String var2 = (String) results.getResultVars().get(1); // ?c
        
        Resource in0, in1, in2;
        
        OntResource in0_ont, in2_ont;
        QuerySolution solution;
        ArrayList <OntResource> pair=null;
        boolean correctResourceType=true;
        
        while (results.hasNext()) {
            solution = results.nextSolution();
            in0 = solution.getResource(var0);
            //in1 = solution.getResource(var1);
            in2 = solution.getResource(var2);

            in0_ont = ontModel.getOntResource(in0); // convert to OntResource for more functions
            in2_ont = ontModel.getOntResource(in2); 
 
            correctResourceType = true;
            if (in0_ont.hasRDFType(NdlCommons.interfaceOntClass)) { // it's a interface
            	if(results_size>1){
            	//if the interface was marked occupied (by existing connection, skip it
            		if(in0_ont.getProperty(NdlCommons.portOccupied)!=null){
            			if(in0_ont.getProperty(NdlCommons.portOccupied).getBoolean()==true)
            				continue;
            		}
            	
            		if(in2_ont.getProperty(NdlCommons.portOccupied)!=null){
            			if(in2_ont.getProperty(NdlCommons.portOccupied).getBoolean()==true)
            				continue;
            		}
            	}
            	//If multiple links, always only added the one directly connecting the two neighbors.
            	if (in0_ont.hasProperty(NdlCommons.topologyInterfaceOfProperty,rs0) && in2_ont.hasProperty(NdlCommons.topologyInterfaceOfProperty,rs1)) {
            		intf_List.clear();
            		if (isEndPoint==-1) correctResourceType = correctResourceType(in0_ont,rType1_str);
            		if (isEndPoint==1) correctResourceType = correctResourceType(in2_ont,rType2_str);

            		if(correctResourceType){
            			if(isEndPoint!=0){
            				if(!check_of_version(in0,nc_of_version) && !check_of_version(in2,nc_of_version))
            					continue;
            			}
            			pair = new ArrayList <OntResource> ();
            			pair.add(rs0_ont);
            			pair.add(in0_ont);
            			intf_List.add(0,pair);
                
            			pair = new ArrayList <OntResource> ();
            			pair.add(rs1_ont);
            			pair.add(in2_ont);
            			intf_List.add(pair);
            			
            			logger.debug("findInterface:"+rs0.getURI()+":"+in0_ont.getURI()+"---"+rs1.getURI()+":"+in2_ont.getURI());
            			
            			break;
            		}
            	}
            	//Otherwise, add the device/interface individually
            	if (in0_ont.hasProperty(NdlCommons.topologyInterfaceOfProperty,rs0)) {
            		if (isEndPoint==-1) correctResourceType = correctResourceType(in0_ont,rType1_str);
            		if(correctResourceType){
                		pair = new ArrayList <OntResource> ();
                    	pair.add(rs0_ont);
                    	pair.add(in0_ont);
                    	intf_List.add(0,pair);
                    	logger.debug("findInterface:"+rs0.getURI()+":"+in0_ont.getURI()+"---");
            		}
            	}    	
                if (in2_ont.hasProperty(NdlCommons.topologyInterfaceOfProperty,rs1)) {      
                		if (isEndPoint==1) correctResourceType = correctResourceType(in2_ont,rType2_str);
                		if(correctResourceType){
                			pair = new ArrayList <OntResource> ();
                			pair.add(rs1_ont);
                			pair.add(in2_ont);
                			intf_List.add(pair);
                			logger.debug("findInterface:"+rs1.getURI()+":"+in2_ont.getURI());
                		}
                } 
            }
        } 
    	
    	return intf_List;
    }
    
    public boolean check_of_version(Resource intf_rs,String nc_of_version){
    	boolean right_version = false;
    	if(intf_rs.hasProperty(this.topologyInterfaceOfProperty)){
    		Resource domain_rs = intf_rs.getProperty(this.topologyInterfaceOfProperty).getResource();
    		right_version =isStitchingNodeInManifest(domain_rs);
    		if(right_version)
    			return right_version;
    	}
    	String intf_of = NdlCommons.getOpenFlowVersion(intf_rs);
		if(nc_of_version!=null){
			if(intf_of!=null){
				if(intf_of.equalsIgnoreCase(nc_of_version)){
					right_version = true;
				}
			}
		}else{
			if(intf_of==null){
				right_version = true;
			}
		}
    	
    	return right_version;
    }
    
    // Check if the end point domain has the required resource type
    boolean correctResourceType(OntResource rs,String resourceType){
    	boolean correct = false;
    	DomainResourceType rType = resourceType(rs);
    	if(rType==null) return true;
    	if(resourceType.equals(rType.getResourceType())) correct = true;
    	return correct;
    }
    
    public DomainResourceType resourceType(OntResource rs){
    	DomainResourceType type=null;
    	if(rs.getProperty(NdlCommons.availableLabelSet)==null) return type;
    	Resource labelSet=rs.getProperty(NdlCommons.availableLabelSet).getResource();
    	if(labelSet!=null) {
    		if(labelSet.getProperty(NdlCommons.domainHasResourceTypeProperty)!=null){ 
    			Resource resourceType=labelSet.getProperty(NdlCommons.domainHasResourceTypeProperty).getResource();
    			int rank = 0;
    			if(resourceType.getProperty(NdlCommons.resourceTypeRank)!=null)
    					rank=resourceType.getProperty(NdlCommons.resourceTypeRank).getInt();
    			type = new DomainResourceType();
    			type.setResourceType(resourceType.getLocalName().toLowerCase());
    			type.setRank(rank);
    		}
    	}
    	
    	if(type!=null) 
    		logger.info("Intf resource type:"+type.toString());
    	return type;
    }
    
    //	define the requested resource : type, unit, rank
    public DomainResourceType getResourceType(String type_str){
    	DomainResourceType type = new DomainResourceType();
    	OntResource resourceType=ontModel.getOntResource(type_str);
    	int rank = 0;
		if(resourceType!=null){
			if(resourceType.getProperty(NdlCommons.resourceTypeRank)!=null)
				rank=resourceType.getProperty(NdlCommons.resourceTypeRank).getInt();
		}
		type.setResourceType(type_str);
		type.setRank(rank);
		
    	return type;
    }


    // get device, rs, from deviceConnection;
    public Device getDevice(Resource rs, LinkedList<Device> list)
    {
        if (list == null)
            return null;
        Iterator<Device> it = list.iterator();
        Device device = null;
        Device itDevice;
        while (it.hasNext()) {
            itDevice = (Device) it.next();
            
            if ((rs.getURI().equals(itDevice.getName())) || (rs.getURI().equals(itDevice.getURI()))) {
            	device = itDevice;
                break;
            }
        }
        return device;
    }

    public boolean isCe(DomainElement device){
    	boolean isCe=false;
		String rType = device.getResourceType().getResourceType().toLowerCase();
    	if(rType.endsWith(DomainResourceType.VM_RESOURCE_TYPE) 
    			|| rType.endsWith(DomainResourceType.BM_RESOURCE_TYPE)
    			|| rType.endsWith(DomainResourceType.FourtyGBM_RESOURCE_TYPE))
    		isCe=true;
    	
    	return isCe;
    }
    
    public Device getDevice(Device d, LinkedList<NetworkElement> list)
    {
        if (list == null)
            return null;
        Iterator<NetworkElement> it = list.iterator();
        Device device = null;
        Device itDevice;
        while (it.hasNext()) {
            itDevice = (Device) it.next();
            if(d.getName().equals(itDevice.getName()) ){            	
            	device = itDevice;
                break;
            }
        }
        return device;
    }
    
	public void removeInConnectionProperty(String ps, Property p){
		String queryPhrase=createQueryStringSubjectData(ps, "true");
		
		ResultSet results=rdfQuery(ontModel,queryPhrase);
		String var0=(String) results.getResultVars().get(0);

		QuerySolution solution=null;
		Resource rs=null;
		
		LinkedList <Resource> tempList=null;
		if(results.hasNext()) tempList=new LinkedList();
		while (results.hasNext()){
			solution=results.nextSolution();
			rs=solution.getResource(var0);
			tempList.add(rs);
		}
		
		while(tempList!=null && !tempList.isEmpty()){
			rs=tempList.removeLast();
			logger.debug("removeInConnectionProperty:"+rs.getURI()+":"+ps);
			rs.removeAll(p);
		}
		
		//results=rdfQuery(ontModel,queryPhrase);
		//outputQueryResult(results);
	}

    // find the layer:Layer
    public String findLayer(Resource resource)
    {

        ResultSet results = getLayer(ontModel, resource.getURI());

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

    // get layer:AdapatationProperty
    public ResultSet getLayerAdapatation(String rsURI)
    {
        ResultSet results = getLayerAdapatation(ontModel, rsURI);
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

    // get the default layer switching action
    public static String getLayerAction(String layer)
    {
        String action = null;

        for (Layer lay : Layer.values()) {
            if (layer.equals(lay.toString())) {
                action = lay.getAction().toString();
                break;
            }
        }
        return action;
    }


    public static String substrateDotString(OntModel ontModel){
		String url1,url2,rs1_name,rs2_name;
		Resource rs1,rs2;
		OntResource ont_rs1,ont_rs2;
		
		LinkedList <NetworkConnection> edgeList = new LinkedList <NetworkConnection> ();
		NetworkConnection edge = null;
		
		String dotString = "graph RDFS {\n";
        dotString += "\trankdir=LR;\n";
        dotString += "\tranksep=\"1.2\";\n";
        dotString += "\tedge [arrowhead=none];\n";
        dotString += "\tnode [label=\"\\N\", fontname=Arial, fixedsize=false, color=lightblue,style=filled];\n";
        
		ResultSet results = connectedDevicePair(ontModel);
		//outputQueryResult(results);
		//results = connectedDevicePair(ontModel);
		OntClass serverOntClass = ontModel.createClass(ORCA_NS + "compute.owl#Server");
		OntClass deviceOntClass = ontModel.createClass(ORCA_NS + "topology.owl#Device");
		OntClass interfaceOntClass = ontModel.createClass(ORCA_NS + "topology.owl#Interface");
		OntProperty interfaceOf = ontModel.createOntProperty(ORCA_NS + "topology.owl#interfaceOf");
		
		String var0=(String) results.getResultVars().get(0);
		String var1=(String) results.getResultVars().get(1);
		QuerySolution solution=null;
		while (results.hasNext()){
			solution=results.nextSolution();
			
			rs1=solution.getResource(var0);
			rs2=solution.getResource(var1);
			url1=rs1.getURI();
			url2=rs2.getURI();
			ont_rs1=ontModel.createOntResource(url1);
			ont_rs2=ontModel.createOntResource(url2);
			
			if(ont_rs1.hasRDFType(interfaceOntClass)) 
				ont_rs1=(OntResource) ont_rs1.getPropertyValue(interfaceOf);
			if(ont_rs2.hasRDFType(interfaceOntClass)) 
				ont_rs2=(OntResource) ont_rs2.getPropertyValue(interfaceOf);
			
			
			if(ont_rs1.hasRDFType(serverOntClass) | ont_rs1.hasRDFType(deviceOntClass)){
				if(!existEdge(edgeList,url1,url2)){
					edge = new NetworkConnection();
					edge.setConnectionType(ConnectionType.link.toString());
					NetworkElement ne1 = new NetworkElement(ontModel, ont_rs1);
					NetworkElement ne2 = new NetworkElement(ontModel, ont_rs2);
					edge.setNe1(ne1);
					edge.setNe2(ne2);
					edgeList.add(edge);
					rs1_name=(ont_rs1.getLabel(null)!=null)?ont_rs1.getLabel(null):ont_rs1.getLocalName();
					rs2_name=(ont_rs2.getLabel(null)!=null)?ont_rs2.getLabel(null):ont_rs2.getLocalName();
					dotString+="\"" + rs1_name +"\"" + "--" +"\"" + rs2_name +"\"\n";
				}
			}
		}
        
		dotString += "\n}";
		
        return dotString;
	}
	
	public static boolean existEdge(LinkedList <NetworkConnection> edgeList, String url1,String url2){
		boolean exist = false;
		int size = edgeList.size();
		NetworkConnection thisEdge;
		//System.out.print(size);
		for(int i=0;i<size;i++){
			thisEdge = edgeList.get(i);
			
			if((url1.equals(thisEdge.getNe1().getURI())) && (url2.equals(thisEdge.getNe2().getURI()))){
				exist=true;
				break;
			}
			if((url1.equals(thisEdge.getNe2().getURI())) && (url2.equals(thisEdge.getNe1().getURI()))){
				exist=true;
				break;
			}
		}	
		return exist;
	}
	
	//List all pairs of connected device
	public static ResultSet connectedDevicePair(OntModel model){
		String queryPhrase=createQueryStringConnect();
		
        return rdfQuery(model,queryPhrase);
	}
    
	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}
    
}
