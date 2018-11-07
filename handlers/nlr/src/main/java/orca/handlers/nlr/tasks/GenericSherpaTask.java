/**
 * Copyright (c) 2009 Renaissance Computing Institute and Duke University
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and/or hardware specification (the �Work�) to deal in the
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

package orca.handlers.nlr.tasks;

import java.util.concurrent.TimeUnit;

import orca.handlers.nlr.SherpaAPI;
import orca.handlers.nlr.SherpaSession;
import orca.shirako.plugins.config.OrcaAntTask;
import orca.shirako.plugins.config.SliceProject;
import orca.shirako.util.SemaphoreMap;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * @author ibaldin@renci.org
 */
public class GenericSherpaTask extends OrcaAntTask {
    // this will be used in descriptions of any reservations
    // to help identify them more easily. We allow it to be modified (e.g. for
    // testing)
    public static final String SherpaDescKeyword = "ORCA_AUTOMATED";

    public static final String SherpaLoginProperty = "NLRSherpa.login";
    public static final String SherpaPasswordProperty = "NLRSherpa.password";
    public static final String SherpaWorkgroupProperty = "NLRSherpa.wg";
    public static final String SherpaNodeAProperty = "NLRSherpa.nodeA";
    public static final String SherpaNodeZProperty = "NLRSherpa.nodeZ";
    public static final String SherpaIntAProperty = "NLRSherpa.intA";
    public static final String SherpaIntZProperty = "NLRSherpa.intZ";
    public static final String SherpaBandwidthProperty = "NLRSherpa.bandwidth";
    public static final String SherpaRequestIdProperty = "NLRSherpa.request_id";
    public static final String SherpaVlanIdProperty = "NLRSherpa.vlan_id";

    protected static final int SherpaRequestId = 1234;

    protected static final String sherpaLockName = "sherpa.lock";

    protected SherpaSession ss = null;
    protected SherpaAPI sapi = null;

    protected String login, pass;
    protected int wg;

    /**
     * Create a login session using properties (login/password/wg)
     */
    public void initializeApi() throws BuildException {
        try {
            super.execute();
            ss = new SherpaSession(login, pass, logger);
            sapi = new SherpaAPI(ss, wg, logger);
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
    public void lockSherpa() {
        Project opr = getProject();

        // when testing handlers, it really is just Project, cant get the map
        if (!(opr instanceof SliceProject))
            return;

        SliceProject pr = (SliceProject) getProject();

        SemaphoreMap sems = (SemaphoreMap) pr.getSemaphoreMap();

        try {
            sems.tryAcquire(sherpaLockName, 900, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new BuildException("Exception encountered waiting for sequence lock " + sherpaLockName + ": " + e);
        }
    }

    public void unlockSherpa() {
        Project opr = getProject();

        // when testing handlers, it really is just Project, cant get the map
        if (!(opr instanceof SliceProject))
            return;

        SliceProject pr = (SliceProject) getProject();

        SemaphoreMap sems = (SemaphoreMap) pr.getSemaphoreMap();

        try {
            sems.release(sherpaLockName);
        } catch (Exception e) {
            throw new BuildException("Exception encountered releasing sequence lock " + sherpaLockName + ": " + e);
        }
    }
}
