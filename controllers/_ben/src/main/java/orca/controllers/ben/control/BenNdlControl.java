package orca.controllers.ben.control;

import java.io.ByteArrayInputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Map.Entry;

import orca.ndl.NetworkConnection;
import orca.network.NetworkHandler;
import orca.policy.core.ResourceControl;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.common.ResourceData;
import orca.shirako.common.ResourceType;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.container.ConfigurationException;
import orca.shirako.core.Ticket;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.core.Units;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.meta.ResourceProperties;
import orca.shirako.meta.UnitProperties;
import orca.shirako.plugins.config.ConfigToken;
import orca.shirako.time.Term;
import orca.util.PropList;

public class BenNdlControl extends ResourceControl {
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
    protected NetworkHandler handler;

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
            
        System.out.println("Substrate file: " + substrateFile);
        handler = new NetworkHandler(substrateFile);

    }

    /**
     * {@inheritDoc}
     */
    public ResourceSet assign(IAuthorityReservation r) throws Exception {
	System.out.println("*****************BenNdlControl assign being called!!****************");
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
        /*
        HashMap<String, String> map = new HashMap  <String, String> ();
        Properties configuration_properties = requested.getConfigurationProperties();
        String edge_interface="",edge_interface_remote="",domain="";
        for(Object key:  configuration_properties.keySet()){
        	String key_str = (String) key;
        	if(key_str.contains("edge.interface.remote")){
        		edge_interface_remote=configuration_properties.getProperty(key_str);
        		domain = key_str.split("\\.")[0];
        		map.put(domain, edge_interface_remote);
        		logger.debug("Remote domain:"+domain+"; Remote Edge Interface:"+edge_interface_remote);
        	}
        }
        
        String parent_tag = "",parent_tag_key="";
        for(Entry <String, String> pair:map.entrySet()){
        	domain = pair.getKey();
        	parent_tag_key = domain + "." + UnitProperties.UnitVlanTag;
        	parent_tag=(String) configuration_properties.getProperty(parent_tag_key);
            logger.info("Configuration Parent tag:"+parent_tag_key+"="+parent_tag);
        	if(parent_tag==null){
        		logger.debug("No this tag:"+parent_tag_key);
        		continue;
        	}
        	String [] parent_tag_array = parent_tag.split("\\|");
        	if(parent_tag_array.length<2){
        		logger.debug("Parent Not from ION or NLR:" + domain+":"+parent_tag+"\n");
        	}
        	else{
        		if(parent_tag_array[1].equals(map.get(domain))){
        			parent_tag=parent_tag_array[2];
        		}
        		if(parent_tag_array[3].equals(map.get(domain))){
        			parent_tag=parent_tag_array[4];
        		}
        		logger.debug("Parent unit tag:"+domain+" :"+parent_tag);
        		configuration_properties.remove(parent_tag_key);
        		configuration_properties.put(parent_tag_key,parent_tag);
        	}
        	logger.info("New Configuration Parent tag:"+parent_tag_key+"="+parent_tag);
        }
        
        requested.setConfigurationProperties(configuration_properties);
        */
        // convert to cycles
        long start = authority.getActorClock().cycle(term.getNewStartTime());
        long end = authority.getActorClock().cycle(term.getEndTime());
System.out.println("BenNdlControl: BEN Handler starts the RequestMapping!\n");
        // determine if this is a new lease or an extension request
        if (current == null) {
            // this is a new request
            // how much does the client want?
            long needed = ticket.getUnits();
            if (needed > 1) {
                r.fail("Cannot assign more than 1 VLAN per reservation");
            }

            if (inprogress) {
                // we can handle only one request at a time
                // delay this request until the current request completes
System.out.println("BenNdlControl:In Progress, retuen null");
                return null;
            }
System.out.println("BenNdlControl: this request can peoceed: inProgress = " + inprogress);
            if (handler != null) {
                // set the inprogress flag
                setInprogress(true);
                // FIXME: get this from the client request
                String requestSpec = requested.getConfigurationProperties().getProperty(PropertyRequestNdl);
                ByteArrayInputStream is = new ByteArrayInputStream(requestSpec.getBytes());
try{
                handler.handleMapping(is, true);
}catch(Exception e){
	System.out.println(e.toString());
}
                NetworkConnection con = handler.getLastConnection();
System.out.println("BenNdlControl: returned connection:"+con);
                String uri = handler.getCurrentRequestURI();

                Properties handlerProperties = BenNdlPropertiesConverter.convert(con);

                ResourceData rd = new ResourceData();
                PropList.setProperty(rd.getResourceProperties(), UnitProperties.UnitVlanTag, handlerProperties.getProperty(UnitProperties.UnitVlanTag));
                PropList.setProperty(rd.getLocalProperties(), PropertyRequestID, uri);

                UnitSet gained = new UnitSet(authority.getShirakoPlugin());

                Unit u = new Unit();
                u.setResourceType(type);
                u.setProperty(UnitProperties.UnitVlanTag, handlerProperties.getProperty(UnitProperties.UnitVlanTag));
                // if the broker allocated specific bandwidth, set it as a property on the unit
                if (ticketProperties.containsKey(ResourceProperties.ResourceBandwidth)) {
                    long bw = Long.parseLong(ticketProperties.getProperty(ResourceProperties.ResourceBandwidth));
                    long burst = bw / 8;
                    u.setProperty(UnitProperties.UnitVlanQoSRate, Long.toString(bw));
                    u.setProperty(UnitProperties.UnitVlanQoSBurstSize, Long.toString(burst));
                }

                // attach the handler properties to the node properties list
                u.mergeProperties(handlerProperties);
                u.setProperty(PropertyRequestID, uri);

                gained.add(u);
                
                return new ResourceSet(gained, null, null, type, rd);
            } else {
                // no resource - delay the allocation
System.out.println("BenNdlControl: handler is null !!! ");
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
System.out.println("BenNdlControl: this request is finished: inProgress = " + inprogress);
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

        if (u != null) {
            String uri = u.getProperty(PropertyRequestID);
            // unset the setup properties
            NetworkConnection con = handler.getConnection(uri);
            Properties setupProperties = BenNdlPropertiesConverter.convert(con);
            u.unsetProperties(setupProperties);
            // set the teardown properties
            NetworkConnection teardown = handler.getConnectionTeardownActions(uri);
            Properties teardownProperties = BenNdlPropertiesConverter.convert(teardown);
            u.mergeProperties(teardownProperties);
        }
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
