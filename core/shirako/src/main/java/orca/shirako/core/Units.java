package orca.shirako.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import orca.shirako.common.UnitID;

public class Units implements Collection<Unit> {
    /**
     * The units.
     */
    protected HashMap<UnitID, Unit> units;

    public Units() {
        units = new HashMap<UnitID, Unit>();
    }
    
    public Units(Units other) {
        this();
        addAll(other);
    }
    
    public boolean add(Unit o) {
        if (units.containsKey(o.getID())) {
            return false;
        }
        units.put(o.getID(), o);
        return true;
    }

    public boolean addAll(Collection<? extends Unit> c) {
        boolean changed = false;
        for (Unit u : c) {
            if (!units.containsKey(u.getID())) {
                units.put(u.getID(), u);
                changed = true;
            }
        }
        return changed;
    }

    public void clear() {
        units.clear();
    }

    public boolean contains(Object o) {
        if (o instanceof Unit) {
            return units.containsKey(((Unit) o).getID());
        }
        return false;
    }

    public boolean contains(UnitID id){
        return units.containsKey(id);
    }
    
    public boolean contains(Unit u){
        return units.containsKey(u.id);
    }
    
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return units.size() == 0;
    }

    public Iterator<Unit> iterator() {
        return units.values().iterator();
    }

    public boolean remove(Object o) {
        if (o instanceof Unit) {
            return units.remove(((Unit) o).getID()) != null;
        }
        return false;
    }

    public Unit remove(UnitID id){
        return units.remove(id);
    }
    
    public boolean remove(Unit u){
        return units.remove(u.getID()) != null;
    }
    
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            changed |= remove(o);
        }
        return changed;
    }

    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        ArrayList<Unit> toRemove = new ArrayList<Unit>();

        for (Unit u : units.values()) {
            if (!c.contains(u)) {
                toRemove.add(u);
            }
        }
        if (toRemove.size() > 0) {
            changed = true;
            for (int i = 0; i < toRemove.size(); i++) {
                units.remove(toRemove.get(i).getID());
            }
        }
        return changed;
    }

    public int size() {
        return units.size();
    }

    public Object[] toArray() {
        return units.values().toArray();
    }

    public <T> T[] toArray(T[] a) {
        return units.values().toArray(a);
    }
    
    @Override
    public Object clone(){
        return new Units(this);
    }
    
    public Units deepClone() {
        Units set = new Units();
        for (Unit u : units.values()){
            Unit clone = (Unit)u.clone();
            set.add(clone);
        }
        return set;
    }
    
    
    /**
     * Returns a new collection that contains the units
     * from this collection that are not in the passed in
     * collection.
     * @param units
     * @return
     */
    public Units missing(Units units){
        // clone this collection
        Units result = new Units(this);
        if (units == null){
            return result;
        }
        // remove from the clone all common entries
        result.removeAll(units);
        return result;
    }
    
    public Unit get(UnitID id){
        return units.get(id);
    }    
}
