/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.boot;

import orca.shirako.api.ITicket;


/**
 * This is a factory for creating java physical ticket objects from strings.
 * @author aydan
 * XXX: this class is not functional and must be moved outside of boot
 */
public class TicketFactory
{
    /**
     * When used in site.properties specifies that no physical tickets should be
     * issued
     */
    public static final String NONE_STRING = "none";

    /**
     * When used in site.properties specifies that SHARPphysical tickets should
     * be issued
     */
    public static final String SHARP_STRING = "sharp";

    /**
     * Integer value for no physical tickets
     */
    public static final int NONE = 1;

    /**
     * Integer value for SHARP physical tickets
     */
    public static final int SHARP = 10;

// FIXME:    
//    /**
//     * Identifying prefix for a SHARP ticket
//     */
//    public static final String SHARP_START = "<" + orca.sharp.SharpTicket.TICKET_TAG;

    /**
     * Parses the string representation of a physical ticket and produces a java
     * ticket object. The code applies heuristics to determine the type of the
     * ticket.
     * @param ticketString
     * @return
     * @throws Exception
     */
    public static ITicket getTicket(String ticketString) throws Exception
    {
        if (ticketString == null) {
            throw new Exception("argument cannot be null");
        }

        ITicket result = null;
// FIXME:
//        if (ticketString.indexOf(SHARP_START) > -1) {
//            result = orca.sharp.SharpTicket.fromXML(ticketString);
//        }

        return result;
    }
}