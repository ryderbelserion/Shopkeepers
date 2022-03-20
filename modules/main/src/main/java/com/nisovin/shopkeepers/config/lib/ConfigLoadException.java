package com.nisovin.shopkeepers.config.lib;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This exception is thrown when a {@link Config} cannot be loaded due to some severe issue.
 */
public class ConfigLoadException extends Exception {

	private static final long serialVersionUID = 3283134205506144514L;

	/**
	 * Creates a new {@link ConfigLoadException}.
	 * 
	 * @param message
	 *            the detail message
	 */
	public ConfigLoadException(@Nullable String message) {
		super(message);
	}

	/**
	 * Creates a new {@link ConfigLoadException}.
	 * 
	 * @param message
	 *            the detail message
	 * @param cause
	 *            the cause
	 */
	public ConfigLoadException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}
}
