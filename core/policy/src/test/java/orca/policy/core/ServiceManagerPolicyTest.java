package orca.policy.core;

import orca.shirako.api.IServiceManager;
import orca.shirako.container.OrcaTestCase;
import orca.shirako.kernel.ServiceManagerTestWrapper;
import orca.shirako.time.Term;

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
