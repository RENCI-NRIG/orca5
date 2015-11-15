package orca.controllers.xmlrpc.x509util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.security.auth.login.CredentialException;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;

import orca.controllers.OrcaController;
import orca.controllers.xmlrpc.XmlRpcController;
import static orca.controllers.xmlrpc.XmlrpcHandlerHelper.getExtensionValue;
import org.apache.commons.io.FileUtils;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.DERObjectIdentifier;

public class CredentialValidator {

        
	protected static Logger logger = OrcaController.getLogger(CredentialValidator.class.getSimpleName());
        //protected static Logger logger = Logger.getLogger(CredentialValidator.class.getName());

	public static final String PropertyChTruststorePath = "credential.truststore.location";
	public static final String PropertyChTruststorePassword = "credential.truststore.password";
	
	/**
	 * function to validate if a given credential
	 * @param sliceUrn
	 * @param credentialString
	 * @param requiredPrivilege
	 * @param clientCertificateChain
	 * @returns slice expiration date
	 * @throws CredentialException
	 */
	public static Date validateCredential(String sliceUrn, String credentialString, Set<String> requiredPrivilege, X509Certificate userCert) throws CredentialException{
		
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

                String CH_TRUSTSTORE_PATH = XmlRpcController.HomeDirectory + XmlRpcController.getProperty(PropertyChTruststorePath);
                String CH_TRUSTSTORE_PASS = XmlRpcController.getProperty(PropertyChTruststorePassword);

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
                //if (!credentialOwnerCertificate.getPublicKey().equals(clientCertificateChain[0].getPublicKey())){
                //    throw new CredentialException("caller != owner");
                //}
                if (!credentialOwnerCertificate.getPublicKey().equals(userCert.getPublicKey())){
                    throw new CredentialException("caller != owner");
                }
                
		if(sliceUrn != null){
			logger.info("Credential should be a slice credential");
			logger.info("Slice URN: " + sliceUrn);
			
			if(!sliceUrn.equals(credential.getObejctUrn())){
				throw new CredentialException("slice urn != target urn");
			}
		} else {
			logger.error("Credential should be a user credential");
		}
		
		return credential.getExpirationDate();
	}

        /** Method to check the type of credential - "abac", "privilege"
         *  @param credential string
         *  @return type of credential
         */

        public static String checkCredentialType(String credentialString) throws CredentialException{

            Credential credential;

            try{
		credential = new Credential(credentialString);
            } catch(Exception exception){
		logger.error(exception.getMessage());
		throw new CredentialException("Exception parsing credential xml");
            }

            return credential.getType();
            
        }

        /**
	 * function to validate if a speaks-for credential is valid given the spoken for user urn and the caller certificate from the SSL connection
	 * @param credentialString
	 * @param spoken_for_user_urn
	 * @param callerCert
	 * @returns certificate of signing user inside speaks-for credential
	 * @throws CredentialException
	 */

        public static X509Certificate validateSpeaksForCredential(String credentialString, String spoken_for_user_urn, X509Certificate callerCert) throws CredentialException, CertificateException, CertPathValidatorException, XMLSignatureException, MarshalException{

            Credential credential;
		
            try{
                //logger.info(credentialString);
		credential = new Credential(credentialString);
            } catch(Exception exception){
		logger.error(exception);
                exception.printStackTrace();
		throw new CredentialException("Exception parsing speaks-for credential xml");
            }

            logger.info("Done parsing credential");

            if(credential == null){
                throw new CredentialException("Parsing Speaks-for credential document resulted in a null credential object");
            }

            if(credential.getSignature() == null){
                throw new CredentialException("Signature in Speaks-for credential is null");
            }
            
            Gid userGid = credential.getSignature().getIssuerGid(); // user gid
            if(userGid == null)
                throw new CredentialException("Speaks-for credential doesn't contain user(signer) certificate");


            // Get certificate of signer from the speaks-for credential. (Gid object carries the certificate inside it)
            // From this certificate, get the urn of the signer and check it against the user-urn passed in speaks-for in ‘options'. These should match.
            if(userGid != null){
                String signer_urn = userGid.getUrn();
                logger.info("Signer (user) urn from signature portion of credential = " + signer_urn);
                if(!signer_urn.equalsIgnoreCase(spoken_for_user_urn)){
                    throw new CredentialException("User URN doesn't match speaking_for URN");
                }
                else{
                    logger.info("User URN matches speaking_for URN");
                }
            }


            // Get tool cert from the client side of SSL connection. Get the SHA1 hash of the public key in the tool cert.
            // Match that hash (i.e. tool_keyid) with the keyid in the ‘tail' of the speaks-for credential. These should be the same.
            String tool_keyid = null;
            try {
                tool_keyid = CertificateUtil.getCertKeyid(callerCert);
            } catch (NoSuchAlgorithmException ex) {
                logger.error("Exception while obtaining keyid from certificate" + ex);
            } catch (IOException ex) {
                logger.error("Exception while obtaining keyid from certificate" + ex);
            }

            String tail_keyid = credential.getTailKeyid();

            logger.info("tool_keyid = " + tool_keyid + " | tail_keyid = " + tail_keyid);

            if(tool_keyid != null && tail_keyid != null){
                if(tool_keyid.equalsIgnoreCase(tail_keyid)){
                    logger.info("Tail keyid matches keyid in caller certificate");
                }
                else{
                    logger.error("Tail keyid does not match keyid in caller certificate");
                    throw new CredentialException("ABAC statement doesn't assert U.speaks_for(U)<-T ; Tail keyid in credential does not match keyid in caller certificate");
                }
            }
            else{
                throw new CredentialException("Tail keyid in credential or keyid in caller certificate is null");
            }

            // Assert user.speaks_for(user)<-tool . Get user cert (== signer cert) from speaks-for credential and obtain the SHA1 hash of the public key in user cert, which is the user_keyid.
            // Match user_keyid with the keyid in the ‘head' of the speaks-for credential.
            // Match string in ‘role' in ‘head' of speaks-for credential with “speaks_for_<user_keyid>”.
            // Make sure tool_keyid == keyid in ‘tail’ of speaks-for credential. // already done above
            String user_keyid = null;
            try {
                user_keyid = CertificateUtil.getCertKeyid(userGid.getCertificate());
            } catch (NoSuchAlgorithmException ex) {
                logger.error("Exception while obtaining keyid from certificate" + ex);
            } catch (IOException ex) {
                logger.error("Exception while obtaining keyid from certificate" + ex);
            }

            String head_keyid = credential.getHeadKeyid();

            if(user_keyid != null && head_keyid != null){
                if(user_keyid.equalsIgnoreCase(head_keyid)){
                    logger.info("Head keyid matches keyid in user (=signer) certificate");
                }
                else{
                    logger.error("Head keyid does not match keyid in user (=signer) certificate");
                    throw new CredentialException("ABAC statement doesn't assert U.speaks_for(U)<-T ; Head keyid does not match keyid in user (=signer) certificate");
                }
            }
            else{
                throw new CredentialException("Head keyid in credential or keyid in user(=signer) certificate is null");
            }

            String role_in_credentialhead = credential.getHeadRole();
            if(role_in_credentialhead != null && role_in_credentialhead.equalsIgnoreCase("speaks_for_" + user_keyid)){
                logger.info("role in head matches speaks_for_<user_keyid>");
            }
            else{
                logger.error("ABAC statement doesn't assert U.speaks_for(U)<-T ; role in head does not match speaks_for_<user_keyid>");
                throw new CredentialException("ABAC statement doesn't assert U.speaks_for(U)<-T ; role in head does not match speaks_for_<user_keyid>");
            }

            // Do expiry check to make sure the cred has not expired
            if(!credential.getExpirationDate().after(new Date())){
                logger.info("Speaks-for credential expired on " + credential.getExpirationDate());
                logger.error("Speaks-for Credential expired");
                throw new CredentialException("Speaks-for Credential expired");
            }
            else{
                logger.info("Speaks-for credential expires on " + credential.getExpirationDate());
                logger.info("Speaks-for Credential has not expired");
            }

            //validating all the xml signatures; xmlsec verification
            XmlDigitalSigValidator.validate(credential.getCredentialDocument());
            logger.info("xmlsec verification succeeded");

            // Validate user cert and tool cert against trust roots

            // *** Toggle paths for testing
            String CH_TRUSTSTORE_PATH = XmlRpcController.HomeDirectory + XmlRpcController.getProperty(PropertyChTruststorePath);
            String CH_TRUSTSTORE_PASS = XmlRpcController.getProperty(PropertyChTruststorePassword);
            //String CH_TRUSTSTORE_PATH = "/tmp/geni-trusted-test.jks";
            //String CH_TRUSTSTORE_PASS = "whoisthere";
            userGid.verifyCertChain(CH_TRUSTSTORE_PATH, CH_TRUSTSTORE_PASS);
            Gid toolGid = new Gid(callerCert);
            if(toolGid != null){
                toolGid.verifyCertChain(CH_TRUSTSTORE_PATH, CH_TRUSTSTORE_PASS);
            }
            else{
                throw new CredentialException("Null toolGid; can't verify tool certificate chain against trust roots");
            }

            logger.info("Validated user cert and tool cert against trust roots");

            // If code has reached here without throwing a CredentialException, it means it is safe to return the user (==signer)cert in the tool credential
            if(userGid != null){
                return userGid.getCertificate();
            }
            else{
                return null;
            }

        }


	/**
	 * Only check the client certs against a trust root
	 * @param clientCertificate
	 * @throws CredentialException
	 */
	public static void checkTrustRoot(X509Certificate[] clientCertificate) throws CredentialException {
		
	}


        // Everything below for testing

        public static void main(String[] args) {
            FileInputStream is;
            try {
                    System.out.println("Hello /world");
                    String toolCredentialString = FileUtils.readFileToString(new File("/tmp/tool-cred.xml"), "UTF-8" );

                    // checking userDN
                    
                    Credential credential;
                    try{
                        credential = new Credential(toolCredentialString);
                    } catch(Exception exception){
                        System.out.println(exception.getMessage());
                        throw new CredentialException("Exception parsing speaks_for credential xml while setting userDN");
                    }
                
                    Gid userGid = credential.getSignature().getIssuerGid(); // user gid
                    X509Certificate userCert = userGid.getCertificate(); // this is the cert of the user on behalf of whool the tool is speaking for

                    List<String> altNameStringsInUserCert = null;
                
                    try {
                        String SUBJECT_ALTERNATIVE_NAME = "2.5.29.17";
                        altNameStringsInUserCert = getExtensionValue(userCert, SUBJECT_ALTERNATIVE_NAME);
                        for (String altNameInUserCert: altNameStringsInUserCert) {
                            System.out.println("User certificate (speaks-for) contains subject alternative name: " + altNameInUserCert);
                        }
                    } catch (Exception e) {
                        System.out.println("Exception while getting extensionvalue");
                    }
                
                    String speaksforUserDN = ((altNameStringsInUserCert != null) && (altNameStringsInUserCert.size() > 0)) ? altNameStringsInUserCert.toString() : userCert.getSubjectDN().getName();
                    System.out.println("speaksforUserDN = " + speaksforUserDN);
                    
                    
                    is = new FileInputStream("/tmp/tool-cert.pem");
                    CertificateFactory x509CertFact = CertificateFactory.getInstance("X.509");
                    X509Certificate tool_cert = (X509Certificate)x509CertFact.generateCertificate(is);

                    //String spokenfor_user_urn = "urn:publicid:IDN+geni:gpo:gcf+user+user1";
                    String spokenfor_user_urn = "urn:publicid:IDN+ch.geni.net+user+tmitchel";

                    // validating speaks for credential

                    X509Certificate user_cert = validateSpeaksForCredential(toolCredentialString, spokenfor_user_urn, tool_cert);

                    System.out.println("DONE validateSpeaksForCredential");
                    System.out.println("Further credential verification using user cert with subject = " + user_cert.getSubjectX500Principal());


                    //DERInputStream inp = new DERInputStream(new ByteArrayInputStream(cert.getPublicKey().getEncoded()));
                    //print(inp.readObject());


                    //System.out.println("DONE");
                    
                    //String thumbprint = getThumbPrint(cert);
                    //System.out.println(thumbprint);

            } catch (FileNotFoundException e) {
                    e.printStackTrace();
            } catch (CertificateException e) {
                    e.printStackTrace();
            //} catch (NoSuchAlgorithmException e) {
                    //e.printStackTrace();
            } catch (Exception e){
                    e.printStackTrace();
            }

        }

        public static String getThumbPrint(X509Certificate cert)
            throws NoSuchAlgorithmException, CertificateEncodingException {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] der = cert.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            System.out.println(Arrays.toString(digest));
            return hexify(digest);

        }

        public static String hexify (byte bytes[]) {

            char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7',
                            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

            StringBuffer buf = new StringBuffer(bytes.length * 2);

            for (int i = 0; i < bytes.length; ++i) {
                    buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
                buf.append(hexDigits[bytes[i] & 0x0f]);
            }

            return buf.toString();
        }

        static void print(DEREncodable obj) throws NoSuchAlgorithmException {
            if (obj instanceof ASN1Sequence) {
                
                Enumeration seq = ((ASN1Sequence) obj).getObjects();
                while (seq.hasMoreElements()) {
                    print((DEREncodable) seq.nextElement());
                }
                
            } else {
                if (obj instanceof DERObjectIdentifier) {
                    //System.out.println(((DERObjectIdentifier) obj).getId());
                    //Do nothing
                }
                if (obj instanceof DERBitString) {
                    //System.out.println((new BigInteger(((DERBitString)obj).getBytes())).toString(16));
                    MessageDigest md = MessageDigest.getInstance("SHA-1");
                    byte[] der = ((DERBitString)obj).getBytes();
                    md.update(der);
                    byte[] digest = md.digest();
                    System.out.println(hexify(digest));
                    
                }
            }
         }





}