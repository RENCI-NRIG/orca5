/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.nodeagent.drivers;

import org.apache.tools.ant.BuildException;

public class DriverManagementTask extends DriverBaseTask {
    protected String driverClass;
    protected String driverPackage;

    @Override
    public void execute() throws BuildException {
        super.execute();

        if (driverId == null) {
            throw new RuntimeException("Missing driver identifier");
        }
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public void setDriverPackage(String driverPackage) {
        this.driverPackage = driverPackage;
    }
}