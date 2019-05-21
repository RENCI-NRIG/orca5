package orca.handlers.ec2.tasks;

import orca.comet.NEucaCometDataGenerator;
import orca.comet.NEucaCometException;
import orca.shirako.common.meta.ConfigurationProperties;
import orca.shirako.common.meta.UnitProperties;

import java.util.*;

class NEucaGenerateCometData extends NEucaCometDataProcessor{

    public NEucaGenerateCometData(org.apache.tools.ant.Project project){
        super(project);
    }

    protected String sanitizeBootScript(String script) {
        if (script != null)
            return script.replaceAll("\n[\\s]*\n", "\n").replaceAll("\n", "\n\t");
        return script;
    }

    public void doIt() throws Exception {
        initCometDataGenerator();
        generateUsers();
        generateInterfaces();
        generateStorage();
        generateRoutes();
        generateScripts();
    }

    protected void generateUsers() throws Exception {
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

            cometDataGenerator.addUser(login, sudo, key);
        }
        if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.users, "")) {
            throw new NEucaCometException("Failed to save users in Comet");
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

    protected void generateInterfaces() throws Exception {
        System.out.println("Processing interfaces section");

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

            cometDataGenerator.addInterface(mac.replace(":", "") , state, ipVersion, ip, hosteth, vlanTag);

            // append iface to output property
            outputProperty += hosteth + "." + vlanTag + "." + mac + " ";

        }

        // Save meta data to comet if comet is enabled
        if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.interfaces, "")) {
            throw new NEucaCometException("Failed to save interfaces in Comet");
        }
    }

    protected void generateStorage() throws Exception {
        System.out.println("Processing storage section");

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

            cometDataGenerator.addStorage("dev" +i, store_type, target_ip, target_port, target_lun,
                    target_chap_user, target_chap_secret, target_should_attach,
                    fs_type, fs_options, fs_should_format, fs_mount_point);

        }

        // Save meta data to comet if comet is enabled
        if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.storage, "")) {
            throw new NEucaCometException("Failed to save storage in Comet");
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

    protected void generateRoutes() throws Exception {
        System.out.println("Processing routes section");

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

            // TODO: allow device and gateway to be configured too
            cometDataGenerator.addRoute(routeNetwork, routeNexthop, null, null);
        }

        // Save meta data to comet if comet is enabled
        if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.routes, "")) {
            throw new NEucaCometException("Failed to save routes in Comet");
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

    protected void generateScripts() throws Exception {
        System.out.println("Processing scripts section");

        String config = getProject().getProperty(UnitProperties.UnitInstanceConfig);
        if (config == null) {
            return;
        }

        System.out.println("BootScript: " + config);
        cometDataGenerator.addScript("bootscript", sanitizeBootScript(config));

        Integer[] scripts = getScripts();
        for (int i = 0; i < scripts.length; i++) {
            Integer scriptNum = scripts[i];
            String scriptName = UnitProperties.UnitScriptPrefix + scriptNum.toString();
            String scriptBody = getProject().getProperty(scriptName);

            if (scriptBody == null)
                continue;

            cometDataGenerator.addScript("script_" + scriptNum.toString(), sanitizeBootScript(scriptBody));
        }

        // Save meta data to comet if comet is enabled
        if(!cometDataGenerator.saveObject(NEucaCometDataGenerator.Family.scripts, "")) {
            throw new NEucaCometException("Failed to save scripts in Comet");
        }
    }
}
