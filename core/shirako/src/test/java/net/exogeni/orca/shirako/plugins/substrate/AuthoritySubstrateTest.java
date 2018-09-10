package net.exogeni.orca.shirako.plugins.substrate;

import net.exogeni.orca.shirako.api.IActor;
import net.exogeni.orca.shirako.api.IAuthority;
import net.exogeni.orca.shirako.core.Authority;
import net.exogeni.orca.shirako.time.Term;

public class AuthoritySubstrateTest extends SubstrateTestBase {
    
    @Override
    protected IActor getActorInstance() {
        return new Authority();
    }
    
    public IAuthority getAuthority() throws Exception {
        IAuthority authority = (IAuthority)getRegisteredNewActor();
        authority.setRecovered(true);
        Term.setClock(authority.getActorClock());
        return authority;
    }    
}
