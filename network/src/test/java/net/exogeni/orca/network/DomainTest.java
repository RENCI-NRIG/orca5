package orca.network;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;
import orca.embed.workflow.Domain;
import orca.ndl.DomainResource;
import orca.ndl.DomainResourceType;
import orca.ndl.DomainResources;
import orca.ndl.elements.DomainElement;
import orca.network.policyhelpers.VMPolicyNDLPoolPropertyExtractor;
import orca.network.policyhelpers.VlanPolicyNDLPoolPropertyExtractor;

public class DomainTest extends TestCase {
    String inputFileNameTest;
    Domain domainTest;
    String[] inputFileName = { "orca/network/dukevmsite.rdf", "orca/network/rencivmsite.rdf",
            "orca/network/uncvmsite.rdf", "orca/network/mass.rdf", "orca/network/ben-6509.rdf", "orca/network/nlr.rdf",
            "orca/network/starlight.rdf", "orca/network/dukeNet.rdf", "orca/network/renciNet.rdf",
            "orca/network/uncNet.rdf" };

    // String [] inputFileName={"orca/network/rencivmsite.rdf"};

    protected void setUp() throws Exception {
        super.setUp();
        inputFileNameTest = "orca/network/rencivmsite.rdf";
        domainTest = new Domain(inputFileNameTest);
    }

    public void testGetDomain() {
        System.out.println(domainTest.getDomain());
    }

    public void testGetDomainPrefix() {
        System.out.println(domainTest.getDomainElement().getDomainPrefix(domainTest.getDomainElement().getResource()));
    }

    public void testFindBorderInterface() {
        domainTest.findBorderInterface(domainTest.getDomainElement().getModel());
    }

    public void testDelegateDomainModel() {
        System.out.println(domainTest.delegateDomainModelToString());
    }

    public void testDelegateDomain() throws IOException {
        int numDomain = inputFileName.length;
        String[] type = { "site.vm", "site.vlan" };
        Domain domain = null;
        String abstractModel = null;
        VMPolicyNDLPoolPropertyExtractor vmProperty;
        VlanPolicyNDLPoolPropertyExtractor vlanProperty;

        for (int i = 0; i < numDomain; i++) {
            System.out.println("\n Substrate: " + inputFileName[i]);
            if (i < 3) {
                for (String j : type) {
                    domain = new Domain(inputFileName[i]);
                    abstractModel = domain.delegateDomainModelToString(j);
                    // System.out.println(abstractModel);
                    DomainResources res = domain.getDomainResources(abstractModel);
                    assertNotNull(res);
                    List<DomainResourceType> types = res.getResourceType();
                    assertNotNull(types);
                    System.out.println("Resource Type count: " + types.size());
                    for (DomainResourceType t : types) {
                        System.out.println("Resource type: " + t.getResourceType() + ": Count= " + t.getCount());
                    }

                    List<DomainResource> all = res.getResources();
                    assertNotNull(all);
                    System.out.println("Interface count: " + all.size());
                    for (DomainResource r : all) {
                        System.out.println("Interface: " + r.getInterface() + ": bw=" + r.getBandwidth());
                    }

                    if (j.equals("site.vm")) {
                        vmProperty = new VMPolicyNDLPoolPropertyExtractor(abstractModel);
                        System.out.println(vmProperty.getPoolAttributes().toString());
                    } else {
                        vlanProperty = new VlanPolicyNDLPoolPropertyExtractor(abstractModel);
                        System.out.println(vlanProperty.getPoolProperties().toString());
                    }
                }
                // continue;
            } else if (i == 3) {
                domain = new Domain(inputFileName[i]);
                abstractModel = domain.delegateDomainModelToString("vise.vm");
                vmProperty = new VMPolicyNDLPoolPropertyExtractor(abstractModel);
                System.out.println(vmProperty.getPoolAttributes().toString());
            } else {
                domain = new Domain(inputFileName[i]);
                abstractModel = domain.delegateDomainModelToString("site.vlan");
                vlanProperty = new VlanPolicyNDLPoolPropertyExtractor(abstractModel);
                System.out.println(vlanProperty.getPoolProperties().toString());
            }

            DomainResources res = domain.getDomainResources(abstractModel);
            assertNotNull(res);
            List<DomainResourceType> types = res.getResourceType();
            assertNotNull(types);
            System.out.println("Resource Type count: " + types.size());
            for (DomainResourceType t : types) {
                System.out.println("Resource type: " + t.getResourceType() + ": Count= " + t.getCount());
            }

            List<DomainResource> all = res.getResources();
            assertNotNull(all);
            System.out.println("Interface count: " + all.size());
            for (DomainResource r : all) {
                System.out.println("Interface: " + r.getInterface() + ": bw=" + r.getBandwidth());
            }
        }
    }

    public void testAbstractDomain() {
        if (domainTest.abstractDomain(domainTest.getDomainElement().getModel(), null) != null)
            assertTrue(true);
    }

    public void testDelegateFullModelToString() {
        System.out.println(domainTest.delegateFullModelToString());
    }
}
