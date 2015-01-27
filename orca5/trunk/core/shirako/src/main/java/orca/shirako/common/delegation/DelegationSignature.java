package orca.shirako.common.delegation;

import java.util.Date;

import orca.util.Base64;

public class DelegationSignature
{
	protected String signature;
	
	protected Date ts;

	public DelegationSignature(byte[] bytes)
	{
		ts = new Date();
		setSignature(bytes);
	}
	
	public void setSignature(byte[] bytes)
	{
		signature = Base64.encodeBytes(bytes);
	}
	
	public byte[] getSignature()
	{
		return Base64.decode(signature);
	}
	
	public Date getTimestamp()
	{
		return ts;
	}
}