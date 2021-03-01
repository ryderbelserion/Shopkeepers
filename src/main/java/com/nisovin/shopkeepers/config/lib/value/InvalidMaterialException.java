package com.nisovin.shopkeepers.config.lib.value;

import org.bukkit.Material;

/**
 * This exception is thrown by {@link ValueType#load(Object)} when an unknown {@link Material} is encountered, as well
 * as for unsupported {@link Material#isLegacy() legacy} Materials.
 */
public class InvalidMaterialException extends ValueLoadException {

	private static final long serialVersionUID = 1653518607452366268L;

	public InvalidMaterialException(String message) {
		super(message);
	}

	public InvalidMaterialException(String message, Throwable cause) {
		super(message, cause);
	}
}
