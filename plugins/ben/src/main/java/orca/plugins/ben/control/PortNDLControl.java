package orca.plugins.ben.control;

import java.util.Properties;

import orca.embed.cloudembed.PortHandler;
import orca.embed.policyhelpers.RequestReservation;
import orca.policy.core.ResourceControl;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.common.ConfigurationException;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.common.meta.ResourceProperties;
import orca.shirako.common.meta.UnitProperties;
import orca.shirako.core.Ticket;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.core.Units;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.plugins.config.ConfigToken;
import orca.shirako.time.Term;
import orca.shirako.util.ResourceData;
import orca.util.PropList;
import orca.util.ResourceType;

public class PortNDLControl extends ResourceControl {
    public static final String PropertySubstrateFile = "substrate.file";
    public static final String PropertyRequestID = "request.id";
    public static final String PropertyRequestNdl = "request.ndl";

    /**
     * The resource type this control is responsible for.
     */
    protected ResourceType type;

    /**
     * The NDL network handler.
     */
    protected PortHandler handler;

    /**
     * This control can have only one reservation waiting for the completion of
     * a configuration action. This flag indicates if an action is in progress.
     * If this flag is true, the control is going to delay all requests for new
     * resources until the configuration action completes.
     */
    protected boolean inprogress = false;

    synchronized void setInprogress(boolean val) {
      inprogress = val;
    }

    @Override
    public void donate(IClientReservation r) throws Exception {
        if (handler != null) {
            throw new Exception("only a single source reservation is supported");
        }

        ResourceSet set = r.getResources();
        ResourceType rtype = r.getType();
        // note: ignoring term
        type = rtype;
        // note: all demo required this as a resource property
        // new setup (gec7) requires it as a local property.
        String substrateFile = PropList.getRequiredProperty(set.getResourceProperties(), PropertySubstrateFile);
        if (substrateFile == null) {
            substrateFile = PropList.getRequiredProperty(set.getLocalProperties(), PropertySubstrateFile);
        }
        
        if (substrateFile == null) {
            throw new ConfigurationException("Missing substrate file property");
        }
            
        //System.out.println("Substrate file: " + substrateFile);
        handler = new PortHandler(substrateFile);

    }

    /**
     * {@inheritDoc}
     */
    public ResourceSet assign(IAuthorityReservation r) throws Exception {
        if (handler == null) {
            throw new Exception("no inventory");
        }

        // get the requested resources
        ResourceSet requested = r.getRequestedResources();
        // get the requested resource type
        ResourceType type = requested.getType();

        // get the currently assigned resources (if any)
        ResourceSet current = r.getResources();
        // get the ticket
        Ticket ticket = (Ticket) requested.getResources();
        // get the resource ticket
        ResourceTicket rt = ticket.getTicket();
        // get the properties assigned by the broker
        Properties ticketProperties = rt.getProperties();
        // get the requested lease term
        Term term = r.getRequestedTerm();
        

        // convert to cycles
        long start = authority.getActorClock().cycle(term.getNewStartTime());
        long end = authority.getActorClock().cycle(term.getEndTime());

        // determine if this is a new lease or an extension request
        if (current == null) {
            // this is a new request
            // how much does the client want?
            long needed = ticket.getUnits();
            if (needed > 1) {
                r.fail("Cannot assign more than 1 port per reservation");
            }

            if (inprogress) {
                // we can handle only one request at a time
                // delay this request until the current request completes
                return null;
            }

            if (handler != null) {
                // set the inprogress flag
                setInprogress(true);
                // FIXME: get this from the client request
                String requestSpec = requested.getConfigurationProperties().getProperty(PropertyRequestNdl);
                
                RequestReservation rr = handler.getRequestReservation(requestSpec);
                handler.handleRequest(rr);
                String uri = rr.getReservation();
                String portList=handler.getPortListToString(uri);
                
                Properties handlerProperties = BenNdlPropertiesConverter.convert(portList, logger);

                ResourceData rd = new ResourceData();
                PropList.setProperty(rd.getResourceProperties(), UnitProperties.UnitPortList, handlerProperties.getProperty(UnitProperties.UnitPortList));
                PropList.setProperty(rd.getLocalProperties(), PropertyRequestID, uri);

                UnitSet gained = new UnitSet(authority.getShirakoPlugin());

                Unit u = new Unit();
                u.setResourceType(type);
                u.setProperty(UnitProperties.UnitPortList, handlerProperties.getProperty(UnitProperties.UnitPortList));
                // if the broker allocated specific bandwidth, set it as a property on the unit
                if (ticketProperties.containsKey(ResourceProperties.ResourceBandwidth)) {
                    long bw = Long.parseLong(ticketProperties.getProperty(ResourceProperties.ResourceBandwidth));
                }

                // attach the handler properties to the node properties list
                u.mergeProperties(handlerProperties);
                u.setProperty(PropertyRequestID, uri);

                gained.add(u);

                return new ResourceSet(gained, null, null, type, rd);
            } else {
                // no resource - delay the allocation
                return null;
            }
        } else {
            // extend automatically
            return new ResourceSet(null, null, null, type, null);
        }
    }

    @Override
    public void configurationComplete(String action, ConfigToken token, Properties outProperties) {
        super.configurationComplete(action, token, outProperties);

        // we handle only one action at a time, so we simply reset the
        // inprogress bit
        setInprogress(false);
    }

    @Override
    public void close(IReservation reservation) {
        super.close(reservation);

        Unit u = null;

        try {
            u = ((UnitSet) reservation.getResources().getResources()).getSet().iterator().next();
        } catch (Exception e) {
            u = null;
        }
        //Do nothing, the setup and teardown interface lists are the same.
        //if (u != null) {
            String uri = u.getProperty(PropertyRequestID);
            // unset the setup properties
            //String portList = handler.getPortListToString();
            //Properties setupProperties = BenNdlPropertiesConverter.convert(portList);
            //u.unsetProperties(setupProperties);
            // set the teardown properties
            //NetworkConnection teardown = handler.getConnectionTeardownActions(uri);
            //Properties teardownProperties = BenNdlPropertiesConverter.convert(teardown);
            //u.mergeProperties(teardownProperties);
        //}
    }

    protected void releaseResources(Unit u) throws Exception {
        String requestID = u.getProperty(PropertyRequestID);
        if (requestID == null) {
            throw new Exception("Request id is missing");
        }
        handler.releaseReservation(requestID);
    }

    protected void free(Units units) throws Exception {
        if (units != null) {
            for (Unit u : units) {
                try {
                    releaseResources(u);
                } catch (Exception e) {
                    logger.error("Failed to release vlan tag", e);
                }
            }
        }
    }

    public void revisit(IReservation r) throws Exception {
        UnitSet uset = (UnitSet) (r.getResources().getResources());   
        for (Unit u : uset.getSet()) {
            try {
                /*
                 * No need for locking. No other thread is accessing nodes of
                 * recovered reservations. State cannot be Closed, since closed
                 * nodes are filtered in NodeGroup.revisit(Actor).
                 */
                switch (u.getState()){
                  default:
                        throw new RuntimeException("Revisit for BenNdlControl is not supported yet");                   
                }
            } catch (Exception e) {
                fail(u, "revisit with vlancontrol", e);
            }
        }
    }


}
