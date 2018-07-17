package orca.handlers.ec2.tasks;

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
import javax.net.ssl.KeyManagerFactory;

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

public class NEucaCometInterface {
    public final static String ReponseOk = "OK";
    public final static String JsonKeyValue = "value";
    public final static String JsonKeyVal = "val_";

    private ApiClient apiClient;
    private DefaultApi api;
    private InputStream sslCaCert;
    private InputStream sslClientCertKS;

    NEucaCometInterface(String cometHost) {

        apiClient = new ApiClient();
        apiClient.setBasePath(cometHost);
        api = new DefaultApi(apiClient);


    }

    public void setSslCaCert(String caCert, String clientCertKeyStore, String clientCertKeyStorePwd) {
        try {
            if(sslCaCert == null && apiClient != null) {
                //TODO load cert from properties
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
            System.out.println("NEucaCometInterface::setSslCaCert: Exception occurred while constructing NEucaCometInterface");
            e.printStackTrace();
        }
    }

    public JSONArray read(String contextId, String key, String readToken, String family) {
        JSONArray returnValue = null;
        try {
            CometResponse response = api.readScopeGet(contextId, family, key, readToken);
            if (!response.getStatus().equals(ReponseOk)) {
                System.out.println("NEucaCometInterface::read: Unable to read data for context:" + contextId + " key:" + key + " readToken:" + readToken + " family:" + family);
                System.out.println("NEucaCometInterface::read: Status: " + response.getStatus() + "Message: " + response.getMessage());
                return returnValue;
            }
            com.google.gson.internal.LinkedTreeMap o1 = (com.google.gson.internal.LinkedTreeMap) response.getValue();
            if( o1 != null) {
                if(o1.containsKey(JsonKeyValue)) {
                    System.out.println("NEucaCometInterface::read: value=" + o1.get(JsonKeyValue));
                    JSONObject o2 = (JSONObject)JSONValue.parse(o1.get(JsonKeyValue).toString());
                    if(o2 != null) {
                        if(o2.containsKey(JsonKeyVal)) {
                            System.out.println("NEucaCometInterface::read: found " + JsonKeyVal + "=" + o2.get("val_").toString());
                            returnValue = (JSONArray) JSONValue.parse(o2.get("val_").toString());
                        }
                        else {
                            System.out.println("NEucaCometInterface::read: not found " + JsonKeyVal);
                        }
                    }
                    else {
                        System.out.println("NEucaCometInterface::read: Unable to get JSONObject value");
                    }
                }
                else {
                    System.out.println("NEucaCometInterface::read: CometResponse does not contain value");
                }
            }
            else {
                System.out.println("NEucaCometInterface::read: unable to load json object from CometResponse");
            }
        }
        catch (ApiException e) {
            System.out.println("NEucaCometInterface::read: ApiException occurred while read: " + e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e) {
            System.out.println("NEucaCometInterface::read: Exception occurred while read: " + e.getMessage());
            e.printStackTrace();
        }
        return returnValue;
    }
    public boolean write(String contextId, String key, String readToken, String writeToken, String family, String value) {
        boolean returnValue = false;
        try {
            CometValue v = new CometValue(value);
            CometResponse response = api.writeScopePost(v, contextId, family, key, readToken, writeToken);
            if (!response.getStatus().equals(ReponseOk)) {
                System.out.println("NEucaCometInterface::write: Unable to write data for context:" + contextId + " key:" + key + " readToken:" + readToken + " family:" + family);
                System.out.println("NEucaCometInterface::write: Status: " + response.getStatus() + " Message: " + response.getMessage());
                return returnValue;
            }
            System.out.println("NEucaCometInterface::write: Write successful: " + response.getMessage());
            returnValue = true;
        }
        catch (ApiException e) {
            System.out.println("NEucaCometInterface::write: ApiException occurred while write: " + e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e) {
            System.out.println("NEucaCometInterface::write: Exception occurred while write: " + e.getMessage());
            e.printStackTrace();
        }
        return returnValue;
    }
    public boolean remove(String contextId, String key, String readToken, String writeToken, String family) {
        boolean returnValue = false;
        try {
            CometResponse response = api.deleteScopeDelete(contextId, family, key, readToken, writeToken);
            if (!response.getStatus().equals(ReponseOk)) {
                System.out.println("NEucaCometInterface::remove: Unable to remove data for context:" + contextId + " key:" + key + " readToken:" + readToken + " family:" + family);
                System.out.println("NEucaCometInterface::remove: Status: " + response.getStatus() + " Message: " + response.getMessage());
                return returnValue;
            }
            System.out.println("NEucaCometInterface::remove: Remove successful" + response.getMessage());
            returnValue = true;
        }
        catch (ApiException e) {
            System.out.println("NEucaCometInterface::remove: ApiException occurred while remove: " + e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e) {
            System.out.println("NEucaCometInterface::remove: Exception occurred while remove: " + e.getMessage());
            e.printStackTrace();
        }
        return returnValue;
    }
}
