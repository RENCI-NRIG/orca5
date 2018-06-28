package orca.handlers.ec2.tasks;

import org.json.simple.*;

public class NEucaCometDataGenerator {
    // Statics
    // User fields
    public final static String JsonKeyUserName = "user";
    public final static String JsonKeyUserSudo = "sudo";
    public final static String JsonKeyUserKey = "key";

    // Interface fields
    public final static String JsonKeyInterfaceMac = "mac";
    public final static String JsonKeyInterfaceState = "state";
    public final static String JsonKeyInterfaceIpVersion = "ipVersion";
    public final static String JsonKeyInterfaceIp = "ip";
    public final static String JsonKeyInterfaceHostEth = "hosteth";
    public final static String JsonKeyInterfaceVlanTag = "vlanTag";

    // Storage fields
    public final static String JsonKeyStorageDevice = "device";
    public final static String JsonKeyStorageType = "storageType";
    public final static String JsonKeyStorageTargetIp = "targetIp";
    public final static String JsonKeyStorageTargetPort = "targetPort";
    public final static String JsonKeyStorageTargetLun = "targetLun";
    public final static String JsonKeyStorageTargetChapUser = "targetChapUser";
    public final static String JsonKeyStorageTargetChapSecret = "targetChapSecret";
    public final static String JsonKeyStorageTargetShouldAttach = "targetShouldAttach";
    public final static String JsonKeyStorageFsType = "fsType";
    public final static String JsonKeyStorageFsOptions = "fsOptions";
    public final static String JsonKeyStorageFsShouldFormat = "fsShouldFormat";
    public final static String JsonKeyStorageFsMountPoint = "fsMountPoint";

    // Route fields
    public final static String JsonKeyRouteNetwork = "routeNetwork";
    public final static String JsonKeyRouteNextHop = "routeNextHop";
    public final static String JsonKeyRouteDevice = "device";
    public final static String JsonKeyRouteGateway = "gateway";

    // Script fields
    public final static String JsonKeyScriptName = "scriptName";
    public final static String JsonKeyScriptBody = "scriptBody";

    // Types
    public enum Family {
        users,
        interfaces,
        storages,
        routes,
        scripts
    };

    // DATA
    private JSONArray users_;
    private JSONArray interfaces_;
    private JSONArray storages_;
    private JSONArray routes_;
    private JSONArray scripts_;
    private String unitId_;
    private String sliceId_;
    private String readToken_;
    private String writeToken_;

    private NEucaCometInterface comet_;

    // Constructors
    public NEucaCometDataGenerator() {
        users_ = null;
        interfaces_ = null;
        storages_ = null;
        routes_ = null;
        scripts_ = null;
        unitId_ = null;
        sliceId_ = null;
        comet_ = null;
        readToken_ = null;
        writeToken_ = null;
    }
    public NEucaCometDataGenerator(String cometHost, String unitId, String sliceId, String readToken, String writeToken)  {
        users_ = null;
        interfaces_ = null;
        storages_ = null;
        routes_ = null;
        scripts_ = null;
        unitId_ = unitId;
        sliceId_ = sliceId;
        readToken_ = readToken;
        writeToken_ = writeToken;
        comet_ = new NEucaCometInterface(cometHost);
        comet_.setSslCaCert();
    }
    public boolean loadObject(Family family) {
        try {
            if (unitId_ != null && sliceId_ != null) {
                // Send Rest Request to get
                // ContextId = sliceId_
                // Family = family.toString()
                // Key = unitId_
                // readToken = unitId_ + "-rid"
                JSONArray value = comet_.read(sliceId_, unitId_, readToken_, family.toString());
                if(value != null) {
                    switch (family) {
                        case users:
                            users_ = value;
                            break;
                        case interfaces:
                            interfaces_ = value;
                            break;
                        case storages:
                            storages_ = value;
                            break;
                        case routes:
                            routes_ = value;
                            break;
                        case scripts:
                            scripts_ = value;
                            break;
                    }
                    return true;
                }
            }
        }
        catch (Exception e) {
            System.out.println("NEucaCometDataGenerator::loadObject: Exception occurred while loadingObject: " + e.getStackTrace());
        }
        return false;
    }

    public String getObject(Family family) {
        try {
            switch (family) {
                case users:
                    if (users_ != null && users_.size() > 0) {
                        return users_.toString();
                    }
                    break;
                case interfaces:
                    if (interfaces_ != null && interfaces_.size() > 0) {
                        return interfaces_.toString();
                    }
                    break;
                case storages:
                    if (storages_ != null && storages_.size() > 0) {
                        return storages_.toString();
                    }
                    break;
                case routes:
                    if (routes_ != null && routes_.size() > 0) {
                        return routes_.toString();
                    }
                    break;
                case scripts:
                    if (scripts_ != null && scripts_.size() > 0) {
                        return scripts_.toString();
                    }
                    break;
            }
        }
        catch (Exception e) {
            System.out.println("NEucaCometDataGenerator::getObject: Exception occurred while getObject: " + e.getStackTrace());
        }
        return null;
    }

    public boolean saveObject(Family family) {
        try {
            if (unitId_ != null && sliceId_ != null) {
                // Send Rest Request to get
                // ContextId = sliceId_
                // Family = family.toString()
                // Key = unitId_
                // readToken = unitId_ + "-rid"
                // writeToken = unitId_ + "-wid"
                String value = getObject(family);
                if(value != null && !value.isEmpty()) {
                    System.out.println("NEucaCometDataGenerator::saveObject: Saving family: " + family + " value: " + value);
                    return comet_.write(sliceId_, unitId_, readToken_, writeToken_, family.toString(), value);
                }
                else {
                    System.out.println("NEucaCometDataGenerator::saveObject: Removing family: " + family);
                    return comet_.remove(sliceId_, unitId_, readToken_, writeToken_, family.toString());
                }
            }
        }
        catch (Exception e) {
            System.out.println("NEucaCometDataGenerator::saveObject: Exception occurred while saveObject: " + e.getStackTrace());
        }
        return false;
    }

    public boolean addUser(String user, String sudo, String key) {
        boolean retVal = false;
        try {
            if(user == null|| sudo == null || key == null) {
                System.out.println("NEucaCometDataGenerator::addUser: Missing mandatory user parameters!");
                return retVal;
            }
            if (users_ == null) {
                users_ = new JSONArray();
            }
            JSONObject u = new JSONObject();
            u.put(JsonKeyUserName, user);
            u.put(JsonKeyUserSudo, sudo);
            u.put(JsonKeyUserKey, key);
            retVal = users_.add(u);
        }
        catch(Exception e) {
            System.out.println("NEucaCometDataGenerator::addUser: Exception occurred while addUser: " + e.getMessage() + " " + e.getStackTrace().toString());
        }
        return retVal;
    }

    public boolean addInterface(String mac, String state, String ipVersion, String ip, String hosteth, String vlanTag) {
        boolean retVal = false;
        try {
            if (mac == null || state == null || ipVersion == null) {
                System.out.println("NEucaCometDataGenerator::addInterface: Missing mandatory interface parameters!");
                return retVal;
            }
            if (interfaces_ == null) {
                interfaces_ = new JSONArray();
            }
            JSONObject i = new JSONObject();

            i.put(JsonKeyInterfaceMac, mac);
            i.put(JsonKeyInterfaceState, state);
            i.put(JsonKeyInterfaceIpVersion, ipVersion);
            i.put(JsonKeyInterfaceHostEth, hosteth);
            if (ip != null) {
                i.put(JsonKeyInterfaceIp, ip);
            }
            if (vlanTag != null) {
                i.put(JsonKeyInterfaceVlanTag, vlanTag);
            }
            retVal = interfaces_.add(i);

        }
        catch(Exception e) {
                System.out.println("NEucaCometDataGenerator::addInterface: Exception occurred while addInterface: " + e.getStackTrace());
        }
        return retVal;
    }

    public boolean addStorage(String device, String storageType, String targetIp, String targetPort, String targetLun,
                           String targetChapUser, String targetChapSecret, String targetShouldAttach,
                           String fsType, String fsOptions, String fsShouldFormat, String fsMountPoint) {
        boolean retVal = false;
        try {
            if (device == null || storageType == null || targetIp == null || targetPort == null) {
                System.out.println("NEucaCometDataGenerator::addStorage: Missing mandatory storage parameters!");
                return retVal;
            }
            if (storages_ == null) {
                storages_ = new JSONArray();
            }
            JSONObject storage = new JSONObject();
            storage.put(JsonKeyStorageDevice, device);
            storage.put(JsonKeyStorageType, storageType);
            storage.put(JsonKeyStorageTargetIp, targetIp);
            storage.put(JsonKeyStorageTargetPort, targetPort);
            if (targetLun != null) {
                storage.put(JsonKeyStorageTargetLun, targetLun);
            }
            if (targetChapUser != null) {
                storage.put(JsonKeyStorageTargetChapUser, targetChapUser);
            }
            if (targetChapSecret != null) {
                storage.put(JsonKeyStorageTargetChapSecret, targetChapSecret);
            }
            if (targetShouldAttach != null) {
                storage.put(JsonKeyStorageTargetShouldAttach, targetShouldAttach);
            }
            if (fsType != null) {
                storage.put(JsonKeyStorageFsType, fsType);
            }
            if (fsOptions != null) {
                storage.put(JsonKeyStorageFsOptions, fsOptions);
            }
            if (fsShouldFormat != null) {
                storage.put(JsonKeyStorageFsShouldFormat, fsShouldFormat);
            }
            if (fsMountPoint != null) {
                storage.put(JsonKeyStorageFsMountPoint, fsMountPoint);
            }

            retVal = storages_.add(storage);
        }
        catch (Exception e) {
                System.out.println("NEucaCometDataGenerator::addStorage: Exception occurred while addStorage: " + e.getStackTrace());
        }
        return retVal;
    }

    public boolean addRoute(String routeNetwork, String routeNextHop, String device, String gateway) {
        boolean retVal = false;
        try {
            if (routeNetwork == null || routeNextHop == null) {
                System.out.println("NEucaCometDataGenerator::addRoute: Missing mandatory route parameters!");
                return retVal;
            }
            if (routes_ == null) {
                routes_ = new JSONArray();
            }
            JSONObject route = new JSONObject();
            route.put(JsonKeyRouteNetwork, routeNetwork);
            route.put(JsonKeyRouteNextHop, routeNextHop);
            if(device != null) {
                route.put(JsonKeyRouteDevice, device);
            }
            if(gateway != null) {
                route.put(JsonKeyRouteGateway, gateway);
            }
            retVal= routes_.add(route);
        }
        catch (Exception e) {
            System.out.println("NEucaCometDataGenerator::addRoute: Exception occurred while addRoute: " + e.getStackTrace());
        }
        return retVal;
    }

    public boolean addScript(String scriptName, String scriptBody) {
        boolean retVal = false;
        try {
            if (scriptBody == null || scriptName == null) {
                System.out.println("NEucaCometDataGenerator::addScript: Missing mandatory script parameters!");
                return retVal;
            }
            if (scripts_ == null) {
                scripts_ = new JSONArray();
            }

            JSONObject script = new JSONObject();
            script.put(JsonKeyScriptName, scriptName);
            script.put(JsonKeyScriptBody, scriptBody);
            retVal = scripts_.add(script);
        }
        catch (Exception e) {
            System.out.println("NEucaCometDataGenerator::addScript: Exception occurred while addScript: " + e.getStackTrace());
        }
        return retVal;
    }

    public boolean remove(Family family, String key) {
        boolean retVal = false;
        try {
            if(key == null) {
                System.out.println("NEucaCometDataGenerator::remove: Missing mandatory key parameters!");
                return retVal;
            }
            JSONArray familyToBeUpdated = null;
            switch (family) {
                case users:
                    familyToBeUpdated = users_;
                    break;
                case interfaces:
                    familyToBeUpdated = interfaces_;
                    break;
                case storages:
                    familyToBeUpdated = storages_;
                    break;
                case routes:
                    familyToBeUpdated = routes_;
                    break;
                case scripts:
                    familyToBeUpdated = storages_;
                    break;
            }
            if (familyToBeUpdated == null) {
                System.out.println("NEucaCometDataGenerator::remove: familyToBeUpdated array not loaded");
                return retVal;
            }

            for(int i = 0; i < familyToBeUpdated.size(); ++i) {
                JSONObject u = (JSONObject)familyToBeUpdated.get(i);
                if(u.containsValue(key)) {
                    System.out.println("NEucaCometDataGenerator::remove: Removed object with key " + key + " from " + family.toString());
                    familyToBeUpdated.remove(i);
                    retVal = true;
                    break;
                }
            }
        }
        catch(Exception e) {
            System.out.println("NEucaCometDataGenerator::remove: Exception occurred while removeUser: " + e.getMessage() + " " + e.getStackTrace().toString());
        }
        return retVal;
    }

}
