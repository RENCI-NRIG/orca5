package orca.controllers.xmlrpc;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import orca.controllers.OrcaController;
import orca.controllers.xmlrpc.statuswatch.ReservationStatusUpdateThread;
import orca.manage.IOrcaServiceManager;

/**
 * Thread to sync path labels periodically, started in XmlrpcOrcaState on a schedule
 * @author geni-orca
 *
 */
public class LabelSyncThread implements Runnable {
	public static final String PropertyMaxXmlrpcWaitTime = "xmlrpc.controller.label.sync.max.wait.time";
	public static final String PropertySyncThreadPeriod = "xmlrpc.controller.label.sync.period";
	public static final String PropertyRunSyncFile = "xmlrpc.controller.label.sync.enable.file";
	public static final String DEFAULT_SYNC_THREAD_ENABLE_FILE=OrcaController.ConfigDirectory + "enable-label-sync";
	public static final int MAX_DEFAULT_WAIT_TIME = 60; //seconds
	public static final int DEFAULT_SYNC_THREAD_PERIOD=60; // seconds
	final static Logger logger = OrcaController.getLogger(LabelSyncThread.class.getSimpleName());
	protected static Lock syncLock = new ReentrantLock();
	protected static int waitTimeInt = 0;
	protected static int periodTimeInt = 0;
	private boolean running;
	
	public LabelSyncThread() {
		;
	}
	
	@Override
	public void run() {
		synchronized(this) {
			if (running) {
				logger.error("LabelSyncThread ran into itself, leaving");
				return;
			}
			running = true;
		}
		logger.info("LabelSyncThread executing cycle");
		
		// this thread needs to be run with caution and in general a better solution is needed
		// if it comes in on the heels of a deleteSlice operation, some labels just taken
		// off the global controller list may end up being put back on because the reservations
		// haven't closed yet, so it may take another run of the thread to actually sync the state. 
		
		// therefore it is best not to run it in general, but enable only if problems arise /ib 11/15/15
		String patFileName = OrcaController.getProperty(PropertyRunSyncFile);
		if (patFileName == null){
			patFileName = DEFAULT_SYNC_THREAD_ENABLE_FILE;
		} 
		
		File rf = new File(patFileName);
		if (!rf.exists()) {
			logger.info("LabelSyncThread enable file " + patFileName + " doesn't exist, skipping");
			running = false;
			return;
		}
		
		IOrcaServiceManager sm = null;
		XmlrpcOrcaState instance = XmlrpcOrcaState.getInstance();

		try {
			getLock();
			logger.debug("LabelSyncThread executing cycle, lock aquired");
			sm = instance.getSM();
			instance.syncTags(sm);
			logger.info("LabelSyncThread completed sync");
		} catch (Exception e) {
			logger.warn("LabelSyncThread unable to get SM");
		} finally {
			if (sm != null)
				instance.returnSM(sm);
			releaseLock();
			running = false;
		}
	}
	
	/**
	 * wait to acquire lock
	 */
	public static void getLock() {
		syncLock.lock();
	}
	
	/**
	 * returns true if acquired
	 * @param sec
	 * @return
	 */
	public static boolean tryLock(int sec) {
		boolean ret = false;
		try {
			ret = syncLock.tryLock(sec, TimeUnit.SECONDS);
		} catch (InterruptedException ie) {
			logger.warn("LabelSyncThread.tryLock interrupted externally");
		} 
		return ret;
	}
	
	/**
	 * release previously acquired lock
	 */
	public static void releaseLock() {
		try {
			syncLock.unlock();
		} catch (Exception e) {
			;
		}
	}
	
	private static int getPropertyOrDefault(String pName, int defaultVal) {
		String pVal = OrcaController.getProperty(pName);
		if (pVal == null) {
			return defaultVal;
		} else {
			try {
				int parseVal = Integer.parseInt(pVal);
				if (parseVal <= 0) 
					return defaultVal;
				return parseVal;
			} catch (NumberFormatException nfe) {
				logger.error("getPropertyOrDefault unable to parse property " + pName + ": " + pVal + ", using default " + defaultVal);
				return defaultVal;
			}
		}
	}
	
	/**
	 * Return cached wait time from properties or default
	 * @return
	 */
	public static int getWaitTime() {
		if (waitTimeInt > 0)
			return waitTimeInt;
		
		waitTimeInt = getPropertyOrDefault(PropertyMaxXmlrpcWaitTime, MAX_DEFAULT_WAIT_TIME);
		return waitTimeInt;
	}
	
	public static int getPeriod() {
		if (periodTimeInt > 0)
			return periodTimeInt;
		
		periodTimeInt = getPropertyOrDefault(PropertySyncThreadPeriod, DEFAULT_SYNC_THREAD_PERIOD);
		return periodTimeInt;
	}
}
