package net.exogeni.orca.embed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.exogeni.orca.embed.workflow.Domain;
import net.exogeni.orca.ndl.DomainResourceType;
import net.exogeni.orca.ndl.DomainResources;
import net.exogeni.orca.ndl.NdlException;
import net.exogeni.orca.shirako.common.meta.ResourcePoolAttributeDescriptor;
import net.exogeni.orca.shirako.common.meta.ResourcePoolAttributeType;
import net.exogeni.orca.shirako.common.meta.ResourcePoolDescriptor;
import net.exogeni.orca.shirako.common.meta.ResourcePoolsDescriptor;
import net.exogeni.orca.shirako.common.meta.ResourceProperties;
import net.exogeni.orca.util.ResourceType;

public class MappingHandlerTest extends TestCase {
    String[] inputFileName = { "net/exogeni/orca/ndl/substrate/slvmsite.rdf", "net/exogeni/orca/ndl/substrate/mass.rdf",
            "net/exogeni/orca/ndl/substrate/nictavmsite.rdf", "net/exogeni/orca/ndl/substrate/rcivmsite.rdf",
            "net/exogeni/orca/ndl/substrate/bbnvmsite.rdf", "net/exogeni/orca/ndl/substrate/fiuvmsite.rdf", "net/exogeni/orca/ndl/substrate/uhvmsite.rdf",
            "net/exogeni/orca/ndl/substrate/uflvmsite.rdf", "net/exogeni/orca/ndl/substrate/osfvmsite.rdf",
            "net/exogeni/orca/ndl/substrate/cienavmsite.rdf", "net/exogeni/orca/ndl/substrate/uvanlvmsite.rdf",
            "net/exogeni/orca/ndl/substrate/ben-6509.rdf", "net/exogeni/orca/ndl/substrate/nlr.rdf", "net/exogeni/orca/ndl/substrate/starlight.rdf",
            "net/exogeni/orca/ndl/substrate/ion.rdf", "net/exogeni/orca/ndl/substrate/uvanlNet.rdf", "net/exogeni/orca/ndl/substrate/uflNet.rdf",
            "net/exogeni/orca/ndl/substrate/uncNet.rdf", "net/exogeni/orca/ndl/substrate/osfNet.rdf", "net/exogeni/orca/ndl/substrate/slNet.rdf",
            "net/exogeni/orca/ndl/substrate/rciNet.rdf", "net/exogeni/orca/ndl/substrate/bbnNet.rdf", "net/exogeni/orca/ndl/substrate/fiuNet.rdf",
            "net/exogeni/orca/ndl/substrate/cienaNet.rdf", "net/exogeni/orca/ndl/substrate/uhNet.rdf", "net/exogeni/orca/ndl/substrate/learnNet.rdf",
            "net/exogeni/orca/ndl/substrate/learn.rdf", "net/exogeni/orca/ndl/substrate/nictaNet.rdf" };

    String requestFile, requestFileDuke, requestFile33, requestFileVMSDuke, requestFileUNC, requestFileRenci,
            requestFileDukeUnc;
    String requestFile2, requestFileNSF, requestFileDukeRenci, requestFile0, requestFileUncRenci, requestFileConverter;
    String requestFileDuke26Node;

    public String requestFileGush, requestFileGPO;

    String requestFileRenciNersc;
    String requestFileCondor, requestFileHadoop, requestFileDukeVMSVlan, requestFileRenciVMSVlan;
    String substrateFileName, rencivmsubstrateFileName, dukevmsubstrateFileName, uncvmsubstrateFileName;
    String requestFileDukeUHouston, requestFileDukeRice;
    String requestFileFluke;

    public ResourcePoolsDescriptor pools;
    public List<String> abstractModels;

    protected void setUp() throws Exception {
        super.setUp();

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // Empty test. Fixes error: Failed tests: warning(junit.framework.TestSuite$1): No tests found in
    // orca.embed.MappingHandlerTest
    public void testNothing() {

    }

    public List<String> getAbstractModels() throws IOException, NdlException {
        abstractModels = new ArrayList<String>();
        pools = new ResourcePoolsDescriptor();
        String abstractModel = null;
        int numDomain = inputFileName.length;
        String[] type = { "site.vm", "site.vlan" };
        String[] vmvlantype = { "site.vm", "site.vlan", "site.baremetalce" };
        String[] lunvmvlantype = { "site.vm", "site.vlan", "site.baremetalce", "site.lun" };
        DomainResources domainResources = null;

        ResourcePoolDescriptor pool = null;

        for (int i = 0; i < numDomain; i++) {
            System.out.println("Abstracting domain:" + i);
            if ((i < 3) || (i == 7) || (i == 8)) {
                Domain d = new Domain(inputFileName[i]);
                abstractModel = d.delegateDomainModelToString("site.vm");
                try {
                    domainResources = d.getDomainResources(abstractModel, 2);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                pool = getResourcePoolDescriptor(domainResources);
                // System.out.println(pool.getResourceType().toString()+":"+pool.getDescription());
                pools.add(pool);
                abstractModels.add(abstractModel);
            } else if (i < 7) {
                for (String j : lunvmvlantype) {
                    Domain d = new Domain(inputFileName[i]);
                    abstractModel = d.delegateDomainModelToString(j);
                    try {
                        domainResources = d.getDomainResources(abstractModel, 2);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    pool = getResourcePoolDescriptor(domainResources);
                    pools.add(pool);
                    abstractModels.add(abstractModel);
                }
            } else if (i < 9) {
                for (String j : vmvlantype) {
                    Domain d = new Domain(inputFileName[i]);
                    abstractModel = d.delegateDomainModelToString(j);
                    try {
                        domainResources = d.getDomainResources(abstractModel, 2);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    pool = getResourcePoolDescriptor(domainResources);
                    pools.add(pool);
                    abstractModels.add(abstractModel);
                }
            } else if (i < 11) {
                for (String j : type) {
                    Domain d = new Domain(inputFileName[i]);
                    abstractModel = d.delegateDomainModelToString(j);
                    try {
                        domainResources = d.getDomainResources(abstractModel, 2);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    pool = getResourcePoolDescriptor(domainResources);
                    pools.add(pool);
                    abstractModels.add(abstractModel);
                }
            }
            /*
             * else if(i==10){ Domain d = new Domain(inputFileName[i]); abstractModel =
             * d.delegateDomainModelToString("nerscvmsite.vm");
             * 
             * domainResources=d.getDomainResources(abstractModel,20); pool=getResourcePoolDescriptor(domainResources);
             * pools.add(pool); abstractModels.add(abstractModel);
             * 
             * d = new Domain(inputFileName[i]); abstractModel = d.delegateDomainModelToString("nerscvmsite.GEPort");
             * 
             * domainResources=d.getDomainResources(abstractModel,2); pool=getResourcePoolDescriptor(domainResources);
             * pools.add(pool); abstractModels.add(abstractModel); }
             */
            else {
                Domain d = new Domain(inputFileName[i]);
                abstractModel = d.delegateDomainModelToString("site.vlan");
                try {
                    domainResources = d.getDomainResources(abstractModel, 5);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                pool = getResourcePoolDescriptor(domainResources);
                pools.add(pool);
                abstractModels.add(abstractModel);
                // if(i==9){
                // System.out.println(abstractModel);
                // }
            }
        }

        return abstractModels;
    }

    protected ResourcePoolDescriptor getResourcePoolDescriptor(DomainResources domainResources) {
        ResourcePoolDescriptor pool = new ResourcePoolDescriptor();

        String rdf = null;
        DomainResourceType dType = domainResources.getResourceType().get(0);
        rdf = dType.getDomainURL().split("\\#")[0];

        String type = dType.getResourceType().toLowerCase();

        String value = dType.generateDomainName(rdf, type);

        ResourceType rType = new ResourceType(value + "." + type);

        // System.out.println("ResourceDescriptor:"+rdf+":"+type+":"+value+":"+rType.toString());

        pool.setResourceType(rType);
        pool.setUnits(domainResources.getResourceType().get(0).getCount());

        ResourcePoolAttributeDescriptor att = new ResourcePoolAttributeDescriptor();
        att.setKey(ResourceProperties.ResourceDomain);
        att.setValue(value);
        pool.addAttribute(att);

        att = new ResourcePoolAttributeDescriptor();
        att.setKey(ResourceProperties.ResourceAvailableUnits);
        att.setType(ResourcePoolAttributeType.INTEGER);
        att.setValue(String.valueOf(domainResources.getResourceType().get(0).getCount()));
        pool.addAttribute(att);

        return pool;
    }

}
