package com.nisovin.shopkeepers.config.lib.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.nisovin.shopkeepers.config.lib.value.types.StringListValue;
import com.nisovin.shopkeepers.config.lib.value.types.StringValue;

/**
 * Shortcut for using {@link StringValue} or {@link StringListValue}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Uncolored {
}
