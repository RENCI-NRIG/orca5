/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.xmlrpc.pubsub;

import java.util.Date;

/**
 *
 * @author anirban
 */
public class SliceState {

	public static enum PubSubState {
		SUBMITTED("SUBMITTED"),
		INPROGRESS("INPROGRESS"),
		WAITINPROGRESS("WAITINPROGRESS"),
		DONE("DONE"),
		DELETED("DELETED"),
		EXPUNGE("EXPUNGE"),
		DONEACTIVE("DONEACTIVE"),
		DONECLOSED("DONECLOSED");
		
		private String name;
		private PubSubState(String n) {
			name = n;
		}
		@Override
		public String toString() {
			return name;
		}
	}
    private String slice_urn; // human readable slice id
    private String orcaSliceID; // needed because sometimes pubsub actions happen after a slice no longer exists in the orca SM
    private PubSubState state; // SUBMITTED, INPROGRESS, WAITINPROGRESS, DONE, EXPUNGE
    private Date startTime; // date when slice was created
    private Date endTime; // date when slice was closed
    private int waitTime; // waiting time in terms of number of ticks of the publisher timer task
    private String sliceOwner; // cert DN of slice owner

    public SliceState(String slice_urn, PubSubState state, int waitTime) {
        this.slice_urn = slice_urn;
        this.state = state;
        this.waitTime = waitTime;
    }

    public SliceState(String slice_urn, String orcaSliceID, PubSubState state, Date startTime, Date endTime, int waitTime, String sliceOwner) {
        this.slice_urn = slice_urn;
        this.orcaSliceID = orcaSliceID;
        this.state = state; // mutable
        this.startTime = startTime;
        this.endTime = endTime; // mutable
        this.waitTime = waitTime; // mutable
        this.sliceOwner = sliceOwner;
    }

    public String getSlice_urn() {
        return slice_urn;
    }

    public void setSlice_urn(String slice_urn) {
        this.slice_urn = slice_urn;
    }

    public PubSubState getState() {
        return state;
    }

    public void setState(PubSubState state) {
        this.state = state;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getOrcaSliceID() {
        return orcaSliceID;
    }

    public void setOrcaSliceID(String orcaSliceID) {
        this.orcaSliceID = orcaSliceID;
    }

    public String getSliceOwner() {
        return sliceOwner;
    }

    public void setSliceOwner(String sliceOwner) {
        this.sliceOwner = sliceOwner;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

}
