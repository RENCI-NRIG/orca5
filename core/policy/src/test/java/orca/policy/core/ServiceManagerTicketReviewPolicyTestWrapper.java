package orca.policy.core;

import orca.shirako.api.IClientReservation;

/**
 * This class is copied from ServiceManagerSimplePolicyTestWrapper,
 * but extends the class we want to test: ServiceManagerTicketReviewPolicy.
 */
public class ServiceManagerTicketReviewPolicyTestWrapper extends ServiceManagerTicketReviewPolicy
{
    @Override
    public long getRenew(final IClientReservation reservation) throws Exception
    {
        // renew as soon as the term becomes active
        return clock.cycle(reservation.getTerm().getNewStartTime());
    }
}