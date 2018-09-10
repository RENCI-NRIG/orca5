package net.exogeni.orca.policy.core;

import net.exogeni.orca.shirako.api.IAuthorityPolicy;

/**
 * <code>AuthorityCalendarPolicyTest</code> is the base class for unit tests
 * involving <code>AuthorityCalendarPolicy</code>
 * @author aydan
 */
public abstract class AuthorityCalendarPolicyTest extends AuthorityPolicyTest
{
    @Override
    public IAuthorityPolicy getAuthorityPolicy() throws Exception
    {
        AuthorityCalendarPolicy policy = new AuthorityCalendarPolicy();
        policy.registerControl(getControl());
        return policy;
    }

    /**
     * Returns the <code>IResourceControl</code> used by the policy. *
     * @param policy policy
     * @return
     */
    public IResourceControl getControl(AuthorityCalendarPolicy policy)
    {
        return policy.controlsByGuid.values().iterator().next();
    }

    /**
     * Creates the resource control to be used by the policy. If the policy
     * requires more than one resource control, you must override
     * {@link #getPolicy()}.
     * @return
     * @throws Exception
     */
    protected abstract IResourceControl getControl() throws Exception;
    
}
