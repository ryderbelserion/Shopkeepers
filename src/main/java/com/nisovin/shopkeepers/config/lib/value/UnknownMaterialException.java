package com.nisovin.shopkeepers.config.lib.value;

import org.bukkit.Material;

/**
 * This exception is issued by {@link ValueType#load(Object)} when an unknown {@link Material} is encountered.
 */
public class UnknownMaterialException extends ValueLoadException {

	private static final long serialVersionUID = 1653518607452366268L;

	public UnknownMaterialException(String message) {
		super(message);
	}

	public UnknownMaterialException(String message, Throwable cause) {
		super(message, cause);
	}
}
