package orca.network;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.util.*;
import com.hp.hpl.jena.vocabulary.*;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.QuerySolution;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;

import orca.embed.policyhelpers.RequestMappingException;
import orca.ndl.*;

import net.jwhoisserver.utils.InetNetworkException;

import orca.ndl.LayerConstant.Layer;
import orca.ndl.elements.Device;
import orca.ndl.elements.Interface;
import orca.ndl.elements.Label;
import orca.ndl.elements.LabelSet;
import orca.ndl.elements.NetworkConnection;
import orca.ndl.elements.NetworkElement;
import orca.ndl.elements.SwitchMatrix;
import orca.ndl.elements.SwitchingAction;

/*
 * ontModel:
 * requestModel:
 * phymodel:
 * requestMapModel: mapped request connections in OntModel
 * 
 */
public class RequestMapping extends SliceRequest {

    RequestMapping(String requestFile, String substrateFile) throws IOException {
        super(requestFile, substrateFile);
    }

    RequestMapping(String substrateFile) throws IOException {
        super(substrateFile);
    }

    public RequestMapping() {
        super();
    }

    public RequestMapping(OntModel ontModel) {
        super(ontModel);
    }

    public RequestMapping(InputStream stream) throws IOException {
        super(stream);
    }

    // Mapping the request to the physical substrate
    public NetworkConnection deviceMapping()
            throws RequestMappingException, UnknownHostException, InetNetworkException {
        // setOfDevices=requestModel.listIndividuals(deviceOntClass);

        NetworkConnection deviceConnection = new NetworkConnection();

        // ResultSet results = connectedDevicePair(requestModel);

        Hashtable<String, NetworkConnection> connectionNodeList = this.parseRequest(requestModel);

        Hashtable<String, LinkedList<Device>> connectionList = new Hashtable<String, LinkedList<Device>>();
        Resource rs1 = null, rs2 = null;
        String rs1_str = null, rs2_str = null;
        long bw = 0;
        String connectionName;

        if (connectionNodeList.isEmpty())
            return null;

        int connected = 0;
        int egressNode = 0;
        boolean valid = true;
        for (Entry<String, NetworkConnection> entry : connectionNodeList.entrySet()) {

            connectionName = entry.getKey();
            rs1_str = entry.getValue().getNe1().getURI();
            rs2_str = entry.getValue().getNe2().getURI();
            bw = entry.getValue().getBandwidth();

            rs1 = requestModel.getResource(rs1_str);
            rs2 = requestModel.getResource(rs2_str);

            deviceConnection.setName(connectionName);
            deviceConnection.setBandwidth(bw);
            egressNode = sdDevice(requestModel, rs2.getURI()); // destination? -1
            connected = deviceConnected(rs1, rs2, egressNode, entry.getValue());

            if (connected == -1) {
                valid = false;
                logger.info(rs1 + " is NOT switcheabally connected to " + rs2 + " in " + inputFileName);
                break;
            }

            if (connected == 0) {
                valid = false;
                logger.info(rs1 + " is NOT connected to " + rs2 + " in " + inputFileName);

                break;
            }
            if (connected == 1) {
                logger.info(rs1 + " is DIRECTLY connected to " + rs2 + " in " + inputFileName);

                connected = -1;
                continue;
            }
            if (connected >= 1) {
                logger.info(rs1 + " is INDIRECTLY connected to " + rs2 + " in " + inputFileName);

                connected = -1;
                continue;
            }

            removeInConnectionProperty("ndl:inConnection", this.inConnection);
        }

        if (!valid) {
            throw new RequestMappingException("request can not be facilited on the substrate!");
        }

        // ontModel.write(System.out);

        // saveOntModel();

        return deviceConnection;
    }

    // Step 1: Decide if two device are connected in the substrate:
    // (1) find the physical device/interface level path
    // (2) generate the switching action per device on the path
    public int deviceConnected(Resource rs1, Resource rs2, int gress, NetworkConnection requestConnection)
            throws OntologyException {
        boolean validSwitching = true;

        ArrayList<ArrayList<OntResource>> solution = findShortestPath(ontModel, rs1, rs2, requestConnection.getBw(),
                null);

        int connected = solution == null ? 0 : solution.size();

        validSwitching = toConnection(solution, requestConnection);
        if (!validSwitching) {
            connected = -2;
        }

        return connected;
    }

    // ****************************
    // Step 2: Further process after the device connection:
    // (1)find the uni interface switching; (2) finalize the action with correct label and adapted client intf
    // ****************************
    public void processDeviceConnection(NetworkConnection deviceConnection) {

        logger.info("Step 2: Process connection.\n");

        LinkedList<Device> deviceList = null;
        Iterator<Device> it;
        try {
            if (deviceConnection != null) {
                deviceList = deviceConnection.getConnection();

                findCommonLabel(deviceList);

                it = deviceList.iterator();
                while (it.hasNext())
                    processInterface(it.next());

                Collections.sort(deviceList, new NetworkElement.LayerComparator()); // sort the devices according to
                                                                                    // their layers, lower layer first
            }
        } catch (RequestMappingException e) {

        }
    }

    Stack<Label> labelStack = new Stack<Label>();
    Hashtable<String, BitSet> lableSetPerLayer;

    public int getLayerLable(String layer) {
        if (layer == null)
            return -1;
        BitSet lableSet = lableSetPerLayer.get(layer);
        if (lableSet != null) {
            return lableSet.nextSetBit(0);
        } else
            return -1;
    }

    public void findCommonLabel(LinkedList<Device> deviceList) {
        Iterator<Device> it = deviceList.iterator();
        int numDevice = deviceList.size(), count = 0;
        Device device = null;
        String currentLayer = null;
        lableSetPerLayer = new Hashtable<String, BitSet>();
        BitSet currentLableSet = null, lableSet;
        logger.debug("------Find Common Label range-----\n");
        while (it.hasNext()) {
            count++;
            device = it.next();
            logger.debug("Find Common Label range:" + device.getURI() + ":" + count + "\n");
            if (count == 1)
                continue;
            if (count == numDevice)
                continue; // the first and last devices are the servers or fake, ignore.
            SwitchMatrix matrix = null;
            LinkedList<SwitchMatrix> matrixList = device.getSwitchingMatrix();

            if (matrixList != null) {
                int size = matrixList.size();
                for (int i = 0; i < size; i++) {
                    matrix = (SwitchMatrix) matrixList.get(i);
                    logger.debug(i + ":SwitchingMatrix layer:" + matrix.getAtLayer() + ":size=" + size);
                    currentLayer = matrix.getAtLayer();
                    if (matrix.getAtLayer() != null) {
                        if (Layer.valueOf(currentLayer).rank() > 1) { // OCG Layer above for now
                            currentLableSet = findAvailableLabel(device, matrix.getAtLayer());
                            if (lableSetPerLayer.containsKey(currentLayer)) {
                                lableSet = lableSetPerLayer.get(currentLayer);
                                if (currentLableSet != null)
                                    lableSet.and(currentLableSet);
                            } else {
                                if (currentLableSet != null)
                                    lableSetPerLayer.put(currentLayer, currentLableSet);
                            }
                        }
                    }
                }
            } else {
                try {
                    throw new RequestMappingException("No Switching Matrix at this position, error!");
                } catch (RequestMappingException e) {
                    logger.error("No Switching Matrix at this position, error!");
                }
            }
        }
    }

    public BitSet findAvailableLabel(Device device, String currentLayer) {
        BitSet lableBitSet = null, currentLableBitSet = null;

        if (currentLayer.equals(Layer.EthernetNetworkElement.toString())) {
            lableBitSet = new BitSet(1002);
            lableBitSet.set(2, 1002);
        }

        if (currentLayer.equals(Layer.LambdaNetworkElement.toString())) {
            lableBitSet = new BitSet(11);
            lableBitSet.set(1, 11);
        }

        LinkedList<SwitchingAction> actionList = device.getActionList();
        SwitchingAction action = null;
        Interface intf;
        Interface intf_client = null;
        int actionCount = actionList == null ? 0 : actionList.size();
        int size, layer_rank;
        String layer;
        int currentLayer_rank = Layer.valueOf(currentLayer).rank();
        for (int j = 0; j < actionCount; j++) {
            action = (SwitchingAction) actionList.get(j);
            logger.debug("--Find Available Label:" + device.getURI() + ":" + actionCount + ":" + currentLayer + ":"
                    + action.getAtLayer());
            if (action.getAtLayer().equals(currentLayer)) {
                LinkedList<Interface> interfaceList = action.getClientInterface();
                size = interfaceList.size();
                for (int i = 0; i < size; i++) {
                    intf = interfaceList.get(i);
                    layer = intf.getAtLayer();
                    layer_rank = Layer.valueOf(layer).rank();
                    logger.debug(intf.getURI() + ";" + layer + ":" + layer_rank + ";" + currentLayer + ":"
                            + currentLayer_rank + ";" + i);
                    if (!layer.equals(currentLayer)) {
                        if (layer_rank < currentLayer_rank) {
                            intf_client = getAdaptationClient(intf, currentLayer);
                            intf = intf_client;
                        }
                    }
                    currentLableBitSet = getAvailableLabelRange(intf, currentLayer);
                    if (currentLableBitSet != null)
                        lableBitSet.and(currentLableBitSet);
                }
            }
        }

        return lableBitSet;
    }

    public Interface getAdaptationClient(Interface parent, String currentLayer) {
        LinkedList<Interface> clientList = parent.getClientInterface();
        ListIterator<Interface> it;
        Interface intf = null;
        OntResource rs_client = null;
        String url, name;
        int labelID = -1;

        Resource label_rs = null;
        logger.debug("Client:" + parent.getURI() + ":" + parent.getAtLayer() + ";" + currentLayer + "\n");
        if (clientList == null) {
            logger.debug(currentLayer + ";" + parent.getResource());
            if (currentLayer.equals(Layer.LambdaNetworkElement.toString())) {
                if (parent.getAtLayer().equals(Layer.OCGNetworkElement.toString())) {
                    return parent;
                }
            }
        } else {
            it = clientList.listIterator();

            while (it.hasNext()) {
                intf = (Interface) it.next();
                if (intf.getAtLayer().equals(currentLayer)) {
                    break;
                } else {
                    intf = getAdaptationClient(intf, currentLayer);
                    if (intf != null)
                        break;
                }
            }
        }

        return intf;
    }

    public BitSet getAvailableLabelRange(Interface intf, String currentLayer) {
        BitSet lableBitSet = new BitSet();

        String rsURI = intf.getResource().getURI();
        String layer = intf.getAtLayer();
        String availableLabelSet = Layer.valueOf(layer).getPrefix() + ":" + Layer.valueOf(layer).getASet();

        ResultSet results = getAvailableLabelSet(rsURI, availableLabelSet);
        // outputQueryResult(results);
        // results= getAvailableLabelSet(rsURI,availableLableSet_str);

        int lower = 0;
        int upper = 0;
        String lowerBound = (String) results.getResultVars().get(0);
        String upperBound = (String) results.getResultVars().get(1);
        String l = (String) results.getResultVars().get(2);
        String u = (String) results.getResultVars().get(3);
        String setElement = (String) results.getResultVars().get(4);
        String availableSet = (String) results.getResultVars().get(5);

        Resource lowerLabel = null;
        Resource upperLabel = null;
        Resource labelRange_rs = null;
        Resource availableSet_rs = null;
        QuerySolution solution = null;

        if (!results.hasNext()) {
            logger.error("No available label!\n");
            return null;
        }

        while (results.hasNext()) {
            solution = results.nextSolution();
            availableSet_rs = solution.getResource(availableSet);
            labelRange_rs = solution.getResource(setElement);

            if (solution.getLiteral(lowerBound) != null) {
                lower = solution.getLiteral(lowerBound).getInt();
                upper = solution.getLiteral(upperBound).getInt();
                lowerLabel = solution.getResource(l);
                upperLabel = solution.getResource(u);
                lableBitSet.set(lower, upper);
            } else {
                lowerLabel = labelRange_rs;
                if (lowerLabel.getProperty(label_ID) != null) {
                    lower = lowerLabel.getProperty(label_ID).getInt();
                    lableBitSet.set(lower);
                    upper = 0;
                    upperLabel = null;
                }
            }
            logger.debug("\n getAvailableLabelRange:-------Label Range:" + availableSet_rs + "----:" + lower + ":"
                    + upper + ":" + lowerLabel + ":" + upperLabel + "\n");
        }

        return lableBitSet;
    }

    public int findCommonLabel(LabelSet sSet, LabelSet nSet) {
        int min = 0, max = 0;
        BitSet sBitSet = new BitSet(4001);
        min = (int) sSet.getMinLabel_ID();
        max = (int) sSet.getMaxLabe_ID();
        if (min == max) {
            sBitSet.set(min);
        } else {
            sBitSet.set(min, max);
        }
        if ((min == 0) && (max == 0)) {
            sBitSet.set(0, 4000);
        }
        sBitSet.andNot(sSet.getUsedBitSet());

        min = (int) nSet.getMinLabel_ID();
        max = (int) nSet.getMaxLabe_ID();
        BitSet nBitSet = new BitSet(4001);
        if (min == max) {
            nBitSet.set(min);
        } else {
            nBitSet.set(min, max);
        }
        if ((min == 0) && (max == 0)) {
            nBitSet.set(0, 4000);
        }
        nBitSet.andNot(nSet.getUsedBitSet());
        sBitSet.and(nBitSet);
        int commonLabel = sBitSet.nextSetBit(0);
        if (commonLabel > 0) {
            sSet.setUsedBitSet(commonLabel);
            nSet.setUsedBitSet(commonLabel);
        }
        return commonLabel;
    }

    // (1) Layer adaptation process
    // (2) Label processing
    // (3) change for the uni switching interface (eg, Polatis)
    public void processInterface(Device device) throws RequestMappingException {
        logger.debug("----processing Interface-----" + device.getURI());

        SwitchMatrix matrix = null;
        LinkedList<SwitchMatrix> matrixList = device.getSwitchingMatrix();
        if (matrixList != null) {
            int size = matrixList.size();

            for (int i = 0; i < size; i++) {
                matrix = (SwitchMatrix) matrixList.get(i);
                if (matrix == null)
                    logger.error("No Switching Matrix at this position, error!");
                else {
                    logger.debug(i + ":SwitchingMatrix layer:" + matrix.getAtLayer() + ":size=" + size);
                    if (matrix.getAtLayer() != null) {
                        if (matrix.getDirection().equals(Direction.UNIDirectional.toString())) {
                            logger.debug("----------Unidirectional-------");
                            device.processUNIInterface(matrix.getAtLayer());
                        }
                        // processing adaptation, Labels...
                        processAdaptation(device, matrix.getAtLayer());
                    }
                }
            }
        } else {
            logger.error("No Switching Matrix at this position, error!");
            throw new RequestMappingException("No Switching Matrix at this position, error!");
        }
        device.getResource().removeAll(inConnection);
    }

    // process the layer adaptation:
    // 1. create the inferred client interface from adaptation
    // 2. label assignment
    // 3. update available labels
    public void processAdaptation(Device device, String currentLayer) {
        LinkedList<SwitchingAction> actionList = device.getActionList();

        SwitchingAction action = null;
        Interface intf;
        Interface intf_client = null;
        String layer;
        int layer_rank, size, i;

        // int currentLayer_rank=getLayerRank(currentLayer);
        int currentLayer_rank = Layer.valueOf(currentLayer).rank();
        Label labelFromStack;
        Label currentLabel = null;
        int actionCount = actionList == null ? 0 : actionList.size();
        for (int j = 0; j < actionCount; j++) {
            action = (SwitchingAction) actionList.get(j);
            logger.debug("---------Adaptation Client --------" + device.getURI() + ":" + actionCount + ":"
                    + currentLayer + ":" + action.getAtLayer());
            if (action.getAtLayer().equals(currentLayer)) {

                labelFromStack = checkLabelStack(currentLayer);
                logger.debug("Label from the stack:" + labelFromStack);

                LinkedList<Interface> interfaceList = action.getClientInterface();
                size = interfaceList.size();
                for (i = 0; i < size; i++) {
                    currentLabel = action.getLabel();

                    // replace this interface w/ adapted client interface when available: check label
                    intf = interfaceList.get(i);
                    layer = intf.getAtLayer();

                    markOccupy(intf);

                    // layer_rank=getLayerRank(layer);
                    layer_rank = Layer.valueOf(layer).rank();
                    logger.debug(intf.getURI() + ";" + layer + ":" + layer_rank + ";" + currentLayer + ":"
                            + currentLayer_rank + ";" + i);
                    if (!layer.equals(currentLayer)) {
                        if (layer_rank < currentLayer_rank) {
                            intf_client = getAdaptationClientInterface(intf, currentLayer, currentLabel,
                                    labelFromStack);
                        } else {
                            logger.error("Upper layer comes in earlier, error!");
                        }
                    } else {
                        intf_client = processClientInterface(intf, currentLabel, labelFromStack);
                    }

                    if (intf_client != null) {
                        interfaceList.remove(i);
                        interfaceList.add(i, intf_client);
                        if (currentLabel == null) {
                            action.setLabel_ID(intf_client.getLabel().label);
                            action.setLabel(intf_client.getLabel());
                        } else {
                            if (currentLabel.label <= 0) {
                                action.setLabel_ID(intf_client.getLabel().label);
                                action.setLabel(intf_client.getLabel());
                            }
                        }
                        logger.debug("Current action:" + intf_client.getURI() + ":" + action.getLabel_ID());
                    }
                }
                // pass the right para to the interface not assigning label.
                Interface tmp = null;
                Resource tmp_set = null;
                for (i = 0; i < size; i++) {
                    intf = interfaceList.get(i);
                    if (action.getLabel() == null) {
                        break;
                    }
                    if (intf.getLabel() == null) {
                        setLabel(intf, action.getLabel().label_rs);
                    } else {
                        if (intf.getLabel().label <= 0) {
                            setLabel(intf, action.getLabel().label_rs);
                        }
                    }
                    if (intf.getUsedLabelSet() == null) {
                        tmp = intf;
                    } else {
                        tmp_set = intf.getUsedLabelSet();
                    }
                }
                if (tmp != null) {
                    layer = tmp.getAtLayer();
                    Property usedLabelSet = null;
                    if (tmp_set != null) {
                        // usedLabelSet = ontModel.createProperty(NdlCommons.ORCA_NS + "layer.owl#" +
                        // Layer.valueOf(layer).getUSet());
                        usedLabelSet = ontModel.createProperty(NdlCommons.ORCA_NS + Layer.valueOf(layer).getPrefix()
                                + ".owl#" + Layer.valueOf(layer).getUSet());
                        tmp.setUsedLabelSet(tmp_set);
                        tmp.getResource().addProperty(usedLabelSet, tmp_set);

                    }
                }
                logger.debug("Label to the stack?:" + action.getLabel().toString());
                if (action.getLabel_ID() >= 0) // the edge device may not have a label ID
                    labelStack.add(action.getLabel());

                // System.out.println("Action info:"+actionCount);
                // action.print();
                switchingActionInModel(action, currentLayer, false, false, null);
            }

        }
    }

    // for now, it only works on the client port of DTN
    public void markOccupy(Interface intf) {
        Resource intf_rs = intf.getResource();
        if (intf_rs.hasProperty(ocgLine)) {
            intf_rs.addLiteral(portOccupied, true);
        }
    }

    // check the label stack to get the label from previous hops.
    public Label checkLabelStack(String currentLayer) {
        if (labelStack.empty())
            return null;
        Label exist = labelStack.peek();
        logger.debug(exist.toString() + ":" + currentLayer);
        if (exist.type.equals(currentLayer)) {
            exist = labelStack.pop();
        } else {
            if (Layer.valueOf(exist.type).rank() < Layer.valueOf(currentLayer).rank()) {
                labelStack.pop();
                if (!labelStack.empty())
                    exist = labelStack.pop();
                else
                    exist = null;
            } else {
                exist = null;
            }
        }
        return exist;
    }

    // find/create the client interface with the adaptation
    public Interface getAdaptationClientInterface(Interface parent, String currentLayer, Label currentLabel,
            Label labelFromStack) {
        LinkedList<Interface> clientList = parent.getClientInterface();
        ListIterator<Interface> it;
        Interface intf = null;
        OntResource rs_client = null;
        String url, name;
        int labelID = -1;

        Resource label_rs = null;
        logger.debug("Client:" + parent.getURI() + ":" + parent.getAtLayer() + ";" + currentLayer + "\n");
        if (clientList == null) {
            logger.debug(currentLayer + ";" + parent.getResource());
            if (currentLayer.equals(Layer.LambdaNetworkElement.toString())) {
                if (parent.getAtLayer().equals(Layer.OCGNetworkElement.toString())) {
                    // use an available label from the server interface
                    label_rs = processLabel(parent, currentLabel, labelFromStack);
                    if (label_rs != null)
                        labelID = label_rs.getProperty(label_ID).getInt();
                    Integer ID = new Integer(labelID);
                    url = parent.getURI() + "/Lambda/" + ID.toString();
                    name = parent.getName() + "-" + ID.toString();
                    intf = new Interface(this.getOntModel(), url, name);
                    intf.setAtLayer(currentLayer);

                    OntClass layerOntClass = ontModel.getOntClass(NdlCommons.ORCA_NS + "dtn.owl#LambdaNetworkElement");

                    rs_client = ontModel.createIndividual(url, layerOntClass);
                    rs_client.addRDFType(interfaceOntClass);
                    rs_client.setLabel(name, null);
                    intf.setResource(rs_client);

                    setLabel(intf, label_rs);
                    intf.addServerInterface(parent);
                    parent.addClientInterface(ontModel, intf, AdaptationProperty.WDM.toString());
                }
            }
        } else {
            it = clientList.listIterator();

            while (it.hasNext()) {
                intf = (Interface) it.next();
                intf.getResource().addProperty(interfaceOf,
                        parent.getResource().getProperty(interfaceOf).getResource());
                if (intf.getAtLayer().equals(currentLayer)) {
                    intf = processClientInterface(intf, currentLabel, labelFromStack);
                    break;
                } else {
                    intf = getAdaptationClientInterface(intf, currentLayer, currentLabel, labelFromStack);
                    if (intf != null)
                        break;
                }
            }
        }

        return intf;
    }

    // 1. generate vlan interface for Ethernet interface
    // 2. get the correct label update and assignment
    public Interface processClientInterface(Interface intf, Label currentLabel, Label labelFromStack) {
        Interface vlanInterface = null;
        int label = -1;
        Label vlan = null;
        Resource label_rs = null;

        if (intf.getAtLayer().equals(Layer.EthernetNetworkElement.toString())) {
            label_rs = processLabel(intf, currentLabel, labelFromStack);
            if (label_rs != null)
                label = label_rs.getProperty(label_ID).getInt();

            String url = intf.getURI() + "/VLAN/" + label;
            // String name=intf.getName().concat("-")+label;
            String name = intf.getName();
            vlanInterface = new Interface(this.getOntModel(), url, name);
            vlanInterface.setAtLayer(intf.getAtLayer());
            vlanInterface.setResource(ontModel.createOntResource(url));

            setLabel(vlanInterface, ontModel.getOntResource(label_rs));
            vlanInterface.addServerInterface(intf);
            intf.addClientInterface(ontModel, vlanInterface, AdaptationProperty.TaggedEthernet.toString());
        } else if (intf.getAtLayer().equals(Layer.LambdaNetworkElement.toString())) {
            label_rs = processLabel(intf, currentLabel, labelFromStack);
            setLabel(intf, ontModel.getOntResource(label_rs));
            vlanInterface = intf;
        }
        logger.debug("Obtained Label ID:" + label_rs + ":" + intf.getURI());
        return vlanInterface;
    }

    public void setLabel(Interface intf, OntResource label_rs) {

        Label label = new Label();

        Resource intf_rs = intf.getResource();

        if (label_rs != null) {
            label.label_rs = label_rs;
            label.label = label_rs.getProperty(label_ID).getFloat();
            String layer = intf.getAtLayer();
            label.type = layer;
            String prefix = Layer.valueOf(layer).getPrefix().toString();
            // String prefix;
            // if(layer.equals(Layer.EthernetNetworkElement.toString())) prefix="ethernet";
            // else prefix="dtn";
            String labelP = NdlCommons.ORCA_NS + prefix + ".owl#" + Layer.valueOf(layer).getLabelP();
            ObjectProperty label_p = ontModel.getObjectProperty(labelP);
            // System.out.println(labelP);
            intf_rs.addProperty(label_p, label_rs);
        } else
            label.label = -1;

        intf.setLabel(label);
    }

    // first fit label assignment
    public Resource processLabel(Interface intf, Label currentLabel, Label labelFromStack) {
        String rsURI = intf.getResource().getURI();
        String layer = intf.getAtLayer();
        String availableLableSet = Layer.valueOf(layer).getPrefix() + ":" + Layer.valueOf(layer).getASet();
        String usedLabelSet = Layer.valueOf(layer).getPrefix() + ":" + Layer.valueOf(layer).getUSet();
        String usedLabelSet_str = NdlCommons.ORCA_NS + Layer.valueOf(layer).getPrefix() + ".owl#"
                + Layer.valueOf(layer).getUSet();

        String usedSet = intf.getURI() + "/" + Layer.valueOf(layer).getUSet();

        String currentLayer = intf.getAtLayer();

        Resource label_rs = processLabel(rsURI, availableLableSet, usedLabelSet, usedLabelSet_str, usedSet,
                currentLabel, labelFromStack, currentLayer);

        return label_rs;
    }

    public Resource processLabel(String rsURI, String availableLableSet_str, String usedLabelSet,
            String usedLabelSet_str, String usedSet_str, Label currentLabel, Label labelFromStack,
            String currentLayer) {

        if (currentLabel != null) {
            if (currentLabel.label > 0) {
                return currentLabel.label_rs;
            }
        }

        if (currentLayer.equals(Layer.OCGNetworkElement.toString())) {
            currentLayer = Layer.LambdaNetworkElement.toString();
        }

        int label = getLayerLable(currentLayer);

        logger.debug("From available label range:" + currentLayer + ":" + label + "\n");

        int stackLabel = 0, lower = 0, upper = 0, lowest = 0, lowestUpper = 0;
        if (labelFromStack != null)
            stackLabel = (int) labelFromStack.label;

        ResultSet results;
        QuerySolution solution = null;
        String availableSet = null;

        Resource lowerLabel = null, lowestLabel = null, lowestUpperLabel = null;
        Resource upperLabel = null;
        Resource labelRange_rs = null, lowestLabelRange_rs = null;
        Resource availableSet_rs = null;
        if (label > 0) {
            results = getAvailableLabelSet(rsURI, availableLableSet_str);
            // outputQueryResult(results);
            // results= getAvailableLabelSet(rsURI,availableLableSet_str);

            String lowerBound = (String) results.getResultVars().get(0);
            String upperBound = (String) results.getResultVars().get(1);
            String l = (String) results.getResultVars().get(2);
            String u = (String) results.getResultVars().get(3);
            String setElement = (String) results.getResultVars().get(4);

            availableSet = (String) results.getResultVars().get(5);

            if (!results.hasNext()) {
                logger.error("No available label!\n");
                return null;
            }
            int i = 0;
            while (results.hasNext()) {
                solution = results.nextSolution();
                availableSet_rs = solution.getResource(availableSet);
                labelRange_rs = solution.getResource(setElement);

                if (solution.getLiteral(lowerBound) != null) {
                    lower = solution.getLiteral(lowerBound).getInt();
                    upper = solution.getLiteral(upperBound).getInt();
                    lowerLabel = solution.getResource(l);
                    upperLabel = solution.getResource(u);
                    if ((stackLabel >= lower) && (stackLabel <= upper)) {
                        label = stackLabel;
                        lowest = lower;
                        lowestLabel = lowerLabel;
                        lowestUpper = upper;
                        lowestUpperLabel = upperLabel;
                        lowestLabelRange_rs = labelRange_rs;

                        break;
                    }
                } else {
                    upper = 0;
                    upperLabel = null;
                    lowerLabel = labelRange_rs;
                    if (lowerLabel.getProperty(label_ID) != null)
                        lower = lowerLabel.getProperty(label_ID).getInt();
                    if (lower == stackLabel) {
                        label = stackLabel;
                        lowest = lower;
                        lowestLabel = lowerLabel;
                        lowestUpper = upper;
                        lowestUpperLabel = upperLabel;
                        lowestLabelRange_rs = labelRange_rs;

                        break;
                    }
                }
                if ((i == 0) || (lower <= lowest)) {
                    lowest = lower;
                    lowestLabel = lowerLabel;
                    lowestUpper = upper;
                    lowestUpperLabel = upperLabel;
                    lowestLabelRange_rs = labelRange_rs;
                }
                logger.debug("\n Label Range:" + availableSet_rs + "-" + lowestLabelRange_rs + "----:" + lower + ":"
                        + lowestUpper + ":" + lowerLabel + ":" + lowestUpperLabel + ":StackLabel=" + stackLabel
                        + ":Label=" + label);
                i++;
            }
        }
        // double check the picked label was not in the usedLabelSet
        if (stackLabel != 0) {
            if (label == 0) {
                logger.error("The passed label from the stack is not within the available label range!:" + stackLabel);
                return null; // depends on the label continuity requirement
            }
        } else {// no stack label, pick label locally
            if (label == 0) {
                label = lowest;
            }
        }

        results = getUsedLabelSet(rsURI, usedLabelSet);
        String used = (String) results.getResultVars().get(0);
        String usedSet = (String) results.getResultVars().get(1);
        Resource usedSet_rs = null;
        while (results.hasNext()) {
            solution = results.nextSolution();
            usedSet_rs = solution.getResource(usedSet);
            if (label == solution.getResource(used).getProperty(this.label_ID).getInt()) {
                logger.error("Existing Used Label:" + solution.getResource(used) + "\n");
            }
        }
        Resource picked_label_rs = ontLabelUpdate(availableSet_rs, lowestLabelRange_rs, lowest, lowestUpper,
                lowestLabel, lowestUpperLabel, stackLabel);

        // send the picked one to the usedLabelSet

        setUsedLabelSet(rsURI, usedLabelSet_str, usedSet_str, usedSet_rs, picked_label_rs);

        Resource label_rs = getLabelResource(picked_label_rs, label);

        return label_rs;
    }

    // ************************
    // Step 3:Find and create the virtue connection/link between devices in the same layer
    // ************************

    public void createVirtualConnection(NetworkConnection deviceConnection) {
        Device device = null, device_peer = null, device_peer_next = null;
        Resource rs = null, rs_peer = null, rs_peer_next = null;
        LinkedList<Device> deviceList = null;
        Iterator<Device> it;
        String type = null, type_peer = null, type_peer_next = null;

        OntResource connection_ont = null;
        LinkedList<OntResource> top_connection_ont = new LinkedList<OntResource>();

        boolean isServer = false;
        try {
            if (deviceConnection != null) {
                deviceList = deviceConnection.getConnection();

                int size = deviceList.size();
                int j = 0;
                it = deviceList.iterator();
                while (it.hasNext()) {
                    device = (Device) it.next();
                    rs = device.getResource();
                    type = device.getType();

                    if (type == null) {
                        j++;
                        continue;
                    }
                    if (type.equals("ServerCloud")) {
                        isServer = true;
                    } else {
                        isServer = false;
                    }

                    for (int i = j + 1; i < size; i++) {
                        device_peer = deviceList.get(i);
                        rs_peer = device_peer.getResource();
                        type_peer = device_peer.getType();

                        logger.debug("Device pair:" + device.getURI() + ":" + type + ";" + device_peer.getURI() + ":"
                                + type_peer + "\n");

                        if (type_peer != null) {
                            if ((!isServer) && (!type_peer.equals("Server"))) {
                                if (peerLayer(device, device_peer)) {
                                    logger.info("Device peer:" + device.getURI() + ":" + type + ";"
                                            + device_peer.getURI() + ":" + type_peer + "\n");
                                    if (i < size - 1) {
                                        device_peer_next = deviceList.get(i + 1);
                                        type_peer_next = device_peer_next.getType();
                                        if (type_peer_next != null) {
                                            if (!type_peer_next.equals("Server")) {
                                                if (peerLayer(device, device_peer_next)) {

                                                    connection_ont = createConnection(device_peer, device_peer_next,
                                                            deviceList);
                                                    if (connection_ont != null) {
                                                        top_connection_ont.add(connection_ont);
                                                    }
                                                    continue;
                                                }
                                            }
                                        }
                                    }
                                    // if(i>j+1){ // it is the next hop link, no need to generate the virtual connection
                                    connection_ont = createConnection(device, device_peer, deviceList);

                                    if (connection_ont != null)
                                        top_connection_ont.add(connection_ont);
                                    // }
                                }
                            } else if ((isServer) & (type_peer.equals("ServerCloud"))) {
                                logger.info("----No virtual connection between Servers-----" + rs.getURI() + ":"
                                        + rs_peer.getURI());
                                // connection_ont=createConnection(device,device_peer);

                                // rs_peer.addProperty(visited, "true",XSDDatatype.XSDboolean);
                            }
                        }
                        break; // no need to consider this virtual connection hop.
                    }

                    j++;
                }

                reservationOntClass = (Resource) requestModel
                        .getOntClass(NdlCommons.ORCA_NS + "request.owl#Reservation");
                carryReservation = ontModel.createOntProperty(NdlCommons.ORCA_NS + "request.owl#carryReservation");
                // carryReservation.addDomain(networkConnectionOntClass);
                // carryReservation.addRange(reservationOntClass);
                Resource random_reservation = ontModel.createResource(this.getRequestURI(), reservationOntClass);
                for (OntResource top : top_connection_ont) {
                    // logger.debug("RequestMapping: carryReservation: " + top.getURI() + ";this.reservation: " +
                    // this.reservation);
                    // top.addProperty(carryReservation, this.reservation);
                    logger.info("RequestMapping: carryReservation: " + top.getURI() + ";random.reservation: "
                            + random_reservation);
                    // top.addProperty(carryReservation, this.reservation);
                    top.addProperty(carryReservation, random_reservation);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        removeInConnectionProperty("ndl:inConnection", inConnection);
        removeInConnectionProperty("ndl:visited", visited);
    }

    public OntResource createConnection(Device device, Device device_peer, LinkedList<Device> deviceList) {

        Resource intf = null, intf_peer = null;
        // intf=findConnectionInterface(device.getURI());
        // intf_peer=findConnectionInterface(device_peer.getURI());

        // logger.debug(device.getURI()+":"+device.getDownNeighbour()+":"+"\n");
        // logger.debug(device_peer.getURI()+":"+device_peer.getUpNeighbour()+":"+"\n");
        // ontModel.write(System.out);
        // ArrayList<ArrayList<OntResource>> intf_List = findInterface(device.getResource(),device_peer.getResource());
        // if(intf_List.size()!=0){
        // intf=intf_List.get(0).get(1);
        // intf_peer=intf_List.get(1).get(1);
        // }else{

        intf = device.getDownNeighbour();
        OntResource device_ont = ontModel.getOntResource(intf.getProperty(interfaceOf).getResource());
        Device neighbor_device = getDevice(device_ont, deviceList);
        // logger.debug("This VC end point
        // 1:"+intf.getURI()+":"+neighbor_device.getURI()+":"+neighbor_device.getUpNeighbour().getURI());
        if (neighbor_device == null)
            return null;
        intf = neighbor_device.getUpNeighbour();

        intf_peer = device_peer.getUpNeighbour();
        device_ont = ontModel.getOntResource(intf_peer.getProperty(interfaceOf).getResource());
        neighbor_device = getDevice(device_ont, deviceList);
        logger.debug("This VC end point 2:" + intf_peer.getURI() + ":" + neighbor_device.getURI() + ":"
                + neighbor_device.getDownNeighbour().getURI());
        intf_peer = neighbor_device.getDownNeighbour();

        if (intf != null) {
            if (intf.getProperty(visited) != null) {
                logger.warn("Interface was used in existing VC:" + intf);
                return null;
            }
            intf.addProperty(visited, "true", XSDDatatype.XSDboolean);
        }
        if (intf_peer != null) {
            if (intf_peer.getProperty(visited) != null) {
                logger.warn("Interface was used in existing VC:" + intf_peer);
                return null;
            }
            intf_peer.addProperty(visited, "true", XSDDatatype.XSDboolean);
        }

        logger.info("----creating virtual connection-----" + device.getResource() + ":" + device_peer.getResource());

        OntResource connection_ont = null;
        ResultSet results = null;
        if ((intf != null) && (intf_peer != null)) {
            if (intf.getProperty(connectedTo) == null) {
                if (!device.getType().equals("ServerCloud")) {
                    connection_ont = toNetworkConnection(intf, intf_peer, device, device_peer);
                    intf.addProperty(connectedTo, intf_peer);
                    intf_peer.addProperty(connectedTo, intf);
                }
            }
            if (!intf.getRequiredProperty(connectedTo).getResource().equals(intf_peer)) {
                if (!device.getType().equals("ServerCloud")) {
                    connection_ont = toNetworkConnection(intf, intf_peer, device, device_peer);
                    intf.addProperty(connectedTo, intf_peer);
                    intf_peer.addProperty(connectedTo, intf);
                }
            } else {
                results = getNetworkConnection(intf.getURI(), intf_peer.getURI());
                if (results.hasNext()) {
                    String var0 = (String) results.getResultVars().get(0);
                    connection_ont = ontModel.getOntResource(results.nextSolution().getResource(var0));
                }
            }
        }

        return connection_ont;
    }

    // create networkconnection
    public OntResource toNetworkConnection(Resource intf0_rs, Resource intf1_rs, Device device0, Device device1) {
        // Form a network connection and Find the links

        logger.info("Generating connections:" + intf0_rs + ":" + intf1_rs);

        ResultSet results = getConnectionSubGraphSwitchedTo(intf0_rs.getURI(), intf1_rs.getURI());
        // this.outputQueryResult(results);
        // results=getConnectionSubGraphSwitchedTo(intf0_rs.getURI(),intf1_rs.getURI());
        if (!results.hasNext()) {
            results = getConnectionSubGraphSwitchedToAdaptation(intf0_rs.getURI(), intf1_rs.getURI());
            // this.outputQueryResult(results);
            // results=getConnectionSubGraphSwitchedToAdaptation(intf0_rs.getURI(),intf1_rs.getURI());
        }
        String var0 = (String) results.getResultVars().get(0); // ?a
        String var1 = (String) results.getResultVars().get(1); // ?b
        String var2 = (String) results.getResultVars().get(2); // ?c

        Resource in0, in1, in2;
        OntResource in0_ont, in2_ont;
        OntResource connection_ont = null;
        OntResource link_ont = null;
        QuerySolution solution;
        String url;
        Random randomGenerator = new Random();
        if (results.hasNext()) {
            url = intf0_rs.getURI() + "-" + intf1_rs.getURI().split("\\#")[1];

            connection_ont = ontModel.createIndividual(url, networkConnectionOntClass);
            connection_ont.addProperty(hasInterface, intf0_rs);
            connection_ont.addProperty(hasInterface, intf1_rs);
            logger.info("Virtual connection:" + connection_ont + ":" + intf0_rs + ":" + intf1_rs + "\n");
        }

        Hashtable<String, Resource[]> linkList = new Hashtable<String, Resource[]>();

        while (results.hasNext()) {
            solution = results.nextSolution();
            in0 = solution.getResource(var0);
            in1 = solution.getResource(var1);
            in2 = solution.getResource(var2);

            // logger.debug("Possible
            // hop:"+in0.getURI()+":"+in2.getURI()+":"+device0.getDownNeighbour()+":"+device1.getUpNeighbour()+"\n");

            if (!in2.getURI().equals(device0.getDownNeighbour()) && (device1.getDownNeighbour() == null)) // last hop
                continue;
            if (!in0.getURI().equals(device1.getUpNeighbour()) && (device0.getUpNeighbour() == null)) // first hop
                continue;

            url = in0.getURI() + "-" + in2.getURI().split("\\#")[1];
            if (in1.equals(connectedTo)) {
                Resource[] link_src = { linkConnectionOntClass, in0, in2 };
                linkList.put(url, link_src);
            }
            if (in1.equals(switchedTo)) {
                Resource[] link_src = { crossConnectOntClass, in0, in2 };
                linkList.put(url, link_src);
            }
        }
        // add the facts to the graph
        for (Entry<String, Resource[]> link : linkList.entrySet()) {
            if (link.getValue()[1].getProperty(interfaceOf).getResource() != intf0_rs.getProperty(interfaceOf)
                    .getResource()
                    & link.getValue()[2].getProperty(interfaceOf).getResource() != intf0_rs.getProperty(interfaceOf)
                            .getResource()) {
                if (link.getValue()[1].getProperty(interfaceOf).getResource() != intf1_rs.getProperty(interfaceOf)
                        .getResource()
                        & link.getValue()[2].getProperty(interfaceOf).getResource() != intf1_rs.getProperty(interfaceOf)
                                .getResource()) {
                    link_ont = ontModel.createIndividual(link.getKey(), link.getValue()[0]);
                    link_ont.addProperty(hasInterface, link.getValue()[1]);
                    link_ont.addProperty(hasInterface, link.getValue()[2]);
                    connection_ont.addProperty(item, link_ont);
                    logger.debug("Link hop:" + link.getKey() + "|" + link.getValue()[0]);
                }
            }
        }

        return connection_ont;
    }

    public boolean peerLayer(Device d1, Device d2) {
        boolean peer = false;
        LinkedList<SwitchMatrix> sw1 = d1.getSwitchingMatrix();
        LinkedList<SwitchMatrix> sw2 = d2.getSwitchingMatrix();
        Iterator<SwitchMatrix> it1 = sw1.iterator();
        Iterator<SwitchMatrix> it2 = sw2.iterator();
        String l1, l2;

        while (it1.hasNext()) {
            l1 = it1.next().getAtLayer();
            while (it2.hasNext()) {
                l2 = it2.next().getAtLayer();
                // System.out.println("Layer:"+l1+":"+l2);
                if (l1.equals(l2)) {
                    peer = true;
                    break;
                }
            }
            if (peer)
                break;
        }

        return peer;
    }

    // Real release in the RDF model
    public NetworkConnection releaseInModel(NetworkConnection connection, String requestURI) {
        LinkedList<Device> deviceList = null;
        Iterator<Device> it;
        Device device = null;
        logger.info("----Releasing CrossConnect in the ontology model: (Real Release)-----");
        if (connection != null) {
            deviceList = connection.getConnection();
            it = deviceList.iterator();
            while (it.hasNext()) {
                device = (Device) it.next();
                logger.debug("Releasing device: " + device.getURI());
                releaseCRS(device, true, requestURI);
            }
        }

        for (Entry<Resource, OntResource[]> intf_pair : nc_intf_list.entrySet()) {
            intf_pair.getValue()[0].removeProperty(this.connectedTo, intf_pair.getValue()[1]);
            intf_pair.getValue()[1].removeProperty(this.connectedTo, intf_pair.getValue()[0]);
            logger.debug("Releasing the virtual connection:" + intf_pair.getValue()[0] + ":" + intf_pair.getValue()[1]
                    + "\n");
        }

        removeInConnectionProperty("ndl:portOccupied", portOccupied);

        logger.info("Release is Done!\n");

        return connection;
    }

    // Delete a particular connection and tear down the crossconnect when proper
    public NetworkConnection releaseConnection(NetworkConnection connection, String requestURI) {
        // usedlabelset not empty?
        LinkedList<Device> deviceList = null;
        Iterator<Device> it;
        Device device = null;

        releaseNetworkConnection = new NetworkConnection();
        LinkedList<Device> releaseDeviceList = releaseNetworkConnection.getConnection();

        logger.info("----Releasing CrossConnect in Action-----");

        if (connection != null) {
            deviceList = connection.getConnection();
            Collections.reverse(deviceList);
            it = deviceList.iterator();
            while (it.hasNext()) {
                device = (Device) it.next();

                if (device.getType() == null) {
                    logger.warn("Device has no type:" + device.getName());
                    continue;
                }

                if (device.getType().equalsIgnoreCase("ServerCloud"))
                    continue;

                if (device.getResource().getProperty(visited) != null) {// underneath the occupied connection
                    continue;
                }

                releaseCRS(device, false, requestURI);
                // always remove crs of the two end devices of the connection
                Device tmpDevice = getDevice(device.getResource(), releaseDeviceList);
                if (tmpDevice == null) {
                    logger.debug("To Release:" + device.getURI());
                    releaseDeviceList.add(device);
                }
                /*
                 * if(releaseCRS(device,false,requestURI)){ Device tmpDevice =
                 * getDevice(device.getResource(),releaseDeviceList); if(tmpDevice==null) {
                 * logger.debug("To Release:"+device.getURI()); releaseDeviceList.add(device); } if(it.hasNext()){
                 * device=(Device) it.next(); //it will stop after the peer tmpDevice =
                 * getDevice(device.getResource(),releaseDeviceList); if(tmpDevice==null) {
                 * if(releaseCRS(device,false,requestURI)){ logger.debug("To Release next:"+device.getURI());
                 * releaseDeviceList.add(device); } } } //break; }
                 */
                // else
                // releaseDeviceList.add(device);
            }
        }

        releaseNetworkConnection.setConnection(releaseDeviceList);
        removeInConnectionProperty("ndl:visited", visited);
        return releaseNetworkConnection;
    }

    public boolean releaseCRS(Device device, boolean releaseFlag, String requestURI) {
        boolean valid = false;
        SwitchingAction action = null;
        LinkedList<SwitchingAction> actionList = device.getActionList();

        if (actionList == null) {
            logger.error("No switching action:" + device.getResource());
            return valid;
        }
        Iterator<SwitchingAction> it_action = actionList.iterator();

        while (it_action.hasNext()) {
            action = it_action.next();

            action.setDefaultAction(Action.Delete.toString());
            String current_layer = action.getAtLayer();

            valid = switchingActionInModel(action, current_layer, true, releaseFlag, requestURI);
            logger.info("end of releasing crs in model:" + device.getResource());
        }
        return valid;
    }

    // added "switchedTo" property in the Model
    public boolean switchingActionInModel(SwitchingAction action, String current_layer, boolean deleteFlag,
            boolean releaseFlag, String requestURI) {

        LinkedList<Interface> interfaceList = action.getClientInterface();
        Interface intf = null;
        Interface intf_next = null;
        Resource rs, rs_next;
        OntResource rs_ont, rs_next_ont;
        boolean done = false;

        Iterator<Interface> it = interfaceList.iterator();
        int size = interfaceList.size();
        int i = 0;

        while (it.hasNext()) {
            intf = it.next();
            i++;
            rs = intf.getResource();
            rs_ont = ontModel.getOntResource(rs);
            for (int j = i; j < size; j++) {
                // logger.debug(i+":"+j+":"+size);
                intf_next = interfaceList.get(j);
                rs_next = intf_next.getResource();
                rs_next_ont = ontModel.getOntResource(rs_next);
                if (deleteFlag) {
                    if (releaseFlag) {
                        removeReservation(rs_ont, rs_next_ont, requestURI);
                        releaseCrossConnect(rs_ont.getURI(), rs_next_ont.getURI());
                        returnLabel(action, intf, intf_next);
                    } else {
                        done = removeConnection(rs_ont, rs_next_ont, releaseFlag, requestURI);
                    }
                } else {
                    logger.debug("---SwitchedTo:" + rs + "----" + rs_next);
                    rs.addProperty(switchedTo, rs_next);
                    rs_next.addProperty(switchedTo, rs);
                }
            }
        }
        return done;
    }

    public boolean removeConnection(OntResource rs_ont, OntResource rs_next_ont, boolean releaseFlag,
            String requestURI) {

        boolean done = false;

        // ResultSet results=interfaceOfNetworkConnection(rs_ont.getURI());
        // if(!results.hasNext())
        // results=interfaceOfNetworkConnection(rs_next_ont.getURI());

        ResultSet results = getNetworkConnection(rs_ont, rs_next_ont);

        String var0 = null;
        if (results.hasNext()) {
            var0 = (String) results.getResultVars().get(0);
        }

        logger.info("Connection removal:" + releaseFlag + ":" + rs_ont + ":" + rs_next_ont);

        Resource connection_rs = null;
        OntResource connection_rs_ont = null;

        while (results.hasNext()) {
            connection_rs = results.nextSolution().getResource(var0);
            logger.debug("NetworkConnection of:" + connection_rs);
            if (carryOtherReservation(connection_rs, requestURI)) { // leave the connection and return
                releaseNetworkConnection(connection_rs, releaseFlag, true);
                done = true;
                break;
            } else {
                releaseNetworkConnection(connection_rs, releaseFlag, false); // try to tear down the connection
            }
        }
        return done;
    }

    public boolean carryOtherReservation(Resource nc_rs, String requestURI) {
        boolean carry = false;
        Statement st = null;
        StmtIterator stit = nc_rs.listProperties(carryReservation);
        Resource rs = null;
        OntResource rs_ont = null;

        if (stit != null) {
            while (stit.hasNext()) {
                st = stit.nextStatement();
                rs = st.getResource();
                logger.debug("Carried Reservation:" + rs + ":" + requestURI);
                if (!rs.getURI().equals(requestURI)) {
                    carry = true;
                    break;
                }
            }
        }
        return carry;
    }

    public ResultSet getNetworkConnection(OntResource rs_ont, OntResource rs_next_ont) {
        ResultSet results = interfaceOfNetworkConnection(rs_ont.getURI());
        if (!results.hasNext())
            results = interfaceOfNetworkConnection(rs_next_ont.getURI());
        return results;
    }

    // release a given network connection -> Device list w/ crs delete switching actions
    public boolean releaseNetworkConnection(Resource nc_rs, boolean releaseFlag, boolean markFlag) {
        boolean release = false;

        LinkedList<Device> releaseDeviceList = this.releaseNetworkConnection.getConnection();
        LinkedList<Resource> markDeviceList = new LinkedList<Resource>();
        logger.info("NetworkConnection release recursively:" + nc_rs);
        Statement st = null;
        StmtIterator stit = nc_rs.listProperties(item);
        Resource rs = null;
        OntResource rs_ont = null;
        if (stit != null) {
            while (stit.hasNext()) {
                st = stit.nextStatement();
                rs = st.getResource();
                rs_ont = ontModel.getOntResource(rs);
                if (rs_ont.hasRDFType(networkConnectionOntClass)) {
                    releaseNetworkConnection(rs, releaseFlag, markFlag);
                }
                if (rs_ont.hasRDFType(crossConnectOntClass)) {
                    logger.debug("Release CrossConnect:" + rs_ont);
                    Device device = getReleaseDevice(rs);
                    if (device != null) {
                        markDeviceList.add(device.getResource());
                        if (!markFlag) {
                            releaseDeviceList.add(device);
                        }
                    }
                }
            }
        }

        for (Resource d_rs : markDeviceList) {
            d_rs.addProperty(visited, "true", XSDDatatype.XSDboolean);
        }
        deleteConnectionInModel(nc_rs);

        return release;
    }

    public Device getReleaseDevice(Resource crs_rs) {

        Resource device_rs = null;
        Resource intf_rs = null;
        Resource intf_rs_next = null;

        ResultSet results = getCRSDevice(crs_rs.getURI());

        QuerySolution solution = null;

        Interface intf = null;

        String var0 = (String) results.getResultVars().get(0);
        String var1 = (String) results.getResultVars().get(1);

        if (results.hasNext()) {
            solution = results.nextSolution();
            device_rs = solution.getResource(var0);
            intf_rs = solution.getResource(var1);
        }

        if (results.hasNext()) {
            intf_rs_next = results.nextSolution().getResource(var1);
        }

        Device device = getDevice(device_rs, deviceConnection.getConnection());
        logger.debug("device:" + device_rs + ":" + crs_rs);
        if (device == null) {
            device = getDevice(device_rs, releaseNetworkConnection.getConnection());
            if (device == null) {
                device = new Device(ontModel, this, device_rs);

                SwitchingAction action = new SwitchingAction();

                intf = new Interface(this, ontModel.getOntResource(intf_rs), false);
                action.addInterface(intf);
                intf = new Interface(this, ontModel.getOntResource(intf_rs_next), false);
                action.addInterface(intf);
                action.setAtLayer(intf.getAtLayer());
                action.setDefaultAction(Action.Delete.toString());

                device.addSwitchingAction(action);
                if (device.getDirection().equals(Direction.UNIDirectional.toString())) {
                    device.processUNIInterface(action.getAtLayer());
                    action.setDefaultAction(Action.Temporary.toString());
                }
            } else
                device = null;
        } else {
            Device tmpDevice = getDevice(device_rs, this.releaseNetworkConnection.getConnection());
            if (tmpDevice != null)
                device = null;
        }
        if (device != null)
            logger.info("Release CRS device:" + device.getURI());

        return device;
    }

    Hashtable<Resource, OntResource[]> nc_intf_list = new Hashtable<Resource, OntResource[]>();

    public void deleteConnectionInModel(Resource vc) {
        OntResource[] nc_intf = new OntResource[2];
        StmtIterator stit_nc = vc.listProperties(this.hasInterface);
        OntResource rs_nc;
        int i = 0;
        while (stit_nc.hasNext()) {

            rs_nc = ontModel.getOntResource(stit_nc.nextStatement().getResource());
            nc_intf[i] = rs_nc;
            i++;
        }
        // nc_intf[0].removeProperty(this.connectedTo, nc_intf[1]);
        // nc_intf[1].removeProperty(this.connectedTo, nc_intf[0]);
        nc_intf_list.put(vc, nc_intf);

        logger.debug("Connection interfaces to be removed put in the waiting list:" + nc_intf[0] + ":" + nc_intf[1]);
    }

    public void releaseCrossConnect(String rs1, String rs2) {

        String s = "SELECT ?r ";
        String f = "";
        String w = "WHERE {" + "?r ndl:hasInterface " + "<" + rs1 + ">." + "?r ndl:hasInterface " + "<" + rs2 + ">."
                + " ?r rdf:type " + "ndl:CrossConnect" + "      }";
        String queryPhrase = createQueryString(s, f, w);

        ResultSet results = rdfQuery(ontModel, queryPhrase);

        String var0 = null;
        if (results.hasNext())
            var0 = (String) results.getResultVars().get(0);

        Resource crs_rs = null;

        if (results.hasNext()) {
            crs_rs = results.nextSolution().getResource(var0);
            logger.info("Tear dwon CRS:" + crs_rs);
            // crs_rs.removeProperties();
        }
    }

    public Resource returnLabel(SwitchingAction action, Interface intf1, Interface intf2) {

        if (action.getLabel() == null)
            return null;

        Resource label_rs = action.getLabel().label_rs;
        Resource rs1_parent = null;
        Interface parent = null;
        String layer = null, prefix = null;
        ;

        if (intf1.getResource().getProperty(adaptationPropertyOf) != null)
            rs1_parent = intf1.getResource().getProperty(adaptationPropertyOf).getResource();

        parent = intf1.getServerInterface().element();

        if (parent != null)
            layer = parent.getAtLayer();

        if (layer.equals(Layer.EthernetNetworkElement.toString()))
            prefix = "ethernet";
        else
            prefix = "dtn";

        String aSet = NdlCommons.ORCA_NS + prefix + ".owl#" + Layer.valueOf(layer).getASet();
        ObjectProperty aSet_p = ontModel.getObjectProperty(aSet);

        OntResource rs1_parent_availableSet = null;

        if (rs1_parent.getProperty(aSet_p) != null)
            rs1_parent_availableSet = ontModel.getOntResource(rs1_parent.getProperty(aSet_p).getResource());

        if (rs1_parent_availableSet == null) { // try the second interface
            if (intf2.getResource().getProperty(adaptationPropertyOf) != null) {
                rs1_parent = intf2.getResource().getProperty(adaptationPropertyOf).getResource();
                parent = intf2.getServerInterface().element();

                if (parent != null) {
                    layer = parent.getAtLayer();
                    aSet = NdlCommons.ORCA_NS + prefix + ".owl#" + Layer.valueOf(layer).getASet();
                    aSet_p = ontModel.getObjectProperty(aSet);
                    if (rs1_parent.getProperty(aSet_p) != null)
                        rs1_parent_availableSet = ontModel.getOntResource(rs1_parent.getProperty(aSet_p).getResource());
                }
            }

        }

        logger.debug("This labeled interface:" + layer + ":" + aSet_p + ":" + rs1_parent + ":" + rs1_parent_availableSet
                + "\n");

        if (rs1_parent_availableSet == null)
            return null;

        String uSet = NdlCommons.ORCA_NS + prefix + ".owl#" + Layer.valueOf(layer).getUSet();
        ObjectProperty uSet_p = ontModel.createObjectProperty(uSet);
        OntResource rs1_parent_usedSet = null;
        logger.debug("Returned used label:" + uSet_p + ":" + rs1_parent + ":" + rs1_parent.getProperty(uSet_p));
        if (rs1_parent.getProperty(uSet_p) != null) {
            rs1_parent_usedSet = ontModel.getOntResource(rs1_parent.getProperty(uSet_p).getResource());
            if (rs1_parent_usedSet.hasProperty(element, label_rs) && (label_rs != null)) {
                rs1_parent_usedSet.removeProperty(element, label_rs);
                rs1_parent_availableSet.addProperty(element, label_rs);
            }

            logger.info("Returned used label:" + label_rs + ":" + rs1_parent_availableSet.hasProperty(element, label_rs)
                    + ":" + rs1_parent_usedSet + ":" + rs1_parent_usedSet.hasProperty(element, label_rs) + "\n");
        }
        return label_rs;
    }

    public void removeReservation(OntResource rs_ont, OntResource rs_next_ont, String requestURI) {
        rs_ont.removeProperty(switchedTo, rs_next_ont);
        rs_next_ont.removeProperty(switchedTo, rs_ont);

        ResultSet results = getNetworkConnection(rs_ont, rs_next_ont);

        String var0 = null;
        if (results.hasNext()) {
            var0 = (String) results.getResultVars().get(0);
        }

        logger.info("Reservation in Connection removal:" + requestURI + ":" + rs_ont + ":" + rs_next_ont);

        Resource connection_rs = null;
        OntResource connection_rs_ont = null;
        LinkedList<OntResource> connectionReservationList = new LinkedList<OntResource>();
        while (results.hasNext()) {
            connection_rs = results.nextSolution().getResource(var0);
            if (connection_rs != null) {
                connection_rs_ont = ontModel.getOntResource(connection_rs);
                // connection_rs_ont.removeProperty(carryReservation, ontModel.getResource(requestURI));
                // logger.info("Carried Reservation removed:"+ontModel.getResource(requestURI)+":"+connection_rs+"\n");
                connectionReservationList.add(connection_rs_ont);
            }
        }
        for (OntResource or : connectionReservationList) {
            or.removeProperty(carryReservation, ontModel.getResource(requestURI));
            logger.info("Carried Reservation removed:" + ontModel.getResource(requestURI) + ":" + connection_rs + "\n");
        }

    }

    public Resource findConnectionInterface(String deviceURI) {

        boolean sw = false;
        ResultSet results = getSwitchedToAdaptation(deviceURI);
        outputQueryResult(results);
        results = getSwitchedToAdaptation(deviceURI);

        if (!results.hasNext()) {
            results = getSwitchedToInterface(deviceURI);
            outputQueryResult(results);
            results = getSwitchedToInterface(deviceURI);
        }
        Resource intf = null;

        String switchingCapability = null;

        String var0 = (String) results.getResultVars().get(0);
        String var1 = (String) results.getResultVars().get(1);
        String var2 = (String) results.getResultVars().get(2);

        QuerySolution solution;
        while (results.hasNext()) {
            solution = results.nextSolution();
            intf = solution.getResource(var0);
            switchingCapability = solution.getResource(var2).getLocalName();
            if (isSwitcheable(intf, switchingCapability)) {
                sw = true;
                break;
            }
        }

        if (sw == false) {
            results = getSwitchedToAdaptation(deviceURI);

            // outputQueryResult(results);

            intf = null;

            switchingCapability = null;

            var0 = (String) results.getResultVars().get(0);
            var1 = (String) results.getResultVars().get(1);
            var2 = (String) results.getResultVars().get(2);

            while (results.hasNext()) {
                solution = results.nextSolution();
                intf = solution.getResource(var0);
                switchingCapability = solution.getResource(var2).getLocalName();
                // System.out.println(switchingCapability);
                if (isSwitcheableAdaptation(intf, switchingCapability)) {
                    sw = true;
                    break;
                }
            }
        }

        if (sw)
            return intf;
        else
            return null;
    }

    // directly switcheable in the neighbor: eg DTN ->Polatis: fiberswitching
    public boolean isSwitcheable(Resource intf, String switchingCapability) {
        boolean sw = false;
        Resource layer = null;
        ResultSet results = getLayer(ontModel, intf.getURI());
        String var0 = (String) results.getResultVars().get(0);
        QuerySolution solution;
        while (results.hasNext()) {
            solution = results.nextSolution();
            layer = solution.getResource(var0);
            // System.out.println(intf.getURI()+":"+layer);
            if (layer.getLocalName().equals(switchingCapability)) {
                sw = true;
                break;
            }
        }
        return sw;
    }

    // need adaptation switching in the neighbour, eg: 6509->DTN:Lambdaswitching
    public boolean isSwitcheableAdaptation(Resource intf, String switchingCapability) {
        boolean sw = false;
        Resource rs_client, layer;
        ResultSet results = getLayerAdapatation(intf.getURI());
        String var0 = (String) results.getResultVars().get(0);
        QuerySolution solution = null;
        while (results.hasNext()) {
            solution = results.nextSolution();
            // System.out.println(solution.getResource(var0).getURI()+":"+switchingCapability);
            rs_client = solution.getResource(var0);
            if (isSwitcheable(rs_client, switchingCapability)) {
                sw = true;
                break;
            } else if (isSwitcheableAdaptation(rs_client, switchingCapability)) {
                sw = true;
                break;
            }
        }

        return sw;
    }

}
