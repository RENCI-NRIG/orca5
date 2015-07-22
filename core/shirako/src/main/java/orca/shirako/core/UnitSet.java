package orca.shirako.core;

import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import orca.shirako.api.IActor;
import orca.shirako.api.IAuthorityProxy;
import orca.shirako.api.IConcreteSet;
import orca.shirako.api.IReservation;
import orca.shirako.api.IShirakoPlugin;
import orca.shirako.common.UnitID;
import orca.shirako.plugins.substrate.ISubstrate;
import orca.shirako.time.Term;
import orca.shirako.util.Notice;
import orca.util.OrcaException;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.CustomRecoverable;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.PersistenceUtils;
import orca.util.persistence.Persistent;
import orca.util.persistence.RecoverParent;

import org.apache.log4j.Logger;

// FIXME: check synchronization!!!

public class UnitSet implements IConcreteSet, CustomRecoverable {
	public static final String PropertyUnitPrefix = "unit.";

	/**
	 * The collection of units wrapped by this set.
	 */
	@NotPersistent
	protected Units units;
	/**
	 * The containing reservation object.
	 */
	@Persistent(reference = true)
	protected IReservation reservation;
	/**
	 * The underlying substrate.
	 */
	@Persistent(reference = true)
	protected ISubstrate substrate;
	/**
	 * Logger to use for this set;
	 */
	@Persistent(reference = true)
	protected Logger logger;
	/**
	 * Set to true when close is called.
	 */
	@Persistent(key = "closed")
	protected boolean isClosed;
	/**
	 * Freshness bit. Set in {@link #cloneEmpty()} and cleared in add. Used to
	 * indicate that the concrete set has not had any resources added to it.
	 */
	@Persistent(key = "fresh")
	protected boolean isFresh;
	/**
	 * Units released from this set: either failed or closed.
	 */
	@NotPersistent
	protected Units released;

	/**
	 * Default constructor: used with reflection.
	 */
	public UnitSet() {
		initCommon();
	}

	public UnitSet(IShirakoPlugin plugin) {
		if (!(plugin instanceof ISubstrate)) {
			throw new IllegalArgumentException(
					"plugin must implement ISubstrate");
		}
		this.substrate = (ISubstrate) plugin;
		initCommon();
	}

	/**
	 * Creates a unit set populated with the specified units.
	 * 
	 * @param substrate
	 * @param units
	 */
	public UnitSet(IShirakoPlugin plugin, Units units) {
		if (!(plugin instanceof ISubstrate)) {
			throw new IllegalArgumentException(
					"plugin must implement ISubstrate");
		}
		this.substrate = (ISubstrate) plugin;
		this.units = units;
		initCommon();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param uset
	 */
	public UnitSet(UnitSet uset) {
		this(uset, uset.substrate);
	}

	public UnitSet(UnitSet uset, IShirakoPlugin plugin) {
		this(plugin, (Units) uset.units.clone());
		this.isClosed = uset.isClosed;
		this.isFresh = uset.isFresh;
		this.reservation = uset.reservation;

	}

	public Properties encode(String protocol) throws Exception {
		Properties enc = new Properties();
		enc.setProperty(PersistenceUtils.PropertyClassName, this.getClass()
				.getCanonicalName());
		PropList.setProperty(enc, PropertyUnits, units.size());

		int i = 0;
		for (Unit u : units) {
			Properties up = PersistenceUtils.save(u);
			PropList.setProperty(enc, PropertyUnitPrefix + i, up);
			i++;
		}
		return enc;
	}

	public void decode(Properties enc, IShirakoPlugin plugin) throws Exception {
		if (!(plugin instanceof ISubstrate)) {
			throw new IllegalArgumentException(
					"plugin must implement ISubstrate");
		}
		this.substrate = (ISubstrate) plugin;
		this.logger = plugin.getLogger();

		int count = PropList.getIntegerProperty(enc, PropertyUnits);
		for (int i = 0; i < count; i++) {
			Properties up = PropList.getPropertiesProperty(enc,
					PropertyUnitPrefix + i);
			Unit u = new Unit();
			PersistenceUtils.restore(u, up);
			units.add(u);
		}
	}

	/**
	 * Performs common initialization functions.
	 */
	private void initCommon() {
		if (units == null) {
			units = new Units();
		}
		if (substrate != null) {
			logger = substrate.getLogger();
		}
	}

	/**
	 * Ensures that the passed set is an instance of <code>UnitSet</code>.
	 * 
	 * @param set
	 * @throws RuntimeException
	 */
	protected void ensureType(IConcreteSet set) throws RuntimeException {
		if (!(set instanceof UnitSet)) {
			throw new RuntimeException("set must be a UnitSet");
		}
	}

	/*
	 * IConcreteSet implementation.
	 */

	// FIXME: check the locking
	// FIXME: NodeGroup called checkAddRemoveModify to make sure the
	// group has not been closed
	public void add(IConcreteSet set, boolean configure)
			throws Exception {
		ensureType(set);
		add(((UnitSet) set).units, configure);
	}

	protected void add(Units toAdd, boolean configure) {
		// clear the fresh bit
		isFresh = false;
		this.units.addAll(toAdd);
		if (configure) {
			transferIn(toAdd);
		}
	}

	public void add(Unit u) {
		units.add(u);
	}

	public void change(IConcreteSet set, boolean configure)
			throws Exception {
		ensureType(set);
		
		UnitSet uset = (UnitSet) set;
		Units lost = units.missing(uset.units);
		Units gained = uset.units.missing(units);

		// the incoming units should be set to the DEFAULT state
		for (Unit u : gained) {
			u.setState(UnitState.DEFAULT);
		}
		
		// @anirban (07/17/15)
		// When nothing has been gained or lost, it means it is an update for modify or extend, i.e. updateLease
		// being serviced on the SM. In this case, just update the properties on the client (SM) side by 
		// pushing updated unit properties to the Substrate database
		// Remember that SM also keeps a substrate database; in case of join or leave, gained or lost
		// will be non null, and then on the SM side, add/remove will trigger SM side join/leave handler
		// During that process, for the join case, the SM side unit properties will be updated in the substrate database
		// through getSubstrateDataBase().updateUnits() calls in substrate.transferIn and substrate.processJoinComplete
		
		if(gained.isEmpty() && lost.isEmpty()){
			logger.debug("Updating properties on SM side for modify or extend");
			update(uset.units);
		}
		
		remove(lost, configure);
		add(gained, configure);
	}

	/*
	 * Updates unit properties in substrate database 
	 */
	protected void update(Units toUpdate){
		for(Unit u: toUpdate){
			u.setReservation(reservation);
			u.setSliceID(reservation.getSliceID());
			u.setActorID(substrate.getActor().getGuid());
			substrate.updateProps(reservation, u);
		}
	}
	
	public IConcreteSet cloneEmpty() {
		UnitSet result = new UnitSet(substrate);
		// this is a fresh set.
		result.isFresh = true;
		return result;
	}

	public void close() {
		Units lost = (Units) units.clone();
		transferOut(lost);
		isClosed = true;
	}

	public IConcreteSet collectReleased() throws Exception {
		UnitSet set = null;
		if (released != null && released.size() > 0) {
			set = new UnitSet(substrate, released);
			released = null;
		}
		return set;
	}

	public Units selectExtract(int count, String victims)
			throws Exception {
		int numTaken = 0;
		Units taken = new Units();

		/*
		 * Space-delimited list of preferred victim unit ids. The control is
		 * free to manipulate this list above since the selection of victims is
		 * rightly a policy choice.
		 */

		if (victims != null) {
			StringTokenizer st = new StringTokenizer(victims, " ");

			/* Iterate through list and add victims if they exist to taken set */
			while (st.hasMoreTokens() && (numTaken < count)) {
				String sid = st.nextToken();
				UnitID id = new UnitID(sid);
				Unit toTake = units.get(id);
				if (toTake != null) {
					taken.add(toTake);
					numTaken++;
				}
			}
		}

		/*
		 * If we need to select more units iterate through list and grab the
		 * first units not already in the list.
		 */

		for (Unit u : units) {
			if (numTaken == count) {
				break;
			}
			if (!taken.contains(u)) {
				taken.add(u);
				numTaken++;
			}
		}

		return taken;
	}

	public Notice getNotices() {
		Notice result = new Notice();
		for (Unit u : units) {
			Notice n = u.getNotices();
			if (!n.isEmpty()) {
				result.add(n.getNotice());
			}
		}
		return result;
	}

	public IAuthorityProxy getSiteProxy() throws Exception {
		// no site proxy for UnitSet.
		return null;
	}

	public int getUnits() {
		return units.size();
	}

	public int holding(Date date) {
		// date is irrelevant for UnitSet
		return getUnits();
	}

	protected int getPendingCount() { // Now only returns the count for number of units in PRIMING or CLOSING states
		int count = 0;
		for (Unit u : units) {
			if (u.hasPendingAction()) {
				// Check if pending action is a modify
				// If pending action is not modify, increment count
				//if(!u.isPendingModifying()){
				//	count++;
				//}
				count++;
			}
		}
		return count;
	}

	public boolean isActive() {
		return (!isFresh && reservation != null && getPendingCount() == 0);
	}

	public void modify(IConcreteSet set, boolean configure) throws Exception {
		ensureType(set);

		UnitSet uset = (UnitSet) set;
		for (Unit u : uset.units) {
			// get the current version of this unit
			Unit unow = units.get(u.getID());
			if (unow != null) {
				// attach the future version of this unit
				// to the current version
				unow.setModified(u);
				// change the current to the future
				if (configure) {
					modify(unow);
				}
			} else {
				logger.warn("Modify for unit not present in seet: " + u.getID());
			}
		}
	}

	public void probe() throws Exception {
		Units rel = null;

		// find all closed and failed units
		for (Unit u : units) {
			if (u.isClosed() || u.isFailed()) {
				if (rel == null) {
					rel = new Units();
				}
				rel.add(u);
			}
		}

		if (rel != null) {
			if (released == null) {
				released = rel;
			} else {
				released.addAll(rel);
			}
			// remove the units we found from this set
			for (Unit u : rel) {
				units.remove(u);
			}
		}
	}

	public void remove(IConcreteSet set, boolean configure) throws Exception {
		ensureType(set);

		remove(((UnitSet) set).units, configure);
	}

	protected void remove(Units toRemove, boolean configure) {
		// clear the fresh bit
		isFresh = false;
		units.removeAll(toRemove);
		if (configure) {
			transferOut(toRemove);
		}
	}

	public void setup(IReservation reservation) {
		this.reservation = reservation;
	}

	public void validateConcrete(ResourceType type, int units, Term term)
			throws Exception {
		if (getUnits() < units) {
			throw new RuntimeException("Insufficient units");
		}
	}

	public void validateIncoming() throws Exception {
	}

	public void validateOutgoing() throws Exception {
	}

	public void recover(RecoverParent parent, Properties properties)
			throws OrcaException {
	    IActor actor = parent.getObject(IActor.class);
		// obtain all units that belong to this reservation: they are part
		// of this set. Add the non-closed units to the set.

		Vector<Properties> v = substrate.getSubstrateDatabase().getUnits(
				reservation.getReservationID());
		for (Properties p : v) {
			Unit u = PersistenceUtils.restore(p);

			// FIXME: how about the failed units?
            // FIXME: ignoring closed units may cause a problem for code that has its own persistent
            // state, e.g., our NDL-based controls.
            
            //if (!u.isClosed()) {
				// add the unit to the set
				units.add(u);
				// NOTE: do not use the actor object from the reservation since that field may not have been recovered by now. 
				u.setActorID(actor.getGuid());
				u.setReservation(reservation);
				u.setSliceID(reservation.getSliceID());
			//}
		}
	}

	public void restartActions() throws Exception {
		for (Unit u : units) {
			restartActions(u);
		}
	}

	protected void restartActions(Unit u) throws Exception {
		switch (u.getState()) {
		case ACTIVE:
			break;
		case CLOSING:
			u.decrementSequence();
			transferOut(u);
			break;
		case PRIMING:
		case DEFAULT:
			u.decrementSequence();
			transferIn(u);
			break;
		case MODIFYING:
			u.decrementSequence();
			modify(u);
		case FAILED:
		case CLOSED:
			break;
		}
	}

	protected void transferIn(Unit u) {
		try {
			// can we transition?
			if (u.startPrime()) {
				u.setReservation(reservation);
				u.setSliceID(reservation.getSliceID());
				u.setActorID(substrate.getActor().getGuid());
				substrate.transferIn(reservation, u);
			} else {
				post(u, "Unit cannot be transfered., State=" + u.getState());
			}
		} catch (Exception e) {
			fail(u, "Transfer in for node failed", e);
		}
	}

	protected void post(Unit u, String message) {
		logger.error(message);
		u.addNotice(message);
	}

	protected void fail(Unit u, String message) {
		fail(u, message, null);
	}

	protected void fail(Unit u, String message, Exception e) {
		if (e != null) {
			logger.error(message, e);
		} else {
			logger.error(e);
		}
		u.fail(message, e);
	}

	protected void transferIn(Units units) {
		for (Unit u : units) {
			transferIn(u);
		}
	}

	protected void modify(Unit u) {
		try {
			u.startModify();
			substrate.modify(reservation, u);
		} catch (Exception e) {
			fail(u, "Modify for node failed", e);
		}
	}

	protected void transferOut(Unit u) {
		if (u.transferOutStarted) {
			return;
		}
		try {
			// transition the node to the closed state
			u.startClose();
			substrate.transferOut(reservation, u);
		} catch (Exception e) {
			fail(u, "tranferOut error", e);
		}
	}

	protected void transferOut(Units units) {
		for (Unit u : units) {
			transferOut(u);
		}
	}

	@Override
	public IConcreteSet clone() {
		return new UnitSet(this);
	}

	public Units getSet() {
		return (Units) units.clone();
	}
}
