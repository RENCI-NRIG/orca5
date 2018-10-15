package net.exogeni.orca.shirako.common.delegation;

import java.util.Properties;

import net.exogeni.orca.shirako.common.ResourceVector;
import net.exogeni.orca.shirako.time.Term;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.ResourceType;

public class SignedResourceDelegation extends ResourceDelegation {
	protected DelegationSignature signature;

	public SignedResourceDelegation(int units, ResourceVector vector,
			Term term, ResourceType type, ID[] sources, ResourceBin[] bins,
			Properties properties, ID issuer, ID holder) {
		super(units, vector, term, type, sources, bins, properties, issuer,
				holder);
	}

	public DelegationSignature getSignature()
	{
		return signature;
	}
	
	public void setSignature(DelegationSignature signature)
	{
		this.signature = signature;
	}
	
	public boolean isSigned()
	{
		return signature != null && signature.getSignature() != null && signature.getTimestamp() != null;
	}
}
