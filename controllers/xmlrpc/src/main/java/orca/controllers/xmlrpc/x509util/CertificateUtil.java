package orca.controllers.xmlrpc.x509util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import orca.util.Base64;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.DERObjectIdentifier;

public class CertificateUtil {
	
	/**
	 * converts a certificate chain string in to a list of certificate objects
	 * @param certChainString
	 * @return list of certificate objects
	 * @throws CertificateException
	 * @throws IOException
	 */
	public static List<X509Certificate> getCertChain(String certChainString) throws CertificateException, IOException{
		
		ArrayList<X509Certificate> certificateChain = new ArrayList<X509Certificate>();
		
		if(certChainString.indexOf("-----BEGIN CERTIFICATE") != -1){
        	
        	BufferedReader bufferedReader = new BufferedReader(new StringReader(certChainString));
        	
        	String nextLine = bufferedReader.readLine();
        	while(nextLine != null && nextLine.indexOf("-----BEGIN CERTIFICATE") != 1){
        		StringBuffer stringbuffer = new StringBuffer();
				String s1;
		        for (; (s1 = bufferedReader.readLine()) != null && s1.indexOf("-----END CERTIFICATE") == -1; stringbuffer.append(s1.trim()));
		        
		        if (s1 == null)
					throw new IOException((new StringBuilder()).append("-----END CERTIFICATE").append(" not found").toString());
		        
				certificateChain.add(createCertFromPem(stringbuffer.toString()));
				
				nextLine = bufferedReader.readLine();
        	}
	        
        }else{
			certificateChain.add(createCertFromPem(certChainString));
        }
		
		return certificateChain;
	}
	
	/**
	 * converts a pem encoded certificate string in to a certificate object
	 * @param pemString
	 * @return
	 * @throws CertificateException
	 */
	public static X509Certificate createCertFromPem(String pemString) throws CertificateException{
		ByteArrayInputStream bytearrayinputstream = new ByteArrayInputStream(Base64.decode(pemString));
        CertificateFactory certificatefactory = CertificateFactory.getInstance("X.509");
		X509Certificate certificate = (X509Certificate) certificatefactory.generateCertificate(bytearrayinputstream);
		return certificate;
	}
	
	/**
	 * verifies if the given certificate chain has a path up to a trusted authority
	 * @param certChain
	 * @param trustKeyStorePath
	 * @param keyStorePassword
	 * @return the trusted root authority
	 * @throws CertPathValidatorException - if a path does not exist
	 */
	public static X509Certificate verifyCertChain(List<X509Certificate> certChain, String trustKeyStorePath, String keyStorePassword)  throws CertPathValidatorException{
		
		try{
			/* Givens. */
			InputStream trustStoreInput = new FileInputStream(trustKeyStorePath);
			char[] password = keyStorePassword.toCharArray();
			List<X509Certificate> chain = certChain;
			//Collection<X509CRL> crls = new ArrayList<X509CRL>();

			/* Construct a valid path. */
			KeyStore anchors = KeyStore.getInstance(KeyStore.getDefaultType());
			anchors.load(trustStoreInput, password);
			
			X509CertSelector target = new X509CertSelector();
			target.setCertificate(chain.get(0));
			
			PKIXBuilderParameters params = new PKIXBuilderParameters(anchors, target);
			
			CertStoreParameters intermediates = new CollectionCertStoreParameters(chain);
			params.addCertStore(CertStore.getInstance("Collection", intermediates));
			
			params.setRevocationEnabled(false);
			
			//CertStoreParameters revoked = new CollectionCertStoreParameters(crls);
			//params.addCertStore(CertStore.getInstance("Collection", revoked));
			
			CertPath certPath = CertificateFactory.getInstance("X.509").generateCertPath(chain);
			//CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
			//CertPath certPath = builder.build(params).getCertPath();
			
			CertPathValidator validator = CertPathValidator.getInstance("PKIX");
			PKIXCertPathValidatorResult pkixCertPathValidatorResult = (PKIXCertPathValidatorResult)validator.validate(certPath, params);
			
			return pkixCertPathValidatorResult.getTrustAnchor().getTrustedCert();
			
		}catch(Exception exception){
			throw new CertPathValidatorException("A valid certificate path does not exist" + System.getProperty("line.separator") + exception.getMessage());
		}
	}

        /**
         * Method to get the keyid of a certificate; Calls getKeyidFromDER to do the actual work
         * @param cert
         * @return string representing the sha-1 hash of the public key in cert
         * @throws NoSuchAlgorithmException
         * @throws IOException
         */

        public static String getCertKeyid(X509Certificate cert) throws NoSuchAlgorithmException, IOException{

            if(cert == null){
                return null;
            }

            DERInputStream inp = new DERInputStream(new ByteArrayInputStream(cert.getPublicKey().getEncoded())); // get encoded pub key from cert
            String keyid = getKeyidFromDER(inp.readObject());
            if(keyid != null){
                return keyid;
            }
            else{
                return null;
            }

        }

        /**
         * Method to find keyid from a DER encoded object (public key in this case)
         * @param obj
         * @return
         * @throws NoSuchAlgorithmException
         */
        public static String getKeyidFromDER(DEREncodable obj) throws NoSuchAlgorithmException {
            String returnString = null;
            if (obj instanceof ASN1Sequence) {

                Enumeration seq = ((ASN1Sequence) obj).getObjects();
                while (seq.hasMoreElements()) {
                    String hexString = getKeyidFromDERIdOrBitString((DEREncodable) seq.nextElement());
                    if(hexString != null){
                        returnString = hexString;
                    }
                }

            } 

            return returnString;
         }

        public static String getKeyidFromDERIdOrBitString(DEREncodable obj) throws NoSuchAlgorithmException {
            String hexString = null;
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
                hexString = hexify(digest);
            }

            return hexString;
        }

        /**
         * Method to convert to hex from byte array
         * @param bytes
         * @return
         */
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


}
