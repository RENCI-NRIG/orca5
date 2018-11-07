package orca.network.policyhelpers;

import java.io.IOException;
import java.util.Properties;

import orca.shirako.container.Globals;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;

public class VlanPolicyNDLPropertyExtractor extends GenericNDLPoolPropertyExtractor {

    public static String vlanStart = "vlan.tag.start";
    public static String vlanEnd = "vlan.tag.end";

    public VlanPolicyNDLPropertyExtractor(String modelStr) throws IOException {
        super(modelStr);
        // TODO Auto-generated constructor stub
    }

    @Override
    public Properties getPoolProperties() {
        Properties vlanProperty = new Properties();
        Resource set;
        for (ResIterator j = delegateModel.listResourcesWithProperty(ontProcessor.availableLabelSet); j.hasNext();) {
            set = j.next();
            Resource lb = set.getProperty(ontProcessor.lowerBound).getResource();
            Resource ub = set.getProperty(ontProcessor.upperBound).getResource();

            float lb_id = lb.getProperty(ontProcessor.label_ID).getFloat();
            float ub_id = lb.getProperty(ontProcessor.label_ID).getFloat();

            vlanProperty.setProperty(vlanStart, String.valueOf(lb_id));
            vlanProperty.setProperty(vlanEnd, String.valueOf(ub_id));
        }

        return vlanProperty;
    }

}
