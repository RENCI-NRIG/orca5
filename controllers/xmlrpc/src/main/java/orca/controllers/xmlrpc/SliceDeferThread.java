package orca.controllers.xmlrpc;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import orca.controllers.OrcaController;
import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.beans.ReservationMng;
import orca.manage.beans.TicketReservationMng;

import org.apache.log4j.Logger;

/**
 * This runs as a standalone thread started by OrcaXmlrpcHandler and
 * deals with slices that have to wait for other slices to complete.
 * @author ibaldin
 *
 */
public class SliceDeferThread implements Runnable {
	final Queue<XmlrpcControllerSlice> deferredSlices = new LinkedList<XmlrpcControllerSlice>();
	final Logger logger = OrcaController.getLogger(this.getClass().getSimpleName());
	final Lock queueLock = new ReentrantLock();
	final Condition avail = queueLock.newCondition();
	final long THREAD_SLEEP_TIME = 10000L;
	
	// keep last active slice around
	protected static XmlrpcControllerSlice lastSlice = null;
	protected static long lastSliceTime = 0;
	protected long maxCreateWaitTime = 0;
	protected final long DEFAULT_MAX_CREATE_TIME = 600000L; 
	
	/**
	 * Resource types to wait for - due to possible race conditions because of long
	 * provisioning times.
	 */
	protected static final String DEFAULT_DELAY_RESOURCE_TYPES = "nlr.vlan ion.vlan ben.vlan";
	protected String[] delayResourceTypes = null;

	SliceDeferThread() {
		
		try {
			if (OrcaController.getProperty(XmlRpcController.PropertyControllerMaxCreateTimeMs) != null) 
				maxCreateWaitTime = Integer.parseInt(OrcaController.getProperty(XmlRpcController.PropertyControllerMaxCreateTimeMs));
			else
				maxCreateWaitTime = DEFAULT_MAX_CREATE_TIME;
		} catch (Exception e) {
			maxCreateWaitTime = DEFAULT_MAX_CREATE_TIME;
		}
		
		try {
			if (OrcaController.getProperty(XmlRpcController.PropertyDelayResourceTypes) != null) 
				delayResourceTypes = OrcaController.getProperty(XmlRpcController.PropertyDelayResourceTypes).split("[\\s]");
			else
				delayResourceTypes = DEFAULT_DELAY_RESOURCE_TYPES.split("[\\s]");
		} catch (Exception e) {
			delayResourceTypes = DEFAULT_DELAY_RESOURCE_TYPES.split("[\\s]");
		}
	}

	/**
	 * Put element on tail of queue and signal in case anyone's waiting
	 * @param s
	 */
	private void putTail(XmlrpcControllerSlice s) {
		queueLock.lock();
		if (s != null) {
			deferredSlices.add(s);
			avail.signal();
		}
		queueLock.unlock();
	}
	
	/**
	 * Wait in slice queue for up to THREAD_SLEEP_TIME and return
	 * either queue head or null;
	 * @param s
	 * @return
	 */
	private XmlrpcControllerSlice getHead() {
		XmlrpcControllerSlice ret = null;
		
		queueLock.lock();
		
		try {
			// wait for max time and return an element or null
			avail.await(THREAD_SLEEP_TIME, TimeUnit.MILLISECONDS);
			ret =  deferredSlices.peek();
		} catch (InterruptedException e) {
			return null;
		} finally {
			queueLock.unlock();
		}
		return ret;
	}
	
	private void removeHead() {
		queueLock.lock();
		
		deferredSlices.remove();
		
		queueLock.unlock();
	}
	
	/**
	 * Is this slice in defer queue or being processed now?
	 * @param s
	 * @return
	 */
	public boolean inDeferredQueue(XmlrpcControllerSlice s) {
		queueLock.lock();
		
		boolean ret = deferredSlices.contains(s);
		
		queueLock.unlock();
		
		return ret;
	}
	
	private synchronized void updateLast(XmlrpcControllerSlice s) {
		if (s == null)
			return;
		logger.info("updateLast(): updating last slice with " + s.getSliceUrn() + "/" + s.getSliceID());
		lastSlice = s;
		lastSliceTime = System.currentTimeMillis();
	}
	
	/**
	 * This is where the slice gets into the deferred class.
	 * Either it is not delayed because nothing else is going on
	 * or it doesn't touch on delay domains, so it is submitted
	 * immediately, or it is put on a queue for later FIFO processing.
	 * It is called with slice lock held.
	 *
	 * @param s - slice
	 */
	public void processSlice(XmlrpcControllerSlice s) {
		
		if (s == null)
			return;
		
		// with the introduction of sliceModify, a slice can end up waiting for itself if
		// we don't check that last slice isn't the same as new one
		// It remains to be seen how reliable this is - if a slice is redeeming a previous inter-domain modify
		// and a new one comes in and is allowed to proceed, will it screw up OSCARS/NLR? /ib
		if ((s != lastSlice) && checkComputedReservations(s) && delayNotDone(lastSlice)) {
			logger.info("processSlice(): Putting slice " + s.getSliceUrn() + "/" + s.getSliceID() + " on wait queue");
			// put on queue
			putTail(s);
		} else {
			logger.info("processSlice(): Processing slice " + s.getSliceUrn() + "/" + s.getSliceID() + " immediately");
			if (checkComputedReservations(s)) {
				updateLast(s);
			}
			// demand now
			demandSlice(s);
		}
	}
	
	public void run() {
		while(true) {
			XmlrpcControllerSlice slice = getHead();
			logger.debug("SliceDeferThread tick.");

			if (slice == null)
				continue;
			
			logger.info("run(): processing previously deferred slice " + lastSlice.getSliceUrn() + "/" + lastSlice.getSliceID());
			// we know it touches on delay domains,
			// just check if last one done
			try {
				lastSlice.lock();
				if (delayNotDone(lastSlice)) {
					if ((System.currentTimeMillis() - lastSliceTime) > maxCreateWaitTime) 
						logger.info("run(): maximum wait time exceeded for slice " + lastSlice.getSliceUrn() + "/" + lastSlice.getSliceID() + ", proceeding anyway.");
					else
						continue;
				}
			} catch (InterruptedException e) {
				continue;
			} catch (Exception e) {
				logger.error("run(): exception while checking slice " + lastSlice.getSliceUrn() + "/" + lastSlice.getSliceID() + ": " + e);
			} finally {
				lastSlice.unlock();
			}

			logger.info("run(): performing demand on deferred slice " + slice.getSliceUrn() + "/" + slice.getSliceID());
			updateLast(slice);
			try {
				slice.lock();
				demandSlice(slice);
			} catch (InterruptedException e) {
				continue;
			} catch (Exception ee) {
				logger.error("run(): exception while demanding slice " + lastSlice.getSliceUrn() + "/" + lastSlice.getSliceID() + ": " + ee);
			} finally {
				slice.unlock();
			}
			
			// remove from head 
			removeHead();
		}
	}

	private void demandSlice(XmlrpcControllerSlice s) {

		if (s == null) {
			logger.error("demandSlice(): demandSlice was given a null slice");
			return;
		}
		
		List<TicketReservationMng> compRes = s.getComputedReservations();
		if (compRes == null)
			return;
		
		Iterator<TicketReservationMng> it = compRes.iterator();
		IOrcaServiceManager sm = null;

		try {
			sm = XmlrpcOrcaState.getInstance().getSM();		

			while (it.hasNext()) {
				try {
					TicketReservationMng currRes = it.next();
					logger.debug("demandSlice(): Issuing demand for reservation: " + currRes.getReservationID().toString());
					if(currRes.getState() != OrcaConstants.ReservationStateUnknown) //existing reservations, bypass
						continue;
					if (!sm.demand(currRes)){
						throw new Exception("Could not demand resources: " + sm.getLastError());
					}
				} catch (ThreadDeath td) {
					throw td;
				} catch (Throwable t) {
					logger.error("Exception, failed to demand reservation" + t, t);
				}
			}
		} catch (Exception e) {
			logger.error("demandSlice(): Unable to get SM or demand reservation: " + e);
			return;
		} finally {
			if (sm != null)
				XmlrpcOrcaState.getInstance().returnSM(sm);
		}
	}
	
	/**
	 * Do the computed reservations touch one of delay domains?
	 * @param slice
	 * @return
	 */
	private boolean checkComputedReservations(XmlrpcControllerSlice slice) {
		
		if ((slice == null) || (slice.getComputedReservations() == null)) {
			logger.info("checkComputedReservations(): checkComputedReservaions empty slice or no computed reservations");
			return false;
		}
		
		for(TicketReservationMng cr: slice.getComputedReservations()) {
			for (String drt: delayResourceTypes) {
				if (drt.equals(cr.getResourceType())) {
					logger.info("checkComputedReservations(): checkComputedReservaions " + slice.getSliceUrn() + "/" + slice.getSliceID() + " has delayed domain");
					return true;
				}
			}
		}
		logger.info("checkComputedReservations(): checkComputedReservaions " + slice.getSliceUrn() + "/" + slice.getSliceID() + " has no delayed domains");
		return false;
	}
	
	/**
	 * See if a slice not yet done with delay domains
	 * @param slice
	 * @return
	 * @throws Exception
	 */
	private boolean delayNotDone(XmlrpcControllerSlice slice) {

		if (slice == null) {
			return false;
		}
		
		logger.info("delayNotDone(): checking slice " + slice.getSliceUrn() + "/" + slice.getSliceID());
		
		IOrcaServiceManager sm = null;

		List<ReservationMng> allRes;
		try {
			sm = XmlrpcOrcaState.getInstance().getSM();
			allRes = slice.getAllReservations(sm);

		} catch (Exception e) {
			logger.error("delayNotDone(): Exception in delayNotDone for slice " + slice.getSliceUrn() + "/" + slice.getSliceID() + ": " + e);
			return false;
		} finally {
			if (sm != null)
				XmlrpcOrcaState.getInstance().returnSM(sm);
		}

		if (allRes == null) {
			logger.info("delayNotDone(): Slice " + slice.getSliceUrn() + "/" + slice.getSliceID() + " has null reservations in delayNotDone");
			// slice has not been submitted
			return checkComputedReservations(slice);
		}
		else {
			if (allRes.size() <= 0){
				logger.info("delayNotDone(): Slice " + slice.getSliceUrn() + "/" + slice.getSliceID() + " has empty reservations in delayNotDone");
				// slice has not been submitted
				return checkComputedReservations(slice);
			}

			for (ReservationMng r: allRes) {
				String rType = r.getResourceType();
				// if a reservation matches a known delay type and that
				// reservation is not in a final state, we need to wait
				for (String drt: delayResourceTypes) {
					if (drt.equals(rType)) {
						// if it isn't ready or failed, we have to wait
						if ((r.getState() != OrcaConstants.ReservationStateActive) &&
								(r.getState() != OrcaConstants.ReservationStateClosed) &&
								(r.getState() != OrcaConstants.ReservationStateCloseWait) &&
								(r.getState() != OrcaConstants.ReservationStateFailed)) {
							logger.info("delayNotDone(): Slice " + slice.getSliceUrn() + "/" + slice.getSliceID() + " has domain " + drt + " with reservation " + r.getReservationID() + " that is not yet done");
							return true;
						}
					}
				}
			}
			logger.info("delayNotDone(): Slice " + slice.getSliceUrn() + "/" + slice.getSliceID() + " has no non-final reservations (" + allRes.size() + ")");
			return false;
		}
	}

}
