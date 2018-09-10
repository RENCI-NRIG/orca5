package net.exogeni.orca.util.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Indicates types that implement their own restore logic.
 * @author aydan
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Restore {
	/**
	 * Returns the class responsible for restoring saved instances of the annotated type.
	 * The class must implement Restorer&lt;T&gt;.
	 * @return returns the responsible class
	 */
	Class<?> value();
}
