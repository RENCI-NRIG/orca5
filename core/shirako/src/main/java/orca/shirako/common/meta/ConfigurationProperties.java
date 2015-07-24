package orca.shirako.common.meta;

public interface ConfigurationProperties {
    /*
     * NOTE: Conventions used in this file:
     * 
     * 1. Each property name starts with Config
     * 2. Each property value starts with "config."
     */
    
    public static final String ConfigHandler = "config.handler";
    public static final String ConfigVictims = "config.victims";
    public static final String ConfigImageGuid = "config.image.guid";
       
    public static final String ConfigSSHKeyPattern = "config.ssh.user%d.keys";
    public static final String ConfigSSHLoginPattern = "config.ssh.user%d.login";
    public static final String ConfigSSHSudoPattern = "config.ssh.user%d.sudo";
    public static final String ConfigSSHUrnPattern = "config.ssh.user%d.urn";
    public static final String ConfigSSHNumLogins = "config.ssh.numlogins";
    
    public static final String ConfigSSHPrefix     = "config.ssh.user";
    public static final String ConfigSSHKeySuffix = ".keys";
    public static final String ConfigSSHLoginSuffix = ".login";
    public static final String ConfigSSHSudoSuffix = ".sudo";
    public static final String ConfigSSHUrnSuffix = ".urn";
    
    // ignore these two for now. policies assume gloabl space
    // for image ids, know to all sites
    public static final String ConfigImageMetaURL = "config.image.metaurl";
    public static final String ConfigImageUrl = "config.image.url";    
}