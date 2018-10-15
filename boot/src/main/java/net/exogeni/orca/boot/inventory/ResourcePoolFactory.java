package net.exogeni.orca.boot.inventory;

import java.util.Date;

import net.exogeni.orca.security.AuthToken;
import net.exogeni.orca.shirako.api.IAuthorityProxy;
import net.exogeni.orca.shirako.api.IClientReservation;
import net.exogeni.orca.shirako.api.IProxy;
import net.exogeni.orca.shirako.api.ISlice;
import net.exogeni.orca.shirako.common.ConfigurationException;
import net.exogeni.orca.shirako.common.delegation.ResourceDelegation;
import net.exogeni.orca.shirako.common.delegation.ResourceTicket;
import net.exogeni.orca.shirako.common.meta.ResourcePoolDescriptor;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.shirako.core.Ticket;
import net.exogeni.orca.shirako.kernel.ClientReservationFactory;
import net.exogeni.orca.shirako.kernel.ResourceSet;
import net.exogeni.orca.shirako.plugins.substrate.ISubstrate;
import net.exogeni.orca.shirako.registry.ActorRegistry;
import net.exogeni.orca.shirako.time.ActorClock;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.shirako.util.ResourceData;
import net.exogeni.orca.util.PropList;

public class ResourcePoolFactory implements IResourcePoolFactory {
    /**
     * The resource pool descriptor. Its initial version is passed during initialization. The factory can manipulate it
     * as it sees fit and returns it back the the PoolCreator.
     */
    protected ResourcePoolDescriptor desc;
    /**
     * The actor's substrate
     */
    protected ISubstrate substrate;
    /**
     * The authority proxy for this actor.
     */
    protected IAuthorityProxy proxy;
    /**
     * Slice representing the resource pool.
     */
    protected ISlice slice;

    /**
     * Modifies the resource pool descriptor as needed
     * @throws ConfigurationException in case of error
     */
    protected void updateDescriptor() throws ConfigurationException {
        /*
         * Use this function to modify the resource pool descriptor, as needed. For example, you can define attributes
         * and resource pool properties needed by the resource pool. Resource pool attributes will become resource
         * properties (of the pool/slice and source reservation), while properties attached to the resource pool
         * descriptor will become local properties.
         */
    }

    /**
     * Creates the term for the source reservation.
     * @return Term 
     * @throws ConfigurationException in case of error
     */
    protected Term createTerm() throws ConfigurationException {
        ActorClock clock = substrate.getActor().getActorClock();

        long now = Globals.getContainer().getCurrentCycle();
        Date start = desc.getStart();
        if (start == null) {
            start = clock.cycleStartDate(now);
        }
        Date end = desc.getEnd();
        if (end == null) {
            // export for one year
            long length = 1000 * 60 * 60 * 24 * 365;
            end = clock.cycleEndDate(now + length);
        }
        return new Term(start, end);
    }

    /**
     * Creates the resource ticket for the source reservation
     * 
     * @param term term
     * @return ResourceTicket
     *
     * @throws ConfigurationException in case of error
     */
    protected ResourceTicket createResourceTicket(Term term) throws ConfigurationException {
        // NOTE/FIXME: we could/should use the properties list inside the delegation to pass in properties
        // that we want to be certified and protected by the signature. For now this properties list is empty, but
        // we may consider making this properties list contain all resource properties.
        try {
            ResourceDelegation rd = substrate.getTicketFactory().makeDelegation(desc.getUnits(), term,
                    desc.getResourceType());
            return substrate.getTicketFactory().makeTicket(rd);
        } catch (Exception e) {
            throw new ConfigurationException("Could not make ticket", e);
        }
    }

    /**
     * Use this function to set additional resource and local properties to be associated with the source reservation.
     * 
     * @return ResourceData
     * @throws ConfigurationException in case of error
     */
    protected ResourceData createResourceData() throws ConfigurationException {
        ResourceData rdata = new ResourceData();
        // the slice resource properties go in the resource properties of the source reservation
        PropList.mergeProperties(slice.getResourceProperties(), rdata.getResourceProperties());
        // the resource pool properties go in the local properties of the source reservation
        PropList.mergeProperties(desc.getPoolProperties(), rdata.getLocalProperties());
        // pool.name is a resource property that equals the name of the resource pool
        rdata.getResourceProperties().setProperty("pool.name", slice.getName());
        return rdata;
    }

    public IClientReservation createSourceReservation(ISlice slice) throws ConfigurationException {
        this.slice = slice;
        Term term = createTerm();
        ResourceTicket resourceTicket = createResourceTicket(term);
        Ticket ticket = new Ticket(resourceTicket, substrate, proxy);
        ResourceData rdata = createResourceData();
        ResourceSet resources = new ResourceSet(ticket, desc.getResourceType(), rdata);
        IClientReservation r = ClientReservationFactory.getInstance().create(resources, term, slice);
        ClientReservationFactory.getInstance().setAsSource(r);
        return r;

    }

    public ResourcePoolDescriptor getDescriptor() throws ConfigurationException {
        updateDescriptor();
        return desc;
    }

    public void setDescriptor(ResourcePoolDescriptor desc) throws ConfigurationException {
        this.desc = desc;
    }

    public void setSubstrate(ISubstrate substrate) throws ConfigurationException {
        this.substrate = substrate;
        AuthToken auth = substrate.getActor().getIdentity();
        try {
            proxy = (IAuthorityProxy) ActorRegistry.getProxy(IProxy.ProxyTypeLocal, auth.getName());
            if (proxy == null) {
                throw new RuntimeException("Missing proxy");
            }
        } catch (Exception e) {
            throw new ConfigurationException("Could not obtain authority proxy: " + auth.getName(), e);
        }
    }
}
