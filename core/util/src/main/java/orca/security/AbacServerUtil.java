package orca.security;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import net.deterlab.abac.Credential;
import net.deterlab.abac.Identity;
import net.deterlab.abac.Role;
import orca.util.ID;

import org.apache.log4j.Logger;
import org.bouncycastle.x509.X509V2AttributeCertificate;

public class AbacServerUtil {
	
	public static final String ROLE_SEPARATOR 			= ".";
	public static final String ROLE_OBJECT_SEPARATOR 	= "_";
	
	private static final String CERT_SUFFIX 			= "_ID.pem";
	private static final String ATTR_CERT_SUFFIX 		= "_attr.der";
	
	public static final Logger logger = Logger.getLogger(AbacServerUtil.class);
	
	public static final String PropertyCreateSliceCredential = "abac.create.slice.credential";
	public static final boolean createSliceCredential;
	
	static{
		if (AbacUtil.getProperty(PropertyCreateSliceCredential) != null){
			createSliceCredential = new Boolean(AbacUtil.getProperty(PropertyCreateSliceCredential));
		}else{
			createSliceCredential = false;
		}
	}
	
	/**
	 * create a slice credential
	 * @param sliceAuthorityPrivateKey slice priavte key
	 * @param sliceAuthorityCertificate slice certificate
	 * @param userCertificate user certificate
	 * @param sliceId slice Id
         * @return return the 509 certificate
	 * @throws Exception in case of error
	 */
	public static X509V2AttributeCertificate createSliceCredential(PrivateKey sliceAuthorityPrivateKey, X509Certificate sliceAuthorityCertificate, 
			X509Certificate userCertificate, ID sliceId) throws Exception{
		
		if(sliceAuthorityPrivateKey == null || sliceAuthorityCertificate == null || userCertificate == null){
			throw new CertificateException("slice authority private key/slice authority certificate/user certificate not available.");
		}
		
		return createCredential(sliceAuthorityPrivateKey, sliceAuthorityCertificate, userCertificate, sliceId, AbacUtil.AbacRoleOwner);
	}
	
	/**
	 * create an attribute certificate
	 * @param assignerPrivateKey assigner private key
	 * @param assignerCertificate assigner certificate
	 * @param assigneeCertificate assignee certificate
	 * @param objectId object id
	 * @param privilege privileges
         * @return returns 509V2 certificate
	 * @throws Exception in case of error
	 */
	public static X509V2AttributeCertificate createCredential(PrivateKey assignerPrivateKey, X509Certificate assignerCertificate, 
										X509Certificate assigneeCertificate, ID objectId, String privilege) throws Exception{
		
		if(assignerPrivateKey == null){
			throw new CertificateException("Assigner private key not available.");
		}
		
		if(assignerCertificate == null){
			throw new CertificateException("Assigner certificate not available.");
		}
		
		if(assigneeCertificate == null){
			throw new CertificateException("Assignee certificate not available.");
		}
		
		try{
			Identity assigner = new Identity(assignerCertificate);
			KeyPair assignerKeyPair = new KeyPair(assignerCertificate.getPublicKey(), assignerPrivateKey);
			assigner.setKeyPair(assignerKeyPair);
			
			Identity assignee = new Identity(assigneeCertificate);
			
			Role role = new Role(assigner.getKeyID() + ROLE_SEPARATOR + AbacUtil.createRoleName(privilege, objectId));
			Role prin = new Role(assignee.getKeyID());
			
			Credential cred = new Credential(role, prin);
			cred.make_cert(assigner);
			
			String credentialDirectory = AbacUtil.getContextHome(assigneeCertificate, objectId);
			createDirectory(new File(credentialDirectory));
			assigner.write(credentialDirectory + File.separator + assigner.getKeyID() + CERT_SUFFIX);
			cred.write(credentialDirectory + File.separator + getCredentialFileName(cred));
		
			return cred.attributeCert();
			
		}catch(Exception exception){
			throw new CertificateException("Could not create credential");
		}
	}
	
	/**
	 * generate name for a credential output file
	 * @param cred credential
	 * @return name for a credential output file
	 */
	public static String getCredentialFileName(Credential cred){
		StringBuffer result = new StringBuffer();
		result.append(cred.head().toString().replaceAll("\\.", "_"));
		result.append("__");
		result.append(cred.tail().toString().replaceAll("\\.", "_"));
		result.append(ATTR_CERT_SUFFIX);
		return result.toString();
	}
	
	/**
	 * register an already existing credential
	 * @param userCertificate user certificate 
	 * @param objectId object Id
	 * @param certificate certificate
	 * @param fileName file name 
	 * @throws Exception in case of error
	 */
	public static void registerCredential(X509Certificate userCertificate, String objectId, String certificate, String fileName) throws Exception{
		
		String credentialDirectory;
		
		if(objectId == null){
			credentialDirectory = AbacUtil.getContextHome(userCertificate);
		}else{
			credentialDirectory = AbacUtil.getContextHome(userCertificate, new ID(objectId));
		}
		
		createDirectory(new File(credentialDirectory));
		
		try{
			X509Certificate identityCertificate = AbacUtil.getCertificate(certificate.getBytes());
			if(identityCertificate != null){
				Identity identity = new Identity(identityCertificate);
				if(fileName == null){
					fileName = identity.getKeyID() + CERT_SUFFIX;
				}
				identity.write(credentialDirectory + File.separator + fileName);
				return;
			}
		}catch(Exception exception){
			//
		}
		
		try{
			X509V2AttributeCertificate credential = AbacUtil.getAttributeCertificate(certificate.getBytes());
			if(credential != null){
				if(fileName == null){
					fileName = credential.getIssuer().hashCode() + "_" + credential.getHolder().hashCode() + "_" + System.currentTimeMillis() + ATTR_CERT_SUFFIX;
				}
				String filePath = credentialDirectory + File.separator + fileName;
				FileOutputStream fileOutputStream = new FileOutputStream(filePath);
				fileOutputStream.write(credential.getEncoded());
				fileOutputStream.close();
				return;
			}
		}catch(Exception exception){
			//
		}
		
		throw new CertificateException(fileName + " Invalid certificate");
	}
	
//	/**
//	 * adding a user identity certificate
//	 * @param certificate
//	 * @throws CertificateException
//	 */
//	public static void updateUserCert(X509Certificate certificate) throws CertificateException{
//		try {
//			Identity user = new Identity(certificate);
//			String certificateDirectory = getContextHome(certificate);
//			createDirectory(new File(certificateDirectory));
//			user.write(certificateDirectory + File.separator + user.getKeyID() + CERT_SUFFIX);
//		} catch (Exception exception) {
//			throw new CertificateException("Could not save user certificate");
//		}
//	}
	
	/**
	 * Creates directory with the given name
	 * @param file file to be created
	 * @return true for success; otherwise false
	 */
    public static boolean createDirectory(File file) {
	    if (file.exists())
	    	return true;
	    boolean status = file.mkdirs();
	    if (status)
	        logger.debug(" Successful in creating directory " + file.getPath());
	    return status;
	}
    
    /**
     * returns hash of the public key
     * @param cert certificate
     * @return string containing the hask of public key
     * @throws Exception in case of error
     */
    public static String extractKeyId(X509Certificate cert) throws Exception{
    	return new Identity(cert).getKeyID();
    }
}
