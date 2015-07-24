package orca.shirako.proxies.soapaxis2;

import java.util.HashMap;

import orca.shirako.container.Globals;
import orca.shirako.proxies.soapaxis2.services.ActorServiceStub;
import orca.shirako.proxies.soapaxis2.services.AuthorityServiceStub;
import orca.shirako.proxies.soapaxis2.services.BrokerServiceStub;
import orca.tools.axis2.Axis2ClientConfigurationManager;
import orca.util.ID;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.Stub;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;

public class StubManager
{
    // FIXME: need a capacity limit on how many stubs get cached

    private static final StubManager instance = new StubManager();
    private static final int connectionTimeout = 5;
    private static final int socketTimeout = 30;

    public static StubManager getInstance()
    {
        return instance;
    }

    /**
     * Map of stubs (destination url-> (caller, stub))
     */
    private HashMap<String, HashMap<String, Object>> stubs;

    private StubManager()
    {
        stubs = new HashMap<String, HashMap<String, Object>>();
    }

    private synchronized Object getStub(String destination, ID source)
    {
        HashMap<String, Object> map = stubs.get(destination);
        if (map != null) {
            return map.get(source.toString());
        }
        return null;
    }

    private synchronized Object registerStub(String destination, ID source, Object stub)
    {
        HashMap<String, Object> map = stubs.get(destination);
        if (map == null) {
            map = new HashMap<String, Object>();
            map.put(source.toString(), stub);
            stubs.put(destination, map);
            return stub;
        } else {
            Object tmp = map.get(source.toString());
            if (tmp != null) {
                // someone else beat us to this
                return tmp;
            } else {
                map.put(source.toString(), stub);
                return stub;
            }
        }
    }

    /**
     * Returns a stub for communication to destination by source.
     * @param destination destination actor guid
     * @param source source actor guid
     * @param url url for destination service
     * @param type stub type (return, broker, site)
     * @return
     */
    public Object getStub(ID source, String url, int type) throws Exception
    {
        assert source != null && url != null;

        Object result = getStub(url, source);
        if (result == null) {
            result = createStub(url, type, source);
            result = registerStub(url, source, result);
        }
        return result;
    }

    private Object createStub(String url, int type, ID source) throws Exception
    {
        Object result = null;
        String repository = Globals.getContainer().getAxis2ClientRepository();
        String config = null;

        Globals.Log.debug("creating a stub. source: " + source + " url=" + url);
        if (Globals.getConfiguration().isSecureActorCommunication()) {
            Globals.Log.debug("using secure communication");
            config = Globals.getContainer().getAxis2Configuration(source.toString());
        } else {
            Globals.Log.debug("not using secure communication");
            config = Globals.getContainer().getAxis2UnsecureConfiguration(source.toString());
        }

        Globals.Log.debug("axis2 configuration for client stub: " + config);

        ConfigurationContext axis2Context = Axis2ClientConfigurationManager.getInstance().getContext(repository, config);

        switch (type) {
            case SoapAxis2Proxy.TypeReturn:
                result = new ActorServiceStub(axis2Context, url);
                break;
            case SoapAxis2Proxy.TypeBroker:
                result = new BrokerServiceStub(axis2Context, url);
                break;
            case SoapAxis2Proxy.TypeSite:
                result = new AuthorityServiceStub(axis2Context, url);
                break;
        }

        // Before returning the Stub, set some sane timeouts.
        Options stubOpts = ((Stub) result)._getServiceClient().getOptions();
        stubOpts.setProperty(HTTPConstants.CONNECTION_TIMEOUT, connectionTimeout * 1000);
        stubOpts.setProperty(HTTPConstants.SO_TIMEOUT, socketTimeout * 1000);

        return result;
    }
}
