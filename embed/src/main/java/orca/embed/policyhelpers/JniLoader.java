package orca.embed.policyhelpers;

import java.lang.SecurityException;
import java.lang.UnsatisfiedLinkError;
import org.apache.log4j.Logger;

/**
 * Use an instance of this class to load our JNI.
 *
 * @author Russell Bateman
 */

public class JniLoader {
    private static final Logger log = Logger.getLogger(JniLoader.class);
    private static String DEFAULT_HOMEPATH = System.getProperty("user.dir");
    private static String DEFAULT_PATHNAME = "lib";
    // private static String JNI_LIBRARY = "syscall";
    private static String LINUX_SUFFIX = ".so";
    private static String WINDOWS_SUFFIX = ".dll";
    private static String LINUX_SEPARATOR = "/";
    private static String WINDOWS_SEPARATOR = "\\";

    private String JNI_LIBRARY;

    /*
     * Obviously, these paths cannot go unmodified if running on Windows whose paths will be quite different.
     */
    private String jniHomepath = DEFAULT_HOMEPATH;
    private String jniPathname = DEFAULT_PATHNAME;
    private boolean libraryLoaded = false;

    public JniLoader(String libName) {
        JNI_LIBRARY = libName;
    }

    public JniLoader(String homepath, String jnipath) {
        this.jniHomepath = homepath;
        this.jniPathname = jnipath;
    }

    public final void setJniHomepath(String path) {
        this.jniHomepath = path;
    }

    public final void setJniPathname(String path) {
        this.jniPathname = path;
    }

    public String getJniPathname() {
        return this.jniPathname;
    }

    public String getJniHomepath() {
        return this.jniHomepath;
    }

    /**
     * Load the JNI if at all possible. This must be done prior to calling <tt>system()</tt>.
     * 
     * @return a description of any error that occurs (SystemNativeError)
     */

    public SystemNativeError loadJni() {
        int err = 0;
        String msg = null, add = null;
        String separator = null, prefix = null, suffix = null;
        SystemNativeError error = new SystemNativeError();
        OperatingSystem system = OperatingSystem.system();
        if (this.libraryLoaded) // (don't go load it if already loaded)
            return error;

        if (system == OperatingSystem.LINUX) {
            separator = LINUX_SEPARATOR;
            prefix = "lib";
            suffix = LINUX_SUFFIX;
        } else if (system == OperatingSystem.WINDOWS) {
            separator = WINDOWS_SEPARATOR;
            prefix = "";
            suffix = WINDOWS_SUFFIX;
        } else {
            separator = LINUX_SEPARATOR;
            prefix = "lib";
            suffix = LINUX_SUFFIX;
        }
        this.jniPathname = this.jniHomepath + separator + this.jniPathname + separator + prefix + JNI_LIBRARY + suffix;

        log.debug("Opening shared-object library on " + this.jniPathname);

        try {
            System.load(this.jniPathname);
        } catch (SecurityException e) {
            err = 1;
            msg = "security";
            add = e.getMessage();
        } catch (UnsatisfiedLinkError e) {
            err = 2;
            msg = "link error";
            add = e.getMessage();
        } catch (Throwable e) {
            err = 9;
            msg = "unknown";
            add = e.getMessage();
        }
        if (msg != null) {
            error.setErrno(err);
            error.setMessage(msg);
            error.setAdditional(add);
            log.error(error.getMessage());
            return error;
        }

        log.info("JNI loaded from " + this.jniPathname);

        this.libraryLoaded = true;
        return error;
    }
}
