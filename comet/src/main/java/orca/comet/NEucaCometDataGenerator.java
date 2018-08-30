/*
 *
 * @class
 *
 * @brief This class implements an interface to generate Instance Meta Data JSON based on input and save
 * the generated JSON in Comet
 *
 *
 */
package orca.comet;

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

    // Hostname fields
    public final static String JsonKeyHostName = "hostName";
    public final static String JsonKeyIp = "ip";

    // Key fields
    public final static String JsonKeyPublicKey = "publicKey";
    public final static String JsonKeyPrivateKey = "privateKey";

    // Types
    public enum Family {
        users,
        interfaces,
        storage,
        routes,
        scripts,
        hosts,
        pubkeys
    };

    // DATA
    private JSONArray users_;
    private JSONArray interfaces_;
    private JSONArray storage_;
    private JSONArray routes_;
    private JSONArray scripts_;
    private JSONArray hosts_;
    private JSONArray keys_;
    private String rId_;
    private String sliceId_;
    private String readToken_;
    private String writeToken_;
    private NEucaCometInterface comet_;

    // Constructors
    public NEucaCometDataGenerator() {
        users_ = null;
        interfaces_ = null;
        storage_ = null;
        routes_ = null;
        scripts_ = null;
        rId_ = null;
        sliceId_ = null;
        comet_ = null;
        readToken_ = null;
        writeToken_ = null;
    }

    /*
     * @brief constructor
     *
     * @param cometHostList         list of comethost names
     * @param caCert                Ca Certificate
     * @param clientCertKeyStore    client certificate key store
     * @param clientCertKeyStorePwd client certificate key store password
     * @param rid                   reservation id
     * @param sliceId               slice Id
     * @param readToken             readToken
     * @param writeToken            writeToken
     *
     */
    public NEucaCometDataGenerator(String cometHosts, String caCert, String clientCertKeyStore, String clientCertKeyStorePwd,
                                   String rId, String sliceId, String readToken, String writeToken)  {
        users_ = null;
        interfaces_ = null;
        storage_ = null;
        routes_ = null;
        scripts_ = null;
        rId_ = rId;
        sliceId_ = sliceId;
        readToken_ = readToken;
        writeToken_ = writeToken;
        comet_ = new NEucaCometInterface(cometHosts);
        comet_.setSslCaCert(caCert, clientCertKeyStore, clientCertKeyStorePwd);
    }

    /*
     * @brief function to load/read existing data from comet. The read meta data is maintained in the JSON objects
     * which are later used for subsequent operations
     *
     * @param family - Specifies category of the metadata to be read from comet
     *
     * @return true for success; false otherwise
     *
     */

    public boolean loadObject(Family family, String familySuffix) {
        try {
            if (rId_ != null && sliceId_ != null) {
                JSONArray value = comet_.read(sliceId_, rId_, readToken_, family.toString() + familySuffix);
                if (value != null) {
                    switch (family) {
                        case users:
                            users_ = value;
                            break;
                        case interfaces:
                            interfaces_ = value;
                            break;
                        case storage:
                            storage_ = value;
                            break;
                        case routes:
                            routes_ = value;
                            break;
                        case scripts:
                            scripts_ = value;
                            break;
                        case hosts:
                            hosts_ = value;
                            break;
                        case pubkeys:
                            keys_ = value;
                            break;
                    }
                    return true;
                }
            }
        }
        catch (Exception e) {
            System.out.println("NEucaCometDataGenerator::loadObject: Exception occurred while loadingObject: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /*
     * @brief function returns the specific category of the meta data requested
     *
     * @param family - Specifies category of the metadata to be read from comet
     *
     * @return String containing the metadata in case of success; otherwise null
     *
     */
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
                case storage:
                    if (storage_ != null && storage_.size() > 0) {
                        return storage_.toString();
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
                case hosts:
                    if (hosts_ != null && hosts_.size() > 0) {
                        return hosts_.toString();
                    }
                    break;
                case pubkeys:
                    if (keys_ != null && keys_.size() > 0) {
                        return keys_.toString();
                    }
                    break;
            }
        }
        catch (Exception e) {
            System.out.println("NEucaCometDataGenerator::getObject: Exception occurred while getObject: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /*
     * @brief function saves the constructed json meta data in comet by invoke writeScope API
     *
     * @param family - Specifies category of the metadata to be saved to comet
     * @param familySuffix - Specifies suffix to be added to the family in the metadata to be saved to comet
     *
     * @return true for success; otherwise false
     *
     */
    public boolean saveObject(Family family, String familySuffix) {
        try {
            if (rId_ != null && sliceId_ != null && familySuffix != null) {
                // Send Rest Request to get
                String value = getObject(family);
                if(value != null && !value.isEmpty()) {
                    System.out.println("NEucaCometDataGenerator::saveObject: Saving family: " + family+familySuffix + " value: " + value);
                    return comet_.write(sliceId_, rId_, readToken_, writeToken_, family.toString() + familySuffix, value);
                }
                else {
                    System.out.println("NEucaCometDataGenerator::saveObject: Removing family: " + family);
                    return comet_.write(sliceId_, rId_, readToken_, writeToken_, family.toString() + familySuffix, "[]");
                }
            }
            else {
                System.out.println("NEucaCometDataGenerator::saveObject: rId or sliceId or familySuffix is null");
            }
        }
        catch (Exception e) {
            System.out.println("NEucaCometDataGenerator::saveObject: Exception occurred while saveObject: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /*
     * @brief function adds a user to the Users JSON object
     *
     * @param user - user name
     * @param sudo - sudo permissions
     * @param key - keys
     *
     * @return true for success; otherwise false
     *
     */
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
            System.out.println("NEucaCometDataGenerator::addUser: Exception occurred while addUser: " + e.getMessage());
            e.printStackTrace();
        }
        return retVal;
    }

    /*
     * @brief function adds an interface to the interfaces JSON object
     *
     * @param mac - mac address
     * @param state - state of the interface up or down
     * @param ipVersion - ipv4 or ipv6
     * @param ip - ip address
     * @param hosteth - host ethernet interface to which the interface is bound to
     * @param vlanTag - vlan tag
     *
     * @return true for success; otherwise false
     *
     */
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
            System.out.println("NEucaCometDataGenerator::addInterface: Exception occurred while addInterface: " + e.getMessage());
            e.printStackTrace();
        }
        return retVal;
    }

    /*
     * @brief function adds storage to the storage JSON object
     *
     * @param device - device name
     * @param storageType - storage type
     * @param targetIp - ip address
     * @param targetPort - target Port
     * @param targetLun - target Lun
     * @param targetChapUser - targetChapUser
     * @param targetChapSecret - targetChapSecret
     * @param targetShouldAttach - targetShouldAttach
     * @param fsType - fsType
     * @param fsOptions - fsOptions
     * @param fsShouldFormat - fsShouldFormat
     * @param fsMountPoint - fsMountPoint
     *
     * @return true for success; otherwise false
     *
     */
    public boolean addStorage(String device, String storageType, String targetIp, String targetPort, String targetLun,
                           String targetChapUser, String targetChapSecret, String targetShouldAttach,
                           String fsType, String fsOptions, String fsShouldFormat, String fsMountPoint) {
        boolean retVal = false;
        try {
            if (device == null || storageType == null || targetIp == null ||
                targetPort == null || targetLun == null || targetChapUser  == null ||
                targetChapSecret == null || targetShouldAttach == null) {
                System.out.println("NEucaCometDataGenerator::addStorage: Missing mandatory storage parameters!");
                return retVal;
            }
            if (storage_ == null) {
                storage_ = new JSONArray();
            }
            JSONObject storage = new JSONObject();
            storage.put(JsonKeyStorageDevice, device);
            storage.put(JsonKeyStorageType, storageType);
            storage.put(JsonKeyStorageTargetIp, targetIp);
            storage.put(JsonKeyStorageTargetPort, targetPort);
            storage.put(JsonKeyStorageTargetLun, targetLun);
            storage.put(JsonKeyStorageTargetChapUser, targetChapUser);
            storage.put(JsonKeyStorageTargetChapSecret, targetChapSecret);
            storage.put(JsonKeyStorageTargetShouldAttach, targetShouldAttach);

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

            retVal = storage_.add(storage);
        }
        catch (Exception e) {
            System.out.println("NEucaCometDataGenerator::addStorage: Exception occurred while addStorage: " + e.getMessage());
            e.printStackTrace();
        }
        return retVal;
    }

    /*
     * @brief function adds route to the routes JSON object
     *
     * @param routeNetwork - route network
     * @param routeNextHop - route next hop
     * @param device - device
     * @param gateway - gateway
     *
     * @return true for success; otherwise false
     *
     */
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
            System.out.println("NEucaCometDataGenerator::addRoute: Exception occurred while addRoute: " + e.getMessage());
            e.printStackTrace();
        }
        return retVal;
    }

    /*
     * @brief function adds script to the scripts JSON object
     *
     * @param scriptName - scriptName
     * @param scriptBody - scriptBody
     *
     * @return true for success; otherwise false
     *
     */
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
            System.out.println("NEucaCometDataGenerator::addScript: Exception occurred while addScript: " + e.getMessage());
            e.printStackTrace();
        }
        return retVal;
    }

    /*
     * @brief function adds Host to the HostName JSON object
     *
     * @param hostName - hostName
     * @param ip - ip
     *
     * @return true for success; otherwise false
     *
     */
    public boolean addHost(String hostName, String ip) {
        boolean retVal = false;
        try {
            if (hostName == null || ip == null) {
                System.out.println("NEucaCometDataGenerator::addHost: Missing mandatory host parameters!");
                return retVal;
            }
            if (hosts_ == null) {
                hosts_ = new JSONArray();
            }

            JSONObject host = new JSONObject();
            host.put(JsonKeyHostName, hostName);
            host.put(JsonKeyIp, ip);
            retVal = hosts_.add(host);
        }
        catch (Exception e) {
            System.out.println("NEucaCometDataGenerator::addHost: Exception occurred while addHost: " + e.getMessage());
            e.printStackTrace();
        }
        return retVal;
    }

    /*
     * @brief function adds key to the Keys JSON object
     *
     * @param publicKey - publicKey
     *
     * @return true for success; otherwise false
     *
     */
    public boolean addKey(String publicKey) {
        boolean retVal = false;
        try {
            if (publicKey == null) {
                System.out.println("NEucaCometDataGenerator::addKey: Missing mandatory key parameters!");
                return retVal;
            }
            if (keys_ == null) {
                keys_ = new JSONArray();
            }

            JSONObject key = new JSONObject();
            key.put(JsonKeyPublicKey, publicKey);
            retVal = keys_.add(key);
        }
        catch (Exception e) {
            System.out.println("NEucaCometDataGenerator::addKey: Exception occurred while addKey: " + e.getMessage());
            e.printStackTrace();
        }
        return retVal;
    }

    /*
     * @brief function removes specific element from specific category of JSON representing meta data
     *
     * @param family - Specifies category of the metadata
     * @param key - element to be removed
     *
     * @return true for success; otherwise false
     *
     */
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
                case storage:
                    familyToBeUpdated = storage_;
                    break;
                case routes:
                    familyToBeUpdated = routes_;
                    break;
                case scripts:
                    familyToBeUpdated = scripts_;
                    break;
                case hosts:
                    familyToBeUpdated = hosts_;
                    break;
                case pubkeys:
                    familyToBeUpdated = keys_;
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
            System.out.println("NEucaCometDataGenerator::remove: Exception occurred while removeUser: " + e.getMessage());
            e.printStackTrace();
        }
        return retVal;
    }

}
