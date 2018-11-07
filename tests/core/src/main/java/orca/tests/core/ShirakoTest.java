/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.tests.core;

import java.util.Properties;

import orca.manage.extensions.api.beans.ResultBooleanMng;
import orca.manage.extensions.standard.container.StandardContainerManagerObject;
import orca.shirako.container.Globals;
import orca.util.PropList;

import org.apache.log4j.Logger;

/**
 * The arguments passed to this test should be of the form: <br>
 * &lt;config file&gt; &lt;name=value&gt; &lt;name=value&gt; &lt;name=value&gt ... <br>
 * The first argument is the XML configuration file to use. The arguments that follow are name/value pairs with
 * properties to be used by the test.<br>
 * <br>
 * BaseTest supports two properties:<br>
 * <ol>
 * <li>tickLength: The length of a tick in milliseconds</li>
 * <li>testLength: The length of the test in milliseconds</li>
 * </ol>
 * Classes extending BaseTest should override readParameters() to extract whatever parameters they need. <br>
 * <br>
 * In case a test requires additional configuration actions, for example passing parameters to the load source of a
 * service manager, the test can override the loadConfiguration method.
 */
public class ShirakoTest extends TestBase {
    /**
     * Length of a tick (milliseconds)
     */
    public static final String PropertySleepTime = "tickLength";

    /**
     * Length of a test (ticks)
     */
    public static final String PropertyTestLength = "testLength";

    /**
     * Flag that directs recovery
     */
    public static final String PropertyRecover = "recover";

    /**
     * Flag that directs manual ticks on recovery
     */
    public static final String PropertyRecoveryManualTicks = "recover.manual";

    /**
     * Flags that directs manual ticks in general
     */
    public static final String PropertyManualTicks = "manual";

    /**
     * Flag that directs whether or not to clean the machines before starting
     */
    public static final String PropertyClean = "clean.machines";

    /**
     * Set minimum debug level
     */
    public static final String PropertyMinDebug = "debug.min";

    /**
     * Set maximum debug level
     */
    public static final String PropertyMaxDebug = "debug.max";

    /**
     * If true, the container will destroy all previous state and start fresh.
     */
    public static final String PropertyDoNotRecover = "do.not.recover";

    /**
     * The test name
     */
    public static final String DefaultTestName = "BaseTest";

    /**
     * The default test length
     */
    public static final long DefaultTestLength = 100;

    /**
     * The default tick length
     */
    public static final long DefaultTickLength = 0;

    /**
     * Run the test.
     * 
     * @param args
     *            the test parameters and properties
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            ShirakoTest test = new ShirakoTest(args);
            test.run();
        } else {
            System.out.println("Insufficient arguments");
        }
    }

    /**
     * the logger
     */
    protected Logger logger;

    /**
     * the name of the test
     */
    protected String testName;

    /**
     * indicates if we clean the machine
     */
    protected boolean clean = false;

    /**
     * the length of the test
     */
    protected long testLength;

    /**
     * the length of a tick
     */
    protected long tickLength;

    /**
     * the configuration file
     */
    protected String configFile;

    /**
     * the test properties
     */
    protected Properties properties;

    /**
     * indicates if the test will record how long the test took to run
     */
    protected boolean measureTime = false;

    public ShirakoTest() {
        this(DefaultTestName, DefaultTestLength, DefaultTickLength);
    }

    public ShirakoTest(String name, long cycles, long sleepTime) {
        fixClassPath();

        this.testName = name;
        this.testLength = cycles;
        this.tickLength = sleepTime;

        properties = new Properties();
        logger = Globals.getLogger(testName);
    }

    public ShirakoTest(String[] args) {
        this(DefaultTestName, DefaultTestLength, DefaultTickLength);
        setParameters(args);
    }

    protected long getCurrentCycle() {
        return Globals.getContainer().getCurrentCycle();
    }

    /**
     * Load the configuration file for this test.
     * 
     * @throws Exception
     */
    protected void loadConfiguration() throws Exception {
        Globals.getContainer().loadConfiguration(configFile);
    }

    /**
     * What happens on a single iteration of the test.
     * 
     * @throws Exception
     */
    protected void oneIteration() throws Exception {
        Globals.getContainer().tick();

        if (tickLength > 0) {
            Thread.sleep(tickLength);
        }
    }

    /**
     * Read in all of the test parameters that are relevant to this test.
     * 
     * @throws Exception
     */
    protected void readParameters() throws Exception {
        String temp = properties.getProperty(PropertySleepTime);

        if (temp != null) {
            tickLength = Integer.parseInt(temp);
        }

        temp = properties.getProperty(PropertyTestLength);

        if (temp != null) {
            testLength = Integer.parseInt(temp);
        }

        temp = properties.getProperty(PropertyClean);

        if (temp != null) {
            clean = Boolean.parseBoolean(temp);
        }
    }

    /**
     * Run the test. Read in the parameters and the configuration file for the test and then run. Record the length of
     * the test in seconds if indicated.
     * 
     * @throws Exception
     */
    public void run() throws Exception {
        if (configFile == null) {
            throw new Exception("Missing config file");
        }

        readParameters();

        boolean started = false;
        if (properties.getProperty(PropertyDoNotRecover) != null) {
            if (PropList.getBooleanProperty(properties, PropertyDoNotRecover)) {
                Globals.start(true);
                started = true;
            }
        }
        if (!started) {
            Globals.start();
        }

        loadConfiguration();

        if (clean) {
            System.out.println("Reseting of inventory is no longer supported");
        }

        boolean manualTicks = Globals.getContainer().isManualClock();

        if (manualTicks) {
            long start = System.currentTimeMillis();

            for (long i = 0; i < testLength; i++) {
                String str = "Starting cycle: " + getCurrentCycle();
                logger.info(str);
                System.out.println(str);
                oneIteration();
            }

            long end = System.currentTimeMillis();
            long elapsed = end - start;

            if (measureTime) {
                System.out.println("TIME:" + elapsed);
            }

            stopTest();
        } else {
            runTest();
        }
    }

    /**
     * Test function to execute if not using manual ticks.
     */
    protected void runTest() {
    }

    /**
     * Set the configuration file.
     * 
     * @param file
     *            the configuration file for the test
     */
    public void setConfigFile(String file) {
        this.configFile = file;
    }

    /**
     * Set the duration of the test.
     * 
     * @param cycles
     *            length of the test
     */
    public void setDuration(long cycles) {
        this.testLength = cycles;
    }

    /**
     * Set all of the parameters for the test.
     * 
     * @param args
     *            the parameters for the test
     */
    public void setParameters(String[] args) {
        configFile = args[0];

        for (int i = 1; i < args.length; i++) {
            String temp = args[i];
            String[] vals = temp.split("=");

            if (vals.length == 2) {
                properties.setProperty(vals[0], vals[1]);
            }
        }
    }

    /**
     * Set the amount of time to sleep between ticks.
     * 
     * @param sleepTime
     *            time between ticks
     */
    public void setSleepTime(long sleepTime) {
        this.tickLength = sleepTime;
    }

    protected void stopTest() throws Exception {
        Globals.getContainer().stop();
    }
}