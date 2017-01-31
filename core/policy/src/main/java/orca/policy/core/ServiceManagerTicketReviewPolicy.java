package orca.policy.core;

import orca.shirako.api.IReservation;
import orca.shirako.common.SliceID;
import orca.shirako.kernel.IKernelSlice;
import orca.shirako.util.ReservationSet;
import orca.util.persistence.NotPersistent;

import java.util.*;

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

        // keep track of which sites (per slice) had a failure
        Map<SliceID, List<String>> sliceFailureSites = new HashMap<>();

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
                // check if we've examined this slice already
                if (Objects.equals(sliceStatusMap.get(sliceID), ReservationStateActive)) {
                    // we've looked at all of the reservations for this slice, and they are ok
                    continue;

                } else if (!sliceStatusMap.containsKey(sliceID)) {
                    // set the default status
                    sliceStatusMap.put(sliceID, ReservationStateActive);

                    // examine every reservation contained within the slice,
                    // looking for either a Failed or Nascent reservation
                    // we have to look at everything in a slice once, to determine all/any Sites with failures
                    for (IReservation sliceReservation : slice.getReservations()) {
                        if (sliceReservation.getState() == ReservationStateFailed) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Found Failed Reservation " +
                                        sliceReservation.getReservationID() +
                                        " in Slice " + slice.getName() +
                                        " when checkPending() for " + reservation.getReservationID());
                            }

                            // if any tickets are Nascent,
                            // as soon as we remove the Failed reservation,
                            // those Nascent tickets might get redeemed.
                            // we must wait to Close any failed reservations
                            // until all Nascent tickets are either Ticketed or Failed
                            if (Objects.equals(sliceStatusMap.get(sliceID), ReservationStateActive)) {
                                sliceStatusMap.put(sliceID, ReservationStateFailed);

                                // Keep track of which sites had failures
                                // Using Authority Name would be better than ResourceType,
                                // but failed reservations don't contain an Authority Name

                                // Find the site from type.
                                // If e.g. a Slice has a failed VM at a site, we don't want to Ticket the VLAN there
                                if (sliceReservation.getResources() != null && sliceReservation.getResources().getType() != null) {
                                    String site = getSiteFromType(sliceReservation.getResources().getType().getType());

                                    // compare site to list of site failures
                                    if (!sliceFailureSites.containsKey(sliceID)) {
                                        sliceFailureSites.put(sliceID, new ArrayList<String>());
                                    }
                                    List<String> failureSites = sliceFailureSites.get(sliceID);
                                    if (!failureSites.contains(site)) {
                                        failureSites.add(site);
                                    }
                                }
                            }
                        } else if (sliceReservation.getState() == ReservationStateNascent) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Found Nascent Reservation " +
                                        sliceReservation.getReservationID() +
                                        " in Slice " + slice.getName() +
                                        " when checkPending() for " + reservation.getReservationID());
                            }

                            sliceStatusMap.put(sliceID, ReservationStateNascent);
                        }
                    }
                }

                // take action on the current reservation
                if (sliceStatusMap.get(sliceID) == ReservationStateFailed) {
                    if (reservation.getResources() != null && reservation.getResources().getType() != null) {
                        // only fail the reservation if it from the same site as another failed reservation
                        String site = getSiteFromType(reservation.getResources().getType().getType());
                        if (sliceFailureSites.containsKey(sliceID) && sliceFailureSites.get(sliceID).contains(site)) {
                            // Fail the reservation, and remove it from everything
                            logger.info("TicketReview: Closing reservation " + reservation.getReservationID() +
                                    " due to failure in Slice " + slice.getName());
                            reservation.fail("TicketReview: Closing reservation due to failure in Slice.");
                            actor.close(reservation); // "perform local close operations and issue close request to authority"
                            calendar.removePending(reservation);
                            pendingNotify.remove(reservation);
                        }
                    }
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

    /**
     * Assumes a ResourceType is like "slvmsite.vm" or "slvmsite.vlan"
     * Or like "rencivmsite/vm.vm" or "rencivmsite/vlan.vlan"
     * in which case the Site is "slvmsite" (dropping everything after the '.')
     * or Site become "rencivmsite" dropping everything after the '/' and '.'
     *
     * @param type a ResourceType string
     * @return the name of the site
     */
    private String getSiteFromType(String type) {
        // remove everything after a '.' if it exists
        int siteEnd = type.lastIndexOf('.');
        String site;
        if (-1 != siteEnd) {
            site = type.substring(0, siteEnd);
        } else {
            site = type;
        }

        // remove everything after a '/' if it exists
        siteEnd = site.lastIndexOf('/');
        if (-1 != siteEnd) {
            site = site.substring(0, siteEnd);
        }

        if (logger.isDebugEnabled()){
            logger.debug("Reservation had type " + type + " treating Site as " + site);
        }
        return site;
    }
}
