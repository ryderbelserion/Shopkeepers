package com.nisovin.shopkeepers.config.lib.value;

import java.util.Collections;
import java.util.List;

public class ValueLoadException extends Exception {

	private static final long serialVersionUID = -3068903999888105245L;

	private final List<String> extraMessages;

	public ValueLoadException(String message) {
		this(message, null, null);
	}

	public ValueLoadException(String message, Throwable cause) {
		this(message, null, cause);
	}

	public ValueLoadException(String message, List<String> extraMessages) {
		this(message, extraMessages, null);
	}

	public ValueLoadException(String message, List<String> extraMessages, Throwable cause) {
		super(message, cause);
		this.extraMessages = (extraMessages != null) ? extraMessages : Collections.emptyList();
	}

	/**
	 * Gets additional detail messages for this exception.
	 * 
	 * @return additional detail messages, not <code>null</code> but can be empty
	 */
	public List<String> getExtraMessages() {
		return extraMessages;
	}
}
