package orca.comet;

import org.renci.io.swagger.client.ApiClient;
import org.renci.io.swagger.client.ApiException;
import org.renci.io.swagger.client.api.DefaultApi;
import org.renci.io.swagger.client.model.CometResponse;
import org.renci.io.swagger.client.model.Value;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import javax.net.ssl.KeyManagerFactory;

/*
 *
 * @class
 *
 * @brief This class represents the value saved in Comet.
 *
 *
 */

class CometValue extends Value {
    CometValue(String v) {
        val_ = v;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Value {\n");
        sb.append(val_);
        sb.append("}");
        return sb.toString();
    }

    private String val_;
}

/*
 *
 * @class
 *
 * @brief This class implements an interface to interact with Comet by invoking REST APIs
 *
 *
 */
public class NEucaCometInterface {
    public final static String ReponseOk = "OK";
    public final static String JsonKeyValue = "value";
    public final static String JsonKeyVal = "val_";

    private ArrayList<String> cometHosts_;
    private ApiClient apiClient;
    private DefaultApi api;
    private InputStream sslCaCert;
    private InputStream sslClientCertKS;

    /*
     * @brief Constructor
     */
    NEucaCometInterface(String cometHosts) {
        cometHosts_ = new ArrayList<String>(Arrays.asList(cometHosts.split(",")));
        apiClient = new ApiClient();
        api = new DefaultApi(apiClient);
    }

    /*
     * @brief funtion to set the CA certficate, client certificate and key
     *
     * @param caCert - complete path of the CA certificate
     * @param clientCertKeyStore - complete path of the Client certificate keystore
     * @param clientCertKeyStorePwd - Client certificate keystore password
     *
     */
    public void setSslCaCert(String caCert, String clientCertKeyStore, String clientCertKeyStorePwd) {
        try {
            if(sslCaCert == null && apiClient != null) {
                sslCaCert = new FileInputStream(caCert);
                sslClientCertKS = new FileInputStream(clientCertKeyStore);
                apiClient.setSslCaCert(sslCaCert);
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(sslClientCertKS, clientCertKeyStorePwd.toCharArray());
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
                keyManagerFactory.init(ks, clientCertKeyStorePwd.toCharArray());
                apiClient.setKeyManagers(keyManagerFactory.getKeyManagers());
                apiClient.applySslSettings();
            }
            else {
                System.out.println("NEucaCometInterface::setSslCaCert: SSL cert is already configured or apiClient does not exist");
            }
        }
        catch (Exception e) {
            System.out.println("NEucaCometInterface::setSslCaCert: Exception occurred while constructing NEucaCometInterface cometHost=" + apiClient.getBasePath() + " " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * @brief funtion invokes readScope REST API to read meta data from Comet for a specific category
     *
     * @param contextId - Context Id(sliceId)
     * @param readToken - Read Token (random generated string)
     * @param family - Specifies category of the metadata
     *
     * @return true for success; false otherwise
     *
     */
    private CometResponse readScopeGet(String contextId, String key, String readToken, String family) {
        CometResponse response = null;
        Collections.shuffle(cometHosts_);
        for (String host : cometHosts_) {
            System.out.println("NEucaCometInterface::readScopeGet: Using cometHost=" + host);
            apiClient.setBasePath(host);
            try {
                response = null;
                response = api.readScopeGet(contextId, family, key, readToken);
                break;
            }
            catch (ApiException e) {
                System.out.println("NEucaCometInterface::readScopeGet: ApiException occurred while read: cometHost=" + apiClient.getBasePath() + " " + e.getMessage() + " " + e.getResponseBody());
                e.printStackTrace();
                response = null;
                continue;
            }
            catch (Exception e) {
                System.out.println("NEucaCometInterface::readScopeGet: Exception occurred while read: cometHost=" + apiClient.getBasePath() + " " + e.getMessage());
                e.printStackTrace();
                response = null;
                continue;
            }
        }
        System.out.println("NEucaCometInterface::readScopeGet: response=" + response); 
        return response;
    }

    /*
     * @brief funtion invokes writeScope REST API to write meta data from Comet for a specific category
     *
     * @param contextId - Context Id(sliceId)
     * @param key - key (UnitId)
     * @param readToken - Read Token (random generated string)
     * @param writeToken - Write Token (random generated string)
     * @param family - Specifies category of the metadata
     * @param value - Specifies Json containing metadata to be saved
     *
     * @return true for success; false otherwise
     *
     */
    private CometResponse writeScopePost(String contextId, String key, String readToken, String writeToken, String family, String value) {
        CometResponse response = null;
        CometValue v = new CometValue(value);
        Collections.shuffle(cometHosts_);
        for (String host : cometHosts_) {
            System.out.println("NEucaCometInterface::writeScopePost: Using cometHost=" + host);
            apiClient.setBasePath(host);
            try {
                response = null;
                response = api.writeScopePost(v, contextId, family, key, readToken, writeToken);
                break;
            }
            catch (ApiException e) {
                System.out.println("NEucaCometInterface::writeScopePost: ApiException occurred while read: cometHost=" + apiClient.getBasePath() + " " + e.getMessage() + " " + e.getResponseBody());
                e.printStackTrace();
                response = null;
                continue;
            }
            catch (Exception e) {
                System.out.println("NEucaCometInterface::writeScopePost: Exception occurred while read: cometHost=" + apiClient.getBasePath() + " " + e.getMessage());
                e.printStackTrace();
                response = null;
                continue;
            }
        }
        System.out.println("NEucaCometInterface::writeScopePost: response=" + response); 
        return response;
    }


    /*
     * @brief funtion invokes deletScope REST API to delete meta data from Comet for a specific category
     *
     * @param contextId - Context Id(sliceId)
     * @param key - key (UnitId)
     * @param readToken - Read Token (random generated string)
     * @param writeToken - Write Token (random generated string)
     * @param family - Specifies category of the metadata
     *
     * @return true for success; false otherwise
     *
     */
    private CometResponse deleteScopeDelete(String contextId, String key, String readToken, String writeToken, String family) {
        CometResponse response = null;
        Collections.shuffle(cometHosts_);
        for (String host : cometHosts_) {
            System.out.println("NEucaCometInterface::deleteScopeDelete: Using cometHost=" + host);
            apiClient.setBasePath(host);
            try {
                response = null;
                response = api.deleteScopeDelete(contextId, family, key, readToken, writeToken);
                break;
            }
            catch (ApiException e) {
                System.out.println("NEucaCometInterface::deleteScopeDelete: ApiException occurred while read: cometHost=" + apiClient.getBasePath() + " " + e.getMessage() + " " + e.getResponseBody());
                e.printStackTrace();
                response = null;
                continue;
            }
            catch (Exception e) {
                System.out.println("NEucaCometInterface::deleteScopeDelete: Exception occurred while read: cometHost=" + apiClient.getBasePath() + " " + e.getMessage());
                e.printStackTrace();
                response = null;
                continue;
            }
        }
        System.out.println("NEucaCometInterface::deleteScopeDelete: response=" + response); 
        return response;
    }

    /*
     * @brief funtion invokes readScope REST API to read meta data from Comet for a specific category
     *
     * @param contextId - Context Id(sliceId)
     * @param readToken - Read Token (random generated string)
     * @param family - Specifies category of the metadata
     *
     * @return true for success; false otherwise
     *
     */
    public JSONArray read(String contextId, String key, String readToken, String family) {
        JSONArray returnValue = null;
        try {
            CometResponse response = readScopeGet(contextId, key, readToken, family);
            if(response != null) {
                if (!response.getStatus().equals(ReponseOk)) {
                    System.out.println("NEucaCometInterface::read: Unable to read data for context:" + contextId + " key:" + key + " readToken:" + readToken + " family:" + family);
                    System.out.println("NEucaCometInterface::read: Status: " + response.getStatus() + "Message: " + response.getMessage());
                    return returnValue;
                }
                com.google.gson.internal.LinkedTreeMap o1 = (com.google.gson.internal.LinkedTreeMap) response.getValue();
                if (o1 != null) {
                    if (o1.containsKey(JsonKeyValue)) {
                        System.out.println("NEucaCometInterface::read: value=" + o1.get(JsonKeyValue));
                        JSONObject o2 = (JSONObject) JSONValue.parse(o1.get(JsonKeyValue).toString());
                        if (o2 != null) {
                            if (o2.containsKey(JsonKeyVal)) {
                                System.out.println("NEucaCometInterface::read: found " + JsonKeyVal + "=" + o2.get("val_").toString());
                                returnValue = (JSONArray) JSONValue.parse(o2.get("val_").toString());
                            } else {
                                System.out.println("NEucaCometInterface::read: not found " + JsonKeyVal);
                            }
                        } else {
                            System.out.println("NEucaCometInterface::read: Unable to get JSONObject value");
                        }
                    } else {
                        System.out.println("NEucaCometInterface::read: CometResponse does not contain value");
                    }
                } else {
                    System.out.println("NEucaCometInterface::read: unable to load json object from CometResponse");
                }
            }
            else {
                System.out.println("NEucaCometInterface::read: response null");
            }
        }
        catch (Exception e) {
            System.out.println("NEucaCometInterface::read: Exception occurred while read: cometHost=" + apiClient.getBasePath() + " " + e.getMessage());
            e.printStackTrace();
        }
        return returnValue;
    }

    /*
     * @brief funtion invokes writeScope REST API to write meta data from Comet for a specific category
     *
     * @param contextId - Context Id(sliceId)
     * @param key - key (UnitId)
     * @param readToken - Read Token (random generated string)
     * @param writeToken - Write Token (random generated string)
     * @param family - Specifies category of the metadata
     * @param value - Specifies Json containing metadata to be saved
     *
     * @return true for success; false otherwise
     *
     */
    public boolean write(String contextId, String key, String readToken, String writeToken, String family, String value) {
        boolean returnValue = false;
        try {
            CometResponse response = writeScopePost(contextId, key, readToken, writeToken, family, value);
            if(response != null) {
                if (!response.getStatus().equals(ReponseOk)) {
                    System.out.println("NEucaCometInterface::write: Unable to write data for context:" + contextId + " key:" + key + " readToken:" + readToken + " family:" + family);
                    System.out.println("NEucaCometInterface::write: Status: " + response.getStatus() + " Message: " + response.getMessage());
                    return returnValue;
                }
                System.out.println("NEucaCometInterface::write: Write successful: " + response.getMessage());
                returnValue = true;
            }
            else {
                System.out.println("NEucaCometInterface::write: response null");
            }
        }
        catch (Exception e) {
            System.out.println("NEucaCometInterface::write: Exception occurred while write: cometHost=" + apiClient.getBasePath() + " " + e.getMessage());
            e.printStackTrace();
        }
        return returnValue;
    }

    /*
     * @brief funtion invokes deletScope REST API to delete meta data from Comet for a specific category
     *
     * @param contextId - Context Id(sliceId)
     * @param key - key (UnitId)
     * @param readToken - Read Token (random generated string)
     * @param writeToken - Write Token (random generated string)
     * @param family - Specifies category of the metadata
     *
     * @return true for success; false otherwise
     *
     */
    public boolean remove(String contextId, String key, String readToken, String writeToken, String family) {
        boolean returnValue = false;
        try {
            CometResponse response = deleteScopeDelete(contextId, key, readToken, writeToken, family);
            if(response != null) {
                if (!response.getStatus().equals(ReponseOk)) {
                    System.out.println("NEucaCometInterface::remove: Unable to remove data for context:" + contextId + " key:" + key + " readToken:" + readToken + " family:" + family);
                    System.out.println("NEucaCometInterface::remove: Status: " + response.getStatus() + " Message: " + response.getMessage());
                    return returnValue;
                }
                System.out.println("NEucaCometInterface::remove: Remove successful" + response.getMessage());
                returnValue = true;
            }
            else {
                System.out.println("NEucaCometInterface::remove: response null");
            }
        }
        catch (Exception e) {
            System.out.println("NEucaCometInterface::remove: Exception occurred while remove: cometHost=" + apiClient.getBasePath() + " " + e.getMessage());
            e.printStackTrace();
        }
        return returnValue;
    }
}
