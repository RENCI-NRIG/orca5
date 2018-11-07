package orca.util.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates types that implement their own recovery logic.
 * @author aydan
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Recover {
	/**
	 * Obtains the class responsible for recovering instances of the annotated type.
	 * The returned class must implement Recoverer&lt;T&gt;
	 * @return returns the responsible class
	 */
	Class<?> value();
}
