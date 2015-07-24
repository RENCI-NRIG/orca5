package orca.handlers.network.tasks;

import orca.shirako.plugins.config.OrcaAntTask;
import orca.shirako.plugins.config.SliceProject;
import orca.shirako.util.SemaphoreMap;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Use file locking to guarantee sequence atomicity
 * on a particular device
 * @author ibaldin
 *
 */
public class StartAtomicSequenceTask extends OrcaAntTask {
	// get semaphore map from config
	protected String exclusiveDevice;
	protected int timeout;
	
	@Override
    public void execute() throws BuildException {
    	super.execute();
		
		if (exclusiveDevice == null) {
			throw new BuildException("StartAtomicSequenceTask: Missing exclusive device property");
		}
		
		Project opr = getProject();
		
		// when testing handlers, it really is just Project, cant get the map
		if (!(opr instanceof SliceProject))
			return;
		
		SliceProject pr = (SliceProject)getProject();
		
		SemaphoreMap sems = (SemaphoreMap)pr.getSemaphoreMap();
		
		try {
    		sems.acquire(exclusiveDevice);
    	} catch (Exception e) {
    		throw new BuildException("Exception encountered waiting for sequence lock " + exclusiveDevice + ": " + e);
    	}
	}
	
	public void setDevice(String s) {
		exclusiveDevice = s;
	}
}
