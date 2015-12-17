/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.kernel;

public class ReservationStates
{
    /**
     * Reservation states (enum ReservationState) Should be protected:
     * public just for logging
     */
    public static final int Nascent = 1;
    public static final int Ticketed = 2;
    public static final int Active = 3;
    public static final int ActiveTicketed = 4;
    public static final int Closed = 5;
    public static final int CloseWait = 6;
    public static final int Failed = 7;
    public static final String[] states = {
                                              "No state", "Nascent", "Ticketed", "Active",
                                              "ActiveTicketed", "Closed", "CloseWait", "Failed"
                                          };

    /**
     * Pending operation states (enum ReservationPending) Should be
     * protected: public just for logging
     */
    public static final int None = 1;
    public static final int Ticketing = 2;
    public static final int Redeeming = 3;
    public static final int ExtendingTicket = 4;
    public static final int ExtendingLease = 5;
    public static final int Priming = 6;
    public static final int Blocked = 7;
    public static final int Closing = 8;
    public static final int Probing = 9;
    public static final int ClosingJoining = 10;
    public static final int ModifyingLease = 11;
    public static final String[] pendings = {
                                                "No pending", "None", "Ticketing", "Redeeming",
                                                "ExtendingTicket", "ExtendingLease", "Priming",
                                                "Blocked", "Closing", "Probing", "ClosingJoining", "ModifyingLease"
                                            };
    public static final int AbsorbUpdate = 11;

    public static final int SendUpdate = 12;
    
    /**
     * Values for joinstate (service manager only).
     */
    public static final int NoJoin = 1;
    public static final int BlockedJoin = 2;
    public static final int BlockedRedeem = 3;
    public static final int Joining = 4;
    public static final String[] joinstates = {
                                                  "No state", "NoJoin", "BlockedJoin",
                                                  "BlockedRedeem", "Joining"
                                              };

    /**
     * Helper method to return the name of the specified pending state
     *
     * @param joining pending state
     *
     * @return
     */
    public static String getJoiningName(int joining)
    {
        if ((joining >= 0) && (joining < joinstates.length)) {
            return joinstates[joining];
        } else {
            return null;
        }
    }

    /**
     * Helper method to return the name of the specified pending state
     *
     * @param pending pending state
     *
     * @return
     */
    public static String getPendingName(int pending)
    {
        if ((pending >= 0) && (pending < pendings.length)) {
            return pendings[pending];
        } else {
            return null;
        }
    }

    /**
     * Helper method to return the name of the specified state
     *
     * @param state state
     *
     * @return
     */
    public static String getStateName(int state)
    {
        if ((state >= 0) && (state < states.length)) {
            return states[state];
        } else {
            return null;
        }
    }
}