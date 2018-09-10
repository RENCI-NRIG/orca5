/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.nodeagent;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import net.exogeni.orca.drivers.DriverFactory;
import net.exogeni.orca.drivers.DriverId;
import net.exogeni.orca.drivers.IDriver;
import net.exogeni.orca.drivers.util.DriverScriptExecutionResult;
import net.exogeni.orca.drivers.util.DriverScriptExecutor;
import net.exogeni.orca.nodeagent.documents.GetServiceKeyResultElement;
import net.exogeni.orca.nodeagent.documents.RegisterAuthorityKeyResultElement;
import net.exogeni.orca.nodeagent.documents.GetServiceKeyResultElement;
import net.exogeni.orca.nodeagent.documents.RegisterAuthorityKeyResultElement;

import org.apache.axiom.attachments.utils.IOUtils;
import org.apache.axis2.util.Base64;
import org.apache.log4j.Logger;

public class NodeAgentService {
    public static String PropertyExceptionMessage = "driver.exception.message";
    public static String PropertyExceptionStack = "driver.exception.stack";
    public static String AuthHash = "AUTH_HASH";
    public static String NodeToken = "NODE_TOKEN";
    private String cmdlineAuthorityHash = null;
    private String cmdlineNodeToken = null;
    static NodeAgentService instance = new NodeAgentService();
    protected DriverFactory factory;
    protected Logger logger;
    protected boolean debug = true;
    protected String root = RootPathResolver.getRoot();
    protected String dataFolder = root + "/data";
    protected String driversFile = dataFolder + "/drivers.xml";
    protected String ServerKeyStoreDestination = dataFolder + "/server.jks";
    private boolean called = false; // flag if registerFirstKey was called
    protected String serverKeyStoreName = "server.jks";
    protected String serverStorePassword = "serverstorepass";
    protected String serverAlias = "serverkey";

    // alias of the first registered key
    protected String authorityKeyEntry = "firstkey";

    // filename to load the cmdline from (only for testing purposes)
    protected String testCmdLineFile = NodeAgentConstants.TestCmdLineFile;

    protected NodeAgentService() {
        logger = Logger.getLogger(this.getClass().getCanonicalName());
        factory = new DriverFactory();
        factory.setDriversRoot(dataFolder + "/drivers/");
        loadDrivers();
    }

    protected void loadDrivers() {
        try {
            logger.debug("Root folder: " + root);
            logger.debug("Data folder: " + dataFolder);
            logger.debug("Drivers file: " + driversFile);

            int code = factory.load(driversFile);
            logger.debug("loadDrivers exit code: " + code);
        } catch (Exception e) {
            logger.error("An error in loadDrivers", e);
        }
    }

    public String getRoot() {
        return root;
    }

    public String getDataFolder() {
        return dataFolder;
    }

    protected synchronized int expandPackage(DriverId id, DataHandler pkg, boolean fresh) {
        logger.debug("Trying to expand package: fresh=" + fresh);

        int code = 0;

        String folder = factory.getDriverRoot(id);
        File f = new File(folder);

        if (f.exists() && fresh) {
            logger.debug("driver is already installed");

            return DriverFactory.CodeDriverIsAlreadyInstalled;
        } else {
            logger.debug("driver folder does not exist");
        }

        if (fresh && !f.mkdir()) {
            logger.debug("cannot create folder: " + f.getAbsolutePath());

            return DriverFactory.CodeCannotCreateDirectory;
        } else {
            logger.debug("folder created: " + f.getAbsolutePath());
        }

        try {
            File temp = new File(f.getAbsolutePath() + "/install.tar.gz");
            InputStream inStream;
            inStream = pkg.getDataSource().getInputStream();

            byte[] bytes = IOUtils.getStreamAsByteArray(inStream);

            // no longer needed for axis2 1.1.1
            // String encoded = new String(bytes);
            // bytes = Base64.decode(encoded);
            FileOutputStream fs = new FileOutputStream(temp);
            fs.write(bytes);
            fs.close();

            String fileName = temp.getAbsolutePath();
            String command = "tar xvfz " + fileName + " -C " + folder;
            DriverScriptExecutor exec = new DriverScriptExecutor(command);
            DriverScriptExecutionResult result = exec.execute();
            code = result.code;
            temp.delete();

            if (code == 0) {
                File inst = new File(f.getAbsolutePath() + "/install.sh");

                if (inst.exists()) {
                    command = "chmod u+x " + f.getAbsolutePath() + "/install.sh";
                    exec = new DriverScriptExecutor(command);
                    result = exec.execute();
                    code = result.code;

                    if (code == 0) {
                        command = f.getAbsolutePath() + "/install.sh " + f.getAbsolutePath();
                        exec = new DriverScriptExecutor(command);
                        result = exec.execute();
                        code = result.code;

                        if (code != 0) {
                            removePackage(id);
                        }
                    } else {
                        removePackage(id);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("expandPackage", e);
            code = DriverFactory.CodeCannotExpandPackage;

            try {
                removePackage(id);
            } catch (Exception ee) {
                logger.error("", ee);
            }
        }

        return code;
    }

    /**
     * Installs the specified driver
     * 
     * @param id
     *            Driver id
     * @param className
     *            Class name
     * @param pkg 
     *            Path to jar file
     * @return int
     */
    public int installDriver(DriverId id, String className, DataHandler pkg) {
        int code = 0;

        try {
            if (pkg != null) {
                code = expandPackage(id, pkg, true);

                if (code != 0) {
                    return code;
                } // else {
                  // path = factory.getDriverRoot(id) + "/" + path;
                  // }
            }

            code = factory.install(id, className);

            if (code == 0) {
                synchronized (driversFile) {
                    code = factory.save(driversFile);
                }
            } else {
                removePackage(id);
            }
        } catch (Exception e) {
            code = NodeAgentConstants.CodeInternalError;
        }

        logger.debug("Finished install driver");
        logger.debug("Exit code: " + code);

        return code;
    }

    /**
     * Removes the package folder and its contents.
     * 
     * @param id
     *            driver identifier
     * @return exit code
     * @throws Exception in case of error
     */
    protected int removePackage(DriverId id) throws Exception {
        String command = "rm -rf " + factory.getDriverRoot(id);
        DriverScriptExecutor exec = new DriverScriptExecutor(command);
        DriverScriptExecutionResult result = exec.execute();

        return result.code;
    }

    /**
     * Upgrades the specified driver
     * 
     * @param id
     *            Driver id
     * @param className
     *            Class name
     * @param pkg 
     *            Path to jar file
     * @return int
     */
    public int upgradeDriver(DriverId id, String className, DataHandler pkg) {
        int code = 0;

        try {
            if (pkg != null) {
                code = expandPackage(id, pkg, false);

                if (code != 0) {
                    return code;
                } // else {
                  // path = factory.getDriverRoot(id) + "/" + path;
                  // }
            }

            code = factory.upgrade(id, className);

            if (code == 0) {
                synchronized (driversFile) {
                    code = factory.save(driversFile);
                }
            }
        } catch (Exception e) {
            code = NodeAgentConstants.CodeInternalError;
        }

        logger.debug("Finished upgrade driver");
        logger.debug("Exit code: " + code);

        return code;
    }

    /**
     * Uninstalls the specified driver
     * 
     * @param id
     *            Driver id
     * @return int
     */
    public int uninstall(DriverId id) {
        int code = 0;

        try {
            removePackage(id);
            code = factory.uninstall(id);

            if (code == 0) {
                synchronized (driversFile) {
                    code = factory.save(driversFile);
                }
            }
        } catch (Exception e) {
            code = NodeAgentConstants.CodeInternalError;
        }

        logger.debug("Finished uninstall driver");
        logger.debug("Exit code: " + code);

        return code;
    }

    /**
     * Executes the specified script passing the given arguments
     * 
     * @param name name
     * @param arguments arguments
     * @return DriverScriptExecutionResult
     */
    public DriverScriptExecutionResult executeScript(String name, String arguments) {
        DriverScriptExecutionResult r = new DriverScriptExecutionResult();

        try {
            String command = name + " " + arguments;
            logger.debug("command: " + command);
            logger.debug("Starting execution");

            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(command);

            InputStream in = proc.getInputStream();
            InputStreamReader inr = new InputStreamReader(in);
            BufferedReader inbr = new BufferedReader(inr);

            InputStream err = proc.getErrorStream();
            InputStreamReader errr = new InputStreamReader(err);
            BufferedReader errbr = new BufferedReader(errr);

            String line;
            boolean moreIn = true;
            boolean moreErr = true;
            StringBuffer inb = new StringBuffer();
            StringBuffer eb = new StringBuffer();

            while (moreIn || moreErr) {
                if (moreIn) {
                    line = inbr.readLine();

                    if (line == null) {
                        moreIn = false;
                    } else {
                        if (inb.length() > 0) {
                            inb.append("\n");
                        }

                        inb.append(line);
                    }
                }

                if (moreErr) {
                    line = errbr.readLine();

                    if (line == null) {
                        moreErr = false;
                    } else {
                        if (eb.length() > 0) {
                            eb.append("\n");
                        }

                        eb.append(line);
                    }
                }
            }

            int code = proc.waitFor();
            r.code = code;
            r.stdout = inb.toString();
            r.stderr = eb.toString();
        } catch (Exception e) {
            r.code = NodeAgentConstants.CodeInternalError;
            r.stdout = e.getMessage();
            r.stderr = getStackTraceString(e);
        }

        logger.debug("Finished execution");
        logger.debug("Error code: " + r.code);
        logger.debug("Standard out: " + r.stdout);
        logger.debug("Standard error: " + r.stderr);

        return r;
    }

    /**
     * Executes the specified operation on the given driver
     * 
     * @param driverId
     *            Driver identifier
     * @param actionId
     *            Action identifier
     * @param in
     *            Incoming properties list
     * @param out
     *            Outgoing properties list
     * @return int
     */
    public int executeDriver(DriverId driverId, String actionId, Properties in, Properties out) {
        logger.debug("DriverId: " + driverId);
        logger.debug("ActionId: " + actionId);
        logger.debug("Properties: " + in.toString());

        IDriver driver = factory.getDriver(driverId);

        if (driver == null) {
            logger.warn("Invalid Driver");

            return NodeAgentConstants.CodeInvalidDriver;
        }

        int code = 0;

        try {
            logger.debug("Starting driver execution");
            code = driver.dispatch(actionId, in, out);
        } catch (Exception e) {
            logger.error("executionDriver", e);
            out.setProperty(PropertyExceptionMessage, e.getMessage());
            out.setProperty(PropertyExceptionStack, getStackTraceString(e));

            return NodeAgentConstants.CodeInternalDriverError;
        }

        logger.debug("Finished driver execution");
        logger.debug("Exit code: " + code);

        return code;
    }

    /**
     * Executes the specified operation on the given driver for the given object
     * 
     * @param driverId
     *            Driver identifier
     * @param objectId
     *            Object identifier
     * @param actionId
     *            Action identifier
     * @param in
     *            Incoming properties list
     * @param out
     *            Outgoing properties list
     * @return int
     */
    public int executeObjectDriver(DriverId driverId, String objectId, String actionId, Properties in, Properties out) {
        logger.debug("DriverId: " + driverId);
        logger.debug("ObjectId: " + objectId);
        logger.debug("ActionId: " + actionId);
        logger.debug("Properties: " + in.toString());

        IDriver driver = factory.getDriver(driverId);

        if (driver == null) {
            logger.warn("Invalid Driver");

            return NodeAgentConstants.CodeInvalidDriver;
        }

        int code = 0;

        try {
            logger.debug("Starting driver execution");
            code = driver.dispatch2(objectId, actionId, in, out);
        } catch (Exception e) {
            logger.error("executionDriver", e);
            out.setProperty(PropertyExceptionMessage, e.getMessage());
            out.setProperty(PropertyExceptionStack, getStackTraceString(e));

            return NodeAgentConstants.CodeInternalDriverError;
        }

        logger.debug("Finished driver execution");
        logger.debug("Exit code: " + code);

        return code;
    }

    /**
     * register the public key/certificate with the service (the certificate is stored in the keystore)
     * 
     * @param alias
     *            used to store the certificate in the server keystore
     * @param certificateEncoding
     *            - certificate enconded in bytes
     * @return 0 success 1 error
     */
    public int registerKey(String alias, byte[] certificateEncoding) {
        logger.debug("registerKey called for alias " + alias);

        ByteArrayInputStream inStream = new ByteArrayInputStream(certificateEncoding);

        Certificate cert = null;

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = cf.generateCertificate(inStream);

            while (inStream.available() > 0) {
                cert = cf.generateCertificate(inStream);
                logger.debug("Adding certificate to server key store " + cert.toString());
            }

            logger.debug("Request for registering certificate: " + cert.toString());
        } catch (Exception ex) {
            logger.debug("Exception at registerKey");
            logger.debug(ex.getMessage());

            return NodeAgentConstants.CodeInternalError;
        }

        // load certificate into the server keystore
        if (cert != null) {
            MerlinKeyRegister mr = MerlinKeyRegister.getInstance();

            if (mr == null) {
                logger.debug("MerlinReloader is NULL !!!");

                return NodeAgentConstants.CodeInternalError;
            } else {
                logger.debug("MerlinReloader is not NULL !!!");

                try {
                    mr.addKeyToKeystore(alias, cert); // add the certificate
                                                      // to the keystore
                } catch (Exception ex) {
                    logger.debug("Exception while registering key " + ex.getMessage());

                    return NodeAgentConstants.CodeInternalError;
                }
            }
        } else {
            logger.debug("Certificate is null !!!");

            return NodeAgentConstants.CodeInternalError;
        }

        return NodeAgentConstants.CodeOK;
    }

    /**
     * uregister the public key/certificate associated with alias
     * 
     * @param alias
     *            of the certificate to be removed from the server keystore
     * @return 0 success 1 error
     */
    public int unregisterKey(String alias) {
        MerlinKeyRegister mr = MerlinKeyRegister.getInstance();

        if (mr == null) {
            logger.debug("MerlinReloader is NULL !!!");

            return NodeAgentConstants.CodeInternalError;
        } else {
            try {
                mr.removeKeyFromKeyStore(alias); // remove the key from the
                                                 // keystore
            } catch (Exception ex) {
                logger.debug("Exception while unregistering key " + ex.getMessage());

                return NodeAgentConstants.CodeInternalError;
            }
        }

        return NodeAgentConstants.CodeOK;
    }

    /**
     * Registers the authority key with the node. Used to claim the node either by the Authority or Service Manager.
     * 
     * @param alias
     *            authority alias
     * @param certificateEncoding
     *            certificate
     * @param message
     *            parameters passed to the node (Authority IP, timestamp ...)
     * @param signature
     *            signature of the message
     * @return RegisterAuthorityKeyResultElement
     */
    public RegisterAuthorityKeyResultElement registerAuthorityKey(String alias, byte[] certificateEncoding,
                                                                  byte[] message, byte[] signature) {
        logger.debug("registerAuthorityKey requested for alias " + alias);

        if (getCalled() == true) // function already called
        {
            RegisterAuthorityKeyResultElement gskre = buildServiceKeyResultElement();

            return gskre;
        }

        // load the bootcommandline
        int val = loadCmdLineParameters();

        if (val != NodeAgentConstants.CodeOK) {
            // testing purposes try to load the cmdline from a file
            val = loadCmdLineParametersFromFile(testCmdLineFile);

            if (val != NodeAgentConstants.CodeOK) {
                RegisterAuthorityKeyResultElement gskre = errorServiceKeyResultElement(val);

                return gskre;
            }
        }

        // verify hash(Cert) == hash from boot commandline
        val = verifyCertificateHash(certificateEncoding);

        if (val != NodeAgentConstants.CodeOK) {
            RegisterAuthorityKeyResultElement gskre = errorServiceKeyResultElement(val);

            return gskre;
        }

        // the two hashes match
        // verify signature of the message (NodeIP, NodeToken
        // AuthorityIP, Timestamp)
        val = verifyMessageSignature(certificateEncoding, message, signature);

        if (val != NodeAgentConstants.CodeOK) {
            RegisterAuthorityKeyResultElement gskre = errorServiceKeyResultElement(val);

            return gskre;
        }

        // get the message
        String messageString = new String(message);
        logger.debug("message received from client");
        logger.debug(messageString);

        KeyMasterMessage km = new KeyMasterMessage();
        logger.debug("creating keymastermessage");
        km.buildXMLDocument(messageString);

        logger.debug("extracting msgNodeIP");

        String msgNodeIP = km.getNode("nodeIP");
        logger.debug("msgNodeIP " + msgNodeIP);

        String msgAuthIP = km.getNode("authorityIP");
        logger.debug("msgAuthIP " + msgAuthIP);

        String msgTimestamp = km.getNode("timestamp");
        logger.debug("msgTimestamp " + msgTimestamp);

        logger.debug("check message parameters!!!");
        val = checkMessageParameters(msgNodeIP, msgTimestamp, msgAuthIP);

        if (val != NodeAgentConstants.CodeOK) {
            RegisterAuthorityKeyResultElement gskre = errorServiceKeyResultElement(val);

            return gskre;
        }

        // check if server.jks is in /data check if it is ok
        // create the server.jks
        logger.debug("cheking recovery!!!");

        int recoveryVal = MerlinKeyRegister.checkRecovery();

        // idempotent code ok we have already registered a key
        if (recoveryVal == MerlinKeyRegister.CodeRecoverySuccessful) {
            logger.debug("recovery succeeded");

            // the initial call was restored through recovery
            setCalled(true);

            RegisterAuthorityKeyResultElement gskre = buildServiceKeyResultElement();

            return gskre;
        } else {
            if (recoveryVal == MerlinKeyRegister.CodeNoRecovery) {
                // insert Authority certificate in server.jks
                // generate K+ and K- for the node manager
                Certificate cert = generateCertificate(certificateEncoding);

                if (cert == null) {
                    logger.debug("received certificate is null!");

                    RegisterAuthorityKeyResultElement gskre = errorServiceKeyResultElement(val);

                    return gskre;
                }

                val = MerlinKeyRegister.createServiceKeyStore(alias, cert);

                if (val != NodeAgentConstants.CodeOK) {
                    logger.debug("error while creating service keys");

                    RegisterAuthorityKeyResultElement gskre = errorServiceKeyResultElement(val);

                    return gskre;
                }

                setCalled(true); // created service keys successfuly

                RegisterAuthorityKeyResultElement gskre = buildServiceKeyResultElement();

                return gskre;
            } else {
                RegisterAuthorityKeyResultElement gskre = errorServiceKeyResultElement(
                        NodeAgentConstants.CodeInternalError);

                return gskre;
            }
        }
    }

    /**
     * Retrieves the service public key.
     * 
     * @return GetServiceKeyResultElement
     */
    public GetServiceKeyResultElement getServiceKey() {
        logger.debug("getServiceKey invoked");

        GetServiceKeyResultElement gskre = new GetServiceKeyResultElement();

        // check if the authority registered a key with this node
        if (getCalled() != true) {
            // check if recovery
            int recoveryVal = MerlinKeyRegister.checkRecovery(); // idempotent
                                                                 // code ok we have already registered a key

            if (recoveryVal != MerlinKeyRegister.CodeRecoverySuccessful) {
                // no recovery and no key registration
                logger.debug("The Service Key has not been generated");
                gskre.setCode(NodeAgentConstants.CodeInternalError);
                gskre.setKey(new byte[1]);

                return gskre;
            } else {
                // XXX: is this code necessary??
                int val = loadCmdLineParameters(); // load the commandline
                                                   // parameters

                if (val != NodeAgentConstants.CodeOK) {
                    // testing purposes try to load the cmdline from a file
                    val = loadCmdLineParametersFromFile(testCmdLineFile);

                    if (val != NodeAgentConstants.CodeOK) {
                        gskre.setCode(NodeAgentConstants.CodeInternalError);
                        gskre.setKey(new byte[1]);

                        return gskre;
                    }
                }

                setCalled(true); // mark that recovery succeeded
            }
        }

        // load the certificate - there should be no errors if recovery
        // succeeded
        Certificate cert = MerlinKeyRegister.getServiceKey();

        if (cert == null) {
            // no service key
            gskre.setCode(NodeAgentConstants.CodeInternalError);
            gskre.setKey(new byte[1]);
        } else {
            try {
                byte[] certBytes = cert.getEncoded();
                gskre.setCode(NodeAgentConstants.CodeOK);
                gskre.setKey(certBytes);
            } catch (CertificateEncodingException ex) {
                logger.debug("Service certificate encoding exception");
                gskre.setCode(NodeAgentConstants.CodeInternalError);
                gskre.setKey(new byte[1]);

                return gskre;
            }
        }

        return gskre;
    }

    /**
     * Constructs the reply message in case of successful authority key registration.
     * 
     * @return RegisterAuthorityKeyResultElement
     */
    private RegisterAuthorityKeyResultElement buildServiceKeyResultElement() {
        RegisterAuthorityKeyResultElement gskre = new RegisterAuthorityKeyResultElement();

        try {
            Certificate cert = MerlinKeyRegister.getServiceKey();
            PublicKey pk = cert.getPublicKey();
            String pkFormat = pk.getFormat();

            logger.debug("pkFormat for the public key is " + pkFormat);

            // create a shared key
            KeyGenerator kg = KeyGenerator.getInstance("TripleDES");
            SecretKey secretKey = kg.generateKey();
            byte[] skByteEnc = secretKey.getEncoded();
            String base64Str = Base64.encode(skByteEnc);

            logger.debug("answer to encrypt with private key");
            logger.debug(base64Str);

            // encrypt strDoc
            byte[] toEncrypt = base64Str.getBytes();

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            Certificate certAuthority = MerlinKeyRegister.getAuthorityKey();
            cipher.init(Cipher.ENCRYPT_MODE, certAuthority.getPublicKey());

            byte[] encryptedBytes = cipher.doFinal(toEncrypt);

            gskre.setKey(encryptedBytes);

            byte[] certBytes = cert.getEncoded();

            KeyMasterMessage km = new KeyMasterMessage();
            String[] nodes1 = { "nodecertificate", "nodetoken" };
            km.createXMLDocument(nodes1);

            String base64StrCert = Base64.encode(certBytes);
            km.setNode("nodecertificate", base64StrCert);
            km.setNode("nodetoken", cmdlineNodeToken);

            String strDoc = km.getXMLDocumentAsString();
            byte[] toEncryptDoc = strDoc.getBytes();

            Cipher cipherSym = Cipher.getInstance("TripleDES");
            cipherSym.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptedBytesDoc = cipherSym.doFinal(toEncryptDoc);

            gskre.setResponse(encryptedBytesDoc);
            gskre.setCode(NodeAgentConstants.CodeOK);

            return gskre;
        } catch (Exception ex) {
            logger.debug("the certificate could not be read");
            logger.debug("error message is " + ex.getMessage());
            gskre.setKey(new byte[1]);
            gskre.setResponse(new byte[1]);
            gskre.setCode(NodeAgentConstants.CodeInternalError);

            return gskre;
        }
    }

    /**
     * Builds an error message to return to the Authority in case of a fault during authority key registration.
     * 
     * @param val
     *            error returned
     * @return RegisterAuthorityKeyResultElement
     */
    private RegisterAuthorityKeyResultElement errorServiceKeyResultElement(int val) {
        RegisterAuthorityKeyResultElement gskre = new RegisterAuthorityKeyResultElement();
        gskre.setCode(val);
        gskre.setKey(new byte[1]);
        gskre.setResponse(new byte[1]);

        return gskre;
    }

    /**
     * Constructs a string from the given stack trace
     * 
     * @param e e
     * @return String
     */
    public String getStackTraceString(Exception e) {
        StackTraceElement[] trace = e.getStackTrace();

        StringBuffer sb = new StringBuffer();
        sb.append("Exception stack trace: \n");

        for (int i = 0; i < trace.length; i++) {
            sb.append(trace[i].toString());
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Returns the logger
     * 
     * @return logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns the singleton instance
     * 
     * @return NodeAgentService
     */
    public static NodeAgentService getInstance() {
        return instance;
    }

    /**
     * Helper function: Generate a certificate from a an array of bytes.
     * 
     * @param certificateEncoding certificateEncoding
     * @return Certificate
     */
    private Certificate generateCertificate(byte[] certificateEncoding) {
        ByteArrayInputStream inStream = new ByteArrayInputStream(certificateEncoding);

        Certificate cert = null;

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = cf.generateCertificate(inStream);

            while (inStream.available() > 0) {
                cert = cf.generateCertificate(inStream);
            }
        } catch (Exception ex) {
            logger.debug("Invalid certificate !!!");
            logger.debug(ex.getMessage());
        }

        return cert;
    }

    /**
     * Helper function: Compute the hexadecimal representation of a computed digest.
     * 
     * @param digest digest
     * @return String
     */
    private String toHexString(byte[] digest) {
        char[] hexValues = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        StringBuffer stringBuf = new StringBuffer();
        int len = digest.length;

        for (int i = 0; i < len; i++) {
            int first = ((digest[i] & 0xf0) >> 4);
            int second = (digest[i] & 0x0f);
            stringBuf.append(hexValues[first]);
            stringBuf.append(hexValues[second]);

            if (i != (len - 1)) {
                stringBuf.append(":");
            }
        }

        return stringBuf.toString();
    }

    /**
     * Checks if the hash value provided on the boot commandline equals the hash value computed over the received
     * certificate.
     * 
     * @param certificateEnconding certificateEnconding
     * @return int
     */
    private int verifyCertificateHash(byte[] certificateEnconding) {
        // verify the certificate provided
        Certificate cert = generateCertificate(certificateEnconding);

        if (cert == null) {
            return NodeAgentConstants.CodeInternalError;
        }

        try {
            cert.verify(cert.getPublicKey());
        } catch (Exception ex) {
            logger.debug("exception while verifying the certificate");

            return NodeAgentConstants.CodeInvalidArguments;
        }

        // compute the hash of the certificate
        String digestType = null;
        String sigAlg = ((X509Certificate) cert).getSigAlgName();

        if (sigAlg.contains("SHA")) {
            digestType = "SHA";
        } else if (sigAlg.contains("MD5")) {
            digestType = "MD5";
        } else {
            logger.debug("unknown digest type sigAlg is " + sigAlg);

            return NodeAgentConstants.CodeInvalidArguments;
        }

        String computedHash = null;

        try {
            MessageDigest messageDigest = MessageDigest.getInstance(digestType);

            messageDigest.update(certificateEnconding);

            byte[] digestValue = messageDigest.digest();

            computedHash = toHexString(digestValue);

            logger.debug(digestType + " for certificate is " + computedHash);
        } catch (NoSuchAlgorithmException ex) {
            logger.debug("exception while computing the digest value");

            return NodeAgentConstants.CodeInternalError;
        }

        if (cmdlineAuthorityHash.compareTo(computedHash) != 0) {
            logger.debug("hashes do not match: " + cmdlineAuthorityHash + " != " + computedHash);

            return NodeAgentConstants.CodeInvalidArguments;
        }

        return NodeAgentConstants.CodeOK;
    }

    /**
     * Verifies if the message provided by the controller is signed with its certificate.
     * 
     * @param certificateEnconding certificateEnconding
     * @param message message
     * @param signature signature
     *
     * @return int
     */
    private int verifyMessageSignature(byte[] certificateEnconding, byte[] message, byte[] signature) {
        Certificate cert = generateCertificate(certificateEnconding);

        if (cert == null) {
            return NodeAgentConstants.CodeInternalError;
        }

        try {
            cert.verify(cert.getPublicKey());
        } catch (Exception ex) {
            logger.debug("exception while verifying the certificate");

            return NodeAgentConstants.CodeInvalidArguments;
        }

        // compute the hash of the certificate
        String digestType = null;
        String sigAlg = ((X509Certificate) cert).getSigAlgName();

        if (sigAlg.contains("SHA")) {
            digestType = "SHA";
        } else if (sigAlg.contains("MD5")) {
            digestType = "MD5";
        } else {
            logger.debug("unknown digest type sigAlg is " + sigAlg);

            return NodeAgentConstants.CodeInvalidArguments;
        }

        try {
            Signature sig = null;

            if (digestType.contains("MD5")) {
                sig = Signature.getInstance("MD5withRSA");
            } else if (digestType.contains("SHA")) {
                sig = Signature.getInstance("SHA1withRSA");
            }

            sig.initVerify(cert);
            sig.update(message);

            if (sig.verify(signature)) {
                logger.debug("Signature verified succesfully!");
            } else {
                logger.debug("Signature verification failed!");

                return NodeAgentConstants.CodeInvalidArguments;
            }
        } catch (Exception ex) {
            logger.debug("Exception while verifying the signature");

            return NodeAgentConstants.CodeInvalidArguments;
        }

        return NodeAgentConstants.CodeOK;
    }

    /**
     * Retrieves the parameters from the boot command line.
     * 
     * @return int
     */
    private int loadCmdLineParameters() {
        // read hash from the cmdline
        try {
            RandomAccessFile rf = new RandomAccessFile("/proc/cmdline", "r");
            String cmdline = rf.readLine();

            int fromIndex = cmdline.indexOf(AuthHash);

            if (fromIndex == -1) {
                logger.debug("could not read " + AuthHash + " from /proc/cmdline");

                return NodeAgentConstants.CodeInternalError;
            }

            int argLen = AuthHash.length() + 1;
            int toIndex = cmdline.indexOf(" ", fromIndex);

            // in case the AUTH_HASH is the last parameter in the command line
            if (toIndex == -1) {
                toIndex = cmdline.length();
            }

            cmdlineAuthorityHash = cmdline.substring(fromIndex + argLen, toIndex);

            if (cmdlineAuthorityHash == null) {
                logger.debug("hash not provided in the boot command line");

                return NodeAgentConstants.CodeInternalError;
            }

            fromIndex = cmdline.indexOf(NodeToken);

            if (fromIndex == -1) {
                logger.debug("could not read " + NodeToken + " from /proc/cmdline");

                return NodeAgentConstants.CodeInternalError;
            }

            argLen = NodeToken.length() + 1;
            toIndex = cmdline.indexOf(" ", fromIndex);

            // in case the NODE_TOKEN is the last parameter in the command line
            if (toIndex == -1) {
                toIndex = cmdline.length();
            }

            cmdlineNodeToken = cmdline.substring(fromIndex + argLen, toIndex);

            if (cmdlineNodeToken == null) {
                logger.debug("Node's token not provided in the boot command line");

                return NodeAgentConstants.CodeInternalError;
            }

            logger.debug("read cmdlineAuthorityHash " + cmdlineAuthorityHash);
            logger.debug("read cmdlineNodeToken " + cmdlineNodeToken);
            logger.debug("cmdline read is " + cmdline);
        } catch (Exception ex) {
            logger.debug("exception while reading /proc/cmdline");

            return NodeAgentConstants.CodeInternalError;
        }

        return NodeAgentConstants.CodeOK;
    }

    /**
     * For testing purposes: retrieves the boot command line parameters from a file.
     * 
     * @param filename filename
     * @return int
     */
    private int loadCmdLineParametersFromFile(String filename) {
        InputStream in = NodeAgentServiceStub.class.getClassLoader().getResourceAsStream(filename);

        if (in == null) {
            logger.debug("Could not read file " + filename);
        }

        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);

        // read hash from the file
        try {
            String cmdline = br.readLine();
            logger.debug("cmdline read is " + cmdline);

            if (cmdline == null) {
                logger.debug("could not read the cmdline from file " + filename);

                return NodeAgentConstants.CodeInternalError;
            }

            int fromIndex = cmdline.indexOf(AuthHash);

            if (fromIndex == -1) {
                logger.debug("could not read " + AuthHash + " from " + filename);

                return NodeAgentConstants.CodeInternalError;
            }

            int argLen = AuthHash.length() + 1;
            int toIndex = cmdline.indexOf(" ", fromIndex);

            // in case the AUTH_HASH is the last parameter in the command line
            if (toIndex == -1) {
                toIndex = cmdline.length();
            }

            cmdlineAuthorityHash = cmdline.substring(fromIndex + argLen, toIndex);

            if (cmdlineAuthorityHash == null) {
                logger.debug("hash not provided in the boot command line");

                return NodeAgentConstants.CodeInternalError;
            }

            fromIndex = cmdline.indexOf(NodeToken);

            if (fromIndex == -1) {
                logger.debug("could not read " + NodeToken + " from " + filename);

                return NodeAgentConstants.CodeInternalError;
            }

            argLen = NodeToken.length() + 1;
            toIndex = cmdline.indexOf(" ", fromIndex);

            // in case the NODE_TOKEN is the last parameter in the command line
            if (toIndex == -1) {
                toIndex = cmdline.length();
            }

            cmdlineNodeToken = cmdline.substring(fromIndex + argLen, toIndex);

            if (cmdlineNodeToken == null) {
                logger.debug("Node's token not provided in the boot command line");

                return NodeAgentConstants.CodeInternalError;
            }

            logger.debug("read cmdlineAuthorityHash " + cmdlineAuthorityHash);
            logger.debug("read cmdlineNodeToken " + cmdlineNodeToken);
        } catch (Exception ex) {
            logger.debug("exception while reading file " + filename);
            logger.debug(ex.getMessage());

            return NodeAgentConstants.CodeInternalError;
        }

        return NodeAgentConstants.CodeOK;
    }

    /**
     * Verifies the recived message parameters. Currently only the node IP is verified. Timestamp validation is not
     * implemented.
     * 
     * @param msgNodeIP msgNodeIP
     * @param msgTimestamp msgTimestamp
     * @param msgAuthIP msgAuthIP
     * @return int
     */
    private int checkMessageParameters(String msgNodeIP, String msgTimestamp, String msgAuthIP) {
        // check if IP provided == node own IP
        // try {
        // InetAddress addr = InetAddress.getLocalHost();
        //
        // // Get IP Address
        // String nodeIP = addr.getHostAddress();
        // if (msgNodeIP.compareTo(nodeIP) != 0) {
        // logger.debug("NodeIP in message " + msgNodeIP + " != " + nodeIP + "
        // retrieved by node");
        // return NodeAgentConstants.CodeInvalidArguments;
        // }
        // } catch (UnknownHostException e) {
        // logger.error("could not retrieve local address", e);
        // return NodeAgentConstants.CodeInternalError;
        // }

        // check if the timestamp is recent ???? NOT IMPLEMENTED
        // if there is no time value available on the node set the time to
        // timestamp
        return NodeAgentConstants.CodeOK;
    }

    /**
     * Sets the called flag once the registerAuthorityKey was called.
     * 
     * @param flag flag
     */
    public synchronized void setCalled(boolean flag) {
        called = flag;
    }

    /**
     * Gets the called flag value (used to test if the registerAuthorityKey function was called.).
     * 
     * @return true or false
     */
    public synchronized boolean getCalled() {
        return called;
    }
}
