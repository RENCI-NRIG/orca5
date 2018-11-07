package orca.shirako.plugins.substrate;

import orca.shirako.api.IActor;
import orca.shirako.api.IDatabase;
import orca.shirako.api.IShirakoPlugin;
import orca.shirako.container.OrcaTestCase;
import orca.shirako.plugins.substrate.db.SubstrateActorDatabase;

public class SubstrateTestBase extends OrcaTestCase {
    
    protected IDatabase makeActorDatabase() {
        SubstrateActorDatabase db = new SubstrateActorDatabase();
        return db;
    }
    
    @Override
    public IShirakoPlugin getShirakoPlugin(String name) throws Exception{
        IShirakoPlugin plugin = new SubstrateTestWrapper();
        plugin.setDatabase(getActorDatabase(name));
        return plugin;
    }
    
    public void testActor() throws Exception{
        IActor actor = getActor();
        assertNotNull(actor);
    }
}