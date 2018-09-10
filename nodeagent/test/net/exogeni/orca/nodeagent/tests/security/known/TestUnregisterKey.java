/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.nodeagent.tests.security.known;

import net.exogeni.orca.nodeagent.NodeAgentServiceStub;
import net.exogeni.orca.nodeagent.documents.UnregisterKeyElement;
import net.exogeni.orca.nodeagent.documents.UnregisterKeyResultElement;
import net.exogeni.orca.nodeagent.NodeAgentServiceStub;
import net.exogeni.orca.nodeagent.documents.UnregisterKeyElement;
import net.exogeni.orca.nodeagent.documents.UnregisterKeyResultElement;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;

public class TestUnregisterKey {
    String serviceLocation;
    String repositoryPath;
    String configFile;

    public TestUnregisterKey() {
        serviceLocation = "http://localhost:8080/axis2/services/NodeAgentService";
        repositoryPath = "../lib/external";
        configFile = "test/orca/nodeagent/tests/security/known/client.axis2.xml";
    }

    public TestUnregisterKey(String serviceLocation, String repositoryPath, String configFile) {
        this.serviceLocation = serviceLocation;
        this.repositoryPath = repositoryPath;
        this.configFile = configFile;
    }

    public int run() throws Exception {
        ConfigurationContext cc = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repositoryPath,
                configFile);

        NodeAgentServiceStub stub = new NodeAgentServiceStub(cc, serviceLocation);

        UnregisterKeyElement urke = new UnregisterKeyElement();
        urke.setAlias("trudykey");

        UnregisterKeyResultElement urkre = stub.unregisterKey(urke);
        int res = urkre.getUnregisterKeyResultElement();

        if (res == 0) {
            System.out.println("Key Unregistered succesfully");
        } else {
            System.out.println("Key unregistration failed");
        }

        return res;
    }

    public static void main(String[] args) throws Exception {
        TestUnregisterKey test2 = null;
        test2 = new TestUnregisterKey();
        test2.run();
    }
}