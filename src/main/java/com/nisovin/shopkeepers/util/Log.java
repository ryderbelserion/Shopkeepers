package com.nisovin.shopkeepers.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.nisovin.shopkeepers.Settings;

public final class Log {

	private Log() {
	}

	// gets set early on plugin startup and then (ideally) never unset
	// -> volatile is therefore not expected to be required
	private static Logger logger = null;

	public static void setLogger(Logger logger) {
		Log.logger = logger;
	}

	public static Logger getLogger() {
		return logger;
	}

	public static void info(String message) {
		if (message == null || message.isEmpty()) return;
		getLogger().info(message);
	}

	public static void debug(String message) {
		if (Settings.isDebugging()) {
			info(message);
		}
	}

	public static void warning(String message) {
		getLogger().warning(message);
	}

	public static void warning(String message, Throwable throwable) {
		getLogger().log(Level.WARNING, message, throwable);
	}

	public static void severe(String message) {
		getLogger().severe(message);
	}

	public static void severe(String message, Throwable throwable) {
		getLogger().log(Level.SEVERE, message, throwable);
	}
}
