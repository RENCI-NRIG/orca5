package orca.network.policyhelpers;

import java.io.IOException;
import java.util.Properties;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Statement;

public class VlanPolicyNDLPoolPropertyExtractor extends GenericNDLPoolPropertyExtractor {

    public static String vlanStart = "vlan.tag.start";
    public static String vlanEnd = "vlan.tag.end";

    public VlanPolicyNDLPoolPropertyExtractor(String modelStr) throws IOException {
        super(modelStr);
        // TODO Auto-generated constructor stub
    }

    @Override
    public Properties getPoolProperties() {
        Properties vlanProperty = new Properties();
        Resource set, setElement, lb, ub;
        Statement setStm, lbStm, ubStm;
        int lb_id = 0, ub_id = 0;
        for (NodeIterator j = delegateModel.listObjectsOfProperty(ontProcessor.availableLabelSet); j.hasNext();) {
            set = (Resource) j.next();
            for (StmtIterator element = set.listProperties(ontProcessor.element); element.hasNext();) {
                setElement = element.next().getResource();

                lbStm = setElement.getProperty(ontProcessor.lowerBound);
                ubStm = setElement.getProperty(ontProcessor.upperBound);
                if (lbStm != null) {
                    lb = lbStm.getResource();
                    lb_id = lb.getProperty(ontProcessor.label_ID).getInt();
                }
                if (ubStm != null) {
                    ub = ubStm.getResource();
                    ub_id = ub.getProperty(ontProcessor.label_ID).getInt();
                }
                // single label
                if ((lbStm == null) && (ubStm == null)) {
                    setStm = setElement.getProperty(ontProcessor.label_ID);
                    if (setStm != null) {
                        lb_id = setStm.getInt();
                        ub_id = lb_id;
                    }
                }

                vlanProperty.setProperty(vlanStart, String.valueOf(lb_id));
                vlanProperty.setProperty(vlanEnd, String.valueOf(ub_id));
            }
        }

        return vlanProperty;
    }

}
