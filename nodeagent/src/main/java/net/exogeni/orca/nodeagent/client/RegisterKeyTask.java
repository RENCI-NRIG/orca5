/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.nodeagent.client;

import net.exogeni.orca.nodeagent.documents.RegisterKeyElement;
import net.exogeni.orca.nodeagent.documents.RegisterKeyResultElement;
import net.exogeni.orca.nodeagent.drivers.DriverBaseTask;
import net.exogeni.orca.nodeagent.NodeAgentServiceStub;
import net.exogeni.orca.nodeagent.documents.RegisterKeyElement;
import net.exogeni.orca.nodeagent.documents.RegisterKeyResultElement;
import net.exogeni.orca.nodeagent.drivers.DriverBaseTask;

import org.apache.tools.ant.BuildException;

import java.io.FileInputStream;

import javax.security.cert.X509Certificate;

public class RegisterKeyTask extends DriverBaseTask {
    protected String keyAlias;
    protected String certificateFile;

    @Override
    public void execute() throws BuildException {
        super.execute();

        try {
            if (keyAlias == null) {
                throw new Exception("Missing key alias");
            }

            if (certificateFile == null) {
                throw new Exception("Missing certificate file");
            }

            FileInputStream is = null;
            X509Certificate certificate = null;

            try {
                is = new FileInputStream(certificateFile);
                certificate = X509Certificate.getInstance(is);
            } finally {
                if (is != null) {
                    is.close();
                }
            }

            byte[] certEncoding = certificate.getEncoded();
            RegisterKeyElement rke = new RegisterKeyElement();
            rke.setAlias(keyAlias);
            rke.setPublickey(certEncoding);

            NodeAgentServiceStub stub = getStub();
            RegisterKeyResultElement rkre = stub.registerKey(rke);
            int code = rkre.getRegisterKeyResultElement();
            setResult(code);
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public void setCertificateFile(String certificateFile) {
        this.certificateFile = certificateFile;
    }
}