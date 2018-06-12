package orca.handlers.ec2.tasks;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.model.CometResponse;
import io.swagger.client.model.Value;
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
                System.out.println("SSL cert is already configured or apiClient does not exist");
            }
        }
        catch (Exception e) {
            System.out.println("Exception occurred while constructing NEucaCometInterface");
            e.printStackTrace();
        }
    }

    public JSONArray read(String contextId, String key, String readToken, String family) {
        JSONArray returnValue = null;
        try {
            CometResponse response = api.readScopeGet(contextId, key, readToken, family);
            if (!response.getStatus().equals(ReponseOk)) {
                System.out.println("Unable to read data for context:" + contextId + " key:" + key + " readToken:" + readToken + " family:" + family);
                System.out.println("Status: " + response.getStatus() + "Message: " + response.getMessage());
                return returnValue;
            }
            JSONObject o = (JSONObject) response.getValue();
            if(o.values().size() == 0) {
                System.out.println("Empty scope read");
                return returnValue;
            }
            returnValue = (JSONArray) JSONValue.parse(o.values().toString());
        }
        catch (ApiException e) {
            System.out.println("ApiException occured while read: " + e.getMessage());
        }
        catch (Exception e) {
            System.out.println("Exception occured while read: " + e.getMessage());
        }
        return returnValue;
    }
    public boolean write(String contextId, String key, String readToken, String writeToken, String family, String value) {
        boolean returnValue = false;
        try {
            CometValue v = new CometValue(value);
            CometResponse response = api.writeScopePost(v, contextId, family, key, readToken, writeToken);
            if (!response.getStatus().equals(ReponseOk)) {
                System.out.println("Unable to write data for context:" + contextId + " key:" + key + " readToken:" + readToken + " family:" + family);
                System.out.println("Status: " + response.getStatus() + " Message: " + response.getMessage());
                return returnValue;
            }
            System.out.println("Write successful" + response.getMessage());
            returnValue = true;
        }
        catch (ApiException e) {
            System.out.println("ApiException occured while write: " + e.getMessage());
        }
        catch (Exception e) {
            System.out.println("Exception occured while write: " + e.getMessage());
        }
        return returnValue;
    }
}
