/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.security;


/**
 * Cred is a simple representation of identity, which may be associated with an
 * AuthToken.
 * @deprecated
 */
public class Cred
{
    public int uid;
    public int gid;
    public int priority;

    public Cred()
    {
        uid = 0;
        gid = 0;
        priority = 0;
    }

    /*
     * Hash by UIDs now, public key hashes later.
     */
    public String ID()
    {
        return String.valueOf(uid);
    }

    /*
     * Upcalled from XML parser.
     */
    public void startElement(String e, String s) throws NumberFormatException
    {
        if (e.equals("uid")) {
            uid = Integer.parseInt(s);
        } else if (e.equals("gid")) {
            gid = Integer.parseInt(s);
        } else if (e.equals("priority")) {
            priority = Integer.parseInt(s);
        }
    }

    /*
     * Generate XML.
     */
    public void formatXML(StringBuffer sb, String prefix)
    {
        String nest = prefix + "   ";

        sb.append(prefix);
        sb.append("<credential>\n");

        sb.append(nest);
        sb.append("<uid>");
        sb.append(Integer.toString(uid));
        sb.append("</uid>\n");

        sb.append(nest);
        sb.append("<gid>");
        sb.append(Integer.toString(gid));
        sb.append("</gid>\n");

        sb.append(nest);
        sb.append("<priority>");
        sb.append(Integer.toString(priority));
        sb.append("</priority>\n");

        sb.append(prefix);
        sb.append("</credential>\n");
    }
}
