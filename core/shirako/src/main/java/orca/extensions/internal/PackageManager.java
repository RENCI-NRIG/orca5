/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.extensions.internal;

import java.io.*;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import orca.extensions.PackageId;
import orca.extensions.PluginId;
import orca.extensions.internal.db.IPackageDatabase;
import orca.shirako.container.ContainerConstants;
import orca.shirako.container.Globals;
import orca.util.ChangeClasspath;
import orca.util.ExecutionResult;
import orca.util.ScriptExecutor;
import shirako.util.tomcat.IShirakoWebappClassLoader;
import shirako.util.tomcat.ShirakoURLClassLoader;

/**
 * The PackageManager is responsible for managing a collection of extension
 * packages. This class offers functions to install/upgrade/uninstall an
 * extension package. The class does not maintain any state in memory: all state
 * is kept inside the container database.
 */
public class PackageManager {
    public static final String PackageManagerStateFile = "pkgmgr.state";
    /**
     * Location for web content (relative to ContainerManager.getPathPrefix).
     */
    public static final String PackagesWebRoot = "/../packages";

    /**
     * Handlers installation directory: relative to ContainerManager.getRoot()).
     */
    public static final String HandlersRoot = "/handlers";
    /**
     * Directory within a package that contains configuration handlers.
     */
    public static final String PackageHandlersDir = "/handlers";
    /**
     * Directory within a package that contains dependent jar files.
     */
    public static final String PackageLibDir = "/lib";

    /**
     * Directory within a package that contains web content.
     */
    public static final String PackageWebDir = "/web";

    /**
     * Location for storing package data on disk (relative to
     * ContainerManager.getPathPrefix()).
     */
    public static final String PackagesRootDir = "/packages";

    /**
     * Location for storing installed packages (relative to PackagesRootDir).
     */
    public static final String PackagesDir = "/pkg";

    /**
     * Temporary directory for package-related operations (relative to
     * PackagesRootDir).
     */
    public static final String PackagesTempDir = "/tmp";

    /**
     * Prefix used for creating temporary files.
     */
    public static final String TempPrefix = "package";

    /**
     * Name of the package descriptor file (relative to the package install
     * directory).
     */
    public static final String PackageDescriptorFile = "package.xml";

    /*
     * Error codes
     */
    public static final int ErrorCannotExpand = -1;
    public static final int ErrorPackageAlreadyInstalled = -2;
    public static final int ErrorPackagePendingOperation = -3;
    public static final int ErrorInternalError = -4;

    /**
     * Returns the relative package installation folder (relative to
     * ContainerManager.getPathPrefix()).
     * @param id package identifier
     * @return relative folder path
     */
    public static String getPackageRelativeRootFolder(PackageId id) {
        return PackagesRootDir + PackagesDir + "/" + id.toString();
    }

    /**
     * Returns the location of the handlers folder.
     * @return
     */
    public static String getHandlersFolder() {
        return Globals.HomeDirectory + HandlersRoot;
    }

    /**
     * Returns the absolute installation folder for the specified package. Every
     * package is installed under: PackagesDir/PackageId.
     * @param id package identifier
     * @return absolute folder path
     */
    public static String getPackageRootFolder(PackageId id) {
        return Globals.HomeDirectory + PackagesRootDir + PackagesDir + "/" + id.toString();
    }

    /**
     * Returns the absolute web folder for the specified package
     * @param id absolute web folder path
     */
    public static String getPackageWebRootFolder(PackageId id) {
        return Globals.HomeDirectory + PackagesWebRoot + "/" + id.toString();
    }

    /**
     * The package datbase.
     */
    protected IPackageDatabase database;

    /*
     * ========================================================================
     * Initialization
     * ========================================================================
     */

    /**
     * List of ids for packages that have a pending operation on them. Helps
     * with synchronization: if a package's id is in this list, an operations
     * concerning this package is already in progress and no other operation
     * affecting this package can proceed.
     */
    protected HashSet<PackageId> pending;

    /*
     * ========================================================================
     * Locating content
     * ========================================================================
     */

    /**
     * Initialization status
     */
    private boolean initialized = false;

    /**
     * Creates a new instance of the PackageManager
     */
    public PackageManager() {
        pending = new HashSet<PackageId>();
    }

    /**
     * Copies the temporary folder of the expanded package to its permanent
     * location
     * @param folder Temporary folder containing the expanded package
     * @param id Package identifier
     * @return
     * @throws Exception
     */
    protected boolean copyTempDirectory(String folder, PackageId id) throws Exception {
        int code = 0;
        File dest = new File(getPackageRootFolder(id));

        // we are trying to delete something under the packages root
        String command = "rm -rf " + dest.getAbsolutePath() + ";cp -r " + folder + "/ " + dest.getAbsolutePath();
        ScriptExecutor exec = new ScriptExecutor(new String[] { "bash", "-c", command });
        ExecutionResult r = exec.execute();

        code = r.code;
        if (code == 0) {
            // copy web content
            File f = new File(getPackageRootFolder(id) + PackageWebDir);
            if (f.exists() && f.isDirectory()) {
                if (f.getAbsolutePath().contains("WEB-INF")) {
                    command = "rm -rf " + getPackageWebRootFolder(id) + ";mv " + f.getAbsolutePath() + " " + getPackageWebRootFolder(id);
                    exec = new ScriptExecutor(new String[] { "bash", "-c", command });
                    r = exec.execute();
                    code = r.code;
                }
            }
            // copy handlers
            f = new File(getPackageRootFolder(id) + PackageHandlersDir);
            if (f.exists() && f.isDirectory()) {
                command = "cp -r " + f.getAbsolutePath() + "/. " + getHandlersFolder();
                exec = new ScriptExecutor(new String[] { "bash", "-c", command });
                r = exec.execute();
                code = r.code;
            }
        }

        return (code == 0);
    }

    /*
     * ========================================================================
     * Package life-cycle: install/upgrade/uninstall
     * ========================================================================
     */

    /**
     * Ensures that the top level directories exist. Attempts to create any
     * missing directory. Throws an exception if a failure occurs.
     */
    protected synchronized void createDirs() throws Exception {
        File pack = new File(Globals.HomeDirectory + PackagesRootDir);

        if (!pack.exists()) {
            if (!pack.mkdir()) {
                throw new Exception("Cannot create package root dir: " + pack.getAbsolutePath());
            }
        }

        File tempDir = new File(pack.getAbsolutePath() + PackagesTempDir);

        if (!tempDir.exists()) {
            if (!tempDir.mkdir()) {
                throw new Exception("Cannot create package temp directory: " + tempDir.getAbsolutePath());
            }
        }

        File pkgDir = new File(pack.getAbsolutePath() + PackagesDir);

        if (!pkgDir.exists()) {
            if (!pkgDir.mkdir()) {
                throw new Exception("Cannot create package pkg directory: " + pkgDir.getAbsolutePath());
            }
        }

        File handlers = new File(getHandlersFolder());
        if (!handlers.exists()) {
            if (!handlers.mkdir()) {
                throw new Exception("cannot create handlers directory:" + handlers.getAbsolutePath());
            }
        }
    }

    /**
     * Deletes (recursively) the specified directory
     * @param folder
     * @return
     * @throws Exception
     */
    protected boolean deleteDirectory(String folder) throws Exception {
        boolean result = false;

        File test = new File(Globals.HomeDirectory+ PackagesRootDir);
        String testPath = test.getAbsolutePath();
        File folderFile = new File(folder);
        String folderPath = folderFile.getAbsolutePath();

        /*
         * Be careful here: we do not want to delete any directory. Check to
         * make sure that the directory we are deleting is under our control.
         */
        if (folderPath.startsWith(testPath)) {
            // we are trying to delete something under the packages root
            String command = "rm -rf " + folderPath;
            ScriptExecutor exec = new ScriptExecutor(command);
            ExecutionResult r = exec.execute();

            if (r.code == 0) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Expands the specified package to a temporary directory
     * @param bytes
     * @return On success, the path to the directory where the package is
     *         installed.
     * @throws Exception
     */
    protected String expandPackage(byte[] bytes) throws Exception {
        String result = null;

        createDirs();

        File tempDir = new File(Globals.HomeDirectory + PackagesRootDir + PackagesTempDir);
        File folder = File.createTempFile(TempPrefix, "", tempDir);

        // this actually creates the file.
        // now make the file
        if (!(folder.delete() && folder.mkdir())) {
            throw new Exception("Cannot create package temp directory: " + folder.getAbsolutePath());
        }

        File temp = File.createTempFile("install", ".tar.gz", tempDir);

        try {
            FileOutputStream fs = new FileOutputStream(temp);
            fs.write(bytes);
            fs.close();
        } catch (Exception e) {
            // delete the temp directory
            deleteDirectory(folder.getAbsolutePath());
            throw new Exception("Cannot write package to disk", e);
        }

        int code = 0;

        try {
            String fileName = temp.getAbsolutePath();
            String command = "tar xvfz " + fileName + " -C " + folder.getAbsolutePath();
            ScriptExecutor exec = new ScriptExecutor(command);
            ExecutionResult r = exec.execute();
            code = r.code;
        } catch (Exception e) {
            code = -1;
        } finally {
            // delete the tar.gz file
            temp.delete();
        }

        if (code < 0) {
            // delete the temp directory
            deleteDirectory(folder.getAbsolutePath());
        } else {
            result = folder.getAbsolutePath();
        }

        return result;
    }

    /**
     * Creates a java.util.Properties from data read from the package XML
     * descriptor
     * @param mng
     * @return
     */
    protected Properties fill(orca.extensions.beans.Properties mng) {
        Properties p = null;

        if (mng != null) {
            List<orca.extensions.beans.Property> props = mng.getProperty();

            if (props != null) {
                p = new Properties();

                for (orca.extensions.beans.Property property : props) {
                    p.setProperty(property.getName(), property.getValue());
                }
            }
        }

        return p;
    }

    /*
     * ========================================================================
     * Helper functions
     * ========================================================================
     */

    /**
     * Creates a manage.extensions.ExtensionPackage object from data read from
     * the package XML descriptor.
     * @param pkg
     * @return
     */
    protected ExtensionPackage getPackage(orca.extensions.beans.ExtensionPackage pkg) {
        ExtensionPackage temp = new ExtensionPackage();
        temp.setId(new PackageId(pkg.getId()));
        temp.setName(pkg.getName());
        temp.setDescription(pkg.getDescription());

        return temp;
    }

    /**
     * Retrieves information for the specified package.
     * @param id Package identifier
     * @return
     */
    public ExtensionPackage getPackage(PackageId id) {
        ExtensionPackage result = null;

        try {
            Vector<Properties> v = database.getPackage(id);

            if ((v != null) && (v.size() > 0)) {
                Properties p = (Properties) v.get(0);
                result = new ExtensionPackage();
                result.reset(p);
            }
        } catch (Exception e) {
            Globals.Log.error("getPackageMng", e);
        }

        return result;
    }

    /**
     * Retrieves information about installed packages suitable to be passed back
     * from the management interface
     * @return
     */
    public ExtensionPackage[] getPackages() {
        ExtensionPackage[] result = null;

        try {
            Vector<Properties> v = database.getPackages();

            if ((v != null) && (v.size() > 0)) {
                result = new ExtensionPackage[v.size()];

                for (int i = 0; i < v.size(); i++) {
                    Properties p = (Properties) v.get(i);
                    result[i] = new ExtensionPackage();
                    result[i].reset(p);
                }
            }
        } catch (Exception e) {
            Globals.Log.error("getPackageMng", e);
        }

        return result;
    }

    /**
     * Creates a manage.extension.Plugin object from data read from the package
     * XML descriptor
     * @param pkg
     * @param p
     * @return
     */
    protected Plugin getPlugin(ExtensionPackage pkg, orca.extensions.beans.Plugin p) {
        Plugin plugin = new Plugin();
        plugin.setClassName(p.getClassName());
        plugin.setConfigProperties(fill(p.getConfigurationProperties()));
        plugin.setConfigTemplate(p.getConfigurationTemplate());
        plugin.setDescription(p.getDescription());
        plugin.setFactory(p.isFactory());
        plugin.setId(new PluginId(p.getId()));
        plugin.setName(p.getName());
        plugin.setPackageId(pkg.getId());
        plugin.setPluginType(p.getType());
        plugin.setPortalLevel(p.getLevel());
        plugin.setActorType(p.getActorType());

        return plugin;
    }

    /**
     * initializes the package manager. Called by the management layer.
     * Important: some parts of the maangement layer may not be initialized at
     * the time this method is invoked, e.g., when we create a fresh new Shirako
     * instance, which does not have a database attached to it in the beginning.
     * @param database
     * @throws Exception
     */
    public synchronized void initialize(IPackageDatabase database) throws Exception {
        if (database == null) {
            throw new IllegalArgumentException("database cannot be null");
        }
        
        if (!initialized) {
            this.database = database;
            if (!Globals.getContainer().isFresh()){
                recover();
            }
            initialized = true;
        }
    }

    /**
     * Installs the specified package.
     * @param bytes package contents as a byte array
     * @param failIfExists true: if the package exists, fail the installation
     * @return result code
     * @throws Exception
     */
    public int installPackage(byte[] bytes, boolean failIfExists) {
        int code = 0;
        boolean addedToPending = false;
        String folder = null;
        PackageId id = null;

        try {
            folder = expandPackage(bytes);

            if (folder == null) {
                code = ErrorCannotExpand;
            } else {
            	orca.extensions.beans.ExtensionPackage desc = readPackageDescriptor(folder);
                id = new PackageId(desc.getId());

                /*
                 * Check for pending operations for this package
                 */
                synchronized (this) {
                    if (pending.contains(id)) {
                        /*
                         * This package has a pending operation
                         */
                        code = ErrorPackagePendingOperation;
                    } else {
                        addedToPending = true;
                        pending.add(id);
                    }
                }

                boolean packageExists = false;
                if (code == 0) {
                    /*
                     * Check if this package is installed
                     */
                    ExtensionPackage dbPackage = getPackage(id);

                    if (dbPackage != null) {
                        packageExists = true;
                        if (failIfExists){
                            code = ErrorPackageAlreadyInstalled; // package is
                            // installed;
                        }
                    }
                }

                if (code == 0) {
                    // copy from temp to pkg
                    if (!copyTempDirectory(folder, id)) {
                        throw new Exception("Cannot copy package to destination directory");
                    }

                    // run the install script if present
                    int exitCodeInstall = runInstallScript(folder, id);

                    if (exitCodeInstall != 0) {
                        deleteDirectory(getPackageRootFolder(id));
                        throw new Exception("install script failed. exit code: " + exitCodeInstall);
                    }

                    if (!packageExists) {
                        // add the database record
                        registerPackage(desc);
                    }
                    // register the class loader
                    registerClassLoader(id);
                }
            }
        } catch (Exception e) {
            Globals.Log.error("installPackage", e);
            code = ErrorInternalError;
        } finally {
            if (addedToPending) {
                synchronized (this) {
                    pending.remove(id);
                }
            }

            if (folder != null) {
                try {
                    deleteDirectory(folder);
                } catch (Exception e) {
                    Globals.Log.error("deleteFolder", e);
                }
            }
        }

        return code;
    }

    /**
     * Reads the package descriptor of the package expanded in the specified
     * folder
     * @param folder
     * @return
     * @throws Exception
     */
    protected orca.extensions.beans.ExtensionPackage readPackageDescriptor(String folder) throws Exception {
        String file = folder + "/" + PackageDescriptorFile;
        InputStream is = new FileInputStream(file);
        JAXBContext context = JAXBContext.newInstance("orca.extensions.beans");
        Unmarshaller um = context.createUnmarshaller();

        return (orca.extensions.beans.ExtensionPackage) um.unmarshal(is);
    }

    /**
     * Called at boot time to register the classes supplied by each already
     * installed package with the class loader.
     */
    protected void registerClasses() {
        Globals.Log.debug("Registering class loaders for installed packages");

        try {
            Vector<Properties> v = database.getPackages();

            if ((v != null) && (v.size() > 0)) {
                for (int i = 0; i < v.size(); i++) {
                    try {
                        Properties p = v.get(i);
                        ExtensionPackage pkg = new ExtensionPackage();
                        pkg.reset(p);

                        registerClassLoader(pkg.getId());
                    } catch (Exception e) {
                        Globals.Log.error("Failed to register class loader for extension package: " + v.get(i).getProperty(ExtensionPackage.PropertyId), e);
                    }
                }
            }
        } catch (Exception e) {
            Globals.Log.error("getPackageMng", e);
        }
    }

    /**
     * Creates and registers a class loader for the specified package. The
     * created class loader provides access to all jar files inside the
     * package's lib directory.
     * @param id
     * @throws Exception
     */
    protected void registerClassLoader(PackageId id) throws Exception {
        if (Globals.Log.isDebugEnabled()) {
            Globals.Log.debug("Registering class loader for extension package: " + id);
        }
        
        Vector<URL> urls = new Vector<URL>();
        File f = new File(getPackageRootFolder(id) + PackageLibDir);

        if (f.exists() && f.isDirectory()) {
            File[] files = f.listFiles();

            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isFile()) {
                        URL url = files[i].toURL();
                        urls.add(url);
                    }
                }
            }
        }

        if (urls.size() > 0) {
            URL[] arr = new URL[urls.size()];
            urls.copyInto(arr);

            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            if (loader instanceof IShirakoWebappClassLoader) {
                ShirakoURLClassLoader packageLoader = new ShirakoURLClassLoader(arr);
                ((IShirakoWebappClassLoader) Thread.currentThread().getContextClassLoader()).register(id.toString(), packageLoader);
            } else {
                // attempt a drastic hack: we will not be able to
                // upgrade/unistall this package properly
                // if (!id.equals(new
                // PackageId("63566d10-9eb1-11db-b606-0800200c9a66"))) {
                for (int i = 0; i < arr.length; i++) {
                    ChangeClasspath.addURL(loader, arr[i]);
                }

                // }
            }
        }
        if (Globals.Log.isDebugEnabled()) {
            Globals.Log.debug("Registering class loader for extension package: " + id + " complete");
        }
    }

    /**
     * Adds the package to the database and registers all plugins defined in
     * this package with the plugin manager
     * @param pkg
     * @throws Exception
     */
    protected void registerPackage(orca.extensions.beans.ExtensionPackage pkg) throws Exception {
        /*
         * Add the package record
         */
        ExtensionPackage temp = getPackage(pkg);
        database.addPackage(temp);

        /*
         * Register every plugin with the plugin manager
         */
        orca.extensions.beans.Plugins plugins = pkg.getPlugins();
        List<orca.extensions.beans.Plugin> list = plugins.getPlugin();

        if (list != null) {
            for (orca.extensions.beans.Plugin p : list) {
                Plugin plugin = getPlugin(temp, p);
                Globals.getContainer().getPluginManager().register(plugin);
            }
        }
    }

    /**
     * Runs the package installation script (if present)
     * @param folder
     * @param id
     * @return
     * @throws Exception
     */
    protected int runInstallScript(String folder, PackageId id) throws Exception {
        int code = 0;
        File inst = new File(getPackageRootFolder(id) + "/install.sh");

        if (inst.exists()) {
            String command = "chmod u+x " + inst.getAbsolutePath();
            ScriptExecutor exec = new ScriptExecutor(command);
            ExecutionResult result = exec.execute();
            code = result.code;

            if (code == 0) {
                command = inst.getAbsolutePath() + " " + getPackageRootFolder(id);
                exec = new ScriptExecutor(command);
                result = exec.execute();
                code = result.code;
            }
        }

        return code;
    }

    /**
     * Uninstalls the specified package
     * @param id Package identifier
     * @return
     * @throws Exception
     */
    public int uninstallPackage(PackageId id) throws Exception {
        int code = 0;
        boolean addedToPending = false;

        try {
            // obtain the lock
            synchronized (this) {
                if (pending.contains(id)) {
                    code = -2;
                } else {
                    pending.add(id);
                    addedToPending = true;
                }

                if (code == 0) {
                    // check if the package is actually installed
                    ExtensionPackage p = getPackage(id);

                    if (p == null) {
                        code = -4;
                    } else {
                        // remove from the database
                        unregisterPackage(id);
                        // unregister the class loader
                        unregisterClassLoader(id);

                        // delete the package folder
                        if (!deleteDirectory(getPackageRootFolder(id))) {
                            Globals.Log.error("could not remove package directory: " + getPackageRootFolder(id));
                        }
                    }
                }
            }
        } finally {
            if (addedToPending) {
                synchronized (this) {
                    pending.remove(id);
                }
            }
        }

        return code;
    }

    /**
     * Unregisters the class loader for the specified package
     * @param id
     */
    protected void unregisterClassLoader(PackageId id) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader instanceof IShirakoWebappClassLoader)
            ((IShirakoWebappClassLoader) loader).unregister(id.toString());
    }

    /**
     * Removes the package from the database and unregisters all package
     * supplied plugins
     * @param id
     * @throws Exception
     */
    protected void unregisterPackage(PackageId id) throws Exception {
        Globals.getContainer().getPluginManager().unregister(id);
        database.removePackage(id);
    }

    /**
     * Upgrades the specified package.
     * @param id Package identifier
     * @param bytes Contents of the new package
     * @return
     * @throws Exception
     */
    public int upgradePackage(PackageId id, byte[] bytes) throws Exception {
        int code = 0;
        code = uninstallPackage(id);

        if (code == 0) {
            code = installPackage(bytes, true);
        }

        return code;
    }
    
    protected void recover() throws Exception {
        try {
            Globals.Log.info("Recovering package manager");
            File state = new File(Globals.HomeDirectory + PackagesRootDir + "/" + PackageManagerStateFile);
            if (state.exists()) {
                Globals.Log.info("Package manager state file exists. No need to expand packages.");
                registerClasses();
            }else {
                Globals.Log.info("Package manager state file does not exist. This is probably a redeploy. Need to expand packages");
                // note: also performs registration
                expandPackages();
            }
        }catch (Exception e) {
            Globals.Log.error("An error occurred while recovering package manager", e);
            throw e;
        }               
    }
    
    
    public void expandPackages() throws Exception {
        Globals.Log.info("Starting packages expansion");
        File f = new File(Globals.HomeDirectory + ContainerConstants.PackageStartupFolder);
        Globals.Log.debug("Package startup directory is: " + f.getAbsolutePath());
        
        createStateFile();
        
        if (f.exists() && f.getCanonicalFile().isDirectory()) {
            File[] packages = f.listFiles();

            if (packages != null) {
                if (Globals.Log.isDebugEnabled()) {
                    Globals.Log.debug("Found " + packages.length + " packages");
                }

                for (int i = 0; i < packages.length; i++) {
                    File p = packages[i];

                    if (p.isFile()) {
                        int size = (int) p.length();
                        byte[] buffer = new byte[size];
                        FileInputStream fs = new FileInputStream(packages[i]);
                        fs.read(buffer);
                        installPackage(buffer, false);
                    }
                }
            }
        } else {
            Globals.Log.debug("Startup folder does not exist. No packages need to be expanded.");
        }
    }
    
    protected void createStateFile() throws Exception {
        Globals.Log.info("Creating package manager state file");
        createDirs();
        File state = new File(Globals.HomeDirectory + PackagesRootDir + "/" + PackageManagerStateFile);
        state.createNewFile();
    }

    /**
     * Installs packages during container bootstrap.
     */
    public void installPackages() throws Exception
    {
        try {
            Globals.Log.info("Starting package installation");
            
            createStateFile();
                
            File f = new File(Globals.HomeDirectory + ContainerConstants.PackageStartupFolder);
            Globals.Log.debug("Package directory is: " + f.getAbsolutePath());
            
            if (f.exists() && f.isDirectory()) {
                File[] packages = f.listFiles();

                if (packages != null) {
                    for (int i = 0; i < packages.length; i++) {
                        File p = packages[i];

                        if (p.isFile() && p.getName().endsWith("tar.gz")) {
                            Globals.Log.info("Found package file: " + p.getAbsolutePath());
                            byte[] buffer = null;
                            try {
                                // FIXME: not great: reads the whole package in memory into a large array!!!
                                int size = (int) p.length();
                                buffer = new byte[size];
                                FileInputStream fs = new FileInputStream(packages[i]);
                                fs.read(buffer);
                            } catch(IOException e) {
                                Globals.Log.error("Could not read package file " + p.getAbsolutePath(), e);
                                buffer = null;
                            }
                            if (buffer != null) {
                                int code = installPackage(buffer, true);
                                if (code == 0) {
                                    Globals.Log.info("Successfully installed package file " + p.getAbsolutePath());
                                }else if (code == ErrorPackageAlreadyInstalled) {
                                    Globals.Log.info("Package file " + p.getAbsolutePath() + " is already installed");
                                }else {
                                    Globals.Log.error("Could not install package file " + p.getAbsolutePath() + " error=" + code);
                                }
                            }
                        }
                    }
                }
            } else {
                Globals.Log.debug("Startup folder does not exist");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Globals.Log.error("installPackages", e);
        }
    }
}
