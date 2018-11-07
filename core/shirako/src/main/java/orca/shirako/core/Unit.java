package orca.shirako.core;

import java.util.Properties;

import orca.shirako.api.IReservation;
import orca.shirako.common.ReservationID;
import orca.shirako.common.SliceID;
import orca.shirako.common.UnitID;
import orca.shirako.common.meta.UnitProperties;
import orca.shirako.plugins.config.ConfigToken;
import orca.shirako.util.Notice;
import orca.util.ID;
import orca.util.PropList;
import orca.util.ResourceType;
import orca.util.persistence.NotPersistent;
import orca.util.persistence.Persistable;
import orca.util.persistence.Persistent;


// FIXME: synchronized methods? probably not needed anymore
public class Unit implements Persistable, Cloneable, UnitProperties, ConfigToken {
    /**
     * Unique identifier.
     */
	@Persistent (key = UnitID)
    protected UnitID id;    
    /**
     * Resource type.
     */
	@Persistent (key = UnitResourceType)
    protected ResourceType rtype;
    /**
     * Unique identifier of parent unit (optional).
     */
	@Persistent (key = UnitParentID)
    protected UnitID parentId;
    /**
     * Properties list.
     */
	@Persistent (key = "ignored", merge = true)
    protected Properties properties = new Properties();
    /**
     * Unit state.
     */
	@Persistent (key = UnitInternalState)
    protected UnitState state = UnitState.DEFAULT;
    /**
     * Configuration sequence number. Each unique configuration action is
     * identified by a sequence number.
     */
	@Persistent (key = UnitActionSequence)
    protected long sequence;
    /**
     * Reservation this unit belongs to (id).
     */
	@Persistent (key = UnitReservationID)
    protected ReservationID reservationId;
    /**
     * Slice this unit belongs to.
     */
	@Persistent (key = UnitSliceID)
    protected SliceID sliceId;
    /**
     * Actor this unit belongs to.
     */
	@Persistent (key = UnitActorID)
    protected ID actorId;    
	
	@Persistent (key = UnitNotices)
	protected Notice notices = new Notice();

	/**
     * Reservation this unit belongs to.
     */
	@NotPersistent
    protected IReservation reservation;
    /**
     * The modified version of this unit.
     */
	@NotPersistent
	protected Unit modified;
	
	@NotPersistent
    protected boolean transferOutStarted;
    
    public Unit() {
        id = new UnitID();
    }

    public Unit(ReservationID rid, SliceID sliceID, ID actorID) {
        this (new UnitID(), rid, sliceID, actorID);
    }
    
    public Unit(UnitID id, ReservationID rid, SliceID sliceID, ID actorID){
        this.id = id;
        this.reservationId = rid;
        this.sliceId = sliceID;
        this.actorId = actorID;
    }
    
    public Unit(UnitID id, Properties properties, UnitState state) {
        this.id = id;
        this.properties = properties;
        this.state = state;
    }


    public synchronized void mergeProperties(Properties incoming) {
        PropList.mergeProperties(incoming, properties);
    }
    
    public synchronized void unsetProperties(Properties toUnset) {
        PropList.unsetProperties(toUnset, properties);
    }
    
    private void transition(UnitState tostate) {
        state = tostate;
    }

    public synchronized void fail(String message, Exception e) {
        notices.add(message, e);
        transition(UnitState.FAILED);
    }

    public synchronized void fail(String message) {
        fail(message, null);
    }
    
    public synchronized void failOnModify(String message, Exception e) {
        notices.add(message, e);
        // transition to ACTIVE even when modify failed with non-zero exit code or an exception was raised
        // because a particular modify target was not found
        transition(UnitState.ACTIVE);
        mergeProperties(modified.properties);
    }

    public synchronized void failOnModify(String message) {
        failOnModify(message, null);
    }

    public synchronized void setState(UnitState state) {
        transition(state);
    }
    
    public synchronized void startClose() {
        transition(UnitState.CLOSING);
        transferOutStarted = true;
    }

    public synchronized boolean startPrime() {
        switch (state) {
            case DEFAULT:
            case PRIMING:
                transition(UnitState.PRIMING);
                return true;
            default:
                return false;
        }
    }

    public synchronized boolean startModify() {
        switch (state) {
            case ACTIVE:
            case MODIFYING:
                transition(UnitState.MODIFYING);
                return true;
            default:
                return false;
        }
    }

    public synchronized void activate() {
        state = UnitState.ACTIVE;
    }
    
    public synchronized void close() {
        state = UnitState.CLOSED;
    }
    
    public synchronized UnitID getID() {
        return id;
    }

    public synchronized String getProperty(String name) {
        return properties.getProperty(name);
    }

    public synchronized Properties getProperties() {
        return properties;
    }
    
    public synchronized void setProperty(String name, String value) {
        properties.setProperty(name, value);
    }

    public synchronized void deleteProperty(String name) {
        properties.remove(name);
    }

    public synchronized UnitState getState() {
        return state;
    }

    @Override
    public synchronized Object clone() {
        return new Unit(id, properties, state);
    }

    public synchronized long getSequence() {
        return sequence;
    }

    public synchronized long getSequenceIncrement() {
        return ++sequence;
    }

    protected synchronized long incrementSequence() {
        sequence++;
        return sequence;
    }

    protected synchronized long decrementSequence() {
        sequence--;
        return sequence;
    }

    protected synchronized void setReservation(IReservation reservation){
    	this.reservationId = reservation.getReservationID();
    	this.reservation = reservation;
    }
    
    protected synchronized void setSliceID(SliceID sliceId) {
        this.sliceId = sliceId;
    }

    protected synchronized void setActorID(ID actorId) {
        this.actorId = actorId;
    }

    public synchronized boolean isFailed() {
        return state == UnitState.FAILED;
    }

    public synchronized boolean isClosed() {
        return state == UnitState.CLOSED;
    }

    public synchronized boolean isActive() {
        return state == UnitState.ACTIVE;
    }

    public synchronized boolean hasPendingAction() {
        return UnitState.isPending(state);
    }

    public synchronized boolean isPendingModifying() {
        return UnitState.isPendingModifying(state);
    }
    
    public synchronized void setModified(Unit modified) {
        this.modified = modified;
    }

    public synchronized Unit getModified() {
        return modified;
    }
    
    public synchronized SliceID getSliceID(){
        return sliceId;
    }
    
    public synchronized ReservationID getReservationID() {
        return reservationId;
    }
    
    public synchronized IReservation getReservation() {
    	return reservation;
    }
    
    public synchronized UnitID getParentID() {
        return parentId;
    }
    
    public synchronized void setParentID(UnitID parentId) {
        this.parentId = parentId;
    }
    
    public synchronized void completeModify() {
        transition(UnitState.ACTIVE);
        mergeProperties(modified.properties);
    }
    
    public synchronized ID getActorID() {
        return actorId;
    }
    
    public synchronized ResourceType getResourceType() {
        return rtype;
    }
    
    public synchronized void setResourceType(ResourceType rtype) {
        this.rtype = rtype;
    } 

    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override 
    public boolean equals(Object obj) {
        if (!(obj instanceof Unit)) {
            return false;
        }
        Unit other = (Unit)obj;
        
        return this.id.equals(other.id);
    }
    
    public Notice getNotices() {
        return notices;
    }
    
    public void addNotice(String notice) {
        notices.add(notice);
    }
    
    public String toString() {
    	return "[unit: " + id + " reservation: " + reservationId + " actor: " + actorId + " state: " + state + "]";
    }
}
