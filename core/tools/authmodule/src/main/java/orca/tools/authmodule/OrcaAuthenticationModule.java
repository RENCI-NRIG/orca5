package orca.tools.authmodule;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.modules.Module;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;

public class OrcaAuthenticationModule implements Module {
    // initialize the module
    public void init(ConfigurationContext configContext, AxisModule module) throws AxisFault {
    }

    public void engageNotify(AxisDescription axisDescription) throws AxisFault {
    }

    // shutdown the module
    public void shutdown(ConfigurationContext configurationContext) throws AxisFault {
    }

    public String[] getPolicyNamespaces() {
        return null;
    }

    public void applyPolicy(Policy policy, AxisDescription axisDescription) throws AxisFault {
    }

    public boolean canSupportAssertion(Assertion assertion) {
        return true;
    }
}
