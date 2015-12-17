/**
 * PublishQueue for publishing manifests and slice lists
 */

package orca.controllers.xmlrpc.pubsub;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import orca.shirako.container.Globals;


/**
 *
 * @author anirban
 */
@SuppressWarnings("serial")
public final class PublishQueue implements Serializable{

    private Set<SliceState> slicesToWatch = new HashSet<SliceState>(); // slices to watch for the publisher state machine
    private Set<SliceState> newSlices = new HashSet<SliceState>(); // list of newly added slices
    private Set<String> deletedSlices = new HashSet<String>(); // list of IDs of newly deleted slices
    private Set<String> modifiedSlices = new HashSet<String>(); // list of modified slices

    // use output compression
    private static boolean compressOutput = true;

    private static final PublishQueue fINSTANCE =  new PublishQueue();

    private PublishQueue(){
            // Can't call this constructor
    }

    public static PublishQueue getInstance() {
        return fINSTANCE;
    }


    public Set<SliceState> getCurrentQ() {
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
     * Add a modified slice to publish queue 
     * @param sliceID
     */
    public void addToModifiedSlicesQ(String sliceID) {
    	synchronized(modifiedSlices) {
    		modifiedSlices.add(sliceID);
    	}
    }
    
    /**
     * Flag slice as deleted
     * @param sliceUrn
     */
    public void addToDeletedSlicesQ(String sliceID) {
    	// locking this separately from main object monitor /ib
    	synchronized(deletedSlices) {
    		deletedSlices.add(sliceID);
    	}
    }

    /**
     * Any deleted slices accumulated on deleted slices Q
     * get dealt with here
     */
    public void drainDeleted() {
    	synchronized(deletedSlices) {
    		for(String ss: deletedSlices) {
    			Globals.Log.debug("PublishQueue: Deleting slice " + ss);
    			deleteFromPubQ(ss);
    		}
    		deletedSlices.clear();
    	}
    }
    
    /**
     * Any new slices accumulated on new slices Q
     * get dealt with here
     */
    public void drainNew() {
    	synchronized(newSlices) {
    		for(SliceState ss: newSlices) {
    			Globals.Log.debug("PublishQueue: Adding slice " + ss.getSlice_urn() + "/" + ss.getSlice_ID());
    			addToPubQ(ss);
    		}
    		newSlices.clear();
    	}
    }
    
    public void drainModified() {
    	synchronized(modifiedSlices) {
    		for(String sliceID: modifiedSlices) {
    			Globals.Log.debug("PublishQueue: Adding modified slice " + sliceID);
    			modifyPubQ(sliceID);
    		}
    		modifiedSlices.clear();
    	}
    }
    
    private void addToPubQ(SliceState slice){
    	slicesToWatch.add(slice);
    }
    
    private void deleteFromPubQ(String slice_ID){
    	if(slicesToWatch != null){
    		Iterator<SliceState> it = slicesToWatch.iterator();
    		while(it.hasNext()){ // go through all the slicestates
    			SliceState currSliceState = (SliceState) it.next();
    			String currSliceID = currSliceState.getSlice_ID();
    			if(currSliceID.equalsIgnoreCase(slice_ID)){
    				currSliceState.setState(SliceState.PubSubState.DELETED);
    			}
    		}
    	}
    }
    
    private void modifyPubQ(String slice_ID) {
    	if (slicesToWatch != null) {
    		Iterator<SliceState> it = slicesToWatch.iterator();
    		while(it.hasNext()){ // go through all the slicestates
    			SliceState currSliceState = (SliceState) it.next();
    			String currSliceID = currSliceState.getSlice_ID();
    			if(currSliceID.equalsIgnoreCase(slice_ID)){
    				currSliceState.setState(SliceState.PubSubState.INPROGRESS);
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
