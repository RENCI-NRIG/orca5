package net.exogeni.orca.shirako.common.currency;

import net.exogeni.orca.shirako.common.delegation.IResourceTicketFactory;
import net.exogeni.orca.shirako.common.delegation.ResourceDelegation;
import net.exogeni.orca.shirako.common.delegation.SharpCertificate;
import net.exogeni.orca.shirako.common.delegation.SharpResourceTicket;


public class CreditsNote extends SharpResourceTicket
{
	protected CreditsNote() {}
	
    public CreditsNote(IResourceTicketFactory factory, ResourceDelegation delegation, SharpCertificate certificate)
    {
        super(factory, delegation, certificate);
    }
    
    public CreditsNote(IResourceTicketFactory factory, CreditsNote source, ResourceDelegation delegation, SharpCertificate certificate)
    {
        super(factory, source, delegation, certificate);
    }
    
    public CreditsNote(IResourceTicketFactory factory, CreditsNote[] sources, ResourceDelegation delegation, SharpCertificate certificate, SharpCertificate[] otherCertificates)
    {
        super(factory, sources, delegation, certificate, otherCertificates);
    }

}
