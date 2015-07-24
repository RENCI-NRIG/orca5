package orca.shirako.common.delegation;

import orca.tools.axis2.Axis2ClientSecurityConfigurator;
import orca.util.ID;
import orca.util.KeystoreManager;

public class KeyHelper
{
    public static String RootDirectory = "/tmp";
    
    private static String KeystorePassword = "clientkeystorepass";
    private static String KeyPassword = "clientkeypass";
        
    protected KeystoreManager keystore;
    protected ID actorGuid;
    private boolean initialized = false;
    
    public KeyHelper()
    {
        actorGuid = new ID();
    }
    
    public KeyHelper(ID actorGuid)
    {
        this.actorGuid = actorGuid;
    }
    
    
    public void initialize() throws Exception
    {
        if (initialized){
            return;
        }
        
        createKeyStore();  
        initialized = true;
    }
    
    protected void createKeyStore() throws Exception
    {
        Axis2ClientSecurityConfigurator configurator = Axis2ClientSecurityConfigurator.getInstance();

        if (configurator.createActorConfiguration(RootDirectory, actorGuid.toString()) != 0){
            throw new Exception("Cannot create security configuration for actor");
        }
        
        keystore = new KeystoreManager(configurator.getKeyStorePath(RootDirectory, actorGuid.toString()), KeystorePassword, KeyPassword);
        keystore.initialize();
    }  
    
    public KeystoreManager getKeystore()
    {
        return keystore;
    }
    
    public ID getGuid()
    {
        return actorGuid;
    }
}