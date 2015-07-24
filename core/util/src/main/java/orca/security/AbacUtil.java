package orca.security;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.deterlab.abac.Context;
import net.deterlab.abac.Credential;
import net.deterlab.abac.Identity;
import net.deterlab.abac.Role;
import orca.util.Base64;
import orca.util.ID;
import orca.util.PropList;
import orca.util.persistence.PersistenceUtils;

import org.apache.log4j.Logger;
import org.bouncycastle.x509.X509V2AttributeCertificate;

public class AbacUtil {
	
	public static final String AbacRoleUser				= "RegisteredUser";
	public static final String AbacRoleSpeaksFor		= "SpeaksFor";
	public static final String AbacRoleOwner			= "Owner";
	public static final String ActorTrustSliceAuthority	= "SliceAuthority";
	
	public static final String ROLE_SEPARATOR			= ".";
	public static final String ROLE_OBJECT_SEPARATOR	= "_";
	public static final String CERT_SUFFIX				= "_ID.pem";
	public static final String ATTR_CERT_SUFFIX			= "_attr.der";
	
	public static final Logger logger = Logger.getLogger(AbacUtil.class);
	
	private static boolean initialized = false;
    
	public static final String RootDirectory;
	public static final String ConfigDir = "/config/";
	public static final String AbacPropertiesFile="abac.properties";
	protected static Properties configurationProperties = null;
    
	public static final String PropertyCredentialVerification = "abac.credential.verification.required";
	public static final boolean verifyCredentials;
	
	public static final String PropertyAbacLocalRepoHome = "abac.local.credential.repository.home";
	public static final String AbacContextHome;
	
	public static final String PropertyGlobalCertificateRepository = "abac.global.certificate.repository.available";
	public static final boolean globalCertificateRepositoryAvailable;
	
	public static final String PropertyAbacGlobalRepoUrl = "abac.global.credential.repository.url";
	public static final String AbacRepositoryUrl;
	
	public static final String ABAC_ROOT = "abac.root";
	
	static{
		String root = System.getProperty(ABAC_ROOT);
		if (root == null){
			root = ".";
		}
		
		RootDirectory = root;
		initialize();
		if (getProperty(PropertyCredentialVerification) != null){
			verifyCredentials = new Boolean(getProperty(PropertyCredentialVerification));
		}else{
			verifyCredentials = false;
		}
		
		if (getProperty(PropertyAbacLocalRepoHome) != null){
			AbacContextHome = new String(getProperty(PropertyAbacLocalRepoHome));
		}else{
			AbacContextHome = RootDirectory + "/abac_context/";
		}
		
		if (getProperty(PropertyGlobalCertificateRepository) != null){
			globalCertificateRepositoryAvailable = new Boolean(getProperty(PropertyGlobalCertificateRepository));
		}else{
			globalCertificateRepositoryAvailable = false;
		}
		
		if (getProperty(PropertyAbacGlobalRepoUrl) != null){
			AbacRepositoryUrl = new String(getProperty(PropertyAbacGlobalRepoUrl));
		}else{
			AbacRepositoryUrl = "https://10.180.130.74/yii/orca-pod/";
		}
	}
	
	/**
	 * Loads authorization properties
	 */
	public static synchronized void initialize()
	{
		try{
			if(!initialized){
				// load properties file configuring controller behavior
				configurationProperties = new Properties();
				logger.info("Checking for " + RootDirectory + ConfigDir + AbacPropertiesFile);
            	
				File f = new File(RootDirectory + ConfigDir + AbacPropertiesFile);
				if (f.exists()) {
					logger.info("Succeeded, loading configuration");
					configurationProperties.load(new FileInputStream(f));
				} else {
					logger.info("No ABAC configuration found, defaults will be used");
				}
				initialized = true;
			}
		}catch(Exception exception){
			logger.info("ABAC configuration could not be loaded, defaults will be used");
			logger.error(exception.getMessage());
		}
	}
	
	/**
	 * Gets a specific property value
	 * @param p - property key
	 * @return property value
	 */
	public static String getProperty(String p) {
		if (configurationProperties != null)
			return configurationProperties.getProperty(p);
		return null;
	}
    
	/**
	 * Converts a certificate object into a pem encoded string
	 * @param certificate
	 * @return pem encoded certificate string
	 * @throws CertificateEncodingException
	 */
	public static String getPemEncodedCert(X509Certificate certificate) throws CertificateEncodingException{
		return addPemCertHeaderFooter(Base64.encodeBytes(certificate.getEncoded()));
	}
	
	/**
	 * Creates a certificate object from a pem encoded certificate string
	 * @param pemEncodedCert - pem encoded certificate string
	 * @return certificate
	 * @throws CertificateException
	 */
	public static X509Certificate getCertificate(String pemEncodedCert) throws CertificateException{
		return getCertificate(addPemCertHeaderFooter(pemEncodedCert).getBytes());
	}
	
	/**
	 * Creates a certificate object from a pem encoded certificate byte array
	 * @param encoded - pem encoded certificate byte array
	 * @return certificate
	 * @throws CertificateException
	 */
	public static X509Certificate getCertificate(byte[] encoded) throws CertificateException{
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		X509Certificate cert = (X509Certificate)factory.generateCertificate(new ByteArrayInputStream(encoded));
		return cert;
	}
    
	/**
	 * Adds header and footer to a pem encoded certificate string
	 * @param pemEncodedCert - pem encoded certificate string
	 * @return pem encoded certificate string with header and footer
	 */
	private static String addPemCertHeaderFooter(String pemEncodedCert){
		if(!pemEncodedCert.startsWith("-----BEGIN CERTIFICATE-----")){
			StringBuffer result = new StringBuffer();
			result.append("-----BEGIN CERTIFICATE-----");
			result.append(newline());
			result.append(pemEncodedCert);
			result.append(newline());
			result.append("-----END CERTIFICATE-----");
			result.append(newline());
			pemEncodedCert = result.toString();
		}
		return pemEncodedCert;
	}
    
	/**
	 * Creates an attribute certificate object from pem encoded certificate byte array
	 * @param encoded - pem encoded certificate byte array
	 * @return attribute certificate
	 * @throws IOException
	 */
	public static X509V2AttributeCertificate getAttributeCertificate(byte[] encoded) throws IOException{
		X509V2AttributeCertificate cert = new X509V2AttributeCertificate(encoded);
		return cert;
	}
    
	/**
	 * Returns the system specific new line character
	 * @return new line character
	 */
	public static String newline(){
		return System.getProperty("line.separator");
	}
    
	/**
	 * Creates an object specific policy for a given authority
	 * @param authorityCert
	 * @param authorityKey
	 * @param trustAnchorRoleName
	 * @param objectId
	 * @param requiredPrivileges
	 * @throws CertificateException
	 */
	public static void createObjectPolicy(X509Certificate authorityCert, PrivateKey authorityKey, 
			String trustAnchorRoleName, ID objectId, String[] requiredPrivileges) throws CertificateException{
		try{
			Identity authority = new Identity(authorityCert);
			KeyPair authorityKeyPair = new KeyPair(authorityCert.getPublicKey(), authorityKey);
			authority.setKeyPair(authorityKeyPair);
			
			for(int count = 0; count < requiredPrivileges.length; count++){
				Role role = new Role(authority.getKeyID() + ROLE_SEPARATOR + createRoleName(requiredPrivileges[count], objectId));
				Role prin = new Role(authority.getKeyID() + ROLE_SEPARATOR + trustAnchorRoleName + ROLE_SEPARATOR + createRoleName(requiredPrivileges[count], objectId));
			
				Credential cred = new Credential(role, prin);
				cred.make_cert(authority);
				
				String credentialDirectory = getContextHome(authorityCert, objectId);
				createDirectory(new File(credentialDirectory));
				authority.write(credentialDirectory + File.separator + authority.getKeyID() + CERT_SUFFIX);
				cred.write(credentialDirectory + File.separator + getCredentialFileName(cred));
			}
		}catch(Exception exception){
			throw new CertificateException("Could not create credential");
		}
	}
    
	/**
	 * Validates a given user
	 * @param requester
	 * @param authority
	 * @param contextPath
	 * @return true/false indicating whether the given user is trusted
	 * @throws Exception
	 */
	public static boolean validateUser(X509Certificate requester, X509Certificate authority) throws Exception{
		return checkPrivilege(requester, authority, null, new String[]{AbacRoleUser});
	}

	/**
	 * Checks if according to the authority, the given principal has one of the required privileges
	 * @param principal
	 * @param authority
	 * @param objectId
	 * @param requiredPrivileges
	 * @param contextPath
	 * @return true/false indicating if the principal has a reuiqred privilege
	 */
	public static boolean checkPrivilege(X509Certificate principal, X509Certificate authority, ID objectId, String[] requiredPrivileges){
		
		try{
			if(requiredPrivileges == null || requiredPrivileges.length == 0){
				return false;
			}
			
			String[] objectPrivileges = new String[requiredPrivileges.length];
			if(objectId != null){
				for(int count = 0; count < requiredPrivileges.length; count++){
					objectPrivileges[count] = createRoleName((requiredPrivileges[count]), objectId);
				}
			}else{
				objectPrivileges = requiredPrivileges;
			}
			
			Context context = new Context();
			//Map<String, Exception> errs = new HashMap<String, Exception>();
			
			//loading actor policy
			loadCredentials(context, authority);
			
			//loading user context
			loadCredentials(context, principal);
			
			if(objectId != null){
				//loading actor slice policy
				loadCredentials(context, authority, objectId);

				//loading subject object context
				loadCredentials(context, principal, objectId);
			}
			
			try{
				return hasPrivilege(principal, authority, context, objectPrivileges);
			}catch(Exception exception){
				logger.debug(exception.getMessage());
				return false;
			}
	        
		} catch (Exception exception){
			logger.debug(exception.getMessage());
			return false;
		}
	}
	
	public static boolean hasPrivilege(X509Certificate principal, X509Certificate authority, Context context, String[] requiredPrivilege){
		
		Set<String> privilegeSet = new HashSet<String>();
		if(requiredPrivilege != null){
			for(int count = 0; count < requiredPrivilege.length; count++){
				privilegeSet.add(requiredPrivilege[count]);
			}
		}
		return hasPrivilege(principal, authority, context, privilegeSet);
	}

	public static boolean hasPrivilege(X509Certificate principal, X509Certificate authority, Context context, Set<String> privilegeSet){
		
		try{
			Identity prinIdentity = new Identity(principal);
			Identity authIdentity = new Identity(authority);
			
			Iterator<String> privilegeItr = privilegeSet.iterator();
			
			String privilege;
			Role role, prin;
			Context.QueryResult ret;
			
			while(privilegeItr.hasNext()){
				
				privilege = privilegeItr.next();
				
				role = new Role(authIdentity.getKeyID() + ROLE_SEPARATOR + privilege, context);
				prin = new Role(prinIdentity.getKeyID(), context);
				
				ret = context.query(role.toString(), prin.toString());
				logger.debug("Result: " + ret.getSuccess());
				logger.debug("Proof");
		        for (Credential cred : ret.getCredentials()) {
		        	logger.debug(cred.simpleString(context));
		        }
				
		        if(ret.getSuccess()){
		        	return true;
		        }
			}
			
			return false;
		
		}catch(Exception exception){
			logger.debug(exception.getMessage());
			return false;
		}
	}
	
	public static void loadCredentials(Context context, X509Certificate subject) throws Exception{
		loadCredentials(context, subject, null);
	}
	
	public static void loadCredentials(Context context, X509Certificate subject, ID object) throws Exception{
		try{
			context.load_zip(getLocalContextZip(subject, object));
		}catch(Exception exception){
			if(object == null)
				logger.debug("Local credentials not available for subject = " + new Identity(subject).getKeyID());
			else
				logger.debug("Local credentials not available for subject = " + new Identity(subject).getKeyID() + " , object = " + object.toSha1HashString());
		}
		
		if(globalCertificateRepositoryAvailable){
			context.load_zip(downloadFromPod(subject, object));
		}
	}
	
	/**
	 * Gets the context for a subject scoped to an object
	 * @param subject
	 * @param object
	 * @return context zip
	 * @throws Exception
	 */
	public static File getLocalContextZip(X509Certificate subject, ID object) throws Exception{
		return new File(createContextZip(subject, object));
	}
	
	public static File downloadFromPod(X509Certificate subject, ID object) throws Exception{
		
		String surl;
		
		if(object != null){
			surl = AbacRepositoryUrl + "/manage/downloadByScopeSubject?subject=" + extractKeyId(subject) 
					+ "&scope=" + object.toSha1HashString();
		}else{
			surl = AbacRepositoryUrl + "/manage/downloadBySubject?subject=" + extractKeyId(subject);
		}
		
		URL url = new URL(surl);
		
		BufferedInputStream bis;
		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			bis = new BufferedInputStream(connection.getInputStream());
		}
		catch (IOException ioException) {
			throw new IOException("Error encountered while attempting to " +
						"establish HTTP connection to URL: " +
						surl + " ; reason was: " + ioException.getMessage());
		}
		
		File newfile = new File(getContextFileName(subject, object));
		
		try {
			BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(newfile));
			int b;
			while ((b = bis.read()) != -1) fos.write(b);
			fos.flush();
			fos.close();
		}
		catch (IOException ioException) {
			throw new IOException("Error encountered while writing to file: " +
						newfile.getPath() + " ; reason was: " +
						ioException.getMessage());
		}
		
		return newfile;
	}
	/**
	 * create a context archive for a given subject and object
	 * @param subjectCertificate
	 * @param objectId
	 * @return
	 * @throws CertificateException
	 */
	public static String createContextZip(X509Certificate subjectCertificate, ID objectId) throws CertificateException{
		try {
			String contextFileName;
			Context context = new Context();
	        Map<String, Exception> errors = new HashMap<String, Exception>();
	        
	        if(objectId == null){
	        	contextFileName = getContextFileName(subjectCertificate);
	        	File contextDirectory = new File(AbacUtil.getContextHome(subjectCertificate));
	        	if(!contextDirectory.exists()){
	        		throw new CertificateException("Context directory does not exist. Folder name = " + contextDirectory.getName());
	        	}
	 	        context.load_directory(contextDirectory, errors);
	        }else{
	        	contextFileName = getContextFileName(subjectCertificate, objectId);
	        	File contextDirectory = new File(AbacUtil.getContextHome(subjectCertificate, objectId));
	        	if(!contextDirectory.exists()){
	        		throw new CertificateException("Context directory does not exist. Folder name = " + contextDirectory.getName());
	        	}
	        	context.load_directory(contextDirectory, errors);
	        }
	        
	        context.write_zip(new File(contextFileName), true, false);
	        
	        return contextFileName;
		
		} catch (Exception exception) {
			throw new CertificateException("Could not create compressed context file");
		}
	}
	
	public static String getContextFileName(X509Certificate cert) throws Exception{
		return getContextFileName(cert, null);
    }
	
	public static String getContextFileName(X509Certificate cert, ID objectId) throws Exception{
		if(objectId != null){
			return AbacUtil.AbacContextHome + File.separator +  extractKeyId(cert) + "_" + objectId.toSha1HashString() + ".zip";
		}else{
			return AbacUtil.AbacContextHome + File.separator +  extractKeyId(cert) + ".zip";
		}
    }
	/**
	 * returns subject specific context home
	 */
	public static String getContextHome(Identity identity) throws Exception{
		return AbacContextHome + File.separator + identity.getKeyID();
	}
    
	/**
	 * returns subject and object specific context home
	 */
	public static String getContextHome(Identity identity, ID objectId) throws Exception{
		return getContextHome(identity) + "_" + objectId.toSha1HashString();
	}
    
	/**
	 * returns subject specific context home
	 */
	public static String getContextHome(X509Certificate subjectCertificate) throws Exception{
		return getContextHome(new Identity(subjectCertificate));
	}
    
	/**
	 * returns subject and object specific context home
	 */
	public static String getContextHome(X509Certificate subjectCertificate, ID objectId) throws Exception{
		return getContextHome(subjectCertificate) + "_" + objectId.toSha1HashString();
	}
    
	/**
	 * generate name for a credential output file
	 * @param cred
	 * @return
	 */
	public static String getCredentialFileName(Credential cred){
		StringBuffer result = new StringBuffer();
		result.append(cred.head().toString().replaceAll("\\.", "_"));
		result.append("__");
		result.append(cred.tail().toString().replaceAll("\\.", "_"));
		result.append(ATTR_CERT_SUFFIX);
		return result.toString();
	}
	
	public static boolean createDirectory(File file) {
	    if (file.exists())
	    	return true;
	    boolean status = file.mkdirs();
	    if (status)
	        logger.debug(" Successful in creating directory " + file.getPath());
	    return status;
	}
	
	public static String extractKeyId(X509Certificate cert) throws Exception{
		return new Identity(cert).getKeyID();
	}
    
	public static String getCommonName(X509Certificate cert) throws Exception{
		return new Identity(cert).getName();
	}
    
	public static String createRoleName(String privilege, ID objectId) throws Exception{
		return privilege + ROLE_OBJECT_SEPARATOR + objectId.toSha1HashString();
	}
    
	//methods to pass authorization related parameters from client to authority
	public static final String RequesterAuthToken 		= "request.requesterAuthToken";
    
	public static void setRequesterAuthToken(Properties p, AuthToken caller) throws Exception
	{
		PropList.setProperty(p, getRequesterAuthTokenKey(), getRequesterAuthTokenValue(caller));
	}

	public static String getRequesterAuthTokenKey() {
		return RequesterAuthToken;
	}
	
	public static String getRequesterAuthTokenValue(AuthToken caller) throws Exception {
		return PropList.toString(PersistenceUtils.save(caller));
	}
	
	public static AuthToken getRequesterAuthToken(String value) throws Exception {
		if (value != null) {
			return PersistenceUtils.restore(PropList.toProperties(value));
		}
		return null;
	}
	
	public static AuthToken getRequesterAuthToken(Properties p) throws Exception{
		return getRequesterAuthToken(p.getProperty(getRequesterAuthTokenKey()));
	}
}
