package com.nisovin.shopkeepers.util.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a reference marked with this annotation is not used to modify the state of the
 * referenced object.
 * <p>
 * If this annotation is used on a container type, such as a collection or an array, its meaning
 * applies to the container itself, but not necessarily to its contained elements: The container
 * itself will not be modified, but no statement is made on whether the states of the contained
 * elements are modified.
 * <p>
 * See also {@link ReadWrite}, which can be used to explicitly indicate that the referenced object
 * may be modified.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE_PARAMETER, ElementType.TYPE_USE })
public @interface ReadOnly {
}
