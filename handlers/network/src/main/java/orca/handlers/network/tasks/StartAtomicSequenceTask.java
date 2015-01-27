package orca.handlers.network.tasks;

import java.util.concurrent.TimeUnit;

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
@SuppressWarnings("unchecked")
public class StartAtomicSequenceTask extends OrcaAntTask {
	// get semaphore map from config
	protected String exclusiveDevice;
	
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
    		sems.tryAcquire(exclusiveDevice, 900, TimeUnit.SECONDS);
    	} catch (Exception e) {
    		throw new BuildException("Exception encountered waiting for sequence lock " + exclusiveDevice + ": " + e);
    	}
	}
	
	public void setDevice(String s) {
		exclusiveDevice = s;
	}
}
