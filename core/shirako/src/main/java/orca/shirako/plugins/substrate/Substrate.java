package orca.shirako.plugins.substrate;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import orca.manage.OrcaConstants;
import orca.shirako.api.IDatabase;
import orca.shirako.api.IReservation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.core.Actor;
import orca.shirako.core.Ticket;
import orca.shirako.core.Unit;
import orca.shirako.plugins.ShirakoPlugin;
import orca.shirako.plugins.config.AntConfig;
import orca.shirako.plugins.config.Config;
import orca.util.OrcaException;
import orca.util.PropList;
import orca.util.persistence.PersistenceUtils;

public class Substrate extends ShirakoPlugin implements ISubstrate {

	public Substrate() {
    }
    
    public Substrate(Actor actor, ISubstrateDatabase db, Config config) {
        super(actor, db, config);
    }
    
    
    @Override
    public void initialize() throws OrcaException {
        super.initialize();
        if (!(db instanceof ISubstrateDatabase)) {
            throw new OrcaException("Substrate database class must implement ISubstrateDatabase");
        }
    }

    public void transferIn(IReservation r, Unit unit) {
        try {
            // record the node in the database
            getSubstrateDatabase().addUnit(unit);
            // prepare the transfer
            prepareTransferIn(r, unit);
            // update the unit database record
            // since prepareTransferIn may have added new properties.
            ((ISubstrateDatabase) db).updateUnit(unit);
            // perform the node configuration
            doTransferIn(r, unit);
        } catch (Exception e) {
            failAndUpdate(unit, "transferIn error", e);
        }
    }

    public void transferOut(IReservation r, Unit unit) {
        try {
            // prepare the transfer out
            prepareTransferOut(r, unit);
            // update the unit database record
            getSubstrateDatabase().updateUnit(unit);
            // perform the node configuration
            doTransferOut(r, unit);
        } catch (Exception e) {
            failAndUpdate(unit, "transferOut error", e);
        }
    }

    public void modify(IReservation r, Unit unit) {
        try {
            // prepare the transfer out
            prepareModify(r, unit);
            // update the unit database record
            ((ISubstrateDatabase) db).updateUnit(unit);
            // perform the node configuration
            doModify(r, unit);
        } catch (Exception e) {
            failAndUpdate(unit, "modify error", e);
        }
    }

    /**
     * Performs additional setup operations before configuring the unit. This is
     * an optional step of the transfer in process and can be used to set custom
     * properties on the reservation/unit objects before invoking the
     * configuration subsystem. For example, if the policy did not allocate an
     * IP address for the unit, but an IP address is required to configure the
     * unit, the IP address can be allocated in this function.
     * @param r reservation containing the unit
     * @param unit unit to prepare
     * @throws Exception
     */
    protected void prepareTransferIn(IReservation r, Unit unit) throws Exception {
    }

    /**
     * Prepares the unit for transfer out. Note: resources assigned in
     * prepareTransferIn cannot be released yet, since they are still in use.
     * The resources can be released only when the transferOut operation
     * completes. See {@link #processLeaveComplete(Object, Properties)}.
     * @param r reservation containing the unit
     * @param unit unit to prepare
     * @throws Exception
     */
    protected void prepareTransferOut(IReservation r, Unit unit) throws Exception {
    }

    /**
     * Prepares the unit for modification.
     * @param r reservation containing the unit
     * @param unit unit to prepare
     * @throws Exception
     */
    protected void prepareModify(IReservation r, Unit unit) throws Exception {
    }

    protected void doTransferIn(IReservation r, Unit unit) throws Exception {
        Properties p = getConfigurationProperties(r, unit);
        Config.setActionSequenceNumber(p, unit.getSequenceIncrement());
        config.join(unit, p);
    }

    protected void doTransferOut(IReservation r, Unit unit) throws Exception {
        Properties p = getConfigurationProperties(r, unit);
        Config.setActionSequenceNumber(p, unit.getSequenceIncrement());
        config.leave(unit, p);
    }

    protected void doModify(IReservation r, Unit unit) throws Exception {
        Properties p = getConfigurationProperties(r, unit);
        Config.setActionSequenceNumber(p, unit.getSequenceIncrement());
        logger.debug("Properties in Substrate.doModify() = " + p);
        config.modify(unit, p);
    }

    protected Properties getConfigurationProperties(IReservation r, Unit u) throws Exception {
        Properties temp = new Properties();
        // all local reservation properties (actor)
        PropList.mergeProperties(r.getResources().getLocalProperties(), temp);
        // all local slice properties (actor)
        PropList.mergePropertiesPriority(r.getSlice().getLocalProperties(), temp);
        if (isSiteAuthority()) {
            // all configuration properties passed by the SM (reservation)
            PropList.mergePropertiesPriority(r.getResources().getConfigurationProperties(), temp);
            // all configuration properties passed by the SM (slice)
            PropList.mergePropertiesPriority(r.getSlice().getConfigurationProperties(), temp);
            // all ticket properties
            if (r.getRequestedResources() != null) {
                Ticket ticket = (Ticket)r.getRequestedResources().getResources();
                // FIXME: is this properties list used anymore? We used to use it poss IDs back
                // from broker to site, but this properties list is not signed and cannot be trusted
                // The authoritative, signed list is inside the resource delegation
                PropList.mergeProperties(ticket.getProperties(), temp);
                // extract the signed delegation properties
                ResourceTicket rticket = ticket.getTicket();
                PropList.mergeProperties(rticket.getProperties(), temp);
            }
        }
        
        // TODO: pass the ticket properties in the case of a service manager.

        // finally, take all unit properties
        // NOTE: unit properties take precedence over all other properties
        Properties p = PersistenceUtils.save(u);
        PropList.mergePropertiesPriority(temp, p);

        //System.out.println("Properties for config action: " + p);
        return p;
    }

    protected void failAndUpdate(Unit unit, String message, Exception e) {
        logger.error(message, e);
        try {
            // fail the unit
            unit.fail(message, e);
            // update the unit database record
            ((ISubstrateDatabase) db).updateUnit(unit);
        } catch (Exception ee) {
            logger.error("could not update unit in database", ee);
        }
    }

    protected void failNoUpdate(Unit unit, String message) {
        failNoUpdate(unit, message, null);
    }

    protected void failNoUpdate(Unit unit, String message, Exception e) {
        logger.error(message, e);
        unit.fail(message, e);
    }

    protected void failModifyNoUpdate(Unit unit, String message) {
        failModifyNoUpdate(unit, message, null);
    }

    protected void failModifyNoUpdate(Unit unit, String message, Exception e) {
        logger.error(message, e);
        unit.failOnModify(message, e);
    }    
    
    
    protected void processSavedProperties(Unit u, Properties p) {
        Properties p2 = new Properties();

        Enumeration<?> e = p.keys();

        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = p.getProperty(key);

            if (key.startsWith(Config.PropertySavePrefix)) {
                p2.setProperty(key, value);
            }
        }

        u.mergeProperties(p2);
    }

    /**
     * Absorbs properties passed up by a handler.
     * @param node node
     * @param properties properties
     */
    protected void mergeUnitProperties(Unit u, Properties properties) {
        Iterator<?> iter = properties.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();

            if (name.startsWith(Config.PropertySavePrefix)) {
                // Take each property starting with Config.PropertySavePrefix
                // and store it on the unit, stripping Config.PropertySavePrefix
                name = name.substring(Config.PropertySavePrefix.length());
                u.setProperty(name, value);
            } else {
                if (name.startsWith(Config.PropertyUpdatePrefix)) {
                    // the handler specified that the incoming value should
                    // update
                    // the value stored in the unit
                    String property = name.substring(Config.PropertyUpdatePrefix.length());
                    if (logger.isDebugEnabled()) {
                        logger.debug("Overwriting unit property " + property + " to " + value);
                    }
                    u.setProperty(property, value);
                }
            }
        }
    }

    @Override
    protected void processJoinComplete(Object token, Properties properties) {
        if (actor.isStopped()) {
            throw new RuntimeException("This actor cannot receive updates");
        }

        Unit u = (Unit) token;
        long sequence = Config.getActionSequenceNumber(properties);
        String notice = null;
        
        synchronized (u) {
            if (sequence != u.getSequence()) {
                logger.warn("(join complete) sequences mismatch: incoming (" + sequence + ") local: (" + u.getSequence() + "). Ignoring event.");

                return;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("(join complete) incoming (" + sequence + ") local: (" + u.getSequence() + ")");
                }
            }

            int result = getResultCode(properties);

            processSavedProperties(u, properties);
            
            String msg = properties.getProperty(Config.PropertyExceptionMessage);
            if (msg == null)
            	msg = properties.getProperty(Config.PropertyTargetResultCodeMessage);
            
            switch (result) {
                case 0:
                	logger.debug("join code 0 (success)");
                    // all went fine
                    // merge properties if needed
                    mergeUnitProperties(u, properties);
                    u.activate();
                    break;
                case -1:
                	logger.debug("join code -1 with message: " + msg);
                    notice = "Exception during join for unit: " + u.getID().toHashString() + " " + msg;
                    failNoUpdate(u, notice);
                    break;
                default:
                	logger.debug("join code " + result + " with message: " + msg);
                	notice = "Error code " + Integer.toString(result) + " during join for unit: " + u.getID().toHashString() + 
                	" with message: " + msg;
                    failNoUpdate(u, notice);
                    break;
            }
        }

        try {
            getSubstrateDatabase().updateUnit(u);
        } catch (Exception e) {
            logger.error("process join complete", e);
        }
    }

    @Override
    protected void processLeaveComplete(Object token, Properties properties) {
        if (actor.isStopped()) {
            throw new RuntimeException("This actor cannot receive updates");
        }

        Unit u = (Unit) token;
        long sequence = Config.getActionSequenceNumber(properties);

        synchronized (u) {
            if (sequence != u.getSequence()) {
                logger.warn("(leave complete) sequences mismatch: incoming (" + sequence + ") local: (" + u.getSequence() + "). Ignoring event.");
                return;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("(leave complete) incoming (" + sequence + ") local: (" + u.getSequence() + ")");
                }
            }

            int result = getResultCode(properties);

            switch (result) {
                case 0:
                    // all went fine
                    // FIXME: should we merge properties?
                    u.close();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Leave complete [0]: " + u.getID().toHashString());
                    }
                    break;
                case -1:
                    String msg = properties.getProperty(Config.PropertyExceptionMessage);
                    failNoUpdate(u, "Exception during leave for node [-1]: " + u.getID().toHashString() + " " + msg);
                    break;
                default:
                    failNoUpdate(u, "Error during leave for node [default]: " + u.getID().toHashString() + " " + Integer.toString(result));
                    break;
            }
        }

        try {
            getSubstrateDatabase().updateUnit(u);
        } catch (Exception e) {
            logger.error("process leave complete", e);
        }
    }

    protected void processModifyComplete(Object token, Properties properties) {
        if (actor.isStopped()) {
            throw new RuntimeException("This actor cannot receive updates");
        }

        Unit u = (Unit) token;
        long sequence = Config.getActionSequenceNumber(properties);
        String notice = null;

        synchronized (u) {
            if (sequence != u.getSequence()) {
                logger.warn("(modify complete) sequences mismatch: incoming (" + sequence + ") local: (" + u.getSequence() + "). Ignoring event.");

                return;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("(modify complete) incoming (" + sequence + ") local: (" + u.getSequence() + ")");
                }
            }

            int result = getResultCode(properties); // "shirako.target.code"
            
            processSavedProperties(u, properties);
            
            String msg = properties.getProperty(Config.PropertyExceptionMessage); // in case of ant exception
            if (msg == null)
            	msg = properties.getProperty(Config.PropertyTargetResultCodeMessage); // "shirako.target.code.message"
            
            // modify sequence number is passed from AntConfig in the "result" in AntConfig.Runconfig.execute()
            String modifySequenceNum = properties.getProperty(Config.PropertyModifySequenceNumber);
            if(modifySequenceNum == null){ // should never hit
            	modifySequenceNum = "-1"; 
            }
            
            logger.debug("processModifyComplete(): modifySequenceNum from AntConfig:" + modifySequenceNum);
            
            switch (result) {
                case 0:
                    // all went fine
                    // complete operation
                    notice = "modify action succeeded: message from handler = " + msg;
                    properties.setProperty(Config.PropertyModifyPropertySavePrefix + "." + modifySequenceNum +".message", notice);
                    properties.setProperty(Config.PropertyModifyPropertySavePrefix + "." + modifySequenceNum +".code", "0");
                    // merge properties if needed
                    //logger.debug("Properties in processModifyComplete(): " + properties);
                    mergeUnitProperties(u, properties);
                    u.completeModify();
                    break;

                case -1:
                    notice = "Exception during modify for unit: " + u.getID().toHashString() + " " + msg;  
                    properties.setProperty(Config.PropertyModifyPropertySavePrefix + "." + modifySequenceNum + ".message", notice);
                    properties.setProperty(Config.PropertyModifyPropertySavePrefix + "." + modifySequenceNum +".code", "-1");                    
                    // merge properties if needed
                    mergeUnitProperties(u, properties);
                    failModifyNoUpdate(u, notice);
                    break;
                    
                default:
                	notice = "Error during modify for node: " + u.getID().toHashString() + " " + Integer.toString(result);  
                    properties.setProperty(Config.PropertyModifyPropertySavePrefix + "." + modifySequenceNum + ".message", notice);
                    properties.setProperty(Config.PropertyModifyPropertySavePrefix + "." + modifySequenceNum +".code", Integer.toString(result));
                    // merge properties if needed
                    mergeUnitProperties(u, properties);
                    failModifyNoUpdate(u, notice);
                    break;
                    
            }
        }

        try {
            getSubstrateDatabase().updateUnit(u);
        } catch (Exception e) {
            logger.error("process modify complete", e);
        }
    }

    public ISubstrateDatabase getSubstrateDatabase() {
        return (ISubstrateDatabase) db;
    }
    
    @Override
    public void setDatabase(IDatabase db) {
        if (!(db instanceof ISubstrateDatabase)){
            throw new IllegalArgumentException("db must implement ISubstrateDatabase");
        }
        super.setDatabase(db);
    }

	@Override
	public void updateProps(IReservation r, Unit unit) {
		
		try {
            // update the unit database record
            ((ISubstrateDatabase) db).updateUnit(unit);
        } catch (Exception e) {
            failAndUpdate(unit, "update properties error", e);
        }
		
	}
}
