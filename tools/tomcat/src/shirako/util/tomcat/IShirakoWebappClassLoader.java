package shirako.util.tomcat;

/**
 * Class loader interface for the custom Tomcat class loader. This interface
 * allows us to access the custom class loader, as it deals with restrictions
 * that Tomcat imposes on class loaders. See {@link ShirakoWebappClassLoader}.
 */
public interface IShirakoWebappClassLoader
{
    /**
     * Register a class loader
     * @param name key for this loader
     * @param loader loader instance
     * @return 0 on success, -1 if there is already a loader registered under
     *         the specified key
     */
    public int register(String name, ShirakoURLClassLoader loader);

    /**
     * Unregister a class loader
     * @param name key of the loader
     * @return 0 on success, -1 if there is no loader registered under the
     *         specified key
     */
    public int unregister(String name);
}
