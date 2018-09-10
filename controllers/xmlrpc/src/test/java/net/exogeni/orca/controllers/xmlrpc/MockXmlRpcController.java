package net.exogeni.orca.controllers.xmlrpc;

import net.exogeni.orca.controllers.OrcaXmlrpcServlet;
import net.exogeni.orca.controllers.mock.MockOrcaConnectionFactory;
import net.exogeni.orca.controllers.xmlrpc.statuswatch.ReservationStatusUpdateThread;
import net.exogeni.orca.manage.IOrcaServiceManager;
import net.exogeni.orca.manage.beans.TicketReservationMng;
import net.exogeni.orca.ndl.NdlCommons;
import net.exogeni.orca.shirako.common.ConfigurationException;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.container.Globals;
import net.exogeni.orca.util.ID;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletContextEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * A messy attempt at an XmlRpcController that doesn't need to talk to 'Live' servers. The main thing is that it returns
 * a MockOrcaConnectionFactory, but several methods had to be overridden.
 *
 */
public class MockXmlRpcController extends XmlRpcController {

    protected Map<ReservationID, TicketReservationMng> reservationMap;
    protected boolean failReservation = false;

    /**
     *
     * @throws Exception
     */
    @Override
    protected void init() throws Exception {
        this.init(reservationMap);
    }

    /**
     * Jump-start a new SM with some fake reservations already in place.
     *
     * @param reservationMap
     *            a Map of fake reservations that any new SM will have. Useful for testing modifySlice()
     * @throws Exception
     */
    protected void init(Map<ReservationID, TicketReservationMng> reservationMap) throws Exception {
        this.init(reservationMap, failReservation);
    }

    /**
     * Jump-start a new SM with some fake reservations already in place, as well as an indicator to the (Mock) SM
     * whether it should fail any reservations.
     *
     * @param reservationMap
     *            a Map of fake reservations that any new SM will have. Useful for testing modifySlice()
     * @param failReservation
     *            an indicator to any SM created whether it should fail any reservations
     * @throws Exception
     */
    public void init(Map<ReservationID, TicketReservationMng> reservationMap, boolean failReservation)
            throws Exception {
        if (null != reservationMap) {
            this.reservationMap = reservationMap;
        } else {
            this.reservationMap = new HashMap<>();
        }
        this.failReservation = failReservation;

        XmlrpcOrcaState.getInstance().resetInstance();
        XmlrpcOrcaState.getInstance().setController(this);
        initConnectionFactory();
    }

    /**
     * Initialize a MockOrcaConnectionFactory, with additional Testing parameters
     *
     * @throws Exception
     */
    private void initConnectionFactory() throws Exception {
        getLogger(this.getClass().getSimpleName()).debug("initializing MockOrcaConnectionFactory");
        String smGuid = controllerProperties.getProperty(ControllerServiceManager);
        if (smGuid == null) {
            throw new ConfigurationException("Please specify a service manager to connect to");
        }

        String url = controllerProperties.getProperty(OrcaURL);
        String user = controllerProperties.getProperty(OrcaUser);
        String password = controllerProperties.getProperty(OrcaLogin);

        orca = new MockOrcaConnectionFactory(url, user, password, new ID(smGuid), reservationMap, failReservation);
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
        ServletContextHandler servletHandler = new ServletContextHandler(
                ServletContextHandler.SECURITY | ServletContextHandler.SECURITY);
        servletHandler.setContextPath("/");

        // add the orca xmlrpc servlet
        OrcaXmlrpcServlet xmlrpc = new OrcaXmlrpcServlet();
        ServletHolder xmlrpcHolder = new ServletHolder(xmlrpc);
        xmlrpcHolder.setName("orca-xmlrpc");
        servletHandler.addServlet(xmlrpcHolder, "/orca/xmlrpc");

        ControllerContextListener l = new MockControllerContextListener();
        servletHandler.addEventListener(l);

        l.start();
    }

    /**
     * Returns a dummy (new) broker ID
     *
     * @param sm
     *            ignored
     * @return new ID()
     */
    @Override
    public ID getBroker(IOrcaServiceManager sm) {
        return new ID();
    }

    protected class MockControllerContextListener extends ControllerContextListener {
        public void contextInitialized(ServletContextEvent arg0) {
        }

        public void contextDestroyed(ServletContextEvent arg0) {
            stop();
        }

        public void start() {
            try {
                Log.info("Initializing the XMLRPC controller");
                NdlCommons.init();
                init();
                // Log.info("Recovering the XMLRPC controller");
                // recover();
                // Log.info("Starting XMLRPC handlers");
                // setupXmlRpcHandlers();
                Log.info("Starting support threads");
                // XmlrpcOrcaState.startThreads();
                startThreads();
            } catch (Exception e) {
                Log.fatal("Could not start the XMLRPC controller", e);
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }

        // start the threads (called from the controller startup code)
        // original code in XmlrpcOrcaState
        public void startThreads() {
            // slice defer thread
            Globals.Log.info("Starting SliceDeferThread");
            XmlrpcOrcaState.sdt = new SliceDeferThread();
            // XmlrpcOrcaState.sdtThread = new Thread(XmlrpcOrcaState.sdt);
            // XmlrpcOrcaState.sdtThread.setDaemon(true);
            // XmlrpcOrcaState.sdtThread.setName("SliceDeferThread");
            // XmlrpcOrcaState.sdtThread.start();

            // modify status thread
            Globals.Log.info("Scheduling periodic ReservationStatusUpdateThread at "
                    + ReservationStatusUpdateThread.getPeriod() + " sec.");
            XmlrpcOrcaState.sut = new ReservationStatusUpdateThread();
            // sutFuture = scheduler.scheduleAtFixedRate(sut, ReservationStatusUpdateThread.getPeriod(),
            // ReservationStatusUpdateThread.getPeriod(), TimeUnit.SECONDS);

        }

        private void stop() {
            try {
                Log.info("Stopping the XMLRPC controller");
            } catch (Exception e) {
            }
        }
    }

    /**
     *
     * @param p
     *            property name
     * @param value
     *            new value
     */
    public void setProperty(String p, String value) {
        controllerProperties.setProperty(p, value);
    }

}
