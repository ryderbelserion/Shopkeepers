package com.nisovin.shopkeepers.config.lib.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.nisovin.shopkeepers.config.lib.value.DefaultValueTypes;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.config.lib.value.ValueTypeProvider;

/**
 * Specifies a {@link ValueTypeProvider} to use for config fields.
 * <p>
 * This overrides the defaults specified by {@link DefaultValueTypes}.
 * <p>
 * The specified value type provider has to provide a no-args constructor.
 * <p>
 * Default value type providers specified by this annotation are inherited to subclasses. The first
 * provider (starting with those specified at the most specific class) that can provide a
 * {@link ValueType} is used.
 */
@Target(ElementType.TYPE)
@Repeatable(WithValueTypeProviders.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithValueTypeProvider {

	Class<? extends ValueTypeProvider> value();
}
