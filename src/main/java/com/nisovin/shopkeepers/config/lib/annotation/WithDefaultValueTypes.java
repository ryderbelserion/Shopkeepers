package com.nisovin.shopkeepers.config.lib.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple instances of {@link WithDefaultValueType}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface WithDefaultValueTypes {

	WithDefaultValueType[] value();
}
