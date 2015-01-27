/**
 * Copyright (c) 2009 Renaissance Computing Institute and Duke University
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and/or hardware specification (the “Work”) to deal in the
 * Work without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Work, and to permit persons to whom the Work is furnished to do so,
 * subject to the following conditions: The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of
 * the Work. THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS IN THE
 * WORK.
 */

package orca.handlers.oess.tasks;

import java.util.concurrent.TimeUnit;

import orca.handlers.oess.OESSAPI;
import orca.handlers.oess.OESSSession;
import orca.shirako.plugins.config.OrcaAntTask;
import orca.shirako.plugins.config.SliceProject;
import orca.shirako.util.SemaphoreMap;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * @author ibaldin@renci.org
 */
public class GenericOESSTask extends OrcaAntTask {
    // this will be used in descriptions of any reservations
    // to help identify them more easily. We allow it to be modified (e.g. for
    // testing)
    public static final String OESSDescKeyword = "ORCA_AUTOMATED";

    public static final String OESSLoginProperty = "OESS.login";
    public static final String OESSPasswordProperty = "OESS.password";
    public static final String OESSWorkgroupProperty = "OESS.wg";
    public static final String OESSNodeAProperty = "OESS.nodeA";
    public static final String OESSNodeZProperty = "OESS.nodeZ";
    public static final String OESSIntAProperty = "OESS.intA";
    public static final String OESSIntZProperty = "OESS.intZ";
    public static final String OESSBandwidthProperty = "OESS.bandwidth";
    public static final String OESSRequestIdProperty = "OESS.request_id";
    public static final String OESSVlanIdProperty = "OESS.vlan_id";
    
    protected static final int OESSRequestId = 1234;

    protected static final String OESSLockName="OESS.lock";
    
    protected OESSSession ss = null;
    protected OESSAPI sapi = null;

    protected String login, pass;
    protected int wg;

    /**
     * Create a login session using properties (login/password/wg)
     */
    public void initializeApi() throws BuildException {
        try {
            super.execute();
            ss = new OESSSession(login, pass, logger);
            sapi = new OESSAPI(ss, wg, logger);
        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void execute() throws BuildException {
    	initializeApi();
    }

    public void setLogin(String l) {
        this.login = l;
    }

    public void setPassword(String p) {
        this.pass = p;
    }

    public void setWg(String w) {
        this.wg = Integer.valueOf(w);
    }
    
    // use same locking as for sync handler tasks
    public void lockOESS() {
		Project opr = getProject();
		
		// when testing handlers, it really is just Project, cant get the map
		if (!(opr instanceof SliceProject))
			return;
		
		SliceProject pr = (SliceProject)getProject();
		
		SemaphoreMap sems = (SemaphoreMap)pr.getSemaphoreMap();
		
		try {
    		sems.tryAcquire(OESSLockName, 900, TimeUnit.SECONDS);
    	} catch (Exception e) {
    		throw new BuildException("Exception encountered waiting for sequence lock " + OESSLockName + ": " + e);
    	}
    }
    
    public void unlockOESS() {
		Project opr = getProject();
		
		// when testing handlers, it really is just Project, cant get the map
		if (!(opr instanceof SliceProject))
			return;
		
		SliceProject pr = (SliceProject)getProject();
		
		SemaphoreMap sems = (SemaphoreMap)pr.getSemaphoreMap();
		
    	try {
    		sems.release(OESSLockName);
    	} catch (Exception e) {
    		throw new BuildException("Exception encountered releasing sequence lock " + OESSLockName + ": " + e);
    	}
    }
}
