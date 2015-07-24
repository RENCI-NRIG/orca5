package orca.shirako.core;

import java.util.Properties;
import java.util.Vector;

import orca.shirako.api.IActorIdentity;
import orca.shirako.api.IDatabase;
import orca.shirako.api.ISlice;
import orca.shirako.common.SliceID;
import orca.shirako.kernel.SliceFactory;
import orca.shirako.util.ResourceData;
import orca.util.ResourceType;

import org.apache.log4j.Logger;

// FIXME: why are we accessing the DB directly?

public class PoolManager {
    public class CreatePoolResult {
        public int code = ErrorNone;
        public ISlice pool;
    }

    public static final String UnassingedPoolName = "unassigned";
    public static final String PoolPrefix = "inventory";
    public static final int ErrorNone = 0;
    public static final int ErrorPoolExists = -10;
    public static final int ErrorTypeExists = -20;
    public static final int ErrorInvalidArguments = -30;
    public static final int ErrorDatabaseError = -40;
    public static final int ErrorInternalError = -50;
    
    protected IDatabase db;
    protected IActorIdentity identity;
    protected Logger logger;
    
    public PoolManager(IDatabase db, IActorIdentity identity, Logger logger) {
        if (db == null) {
            throw new IllegalArgumentException("db cannot be null");
        }
        if (identity == null) {
            throw new IllegalArgumentException("identity cannot be null");
        }
        if (logger == null) {
            throw new IllegalArgumentException("logger");
        }
        this.db = db;
        this.identity = identity;
        this.logger = logger;
    }
    
    /**
     * Create a new resource pool
     * @param name Pool name
     * @param type Resource type
     * @param p Resource properties
     * @return
     */
    public synchronized CreatePoolResult createPool(SliceID sliceID, String name, ResourceType type, ResourceData rd) {
        CreatePoolResult result = new CreatePoolResult();

        if ((sliceID == null) || (name == null) || (type == null)) {
            result.code = ErrorInvalidArguments;

            return result;
        }

        try {
            /*
             * Check that there is a pool with this name.
             */
            Vector<Properties> temp = db.getSlice(sliceID);

            if ((temp != null) && (temp.size() > 0)) {
                result.code = ErrorPoolExists;

                return result;
            }

            /*
             * Check if there is a pool with this resource type. For now we will
             * do it very it by scanning all inventory slices. Later, we should
             * introduce a new call to the db interface to obtain the pool for
             * the given resource type.
             */
            temp = db.getInventorySlices();

            if ((temp != null) && (temp.size() > 0)) {
                for (Properties props : temp) {
                    ISlice slice = SliceFactory.createInstance(props);

                    ResourceType rt = slice.getResourceType();
                    assert rt != null;

                    if (type.equals(rt)) {
                        result.code = ErrorTypeExists;

                        return result;
                    }
                }
            }

            ISlice slice = SliceFactory.getInstance().create(sliceID, name, (ResourceData)rd.clone());
            slice.setInventory(true);
            slice.setOwner(identity.getIdentity());
            slice.setResourceType(type);

            try {
                db.addSlice(slice);
                result.pool = slice;
            } catch (Exception e) {
                result.code = ErrorDatabaseError;
            }
        } catch (Exception e) {
            result.code = ErrorInternalError;
        }

        return result;
    }

    public void removePool(SliceID poolID, ResourceType type) throws Exception {
        Vector<Properties> temp = db.getSlice(poolID);

        if ((temp != null) && (temp.size() > 0)) {
            ISlice slice = SliceFactory.createInstance(temp.get(0));

            if (!slice.isInventory() || !type.equals(slice.getResourceType())) {
                throw new Exception("Invalid arguments");
            }

            db.removeSlice(poolID);
        }
    }
    
    public void updatePool(ISlice slice) throws Exception {
        try {
            db.updateSlice(slice);
        } catch (Exception e) {
            throw new Exception("Could not update slice", e);
        }
    }
}
