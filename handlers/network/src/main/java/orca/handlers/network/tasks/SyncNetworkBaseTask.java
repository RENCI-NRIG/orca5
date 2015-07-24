package orca.handlers.network.tasks;

import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.tools.ant.BuildException;

/** 
 * If your network elements require exclusive access, this
 * guarantees it separated by network element name (parallel
 * operations are allowed on different elements)
 * @author ibaldin
 *
 */
public abstract class SyncNetworkBaseTask extends NetworkBaseTask {
	protected static HashMap<String, Semaphore> sems = new HashMap<String, Semaphore>();
	
	protected synchronized static void addSem(String hostName) {
		if (sems.containsKey(hostName)) {
			return;
		}
		sems.put(hostName, new Semaphore(1));
	}
	
	protected synchronized static Semaphore getSem(String hostName) {
		return sems.get(hostName);
	}
	
	public SyncNetworkBaseTask() {
		super();
	}
	
    @Override
    public void execute() throws BuildException {
    	super.execute();
    	
    	Semaphore sem = getSem(deviceAddress);
    	if (sem == null)
    		throw new BuildException("Semaphore for device " + deviceAddress + " is null");
    	try {
			//super.execute();
    		if (sem.tryAcquire(10, TimeUnit.MINUTES)) {
    			try {
    				synchronizedExecute();
    			} finally {
    				sem.release();
    			}
    		}
    	} catch (InterruptedException e) {
    		throw new BuildException("Timing out after 10 minutes on aquiring semaphore for " + deviceAddress);
    	} 
    }
    
    /**
     * This needs to be overwritten. Entry to this is guaranteed to be
     * synchronized by deviceAddress
     * @throws BuildException
     */
    public abstract void synchronizedExecute() throws BuildException;
    
    @Override
    protected void makeDevice() {
    	addSem(deviceAddress);
    }
}
