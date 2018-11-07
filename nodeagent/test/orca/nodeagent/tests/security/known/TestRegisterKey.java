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
import orca.nodeagent.documents.RegisterKeyElement;
import orca.nodeagent.documents.RegisterKeyResultElement;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;

import java.io.FileInputStream;

import java.security.KeyStore;
import java.security.cert.Certificate;

public class TestRegisterKey {
    String serviceLocation;
    String repositoryPath;
    String configFile;
    String trudyKeyStoreLocation;
    char[] trudyStorePasswordChar;

    public TestRegisterKey() {
        serviceLocation = "http://localhost:8080/axis2/services/NodeAgentService";
        repositoryPath = "../lib/external";
        configFile = "test/orca/nodeagent/tests/security/known/client.axis2.xml";
        trudyKeyStoreLocation = "test/orca/nodeagent/tests/security/unknown/trudy.jks";
        trudyStorePasswordChar = new String("trudystorepass").toCharArray();
    }

    public TestRegisterKey(String serviceLocation, String repositoryPath, String configFile,
            String trudyKeyStoreLocation, String trudyStorePass) {
        this.serviceLocation = serviceLocation;
        this.repositoryPath = repositoryPath;
        this.configFile = configFile;
        this.trudyKeyStoreLocation = trudyKeyStoreLocation;
        this.trudyStorePasswordChar = trudyStorePass.toCharArray();
    }

    public int run() throws Exception {
        ConfigurationContext cc = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repositoryPath,
                configFile);

        NodeAgentServiceStub stub = new NodeAgentServiceStub(cc, serviceLocation);

        // register Trudy Key
        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream fis = new FileInputStream(trudyKeyStoreLocation);
        ks.load(fis, trudyStorePasswordChar);
        fis.close();

        Certificate cert = ks.getCertificate("trudykey");
        byte[] certEncoding = cert.getEncoded();
        RegisterKeyElement rke = new RegisterKeyElement();
        rke.setAlias("trudykey");
        rke.setPublickey(certEncoding);

        RegisterKeyResultElement rkre = stub.registerKey(rke);

        int res = rkre.getRegisterKeyResultElement();

        return res;
    }

    public static void main(String[] args) throws Exception {
        TestRegisterKey test1 = null;

        test1 = new TestRegisterKey();

        test1.run();
    }
}