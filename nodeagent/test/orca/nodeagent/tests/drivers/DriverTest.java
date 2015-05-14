/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.nodeagent.tests.drivers;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import orca.drivers.DriverFactory;

import orca.nodeagent.NodeAgentServiceStub;
import orca.nodeagent.documents.DriverRequestElement;
import orca.nodeagent.documents.ResultElement;
import orca.nodeagent.tools.DriverTool;
import orca.nodeagent.util.Serializer;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;

import java.util.Properties;


/**
 * A unit test for driver operations.
 */
public class DriverTest extends TestCase
{
    protected String location = null;
    protected String repository = null;
    protected String config = null;

    public DriverTest()
    {
        this(System.getenv("na.location"),
             System.getenv("na.repository"),
             System.getenv("na.config"));
    }

    public DriverTest(String location, String repository, String config)
    {
        if (location == null) {
            throw new RuntimeException("Location cannot be null");
        }

        this.location = location;
        this.repository = repository;
        this.config = config;
    }

    /**
     * Returns a stub to the node agent service
     * @return
     * @throws Exception
     */
    protected NodeAgentServiceStub getStub() throws Exception
    {
        if ((repository != null) || (config != null)) {
            ConfigurationContext cc = ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                repository,
                config);

            return new NodeAgentServiceStub(cc, location);
        } else {
            return new NodeAgentServiceStub(location);
        }
    }

    public void test() throws Exception
    {
        DriverTool tool = new DriverTool(location, repository, config);

        String id = TestDriver.MyDriverId.toString();
        String className = "orca.nodeagent.tests.drivers.TestDriver";
        String pkg = "dist/testdriver.tar.gz";

        tool.uninstallDriver(id);

        System.out.println("\nTesting install of a new driver...");

        int code = tool.installDriver(id, className, pkg);
        Assert.assertEquals(code, 0);
        System.out.println("Testing install of a new driver...OK");

        System.out.println("Testing install of an already installed driver...");
        code = tool.installDriver(id, className, pkg);
        Assert.assertEquals(code, DriverFactory.CodeDriverIsAlreadyInstalled);
        System.out.println("Testing install of an already installed driver...OK");

        System.out.println("Testing upgrade of an already installed driver...");
        code = tool.upgradeDriver(id, className, pkg);
        Assert.assertEquals(code, 0);
        System.out.println("Testing upgrade of an already installed driver...OK");

        System.out.println("Testing uninstall of an already installed driver...");
        code = tool.uninstallDriver(id);
        Assert.assertEquals(code, 0);
        System.out.println("Testing uninstall of an already installed driver...OK");

        System.out.println("Testing uninstall of an already uninstalled driver...");
        code = tool.uninstallDriver(id);
        Assert.assertEquals(code, DriverFactory.CodeDriverIsNotInstalled);
        System.out.println("Testing uninstall of an already uninstalled driver...OK");

        System.out.println("Testing driver invocation...");
        System.out.println("Installing driver...");
        code = tool.installDriver(id, className, pkg);
        Assert.assertEquals(0, code);
        System.out.println("Installing driver...OK");

        DriverRequestElement request = new DriverRequestElement();
        request.setDriverId(id.toString());
        request.setActionId(TestDriver.TestAction);

        Properties p = new Properties();
        p.setProperty("a1", "b1");
        p.setProperty("a2", "b2");
        p.setProperty("a3", "b3");

        request.setProperties(Serializer.serialize(p));

        NodeAgentServiceStub stub = getStub();
        ResultElement result = stub.executeDriver(request);
        Assert.assertEquals(result.getCode(), 0);
        System.out.println("Testing driver invocation...OK");

        System.out.println("Checking if properties are passed correctly...");

        Properties rp = Serializer.serialize(result.getProperties());
        Assert.assertNotNull(rp);
        Assert.assertEquals("b1", rp.getProperty("a1"));
        Assert.assertEquals("b2", rp.getProperty("a2"));
        Assert.assertEquals("b3", rp.getProperty("a3"));
        Assert.assertEquals("test2", rp.getProperty("test1"));
        Assert.assertEquals("test4", rp.getProperty("test3"));
        Assert.assertEquals("test6", rp.getProperty("test5"));
        Assert.assertEquals("test8", rp.getProperty("test7"));

        // System.out.println(rp.toString());
        System.out.println("Checking if properties are passed correctly...OK");
        System.out.println("Driver support unit test successful.");
    }

    public static Test suite()
    {
        return new TestSuite(DriverTest.class);
    }
}