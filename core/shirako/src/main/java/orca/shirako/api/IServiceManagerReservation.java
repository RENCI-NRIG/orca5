/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in the file named
 * LICENSE.Eclipse, which was shipped with this distribution. Any use,
 * reproduction or distribution of this software constitutes the recipient's
 * acceptance of the Eclipse license terms. This notice and the full text of the
 * license must be included with any distribution of this software.
 */

package orca.shirako.api;

import java.util.List;
import java.util.Properties;

import orca.shirako.kernel.ResourceSet;
import orca.shirako.time.Term;

public interface IServiceManagerReservation extends IClientReservation {
    /**
     * Serialization property name: leased resource set.
     */
    public static final String PropertyLeasedResources = "ReservationClientLeasedResources";

    /**
     * Serialization property name: leased term.
     */
    public static final String PropertyTermLeased = "ReservationClientTermLeased";

	/**
	 * Serialization property name: reservation joining state.
	 */
	public static final String PropertyJoining = "ReservationJoining";
    
    /**
     * Returns the join state.
     * @return join state
     */
    public int getJoinState();

    /**
     * Returns the name of the join state.
     * @return
     */
    public String getJoinStateName();

    /**
     * Returns the resources leased by the reservation. If the reservation has
     * not yet issued a redeem request, returns null.
     * @return resources leased by the reservation. Can be null.
     */
    public ResourceSet getLeasedResources();

    /**
     * Returns the reservation sequence number for incoming redeem/extend lease
     * messages.
     * @return reservation sequence number for incoming redeem/extend lease
     *         messages
     */
    public int getLeaseSequenceIn();

    /**
     * Returns the reservation sequence number for outgoing ticket/extend ticket
     * messages.
     * @return reservation sequence number for outgoing ticket/extend ticket
     *         messages
     */
    public int getLeaseSequenceOut();

    /**
     * Returns the term of the current lease.
     * @return current lease term
     */
    public Term getLeaseTerm();

    /**
     * Returns the previous lease term.
     * @return previous lease term
     */
    public Term getPreviousLeaseTerm();

    /**
     * Returns true if this reservation is currently active, and has completed
     * joining the guest, i.e., successor reservations (with join dependencies
     * on this reservation) may now join. Note: if this reservation is closed or
     * failed, activeJoined returns false and successors will remain blocked,
     * i.e, the caller must close them.
     * @return DOCUMENT ME!
     */
    public boolean isActiveJoined();

    /**
     * Indicates whether the reservation represents exported resources.
     * @param exported value for the exported flag
     */
    public void setExported(boolean exported);

    /**
     * Sets the join predecessor: the reservation, for which the kernel must
     * issue a join before a join may be issued for the current reservation.
     * @param predecessor predecessor reservation
     */
    public void setJoinPredecessor(IServiceManagerReservation predecessor);

    /**
     * Sets the reservation sequence number for incoming lease messages.
     * @param sequence sequence number
     */
    public void setLeaseSequenceIn(int sequence);

    /**
     * Sets the reservation sequence number for outgoing redeem/extend lease
     * messages.
     * @param sequence sequence number
     */
    public void setLeaseSequenceOut(int sequence);

    /**
     * Sets the redeem predecessor: the reservation, which must be redeemed
     * before a redeem may be issued for the current reservation.
     * @param predecessor predecessor reservation
     * @deprecated use {@link #addRedeemPredecessor(IServiceManagerReservation)}
     */
    public void setRedeemPredecessor(IServiceManagerReservation predecessor);

    /**
     * @deprecated use {@link #addRedeemPredecessor(IServiceManagerReservation, Properties)}
     * @param predecessor
     * @param filter
     */
    public void setRedeemPredecessor(IServiceManagerReservation predecessor, Properties filter);

    /**
     * Adds a redeem predecessor to this reservation: the passed in reservation
     * must be redeemed before this reservation.
     * @param r predecessor reservation
     */
    public void addRedeemPredecessor(IServiceManagerReservation r);

    /**
     * Adds a redeem predecessor to this reservation: the passed in reservation
     * must be redeem before this reservation. Specifies filter to allow properties from the predecessor
     * to be passed to this reservation.
     * @param r
     * @param filter
     */
    public void addRedeemPredecessor(IServiceManagerReservation r, Properties filter);

    /**
     * Returns the redeem predecessors list for the reservation.
     * @return
     */
    public List<IServiceManagerReservation> getRedeemPredecessors();
    
    /**
     * Returns the join predecessors list for the reservation
     * @return
     */
    public List<IServiceManagerReservation> getJoinPredecessors();

    /**
     * @deprecated use addJoinPredecessor instead
     * @param predecessor
     * @param filter
     */
    public void setJoinPredecessor(IServiceManagerReservation predecessor, Properties filter);
    /**
     * Sets a configuration property.
     * @param key
     * @param value
     */
    
    public void setConfigurationProperty(String key, String value);
    /**
     * Sets a request property.
     * @param key
     * @param value
     */
    public void setRequestProperty(String key, String value);    
}
