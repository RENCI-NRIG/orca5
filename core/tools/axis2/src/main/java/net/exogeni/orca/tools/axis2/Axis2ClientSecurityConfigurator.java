/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package net.exogeni.orca.tools.axis2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import net.exogeni.orca.util.ScriptExecutionResult;
import net.exogeni.orca.util.ScriptExecutor2;

import org.apache.log4j.Logger;

/**
 * This class is responsible for creating configuration and security files
 * needed to use axis2. There is a single instance of this class in a given JVM.
 * @author aydan
 */
public class Axis2ClientSecurityConfigurator {
    private static final String CNBase = "OU=shirako,O=orca,L=Durham,S=NC,C=US";
    private static final String ScriptsFolder = "/scripts";
    private static final String MyScriptsFolder = "/axis2tools";
    private static String RuntimeDirectory = "/config/runtime";
    private static final String KeyStoresDirectory = "/keystores";
    private static final String Axis2Directory = "/axis2";
    private static final int CodeError = -1;
    private static final String ScriptsPackage = "/net/exogeni/orca/tools/axis2/scripts.tar";
    private static final String CommandCreateKeyStore = "/create.keystore.sh";
    private static final String CommandGenerateKeypair = "/create.keypair.sh";
    private static final String CommandCreateClientProperties = "/create.client.properties.sh";
    private static final String CommandCreateAxis2Config = "/create.axis2.xml.sh";
    private static final String CommandCreateAxis2ConfigNoSign = "/create.nosign.axis2.xml.sh";
    private static final String Axis2XmlTemplate = "/axis2.xml.template";
    
    /**
     * Singleton instance.
     */
    private static Axis2ClientSecurityConfigurator instance;

    static {
        instance = new Axis2ClientSecurityConfigurator();
    }
    
    /**
     * Logging tool.
     */
    private Logger logger;
    /**
     * Directory creation flag.
     */
    private boolean createdDirs = false;

    private Axis2ClientSecurityConfigurator() {
        logger = Logger.getLogger(this.getClass().getCanonicalName());
    }

    /**
     * Creates the given folder
     * @param path path for the folder location
     * @return 0 success, negative - error
     */
    private int createFolder(String path) {
        int code = 0;

        try {
            File folder = new File(path);
            if (folder.exists()) {
                if (!folder.isDirectory()) {
                    logger.error(path + " is a file!");
                    return CodeError;
                }
            } else {
                if (!folder.mkdir()) {
                    logger.error("Failed to create directory: " + path);
                    code = CodeError;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to create directory: " + path, e);
            code = CodeError;
        }

        return code;
    }

    /**
     * Expands the tar package.
     * @param path directory where to expand the package
     * @return
     */
    private int expandPackage(String path) {
        InputStream is = null;
        FileOutputStream fs = null;
        int code = 0;

        try {
            try {
                is = this.getClass().getResourceAsStream(ScriptsPackage);
                if (is == null) {
                    logger.error("Cannot resolve the scripts package");
                    return CodeError;
                }

                File f = new File(path + "/scripts.tar");
                fs = new FileOutputStream(f);

                byte[] buffer = new byte[4096];
                int read = is.read(buffer);

                while (read != -1) {
                    fs.write(buffer, 0, read);
                    read = is.read(buffer);
                }

                String command = "tar -xvf " + f.getAbsolutePath() + " -C " + path;
                code = execute(command);

                if (code == 0) {
                    command = "chmod u+x " + path + "/install.sh";
                    code = execute(command);

                    if (code == 0) {
                        command = path + "/install.sh " + path;
                        code = execute(command);
                        if (code != 0){
                            logger.error("install.sh returned with code: " + code);
                        }
                    } else {
                        logger.error("chmod returned with code: " + code);
                    }
                } else {
                    logger.error("Tar command returned: " + code);
                }
            } finally {
                if (fs != null) {
                    fs.close();
                }

                if (is != null) {
                    is.close();
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error while expanding package to: " + path, e);
            code = CodeError;
        }

        return code;
    }

    private int installScripts(String rootDir) {
        int code = 0;

        try {
            code = createFolder(rootDir + ScriptsFolder);
            if (code != 0) {
                return code;
            }
            
            code = createFolder(rootDir + ScriptsFolder + MyScriptsFolder);
            if (code != 0) {
                return code;
            }
            return expandPackage(rootDir + ScriptsFolder + MyScriptsFolder);
        } catch (Exception e) {
            logger.error("Unexpected error while installing scripts", e);
            code = CodeError;
        }

        return code;
    }

    /**
     * Prepares the folder hierarchy
     * @param rootDir root directory
     * @return 0 success, negative - error
     */
    private synchronized int prepareFolders(String rootDir) {
        int code = 0;

        if (createdDirs) {
            return code;
        }

        try {
            code = installScripts(rootDir);

            if (code != 0) {
                return code;
            }

            code = createFolder(rootDir + RuntimeDirectory);
            if (code != 0) {
                return code;
            }

            code = createFolder(rootDir + RuntimeDirectory + KeyStoresDirectory);
            if (code != 0) {
                return code;
            }

            code = createFolder(rootDir + RuntimeDirectory + Axis2Directory);
            if (code != 0) {
                return code;
            }

            createdDirs = true;
        } catch (Exception e) {
            logger.error("Unexpected error while preparing folders", e);
            code = CodeError;
        }

        return code;
    }

    private String getCN(String actorID) {
        return "CN=" + actorID + "," + CNBase;
    }

    private String getKeyStorePassword(String rootFolder, String actorID) {
        // NOTE: change this only if you know what you are doing!!!
        return "clientkeystorepass";
    }

    private String getKeyAlias(String rootFolder, String actorID) {
        return "actorKey";
    }

    private String getKeyPassword(String rootFolder, String actorID) {
        return "clientkeypass";
    }

    /**
     * Returns the path to the keystore for the given actor.
     * @param rootFolder root folder
     * @param actorID actor identifier
     * @return
     */
    public String getKeyStorePath(String rootFolder, String actorID) {
        return rootFolder + RuntimeDirectory + KeyStoresDirectory + "/" + actorID + ".jks";
    }

    public String getKeyStoreRelativePath(String rootFolder, String actorID) {
        return "." + RuntimeDirectory + KeyStoresDirectory + "/" + actorID + ".jks";
    }

    public String getClientPropertiesPath(String rootFolder, String actorID) {
        return rootFolder + RuntimeDirectory + Axis2Directory + "/" + actorID + ".client.properties";
    }

    public String getClientPropertiesRelativePath(String rootFolder, String actorID) {
        return "." + RuntimeDirectory + Axis2Directory + "/" + actorID + ".client.properties";
    }

    public String getAxis2ConfigPath(String rootFolder, String actorID) {
        return rootFolder + RuntimeDirectory + Axis2Directory + "/" + actorID + ".axis2.xml";
    }

    public String getAxis2ConfigNoSignPath(String rootFolder, String actorID) {
        return rootFolder + RuntimeDirectory + Axis2Directory + "/" + actorID + ".nosign.axis2.xml";
    }

    public String getScriptsPath(String rootFolder) {
        return rootFolder + ScriptsFolder + MyScriptsFolder;
    }

    public String getRuntimeDirectory(String rootFolder) {
        return rootFolder + RuntimeDirectory;
    }

    public void setRuntimeDirectory(String runtimeDirectory) {
        RuntimeDirectory = runtimeDirectory;
    }

    /**
     * Executes the given shell command/script.
     * @param command
     * @return 0 success, negative number - error
     */
    private int execute(String command) {
        int code = 0;

        try {
            logger.debug("executing: " + command);
            ScriptExecutor2 exec = new ScriptExecutor2(new String[] { "bash", "-c", command });
            ScriptExecutionResult result = exec.execute();
            code = result.code;
            if (code != 0){
                logger.debug("code: " + code);
                logger.debug("stdout: " + result.stdout);
                logger.debug("stderr: " + result.stderr);
            }
        } catch (Exception e) {
            logger.error("Unexpected error while executing command", e);
            return CodeError;
        }

        return code;
    }

    /**
     * Creates a keystore for the given actor.
     * @param rootFolder
     * @param actorID
     * @return
     */
    private int createKeyStore(String rootFolder, String actorID) {
        int code = 0;

        try {
            String path = getKeyStorePath(rootFolder, actorID);
            String password = getKeyStorePassword(rootFolder, actorID);
            String command = getScriptsPath(rootFolder) + CommandCreateKeyStore + " " + path + " " + password;
            code = execute(command);
        } catch (Exception e) {
            logger.error("", e);
            code = CodeError;
        }

        return code;
    }

    private int generateKeypair(String rootFolder, String actorID) {
        int code = 0;

        try {
            String path = getKeyStorePath(rootFolder, actorID);
            String keystorePassword = getKeyStorePassword(rootFolder, actorID);
            String alias = getKeyAlias(rootFolder, actorID);
            String keyPassword = getKeyPassword(rootFolder, actorID);
            String cn = getCN(actorID);
            String command = getScriptsPath(rootFolder) + CommandGenerateKeypair + " " + path + " " + keystorePassword + " " + alias + " " + keyPassword + " " + cn;
            code = execute(command);
        } catch (Exception e) {
            logger.error("", e);
            code = CodeError;
        }

        return code;
    }

    private int generateClientProperties(String rootFolder, String actorID) {
        int code = 0;

        try {
            String path = getClientPropertiesPath(rootFolder, actorID);
            String keystore = getKeyStoreRelativePath(rootFolder, actorID);
            String keystorePassword = getKeyStorePassword(rootFolder, actorID);

            String command = getScriptsPath(rootFolder) + CommandCreateClientProperties + " " + path + " " + keystore + " " + keystorePassword;
            code = execute(command);
        } catch (Exception e) {
            logger.error("", e);
            code = CodeError;
        }

        return code;
    }

    private int generateAxis2Config(String rootFolder, String actorID) {
        int code = 0;

        try {
            String propertiesPath = getClientPropertiesRelativePath(rootFolder, actorID);
            String path = getAxis2ConfigPath(rootFolder, actorID);
            String key = getKeyAlias(rootFolder, actorID);
            String template = getScriptsPath(rootFolder) + Axis2XmlTemplate;

            String command = getScriptsPath(rootFolder) + CommandCreateAxis2Config + " " + template + " " + path + " " + key + " " + propertiesPath;
            code = execute(command);

            if (code == 0) {
                path = getAxis2ConfigNoSignPath(rootFolder, actorID);
                command = getScriptsPath(rootFolder) + CommandCreateAxis2ConfigNoSign + " " + template + " " + path + " " + key + " " + propertiesPath;
                code = execute(command);
            }
        } catch (Exception e) {
            logger.error("", e);
            code = CodeError;
        }

        return code;
    }

    /**
     * Creates the security configuration for
     * @param rootFolder
     * @param actorID
     * @return
     */
    public int createActorConfiguration(String rootFolder, String actorID) {
        int code = 0;
        /* make sure all required folders exist */
        code = prepareFolders(rootFolder);

        if (code != 0) {
            return code;
        }

        /* create a keystore for the actor */
        code = createKeyStore(rootFolder, actorID);

        if (code != 0) {
            return code;
        }

        /* generate a keypair for the actor */
        code = generateKeypair(rootFolder, actorID);

        if (code != 0) {
            return code;
        }

        /* generate a client.properties for the actor */
        code = generateClientProperties(rootFolder, actorID);

        if (code != 0) {
            return code;
        }

        /* generate an axis2.config for the actor */
        code = generateAxis2Config(rootFolder, actorID);

        return code;
    }

    /**
     * Removes the security settings for a given actor.
     * @param rootFolder
     * @param actorID
     * @return
     */
    public int removeActorConfiguration(String rootFolder, String actorID) {
        // FIXME: add implementation
        return -1;
    }

    public static Axis2ClientSecurityConfigurator getInstance() {
        return instance;
    }
}
