package com.nisovin.shopkeepers.config.lib.value;

import org.bukkit.entity.EntityType;

/**
 * This exception is thrown by {@link ValueType#load(Object)} when an unknown {@link EntityType} is encountered.
 */
public class UnknownEntityTypeException extends ValueLoadException {

	private static final long serialVersionUID = 4248498809095698671L;

	public UnknownEntityTypeException(String message) {
		super(message);
	}

	public UnknownEntityTypeException(String message, Throwable cause) {
		super(message, cause);
	}
}
