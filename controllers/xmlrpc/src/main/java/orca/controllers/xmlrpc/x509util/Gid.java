package orca.controllers.xmlrpc.x509util;

import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import orca.controllers.OrcaController;

import org.apache.log4j.Logger;

public class Gid {
	
	private X509Certificate certificate;
	private List<X509Certificate> certificateChain;
	private UUID uuid; //8cc8545b-7cb6-42dd-8519-c3012221b2e9
	private String urn; //urn:publicid:IDN+plc:bbn+authority+sa
	private String hrn; //plc.bbn (sa gets dropped, in other cases plc.bbn.name)
	private String type; //authority
	
	static final String URN_PREFIX = "urn:publicid:IDN";

        
	protected static Logger logger = OrcaController.getLogger(Gid.class.getSimpleName());
        //protected static Logger logger = Logger.getLogger(Gid.class.getName());

	public Gid(String gid) throws CertificateException, IOException {
		this.certificateChain = CertificateUtil.getCertChain(gid);
		this.certificate = this.certificateChain.get(0);
		decode();
	}
	
	public Gid(X509Certificate certificate) throws CertificateException {
		this.certificate = certificate;
		this.certificateChain = new ArrayList<X509Certificate>();
		this.certificateChain.add(this.certificate);
		decode();
	}
	
	/**
	 * decodes the certificate to get the urn, hrn and type
	 * @throws CertificateException
	 */
	void decode() throws CertificateException{
		Collection<List<?>> collection = this.certificate.getSubjectAlternativeNames();
		Iterator<List<?>> itr = collection.iterator();
		
		while(itr.hasNext()){
			List<?> alternativeNames = itr.next();
			//looking for only Uniform Resource Identifier
			if((Integer)alternativeNames.get(0) != 6){
				continue;
			}
			String alternativeName = (String)alternativeNames.get(1);
			/*if(alternativeName.toLowerCase().startsWith("http://<params>")){
				
			}*/
			if(alternativeName.toLowerCase().startsWith("urn:uuid:")){
				//the last 36 characters represent the UUID eg string urn:uuid:8cc8545b-7cb6-42dd-8519-c3012221b2e9
				this.uuid = UUID.fromString(alternativeName.substring(alternativeName.length()-36, alternativeName.length()));
			}else if(alternativeName.toLowerCase().startsWith("urn:publicid:idn+")){
				//urn:publicid:IDN+plc:bbn+authority+sa
				this.urn = alternativeName;
			}
		}
		
		setHrnAndType(this.urn);
	}
	
	/**
	 * decodes the urn to get hrn and type
	 * @param urn
	 * @throws CertificateException
	 */
	void setHrnAndType(String urn) throws CertificateException {
		
		if(!urn.startsWith(URN_PREFIX)){
			throw new CertificateException("Invalid URN");
		}
		
		List<String> parts = getUrnSplit();
		
		//the second last part represents the type
		type = parts.remove(parts.size() - 2);
		
		// Remove the authority name (e.g. '.sa')
		if(type.equals("authority")){
			String name = parts.remove(parts.size() - 1);
			
			//Drop the sa. This is a bad hack, but its either this or completely change how record types are generated/stored
			if(!name.equals("sa")){
				type = type + "+" + name;
			}
		}
		
		// convert parts into hrn by doing the following
        // 1. remove blank parts
        // 2. escape dots inside parts
        // 3. replace ':' with '.' inside parts
        // 4. join parts using '.'
        
		StringBuffer hrnBuffer = new StringBuffer();
		
		for(int itr = 0; itr < parts.size(); itr++){
			//append only if string isn't blank
			if(parts.get(itr).trim().length() > 0){
				hrnBuffer.append(parts.get(itr).trim().replaceAll("\\.", "\\\\.").replaceAll(":", "."));
				hrnBuffer.append(".");
			}
		}
		
		//removing the "." at the end
		hrn = hrnBuffer.toString().substring(0, hrnBuffer.length() - 1);
	}
	
	/**
	 * finds if the gid is signed by the passed gid
	 * @param signer
	 * @return
	 */
	public boolean isSignedBy(Gid signer){
		try{
			this.certificateChain.get(0).verify(signer.certificateChain.get(0).getPublicKey());
		}catch(Exception exception){
			return false;
		}
		return true;
	}
	
	/**
	 * verifies if the certificate chain has a path up to a trusted authority
	 * also if the namespace constraints are followed
	 * @param trustKeyStorePath
	 * @param keyStorePassword
	 * @throws CertPathValidatorException
	 * @throws CertificateException
	 */
	public void verifyCertChain(String trustKeyStorePath, String keyStorePassword) throws CertPathValidatorException, CertificateException{
		
		X509Certificate trustAnchor = CertificateUtil.verifyCertChain(this.certificateChain, trustKeyStorePath, keyStorePassword);
		
		Gid subjectGid, issuerGid;
		
		Iterator<X509Certificate> certChainItr = this.certificateChain.iterator();
		subjectGid = new Gid(certChainItr.next());
		
		while(certChainItr.hasNext()){
			issuerGid = new Gid((X509Certificate)certChainItr.next());
			verifyIssuerDomain(subjectGid, issuerGid);
			subjectGid = issuerGid;
		}
		
		issuerGid = new Gid(trustAnchor);
		verifyIssuerDomain(subjectGid, issuerGid);
	}
	
	/**
	 * verifies if the issuers namespace is a prefix of the subjects namespace
	 * @param subjectGid
	 * @param issuerGid
	 * @throws CertificateException
	 */
	private void verifyIssuerDomain(Gid subjectGid, Gid issuerGid) throws CertificateException{
		logger.debug("Subject HRN: " + subjectGid.getHrn());
		logger.debug("Issuer HRN: " + issuerGid.getHrn());
		
    	if(!subjectGid.getHrn().startsWith(issuerGid.getHrn())){
    		throw new CertificateException("Invalid Gid Chain");
    	}
	}
	
	//urn:publicid:IDN+plc:bbn+authority+sa
	public String getUrn(){
		return this.urn;
	}
	
	//+plc:bbn+authority+sa
	String getUrnMeaningful(){
		return this.urn.substring(URN_PREFIX.length());
	}
	
	//[plc:bbn, authority, sa]
	List<String> getUrnSplit(){
		//the extra new ArrayList<String> is to make the list mutable
		return new ArrayList<String>(Arrays.asList(this.getUrnMeaningful().split("\\+")));
	}
	
	//plc.bbn (sa gets dropped, in other cases plc.bbn.name)
	public String getHrn(){
		return this.hrn;
	}
	
	//8cc8545b-7cb6-42dd-8519-c3012221b2e9
	public UUID getUuid(){
		return this.uuid;
	}
	
	//authority
	public String getType(){
		return this.type;
	}
	
	public X509Certificate getCertificate(){
		return this.certificate;
	}
	
	/*public List<X509Certificate> getCertificateChain(){
		return this.certificateChain;
	}*/
	
	@Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof Gid))return false;
        Gid otherGid = (Gid)other;
        return (certificate.equals(otherGid.certificate));
    }
	
	@Override
	public int hashCode() { 
		return certificate.hashCode();
	}
	
	@Override
    public String toString()
    {
        return "(" + urn.toString() + ")";
    }
}
