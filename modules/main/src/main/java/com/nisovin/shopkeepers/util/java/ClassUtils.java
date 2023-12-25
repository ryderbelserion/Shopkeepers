package com.nisovin.shopkeepers.util.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;

public final class ClassUtils {

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
		Validate.isTrue(primitive.isPrimitive(), "primitive is not a primitive type");
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

	public static boolean isAssignableFrom(Class<?> to, @Nullable Object value) {
		if (value != null) {
			return isAssignableFrom(to, value.getClass());
		} else {
			return !to.isPrimitive();
		}
	}

	/**
	 * Casts the given {@link Class} to a class with a more specific type argument.
	 * <p>
	 * The cast is unchecked. This method is especially useful when working with class literals of
	 * generic types: These class literals are raw, unparameterized types, but we may require a
	 * class with a type argument that matches some specific parameterization.
	 * 
	 * @param <T>
	 *            the input type argument
	 * @param <U>
	 *            the output type argument
	 * @param clazz
	 *            the input class
	 * @return the class after casting
	 */
	@SuppressWarnings("unchecked")
	public static <T, U extends T> Class<U> parameterized(Class<T> clazz) {
		return (Class<U>) clazz;
	}

	/**
	 * Gets the {@link Class#getSimpleName() simple name} of the given class, but accounts for
	 * {@link Class#isAnonymousClass() anonymous} classes and arrays with anonymous component types.
	 * <p>
	 * If the given class is {@link Class#isAnonymousClass() anonymous}, this returns the last
	 * segment (after the last dot) of the {@link Class#getName() full class name}.
	 * <p>
	 * If the given class represents an array, this returns the {@link #getSimpleTypeName(Class)
	 * simple type name} of the component type with "[]" appended.
	 * 
	 * @param clazz
	 *            the class, not <code>null</code>
	 * @return the simple type name, not <code>null</code> or empty
	 */
	public static String getSimpleTypeName(Class<?> clazz) {
		Validate.notNull(clazz, "clazz is null");
		if (clazz.isArray()) {
			Class<?> componentType = Unsafe.assertNonNull(clazz.getComponentType());
			return getSimpleTypeName(componentType) + "[]";
		}

		if (clazz.isAnonymousClass()) {
			String name = clazz.getName();
			return name.substring(name.lastIndexOf('.') + 1);
		}

		return clazz.getSimpleName();
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
	public static boolean loadAllClassesFromJar(
			File jarFile,
			Predicate<String> filter,
			Logger logger
	) {
		Validate.notNull(jarFile, "jarFile is null");
		Validate.notNull(filter, "filter is null");
		Validate.notNull(logger, "logger is null");

		try (ZipInputStream jar = new ZipInputStream(new FileInputStream(jarFile))) {
			for (ZipEntry entry = jar.getNextEntry(); entry != null; entry = jar.getNextEntry()) {
				if (entry.isDirectory()) continue;
				String entryName = entry.getName();
				if (!entryName.endsWith(CLASS_FILE_EXTENSION)) continue;

				// Check filter:
				String className = entryName.substring(
						0,
						entryName.length() - CLASS_FILE_EXTENSION.length()
				).replace('/', '.');
				if (!filter.test(className)) {
					continue;
				}

				// Try to load the class:
				// Log.info(" " + className);
				try {
					Class.forName(className);
				} catch (LinkageError | ClassNotFoundException e) {
					// ClassNotFoundException: Not expected here.
					// LinkageError: If some class dependency could not be found, or if some static
					// class initialization code fails.
					logger.log(Level.WARNING, "Could not load class '" + className
							+ "' from jar file '" + jarFile.getPath() + "'.", e);
					// Continue loading any other remaining classes.
				}
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, "Could not load classes from jar file '" + jarFile.getPath()
					+ "'.", e);
			return false;
		}
		return true;
	}

	public static ClassLoader getClassLoader(Class<?> clazz) {
		Validate.notNull(clazz, "clazz is null");
		ClassLoader classLoader = clazz.getClassLoader();
		return Validate.notNull(classLoader, "The ClassLoader of clazz is null");
	}

	public static InputStream getResource(Class<?> clazz, String resourcePath) {
		ClassLoader classLoader = getClassLoader(clazz);
		InputStream resource = classLoader.getResourceAsStream(resourcePath);
		if (resource == null) {
			throw new MissingResourceException(
					"Missing resource: " + resourcePath,
					clazz.getName(),
					resourcePath
			);
		}
		return resource;
	}

	public static @Nullable Class<?> getClassOrNull(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	private ClassUtils() {
	}
}
