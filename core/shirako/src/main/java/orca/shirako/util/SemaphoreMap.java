package orca.shirako.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.tools.ant.BuildException;

/**
 * This class keeps a map of Strings to Semaphores and atomically
 * allocates new semaphores and lets users lock and unlock specific
 * semaphores. Used by handlers in start/stop atomic sequence code.
 * @author ibaldin
 *
 */
public class SemaphoreMap {
	Map<String, Semaphore> sems = new HashMap<String, Semaphore>();
	
	protected Semaphore addSem(String hostName) {
		assert(hostName != null);
		synchronized(sems) {
			if (sems.containsKey(hostName)) {
				return sems.get(hostName);
			}
			Semaphore s = new Semaphore(1);
			sems.put(hostName, s);
			return s;
		}
	}
	
	protected Semaphore getSem(String hostName) {
		assert(hostName != null);
		synchronized(sems) {
			return sems.get(hostName);
		}
	}
	
	/**
	 * safely acquire a semaphore with this name (create if necessary)
	 * @param semName
	 */
	public void acquire(String semName) {
		
	   	Semaphore sem = addSem(semName);
		
    	try {
    		sem.acquire();
    	} catch (Exception e) {
    		throw new BuildException("Exception encountered waiting for semaphore " + semName + ": " + e);
    	}
	}
	
	/**
	 * try acquire the semaphore for specified timeout
	 * @param semName
	 * @param timeout
	 * @param unit
	 */
	public void tryAcquire(String semName, long timeout, TimeUnit unit) {
		
	   	Semaphore sem = addSem(semName);
		
    	try {
    		sem.tryAcquire(timeout, unit);
    	} catch (Exception e) {
    		throw new BuildException("Exception encountered timed waiting for semaphore " + semName + ": " + e);
    	}
	}
	
	/**
	 * safely release this semaphore
	 * @param semName
	 */
	public void release(String semName) {
    	Semaphore sem = getSem(semName);
    	
    	try {
    		if (sem != null)
    			sem.release();
    		else
    			throw new Exception("Semaphore not found");
    	} catch (Exception e) {
    		throw new BuildException("Exception encountered releasing semaphore " + semName + ": " + e);
    	}
	}
	
	/**
	 * Delete the semaphore from map (garbage collection)
	 * @param semName
	 */
	public void delete(String semName) {
		Semaphore sem = null;

		synchronized(sems) {
			sem = sems.get(semName);
			if (sem == null)
				return;
			try {
				sem.acquire();
				sems.remove(semName);
				sem.release();
			} catch(Exception e) {
				throw new BuildException("Exception encountered clearing semaphore " + semName + ": " + e);
			}
		}
	}
}
