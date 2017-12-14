/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.nodeagent.client;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import orca.nodeagent.tests.drivers.DriverTest;
import orca.tools.axis2.Axis2ClientSecurityConfigurator;

public class SecurityConfiguratorTest extends TestCase {
    String actorID = "myactor";

    public SecurityConfiguratorTest() {
    }

    public void testCreate() {
        int code = Axis2ClientSecurityConfigurator.getInstance().createActorConfiguration(".", actorID);
        Assert.assertEquals(code, 0);
    }

    public static Test suite() {
        return new TestSuite(SecurityConfiguratorTest.class);
    }
}