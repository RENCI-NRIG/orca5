package orca.util.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a persistent field that is persisted explicitly by its owning object.
 * The owning object must implement CustomSaveable and CustomRestorable.
 * @author aydan
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomPersistent {
}