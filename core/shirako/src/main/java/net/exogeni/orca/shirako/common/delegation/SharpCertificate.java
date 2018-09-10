package net.exogeni.orca.shirako.common.delegation;

import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Date;

import net.exogeni.orca.util.ID;

import org.apache.log4j.Logger;

public class SharpCertificate
{
	static Logger logger = Logger.getLogger(SharpCertificate.class.getCanonicalName());
	
	protected ID id;
	
	protected Date ts;
	
	protected SharpCertificate issuerCertificate;
	
	protected Certificate holderCertificate;
	
	protected byte[] signature;
	
		
	public SharpCertificate(SharpCertificate issuerCertificate, Certificate holderCertificate)
	{
		this.id = new ID();
		this.ts = new Date();
		this.issuerCertificate = issuerCertificate;
		this.holderCertificate = holderCertificate;
	}

	public boolean isValid()
	{
		return validateSignatures(issuerCertificate, holderCertificate, signature);
	}
	
	/**
	 * Checks if the sharp certificate is rooted at the specified master certificate.
	 * @param root root 
	 * @return true or false
	 */
	public boolean startsWith(Certificate root)
	{
		return checkStartsWith(this, root);
	}
	
	public Certificate getRoot()
	{
		if (isSelfSigned()){
			return holderCertificate;
		} else {
			return issuerCertificate.getRoot();
		}
	}
	
	protected boolean checkStartsWith(SharpCertificate sc, Certificate root)
	{
		try {
			if (sc.isSelfSigned()){
				if (root == sc.holderCertificate){
					return true;
				}
				return false;
			}
			return checkStartsWith(sc.issuerCertificate, root);
		}catch(Exception e){
			logger.error("checkStartsWith failed", e);
			return false;
		}
	}
	/**
	 * Validates recursively the certificate chain.
	 * @param issuer issuer
	 * @param holder holder 
	 * @param signature signature
	 * @return true or false
	 */
	protected boolean validateSignatures(SharpCertificate issuer, Certificate holder, byte[] signature)
	{
		try {
			// termination condition
			if (issuer.isSelfSigned()){
				return true;
			}
			PublicKey pk = issuer.getHolderCertificate().getPublicKey();
			byte[] bytes = holder.getEncoded();
			// intialize the signature object
			Signature rsa = Signature.getInstance("MD5withRSA"); 
			rsa.initVerify(pk);	
			// add the data
			rsa.update(bytes, 0, bytes.length);
			boolean result = rsa.verify(signature);
			
			return result && validateSignatures(issuer.getIssuerCertificate(), issuer.getHolderCertificate(), issuer.signature);
		}catch (Exception e){
			logger.error("signature validation failed", e);
			return false;
		}
	}
	
	public ID getId() {
		return id;
	}

	public void setId(ID id) {
		this.id = id;
	}

	public Date getTs() {
		return ts;
	}

	public void setTs(Date ts) {
		this.ts = ts;
	}

	public SharpCertificate getIssuerCertificate() {
		return issuerCertificate;
	}

	public void setIssuerCertificate(SharpCertificate issuerCertificate) {
		this.issuerCertificate = issuerCertificate;
	}

	public Certificate getHolderCertificate() {
		return holderCertificate;
	}

	public void setHolderCertificate(Certificate holderCertificate) {
		this.holderCertificate = holderCertificate;
	}

	public byte[] getSignature() {
		return signature;
	}

	public void setSignature(byte[] signature) {
		this.signature = signature;
	}
	
	public boolean isSelfSigned()
	{
		// a self-signed sharp certificate simply contains 
		// a holder certificate. There is no need for a signature.
		
		return (issuerCertificate == null && signature == null);
	}	
}
