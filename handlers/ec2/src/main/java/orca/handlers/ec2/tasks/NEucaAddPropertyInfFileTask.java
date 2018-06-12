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

                    // COMET KOMAL
                    String unitId = getProject().getProperty(UnitProperties.UnitID);
                    String sliceId= getProject().getProperty(UnitProperties.UnitSliceID);
                    String cometHost = getProject().getProperty(OrcaConfiguration.CometHost);
                    NEucaCometDataGenerator cometDataGenerator = new NEucaCometDataGenerator(cometHost, unitId, sliceId);

                    if(NEucaCometDataGenerator.Family.users.toString().equals(section_)) {
                        System.out.println("NEucaAddPropertyInfFileTask::execute: modifying users in");
                        modifyUsers(cometDataGenerator);
                        System.out.println("NEucaAddPropertyInfFileTask::execute: modifying users out");
                    }
                    else if(NEucaCometDataGenerator.Family.interfaces.toString().equals(section_)) {
                        System.out.println("NEucaAddPropertyInfFileTask::execute: modifying interfaces in");
                        modifyInterfaces(cometDataGenerator);
                        System.out.println("NEucaAddPropertyInfFileTask::execute: modifying interfaces out");
                    }
                    else if(NEucaCometDataGenerator.Family.storages.toString().equals(section_)) {
                        System.out.println("NEucaAddPropertyInfFileTask::execute: modifying storages in");
                        modifyStorages(cometDataGenerator);
                        System.out.println("NEucaAddPropertyInfFileTask::execute: modifying storages out");

                    }
                    else if(NEucaCometDataGenerator.Family.routes.toString().equals(section_)) {
                        System.out.println("NEucaAddPropertyInfFileTask::execute: modifying routes in");
                        modifyRoutes(cometDataGenerator);
                        System.out.println("NEucaAddPropertyInfFileTask::execute: modifying routes out");
                    }
                    else if(NEucaCometDataGenerator.Family.scripts.toString().equals(section_)) {
                        System.out.println("NEucaAddPropertyInfFileTask::execute: modifying scripts in");
                        modifyScripts(cometDataGenerator);
                        System.out.println("NEucaAddPropertyInfFileTask::execute: modifying scripts out");
                    }
                    // COMET KOMAL

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

                        // Look up which section to process; section could be any of [global, users, interfaces, storages, routes, scripts]
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

                        if (section.equals("global")) {
                            if(key.equals("unit_id")) {
                                unitId = key;
                            }
                            else if(key.equals("slice_id")) {
                                sliceId = key;
                            }

                        }
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

                    if (!processedKey) {
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

    private void modifyUsers(NEucaCometDataGenerator cometDataGenerator) {
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.users);
        cometDataGenerator.remove(NEucaCometDataGenerator.Family.users, key_);
        String [] arrOfStr = value_.split(":");
        if(arrOfStr.length < 2) {
            System.out.println("NEucaAddPropertyInfFileTask::modifyUsers: Incorret number of parameters");
            return;
        }
        cometDataGenerator.addUser(key_, arrOfStr[0], arrOfStr[1]);
        cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.users);
    }
    private void modifyInterfaces(NEucaCometDataGenerator cometDataGenerator) {
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.interfaces);
        cometDataGenerator.remove(NEucaCometDataGenerator.Family.interfaces, key_);
        String [] arrOfStr = value_.split(":");
        if(arrOfStr.length < 2) {
            System.out.println("NEucaAddPropertyInfFileTask::modifyInterfaces: Incorret number of parameters");
            return;
        }
        if(arrOfStr.length == 2) {
            cometDataGenerator.addInterface(key_, arrOfStr[0], arrOfStr[1], null, null, null);
        }
        else if(arrOfStr.length == 3) {
            cometDataGenerator.addInterface(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], null, null);
        }
        cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.interfaces);
    }
    private void modifyStorages(NEucaCometDataGenerator cometDataGenerator) {
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.storages);
        cometDataGenerator.remove(NEucaCometDataGenerator.Family.storages, key_);
        String [] arrOfStr = value_.split(":");
        if(arrOfStr.length < 3) {
            System.out.println("NEucaAddPropertyInfFileTask::modifyStorages: Incorret number of parameters");
            return;
        }
        if(arrOfStr.length == 3) {
            cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], null, null,
                    null, null, null, null, null,
                    null);
        }
        else if(arrOfStr.length == 4) {
            cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], null,
                    null, null, null, null, null,
                    null);
        }
        else if(arrOfStr.length == 5) {
            cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    null, null, null, null, null,
                    null);
        }
        else if(arrOfStr.length == 6) {
            cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], null, null, null, null,
                    null);
        }
        else if(arrOfStr.length == 7) {
            cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], null, null, null,
                    null);
        }
        else if(arrOfStr.length == 8) {
            cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], arrOfStr[7], null, null,
                    null);
        }
        else if(arrOfStr.length == 9) {
            cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], arrOfStr[7], arrOfStr[8], null,
                    null);
        }
        else if(arrOfStr.length == 10) {
            cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], arrOfStr[7], arrOfStr[8], arrOfStr[9],
                    null);
        }
        else if(arrOfStr.length == 11) {
            cometDataGenerator.addStorage(key_, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], arrOfStr[7], arrOfStr[8], arrOfStr[9],
                    arrOfStr[10]);
        }
        cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.storages);
    }
    private void modifyRoutes(NEucaCometDataGenerator cometDataGenerator) {
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.routes);
        cometDataGenerator.remove(NEucaCometDataGenerator.Family.routes, key_);
        String [] arrOfStr = value_.split(":");
        if(arrOfStr.length < 1) {
            System.out.println("NEucaAddPropertyInfFileTask::modifyRoutes: Incorret number of parameters");
            return;
        }
        cometDataGenerator.addRoute(key_, arrOfStr[0]);
        cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.routes);
    }
    private void modifyScripts(NEucaCometDataGenerator cometDataGenerator) {
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.scripts);
        cometDataGenerator.remove(NEucaCometDataGenerator.Family.scripts, key_);
        String [] arrOfStr = value_.split(":");
        if(arrOfStr.length < 1) {
            System.out.println("NEucaAddPropertyInfFileTask::modifyScripts: Incorret number of parameters");
            return;
        }
        cometDataGenerator.addScript(key_, arrOfStr[0]);
        cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.scripts);
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
