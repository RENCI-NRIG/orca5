package orca.embed;

import orca.embed.workflow.Domain;
import orca.ndl.DomainResourceType;
import orca.ndl.DomainResources;
import orca.ndl.NdlException;
import orca.shirako.common.meta.*;
import orca.util.ResourceType;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class EmbedTestHelper {

    public static void populateModelsAndPools(List<String> abstractModels, ResourcePoolsDescriptor pools,
            Map<Domain, Map<String, Integer>> resourceMap) throws IOException, NdlException {
        for (Domain domain : resourceMap.keySet()) {
            Map<String, Integer> resource = resourceMap.get(domain);

            for (String resourceType : resource.keySet()) {
                String abstractModel = domain.delegateDomainModelToString(resourceType);
                abstractModels.add(abstractModel);

                DomainResources domainResources = domain.getDomainResources(abstractModel, resource.get(resourceType));
                pools.add(getResourcePoolDescriptor(domainResources, abstractModel));
            }
        }
    }

    protected static ResourcePoolDescriptor getResourcePoolDescriptor(DomainResources domainResources,
            String abstractModel) {
        ResourcePoolDescriptor pool = new ResourcePoolDescriptor();

        String rdf = null;
        DomainResourceType dType = domainResources.getResourceType().get(0);
        rdf = dType.getDomainURL().split("\\#")[0];

        String type = dType.getResourceType().toLowerCase();

        String value = DomainResourceType.generateDomainName(rdf, type);

        ResourceType rType = new ResourceType(value + "." + type);

        // System.out.println("ResourceDescriptor:"+rdf+":"+type+":"+value+":"+rType.toString());

        pool.setResourceType(rType);
        pool.setResourceTypeLabel("label");
        pool.setUnits(domainResources.getResourceType().get(0).getCount());

        ResourcePoolAttributeDescriptor att = new ResourcePoolAttributeDescriptor();
        att.setKey(ResourceProperties.ResourceDomain);
        att.setType(ResourcePoolAttributeType.STRING);
        att.setValue(value);
        pool.addAttribute(att);

        att = new ResourcePoolAttributeDescriptor();
        att.setKey(ResourceProperties.ResourceAvailableUnits);
        att.setType(ResourcePoolAttributeType.INTEGER);
        att.setValue(String.valueOf(domainResources.getResourceType().get(0).getCount()));
        pool.addAttribute(att);

        att = new ResourcePoolAttributeDescriptor();
        att.setKey(ResourceProperties.ResourceNdlAbstractDomain);
        att.setType(ResourcePoolAttributeType.STRING);
        att.setValue(abstractModel);
        pool.addAttribute(att);

        return pool;
    }

}
