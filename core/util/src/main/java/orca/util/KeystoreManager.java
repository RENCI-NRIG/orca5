/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

import org.apache.log4j.Logger;

// FIXME: not thread-safe
public class KeystoreManager {
    static Logger logger = Logger.getLogger(KeystoreManager.class.getCanonicalName());

    /**
     * Alias under which the actor certificate and private key are stored.
     */
    public static final String ActorAlias = "actorKey";
    /**
     * Path to the keystore location.
     */
    protected String path;
    /**
     * Password to access the keystore.
     */
    protected String storePassword;
    /**
     * Password to access the private key.
     */
    protected String keyPassword;
    /**
     * The java key store object.
     */
    protected KeyStore store;

    public KeystoreManager(String path, String storePassword, String keyPassword) {
        this.path = path;
        this.storePassword = storePassword;
        this.keyPassword = keyPassword;
    }

    public void addTrustedCertificate(String alias, Certificate certificate) {
        synchronized (this) {
            try {
                logger.debug(path + " adding certificate alias=" + alias);
                if (store.containsAlias(alias)) {
                    logger.debug(path + " alis=" + alias + " is already preasent");
                    return;
                }
                store.setCertificateEntry(alias, certificate);
                storeKeystore();
            } catch (Exception e) {
                throw new RuntimeException("cannot add trusted certificate for: " + alias, e);
            }
        }
    }

    public void removeTrustedCertificate(String alias) {
        synchronized (this) {
            try {
                logger.debug(path + " removing certificate alias=" + alias);
                if (ActorAlias.equals(alias)){ 
                	throw new Exception("Cannot remove the actor certificate from its own keystore");                	
                }
                store.deleteEntry(alias);
                storeKeystore();
            } catch (Exception e) {
                throw new RuntimeException("cannot remove certificate for: " + alias, e);
            }
        }
    }

    public Certificate getActorCertificate() {
        return getCertificate(ActorAlias);
    }

    public PrivateKey getActorPrivateKey() {
        synchronized (this) {
            try {
                KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) store.getEntry(ActorAlias, new PasswordProtection(keyPassword.toCharArray()));
                if (pkEntry != null) {
                    return pkEntry.getPrivateKey();
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }
        return null;
    }

    public Certificate getCertificate(String alias) {
        synchronized (this) {
            try {
                logger.debug(path + " requesting certificate alias=" + alias);

                return store.getCertificate(alias);
            } catch (Exception e) {
                throw new RuntimeException("cannot obtain certificate for : " + alias, e);
            }
        }
    }

    public void initialize() throws Exception {
        try {
            loadKeystore();
        } catch (Exception e) {
            throw new RuntimeException("KeystoreManager failed to initialize", e);
        }
    }

    protected void loadKeystore() throws Exception {
        FileInputStream fis = null;

        try {
            store = KeyStore.getInstance("JKS");
            fis = new FileInputStream(path);
            store.load(fis, storePassword.toCharArray());
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    protected void storeKeystore() throws Exception {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(path);
            store.store(fos, storePassword.toCharArray());
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static void export64(String[] args) {
        KeystoreManager man = new KeystoreManager(args[1], args[2], null);
        try {
            man.initialize();
        } catch (Exception e) {
            throw new RuntimeException("Cannot initialize actor keystore");
        }
        Certificate cert = man.getActorCertificate();
        if (cert == null) {
            throw new RuntimeException("Missing certificate");
        }
        byte[] bytes = null;

        try {
            bytes = cert.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new RuntimeException("Failed to encode the certificate");
        }

        String base64 = Base64.encodeBytes(bytes);

        System.out.println("Actor certificate in BASE64: \n" + base64);

    }

    public static void usage() {
        System.out.println("Keystore helper. Usage:\n" + "-export64 path storepass");
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
        }

        String cmd = args[0];

        if (cmd.equals("-export64")) {
            if (args.length != 3) {
                usage();
            }

            export64(args);
        } else {
            usage();
        }
    }
}
