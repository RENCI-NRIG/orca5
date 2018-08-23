package orca.shirako.common.currency;

import java.util.ArrayList;
import java.util.Date;

import orca.util.ID;

/**
 * <code>Wallet</code> organizes one or more <code>CreditsNote</code>-s for a
 * given actor.
 * @author aydan
 */
public class Wallet
{
    protected ArrayList<CreditsNote> notes;
    
    // FIXME: need a creditsfactory 
    //        need the recharge interval
    
    public Wallet()
    {
        notes = new ArrayList<CreditsNote>();
    }
    
    
    /**
     * Adds a new credits note to the wallet.
     * @param note note
     */
    public void add(CreditsNote note)
    {
    }
    
    /**
     * Attempts to construct a new credits note for the specified amount to be issued to the 
     * specified holder.
     * @param credits amount to spend
     * @param holder identity of actor the credits are to be transferred to
     * @return a valid credits note if the wallet contains a sufficient balance, null otherwise
     */
    public CreditsNote spend(int credits, ID holder)
    {
        return null;
    }
    
    
    /**
     * Returns the available budget at the specified time instance.
     * @param when time instance
     * @return available budget
     */
    public int getBudget(Date when)
    {
        return 0;
    }
    
    /**
     * Returns the number of credits that will expire until the specified date (inclusive) 
     * @param untilWhen expiration date
     * @return number of expired credits
     */
    public int getExpiring(Date untilWhen)
    {
        return 0;
    }    
}
