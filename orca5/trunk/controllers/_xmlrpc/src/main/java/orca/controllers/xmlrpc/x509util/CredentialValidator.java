package orca.controllers.xmlrpc.x509util;

import java.security.cert.X509Certificate;
import java.util.Set;

import javax.security.auth.login.CredentialException;

import orca.controllers.xmlrpc.XmlrpcController;
import orca.shirako.container.Globals;

import org.apache.log4j.Logger;

public class CredentialValidator {

	protected static Logger logger = Globals.getLogger(CredentialValidator.class.getName());
	
	public static final String PropertyChTruststorePath = "credential.truststore.location";
	public static final String PropertyChTruststorePassword = "credential.truststore.password";
	
	/**
	 * function to validate if a given credential
	 * @param sliceUrn
	 * @param credentialString
	 * @param requiredPrivilege
	 * @param clientCertificateChain
	 * @throws CredentialException
	 */
	public static void validateCredential(String sliceUrn, String credentialString, Set<String> requiredPrivilege, X509Certificate[] clientCertificateChain) throws CredentialException{
		
		Credential credential;
		
		try{
			credential = new Credential(credentialString);
		} catch(Exception exception){
			logger.error(exception.getMessage());
			throw new CredentialException("Exception parsing credential xml");
		}
		
        if((requiredPrivilege != null) && (!credential.hasPrivilege(requiredPrivilege))){
        	throw new CredentialException("Credential does not have the required privilege");
        }
        
        String CH_TRUSTSTORE_PATH = XmlrpcController.getProperty(PropertyChTruststorePath);
        String CH_TRUSTSTORE_PASS = XmlrpcController.getProperty(PropertyChTruststorePassword);
        
        if (CH_TRUSTSTORE_PATH == null) {
    		throw new CredentialException("Clearing house truststore location not set.");
    	}
        
        try{
        	credential.verify(CH_TRUSTSTORE_PATH, CH_TRUSTSTORE_PASS);
        } catch(Exception exception){
        	logger.error(exception.getMessage());
        	throw new CredentialException("Credential could not be verified against trust store.");
        }
        
        X509Certificate credentialOwnerCertificate = credential.getCallerGid().getCertificate();
        if (!credentialOwnerCertificate.getPublicKey().equals(clientCertificateChain[0].getPublicKey())){
        	throw new CredentialException("caller != owner");
        }
		
		if(sliceUrn != null){
			logger.error("Credential should be a slice credential");
			logger.error("Slice URN: " + sliceUrn);
			
			if(!sliceUrn.equals(credential.getObejctUrn())){
				throw new CredentialException("slice urn != target urn");
			}
		} else {
			logger.error("Credential should be a user credential");
		}
	}
	
	/**
	 * Only check the client certs against a trust root
	 * @param clientCertificate
	 * @throws CredentialException
	 */
	public static void checkTrustRoot(X509Certificate[] clientCertificate) throws CredentialException {
		
	}
}