package orca.tools.authmodule;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.handlers.AbstractHandler;

public class OrcaLoggingHandler extends AbstractHandler implements Handler
{
    private String name;

    public InvocationResponse invoke(MessageContext context) throws AxisFault
    {
        return InvocationResponse.CONTINUE;
    }

    public void revoke(MessageContext msgContext)
    {
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
}
