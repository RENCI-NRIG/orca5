package orca.controllers.xmlrpc;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import orca.controllers.OrcaController;
import orca.controllers.OrcaXmlrpcServlet;
import orca.controllers.xmlrpc.geni.GeniAmV1Handler;
import orca.controllers.xmlrpc.geni.GeniAmV2Handler;
import orca.controllers.xmlrpc.x509util.CredentialValidator;
import orca.manage.IOrcaServiceManager;
import orca.ndl.NdlCommons;
import orca.ndl.NdlModifyParser;
import orca.ndl.NdlRequestParser;
import orca.shirako.common.ConfigurationException;
import orca.util.ID;

import org.apache.xmlrpc.XmlRpcException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class XmlRpcController extends OrcaController {
	public static final String PropertyRequestNdl = "request.ndl";
	public static final String PropertyOrcaCredentialVerification = "orca.credential.verification.required";
	public static final String PropertyControllerWhiteListFile = "controller.whitelist.file";
	public static final String PropertyControllerBlackListFile = "controller.blacklist.file";
	public static final String PropertyControllerDisableMemoryCheckFile = "controller.disable.memory.check.file";
	public static final String PropertyControllerMemoryThreshold = "controller.memory.threshold";
	public static final String PropertyVelocityTmpdir = "velocity.tmpdir";
	public static final String PropertyDefaultBrokerName = "xmlrpc.controller.defaultBroker";
	public static final String KeystoreLocation = "config/xmlrpc.jks";
	public static final String KeystorePasswordProperty = "xmlrpc.controller.keystore.pass";
	public static final String CtrlPortProperty = "xmlrpc.controller.port";
	public static final String CtrlThreadsProperty = "xmlrpc.controller.threads";
	public static final String PropertyControllerCallWaitTimeMs = "controller.wait.time.ms";
	public static final String PropertyControllerMaxCreateTimeMs = "controller.create.wait.time.ms";
	public static final String PropertyDelayResourceTypes = "controller.delay.resource.types";
	public static final String PropertyUserRequestRulesFile = "controller.user.request.rules.file";
	public static final String PropertyUserModifyRulesFile = "controller.user.modify.rules.file";
	
	private static final int defaultPort = 9443;
	private static final int defaultThreads = 10;
	private static final String defaultKeystorePass = "xmlrpc";
	/**
	 * The jetty server.
	 */
	private Server server;

	@Override
	public ID getBroker(IOrcaServiceManager sm) {
		if (getProperty(PropertyDefaultBrokerName) != null){
			return new ID(getProperty(PropertyDefaultBrokerName));
		}
		return super.getBroker(sm);
	}

	@Override
	protected void init() throws Exception {
		super.init();
		XmlrpcOrcaState.getInstance().setController(this);
		
		// copy NDL-related controller properties into system properties
		String requestRulesFileName = OrcaController.getProperty(XmlRpcController.PropertyUserRequestRulesFile);
		if (requestRulesFileName != null) {
			Log.info("Copying " + XmlRpcController.PropertyUserRequestRulesFile + " set to " + requestRulesFileName + " to " + NdlRequestParser.USER_REQUEST_RULES_FILE_PROPERTY + " system property");
			System.setProperty(NdlRequestParser.USER_REQUEST_RULES_FILE_PROPERTY, 
					requestRulesFileName);
		}
		
		String modifyRulesFileName = OrcaController.getProperty(XmlRpcController.PropertyUserModifyRulesFile);
		if (modifyRulesFileName != null) {
			Log.info("Copying " + XmlRpcController.PropertyUserModifyRulesFile + " set to " + modifyRulesFileName + " to " + NdlModifyParser.USER_MODIFY_RULES_FILE_PROPERTY + " system property");
			System.setProperty(NdlModifyParser.USER_MODIFY_RULES_FILE_PROPERTY, 
					modifyRulesFileName);
		}
	}
	  
	private void setupXmlRpcHandlers() throws ConfigurationException {		
		try {
			Log.info("Adding XMLRPC Orca handler to global list (namespace 'orca')");
	        OrcaXmlrpcServlet.addXmlrpcHandler("orca", OrcaXmlrpcHandler.class, false);
	        Log.info("Adding XMLRPC GENI AM v1 handler to global list");
	        OrcaXmlrpcServlet.addXmlrpcHandler(GeniAmV1Handler.XMLRPC_SUFFIX, GeniAmV1Handler.class, false);
	        Log.info("Adding XMLRPC GENI AM v2 handler to global list (default namespace 'geni')");
	        OrcaXmlrpcServlet.addXmlrpcHandler(GeniAmV2Handler.XMLRPC_SUFFIX, GeniAmV2Handler.class, true);
		} catch (XmlRpcException e){
			throw new ConfigurationException(e);
		}
	}

	public void start() throws Exception {
		server = new Server();
		server.setStopAtShutdown(true);
		
		Log.info("Starting XMLRPC controller");
		
		int serverSslPort = defaultPort;
		if (getProperty(CtrlPortProperty) != null) {
			try {
				serverSslPort = Integer.parseInt(getProperty(CtrlPortProperty));
			} catch (NumberFormatException e) {
				Log.error("Unable to parse controller port property " + CtrlPortProperty + " = " + getProperty(CtrlPortProperty));
			}
		}
		int acceptorThreads = defaultThreads;
		if (getProperty(CtrlThreadsProperty) != null) {
			try {
				acceptorThreads = Integer.parseInt(getProperty(CtrlThreadsProperty));
			} catch (NumberFormatException e) {
				Log.error("Unable to parse controller port property " + CtrlThreadsProperty + " = " + getProperty(CtrlThreadsProperty));
			}
		}
		String keyStorePass = defaultKeystorePass;
		if (getProperty(KeystorePasswordProperty) != null) {
			keyStorePass = getProperty(KeystorePasswordProperty).trim();
		}
		
		// disable the non-ssl connector
//		SelectChannelConnector connector = new SelectChannelConnector();
//		connector.setPort(9090);
//		connector.setAcceptors(2);
//		server.addConnector(connector);

		SslSelectChannelConnector ssl_connector = new SslSelectChannelConnector();
	        ssl_connector.setPort(serverSslPort);
	        ssl_connector.setPassword(keyStorePass);
	        ssl_connector.setKeyPassword(keyStorePass);
	        ssl_connector.setKeystore(HomeDirectory + KeystoreLocation);
	        ssl_connector.setNeedClientAuth(true);
	        ssl_connector.setTruststore(HomeDirectory + getProperty(CredentialValidator.PropertyChTruststorePath));
	        ssl_connector.setTrustPassword(getProperty(CredentialValidator.PropertyChTruststorePassword));
	        ssl_connector.setAcceptors(acceptorThreads);
	        server.addConnector(ssl_connector);
		
	        // the handler
		ServletContextHandler servletHandler =
                    new ServletContextHandler(ServletContextHandler.SECURITY|ServletContextHandler.SECURITY);
		servletHandler.setContextPath("/");
		
		// add the orca xmlrpc servlet
		OrcaXmlrpcServlet xmlrpc = new OrcaXmlrpcServlet();
		ServletHolder xmlrpcHolder = new ServletHolder(xmlrpc);
		xmlrpcHolder.setName("orca-xmlrpc");
		servletHandler.addServlet(xmlrpcHolder, "/orca/xmlrpc");

		// register the handler
		server.setHandler(servletHandler);
			
		ControllerContextListener l = new ControllerContextListener();
		servletHandler.addEventListener(l);

		// Set up a thread pool for the server, that has (at a minimum)
		// enough threads to accomodate all of the acceptor threads.
		// The multipliers for setMinThreads() and setMaxThreads()
		// are arbitrarily chosen, otherwise.
		QueuedThreadPool qtp = new QueuedThreadPool();
		qtp.setMinThreads(acceptorThreads * 2);
		qtp.setMaxThreads(acceptorThreads * 4);
		server.setThreadPool(qtp);
		server.start();
		l.start();
	}
	
	
	private class ControllerContextListener implements ServletContextListener {
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
				Log.info("Recovering the XMLRPC controller");
				recover();
				Log.info("Starting XMLRPC handlers");
				setupXmlRpcHandlers();
				Log.info("Starting support threads");
				XmlrpcOrcaState.startThreads();
			} catch (Exception e){
				Log.fatal("Could not start the XMLRPC controller", e);
				System.err.println(e.getMessage());
				System.exit(1);
			}
		}
		
		private void stop() {
			try {
				Log.info("Stopping the XMLRPC controller");
			} catch (Exception e){
			}
		}
	}
	
	/**
	 * Recover the XMLRPC controller
	 */
	protected void _recover() throws Exception {		
		XmlrpcOrcaState.getInstance().recover();
	}
	
	public static void main(String args[]) throws Exception{
		XmlRpcController cont = new XmlRpcController();
		cont.start();
	}
}
