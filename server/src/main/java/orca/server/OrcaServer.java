package orca.server;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import orca.shirako.container.Globals;
import orca.shirako.proxies.soapaxis2.SoapAxis2ServiceFactory;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.AxisServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.ws.transport.http.MessageDispatcherServlet;

public class OrcaServer {
	private static final String PROPERTY_BASEDIR = "basedir";
	public static final String ORCA_SERVER_PORT = "ORCA_SERVER_PORT";
	public static final String ORCA_SSL_SERVER_PORT = "ORCA_SSL_SERVER_PORT";
	public static final String ORCA_KEYSTORE="ORCA_KEYSTORE";
	public static final String ORCA_TRUSTSTORE="ORCA_TRUSTSTORE";
	public static final String ORCA_PASSPHRASE="ORCA_PASSPHRASE";
	public static final String ORCA_ALIAS="ORCA_ALIAS";
	public static final int DefaultServerPort = 8080;
	public static final int DefaultSSLServerPort = 8443;
	
	private static final int ServerPort, SSLServerPort;
	private static final String keystore, truststore, alias, passphrase;
	private static boolean enableSsl = true;
	
	static {
		ServerPort = getServerPort(ORCA_SERVER_PORT, DefaultServerPort);
		SSLServerPort = getServerPort(ORCA_SSL_SERVER_PORT, DefaultSSLServerPort);
		
		System.out.println("Got ports: " + ServerPort + "/" + SSLServerPort);
		
		keystore = getStringProperty(ORCA_KEYSTORE);
		truststore = getStringProperty(ORCA_TRUSTSTORE);
		alias = getStringProperty(ORCA_ALIAS);
		passphrase = getStringProperty(ORCA_PASSPHRASE);
		
		if ((keystore == null) || (alias == null) || (passphrase == null))
			enableSsl = false;
		System.out.println("Parsed SSL parameters: " + enableSsl + " " + keystore + " " + alias);
	}

	/**
	 * FIgure out the port based on environment settings and defaults
	 * @param env
	 * @param def
	 * @return
	 */
	private static int getServerPort(String env, int def) {
		int serverPortToSet = def;
		if (System.getProperty(env) != null) {
			try {
				serverPortToSet = Integer.parseInt(System.getProperty(env));
			} catch (NumberFormatException e) {
				System.err.println("Unable to parse server port number " + System.getenv(env) + 
						", using default " + def + " instead");
			}
		} else if (System.getenv(env) != null) {
			try {
				serverPortToSet = Integer.parseInt(System.getenv(env));
			} catch (NumberFormatException e) {
				System.err.println("Unable to parse server port number " + System.getenv(env) + 
						", using default " + def + " instead");
			}
		}
		return serverPortToSet;
	}
	
	/**
	 * Check system properties, then environment variables, return null if not found
	 * @param name
	 * @return
	 */
	private static String getStringProperty(String name) {
		if (System.getProperty(name) != null)
			return System.getProperty(name);
		if (System.getenv(name) != null)
			return System.getenv(name);
		return null;
	}
	
	private Server server;
	private boolean forceFresh;
	
	public OrcaServer() {
		this(false);
	}
	
	public OrcaServer(boolean forceFresh){
		this.forceFresh = forceFresh;
	}

	private void startOrca() {
	    try {
	        Globals.Log.info("Starting Orca ");
	        Globals.start(forceFresh);
	    } catch (Exception e) {
	        Globals.Log.error("Could not start Orca", e);
	        System.err.println(e.toString());
	        System.exit(1);
	    }
	}

	public void start() throws Exception {		
		if (System.getProperty(PROPERTY_BASEDIR) != null) {
			System.err.println("WARNING: basedir property is set system-wide. ORCA requires this property is NOT set. Clearing and proceeding.");
			System.clearProperty(PROPERTY_BASEDIR);
		}
		
		server = new Server();
		server.setStopAtShutdown(true);
		
		// the connector
		SelectChannelConnector connector = new SelectChannelConnector();
		connector.setPort(ServerPort);
		connector.setAcceptors(2);
		server.addConnector(connector);
		
		System.out.println("Checking ssl " + enableSsl);
		// SSL connector
		if (enableSsl) { 
			try {
				System.out.println("Enabling SSL with " + keystore);
				SslSelectChannelConnector sslConnector = new SslSelectChannelConnector();
				SSLContext sslContext = SSLContext.getInstance("TLS");
				char[] passphraseChar = passphrase.toCharArray();

				KeyStore keyStore=KeyStore.getInstance("JKS");
				keyStore.load(new FileInputStream(keystore),passphraseChar);

				String defaultAlgorithm=KeyManagerFactory.getDefaultAlgorithm();
				KeyManagerFactory keyFactory=KeyManagerFactory.getInstance(defaultAlgorithm);
				keyFactory.init(keyStore,passphraseChar);
				
				KeyStore trustStore = null;
				if (truststore != null) {
					trustStore = KeyStore.getInstance("JKS");
					trustStore.load(new FileInputStream(truststore),passphraseChar);
					
					defaultAlgorithm=TrustManagerFactory.getDefaultAlgorithm();
					TrustManagerFactory trustFactory=TrustManagerFactory.getInstance(defaultAlgorithm);
					trustFactory.init(trustStore);
					sslContext.init(keyFactory.getKeyManagers(), trustFactory.getTrustManagers(), null);
				} else
					sslContext.init(keyFactory.getKeyManagers(), null, null);

				sslConnector.setSslContext(sslContext);
				sslConnector.setPort(SSLServerPort);
				server.addConnector(sslConnector);
			} catch (Exception e) {
				System.err.println("Unable to configure SSL due to: " + e);
			}
		}
		
		// the handler
		ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SECURITY|ServletContextHandler.SECURITY);
		servletHandler.setContextPath("/orca");
		// initialize the spring application context
		XmlWebApplicationContext wctx = new XmlWebApplicationContext();
		wctx.setConfigLocation("");
		wctx.setServletContext(servletHandler.getServletContext());
		wctx.refresh();
		// store the context in the servlet context, so that spring will use it and we can find it
		servletHandler.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wctx);
		// add the spring-ws servlet
		MessageDispatcherServlet springws = new MessageDispatcherServlet();
		springws.setContextConfigLocation("classpath:/orca/shirako/orca-ws-context.xml");
		ServletHolder springwsHolder = new ServletHolder(springws);
		springwsHolder.setName("spring-ws");
		servletHandler.addServlet(springwsHolder, "/spring-services/*");
		// add the axis2 servlet
		ServletHolder axisHolder = new ServletHolder(new AxisServlet());
		axisHolder.setName("AxisServlet");
		servletHandler.addServlet(axisHolder, "/services/*");		
		
		// register the handler
		server.setHandler(servletHandler);
		
		// add the context listener so that we can perform some setup when the server starts and can also
		// shutdown orca when the server is about to stop
		OrcaServerContextListener l = new OrcaServerContextListener();
		servletHandler.addEventListener(l);
		// start the server
		server.start();
		// start Orca
		startOrca();
	}
	
	public void stop() throws Exception {
		server.stop();
	}
	
	static class OrcaServerContextListener implements ServletContextListener {
		public void contextInitialized(ServletContextEvent event) {
	        try {
	            Globals.Log.info("Orca application context initializing port=" + ServerPort + " ORCA_HOME=" + Globals.HomeDirectory);
	            // store the servlet context, so that we can access it later, if needed
	            ServletContext sc = event.getServletContext();
                Globals.ServletContext = sc;
                
                // Configure Axis2 and store the configuration so that it can be shared
                // by the Axis2 servlet and by our own deployment code.
                // We do this to ensure that the services we deploy can be found
                // by the Axis2 servlet.
                Globals.Log.info("Initializing the soap (axis2) container context");
                String root = Globals.HomeDirectory + "config/";
                String config = root + "axis2.xml";
                String repo = Globals.HomeDirectory + "axis2repository";
                
                Globals.Log.debug("Context soap (axis2) configuration file: " + config);
                Globals.Log.debug("Context soap (axis2) repository: " + repo);
                
                // store the config in the context, so that the axis2 servlet can use it
                ConfigurationContext context = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repo, config);
                context.setProperty(org.apache.axis2.Constants.CONTAINER_MANAGED, org.apache.axis2.Constants.VALUE_TRUE);
                sc.setAttribute(AxisServlet.CONFIGURATION_CONTEXT, context);

                // store the context in our service factory, so that we can use it
                SoapAxis2ServiceFactory.context = context;
                
                // note: we cannot start Orca here, because the server has not completed its initialization, yet.
            } catch (Exception e) {
                Globals.Log.error("An error occurred while initializing Orca", e);
                System.err.println(e.toString());
                System.exit(1);
            }
	    }

	    public void contextDestroyed(ServletContextEvent event) {
	    	Globals.Log.info("Jetty shutting down. Destroying Orca context");
	        Globals.stop();
	    }
	}

	public static void main(String[] args) throws Exception {
		OrcaServer s = new OrcaServer();
		s.start();
	}		
}
