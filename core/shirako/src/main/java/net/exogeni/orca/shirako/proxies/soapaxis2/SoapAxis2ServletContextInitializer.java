package net.exogeni.orca.shirako.proxies.soapaxis2;

import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.shirako.container.api.IOrcaConfiguration;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.AxisServlet;

public class SoapAxis2ServletContextInitializer
{
    public static final String Axis2SecureConfigurationFile = "/axis2.xml";
    public static final String Axis2InsecureConfigurationFile = "/axis2.no.signatures.xml";
        
    public SoapAxis2ServletContextInitializer()
    {
    }
    
    /**
     * Initializes an axis2 configuration context, so that all services
     * instantiated within this container reside in the same context.
     * @param config config
     * @throws Exception in case of error
     */
    public void initialize(IOrcaConfiguration config) throws Exception
    {
        assert config != null;
        
        Globals.Log.info("Initializing soap (axis2) container context");
        if (SoapAxis2ServiceFactory.context != null) {
        	Globals.Log.info("The soap (axis2) container context has already been initialized");
        	return;
        }
        
        String root = Globals.HomeDirectory + "config/";
    	Globals.Log.info("root: " + root);
        String configFile = null;

        if (config.isSecureActorCommunication()) {
            Globals.Log.debug("using secure communication");
            configFile = root + Axis2SecureConfigurationFile;
        } else {
            Globals.Log.debug("not using secure communication");
            configFile = root + Axis2InsecureConfigurationFile;
        }

        Globals.Log.debug("context configuration file: " + configFile);
        
        String repo = config.getAxis2ClientRepository();
        Globals.Log.debug("repository location: " + repo);
        
        ConfigurationContext context = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repo, configFile);
        context.setProperty(org.apache.axis2.Constants.CONTAINER_MANAGED, org.apache.axis2.Constants.VALUE_TRUE);

        Globals.getServletContext().setAttribute(AxisServlet.CONFIGURATION_CONTEXT, context);
        SoapAxis2ServiceFactory.context = context;
    }

    public void shutdown()
    {
        Globals.getServletContext().removeAttribute(AxisServlet.CONFIGURATION_CONTEXT);
        SoapAxis2ServiceFactory.context = null;
    }
}
