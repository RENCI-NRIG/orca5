package net.exogeni.orca.shirako.core;

import java.util.HashMap;
import java.util.HashSet;

import net.exogeni.orca.shirako.api.IEvent;
import net.exogeni.orca.shirako.api.IEventHandler;
import net.exogeni.orca.shirako.api.IReservationCallback;
import net.exogeni.orca.shirako.api.IReservationTracker;
import net.exogeni.orca.shirako.common.ReservationID;
import net.exogeni.orca.shirako.kernel.ReservationPurgedEvent;
import net.exogeni.orca.shirako.kernel.ReservationStateTransitionEvent;
import net.exogeni.orca.shirako.kernel.ReservationStates;
import net.exogeni.orca.shirako.util.ReservationState;

public class ReservationTracker implements IEventHandler, IReservationTracker {
	protected class ReservationTrackerState {
		public ReservationState state;
		public boolean purged;
		public final HashSet<IReservationCallback> cbs = new HashSet<IReservationCallback>();
				
		public boolean isTicketing() {
			return state.getPending() == ReservationStates.Ticketing || state.getPending() == ReservationStates.ExtendingTicket;
		}

		public boolean isTicketed() {
			return state.getState() == ReservationStates.Ticketed ||
					state.getState() == ReservationStates.ActiveTicketed;
		}
		
		public boolean isExtendingTicketing() {
			return state.getPending() == ReservationStates.ExtendingTicket;
		}

		public boolean isActive() {
			if (state.getJoining() == -1){
				return state.getState() == ReservationStates.Active && state.getPending() == ReservationStates.None;
			}else {
				return (state.getState() == ReservationStates.Active || state.getState() == ReservationStates.ActiveTicketed) && state.getJoining() == ReservationStates.NoJoin;
			}
		}

		public boolean isClosed() {
			return state.getState() == ReservationStates.Closed;			
		}
		
		public boolean isFailed() {
			return state.getState() == ReservationStates.Failed;			
		}

		public boolean isTerminal() {
			return isFailed() || isClosed() || purged;
		}
	};
	
	protected final HashMap<ReservationID, ReservationTrackerState> state;
	
	public ReservationTracker() {
		state = new HashMap<ReservationID, ReservationTrackerState>();
	}
	
	private void handleStateTransition(ReservationStateTransitionEvent e){
		ReservationTrackerState ts;
		
		synchronized(state) {
			ts = state.get(e.getReservationID());
			if (ts == null){
				ts = new ReservationTrackerState();
				state.put(e.getReservationID(), ts);
			}
		}
		
		synchronized(ts){
			ts.state = e.getState();
			ts.notifyAll();
		}
	}
	
	private void handleReservationPurged(ReservationPurgedEvent e) {
		ReservationTrackerState ts = null;
		
		synchronized(state){
			ts = state.remove(e.getReservationID());
			if (ts == null){
				return;
			}
		}
		
		synchronized(ts){
			ts.purged = true;
			ts.notifyAll();
		}
	}
	
	public void handle(IEvent event) {
		if (event instanceof ReservationStateTransitionEvent) {
			handleStateTransition((ReservationStateTransitionEvent)event);
		}else if (event instanceof ReservationPurgedEvent){
			handleReservationPurged((ReservationPurgedEvent)event);
		}
	}	
	
	public synchronized ReservationState getState(ReservationID id){
		ReservationTrackerState ts = state.get(id);
		if (ts == null){
			return null;
		}
		return ts.state;
	}

	public void awaitTicketed(ReservationID id){
		ReservationTrackerState ts;
		
		synchronized(state){
			ts = state.get(id);
			if (ts == null){
				throw new RuntimeException("Unknown reservation: " + id);
			}
		}
		
		synchronized(ts){
			while (!ts.isTerminal() && !ts.isTicketed()){
				try {
					ts.wait();
				} catch (InterruptedException e){
					throw new RuntimeException("Interrupted while waiting for reservation state change", e);
				}
			}
		}
	}

	public synchronized void registerCallback(IReservationCallback cb) {
		throw new RuntimeException("Not implemented yet");		
	}

	public synchronized void registerCallback(ReservationID rid, IReservationCallback cb) {
		throw new RuntimeException("Not implemented yet");		
	}

	public synchronized void unregisterCallback(IReservationCallback cb) {
		throw new RuntimeException("Not implemented yet");	
	}
	
	public synchronized void unregisterCallback(ReservationID rid, IReservationCallback cb) {
		throw new RuntimeException("Not implemented yet");
	}
}
