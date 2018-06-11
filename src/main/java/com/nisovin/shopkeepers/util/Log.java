package com.nisovin.shopkeepers.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;

public final class Log {

	private Log() {
	}

	public static Logger getLogger() {
		return ShopkeepersPlugin.getInstance().getLogger();
	}

	public static void info(String message) {
		if (message == null || message.isEmpty()) return;
		getLogger().info(message);
	}

	public static void debug(String message) {
		if (Settings.debug) {
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
