/**
 * (c) Copyright 2008, RENCI
 * All rights reserved.
 * [See end of file]
 * $Id: Ndl.java,v 1.0 2008/12/12 09:30:07 der Exp $

 */

package orca.network;

import java.io.InputStream;

import orca.ndl.LayerConstant;
import orca.ndl.NdlCommons;
import orca.network.policyhelpers.SystemNativeError;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.FileManager;


/**
 * @author yxin
 *
 */
public class Ndl implements LayerConstant {
	static final String inputFileName  = "orca/network/ben-dell.rdf";
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String src="1-a-3-l1-1",srcc="1-a-3-t1-1";
		String dst="1-a-4-l1-1";
		
		String payloadType = "10gbe_lan";
		String payload = payloadType;
		
	    String line = "l";
	    String client = "t";
		
	    int index = src.indexOf(line);
	    int index_c=srcc.indexOf(client);
	    int index_d=dst.indexOf(line);
	    
	    System.out.print(index+":"+index_c+":"+index_d+"\n");
	    
	    if(index>0 & index_d>0){
	    	if(payloadType.toLowerCase().startsWith("4xoc192") || payloadType.toLowerCase().startsWith("oc768")) 
    			payload="40G";
	    	if(payloadType.toLowerCase().startsWith("10g") || payloadType.toLowerCase().startsWith("oc192")) 
	    			payload="10G";
	    	if(payloadType.toLowerCase().startsWith("1g") || payloadType.toLowerCase().startsWith("oc48")) 
    			payload="25G";
	    }
	    System.out.println(payload+"\n");
		
		OntModel model = ModelFactory.createOntologyModel();

        InputStream in = FileManager.get().open( inputFileName );
        if (in == null) {
            throw new IllegalArgumentException( "File: " + inputFileName + " not found");
        }
        
        // read the RDF/XML file
        model.read(in, "");
                    
        // write it to standard out
        //model.write(System.out);   
        
       OntProperty RDFS_Label=model.createOntProperty("http://www.w3.org/2000/01/rdf-schema#label");
      
        
        String url3="<http://geni-orca.renci.org/owl/ben.rdf#Duke/Dell/go-1>";
        
        OntResource rs_ont=model.createOntResource(url3);
        
        String type=rs_ont.getLabel(null);
        Statement v=rs_ont.getProperty(RDFS_Label);
        if(v!=null) System.out.println("Type:"+type+":"+rs_ont.getNameSpace()+":"+v.getLiteral());
		
//*      Create a new query
        String url1="<http://geni-orca.renci.org/owl/ben.rdf#Renci/Polatis>";
        String url2="<http://geni-orca.renci.org/owl/ben.rdf#Duke/Polatis>";
        
        String queryPhrase1 = 
        	"SELECT ?resource " +
        	"FROM <http://geni-orca.renci.org/owl/ben.rdf>" +
        	"WHERE {" +
        	"<http://geni-orca.renci.org/owl/ben.rdf#Renci/Polatis> gleen:OnPath ('[ndl:hasInterface]+/([ndl:linkTo]|[ndl:connectedTo])+/[ndl:interfaceOf]+' ?resource)."+
        	"      }";

        String queryPhrase2 = 
        	"SELECT ?p ?r ?s " +
        	"FROM <http://geni-orca.renci.org/owl/ben-dtn.rdf> " +
        	"WHERE {" +
        	"<"+NdlCommons.ORCA_NS+"ben-dtn.rdf#Renci/Infinera/DTN/t1B/1/lambda>" +" ?p ?r."+
        	" ?p rdf:type layer:AdaptationProperty"+
        	"      }";   
        
        String queryPhrase3 =
    	"SELECT ?a ?b ?c " +
    	"" +
    	"WHERE {" +
    	"("+url1+" '[ndl:hasInterface]+/[ndl:linkTo]+/[ndl:interfaceOf]+'"+url2+") gleen:Subgraph (?a ?b ?c)"+
    	"      }";
        
        String queryPhrase4=
        	"SELECT ?r " +
        	"FROM <http://geni-orca.renci.org/owl/ben-dell.rdf> " +
        	"WHERE {" +
        	url3 +" rdfs:label ?r"+
        	"      }"; 
        
        String queryString=NdlCommons.ontPrefix.concat(queryPhrase3);
        System.out.println(queryString);
        Query query = QueryFactory.create(queryString);

//      Execute the query and obtain results
        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet results = qe.execSelect();

//      Output query results	
        ResultSetFormatter.out(System.out, results, query);

//      Important - free up resources used running the query
        qe.close();
        
        
        NdlCreator ben = new NdlCreator();
        if( ben == null ){
        	System.out.println( "Failed to load JNI!" );
        }
        else{
        	System.out.println( "Loaded JNI..." );

        	SystemNativeError   error = ben.system( "date" );

        	if( error.getErrno() != 0 )
        	  System.out.println( error.toString() );
        }	
        ben.print();
        ben.create();
	}

}
