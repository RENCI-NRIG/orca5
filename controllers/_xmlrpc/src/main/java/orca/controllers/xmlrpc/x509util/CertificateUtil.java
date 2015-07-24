package orca.controllers.xmlrpc.x509util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.KeyStore;
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
import java.util.List;

import orca.util.Base64;

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
}
