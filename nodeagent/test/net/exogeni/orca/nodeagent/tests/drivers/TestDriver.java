/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.nodeagent.tests.drivers;

import net.exogeni.orca.drivers.DriverExitCodes;
import net.exogeni.orca.drivers.DriverFactory;
import net.exogeni.orca.drivers.DriverId;
import net.exogeni.orca.drivers.IDriver;

import java.util.Properties;

/**
 * A simple driver used for testing purposes.
 */
public class TestDriver implements IDriver {
    public static DriverId MyDriverId = new DriverId("bfc1aa30-451d-11db-b0de-0800200c9a66");
    public static String TestAction = "test";
    protected DriverFactory factory;

    public TestDriver() {
    }

    public int dispatch(String actionId, Properties in, Properties out) throws Exception {
        int code = 0;

        if (actionId.equals(TestAction)) {
            out.setProperty("a1", in.getProperty("a1"));
            out.setProperty("a2", in.getProperty("a2"));
            out.setProperty("a3", in.getProperty("a3"));
            out.setProperty("test1", "test2");
            out.setProperty("test3", "test4");
            out.setProperty("test5", "test6");
            out.setProperty("test7", "test8");
        } else {
            code = DriverExitCodes.UnknownAction;
        }

        return code;
    }

    public int dispatch2(String objectId, String actionId, Properties in, Properties out) throws Exception {
        return 0;
    }

    public DriverId getId() {
        return MyDriverId;
    }

    public int initialize() throws Exception {
        return 0;
    }

    public int cleanup() throws Exception {
        return 0;
    }

    public boolean isStateful() {
        return false;
    }

    public void setFactory(DriverFactory factory) {
        this.factory = factory;
    }
}