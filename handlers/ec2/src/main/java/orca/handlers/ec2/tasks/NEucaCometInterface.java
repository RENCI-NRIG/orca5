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
    public final static String JsonKeyVal = "val_";

    private ApiClient apiClient;
    private DefaultApi api;
    private InputStream sslCaCert;

    NEucaCometInterface(String cometHost) {

        apiClient = new ApiClient();
        apiClient.setBasePath(cometHost);
        api = new DefaultApi(apiClient);


    }

    public void setSslCaCert() {
        try {
            if(sslCaCert == null && apiClient != null) {
                //TODO load cert from properties
                sslCaCert = new FileInputStream("/Users/komalthareja/comet/certs.der");
                apiClient.setSslCaCert(sslCaCert);
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
            String val = response.getValue().toString();
            if(!val.isEmpty()) {
                String [] arrOfStr = val.split("=");
                if(arrOfStr.length < 2) {
                    System.out.println("NEucaCometInterface::read: Empty Scope read");
                    return returnValue;
                }
                val = arrOfStr[1].substring(0, arrOfStr[1].length()-1);
                JSONObject o = (JSONObject)JSONValue.parse(val);
                returnValue = (JSONArray)JSONValue.parse(o.get(JsonKeyVal).toString());
            }
            else {
                System.out.println("NEucaCometInterface::read: Empty Scope read");
            }
        }
        catch (ApiException e) {
            System.out.println("NEucaCometInterface::read: ApiException occurred while read: " + e.getMessage());
        }
        catch (Exception e) {
            System.out.println("NEucaCometInterface::read: Exception occurred while read: " + e.getMessage());
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
        }
        catch (Exception e) {
            System.out.println("NEucaCometInterface::write: Exception occurred while write: " + e.getMessage());
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
        }
        catch (Exception e) {
            System.out.println("NEucaCometInterface::remove: Exception occurred while remove: " + e.getMessage());
        }
        return returnValue;
    }
}
