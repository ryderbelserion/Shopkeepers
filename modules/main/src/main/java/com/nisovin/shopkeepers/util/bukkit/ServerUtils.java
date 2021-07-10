package com.nisovin.shopkeepers.util.bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.UnsafeValues;

public final class ServerUtils {

	private ServerUtils() {
	}

	private static final String MAPPINGS_VERSION;

	static {
		UnsafeValues unsafeValues = Bukkit.getUnsafe();
		Method getMappingsVersionMethod;
		try {
			getMappingsVersionMethod = unsafeValues.getClass().getDeclaredMethod("getMappingsVersion");
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Could not find method 'getMappingsVersion' in the UnsafeValues implementation!", e);
		}
		try {
			MAPPINGS_VERSION = (String) getMappingsVersionMethod.invoke(unsafeValues);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Could not retrieve the server's mappings version!", e);
		}
	}

	public static String getMappingsVersion() {
		return MAPPINGS_VERSION;
	}

	public static String getCraftBukkitVersion() {
		String packageName = Bukkit.getServer().getClass().getPackage().getName();
		String cbVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
		return cbVersion;
	}
}
