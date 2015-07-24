package orca.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.security.cert.CertificateException;

import net.deterlab.abac.Credential;
import net.deterlab.abac.Identity;
import net.deterlab.abac.Role;
import orca.util.ID;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMReader;

public class AbacTestUtil {

	public static final Logger logger = Logger.getLogger(AbacTestUtil.class);
	
	public static final String AbacRoleUserAuthority = "UserAuthority";
	
	//set this to true if new identities and credentials need to be created
	//else only a verification on existing identities and credentials can be done
	public static final boolean createIdentitiesAndCredentials = false;
	
	//set this to true if new actor identities need to be created
	//else actor identities will be loaded from existing keystores
	public static final boolean createNewActorIdentities = false;
	
	//set the following if actor identities need to be loaded from existing keystores
	public static final String smKeyStore = "/opt/orca/runtime/keystores/0d3190eb-5630-45df-a13d-5c5f47b53a94.jks";
	public static final String brokerKeyStore = "/opt/orca/runtime/keystores/5271a107-10c3-45e4-9c06-be4f4928abfb.jks";
	public static final String amKeyStore = "/opt/orca/runtime/keystores/96f49c1a-44a7-4bcc-9b41-19033e27e02b.jks";
	
	public static final String sliceIdString = "7b32a568-1325-43e7-b298-db91e53b0b58";
	
	/*the next set of properties need to be set in the verify only case*/
	
	//set this to true if actor identities need to be loaded from existing keystores
	//else the same will have be loaded from existing certificates
	public static final boolean loadActorIdentitiesFromKeyStore = false;
	
	public static final String userCertificate = AbacUtil.AbacContextHome + File.separator + "temp" + File.separator + "user1_id.pem";
	
	public static final String smCertificate = AbacUtil.AbacContextHome + File.separator + "temp" + File.separator + "sm1_id.pem";;
	public static final String brokerCertificate = AbacUtil.AbacContextHome + File.separator + "temp" + File.separator + "broker_id.pem";;
	public static final String amCertificate = AbacUtil.AbacContextHome + File.separator + "temp" + File.separator + "am_id.pem";;
	
	public static final String smKeyPair = AbacUtil.AbacContextHome + File.separator + "temp" + File.separator + "sm1_private.pem";;
	public static final String brokerKeyPair = AbacUtil.AbacContextHome + File.separator + "temp" + File.separator + "broker_private.pem";;
	public static final String amKeyPair = AbacUtil.AbacContextHome + File.separator + "temp" + File.separator + "am_private.pem";;
	
	public static void main(String args[]){
		
		BasicConfigurator.configure();
		logger.info("Start");
		
		try{
			if(createIdentitiesAndCredentials){
				createAndVerify();
			}else{
				
				Identity user;
				Identity sm;
				Identity broker;
				Identity am;
				ID sliceId;
				
				user = new Identity(new File(userCertificate));
				
				if(loadActorIdentitiesFromKeyStore){
					sm = createIdentity(new File(smKeyStore));
					broker = createIdentity(new File(brokerKeyStore));
					am = createIdentity(new File(amKeyStore));
				}else{
					sm = new Identity(new File(smCertificate));
					broker = new Identity(new File(brokerCertificate));
					am = new Identity(new File(amCertificate));
					
					sm.setKeyPair((KeyPair)new PEMReader(new FileReader(new File(smKeyPair))).readObject());
					broker.setKeyPair((KeyPair)new PEMReader(new FileReader(new File(brokerKeyPair))).readObject());
					am.setKeyPair((KeyPair)new PEMReader(new FileReader(new File(amKeyPair))).readObject());
				}
				
				sliceId = new ID(sliceIdString);
				
				verify(user, sm, broker, am, sliceId);
			}
		}catch(Exception exception){
			exception.printStackTrace();
		}
		
		
		logger.info("End");
	}
	
	public static void createAndVerify() throws Exception{
		createDirectory(new File(AbacUtil.AbacContextHome));
		
		logger.info("Creating ABAC Identities");
		
		logger.info("Registry");
		Identity registry = createIdentity("registry");
		
		logger.info("Aggregate Manager");
		Identity am;
		if(!createNewActorIdentities){
			am = createIdentity(new File(amKeyStore));
		}else{
			am = createIdentity("am");
		}
		
		logger.info("Service Manager 1");
		Identity sm1;
		if(!createNewActorIdentities){
			sm1 = createIdentity(new File(smKeyStore));
		}else{
			sm1 = createIdentity("sm1");
		}
		
		logger.info("Broker");
		Identity broker;
		if(!createNewActorIdentities){
			broker = createIdentity(new File(brokerKeyStore));
		}else{
			broker = createIdentity("broker");
		}
		
		//logger.info("Service Manager 2");
		//Identity sm2 = createIdentity("sm2");
		
		logger.info("Clearing House");
		Identity ch = createIdentity("ch");
		
		logger.info("User");
		Identity user1 = createIdentity("user1");
		
		logger.info("User");
		Identity user2 = createIdentity("user2");
		
		logger.info("Done with creation of ABAC Identities");
		
		logger.info("Aggregate Manager 'am' registering Registry 'registry' as a source of trusted slice authorities");
		//am.SliceAuthority <- registry.SliceAuthority
		assignAttributeSimpleInclusion(am, AbacUtil.ActorTrustSliceAuthority, registry, AbacUtil.ActorTrustSliceAuthority, AbacUtil.getContextHome(am));
		
		logger.info("Broker 'broker' registering Registry 'registry' as a source of trusted slice authorities");
		//broker.SliceAuthority <- registry.SliceAuthority
		assignAttributeSimpleInclusion(broker, AbacUtil.ActorTrustSliceAuthority, registry, AbacUtil.ActorTrustSliceAuthority, AbacUtil.getContextHome(broker));
		
		logger.info("Service Manager 'sm1' registering Registry 'registry' as a source of trusted slice authorities");
		//broker.SliceAuthority <- registry.SliceAuthority
		assignAttributeSimpleInclusion(sm1, AbacUtil.ActorTrustSliceAuthority, registry, AbacUtil.ActorTrustSliceAuthority, AbacUtil.getContextHome(sm1));
		
		logger.info("Service Manager 'sm1' registering Registry 'registry' as a source of trusted user authorities");
		//sm1.UserAuthority <- registry.UserAuthority
		assignAttributeSimpleInclusion(sm1, AbacRoleUserAuthority, registry, AbacRoleUserAuthority, AbacUtil.getContextHome(sm1));
		
		logger.info("Service Manager 'sm1' registering trusted user authorities 'sm1.UserAuthority' as a source of trusted users");
		//sm1.User <- sm1.UserAuthority.User
		assignAttributeLinkingInclusion(sm1, AbacUtil.AbacRoleUser, sm1, AbacRoleUserAuthority, AbacUtil.AbacRoleUser, AbacUtil.getContextHome(sm1));
		
		logger.info("Registry 'registry' registering Clearing House 'ch' as a trusted Slice Authority");
		//registry.SliceAuthority <- ch
		assignAttributeSimpleMember(registry, ch, AbacUtil.ActorTrustSliceAuthority, AbacUtil.getContextHome(ch));
		
		logger.info("Registry 'registry' registering Clearing House 'ch' as a trusted User Authority");
		//registry.UserAuthority <- ch
		assignAttributeSimpleMember(registry, ch, AbacRoleUserAuthority, AbacUtil.getContextHome(ch));
		
		//user1.SpeaksFor <- sm1
		assignAttributeSimpleMember(user1, sm1, AbacUtil.AbacRoleSpeaksFor, AbacUtil.getContextHome(sm1));
		//user2.SpeaksFor <- sm1
		assignAttributeSimpleMember(user2, sm1, AbacUtil.AbacRoleSpeaksFor, AbacUtil.getContextHome(sm1));
		
		authorizeUser(user1, ch);
		
		ID sliceId = new ID(sliceIdString);
		createSlice(user1, ch, sliceId);
		
		verify(user1, sm1, broker, am, sliceId);
	}
	
	public static void verify(Identity user, Identity sm, Identity broker, Identity am, ID sliceId) throws Exception{
		isValidUser(user, sm);
		
		operate(sm, user, sliceId, sm);
		operate(sm, user, sliceId, broker);
		operate(sm, user, sliceId, am);
		
		copyDirectory(new File(AbacUtil.getContextHome(user, sliceId)), new File(AbacUtil.getContextHome(sm, sliceId)));
		operateWithoutProxy(sm, sliceId, am);
	}
	
	public static Identity createIdentity(File keyStore) throws Exception{
    	
		logger.info("Creating Identity from keystore = " + keyStore.getName());
		
    	KeyStore ks = KeyStore.getInstance("JKS");
    	ks.load(new FileInputStream(keyStore), "clientkeystorepass".toCharArray());

        // Get private key
        Key key = ks.getKey("actorkey", "clientkeypass".toCharArray());
        if (key instanceof PrivateKey) {
            // Get certificate of public key
            Certificate cert = ks.getCertificate("actorkey");

            // Get public key
            PublicKey publicKey = cert.getPublicKey();

            // Return a key pair
            KeyPair keypair = new KeyPair(publicKey, (PrivateKey)key);
            
            Identity identity = new Identity((X509Certificate)cert);
            identity.setKeyPair(keypair);
            
            String contextHome = AbacUtil.getContextHome(identity);
            createDirectory(new File(contextHome));
    		identity.write(contextHome + File.separator + identity.getName() + "_id.pem");
    		//identity.writePrivateKey(contextHome + File.separator + cn + "_private.pem");

    		logger.info("Done");
    		return identity;
        }
    
        throw new CertificateException("Cannot create identity");
    }

	public static Identity createIdentity(String cn) throws Exception{
		logger.info("Creating Identity with CN = " + cn);
		Identity identity = new Identity(cn);
		String contextHome = AbacUtil.getContextHome(identity);
        createDirectory(new File(contextHome));
		identity.write(contextHome + File.separator + identity.getName() + "_id.pem");
		//identity.writePrivateKey(contextHome + File.separator + cn + "_private.pem");
		createDirectory(new File(AbacUtil.AbacContextHome + File.separator + "temp"));
		identity.write(AbacUtil.AbacContextHome + File.separator + "temp" + File.separator + identity.getName() + "_id.pem");
		identity.writePrivateKey(AbacUtil.AbacContextHome + File.separator + "temp" + File.separator + identity.getName() + "_private.pem");
		logger.info("Done");
		return identity;
	}
	
	public static void assignAttributeSimpleMember(Identity assignor, Identity assignee, String attribute, String contextHome) throws Exception{
		logger.info(assignor.getName() + "." + attribute + " <- " + assignee.getName());
		//assignor.assignorAttribute <- assignee
		Role role = new Role(assignor.getKeyID() + "." + attribute);
		Role prin = new Role(assignee.getKeyID());
		Credential cred = new Credential(role, prin);
		cred.make_cert(assignor);;
		createDirectory(new File(contextHome));
		cred.write(contextHome + File.separator + assignor.getName() + "_" + attribute + "_" + assignee.getName() + ".der");
		assignor.write(contextHome + File.separator + assignor.getName() + "_id.pem");
	}
	
	public static void assignAttributeSimpleInclusion(Identity assignor, String assignorAttribute, Identity assignee, String assigneeAttribute, String contextHome) throws Exception{
		logger.info(assignor.getName() + "." + assignorAttribute + " <- " + assignee.getName() + "." + assigneeAttribute);
		//assignor.assignorAttribute <- assignee.assigneeAttribute
		Role role = new Role(assignor.getKeyID() + "." + assignorAttribute);
		Role prin = new Role(assignee.getKeyID() + "." + assigneeAttribute);
		Credential cred = new Credential(role, prin);
		cred.make_cert(assignor);
		createDirectory(new File(contextHome));
		cred.write(contextHome + File.separator + assignor.getName() + "_" + assignorAttribute + "_" + assignee.getName() + "_" + assigneeAttribute + ".der");
		assignor.write(contextHome + File.separator + assignor.getName() + "_id.pem");
	}
	
	public static void assignAttributeLinkingInclusion(Identity assignor, String assignorAttribute, Identity assignee, String assigneeAttribute1, String assigneeAttribute2, String contextName) throws Exception{
		//assignor.assignorAttribute <- assignee.assigneeAttribute1.assigneeAttribute2
		assignAttributeSimpleInclusion(assignor, assignorAttribute, assignee, assigneeAttribute1 + "." + assigneeAttribute2, contextName);
	}
	
	public static void authorizeUser(Identity user, Identity ch) throws Exception{
		logger.info(user.getName() + " being authorized by " + ch.getName());
		//ch.ValidUser <- user
		File chContext = new File(AbacUtil.getContextHome(ch));
		File userContext = new File(AbacUtil.getContextHome(user));
		copyDirectory(chContext, userContext);
		assignAttributeSimpleMember(ch, user, AbacUtil.AbacRoleUser, AbacUtil.getContextHome(user));
	}
	
	public static void createSlice(Identity user, Identity sa, ID sliceId) throws Exception{
		logger.info(user.getName() + " creating a slice with id '" + sliceId + "' at " + sa.getName());
		//sa.SliceOwner_sliceId <- user
		File saContext = new File(AbacUtil.getContextHome(sa));
		File userSliceContext = new File(AbacUtil.getContextHome(user, sliceId));
		copyDirectory(saContext, userSliceContext);
		assignAttributeSimpleMember(sa, user, AbacUtil.AbacRoleOwner + "_" + sliceId.toSha1HashString(), AbacUtil.getContextHome(user, sliceId));
	}
	
	public static void isValidUser(Identity user, Identity authority) throws Exception{
		logger.info("-----------------------------------------------------------------------------------");
		logger.info("Validating user " + user.getName() + " at " + authority.getName());
		
		if(AbacUtil.validateUser(user.getCertificate(), (X509Certificate)authority.getCertificate())){
			logger.info(user.getName() + " is a valid user");
		}else{
			logger.info(user.getName() + " is not a valid user");
		}
		logger.info("-----------------------------------------------------------------------------------");
	}
	
	public static void operate(Identity sm, Identity user, ID sliceId, Identity am) throws Exception{
		
		logger.info("-----------------------------------------------------------------------------------");
		logger.info(sm.getName() + " on behalf of " + user.getName() + " requesting an operation on slice with id '" + sliceId + "' at " + am.getName());
		
		logger.info("Testing " + user.getName() + ".SpeaksFor <- " + sm.getName());
		
		if(AbacUtil.checkPrivilege(sm.getCertificate(), user.getCertificate(), null, new String[]{AbacUtil.AbacRoleSpeaksFor})){
			logger.info(sm.getName() + " has the privilege to speak on behalf of " + user.getName());
			logger.info("-----------------------------------------------------------------------------------");
		}else{
			logger.info(sm.getName() + " does not have the privilege to speak on behalf of " + user.getName());
			logger.info("Operation Failed");
	        logger.info("-----------------------------------------------------------------------------------");
			return;
		}
		
		AbacUtil.createObjectPolicy(am.getCertificate(), am.getKeyPair().getPrivate(), AbacUtil.ActorTrustSliceAuthority, sliceId, new String[]{AbacUtil.AbacRoleOwner});
		
        logger.info("Testing " + am.getName() + "." + AbacUtil.AbacRoleOwner +"_" + sliceId + " <- " + user.getName());
        
        if(AbacUtil.checkPrivilege(user.getCertificate(), am.getCertificate(), sliceId, new String[]{AbacUtil.AbacRoleOwner})){
			logger.info(user.getName() + " has the privilege to operate on slice " + sliceId);
			logger.info("-----------------------------------------------------------------------------------");
		}else{
			logger.info(user.getName() + " does not have the privilege to operate on slice " + sliceId);
			logger.info("Operation Failed");
	        logger.info("-----------------------------------------------------------------------------------");
			return;
		}
		
        logger.info("Operation Successful");
        logger.info("-----------------------------------------------------------------------------------");
	}
	
	public static void operateWithoutProxy(Identity sm, ID sliceId, Identity am) throws Exception{
		
		logger.info("-----------------------------------------------------------------------------------");
		logger.info(sm.getName() + " requesting an operation on slice with id '" + sliceId + "' at " + am.getName());
		
		AbacUtil.createObjectPolicy(am.getCertificate(), am.getKeyPair().getPrivate(), AbacUtil.ActorTrustSliceAuthority, sliceId, new String[]{AbacUtil.AbacRoleOwner});
		
		assignAttributeLinkingInclusion(am, AbacUtil.AbacRoleOwner + "_" + sliceId.toSha1HashString(), am, AbacUtil.AbacRoleOwner + "_" + sliceId.toSha1HashString(), AbacUtil.AbacRoleSpeaksFor, AbacUtil.getContextHome(am));
		
        logger.info("Testing " + am.getName() + "." + AbacUtil.AbacRoleOwner + "_" + sliceId + " <- " + sm.getName());
        
        if(AbacUtil.checkPrivilege(sm.getCertificate(), am.getCertificate(), sliceId, new String[]{AbacUtil.AbacRoleOwner})){
			logger.info(sm.getName() + " has the privilege to operate on slice " + sliceId);
			logger.info("-----------------------------------------------------------------------------------");
		}else{
			logger.info(sm.getName() + " does not have the privilege to operate on slice " + sliceId);
			logger.info("Operation Failed");
	        logger.info("-----------------------------------------------------------------------------------");
			return;
		}
		
        logger.info("Operation Successful");
        logger.info("-----------------------------------------------------------------------------------");
	}

	public static boolean createDirectory(File file) 
	{
	    // Delete Directory if alreday exists
	    if (file.exists())
	        ;//deleteDirectory(file);
	    boolean status = file.mkdirs();
	    if (status)
	        logger.info(" Successful in creating directory " + file.getPath());
	    return status;
	}

	public static boolean deleteDirectory(File dir) {
	    if (dir.isDirectory()) {
	        String[] children = dir.list();
	        for (int i = 0; i < children.length; i++) {
	            File delFile = new File(dir, children[i]);
	            if (!delFile.exists()) {
	            logger.info("Cannot find directory to delete " + delFile.getPath());
	                return false;
	            }
	            boolean success = deleteDirectory(delFile);
	            //logger.info(delFile+": success? "+success);
	            if (!success) {
	            }
	        }
	    }
	    // The directory is now empty so now it can be smoked
	    return dir.delete();
	}
	
	public static void copyDirectory(File sourceLocation , File targetLocation) throws IOException {
		if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            
            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
            }
        } else {
            
            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);
            
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }
}
