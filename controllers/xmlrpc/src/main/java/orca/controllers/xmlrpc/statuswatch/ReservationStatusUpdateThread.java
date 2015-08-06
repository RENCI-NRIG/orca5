package orca.controllers.xmlrpc.statuswatch;

import java.util.ArrayList;
import java.util.List;

import orca.controllers.OrcaController;
import orca.controllers.xmlrpc.XmlrpcOrcaState;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.beans.PropertiesMng;
import orca.manage.beans.PropertyMng;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.UnitMng;
import orca.shirako.common.ReservationID;

import org.apache.log4j.Logger;

/**
 * This thread allows expressing interest in completion
 * of certain reservations and can run callbacks on other
 * specified reservations when the status changes accordingly
 * 
 * Supports modify and more traditional ticketed-active
 * 
 * Principle of operation: allows to watch the following state transitions and
 * events:
 * - to Active transitions on reservations
 * - to Active transitions followed by OK unit status on modify
 * 
 * The periodic thread polls reservations of interest to determine whether the
 * they have reached the required state, upon which 
 * @author ibaldin
 *
 */
public class ReservationStatusUpdateThread implements Runnable {
	final List<WatchEntry<ReservationID>> activeWatch;
	final List<WatchEntry<ReservationIDWithModifyIndex>> modifyWatch;
	
	final static Logger logger = OrcaController.getLogger(ReservationStatusUpdateThread.class.getName());

	public ReservationStatusUpdateThread() {
		modifyWatch = new ArrayList<WatchEntry<ReservationIDWithModifyIndex>>();
		activeWatch = new ArrayList<WatchEntry<ReservationID>>();
	}
	
	/**
	 * Internal bookkeeping classes
	 * @author ibaldin
	 *
	 */
	private static class WatchEntry<T> {
		protected List<T> watch;
		protected List<ReservationID> act;
		protected IStatusUpdateCallback cb;
		
		/**
		 * Copies incoming lists
		 * @param w
		 * @param a
		 * @param c
		 */
		WatchEntry(final List<T> w, final List<ReservationID> a, final IStatusUpdateCallback c) {
			watch = new ArrayList<T>(w); 
			if (a != null)
				act = new ArrayList<ReservationID>(a);
			else
				act = null;
			cb = c;
		}
	}
	
	/**
	 * Helper interface to abstract checking for various state or status changes for a given
	 * list of reservations
	 * @author ibaldin
	 *
	 */
	private abstract static class StatusChecker {
		public enum Status {OK, NOTOK, NOTREADY};
		
		public Status check(Object o, List<ReservationID> ok, List<ReservationID> notok) {
			Status resSt = check_(o);
			
			switch(resSt) {
			case OK:
				addRid(o, ok);
				break;
			case NOTOK:
				addRid(o, notok);
				break;
			case NOTREADY:
			}
			return resSt;
		}
		
		/**
		 * Check status of a reservation and return OK, NOTOK or NOTREADY
		 * @param o
		 * @return
		 */
		protected abstract Status check_(Object o);
		
		protected abstract void addRid(Object o, List<ReservationID> to);
	}
	
	private static class ModifyStatusChecker extends StatusChecker {
		
		private static final String UNIT_MODIFY_PROP_MSG_SUFFIX = ".message";
		private static final String UNIT_MODIFY_PROP_CODE_SUFFIX = ".code";
		private static final String UNIT_MODIFY_PROP_PREFIX = "unit.modify.";

		protected void addRid(Object l, List<ReservationID> to) {
			if (!(l instanceof ReservationIDWithModifyIndex))
				return;
			ReservationIDWithModifyIndex rid = (ReservationIDWithModifyIndex)l;
			to.add(rid.getReservationID());
		}
		
		protected StatusChecker.Status check_(Object l) {
			if (!(l instanceof ReservationIDWithModifyIndex))
				return StatusChecker.Status.NOTREADY;
			
			ReservationIDWithModifyIndex rid = (ReservationIDWithModifyIndex)l;
			
			IOrcaServiceManager sm = null;

			try {
				sm = XmlrpcOrcaState.getInstance().getSM();

				// check state for active, closed or failed
				ReservationMng rm = sm.getReservation(rid.getReservationID());
				if (rm == null)
					throw new Exception("Unable to obtain reservation information for " + rid.getReservationID());

				if (rm.getState() != OrcaConstants.ReservationStateActive)
					return StatusChecker.Status.NOTREADY;
				
				List<UnitMng> units = sm.getUnits(rid.getReservationID());
				
				if (units.size() == 0) 
					throw new Exception("No units associated with reservation " + rid.getReservationID());
				
				// use only unit0
				UnitMng unit0 = units.get(0);
				// check unit properties for modify status
				PropertiesMng uProps = unit0.getProperties();
				String codePropname = UNIT_MODIFY_PROP_PREFIX + rid.getModifyIndex() + UNIT_MODIFY_PROP_CODE_SUFFIX;
				String msgPropname = UNIT_MODIFY_PROP_PREFIX + rid.getModifyIndex() + UNIT_MODIFY_PROP_MSG_SUFFIX;
				boolean modifyFailed = false;
				String modifyErrorMsg = null;
				Integer modifyErrorCode = 0;
				for (PropertyMng prop: uProps.getProperty()) {
					if ((!modifyFailed) && (codePropname.equals(prop.getName()))) {
						modifyErrorCode = Integer.parseInt(prop.getValue());
						if (modifyErrorCode == 0) {
							logger.info("Reservation " + rid.getReservationID() + " was modified successfully for modify index " + rid.getModifyIndex());
							return StatusChecker.Status.OK;
						} else {
							modifyFailed = true;
						}
					}
					if (msgPropname.equals(prop.getName())) {
						modifyErrorMsg = prop.getValue();
					}
				}
				if (modifyFailed) {
					logger.info("Reservation " + rid.getReservationID() + " failed modify for modify index " + rid.getModifyIndex() + " with code " + modifyErrorCode + " and message " + modifyErrorMsg);
					return StatusChecker.Status.NOTOK;
				}
			} catch (Exception e) {
				logger.error("ModifyStatusChecker: Unable to get SM or other exception " + e);
			} finally {
				if (sm != null)
					XmlrpcOrcaState.getInstance().returnSM(sm);
			}
			return StatusChecker.Status.NOTREADY;
		}
	}
	
	private static class ActiveStatusChecker extends StatusChecker {
		
		protected void addRid(Object l, List<ReservationID> to) {
			if (!(l instanceof ReservationID)) 
				return;
			ReservationID rid = (ReservationID)l;
			to.add(rid);
		}
		
		protected StatusChecker.Status check_(Object l) {
			if (!(l instanceof ReservationID)) 
				return StatusChecker.Status.NOTREADY;
			ReservationID rid = (ReservationID)l;
			
			IOrcaServiceManager sm = null;

			try {
				sm = XmlrpcOrcaState.getInstance().getSM();

				// check state for active, closed or failed
				ReservationMng rm = sm.getReservation(rid);
				if (rm == null)
					throw new Exception("Unable to obtain reservation information for " + rid);

				switch(rm.getState()) {
				case OrcaConstants.ReservationStateActive:
				case OrcaConstants.ReservationStateClosed:
					return StatusChecker.Status.OK;
				case OrcaConstants.ReservationStateFailed:
					return StatusChecker.Status.NOTOK;
				}
			} catch (Exception e) {
				logger.error("ActiveStatusChecker: Unable to get SM or other exception " + e);
			} finally {
				if (sm != null)
					XmlrpcOrcaState.getInstance().returnSM(sm);
			}
			return StatusChecker.Status.NOTREADY;
		}
	}

	
	/**
	 * Watch for OK unit modify status (or not OK). Callback is called when ALL
	 * reservations in the watch list have reached some sort of modify status (OK or not OK)
	 * If all reservations on the watch list went to OK, the success method of the callback is called
	 * If some or all reservations on the watch list went to not OK, the failure method of the callback
	 * is called.
	 * 
	 * @param watch - reservations to watch (with associated modify operation indices)
	 * @param act - reservations to act on (can be null)
	 * @param cb - callback
	 */
	public void addModifyStatusWatch(List<ReservationIDWithModifyIndex> watch, List<ReservationID> act, IStatusUpdateCallback cb) {
		if ((watch == null) || (watch.size() == 0) || (cb == null)) {
			logger.info("addModifyStatusWatch: watch list is size 0 or callback is null, ignoring");
			return;
		} 
		synchronized(modifyWatch) {
			modifyWatch.add(new WatchEntry<ReservationIDWithModifyIndex>(watch, act, cb));
		}
	}

	/**
	 * Watch for transition to Active or Failed. Callback is called when ALL reservations in the watch
	 * list have reached Active or Failed or Closed state. 
	 * If all reservations on the watch list went to Active or Closed, the success method of the callback is called
	 * If some or all reservations on the watch list went to Failed state, the failure method of the callback
	 * is called. 
	 * @param watch - reservations to watch 
	 * @param act - reservations to act on (can be null)
	 * @param cb - callback object
	 */
	public void addActiveStatusWatch(List<ReservationID> watch, List<ReservationID> act, IStatusUpdateCallback cb) {
		if ((watch == null) || (watch.size() == 0) || (cb == null)) {
			logger.info("addActiveStatusWatch: watch list is size 0 or callback is null, ignoring");
			return;
		} 
		synchronized(activeWatch) {
			activeWatch.add(new WatchEntry<ReservationID>(watch, act, cb));
		}
	}
	
	/**
	 * true means the watch entry has fired and no longer needed (ie all entries on watch list
	 * either OK or NOTOK). false otherwise (some are NOTREADY)
	 * @param we
	 * @param st
	 * @return
	 */
	private boolean checkWatchEntry(WatchEntry<?> we, StatusChecker sc) throws IStatusUpdateCallback.StatusCallbackException {
		// scan through the list 
		// if any reservations are in NOTREADY, skip
		// if all are OK, not OK - split into two lists if necessary
		// and call callbacks
		
		List<ReservationID> ok = new ArrayList<>();
		List<ReservationID> notok = new ArrayList<>();
		
		boolean ready = true;
		for(Object rid: we.watch) {
			StatusChecker.Status st = sc.check(rid, ok, notok);
			if (st == StatusChecker.Status.NOTREADY)
				ready = false;
		}
		
		if (!ready) {
			logger.debug("Reservation watch not ready for reservations " + we.watch);
			return false;
		}
		
		if (notok.size() == 0) {
			// call success callback method
			logger.debug("Invoking success callback for reservations " + we.watch);
			we.cb.success(ok, we.act);
			return true;
		} else {
			// call failure callback method
			logger.debug("Invoking failure callback for reservation " + we.watch);
			we.cb.failure(notok, ok, we.act);
			return true;
		}

	}
	
	@Override
	public void run() {
		// wake up periodically and check the status of reservations (state or unit modify properties)
		// and call appropriate callbacks if necessary. 
		
		// scan both lists and check if any of the 
		// reservation groups on them are ready for callbacks
		// remove those ready for callbacks off the lists and invoke callbacks
		// outside the critical section
		List<WatchEntry<ReservationID>> activeRemove = new ArrayList<WatchEntry<ReservationID>>();
		List<WatchEntry<ReservationIDWithModifyIndex>> modifyRemove = new ArrayList<WatchEntry<ReservationIDWithModifyIndex>>();

		logger.info("Scanning active watch list");
		synchronized(activeWatch) {
			for(WatchEntry<ReservationID> we: activeWatch) {
				try {
					if (checkWatchEntry(we, new ActiveStatusChecker()))
						activeRemove.add(we);
				} catch(IStatusUpdateCallback.StatusCallbackException e) {
					logger.error("Active watch entry for reservations " + we.watch + " returned with callback exception " + 
							e.getMessage() + ", removing off the watch list");
					activeRemove.add(we);
				}
			}
			logger.debug("Removing active entries from watch " + activeRemove);
			activeWatch.removeAll(activeRemove);
		}
		
		logger.info("Scanning modify watch list");
		synchronized(modifyWatch) {
			for (WatchEntry<ReservationIDWithModifyIndex> we: modifyWatch) {
				try {
					if (checkWatchEntry(we, new ModifyStatusChecker())) 
						modifyRemove.add(we);
				} catch (IStatusUpdateCallback.StatusCallbackException e) {
					logger.error("Modify watch entry for reservations " + we.watch + " returned with exception " + 
							e.getMessage() + ", removing off the watch list");
					modifyRemove.add(we);
				}
			}
			logger.debug("Removing modify entries from watch " + modifyRemove);
			modifyWatch.removeAll(modifyRemove);
		}
	}

}
