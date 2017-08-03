package orca.shirako.common.delegation;

import java.util.Properties;

import orca.shirako.api.IActor;
import orca.shirako.common.ResourceVector;
import orca.shirako.container.Globals;
import orca.shirako.time.Term;
import orca.util.ID;
import orca.util.ResourceType;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.SingleValueConverter;

// FIXME: if caller is not using resource bins, then cannot have a ticket with
// multiple sources

public class SimpleResourceTicketFactory implements IResourceTicketFactory
{
    class IDConverter implements SingleValueConverter
    {

        public String toString(Object obj)
        {
            return ((ID) obj).toString();
        }

        public Object fromString(String value)
        {
            return new ID(value);
        }

        public boolean canConvert(Class type)
        {
            return type.equals(ID.class);
        }
    }

    protected XStream xsm;

    protected IActor actor;

    private boolean initialized = false;

    public SimpleResourceTicketFactory()
    {
        xsm = new XStream();
    }

    public void initialize() throws Exception
    {
        if (!initialized) {
            if (actor == null) {
                throw new Exception("Factory does not have an actor");
            }
            initialize(xsm);
            initialized = true;
        }
    }

    protected void initialize(XStream xs)
    {
        assert xs != null;

        xs.alias("delegation", ResourceDelegation.class);
        xs.alias("ticket", ResourceTicket.class);
        xs.alias("bin", ResourceBin.class);
        xs.alias("id", ID.class);
        xs.useAttributeFor(ID.class, "id");
        xs.useAttributeFor(ResourceType.class, "type");
        xs.registerConverter(new IDConverter());

        xs.omitField(Term.class, "cycleStart");
        xs.omitField(Term.class, "cycleEnd");
        xs.omitField(Term.class, "cycleNewStart");
        xs.omitField(ResourceTicket.class, "factory");
        
        // use this one in production
        //xs.setMode(XStream.ID_REFERENCES);
        // use this one for demo: clearer
        xs.setMode(XStream.NO_REFERENCES);
    }

    protected void ensureInitialized() throws RuntimeException
    {
        if (!initialized) {
            throw new RuntimeException("ticket factory has not been initialized");
        }
    }

    protected ID getIssuerID()
    {
        return actor.getIdentity().getGuid();
    }

    public ResourceDelegation makeDelegation(int units, Term term, ResourceType type) throws DelegationException
    {
        return makeDelegation(units, null, term, type, null, null, null, getIssuerID());
    }

    public ResourceDelegation makeDelegation(int units, Term term, ResourceType type, Properties properties) throws DelegationException
    {
        return makeDelegation(units, null, term, type, null, null, properties, getIssuerID());
    }

    public ResourceDelegation makeDelegation(int units, Term term, ResourceType type, ID holder) throws DelegationException
    {
        return makeDelegation(units, null, term, type, null, null, null, holder);
    }

    public ResourceDelegation makeDelegation(int units, Term term, ResourceType type, Properties properties, ID holder) throws DelegationException
    {
        return makeDelegation(units, null, term, type, null, null, properties, holder);
    }

    public ResourceDelegation makeDelegation(int units, ResourceVector vector, Term term, ResourceType type, ID[] sources, ResourceBin[] bins, Properties properties, ID holder) throws DelegationException
    {

        ensureInitialized();
        if ((sources == null && bins != null) || (sources != null && bins == null)) {
            throw new DelegationException("sources and bins must both be null or non-null");
        }

        ResourceDelegation result = new ResourceDelegation(units, vector, term, type, sources, bins, properties, getIssuerID(), holder);
        return result;
    }

    public ResourceTicket makeTicket(ResourceDelegation delegation) throws TicketException
    {
        ensureInitialized();
        return new ResourceTicket(this, delegation);
    }

    public ResourceTicket makeTicket(ResourceTicket source, ResourceDelegation delegation) throws TicketException
    {
        ensureInitialized();
        return new ResourceTicket(this, source, delegation);
    }

    public ResourceTicket makeTicket(ResourceTicket[] sources, ResourceDelegation delegation) throws TicketException
    {
        assert sources != null;

        ensureInitialized();
        // the delegation must have number of sources bins
        if (delegation.bins == null || delegation.bins.length != sources.length) {
            throw new TicketException("Delegation does not use resource bins or insufficient number of bins. Operation not supported");
        }

        return new ResourceTicket(this, sources, delegation);
    }

    public void setActor(IActor actor)
    {
        this.actor = actor;
    }

    public IActor getActor()
    {
        return actor;
    }
    
    public String toXML(ResourceTicket ticket)
    {
        assert ticket != null;
        ensureInitialized();

        if (Globals.Log.isDebugEnabled()){
            Globals.Log.debug("Ticket has delegations: " + ticket.getDelegation().toString());
        }

        String xml = xsm.toXML(ticket);
        if (Globals.Log.isTraceEnabled()){
            Globals.Log.trace("Ticket converted to XML: " + xml);
        }
        return xml;
    }

    public ResourceTicket fromXML(String xml)
    {
        ensureInitialized();
        ResourceTicket rt = (ResourceTicket) xsm.fromXML(xml);
        rt.setFactory(this);
        return rt;
    }

    public ResourceTicket clone(ResourceTicket ticket)
    {
        ensureInitialized();
        String xml = toXML(ticket);
        return fromXML(xml);
    }
}
