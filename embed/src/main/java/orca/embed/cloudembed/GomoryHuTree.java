package orca.embed.cloudembed;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import orca.ndl.*;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;

import org.apache.log4j.Logger;

import orca.embed.policyhelpers.JniLoader;
import orca.embed.policyhelpers.SystemNativeError;

public class GomoryHuTree {
	private static final Logger log = Logger.getLogger(GomoryHuTree.class);
	
	public int [][] Edges;
	public int [][] Adj;
	public int numEdges,numVertices;
	public float [] capacities ;
	public int [] resourcePools ;
	
	public static String gomory_Input_File="input3.in";
	
	public GomoryHuTree(){
		jniLoader();
	}
	
	public GomoryHuTree(LinkedList <DomainResourceType> sc){		
		jniLoader();
		resourcePools = new int [sc.size()];
		int i=0;
		for(DomainResourceType dType:sc){
			resourcePools[i]=sc.get(i).getCount();
			System.out.println(sc.get(i).toString());
			i++;              
		}
	}
	
	public void jniLoader(){
		JniLoader   loader = new JniLoader("gomory");

		if( loader != null )
		{
			SystemNativeError   error = loader.loadJni();
			String  message = "Error loading JNI: " + error.getMessage()
		+ " (" + error.getErrno() + ")";

			if( error.getAdditional() != null && !error.getAdditional().isEmpty() )
				message += ": " + error.getAdditional();
			if( error.getErrno() != 0 )
				log.error( message );
		}
	}
	
	public void createGraph(int numNode, Collection <NetworkElement>requestMap){
		numVertices=numNode;
		numEdges=requestMap.size();
		
		Edges=new int[2][numEdges];
		capacities = new float[numEdges];
		Adj = new int[numVertices][numEdges];
		
		int s1,s2,i=0;
		float bw;
		NetworkConnection requestConnection=null;
		
		Map<String, NetworkConnection> links = new HashMap<String, NetworkConnection>();
		for(Iterator <NetworkElement> j=requestMap.iterator();j.hasNext();){	
			requestConnection=(NetworkConnection)j.next();
			links.put(requestConnection.getResource().getLocalName(), requestConnection);
		}
		
		Random random = new Random();
		for(Entry <String,NetworkConnection > entry:links.entrySet()){	
	    	requestConnection=entry.getValue();
	    	if(requestConnection.getNe2()==null)
	    		continue;	
			s1=requestConnection.getNe1().getSn();
			s2=requestConnection.getNe2().getSn();
			System.out.println("s1="+s1+";s2="+s2+";i="+i);
			Edges[0][i] = s1;
			Edges[1][i] = s2;
			Adj[s1][i] = 1;
			Adj[s2][i] = -1; 
			capacities[i]=requestConnection.getBandwidth();
			if(capacities[i]==0.0){
				capacities[i]=random.nextInt(100)*10+50;
			}
			System.out.println("Java Graph:"+entry.getKey()+":"+Edges[0][i]+";"+Edges[1][i]+":"+capacities[i]+"\n");
			i++;
		}
		
		try {
			outputGraph(gomory_Input_File,numNode+1,links);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// output the graph to the format file as the input to the gomory code
	public void outputGraph(String outputFile, int numNode, Map<String, NetworkConnection> links) throws IOException{
	    Writer output = null;

	    File file = new File(outputFile);
	    output = new BufferedWriter(new FileWriter(file));
	    output.write(numNode+"\n");
	    output.write(links.size()+"\n");
	    
	    NetworkConnection requestConnection=null;
	    String rs1_name,rs2_name;
	    int rs1_sn = 0,rs2_sn = 0,i=0;
		long bw = 0;
	    for(Entry <String,NetworkConnection > entry:links.entrySet()){	
	    	requestConnection=entry.getValue();
	    	if(requestConnection.getNe2()==null)
	    		continue;
	    	rs1_name = requestConnection.getNe1().getResource().getLocalName();
			rs1_sn=  requestConnection.getNe1().getSn();
			rs2_name = requestConnection.getNe2().getResource().getLocalName();
			rs2_sn = requestConnection.getNe2().getSn();
			bw=requestConnection.getBandwidth();
			if(bw==0) 
				bw=100;
			output.write(i+":"+rs1_name+"("+rs1_sn+") " +rs2_name+ "("+rs2_sn+") " + bw +"\n");
			i++;
	    }
	    
	    output.close();
	}
	
	public static native GomoryHuTreeEdge [] gomory(int numVertices,int numEdges,int [][]Edges,float [] capacities, int [][] Adj);
	
	public static native GomoryHuTreeEdge [] setpartition(int numVertices, int numedges, int []partition,int [][]Adj,GomoryHuTreeEdge[] tree_c_array);
	
	public static native int[] partition(int numVertices,int sites,int [] resourcePools);
	
}
