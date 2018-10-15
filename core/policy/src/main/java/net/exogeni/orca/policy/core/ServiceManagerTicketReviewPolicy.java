package net.exogeni.orca.policy.core;

import net.exogeni.orca.shirako.api.IReservation;
import net.exogeni.orca.shirako.common.SliceID;
import net.exogeni.orca.shirako.kernel.IKernelSlice;
import net.exogeni.orca.shirako.util.ReservationSet;
import net.exogeni.orca.util.persistence.NotPersistent;

import java.util.*;

/**
 * This implementation of a Service Manager policy is almost identical to the parent
 * ServiceManagerSimplePolicy.
 *
 * The only real difference is that it addresses the following issue:
 * https://github.com/RENCI-NRIG/net.exogeni.orca5/issues/88
 * Tickets should not be redeemed if any reservations are currently Failed or Nascent.
 *
 * This effectively acts as a "gate" between the SM and AM.
 * All reservations must be Ticketed, before any reservations are allowed to be redeemed.
 *
 */
public class ServiceManagerTicketReviewPolicy extends ServiceManagerSimplePolicy {

    @NotPersistent
    protected ReservationSet pendingRedeem;

    public ServiceManagerTicketReviewPolicy(){
        super();
        pendingRedeem = new ReservationSet();
    }

    /**
     * Redeemable: the default.  No slice reservations have been found that are either Nascent or Failed.
     * Nascent: occurs when any reservation is Nascent, i.e. not yet Ticketed.  Will take precedence over Failing.
     * Failing: occurs when a slice reservation is found that is Failed.
     */
    public enum TicketReviewSliceState {
        Nascent,
        Failing,
        Redeemable
    }

    /**
     * Check to make sure all reservations are Ticketed (not Failed or Nascent)
     * before calling the parent method.
     *
     * @throws Exception in case of error
     */
    @Override
    protected void checkPending() throws Exception {

        // add all of our pendingRedeem, so they can be checked
        for (IReservation reservation : pendingRedeem) {
            calendar.addPending(reservation);
        }

        // get set of reservations that need to be redeemed
        ReservationSet myPending = calendar.getPending();

        // keep track of status of the slice containing each reservation
        Map<SliceID, TicketReviewSliceState> sliceStatusMap = new HashMap<>();

        // keep track of which sites (per slice) had a failure
        // Note: this feature is not currently used
        //Map<SliceID, List<String>> sliceFailureSites = new HashMap<>();

        // nothing to do!
        if (myPending == null) {
            return;
        }

        // check the status of the Slice of each reservation
        for (IReservation reservation : myPending) {
            IKernelSlice slice = (IKernelSlice) reservation.getSlice();
            SliceID sliceID = slice.getSliceID();

            // only want to do this for 'new' tickets
            if (reservation.isFailed() || reservation.isTicketed()) {
                // check if we've examined this slice already
                if (!sliceStatusMap.containsKey(sliceID)) {
                    // set the default status
                    sliceStatusMap.put(sliceID, TicketReviewSliceState.Redeemable);

                    // examine every reservation contained within the slice,
                    // looking for either a Failed or Nascent reservation
                    // we have to look at everything in a slice once, to determine all/any Sites with failures
                    for (IReservation sliceReservation : slice.getReservations()) {

                        // If any Reservations that are being redeemed, that means the slice has already cleared TicketReview.
                        if (sliceReservation.isRedeeming()){
                            // There shouldn't be any Nascent reservations, if a reservation is being Redeemed.
                            if (sliceStatusMap.get(sliceID) == TicketReviewSliceState.Nascent){
                                logger.error("TicketReview: Nascent reservation found while Reservation " +
                                        sliceReservation.getReservationID() +
                                        " in Slice " + slice.getName() +
                                        " isRedeeming().");
                            }

                            // We may have previously found a Failed Reservation,
                            // but if a ticketed reservation is being redeemed,
                            // the failure _should_ be from the AM, not SM,
                            // so it should be ignored by TicketReview
                            sliceStatusMap.put(sliceID, TicketReviewSliceState.Redeemable);

                            // we don't need to look at any other reservations in this slice
                            break;
                        }

                        // if any tickets are Nascent,
                        // as soon as we remove the Failed reservation,
                        // those Nascent tickets might get redeemed.
                        // we must wait to Close any failed reservations
                        // until all Nascent tickets are either Ticketed or Failed
                        if (sliceReservation.isNascent()) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Found Nascent Reservation " +
                                        sliceReservation.getReservationID() +
                                        " in Slice " + slice.getName() +
                                        " when checkPending() for " + reservation.getReservationID());
                            }

                            sliceStatusMap.put(sliceID, TicketReviewSliceState.Nascent);

                            // once we have found a Nascent reservation, that is what we treat the entire slice
                            break;
                        }

                        // track Failed reservations, but need to keep looking for Nascent or Redeemable.
                        if (sliceReservation.isFailed()) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Found Failed Reservation " +
                                        sliceReservation.getReservationID() +
                                        " in Slice " + slice.getName() +
                                        " when checkPending() for " + reservation.getReservationID());
                            }

                            sliceStatusMap.put(sliceID, TicketReviewSliceState.Failing);

                                // Keep track of which sites had failures
                                // Using Authority Name would be better than ResourceType,
                                // but failed reservations don't contain an Authority Name

                                // Find the site from type.
                                // If e.g. a Slice has a failed VM at a site, we don't want to Ticket the VLAN there
                                // Note: this feature is not currently used
                                //if (sliceReservation.getResources() != null && sliceReservation.getResources().getType() != null) {
                                    //String site = getSiteFromType(sliceReservation.getResources().getType().getType());

                                    // compare site to list of site failures
                                    //if (!sliceFailureSites.containsKey(sliceID)) {
                                    //    sliceFailureSites.put(sliceID, new ArrayList<String>());
                                    //}
                                    //List<String> failureSites = sliceFailureSites.get(sliceID);
                                    //if (!failureSites.contains(site)) {
                                    //    failureSites.add(site);
                                    //}
                                //} // sliceSiteFailure
                        }
                    }
                }

                // take action on the current reservation
                if (sliceStatusMap.get(sliceID) == TicketReviewSliceState.Failing) {
                    if (reservation.getResources() != null && reservation.getResources().getType() != null) {
                        // only fail the reservation if it from the same site as another failed reservation
                        // Note: this feature is not currently used
                        //String site = DomainResourceType.getSiteFromType(reservation.getResources().getType().getType());
                        //if (sliceFailureSites.containsKey(sliceID) && sliceFailureSites.get(sliceID).contains(site)) {
                            // Fail the reservation, and remove it from everything
                            logger.info("TicketReview: Closing reservation " + reservation.getReservationID() +
                                    " due to failure in Slice " + slice.getName());

                            // Important: closing a reservation in the SM will also close the reservation in the AM and Broker.
                            // BUT ONLY if the reservation is not failed.
                            actor.close(reservation); // "perform local close operations and issue close request to authority"
                            calendar.removePending(reservation);
                            pendingNotify.remove(reservation);
                        //} // sliceFailureSites
                    }
                } else if (sliceStatusMap.get(sliceID) == TicketReviewSliceState.Nascent) {
                    // save this reservation for later
                    logger.info("Moving reservation " + reservation.getReservationID() +
                            " to pendingRedeem list, due to nascent reservation in slice " + slice.getName());
                    pendingRedeem.add(reservation);
                    calendar.removePending(reservation);

                }
            }
        }

        // anything remaining in calendar.pending will be processed in parent class
        super.checkPending();
    }

}
