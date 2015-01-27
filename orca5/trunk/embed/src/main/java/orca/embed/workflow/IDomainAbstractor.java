package orca.embed.workflow;

import java.io.IOException;

import orca.ndl.DomainResources;
import orca.ndl.NdlException;

import com.hp.hpl.jena.ontology.OntModel;

public interface IDomainAbstractor {
	// to be called by the broker policy, to get the allocatable resource <type, unit>, and <interface,bw>, etc constraints
	public DomainResources getDomainResources(String str, int total) throws IOException, NdlException;
	// to be called by the AM policy, to get the abstract domain NDL
	public OntModel delegateDomainModel(String resourceType);
}
