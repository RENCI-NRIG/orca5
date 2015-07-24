package orca.shirako.plugins.substrate;

import orca.shirako.api.IActor;
import orca.shirako.api.IAuthority;
import orca.shirako.core.Authority;
import orca.shirako.time.Term;

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