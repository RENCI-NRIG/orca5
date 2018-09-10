package net.exogeni.orca.util.persistence;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Properties;

import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.OrcaException;
import net.exogeni.orca.util.ReflectionUtils;
import net.exogeni.orca.util.Serializer;

public class PersistenceUtils implements PersistenceProperties {
    // CHECKBEFORESHIPPING: turn this off before release!!!
    public static boolean validatePersistence = false;

    private static HashMap<Class<?>, PersistenceState> persistenceMap = new HashMap<Class<?>, PersistenceState>();
    private static final Object persistenceLock = new Object();

    private static PersistenceState getResolvedState(Class<?> c) throws PersistenceException {
        synchronized (persistenceLock) {
            PersistenceState state = persistenceMap.get(c);
            if (state == null) {
                state = new PersistenceState(c);
                persistenceMap.put(c, state);
            }
            return state;
        }
    }

    /**
     * Resolves a persistable class
     * 
     * @param item
     *            a Persistable type
     * @throws PersistenceException in case of error
     */
    public static void resolve(Class<?> item) throws PersistenceException {
        if (!Persistable.class.isAssignableFrom(item)) {
            throw new PersistenceException("Class " + item.getName() + " is not Persistable");
        }

        synchronized (persistenceLock) {
            if (persistenceMap.containsKey(item)) {
                return;
            }
            PersistenceState state = new PersistenceState(item);
            persistenceMap.put(item, state);
        }
    }

    /**
     * Persists the specified object into a Properties list.
     * 
     * @param toPersist
     *            object to persist
     * @param <V> Type of the parameter
     * @return Properties list representing the persisted object
     * @throws PersistenceException in case of error
     */
    public static <V extends Persistable> Properties save(V toPersist) throws PersistenceException {
        if (toPersist == null) {
            throw new IllegalArgumentException("toPersist");
        }

        //System.out.println("Saving " + toPersist.getClass().getSimpleName());
        
        PersistenceState state = getResolvedState(toPersist.getClass());
        Properties p = state.save(toPersist);

        if (validatePersistence) {
            // restore the saved object and compare it to the original
            V restored = restore(p);
            validateRestore(toPersist, restored);
        }

        return p;
    }

    /**
     * Restores an object from a Properties list
     * 
     * @param savedState saved properties
     * @param <V> Type of the parameter
     * @return the restored object
     * @throws PersistenceException in case of error
     */
    public static <V extends Persistable> V restore(Properties savedState)
            throws PersistenceException {
        return restore(savedState, false);
    }

    /**
     * Restores an object from a Properties list
     * 
     * @param savedState saved properties
     * @param overrideCustomRestorer flag indicating if custom restorer is overriden
     * @param <V> Type of the parameter
     * @return the restored object
     * @throws PersistenceException in case of error
     */
    public static <V extends Persistable> V restore(Properties savedState,
            boolean overrideCustomRestorer) throws PersistenceException {
        // FIXME: not sure if this is the right classloader
        return restore(savedState, PersistenceUtils.class.getClassLoader(), overrideCustomRestorer);
    }

    public static <V extends Persistable> V restore(Properties savedState, ClassLoader cl)
            throws PersistenceException {
        return restore(savedState, cl, false);
    }

    /**
     * Restores an object from a Properties list using the specified class
     * loader.
     * 
     * @param savedState saved properties
     * @param cl class loader
     * @param overrideCustomRestorer flag indicating if custom restorer is overriden
     * @param <V> Type of the parameter
     * @return the restored object
     * @throws PersistenceException in case of exception
     */
    public static <V extends Persistable> V restore(Properties savedState, ClassLoader cl,
            boolean overrideCustomRestorer) throws PersistenceException {
        if (savedState == null) {
            throw new IllegalArgumentException("savedState cannot be null");
        }
        if (cl == null) {
            throw new IllegalArgumentException("cl cannot be null");
        }

        String className = savedState.getProperty(PropertyClassName);
        if (className == null) {
            throw new PersistenceException("Missing class name property");
        }

        try {
            Class<V> c = ReflectionUtils.getClass(className, cl);
            return restore(c, savedState, overrideCustomRestorer);
        } catch (Exception e) {
            throw new PersistenceException("Could not instantiate object from saved state", e);
        }
    }

    public static <V extends Persistable> V restore(Class<V> c, Properties savedState)
            throws PersistenceException {
        return restore(c, savedState, false);
    }

    public static <V extends Persistable> V restore(Class<V> c, Properties savedState,
            boolean overrideCustomeRestorer) throws PersistenceException {
        if (c == null) {
            throw new IllegalArgumentException("c cannot be null");
        }
        if (savedState == null) {
            throw new IllegalArgumentException("savedState cannot be null");
        }

        try {
            if (!overrideCustomeRestorer && c.isAnnotationPresent(Restore.class)) {
                // if the class specifies a custom restorer, then use it
                Restore restore = c.getAnnotation(Restore.class);
                Restorer restorer = (Restorer) ReflectionUtils.createInstance(restore.value());
                // FIXME: ugly: serializing back to string
                return (V) restorer.restore(Serializer.toString(savedState));
            } else {
                V result = ReflectionUtils.createInstance(c);
                restore(result, savedState);
                return result;
            }
        } catch (Exception e) {
            throw new PersistenceException("Could not instantiate object from saved state", e);
        }
    }

    public static <V extends Persistable> void restore(V object, Properties savedState)
            throws PersistenceException {
        if (object == null) {
            throw new IllegalArgumentException("object cannot be null");
        }
        if (savedState == null) {
            throw new IllegalArgumentException("savedState cannot be null");
        }

        PersistenceState state = getResolvedState(object.getClass());
        state.restore(object, savedState);
    }

    public static <V extends Recoverable & RecoverParent> void recover(V obj, Properties savedState)
            throws OrcaException {
        recover(obj, obj, savedState);
    }

    public static <V extends Recoverable, Y extends RecoverParent> V recover(Y parent,
            Properties savedState) throws OrcaException {
        V obj = restore(savedState);
        recover(obj, parent, savedState);
        return obj;
    }

    /**
     * Recovers the specified object using the parent and saved state.
     * 
     * @param obj
     *            object to recover
     * @param parent parent object
     * @param savedState saved Properties
     * @param <V> Type of the object 
     * @param <Y> Type of the parent 
     * @throws net.exogeni.orca.util.OrcaException in case of error
     */
    public static <V extends Recoverable, Y extends RecoverParent> void recover(V obj, Y parent,
            Properties savedState) throws OrcaException {
        recover(obj, new RecoveredObjects<Y>(parent), savedState);
    }

    public static <V extends Recoverable, Y extends RecoverParent> void recover(V obj,
            RecoveredObjects<Y> ros, Properties savedState) throws OrcaException {
        ros.add(obj);
        PersistenceState state = getResolvedState(obj.getClass());
        state.recover(obj, ros, savedState);
    }

    public static Properties persistObject(Object value) throws PersistenceException,
            NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {
        return PersistenceState.persistObject(value);
    }

    public static void persistObject(Object value, Properties saved, String key, boolean merge,
            boolean reference) throws PersistenceException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException, InstantiationException {
        PersistenceState.persistObject(value, saved, key, merge, reference);
    }

    public static Object restoreObject(Properties saved) throws PersistenceException,
            ClassNotFoundException {
        return PersistenceState.restoreObject(saved);
    }

    public static Object restoreObject(Class<?> t, String saved) throws PersistenceException {
        return PersistenceState.restoreObject(t, saved);
    }

    public static <V extends Persistable> void validateRestore(V original, V restored)
            throws PersistenceException {
        PersistenceState state = getResolvedState(original.getClass());
        state.validateRestore(original, restored);
    }

    public static class RecoveredObject {
        private Class<?> type;
        private Object obj;

        public RecoveredObject(Object obj) {
            this.obj = obj;
            this.type = obj.getClass();
        }

        public Class<?> getType() {
            return type;
        }

        public Object getObject() {
            return obj;
        }
    }

    public static class RecoveredObjects<Y extends RecoverParent> implements RecoverParent {
        private Y parent;
        // private HashMap<Class<?>, RecoveredObject> normal;
        private HashMap<Class<?>, HashMap<ID, RecoveredObject>> set;

        public RecoveredObjects(Y parent) {
            this.parent = parent;
            // normal = new HashMap<Class<?>,
            // PersistenceUtils.RecoveredObject>();
            set = new HashMap<Class<?>, HashMap<ID, RecoveredObject>>();
        }

        public Y getRecoveryRoot() {
            return parent;
        }

        public void add(Object obj) {
            RecoveredObject ro = new RecoveredObject(obj);
            if (obj instanceof Referenceable) {
                ID id = ((Referenceable) obj).getReference();
                HashMap<ID, RecoveredObject> map = set.get(ro.getType());
                if (map == null) {
                    map = new HashMap<ID, PersistenceUtils.RecoveredObject>();
                    set.put(ro.getType(), map);
                }
                map.put(id, ro);
            } else {
                // normal.put(ro.getType(), ro);
            }
        }

        public <V> V getObject(Class<V> type) throws PersistenceException {
            V result = parent.getObject(type);
            if (result != null) {
                return result;
            }

            //
            // RecoveredObject ro = normal.get(type);
            // if (ro == null) {
            // for (Class<?> t : normal.keySet()){
            // if (t.isAssignableFrom(type)) {
            // ro = normal.get(t);
            // break;
            // }
            // }
            // }
            // if (ro != null) {
            // return (V)ro.getObject();
            // }

            throw new PersistenceException("Could not locate an object of type " + type.getName());
        }

        public <V> V getObject(Class<V> type, ID id) throws PersistenceException {
            V result = (V) parent.getObject(type, id);
            if (result != null) {
                return result;
            }

            RecoveredObject ro = null;
            HashMap<ID, RecoveredObject> map = set.get(type);
            if (map != null) {
                ro = map.get(id);
            }

            if (ro == null) {
                for (Class<?> t : set.keySet()) {
                    if (type.isAssignableFrom(t)) {
                        map = set.get(t);
                        if (map != null) {
                            ro = map.get(id);
                        }
                        if (ro != null) {
                            break;
                        }
                    }
                }
            }
            if (ro != null) {
                return (V) ro.getObject();
            }

            throw new PersistenceException("Could not locate an object of type " + type.getName()
                    + " and id " + id + "(" + id.toHashString() + ")");
        }
    }
}
