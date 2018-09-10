package orca.network;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.util.*;
import com.hp.hpl.jena.vocabulary.*;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.QuerySolution;

import orca.ndl.*;
import orca.ndl.elements.Device;
import orca.ndl.elements.NetworkConnection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Map.Entry;

import net.jwhoisserver.utils.InetNetworkException;

import org.apache.log4j.Logger;

public class VTRequestMapping extends RequestMapping {

    private ArrayList<DomainResourceType> setOfCloudSite;

    public VTRequestMapping(String requestFile, String substrateFile, String currentSubstrateFileName)
            throws IOException {
        // super(requestFile,substrateFile);
        // super(substrateFile); //this created 'ontModel': the configured substrate ontModel
        super();
        // logger=Logger.getLogger(this.getClass().getCanonicalName());

        setInputFileName(substrateFile);
        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

        InputStream in = FileManager.get().openNoMap(substrateFile);
        if (in == null) {
            throw new IllegalArgumentException("File: " + substrateFile + " not found");
        }

        // read the RDF/XML file
        ontModel.read(in, "");
        in.close();
        // OntModel model=ontCreate(fileName);
        // ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF,model);

        createProperty();
        addRequest(requestFile);
        outputFileName = currentSubstrateFileName;
    }

    public VTRequestMapping(String requestFile, String substrateFile) throws IOException {
        super(requestFile, substrateFile);
    }

    public VTRequestMapping(String substrateFile) throws IOException {
        super(substrateFile);
    }

    public VTRequestMapping() {
        super();
    }

    public Hashtable<String, LinkedList<Device>> handleMapping(int numVM,
            Hashtable<String, NetworkConnection> requestMap,
            Hashtable<String, DomainResourceType> domainResourcePools) {
        Hashtable<String, LinkedList<Device>> connectionList = null;

        GomoryHuTree gomory = new GomoryHuTree();

        gomory.createGraph(numVM, requestMap);

        gomory.gomory(gomory.numVertices, gomory.numEdges, gomory.Edges, gomory.capacities, gomory.Adj);

        // LinkedList <NetworkConnection> ghTree = kCut(nodeMap);

        int numCloud = setOfCloudSite.size();

        int min_k = 0;
        float mappingCost = 0, minMappingCost = 100000;

        needPartition(numVM, domainResourcePools);

        /*
         * for(int k =1; k<numCloud;k++){ if(!needPartition(k,Hashtable <String,DomainResourceType>
         * domainResourcePools){ break; } OntModel vt=partition(requestModel,k,ghTree); mappingCost =
         * vtMapping(vt,cloudMeshOntModel); if(mappingCost<minMappingCost){ min_k=k; } } // expand the real inter-cloud
         * links of min_k mapping
         * 
         * // call the cloud handler for each cloud embedding with subRequest model
         */

        return connectionList;
    }

    public boolean needPartition(int numVM, Hashtable<String, DomainResourceType> domainResourcePools) {

        boolean need = false;
        String domainURL;
        int maxVM = 0, secondMaxVM = 0, count = 0;

        Collections.sort(setOfCloudSite);
        maxVM = setOfCloudSite.get(0).getCount();
        secondMaxVM = setOfCloudSite.get(1).getCount();
        if (numVM > maxVM - secondMaxVM / 3)
            need = true;
        System.out.println("VM counts:" + maxVM + ":" + secondMaxVM + ":" + numVM + ":" + need + "\n");

        return need;
    }

    // running the kcut algorithm and get the GH tree as a collection of edges.
    public LinkedList<NetworkConnection> kCut(Hashtable<Integer, String> nodeMap) {

        LinkedList<NetworkConnection> tree = new LinkedList<NetworkConnection>();

        File dir = new File(".");
        try {
            System.out.println(dir.getCanonicalPath());
            Runtime runtime = Runtime.getRuntime();

            Process kcut = runtime.exec("./gomory");

            InputStreamReader isr = new InputStreamReader(kcut.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line, source, destination, weight;
            NetworkConnection edge = null;
            while ((line = br.readLine()) != null) {
                // System.out.println(line);
                if (line.startsWith("Edge ")) {
                    // line=br.readLine(); //"Edge "
                    source = br.readLine(); // source
                    destination = br.readLine(); // destination
                    weight = br.readLine(); // weight
                    System.out.println("Return:" + source + ":" + destination + ":" + weight + "\n");

                    edge = new NetworkConnection();
                    edge.setSn1(Integer.valueOf(source.trim()));
                    edge.setEndPoint1(nodeMap.get(edge.getSn1()));

                    edge.setSn2(Integer.valueOf(destination.trim()));
                    edge.setEndPoint2(nodeMap.get(edge.getSn2()));

                    edge.setBw(Long.valueOf(weight));

                    tree.add(edge);

                    // System.out.println(edge.getSn1()+":"+edge.getEndPoint1()+"-"+edge.getSn2()+":"+edge.getEndPoint2()+":"+edge.getBw());
                }
            }

            Collections.sort(tree); // sort the tree edges according to the weight

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return tree;
    }

    public LinkedList directVTMapping() {
        // ontModel.write(System.out);
        LinkedList connectionList = new LinkedList();
        QuerySolution solution = null;
        ResultSet results = null;

        String url1, url2, source, destination, queryPhrase;
        String selectStr = "SElECT ?Node1 ?Node2 ?Node3 ?Node4 ?Inf1 ?Inf21 ?Inf3 ?Inf4 ";
        /*
         * String whereStr="WHERE {" + "?Node1 ndl:hasInterface/ndl:linkTo/ndl:interfaceOf ?Node2." +
         * "?Node3 ndl:hasInterface/ndl:linkTo/ndl:interfaceOf ?Node2"+ " }";
         */

        String whereStr = "WHERE {" + "?Node1 ndl:hasInterface ?Inf11." + "?Inf11 ndl:linkTo ?Inf21."
                + "?Inf21 ndl:interfaceOf ?Node2. " +

                "?Node2 ndl:hasInterface ?Inf22." + "?Inf22 ndl:linkTo ?Inf31. "
                + "?Inf31 ndl:interfaceOf ?Node3. FILTER(?Node1 != ?Node3) " +

                "?Node3 ndl:hasInterface ?Inf32." + "?Inf32 ndl:linkTo ?Inf41. "
                + "?Inf41 ndl:interfaceOf ?Node4.FILTER(?Node2 != ?Node4)" +

                "?Node4 ndl:hasInterface ?Inf42." + "?Inf42 ndl:linkTo ?Inf12. " + "?Inf12 ndl:interfaceOf ?Node1." +

                " }";
        /*
         * String whereStr="WHERE {" + "?Node1 ndl:interfaceOf ?Node2."+ " }";
         */
        String filterStr = "FILTER ";
        String fromStr = "";

        queryPhrase = createQueryString(selectStr, fromStr, whereStr);
        System.out.println(queryPhrase);

        results = rdfQuery(ontModel, queryPhrase);
        outputQueryResult(results);
        return connectionList;
    }

    public LinkedList vtMapping() throws IOException {

        LinkedList connectionList = new LinkedList();
        Hashtable<String, NetworkConnection> requestMap = null;
        try {
            requestMap = parseRequest(requestModel);
        } catch (InetNetworkException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String url1, url2, p_source = null, p_destination, source, destination, queryPhrase;

        String selectStr = "SElECT ";
        String whereStr = "WHERE {";
        String filterStr = "FILTER (?";
        p_destination = "";

        int i = 1;
        NetworkConnection requestConnection = null;
        if (requestMap != null)
            return null;
        for (Entry<String, NetworkConnection> entry : requestMap.entrySet()) {
            requestConnection = entry.getValue();

            url1 = requestConnection.getEndPoint1();
            url2 = requestConnection.getEndPoint2();

            source = url1.split("\\#")[1];
            destination = url2.split("\\#")[1];

            selectStr += "?" + source + " " + "?" + destination + " ";

            String source_intf = source + "Intf" + String.valueOf(i);
            String destination_intf = destination + "Intf" + String.valueOf(i);
            whereStr += " ?" + source + " ndl:hasInterface ?" + source_intf + ". ?" + source_intf + " ndl:linkTo ?"
                    + destination_intf + ". ?" + destination_intf + " ndl:interfaceOf ?" + destination + ".";

            // System.out.println(source+":"+destination+":"+p_source+":"+p_destination);
            if (i > 1) {
                whereStr += filterStr + destination + "!=?" + p_destination + ")";
            }

            p_source = source;
            p_destination = destination;

            // deviceMapping(url1,url2);
            i++;
        }

        whereStr += "}";
        String fromStr = "";

        queryPhrase = createQueryString(selectStr, fromStr, whereStr);
        // System.out.println(queryPhrase);
        ResultSet mappingResults = rdfQuery(ontModel, queryPhrase);

        // outputQueryResult(mappingResults);
        // mappingResults = rdfQuery(ontModel,queryPhrase);

        String dotString = substrateDotString(ontModel);

        String dotString1 = processVTMapping(requestMap, mappingResults);

        // System.out.println("okkkk"+dotString1+"\n");
        // ontWrite(outputFileName,dotString);
        ontWrite(outputFileName, dotString.split("}")[0] + dotString1);
        return connectionList;
    }

    public String processVTMapping(Hashtable<String, NetworkConnection> vtResults, ResultSet mappingResults) {
        ArrayList<String> nodeList = (ArrayList) mappingResults.getResultVars();
        int numNode = nodeList.size();

        Resource[] resourceList;
        LinkedList vtList = new LinkedList();
        QuerySolution solution = null;
        int i, numVT;
        Resource rs = null;
        while (mappingResults.hasNext()) {
            solution = mappingResults.nextSolution();
            resourceList = new Resource[numNode];
            for (i = 0; i < numNode; i++) {
                resourceList[i] = solution.getResource(nodeList.get(i));
            }
            // numVT=vtList.size();
            // printVTList(vtList,numVT, numNode);
            if (!existVT(vtList, resourceList, numNode)) {
                vtList.add(resourceList);
            }
        }

        numVT = vtList.size();

        return addVT(vtList, numVT, numNode);

    }

    public boolean existVT(LinkedList<Resource[]> vtList, Resource[] vt, int numNode) {
        boolean exist = false;
        boolean flag1, flag2;
        int numVT = vtList.size();
        int i, j, k;
        Resource[] thisVT = null;

        for (i = 0; i < numVT; i++) {
            thisVT = vtList.get(i);
            flag2 = true;
            for (j = 0; j < numNode; j++) {
                flag1 = false;
                for (k = 0; k < numNode; k++) {
                    if (thisVT[j] == vt[k]) {
                        flag1 = true;
                        break;
                    }
                }
                if (!flag1) {
                    flag2 = false;
                    break;
                }
            }
            if (flag2) {
                exist = true;
                break;
            }
        }

        return exist;
    }

    public void ontWrite(String aFile, String dotString) throws IOException {

        FileWriter out = new FileWriter(aFile);

        if (out == null) {
            throw new IllegalArgumentException("File: " + aFile + " not found");
        }

        // System.out.println(dotString);

        BufferedWriter ou = new BufferedWriter(out);

        ou.write(dotString);

        ou.write("\n}");

        ou.flush();
        ou.close();
    }

    public String addVT(LinkedList<Resource[]> vtList, int numList, int numNode) {
        String dotString = "";
        Resource[] resourceList = null;

        OntResource ont_rs1, ont_rs2;
        logger.info("Number of VT mapped:" + numList + "; #node of the VT:" + numNode + "\n");
        for (int i = 0; i < numList; i++) {
            resourceList = vtList.get(i);
            dotString += "edge [style=bold,color=" + color[i % 5] + "]\n";
            for (int j = 0; j < numNode; j++) {
                for (int k = j + 1; k < numNode; k++) {
                    if (directConnected(resourceList[j], resourceList[k], i)) {

                        ont_rs1 = ontModel.createOntResource(resourceList[j].getURI());
                        ont_rs2 = ontModel.createOntResource(resourceList[k].getURI());

                        dotString += "\"" + ont_rs1.getLabel(null) + "\"" + "--" + "\"" + ont_rs2.getLabel(null)
                                + "\"\n";

                        // System.out.print(resourceList[j]+":"+resourceList[k]+"\n");
                    }
                }
            }
            // System.out.print("\n");
        }
        return dotString;
    }

    public boolean directConnected(Resource node_rs1, Resource node_rs2, int vt) {
        boolean connected = false;
        String queryPhrase, url1, url2;
        Resource rs_intf1, rs_intf2;

        String selectStr = "SElECT ?Inf1 ?Inf2 ";

        String whereStr = "WHERE {" + "<" + node_rs1 + ">" + " ndl:hasInterface ?Inf1." + "?Inf1 ndl:linkTo ?Inf2."
                + "?Inf2 ndl:interfaceOf " + "<" + node_rs2 + ">" +

                " }";
        String fromStr = "";

        queryPhrase = createQueryString(selectStr, fromStr, whereStr);
        // System.out.println(queryPhrase);

        ResultSet results = rdfQuery(ontModel, queryPhrase);
        String var0 = (String) results.getResultVars().get(0);
        String var1 = (String) results.getResultVars().get(1);
        QuerySolution solution = null;
        // outputQueryResult(results);
        if (results.hasNext()) {
            solution = results.nextSolution();
            url1 = solution.getResource(var0).getURI() + "/" + String.valueOf(vt);
            url2 = solution.getResource(var1).getURI() + "/" + String.valueOf(vt);
            rs_intf1 = ontModel.createResource(url1);
            rs_intf2 = ontModel.createResource(url2);

            rs_intf1.addProperty(connectedTo, rs_intf2);
            rs_intf2.addProperty(connectedTo, rs_intf1);

            rs_intf1.addProperty(hasInterface, node_rs1);
            rs_intf2.addProperty(hasInterface, node_rs2);
            connected = true;
        }

        return connected;
    }

    public NetworkConnection deviceMapping(String url1, String url2) {
        // setOfDevices=requestModel.listIndividuals(deviceOntClass);

        NetworkConnection deviceConnection = new NetworkConnection();

        return deviceConnection;
    }

    public ArrayList<DomainResourceType> getSetOfCloudSite() {
        return setOfCloudSite;
    }

    public void setSetOfCloudSite(ArrayList<DomainResourceType> setOfCloudSite) {
        this.setOfCloudSite = setOfCloudSite;
    }

}
