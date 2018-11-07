/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.util;

public class SshUtil
{
    public final static String binary = "ssh";
    public final static String arguments = "-o PreferredAuthentications=publickey -o HostbasedAuthentication=no -o PasswordAuthentication=no -o StrictHostKeyChecking=no";
    public final static String VmmKey = "tests/common/keys/sharp_dsa";
    public final static String VmmUser = "sharp";
    public final static String VmKey = "tests/common/keys/root_dsa";
    public final static String VmUser = "root";
}