package com.nisovin.shopkeepers.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

/**
 * Ensures that {@link ShopkeepersAPI} mirrors the methods of {@link ShopkeepersPlugin}.
 */
public class APIMirrorTest {

	/**
	 * Tests that {@link ShopkeepersAPI} mirrors the methods of {@link ShopkeepersPlugin}.
	 */
	@Test
	public void testMatchingMethods() {
		List<Method> pluginMethods = Arrays.stream(ShopkeepersPlugin.class.getDeclaredMethods()).filter(method -> {
			return Modifier.isAbstract(method.getModifiers());
		}).collect(Collectors.toList());
		for (Method pluginMethod : pluginMethods) {
			Method apiMethod = null;
			try {
				apiMethod = ShopkeepersAPI.class.getDeclaredMethod(pluginMethod.getName(), pluginMethod.getParameterTypes());
			} catch (NoSuchMethodException e) {
			}
			assertNotNull(ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName() + "' missing!", apiMethod);
			assertTrue(ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName() + "' is not static!", Modifier.isStatic(apiMethod.getModifiers()));
			assertEquals(ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName() + "' mismatching return type!",
					apiMethod.getReturnType(), pluginMethod.getReturnType());
			assertEquals(ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName() + "' mismatching deprecation!",
					apiMethod.isAnnotationPresent(Deprecated.class), pluginMethod.isAnnotationPresent(Deprecated.class));
			assertArrayEquals(ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName() + "' mismatching exceptions!",
					apiMethod.getGenericExceptionTypes(), pluginMethod.getGenericExceptionTypes());
		}
	}
}
