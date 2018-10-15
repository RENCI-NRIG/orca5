package net.exogeni.orca.shirako.common.delegation;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.Properties;

import net.exogeni.orca.shirako.common.ResourceVector;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.ResourceType;

import com.thoughtworks.xstream.XStream;

public class SignedResourceTicketFactory extends SimpleResourceTicketFactory
{
	protected XStream xsmForSigning;
	
	private boolean initialized = false;
	
	public SignedResourceTicketFactory()
	{
		xsmForSigning = new XStream();
	}
	@Override
	public void initialize() throws Exception
	{
		if (!initialized){
			super.initialize();
			initialize(xsmForSigning);
			xsmForSigning.omitField(SignedResourceDelegation.class, "signature");
			initialized = true;
		}
	}
	
	@Override
	protected void initialize(XStream xs)
	{
		super.initialize(xs);
		
		xs.alias("signature", DelegationSignature.class);
		xs.alias("signedDelegation", SignedResourceDelegation.class);		
	}
	
	@Override
	public ResourceDelegation makeDelegation(int units, ResourceVector vector,
			Term term, ResourceType type, ID[] sources, ResourceBin[] bins,
			Properties properties, ID holder) throws DelegationException {
		
		ensureInitialized();
		if ((sources == null && bins != null) || (sources != null && bins == null)){
			throw new DelegationException("sources and bins must both be null or non-null");
		}

		// NOTE: in this base version, we do not check if we have the holder's certificate
				
		// get the issuer's private key
		PrivateKey key = getIssuerPrivateKey();
				
		SignedResourceDelegation result = new SignedResourceDelegation(units, vector, term, type, sources, bins, properties, getIssuerID(), holder);
		
		try {
			sign(result, key);
		} catch (Exception e){
			throw new DelegationException("Failed to sign delegation", e);
		}

		return result;
	}

	public void sign(SignedResourceDelegation delegation, PrivateKey key) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException
	{
		// serialize the delegation without the signature
		String xml = xsmForSigning.toXML(delegation);
		// FIXME: charset? maybe encode to base64?
		byte[] bytes = xml.getBytes();
		// sign the xml		
		// intialize the signature object
		Signature dsa = Signature.getInstance("MD5withRSA"); 
		dsa.initSign(key);		
		// add the datas
		dsa.update(bytes, 0, bytes.length);
		// get the signature
		byte[] signature = dsa.sign();
		DelegationSignature ds = new DelegationSignature(signature);
		delegation.setSignature(ds);
	} 

	public boolean verifySignature(SignedResourceDelegation delegation, PublicKey key) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException
	{
		if (!delegation.isSigned()){
			return false;
		}
		byte[] signatureBytes = delegation.getSignature().getSignature();
		if (signatureBytes == null){
			return false;
		}
		// serialize the delegation without the signature
		String xml = xsmForSigning.toXML(delegation);
		// FIXME: charset? maybe use base64
		byte[] bytes = xml.getBytes();
		// initialize the signature object
		Signature dsa = Signature.getInstance("MD5withRSA"); 
		dsa.initVerify(key);
		// add the data
		dsa.update(bytes, 0, bytes.length);
		
		// check the signature
		return dsa.verify(signatureBytes);
	} 	

	/**
	 * Obtains the certificate of the specified actor.
	 * @param holder actor identity
	 * @return Certificate
	 */
	protected Certificate getHolderCertificate(ID holder)
	{
		Certificate cert = null;
		try {
			cert = actor.getShirakoPlugin().getKeyStore().getCertificate(holder.toString());
		} catch (Exception e){
			actor.getLogger().error("failed to obtain actor certificate. Actor id:=" + holder.toString(), e);
		}
		return cert;
	}
		
	protected Certificate getIssuerCertificate()
	{
		Certificate cert = null;
		try {
			cert = actor.getShirakoPlugin().getKeyStore().getActorCertificate();
		} catch (Exception e){
			actor.getLogger().error("failed to obtain actor certificate", e);
		}
		return cert;
	}

	protected PrivateKey getIssuerPrivateKey()
	{
		PrivateKey key = null;
		try {
			key = actor.getShirakoPlugin().getKeyStore().getActorPrivateKey();
		} catch (Exception e){
			actor.getLogger().error("failed to obtain actor private key", e);
		}
		return key;
	}
}
