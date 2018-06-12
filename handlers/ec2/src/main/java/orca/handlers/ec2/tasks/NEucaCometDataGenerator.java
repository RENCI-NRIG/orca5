package orca.handlers.ec2.tasks;

import org.json.simple.*;



import java.util.Iterator;

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

    // Script fields
    public final static String JsonKeyScriptName = "scriptName";
    public final static String JsonKeyScriptBody = "scriptBody";

    // Types
    enum Family {
        Users,
        Interfaces,
        Storages,
        Routes,
        Scripts
    };

    // DATA
    private JSONArray users_;
    private JSONArray interfaces_;
    private JSONArray storages_;
    private JSONArray routes_;
    private JSONArray scripts_;
    private String unitId_;
    private String sliceId_;

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
    }
    public NEucaCometDataGenerator(String cometHost, String unitId, String sliceId)  {
        users_ = null;
        interfaces_ = null;
        storages_ = null;
        routes_ = null;
        scripts_ = null;
        unitId_ = unitId;
        sliceId_ = sliceId;
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
                JSONArray value = comet_.read(sliceId_, unitId_, unitId_+"-rid", family.toString());
                if(value != null) {
                    switch (family) {
                        case Users:
                            users_ = value;
                            break;
                        case Interfaces:
                            interfaces_ = value;
                            break;
                        case Storages:
                            storages_ = value;
                            break;
                        case Routes:
                            routes_ = value;
                            break;
                        case Scripts:
                            scripts_ = value;
                            break;
                    }
                    return true;
                }
            }
        }
        catch (Exception e) {
            System.out.println("Exception occured while loadingObject: " + e.getStackTrace());
        }
        return false;
    }

    public String getObject(Family family) {
        try {
            switch (family) {
                case Users:
                    if (users_ != null) {
                        return users_.toString();
                    }
                    break;
                case Interfaces:
                    if (interfaces_ != null) {
                        return interfaces_.toString();
                    }
                    break;
                case Storages:
                    if (storages_ != null) {
                        return storages_.toString();
                    }
                    break;
                case Routes:
                    if (routes_ != null) {
                        return routes_.toString();
                    }
                    break;
                case Scripts:
                    if (scripts_ != null) {
                        return scripts_.toString();
                    }
                    break;
            }
        }
        catch (Exception e) {
            System.out.println("Exception occured while getObject: " + e.getStackTrace());
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
                if(value != null) {
                    System.out.println("Saving family: " + family + " value: " + value);
                    return comet_.write(sliceId_, unitId_, unitId_ + "-rid", unitId_ + "-wid", family.toString(), value);
                }
            }
        }
        catch (Exception e) {
            System.out.println("Exception occured while saveObject: " + e.getStackTrace());
        }
        return false;
    }

    public void addUser(String user, String sudo, String key) {
        try {
            if(user == null|| sudo == null || key == null) {
                System.out.println("Missing mandatory user parameters!");
                return;
            }
            if (users_ == null) {
                users_ = new JSONArray();
            }
            JSONObject u = new JSONObject();
            u.put(JsonKeyUserName, user);
            u.put(JsonKeyUserSudo, sudo);
            u.put(JsonKeyUserKey, key);
            users_.add(u);
        }
        catch(Exception e) {
            System.out.println("Exception occured while addUser: " + e.getMessage() + " " + e.getStackTrace().toString());
        }
    }

    public void addInterface(String mac, String state, String ipVersion, String ip, String hosteth, String vlanTag) {
        try {
            if (mac == null || state == null || ipVersion == null || hosteth == null) {
                System.out.println("Missing mandatory interface parameters!");
                return;
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
            interfaces_.add(i);
        }
        catch(Exception e) {
                System.out.println("Exception occured while addInterface: " + e.getStackTrace());
        }

    }

    public void addStorage(String device, String storageType, String targetIp, String targetPort, String targetLun,
                           String targetChapUser, String targetChapSecret, String targetShouldAttach,
                           String fsType, String fsOptions, String fsShouldFormat, String fsMountPoint) {
        try {
            if (device == null || storageType == null || targetIp == null || targetPort == null) {
                System.out.println("Missing mandatory storage parameters!");
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

            storages_.add(storage);
        }
        catch (Exception e) {
                System.out.println("Exception occured while addStorage: " + e.getStackTrace());
        }
    }

    public void addRoute(String routeNetwork, String routeNextHop) {
        try {
            if (routeNetwork == null || routeNextHop == null) {
                System.out.println("Missing mandatory route parameters!");
            }
            if (routes_ == null) {
                routes_ = new JSONArray();
            }
            JSONObject route = new JSONObject();
            route.put(JsonKeyRouteNetwork, routeNetwork);
            route.put(JsonKeyRouteNextHop, routeNextHop);
            routes_.add(route);
        }
        catch (Exception e) {
            System.out.println("Exception occured while addRoute: " + e.getStackTrace());
        }
    }

    public void addScript(String scriptName, String scriptBody) {
        try {
            if (scriptBody == null || scriptName == null) {
                if(scripts_ == null) {
                    scripts_ = new JSONArray();
                }
                System.out.println("Missing mandatory script parameters!");
                JSONObject script = new JSONObject();
                script.put(JsonKeyScriptName, scriptName);
                script.put(JsonKeyScriptBody, scriptBody);
                scripts_.add(script);
            }
        }
        catch (Exception e) {
            System.out.println("Exception occured while addScript: " + e.getStackTrace());
        }
    }

    public void remove(Family family, String key) {
        try {
            if(key == null) {
                System.out.println("Missing mandatory key parameters!");
                return;
            }
            JSONArray familyToBeUpdated = null;
            switch (family) {
                case Users:
                    familyToBeUpdated = users_;
                    break;
                case Interfaces:
                    familyToBeUpdated = interfaces_;
                    break;
                case Storages:
                    familyToBeUpdated = storages_;
                    break;
                case Routes:
                    familyToBeUpdated = routes_;
                    break;
                case Scripts:
                    familyToBeUpdated = storages_;
                    break;
            }
            if (familyToBeUpdated == null) {
                System.out.println("familyToBeUpdated array not loaded");
                return;
            }

            for(int i = 0; i < familyToBeUpdated.size(); ++i) {
                JSONObject u = (JSONObject)familyToBeUpdated.get(i);
                if(u.containsValue(key)) {
                    System.out.println("Removed object with key " + key + " from " + family.toString());
                    familyToBeUpdated.remove(i);
                    break;
                }
            }
        }
        catch(Exception e) {
            System.out.println("Exception occured while removeUser: " + e.getMessage() + " " + e.getStackTrace().toString());
        }
    }

}
