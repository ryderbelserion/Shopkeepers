package com.nisovin.shopkeepers.api;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;

/**
 * Ensures that {@link ShopkeepersAPI} mirrors the methods of {@link ShopkeepersPlugin}.
 */
public class APIMirrorTest {

	/**
	 * Tests that {@link ShopkeepersAPI} mirrors the methods of {@link ShopkeepersPlugin}.
	 */
	@Test
	public void testMatchingMethods() {
		List<Method> pluginMethods = Arrays.stream(ShopkeepersPlugin.class.getDeclaredMethods())
				.filter(method -> Modifier.isAbstract(method.getModifiers()))
				.toList();
		for (Method pluginMethod : pluginMethods) {
			Method apiMethod = null;
			try {
				Class<?>[] parameters = pluginMethod.getParameterTypes();
				apiMethod = ShopkeepersAPI.class.getDeclaredMethod(
						pluginMethod.getName(),
						parameters
				);
			} catch (NoSuchMethodException e) {
			}

			assertNotNull(
					ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName()
							+ "' is missing!",
					Unsafe.nullableAsNonNull(apiMethod)
			);
			apiMethod = Unsafe.assertNonNull(apiMethod);
			assertTrue(
					ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName()
							+ "' is not static!",
					Modifier.isStatic(apiMethod.getModifiers())
			);
			assertEquals(
					ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName()
							+ "' mismatching return type!",
					apiMethod.getReturnType(),
					pluginMethod.getReturnType()
			);
			assertEquals(
					ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName()
							+ "' mismatching deprecation!",
					apiMethod.isAnnotationPresent(Deprecated.class),
					pluginMethod.isAnnotationPresent(Deprecated.class)
			);
			assertArrayEquals(
					ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName()
							+ "' mismatching exceptions!",
					apiMethod.getGenericExceptionTypes(),
					pluginMethod.getGenericExceptionTypes()
			);
		}
	}
}
