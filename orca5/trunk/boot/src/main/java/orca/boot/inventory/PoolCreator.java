package orca.boot.inventory;

import java.util.Properties;
import java.util.StringTokenizer;

import orca.extensions.internal.Plugin;
import orca.manage.IOrcaContainer;
import orca.manage.Orca;
import orca.shirako.api.IClientReservation;
import orca.shirako.api.ISlice;
import orca.shirako.common.ConfigurationException;
import orca.shirako.common.SliceID;
import orca.shirako.common.UnitID;
import orca.shirako.common.meta.ResourcePoolDescriptor;
import orca.shirako.common.meta.ResourcePoolsDescriptor;
import orca.shirako.container.Globals;
import orca.shirako.core.PoolManager.CreatePoolResult;
import orca.shirako.plugins.config.AntConfig;
import orca.shirako.plugins.config.Config;
import orca.shirako.plugins.config.ConfigurationMapping;
import orca.shirako.plugins.substrate.AuthoritySubstrate;
import orca.shirako.util.Misc;
import orca.shirako.util.ResourceData;
import orca.util.PropList;

public class PoolCreator {
    protected AuthoritySubstrate substrate;
    protected ResourcePoolsDescriptor pools;
    protected String inventory;
    protected IOrcaContainer cont;
    
    public PoolCreator(AuthoritySubstrate substrate, ResourcePoolsDescriptor pools, String inventory) {
        this.substrate = substrate;
        this.pools = pools;
        this.inventory = inventory;
    }

    protected IResourcePoolFactory getFactory(ResourcePoolDescriptor rd) throws ConfigurationException {
        IResourcePoolFactory f = null;
        if (rd.getPoolFactory() == null) {
            f = new ResourcePoolFactory();
        } else {
            Globals.Log.info("Creating resource pool factory class=" + rd.getPoolFactory());
            try {
                f = (IResourcePoolFactory)Misc.createInstance(rd.getPoolFactory());
            } catch (Exception e) {
                throw new ConfigurationException("Could not instantiate class=" + rd.getPoolFactory(), e);
            }
            
        }
        f.setSubstrate(substrate);
        f.setDescriptor(rd);
        return f;
    }
    
    public void process() throws ConfigurationException {
    	cont = Orca.connect();
        // transfer all inventory to the actor
        transferInventory();
        // create each resource pool
        for (ResourcePoolDescriptor pool : pools) {
            // create the factory
            IResourcePoolFactory f = getFactory(pool);
            // obtain the final resource pool descriptor
            pool = f.getDescriptor();
            // save the resource pool on the resource properties list
            ResourceData rd = new ResourceData();
            pool.save(rd.getResourceProperties(), null);
            // save the resource pool properties onto the local properties list
            ResourceData.mergeProperties(pool.getPoolProperties(), rd.getLocalProperties());
            // create the resource pool
            CreatePoolResult r = substrate.getPoolManager().createPool(new SliceID(), pool.getResourceTypeLabel(), pool.getResourceType(), rd);
            if (r.code != 0) {
                throw new ConfigurationException("Could not create resource pool: " + pool.getResourceTypeLabel() + ". error=" + r.code);
            }
            // register the handler for this pool
            registerHandler(pool);            
            IClientReservation source = f.createSourceReservation(r.pool);
            try {
                substrate.getDatabase().addReservation(source);
            } catch (Exception e) {
                throw new ConfigurationException("Could not add source reservation to database", e);
            }
            // transfer resources to the pool
            transferToPool(pool, r.pool);
            // commit any changes made to the slice properties
            try {
                substrate.getPoolManager().updatePool(r.pool);
            } catch (Exception e){
                throw new ConfigurationException(e);
            }
        }
    }

    protected void transferInventory() throws ConfigurationException {
        if (inventory == null) {
            return;
        }

        StringTokenizer st = new StringTokenizer(inventory, ",");

        while (st.hasMoreTokens()) {
            String name = st.nextToken();
            if (!cont.transferInventory(new UnitID(name), substrate.getActor().getGuid())) {
                Globals.Log.error("Error while transferring inventory item (" + name + ") to site " + substrate.getActor().getName() + 
                		" :" + cont.getLastError());
            }
        }
    }

    protected void transferToPool(ResourcePoolDescriptor pool, ISlice slice) throws ConfigurationException {
        if (pool.getInventory() == null) {
            return;
        }
     
        StringTokenizer st = new StringTokenizer(pool.getInventory(), ",");

        while (st.hasMoreTokens()) {
            String name = st.nextToken();
            try {
                substrate.getSubstrateDatabase().transfer(new UnitID(name), slice.getSliceID());
            } catch (Exception e) {
                throw new ConfigurationException("Error while transferring inventory item: " + name  +" into pool: " + slice.getName(), e);
            }
        }
    }
                
    protected String getPathFromPlugin(ResourcePoolDescriptor pool, Properties out) throws ConfigurationException {
        if (pool.getHandlerPackageId() != null && pool.getHandlerPluginId() != null) {
            Plugin plugin = Globals.getContainer().getPluginManager().getPlugin(pool.getHandlerPackageId(), pool.getHandlerPluginId());

            if (plugin == null) {
                throw new ConfigurationException("No such plugin: " + pool.getHandlerPackageId() + ":" + pool.getHandlerPluginId());
            }

            if (!(plugin.getPluginType() == Plugin.TypeHandler)) {
                throw new ConfigurationException("Invalid plugin type: " + plugin.getPluginType());
            }

            /*
             * The handler MUST have a properties list.
             */
            Properties p = plugin.getConfigProperties();

            if (p == null) {
                throw new ConfigurationException("Missing configuration properties");
            }

            /*
             * Extract the handler file name.
             */
            String file = p.getProperty(Plugin.PluginPropertyHandlerFile);

            if (file == null) {
                throw new ConfigurationException("Missing handler file");
            }

            // make the mapping properties list as a clone of the current
            // properties list
            Properties clone = (Properties) p.clone();
            clone.remove(Plugin.PluginPropertyHandlerFile);
            PropList.mergeProperties(clone, out);
            return file;
        }
        return null;
    }

    // XXX: this should use the management layer API
    protected void registerHandler(ResourcePoolDescriptor pool) throws ConfigurationException {
        Properties p = new Properties();
        String path = pool.getHandlerPath();
        if (path == null) {
            path = getPathFromPlugin(pool, p);
        }
        if (path == null) {
            return;
        }

        Config config = substrate.getConfig();

        if (!(config instanceof AntConfig)) {
            throw new ConfigurationException("Unsupported config class: " + config.getClass().getCanonicalName());
        }

        AntConfig ac = (AntConfig) config;

        // create the mapping
        ConfigurationMapping map = new ConfigurationMapping();
        map.setKey(pool.getResourceType().toString());
        map.setConfigFile(path);

        /*
         * If the client passed properties, we need to merge them with the
         * existing properties. Properties can be passed either in
         * getConfigurationProperties() or in getConfigurationString()
         */
        PropList.mergeProperties(pool.getHandlerProperties(), p);
        // attach the properties
        map.setProperties(p);
        // add the mapping
        ac.addConfigMapping(map);
    }
}
