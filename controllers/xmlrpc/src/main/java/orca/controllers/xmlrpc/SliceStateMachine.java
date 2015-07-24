package orca.controllers.xmlrpc;

import java.util.Arrays;
import java.util.List;

import orca.manage.IOrcaServiceManager;
import orca.manage.OrcaConstants;
import orca.manage.beans.ReservationMng;
import orca.shirako.common.SliceID;


/**
 * This state machine describes slice states and their transitions
 * @author ibaldin
 *
 */
public class SliceStateMachine {
	public static enum SliceState {
		NULL("NULL"), 
		CONFIGURING("CONFIGURING"), 
		STABLE_ERROR("STABLE WITH ERRORS"), 
		STABLE_OK("STABLE"), 
		CLOSING("CLOSING"), 
		DEAD("DEAD");
		
		private String n;
		private SliceState(String s) {
			n = s;
		}
		@Override
		public String toString() {
			return n;
		}
	};
	
	public static enum SliceCommand {
		CREATE("Create", SliceState.NULL), 
		MODIFY("Modify", SliceState.STABLE_ERROR, SliceState.STABLE_OK, SliceState.CONFIGURING), 
		DELETE("Delete", SliceState.STABLE_ERROR, SliceState.STABLE_OK, SliceState.CONFIGURING, SliceState.DEAD, SliceState.NULL), 
		REEVALUATE("Re-evaluate", SliceState.CONFIGURING, SliceState.CLOSING, SliceState.NULL, 
				SliceState.STABLE_ERROR, SliceState.STABLE_OK, SliceState.DEAD);
		
		// allowed originating states for each command
		private List<SliceState> validFromStates;
		private String n; 
		private SliceCommand(String s, SliceState ...sliceStates) {
			n = s;
			validFromStates = Arrays.asList(sliceStates);
		}
		
		@Override
		public String toString() {
			return n;
		}
	};
	
	// class to simplify the binning of 
	// reservation states
	private static class StateBins {
		public static final int MaxBins = 16;
		private int[] bins = new int[MaxBins];
		
		public void add(int s) {
			if ((s >= 0) && (s < MaxBins))
				bins[s]++;
		}
		
		/**
		 * Does the specified state appear in the bin?
		 * @param s
		 * @return
		 */
		public boolean hasState(int s) {
			if ((s >= 0) && (s < MaxBins)) {
				if (bins[s] > 0)
					return true;
			}
			return false;
		}
		
		/**
		 * Do any other states, other than s appear in the bin?
		 * @param s
		 * @return
		 */
		public boolean hasStatesOtherThan(int ...s) {
			int count = 0;
			for (int i = 0; i < MaxBins; i++) {
				if (bins[i] > 0)
					count ++;
			}
			
			int count1 = 0;
			for(int i = 0; i < s.length; i++) {
				if (bins[s[i]] > 0)
					count1 ++;
			}
			
			if ((count1 == count) && (count > 0))
				return false;
			return true;
		}
	};
	
	public static class SliceTransitionException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public SliceTransitionException(String s) {
			super(s);
		}
	};
	
	private SliceState state;
	private final String sliceGuid;
	
	/**
	 * Create a new state machine for new or restored slice
	 * @param sid
	 * @param restore
	 */
	public SliceStateMachine(String sid, boolean recover) {
		if (recover)
			state = SliceState.CONFIGURING;
		else
			state = SliceState.NULL;
		sliceGuid = sid;
	}
	
	private List<ReservationMng> getSliceReservations() {

		IOrcaServiceManager sm = null;
		try {
			sm = XmlrpcOrcaState.getInstance().getSM();
			List<ReservationMng> rr = sm.getReservations(new SliceID(sliceGuid));
			return rr;
		} catch (Exception e) {
			return null;
		} finally {
			if (sm != null)
				XmlrpcOrcaState.getInstance().returnSM(sm);
		}
	}
	
	/**
	 * We don't introduce a special state to flag when a slice is ALL FAILED, however
	 * this helper function helps decide when to GC a slice
	 * @return
	 */
	public boolean allFailed() {
		List<ReservationMng> allRes = getSliceReservations();
		
		StateBins b = new StateBins();
		for (ReservationMng r: allRes) {
			b.add(r.getState());
		}
		
		if (!b.hasStatesOtherThan(OrcaConstants.ReservationStateFailed)) 
			return true;
		return false;
	}
	
	/**
	 * Attempt to transition a slice to a new state
	 * @param cmd
	 * @return
	 * @throws SliceTransitionException
	 */
	public SliceState transitionSlice(SliceCommand cmd) throws SliceTransitionException {
		
		// state transitions can be affected by direct commands,
		// or by forcing a re-evaluation of the slice state based
		// on the extended set of parameters (e.g. reservation states)
		
		if (!cmd.validFromStates.contains(state))
			throw new SliceTransitionException("Command " + cmd + " cannot transition from state " + state);
		
		switch(cmd) {
		case CREATE:
			state=SliceState.CONFIGURING;
			return state;
		case MODIFY:
			state=SliceState.CONFIGURING;
			return state;
		case DELETE:
			// a slice that is already dead, can just stay dead
			if (state != SliceState.DEAD)
				state=SliceState.CLOSING;
			return state;
		case REEVALUATE:
			// look at slice reservations and their states
			// and see if a transition is possible
			List<ReservationMng> allRes = getSliceReservations();
			
			// not clear what to do here exactly. Put it in NULL or DEAD?
			if ((allRes == null) || (allRes.size() == 0)){
				return state;
			}
			
			StateBins b = new StateBins();
			for (ReservationMng r: allRes) {
				b.add(r.getState());
			}
			
			switch(state) {
			case NULL:
			case CONFIGURING:
				// if all ticketed/redeeming - stay there
				if (!b.hasStatesOtherThan(OrcaConstants.ReservationStateActive, OrcaConstants.ReservationStateClosed))
					state = SliceState.STABLE_OK;
				// if all active or failed and there are failed - error
				if ((!b.hasStatesOtherThan(OrcaConstants.ReservationStateActive, OrcaConstants.ReservationStateFailed, OrcaConstants.ReservationStateClosed)) &&
						(b.hasState(OrcaConstants.ReservationStateFailed)))
					state = SliceState.STABLE_ERROR;
				// we could be dead (due to timeout)
				if (!b.hasStatesOtherThan(OrcaConstants.ReservationStateClosed, OrcaConstants.ReservationStateCloseWait, OrcaConstants.ReservationStateFailed))
					state = SliceState.CLOSING;
				break;
			case STABLE_OK:
			case STABLE_ERROR:
				if (!b.hasStatesOtherThan(OrcaConstants.ReservationStateClosed, OrcaConstants.ReservationStateCloseWait, OrcaConstants.ReservationStateFailed))
					state = SliceState.DEAD;
				if (!b.hasStatesOtherThan(OrcaConstants.ReservationStateClosed, 
						OrcaConstants.ReservationStateCloseWait, 
						OrcaConstants.ReservationPendingStateClosing, 
						OrcaConstants.ReservationStateFailed))
					state = SliceState.CLOSING;
				break;
			case CLOSING:
				if (!b.hasStatesOtherThan(OrcaConstants.ReservationStateClosed, OrcaConstants.ReservationStateCloseWait, OrcaConstants.ReservationStateFailed))
					state = SliceState.DEAD;
				break;
			case DEAD:
				break;
			}
			
			break;
		}
		return state;
	}
	
	public SliceState getState() {
		return state;
	}
		
	public static void main(String[] argv) {
		StateBins b = new StateBins();
		
		b.add(OrcaConstants.ReservationStateTicketed);
		b.add(OrcaConstants.ReservationStateActive);
		
		System.out.println("Test");
		assert(b.hasState(OrcaConstants.ReservationStateTicketed));
		assert(b.hasState(OrcaConstants.ReservationStateActive));
		assert(!b.hasStatesOtherThan(OrcaConstants.ReservationStateTicketed, OrcaConstants.ReservationStateActive));
		assert(b.hasStatesOtherThan(OrcaConstants.ReservationPendingStateRedeeming));
		System.out.println("Done");
		
	}
}
