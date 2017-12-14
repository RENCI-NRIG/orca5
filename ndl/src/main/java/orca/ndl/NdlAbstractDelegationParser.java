package orca.ndl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import orca.ndl.elements.Label;
import orca.ndl.elements.LabelSet;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.ResultBinding;

public class NdlAbstractDelegationParser extends NdlCommons {
    INdlAbstractDelegationModelListener listener;
    OntModel delegationModel;
    Set<Resource> interfaces = new HashSet<Resource>();

    /**
     * Creates an in-memory model
     * 
     * @param delegation
     * @param l
     * @throws NdlException
     */
    public NdlAbstractDelegationParser(String delegation, INdlAbstractDelegationModelListener l) throws NdlException {
        if ((delegation == null) || (l == null)) {
            throw new NdlException("Null parameters to the NdlAbstractDelegationParser constructor");
        }

        listener = l;
        // by default use new document manager and use TDB
        delegationModel = NdlModel.getModelFromString(delegation, null, true);
    }

    public NdlAbstractDelegationParser(String delegation, INdlAbstractDelegationModelListener l, NdlModel.ModelType t,
            String folderName) throws NdlException {
        if ((delegation == null) || (l == null)) {
            throw new NdlException("Null parameters to the NdlAbstractDelegationParser constructor");
        }

        listener = l;
        delegationModel = NdlModel.getModelFromString(delegation, null, true, t, folderName);
    }

    /**
     * Parse out the labelset.
     * 
     * @param lsr
     * @return
     */
    private List<LabelSet> parseLabelSet(Resource lsr) {
        // each label has resource type (e.g. VLAN or VM)
        // each label set has size
        // label set can have an element that indicates min/max boundaries
        DomainResourceType drt = NdlCommons.getDomainResourceType(lsr);
        List<LabelSet> lsl = new ArrayList<LabelSet>();
        LabelSet ls = null;
        StmtIterator si = lsr.listProperties(collectionElementProperty);
        if (si.hasNext()) {
            while (si.hasNext()) {
                Resource lsp = si.next().getResource();
                Label upper = NdlCommons.getUpperLabel(lsp);
                Label lower = NdlCommons.getLowerLabel(lsp);
                if ((upper != null) && (lower != null))
                    ls = new LabelSet(lower, upper, lsr);
                else {
                    // see if it is a single label
                    Label single = NdlCommons.parseLayerLabel_1(lsp);
                    if (single != null) {
                        ls = new LabelSet(single, single, lsr);
                    }
                }
                if (ls == null)
                    continue;
                ls.setSize = getSizeProperty(lsr);
                ls.resourceUri = lsr.getURI();
                ls.LabelType = drt.resourceType;
                Statement isAllocSt = lsr.getProperty(domainIsAllocatable);
                if (isAllocSt != null) {
                    if ("false".equalsIgnoreCase(isAllocSt.getString()))
                        ls.setNotAllocatable();
                }
                lsl.add(ls);
            }
        } else {
            ls = new LabelSet();
            ls.setSize = getSizeProperty(lsr);
            ls.resourceUri = lsr.getURI();
            ls.LabelType = drt.resourceType;
            lsl.add(ls);
        }
        return lsl;
    }

    /**
     * Parse the model. Look for network domains , nodes and network connections, and finally all interfaces. Note that
     * in the current (02/2012) delegation algorithm domains expose no internal structure, so there will be no nodes or
     * connections.
     * 
     * @throws NdlException
     */
    public synchronized void processDelegationModel() throws NdlException {
        if (delegationModel == null)
            return;

        String query;
        ResultSet rs;
        ResultBinding result;

        // network domains
        {
            for (ResIterator resit = delegationModel.listResourcesWithProperty(RDF_TYPE, networkDomainOntClass); resit
                    .hasNext();) {
                Resource dom = resit.next();

                List<Resource> nsl = new ArrayList<Resource>();
                List<LabelSet> lsl = new ArrayList<LabelSet>();

                for (StmtIterator nsIt = dom.listProperties(domainHasServiceProperty); nsIt.hasNext();) {
                    Resource ns = nsIt.next().getResource();
                    nsl.add(ns);

                    // get services and label sets
                    for (StmtIterator alsIt = ns.listProperties(availableLabelSet); alsIt.hasNext();) {
                        // als.add(alsIt.next().getResource());
                        Resource lsr = alsIt.next().getResource();

                        List<LabelSet> lsT = parseLabelSet(lsr);
                        if (lsT != null)
                            lsl.addAll(lsT);
                    }
                }

                // find interface network labels (VLAN tags)
                // we are looking for availableLabelSet. We could also be looking for availableVLANSet,
                // since those are duplicated in the model.
                Map<Resource, List<LabelSet>> netLabelSets = new HashMap<Resource, List<LabelSet>>();
                for (Resource domInt : getResourceInterfaces(dom)) {
                    List<LabelSet> mapLsl = new ArrayList<LabelSet>();
                    for (StmtIterator alsInt = domInt.listProperties(availableLabelSet); alsInt.hasNext();) {
                        Resource lsr = alsInt.next().getResource();

                        List<LabelSet> lsT = parseLabelSet(lsr);
                        if (lsT != null)
                            mapLsl.addAll(lsT);
                    }
                    netLabelSets.put(domInt, mapLsl);
                }

                // call listener
                listener.ndlNetworkDomain(dom, delegationModel, nsl, getResourceInterfaces(dom), lsl, netLabelSets);

                interfaces.addAll(getResourceInterfaces(dom));
            }
        }

        // nodes
        {

        }

        // network connections
        {

        }

        {
            // now all interface details (IP/netmask)
            for (Iterator<Resource> it = interfaces.iterator(); it.hasNext();) {
                Resource tmpR = it.next();
                // run a query on what is connected to this interface

                query = NdlCommons.createQueryStringWhoHasInterface(tmpR);
                rs = OntProcessor.rdfQuery(delegationModel, query);
                if ((rs == null) || (!rs.hasNext())) {
                    continue;
                }
                result = (ResultBinding) rs.next();
                Resource conn = null, node = null;
                if (result != null) {
                    Resource u = (Resource) result.get("item");
                    if (u.hasProperty(RDF_TYPE, topologyLinkConnectionClass)
                            || u.hasProperty(RDF_TYPE, topologyNetworkConnectionClass))
                        conn = u;
                    else
                        node = u;
                }
                if (rs.hasNext()) {
                    result = (ResultBinding) rs.next();
                    if (result != null) {
                        Resource u = (Resource) result.get("item");
                        if (u.hasProperty(RDF_TYPE, topologyLinkConnectionClass)
                                || u.hasProperty(RDF_TYPE, topologyNetworkConnectionClass))
                            conn = u;
                        else
                            node = u;
                    }
                }
                // retrieve IP address, netmask (may need to turn inference on and
                // do the inverse of hasInterface if we need a list of what it connects to)
                // does it have localIPAddress?
                Statement locIPStmt = tmpR.getProperty(ip4LocalIPAddressProperty);
                if (locIPStmt != null) {
                    listener.ndlInterface(tmpR, delegationModel, conn, node, getAddressIP(locIPStmt.getResource()),
                            getAddressNetmask(locIPStmt.getResource()));
                } else
                    listener.ndlInterface(tmpR, delegationModel, conn, node, null, null);
            }

        }

        listener.ndlParseComplete();
    }

    /**
     * Dispose of the internal model and free up space
     */
    public synchronized void freeModel() {
        NdlModel.closeModel(delegationModel);
        delegationModel = null;
    }
}
