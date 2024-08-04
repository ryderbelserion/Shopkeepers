package com.nisovin.shopkeepers.util.logging;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.debug.Debug;
import com.nisovin.shopkeepers.util.java.Validate;

public final class Log {

	// Gets set early on plugin startup and then (ideally) never unset.
	// -> Volatile is therefore not expected to be required.
	private static @Nullable Logger logger = null;

	public static void setLogger(@Nullable Logger logger) {
		Log.logger = logger;
	}

	public static Logger getLogger() {
		return Validate.State.notNull(logger, "No logger instance set!");
	}

	public static void info(String message) {
		getLogger().info(message);
	}

	public static void info(Supplier<@Nullable String> msgSupplier) {
		getLogger().info(msgSupplier);
	}

	public static void info(String message, @Nullable Throwable throwable) {
		getLogger().log(Level.INFO, message, throwable);
	}

	public static void info(@Nullable Throwable throwable, Supplier<@Nullable String> msgSupplier) {
		getLogger().log(Level.INFO, throwable, msgSupplier);
	}

	public static void debug(String message) {
		debug(null, message);
	}

	public static void debug(Supplier<@Nullable String> msgSupplier) {
		debug((String) null, msgSupplier);
	}

	public static void debug(String message, @Nullable Throwable throwable) {
		debug(null, message, throwable);
	}

	public static void debug(@Nullable Throwable throwable, Supplier<@Nullable String> msgSupplier) {
		debug(null, throwable, msgSupplier);
	}

	public static void debug(@Nullable String debugOption, String message) {
		if (Debug.isDebugging(debugOption)) {
			info(message);
		}
	}

	public static void debug(@Nullable String debugOption, Supplier<@Nullable String> msgSupplier) {
		if (Debug.isDebugging(debugOption)) {
			info(msgSupplier);
		}
	}

	public static void debug(
			@Nullable String debugOption,
			String message,
			@Nullable Throwable throwable
	) {
		if (Debug.isDebugging(debugOption)) {
			info(message, throwable);
		}
	}

	public static void debug(
			@Nullable String debugOption,
			@Nullable Throwable throwable,
			Supplier<@Nullable String> msgSupplier
	) {
		if (Debug.isDebugging(debugOption)) {
			info(throwable, msgSupplier);
		}
	}

	public static void warning(String message) {
		getLogger().warning(message);
	}

	public static void warning(Supplier<@Nullable String> msgSupplier) {
		getLogger().warning(msgSupplier);
	}

	public static void warning(String message, @Nullable Throwable throwable) {
		getLogger().log(Level.WARNING, message, throwable);
	}

	public static void warning(@Nullable Throwable throwable, Supplier<@Nullable String> msgSupplier) {
		getLogger().log(Level.WARNING, throwable, msgSupplier);
	}

	public static void severe(String message) {
		getLogger().severe(message);
	}

	public static void severe(Supplier<@Nullable String> msgSupplier) {
		getLogger().severe(msgSupplier);
	}

	public static void severe(String message, @Nullable Throwable throwable) {
		getLogger().log(Level.SEVERE, message, throwable);
	}

	public static void severe(@Nullable Throwable throwable, Supplier<@Nullable String> msgSupplier) {
		getLogger().log(Level.SEVERE, throwable, msgSupplier);
	}

	private Log() {
	}
}
