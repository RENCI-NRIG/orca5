package orca.util.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a persistent field.
 * @author aydan
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Persistent {
	/**
	 * Specifies the key under which the field should be persisted.
	 * When the key is "", the engine will generate a key using the containing type's fully
	 * qualified name followed by '.' and the name of the field.
	 * @return
	 */
	String key() default "";
	/**
	 * Indicate that the field is a merge field.
	 * @return
	 */
	boolean merge() default false;
	/**
	 * Indicates that the field is a reference.
	 * @return
	 */
	boolean reference() default false;
	/**
	 * Indicates whether the engine should restore the field.
	 * Note: some fields cannot be restored in the restore phase
	 * and their creation needs to be deferred to the recover phase. 
	 * @return
	 */
	boolean restore() default true;
	
	String table() default "";
	String column() default "";
	
}