package orca.shirako.common.delegation;


import java.util.Date;

import orca.shirako.api.IActor;
import orca.shirako.container.OrcaTestCase;
import orca.shirako.time.Term;
import orca.util.ID;
import orca.util.ResourceType;

public class SimpleTicketTest extends OrcaTestCase
{
    protected IResourceTicketFactory[] factories;
    
    long now = System.currentTimeMillis();        
    Term term = new Term(new Date(now), new Date(now + 1000 * 60 * 60 *24));
    ResourceType type = new ResourceType((new ID()).toString());
    int units = 100;                     
    
    
    public SimpleTicketTest() throws Exception
    {
        createFactories();
    }
    
    protected IResourceTicketFactory makeTicketFactory()
    {
        return new SimpleResourceTicketFactory();
    }
    
    protected void createFactories() throws Exception
    {
        factories = new IResourceTicketFactory[5];
        
        // create the site
        IActor actor = getActor("site0", new ID());
        factories[0] = makeTicketFactory();
        factories[0].setActor(actor);
        factories[0].initialize();

        // create the sm
        actor = getActor("sm0", new ID());
        factories[1] = makeTicketFactory();
        factories[1].setActor(actor);
        factories[1].initialize();

        for (int i = 2; i < 5; i++){
            actor = getActor("broker" + Integer.toString(i), new ID());
            factories[i] = makeTicketFactory();
            factories[i].setActor(actor);
            factories[i].initialize();
        }           
    }
    
    public void testSimpleTicket() throws Exception
    {        
        ResourceDelegation d0 = factories[0].makeDelegation(units, term, type);        
        ResourceTicket ticket0 = factories[0].makeTicket(d0);
        System.out.println(factories[0].toXML(ticket0));
        
        ResourceDelegation d1 = factories[0].makeDelegation(units-10, term, type, factories[2].getActor().getGuid());
        ResourceTicket ticket1 = factories[0].makeTicket(ticket0, d1);

        System.out.println(factories[0].toXML(ticket1));
    }


    public void testWithBins() throws Exception
    {        
        ResourceBin b0 = new ResourceBin(units, term);        
        ResourceDelegation d0 = factories[0].makeDelegation(units, null, term, type, new ID[] {b0.getGuid()}, new ResourceBin[]{b0}, null, new ID(factories[0].getActor().getName())); 
        ResourceTicket ticket0 = factories[0].makeTicket(d0);
        System.out.println(factories[0].toXML(ticket0));

        ResourceBin b1 = new ResourceBin(b0.getGuid(), units - 10, term);
        ResourceDelegation d1 = factories[0].makeDelegation(units-10, null, term, type, new ID[] {b1.getGuid()}, new ResourceBin[]{b1}, null, new ID(factories[2].getActor().getName())); 
        ResourceTicket ticket1 = factories[0].makeTicket(ticket0, d1);
        System.out.println(factories[0].toXML(ticket1));
    }

}