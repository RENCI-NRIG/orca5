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

import java.util.Iterator;

import orca.shirako.api.*;
import orca.shirako.common.delegation.ResourceDelegation;
import orca.shirako.common.delegation.ResourceTicket;
import orca.shirako.core.ServiceManager;
import orca.shirako.core.Ticket;
import orca.shirako.time.Term;
import orca.shirako.util.Bids;
import orca.shirako.util.ReservationSet;
import orca.util.ResourceType;

import static orca.manage.OrcaConstants.ReservationStateFailed;

public class ServiceManagerTestWrapper extends ServiceManager {
	@Override
	protected void bid() throws Exception {
		Bids candidates = ((IClientPolicy) policy).formulateBids(currentCycle);

		if (candidates != null) {
			ReservationSet ticketing = candidates.getTicketing();

			if (ticketing != null) {
				/*
				 * Issue new ticket requests.
				 */
				for (IReservation r : ticketing){

					System.out.println("cycle: " + currentCycle
							+ ". Ticket request for: " + r);

					// testing failed reservations
					if (r.getSliceName().startsWith("fail")){
						boolean alreadyFailed = false;
						IKernelSlice slice = (IKernelSlice) r.getSlice();
						for (IReservation sliceReservation : slice.getReservations()){
							if (sliceReservation.getState() == ReservationStateFailed){
								alreadyFailed = true;
								break;
							}
						}

						if (!alreadyFailed){
							failTicket((ReservationClient) r);
							continue; // don't call updateTicket
						}
					}

					// make a ticket: exactly as the client wanted
					ResourceDelegation del = getShirakoPlugin().getTicketFactory().makeDelegation(r.getApprovedResources().getUnits(), r.getApprovedTerm(), r.getApprovedType());
					ResourceTicket t = getShirakoPlugin().getTicketFactory().makeTicket(del);

					Ticket ticket = new Ticket(t, spi, (IAuthorityProxy) null);

					updateTicket((ReservationClient) r, r.getApprovedType(), r.getApprovedUnits(),
								ticket, r.getApprovedTerm());
				}
			}

			ReservationSet extending = candidates.getExtending();

			if (extending != null) {
				/*
				 * Issue extend ticket requests.
				 */
				for (IReservation r : extending){

					System.out.println("cycle: " + currentCycle
							+ ". Extend Ticket request for: " + r);

					// make a ticket: exactly as the client wanted
					// make a ticket: exactly as the client wanted
					ResourceDelegation del = getShirakoPlugin().getTicketFactory().makeDelegation(
							r.getApprovedResources().getUnits(), r.getApprovedTerm(), r.getApprovedType());
					ResourceTicket t = getShirakoPlugin().getTicketFactory().makeTicket(del);
					
					Ticket ticket = new Ticket(t, spi, (IAuthorityProxy) null);

					updateTicket((ReservationClient) r, r.getApprovedType(), r.getApprovedUnits(),
							ticket, r.getApprovedTerm());
				}
			}
		}
	}

	@Override
	protected void closeExpiring() {
		ReservationSet set = policy.getClosing(currentCycle);

		if (set != null) {
			for (IReservation r : set) {
				System.out.println("cycle: " + currentCycle
						+ " closing reservation: " + r);

				ReservationClient rc = (ReservationClient) r;
				// transition
				rc.transition("close", ReservationStates.Closed,
						ReservationStates.None);
			}
		}
	}

	@Override
	protected void processRedeeming() {
		ReservationSet set = ((IServiceManagerPolicy) policy)
				.getRedeeming(currentCycle);

		if (set != null) {
			for (IReservation r : set) {
				if (r.getState() == ReservationStates.Ticketed) {
					System.out.println("cycle: " + currentCycle
							+ " Redeeming reservation: " + r);
				} else {
					System.out.println("cycle: " + currentCycle
							+ " Extending lease for reservation: " + r);
				}

				ReservationClient rc = (ReservationClient) r;
				
				try {
					// make a ticket: we will pretend it is a nodegroup: we cannot
					// make a node group here since we will introduce circular
					// dependency.
					ResourceDelegation del = getShirakoPlugin().getTicketFactory().makeDelegation(rc.resources.getUnits(), rc.term, rc.resources.getType()); 
					ResourceTicket t = getShirakoPlugin().getTicketFactory().makeTicket(del);
					Ticket cs = new Ticket(t, spi, (IAuthorityProxy) null);
					updateLease(rc, r.getApprovedType(), r.getApprovedUnits(),
							cs, r.getApprovedTerm());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	protected void updateLease(ReservationClient r, ResourceType type,
			int units, IConcreteSet cs, Term term) throws Exception {
		if (r.state == ReservationStates.Ticketed) {
			r.leasedResources = r.resources.abstractClone();
			r.leasedResources.units = units;
			r.leasedResources.type = type;
			r.leasedResources.setResources(cs);

			r.previousLeaseTerm = null;
			r.previousTerm = r.term;
			r.leaseTerm = (Term) term.clone();
			r.term = r.leaseTerm;

			// transition
			r.transition("redeem", ReservationStates.Active,
					ReservationStates.None);
		} else {
			r.leasedResources.units = units;
			r.leasedResources.type = type;
			r.leasedResources.resources.change(cs, false);

			r.previousLeaseTerm = r.requestedTerm;
			r.previousTerm = r.term;
			r.leaseTerm = (Term) term.clone();
			r.term = r.leaseTerm;

			// transition
			r.transition("redeem", ReservationStates.Active,
					ReservationStates.None);
		}
	}

	protected void updateTicket(ReservationClient r, ResourceType type,
			int units, Ticket ticket, Term term) throws Exception {
		if (r.state == ReservationStates.Nascent) {
			r.resources = r.getApprovedResources().abstractClone();
			r.resources.type = type;
			r.resources.units = units;
			r.resources.setResources(ticket);
			r.previousTerm = null;
			r.previousTicketTerm = null;
			r.term = (Term) term.clone();
			r.ticketTerm = r.term;

			// transition
			r.transition("ticket", ReservationStates.Ticketed,
					ReservationStates.None);
		} else {
			r.resources.units = units;
			r.resources.type = type;
			r.resources.resources.change(ticket, false);
			r.previousTerm = r.term;
			r.previousTicketTerm = r.ticketTerm;
			r.term = (Term) term.clone();
			r.ticketTerm = r.term;

			// transition
			r.transition("extendticket", ReservationStates.ActiveTicketed,
					ReservationStates.None);
		}
	}

	protected void failTicket(ReservationClient r) {
		r.transition("fail", ReservationStates.Failed, ReservationStates.None);
	}
}