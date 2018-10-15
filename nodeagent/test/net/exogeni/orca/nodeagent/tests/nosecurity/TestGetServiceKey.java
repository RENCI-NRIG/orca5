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
import net.exogeni.orca.nodeagent.client.KeyMasterClient;
import net.exogeni.orca.nodeagent.NodeAgentServiceStub;
import net.exogeni.orca.nodeagent.client.KeyMasterClient;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;

public class TestGetServiceKey {
    String location = "http://localhost:8080/axis2/services/NodeAgentService";
    String clientPassword = "clientkeypass";
    char[] clientPasswordChar = clientPassword.toCharArray();
    String clientStorePassword = "clientstorepass";
    char[] clientStorePasswordChar = clientStorePassword.toCharArray();
    String serverAlias = "serverkey";
    NodeAgentServiceStub stub;
    String keyStoreLocation;
    String keyStorePass;
    String keyPass;
    String authorityIP;

    public TestGetServiceKey() throws Exception {
        location = "http://localhost:8080/axis2/services/NodeAgentService";

        ConfigurationContext cc = ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                "../lib/external", "test/orca/nodeagent/tests/nosecurity/client.axis2.nosign.xml");
        stub = new NodeAgentServiceStub(cc, location);
        keyStoreLocation = "test/orca/nodeagent/tests/security/known/client.jks";
        authorityIP = "NA";
        clientPassword = "clientkeypass";
        clientPasswordChar = clientPassword.toCharArray();
        clientStorePassword = "clientstorepass";
        clientStorePasswordChar = clientStorePassword.toCharArray();
    }

    public TestGetServiceKey(String location, NodeAgentServiceStub stub, String keyStoreLocation, String keyStorePass,
            String keyPass, String authorityIP) {
        this.location = location;
        this.stub = stub;
        this.keyStoreLocation = keyStoreLocation;
        this.authorityIP = authorityIP;
        this.clientStorePassword = keyStorePass;
        this.clientPassword = keyPass;
        clientPasswordChar = this.clientPassword.toCharArray();
        clientStorePasswordChar = this.clientStorePassword.toCharArray();
    }

    public int run() throws Exception {
        KeyMasterClient kmc = new KeyMasterClient(location, stub, keyStoreLocation, clientStorePassword, "clientkey",
                clientPassword, authorityIP, "serverKey");
        int code = kmc.callGetServiceKey();

        return code;
    }

    public static void main(String[] args) throws Exception {
        TestGetServiceKey test = null;
        test = new TestGetServiceKey();
        test.run();
    }
}