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

import orca.shirako.container.Globals;

import org.apache.log4j.Logger;

/**
 * A very simple test to ensure the correctness of the container manager.
 */
public class LoadConfigurationTest extends TestBase {
    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                System.out.println("Usage: <configuration file>");
                System.exit(-1);
            }

            fixClassPath();
            Globals.start(true);
            LoadConfigurationTest test = new LoadConfigurationTest();
            test.runTest(args[0]);
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    protected Logger logger;

    public LoadConfigurationTest() {
        logger = Globals.getLogger(this.getClass().getCanonicalName());
    }

    public void runTest(String config) throws Exception {
        if (Globals.getContainer().isFresh()) {
            Globals.getContainer().loadConfiguration(config);
        }

        System.out.println("Test Complete");
        System.exit(0);
    }
}