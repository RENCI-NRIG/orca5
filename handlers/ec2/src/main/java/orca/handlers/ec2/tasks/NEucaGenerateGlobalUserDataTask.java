package orca.handlers.ec2.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import orca.shirako.common.meta.UnitProperties;
import orca.shirako.common.meta.ConfigurationProperties;
import orca.shirako.container.OrcaConfiguration;
import orca.shirako.plugins.config.Config;
import orca.shirako.plugins.config.OrcaAntTask;

import org.apache.tools.ant.BuildException;
import org.apache.commons.lang3.RandomStringUtils;
import java.security.SecureRandom;
import orca.comet.*;

class NEucaGenerateGlobalUserData {
    String outputProperty;

    org.apache.tools.ant.Project project;

    protected org.apache.tools.ant.Project getProject() {
        return project;
    }

    // Random string generator for read and write token
    public static String generateRandomString() {
        return RandomStringUtils.random( 10, true, true);
    }

    public NEucaGenerateGlobalUserData (org.apache.tools.ant.Project project) {
        this.project = project;
        this.outputProperty = "";
    }

    public void doIt(PrintWriter out) throws Exception {
        generateGlobal(out);
    }

    public String getOutputProperty() {
        return outputProperty;
    }

    protected void generateGlobal(PrintWriter out) throws Exception {
        out.println("[global]");
        String temp = getProject().getProperty(UnitProperties.UnitActorID);
        if (temp != null) {
            out.println("actor_id=" + temp);
        } else {
            out.println(";actor_id= Not Specified");
        }

        String sliceId = null;
        temp = getProject().getProperty(UnitProperties.UnitSliceID);
        if (temp != null) {
            out.println("slice_id=" + temp);
            sliceId = temp;
        } else {
            out.println(";slice_id= Not Specified");
        }

        String rId = null;
        temp = getProject().getProperty(UnitProperties.UnitReservationID);
        if (temp != null) {
            out.println("reservation_id=" + temp);
            rId = temp;
        } else {
            out.println(";reservation_id= Not Specified");
        }

        temp = getProject().getProperty(UnitProperties.UnitID);
        if (temp != null) {
            out.println("unit_id=" + temp);
        } else {
            out.println(";unit_id= Not Specified");
        }

        temp = getProject().getProperty(UnitProperties.UnitRouter);
        if (temp != null) {
            out.println("router=" + temp);
        } else {
            out.println(";router= Not Specified");
        }

        temp = getProject().getProperty(UnitProperties.UnitISCSIInitiatorIQN);
        if (temp != null) {
            out.println("iscsi_initiator_iqn=" + temp);
        } else {
            out.println(";iscsi_initiator_iqn= Not Specified");
        }

        temp = getProject().getProperty(UnitProperties.UnitSliceName);
        if (temp != null) {
            out.println("slice_name=" + temp);
        } else {
            out.println(";slice_name= Not Specified");
        }

        temp = getProject().getProperty(UnitProperties.UnitURL);
        if (temp != null) {
            out.println("unit_url=" + temp);
        } else {
            out.println(";unit_url= Not Specified");
        }

        temp = getProject().getProperty(UnitProperties.UnitHostName);
        if (temp != null) {
            out.println("host_name=" + temp);
        } else {
            out.println(";host_name= Not Specified");
        }

        temp = getProject().getProperty("shirako.save." + UnitProperties.UnitManagementIP);
        if (temp != null) {
            out.println("management_ip=" + temp);
        } else {
            out.println(";management_ip= Not Specified");
        }

        temp = getProject().getProperty("shirako.save." + UnitProperties.UnitEC2Host);
        if (temp != null) {
            out.println("physical_host=" + temp);
        } else {
            out.println(";physical_host= Not Specified");
        }

        temp = getProject().getProperty("shirako.save.unit.ec2.instance");
        if (temp != null) {
            out.println("nova_id=" + temp);
        } else {
            out.println(";nova_id= Not Specified");
        }

        // Create Comet Data Generator object if Comet is configured
        temp = getProject().getProperty(OrcaConfiguration.CometHost);
        String caCert = getProject().getProperty(OrcaConfiguration.CometCaCert);
        String clientCertKeyStore = getProject().getProperty(OrcaConfiguration.CometClientKeyStore);
        String clientCertKeyStorePwd = getProject().getProperty(OrcaConfiguration.CometClientKeyStorePwd);

        // Save comethost and readToken in global section of Openstack meta data
        if (temp != null && caCert != null && clientCertKeyStore != null && clientCertKeyStorePwd != null) {
            String readToken = generateRandomString();
            String writeToken = generateRandomString();

            out.println("comethost=" + temp);
            out.println("cometreadtoken=" + readToken);
            temp = getProject().getProperty(UnitProperties.SliceCometReadToken);
            if (temp != null) {
                out.println("slicecometreadtoken=" + temp);
            }
            temp = getProject().getProperty(UnitProperties.SliceCometWriteToken);
            if (temp != null) {
                out.println("slicecometwritetoken=" + temp);
            }
            temp = getProject().getProperty(UnitProperties.UnitCometHostsGroupToRead);
            if (temp != null) {
                out.println("comethostsgroupread=" + temp);
            }
            temp = getProject().getProperty(UnitProperties.UnitCometHostsGroupToWrite);
            if (temp != null) {
                out.println("comethostsgroupwrite=" + temp);
            }
            temp = getProject().getProperty(UnitProperties.UnitCometPubKeysGroupToRead);
            if (temp != null) {
                out.println("cometpubkeysgroupread=" + temp);
            }
            temp = getProject().getProperty(UnitProperties.UnitCometPubKeysGroupToWrite);
            if (temp != null) {
                out.println("cometpubkeysgroupwrite=" + temp);
            }

            // Save the readToken and writeToken in the properties
            getProject().setProperty(Config.PropertySavePrefix + UnitProperties.UnitCometReadToken, readToken);
            getProject().setProperty(Config.PropertySavePrefix + UnitProperties.UnitCometWriteToken, writeToken);
        } else {
            System.out.println("cometHost=" + temp + " caCert=" + caCert + " clientCertKeyStore=" +
                    clientCertKeyStore + " clientCertKeyStorePwd=" + clientCertKeyStorePwd);
            throw new NEucaCometException("Comet is not configured");
        }
    }
}

public class NEucaGenerateGlobalUserDataTask extends OrcaAntTask {
    protected String file;
    protected String cloudType;
    protected String outputProperty;

    public void execute() throws BuildException {

        try {
            super.execute();
            if (file == null) {
                throw new Exception("Missing file parameter");
            }
            if (cloudType == null) {
                throw new Exception("Missing cloudType parameter");
            }
            PrintWriter out = new PrintWriter(new FileWriter(new File(file)));

            System.out.println("file: " + file + ", cloudType: " + cloudType);
            NEucaGenerateGlobalUserData generator = new NEucaGenerateGlobalUserData(getProject());

            generator.doIt(out);
            out.close();

            if (outputProperty != null)
                getProject().setProperty(outputProperty, generator.getOutputProperty());

        } catch (BuildException e) {
            throw e;
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setCloudtype(String cloudtype) {
        this.cloudType = cloudtype;
    }

    public void setOutputproperty(String outputproperty) {
        this.outputProperty = outputproperty;
    }
}
