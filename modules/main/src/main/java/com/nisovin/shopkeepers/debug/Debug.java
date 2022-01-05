package com.nisovin.shopkeepers.debug;

import com.nisovin.shopkeepers.config.Settings;

/**
 * Access to the debugging state.
 */
public final class Debug {

	public static boolean isDebugging() {
		return isDebugging(null);
	}

	public static boolean isDebugging(String option) {
		// Thread-safe:
		Settings.AsyncSettings settings = Settings.async();
		return settings.debug && (option == null || settings.debugOptions.contains(option));
	}

	private Debug() {
	}
}
