package com.nisovin.shopkeepers.util.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ClassUtils {

	private ClassUtils() {
	}

	private static final String CLASS_FILE_EXTENSION = ".class";

	private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS;
	static {
		Map<Class<?>, Class<?>> primitiveWrappers = new HashMap<>();
		primitiveWrappers.put(boolean.class, Boolean.class);
		primitiveWrappers.put(byte.class, Byte.class);
		primitiveWrappers.put(char.class, Character.class);
		primitiveWrappers.put(double.class, Double.class);
		primitiveWrappers.put(float.class, Float.class);
		primitiveWrappers.put(int.class, Integer.class);
		primitiveWrappers.put(long.class, Long.class);
		primitiveWrappers.put(short.class, Short.class);
		PRIMITIVE_WRAPPERS = Collections.unmodifiableMap(primitiveWrappers);
	}

	public static boolean isPrimitiveWrapperOf(Class<?> targetClass, Class<?> primitive) {
		Validate.isTrue(primitive.isPrimitive(), "Second argument has to be a primitive!");
		return (PRIMITIVE_WRAPPERS.get(primitive) == targetClass);
	}

	public static boolean isAssignableFrom(Class<?> to, Class<?> from) {
		if (to.isAssignableFrom(from)) {
			return true;
		}
		if (to.isPrimitive()) {
			return isPrimitiveWrapperOf(from, to);
		}
		if (from.isPrimitive()) {
			return isPrimitiveWrapperOf(to, from);
		}
		return false;
	}

	/**
	 * Loads all classes from the given jar file.
	 * 
	 * @param jarFile
	 *            the jar file
	 * @param filter
	 *            only classes whose names are accepted by this filter are loaded
	 * @param logger
	 *            the logger that is used to log warnings when classes cannot be loaded
	 * @return <code>true</code> on (potentially partial) success and <code>false</code> on failure
	 */
	public static boolean loadAllClassesFromJar(File jarFile, Predicate<String> filter, Logger logger) {
		Validate.notNull(jarFile, "jarFile is null");
		filter = PredicateUtils.orAlwaysTrue(filter);

		try (ZipInputStream jar = new ZipInputStream(new FileInputStream(jarFile))) {
			for (ZipEntry entry = jar.getNextEntry(); entry != null; entry = jar.getNextEntry()) {
				if (entry.isDirectory()) continue;
				String entryName = entry.getName();
				if (!entryName.endsWith(CLASS_FILE_EXTENSION)) continue;

				// Check filter:
				String className = entryName.substring(0, entryName.length() - CLASS_FILE_EXTENSION.length()).replace('/', '.');
				if (!filter.test(className)) {
					continue;
				}

				// Try to load the class:
				// Log.info(" " + className);
				try {
					Class.forName(className);
				} catch (LinkageError | ClassNotFoundException e) {
					// ClassNotFoundException: Not expected here.
					// LinkageError: If some class dependency could not be found.
					logger.log(Level.WARNING, "Could not load class '" + className + "' from jar file '" + jarFile.getPath() + "'.", e);
					// Continue loading any other remaining classes.
				}
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, "Could not load classes from jar file '" + jarFile.getPath() + "'.", e);
			return false;
		}
		return true;
	}
}
