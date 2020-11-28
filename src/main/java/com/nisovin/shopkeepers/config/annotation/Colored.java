package com.nisovin.shopkeepers.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.nisovin.shopkeepers.config.value.types.ColoredStringListValue;
import com.nisovin.shopkeepers.config.value.types.ColoredStringValue;

/**
 * Shortcut for using {@link ColoredStringValue} or {@link ColoredStringListValue}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Colored {
}
