package com.nisovin.shopkeepers.config.value;

public class SettingLoadException extends Exception {

	private static final long serialVersionUID = -3068903999888105245L;

	public SettingLoadException(String message) {
		super(message);
	}

	public SettingLoadException(String message, Throwable cause) {
		super(message, cause);
	}
}
