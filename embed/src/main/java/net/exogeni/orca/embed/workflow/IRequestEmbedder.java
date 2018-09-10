package net.exogeni.orca.embed.workflow;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;

import net.jwhoisserver.utils.InetNetworkException;
import net.exogeni.orca.embed.policyhelpers.DomainResourcePools;
import net.exogeni.orca.embed.policyhelpers.ModifyElement;
import net.exogeni.orca.embed.policyhelpers.RequestReservation;
import net.exogeni.orca.embed.policyhelpers.SystemNativeError;
import net.exogeni.orca.ndl.elements.DomainElement;

import com.hp.hpl.jena.ontology.OntModel;

/**
 * This interface defines an object capable of turning Ndl request model, associated collection basic unbound elements
 * into a collection of bound elements
 * 
 * @author ibaldin
 *
 */
public interface IRequestEmbedder {

    public SystemNativeError runEmbedding(boolean bound, RequestReservation request,
            DomainResourcePools domainResourcePools);

    public SystemNativeError runEmbedding(String domainName, RequestReservation request,
            DomainResourcePools domainResourcePools);

    public SystemNativeError modifySlice(DomainResourcePools domainResourcePools,
            Collection<ModifyElement> modifyElements, OntModel manifestOnt, String sliceId,
            HashMap<String, Collection<DomainElement>> nodeGroupMap, HashMap<String, DomainElement> firstGroupElement,
            OntModel requestModel, OntModel modifyRequestModel) throws UnknownHostException, InetNetworkException;

}
