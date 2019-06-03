package orca.handlers.ec2.tasks;

import orca.comet.NEucaCometDataGenerator;
import orca.comet.NEucaCometException;
import orca.shirako.common.meta.UnitProperties;
import orca.shirako.container.OrcaConfiguration;
import orca.shirako.plugins.config.Config;

public class NEucaCometDataProcessor {
    String outputProperty;
    org.apache.tools.ant.Project project;
    NEucaCometDataGenerator cometDataGenerator;

    public NEucaCometDataProcessor(org.apache.tools.ant.Project project) {
        this.project = project;
        this.outputProperty = "";

    }

    protected org.apache.tools.ant.Project getProject() {
        return project;
    }

    public String getOutputProperty() {
        return outputProperty;
    }

    public void initCometDataGenerator() throws orca.comet.NEucaCometException{
        // Create Comet Data Generator object if Comet is configured
        String cometHost = getProject().getProperty(OrcaConfiguration.CometHost);
        String caCert = getProject().getProperty(OrcaConfiguration.CometCaCert);
        String clientCertKeyStore = getProject().getProperty(OrcaConfiguration.CometClientKeyStore);
        String clientCertKeyStorePwd = getProject().getProperty(OrcaConfiguration.CometClientKeyStorePwd);
        String readToken = getProject().getProperty(Config.PropertySavePrefix + UnitProperties.UnitCometReadToken);
        String writeToken = getProject().getProperty(Config.PropertySavePrefix + UnitProperties.UnitCometWriteToken);
        String rId = getProject().getProperty(UnitProperties.UnitReservationID);
        String sliceId = getProject().getProperty(UnitProperties.UnitSliceID);

        // Save comethost and readToken in global section of Openstack meta data
        if (cometHost != null && caCert != null && clientCertKeyStore != null && clientCertKeyStorePwd != null) {
            // Instantiate cometDataGenerator
            cometDataGenerator = new NEucaCometDataGenerator(cometHost, caCert, clientCertKeyStore, clientCertKeyStorePwd,
                    rId, sliceId, readToken, writeToken);
        }
        else {
            throw new NEucaCometException("Comet is not configured");
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
    private void modifyUsers(String key, String value) throws NEucaCometException{
        System.out.println("NEucaCometDataProcessor::modifyUsers: IN value=" + value);
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.users, "");
        String [] arrOfStr = value.split(":");
        if(arrOfStr.length < 2) {
            System.out.println("NEucaCometDataProcessor::modifyUsers: Incorrect number of parameters");
            throw new NEucaCometException("NEucaCometDataProcessor::modifyUsers: Incorrect number of parameters");
        }
        if(cometDataGenerator.modifyUser(key, arrOfStr[0], arrOfStr[1])) {
            if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.users, "")) {
                throw new NEucaCometException("NEucaCometDataProcessor::modifyUsers: Unable to store users in comet");
            }
        }
        else {
            throw new NEucaCometException("NEucaCometDataProcessor::modifyUsers: Unable to add users in comet");
        }
        System.out.println("NEucaCometDataProcessor::modifyUsers: OUT");
    }

    /*
     * @brief function to update interfaces as part of modifySlice
     *
     * @param cometDataGenerator - Comet Data Generator
     *
     * @throws NEucaCometException in case of error
     *
     */
    private void modifyInterfaces(String key, String value) throws NEucaCometException{
        System.out.println("NEucaCometDataProcessor::modifyInterfaces: IN");
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.interfaces, "");
        cometDataGenerator.remove(NEucaCometDataGenerator.Family.interfaces, key);
        String [] arrOfStr = value.split(":");
        System.out.println("val: " + value);
        System.out.println("Len: " + arrOfStr.length);
        if(arrOfStr.length < 2) {
            System.out.println("NEucaCometDataProcessor::modifyInterfaces: Incorrect number of parameters");
            throw new NEucaCometException("NEucaCometDataProcessor::modifyInterfaces: Incorrect number of parameters");
        }
        boolean save = false;
        if(arrOfStr.length == 2) {
            save = cometDataGenerator.addInterface(key, arrOfStr[0], arrOfStr[1], null, null, null);
        }
        else if(arrOfStr.length == 3) {
            save = cometDataGenerator.addInterface(key, arrOfStr[0], arrOfStr[1], arrOfStr[2], null, null);
        }
        if(save) {
            save = cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.interfaces, "");
        }
        if(!save) {
            throw new NEucaCometException("NEucaCometDataProcessor::modifyInterfaces: Unable to store interfaces in comet");
        }
        System.out.println("NEucaCometDataProcessor::modifyInterfaces: OUT");
    }

    /*
     * @brief function to update storage as part of modifySlice
     *
     * @param cometDataGenerator - Comet Data Generator
     *
     * @throws NEucaCometException in case of error
     *
     */
    private void modifyStorage(String key, String value) throws NEucaCometException{
        System.out.println("NEucaCometDataProcessor::modifyStorage: IN");
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.storage, "");
        cometDataGenerator.remove(NEucaCometDataGenerator.Family.storage, key);
        String[] arrOfStr = value.split(":");
        if (arrOfStr.length < 3) {
            System.out.println("NEucaCometDataProcessor::modifyStorage: Incorrect number of parameters");
            throw new NEucaCometException("NEucaCometDataProcessor::modifyStorage: Incorrect number of parameters");
        }
        boolean save = false;
        if (arrOfStr.length == 3) {
            save = cometDataGenerator.addStorage(key, arrOfStr[0], arrOfStr[1], arrOfStr[2], null, null,
                    null, null, null, null, null,
                    null);
        } else if (arrOfStr.length == 4) {
            save = cometDataGenerator.addStorage(key, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], null,
                    null, null, null, null, null,
                    null);
        } else if (arrOfStr.length == 5) {
            save = cometDataGenerator.addStorage(key, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    null, null, null, null, null,
                    null);
        } else if (arrOfStr.length == 6) {
            save = cometDataGenerator.addStorage(key, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], null, null, null, null,
                    null);
        } else if (arrOfStr.length == 7) {
            save = cometDataGenerator.addStorage(key, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], null, null, null,
                    null);
        } else if (arrOfStr.length == 8) {
            save = cometDataGenerator.addStorage(key, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], arrOfStr[7], null, null,
                    null);
        } else if (arrOfStr.length == 9) {
            save = cometDataGenerator.addStorage(key, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], arrOfStr[7], arrOfStr[8], null,
                    null);
        } else if (arrOfStr.length == 10) {
            save = cometDataGenerator.addStorage(key, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], arrOfStr[7], arrOfStr[8], arrOfStr[9],
                    null);
        } else if (arrOfStr.length == 11) {
            save = cometDataGenerator.addStorage(key, arrOfStr[0], arrOfStr[1], arrOfStr[2], arrOfStr[3], arrOfStr[4],
                    arrOfStr[5], arrOfStr[6], arrOfStr[7], arrOfStr[8], arrOfStr[9],
                    arrOfStr[10]);
        }
        if (save) {
            save = cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.storage, "");
        }
        if(!save) {
            throw new NEucaCometException("NEucaCometDataProcessor::modifyStorage: Unable to store storage in comet");
        }
        System.out.println("NEucaCometDataProcessor::modifyStorage: OUT");
    }

    /*
     * @brief function to update routes as part of modifySlice
     *
     * @param cometDataGenerator - Comet Data Generator
     *
     * @throws NEucaCometException in case of error
     *
     */
    private void modifyRoutes(String key, String value) throws NEucaCometException{
        System.out.println("NEucaCometDataProcessor::modifyRoutes: IN");
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.routes, "");
        cometDataGenerator.remove(NEucaCometDataGenerator.Family.routes, key);
        String [] arrOfStr = value.split(":");
        if(arrOfStr.length < 1) {
            System.out.println("NEucaCometDataProcessor::modifyRoutes: Incorrect number of parameters");
            throw new NEucaCometException("NEucaCometDataProcessor::modifyRoutes: Incorrect number of parameters");
        }
        if(cometDataGenerator.addRoute(value, arrOfStr[0], null, null)) {
            if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.routes, "")) {
                throw new NEucaCometException("NEucaCometDataProcessor::modifyRoutes: Unable to store routes in comet");
            }
        }
        else {
            throw new NEucaCometException("NEucaCometDataProcessor::modifyUsers: Unable to add routes in comet");
        }
        System.out.println("NEucaCometDataProcessor::modifyRoutes: OUT");
    }

    /*
     * @brief function to update scripts as part of modifySlice
     *
     * @param cometDataGenerator - Comet Data Generator
     *
     * @throws NEucaCometException in case of error
     *
     */
    private void modifyScripts(String key, String value) throws NEucaCometException{
        System.out.println("NEucaCometDataProcessor::modifyScripts: IN");
        cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.scripts, "");
        cometDataGenerator.remove(NEucaCometDataGenerator.Family.scripts, key);
        String[] arrOfStr = value.split(":");
        if (arrOfStr.length < 1) {
            System.out.println("NEucaCometDataProcessor::modifyScripts: Incorrect number of parameters");
            throw new NEucaCometException("NEucaCometDataProcessor::modifyScripts: Incorrect number of parameters");
        }
        if (cometDataGenerator.addScript(value, arrOfStr[0])) {
            if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.scripts, "")){
                throw new NEucaCometException("NEucaCometDataProcessor::modifyScripts: Unable to store scripts in comet");
            }
        }
        else {
            throw new NEucaCometException("NEucaCometDataProcessor::modifyUsers: Unable to add scripts in comet");
        }
        System.out.println("NEucaCometDataProcessor::modifyScripts: OUT");
    }

    public void add(String section, String key, String value) throws Exception{
        initCometDataGenerator();
        if (NEucaCometDataGenerator.Family.users.toString().equals(section)) {
            modifyUsers(key,value);
        } else if (NEucaCometDataGenerator.Family.interfaces.toString().equals(section)) {
            modifyInterfaces(key, value);
        } else if (NEucaCometDataGenerator.Family.storage.toString().equals(section)) {
            modifyStorage(key, value);
        } else if (NEucaCometDataGenerator.Family.routes.toString().equals(section)) {
            modifyRoutes(key, value);
        } else if (NEucaCometDataGenerator.Family.scripts.toString().equals(section)) {
            modifyScripts(key, value);
        }
    }

    public void delete(String section, String key) throws Exception{
        initCometDataGenerator();
        // Update users
        if (NEucaCometDataGenerator.Family.users.toString().equals(section)) {
            System.out.println("NEucaCometDataProcessor::execute: removing users in");
            if (cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.users, "") &&
                    cometDataGenerator.remove(NEucaCometDataGenerator.Family.users, key)) {
                if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.users, "")){
                    throw new NEucaCometException("NEucaCometDataProcessor::execute: Unable to store users in comet");
                }
            }
            else {
                throw new NEucaCometException("NEucaCometDataProcessor::execute: Unable to store users in comet");
            }
            System.out.println("NEucaCometDataProcessor::execute: removing users out");
            //Update interfaces
        } else if (NEucaCometDataGenerator.Family.interfaces.toString().equals(section)) {
            System.out.println("NEucaCometDataProcessor::execute: removing interfaces in");
            if (cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.interfaces, "") &&
                    cometDataGenerator.remove(NEucaCometDataGenerator.Family.interfaces, key)) {
                if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.interfaces, "")){
                    throw new NEucaCometException("NEucaCometDataProcessor::execute: Unable to store interfaces in comet");
                }
            }
            else {
                throw new NEucaCometException("NEucaCometDataProcessor::execute: Unable to store interfaces in comet");
            }
            System.out.println("NEucaCometDataProcessor::execute: removing interfaces out");
            // Update Storage
        } else if (NEucaCometDataGenerator.Family.storage.toString().equals(section)) {
            System.out.println("NEucaCometDataProcessor::execute: removing storage in");
            if (cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.storage, "") &&
                    cometDataGenerator.remove(NEucaCometDataGenerator.Family.storage, key)) {
                if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.storage, "")){
                    throw new NEucaCometException("NEucaCometDataProcessor::execute: Unable to store storage in comet");
                }
            }
            else {
                throw new NEucaCometException("NEucaCometDataProcessor::execute: Unable to store storage in comet");
            }
            System.out.println("NEucaCometDataProcessor::execute: removing storage out");
            // Update routes
        } else if (NEucaCometDataGenerator.Family.routes.toString().equals(section)) {
            System.out.println("NEucaCometDataProcessor::execute: removing routes in");
            if (cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.routes, "") &&
                    cometDataGenerator.remove(NEucaCometDataGenerator.Family.routes, key)) {
                if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.routes, "")){
                    throw new NEucaCometException("NEucaCometDataProcessor::execute: Unable to store routes in comet");
                }
            }
            else {
                throw new NEucaCometException("NEucaCometDataProcessor::execute: Unable to store routes in comet");
            }
            System.out.println("NEucaCometDataProcessor::execute: removing routes out");
            // Update scripts
        } else if (NEucaCometDataGenerator.Family.scripts.toString().equals(section)) {
            System.out.println("NEucaCometDataProcessor::execute: removing scripts in");
            if (cometDataGenerator.loadObject(NEucaCometDataGenerator.Family.scripts, "") &&
                    cometDataGenerator.remove(NEucaCometDataGenerator.Family.scripts, key)) {
                if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.scripts, "")){
                    throw new NEucaCometException("NEucaCometDataProcessor::execute: Unable to store scripts in comet");
                }
            }
            else {
                throw new NEucaCometException("NEucaCometDataProcessor::execute: Unable to store scripts in comet");
            }
            System.out.println("NEucaCometDataProcessor::execute: removing scripts out");
        }
    }
}
