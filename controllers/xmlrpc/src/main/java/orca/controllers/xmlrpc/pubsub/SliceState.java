/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.xmlrpc.pubsub;

import java.util.Date;

import orca.controllers.xmlrpc.XmlrpcControllerSlice;

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
	private XmlrpcControllerSlice slice;
    private PubSubState state; // SUBMITTED, INPROGRESS, WAITINPROGRESS, DONE, EXPUNGE
    private Date startTime; // date when slice was created
    private Date endTime; // date when slice was closed
    private int waitTime; // waiting time in terms of number of ticks of the publisher timer task

    public SliceState(XmlrpcControllerSlice s, PubSubState state, Date startTime, Date endTime, int waitTime) {
        this.slice = s;
        this.state = state; // mutable
        this.startTime = startTime;
        this.endTime = endTime; // mutable
        this.waitTime = waitTime; // mutable
    }

    public String getSlice_urn() {
        return slice.getSliceUrn();
    }
    
    public String getSlice_ID() {
    	return slice.getSliceID();
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
        return slice.getId();
    }

    public String getSliceOwner() {
        return slice.getUserDN();
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

}
