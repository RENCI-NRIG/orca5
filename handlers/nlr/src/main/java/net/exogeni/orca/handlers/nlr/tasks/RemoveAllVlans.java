package net.exogeni.orca.handlers.nlr.tasks;

import java.util.Iterator;
import java.util.List;

import net.exogeni.orca.handlers.nlr.SherpaAPIResponse.VlanDefinition;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Remove all provisioned VLANs matching the keyword descriptions used by ORCA
 * 
 * @author ibaldin
 */
public class RemoveAllVlans extends GenericSherpaTask {
    @Override
    public void execute() throws BuildException {
        super.execute();

        // remove all reservations and vlans with description keywords
        try {
            List<VlanDefinition> provisionedVlans = sapi.get_matching_vlans(SherpaDescKeyword);

            Iterator<VlanDefinition> it = provisionedVlans.iterator();
            while (it.hasNext()) {
                int vlan = it.next().tag;
                sapi.remove_vlan(vlan, SherpaRequestId);
                setResult(vlan);
            }
            setResult(0);
        } catch (Exception e) {
            logger.error("Error in removing all pre-existing reservations: ", e);
            throw new BuildException("Error in removing all pre-existing reservations: ", e);
        }
    }

    @Override
    protected String getErrorMessage(int code) {
        return ("Clearing reservation for " + code);
    }

    @Override
    protected void setResult(int code) {
        Project p = getProject();
        if (exitCodeProperty != null) {
            p.setProperty(exitCodeProperty, Integer.toString(code));
            if (code != 0)
                p.setProperty(exitCodeMessageProperty,
                        p.getProperty(exitCodeMessageProperty) + "\n" + getErrorMessage(code));
            else
                p.setProperty(exitCodeMessageProperty, p.getProperty(exitCodeMessageProperty) + "\n" + "Success!");
        } else {
            if (code != 0) {
                throw new RuntimeException("An error has occurred. Error code: " + code);
            }
        }
    }
}
