/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.nodeagent;

public class NodeAgentConstants {
    public static final int CodeOK = 0;
    public static final int CodeInternalError = 1;
    public static final int CodeInvalidArguments = 2;
    public static final int CodeInvalidDriver = 3;
    public static final int CodeInternalDriverError = 4;
    public static final int CodeKeyAlreadyRegistered = 5;
    public static final String[] Messages = new String[] { "Success", "Internal Error", "Invalid Arguments",
            "Invalid Driver", "Internal Driver Error" };
    public static final String MessageUnknownError = "Unknown error";
    public static final String ServiceProtocol = "http://";
    public static final String ServicePort = "8080";
    public static final String ServiceUri = "/axis2/services/";
    public static final String ServiceName = "NodeAgentService";
    public static final String TestCmdLineFile = "testcmdline";

    public static String getMessage(int code) {
        if ((code >= 0) && (code < Messages.length)) {
            return Messages[code];
        }

        return MessageUnknownError;
    }

    public static String getURL(String address) {
        return ServiceProtocol + address + ":" + ServicePort + ServiceUri + ServiceName;
    }
}