package com.nisovin.shopkeepers.util.bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.UnsafeValues;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;

public final class ServerUtils {

	private static final String MAPPINGS_VERSION;

	static {
		UnsafeValues unsafeValues = Bukkit.getUnsafe();
		Method getMappingsVersionMethod;
		try {
			getMappingsVersionMethod = unsafeValues.getClass().getDeclaredMethod("getMappingsVersion");
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(
					"Could not find method 'getMappingsVersion' in the UnsafeValues implementation!",
					e
			);
		}
		try {
			MAPPINGS_VERSION = Unsafe.cast(getMappingsVersionMethod.invoke(unsafeValues));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException("Could not retrieve the server's mappings version!", e);
		}
	}

	public static String getMappingsVersion() {
		return MAPPINGS_VERSION;
	}

	public static String getCraftBukkitVersion() {
		Package pkg = Unsafe.assertNonNull(Bukkit.getServer().getClass().getPackage());
		String packageName = pkg.getName();
		String cbVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
		return cbVersion;
	}

	private ServerUtils() {
	}
}
