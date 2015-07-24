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


/**
 *
 * @author anirban
 */
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
        synchronized(sliceList) {
            return sliceList;
        }
    }

    public void addToSliceList(SliceState slice){
        synchronized(sliceList) {
            sliceList.add(slice);
        }
    }

    public void deleteFromSliceList(String slice_urn){
        SliceState toRemoveSliceState = null;
        synchronized(sliceList) {
            if(sliceList != null){
                Iterator it = sliceList.iterator();
                while(it.hasNext()){ // go through all the slicestates
                    SliceState currSliceState = (SliceState) it.next();
                    String currSliceUrn = currSliceState.getSlice_urn();
                    if(currSliceUrn.equalsIgnoreCase(slice_urn)){
                        toRemoveSliceState = currSliceState;
                    }
                }
                if(toRemoveSliceState != null){
                    try{
                        System.out.println("Removing " + toRemoveSliceState.getSlice_urn() + " from sliceList");
                        sliceList.remove(toRemoveSliceState);
                    }
                    catch(Exception e){
                        System.out.println("Exception while deleting entry from Slice List : " + e);
                    }
                }
            }
        }
    }

    public void setPropsForSliceInSliceList(String slice_urn, SliceState.PubSubState newState, Date newEndTime, int newWaitTime){
        synchronized(sliceList) {
            if(sliceList != null){
                Iterator it = sliceList.iterator();
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
    }

    public ArrayList<SliceState> getCurrentQ() {
        synchronized(slicesToWatch) {
            return slicesToWatch;
        }
    }

    public void addToPubQ(SliceState slice){
        synchronized(slicesToWatch) {
            slicesToWatch.add(slice);
        }
    }

    public ArrayList<SliceState> getNewSlicesQ() {
        synchronized(newSlices) {
            return newSlices;
        }
    }

    public void addToNewSlicesQ(SliceState slice){
        synchronized(newSlices) {
            newSlices.add(slice);
        }
    }

    public ArrayList<String> getDeletedSlicesQ() {
        synchronized(deletedSlices) {
            return deletedSlices;
        }
    }

    public void addToDeletedSlicesQ(String sliceUrn){
        synchronized(deletedSlices) {
            deletedSlices.add(sliceUrn);
        }
    }

    public void deleteFromPubQ(SliceState slice){
        synchronized(slicesToWatch) {
            slicesToWatch.remove(slice);
        }
    }

    public void deleteFromPubQ(String slice_urn){
        SliceState toRemoveSliceState = null;
        synchronized(slicesToWatch) {
            if(slicesToWatch != null){
                Iterator it = slicesToWatch.iterator();
                while(it.hasNext()){ // go through all the slicestates
                    SliceState currSliceState = (SliceState) it.next();
                    String currSliceUrn = currSliceState.getSlice_urn();
                    if(currSliceUrn.equalsIgnoreCase(slice_urn)){
                        currSliceState.setState(SliceState.PubSubState.DELETED);
                    }
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
