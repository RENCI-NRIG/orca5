/**
 * Copyright (c) 2009 Renaissance Computing Institute and Duke University
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and/or hardware specification (the �Work�) to deal in the
 * Work without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Work, and to permit persons to whom the Work is furnished to do so,
 * subject to the following conditions: The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of
 * the Work. THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS IN THE
 * WORK.
 */
package net.exogeni.orca.handlers.nlr.tasks;

import java.util.Iterator;
import java.util.List;

import net.exogeni.orca.handlers.nlr.SherpaAPIResponse.VlanGetReservationDefinition;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Clean all "AUTOMATED" vlan id reservations from Sherpa in this wg This will not touch provisioned vlans
 * 
 * @author ibaldin@renci.org
 */
public class RemoveAllReservations extends GenericSherpaTask {

    @Override
    public void execute() throws BuildException {
        super.execute();

        // remove all reservations and vlans with description keywords
        try {
            List<VlanGetReservationDefinition> reservations = sapi.get_matching_reservations(SherpaDescKeyword);
            Iterator<VlanGetReservationDefinition> it = reservations.iterator();
            while (it.hasNext()) {
                int vlan = it.next().vlan_id;
                sapi.remove_reservation(vlan);
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
