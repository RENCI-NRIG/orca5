package net.exogeni.orca.util.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.Map.Entry;

import net.exogeni.orca.security.AbacUtil;
import net.exogeni.orca.util.Base64;
import net.exogeni.orca.util.ID;
import net.exogeni.orca.util.OrcaException;
import net.exogeni.orca.util.PropList;
import net.exogeni.orca.util.ReflectionUtils;
import net.exogeni.orca.util.ResourceType;
import net.exogeni.orca.util.Serializer;
import net.exogeni.orca.util.persistence.PersistenceUtils.RecoveredObjects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PersistenceState {
	class FieldState {
		private Field f;
		private String key;
		private Persistent p;
		private String name;
		
		public FieldState(Field f) {
			if (f == null) {
				throw new IllegalArgumentException("f");
			}
			
			if (!f.isAnnotationPresent(Persistent.class)){
				throw new IllegalArgumentException("Not @Persistent");
			}

			this.f = f;
			name = type.getName() + "." + f.getName();
			p = f.getAnnotation(Persistent.class);
			if (p.key().isEmpty()) {
				key = name;
			}else {
				key = p.key();
			}
		}
				
		public String getKey() {
			return key;
		}
				
		public String getName() {
			return name;
		}
		
		public boolean isMergeField() {
			return p.merge();
		}
		
		public boolean isReferenceField() {
			return p.reference();
		}	
		
		public boolean shouldRestore() {
			return p.restore();
		}
		
		public Object get(Object instance) throws IllegalAccessException{
			return f.get(instance);
		}
		
		public void set(Object instance, Object value) throws IllegalAccessException {
			f.set(instance, value);
		}
		
		public Class<?> getType() {
			return f.getType();
		}
	};

    private static final Logger logger = LoggerFactory.getLogger("net.exogeni.orca.util.persistence");

	// Fields that need to be persisted
	private List<FieldState> fields = new ArrayList<FieldState>();
	// the persisted type
	private Class<?> type;
	private FieldState mergeField;

	public PersistenceState(Class<?> type) throws PersistenceException {
		this.type = type;
		
		List<Field> fields = ReflectionUtils.getAllFields(type);
		for (Field f : fields) {
			// filter out properties we do not need:
			if (f.getName().startsWith("$") || 
				Modifier.isStatic(f.getModifiers()) ||
				Modifier.isFinal(f.getModifiers())) {
				continue;
			}	
			if (f.isAnnotationPresent(Persistent.class)) {
				addPersistentField(f);
			} else if (f.isAnnotationPresent(CustomPersistent.class)){
				// @CustomPersistent fields require that the class provide save and restore methods
				if (!CustomRestorable.class.isAssignableFrom(type)) {
					throw new PersistenceException("Field " + f.getName() + 
							" of class " + type.getName() + 
							" is @CustomPersistent but the class does not implement CustomRestorable");
				}
				if (!CustomSaveable.class.isAssignableFrom(type)) {
					throw new PersistenceException("Field " + f.getName() + 
							" of class " + type.getName() + 
							" is @CustomPersistent but the class does not implement CustomSaveable");
				}					
			} else if 
					(!f.isAnnotationPresent(NotPersistent.class) && 	// needs no persistence
					!f.isAnnotationPresent(CustomPersistent.class) &&	// FIXME: do we still need this?
					(!type.isAnnotationPresent(Save.class) &&			// have a save-r class and either a restorer/recoverer) 
							(!type.isAnnotationPresent(Restore.class) || !type.isAnnotationPresent(Recover.class))
					)){
				throw new PersistenceException("Do not know how to handle field: " + f.getName() + " of class " + type.getName());
			}
		}			
	}
	
	private void addPersistentField(Field f) throws PersistenceException {
		FieldState fs = new FieldState(f);
		
		validate(fs);
		if (fs.isMergeField()){
			if (mergeField != null) {
				throw new PersistenceException("Cannot register field + " + fs.getName() + 
						": there is already a merge field for type " + type.getName() + " :" + mergeField.getName());
			}
			mergeField = fs;
		}
		fields.add(fs);			
		f.setAccessible(true);
	}
			
	public <V extends Persistable> Properties save(V toPersist) throws PersistenceException {
		if (toPersist == null) {
			throw new IllegalArgumentException("toPersist");
		}
		Properties saved = new Properties();
		// first save the class name
		saved.setProperty(PersistenceProperties.PropertyClassName, toPersist.getClass().getName());

		for (FieldState f : fields) {
			try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Persisting field " + f.getName() + " key=" + f.getKey());
                }
                Object value = f.get(toPersist);
                // this detects only the simplest kind of self-references /ib
                if (toPersist == value) {
                	throw new PersistenceException(" Cannot persist field: " + f.getName() + " due to immediate recursion");
                }
				persistObject(value, saved, f.getKey(), f.isMergeField(), f.isReferenceField());
			} catch (IllegalAccessException e){
				throw new PersistenceException("Cannot access field: " + f.getName() + " " + e , e);
			} catch (PersistenceException e) {
				throw new PersistenceException("Cannot persist field: " + f.getName() + " " + e, e);
			} catch (Exception e){
				throw new PersistenceException("Cannot persist field: " + f.getName() + " " + e, e);
			}
		}
		
		// if the class provides custom save logic, invoke its save method
		if (CustomSaveable.class.isAssignableFrom(type)){
			((CustomSaveable)toPersist).save(saved);
		}
		return saved;
	}
	
	public <V extends Persistable> void restore(V toRestore, Properties savedState) throws PersistenceException {
		if (toRestore == null) {
			throw new IllegalArgumentException("toRestore");
		}
		if (savedState == null) {
			throw new IllegalArgumentException("savedState");
		}
		
		Properties pp = savedState;
		if (mergeField != null) {
			pp = (Properties)pp.clone();
		}
		
		try {	
		for (FieldState f : fields) {
			if (!f.isMergeField()) {
				if (f.shouldRestore()){
                    if (logger.isDebugEnabled()) {
                        logger.debug("Restoring field " + f.getName() + " key=" + f.getKey());
                    }
					Class<?> t = f.getType();
					String savedValue = pp.getProperty(f.getKey());
                    if (savedValue != null) {
                        if (logger.isDebugEnabled()) {
                        	Formatter fmt = new Formatter();
                            // Display at most 100 characters in a string.
                            fmt.format("%.100s", savedValue);
                            logger.debug(" Saved value=" + fmt + " ...");
                            fmt.close();
                        }
                        // if the object also specified a type for us to use, then use that
						// instead of the field type.
						String savedClass = pp.getProperty(f.getKey() + "." + PersistenceUtils.PropertyClassName);
						if (savedClass != null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(" Saved class=" + savedClass);
                            }
							t = ReflectionUtils.getClass(savedClass);
						}else {
                            if (logger.isDebugEnabled()) {
                                logger.debug(" Field class=" + t.getName());
                            }
                        }
						f.set(toRestore, restoreObject(t, savedValue));
					}
				}
				// remove the property since we have a merge field
				if (mergeField != null) {
					pp.remove(f.getKey());
					pp.remove(f.getKey() + "." + PersistenceUtils.PropertyClassName);
				}
			}
		}
		
		if (mergeField != null) {
			Class<?> t = mergeField.getType();
			pp.remove(PersistenceProperties.PropertyClassName);
			mergeField.set(toRestore, restoreObject(t, PropList.toString(pp)));
		}
		
		} catch (PersistenceException e) {
			throw e;
		} catch (Exception e) {
			throw new PersistenceException("Cannot restore object of type " + toRestore.getClass().getName(), e);
		}

		// if the class requires custom restore logic, invoke its restore method
		if (CustomRestorable.class.isAssignableFrom(type)) {
			((CustomRestorable)toRestore).restore(savedState);
		}
	}

	private  <V extends Recoverable, Y extends RecoverParent> void recoverField(FieldState f, V obj, RecoveredObjects ros, Properties pp) throws Exception, IllegalAccessException, OrcaException {		
		// type of the field
		Class<?> t = f.getType();
		// saved value of the field
		String savedValue = pp.getProperty(f.getKey());
		// saved reference value of the field
		String savedReferenceValue = pp.getProperty(f.getKey() + "-reference");
		// we've reset the field earlier, so attempt to get the value of the field
		Object currentValue = f.get(obj);

		if (logger.isDebugEnabled()){
			logger.debug("Recovering field " + f.getName() + " with old value " + currentValue + " and new value " + savedValue + " and reference " + savedReferenceValue);
		}

		if (f.isReferenceField()) {
			// FIXME: suppress this check for now, sice we end up having problems with some objects
			// that are set during initialization, e.g., logger			
//			if (currentValue != null) {
//				throw new IllegalStateException("Reference field has value before recovery: " + f.getName());
//			}
			if (currentValue == null) {
				if (Referenceable.class.isAssignableFrom(t)){
					// do we have the id?
					if (savedReferenceValue != null) {
						ID referenceKey = (ID)restoreObject(Serializer.toProperties(savedReferenceValue));
						if (referenceKey == null) {
							throw new IllegalArgumentException("Reference key cannot be null");
						}
						
						// FIXME: what happens when ro is null!
						Object ro = ros.getObject(t, referenceKey);
						f.set(obj, ro);
					}
				}else {
					// no need for id: get the object from the parent
					f.set(obj, ros.getObject(t));				
				}
			}
		}else {
			// not a reference:
			//  - is this a type with a custom recoverer?
			if (t.isAnnotationPresent(Recover.class)){
				// Fields of types with @Recover attribute should be created by the recover class!
				if (currentValue != null) {
					throw new PersistenceException("Field " + f.getName() + " has type with @Recover, however the field already has a value");
				}
				
				// if the class specifies a custom recoverer, then use it
				Recover recover = t.getAnnotation(Recover.class);
				Recoverer recoverer = (Recoverer)ReflectionUtils.createInstance(recover.value());
				f.set(obj, recoverer.recover(ros, savedValue));
			}else {
				// is this an object that itself needs to be recovered?
				if (currentValue != null) {
					if (Recoverable.class.isAssignableFrom(currentValue.getClass())){
						PersistenceUtils.recover((Recoverable)currentValue, ros, Serializer.toProperties(savedValue));
					}				
				}
			}
		}
	}
	
	public <V extends Recoverable, Y extends RecoverParent> void recover(V obj, RecoveredObjects<Y> ros, Properties savedState) throws OrcaException {
		if (obj == null) {
			throw new IllegalArgumentException("obj");
		}
		if (ros == null) {
			throw new IllegalArgumentException("ros");
		}
		if (savedState == null) {
			throw new IllegalArgumentException("savedState");
		}
		
		Properties pp = savedState;
		if (mergeField != null) {
			pp = (Properties)pp.clone();
		}
		
		try {	
			for (FieldState f : fields) {
				if (!f.isMergeField()) {
					try {
						recoverField(f, obj, ros, pp);
					} catch (Exception e) {
						throw new PersistenceException("Cannot recover field: " + f.getName(), e);
					}
					// remove the property since we have a merge field
					if (mergeField != null) {
						pp.remove(f.getKey());
					}
				}
			}
		
			if (mergeField != null) {
				try {
					recoverField(mergeField, obj, ros, pp);
				} catch (Exception e) {
					throw new PersistenceException("Cannot recover field: " + mergeField.getName());
				}
			}
			
		} catch (Exception e) {
			throw new PersistenceException("Cannot restore object of type " + obj.getClass().getName(), e);
		}

		// if the class requires custom recover logic, invoke its recover method
		if (CustomRecoverable.class.isAssignableFrom(type)) {
			((CustomRecoverable)obj).recover(ros, savedState);
		}	
	}
	
	public static Properties persistObject(Object value) 
			throws PersistenceException, NoSuchMethodException, 
			InvocationTargetException, InstantiationException, IllegalAccessException {
		if (value == null) {
			throw new IllegalArgumentException("value cannot be null");
		}
		// we represent the value by a properties list
		Properties props = new Properties();
		// class name of the value object
		props.setProperty(PersistenceProperties.PropertyClassName, value.getClass().getName());
		// value of the value object
		persistObject(value, props, "value", false, false);
		return props;
	}

    public static void setProperty(Properties p, String key, String value) {
        //if (logger.isDebugEnabled()) {
        //    logger.debug(key + "=" + value);
        //}
        p.setProperty(key, value);
    }

	public static void persistObject(Object value, Properties saved, String key, boolean merge, boolean reference) 
			throws PersistenceException, NoSuchMethodException, 
			InvocationTargetException, IllegalAccessException, InstantiationException  {		


        if (value == null){
            if (logger.isDebugEnabled()) {
                logger.debug("persistObject(): Key " + key + " has no value");
            }
			return;
		}
		
		try {			
			// first handle reference fields
			if (reference) {				
				if ((value instanceof Referenceable)){
					ID id = ((Referenceable)value).getReference();
					Properties pid = persistObject(id);
					PropList.setProperty(saved, key + "-reference", pid);
					if (logger.isDebugEnabled()){
						logger.debug("persistObject(): Key " + key + " saved as reference");
						if (logger.isTraceEnabled()){
							logger.trace("Value " + value + " Properties " + Arrays.toString(pid.entrySet().toArray()));
						}
					}
				}else {
					// nothing to do
					if (logger.isInfoEnabled()){
						logger.info("persistObject(): Key " + key + " is reference, but not referenceable");
					}
				}
				return;
			}

			// if the class provides a custom saver, then use the saver 
			if (value.getClass().isAnnotationPresent(Save.class)){
				Save save = value.getClass().getAnnotation(Save.class);
				Saver saver = (Saver)ReflectionUtils.createInstance(save.value());
				PropList.setProperty(saved, key, saver.save(value));
				if (logger.isDebugEnabled()){
					logger.debug("persistObject(): Key " + key + " saved using custom saver " + save.value());
				}
				return;
			}

			if (logger.isDebugEnabled()){
				logger.debug("persistObject(): Key " + key + " to be saved based on type of object " + value.getClass().getSimpleName());
			}
			
			// handle based on the type of the object
			if (value instanceof Persistable) {
				PropList.setProperty(saved, key, PersistenceUtils.save((Persistable)value));
			}else if (value instanceof X509Certificate) {
				PropList.setProperty(saved,  key, AbacUtil.getPemEncodedCert((X509Certificate)value));
			}else if (value instanceof ID) {
                String v = ((ID)value).toString();
                String cn = value.getClass().getName();
                setProperty(saved, key, v);
				setProperty(saved, key + "." + PersistenceUtils.PropertyClassName, cn);
			}else if (value instanceof ResourceType) {
                String v = ((ResourceType)value).toString();
                String cn = value.getClass().getName();
                setProperty(saved, key, v);
                setProperty(saved, key + "." + PersistenceUtils.PropertyClassName, cn);
   			}else if (value instanceof String){
				setProperty(saved, key, ((String) value).toString());
				if (value.getClass() != String.class) {
					setProperty(saved, key + "." + PersistenceUtils.PropertyClassName, value.getClass().getName());
				}
			}else if (value instanceof Integer) {
				PropList.setProperty(saved, key, ((Integer)value));
				if (value.getClass() != Integer.class) {
					saved.setProperty(key + "." + PersistenceUtils.PropertyClassName, value.getClass().getName());					
				}				
			}else if (value instanceof Long){
				PropList.setProperty(saved, key, ((Long)value));
				if (value.getClass() != Long.class) {
					saved.setProperty(key + "." + PersistenceUtils.PropertyClassName, value.getClass().getName());					
				}				
			}else if (value instanceof Boolean){
				PropList.setProperty(saved, key, ((Boolean)value));
				if (value.getClass() != Boolean.class) {
					saved.setProperty(key + "." + PersistenceUtils.PropertyClassName, value.getClass().getName());					
				}				
			} else if (value instanceof Float) {
				PropList.setProperty(saved, key, ((Float)value));
				if (value.getClass() != Float.class) {
					saved.setProperty(key + "." + PersistenceUtils.PropertyClassName, value.getClass().getName());	
				}
			} else if (value.getClass().isEnum()){
				PropList.setProperty(saved,  key,  ((Enum<?>)value).ordinal());
			}else if (value instanceof Properties) {
				Properties temp = (Properties)value;
				if (merge){
					PropList.mergeProperties(temp, saved);
				}else {
					PropList.setProperty(saved, key, temp);
				}
				if (value.getClass() != Properties.class) {
					saved.setProperty(key + "." + PersistenceUtils.PropertyClassName, value.getClass().getName());					
				}				
			}else if (value instanceof HashSet<?>) {
				HashSet<?> set = (HashSet<?>)value;
				Properties inner = new Properties();
				inner.setProperty(PersistenceProperties.PropertyClassName, value.getClass().getName());
				PropList.setProperty(inner, "count", set.size());
				int count = 0;
				for (Object o : set){
					Properties oo = persistObject(o);
					PropList.setProperty(inner, "entry." + count,  oo);
					count++;
				}
				PropList.setProperty(saved, key, inner);
				if (value.getClass() != HashSet.class) {
					saved.setProperty(key + "." + PersistenceUtils.PropertyClassName, value.getClass().getName());					
				}				
			}else if (value instanceof HashMap<?, ?>){
				HashMap<?,?> temp = (HashMap<?,?>)value;
				Properties inner = new Properties();
				inner.setProperty(PersistenceProperties.PropertyClassName, value.getClass().getName());
				PropList.setProperty(inner, "count", temp.size());
				int count = 0;
				for (Map.Entry<?, ?> entry : temp.entrySet()) {

					Properties k = persistObject(entry.getKey());
					Properties v = persistObject(entry.getValue());
					Properties kv = new Properties();
					PropList.setProperty(kv, "key", k);
					PropList.setProperty(kv, "value", v);
					
					PropList.setProperty(inner, "entry." + count, kv); 
					count++;
				}
				PropList.setProperty(saved, key, inner);
				if (value.getClass() != HashMap.class) {
					saved.setProperty(key + "." + PersistenceUtils.PropertyClassName, value.getClass().getName());					
				}	
			} else if (value instanceof BitSet) {
				Properties inner = new Properties();
				
				try {
					inner.setProperty(PersistenceProperties.PropertyClassName, value.getClass().getName());
					inner.setProperty("bitset", serializeBitset((BitSet) value));
				} catch (IOException ioe) {
					throw new PersistenceException("Unable to save BitSet: " + ioe);
				}
				
				PropList.setProperty(saved, key, inner);
			} else if (value instanceof LinkedList) {
				Properties inner = new Properties();
				LinkedList<?> ll = (LinkedList<?>)value;
				PropList.setProperty(inner, "count", ll.size());
				for(int i = 0; i < ll.size(); i++ ) {
					Object current = ll.get(i);
					PropList.setProperty(inner, "entry." + i, persistObject(current));
				}
				PropList.setProperty(saved, key, inner);
			} else if (value.getClass().isArray()){
				Properties inner = new Properties();
				inner.setProperty(PersistenceProperties.PropertyClassName, value.getClass().getComponentType().getName());
				int length = Array.getLength(value);
				PropList.setProperty(inner, "count", Array.getLength(value));
				for (int i = 0; i < length; i++) {
					Object current = Array.get(value,i);
					if (current != null) {
						Properties item = persistObject(current);
						PropList.setProperty(inner, "entry." + i, item);
					}
				}
				PropList.setProperty(saved, key, inner);	
			} else if (value instanceof Date) {
				Date d = ((Date)value);
				PropList.setProperty(saved, key, d.getTime());
			}else if (value instanceof Saveable) {
				String temp = ((Saveable)value).save();
				PropList.setProperty(saved, key, temp);					
			}else {
				throw new PersistenceException("Unsupported value type " + value.getClass().getName());
			}
		} catch (CertificateEncodingException e){
			throw new PersistenceException("Cannot serialize certificate", e);
		}
	}

	public <V extends Persistable> void validateRestore(V original, V restored) throws PersistenceException {
		if (logger.isDebugEnabled()){
			logger.debug("Validating restore of type " + original.getClass().getSimpleName() + " original: " + original + " restored: " + restored);
		}
		if (original == null) {
			throw new IllegalArgumentException("original");
		}
		if (restored == null) {
			throw new IllegalArgumentException("restored");
		}
		
		if (original.getClass() != this.type) {
			throw new IllegalArgumentException("Invalid type for orginial: " + original.getClass().getName());
		}
		
		if (original.getClass() != restored.getClass()){
			throw new PersistenceException("original and restored have non-matching types");
		}
		
		for (FieldState f : fields) {
			try {
				if (f.isReferenceField()){
					if (logger.isDebugEnabled()) {
						logger.debug("Skipping " + f.getName() + " : reference field");
					}
				}else {
					if (logger.isDebugEnabled()) {
						logger.debug("Comparing " + f.getName());
					}
					Object originalValue = f.get(original);
					Object restoredValue = f.get(restored);
					validateObject(originalValue, restoredValue);
				}
			} catch (IllegalAccessException e){
				throw new PersistenceException("Cannot validate field: " + f.getName(), e);
			} catch (PersistenceException e) {
				throw new PersistenceException("Cannot validate field: " + f.getName(), e);
			} catch (Exception e){
				throw new PersistenceException("Cannot validate field: " + f.getName(), e);
			}
		}
	}
	
	public static void validateObject(Object original, Object restored) throws PersistenceException {
		if (original == null && restored == null){
			return;
		}
		
		if (original == null && restored != null) {
			throw new PersistenceException("Original is null but restored is not");
		}
		
		if (original != null && restored == null) {
			// if the class has a custom restorer, give it the benefit of a doubt and let it pass (for now)
			if (!original.getClass().isAnnotationPresent(Recover.class)){
				throw new PersistenceException("Original is not null but restored is null");
			}else {
				return;
			}
		}
		
		// both are non-null
		
		if (original.getClass() != restored.getClass()) {
			throw new PersistenceException("Types do not match: orig=" + original.getClass().getName() + 
					" restored=" + restored.getClass().getName());
		}

		// classes match
		
		if (original instanceof Persistable) {
			PersistenceUtils.validateRestore((Persistable)original, (Persistable)restored);			
		}else if (original instanceof HashMap<?,?>) {
			HashMap omap = (HashMap)original;
			HashMap rmap = (HashMap)restored;
			if (omap.size() != rmap.size()) {
				throw new PersistenceException("HashMap sizes do not match");
			}
			
			Iterator<Entry<?,?>> i = omap.entrySet().iterator();
            while (i.hasNext()) {
                Entry<?,?> e = i.next();
                Object key = e.getKey();
                Object ovalue = e.getValue();
	 
                Object rvalue = rmap.get(key);
                validateObject(ovalue, rvalue);
            }
		}else {
			if (!original.equals(restored)) {
				throw new PersistenceException("Values do not match orig=" + original +
						" restored=" + restored);
			}
		}		
	}
	
	public static Object restoreObject(Class<?> t, String saved) throws PersistenceException {
		try {
			if (t.isAnnotationPresent(Restore.class)){
				// if the class specifies a custom restorer, then use it
				Restore restore = t.getAnnotation(Restore.class);
				Restorer restorer = (Restorer)ReflectionUtils.createInstance(restore.value());
				return restorer.restore(saved);
			}else if (Persistable.class.isAssignableFrom(t)){
				return PersistenceUtils.restore(Serializer.toProperties(saved));
			}else if (X509Certificate.class.isAssignableFrom(t)) {
				return AbacUtil.getCertificate(saved);
			}else if (ID.class.isAssignableFrom(t) || String.class.isAssignableFrom(t)) {
				return ReflectionUtils.createInstance(t, new Class<?>[]{String.class}, saved);
            }else if (ResourceType.class.isAssignableFrom(t) || String.class.isAssignableFrom(t)) {
                return ReflectionUtils.createInstance(t, new Class<?>[]{String.class}, saved);
			}else if (Integer.class.isAssignableFrom(t) || int.class.isAssignableFrom(t)) {
				return ReflectionUtils.createInstance(Integer.class, new Class<?>[]{int.class}, Integer.parseInt(saved));
			}else if (Long.class.isAssignableFrom(t) || long.class.isAssignableFrom(t)) {
				return ReflectionUtils.createInstance(Long.class, new Class<?>[]{long.class}, Long.parseLong(saved));
			} else if (Float.class.isAssignableFrom(t) || float.class.isAssignableFrom(t)) {
				return ReflectionUtils.createInstance(Float.class, new Class<?>[]{float.class}, Float.parseFloat(saved));
			}else if (Boolean.class.isAssignableFrom(t) || boolean.class.isAssignableFrom(t)) {
				return ReflectionUtils.createInstance(Boolean.class, new Class<?>[]{boolean.class}, Boolean.parseBoolean(saved));
			}else if (Properties.class.isAssignableFrom(t)){
				return Serializer.toProperties(saved);
			}else if (t.isEnum()) {
				// FIXME: not sure!!!
				return ReflectionUtils.getEnum(t, Integer.parseInt(saved));
			}else if (HashSet.class.isAssignableFrom(t)){
				HashSet set = (HashSet)ReflectionUtils.createInstance(t);
				Properties pp = Serializer.toProperties(saved);
				int count = PropList.getIntegerProperty(pp, "count");
				for (int i = 0; i < count; i++){
					Properties oo = PropList.getPropertiesProperty(pp, "entry." + i);
					Object o = restoreObject(oo);
					set.add(o);
				}
				return set;
			}else if (HashMap.class.isAssignableFrom(t)){
				HashMap map = (HashMap)ReflectionUtils.createInstance(t);
				Properties pp = Serializer.toProperties(saved);
				int count = PropList.getIntegerProperty(pp, "count");
				for (int i = 0; i < count; i++){
					Properties entry = PropList.getPropertiesProperty(pp, "entry." + i);
					if (entry == null){
						throw new PersistenceException("Cannot restore HashMap: missing entry");
					}
					Properties k = PropList.getPropertiesProperty(entry, "key");
					Properties v = PropList.getPropertiesProperty(entry, "value");
					if (k == null || v == null) {
						throw new PersistenceException("Cannot restore HashMap: invalid entry");
					}
					Object key = restoreObject(k);
					Object val = restoreObject(v);
					map.put(key, val);
				}
				return map;
			} else if (BitSet.class.isAssignableFrom(t)) {
				Properties pp = Serializer.toProperties(saved);
				try {
					return deserializeBitset(pp.getProperty("bitset"));
				} catch (IOException ioe) {
					throw new PersistenceException("Cannot restore bitset: " + ioe);
				}
			} else if (LinkedList.class.isAssignableFrom(t)) {
				LinkedList ll = (LinkedList)ReflectionUtils.createInstance(t);
				Properties pp = Serializer.toProperties(saved);
				int count = PropList.getIntegerProperty(pp, "count");
				for(int i = 0; i < count; i++) {
					Properties entryProperties = PropList.getPropertiesProperty(pp, "entry." + i);
					if (entryProperties != null){
						Object entry = restoreObject(entryProperties);
						ll.add(entry);
					}
				}
				return ll;
			} else if (t.isArray()){
				Properties pp = Serializer.toProperties(saved);
				int count = PropList.getIntegerProperty(pp, "count");
				Object arr = Array.newInstance(t, count);
				for (int i = 0; i < count; i++){
					Properties entryProperties = PropList.getPropertiesProperty(pp, "entry." + i);
					if (entryProperties != null){
						Object entry = restoreObject(entryProperties);
						Array.set(arr, i, entry);
					}
				}
				return arr;
			} else if (Date.class.isAssignableFrom(t)) {
				return new Date(Long.parseLong(saved));
			} else if (Restorable.class.isAssignableFrom(t)) {
				Restorable obj = ReflectionUtils.createInstance((Class<Restorable>)t);
				obj.restore(saved);
				return obj;
			}else {
				throw new PersistenceException("Do not know to handle type: " + t.getName());
			}
		} catch (PersistenceException e){
			throw e;
		} catch (Exception e){
			throw new PersistenceException("Cannot restore value (cn=" + t.getName() + ")", e);
		}		
	}
	
	public static Object restoreObject(Properties saved) throws PersistenceException, ClassNotFoundException {
		if (saved == null) {
			throw new IllegalArgumentException("saved");
		}
		String cn = saved.getProperty(PersistenceProperties.PropertyClassName);
		
		if (cn == null){
			throw new PersistenceException("Cannot restore: missing class name saved=" + saved.toString());
		}
		
		String savedValue = saved.getProperty("value");
		if (savedValue == null){
			return null;
		}
		
		Class<?> t = ReflectionUtils.getClass(cn); 
		return restoreObject(t, savedValue);
	}

	
	// ensures that we can handle the type
	private static void validate(FieldState fs) throws PersistenceException {
		// reference fields do not need a type check
		if (fs.isReferenceField()) {
			return;
		}
		
		Class<?> t = fs.getType();
		if (!(
				Persistable.class.isAssignableFrom(t) ||
				(Saveable.class.isAssignableFrom(t) && Restorable.class.isAssignableFrom(t)) || 
				X509Certificate.class.isAssignableFrom(t) ||
				ID.class.isAssignableFrom(t) ||
				ResourceType.class.isAssignableFrom(t) ||
				String.class.isAssignableFrom(t) ||
				Integer.class.isAssignableFrom(t) ||
				int.class.isAssignableFrom(t) ||
				Long.class.isAssignableFrom(t) ||
				long.class.isAssignableFrom(t) ||
				Float.class.isAssignableFrom(t) ||
				float.class.isAssignableFrom(t) ||
				Boolean.class.isAssignableFrom(t) ||
				boolean.class.isAssignableFrom(t) ||
				HashMap.class.isAssignableFrom(t) || 
				HashSet.class.isAssignableFrom(t) ||
				Properties.class.isAssignableFrom(t) ||
				t.isEnum() ||
				Date.class.isAssignableFrom(t) ||
				LinkedList.class.isAssignableFrom(t) ||
				BitSet.class.isAssignableFrom(t)
			)) {
			throw new PersistenceException("Do not know how to handle objects of type " + t.getName());
		}
	}
	
	private static String serializeBitset(BitSet bs) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		
		oos.writeObject(bs);
		
		oos.close();
		baos.close();
		
		return Base64.encodeBytes(baos.toByteArray());
	}
	
	private static BitSet deserializeBitset(String s) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decode(s));
		ObjectInputStream ois = new ObjectInputStream(bais);
		
		BitSet ret = null;
		try {
			ret = (BitSet)ois.readObject();
		} catch (ClassNotFoundException ce) {
			;
		}
		
		ois.close();
		bais.close();
		
		return ret;
	}
}

