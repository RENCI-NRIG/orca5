package orca.network.policyhelpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import orca.shirako.meta.ResourceProperties;
import orca.shirako.meta.ResourcePoolAttributeDescriptor;
import orca.shirako.meta.ResourcePoolAttributeType;

import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class VMPolicyNDLPoolPropertyExtractor extends GenericNDLPoolPropertyExtractor {

    public static String resourceMemoryLabel = "Memory";
    public static String resourceCPULabel = "CPU";
    public static String GBUnit = "GB";

    public VMPolicyNDLPoolPropertyExtractor(String modelStr) throws IOException {
        super(modelStr);
        // TODO Auto-generated constructor stub
    }

    @Override
    public List<ResourcePoolAttributeDescriptor> getPoolAttributes() {
        ArrayList<ResourcePoolAttributeDescriptor> attrList = new ArrayList<ResourcePoolAttributeDescriptor>();
        Resource set = null;
        Statement unitServerStm, cpuStm, memoryStm;
        ResourcePoolAttributeDescriptor attr;
        for (NodeIterator j = delegateModel.listObjectsOfProperty(ontProcessor.availableLabelSet); j.hasNext();) {
            set = (Resource) j.next();
            unitServerStm = set.getProperty(ontProcessor.hasUnitServer);
            if (unitServerStm != null) { // to get possible unit server (VM) specification
                Resource unitServer = unitServerStm.getResource();
                cpuStm = unitServer.getProperty(ontProcessor.cpuCapacity);
                if (cpuStm != null) {
                    attr = new ResourcePoolAttributeDescriptor();
                    attr.setKey(ResourceProperties.ResourceCPU);
                    attr.setValue(String.valueOf(cpuStm.getFloat()));
                    attr.setUnit(GBUnit);
                    attr.setType(ResourcePoolAttributeType.FLOAT);
                    attr.setLabel(resourceCPULabel);
                    attrList.add(attr);
                }
                memoryStm = unitServer.getProperty(ontProcessor.memoryCapacity);
                if (memoryStm != null) {
                    attr = new ResourcePoolAttributeDescriptor();
                    attr.setKey(ResourceProperties.ResourceMemory);
                    attr.setType(ResourcePoolAttributeType.FLOAT);
                    attr.setValue(String.valueOf(memoryStm.getFloat()));
                    attr.setUnit(GBUnit);
                    attr.setLabel(resourceMemoryLabel);
                    attrList.add(attr);
                }
            }
        }

        return attrList;
    }

}
