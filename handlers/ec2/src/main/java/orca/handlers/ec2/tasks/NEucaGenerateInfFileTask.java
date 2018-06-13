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
import orca.shirako.plugins.config.OrcaAntTask;

import org.apache.tools.ant.BuildException;

abstract class NEucaInfFileGenerator {
    org.apache.tools.ant.Project project;

    protected org.apache.tools.ant.Project getProject() {
        return project;
    }

    abstract public void doIt(PrintWriter out) throws Exception;

    abstract public String getOutputProperty();

    protected String sanitizeBootScript(String script) {
        if (script != null)
            return script.replaceAll("\n[\\s]*\n", "\n").replaceAll("\n", "\n\t");
        return script;
    }

}

class NEucaInfFileGenerator_v0 extends NEucaInfFileGenerator {

    public NEucaInfFileGenerator_v0(org.apache.tools.ant.Project project) {
        super();

        this.project = project;
    }

    public void doIt(PrintWriter out) throws Exception {
        generateGlobal(out);
        generateInterfaces(out);
        generateInstanceConfig(out);
    }

    public String getOutputProperty() {
        return "Null";
    }

    protected void generateGlobal(PrintWriter out) throws Exception {
        out.println("[global]");
        String temp = getProject().getProperty(UnitProperties.UnitActorID);
        if (temp != null) {
            out.println("actor_id=" + temp);
        }
        temp = getProject().getProperty(UnitProperties.UnitSliceID);
        if (temp != null) {
            out.println("slice_id=" + temp);
        }
        temp = getProject().getProperty(UnitProperties.UnitReservationID);
        if (temp != null) {
            out.println("reservation_id=" + temp);
        }
        temp = getProject().getProperty(UnitProperties.UnitID);
        if (temp != null) {
            out.println("unit_id=" + temp);
        }
        temp = getProject().getProperty(UnitProperties.UnitManagementIP);
        if (temp != null) {
            out.println("management_ip=" + temp);
        }
        temp = getProject().getProperty(UnitProperties.UnitEC2Host);
        if (temp != null) {
            out.println("host=" + temp);
        }
    }

    protected Integer[] getEths() {
        HashSet<Integer> set = new HashSet<Integer>();
        Hashtable<?, ?> h = project.getProperties();

        Iterator<?> i = h.entrySet().iterator();

        while (i.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
            String key = (String) entry.getKey();
            if (key.startsWith(UnitProperties.UnitEthPrefix)) {
                key = key.substring(UnitProperties.UnitEthPrefix.length());
                int index = key.indexOf('.');
                if (index > 0) {
                    key = key.substring(0, index);
                    Integer eth = new Integer(Integer.parseInt(key));
                    set.add(eth);
                }
            }
        }

        Integer[] list = new Integer[set.size()];

        int index = 0;
        for (Integer eth : set) {
            list[index++] = eth;
        }

        Arrays.sort(list);
        return list;
    }

    protected void generateInterfaces(PrintWriter out) throws Exception {
        Integer[] eths = getEths();

        if (eths.length == 0) {
            System.out.println("No interface-specific configuration specified. Checking for unit.vlan.tag");
            String vlan = getProject().getProperty(UnitProperties.UnitVlanTag);
            if (vlan == null) {
                System.out.println("No global unit.vlan.tag specified either");
            } else {
                System.out.println("Found unit.vlan.tag=" + vlan + ". Preparing configuration for eth1 only");
                String hosteth = getProject().getProperty(UnitProperties.UnitVlanHostEth);
                if (hosteth == null) {
                    System.out.println("No global unit.vlan.hosteth specified, skipping");
                    return;
                }
                out.println("[interfaces]");
                out.println("eth1=vlan:" + hosteth + ":" + vlan);
            }
            return;
        }

        out.println("[interfaces]");

        for (int i = 0; i < eths.length; i++) {
            Integer eth = eths[i];
            // see if this is a physical or a vlan attachment
            String mode = getProject()
                    .getProperty(UnitProperties.UnitEthPrefix + eth.toString() + UnitProperties.UnitEthModeSuffix);
            if (mode == null)
                mode = "vlan";

            // see what physical interface on the host we need to attach to (eth0 if unspecified)
            String hosteth = getProject()
                    .getProperty(UnitProperties.UnitEthPrefix + eth.toString() + UnitProperties.UnitHostEthSuffix);
            if (hosteth == null) {
                System.out.println("Eth" + eth.toString() + " is missing hosteth. Ignoring");
                continue;
            }

            String ip = getProject()
                    .getProperty(UnitProperties.UnitEthPrefix + eth.toString() + UnitProperties.UnitEthIPSuffix);
            if (ip == null) {
                System.out.println("Eth" + eth.toString() + " does not specify an IP.");
            }

            // attaching to physical interface
            if (mode.equals("phys")) {
                out.print("eth" + eth.toString() + "=phys:" + hosteth);

            } else {
                // attaching to vlan tag
                if (mode.equals("vlan")) {
                    String tag = getProject().getProperty(
                            UnitProperties.UnitEthPrefix + eth.toString() + UnitProperties.UnitEthVlanSuffix);
                    if (tag == null) {
                        System.out.println("Eth" + eth.toString() + " is missing vlan tag. Ignoring.");
                        continue;
                    }
                    if (ip != null) {
                        System.out.println("Configuring eth" + eth.toString() + " vlan=" + tag + ", ip=" + ip);
                    } else {
                        System.out.println("Configuring eth" + eth.toString() + " vlan=" + tag + ", ip=[no ip]");
                    }

                    out.print("eth" + eth.toString() + "=vlan:" + hosteth + ":" + tag);
                } else {
                    System.out.println("Eth" + eth.toString()
                            + " has invalid mode definition (neither 'phys', nor 'vlan'). Ignoring");
                    continue;
                }
            }
            if (ip != null) {
                out.print(":" + ip);
            }
            out.println();
        }
    }

    protected void generateInstanceConfig(PrintWriter out) throws Exception {
        System.out.println("Processing instanceConfig section");
        String config = getProject().getProperty(UnitProperties.UnitInstanceConfig);
        if (config == null) {
            return;
        }

        out.println("[instanceConfig]");
        out.println("script=" + sanitizeBootScript(config));
    }

}

class NEucaInfFileGenerator_v1 extends NEucaInfFileGenerator {
    String outputProperty;
    NEucaCometDataGenerator cometDataGenerator;

    public NEucaInfFileGenerator_v1(org.apache.tools.ant.Project project) {
        super();

        this.project = project;
        this.outputProperty = "";

    }

    public void doIt(PrintWriter out) throws Exception {

        generateGlobal(out);
        generateUsers(out);
        generateInterfaces(out);
        generateStorage(out);
        generateRoutes(out);
        generateScripts(out);

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

        temp = getProject().getProperty(UnitProperties.UnitReservationID);
        if (temp != null) {
            out.println("reservation_id=" + temp);
        } else {
            out.println(";reservation_id= Not Specified");
        }

        String unitId = null;
        temp = getProject().getProperty(UnitProperties.UnitID);
        if (temp != null) {
            out.println("unit_id=" + temp);
            unitId = temp;
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

        temp = getProject().getProperty(OrcaConfiguration.CometHost);
        if (temp != null) {
            out.println("comethost=" + temp);
            out.println("cometreadtoken=" + unitId + "-rid");
            cometDataGenerator = new NEucaCometDataGenerator(temp, unitId, sliceId);
        } else {
            out.println(";comethost= Not Specified");
        }
    }

    protected void generateUsers(PrintWriter out) throws Exception {
        out.println("[users]");

        StringTokenizer logins = new StringTokenizer(
                getProject().getProperty(ConfigurationProperties.ConfigSSHNumLogins), ",");
        while (logins.hasMoreElements()) {
            String user = logins.nextElement().toString();
            String login = getProject().getProperty(
                    ConfigurationProperties.ConfigSSHPrefix + user + ConfigurationProperties.ConfigSSHLoginSuffix);
            String key = getProject().getProperty(
                    ConfigurationProperties.ConfigSSHPrefix + user + ConfigurationProperties.ConfigSSHKeySuffix);
            // Replace the newlines that separate multiple keys with a colon
            if (key != null) {
                key = key.replaceAll("\n+", ":");
            }
            String sudo = getProject().getProperty(
                    ConfigurationProperties.ConfigSSHPrefix + user + ConfigurationProperties.ConfigSSHSudoSuffix);

            out.println(login + "=" + sudo + ":" + key);

            if(cometDataGenerator != null) {
                cometDataGenerator.addUser(login, sudo, key);
            }
        }
        if(cometDataGenerator != null) {
            cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.users);
        }
    }

    protected Integer[] getStores() {
        HashSet<Integer> set = new HashSet<Integer>();
        Hashtable<?, ?> h = project.getProperties();

        Iterator<?> i = h.entrySet().iterator();

        while (i.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
            String key = (String) entry.getKey();
            if (key.startsWith(UnitProperties.UnitStoragePrefix)) {
                key = key.substring(UnitProperties.UnitStoragePrefix.length());
                int index = key.indexOf('.');
                if (index > 0) {
                    key = key.substring(0, index);
                    Integer store = new Integer(Integer.parseInt(key));
                    set.add(store);
                }
            }
        }

        Integer[] list = new Integer[set.size()];

        int index = 0;
        for (Integer store : set) {
            list[index++] = store;
        }

        Arrays.sort(list);
        return list;
    }

    protected Integer[] getEths() {
        HashSet<Integer> set = new HashSet<Integer>();
        Hashtable<?, ?> h = project.getProperties();

        Iterator<?> i = h.entrySet().iterator();

        while (i.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
            String key = (String) entry.getKey();
            if (key.startsWith(UnitProperties.UnitEthPrefix)) {
                key = key.substring(UnitProperties.UnitEthPrefix.length());
                int index = key.indexOf('.');
                if (index > 0) {
                    key = key.substring(0, index);
                    Integer eth = new Integer(Integer.parseInt(key));
                    set.add(eth);
                }
            }
        }

        Integer[] list = new Integer[set.size()];

        int index = 0;
        for (Integer eth : set) {
            list[index++] = eth;
        }

        Arrays.sort(list);
        return list;
    }

    protected void generateInterfaces(PrintWriter out) throws Exception {
        System.out.println("Processing interfaces section");
        out.println("[interfaces]");

        Integer[] eths = getEths();

        for (int i = 0; i < eths.length; i++) {
            Integer eth = eths[i];

            // see what physical interface on the host we need to attach to
            String hosteth = getProject()
                    .getProperty(UnitProperties.UnitEthPrefix + eth.toString() + UnitProperties.UnitHostEthSuffix);
            if (hosteth == null) {
                System.out.println("Eth" + eth.toString() + " is missing hosteth. Ignoring");
                continue;
            }

            String ip = getProject()
                    .getProperty(UnitProperties.UnitEthPrefix + eth.toString() + UnitProperties.UnitEthIPSuffix);
            if (ip == null) {
                System.out.println("Eth" + eth.toString() + " does not specify an IP.");
            }

            String mac = getProject()
                    .getProperty(UnitProperties.UnitEthPrefix + eth.toString() + UnitProperties.UnitEthMacSuffix);
            if (mac == null) {
                System.out.println("Eth" + eth.toString() + " does not specify a MAC.");
                continue;
            }

            String state = getProject()
                    .getProperty(UnitProperties.UnitEthPrefix + eth.toString() + UnitProperties.UnitEthStateSuffix);
            if (state == null) {
                System.out.println("Eth" + eth.toString() + " does not specify a state.");
                state = "up";
            }

            String ipVersion = getProject()
                    .getProperty(UnitProperties.UnitEthPrefix + eth.toString() + UnitProperties.UnitEthIPVersionSuffix);
            if (ipVersion == null) {
                System.out.println("Eth" + eth.toString() + " does not specify an ip version.");
                ipVersion = "ipv4";
            }

            String vlanTag = getProject()
                    .getProperty(UnitProperties.UnitEthPrefix + eth.toString() + UnitProperties.UnitEthVlanSuffix);
            if (vlanTag == null) {
                System.out.println("Eth" + eth.toString() + " does not specify a vlan tag.");
                continue;
            }

            out.print(mac.replace(":", "") + "=" + state + ":" + ipVersion);

            // attaching to vlan tag
            if (ip != null) {
                out.print(":" + ip);
            }
            out.println();
            if(cometDataGenerator != null) {
                cometDataGenerator.addInterface(mac.replace(":", "") , state, ipVersion, ip, hosteth, vlanTag);
            }

            // append iface to output property
            outputProperty += hosteth + "." + vlanTag + "." + mac + " ";

        }
        if(cometDataGenerator != null && eths.length > 0) {
            cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.interfaces);
        }
    }

    protected void generateStorage(PrintWriter out) throws Exception {
        System.out.println("Processing storage section");
        out.println("[storage]");

        Integer[] stores = getStores();

        for (int i = 0; i < stores.length; i++) {
            Integer store = stores[i];

            String store_type = getProject().getProperty(
                    UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitStoreTypeSuffix);
            if (store_type == null) {
                System.out.println("Store " + store.toString() + " is missing store type. Ignoring");
                continue;
            }

            // for now we only know about iscsi
            if (store_type.compareTo("iscsi") != 0) {
                System.out.println("Unknown storage type " + store_type + " for " + store.toString() + ".  Ignoring");
                continue;
            }

            String target_ip = getProject().getProperty(
                    UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitTargetIPSuffix);
            if (target_ip == null) {
                System.out.println("Store " + store.toString() + " does not specify a target IP. Ignoring");
                continue;
            }

            String target_port = getProject().getProperty(
                    UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitTargetPortSuffix);
            if (target_port == null) {
                System.out.println("Store " + store.toString() + " does not specify a target port. Ignoring");
                continue;
            }

            String target_lun = getProject().getProperty(
                    UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitTargetLunSuffix);
            if (target_lun == null) {
                System.out.println("Store " + store.toString() + " does not specify a target lun. Assuming 0");
                target_lun = "0";
            }

            String target_should_attach = getProject().getProperty(
                    UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitTargetShouldAttachSuffix);
            if (target_should_attach == null) {
                System.out.println(
                        "Store " + store.toString() + " does not specify if it should be attached. Assuming no.");
                target_should_attach = "no";
            }

            String target_chap_user = getProject().getProperty(
                    UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitTargetChapUserSuffix);
            if (target_chap_user == null) {
                System.out.println(
                        "Store " + store.toString() + " does not specify chap user name. Assuming empty string.");
                target_chap_user = "";
            }

            String target_chap_secret = getProject().getProperty(
                    UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitTargetChapSecretSuffix);
            if (target_chap_secret == null) {
                System.out
                        .println("Store " + store.toString() + " does not specify chap secret. Assuming empty string.");
                target_chap_secret = "";
            }

            String fs_type = getProject()
                    .getProperty(UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitFSTypeSuffix);
            if (fs_type == null) {
                System.out.println(
                        "Store " + store.toString() + " does not specify file system type. Assuming empty string.");
                fs_type = "";
            }

            String fs_options = getProject().getProperty(
                    UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitFSOptionsSuffix);
            if (fs_options == null) {
                System.out.println(
                        "Store " + store.toString() + " does not specify file system options. Assuming empty string.");
                fs_options = "";
            }

            String fs_should_format = getProject().getProperty(
                    UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitFSShouldFormatSuffix);
            if (fs_should_format == null) {
                System.out.println("Store " + store.toString()
                        + " does not specify if the file system should be formatted. Assuming no.");
                fs_should_format = "no";
            }

            String fs_mount_point = getProject().getProperty(
                    UnitProperties.UnitStoragePrefix + store.toString() + UnitProperties.UnitFSMountPointSuffix);
            if (fs_mount_point == null) {
                System.out.println("Store " + store.toString()
                        + " does not specify file system mount point. Assuming empty string (i.e. file system will not be mounted).");
                fs_mount_point = "";
            }

            out.print("dev" + i + "=");
            out.print(store_type);
            out.print(":" + target_ip);
            out.print(":" + target_port);
            out.print(":" + target_lun);
            out.print(":" + target_chap_user);
            out.print(":" + target_chap_secret);
            out.print(":" + target_should_attach);
            out.print(":" + fs_type);
            out.print(":" + fs_options);
            out.print(":" + fs_should_format);
            out.print(":" + fs_mount_point);

            out.println();
            if(cometDataGenerator != null) {
                cometDataGenerator.addStorage("dev" +i, store_type, target_ip, target_port, target_lun,
                                              target_chap_user, target_chap_secret, target_should_attach,
                                              fs_type, fs_options, fs_should_format, fs_mount_point);
            }

        }
        if(cometDataGenerator != null && stores.length > 0) {
            cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.storages);
        }
    }

    protected Integer[] getRoutes() {
        HashSet<Integer> set = new HashSet<Integer>();
        Hashtable<?, ?> h = project.getProperties();

        Iterator<?> i = h.entrySet().iterator();

        while (i.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
            String key = (String) entry.getKey();
            if (key.startsWith(UnitProperties.UnitRoutePrefix)) {
                key = key.substring(UnitProperties.UnitRoutePrefix.length());
                int index = key.indexOf('.');
                if (index > 0) {
                    key = key.substring(0, index);
                    Integer route = new Integer(Integer.parseInt(key));
                    set.add(route);
                }
            }
        }

        Integer[] list = new Integer[set.size()];

        int index = 0;
        for (Integer route : set) {
            list[index++] = route;
        }

        Arrays.sort(list);
        return list;
    }

    protected void generateRoutes(PrintWriter out) throws Exception {
        System.out.println("Processing routes section");
        out.println("[routes]");

        Integer[] routes = getRoutes();
        for (int i = 0; i < routes.length; i++) {
            Integer routeNum = routes[i];
            String routeName = UnitProperties.UnitRoutePrefix + routeNum.toString();
            String routeNetwork = getProject().getProperty(routeName + UnitProperties.UnitRouteNetworkSuffix);
            if (routeNetwork == null)
                continue;
            String routeNexthop = getProject().getProperty(routeName + UnitProperties.UnitRouteNexthopSuffix);
            if (routeNexthop == null)
                continue;

            out.print(routeNetwork.toString() + "=" + routeNexthop);
            out.print("\n\n");

            if(cometDataGenerator != null) {
                cometDataGenerator.addRoute(routeNetwork, routeNexthop);
            }
        }
        if(cometDataGenerator != null && routes.length > 0) {
            cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.routes);
        }
    }

    protected Integer[] getScripts() {
        HashSet<Integer> set = new HashSet<Integer>();
        Hashtable<?, ?> h = project.getProperties();

        Iterator<?> i = h.entrySet().iterator();

        while (i.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
            String key = (String) entry.getKey();
            if (key.startsWith(UnitProperties.UnitScriptPrefix)) {
                key = key.substring(UnitProperties.UnitScriptPrefix.length());
                int index = key.indexOf('.');
                if (index > 0) {
                    key = key.substring(0, index);
                }
                Integer script = new Integer(Integer.parseInt(key));
                set.add(script);
            }
        }

        Integer[] list = new Integer[set.size()];

        int index = 0;
        for (Integer script : set) {
            list[index++] = script;
        }

        Arrays.sort(list);
        return list;
    }

    protected void generateScripts(PrintWriter out) throws Exception {
        System.out.println("Processing scripts section");
        out.println("[scripts]");

        String config = getProject().getProperty(UnitProperties.UnitInstanceConfig);
        if (config == null) {
            return;
        }

        System.out.println("BootScript: " + config);
        out.print("bootscript=" + sanitizeBootScript(config));
        out.print("\n\n");

        if(cometDataGenerator != null) {
            cometDataGenerator.addScript("bootscript", sanitizeBootScript(config));
        }

        Integer[] scripts = getScripts();
        for (int i = 0; i < scripts.length; i++) {
            Integer scriptNum = scripts[i];
            String scriptName = UnitProperties.UnitScriptPrefix + scriptNum.toString();
            String scriptBody = getProject().getProperty(scriptName);

            if (scriptBody == null)
                continue;

            out.print("script_" + scriptNum.toString() + "=" + sanitizeBootScript(scriptBody));
            out.print("\n\n");

            if(cometDataGenerator != null) {
                cometDataGenerator.addScript("script_" + scriptNum.toString(), sanitizeBootScript(scriptBody));
            }
        }

        if(cometDataGenerator != null && (config != null || scripts.length > 0)) {
            cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.scripts);
        }
    }

}

public class NEucaGenerateInfFileTask extends OrcaAntTask {
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

            System.out.println("PRUTH-TEST: numlogins:        " + getProject().getProperty("config.ssh.numlogins"));
            StringTokenizer logins = new StringTokenizer(getProject().getProperty("config.ssh.numlogins"), ",");
            while (logins.hasMoreElements()) {
                String user = "user" + logins.nextElement().toString();
                System.out.println("PRUTH-TEST: " + user + ":        "
                        + getProject().getProperty("config.ssh." + user + ".login"));
                System.out.println(
                        "PRUTH-TEST: " + user + ":        " + getProject().getProperty("config.ssh." + user + ".keys"));
                System.out.println(
                        "PRUTH-TEST: " + user + ":        " + getProject().getProperty("config.ssh." + user + ".sudo"));
            }
            System.out.println("PRUTH-TEST: management ip: " + getProject().getProperty("shirako.save.unit.manage.ip"));
            System.out.println("PRUTH-TEST: host:          " + getProject().getProperty("shirako.save.unit.ec2.host"));

            NEucaInfFileGenerator generator;

            // if (cloudType.compareTo("nova-essex") == 0){
            //// Quantum Neuca Plugin v1.x
            // generator = new NEucaInfFileGenerator_v1(getProject());
            // } else {
            //// Original Neuca v0.x
            // generator = new NEucaInfFileGenerator_v0(getProject());
            // }

            // Currently all types use rack type NEucaInfFileGenerator_v1
            // No current need for above code.
            generator = new NEucaInfFileGenerator_v1(getProject());

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
