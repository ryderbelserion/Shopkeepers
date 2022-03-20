package com.nisovin.shopkeepers.config.lib.value;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Thrown by {@link ValueType#parse(String)} if the value cannot be parsed.
 */
public class ValueParseException extends Exception {

	private static final long serialVersionUID = 4926921137692692427L;

	public ValueParseException(@Nullable String message) {
		super(message);
	}

	public ValueParseException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}
}
