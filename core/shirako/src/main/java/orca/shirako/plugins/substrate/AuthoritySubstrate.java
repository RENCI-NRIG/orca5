package orca.shirako.plugins.substrate;

import java.util.Properties;
import java.util.Vector;

import orca.shirako.api.IAuthority;
import orca.shirako.api.ISlice;
import orca.shirako.core.Actor;
import orca.shirako.core.PoolManager;
import orca.shirako.core.Unit;
import orca.shirako.core.UnitSet;
import orca.shirako.core.Units;
import orca.shirako.kernel.ResourceSet;
import orca.shirako.plugins.config.Config;
import orca.shirako.util.ResourceData;
import orca.util.OrcaException;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.PersistenceUtils;

public class AuthoritySubstrate extends Substrate {
    @NotPersistent
    protected PoolManager poolManager;
    @NotPersistent
    private boolean initialized;

    public AuthoritySubstrate() {
    }

    public AuthoritySubstrate(Actor actor, ISubstrateDatabase db, Config config) {
        super(actor, db, config);
    }

    public void initialize() throws OrcaException {
        if (!initialized) {
            super.initialize();
            poolManager = new PoolManager(getDatabase(), actor, getLogger());
            initialized = true;
        }
    }

    public PoolManager getPoolManager() {
        return poolManager;
    }

    @Override
    public void revisit(ISlice slice) throws OrcaException {
        if (slice.isInventory()) {
            recoverInventorySlice(slice);
        }
    }

    protected void recoverInventorySlice(ISlice slice) throws OrcaException {
        try {
            ResourceType type = slice.getResourceType();
            UnitSet uset = getUnits(slice);
            ResourceData rd = new ResourceData();
            ResourceData.mergeProperties(slice.getResourceProperties(), rd.getResourceProperties());
            ResourceData.mergeProperties(slice.getLocalProperties(), rd.getLocalProperties());
            ResourceSet rset = new ResourceSet(uset, type, rd);
            ((IAuthority) actor).donate(rset);
        } catch (Exception e) {
            throw new OrcaException(e);
        }
    }

    protected UnitSet getUnits(ISlice s) throws Exception {
        UnitSet set = null;
        Vector<Properties> v = ((ISubstrateDatabase) db).getInventory(s.getSliceID());
        if (v != null) {
            Units us = new Units();
            for (int i = 0; i < v.size(); i++) {
                Properties p = (Properties) v.get(i);
                Unit u = new Unit();
                PersistenceUtils.restore(u, p);
                us.add(u);
            }
            set = new UnitSet(actor.getShirakoPlugin(), us);
        }
        return set;
    }

}
