package shirako.util.tomcat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.catalina.loader.WebappClassLoader;

/**
 * Custom class loader to be used for loading Shirako classes and extension plugins from Tomcat. This class loader
 * consists of two components: an interface (IShirakoWebappClassLoader) and an implementation
 * (ShirakoWebappClassLoader). The implementation file should be resolvable by Tomcat's Catalina class loader, thus it
 * should reside in a jar inside Tomcat's server/lib folder. Since code within Shirako needs to be able to resolve the
 * interface, the interface should be resolvable by Tomcat's Common loader. Thus, the interface should be in a jar file
 * in common/lib. <br>
 * <br>
 * Note that shirako.war cannot include neither the implementation or the interface of this class loader. If the war
 * contains either of these two files, they will be loaded by the Webapps's loader and it will be impossible to perform
 * type casts of the loader instance as obtained by calling Thread.currentThread().getContextLoader(). <br>
 * <br>
 * To use this class loader for Shirako's web application, add the following to the context describing the application:
 * &lt;Loader loaderClass="shirako.util.tomcat.ShirakoWebappClassLoader" /&gt;
 */
public class ShirakoWebappClassLoader extends WebappClassLoader implements IShirakoWebappClassLoader {
    /**
     * Map of registered loaders
     */
    protected Hashtable<String, ShirakoURLClassLoader> loaders;
    /**
     * Map of resolved classes using the registered loaders class to loader
     */
    protected Hashtable<String, String> classes;
    /**
     * For each loader, a set of resolved classes derived from this loader
     */
    protected Hashtable<String, HashSet<String>> reverse;

    /**
     * Create a new instance
     */
    public ShirakoWebappClassLoader() {
        super();
        init();
    }

    /**
     * Create a new class loader using the specified loader as its parent
     * 
     * @param parent
     *            parent class loader to use
     */
    public ShirakoWebappClassLoader(ClassLoader parent) {
        super(parent);
        init();
    }

    /**
     * Initialization routines
     */
    private void init() {
        loaders = new Hashtable<String, ShirakoURLClassLoader>();
        classes = new Hashtable<String, String>();
        reverse = new Hashtable<String, HashSet<String>>();
    }

    /**
     * Registers the specified class loader
     * 
     * @param name
     *            key
     * @param loader
     *            loader
     */
    public int register(String name, ShirakoURLClassLoader loader) {
        int code = -1;
        synchronized (this) {
            if (!loaders.containsKey(name)) {
                loaders.put(name, loader);
                loader.setTopLoader(this);
                HashSet<String> set = new HashSet<String>();
                reverse.put(name, set);
                code = 0;
            }
        }
        return code;
    }

    /**
     * Unregisters the specified loader
     * 
     * @param name
     *            key
     */
    public int unregister(String name) {
        int code = -1;
        synchronized (this) {
            if (loaders.containsKey(name)) {
                HashSet<String> map = reverse.get(name);
                for (String s : map) {
                    classes.remove(s);
                }
                reverse.remove(name);
                loaders.remove(name);
                code = 0;
            }
        }
        return code;
    }

    /**
     * {@inheritDoc} This loader will load classes in the following order:
     * <ul>
     * <li>If the loader has loaded this class before using a registered loader, it will try to load the class using the
     * corresponding registered loader.
     * <li>If the class has not been loaded before using a registered loader or attempting to load it with the
     * corresponding registered loader failed, the base class loadClass() will be called.
     * <li>If super.loadClass() cannot load the class, registered loaders will be asked, one by one, to load the class.
     * If a loader returns a class definition, this loaders name will be recorded so that no search will be performed
     * next time the class loader is asked to load this class.
     * <li>If none of the above works, a ClassNotFoundException will be thrown
     * </ul>
     */
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // System.out.println("Resolving: " + name);
        boolean add = false;
        Class<?> result = null;
        String loaderName = null;
        synchronized (this) {
            loaderName = classes.get(name);
        }

        /*
         * We have remembered that this class was resolved from one of out loaders. Try to resolve the class using that
         * loader. If we fail, we will try searching for the class again.
         */
        if (loaderName != null) {
            ShirakoURLClassLoader loader = loaders.get(loaderName);
            if (loader != null) {
                try {
                    result = loader.loadClassSpecial(name);
                } catch (Exception e) {
                    /*
                     * The loader we thought knew how to load this class says it cannot do it anymore. This should never
                     * happen
                     */
                }
            }
        }

        if (result == null) {
            /*
             * Ask the base implementation to resolve this class.
             */
            try {
                result = super.loadClass(name, resolve);
            } catch (Exception e) {
            }

            if (result == null) {
                /*
                 * The base does not know how to load this class. Ask each loader in our list if it can load this class
                 */

                // holding the lock while doing this may be a problem!
                synchronized (this) {
                    Iterator i = loaders.entrySet().iterator();
                    while (result == null && i.hasNext()) {
                        Map.Entry<String, ShirakoURLClassLoader> entry = (Map.Entry<String, ShirakoURLClassLoader>) i
                                .next();
                        try {
                            // System.out.println("Attempting to load: " + name
                            // + " with loader: " + entry.getKey() + "(" +
                            // entry.getValue().getClass().getCanonicalName() +
                            // ")");
                            result = entry.getValue().loadClassSpecial(name);
                            if (result != null) {
                                // System.out.println("Succeeded loading: " +
                                // name + " with loader: " + entry.getKey() +
                                // "(" +
                                // entry.getValue().getClass().getCanonicalName()
                                // + ")");
                                loaderName = entry.getKey();
                                add = true;
                            }
                        } catch (Exception e) {
                            // System.out.println("Failed loading: " + name + "
                            // with loader: " + entry.getKey() + "(" +
                            // entry.getValue().getClass().getCanonicalName() +
                            // ")");
                        }
                    }
                }
            }
        }

        if (result != null) {
            if (loaderName != null && add) {
                synchronized (this) {
                    HashSet<String> map = reverse.get(loaderName);
                    if (map != null) {
                        map.add(name);
                        classes.put(name, loaderName);
                    }
                }
            }
            String n = "";
            ClassLoader temp = result.getClassLoader();
            if (temp != null) {
                n = temp.getClass().getCanonicalName();
            }
            // System.out.println("Resolved: " + name + " loader = " + n);
        }
        return result;
    }

    public final Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> result = null;
        Vector<URL> v = new Vector<URL>();

        result = super.getResources(name);
        if (result != null) {
            while (result.hasMoreElements()) {
                v.add(result.nextElement());
            }
        }

        synchronized (this) {
            Iterator i = loaders.entrySet().iterator();
            while (i.hasNext()) {
                try {
                    Map.Entry<String, ShirakoURLClassLoader> entry = (Map.Entry<String, ShirakoURLClassLoader>) i
                            .next();
                    Enumeration<URL> en = entry.getValue().getResourcesSpecial(name);
                    while (en.hasMoreElements()) {
                        v.add(en.nextElement());
                    }
                } catch (Exception e) {
                }
            }
        }

        return v.elements();
    }

    public URL getResource(String name) {
        URL result = null;

        result = super.getResource(name);

        if (result == null) {
            /*
             * The base does not know how to load this resource. Ask each loader in our list if it can load this
             * resource
             */

            // holding the lock while doing this may be a problem!
            synchronized (this) {
                Iterator i = loaders.entrySet().iterator();
                while (result == null && i.hasNext()) {
                    Map.Entry<String, ShirakoURLClassLoader> entry = (Map.Entry<String, ShirakoURLClassLoader>) i
                            .next();
                    result = entry.getValue().getResourceSpecial(name);
                }
            }
        }
        return result;
    }

    public InputStream getResourceAsStream(String name) {
        InputStream result = null;

        result = super.getResourceAsStream(name);

        if (result == null) {
            /*
             * The base does not know how to load this resource. Ask each loader in our list if it can load this
             * resource
             */

            // holding the lock while doing this may be a problem!
            synchronized (this) {
                Iterator i = loaders.entrySet().iterator();
                while (result == null && i.hasNext()) {
                    Map.Entry<String, ShirakoURLClassLoader> entry = (Map.Entry<String, ShirakoURLClassLoader>) i
                            .next();
                    result = entry.getValue().getResourceAsStreamSpecial(name);
                }
            }
        }
        return result;
    }

    public URL findResource(final String name) {
        URL result = null;

        result = super.findResource(name);

        if (result == null) {
            /*
             * The base does not know how to load this resource. Ask each loader in our list if it can load this
             * resource
             */

            // holding the lock while doing this may be a problem!
            synchronized (this) {
                Iterator i = loaders.entrySet().iterator();
                while (result == null && i.hasNext()) {
                    Map.Entry<String, ShirakoURLClassLoader> entry = (Map.Entry<String, ShirakoURLClassLoader>) i
                            .next();
                    result = entry.getValue().findResourceSpecial(name);
                }
            }
        }
        return result;
    }
}
