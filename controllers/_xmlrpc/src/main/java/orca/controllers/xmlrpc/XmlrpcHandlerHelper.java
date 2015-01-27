package orca.controllers.xmlrpc;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.Deflater;

import javax.security.auth.login.CredentialException;

import orca.controllers.xmlrpc.x509util.CertificateUtil;
import orca.controllers.xmlrpc.x509util.CredentialValidator;
import orca.network.InterCloudHandler;
import orca.security.AbacUtil;
import orca.security.AuthToken;
import orca.shirako.api.IReservation;
import orca.shirako.api.IServiceManager;
import orca.shirako.api.ISlice;
import orca.shirako.common.SliceID;
import orca.shirako.container.OrcaXmlrpcServlet;
import orca.shirako.kernel.ResourceSet;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.XmlRpcNoSuchHandlerException;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;

import com.sun.org.apache.xml.internal.resolver.helpers.PublicId;

import edu.emory.mathcs.backport.java.util.Arrays;

public class XmlrpcHandlerHelper {

	private static final String SUBJECT_ALTERNATIVE_NAME = "2.5.29.17";

	/**
	 * 
	 * @param filePath
	 * @return
	 * @throws java.io.IOException
	 */
	public static String readFileAsString(String filePath) throws java.io.IOException {
		byte[] buffer = new byte[(int) new File(filePath).length()];
		FileInputStream f = new FileInputStream(filePath);
		f.read(buffer);
		return new String(buffer);
	}

	/**
	 * To deal with RFC3339 Date
	 * @param datestring
	 * @return
	 * @throws java.text.ParseException
	 */
	public static Date parseRFC3339Date(String datestring) throws java.text.ParseException {
		Date d = new Date();
		//if there is no time zone, we don't need to do any special parsing.
		if(datestring.endsWith("Z")){
			SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");//spec for RFC3339
			d = s.parse(datestring);
			return d;
		}

		//step one, split off the timezone.
		char timezoneSeparator;
		if(datestring.contains("+")){
			timezoneSeparator = '+';
		}else{
			timezoneSeparator = '-';
		}
		
		String firstpart = datestring.substring(0,datestring.lastIndexOf(timezoneSeparator));
		String secondpart = datestring.substring(datestring.lastIndexOf(timezoneSeparator));

		//step two, remove the colon from the timezone offset
		secondpart = secondpart.substring(0,secondpart.indexOf(':')) + secondpart.substring(secondpart.indexOf(':')+1);
		datestring  = firstpart + secondpart;
		SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");//spec for RFC3339
		try{
			d = s.parse(datestring);
		}
		catch(java.text.ParseException pe){//try again with optional decimals
			s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");//spec for RFC3339 (with fractional seconds)
			s.setLenient(true);
			d = s.parse(datestring);
		}
		return d;
	}
	
	public void setAbacAttributes(IReservation reservation, Logger logger) throws Exception{
		
		// insecure comms are not allowed
    	if (OrcaXmlrpcServlet.getSslSessionId() == null || OrcaXmlrpcServlet.getClientCertificateChain() == null) {
    		logger.error("Client " + OrcaXmlrpcServlet.getClientIpAddress() + " is not using secure communications, operations are not allowed");
    		throw new CredentialException("Client " + OrcaXmlrpcServlet.getClientIpAddress() + " is not using secure communications, operations are not allowed");
    	}
		
		X509Certificate[] clientCertificateChain = OrcaXmlrpcServlet.getClientCertificateChain();
		
		if(clientCertificateChain[0] == null){
        	throw new CertificateException("Caller's certificate is missing.");
        }
		
		AuthToken caller = new AuthToken();
		caller.setCertificate(clientCertificateChain[0]);
		ResourceSet rset = reservation.getResources();
		
        AbacUtil.setRequesterAuthToken(rset.getRequestProperties(), caller);
        AbacUtil.setRequesterAuthToken(rset.getConfigurationProperties(), caller);
	}
	
	/**
	 * Compress (gzip) and base64 encode the string
	 * @param res
	 * @return
	 */
	protected static String compressEncode(String inputString) {
		// compress
		byte[] input = inputString.getBytes();
		ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

		Deflater comp = new Deflater();
		comp.setLevel(Deflater.BEST_SPEED);
		comp.setInput(input);
		comp.finish();

		byte[] tmp = new byte[1024];
		while(!comp.finished()) {
			int count = comp.deflate(tmp);
			bos.write(tmp, 0, count);
		}

		try {
			bos.close();
		} catch (IOException e) {
			;
		}
		// base64-encode
		byte[] resBytes = Base64.encodeBase64(bos.toByteArray());

		String res = new String(resBytes);

		return res;
	}
	
    public static IReservation[] getAllSliceReservations(XmlrpcOrcaState instance, String slice_urn, Object[] credentials) throws Exception, 
    CredentialException, XmlRpcNoSuchHandlerException, XmlRpcException {

    	IServiceManager sm = instance.getSM();
    	XmlrpcController controller = instance.getController();
    	Logger logger = sm.getLogger();
    	//h = instance.getHandler();
    	InterCloudHandler h=instance.getHandler(slice_urn);
    	
    	// map from user slice name to ORCA slice ID
    	SliceID sliceIdReal = XmlrpcOrcaState.getInstance().getSliceID(slice_urn);
    	if (sliceIdReal == null)
    		throw new Exception("ERROR: Unknown slice urn " + slice_urn);

    	String sliceId = sliceIdReal.toString();
    	logger.debug("Got sliceId as " + sliceId.trim());

    	ISlice[] slices = (ISlice[]) sm.getSlices();

    	if(controller==null)
    		throw new Exception("ERROR: XMLrpcController not initiated: " + slice_urn);

    	if(h==null)
    		throw new Exception("ERROR: InterDomainHandler not initiated: " + slice_urn);

    	if(slices == null){
    		logger.error("ERROR: No slices for service manager");
    		throw new Exception("ERROR: No slices found for service manager");
    	}
    	else{
    		IReservation [] allRes = null;
    		for(int i=0; i<slices.length; i++){
    			String currSliceId = slices[i].getSliceID().toString();
    			String inputSliceId = sliceId.trim();
    			if(currSliceId.equalsIgnoreCase(inputSliceId)){
    				allRes = (IReservation[]) sm.getReservations(slices[i].getSliceID());
    			}
    		}
    		return allRes;
    	}
    }
    
	/**
	 * Function to do the GENI (non ABAC) authorization check, if required.
	 * @param sliceUrn
	 * @param credentials
	 * @param requiredPrivilege
	 * @throws CredentialException
	 */
	public static void validateGeniCredential(String sliceUrn, Object[] credentials, String[] requiredPrivilege, 
			boolean verifyCredentials, Logger logger) throws CredentialException{
		
		if(!verifyCredentials){
			return;
		}
		
		// insecure comms are not allowed
    	if (OrcaXmlrpcServlet.getSslSessionId() == null || OrcaXmlrpcServlet.getClientCertificateChain() == null) {
    		logger.error("Client " + OrcaXmlrpcServlet.getClientIpAddress() + " is not using secure communications, operations are not allowed");
    		throw new CredentialException("Client " + OrcaXmlrpcServlet.getClientIpAddress() + " is not using secure communications, operations are not allowed");
    	}
		
		X509Certificate[] clientCertificateChain = OrcaXmlrpcServlet.getClientCertificateChain();
		
		if (clientCertificateChain.length > 0)
			logger.info("validateGeniCredential(): Client presented a certificate with subject: " + clientCertificateChain[0].getSubjectX500Principal());
		
		Set<String> privilegeList = null;
		
		if(requiredPrivilege != null && requiredPrivilege.length > 0){
			privilegeList = new HashSet<String>();
			for(int count = 0; count < requiredPrivilege.length; count++){
				privilegeList.add(requiredPrivilege[count]);
			}
		}
		
		for(int itr=0; itr < credentials.length; itr++){
			try{
				CredentialValidator.validateCredential(sliceUrn, (String)credentials[itr], privilegeList, clientCertificateChain);
				return;
			} catch(Exception exception){
				logger.debug(exception.getMessage());
				//check next credential
			}
		}
		
		throw new CredentialException("No credential was found with appropriate privileges.");
	}

	/**
	 * Orca light credential validation - simply check that the presented cert is still valid and traceable to a known trust root.
	 * Ignores all input parameters and gets the client cert chain from the servlet. (11/29/2011) /ib
	 * @param sliceUrn
	 * @param credentials
	 * @param requiredPrivilege
	 * @param logger
	 * @param verifyCredentials
	 * @throws CredentialException
	 */
	@SuppressWarnings("unchecked")
	public static void validateOrcaCredential(String sliceUrn, Object[] credentials, String[] requiredPrivilege, 
			boolean verifyCredentials, Logger logger) throws CredentialException {
		
		if (!verifyCredentials) {
			return;
		}
		
		// insecure comms are not allowed
    	if (OrcaXmlrpcServlet.getSslSessionId() == null || OrcaXmlrpcServlet.getClientCertificateChain() == null) {
    		logger.error("Client " + OrcaXmlrpcServlet.getClientIpAddress() + " is not using secure communications, operations are not allowed");
    		throw new CredentialException("Client " + OrcaXmlrpcServlet.getClientIpAddress() + " is not using secure communications, operations are not allowed");
    	}
		
		X509Certificate[] clientCertificateChain = OrcaXmlrpcServlet.getClientCertificateChain();
		
		if (clientCertificateChain.length > 0) {
			logger.info("validateOrcaCredential(): Client presented a certificate with subject: " + clientCertificateChain[0].getSubjectX500Principal());

			try {
				List<String> altNameStrings = getExtensionValue(clientCertificateChain[0], SUBJECT_ALTERNATIVE_NAME);
				for (String altName: altNameStrings) {
					logger.info("Certificate contains subject alternative name: " + altName);
				}
			} catch (Exception e) {
				;
			}
		}
		
		// check the expiration date on the presented cert chain
		try {
			for(int i = 0; i < clientCertificateChain.length; i++)
				clientCertificateChain[i].checkValidity();
		} catch (Exception e) {
			throw new CredentialException("Certificate invalid: " + e);
		}
		
		// and that it can be traced to a trust root
		String CH_TRUSTSTORE_PATH = XmlrpcController.getProperty(CredentialValidator.PropertyChTruststorePath);
        String CH_TRUSTSTORE_PASS = XmlrpcController.getProperty(CredentialValidator.PropertyChTruststorePassword);
		
        try {
        	CertificateUtil.verifyCertChain((List<X509Certificate>)Arrays.asList(clientCertificateChain), 
        			CH_TRUSTSTORE_PATH, CH_TRUSTSTORE_PASS);
        } catch (CertPathValidatorException e) {
        	throw new CredentialException("Unable to validate trust root: " + e);
        }
	}
	
	/**
	 * Get extension value as string from a cert
	 * @param X509Certificate
	 * @param oid
	 * @return
	 * @throws IOException
	 */
	public static List<String> getExtensionValue(X509Certificate X509Certificate, String oid) throws IOException
	{
	    byte[] extensionValue = X509Certificate.getExtensionValue(oid);
	    List<String> ret = null;
	    
	    if (extensionValue != null)
	    {
	        DERObject derObject = toDERObject(extensionValue);
	        
	        ret = getDERStrings(derObject);
	        System.out.println("List of strings: " + ret);
	    }
	    return ret;
	}
	
	/**
	 * Try to recursively extract strings from DER object
	 * @param derObject
	 * @return
	 */
	private static List<String> getDERStrings(DERObject derObject) {

		if (derObject == null)
			return null;
		
		List<String> ret = new ArrayList<String>();

		if (derObject instanceof DEROctetString)
		{
			DEROctetString derOctetString = (DEROctetString) derObject;

			derObject = toDERObject(derOctetString.getOctets());
			List<String> res = getDERStrings(derObject);
			if (res != null)
				ret.addAll(res);
			else
				ret.add(new String(derOctetString.getOctets()));
			return ret;
		}
		if (derObject instanceof DERSequence) {
			DERSequence derSeq = (DERSequence)DERSequence.getInstance(derObject);

			for (int i = 0; i < derSeq.size(); i++) {
				DEREncodable derEnc = derSeq.getObjectAt(i);
				List<String> res = getDERStrings(derEnc.getDERObject());
				if (res != null)
					ret.addAll(res);
			}
			return ret;
		}
		if (derObject instanceof DERUTF8String)
		{
			DERUTF8String s = DERUTF8String.getInstance(derObject);
			ret.add(s.getString());
			return ret;
		}

		if (derObject instanceof DERTaggedObject) {
			DERTaggedObject derTO = (DERTaggedObject)derObject.getDERObject();
			List<String> res = getDERStrings(derTO.getObject());
			if (res != null)
				ret.addAll(res);
			return ret;
		}

		return ret;
	}
	
	
	/**
	 * Convert byte string to DER Object
	 * @param data
	 * @return
	 * @throws IOException
	 */
	private static DERObject toDERObject(byte[] data) 
	{
		try {
			ByteArrayInputStream inStream = new ByteArrayInputStream(data);
			ASN1InputStream asnInputStream = new ASN1InputStream(inStream);

			return asnInputStream.readObject();
		} catch (Exception e) {
			return null;
		}
	}
	
	public static void main (String[] args) {
		try {
			InputStream inStream = new FileInputStream("/Users/ibaldin/.ssl/IliaBaldine-Emulab/emulab-tmp.pem");
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate)cf.generateCertificate(inStream);
			List<String> altNames = getExtensionValue(cert, SUBJECT_ALTERNATIVE_NAME);
			System.out.println("Alt name is: " + altNames);
			for (String altName: altNames) {
				try {
					URI altNameUri = new URI(altName);
					String r = PublicId.decodeURN(altName);
					System.out.println("Authority: " + altNameUri.getAuthority());
					System.out.println("Scheme-specific part: " + altNameUri.getSchemeSpecificPart());
					System.out.println("Decoded: " + r);
					if (r.matches("IDN .+ user .+")) 
						System.out.println("Matches!");
				} catch(Exception e) { 
					;
				}
			}
			
		} catch (Exception e) {
			System.err.println("Error encountered: " + e);
			e.printStackTrace();
		}
		
	}
}
