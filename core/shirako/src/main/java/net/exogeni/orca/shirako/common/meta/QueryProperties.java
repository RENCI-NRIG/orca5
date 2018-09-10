package net.exogeni.orca.shirako.common.meta;

/**
 * Standardizes the inter-actor query mechanism.
 * @author aydan
 *
 */
public interface QueryProperties {
    public static final String QueryAction = "query.action";
    public static final String QueryResponse = "query.response"; // must echo the action
    
    public static final String QueryActionDisctoverPools = "discover.pools";    
    public static final String PoolsCount = "pools.count";
    public static final String PoolPrefix = "pool.";

    // e.g:
    // query.response=discover.pools
    // pools.count=2
    // pool.0=pool_descriptor_xml
    // pool.1=pool_descriptor_xml
}
