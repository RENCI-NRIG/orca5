/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.nodeagent.tests.security.unknown;

import net.exogeni.orca.nodeagent.NodeAgentServiceStub;
import net.exogeni.orca.nodeagent.documents.TestFuncElement;
import net.exogeni.orca.nodeagent.documents.TestFuncResultElement;
import net.exogeni.orca.nodeagent.NodeAgentServiceStub;
import net.exogeni.orca.nodeagent.documents.TestFuncElement;
import net.exogeni.orca.nodeagent.documents.TestFuncResultElement;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;

public class TestUnauthorizedClient {
    String serviceLocation;
    String repositoryPath;
    String configFile;

    public TestUnauthorizedClient() {
        serviceLocation = "http://localhost:8080/axis2/services/NodeAgentService";
        repositoryPath = "../lib/external";
        configFile = "test/orca/nodeagent/tests/security/unknown/2client.axis2.xml";
    }

    public TestUnauthorizedClient(String serviceLocation, String repositoryPath, String configFile) {
        this.serviceLocation = serviceLocation;
        this.repositoryPath = repositoryPath;
        this.configFile = configFile;
    }

    public int run() throws Exception {
        ConfigurationContext cc = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repositoryPath,
                configFile);

        NodeAgentServiceStub stub = new NodeAgentServiceStub(cc, serviceLocation);

        TestFuncElement tf = new TestFuncElement();
        tf.setTestFuncElement(10);

        TestFuncResultElement trf = stub.testFunc(tf);
        System.out.println("TestFuncResult is: " + trf.getTestFuncResultElement());

        return 0;
    }

    public static void main(String[] args) throws Exception {
        TestUnauthorizedClient t = new TestUnauthorizedClient();
        t.run();
    }
}