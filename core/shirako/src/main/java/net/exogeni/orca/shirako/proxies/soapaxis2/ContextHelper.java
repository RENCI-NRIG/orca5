package net.exogeni.orca.shirako.proxies.soapaxis2;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import net.exogeni.orca.shirako.container.Globals;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.AxisServlet;

public class ContextHelper implements ServletContextListener
{
    public void contextInitialized(ServletContextEvent event)
    {
        Globals.Log.info("Starting web application");
        ServletContext sc = event.getServletContext();
        
        try {
            String base = Globals.HomeDirectory + "config";
            String config = base + "/axis2.xml";
            String repo = base + "/axis2repository";
            ConfigurationContext context = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repo, config);
            context.setProperty(org.apache.axis2.Constants.CONTAINER_MANAGED, org.apache.axis2.Constants.VALUE_TRUE);
            sc.setAttribute(AxisServlet.CONFIGURATION_CONTEXT, context);
            SoapAxis2ServiceFactory.context = context;
            Globals.getContainer();
        } catch (Exception e) {
            Globals.Log.error(e);
            System.err.println(e.toString());
            throw new RuntimeException(e);
        }
    }

    public void contextDestroyed(ServletContextEvent event)
    {
        ServletContext sc = event.getServletContext();
        sc.removeAttribute(AxisServlet.CONFIGURATION_CONTEXT);
        SoapAxis2ServiceFactory.context = null;
    }
}
