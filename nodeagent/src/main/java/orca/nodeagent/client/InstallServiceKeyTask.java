/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.nodeagent.client;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import org.apache.axis2.util.Base64;
import org.apache.tools.ant.BuildException;

public class InstallServiceKeyTask extends AntBaseTask {
    protected String keystore;
    protected String password;
    protected String certificateString;
    protected String certificateFile;
    protected String keyAlias;
    protected KeyStore ks = null;
    protected Certificate certificate = null;

    protected void loadKeyStore() throws Exception {
        FileInputStream fis = null;

        try {
            ks = KeyStore.getInstance("JKS");
            fis = new FileInputStream(keystore);
            ks.load(fis, password.toCharArray());
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    protected void loadCertificate() throws Exception {
        InputStream is = null;
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        try {
            if (certificateString != null) {
                byte[] enc = Base64.decode(certificateString);
                is = new ByteArrayInputStream(enc);
            } else {
                is = new FileInputStream(certificateFile);
            }

            certificate = cf.generateCertificate(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    protected void addCertificate() throws Exception {
        Certificate cert = ks.getCertificate(keyAlias);

        if (cert == null) {
            logger.debug("adding certificate");
            ks.setCertificateEntry(keyAlias, certificate);

            FileOutputStream fos = null;

            try {
                fos = new FileOutputStream(keystore);
                ks.store(fos, password.toCharArray());
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        } else {
            logger.debug("certificate is already present");

            if (!cert.equals(certificate)) {
                throw new Exception("new certificate differs from existing certificate");
            }
        }
    }

    public void execute() throws BuildException {
        super.execute();

        try {
            if (keystore == null) {
                throw new Exception("must psecify a keystore");
            }

            if (password == null) {
                throw new Exception("must specify a password for the keystore");
            }

            if (keyAlias == null) {
                throw new Exception("must specify key alias");
            }

            if ((certificateString == null) && (certificateFile == null)) {
                throw new Exception("must specify a source");
            }

            logger.debug("keystore=" + keystore);
            logger.debug("alias=" + keyAlias);
            logger.debug("loading key store");
            loadKeyStore();
            logger.debug("loading certificate");
            loadCertificate();
            logger.debug("adding certificate");
            addCertificate();
            setResult(0);
        } catch (Exception e) {
            logger.error("", e);
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }

    public void setCertificate(String value) {
        this.certificateString = value;
    }

    public void setCertificateFile(String value) {
        this.certificateFile = value;
    }

    public void setKeystore(String value) {
        this.keystore = value;
    }

    public void setPassword(String value) {
        this.password = value;
    }

    public void setAlias(String value) {
        this.keyAlias = value;
    }
}