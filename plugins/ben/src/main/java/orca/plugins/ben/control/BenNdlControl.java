package orca.plugins.ben.control;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;

import orca.embed.cloudembed.NetworkHandler;
import orca.embed.policyhelpers.RequestReservation;
import orca.embed.policyhelpers.SystemNativeError;
import orca.ndl.elements.NetworkConnection;
import orca.policy.core.ResourceControl;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.common.meta.ResourceProperties;
import orca.shirako.common.meta.UnitProperties;
import orca.shirako.container.Globals;
import orca.shirako.core.Ticket;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.core.Units;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.plugins.config.Config;
import orca.shirako.plugins.config.ConfigToken;
import orca.shirako.util.ResourceData;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.PersistenceUtils;

public class BenNdlControl extends ResourceControl {
	public static final String PropertyRequestID = "request.id";
	public static final String PropertyRequestNdl = "request.ndl";
	public static final String EdgeIntfSuffix = ".edge.interface";
	public static final String EdgeIntfUrlSuffix = ".edge.interface.url";
	public static final String EdgeTagSuffix = "." + UnitProperties.UnitVlanTag;
	public static final String ConfigIntfSuffix = "config.interface.";
	public static final String ConfigTagSuffix = "config.vlan.tag.";
    public static final String PropertyConnectionRecoveryProperty = "connection.recovery.property";
    public static final String PropertyAssignedLabelRecovery = "assigned.label.recovery.property";

	protected static Class<? extends BenNdlPropertiesConverter> propertiesConverter;
	protected static Method propertiesConverterConvertMethod = null;
	{
		propertiesConverter = BenNdlPropertiesConverter.class;
		
		try {
			propertiesConverterConvertMethod = propertiesConverter.getDeclaredMethod("convert", NetworkConnection.class, NetworkHandler.class, Logger.class);
		} catch(NoSuchMethodException nme) {
			System.out.println("Unable to find appropriate convert method in " + propertiesConverter.getCanonicalName());
			Method[] methods = propertiesConverter.getMethods();
			for (Method m: methods) {
				System.out.println("Method " + m);
			}
		}
	}
    
	/**
	 * The resource type this control is responsible for.
	 */
	@NotPersistent
	protected ResourceType type;

	/**
	 * The NDL network handler. NOTE: Need to create revisit to call NetworkHandler.revisit()!
	 */
	@NotPersistent
	protected NetworkHandler handler;

	/**
	 * This control can have only one reservation waiting for the completion of
	 * a configuration action. This flag indicates if an action is in progress.
	 * If this flag is true, the control is going to delay all requests for new
	 * resources until the configuration action completes.
	 */
	@NotPersistent
	protected boolean inprogress = false;

	@NotPersistent
	protected int closeInProgress = 0;

	void setInprogress(boolean val) {
		inprogress = val;
	}

	@Override
	public void donate(IClientReservation r) throws Exception {
		if (handler != null) {
			throw new Exception("only a single source reservation is supported");
		}        
		type = r.getType();
		String substrateFile = getSubstrateFile(r);
		logger.info("BenNdlControl.donate(): substrate file: " + substrateFile);
		handler = new NetworkHandler(substrateFile, 
				Globals.TdbPersistentDirectory + Globals.PathSep + r.getActor().getGuid());
	}

	/**
	 * {@inheritDoc}
	 */
	public ResourceSet assign(IAuthorityReservation r) throws Exception {
		if (handler == null) {
			throw new Exception("no inventory");
		}

		if (inprogress || closeInProgress > 0) {
			// we can handle only one request at a time
			// delay this request until the current request completes
			return null;
		}

		// get the currently assigned resources (if any)
		ResourceSet current = r.getResources();
		// determine if this is a new lease or an extension request
		if (current == null) {
			ResourceSet r_set = formResourceSet(r);
			
			if(r_set==null) {
				return null;
			} else
				return r_set;
		} else {
			// extend automatically
			return new ResourceSet(null, null, null, type, null);
		}
	}
	
	/**
	 * Handle the request
	 * @param r
	 * @return
	 */
	protected RequestReservation handleRequest(IAuthorityReservation r) {
		ResourceSet requested = r.getRequestedResources();
		Ticket ticket = (Ticket) requested.getResources();
				
		// this is a new request
		// how much does the client want?
		long needed = ticket.getUnits();
		if (needed > 1) {
			r.fail("Cannot assign more than 1 VLAN per reservation");
		}
		
		String requestSpec = requested.getConfigurationProperties().getProperty(PropertyRequestNdl);
		
		// set the inprogress flag
		setInprogress(true);
		Properties configuration_properties = requested.getConfigurationProperties();
		Properties pdomain_properties = convertEdgeProperties(configuration_properties);    
		handler.setPdomain_properties(pdomain_properties);

		SystemNativeError error = null;
		RequestReservation rr = null;
		try{
			if(requestSpec==null)
				throw new Exception("No request property."); 
			rr = handler.getRequestReservation(requestSpec);
			error = handler.handleRequest(rr);
			
			if(error!=null)
				throw new Exception("Request embedding failed:"+error.toString()); 
		} catch(Exception e) {
			r.fail("Unable to satisfy request due to " + error);
			setInprogress(false);
			logger.error("Error in handling request embedding: " + error);
			e.printStackTrace();
		}
		return rr;
	}

	/**
	 * This function has a side-effect of setting type
	 * @param r
	 * @return
	 */
	protected Properties getTicketProperties(IAuthorityReservation r) {
		// get the requested resources
		ResourceSet requested = r.getRequestedResources();
		// get the requested resource type
		type = requested.getType();

		// get the currently assigned resources (if any)
		//ResourceSet current = r.getResources();

		// get the ticket
		Ticket ticket = (Ticket) requested.getResources();
		// get the resource ticket
		ResourceTicket rt = ticket.getTicket();
		// get the properties assigned by the broker
		Properties ticketProperties = rt.getProperties();		

		// COMMENTED OUT AS NOT USED /ib 04/09/14
		// get the requested lease term
		//Term term = r.getRequestedTerm();
		// convert to cycles
		//long start = authority.getActorClock().cycle(term.getNewStartTime());
		//long end = authority.getActorClock().cycle(term.getEndTime());

		return ticketProperties;
	}

	protected Properties convertEdgeProperties(Properties configuration_properties) {
		Properties config_properties = new Properties();
		Properties pdomain_properties = new Properties();
		HashSet <String> pdomainSet = new HashSet <String>();

		String key=null,value=null,pdomain=null;
		int index = 0;
		logger.debug("BenNdlControl: configuration_properties="+configuration_properties.size());
		for(Entry<?,?> entry:configuration_properties.entrySet()) {
			key = (String) entry.getKey();
			value = (String) entry.getValue();    		   		
			if(key.contains(EdgeTagSuffix) || key.contains(EdgeIntfSuffix) || key.contains(EdgeIntfUrlSuffix)){
				logger.debug("Match  properties: key =" + key +";value="+value); 
				config_properties.put(key, value);
				index = key.indexOf(EdgeIntfUrlSuffix);
				if(index>0){
					pdomain = key.substring(0, index);
					pdomainSet.add(pdomain);
					logger.debug("NdlVLANControl properties: pdomain =" + pdomain);
				}
			}
		}
		logger.debug("BenNdlControl:pdomainSet size="+pdomainSet.size());
		Iterator <String> pdomainSetIt = pdomainSet.iterator();
		while(pdomainSetIt.hasNext()) {
			pdomain =  pdomainSetIt.next();
			String intf_url=null,p_tag=null;
			for(Entry<?,?> entry: config_properties.entrySet()) {
				key = (String) entry.getKey();
				value = (String) entry.getValue();
				if(key.contains(pdomain)){
					if(key.endsWith(EdgeIntfUrlSuffix))
						intf_url = value;
					if(key.endsWith(EdgeTagSuffix))
						p_tag = value;
				}
			}
			if(intf_url!=null && p_tag!=null) {
				pdomain_properties.put(p_tag,intf_url);
				logger.debug("BenNdlControl edge properties:pdomain_intf="+intf_url+";p_tag="+p_tag);
			}
		}    
		return pdomain_properties;
	}

	protected ResourceSet formResourceSet(IAuthorityReservation r){
		
		Properties ticketProperties = getTicketProperties(r);
		RequestReservation rr = handleRequest(r);
		if (rr == null)
			return null;
		
		String uri = rr.getReservation();
		NetworkConnection con = handler.getConnection(uri);
		Properties handlerProperties = BenNdlPropertiesConverter.convert(con, handler, logger);
		if(handlerProperties==null)
			return null;
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
	}

	/**
	 * Cleanup connection state for a failed or closed unit
	 * @param u
	 */
	protected void closeConnectionCleanup(Unit u) {
		logger.debug("closeConnectionCleanup() called for " + u.getReservationID());
		
		String uri = u.getProperty(PropertyRequestID);
		
		if (uri == null) {
			logger.error("Unable to fund URI of the request to complete connection close");
			return;
		}
		
		// set the teardown properties
		NetworkConnection teardown = handler.getConnectionTeardownActions(uri);
		try {
			//Properties teardownProperties = BenNdlPropertiesConverter.convert(teardown, handler, logger);
			Properties teardownProperties = (Properties)propertiesConverterConvertMethod.invoke(null, teardown, handler, logger);
			u.mergeProperties(teardownProperties);
		} catch (InvocationTargetException ite) {
			logger.error("Unable to get teardown properties: " + ite + " for " + u.getReservationID());
		} catch (IllegalAccessException iae) {
			logger.error("Unable to get teardown properties: " + iae + " for " + u.getReservationID());
		}
		
		try {
			handler.releaseReservation(uri);
		} catch (Exception e) {
			logger.error("NdlMPControl.closeConnectionCleanup(): Exception: " + e + " for " + u.getReservationID());
			e.printStackTrace();
		}	
	}
	
	@Override
	public void configurationComplete(String action, ConfigToken token, Properties outProperties) {
		logger.debug("configurationComplete() called for " + token.getReservationID());
		
		super.configurationComplete(action, token, outProperties);

		// we handle only one action at a time, so we simply reset the
		// inprogress bit
		if (Config.TargetJoin.equals(action)) {
			setInprogress(false);
			logger.debug("BenNdlControl.configurationComplete(): this request " + token.getReservationID() + " is finished: inProgress = " + inprogress);
			
			Unit u = (Unit)token;
			
	        int result = getResultCode(outProperties);
	        
	        // if failure to provision, need to cleanup
	        if (result != 0) {
	        	logger.warn("BenNdlControl.configurationComplete(): reservation " + token.getReservationID() + " failed, cleaning up");
	        	setcloseInProgress();
	        	closeConnectionCleanup(u);
	        	unsetcloseInProgress();
	        }
		}
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
		
		logger.debug("Using properties converter " + propertiesConverter.getCanonicalName() + " in BenNdlControl.close()");
		
		setcloseInProgress();
		if ((u != null) && (propertiesConverterConvertMethod != null)){
			
			String uri = u.getProperty(PropertyRequestID);
			// unset the setup properties
			NetworkConnection con = handler.getConnection(uri);
			//Properties setupProperties = BenNdlPropertiesConverter.convert(con, handler, logger);
			try {
				Properties setupProperties = (Properties)propertiesConverterConvertMethod.invoke(null, con, handler, logger);
				u.unsetProperties(setupProperties);
			} catch (InvocationTargetException ite) {
				logger.error("Unable to get setup properties: " + ite + " for " + reservation.getReservationID());
			} catch (IllegalAccessException iae) {
				logger.error("Unable to get setup properties: " + iae + " for " + reservation.getReservationID());
			}

			closeConnectionCleanup(u);
		} else {
			unsetcloseInProgress();
			logger.debug("BenNdlControl.close(): Missing unit for reservation or unable to get convert method for " + reservation.getReservationID());
		}
	}

	void setcloseInProgress() {
		closeInProgress++;
	}

	void unsetcloseInProgress() {
		closeInProgress--;
	}

	protected void releaseResources(Unit u) throws Exception {
		String requestID = u.getProperty(PropertyRequestID);
		if (requestID == null) {
			throw new Exception("Request id is missing");
		}
		logger.debug("BenNdlControl.releaseResources(): Releasing resources requestID=" + requestID);
		try{
			/*	NetworkConnection releaseConnection=handler.getMapper().getReleaseNetworkConnection();
        	if(releaseConnection==null){
        		setcloseInProgress();
        		NetworkConnection teardown = handler.getConnectionTeardownActions(requestID);
                Properties teardownProperties = BenNdlPropertiesConverter.convert(teardown);
                u.mergeProperties(teardownProperties); 
        	}
            releaseConnection = handler.releaseReservation(requestID);
			 */ 
			unsetcloseInProgress();
		}catch(Exception e){
			unsetcloseInProgress();
			e.printStackTrace();
			throw new Exception("releaseRource in free() failed!");
		}
	}

	@Override
	protected void free(Units units) throws Exception {
		if (units != null) {
			for (Unit u : units) {
				try {
					logger.debug("BenNdlControl.close(): for unit: "+u.getReservationID());
					releaseResources(u);
				} catch (Exception e) {
					logger.error("BenNdlControl.close(): Failed to release vlan tag", e);
				}
			}
		}
	}

	@Override
	public void revisit(IReservation r) throws Exception {

		UnitSet uset = (UnitSet) (r.getResources().getResources());
		Units set = uset.getSet();

		for (Unit u : set) {
			try {
				switch (u.getState()) {
				case DEFAULT:
				case FAILED:
				case CLOSING:
				case PRIMING:
				case ACTIVE:
				case MODIFYING:
					// recover connection map
					String uri = u.getProperty(BenNdlControl.PropertyRequestID);
					logger.debug("BenNdlControl.revisit(): recovering connection " + uri);
					String connEncoding = u.getProperty(BenNdlControl.PropertyConnectionRecoveryProperty);
					if (connEncoding == null) 
						logger.info("No connection information available.");
					else {
						Properties connEnc = PropList.toProperties(connEncoding);
						NetworkConnection nc = PersistenceUtils.restore(connEnc);
						handler.recoverConnection(uri, nc);
					}

					// recover used labels
					logger.debug("BenNdlControl.revisit(): recovering used labels");
					String labelMap = u.getProperty(BenNdlControl.PropertyAssignedLabelRecovery);
					if (labelMap == null) 
						logger.info("No label information available.");
					else 
						handler.restoreAssignedLabels(PropList.toProperties(labelMap));

					break;
				case CLOSED:
					logger.debug("BenNdlControl.revisit(): node is closed. Nothing to recover");
					break;

				}
			} catch (Exception e) {
				logger.error("BenNdlControl.revisit(): exception recovering reservation: " + e);
				fail(u, "revisit with BenNdlControl", e);
			}
		}
	}
	
	@Override
	public void recoveryStarting() {
		logger.info("Beginning BenNdlControl recovery");
	}

	@Override
	public void recoveryEnded() {
		logger.info("Completing BenNdlControl recovery");
		logger.debug("Restored BenNdlControl resource type: " + type);
		logger.debug("with NetworkHandler: " + handler);
	}
	
    /**
     * Returns the status code contained in the properties list. To be use in
     * configuration handlers.
     * 
     * @param properties
     * @return
     */
    protected int getResultCode(Properties properties) {
        int result = 0;
        String temp = properties.getProperty(Config.PropertyTargetResultCode);

        if (temp != null) {
            result = Integer.parseInt(temp);
        }

        return result;
    }
	
}
