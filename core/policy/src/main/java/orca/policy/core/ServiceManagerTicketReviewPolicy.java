package orca.policy.core;

import orca.shirako.api.IReservation;
import orca.shirako.common.SliceID;
import orca.shirako.kernel.IKernelSlice;
import orca.shirako.kernel.ReservationStates;
import orca.shirako.util.ReservationSet;
import orca.util.persistence.NotPersistent;

import java.util.HashMap;
import java.util.Map;

import static orca.manage.OrcaConstants.*;

/**
 * This implementation of a Service Manager policy is almost identical to the parent
 * ServiceManagerSimplePolicy.
 *
 * The only real difference is that it addresses the following issue:
 * https://github.com/RENCI-NRIG/orca5/issues/88
 * Tickets should not be redeemed if any reservations are currently Failed or Nascent.
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
     * Check to make sure all reservations are Ticketed (not Failed or Nascent)
     * before calling the parent method.
     *
     * @throws Exception
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
        Map<SliceID, Integer> sliceStatusMap = new HashMap<>();

        // nothing to do!
        if (myPending == null) {
            return;
        }

        // check the status of the Slice of each reservation
        for (IReservation reservation : myPending){
            IKernelSlice slice = (IKernelSlice) reservation.getSlice();
            SliceID sliceID = slice.getSliceID();

            // only want to do this for 'new' tickets
            if (reservation.isFailed() || reservation.isTicketed()) {
                // set the default status
                if (!sliceStatusMap.containsKey(sliceID)) {
                    sliceStatusMap.put(sliceID, ReservationStateActive);
                }

                // examine every reservation contained within the slice,
                // until we have found either a Failed or Nascent reservation
                for (IReservation sliceReservation : slice.getReservations()) {
                    if (sliceReservation.getState() == ReservationStateFailed) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Found Failed Reservation " +
                                    sliceReservation.getReservationID() +
                                    " in Slice " + slice.getName() +
                                    " when checkPending() for " + reservation.getReservationID());
                        }
                        sliceStatusMap.put(sliceID, ReservationStateFailed);
                        break;

                    } else if (sliceReservation.getState() == ReservationStateNascent) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Found Nascent Reservation " +
                                    sliceReservation.getReservationID() +
                                    " in Slice " + slice.getName() +
                                    " when checkPending() for " + reservation.getReservationID());
                        }
                        sliceStatusMap.put(sliceID, ReservationStateNascent);
                        // there could be a Failed one too, but we'll just stop here
                        break;
                    }
                }

                // take action on the current reservation
                if (sliceStatusMap.get(sliceID) == ReservationStateFailed) {
                    // Fail the reservation, and remove it from everything
                    logger.info("Failing reservation " + reservation.getReservationID() +
                            " due to failure in Slice " + slice.getName());
                    reservation.transition("fail on slice reservation failed", ReservationStates.Failed, ReservationStates.None);
                    calendar.removePending(reservation);
                    pendingNotify.remove(reservation);

                } else if (sliceStatusMap.get(sliceID) == ReservationStateNascent) {
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
