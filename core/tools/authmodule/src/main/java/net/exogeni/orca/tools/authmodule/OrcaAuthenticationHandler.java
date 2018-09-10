package net.exogeni.orca.tools.authmodule;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.handlers.AbstractHandler;

public class OrcaAuthenticationHandler extends AbstractHandler implements Handler {
    /**
     * Handler name.
     */
    private String name;

    /**
     * {@inheritDoc}
     */
    public InvocationResponse invoke(MessageContext context) throws AxisFault {
        try {
            // if (!context.isServerSide()) {
            if (context.getProperty(SecurityFilter.AUTHTOKEN_MINE) != null) {
                SecurityFilter.attachToken(context);
            } else {
                SecurityFilter.detachToken(context);
                SecurityFilter.checkSecurity(context);
            }
        } catch (Exception e) {
            throw AxisFault.makeFault(e);
        }

        return InvocationResponse.CONTINUE;
    }

    /**
     * {@inheritDoc}
     */
    public void revoke(MessageContext context) {
        try {
            if (!context.isServerSide()) {
                SecurityFilter.attachToken(context);
            } else {
                SecurityFilter.detachToken(context);
            }
        } catch (Exception e) {
            // XXX: what should we do here?
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }
}
