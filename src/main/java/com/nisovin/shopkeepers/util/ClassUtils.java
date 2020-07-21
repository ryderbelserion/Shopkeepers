package com.nisovin.shopkeepers.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ClassUtils {

	private static final String CLASS_FILE_EXTENSION = ".class";

	private ClassUtils() {
	}

	/**
	 * Loads all classes from the given jar file.
	 * 
	 * @param jarFile
	 *            the jar file
	 * @param filter
	 *            only classes whose names are accepted by this filter are loaded
	 * @return <code>true</code> on (potentially partial) success and <code>false</code> on failure
	 */
	public static boolean loadAllClassesFromJar(File jarFile, Predicate<String> filter) {
		Validate.notNull(jarFile, "jarFile is null");
		if (filter == null) filter = (className) -> true;

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
					Log.warning("Could not load class '" + className + "' from jar file '" + jarFile.getPath() + "'.", e);
					// Continue loading any other remaining classes.
				}
			}
		} catch (IOException e) {
			Log.warning("Could not load classes from jar file '" + jarFile.getPath() + "'.", e);
			return false;
		}
		return true;
	}
}
