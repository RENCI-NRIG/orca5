package net.exogeni.orca.policy.core;

import net.exogeni.orca.shirako.api.IServiceManager;
import net.exogeni.orca.shirako.container.OrcaTestCase;
import net.exogeni.orca.shirako.kernel.ServiceManagerTestWrapper;
import net.exogeni.orca.shirako.time.Term;

public abstract class ServiceManagerPolicyTest extends OrcaTestCase {
    @Override
    public IServiceManager getSM() throws Exception {
        IServiceManager sm = super.getSM();
        sm.setRecovered(true);
        Term.setClock(sm.getActorClock());
        return sm;
    }

    @Override
    public IServiceManager getSMInstance() {
        return new ServiceManagerTestWrapper();
    }
}
