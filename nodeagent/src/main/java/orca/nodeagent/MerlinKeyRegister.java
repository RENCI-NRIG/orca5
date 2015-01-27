/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.nodeagent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Properties;

import orca.util.ID;

import org.apache.log4j.Logger;
import org.apache.ws.security.components.crypto.CredentialException;
import org.apache.ws.security.components.crypto.Merlin;

/**
 * @author ionut
 * @author aydan
 */
public class MerlinKeyRegister extends Merlin {
    /**
     * Admin certificate alias.
     */
    public static final String AuthorityKeyStoreEntry = "firstkey";
    /**
     * Service certificat alias.
     */
    public static final String ServiceKeyStoreEntry = "serverkey";
    /**
     * Validity interval for the server key: approximately 10 years.
     */
    public static final String ServerKeyValidity = "3650";
    /**
     * Size for the server key: 2KB.
     */
    public static final String ServerKeySize = "2048";
    /**
     * Service keystore name.
     */
    public static final String ServerKeyStoreName = "server.jks";
    /**
     * Properties file.
     */
    public static final String ServicePropertiesFile = "service.properties";
    // error codes
    public static final int CodeInternalError = 1;
    public static final int CodeRecoverySuccessful = 7;
    public static final int CodeNoRecovery = 6;

    /*
     * Keystore password. FIXME: should be changed and obfuscated.
     */
    private static final String ServerKeyPassword = "serverkeypass";

    /**
     * Logger.
     */
    protected final static Logger logger = Logger.getLogger(MerlinKeyRegister.class.getCanonicalName());
    /**
     * Service root directory.
     */
    protected final static String root = RootPathResolver.getRoot();
    /**
     * Service data folder.
     */
    protected final static String dataFolder = root +  "/data/";
    /**
     * Keystore location.
     */
    protected static final String ServerKeyStoreDestination = dataFolder + ServerKeyStoreName;
    /**
     * The singleton instance.
     */
    protected static MerlinKeyRegister mr = null;
    /**
     * The actual keystore.
     */
    protected static KeyStore keyStore = null;

    protected static String serverPassword;

    /**
     * Returns the singleton instance.
     * @return
     */
    public static MerlinKeyRegister getInstance() {
        return mr;
    }

    public MerlinKeyRegister(Properties prop) throws IOException, CredentialException {
        super(prop);
        logger.debug("Constructor (prop) invoked. mr=" + mr);
        try {
            mr = this;
            verify(prop);
        } catch (Exception e) {
            logger.error("Could not create instance", e);
            throw new RuntimeException(e);
        }
        logger.debug("Constructor (prop) completed");
    }

    public MerlinKeyRegister(Properties prop, ClassLoader cls) throws IOException, CredentialException {
        super(prop, cls);
        logger.debug("Constructor (prop, cls) invoked. mr=" + mr);
        try {
            mr = this;
            verify(prop);
        } catch (Exception e) {
            logger.error("Could not create instance", e);
            throw new RuntimeException(e);
        }
        logger.debug("Constructor (prop, cls) completed");
    }

    private void verify(Properties prop) throws IOException {
        logger.debug("verify called");
        logger.debug("Root directory is: " + root);
        logger.debug("Server keystore path is: " + ServerKeyStoreDestination);
        
        if (keyStore != null) {
            // the keyStore is initialized I have to set it
            // so that it used by the crypto provider
            logger.debug("Merlin KeyStore is not null, setting it");
            setKeyStore(keyStore);
        } else {// the keyStore is not initialized we attempt to execute
            // recovery
            logger.debug("Merlin Keystore instance is null.");
            
            File f = new File(ServerKeyStoreDestination);

            if (f.exists()) // call recovery
            {
                logger.debug("We have a keystore");

                int recVal = checkRecovery();

                if (recVal == MerlinKeyRegister.CodeRecoverySuccessful) {
                    logger.debug("Recovery succeeded");
                    setKeyStore(keyStore);
                } else {
                    logger.debug("Recovery failed");
                }
            } else {
                logger.debug("No keystore to recover!");
            }
        }
    }

    /**
     * Sets the keystore to be used by the Merlin Provider.
     */
    public void setKeyStore(KeyStore arg0) {
        super.setKeyStore(arg0);
        logger.debug("keystore set");
    }

    /**
     * Tries recovery for a new instance of the Merlin Provider,
     * @return
     */
    public static synchronized int checkRecovery() {
        // check if there is a file in /data containing the first alias and
        // first certificate
        logger.debug("Recovery invoked!");
        logger.debug("Root directory is: " + root);
        logger.debug("Server keystore path is: " + ServerKeyStoreDestination);
        
        InputStream in = MerlinKeyRegister.class.getClassLoader().getResourceAsStream(ServicePropertiesFile);

        if (in == null) {
            logger.debug("can not get the crypto provider properties");

            return CodeInternalError;
        }

        Properties prop = new Properties();

        try {
            prop.load(in);
        } catch (Exception ex) {
            logger.debug("can not load the crypto provider as properties");

            return CodeInternalError;
        }

        serverPassword = prop.getProperty("org.apache.ws.security.crypto.merlin.keystore.password");

        File file = new File(ServerKeyStoreDestination);

        if (file.exists()) {
            // check if the authority certificate is inside
            try {
                FileInputStream fisKeyStore = new FileInputStream(ServerKeyStoreDestination);
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(fisKeyStore, serverPassword.toCharArray());

                logger.debug("keystore loaded succesfully!");

                // verify the authority certificate
                Certificate cert = ks.getCertificate(AuthorityKeyStoreEntry);

                if (cert == null) // there is no such certificate in the
                // server.jks
                {
                    logger.debug("could not find the authority certificate");
                    file.delete(); // delete server.jks

                    return MerlinKeyRegister.CodeNoRecovery;
                }

                PublicKey pk = cert.getPublicKey();

                try {
                    cert.verify(pk);
                } catch (Exception ex) {
                    logger.debug("exception while verifying authority certificate during recovery");
                    file.delete();

                    return MerlinKeyRegister.CodeNoRecovery; // recovery
                    // failed
                }

                // verify service certificate
                cert = ks.getCertificate(ServiceKeyStoreEntry);

                if (cert == null) // there is no such certificate in the
                // server.jks
                {
                    logger.debug("could not find a service certificate");
                    file.delete(); // delete server.jks

                    return MerlinKeyRegister.CodeNoRecovery;
                }

                pk = cert.getPublicKey();

                try {
                    cert.verify(pk);
                } catch (Exception ex) {
                    logger.debug("exception while verifying service certificate during recovery");
                    file.delete();

                    return MerlinKeyRegister.CodeNoRecovery; // recovery
                    // failed
                }

                // verify that authorized_hosts has the public key
                keyStore = ks; // set the keystore to be used if recovery
                // succeeded

                return MerlinKeyRegister.CodeRecoverySuccessful;
            } catch (KeyStoreException ex) {
                logger.debug("exception at keystore while trying recovery");
                file.delete();

                return MerlinKeyRegister.CodeNoRecovery;
            } catch (NoSuchAlgorithmException ex) {
                logger.debug("exception at keystore while trying recovery");
                file.delete();

                return MerlinKeyRegister.CodeNoRecovery;
            } catch (CertificateException ex) {
                logger.debug("exception at keystore while trying recovery");
                file.delete();

                return MerlinKeyRegister.CodeNoRecovery;
            } catch (IOException ex) {
                logger.debug("exception at keystore while trying recovery");
                file.delete();

                return MerlinKeyRegister.CodeNoRecovery;
            }
        } else {
            logger.debug("Could not locate the keystore file!");

            return MerlinKeyRegister.CodeNoRecovery;
        }
    }

    /**
     * Creates server.jks when registerAuthorityKey is invoked. Creates the
     * private key / certificate of the node
     * @param alias this alias should be "authoritykey", we will use
     *            "authoritykey"
     * @param cert the first trusted certificate to be added
     * @return
     */
    public static synchronized int createServiceKeyStore(String alias, Certificate cert) {
        if (keyStore != null) {
            logger.debug("keyStore already initialized createServiceKeyStore is invoked multiple times!!!");

            return CodeInternalError; // this should be
            // called only once
            // -> if keyStore !=
            // null this has
            // been called
            // already
        }

        InputStream in = MerlinKeyRegister.class.getClassLoader().getResourceAsStream(ServicePropertiesFile);

        if (in == null) {
            logger.debug("can not get the crypto provider properties");

            return CodeInternalError;
        }

        Properties prop = new Properties();

        try {
            prop.load(in);
        } catch (IOException ex) {
            logger.debug("cannot load the crypto provider as properties");

            return CodeInternalError;
        }

        serverPassword = prop.getProperty("org.apache.ws.security.crypto.merlin.keystore.password");

        // String keyStoreInstance =
        // prop.getProperty("org.apache.ws.security.crypto.merlin.keystore.type");
        File file = new File(ServerKeyStoreDestination);

        if (file.exists()) {
            logger.debug(ServerKeyStoreDestination + " exists, multiple calls to createServiceKeystore !!!");

            return MerlinKeyRegister.CodeInternalError; // this should be
            // called only once
            // -> if keyStore !=
            // null this has
            // been called
            // already
        }

        try {
            logger.debug("creating the keystore");

            FileOutputStream fos = new FileOutputStream(file);
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null, serverPassword.toCharArray());
            logger.debug("keystore created");

            ks.setCertificateEntry(AuthorityKeyStoreEntry, cert);

            logger.debug("authority certificate saved");
            ks.store(fos, serverPassword.toCharArray());
            fos.close();

            logger.debug("stored the authority certificate");

            // generate service keys
            int val = generateServiceCertificate();

            if (val != 0) {
                logger.debug("could not generate service certificate");

                return CodeInternalError;
            }

            logger.debug("generated the service certificate\n");

            // load the keyStore with the authority certificate and the service
            // certificate
            FileInputStream fin = new FileInputStream(file);
            keyStore = KeyStore.getInstance("JKS");
            keyStore.load(fin, serverPassword.toCharArray());
        } catch (KeyStoreException ex) {
            logger.debug("Exception while creating service keystore");
            logger.debug(ex.getMessage());

            return CodeInternalError;
        } catch (IOException ex) {
            logger.debug("Exception could not create/write to " + ServerKeyStoreDestination);

            return CodeInternalError;
        } catch (NoSuchAlgorithmException ex) {
            logger.debug("Exception could not store the keystore in " + ServerKeyStoreDestination);

            return CodeInternalError;
        } catch (CertificateException ex) {
            logger.debug("Exception could not store the keystore in " + ServerKeyStoreDestination);

            return CodeInternalError;
        }

        return 0;
    }

    /**
     * Retrieves the service key.
     * @return
     */
    public static synchronized Certificate getServiceKey() {
        logger.debug("Merlin request for service key");

        // retrieve the service certificate
        try {
            Certificate cert = keyStore.getCertificate(ServiceKeyStoreEntry);

            return cert;
        } catch (KeyStoreException ex) {
            logger.debug("could not read the certificate " + ServiceKeyStoreEntry + " from the keystore");

            return null;
        }
    }

    /**
     * Retrieves the first certificate registered with the node
     * @return
     */
    public static synchronized Certificate getAuthorityKey() {
        logger.debug("Merlin request for authority key");

        // retrieve the service certificate
        try {
            Certificate cert = keyStore.getCertificate(AuthorityKeyStoreEntry);

            return cert;
        } catch (KeyStoreException ex) {
            logger.debug("could not read the certificate " + AuthorityKeyStoreEntry + " from the keystore");

            return null;
        }
    }

    /**
     * Adds a certificate to the keystore. Allows multiple clients to use the
     * node. The actor invoking this function has to be trusted.
     * @param alias
     * @param cert
     * @throws IOException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public void addKeyToKeystore(String alias, Certificate cert) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        KeyStore.TrustedCertificateEntry trustedEntry = new KeyStore.TrustedCertificateEntry(cert);

        // the keystore already contains an alias for this certificate
        if (keyStore.containsAlias(alias)) {
            return;
        }

        keyStore.setEntry(alias, trustedEntry, null);
        keyStore.store(new FileOutputStream(ServerKeyStoreDestination), serverPassword.toCharArray());
        setKeyStore(keyStore);
    }

    /**
     * Removes a trusted key from the node. The actor invoking this function has
     * to be trusted.
     * @param alias
     * @throws IOException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     */
    public void removeKeyFromKeyStore(String alias) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        // there is no entry with alias
        if (!keyStore.containsAlias(alias)) {
            return;
        }

        keyStore.deleteEntry(alias);
        keyStore.store(new FileOutputStream(ServerKeyStoreDestination), serverPassword.toCharArray());
        setKeyStore(keyStore);
    }

    /**
     * Generates the node certificate when the first trusted key is registered
     * with the node.
     * @return
     */
    private static int generateServiceCertificate() {
        logger.debug("generate service certificate called");

        try {
            // identifier to use to generate a unique CN
            ID id = new ID();

            String[] cmd = new String[18];
            cmd[0] = "keytool";
            cmd[1] = "-genkey";
            cmd[2] = "-alias";
            cmd[3] = ServiceKeyStoreEntry;
            cmd[4] = "-keystore";
            cmd[5] = ServerKeyStoreName;
            cmd[6] = "-keyalg";
            cmd[7] = "rsa";
            cmd[8] = "-storepass";
            cmd[9] = serverPassword;
            cmd[10] = "-keypass";
            cmd[11] = ServerKeyPassword;
            cmd[12] = "-dname";
            cmd[13] = "CN=" + id.toString() + ",OU=orca,O=nicl,L=Durham,S=NC,C=US";
            cmd[14] = "-validity";
            cmd[15] = ServerKeyValidity;
            cmd[16] = "-keysize";
            cmd[17] = ServerKeySize;

            String cmdToExecute = "";

            for (int i = 0; i < 14; i++) {
                cmdToExecute = cmdToExecute + cmd[i] + " ";
            }

            logger.debug("executing command");
            logger.debug(cmdToExecute);

            File dir = new File(dataFolder);

            logger.debug("executing keytool command");

            Process p = Runtime.getRuntime().exec(cmd, null, dir);
            logger.debug("keytool command executed in " + dir);

            // call keytool to generate the service keys and certificate
            // Process p = Runtime.getRuntime().exec(cmd, null, dir);
            logger.debug("keytool command called waiting for result");
            p.waitFor();

            String line = "stdout \n";
            String line1;

            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            while ((line1 = input.readLine()) != null) {
                line = line + line1 + "\n";
            }

            logger.debug(line);
            line = "stderr \n";

            BufferedReader input2 = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while ((line1 = input2.readLine()) != null) {
                line = line + line1 + "\n";
            }

            logger.debug(line);

            int exitValue = p.exitValue();
            logger.debug("keytool exited with code " + exitValue);

            if (exitValue != 0) {
                logger.debug("keytool exited with error " + exitValue);

                return CodeInternalError;
            }
        } catch (IOException ex) {
            logger.debug("could not run the keytool command");

            return CodeInternalError;
        } catch (InterruptedException ex) {
            logger.debug("interrupted while executing the keytool command");

            return CodeInternalError;
        }

        logger.debug("");

        return 0;
    }
}
