package orca.policy.core;

import orca.shirako.api.IClientReservation;


public class ServiceManagerTicketReviewPolicyTestWrapper extends ServiceManagerTicketReviewPolicy
{
    @Override
    public long getRenew(final IClientReservation reservation) throws Exception
    {
        // renew as soon as the term becomes active
        return clock.cycle(reservation.getTerm().getNewStartTime());
    }
}