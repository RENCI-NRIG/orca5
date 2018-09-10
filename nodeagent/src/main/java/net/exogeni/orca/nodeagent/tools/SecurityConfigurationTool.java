/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.nodeagent.tools;

import net.exogeni.orca.tools.axis2.Axis2ClientSecurityConfigurator;

public class SecurityConfigurationTool {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: create <root dir> <actor id>");
            System.exit(1);
        } else {
            System.out.println("Generating security configuration. This may take some time...");
            int code = Axis2ClientSecurityConfigurator.getInstance().createActorConfiguration(args[1], args[2]);

            if (code != 0) {
                System.err.println("An error occurred while generating security configuration.");
            } else {
                System.out.println("Security configuration generated successfully.");
                System.out.println("axis2 configuration stored in: "
                        + Axis2ClientSecurityConfigurator.getInstance().getAxis2ConfigPath(args[1], args[2]));
            }
        }
    }
}