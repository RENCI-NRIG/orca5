/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.controllers.xmlrpc.pubsub;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import orca.shirako.container.Globals;


/**
 *
 * @author anirban
 */
@SuppressWarnings("serial")
public final class PublishQueue implements Serializable{

    private ArrayList<SliceState> slicesToWatch = new ArrayList<SliceState>(); // slices to watch for the publisher state machine
    private ArrayList<SliceState> sliceList = new ArrayList<SliceState>(); // list of active slices that need to be published (aka sliceList for blowhole consumption)
    private ArrayList<SliceState> newSlices = new ArrayList<SliceState>(); // list of newly added slices
    private ArrayList<String> deletedSlices = new ArrayList<String>(); // list of urns of newly deleted slices

    // use output compression
    private static boolean compressOutput = true;

    private static final PublishQueue fINSTANCE =  new PublishQueue();

    private PublishQueue(){
            // Can't call this constructor
    }

    public static PublishQueue getInstance() {
        return fINSTANCE;
    }

    public ArrayList<SliceState> getCurrentSliceList(){
    	return sliceList;
    }

    public synchronized void addToSliceList(SliceState slice){
    	sliceList.add(slice);
    }

    public synchronized void deleteFromSliceList(String slice_urn){
    	SliceState toRemoveSliceState = null;
    	if(sliceList != null){
    		Iterator<SliceState> it = sliceList.iterator();
    		while(it.hasNext()){ // go through all the slicestates
    			SliceState currSliceState = (SliceState) it.next();
    			String currSliceUrn = currSliceState.getSlice_urn();
    			if(currSliceUrn.equalsIgnoreCase(slice_urn)){
    				toRemoveSliceState = currSliceState;
    			}
    		}
    		if(toRemoveSliceState != null){
    			try{
    				Globals.Log.info("PublishQueue: Removing " + toRemoveSliceState.getSlice_urn() + " from sliceList");
    				sliceList.remove(toRemoveSliceState);
    			}
    			catch(Exception e){
    				Globals.Log.error("PublishQueue: Exception while deleting entry from Slice List : " + e);
    			}
    		}
    	}
    }

    public synchronized void setPropsForSliceInSliceList(String slice_urn, SliceState.PubSubState newState, Date newEndTime, int newWaitTime){
    	if(sliceList != null){
    		Iterator<SliceState> it = sliceList.iterator();
    		while(it.hasNext()){ // go through all the slicestates
    			SliceState currSliceState = (SliceState) it.next();
    			String currSliceUrn = currSliceState.getSlice_urn();
    			if(currSliceUrn.equalsIgnoreCase(slice_urn)){
    				currSliceState.setEndTime(newEndTime);
    				currSliceState.setState(newState);
    				currSliceState.setWaitTime(newWaitTime);
    			}
    		}
    	}
    }

    public ArrayList<SliceState> getCurrentQ() {
    	return slicesToWatch;
    }


    /**
     * Add a new slices for processing
     * @param slice
     */
    public void addToNewSlicesQ(SliceState slice) {
    	// locking this separately from the main object monitor /ib
    	synchronized(newSlices) {
    		newSlices.add(slice);
    	}
    }

    /**
     * Flag slice as deleted
     * @param sliceUrn
     */
    public void addToDeletedSlicesQ(String sliceUrn) {
    	// locking this separately from main object monitor /ib
    	synchronized(deletedSlices) {
    		deletedSlices.add(sliceUrn);
    	}
    }

    /**
     * Any deleted slices accumulated on deleted slices Q
     * get dealt with here
     */
    public synchronized void drainDeleted() {
    	synchronized(deletedSlices) {
    		for(String ss: deletedSlices) {
    			Globals.Log.debug("PublishQueue: Deleting slice " + ss);
    			deleteFromPubQ(ss);
    		}
    	}
    }
    
    /**
     * Any new slices accumulated on new slices Q
     * get dealt with here
     */
    public synchronized void drainNew() {
    	synchronized(newSlices) {
    		for(SliceState ss: newSlices) {
    			Globals.Log.debug("PublishQueue: Adding slice " + ss.getSlice_urn());
    			addToPubQ(ss);
    		}
    	}
    }
    
    private void addToPubQ(SliceState slice){
    	slicesToWatch.add(slice);
    }
    
    private void deleteFromPubQ(String slice_urn){
    	if(slicesToWatch != null){
    		Iterator<SliceState> it = slicesToWatch.iterator();
    		while(it.hasNext()){ // go through all the slicestates
    			SliceState currSliceState = (SliceState) it.next();
    			String currSliceUrn = currSliceState.getSlice_urn();
    			if(currSliceUrn.equalsIgnoreCase(slice_urn)){
    				currSliceState.setState(SliceState.PubSubState.DELETED);
    			}
    		}
    	}
    }

    // manage state of compression of output
    public boolean getCompression() {
            return compressOutput;
    }

    public synchronized void setCompression(boolean f) {
            compressOutput = f;
    }

    /**
    * If the singleton implements Serializable, then this
    * method must be supplied.
    */
    private Object readResolve() throws ObjectStreamException {
        return fINSTANCE;
    }

}
