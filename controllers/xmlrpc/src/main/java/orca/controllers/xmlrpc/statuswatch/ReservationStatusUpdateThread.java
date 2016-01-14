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
	public static final int MODIFY_CHECK_PERIOD=5; //seconds
	final List<WatchEntry<ReservationID>> activeWatch;
	final List<WatchEntry<ReservationIDWithModifyIndex>> modifyWatch;
	
	final static Logger logger = OrcaController.getLogger(ReservationStatusUpdateThread.class.getSimpleName());

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
		protected IStatusUpdateCallback<T> cb;
		
		/**
		 * Copies incoming lists
		 * @param w
		 * @param a
		 * @param c
		 */
		WatchEntry(final List<T> w, final List<ReservationID> a, final IStatusUpdateCallback<T> c) {
			watch = new ArrayList<T>(w); 
			if (a != null)
				act = new ArrayList<ReservationID>(a);
			else
				act = null;
			cb = c;
		}
	}
	
	/**
	 * WatchEntry that has been triggered
	 * @author geni-orca
	 *
	 * @param <T>
	 */
	private static class TriggeredWatchEntry<T> extends WatchEntry<T> {
		protected List<T> ok, notok;
		
		TriggeredWatchEntry(final List<T> w, final List<ReservationID> a, final IStatusUpdateCallback<T> c) {
			super(w, a, c);
		}
		
		TriggeredWatchEntry(WatchEntry<T> we, final List<T> ok, final List<T> notok) {
			super(we.watch, we.act, we.cb);
			this.ok = ok;
			this.notok = notok;
		}

	}
	
	/**
	 * Helper interface to abstract checking for various state or status changes for a given
	 * list of reservations
	 * @author ibaldin
	 *
	 */
	private abstract static class StatusChecker<ID> {
		public enum Status {OK, NOTOK, NOTREADY};
		
		public Status check(IOrcaServiceManager sm, ID o, List<ID> ok, List<ID> notok) {
			Status resSt = check_(sm, o);
			
			switch(resSt) {
			case OK:
				ok.add(o);
				break;
			case NOTOK:
				notok.add(o);
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
		protected abstract Status check_(IOrcaServiceManager sm, ID o);
	}
	
	private static class ModifyStatusChecker extends StatusChecker<ReservationIDWithModifyIndex> {
		
		private static final String UNIT_MODIFY_PROP_MSG_SUFFIX = ".message";
		private static final String UNIT_MODIFY_PROP_CODE_SUFFIX = ".code";
		private static final String UNIT_MODIFY_PROP_PREFIX = "unit.modify.";
		
		protected StatusChecker.Status check_(IOrcaServiceManager sm, ReservationIDWithModifyIndex l) {
			if (!(l instanceof ReservationIDWithModifyIndex))
				return StatusChecker.Status.NOTREADY;
			
			ReservationIDWithModifyIndex rid = (ReservationIDWithModifyIndex)l;
			
			try {
				// check state for active, closed or failed
				ReservationMng rm = sm.getReservation(rid.getReservationID());
				if (rm == null)
					throw new Exception("Unable to obtain reservation information for " + rid.getReservationID());

				// has to be Active, None to be worth checking
				if ((rm.getState() != OrcaConstants.ReservationStateActive) ||
						(rm.getPendingState() != OrcaConstants.ReservationPendingStateNone))
					return StatusChecker.Status.NOTREADY;
				
				// failed or closed abruptly
				if ((rm.getState() == OrcaConstants.ReservationStateFailed) ||
						(rm.getState() == OrcaConstants.ReservationStateClosed))
					return StatusChecker.Status.NOTOK;
				
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
				logger.error("ModifyStatusChecker: " + e);
			}
			return StatusChecker.Status.NOTREADY;
		}
	}
	
	private static class ActiveStatusChecker extends StatusChecker<ReservationID> {
		
		protected StatusChecker.Status check_(IOrcaServiceManager sm, ReservationID l) {
			if (!(l instanceof ReservationID)) 
				return StatusChecker.Status.NOTREADY;
			ReservationID rid = (ReservationID)l;
			
			try {

				// check state for active, closed or failed
				ReservationMng rm = sm.getReservation(rid);
				List<UnitMng> un = sm.getUnits(rid);
				if (rm == null)
					throw new Exception("Unable to obtain reservation information for " + rid);

				switch(rm.getState()) {
				case OrcaConstants.ReservationStateActive:
					// active reservation should have units /ib 11/10/2015
					if ((un == null) || (un.size() == 0))
						return StatusChecker.Status.NOTREADY;
				case OrcaConstants.ReservationStateClosed:
					return StatusChecker.Status.OK;
				case OrcaConstants.ReservationStateFailed:
					return StatusChecker.Status.NOTOK;
				}
			} catch (Exception e) {
				logger.error("ActiveStatusChecker: " + e);
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
	public void addModifyStatusWatch(List<ReservationIDWithModifyIndex> watch, List<ReservationID> act, IStatusUpdateCallback<ReservationIDWithModifyIndex> cb) {
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
	public void addActiveStatusWatch(List<ReservationID> watch, List<ReservationID> act, IStatusUpdateCallback<ReservationID> cb) {
		if ((watch == null) || (watch.size() == 0) || (cb == null)) {
			logger.info("addActiveStatusWatch: watch list is size 0 or callback is null, ignoring");
			return;
		} 
		synchronized(activeWatch) {
			activeWatch.add(new WatchEntry<ReservationID>(watch, act, cb));
		}
	}
	
	/**
	 * Non-null means the watch entry has triggered and no longer needed (ie all entries on watch list
	 * either OK or NOTOK). null otherwise (some are NOTREADY)
	 * @param we
	 * @param st
	 * @return TriggeredWatchEntry or null if it didn't trigger
	 */
	private <ID> TriggeredWatchEntry<ID> checkWatchEntry(IOrcaServiceManager sm, WatchEntry<ID> we, StatusChecker<ID> sc) {
		// scan through the list 
		// if any reservations are in NOTREADY, skip
		// if all are OK, not OK - split into two lists if necessary
		// and call callbacks
		
		List<ID> ok = new ArrayList<>();
		List<ID> notok = new ArrayList<>();
		
		boolean ready = true;
		for(ID rid: we.watch) {
			StatusChecker.Status st = sc.check(sm, rid, ok, notok);
			if (st == StatusChecker.Status.NOTREADY)
				ready = false;
		}
		
		if (!ready) {
			logger.debug("Reservation watch not ready for reservations " + we.watch);
			return null;
		}
		
		return new TriggeredWatchEntry<ID>(we, ok, notok);
	}
	
	/**
	 * Processing the callback of a triggered watch entry
	 * @param we
	 * @throws IStatusUpdateCallback.StatusCallbackException
	 */
	private <ID> void processCallBack(TriggeredWatchEntry<ID> we) throws IStatusUpdateCallback.StatusCallbackException {
 		
		if (we.notok.size() == 0) {
			// call success callback method
			logger.debug("Invoking success callback for reservations " + we.watch);
			we.cb.success(we.ok, we.act);
			return;
		} else {
			// call failure callback method
			logger.debug("Invoking failure callback for reservation " + we.watch);
			we.cb.failure(we.notok, we.ok, we.act);
			return;
		}
	}
	
	private <ID> void processWatchList(IOrcaServiceManager sm, List<WatchEntry<ID>> watchList, String watchType, StatusChecker<ID> sc) {
		List<WatchEntry<ID>> toRemove = new ArrayList<>();
		
		logger.info("run(): Scanning " + watchType + " watch list");
		List<TriggeredWatchEntry<ID>> toProcess;
		synchronized(watchList) {
			toProcess = new ArrayList<>();
			for(WatchEntry<ID> we: watchList) {
				TriggeredWatchEntry<ID> twe = this.<ID>checkWatchEntry(sm, we, sc);
				if (twe != null) {
					// watch entry triggered, add to process list 
					toProcess.add(twe);
					toRemove.add(we);
				} 
			}
			logger.debug("run(): Removing " + watchType + " entries from watch " + toRemove.size());
			watchList.removeAll(toRemove);
		}
		
		// Process triggered active entries, which can create their own watches, so don't do it in the
		// above loop
		logger.info("run(): processing " + toProcess.size() + " triggered " + watchType + " callbacks");
		for(TriggeredWatchEntry<ID> twe: toProcess) {
			try {
				this.<ID>processCallBack(twe);
			} catch(IStatusUpdateCallback.StatusCallbackException e) {
				logger.error("run(): Triggered " + watchType + " watch entry for reservations " + 
						twe.watch + " returned with callback exception " + e);
			}
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
		
		// NOTE: because callback can create another callback, which can add watches, 
		// we must make this code re-entrant hence the need to change the watch list 
		// and only then call the callbacks
		try {
			IOrcaServiceManager sm = null;

			try {
				sm = XmlrpcOrcaState.getInstance().getSM();
				this.<ReservationID>processWatchList(sm, activeWatch, "active", new ActiveStatusChecker());
				this.<ReservationIDWithModifyIndex>processWatchList(sm, modifyWatch, "modify", new ModifyStatusChecker());
			} catch (Exception e) {
				throw new RuntimeException("Unable to acquire connection to SM: " + e);
			} finally {
				if (sm != null)
					XmlrpcOrcaState.getInstance().returnSM(sm);
			}
		} catch (RuntimeException re) {
			logger.error("run(): RuntimeException " + re + ", continuing");
		}
	}
	
	public static int getPeriod() {
		return MODIFY_CHECK_PERIOD;
	}

}
