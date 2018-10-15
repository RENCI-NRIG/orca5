package net.exogeni.orca.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtils {
	private static final Class<?>[] EmptyClassArray = new Class<?>[0];
	private static final Object[] EmptyObjectArray = new Object[0];

	public static <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> annotationType) {
		T result = clazz.getAnnotation(annotationType);
	    if (result == null) {
	        Class<?> superclass = clazz.getSuperclass();
	        if (superclass != null) {
	            return getAnnotation(superclass, annotationType);
	        } else {
	            return null;
	        }
	    } else {
	        return result;
	    }
	}
	
	public static <E extends Enum<E>> E getEnum(Class<?> type, int ordinal) {
	    if (type == null) {
	    	throw new IllegalArgumentException("type");
	    }
	    final E[] enums = (E[])type.getEnumConstants();
	    if (ordinal >= enums.length){
	    	throw new IllegalArgumentException("ordinal");
	    }
	    return enums[ordinal];
	}

	
	public static <V> Class<V> getClass(String className, ClassLoader cl) throws ClassNotFoundException {
		return (Class<V>)Class.forName(className, true, cl);
	}

	public static <V> Class<V> getClass(String className) throws ClassNotFoundException {
		return (Class<V>)Class.forName(className);
	}

	public static List<Field> getAllFields(Class<?> type) {
		return getAllFields(new ArrayList<Field>(), type);
	}
	
	public static List<Field> getAllFields(List<Field> fields, Class<?> type) {
	    for (Field field: type.getDeclaredFields()) {
        	fields.add(field);
	    }

	    Class<?> superClass = type.getSuperclass();
	    if (superClass != null) {
	        fields = getAllFields(fields, superClass);
	    }

	    return fields;
	}

	
	public static boolean isInstanceOf(Object obj, String name) {
		try {
			Class<?> tc = getClass(name, obj.getClass().getClassLoader());
			return tc.isInstance(obj);
		} catch (ClassNotFoundException e) {
			return false;
		} catch (NoClassDefFoundError e) {
			return false;
		}
	}

	public static Object invokeStatic(String cname, String methodName, ClassLoader cl) {
		return invokeStatic(cname, methodName, cl, EmptyClassArray, EmptyObjectArray);
	}

	public static Object invokeStatic(String className, String methodName, ClassLoader cl, Class<?>[] paramTypes,
			Object... params) {
		try {
			Class<?> c = getClass(className, cl);
			Method m = c.getDeclaredMethod(methodName, paramTypes);
			return m.invoke(null, params);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static <V> V createInstance(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException {
		return createInstance(className, ReflectionUtils.class.getClassLoader());
	}

	public static <V> V createInstance(Class<V> c) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return createInstance(c, EmptyClassArray, EmptyObjectArray);
	}
	
	public static <V> V createInstance(String className, ClassLoader cl) throws InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
		Class<V> objectClass = (Class<V>)getClass(className, cl);
		
		return createInstance(objectClass, EmptyClassArray, EmptyObjectArray);
	}
		
	public static <V> V createInstance(String className, Class<?>[] paramTypes, Object... params) throws InstantiationException, IllegalAccessException,
	ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		Class<V> objectClass = (Class<V>)Class.forName(className);
		return createInstance(objectClass,  paramTypes,  params);
	}
	
	public static <V> V createInstance(Class<V> objectClass, Class<?>[] paramTypes, Object... params) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		Constructor<V> con = objectClass.getDeclaredConstructor(paramTypes);
		con.setAccessible(true);		
		return con.newInstance(params);
	}
	
	public static <V> Constructor<V> getDefaultConstructor(Class<V> objectClass) throws NoSuchMethodException, SecurityException {
		return objectClass.getDeclaredConstructor(EmptyClassArray);
	}
	
	public static boolean hasDefaultConstructor(Class<?> objectClass) {
		try {
			getDefaultConstructor(objectClass);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
