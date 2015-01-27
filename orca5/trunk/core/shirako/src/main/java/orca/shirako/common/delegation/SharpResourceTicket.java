package orca.shirako.common.delegation;


public class SharpResourceTicket extends ResourceTicket
{
	/**
	 * Sharp certificate for the key of the holder
	 * of this ticket.
	 */
	protected SharpCertificate certificate;
	
	/**
	 * Additional sharp certificates. This field should be used in case 
	 * of resource delegations, which were satisfied
	 * from multiple source tickets.
	 */
	protected SharpCertificate[] otherCertificates;
	
	protected SharpResourceTicket() {}
	
    public SharpResourceTicket(IResourceTicketFactory factory, ResourceDelegation delegation, SharpCertificate certificate)
    {
    	super(factory, delegation);
    	this.certificate = certificate;
    }
    
    public SharpResourceTicket(IResourceTicketFactory factory, ResourceTicket source, ResourceDelegation delegation, SharpCertificate certificate)
    {
    	super(factory, source, delegation);
    	this.certificate = certificate;
    	
        // if the source had other certificates attached, then copy them as well
        if (((SharpResourceTicket)source).otherCertificates != null){
            otherCertificates = new SharpCertificate[((SharpResourceTicket)source).otherCertificates.length];
            System.arraycopy(((SharpResourceTicket)source).otherCertificates, 0, otherCertificates, 0, ((SharpResourceTicket)source).otherCertificates.length);
        }
    	
    }
    
    public SharpResourceTicket(IResourceTicketFactory factory, ResourceTicket[] sources, ResourceDelegation delegation, SharpCertificate certificate, SharpCertificate[] otherCertificates)
    {
        super(factory, sources, delegation);
        this.certificate = certificate;
        this.otherCertificates = otherCertificates;
    }
    
    public SharpCertificate getCertificate()
    {
    	return certificate;
    }
    
    public SharpCertificate[] getOtherCertificates()
    {
    	return otherCertificates;
    }
    
    public int getOtherCertificatesCount()
    {
        if (otherCertificates == null){
            return 0;
        } else {
            return otherCertificates.length;
        }
    }    
}