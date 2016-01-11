package orca.plugins.ben.control;

import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.log4j.Logger;

import orca.embed.cloudembed.MultiPointNetworkHandler;
import orca.embed.cloudembed.NetworkHandler;
import orca.embed.policyhelpers.RequestReservation;
import orca.ndl.elements.NetworkConnection;
import orca.shirako.api.IAuthorityReservation;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.IReservation;
import orca.shirako.common.meta.ResourceProperties;
import orca.shirako.common.meta.UnitProperties;
import orca.shirako.container.Globals;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.plugins.config.Config;
import orca.shirako.plugins.config.ConfigToken;
import orca.shirako.util.ResourceData;
import orca.util.PropList;

public class NdlMPControl extends BenNdlControl {

	{
		propertiesConverter = NlrNdlPropertiesConverter.class;
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
	
	@Override
	public void donate(IClientReservation r) throws Exception {
		logger.debug("NdlMpControl.donate() for " + r + " is called");
		if (handler != null) {
			throw new Exception("only a single source reservation is supported");
		}        
		String substrateFile = getSubstrateFile(r);
		logger.info("NdlMPControl.donate(): substrate file: " + substrateFile);
		handler = new MultiPointNetworkHandler(substrateFile, 
				Globals.TdbPersistentDirectory + Globals.PathSep + r.getActor().getGuid());
	}
	
	/**
	 * {@overide}
	 */
	public ResourceSet assign(IAuthorityReservation r) throws Exception {
		logger.debug("NdlMpControl.assign() for " + r + " is called");
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
			if (r_set==null) {
				return null;
			} else
				return r_set;
		} else {
			// extend automatically
			return new ResourceSet(null, null, null, type, null);
		}
	}

	@Override
	protected ResourceSet formResourceSet(IAuthorityReservation r){
		Properties ticketProperties = getTicketProperties(r);
		RequestReservation rr = handleRequest(r);
		
		if (rr == null)
			return null;
		String uri = rr.getReservation();
		NetworkConnection con = handler.getConnection(uri);

		Properties handlerProperties = NlrNdlPropertiesConverter.convert(con, handler, logger);
		if(handlerProperties==null)
			return null;
		UnitSet gained = null;
		ResourceData rd = null;
		try{
			rd = new ResourceData();
			PropList.setProperty(rd.getResourceProperties(), UnitProperties.UnitVlanTag, handlerProperties.getProperty(UnitProperties.UnitVlanTag));
			PropList.setProperty(rd.getLocalProperties(), PropertyRequestID, uri);

			gained = new UnitSet(authority.getShirakoPlugin());

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
		}catch(Exception e){
			return null;
		}
		return new ResourceSet(gained, null, null, type, rd);
	}
	
//	@Override
//	public synchronized void close(IReservation reservation) {
//		logger.debug("NdlMPControl.close(): for reservation: " + reservation.getReservationID());
//
//		//super.close(reservation);
//
//		Unit u = null;
//
//		try {
//			u = ((UnitSet) reservation.getResources().getResources()).getSet().iterator().next();
//		} catch (Exception e) {
//			u = null;
//		}
//
//		setcloseInProgress();
//		if (u != null) {   
//			String uri = u.getProperty(PropertyRequestID);
//			logger.debug("NdlMPControl.close(): found unit for reservation: " + reservation.getReservationID() + " requestID=" + uri);
//			// unset the setup properties
//			NetworkConnection con = handler.getConnection(uri);
//			Properties setupProperties = NlrNdlPropertiesConverter.convert(con, handler, logger);
//			u.unsetProperties(setupProperties);
//			// set the teardown properties
//			NetworkConnection teardown = handler.getConnectionTeardownActions(uri);
//			Properties teardownProperties = NlrNdlPropertiesConverter.convert(con, (MultiPointNetworkHandler)handler, logger);
//			u.mergeProperties(teardownProperties);  
//
//			closeConnectionCleanup(u);
//			/*
//			String requestID = u.getProperty(PropertyRequestID);
//			try {
//				handler.releaseReservation(requestID);
//			} catch (Exception e) {
//				logger.error("NdlMPControl.close(): Exception: " + e);
//				e.printStackTrace();
//			}
//			*/	
//		} else {
//			unsetcloseInProgress();
//			logger.debug("NdlMPControl.close(): missing unit for reservation " + reservation.getReservationID());
//		}
//	}
	
	@Override
	public void recoveryStarting() {
		logger.info("Beginning NdlMPControl recovery");
	}

	@Override
	public void recoveryEnded() {
		logger.info("Completing NdlMPControl recovery");
		logger.debug("Restored NdlMPControl resource type: " + type);
		logger.debug("with NetworkHandler: " + handler);
	}
}
