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

import net.exogeni.orca.nodeagent.drivers.DriverBaseTask;
import net.exogeni.orca.nodeagent.NodeAgentServiceStub;
import net.exogeni.orca.nodeagent.drivers.DriverBaseTask;

import org.apache.tools.ant.BuildException;

public class RegisterAuthorityKeyTask extends DriverBaseTask {
    // install the certificate in the service authority and get the certificate
    // installed in local keystore
    String keyStoreLocation;
    String keyStorePassword;
    String authorityIP = "127.0.0.1";
    String keyPassword;
    String keyAlias;
    String serverKey;

    public void setKeystore(String keyStoreLocation) {
        this.keyStoreLocation = keyStoreLocation;
    }

    public void setKeystorePassword(String password) {
        keyStorePassword = password;
    }

    public void setAuthorityIP(String ip) {
        authorityIP = ip;
    }

    public void setKeyPassword(String pass) {
        keyPassword = pass;
    }

    public void setKey(String key) {
        this.keyAlias = key;
    }

    public void setServerKey(String key) {
        this.serverKey = key;
    }

    public void execute() throws BuildException {
        try {
            super.execute();

            if (keyStoreLocation == null) {
                throw new BuildException("keystore location parameter not provided");
            }

            if (keyStorePassword == null) {
                throw new BuildException("keystore password parameter not provided");
            }

            if (authorityIP == null) {
                throw new BuildException("Authority IP parameter not provided");
            }

            if (keyPassword == null) {
                throw new BuildException("key password parameter not provided");
            }

            if (keyAlias == null) {
                throw new BuildException("key password parameter not provided");
            }

            if (serverKey == null) {
                throw new BuildException("server key alias parameter not provided");
            }

            NodeAgentServiceStub stub = null;

            try {
                stub = getStub();
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new BuildException("stub could not be initialized");
            }

            KeyMasterClient kmc = new KeyMasterClient(location, stub, keyStoreLocation, keyStorePassword, keyAlias,
                    keyPassword, authorityIP, serverKey);

            try {
                int code = kmc.callRegisterAuthorityKey();
                setExitCode(code);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new BuildException("Exception while adding the authority key");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BuildException(e);
        }
    }
}