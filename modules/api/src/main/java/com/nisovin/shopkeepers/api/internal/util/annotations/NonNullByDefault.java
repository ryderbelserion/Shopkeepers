package com.nisovin.shopkeepers.api.internal.util.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Marks a package, type, or method as having all fields, parameters, and return types as
 * {@link NonNull} by default.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD })
public @interface NonNullByDefault {
}
