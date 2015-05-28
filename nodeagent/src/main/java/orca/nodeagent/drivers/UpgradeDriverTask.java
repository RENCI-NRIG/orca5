/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.nodeagent.drivers;

import orca.nodeagent.tools.DriverTool;

import org.apache.tools.ant.BuildException;


public class UpgradeDriverTask extends DriverManagementTask
{
    @Override
    public void execute() throws BuildException
    {
        super.execute();

        try {
            if (driverClass == null) {
                throw new Exception("Missing driver class");
            }

            if (driverPackage == null) {
                throw new Exception("Missing driver package");
            }

            logger.debug("upgrading driver");

            DriverTool tool = new DriverTool(location, repository, config);
            int code = tool.upgradeDriver(driverId, driverClass, driverPackage);
            setResult(code);
        } catch (Exception e) {
            throw new BuildException("An error occurred: " + e.getMessage(), e);
        }
    }
}