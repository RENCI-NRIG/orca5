/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.nodeagent.tests.security.known;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import orca.nodeagent.tests.security.unknown.TestUnauthorizedClient;

public class SecurityTest extends TestCase {
    protected String serviceLocation = null;
    protected String repositoryPath = null;
    protected String configFileAuthorized = null;
    protected String configFileUnauthorized = null;
    protected String trudyKeyStoreLocation = null;
    protected String trudyKeyStorePass = null;
    TestAuthorizedClient tac;
    TestUnauthorizedClient tuc;
    TestRegisterKey trk;
    TestUnregisterKey tuk;

    public SecurityTest() {
        this(System.getenv("na.location"), System.getenv("na.repository"), System.getenv("na.config.authorized"),
                System.getenv("na.config.unauthorized"));
    }

    public SecurityTest(String location, String repository, String configFileAuthorized,
            String configFileUnauthorized) {
        if (location == null) {
            throw new RuntimeException("Location cannot be null");
        }

        if (repository == null) {
            throw new RuntimeException("Repository cannot be null");
        }

        if (configFileAuthorized == null) {
            throw new RuntimeException("ConfigFileAuthorized cannot be null");
        }

        if (configFileUnauthorized == null) {
            throw new RuntimeException("ConfigFileUnauthorized cannot be null");
        }

        this.serviceLocation = location;
        this.repositoryPath = repository;
        this.configFileAuthorized = configFileAuthorized;
        this.configFileUnauthorized = configFileUnauthorized;
    }

    public void setTrudyParameters() {
        trudyKeyStoreLocation = System.getenv("na.trudykeystorelocation");

        if (trudyKeyStoreLocation == null) {
            throw new RuntimeException("TrudyKeyStoreLocation cannot be null");
        }

        trudyKeyStorePass = System.getenv("na.trudykeystorepass");

        if (trudyKeyStorePass == null) {
            throw new RuntimeException("TrudyKeyStorePass cannot be null");
        }
    }

    public void test() throws Exception {
        int retVal;
        System.out.println("Running Authorized Client Security suite ...");

        tac = new TestAuthorizedClient(serviceLocation, repositoryPath, configFileAuthorized);
        System.out.println("Testing authorized client ...");

        try {
            retVal = tac.run();
            Assert.assertEquals(retVal, 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Authorized clien call failed ... OK");
        }

        System.out.println("Authorized client test ... OK");

        setTrudyParameters();
        trk = new TestRegisterKey(serviceLocation, repositoryPath, configFileAuthorized, trudyKeyStoreLocation,
                trudyKeyStorePass);
        System.out.println("Testing Trudy key registration ...");
        retVal = trk.run();
        Assert.assertTrue("key registration failed ... code = " + retVal, retVal == 0);
        System.out.println("Key registration ... OK");

        tuc = new TestUnauthorizedClient(serviceLocation, repositoryPath, configFileUnauthorized);
        System.out.println("Testing authorized Trudy ... ");
        retVal = tuc.run();
        Assert.assertTrue("Authorized Trudy call failed ... code = " + retVal, retVal == 0);
        System.out.println("Authorized Trudy test ... OK");

        tuk = new TestUnregisterKey(serviceLocation, repositoryPath, configFileAuthorized);
        System.out.println("Testing Trudy key removal ... ");
        retVal = tuk.run();
        Assert.assertTrue("Key Removal failed ... code = " + retVal, retVal == 0);
        System.out.println("Trudy key removal test ... OK");

        System.out.println("Testing unauthorized Trudy ...");

        try {
            retVal = tuc.run();
            Assert.fail("Trudy call successful code = " + retVal + " test failed");
        } catch (Exception ex) {
            System.out.println("Unauthorized Trudy test ... OK");
        }

        System.out.println("Authorized Client suite completed");
    }

    public static Test suite() {
        return new TestSuite(SecurityTest.class);
    }
}