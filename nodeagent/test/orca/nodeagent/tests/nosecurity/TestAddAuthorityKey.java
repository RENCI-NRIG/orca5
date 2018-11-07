/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.nodeagent.tests.nosecurity;

import orca.nodeagent.NodeAgentServiceStub;
import orca.nodeagent.client.KeyMasterClient;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;

public class TestAddAuthorityKey {
    String keyAlias;
    String clientPassword;
    char[] clientPasswordChar;
    String clientStorePassword;
    char[] clientStorePasswordChar;
    NodeAgentServiceStub stub;
    String location;
    String keyStoreLocation;
    String authorityIP;

    public TestAddAuthorityKey() throws Exception {
        location = "http://localhost:9123/axis2/services/NodeAgentService";

        ConfigurationContext cc = ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                "../lib/external", "test/orca/nodeagent/tests/nosecurity/client.axis2.nosign.xml");
        stub = new NodeAgentServiceStub(cc, location);
        keyStoreLocation = "test/orca/nodeagent/tests/security/known/client.jks";
        authorityIP = "192.168.1.1";
        clientPassword = "clientkeypass";
        clientPasswordChar = clientPassword.toCharArray();
        clientStorePassword = "clientstorepass";
        clientStorePasswordChar = clientStorePassword.toCharArray();
        keyAlias = "clientkey";
    }

    public TestAddAuthorityKey(String location, NodeAgentServiceStub stub, String keyStoreLocation, String keyStorePass,
            String keyAlias, String keyPass, String authorityIP) {
        this.location = location;
        this.stub = stub;
        this.keyStoreLocation = keyStoreLocation;
        this.authorityIP = authorityIP;
        this.clientStorePassword = keyStorePass;
        this.clientPassword = keyPass;
        clientPasswordChar = this.clientPassword.toCharArray();
        clientStorePasswordChar = this.clientStorePassword.toCharArray();
        this.keyAlias = keyAlias;
    }

    public int run() throws Exception {
        System.out.println("Connecting to " + location);

        KeyMasterClient kmc = new KeyMasterClient(location, stub, keyStoreLocation, clientStorePassword, keyAlias,
                clientPassword, authorityIP, "serverKey");
        int code = kmc.callRegisterAuthorityKey();

        return code;
    }

    public static void main(String[] args) throws Exception {
        TestAddAuthorityKey test = null;
        test = new TestAddAuthorityKey();
        test.run();
    }
}