package com.nisovin.shopkeepers.config.lib.value;

import org.bukkit.entity.EntityType;

/**
 * This exception is thrown by {@link ValueType#load(Object)} when an unknown {@link EntityType} is encountered.
 */
public class InvalidEntityTypeException extends ValueLoadException {

	private static final long serialVersionUID = 4248498809095698671L;

	public InvalidEntityTypeException(String message) {
		super(message);
	}

	public InvalidEntityTypeException(String message, Throwable cause) {
		super(message, cause);
	}
}
