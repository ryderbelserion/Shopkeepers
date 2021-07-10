package com.nisovin.shopkeepers.api;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class APIMirrorTest {

	@Test
	public void testMatchingMethods() {
		List<Method> pluginMethods = Arrays.asList(ShopkeepersPlugin.class.getDeclaredMethods()).stream().filter((method) -> {
			return Modifier.isAbstract(method.getModifiers());
		}).collect(Collectors.toList());
		for (Method pluginMethod : pluginMethods) {
			Method apiMethod = null;
			try {
				apiMethod = ShopkeepersAPI.class.getDeclaredMethod(pluginMethod.getName(), pluginMethod.getParameterTypes());
			} catch (NoSuchMethodException e) {
			}
			Assert.assertNotNull(ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName() + "' missing!", apiMethod);
			Assert.assertTrue(ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName() + "' is not static!", Modifier.isStatic(apiMethod.getModifiers()));
			Assert.assertThat(ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName() + "' mismatching return type!",
					apiMethod.getReturnType(), Matchers.is((Object) pluginMethod.getReturnType()));
			Assert.assertThat(ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName() + "' mismatching deprecation!",
					apiMethod.isAnnotationPresent(Deprecated.class), Matchers.is(pluginMethod.isAnnotationPresent(Deprecated.class)));
			Assert.assertThat(ShopkeepersAPI.class.getName() + ": Method '" + pluginMethod.getName() + "' mismatching exceptions!",
					apiMethod.getGenericExceptionTypes(), Matchers.is(pluginMethod.getGenericExceptionTypes()));
		}
	}
}
