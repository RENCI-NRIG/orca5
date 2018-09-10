/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.nodeagent.tests.nosecurity;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.exogeni.orca.nodeagent.NodeAgentServiceStub;
import net.exogeni.orca.nodeagent.NodeAgentServiceStub;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;

public class NoSecurityTest extends TestCase {
    protected String serviceLocation = null;
    protected String repositoryPath = null;
    protected String configFile = null;
    protected String keyStoreLocation = null;
    protected String keyStorePass = null;
    protected String keyPass = null;
    protected String authorityIP = null;
    TestAddAuthorityKey taak;
    TestGetServiceKey tgsk;
    TestSecurityNotEnabled tsne;

    public NoSecurityTest() {
        this(System.getenv("na.location"), System.getenv("na.repository"), System.getenv("na.config"));
    }

    public NoSecurityTest(String location, String repository, String config) {
        if (location == null) {
            throw new RuntimeException("Location cannot be null");
        }

        if (repository == null) {
            throw new RuntimeException("Repository cannot be null");
        }

        if (config == null) {
            throw new RuntimeException("Config cannot be null");
        }

        this.serviceLocation = location;
        this.repositoryPath = repository;
        this.configFile = config;
    }

    public void setSecurityParameters() {
        keyStoreLocation = System.getenv("na.keystorelocation");

        if (keyStoreLocation == null) {
            throw new RuntimeException("KeyStoreLocation cannot be null");
        }

        keyStorePass = System.getenv("na.keystorepass");

        if (keyStorePass == null) {
            throw new RuntimeException("KeyStorePass cannot be null");
        }

        keyPass = System.getenv("na.keypass");

        if (keyPass == null) {
            throw new RuntimeException("KeyPass cannot be null");
        }

        authorityIP = System.getenv("na.authorityip");

        if (authorityIP == null) {
            throw new RuntimeException("authorityIP cannot be null");
        }
    }

    /**
     * Returns a stub to the node agent service
     * 
     * @return
     * @throws Exception
     */
    protected NodeAgentServiceStub getStub() throws Exception {
        ConfigurationContext cc = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repositoryPath,
                configFile);

        return new NodeAgentServiceStub(cc, serviceLocation);
    }

    public void test() throws Exception {
        System.out.println("Running nosecurity suite ...");

        tsne = new TestSecurityNotEnabled(serviceLocation, repositoryPath, configFile);
        System.out.println("Testing if service security is enabled ...");

        try {
            tsne.run();
            Assert.fail("Service security enabled ... Failed");
        } catch (Exception ex) {
            System.out.println("Service security is enabled ... OK");
        }

        NodeAgentServiceStub stub = getStub();
        setSecurityParameters();

        System.out.println("Testing getServiceKey ... assuming that registerAuthorityKey was NOT invoked.");
        tgsk = new TestGetServiceKey(serviceLocation, stub, keyStoreLocation, keyStorePass, keyPass, authorityIP);

        int retVal = tgsk.run();
        Assert.assertTrue(
                "getServiceKey succeeded instead of failing ... code = " + retVal
                        + "\n check if registerAuhorityKey was invoked/server.jks has been created on the service side",
                retVal != 0);
        System.out.println("Testing getServiceKey failed ... registerAuthorityKey was NOT invoked ... OK");

        System.out.println("Testing registerAuthorityKey ... ");

        taak = new TestAddAuthorityKey(serviceLocation, stub, keyStoreLocation, keyStorePass, "clientkey", keyPass,
                authorityIP);
        retVal = taak.run();
        Assert.assertEquals("registerAuthorityKey invocation failed ... code " + retVal, retVal, 0);
        System.out.println("Testing registerAuthorityKey ... OK");

        System.out.println("Testing getServiceKey ... assuming that registerAuthorityKey was invoked.");

        tgsk = new TestGetServiceKey(serviceLocation, stub, keyStoreLocation, keyStorePass, keyPass, authorityIP);
        retVal = tgsk.run();
        Assert.assertTrue("getServiceKey failed ... code = " + retVal, retVal == 0);
        System.out.println("Testing getServiceKey ... registerAuthorityKey was invoked ... OK");

        System.out.println("Nosecurity suite completed");
    }

    public static Test suite() {
        return new TestSuite(NoSecurityTest.class);
    }
}