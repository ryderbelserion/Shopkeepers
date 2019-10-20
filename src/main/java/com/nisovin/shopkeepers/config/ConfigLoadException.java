package com.nisovin.shopkeepers.config;

/**
 * This exception gets used if a severe issue prevents loading a configuration.
 */
public class ConfigLoadException extends Exception {

	private static final long serialVersionUID = 3283134205506144514L;

	public ConfigLoadException(String message) {
		super(message);
	}

	public ConfigLoadException(String message, Throwable cause) {
		super(message, cause);
	}
}
