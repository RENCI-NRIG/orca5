package orca.util.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a class that implements custom save logic.
 * @author aydan
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Save {
	/**
	 * Returns the class responsible for saving intances of the annotated type.
	 * The class must implement Saver<T>.
	 * @return
	 */
	Class<?> value();
}