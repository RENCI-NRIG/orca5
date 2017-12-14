package shirako.util.tomcat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

public class ShirakoURLClassLoader extends URLClassLoader {
    /**
     * The container loader: requests to resolve a class are always redirected to the top loader. The top loader calls
     * us using loadClassSpecial() and we know that we should not redirect back to the top.
     */
    protected ClassLoader top = null;

    public ShirakoURLClassLoader(URL[] urls) {
        super(urls);
    }

    public void setTopLoader(ClassLoader top) {
        this.top = top;
    }

    /**
     * If someone calls this loader directly, we will delegate the request to the top loader (if defined). Otherwise, we
     * will proceed as usual.
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (top != null) {
            // System.out.println("loadClass called directly. Redirecting to top
            // loader");
            return top.loadClass(name);
        } else {
            // System.out.println("loadClass called directly but no top loader
            // defined");
            return loadClassSpecial(name);
        }
    }

    /**
     * New method used by ShirakoWebappClassLoader to delegate to this class loader.
     * 
     * @param name
     * @return
     * @throws ClassNotFoundException
     */
    public Class<?> loadClassSpecial(String name) throws ClassNotFoundException {
        // System.out.println("Top loader asked as to resolve: " + name);
        return super.loadClass(name);
    }

    public URL getResource(String name) {
        if (top != null) {
            // System.out.println("loadClass called directly. Redirecting to top
            // loader");
            return top.getResource(name);
        } else {
            // System.out.println("loadClass called directly but no top loader
            // defined");
            return getResourceSpecial(name);
        }
    }

    public URL getResourceSpecial(String name) {
        return super.getResource(name);
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        if (top != null) {
            // System.out.println("loadClass called directly. Redirecting to top
            // loader");
            return top.getResources(name);
        } else {
            // System.out.println("loadClass called directly but no top loader
            // defined");
            return getResourcesSpecial(name);
        }
    }

    public Enumeration<URL> getResourcesSpecial(String name) throws IOException {
        return super.getResources(name);
    }

    public InputStream getResourceAsStream(String name) {
        if (top != null) {
            // System.out.println("loadClass called directly. Redirecting to top
            // loader");
            return top.getResourceAsStream(name);
        } else {
            // System.out.println("loadClass called directly but no top loader
            // defined");
            return getResourceAsStreamSpecial(name);
        }
    }

    public InputStream getResourceAsStreamSpecial(String name) {
        return super.getResourceAsStream(name);
    }

    public URL findResource(final String name) {
        if (top != null) {
            // System.out.println("loadClass called directly. Redirecting to top
            // loader");
            return ((URLClassLoader) top).findResource(name);
        } else {
            // System.out.println("loadClass called directly but no top loader
            // defined");
            return findResourceSpecial(name);
        }
    }

    public URL findResourceSpecial(final String name) {
        return super.findResource(name);
    }

}
