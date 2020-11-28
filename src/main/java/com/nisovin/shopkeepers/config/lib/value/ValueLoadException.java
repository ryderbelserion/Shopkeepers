package com.nisovin.shopkeepers.config.lib.value;

public class ValueLoadException extends Exception {

	private static final long serialVersionUID = -3068903999888105245L;

	public ValueLoadException(String message) {
		super(message);
	}

	public ValueLoadException(String message, Throwable cause) {
		super(message, cause);
	}
}
