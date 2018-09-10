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

import net.exogeni.orca.nodeagent.NodeAgentServiceStub;
import net.exogeni.orca.nodeagent.documents.TestFuncElement;
import net.exogeni.orca.nodeagent.documents.TestFuncResultElement;
import net.exogeni.orca.nodeagent.NodeAgentServiceStub;
import net.exogeni.orca.nodeagent.documents.TestFuncElement;
import net.exogeni.orca.nodeagent.documents.TestFuncResultElement;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;

public class TestSecurityNotEnabled {
    String serviceLocation;
    String repositoryPath;
    String configFile;

    public TestSecurityNotEnabled() {
        serviceLocation = "http://localhost:9123/axis2/services/NodeAgentService";
        // serviceLocation = "http://shirako068.cod.cs.duke.edu:6/axis2/services/NodeAgentService";
        repositoryPath = null;
        configFile = "test/orca/nodeagent/tests/nosecurity/client.axis2.nosign.xml";
    }

    public TestSecurityNotEnabled(String location, String repository, String config) {
        this.serviceLocation = location;
        this.repositoryPath = repository;
        this.configFile = config;
    }

    public void run() throws Exception {
        ConfigurationContext cc = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repositoryPath,
                configFile);
        NodeAgentServiceStub stub = new NodeAgentServiceStub(cc, serviceLocation);

        System.out.println("Connecting to service: " + serviceLocation);

        TestFuncElement tf = new TestFuncElement();
        tf.setTestFuncElement(10);

        TestFuncResultElement trf = stub.testFunc(tf);

        System.out.println("TestFuncResult is: " + trf.getTestFuncResultElement());
    }

    public static void main(String[] args) throws Exception {
        TestSecurityNotEnabled test = null;
        test = new TestSecurityNotEnabled();
        test.run();
    }
}