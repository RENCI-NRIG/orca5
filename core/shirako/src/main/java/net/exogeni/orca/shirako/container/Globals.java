package net.exogeni.orca.shirako.container;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Properties;
import java.util.Timer;

import javax.servlet.ServletContext;

import net.exogeni.orca.security.AbacUtil;
import net.exogeni.orca.shirako.container.api.IActorContainer;
import net.exogeni.orca.shirako.container.api.IOrcaAdminConfiguration;
import net.exogeni.orca.shirako.container.api.IOrcaConfiguration;
import net.exogeni.orca.shirako.util.SemaphoreMap;
import net.exogeni.orca.util.ChangeClasspath;
import net.exogeni.orca.util.PathGuesser;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Globals {	
	public static final String PathSep = System.getProperty("file.separator");
	public static final String RootLoggerName = "orca";
	public static final String HomeDirectory = PathGuesser.getHomeDirectory();
	public static final String SuperblockLocation = HomeDirectory + "state_recovery.lock";
	public static final String ControllerLockLocation = HomeDirectory + "controller_recovery.lock";
	public static final String ConfigurationFile = HomeDirectory + "config" + PathSep + "orca.properties";
	public static final String ControllerConfigurationFile = HomeDirectory + "config" + PathSep + "controller.properties";
	public static final String TdbPersistentDirectory = HomeDirectory + PathSep + "modelState";
	public static final String TdbEphemeralDirectory = TdbPersistentDirectory + PathSep + "tmp";

	public static final Logger Log = makeLogger();
	public static final Timer Timer = new Timer("Globals.Timer", true);
	public static final EventManager eventManager = new EventManager();
	
	private static IActorContainer container;
	private static IOrcaConfiguration configuration;
	private static IOrcaAdminConfiguration adminConfiguration;
	private static Properties properties;
	private static Object globalLock = new Object();
	private static volatile boolean initialized;
	private static volatile boolean started;
	private static volatile boolean startCompleted;
	
	public static ServletContext ServletContext = null;
    
    // this is container-wide to allow atomic sequences of handler tasks
    public static final SemaphoreMap handlerSemaphoreMap = new SemaphoreMap();
    // .. and secure random number generation in the tasks
    public static final SecureRandom secureRandom = new SecureRandom();

	static {
		try {
			File f = new File(HomeDirectory);
			URL url = f.toURL();
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			ChangeClasspath.addURL(loader, url);
		} catch (Exception e) {
			Log.fatal("Could not initialize classpath", e);
			System.err.println("Could not initialize classpath: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		System.setProperty(AbacUtil.ABAC_ROOT, HomeDirectory);
	}

	public static void start() {
		start(false);
	}

	public static void stop() {
		try {
			synchronized (globalLock) {
				if (!started) {
					return;
				}
				Globals.Log.info("Stopping Orca");
				started = false;
				getContainer().shutdown();
			}
		} catch (Exception e) {
			Globals.Log.error("Error while shutting down: ", e);
		}
	}

	public static void start(boolean forceFresh) {
		try {
			synchronized (globalLock) {
				if (started) {
					return;
				}
				started = true;
				startCompleted = false;
				if (forceFresh) {
					deleteSuperblock();
				}
			}

			// initialize is now a public function, so must request globalLock for itself
			// in normal operation, Orca will already be initialized by OrcaServer
			initialize();

			synchronized (globalLock) {
				String cn = configuration.getProperty(IOrcaConfiguration.PropertyContainerManagerClass);
				if (cn == null) {
					throw new ContainerInitializationException(
							"Container class implementation is missing from the configuration file");
				}

				Log.info("Container implementation class name: " + cn);
				Class<?> c = Class.forName(cn);
				container = (IActorContainer) c.newInstance();
				Log.info("Successfully instantiated the container implementation.");
				Log.info("Initializing container");
				container.initialize(adminConfiguration);
				Log.info("Successfully initialized the container");		
				startCompleted = true;
			}
		} catch (ContainerInitializationException e) {
			fail(e);
		} catch (Exception e) {
			fail(e);
		}
	}

	private static void deleteSuperblock() {
		File ff = new File(Globals.SuperblockLocation);
		if (ff.exists()) {
			ff.delete();
		}
	}

	private static void fail(Exception e) {
		started = false;
		try {
			Log.fatal("Critical error: Orca failed to initialize", e);
		} catch (Exception ee){
		}
		System.err.println("Critical error: Orca failed to initialize");
		e.printStackTrace();
		System.err.println("Exiting...");
		System.exit(-1);
	}

	private static Logger makeLogger() {
		try {
			Properties p = new Properties();
			File logProps = new File(ConfigurationFile);
			if (logProps.exists()) {
				p.load(new FileInputStream(ConfigurationFile));
				p.setProperty("log4j.appender.file.File", HomeDirectory + "logs/orca.log");
				p.setProperty("log4j.appender.ndl.appender.File", HomeDirectory + "logs/ndl.log");
				PropertyConfigurator.configure(p);
			} else {
				logProps = new File(ControllerConfigurationFile);
				if (logProps.exists()) {
					p.load(new FileInputStream(ControllerConfigurationFile));
					p.setProperty("log4j.appender.file.File", HomeDirectory + "logs/controller.log");
					p.setProperty("log4j.appender.ndl.appender.File", HomeDirectory + "logs/ndl.log");
					PropertyConfigurator.configure(p);
				} else
					throw new Exception("Neither " + ConfigurationFile + " nor " + ControllerConfigurationFile + " could be found");
			} 
			// make sure everything sent to System.err and System.out is logged
			// System.setErr(new PrintStream(new LoggingOutputStream(Log,
			// Level.ERROR), true));
			// System.setOut(new PrintStream(new LoggingOutputStream(Log,
			// Level.INFO), true));
		} catch (Exception e) {
			System.err.println("Could not initialize log4j: " + e.getMessage());
		}

		return Logger.getLogger(RootLoggerName);
	}

	private static IOrcaAdminConfiguration makeAdminConfiguration()
			throws ContainerInitializationException {
		IOrcaAdminConfiguration conf = new OrcaAdminConfiguration();
		conf.initialize(properties);
		return conf;
	}

	private static void registerSecurityProvider() {
		Log.debug("Registering Bouncy Castle Security Provider");
		try {
			Security.addProvider(new BouncyCastleProvider());
		} catch (Exception e) {
			Log.error("Could not load Bouncy Castle as a security provider", e);
		}
	}

	/**
	 * Sets up many configurations, including via Properties file.
	 * Can be called separately, in order to access configurations.
	 * Alternatively, the Globals.start() method ensures that initialization has started.
	 *
	 * @throws ContainerInitializationException in case of error
	 */
	public static void initialize() throws ContainerInitializationException {
		synchronized (globalLock) {
			if (!initialized) {
				Log.info("Orca starting...");
				Log.info("Home directory: " + HomeDirectory);

				registerSecurityProvider();

				Log.info("Starting main container initialization...");
				try {
					loadConfiguration();
				} catch (Exception e) {
					Log.fatal("Container initialization failed", e);
					throw new ContainerInitializationException(e);
				}

				Log.info("Main container initialization complete.");


				adminConfiguration = makeAdminConfiguration();
				configuration = adminConfiguration.getConfiguration();

				initialized = true;
			}
		}
	}

	private static void loadConfiguration() throws FileNotFoundException, IOException {

		Log.info("Loading container configuration: path=" + ConfigurationFile);

		File f = new File(ConfigurationFile);
		if (!f.exists()) {
			Log.fatal("Container configuration file does not exist");
			throw new RuntimeException("Cannot access container.properties. Trying to find it at: "
					+ f.getAbsolutePath());
		}

		properties = new Properties();
		InputStream is = new FileInputStream(f);
		try {
			properties.load(is);
		} finally {
			is.close();
		}

		Log.info("Container configuration file loaded successfully.");
	}

	public static IActorContainer getContainer() {
		return container;
	}

	public static IOrcaAdminConfiguration getAdminConfiguration() {
		return adminConfiguration;
	}

	public static IOrcaConfiguration getConfiguration() {
		return configuration;
	}

	public static boolean isInsideServletContainer() {
		return ServletContext != null;
	}

	public static ServletContext getServletContext() {
		return ServletContext;
	}

	public static Logger getLogger(String name) {
		String temp = name;
		if (!temp.startsWith(RootLoggerName)) {
			temp = RootLoggerName + "." + name;
		}
		return Logger.getLogger(temp);
	}
	
	public static boolean isStartCompleted() {
		return startCompleted;
	}
	
	public static boolean isStarted() {
		return started;
	}
}
