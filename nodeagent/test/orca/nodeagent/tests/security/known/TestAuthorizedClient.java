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

import orca.nodeagent.NodeAgentServiceStub;
import orca.nodeagent.documents.TestFuncElement;
import orca.nodeagent.documents.TestFuncResultElement;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;

public class TestAuthorizedClient {
    String serviceLocation;
    String repositoryPath;
    String configFile;

    public TestAuthorizedClient() {
        serviceLocation = "http://localhost:8080/axis2/services/NodeAgentService";
        repositoryPath = "../lib/external";
        configFile = "test/orca/nodeagent/tests/security/known/client.axis2.xml";
    }

    public TestAuthorizedClient(String serviceLocation, String repositoryPath, String configFile) {
        this.serviceLocation = serviceLocation;
        this.repositoryPath = repositoryPath;
        this.configFile = configFile;
    }

    public int run() throws Exception {
        System.out.println("here");
        System.out.print(repositoryPath);
        System.out.print(configFile);

        ConfigurationContext cc = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repositoryPath,
                configFile);

        NodeAgentServiceStub stub = new NodeAgentServiceStub(cc, serviceLocation);

        TestFuncElement tf = new TestFuncElement();
        tf.setTestFuncElement(10);

        TestFuncResultElement trf = stub.testFunc(tf);

        System.out.println("testFunc result is: " + trf.getTestFuncResultElement());

        return 0;
    }

    public static void main(String[] args) throws Exception {
        TestAuthorizedClient test = null;
        test = new TestAuthorizedClient();
        test.run();
    }
}