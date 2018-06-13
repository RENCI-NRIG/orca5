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

public class NEucaRemovePropertyInfFileTask extends OrcaAntTask {
    protected String file_;
    protected String cloudType_;
    protected String outputProperty_;
    protected String userdataOld_;

    protected String section_ = null;
    protected String key_ = null;

    public void execute() throws BuildException {

        try {
            super.execute();
            if (file_ == null) {
                throw new Exception("NEucaRemovePropertyInfFileTask::execute: Missing file parameter");
            }
            if (cloudType_ == null) {
                throw new Exception("NEucaRemovePropertyInfFileTask::execute: Missing cloudType parameter");
            }

            System.out.println("NEucaRemovePropertyInfFileTask::execute: file: " + file_ + ", cloudType: " + cloudType_);

            System.out.println("NEucaRemovePropertyInfFileTask::execute: remove section_ = " + section_ + ", key_ = " + key_);

            BufferedReader userdataSource;
            if (file_.equals(userdataOld_)) {
                System.out.println("NEucaRemovePropertyInfFileTask::execute: reading userdata from file " + file_);
                userdataSource = new BufferedReader(new FileReader(file_));
            } else {
                System.out.println("NEucaRemovePropertyInfFileTask::execute: using userdata from userdataOld string");
                userdataSource = new BufferedReader(new StringReader(userdataOld_));
            }

            String userdataNew = "";
            if (section_ == null || key_ == null) {
                // Nothing to do; no change to UserData
                System.out.println("NEucaRemovePropertyInfFileTask::execute:Nothing to do!");
                userdataNew = userdataOld_;
            }
            else {
                try {
                    StringBuilder sb = new StringBuilder();
                    String line = userdataSource.readLine();

                    String section = "";
                    String prev_section = "";

                    // remove the colons from the mac address
                    if (section_ != null && section_.equals("interfaces")) {
                        key_ = key_.replace(":", "").trim();
                    }

                    // COMET KOMAL
                    String unitId = getProject().getProperty(UnitProperties.UnitID);
                    String sliceId= getProject().getProperty(UnitProperties.UnitSliceID);
                    String cometHost = getProject().getProperty(OrcaConfiguration.CometHost);
                    NEucaCometDataGenerator cometDataGenerator = new NEucaCometDataGenerator(cometHost, unitId, sliceId);

                    if(NEucaCometDataGenerator.Family.users.toString().equals(section_)) {
                        System.out.println("NEucaAddPropertyInfFileTask::execute: removing users in");
                        if(cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.users) &&
                           cometDataGenerator.remove(NEucaCometDataGenerator.Family.users, key_)) {
                            cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.users);
                        }
                        System.out.println("NEucaAddPropertyInfFileTask::execute: removing users out");
                    }
                    else if(NEucaCometDataGenerator.Family.interfaces.toString().equals(section_)) {
                        System.out.println("NEucaAddPropertyInfFileTask::execute: removing interfaces in");
                        if(cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.interfaces) &&
                        cometDataGenerator.remove(NEucaCometDataGenerator.Family.interfaces, key_)) {
                            cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.interfaces);
                        }
                        System.out.println("NEucaAddPropertyInfFileTask::execute: removing interfaces out");
                    }
                    else if(NEucaCometDataGenerator.Family.storages.toString().equals(section_)) {
                        System.out.println("NEucaAddPropertyInfFileTask::execute: removing storages in");
                        if(cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.storages) &&
                        cometDataGenerator.remove(NEucaCometDataGenerator.Family.storages, key_)) {
                            cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.storages);
                        }
                        System.out.println("NEucaAddPropertyInfFileTask::execute: removing storages out");
                    }
                    else if(NEucaCometDataGenerator.Family.routes.toString().equals(section_)) {
                        System.out.println("NEucaAddPropertyInfFileTask::execute: removing routes in");
                        if(cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.routes) &&
                        cometDataGenerator.remove(NEucaCometDataGenerator.Family.routes, key_)) {
                            cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.routes);
                        }
                        System.out.println("NEucaAddPropertyInfFileTask::execute: removing routes out");
                    }
                    else if(NEucaCometDataGenerator.Family.scripts.toString().equals(section_)) {
                        System.out.println("NEucaAddPropertyInfFileTask::execute: removing scripts in");
                        if(cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.scripts) &&
                        cometDataGenerator.remove(NEucaCometDataGenerator.Family.scripts, key_)) {
                            cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.scripts);
                        }
                        System.out.println("NEucaAddPropertyInfFileTask::execute: removing scripts out");
                    }
                    // COMET KOMAL

                    boolean processedKey = false;
                    while (line != null) {
                        System.out.println("NEucaRemovePropertyInfFileTask::execute: line = " + line);

                        // if there is nothing to remove, just copy lines
                        if (section_ == null || key_ == null) {
                            sb.append(line);
                            sb.append(System.lineSeparator());
                            line = userdataSource.readLine();
                            processedKey = true;
                            continue;
                        }

                        Pattern pattern = Pattern.compile("^\\[(.*?)\\]");
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            prev_section = section;
                            section = matcher.group(1);
                            sb.append(line);
                            sb.append(System.lineSeparator());
                            System.out.println("found section " + section);
                            line = userdataSource.readLine();
                            continue;
                        }

                        // check to see if we checked all the existing interfaces. if so, add the new one.
                        String key = line.split("=")[0].trim();
                        System.out.println("NEucaRemovePropertyInfFileTask::execute: processing line:  key: " + key + ", section: " + section);

                        // skip if we find a match... remember we are removing the key/value
                        if (!(section.equals(section_) && key.equals(key_))) {
                            System.out.println("NEucaRemovePropertyInfFileTask::execute: Keeping line: " + line);
                            sb.append(line);
                            sb.append(System.lineSeparator());
                        } else {
                            System.out.println("NEucaRemovePropertyInfFileTask::execute: Deleting line: " + line);
                        }

                        line = userdataSource.readLine();
                    }

                    userdataNew = sb.toString();

                } finally {
                    userdataSource.close();
                }
            }
            userdataSource.close();

            PrintWriter out = new PrintWriter(new FileWriter(new File(file_)));
            out.print(userdataNew);
            out.close();

            if (outputProperty_ != null)
                getProject().setProperty(outputProperty_, userdataNew);

        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("NEucaRemovePropertyInfFileTask::execute: An error occurred: " + e.getMessage(), e);
        }
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

    public void setUserdataold(String userdataold) {
        userdataOld_ = userdataold;
    }

}
