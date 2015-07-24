package orca.plugins.ben.broker;

import java.util.Date;
import java.util.Properties;

import orca.embed.workflow.Domain;
import orca.plugins.ben.broker.NDLVlanInventory;
import orca.policy.core.BrokerSimplerUnitsPolicyTest;
import orca.shirako.api.IBrokerReservation;
import orca.shirako.common.meta.RequestProperties;
import orca.shirako.common.meta.ResourcePoolAttributeDescriptor;
import orca.shirako.common.meta.ResourcePoolAttributeType;
import orca.shirako.common.meta.ResourcePoolDescriptor;
import orca.shirako.common.meta.ResourceProperties;
import orca.util.CompressEncode;
import orca.util.ResourceType;

public class NDLVlanInventoryTest extends BrokerSimplerUnitsPolicyTest {
    
    @Override
    protected ResourcePoolDescriptor getPoolDescriptor(ResourceType rtype) {
        ResourcePoolDescriptor rd = new ResourcePoolDescriptor();
        rd.setResourceType(rtype);
        rd.setResourceTypeLabel("Pool label: " + rtype);
        
        // obtain the abstract NDL model
        String abstractModel = null;
        try {
            Domain d = new Domain("orca/network/ben-6509.rdf");
            abstractModel = d.delegateDomainModelToString();            
        } catch(Exception e) {
            throw new RuntimeException("Could not read substrate ndl");
        }

        // set the abstract NDL
        ResourcePoolAttributeDescriptor attr = new ResourcePoolAttributeDescriptor();
        attr.setType(ResourcePoolAttributeType.NDL);
        attr.setKey(ResourceProperties.ResourceNdlAbstractDomain);
        attr.setValue(CompressEncode.compressEncode(abstractModel));
        attr.setLabel("Abstract Domain NDL");
        rd.addAttribute(attr);

        // the the inventory class
        attr = new ResourcePoolAttributeDescriptor();
        attr.setType(ResourcePoolAttributeType.CLASS);
        attr.setKey(ResourceProperties.ResourceClassInventoryForType);
        attr.setValue(NDLVlanInventory.class.getCanonicalName());       
        rd.addAttribute(attr);
        return rd;
    }
    
    @Override
    protected IBrokerReservation getRequest(int units, ResourceType type, Date start, Date end) {
        IBrokerReservation request = super.getRequest(units, type, start, end);
        Properties p = request.getRequestedResources().getRequestProperties();
        
        p.setProperty(RequestProperties.RequestBandwidth, "100000");
        p.setProperty(RequestProperties.RequestStartIface, "http://geni-orca.renci.org/owl/ben-6509.rdf#Renci/Cisco/6509/TenGigabitEthernet/3/6/ethernet");
        p.setProperty(RequestProperties.RequestEndIface, "http://geni-orca.renci.org/owl/ben-6509.rdf#Duke/Cisco/6509/GigabitEthernet/5/1/ethernet");        
        
        return request;
    }
}