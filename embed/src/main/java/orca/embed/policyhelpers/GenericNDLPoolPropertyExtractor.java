package orca.embed.policyhelpers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import orca.ndl.OntProcessor;
import orca.shirako.common.meta.ResourcePoolAttributeDescriptor;

import com.hp.hpl.jena.ontology.OntModel;

public class GenericNDLPoolPropertyExtractor {

    OntModel delegateModel;
    OntProcessor ontProcessor;

    public GenericNDLPoolPropertyExtractor(String str) throws IOException {
        ByteArrayInputStream modelStream = new ByteArrayInputStream(str.getBytes());

        ontProcessor = new OntProcessor(modelStream);
        delegateModel = ontProcessor.getOntModel();
    }

    public Properties getPoolProperties() {
        return null;
    }

    public List<ResourcePoolAttributeDescriptor> getPoolAttributes() {
        return null;
    }

}
