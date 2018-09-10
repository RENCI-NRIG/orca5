package net.exogeni.orca.shirako.proxies.soapaxis2;

import net.exogeni.orca.shirako.core.Ticket;
import net.exogeni.orca.shirako.plugins.ICertificatePolicy;

public class NullCertificatePolicy implements ICertificatePolicy {

    public void onUpdateTicket(Ticket ticket) throws Exception {
        // no-op for now
    }
}
