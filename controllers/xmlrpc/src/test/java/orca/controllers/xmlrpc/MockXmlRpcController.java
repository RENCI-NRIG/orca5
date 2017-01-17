package orca.controllers.xmlrpc;

import orca.controllers.OrcaXmlrpcServlet;
import orca.controllers.mock.MockOrcaConnectionFactory;
import orca.controllers.xmlrpc.XmlRpcController;
import orca.controllers.xmlrpc.XmlrpcOrcaState;
import orca.manage.IOrcaServiceManager;
import orca.shirako.common.ConfigurationException;
import orca.util.ID;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class MockXmlRpcController extends XmlRpcController {

    /**
     *
     * @throws Exception
     */
    @Override
    protected void init() throws Exception {
        XmlrpcOrcaState.getInstance().setController(this);
        initConnectionFactory();
    }

    /**
     *
     * @throws Exception
     */
    private void initConnectionFactory() throws Exception {
        getLogger(this.getClass().getSimpleName()).debug("initializing MockOrcaConnectionFactory");
        String smGuid = controllerProperties.getProperty(ControllerServiceManager);
        if (smGuid == null){
            throw new ConfigurationException("Please specify a service manager to connect to");
        }

        String url = controllerProperties.getProperty(OrcaURL);
        String user = controllerProperties.getProperty(OrcaUser);
        String password = controllerProperties.getProperty(OrcaLogin);
        orca = new MockOrcaConnectionFactory(url, user, password, new ID(smGuid));
    }

    /**
     * Only the basics from XmlRpcController
     *
     * no Jetty threads for incoming connections avoids Address in use errors from Bind
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        // the handler
        ServletContextHandler servletHandler =
                new ServletContextHandler(ServletContextHandler.SECURITY|ServletContextHandler.SECURITY);
        servletHandler.setContextPath("/");

        // add the orca xmlrpc servlet
        OrcaXmlrpcServlet xmlrpc = new OrcaXmlrpcServlet();
        ServletHolder xmlrpcHolder = new ServletHolder(xmlrpc);
        xmlrpcHolder.setName("orca-xmlrpc");
        servletHandler.addServlet(xmlrpcHolder, "/orca/xmlrpc");

        ControllerContextListener l = new ControllerContextListener();
        servletHandler.addEventListener(l);

        l.start();
    }

    /**
     *
     * @param sm
     * @return
     */
    @Override
    public ID getBroker(IOrcaServiceManager sm) {
        return new ID();
    }
}
