package orca.shirako.common.delegation;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

import orca.util.ID;

import com.thoughtworks.xstream.XStream;

public class SharpResourceTicketFactory extends SignedResourceTicketFactory
{

    // FIXME: there exists an opportunity to reduce the number of sharp certificates
    // being issued: remember certificates and reuse them as needed
    
    
    @Override
    protected void initialize(XStream xs)
    {
        super.initialize(xs);

        xs.alias("sharpTicket", SharpResourceTicket.class);
    }

    protected SharpCertificate getIssuerSelfSignedSharpCertificate()
    {
        Certificate cert = getIssuerCertificate();
        if (cert == null) {
            throw new RuntimeException("Missing issuer certificate");
        }
        // FIXME: we really should store these in the database
        // and then reuse them so that we maintain the certificate GUID.
        return new SharpCertificate(null, cert);
    }

    protected SharpCertificate getHolderSharpCertificate(SharpCertificate issuer, ID holder) throws Exception
    {
        Certificate cert = getHolderCertificate(holder);
        if (cert == null) {
            throw new RuntimeException("Missing holder certificate:" + holder.toString());
        }

        // FIXME: we should find a way to reuse these certificates instead of
        // generating them every time.
        SharpCertificate result = new SharpCertificate(issuer, cert);

        PrivateKey pk = getIssuerPrivateKey();
        sign(result, pk);
        return result;
    }

    public void sign(SharpCertificate cert, PrivateKey key) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, CertificateEncodingException
    {
        // convert the holder cert into a byte array
        Certificate holderCert = cert.holderCertificate;
        byte[] bytes = holderCert.getEncoded();

        // intialize the signature object
        Signature dsa = Signature.getInstance("MD5withRSA");
        dsa.initSign(key);
        // add the data
        dsa.update(bytes, 0, bytes.length);
        // get the signature
        byte[] signature = dsa.sign();
        // attach the signature
        cert.signature = signature;
    }

    public ResourceTicket makeTicket(ResourceDelegation delegation) throws TicketException
    {
        ensureInitialized();

        try {
            SharpCertificate sc = getIssuerSelfSignedSharpCertificate();
            return new SharpResourceTicket(this, delegation, sc);
        } catch (Exception e) {
            throw new TicketException("Failed to create ticket", e);
        }
    }

    public ResourceTicket makeTicket(ResourceTicket source, ResourceDelegation delegation) throws TicketException
    {
        ensureInitialized();
        if (!(source instanceof SharpResourceTicket)) {
            throw new TicketException("source is not an instance of SharpResourceTicket");
        }
        try {
            SharpCertificate issuer = ((SharpResourceTicket) source).getCertificate();
            SharpCertificate sc = getHolderSharpCertificate(issuer, delegation.getHolder());
            return new SharpResourceTicket(this, source, delegation, sc);
        } catch (Exception e) {
            throw new TicketException("Failed to create ticket", e);
        }
    }

    public ResourceTicket makeTicket(ResourceTicket[] sources, ResourceDelegation delegation) throws TicketException
    {
        assert sources != null;
        ensureInitialized();

        for (ResourceTicket source : sources){
            if (!(source instanceof SharpResourceTicket)) {
                throw new TicketException("source is not an instance of SharpResourceTicket");
            }
        }

        try {
            SharpCertificate issuer = ((SharpResourceTicket) sources[0]).getCertificate();
            SharpCertificate sc = getHolderSharpCertificate(issuer, delegation.getHolder());
            SharpCertificate[] other = null;
            
            int count = ((SharpResourceTicket) sources[0]).getOtherCertificatesCount();
            for (int i = 1; i < sources.length; i++){
                count += ((SharpResourceTicket) sources[i]).getOtherCertificatesCount();
            }
            if (count > 0){
                other = new SharpCertificate[count];
                int index = 0;
                for (int i = 0; i < sources.length; i++){
                    SharpResourceTicket srt = (SharpResourceTicket)sources[i];
                    if (i > 0){
                        other[index++] = srt.getCertificate();
                    }
                    SharpCertificate[] certs = srt.getOtherCertificates();
                    if (certs != null){
                        for (int j = 0; j < certs.length; j++){
                            other[index++] = certs[j];
                        }
                    }
                }
            }
            return new SharpResourceTicket(this, sources, delegation, sc, other);
        } catch (Exception e) {
            throw new TicketException("Failed to create ticket", e);
        }
    }

}
