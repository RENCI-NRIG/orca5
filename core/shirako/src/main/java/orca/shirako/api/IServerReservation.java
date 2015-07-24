/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package orca.shirako.api;

import orca.security.AuthToken;
import orca.shirako.util.UpdateData;


/**
 * <code>IServerReservation</code> defines the reservation interface for
 * actors acting as servers for other actors.
 */
public interface IServerReservation extends IReservation
{
    /**
     * Returns the callback proxy.
     *
     * @return callback proxy.
     */
    public ICallbackProxy getCallback();

    /**
     * Returns the identity of the client represented by the
     * reservation.
     *
     * @return identity of client actor
     */
    public AuthToken getClientAuthToken();

    /**
     * Returns the sequence number of the last received message.
     *
     * @return sequence number of the last received message
     */
    public int getSequenceIn();

    /**
     * Returns the sequence number of the last sent message.
     *
     * @return sequence number of the last sent message
     */
    public int getSequenceOut();

    /**
     * Returns the identity of the server actor that controls the
     * reservation.
     *
     * @return identity of server actor
     */
    public AuthToken getServerAuthToken();

    /**
     * Checks if the reservation is in progress of being allocated
     * resources.
     *
     * @return true if the reservation is in the process of being allocated
     *         resources
     */
    public boolean isBidPending();

    /**
     * Indicates whether the reservation is in the progress of
     * obtaining resources. This flag should be set (true) when the actor
     * policy starts processing a reservation and  should be cleared(false)
     * when the policy completes the allocation process.
     *
     * @param value value for the bid pending flag
     */
    public void setBidPending(boolean value);

    /**
     * Sets the identity of the server actor that controls the
     * reservation.
     *
     * @param owner identity of server actor
     */
    public void setOwner(AuthToken owner);

    /**
     * Sets the sequence number of the last received message.
     *
     * @param sequence incoming message sequence number
     */
    public void setSequenceIn(int sequence);

    /**
     * Sets the sequence number of the last sent message.
     *
     * @param sequence outgoing message sequence number
     */
    public void setSequenceOut(int sequence);
    
    /**
     * Returns data to be sent back to the client in an update message.
     * @return
     */
    public UpdateData getUpdateData();
}