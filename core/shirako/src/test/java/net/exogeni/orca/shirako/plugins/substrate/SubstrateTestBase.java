package net.exogeni.orca.shirako.plugins.substrate;

import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IDatabase;
import net.exogeni.orca.shirako.api.IShirakoPlugin;
import net.exogeni.orca.shirako.container.OrcaTestCase;
import net.exogeni.orca.shirako.plugins.substrate.db.SubstrateActorDatabase;

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
