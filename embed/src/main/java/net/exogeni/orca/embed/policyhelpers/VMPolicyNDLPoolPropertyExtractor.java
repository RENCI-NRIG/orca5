package net.exogeni.orca.embed.policyhelpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.exogeni.orca.ndl.NdlCommons;
import net.exogeni.orca.shirako.common.meta.ResourcePoolAttributeDescriptor;
import net.exogeni.orca.shirako.common.meta.ResourcePoolAttributeType;
import net.exogeni.orca.shirako.common.meta.ResourceProperties;

import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

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
        Resource domain = null, set = null;
        Statement unitServerStm, cpuStm, memoryStm;
        ResourcePoolAttributeDescriptor attr;

        for (ResIterator j = delegateModel.listResourcesWithProperty(NdlCommons.RDF_TYPE,
                NdlCommons.networkDomainOntClass); j.hasNext();) {
            domain = j.next();
            break;
        }
        if (domain == null)
            return null;
        Resource networkService = domain.getProperty(NdlCommons.domainHasServiceProperty).getResource();
        for (StmtIterator j = networkService.listProperties(ontProcessor.availableLabelSet); j.hasNext();) {
            set = (Resource) j.next().getResource();
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
