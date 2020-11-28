package com.nisovin.shopkeepers.config.value;

import java.util.Collections;
import java.util.List;

public class SettingLoadException extends Exception {

	private static final long serialVersionUID = -3068903999888105245L;

	private final List<String> extraMessages;

	public SettingLoadException(String message) {
		this(message, Collections.emptyList());
	}

	public SettingLoadException(String message, List<String> extraMessages) {
		super(message);
		this.extraMessages = extraMessages;
	}

	public SettingLoadException(String message, Throwable cause) {
		this(message, Collections.emptyList(), cause);
	}

	public SettingLoadException(String message, List<String> extraMessages, Throwable cause) {
		super(message, cause);
		this.extraMessages = extraMessages;
	}

	public List<String> getExtraMessages() {
		return extraMessages;
	}
}
