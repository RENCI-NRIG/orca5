package orca.shirako.proxies.soapaxis2;

import orca.shirako.core.Ticket;
import orca.shirako.plugins.ICertificatePolicy;

public class NullCertificatePolicy implements ICertificatePolicy {

    public void onUpdateTicket(Ticket ticket) throws Exception {
        // no-op for now
    }
}