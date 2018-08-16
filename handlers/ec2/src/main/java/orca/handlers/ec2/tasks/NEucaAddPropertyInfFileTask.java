package orca.handlers.ec2.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import orca.shirako.common.meta.UnitProperties;
import orca.shirako.common.meta.ConfigurationProperties;
import orca.shirako.container.OrcaConfiguration;
import orca.shirako.core.Unit;
import orca.shirako.plugins.config.OrcaAntTask;

import org.apache.tools.ant.BuildException;

import org.ini4j.Ini;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.StringReader;
import java.io.IOException;
import java.io.FileReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import orca.shirako.plugins.config.Config;
import orca.comet.*;


public class NEucaAddPropertyInfFileTask extends OrcaAntTask {
    protected String file_;
    protected String cloudType_;
    protected String outputProperty_;
    protected String userdataOld_;

    protected String section_ = null;
    protected String key_ = null;
    protected String value_ = null;

    public void execute() throws BuildException {

        try {
            super.execute();
            if (file_ == null) {
                throw new Exception("NEucaAddPropertyInfFileTask::execute: Missing file parameter");
            }
            if (cloudType_ == null) {
                throw new Exception("NEucaAddPropertyInfFileTask::execute: Missing cloudType parameter");
            }

            System.out.println("NEucaAddPropertyInfFileTask::execute: file: " + file_ + ", cloudType: " + cloudType_);

            System.out.println("NEucaAddPropertyInfFileTask::execute: section_ = " + section_ + ", key_ = " + key_
                    + ", " + "value_ = " + value_);

            BufferedReader userdataSource;
            if (file_.equals(userdataOld_)) {
                System.out.println("NEucaAddPropertyInfFileTask::execute: reading userdata from file " + file_);
                userdataSource = new BufferedReader(new FileReader(file_));
            } else {
                System.out.println("NEucaAddPropertyInfFileTask::execute: using userdata from userdataOld string");
                userdataSource = new BufferedReader(new StringReader(userdataOld_));
            }

            String userdataNew = "";

            try {
                String section = "";
                String prev_section = "";
                String key = "";

                // remove the colons from the mac address
                if (section_ != null && section_.equals("interfaces")) {
                    key_ = key_.replace(":", "").trim();
                }

                //
                if (section_ == null || key_ == null || value_ == null) {
                    // Nothing to do; no change to UserData
                    System.out.println("NEucaAddPropertyInfFileTask::execute:Nothing to do!");
                    userdataNew = userdataOld_;
                }
                else {
                    StringBuilder sb = new StringBuilder();
                    String line = userdataSource.readLine();

                    // Update Comet if configured
                    String cometHost = getProject().getProperty(OrcaConfiguration.CometHost);
                    String caCert = getProject().getProperty(OrcaConfiguration.CometCaCert);
                    String clientCertKeyStore = getProject().getProperty(OrcaConfiguration.CometClientKeyStore);
                    String clientCertKeyStorePwd = getProject().getProperty(OrcaConfiguration.CometClientKeyStorePwd);
                    String rId = getProject().getProperty(UnitProperties.UnitReservationID);
                    String sliceId = getProject().getProperty(UnitProperties.UnitSliceID);


                    NEucaCometDataGenerator cometDataGenerator = null;

                    if( cometHost != null && caCert != null && clientCertKeyStore != null && clientCertKeyStorePwd != null) {

                        String readToken = getProject().getProperty(Config.PropertySavePrefix + UnitProperties.UnitCometReadToken);
                        String writeToken = getProject().getProperty(UnitProperties.UnitCometWriteToken);

                        cometDataGenerator = new NEucaCometDataGenerator(cometHost, caCert, clientCertKeyStore,
                                clientCertKeyStorePwd, rId, sliceId, readToken, writeToken);

                        if (NEucaCometDataGenerator.Family.users.toString().equals(section_)) {
                            modifyUsers(cometDataGenerator);
                        } else if (NEucaCometDataGenerator.Family.interfaces.toString().equals(section_)) {
                            modifyInterfaces(cometDataGenerator);
                        } else if (NEucaCometDataGenerator.Family.storage.toString().equals(section_)) {
                            modifyStorage(cometDataGenerator);
                        } else if (NEucaCometDataGenerator.Family.routes.toString().equals(section_)) {
                            modifyRoutes(cometDataGenerator);
                        } else if (NEucaCometDataGenerator.Family.scripts.toString().equals(section_)) {
                            modifyScripts(cometDataGenerator);
                        }
                    }

                    boolean processedKey = false;
                    while (line != null) {
                        System.out.println("NEucaAddPropertyInfFileTask::execute: line = " + line);

                        // if there is nothing to add, just copy lines
                        if (section_ == null || key_ == null || value_ == null) {
                            sb.append(line);
                            sb.append(System.lineSeparator());
                            line = userdataSource.readLine();
                            processedKey = true;
                            continue;
                        }

                        // Look up which section to process; section could be any of [global, users, interfaces, storage, routes, scripts]
                        Pattern pattern = Pattern.compile("^\\[(.*?)\\]");
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            prev_section = section;
                            section = matcher.group(1);
                            System.out.println("NEucaAddPropertyInfFileTask::execute: found section " + section);
                        }

                        // check to see if we checked all the existing interfaces. if so, add the new one.
                        if (!processedKey && prev_section.equals(section_)) {
                            System.out.println(
                                    "NEucaAddPropertyInfFileTask::execute: Adding new key to " + prev_section + " section of userdata: " + key_.trim());
                            sb.append(key_.trim() + "=" + value_.trim() + "\n");
                            processedKey = true;
                        }

                        key = line.split("=")[0].trim();
                        System.out.println("NEucaAddPropertyInfFileTask::execute: processing key: " + key);
                        // find key
                        if (section.equals("interfaces")) {
                            if (!processedKey && key.equals(key_)) {
                                // modify an existing interface
                                System.out.println("NEucaAddPropertyInfFileTask::execute: Modifying interface ing userdata: " + key);
                                sb.append(key_.trim() + "=" + value_.trim() + "\n");
                            } else {
                                System.out.println("NEucaAddPropertyInfFileTask::execute: Ignoring interface in userdata: " + key);
                                sb.append(line);
                            }
                        } else {
                            sb.append(line);
                        }

                        sb.append(System.lineSeparator());
                        line = userdataSource.readLine();
                    }

                    if (cometDataGenerator == null && !processedKey) {
                        System.out.println("NEucaAddPropertyInfFileTask::execute: Adding section interfaces userdata and adding interface: " + key);
                        sb.append("[interfaces]");
                        sb.append(key_.trim() + " = " + value_.trim() + "\n");
                        processedKey = true;
                    }

                    userdataNew = sb.toString();
                }
            } finally {
                userdataSource.close();
            }

            userdataSource.close();

            PrintWriter out = new PrintWriter(new FileWriter(new File(file_)));
            out.print(userdataNew);
            out.close();

            if (outputProperty_ != null) {
                getProject().setProperty(outputProperty_, userdataNew);
            }

        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("NEucaAddPropertyInfFileTask::execute: An error occurred: " + e.getMessage(), e);
        }
    }

    /*
     * @brief function to update users as part of modifySlice
     *
     * @param cometDataGenerator - Comet Data Generator
     *
     * @throws NEucaCometException in case of error
     *
     */
    private void modifyUsers(NEucaCometDataGenerator cometDataGenerator) throws NEucaCometException{
        System.out.println("NEucaAddPropertyInfFileTask::modifyUsers: IN");
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.users);
        cometDataGenerator.remove(NEucaCometDataGenerator.Family.users, key_);
        String [] arrOfStr = value_.split(":");
        if(arrOfStr.length < 2) {
            System.out.println("NEucaAddPropertyInfFileTask::modifyUsers: Incorrect number of parameters");
            throw new NEucaCometException("NEucaAddPropertyInfFileTask::modifyUsers: Incorrect number of parameters");
        }
        if(cometDataGenerator.addUser(key_, arrOfStr[0], arrOfStr[1])) {
            if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.users, "")) {
                throw new NEucaCometException("NEucaAddPropertyInfFileTask::modifyUsers: Unable to store users in comet");
            }
        }
        else {
            throw new NEucaCometException("NEucaAddPropertyInfFileTask::modifyUsers: Unable to add users in comet");
        }
        System.out.println("NEucaAddPropertyInfFileTask::modifyUsers: OUT");
    }

    /*
     * @brief function to update interfaces as part of modifySlice
     *
     * @param cometDataGenerator - Comet Data Generator
     *
     * @throws NEucaCometException in case of error
     *
     */
    private void modifyInterfaces(NEucaCometDataGenerator cometDataGenerator) throws NEucaCometException{
        System.out.println("NEucaAddPropertyInfFileTask::modifyInterfaces: IN");
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.interfaces);
        cometDataGenerator.remove(NEucaCometDataGenerator.Family.interfaces, key_);
        String [] arrOfStr = value_.split(":");
        System.out.println("val: " + value_);
        System.out.println("Len: " + arrOfStr.length);
        if(arrOfStr.length < 2) {
            System.out.println("NEucaAddPropertyInfFileTask::modifyInterfaces: Incorrect number of parameters");
            throw new NEucaCometException("NEucaAddPropertyInfFileTask::modifyInterfaces: Incorrect number of parameters");
        }
        boolean save = false;
        if(arrOfStr.length == 2) {
            save = cometDataGenerator.addInterface(key_, arrOfStr[0], arrOfStr[1], null, null, null);
        }
        else if(arrOfStr.length == 3) {
            save = cometDataGenerator.addInterface(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], null, null);
        }
        if(save) {
            save = cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.interfaces, "");
        }
        if(!save) {
            throw new NEucaCometException("NEucaAddPropertyInfFileTask::modifyInterfaces: Unable to store interfaces in comet");
        }
        System.out.println("NEucaAddPropertyInfFileTask::modifyInterfaces: OUT");
    }

    /*
     * @brief function to update storage as part of modifySlice
     *
     * @param cometDataGenerator - Comet Data Generator
     *
     * @throws NEucaCometException in case of error
     *
     */
    private void modifyStorage(NEucaCometDataGenerator cometDataGenerator) throws NEucaCometException{
        System.out.println("NEucaAddPropertyInfFileTask::modifyStorage: IN");
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.storage);
        cometDataGenerator.remove(NEucaCometDataGenerator.Family.storage, key_);
        String[] arrOfStr = value_.split(":");
        if (arrOfStr.length < 3) {
            System.out.println("NEucaAddPropertyInfFileTask::modifyStorage: Incorrect number of parameters");
            throw new NEucaCometException("NEucaAddPropertyInfFileTask::modifyStorage: Incorrect number of parameters");
        }
        boolean save = false;
        if (arrOfStr.length == 3) {
            save = cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], null, null,
                    null, null, null, null, null,
                    null);
        } else if (arrOfStr.length == 4) {
            save = cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], null,
                    null, null, null, null, null,
                    null);
        } else if (arrOfStr.length == 5) {
            save = cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    null, null, null, null, null,
                    null);
        } else if (arrOfStr.length == 6) {
            save = cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], null, null, null, null,
                    null);
        } else if (arrOfStr.length == 7) {
            save = cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], null, null, null,
                    null);
        } else if (arrOfStr.length == 8) {
            save = cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], arrOfStr[7], null, null,
                    null);
        } else if (arrOfStr.length == 9) {
            save = cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], arrOfStr[7], arrOfStr[8], null,
                    null);
        } else if (arrOfStr.length == 10) {
            save = cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], arrOfStr[7], arrOfStr[8], arrOfStr[9],
                    null);
        } else if (arrOfStr.length == 11) {
            save = cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], arrOfStr[7], arrOfStr[8], arrOfStr[9],
                    arrOfStr[10]);
        }
        if (save) {
            save = cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.storage, "");
        }
        if(!save) {
            throw new NEucaCometException("NEucaAddPropertyInfFileTask::modifyStorage: Unable to store storage in comet");
        }
        System.out.println("NEucaAddPropertyInfFileTask::modifyStorage: OUT");
    }

    /*
     * @brief function to update routes as part of modifySlice
     *
     * @param cometDataGenerator - Comet Data Generator
     *
     * @throws NEucaCometException in case of error
     *
     */
    private void modifyRoutes(NEucaCometDataGenerator cometDataGenerator) throws NEucaCometException{
        System.out.println("NEucaAddPropertyInfFileTask::modifyRoutes: IN");
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.routes);
        cometDataGenerator.remove(NEucaCometDataGenerator.Family.routes, key_);
        String [] arrOfStr = value_.split(":");
        if(arrOfStr.length < 1) {
            System.out.println("NEucaAddPropertyInfFileTask::modifyRoutes: Incorrect number of parameters");
            throw new NEucaCometException("NEucaAddPropertyInfFileTask::modifyRoutes: Incorrect number of parameters");
        }
        if(cometDataGenerator.addRoute(key_, arrOfStr[0], null, null)) {
            if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.routes, "")) {
                throw new NEucaCometException("NEucaAddPropertyInfFileTask::modifyRoutes: Unable to store routes in comet");
            }
        }
        else {
            throw new NEucaCometException("NEucaAddPropertyInfFileTask::modifyUsers: Unable to add routes in comet");
        }
        System.out.println("NEucaAddPropertyInfFileTask::modifyRoutes: OUT");
    }

    /*
     * @brief function to update scripts as part of modifySlice
     *
     * @param cometDataGenerator - Comet Data Generator
     *
     * @throws NEucaCometException in case of error
     *
     */
    private void modifyScripts(NEucaCometDataGenerator cometDataGenerator) throws NEucaCometException{
        System.out.println("NEucaAddPropertyInfFileTask::modifyScripts: IN");
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.scripts);
        cometDataGenerator.remove(NEucaCometDataGenerator.Family.scripts, key_);
        String[] arrOfStr = value_.split(":");
        if (arrOfStr.length < 1) {
            System.out.println("NEucaAddPropertyInfFileTask::modifyScripts: Incorrect number of parameters");
            throw new NEucaCometException("NEucaAddPropertyInfFileTask::modifyScripts: Incorrect number of parameters");
        }
        if (cometDataGenerator.addScript(key_, arrOfStr[0])) {
            if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.scripts, "")){
                throw new NEucaCometException("NEucaAddPropertyInfFileTask::modifyScripts: Unable to store scripts in comet");
            }
        }
        else {
            throw new NEucaCometException("NEucaAddPropertyInfFileTask::modifyUsers: Unable to add scripts in comet");
        }
        System.out.println("NEucaAddPropertyInfFileTask::modifyScripts: OUT");
    }

    public void setFile(String file) {
        file_ = file;
    }

    public void setCloudtype(String cloudtype) {
        cloudType_ = cloudtype;
    }

    public void setSection(String section) {
        section_ = section;
    }

    public void setKey(String key) {
        key_ = key;
    }

    public void setValue(String value) {
        value_ = value;
    }

    public void setOutputproperty(String outputproperty) {
        outputProperty_ = outputproperty;
    }

    public void setUserdataold(String userdataold) {
        userdataOld_ = userdataold;
    }

}
